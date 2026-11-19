'use strict';

/* Pages: sign in and register (login.html), station list (stations.html),
   My Rental and history (rentals.html), and the admin placeholder (admin.html).
   All data comes from the backend through PB.api. */

const esc = (v) => PB.esc(v);
const money = (v) => PB.money(v);
const when = (v) => PB.dateTime(v);

document.addEventListener('DOMContentLoaded', () => {
    PB.updateSignInLink();
    if (document.getElementById('auth-form')) initAuth();
    if (document.getElementById('station-grid')) initStations();
    if (document.getElementById('rentals-content')) initRentals();
    if (document.getElementById('admin-table-body')) initAdmin();
});

/* ---------- sign in / register (login.html, register.html) ---------- */

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

function initAuth() {
    const form = document.getElementById('auth-form');
    const message = document.getElementById('auth-message');
    const submit = document.getElementById('auth-submit');
    const isRegister = document.body.dataset.mode === 'register';

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
        const password = document.getElementById('password').value;
        let ok = true;
        ok = setFieldError('email', 'email-error', !EMAIL_RE.test(email)) && ok;
        ok = setFieldError('password', 'password-error', isRegister ? password.length < 8 : password.length === 0) && ok;
        if (isRegister) {
            const name = document.getElementById('full-name').value.trim();
            const confirm = document.getElementById('confirm-password').value;
            const terms = document.getElementById('terms').checked;
            ok = setFieldError('full-name', 'name-error', !name) && ok;
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
                    password
                })
                : await PB.api.auth.login({ email, password });

            PB.api.setToken(res.token, remember || isRegister);
            const next = new URLSearchParams(location.search).get('next');
            location.href = next && !next.startsWith('http') ? next : 'map.html';
        } catch (err) {
            message.textContent = err.code === 'BAD_CREDENTIALS'
                ? 'Incorrect email or password.'
                : err.code === 'EMAIL_TAKEN'
                    ? 'This email is already registered. Try logging in.'
                    : err.message;
            message.classList.add('error');
            submit.disabled = false;
            submit.textContent = isRegister ? 'Create account →' : 'Log in →';
        }
    });
}

/* ---------- station list (stations.html) ---------- */

let stationList = [];

async function initStations() {
    const grid = document.getElementById('station-grid');
    grid.innerHTML = '<div class="empty-state">Loading stations…</div>';

    try {
        stationList = await PB.loadStations();
    } catch (err) {
        grid.innerHTML = `<div class="empty-state">Can't load stations.<br>${esc(err.message)}</div>`;
        return;
    }

    ['station-search', 'availability-filter', 'price-filter'].forEach((id) => {
        document.getElementById(id).addEventListener(id === 'station-search' ? 'input' : 'change', renderStations);
    });
    renderStations();
}

function stationStatus(s) {
    if (s.available === 0) return 'none';
    if (s.available < 3) return 'low';
    return 'available';
}

function renderStations() {
    const grid = document.getElementById('station-grid');
    const q = document.getElementById('station-search').value.trim().toLowerCase();
    const availability = document.getElementById('availability-filter').value;
    const priceFilter = document.getElementById('price-filter').value;

    const list = stationList
        .filter((s) => !q || `${s.name} ${s.address}`.toLowerCase().includes(q))
        .filter((s) => availability === 'all' || stationStatus(s) === availability)
        .filter((s) => priceFilter === 'all'
            || (priceFilter === 'low' && (s.price ?? 0) < 10)
            || (priceFilter === 'high' && (s.price ?? 0) >= 10))
        .sort((a, b) => b.available - a.available);

    if (!list.length) {
        grid.innerHTML = '<div class="empty-state">No stations match your search or filters.</div>';
        return;
    }

    grid.innerHTML = list.map((s) => {
        const status = stationStatus(s);
        const label = status === 'available' ? 'Available' : status === 'low' ? 'Low availability' : 'Unavailable';
        const cls = status === 'available' ? 'success' : status === 'low' ? 'warning' : 'danger';
        return `
            <article class="station-card">
                <div class="station-art">
                    <span class="station-symbol" aria-hidden="true">⚡</span>
                    <span class="availability">${s.available} / ${s.total} available</span>
                </div>
                <div class="station-content">
                    <div class="station-card-top">
                        <h3>${esc(s.name)}</h3>
                        <span class="badge ${cls}">${label}</span>
                    </div>
                    <div class="station-area">⌖ ${esc(s.address)}</div>
                    <div class="station-meta">
                        <span>Rental price</span>
                        <span><strong>${s.price != null ? money(s.price) : '—'}</strong> / hr</span>
                    </div>
                    <div class="station-meta"><span>Hours</span><span>${esc(s.hours)}</span></div>
                    <div class="station-actions">
                        <button class="button primary" type="button"
                                data-reserve="${esc(s.id)}" data-name="${esc(s.name)}"
                                ${status === 'none' ? 'disabled' : ''}>
                            ${status === 'none' ? 'Unavailable' : 'Reserve'}
                        </button>
                    </div>
                </div>
            </article>`;
    }).join('');

    grid.querySelectorAll('[data-reserve]').forEach((btn) => {
        btn.addEventListener('click', () => PB.reserveStation(Number(btn.dataset.reserve), btn.dataset.name));
    });
}

/* ---------- My Rental and history (rentals.html) ---------- */

let rentalTimer = null;

function initRentals() {
    if (!PB.requireLogin()) return;
    loadRentals();
    rentalTimer = setInterval(loadRentals, 30000); // refresh live costs every 30 s

    // Came back from a return: open the photo step for that return.
    const returnId = new URLSearchParams(location.search).get('return');
    if (returnId) {
        PB.api.returns.get(Number(returnId))
            .then((ret) => renderReturnPanel(ret))
            .catch((err) => PB.toast(err.message, true));
    }
}

async function loadRentals() {
    const content = document.getElementById('rentals-content');
    const reservationBox = document.getElementById('reservation-card');

    try {
        const [holds, active, history, stations] = await Promise.all([
            PB.api.reservations.active(),
            PB.api.rentals.active(),
            PB.api.rentals.history(),
            PB.api.stations.list()
        ]);

        setText('rental-account-status', 'Signed in');
        setText('rental-active-count', active.length);
        setText('rental-completed-count', history.filter((r) => r.status === 'COMPLETED').length);
        setText('rental-total-spending', money(history
            .filter((r) => r.status === 'COMPLETED')
            .reduce((sum, r) => sum + Number(r.cost || 0), 0)));

        renderHold(holds[0], reservationBox);
        renderTable(history, stations, content);
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
        <section class="table-card" style="margin-bottom:30px;padding:24px">
            <span class="badge warning">Reserved</span>
            <h2 style="margin:12px 0 6px;font-size:24px">${esc(hold.stationName)}</h2>
            <p class="muted" style="margin:0 0 16px">
                PowerBank ${esc(hold.powerBankSerial)} is held until ${when(hold.expiresAt)}.
            </p>
            <div style="display:flex;gap:10px;flex-wrap:wrap">
                <button class="button primary" type="button" id="start-hold">Start rental</button>
                <button class="button secondary" type="button" id="cancel-hold">Cancel reservation</button>
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

function renderTable(rentals, stations, content) {
    if (!rentals.length) {
        content.innerHTML = '<div class="empty-state">You have no rentals yet. Find a station to reserve your first PowerBank.</div>';
        return;
    }

    const options = stations.map((s) => `<option value="${esc(s.id)}">${esc(s.name)}</option>`).join('');

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
                                <span class="badge warning">Active</span>
                                <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin-top:8px">
                                    <select data-return-select="${esc(r.id)}" style="width:auto">${options}</select>
                                    <button class="table-action" type="button" data-return="${esc(r.id)}">Return power bank</button>
                                </div>` : '<span class="badge success">Completed</span>'}
                            </td>
                        </tr>`;
                }).join('')}
            </tbody>
        </table>`;

    content.querySelectorAll('[data-return]').forEach((btn) => {
        btn.addEventListener('click', () => returnRental(Number(btn.dataset.return)));
    });
}

async function returnRental(rentalId) {
    const stationId = Number(document.querySelector(`[data-return-select="${rentalId}"]`).value);
    if (!confirm('Return this PowerBank to the selected station?')) return;
    try {
        const res = await PB.api.returns.create(rentalId, stationId);
        PB.toast(`Returned. Total cost: ${money(res.finalCost)}.`);
        renderReturnPanel(res);
        loadRentals();
    } catch (err) {
        PB.toast(err.message, true);
    }
}

const MAX_PHOTOS = 6;
const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

/* Photo step after a return (spec scenario 4). The photos only document the device's condition. */
function renderReturnPanel(ret) {
    const box = document.getElementById('return-card');
    if (!box) return;

    box.innerHTML = `
        <section class="table-card" style="margin-bottom:30px;padding:24px">
            <span class="badge info" id="return-status">${esc(ret.status)}</span>
            <h2 style="margin:12px 0 6px;font-size:22px">Return ${esc(ret.reference)}</h2>
            <p class="muted" style="margin:0 0 14px">
                Duration ${ret.durationMinutes} min · Total ${money(ret.finalCost)}.
                Add photos of the PowerBank's outside (JPG or PNG, up to 5 MB each, up to ${MAX_PHOTOS}).
            </p>
            <input type="file" id="photo-input" accept="image/jpeg,image/png" multiple>
            <div id="photo-list" class="muted" style="margin:10px 0"></div>
            <div style="display:flex;gap:10px;flex-wrap:wrap">
                <button class="button primary" type="button" id="photo-submit">Upload photos</button>
                <button class="button secondary" type="button" id="photo-refresh">Check review status</button>
                <a class="button secondary" href="return-confirmation.html?return=${esc(ret.id)}">View receipt</a>
            </div>
        </section>`;

    const input = document.getElementById('photo-input');
    const list = document.getElementById('photo-list');
    const uploaded = (ret.images || []).length;

    if (uploaded) list.textContent = `${uploaded} photo(s) already uploaded.`;

    input.addEventListener('change', () => {
        const files = Array.from(input.files);
        const problem = files.find((f) => !['image/jpeg', 'image/png'].includes(f.type))
            || files.find((f) => f.size > MAX_PHOTO_BYTES);
        if (files.length + uploaded > MAX_PHOTOS) {
            PB.toast(`At most ${MAX_PHOTOS} photos per return.`, true);
            input.value = '';
            return;
        }
        if (problem) {
            PB.toast(`${problem.name}: only JPG or PNG, up to 5 MB each.`, true);
            input.value = '';
            return;
        }
        list.textContent = `${files.length} photo(s) ready to upload.`;
    });

    document.getElementById('photo-submit').addEventListener('click', async () => {
        const files = input.files;
        if (!files || !files.length) {
            PB.toast('Choose at least one photo.', true);
            return;
        }
        try {
            await PB.api.returns.uploadImages(ret.id, files);
            PB.toast('Photos uploaded. The return is waiting for review.');
            input.value = '';
            list.textContent = '';
            refreshReturnStatus(ret.id);
        } catch (err) {
            PB.toast(err.message, true);
        }
    });

    document.getElementById('photo-refresh').addEventListener('click', () => refreshReturnStatus(ret.id));
}

async function refreshReturnStatus(returnId) {
    try {
        const { status } = await PB.api.returns.status(returnId);
        const el = document.getElementById('return-status');
        if (el) el.textContent = status.replace(/_/g, ' ').toLowerCase();
    } catch (err) {
        PB.toast(err.message, true);
    }
}

/* ---------- admin (admin.html) ---------- */

let adminStations = [];

function initAdmin() {
    if (!PB.requireLogin()) return;

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
        setText('metric-rentals', '—');
        renderAdminStations();
        renderReturnsQueue(returns);
    } catch (err) {
        const denied = err.status === 403;
        const msg = denied
            ? 'Staff access is required. An administrator must give this account a staff role.'
            : err.message;
        document.getElementById('admin-table-body').innerHTML = `<tr><td colspan="6">${esc(msg)}</td></tr>`;
        document.getElementById('returns-table-body').innerHTML = `<tr><td colspan="6">${esc(msg)}</td></tr>`;
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

function renderReturnsQueue(list) {
    const body = document.getElementById('returns-table-body');
    if (!list.length) {
        body.innerHTML = '<tr><td colspan="6">No returns waiting for review.</td></tr>';
        return;
    }
    body.innerHTML = list.map((r) => `
        <tr>
            <td>${esc(r.reference)}<br><span class="muted" style="font-size:11px">${r.durationMinutes ?? '—'} min · ${money(r.finalCost)}</span></td>
            <td>${esc(r.powerBankSerial)}</td>
            <td>${esc(r.returnStation)}</td>
            <td>${r.photoCount}</td>
            <td><span class="badge warning">${esc(r.status.replace(/_/g, ' ').toLowerCase())}</span></td>
            <td><div class="table-actions">
                <button class="text-button" type="button" data-approve="${esc(r.id)}">Approve</button>
                <button class="text-button delete" type="button" data-reject="${esc(r.id)}">Reject</button>
                <button class="text-button" type="button" data-manual="${esc(r.id)}">Manual review</button>
            </div></td>
        </tr>`).join('');

    body.querySelectorAll('[data-approve]').forEach((b) => b.addEventListener('click', async () => {
        if (!confirm('Approve this return? The PowerBank becomes available again.')) return;
        await decide(() => PB.api.admin.approve(b.dataset.approve), 'Return approved.');
    }));
    body.querySelectorAll('[data-reject]').forEach((b) => b.addEventListener('click', async () => {
        const reason = prompt('Reason for rejecting this return (required):');
        if (!reason || !reason.trim()) return;
        await decide(() => PB.api.admin.reject(b.dataset.reject, reason.trim()), 'Return rejected. The PowerBank is out of service.');
    }));
    body.querySelectorAll('[data-manual]').forEach((b) => b.addEventListener('click', async () => {
        await decide(() => PB.api.admin.manualReview(b.dataset.manual), 'Sent for manual review.');
    }));
}

async function decide(action, doneMessage) {
    try {
        await action();
        PB.toast(doneMessage);
        loadAdmin();
    } catch (err) {
        PB.toast(err.message, true);
    }
}
