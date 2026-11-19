'use strict';

/* Helpers shared by every page: sign-in state, station data, reserving, and small UI utilities. */

window.PB = window.PB || {};

PB.esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[c]));

PB.money = (value) => {
    const n = Number(value);
    return Number.isFinite(n) ? `EGP ${n.toFixed(2)}` : '—';
};

PB.dateTime = (value) => (value ? new Date(value).toLocaleString() : '—');

PB.toast = function (message, isError) {
    let el = document.getElementById('toast');
    if (!el) {
        el = document.createElement('div');
        el.id = 'toast';
        el.className = 'toast';
        document.body.appendChild(el);
    }
    el.textContent = message;
    el.classList.toggle('error', !!isError);
    el.classList.remove('hidden');
    clearTimeout(PB.toast.timer);
    PB.toast.timer = setTimeout(() => el.classList.add('hidden'), 4000);
};

PB.requireLogin = function () {
    if (PB.api.hasToken()) return true;
    const next = location.pathname.split('/').pop() + location.search;
    location.href = 'login.html?next=' + encodeURIComponent(next);
    return false;
};

/* Header "Sign in" button becomes "Log out" when a token exists. */
PB.updateSignInLink = function () {
    document.querySelectorAll('.nav-button, .app-nav .sign-link').forEach((link) => {
        if (PB.api.hasToken()) {
            link.textContent = 'Log out';
            link.href = 'index.html';
            link.onclick = async (event) => {
                event.preventDefault();
                try { await PB.api.auth.logout(); } catch (_) { /* logging out locally anyway */ }
                PB.api.setToken(null);
                location.href = 'index.html';
            };
        } else {
            link.textContent = 'Sign in';
            link.href = 'login.html';
            link.onclick = null;
        }
    });
};

/* Converts the API station shape to the one the map and lists use. */
PB.loadStations = async function () {
    const list = await PB.api.stations.list();
    const [pricing, availability] = await Promise.all([
        PB.api.pricing.active().catch(() => null),
        Promise.all(list.map((s) => PB.api.stations.availability(s.id).catch(() => null)))
    ]);
    const price = pricing ? Number(pricing.pricePerHour) : null;

    return list.map((s, i) => {
        const a = availability[i];
        return {
            id: s.id,
            name: s.name,
            address: s.address,
            lat: Number(s.latitude),
            lng: Number(s.longitude),
            hours: s.openingHours || '—',
            total: s.totalCapacity,
            available: s.availableCount,
            returnSlots: a ? a.returnSlotsLeft : 0,
            price
        };
    });
};

/* Reserves one PowerBank at a station, then opens My Rental. */
PB.reserveStation = async function (stationId, stationName, durationMinutes = 15) {
    if (!PB.requireLogin()) return;
    if (!confirm(`Reserve a PowerBank at ${stationName} for ${durationMinutes} minutes?`)) return;
    try {
        await PB.api.reservations.create(stationId, durationMinutes);
        location.href = 'reserve.html';
    } catch (err) {
        PB.toast(err.message, true);
    }
};
