'use strict';

/* Stations map: Leaflet + OpenStreetMap, list, filters, location, and reserving.
   Data comes from the backend. If the backend is unreachable, the sample data is used and a banner says so. */

const CAIRO_CENTER = { lat: 30.0444, lng: 31.2357 };
const DEFAULT_ZOOM = 12;

const state = {
    query: '',
    radiusKm: 3,        // Infinity means "All Cairo"
    unitsOnly: false,
    mode: 'all',        // all | units | returns
    selectedId: null,
    userPos: null
};

let STATIONS = [];
let map = null;
let userMarker = null;
const markers = new Map();

const $ = (id) => document.getElementById(id);
const esc = (v) => PB.esc(v);

/* ---------- distance ---------- */

function distanceKm(a, b) {
    const R = 6371;
    const toRad = (d) => (d * Math.PI) / 180;
    const dLat = toRad(b.lat - a.lat);
    const dLng = toRad(b.lng - a.lng);
    const h = Math.sin(dLat / 2) ** 2 +
        Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(h));
}

const origin = () => state.userPos || CAIRO_CENTER;
const formatDistance = (km) => (km < 1 ? `${Math.round(km * 1000)} m` : `${km.toFixed(1)} km`);

/* ---------- filtering ---------- */

function withDistance() {
    const from = origin();
    return STATIONS.map((s) => ({ ...s, distance: distanceKm(from, s) }));
}

function filteredStations() {
    const q = state.query.trim().toLowerCase();
    return withDistance()
        .filter((s) => {
            if (q && !`${s.name} ${s.address}`.toLowerCase().includes(q)) return false;
            if (s.distance > state.radiusKm) return false;
            if (state.unitsOnly && s.available === 0) return false;
            if (state.mode === 'units' && s.available === 0) return false;
            if (state.mode === 'returns' && !(s.returnSlots > 0)) return false;
            return true;
        })
        .sort((a, b) => a.distance - b.distance);
}

const stationColor = (s) => (s.available > 0 ? '#006c45' : s.returnSlots > 0 ? '#0057c3' : '#5f6672');

/* ---------- list ---------- */

function renderList() {
    const list = filteredStations();
    const ul = $('station-list');

    $('metric-showing').textContent = `Showing ${list.length} sorted by closest`;
    $('metric-units').textContent = `${list.reduce((n, s) => n + s.available, 0)} PowerBanks`;
    $('metric-empty').textContent = list.some((s) => s.returnSlots == null)
        ? '— Empty bays'
        : `${list.reduce((n, s) => n + s.returnSlots, 0)} Empty bays`;

    if (!list.length) {
        ul.innerHTML = '<li class="empty-list">No stations match these filters.</li>';
        return;
    }

    ul.innerHTML = list.map((s) => {
        const selected = s.id === state.selectedId ? ' is-selected' : '';
        const empty = s.available === 0 ? ' is-empty' : '';
        const availBadge = s.available > 0
            ? `<span class="badge ok">${s.available} available units</span>`
            : '<span class="badge">0 units available</span>';
        const returnBadge = s.returnSlots > 0
            ? `<span class="badge info">${s.returnSlots} return slots</span>`
            : '';
        return `
            <li class="station-card${selected}${empty}" data-id="${esc(s.id)}">
                <div class="card-head">
                    <div>
                        <h2>${esc(s.name)}</h2>
                        <p class="card-address">${esc(s.address)}</p>
                    </div>
                    <span class="card-distance">${formatDistance(s.distance)}</span>
                </div>
                <div class="badges">
                    ${availBadge}${returnBadge}
                    <span class="badge">${esc(s.hours)}</span>
                </div>
                <div class="card-footer">
                    <span class="price">${s.price != null ? `${PB.rateMoney(s.price)} / min` : '—'}</span>
                    <div class="card-actions">
                        <button type="button" class="btn btn-light" data-action="view" data-id="${esc(s.id)}">View on map</button>
                        <a class="btn btn-light" href="station-details.html?id=${esc(s.id)}">Details</a>
                        <button type="button" class="btn btn-primary" data-action="reserve"
                                data-id="${esc(s.id)}" data-name="${esc(s.name)}"
                                ${s.available === 0 ? 'disabled' : ''}>Reserve</button>
                    </div>
                </div>
            </li>`;
    }).join('');
}

function updatePills() {
    $('total-count').textContent = STATIONS.length;
    $('pill-all').textContent = STATIONS.length;
    $('pill-units').textContent = STATIONS.filter((s) => s.available > 0).length;
    $('pill-returns').textContent = STATIONS.filter((s) => s.returnSlots > 0).length;
    $('last-updated').textContent = `Updated ${new Date().toLocaleTimeString()}`;
}

/* ---------- map ---------- */

function infoHtml(s) {
    return `
        <div class="iw">
            <h3>${esc(s.name)}</h3>
            <p>${esc(s.address)} · ${formatDistance(s.distance)}</p>
            <div class="row">
                <span class="badge ok">${s.available} available</span>
                <span class="badge info">${s.returnSlots == null ? 'Availability unknown' : `${s.returnSlots} return slots`}</span>
            </div>
            <p style="margin-top:8px">${esc(s.hours)}${s.price != null ? ` · EGP ${s.price}/hr` : ''}</p>
            <div class="row">
                <button type="button" class="btn btn-primary" data-action="reserve"
                        data-id="${esc(s.id)}" data-name="${esc(s.name)}"
                        ${s.available === 0 ? 'disabled' : ''}>Reserve Now</button>
            </div>
        </div>`;
}

function drawMarkers() {
    markers.forEach((m) => map.removeLayer(m));
    markers.clear();

    STATIONS.forEach((s) => {
        const marker = L.circleMarker([s.lat, s.lng], {
            radius: 15, color: '#ffffff', weight: 2,
            fillColor: stationColor(s), fillOpacity: 1
        }).addTo(map);
        marker.bindTooltip(String(s.available), { permanent: true, direction: 'center', className: 'station-marker-label' });
        marker.bindPopup(infoHtml({ ...s, distance: distanceKm(origin(), s) }));
        marker.on('click', () => selectStation(s.id, false));
        markers.set(s.id, marker);
    });
}

function selectStation(id, pan) {
    state.selectedId = id;
    const s = withDistance().find((x) => x.id === id);
    if (!s) return;

    if (map && markers.has(id)) {
        if (pan) map.panTo([s.lat, s.lng]);
        markers.get(id).setPopupContent(infoHtml(s)).openPopup();
    }

    renderList();
    const card = document.querySelector(`.station-card[data-id="${CSS.escape(String(id))}"]`);
    if (card) card.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

function placeUserMarker() {
    if (!map || !state.userPos) return;
    if (userMarker) map.removeLayer(userMarker);
    userMarker = L.circleMarker([state.userPos.lat, state.userPos.lng], {
        radius: 8, color: '#ffffff', weight: 3, fillColor: '#0057c3', fillOpacity: 1
    }).addTo(map).bindPopup('Your location');
}

function locateUser(thenPan) {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(
        (pos) => {
            state.userPos = { lat: pos.coords.latitude, lng: pos.coords.longitude };
            placeUserMarker();
            if (thenPan && map) map.setView([state.userPos.lat, state.userPos.lng], Math.max(map.getZoom(), 14));
            renderList();
        },
        () => { /* permission denied: distances stay measured from central Cairo */ },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
}

function showFallback(message) {
    $('map-fallback-text').textContent = message;
    $('map-fallback').hidden = false;
}

function showBanner(message) {
    const el = $('data-banner');
    if (el) { el.textContent = message; el.hidden = false; }
}

function initMap() {
    if (!window.L) {
        showFallback('Leaflet could not be loaded. Check your internet connection; the station list is still available.');
        return;
    }
    map = L.map($('map'), { zoomControl: false, scrollWheelZoom: true })
        .setView([CAIRO_CENTER.lat, CAIRO_CENTER.lng], DEFAULT_ZOOM);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);
    drawMarkers();
    locateUser(false);
}

/* ---------- wiring ---------- */

function wireControls() {
    $('station-search').addEventListener('input', (e) => { state.query = e.target.value; renderList(); });

    document.querySelectorAll('[data-radius]').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('[data-radius]').forEach((b) => b.classList.remove('is-active'));
            btn.classList.add('is-active');
            state.radiusKm = btn.dataset.radius === 'all' ? Infinity : Number(btn.dataset.radius);
            renderList();
        });
    });

    $('units-only').addEventListener('change', (e) => { state.unitsOnly = e.target.checked; renderList(); });

    document.querySelectorAll('[data-mode]').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('[data-mode]').forEach((b) => b.classList.remove('is-dark'));
            btn.classList.add('is-dark');
            state.mode = btn.dataset.mode;
            renderList();
        });
    });

    $('near-me').addEventListener('click', () => locateUser(true));
    $('recenter').addEventListener('click', () => map && map.panTo(state.userPos ? [state.userPos.lat, state.userPos.lng] : [CAIRO_CENTER.lat, CAIRO_CENTER.lng]));
    $('zoom-in').addEventListener('click', () => map && map.setZoom(map.getZoom() + 1));
    $('zoom-out').addEventListener('click', () => map && map.setZoom(map.getZoom() - 1));

    // One handler for every "View on map" and "Reserve" button, in the list and in the info window.
    document.addEventListener('click', (event) => {
        const btn = event.target.closest('[data-action]');
        if (!btn || btn.disabled) return;
        const id = Number(btn.dataset.id);
        if (btn.dataset.action === 'view') selectStation(id, true);
        if (btn.dataset.action === 'reserve') PB.reserveStation(id, btn.dataset.name);
    });
}

async function loadData() {
    try {
        STATIONS = await PB.loadStations();
    } catch (err) {
        STATIONS = (window.STATIONS_DATA || []).map((s) => ({
            ...s, price: 8
        }));
        showBanner(`Backend not reachable (${err.message}). Showing sample data.`);
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    updateMapAccount();
    await loadData();
    updatePills();
    renderList();
    wireControls();

    initMap();
});

async function updateMapAccount() {
    const link = $('map-account-link');
    if (!link) return;
    if (!PB.api.hasToken()) {
        link.href = 'login.html';
        return;
    }
    link.href = 'profile.html';
    $('map-account-name').textContent = 'My account';
    $('map-account-meta').textContent = 'Loading profile…';
    try {
        const customer = await PB.api.customer.me();
        $('map-account-name').textContent = customer.fullName || 'My account';
        $('map-account-meta').textContent = customer.email || 'Profile';
    } catch (error) {
        if (error.status === 401) {
            link.href = 'login.html';
            $('map-account-name').textContent = 'Guest';
            $('map-account-meta').textContent = 'Sign in';
        } else {
            $('map-account-meta').textContent = 'Open profile';
        }
    }
}
