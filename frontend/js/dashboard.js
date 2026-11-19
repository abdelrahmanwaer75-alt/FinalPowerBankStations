'use strict';

/* Dashboard: loads holds, active rentals, and history in parallel from the backend.
   The page shows real data only. Nothing here is invented. */

let activeRental = null;
let elapsedTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    if (!PB.requireLogin()) return;

    try {
        const [holds, active, history, stations] = await Promise.all([
            PB.api.reservations.active(),
            PB.api.rentals.active(),
            PB.api.rentals.history(),
            PB.loadStations().catch(() => [])
        ]);

        const completed = history.filter((r) => r.status === 'COMPLETED');
        const spent = completed.reduce((sum, r) => sum + Number(r.cost || 0), 0);

        setText('s-active', active.length);
        setText('s-holds', holds.length);
        setText('s-done', completed.length);
        setText('s-spent', PB.money(spent));
        setText('summary', active.length
            ? 'You have a PowerBank out right now.'
            : holds.length ? 'You have a PowerBank on hold.' : 'Ready when you are.');

        if (active.length) {
            activeRental = active[0];
            show('active-card');
            setText('active-ref', activeRental.reference);
            setText('active-unit', activeRental.powerBankSerial);
            setText('active-station', `Picked up at ${activeRental.pickupStationName}`);
            updateActive();
            elapsedTimer = setInterval(updateActive, 1000);
        } else if (holds.length) {
            const h = holds[0];
            show('hold-card');
            setText('hold-unit', h.powerBankSerial);
            setText('hold-station', h.stationName);
            setText('hold-until', `Until ${PB.dateTime(h.expiresAt)}`);
        } else {
            show('none-card');
        }

        renderRecent(history.slice(0, 3));
        renderReturnPoint(stations);
    } catch (err) {
        showBanner(err.message);
    }
});

function updateActive() {
    if (!activeRental) return;
    const s = Math.max(0, Math.floor((Date.now() - new Date(activeRental.startTime)) / 1000));
    const h = String(Math.floor(s / 3600)).padStart(2, '0');
    const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0');
    const sec = String(s % 60).padStart(2, '0');
    setText('active-elapsed', `${h}:${m}:${sec}`);
    setText('active-cost', PB.money(activeRental.cost));
}

function renderRecent(list) {
    const ul = document.getElementById('recent');
    if (!list.length) {
        ul.innerHTML = '<li class="empty-line">No rentals yet.</li>';
        return;
    }
    ul.innerHTML = list.map((r) => `
        <li>
            <div>
                <strong>${PB.esc(r.pickupStationName)}</strong>
                <small>${PB.dateTime(r.startTime)} · ${r.elapsedMinutes} min</small>
            </div>
            <div style="text-align:right">
                <span class="${r.status === 'COMPLETED' ? 'badge-done' : 'badge-live'}">${r.status === 'COMPLETED' ? 'Completed' : 'Active'}</span>
                <small>${PB.money(r.cost)}</small>
            </div>
        </li>`).join('');
}

function renderReturnPoint(stations) {
    // A return needs a free slot, so rank by return slots (from the availability endpoint).
    const best = [...stations].filter((s) => s.returnSlots > 0)
        .sort((a, b) => b.returnSlots - a.returnSlots)[0];
    setText('return-point', best ? `${best.name} · ${best.address} (${best.returnSlots} free slots)` : 'No station has a free return slot right now.');
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
