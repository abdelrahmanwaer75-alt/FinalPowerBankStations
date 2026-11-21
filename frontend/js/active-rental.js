'use strict';

/* Active rental: the server holds the start time and the pricing snapshot.
   The page ticks every second and re-reads the rental every 30 seconds to refresh the cost. */

const RING = 2 * Math.PI * 108;
let rental = null;
let activeRentals = [];
let activeHolds = [];
let selectedHold = null;
let stations = [];
let pricing = null;
let refreshTimer = null;
let holdTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    if (!PB.requireLogin({
        inPlace: true,
        title: 'Sign in to see your active rental',
        message: 'Your rental details and return options are available after you sign in.'
    })) return;

    document.getElementById('return-open').addEventListener('click', openReturn);
    document.getElementById('hold-start').addEventListener('click', startReservedRental);
    document.getElementById('hold-cancel').addEventListener('click', cancelReservation);

    await load();
    refreshTimer = setInterval(load, 30000);
    setInterval(tick, 1000);
    holdTimer = setInterval(tickReservation, 1000);
});

async function load() {
    try {
        const [active, holds, st, p] = await Promise.all([
            PB.api.rentals.active(),
            PB.api.reservations.active(),
            stations.length ? Promise.resolve(stations) : PB.loadStations(),
            pricing ? Promise.resolve(pricing) : PB.api.pricing.active().catch(() => null)
        ]);
        stations = st;
        pricing = p;
        activeRentals = active;
        activeHolds = holds;
        const holdId = selectedHold && holds.some((item) => String(item.id) === String(selectedHold.id))
            ? String(selectedHold.id) : null;
        selectedHold = holds.find((item) => String(item.id) === holdId) || holds[0] || null;
        renderReservationPanel();

        if (!active.length) {
            document.getElementById('content').hidden = true;
            document.getElementById('empty').hidden = holds.length > 0;
            return;
        }
        document.getElementById('content').hidden = false;
        document.getElementById('empty').hidden = true;
        const requestedId = new URLSearchParams(location.search).get('rental');
        rental = active.find((item) => String(item.id) === requestedId) || active[0];
        renderRentalPicker();
        render();
    } catch (err) {
        showBanner(err.message);
    }
}

function renderReservationPanel() {
    const panel = document.getElementById('reservation-panel');
    const picker = document.getElementById('hold-picker');
    const pickerLabel = document.getElementById('hold-picker-label');
    if (!panel) return;
    panel.hidden = !selectedHold;
    if (!selectedHold) return;

    if (activeHolds.length > 1) {
        picker.hidden = false;
        pickerLabel.hidden = false;
        picker.innerHTML = activeHolds.map((item) =>
            `<option value="${item.id}">${PB.esc(item.powerBankSerial)} · ${PB.esc(item.stationName)}</option>`
        ).join('');
        picker.value = String(selectedHold.id);
        picker.onchange = () => {
            selectedHold = activeHolds.find((item) => String(item.id) === picker.value) || null;
            renderReservationPanel();
        };
    } else {
        picker.hidden = true;
        pickerLabel.hidden = true;
    }

    setText('hold-summary', `Unit ${selectedHold.powerBankSerial} is reserved for you.`);
    setText('hold-station', selectedHold.stationName);
    setText('hold-address', selectedHold.stationAddress);
    tickReservation();
}

function tickReservation() {
    const countdown = document.getElementById('hold-countdown');
    if (!selectedHold || !countdown) return;
    const remaining = Math.max(0, Math.floor((new Date(selectedHold.expiresAt) - Date.now()) / 1000));
    const minutes = String(Math.floor(remaining / 60)).padStart(2, '0');
    const seconds = String(remaining % 60).padStart(2, '0');
    countdown.textContent = remaining === 0 ? 'Expired' : `${minutes}:${seconds}`;
    document.getElementById('hold-start').disabled = remaining === 0;
}

async function startReservedRental() {
    if (!selectedHold) return;
    const button = document.getElementById('hold-start');
    button.disabled = true;
    try {
        await PB.api.rentals.start(selectedHold.id);
        PB.toast('Rental started. It is now listed with your active PowerBanks.');
        selectedHold = null;
        await load();
    } catch (error) {
        showBanner(error.message);
        tickReservation();
    }
}

async function cancelReservation() {
    if (!selectedHold) return;
    const button = document.getElementById('hold-cancel');
    button.disabled = true;
    try {
        await PB.api.reservations.cancel(selectedHold.id);
        PB.toast('Reservation cancelled.');
        selectedHold = null;
        await load();
    } catch (error) {
        showBanner(error.message);
    } finally {
        button.disabled = false;
    }
}

function renderRentalPicker() {
    const wrap = document.getElementById('rental-picker-wrap');
    const picker = document.getElementById('rental-picker');
    if (!wrap || !picker) return;
    if (activeRentals.length < 2) { wrap.hidden = true; return; }
    wrap.hidden = false;
    picker.innerHTML = activeRentals.map((item) =>
        `<option value="${item.id}" ${item.id === rental.id ? 'selected' : ''}>${PB.esc(item.powerBankSerial)} · ${PB.esc(item.pickupStationName)}</option>`
    ).join('');
    picker.value = String(rental.id);
    picker.onchange = () => {
        rental = activeRentals.find((item) => String(item.id) === picker.value);
        if (!rental) return;
        const url = new URL(location.href);
        url.searchParams.set('rental', rental.id);
        history.replaceState(null, '', url);
        render();
    };
}

function render() {
    show('content');
    setText('rental-ref', `Reference ${rental.reference}`);
    setText('unit', rental.powerBankSerial);
    setText('origin', rental.pickupStationName);
    setText('started', PB.dateTime(rental.startTime));
    setText('cost', PB.money(rental.cost));
    setText('rate-note', `Rate ${PB.rateMoney(rental.pricePerMinute)} per minute (${rental.planName})${pricing && pricing.dailyMaxFee ? ' · capped at ' + PB.money(pricing.dailyMaxFee) + ' per day' : ''}`);

    renderStations();
    tick();
}

/* Client-side tick: elapsed time and cycle progress come from the server start time. */
function tick() {
    if (!rental) return;
    const elapsed = Math.max(0, Math.floor((Date.now() - new Date(rental.startTime)) / 1000));
    const h = String(Math.floor(elapsed / 3600)).padStart(2, '0');
    const m = String(Math.floor((elapsed % 3600) / 60)).padStart(2, '0');
    const s = String(elapsed % 60).padStart(2, '0');
    setText('elapsed', `${h}:${m}:${s}`);

    const inCycle = elapsed % 3600;
    const fraction = inCycle / 3600;
    const bar = document.getElementById('ring-bar');
    bar.style.strokeDasharray = RING;
    bar.style.strokeDashoffset = RING * (1 - fraction);

    const nextDue = new Date(new Date(rental.startTime).getTime() + (Math.floor(elapsed / 3600) + 1) * 3600 * 1000);
    setText('next-due', nextDue.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));

    const cap = pricing && pricing.dailyMaxFee ? Number(pricing.dailyMaxFee) : null;
    document.getElementById('cost-bar').style.width = cap ? Math.min(100, (Number(rental.cost) / cap) * 100) + '%' : '0%';
}

function renderStations() {
    const list = document.getElementById('stations');
    const near = stations.filter((s) => s.returnSlots > 0)
        .sort((a, b) => b.returnSlots - a.returnSlots)
        .slice(0, 5);
    list.innerHTML = near.map((s) => `
        <li>
            <div><strong>${PB.esc(s.name)}</strong><small>${PB.esc(s.address)}</small></div>
            <span class="free">
                ${s.returnSlots} free return slots
            </span>
        </li>`).join('') || '<li><small>No stations have a free return slot right now.</small></li>';
}

async function openReturn() {
    if (!rental) return;
    location.href = 'return.html?rental=' + rental.id;
}

function show(id) {
    ['content', 'empty'].forEach((x) => {
        const element = document.getElementById(x);
        if (element) element.hidden = x !== id;
    });
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value ?? '—';
}

function showBanner(message) {
    const el = document.getElementById('banner');
    if (!el) { PB.toast(message, true); return; }
    el.textContent = message;
    el.hidden = false;
}
