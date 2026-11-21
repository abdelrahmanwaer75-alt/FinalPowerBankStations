'use strict';

/* Station details: loads one station, its live availability, and the active pricing. */

const params = new URLSearchParams(location.search);
const stationId = Number(params.get('id'));

let current = null;
let plan = null;

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();

    if (!stationId) {
        showBanner('No station selected. Go back to the map and choose a station.');
        setText('station-name', 'Station not found');
        return;
    }

    document.getElementById('reserve-btn').addEventListener('click', () => {
        if (current) PB.reserveStation(current.id, current.name, Number(document.getElementById('duration').value));
    });

    try {
        const [station, availability, pricing] = await Promise.all([
            PB.api.stations.get(stationId),
            PB.api.stations.availability(stationId),
            PB.api.pricing.active().catch(() => null)
        ]);
        current = station;
        plan = pricing;
        render(station, availability);
    } catch (err) {
        showBanner(err.message);
        setText('station-name', 'Station not available');
    }
});

function render(s, a) {
    const open = s.status === 'ACTIVE';
    const hasUnits = a.available > 0;

    document.title = `${s.name} | PowerBank Stations`;
    setText('crumb-name', s.name);
    setText('station-name', s.name);
    setText('station-address', `${s.address}, ${s.city}`);
    setText('station-meta', s.description || '');

    document.getElementById('chips').innerHTML = [
        `<span class="chip">Station #${s.id}</span>`,
        open ? '<span class="chip ok">Open</span>' : '<span class="chip bad">Closed</span>',
        hasUnits ? '' : '<span class="chip warn">No units available</span>'
    ].join('');

    setText('stat-available', a.available);
    setText('stat-available-note', hasUnits ? 'Ready to rent' : 'Empty');
    setText('stat-slots', a.returnSlotsLeft);
    setText('stat-capacity', a.totalCapacity);
    setText('stat-capacity-note', `${a.rented} rented right now`);

    // Slot matrix: one cell per docking slot, filled for each available device.
    const bays = document.getElementById('bays');
    bays.innerHTML = Array.from({ length: a.totalCapacity }, (_, i) => {
        const on = i < a.available;
        return `<div class="bay ${on ? 'on' : 'off'}">${i + 1}</div>`;
    }).join('');

    const reserve = document.getElementById('reserve-btn');
    reserve.disabled = !open || !hasUnits;
    reserve.textContent = !open ? 'Station closed' : hasUnits ? 'Reserve a PowerBank' : 'No units available';

    const directions = document.getElementById('directions');
    directions.href = `https://www.openstreetmap.org/?mlat=${encodeURIComponent(s.latitude)}&mlon=${encodeURIComponent(s.longitude)}#map=17/${encodeURIComponent(s.latitude)}/${encodeURIComponent(s.longitude)}`;
    directions.textContent = 'Open in OpenStreetMap';
    document.getElementById('route-link').href = `route.html?station=${encodeURIComponent(s.id)}`;

    if (plan) {
        setText('rate-hour', PB.rateMoney(plan.pricePerMinute));
        setText('rate-unit', `per minute${plan.freeMinutes ? ', after ' + plan.freeMinutes + ' free minutes' : ''}`);
        setText('rate-daily', plan.dailyMaxFee != null ? PB.money(plan.dailyMaxFee) : '—');
        setText('rate-free', plan.freeMinutes ? `${plan.freeMinutes} minutes free` : '');
        setText('rate-reserve', PB.money(plan.reservationFee));
    }

    setText('hours', s.openingHours || '—');
    setText('contact', s.contactPhone || '—');

    loadMap(s);
}

function loadMap(s) {
    const box = document.getElementById('map');
    if (!window.L) {
        setText('map-note', 'Leaflet could not be loaded. Check your internet connection and reload the page.');
        return;
    }
    const lat = Number(s.latitude);
    const lng = Number(s.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
        setText('map-note', 'This station does not have valid map coordinates.');
        return;
    }
    const map = L.map(box, { scrollWheelZoom: false }).setView([lat, lng], 16);
    const note = document.getElementById('map-note');
    if (note) note.hidden = true;
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);
    L.marker([lat, lng]).addTo(map).bindPopup(PB.esc(s.name)).openPopup();
    setText('map-note', 'Map data © OpenStreetMap contributors.');
    setTimeout(() => map.invalidateSize(), 0);
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
