'use strict';

/* Helpers shared by every page: sign-in state, station data, reserving, and small UI utilities. */

window.PB = window.PB || {};

// Apply the locally cached account theme before page scripts initialize.
try {
    const savedTheme = localStorage.getItem('pb_theme');
    if (savedTheme === 'dark' || savedTheme === 'light') document.documentElement.dataset.theme = savedTheme;
} catch (_) { /* Storage may be unavailable in private browsing. */ }

PB.esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[c]));

PB.money = (value) => {
    const n = Number(value);
    return Number.isFinite(n) ? `EGP ${n.toFixed(2)}` : '—';
};

PB.rateMoney = (value) => {
    const n = Number(value);
    return Number.isFinite(n) ? `EGP ${n.toFixed(6).replace(/0+$/, '').replace(/\.$/, '')}` : '—';
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

PB.requireLogin = function (options = {}) {
    if (PB.api.hasToken()) return true;
    const next = location.pathname.split('/').pop() + location.search;
    if (options.inPlace) {
        const main = document.querySelector('main');
        if (main) {
            const content = document.createElement('div');
            content.className = 'protected-page-content';
            while (main.firstChild) content.appendChild(main.firstChild);
            const gate = document.createElement('section');
            gate.className = 'login-required-card';
            gate.setAttribute('role', 'status');
            gate.innerHTML = `<span class="login-required-icon" aria-hidden="true">🔒</span><h1>${PB.esc(options.title || 'Sign in to continue')}</h1><p>${PB.esc(options.message || 'Sign in to access your account information.')}</p><a class="login-required-button" href="login.html?next=${encodeURIComponent(next)}">Sign in</a>`;
            content.hidden = true;
            main.append(gate, content);
        }
        return false;
    }
    location.href = 'login.html?next=' + encodeURIComponent(next);
    return false;
};

/* Header "Sign in" button becomes "Log out" when a token exists. */
PB.updateSignInLink = function () {
    document.querySelectorAll('.nav-button, .app-nav .sign-link, .app-links .sign-link').forEach((link) => {
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
    if (!Array.isArray(list)) throw new Error('The stations API did not return a station list.');
    if (list.some((station) => !station || station.id == null))
        throw new Error('The stations API returned a station without an ID.');
    const [pricing, availability] = await Promise.all([
        PB.api.pricing.active().catch(() => null),
        Promise.all(list.map((s) => PB.api.stations.availability(s.id).catch(() => null)))
    ]);
    const price = pricing ? Number(pricing.pricePerMinute) : null;

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
            returnSlots: a ? Number(a.returnSlotsLeft) : null,
            price
        };
    });
};

PB.joinWaitlist = async function (stationId, stationName = 'this station') {
    const pending = { stationId: Number(stationId), stationName: String(stationName || 'this station'), createdAt: Date.now() };
    try { sessionStorage.removeItem('pb_pending_reservation'); } catch (_) { /* Ignore storage failures. */ }
    if (!PB.api.hasToken()) {
        try { sessionStorage.setItem('pb_pending_waitlist', JSON.stringify(pending)); } catch (_) { /* The user can still sign in and join manually. */ }
        PB.requireLogin();
        return null;
    }
    try {
        const entry = await PB.api.waitlist.join(pending.stationId);
        try { sessionStorage.removeItem('pb_pending_waitlist'); } catch (_) { /* Ignore storage failures. */ }
        const message = entry.status === 'NOTIFIED'
            ? `A PowerBank became available at ${entry.stationName}. Check availability and reserve it.`
            : `You joined the waitlist for ${entry.stationName}. Your position is ${entry.queuePosition ?? 'next'}.`;
        PB.toast(message);
        window.dispatchEvent(new CustomEvent('pb:waitlist-updated', { detail: entry }));
        return entry;
    } catch (error) {
        if (error.status === 401) {
            try { sessionStorage.setItem('pb_pending_waitlist', JSON.stringify(pending)); } catch (_) { /* Keep the user on this page if storage is blocked. */ }
            location.href = 'login.html?next=' + encodeURIComponent(location.pathname.split('/').pop() + location.search);
            return null;
        }
        try { sessionStorage.removeItem('pb_pending_waitlist'); } catch (_) { /* Ignore storage failures. */ }
        if (error.code === 'POWERBANKS_AVAILABLE_NOW') {
            window.dispatchEvent(new CustomEvent('pb:station-availability-changed', { detail: { stationId: pending.stationId } }));
            PB.toast('A PowerBank is available now. Refreshing this station so you can reserve it.', true);
        } else PB.toast(error.message, true);
        return null;
    }
};

/* Reserves a PowerBank at a station, then opens My Rental. */
PB.reserveStation = async function (stationId, stationName, durationMinutes = 15) {
    const pendingReservation = { stationId: Number(stationId), stationName: String(stationName || 'station'), durationMinutes: Number(durationMinutes) || 15, createdAt: Date.now() };
    try { sessionStorage.removeItem('pb_pending_waitlist'); } catch (_) { /* Ignore storage failures. */ }
    const savePendingReservation = () => {
        try { sessionStorage.setItem('pb_pending_reservation', JSON.stringify(pendingReservation)); } catch (_) { /* Sign-in still works if storage is unavailable. */ }
    };
    const clearPendingReservation = () => {
        try { sessionStorage.removeItem('pb_pending_reservation'); } catch (_) { /* Ignore storage failures. */ }
    };
    if (!PB.api.hasToken()) {
        savePendingReservation();
        PB.requireLogin();
        return;
    }
    const reserveButtons = [...document.querySelectorAll('[data-action="reserve"], #reserve-btn')].filter((button) => !button.disabled);
    reserveButtons.forEach((button) => { button.disabled = true; });
    PB.toast(`Reserving a PowerBank at ${stationName}…`);
    try {
        await PB.api.reservations.create(stationId, durationMinutes);
        clearPendingReservation();
        location.href = 'active-rental.html';
    } catch (err) {
        if (err.status === 401) {
            savePendingReservation();
            location.href = 'login.html?next=' + encodeURIComponent(location.pathname.split('/').pop() + location.search);
            return;
        }
        clearPendingReservation();
        if (err.code === 'NO_POWER_BANK_AVAILABLE' || err.code === 'POWERBANK_JUST_TAKEN') {
            PB.toast(`No PowerBanks are available at ${stationName}. Joining the waiting list…`);
            await PB.joinWaitlist(stationId, stationName);
            reserveButtons.forEach((button) => { button.disabled = false; });
            return;
        }
        PB.toast(err.message, true);
        reserveButtons.forEach((button) => { button.disabled = false; });
    }
};

// Keep customer page branding and navigation consistent even on workflow/auth pages.
document.addEventListener('DOMContentLoaded', () => {
    // Admin pages own their navigation and use different relative destinations.
    // Replacing their header with customer links produces /pages/admin/index.html and /map.html 404s.
    if (location.pathname.toLowerCase().includes('/pages/admin/')) return;

    document.querySelectorAll('.auth-brand, .home-brand, .brand').forEach((brand) => {
        brand.href = 'index.html';
        brand.setAttribute('aria-label', 'PowerBank Stations home');
    });
    const isCustomerPage = location.pathname.toLowerCase().includes('/pages/customer/');
    if (isCustomerPage && PB.api.hasToken()) {
        try {
            const pending = JSON.parse(sessionStorage.getItem('pb_pending_reservation') || 'null');
            if (pending && Number.isFinite(Number(pending.stationId)) && Date.now() - Number(pending.createdAt) < 5 * 60_000) {
                sessionStorage.removeItem('pb_pending_reservation');
                setTimeout(() => PB.reserveStation(pending.stationId, pending.stationName, pending.durationMinutes), 0);
            } else if (pending) sessionStorage.removeItem('pb_pending_reservation');
        } catch (_) { /* Invalid or unavailable session data should not block page startup. */ }
        try {
            const pendingWaitlist = JSON.parse(sessionStorage.getItem('pb_pending_waitlist') || 'null');
            if (pendingWaitlist && Number.isFinite(Number(pendingWaitlist.stationId)) && Date.now() - Number(pendingWaitlist.createdAt) < 10 * 60_000) {
                sessionStorage.removeItem('pb_pending_waitlist');
                setTimeout(() => PB.joinWaitlist(pendingWaitlist.stationId, pendingWaitlist.stationName), 0);
            } else if (pendingWaitlist) sessionStorage.removeItem('pb_pending_waitlist');
        } catch (_) { /* Invalid or unavailable session data should not block page startup. */ }
    }
    const header = document.querySelector('body > header');
    if (!header) return;
    header.querySelectorAll(':scope > nav, .app-header__left > nav').forEach((nav) => nav.remove());
    header.querySelectorAll('.home-staff-link, .home-signin').forEach((link) => link.remove());
    const nav = document.createElement('nav');
    nav.className = 'site-nav';
    nav.setAttribute('aria-label', 'Main navigation');
    const current = location.pathname.split('/').pop();
    const links = [
        ['index.html', 'Home'], ['map.html', 'Stations'],
        ['active-rental.html', 'Active rental'], ['rentals.html', 'History'],
        ['profile.html', 'Profile'], ['login.html?mode=admin', 'Admin']
    ];
    nav.innerHTML = links.map(([href, label]) =>
        `<a href="${href}"${href === current ? ' class="active" aria-current="page"' : ''}>${label}</a>`
    ).join('') + `<a class="site-signin nav-button" href="login.html">${PB.api.hasToken() ? 'Log out' : 'Sign in'}</a>`;
    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.className = 'site-menu-toggle';
    toggle.setAttribute('aria-label', 'Open navigation menu');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.setAttribute('aria-controls', 'site-nav');
    toggle.innerHTML = '<span></span><span></span><span></span>';
    nav.id = 'site-nav';
    const backdrop = document.createElement('button');
    backdrop.type = 'button';
    backdrop.className = 'site-nav-backdrop';
    backdrop.setAttribute('aria-label', 'Close navigation menu');
    const closeMenu = () => {
        nav.classList.remove('is-open');
        backdrop.classList.remove('is-open');
        toggle.setAttribute('aria-expanded', 'false');
        document.body.classList.remove('site-menu-open');
    };
    toggle.addEventListener('click', () => {
        const open = !nav.classList.contains('is-open');
        nav.classList.toggle('is-open', open);
        backdrop.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        document.body.classList.toggle('site-menu-open', open);
    });
    backdrop.addEventListener('click', closeMenu);
    nav.addEventListener('click', (event) => { if (event.target.closest('a')) closeMenu(); });
    document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeMenu(); });
    header.append(toggle, backdrop, nav);
    PB.updateSignInLink();
});
