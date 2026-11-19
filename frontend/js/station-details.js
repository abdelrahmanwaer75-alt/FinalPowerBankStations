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
    directions.href = `https://www.google.com/maps/dir/?api=1&destination=${s.latitude},${s.longitude}`;

    if (plan) {
        setText('rate-hour', PB.money(plan.pricePerHour));
        setText('rate-unit', `per ${plan.freeMinutes ? 'hour, after ' + plan.freeMinutes + ' free minutes' : 'hour'}`);
        setText('rate-daily', plan.dailyMaxFee != null ? PB.money(plan.dailyMaxFee) : '—');
        setText('rate-free', plan.freeMinutes ? `${plan.freeMinutes} minutes free` : '');
        setText('rate-reserve', PB.money(plan.reservationFee));
    }

    setText('hours', s.openingHours || '—');
    setText('contact', s.contactPhone || '—');

    loadMap(s);
}

function loadMap(s) {
    const key = (window.APP_CONFIG && window.APP_CONFIG.GOOGLE_MAPS_BROWSER_KEY) || '';
    const box = document.getElementById('map');
    if (!key) {
        setText('map-note', 'Add the Google Maps browser key in js/config.js to show the map.');
        return;
    }
    const init = () => {
        const center = { lat: Number(s.latitude), lng: Number(s.longitude) };
        const map = new google.maps.Map(box, { center, zoom: 16, disableDefaultUI: true });
        new google.maps.Marker({ position: center, map, title: s.name });
    };
    if (window.google && window.google.maps) return init();
    window.__initDetailsMap = init;
    const script = document.createElement('script');
    script.async = true;
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(key)}&callback=__initDetailsMap&region=EG`;
    script.onerror = () => setText('map-note', 'Google Maps could not be loaded.');
    document.head.appendChild(script);
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
