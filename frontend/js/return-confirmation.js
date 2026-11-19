'use strict';

/* Return confirmation: reads the return and its rental from the backend. Every number shown is stored data. */

const returnId = Number(new URLSearchParams(location.search).get('return'));

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    if (!PB.requireLogin()) return;
    if (!returnId) return show('empty');

    try {
        const ret = await PB.api.returns.get(returnId);
        const rental = await PB.api.rentals.get(ret.rentalId);
        render(ret, rental);
    } catch (err) {
        if (err.status === 404) show('empty');
        else showBanner(err.message);
    }
});

function render(ret, rental) {
    show('content');
    show('done');

    const start = PB.dateTime(rental.startTime);
    const end = PB.dateTime(ret.returnedAt);
    const mins = ret.durationMinutes ?? rental.elapsedMinutes;
    const hours = Math.floor(mins / 60);
    const rest = mins % 60;

    setText('done-line', `${rental.powerBankSerial} is back at ${ret.returnStationName}.`);
    setText('status-pill', ret.status.replace(/_/g, ' ').toLowerCase());
    setText('ref', ret.reference);
    setText('returned-at', end);
    setText('duration', hours ? `${hours}h ${rest}m` : `${rest}m`);
    setText('duration-range', `${start} → ${end}`);
    setText('rate', PB.money(rental.pricePerHour));
    setText('total', PB.money(ret.finalCost));
    setText('unit', rental.powerBankSerial);
    setText('pickup', rental.pickupStationName);
    setText('return-station', ret.returnStationName);
    setText('photos', String(ret.images.length));
    setText('inspection', ret.status.replace(/_/g, ' ').toLowerCase());
}

function show(id) {
    document.getElementById(id).hidden = false;
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
