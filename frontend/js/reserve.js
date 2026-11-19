'use strict';

/* Confirms the customer's active hold. The countdown uses the server's expiry time.
   Confirm starts the rental (POST /rentals/start). Cancel releases the PowerBank (DELETE /reservations/{id}). */

const CIRCLE = 2 * Math.PI * 80;
let hold = null;
let timer = null;

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    if (!PB.requireLogin()) return;

    document.getElementById('confirm-btn').addEventListener('click', confirmHold);
    document.getElementById('cancel-btn').addEventListener('click', cancelHold);

    try {
        const [holds, pricing] = await Promise.all([
            PB.api.reservations.active(),
            PB.api.pricing.active().catch(() => null)
        ]);
        if (!holds.length) {
            show('empty');
            return;
        }
        hold = holds[0];
        render(hold, pricing);
    } catch (err) {
        showBanner(err.message);
        show('empty');
    }
});

function render(h, p) {
    document.getElementById('subtitle').innerHTML =
        `We're holding unit <strong>${PB.esc(h.powerBankSerial)}</strong> for you at <strong>${PB.esc(h.stationName)}</strong>.`;
    setText('unit-serial', h.powerBankSerial);
    setText('station-name', h.stationName);
    setText('station-address', h.stationAddress);
    document.getElementById('station-link').href = `station-details.html?id=${h.stationId}`;
    document.getElementById('back-link').href = `station-details.html?id=${h.stationId}`;
    setText('hold-minutes', h.durationMinutes);

    if (p) {
        setText('rate-hour', PB.money(p.pricePerHour) + ' / hr');
        setText('free-period', p.freeMinutes ? `First ${p.freeMinutes} min` : 'None');
        setText('rate-daily', p.dailyMaxFee != null ? PB.money(p.dailyMaxFee) : '—');
        setText('rate-reserve', PB.money(p.reservationFee));
    }

    show('content');
    tick();
    timer = setInterval(tick, 1000);
}

function tick() {
    const remaining = Math.max(0, Math.floor((new Date(hold.expiresAt) - Date.now()) / 1000));
    const m = String(Math.floor(remaining / 60)).padStart(2, '0');
    const s = String(remaining % 60).padStart(2, '0');
    setText('countdown', `${m}:${s}`);

    const total = hold.durationMinutes * 60;
    const fraction = Math.min(1, remaining / total);
    const bar = document.getElementById('ring-bar');
    bar.style.strokeDasharray = CIRCLE;
    bar.style.strokeDashoffset = CIRCLE * (1 - fraction);

    const expired = remaining === 0;
    bar.classList.toggle('expired', expired);
    setText('hold-state', expired ? 'Hold expired' : 'Unit locked');
    document.getElementById('confirm-btn').disabled = expired;

    if (expired) {
        clearInterval(timer);
        showBanner('This reservation has expired. Reserve again from the station page.');
    }
}

async function confirmHold() {
    if (!hold) return;
    try {
        await PB.api.rentals.start(hold.id);
        PB.toast('Rental started. Return the PowerBank when you are done.');
        location.href = 'active-rental.html';
    } catch (err) {
        showBanner(err.message);
    }
}

async function cancelHold() {
    if (!hold || !confirm('Cancel this reservation? The PowerBank will be released.')) return;
    try {
        await PB.api.reservations.cancel(hold.id);
        PB.toast('Reservation cancelled.');
        location.href = 'map.html';
    } catch (err) {
        showBanner(err.message);
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
