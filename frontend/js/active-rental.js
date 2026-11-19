'use strict';

/* Active rental: the server holds the start time and the pricing snapshot.
   The page ticks every second and re-reads the rental every 30 seconds to refresh the cost. */

const RING = 2 * Math.PI * 108;
let rental = null;
let stations = [];
let pricing = null;
let refreshTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    if (!PB.requireLogin()) return;

    document.getElementById('return-open').addEventListener('click', openReturn);
    document.getElementById('return-cancel').addEventListener('click', () => { document.getElementById('return-modal').hidden = true; });
    document.getElementById('return-confirm').addEventListener('click', confirmReturn);

    await load();
    refreshTimer = setInterval(load, 30000);
    setInterval(tick, 1000);
});

async function load() {
    try {
        const [active, st, p] = await Promise.all([
            PB.api.rentals.active(),
            stations.length ? Promise.resolve(stations) : PB.loadStations(),
            pricing ? Promise.resolve(pricing) : PB.api.pricing.active().catch(() => null)
        ]);
        stations = st;
        pricing = p;

        if (!active.length) {
            show('empty');
            clearInterval(refreshTimer);
            return;
        }
        rental = active[0];
        render();
    } catch (err) {
        showBanner(err.message);
    }
}

function render() {
    show('content');
    setText('rental-ref', `Reference ${rental.reference}`);
    setText('unit', rental.powerBankSerial);
    setText('origin', rental.pickupStationName);
    setText('started', PB.dateTime(rental.startTime));
    setText('cost', PB.money(rental.cost));
    setText('rate-note', `Rate ${PB.money(rental.pricePerHour)} per hour (${rental.planName})${pricing && pricing.dailyMaxFee ? ' · capped at ' + PB.money(pricing.dailyMaxFee) + ' per day' : ''}`);

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
    const near = stations.slice(0, 5);
    list.innerHTML = near.map((s) => `
        <li>
            <div><strong>${PB.esc(s.name)}</strong><small>${PB.esc(s.address)}</small></div>
            <span class="${s.availableCount > 0 ? 'free' : 'full'}">
                ${s.availableCount} available
            </span>
        </li>`).join('') || '<li><small>No stations available.</small></li>';
}

async function openReturn() {
    if (!rental) return;
    location.href = 'return.html?rental=' + rental.id;
    return;
    const select = document.getElementById('return-station');
    select.innerHTML = stations.map((s) => `<option value="${s.id}">${PB.esc(s.name)}</option>`).join('');
    document.getElementById('return-modal').hidden = false;
}

async function confirmReturn() {
    if (!rental) return;
    const stationId = Number(document.getElementById('return-station').value);
    const btn = document.getElementById('return-confirm');
    btn.disabled = true;
    try {
        const ret = await PB.api.returns.create(rental.id, stationId);
        sessionStorage.setItem('pb_return', String(ret.id));
        PB.toast(`Returned. Total ${PB.money(ret.finalCost)}. Add photos to finish.`);
        location.href = 'rentals.html?return=' + ret.id;
    } catch (err) {
        PB.toast(err.message, true);
        btn.disabled = false;
    }
}

function show(id) {
    ['content', 'empty'].forEach((x) => { document.getElementById(x).hidden = x !== id; });
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value ?? '—';
}

function showBanner(message) {
    const el = document.getElementById('banner');
    el.textContent = message;
    el.hidden = false;
}
