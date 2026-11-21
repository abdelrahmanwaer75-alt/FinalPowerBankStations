'use strict';

/* Pages: sign in and register, station list, rental history, and staff station management.
   All data comes from the backend through PB.api. */

const esc = (v) => PB.esc(v);
const money = (v) => PB.money(v);
const when = (v) => PB.dateTime(v);

document.addEventListener('DOMContentLoaded', () => {
    PB.updateSignInLink();
    if (document.getElementById('auth-form')) initAuth();
    if (document.getElementById('rentals-content')) initRentals();
    if (document.getElementById('admin-table-body')) initAdmin();
});

/* ---------- sign in / register (login.html, register.html) ---------- */

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const PHONE_RE = /^(?=(?:\D*\d){7,20}$)\+?[0-9 ()-]{7,20}$/;

function passwordRules(password) {
    return {
        length: password.length >= 8 && password.length <= 72,
        lowercase: /[a-z]/.test(password),
        uppercase: /[A-Z]/.test(password),
        number: /\d/.test(password),
        special: /[^A-Za-z0-9\s]/.test(password)
    };
}
function initAuth() {
    const form = document.getElementById('auth-form');
    const message = document.getElementById('auth-message');
    const submit = document.getElementById('auth-submit');
    const isRegister = document.body.dataset.mode === 'register';
    const passwordInput = document.getElementById('password');
    const strengthBox = document.getElementById('password-strength');
    const roleOptions = [...document.querySelectorAll('input[name="login-role"]')];
    let loginRole = new URLSearchParams(location.search).get('mode') === 'admin' ? 'admin' : 'customer';

    function setLoginRole(role, updateUrl = true) {
        loginRole = role === 'admin' ? 'admin' : 'customer';
        roleOptions.forEach((option) => { option.checked = option.value === loginRole; });
        const heading = document.getElementById('auth-heading');
        const subtitle = document.getElementById('auth-subtitle');
        const switchText = document.getElementById('auth-switch');
        const forgot = document.getElementById('forgot-link');
        if (heading) heading.textContent = loginRole === 'admin' ? 'Admin sign in' : 'Welcome back';
        if (subtitle) subtitle.textContent = loginRole === 'admin'
            ? 'Sign in with an account that has an assigned staff role to open the admin console.'
            : 'Log in to manage your rentals and access stations across Egypt.';
        if (switchText) switchText.hidden = loginRole === 'admin';
        if (forgot) forgot.href = loginRole === 'admin' ? 'forgot-password.html?return=admin' : 'forgot-password.html';
        if (submit) submit.textContent = loginRole === 'admin' ? 'Sign in as admin →' : 'Log in →';
        if (updateUrl) {
            const url = new URL(location.href);
            if (loginRole === 'admin') url.searchParams.set('mode', 'admin');
            else url.searchParams.delete('mode');
            history.replaceState(null, '', url);
        }
    }
    roleOptions.forEach((option) => option.addEventListener('change', () => setLoginRole(option.value)));
    if (!isRegister) setLoginRole(loginRole, false);

    function updatePasswordStrength() {
        if (!strengthBox) return;
        const rules = passwordRules(passwordInput.value);
        const matchedRules = Object.values(rules).filter(Boolean).length;
        const score = matchedRules === 0 ? 0 : matchedRules <= 2 ? 1 : matchedRules < 5 ? 2 : 3;
        strengthBox.dataset.score = String(score);
        strengthBox.querySelectorAll('[data-rule]').forEach((item) => item.classList.toggle('met', rules[item.dataset.rule]));
        document.getElementById('strength-label').textContent = !passwordInput.value ? 'Enter a password' : score === 3 ? 'Strong' : score === 2 ? 'Fair' : 'Weak';
    }
    passwordInput.addEventListener('input', updatePasswordStrength);

    // Show or hide the password
    const toggle = document.getElementById('toggle-password');
    toggle.addEventListener('click', () => {
        const input = document.getElementById('password');
        const show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        toggle.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    });

    const setFieldError = (inputId, errorId, invalid) => {
        document.getElementById(inputId).classList.toggle('is-invalid', invalid);
        document.getElementById(errorId).classList.toggle('show', invalid);
        return !invalid;
    };

    // Client-side checks mirror the backend rules. The server still validates everything.
    function validate() {
        const email = document.getElementById('email').value.trim();
        const password = passwordInput.value;
        let ok = true;
        const validIdentifier = isRegister ? EMAIL_RE.test(email) : (EMAIL_RE.test(email) || PHONE_RE.test(email));
        ok = setFieldError('email', 'email-error', !validIdentifier) && ok;
        const passwordValid = isRegister ? Object.values(passwordRules(password)).every(Boolean) : password.length > 0;
        ok = setFieldError('password', 'password-error', !passwordValid) && ok;
        if (isRegister) {
            const name = document.getElementById('full-name').value.trim();
            const phone = document.getElementById('phone').value.trim();
            const confirm = document.getElementById('confirm-password').value;
            const terms = document.getElementById('terms').checked;
            ok = setFieldError('full-name', 'name-error', !name || name.length > 120) && ok;
            ok = setFieldError('phone', 'phone-error', !PHONE_RE.test(phone)) && ok;
            ok = setFieldError('confirm-password', 'confirm-error', confirm !== password) && ok;
            document.getElementById('terms-error').classList.toggle('show', !terms);
            ok = terms && ok;
        }
        return ok;
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        message.textContent = '';
        message.className = 'form-message';
        if (!validate()) return;

        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const remember = !!document.getElementById('remember')?.checked;

        submit.disabled = true;
        submit.textContent = 'Please wait…';

        try {
            const res = isRegister
                ? await PB.api.auth.register({
                    fullName: document.getElementById('full-name').value.trim(),
                    email,
                    phone: document.getElementById('phone').value.trim(),
                    password
                })
                : await PB.api.auth.login({ email, password });

            if (isRegister) {
                location.href = `verify.html?identifier=${encodeURIComponent(email)}`;
                return;
            }
            PB.api.setToken(res.token, remember);
            if (loginRole === 'admin') {
                try {
                    await PB.api.admin.stations();
                } catch (error) {
                    PB.api.setToken(null);
                    if (error.status === 403 || error.code === 'NOT_AUTHORIZED') {
                        error.message = 'تم تسجيل الدخول، لكن هذا الحساب ليس لديه صلاحية أدمن. استخدم حساب أدمن مُعيّنًا أو راجع المسؤول.';
                    }
                    throw error;
                }
                const next = new URLSearchParams(location.search).get('next');
                let destination = new URL('../admin/admin-dashboard.html', location.href).href;
                if (next && !next.startsWith('//')) {
                    try {
                        const requested = new URL(next, location.href);
                        if (requested.origin === location.origin && requested.pathname.toLowerCase().includes('/pages/admin/')) {
                            destination = requested.href;
                        }
                    } catch (_) { /* Use the admin dashboard as the safe default. */ }
                }
                location.href = destination;
                return;
            }
            const next = new URLSearchParams(location.search).get('next');
            let destination = 'map.html';
            if (next && !next.startsWith('//')) {
                try {
                    const requested = new URL(next, location.href);
                    if (requested.origin === location.origin && !requested.pathname.toLowerCase().includes('/pages/admin/')) destination = requested.href;
                } catch (_) { /* Keep the customer map as the safe default. */ }
            }
            location.href = destination;
        } catch (err) {
            message.textContent = err.code === 'BAD_CREDENTIALS'
                ? 'Incorrect email or phone number, or password.'
                : err.code === 'EMAIL_TAKEN'
                    ? 'This email is already registered. Try logging in.'
                    : err.message;
            message.classList.add('error');
            submit.disabled = false;
            submit.textContent = isRegister ? 'Create account →' : loginRole === 'admin' ? 'Sign in as admin →' : 'Log in →';
        }
    });
}

/* ---------- My Rental and history (rentals.html) ---------- */

let rentalTimer = null;

function initRentals() {
    if (!PB.requireLogin({
        inPlace: true,
        title: 'Sign in to see your rental history',
        message: 'Sign in to view your active rentals, past returns, and account totals.'
    })) return;
    loadRentals();
    rentalTimer = setInterval(loadRentals, 30000); // refresh live costs every 30 s

}

async function loadRentals() {
    const content = document.getElementById('rentals-content');
    const reservationBox = document.getElementById('reservation-card');

    try {
        const [holds, active, history] = await Promise.all([
            PB.api.reservations.active(),
            PB.api.rentals.active(),
            PB.api.rentals.history()
        ]);

        setText('rental-account-status', 'Signed in');
        setText('rental-active-count', active.length);
        setText('rental-completed-count', history.filter((r) => r.status === 'COMPLETED').length);
        setText('rental-total-spending', money(history
            .filter((r) => r.status === 'COMPLETED')
            .reduce((sum, r) => sum + Number(r.cost || 0), 0)));

        renderHold(holds[0], reservationBox);
        renderTable(history, content);
    } catch (err) {
        content.innerHTML = `<div class="empty-state">Unable to load rentals.<br>${esc(err.message)}</div>`;
    }
}

function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value ?? '—';
}

function renderHold(hold, box) {
    if (!box) return;
    if (!hold) { box.innerHTML = ''; return; }

    box.innerHTML = `
        <section class="card history-hold-card">
            <span class="pill-blue">Reserved</span>
            <h2>${esc(hold.stationName)}</h2>
            <p class="muted">
                PowerBank ${esc(hold.powerBankSerial)} is held until ${when(hold.expiresAt)}.
            </p>
            <div class="history-hold-actions">
                <button class="btn-primary" type="button" id="start-hold">Start rental</button>
                <button class="btn-secondary" type="button" id="cancel-hold">Cancel reservation</button>
            </div>
        </section>`;

    document.getElementById('start-hold').addEventListener('click', async () => {
        if (!confirm('Start the rental now?')) return;
        try {
            await PB.api.rentals.start(hold.id);
            PB.toast('Rental started.');
            loadRentals();
        } catch (err) { PB.toast(err.message, true); }
    });

    document.getElementById('cancel-hold').addEventListener('click', async () => {
        if (!confirm('Cancel this reservation?')) return;
        try {
            await PB.api.reservations.cancel(hold.id);
            PB.toast('Reservation cancelled.');
            loadRentals();
        } catch (err) { PB.toast(err.message, true); }
    });
}

function renderTable(rentals, content) {
    if (!rentals.length) {
        content.innerHTML = '<div class="empty-state">You have no rentals yet. Find a station to reserve your first PowerBank.</div>';
        return;
    }

    content.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Rental</th><th>Station</th><th>Started</th><th>Returned</th>
                    <th>Duration</th><th>Cost</th><th>Status / Action</th>
                </tr>
            </thead>
            <tbody>
                ${rentals.map((r) => {
                    const active = r.status === 'ACTIVE';
                    return `
                        <tr>
                            <td>${esc(r.reference)}</td>
                            <td>${esc(r.pickupStationName)}</td>
                            <td>${when(r.startTime)}</td>
                            <td>${r.endTime ? when(r.endTime) : '—'}</td>
                            <td>${r.elapsedMinutes} min</td>
                            <td>${money(r.cost)}${active ? ' <span class="muted">(est.)</span>' : ''}</td>
                            <td>${active ? `
                                <span class="history-badge history-badge--active">Active</span>
                                <div class="history-row-action"><a class="btn-secondary" href="return.html?rental=${encodeURIComponent(r.id)}">Return power bank</a></div>` : '<span class="history-badge history-badge--complete">Completed</span>'}
                            </td>
                        </tr>`;
                }).join('')}
            </tbody>
        </table>`;

}

/* ---------- admin (admin.html) ---------- */

let adminStations = [];

function initAdmin() {
    if (window.PBAdmin ? !PBAdmin.requireStaffPage() : !PB.requireLogin()) return;

    document.getElementById('add-station-button')?.addEventListener('click', () => openStationDialog(null));
    document.getElementById('close-dialog')?.addEventListener('click', () => document.getElementById('station-dialog').close());
    document.getElementById('cancel-dialog')?.addEventListener('click', () => document.getElementById('station-dialog').close());
    document.getElementById('station-form')?.addEventListener('submit', saveStation);
    document.getElementById('admin-search')?.addEventListener('input', renderAdminStations);

    loadAdmin();
}

async function loadAdmin() {
    try {
        const [stations, returns] = await Promise.all([
            PB.api.admin.stations(),
            PB.api.admin.returns('PENDING_REVIEW')
        ]);
        adminStations = stations;
        setText('metric-stations', stations.length);
        setText('metric-available', stations.reduce((n, s) => n + s.availableCount, 0));
        setText('metric-low', stations.filter((s) => s.status === 'ACTIVE' && s.availableCount < 3).length);
        setText('metric-pending', returns.length);
        renderAdminStations();
    } catch (err) {
        const denied = err.status === 403;
        const msg = denied
            ? 'Staff access is required. An administrator must give this account a staff role.'
            : err.message;
        document.getElementById('admin-table-body').innerHTML = `<tr><td colspan="6">${esc(msg)}</td></tr>`;
        setText('metric-pending', '—');
    }
}

function renderAdminStations() {
    const body = document.getElementById('admin-table-body');
    const q = (document.getElementById('admin-search')?.value || '').trim().toLowerCase();
    const list = adminStations.filter((s) => !q || `${s.name} ${s.address}`.toLowerCase().includes(q));

    body.innerHTML = list.map((s) => `
        <tr>
            <td><strong>${esc(s.name)}</strong></td>
            <td>${esc(s.address)}</td>
            <td>${s.availableCount} / ${s.totalCapacity}</td>
            <td>— <span class="muted" style="font-size:11px">(pricing plan)</span></td>
            <td><span class="status-label ${s.status === 'ACTIVE' ? '' : 'off'}">
                <span class="status-dot"></span>${s.status === 'ACTIVE' ? 'Open' : 'Closed'}</span></td>
            <td><div class="table-actions">
                <button class="text-button" type="button" data-edit="${esc(s.id)}">Edit</button>
                <button class="text-button" type="button" data-bank="${esc(s.id)}">+ Power bank</button>
            </div></td>
        </tr>`).join('') || '<tr><td colspan="6">No stations yet.</td></tr>';

    body.querySelectorAll('[data-edit]').forEach((b) => b.addEventListener('click', () => {
        openStationDialog(adminStations.find((s) => s.id === Number(b.dataset.edit)));
    }));
    body.querySelectorAll('[data-bank]').forEach((b) => b.addEventListener('click', () => addBanks(Number(b.dataset.bank))));
}

function openStationDialog(station) {
    const dialog = document.getElementById('station-dialog');
    document.getElementById('dialog-title').textContent = station ? 'Edit station' : 'Add station';
    document.getElementById('edit-station-id').value = station ? station.id : '';
    document.getElementById('new-station-name').value = station ? station.name : '';
    document.getElementById('new-station-area').value = station ? station.address : '';
    document.getElementById('new-station-city').value = station ? station.city : '';
    document.getElementById('new-station-lat').value = station ? station.latitude : '';
    document.getElementById('new-station-lng').value = station ? station.longitude : '';
    document.getElementById('new-station-capacity').value = station ? station.totalCapacity : 4;
    document.getElementById('new-station-hours').value = station && station.openingHours ? station.openingHours : '';
    document.getElementById('new-station-status').value = station ? station.status : 'ACTIVE';
    dialog.showModal();
}

async function saveStation(event) {
    event.preventDefault();
    const id = document.getElementById('edit-station-id').value;
    const body = {
        name: document.getElementById('new-station-name').value.trim(),
        address: document.getElementById('new-station-area').value.trim(),
        city: document.getElementById('new-station-city').value.trim(),
        latitude: Number(document.getElementById('new-station-lat').value),
        longitude: Number(document.getElementById('new-station-lng').value),
        totalCapacity: Number(document.getElementById('new-station-capacity').value),
        openingHours: document.getElementById('new-station-hours').value.trim() || null,
        contactPhone: null,
        description: null,
        status: document.getElementById('new-station-status').value
    };
    try {
        if (id) await PB.api.admin.updateStation(id, body);
        else await PB.api.admin.createStation(body);
        document.getElementById('station-dialog').close();
        PB.toast('Station saved.');
        loadAdmin();
    } catch (err) {
        PB.toast(err.message, true);
    }
}

async function addBanks(stationId) {
    const count = Number(prompt('How many power banks to add?', '4'));
    if (!Number.isInteger(count) || count < 1 || count > 50) {
        PB.toast('Enter a whole number from 1 to 50.', true);
        return;
    }
    try {
        await PB.api.admin.addPowerBanks(stationId, count);
        PB.toast(`${count} power bank(s) added.`);
        loadAdmin();
    } catch (err) {
        PB.toast(err.message, true);
    }
}

