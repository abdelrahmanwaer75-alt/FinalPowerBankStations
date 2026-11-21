'use strict';

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    if (PB.api.hasToken()) {
        const accountAction = document.getElementById('home-account-action');
        accountAction.href = 'dashboard.html';
        accountAction.textContent = 'Open my dashboard';
        document.querySelector('.home-signin-note').hidden = true;
    }
    const status = document.getElementById('home-status');
    try {
        const stations = await PB.loadStations();
        document.getElementById('home-stations').textContent = String(stations.length);
        document.getElementById('home-available').textContent = String(stations.reduce((sum, station) => sum + Number(station.available || 0), 0));
        document.getElementById('home-price').textContent = stations[0]?.price != null ? `${PB.rateMoney(stations[0].price)} / min` : 'Check map';
        status.textContent = stations.length ? 'Live station availability from the network' : 'No active stations are listed right now';
    } catch (error) {
        document.getElementById('home-stations').textContent = '—';
        document.getElementById('home-available').textContent = '—';
        document.getElementById('home-price').textContent = 'Check map';
        status.textContent = 'Station data is unavailable. Open the map to retry when the service is online.';
    }
});
