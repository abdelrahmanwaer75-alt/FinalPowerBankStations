'use strict';

/* Station details: loads one station, its live availability, and the active pricing. */

const params = new URLSearchParams(location.search);
const stationId = Number(params.get('id'));

let current = null;
let plan = null;
let currentAvailability = null;
let waitlistEntry = null;
let waitlistTimer = null;

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
    document.getElementById('waitlist-btn').addEventListener('click', () => {
        if (!current) return;
        const button = document.getElementById('waitlist-btn');
        button.disabled = true;
        PB.joinWaitlist(current.id, current.name).finally(() => { button.disabled = false; });
    });
    document.getElementById('waitlist-leave').addEventListener('click', leaveWaitlist);
    window.addEventListener('pb:waitlist-updated', (event) => {
        if (Number(event.detail.stationId) !== stationId) return;
        waitlistEntry = event.detail;
        renderWaitlistPanel();
    });
    window.addEventListener('pb:station-availability-changed', (event) => {
        if (Number(event.detail?.stationId) === stationId) refreshWaitlist();
    });

    try {
        const [station, availability, pricing] = await Promise.all([
            PB.api.stations.get(stationId),
            PB.api.stations.availability(stationId),
            PB.api.pricing.active().catch(() => null)
        ]);
        current = station;
        plan = pricing;
        if (PB.api.hasToken()) {
            const entries = await PB.api.waitlist.mine().catch(() => []);
            waitlistEntry = entries.find((entry) => Number(entry.stationId) === stationId) || null;
        }
        render(station, availability);
        if (PB.api.hasToken()) waitlistTimer = setInterval(refreshWaitlist, 20000);
    } catch (err) {
        showBanner(err.message);
        setText('station-name', 'Station not available');
    }
});

function render(s, a) {
    document.title = `${s.name} | PowerBank Stations`;
    setText('crumb-name', s.name);
    setText('station-name', s.name);
    setText('station-address', `${s.address}, ${s.city}`);
    setText('station-meta', s.description || '');

    renderAvailability(s, a);
    setText('stat-slots', a.returnSlotsLeft);
    setText('stat-capacity', a.totalCapacity);
    setText('stat-capacity-note', `${a.rented} rented right now`);

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
    renderWaitlistPanel();

    loadMap(s);
}

function renderAvailability(station, availability) {
    currentAvailability = availability;
    const open = station.status === 'ACTIVE';
    const hasUnits = Number(availability.available) > 0;
    document.getElementById('chips').innerHTML = [
        `<span class="chip">Station #${station.id}</span>`,
        open ? '<span class="chip ok">Open</span>' : '<span class="chip bad">Closed</span>',
        hasUnits ? '' : '<span class="chip warn">No units available</span>'
    ].join('');
    setText('stat-available', availability.available);
    setText('stat-available-note', hasUnits ? 'Ready to rent' : 'Empty');
    const bays = document.getElementById('bays');
    bays.innerHTML = Array.from({ length: availability.totalCapacity }, (_, i) => {
        const on = i < availability.available;
        return `<div class="bay ${on ? 'on' : 'off'}">${i + 1}</div>`;
    }).join('');
    const reserve = document.getElementById('reserve-btn');
    reserve.disabled = !open || !hasUnits;
    reserve.textContent = !open ? 'Station closed' : hasUnits ? 'Reserve a PowerBank' : 'No units available';
    const waitlist = document.getElementById('waitlist-btn');
    waitlist.hidden = !open || hasUnits || !!waitlistEntry;
}

function renderWaitlistPanel() {
    const panel = document.getElementById('waitlist-panel');
    const copy = document.getElementById('waitlist-copy');
    if (!panel || !copy) return;
    panel.hidden = !waitlistEntry;
    if (!waitlistEntry) return;
    copy.textContent = waitlistEntry.status === 'NOTIFIED' && Number(currentAvailability?.available) > 0
        ? 'A PowerBank was released at this station. Check the live availability above and reserve it while it is available.'
        : `You are in the waiting list${waitlistEntry.queuePosition ? ` at position ${waitlistEntry.queuePosition}` : ''}. We will update this page when stock is released.`;
    if (current) document.getElementById('waitlist-btn').hidden = true;
}

async function refreshWaitlist() {
    if (!PB.api.hasToken()) return;
    try {
        const [entries, availability] = await Promise.all([
            PB.api.waitlist.mine(),
            PB.api.stations.availability(stationId)
        ]);
        waitlistEntry = entries.find((entry) => Number(entry.stationId) === stationId) || null;
        if (current) renderAvailability(current, availability);
        renderWaitlistPanel();
    } catch (_) { /* Keep the current waitlist state visible during a temporary outage. */ }
}

async function leaveWaitlist() {
    if (!waitlistEntry) return;
    const button = document.getElementById('waitlist-leave');
    button.disabled = true;
    try {
        await PB.api.waitlist.leave(waitlistEntry.id);
        waitlistEntry = null;
        renderWaitlistPanel();
        if (current) {
            const availability = await PB.api.stations.availability(stationId);
            renderAvailability(current, availability);
        }
        PB.toast('You left the station waiting list.');
    } catch (error) {
        showBanner(error.message);
    } finally {
        button.disabled = false;
    }
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
