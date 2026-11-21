'use strict';

(() => {
    const stationId = Number(new URLSearchParams(location.search).get('station'));
    const $ = (selector) => document.querySelector(selector);
    let station;
    let origin = null;
    let map;
    let routeLayer;
    let originMarker;

    document.addEventListener('DOMContentLoaded', async () => {
        PB.updateSignInLink();
        if (!stationId) { $('#empty').hidden = false; return; }
        try {
            station = await PB.api.stations.get(stationId);
            render();
        } catch (error) {
            if (error.status === 404) $('#empty').hidden = false;
            else banner(error.message);
        }
    });

    function render() {
        $('#content').hidden = false;
        $('#station-name').textContent = `${station.name} · ${station.address}, ${station.city}`;
        $('#open-maps').href = destinationUrl();
        $('#use-location').addEventListener('click', useLocation);
        $('#show-route').addEventListener('click', showRoute);
        $('#origin').addEventListener('input', () => { origin = null; });
        loadMap();
    }

    function destination() {
        return { lat: Number(station.latitude), lng: Number(station.longitude) };
    }

    function destinationUrl(start = null) {
        const end = destination();
        const route = start ? `${start.lat},${start.lng};${end.lat},${end.lng}` : '';
        return route
            ? `https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=${encodeURIComponent(route)}`
            : `https://www.openstreetmap.org/?mlat=${end.lat}&mlon=${end.lng}#map=16/${end.lat}/${end.lng}`;
    }

    function loadMap() {
        if (!window.L) {
            $('#map').innerHTML = '<div class="route-demo">Map tiles could not be loaded. Check your internet connection and reload.</div>';
            $('#route-note').textContent = 'Map data is provided by OpenStreetMap.';
            return;
        }
        const end = destination();
        $('#map').replaceChildren();
        map = L.map('map').setView([end.lat, end.lng], 14);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        L.marker([end.lat, end.lng]).addTo(map).bindPopup(PB.esc(station.name)).openPopup();
        $('#route-note').textContent = 'Map and station location use OpenStreetMap. Allow location access or enter a starting address to plan a route.';
        setTimeout(() => map.invalidateSize(), 0);
    }

    function useLocation() {
        if (!navigator.geolocation) return banner('Your browser does not support location access.');
        navigator.geolocation.getCurrentPosition((position) => {
            origin = { lat: position.coords.latitude, lng: position.coords.longitude };
            $('#origin').value = 'Current location';
            $('#route-note').textContent = 'Starting location selected. Choose a travel mode and show the route.';
        }, () => banner('Location permission was not granted. You can enter a starting address instead.'),
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 });
    }

    async function resolveOrigin() {
        if (origin) return origin;
        const address = $('#origin').value.trim();
        if (!address) throw new Error('Use your location or enter a starting address.');
        const response = await fetch(`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=eg&q=${encodeURIComponent(address)}`, {
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) throw new Error('Could not look up that address. Try a more specific location.');
        const results = await response.json();
        if (!results.length) throw new Error('No matching starting location was found.');
        origin = { lat: Number(results[0].lat), lng: Number(results[0].lon) };
        return origin;
    }

    async function showRoute() {
        const button = $('#show-route');
        button.disabled = true;
        try {
            const start = await resolveOrigin();
            const end = destination();
            if (originMarker) map.removeLayer(originMarker);
            originMarker = L.marker([start.lat, start.lng]).addTo(map).bindPopup('Starting point');
            if (routeLayer) map.removeLayer(routeLayer);

            if ($('#mode').value === 'DRIVING') {
                const coords = `${start.lng},${start.lat};${end.lng},${end.lat}`;
                const response = await fetch(`https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`);
                const data = await response.json();
                if (!response.ok || data.code !== 'Ok' || !data.routes?.length) throw new Error('A driving route could not be calculated for these locations.');
                const best = data.routes[0];
                routeLayer = L.geoJSON(best.geometry, { style: { color: '#0057c3', weight: 5 } }).addTo(map);
                $('#distance').textContent = `${(best.distance / 1000).toFixed(1)} km`;
                $('#duration').textContent = `${Math.max(1, Math.round(best.duration / 60))} min`;
            } else {
                routeLayer = L.polyline([[start.lat, start.lng], [end.lat, end.lng]], {
                    color: '#0057c3', weight: 4, dashArray: '8 8'
                }).addTo(map);
                const distance = distanceKm(start, end);
                $('#distance').textContent = `${distance.toFixed(1)} km (direct)`;
                $('#duration').textContent = `About ${Math.max(1, Math.round(distance / 5 * 60))} min`;
            }

            const bounds = L.latLngBounds([[start.lat, start.lng], [end.lat, end.lng]]);
            if (routeLayer.getBounds) bounds.extend(routeLayer.getBounds());
            map.fitBounds(bounds.pad(0.18));
            $('#open-maps').href = destinationUrl(start);
            $('#route-note').textContent = $('#mode').value === 'WALKING'
                ? 'Walking line and travel time are estimates; the line shows direct distance.'
                : 'Driving route estimated by OSRM. Map data © OpenStreetMap contributors.';
            $('#banner').hidden = true;
        } catch (error) {
            banner(error.message || 'A route could not be calculated.');
        } finally {
            button.disabled = false;
        }
    }

    function distanceKm(a, b) {
        const radians = (value) => value * Math.PI / 180;
        const dLat = radians(b.lat - a.lat);
        const dLng = radians(b.lng - a.lng);
        const h = Math.sin(dLat / 2) ** 2 + Math.cos(radians(a.lat)) * Math.cos(radians(b.lat)) * Math.sin(dLng / 2) ** 2;
        return 6371 * 2 * Math.asin(Math.sqrt(h));
    }

    function banner(message) {
        $('#banner').textContent = message;
        $('#banner').hidden = false;
    }
})();
