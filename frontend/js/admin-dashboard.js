'use strict';
document.addEventListener('DOMContentLoaded', async () => {
    if (!PBAdmin.requireStaffPage()) return;
    try {
        const [stations, returns] = await Promise.all([
            PB.api.admin.stations(),
            PB.api.admin.returns('PENDING_REVIEW')
        ]);
        const availability = await Promise.all(stations.map((station) =>
            PB.api.stations.availability(station.id).catch(() => null)
        ));
        const active = stations.filter((station) => station.status === 'ACTIVE');
        const available = stations.reduce((total, station) => total + Number(station.availableCount || 0), 0);
        const freeSlots = availability.reduce((total, item) => total + Number(item?.returnSlotsLeft || 0), 0);
        document.getElementById('station-count').textContent = stations.length;
        document.getElementById('station-note').textContent = `${active.length} active · ${stations.length - active.length} inactive`;
        document.getElementById('available-count').textContent = available;
        document.getElementById('return-slots').textContent = freeSlots;
        document.getElementById('pending-count').textContent = returns.length;
        document.getElementById('stations').innerHTML = stations.length ? stations.map((station, index) => {
            const slots = availability[index]?.returnSlotsLeft;
            return `<tr><td><strong>${PBAdmin.esc(station.name)}</strong><small>${PBAdmin.esc(station.city)}</small></td><td>${station.availableCount} / ${station.totalCapacity}</td><td>${slots == null ? '—' : slots}</td><td><span class="admin-pill ${station.status === 'ACTIVE' ? '' : 'danger'}">${PBAdmin.esc(station.status)}</span></td></tr>`;
        }).join('') : '<tr><td colspan="4" class="admin-empty">No station records.</td></tr>';
    } catch (error) { PBAdmin.message(error.message, true); }
});
