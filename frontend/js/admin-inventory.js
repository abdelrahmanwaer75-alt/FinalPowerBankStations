'use strict';
document.addEventListener('DOMContentLoaded', async () => {
    if (!PBAdmin.requireStaffPage()) return;
    let stations = [];
    let availability = [];
    const render = () => {
        const q = document.getElementById('search').value.trim().toLowerCase();
        const rows = stations.map((station, index) => ({ station, slots: availability[index]?.returnSlotsLeft }))
            .filter(({ station }) => !q || `${station.name} ${station.city}`.toLowerCase().includes(q));
        document.getElementById('inventory').innerHTML = rows.length ? rows.map(({ station, slots }) =>
            `<tr><td><strong>${PBAdmin.esc(station.name)}</strong><small>${PBAdmin.esc(station.address)}, ${PBAdmin.esc(station.city)}</small></td><td>${station.availableCount}</td><td>${station.totalCapacity}</td><td>${slots == null ? '—' : slots}</td><td><span class="admin-pill ${station.status === 'ACTIVE' ? '' : 'danger'}">${PBAdmin.esc(station.status)}</span></td></tr>`
        ).join('') : '<tr><td colspan="5" class="admin-empty">No matching stations.</td></tr>';
    };
    try {
        stations = await PB.api.admin.stations();
        availability = await Promise.all(stations.map((station) =>
            PB.api.stations.availability(station.id).catch(() => null)
        ));
        const available = stations.reduce((n, station) => n + Number(station.availableCount || 0), 0);
        const capacity = stations.reduce((n, station) => n + Number(station.totalCapacity || 0), 0);
        const free = availability.reduce((n, item) => n + Number(item?.returnSlotsLeft || 0), 0);
        document.getElementById('available').textContent = available;
        document.getElementById('capacity').textContent = capacity;
        document.getElementById('free').textContent = free;
        document.getElementById('low').textContent = stations.filter((station) => station.status === 'ACTIVE' && station.availableCount < 3).length;
        render();
        document.getElementById('search').addEventListener('input', render);
    } catch (error) { PBAdmin.message(error.message, true); }
});
