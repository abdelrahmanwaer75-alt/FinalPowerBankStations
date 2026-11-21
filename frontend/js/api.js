'use strict';

/* REST client for the Spring Boot backend (see backend/README.md).
   Every page calls PB.api instead of building fetch requests itself. */

window.PB = window.PB || {};

PB.api = (function () {
    const base = (window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || 'http://localhost:8080/api';

    // Remembered sign-in lives in localStorage; otherwise the token lasts for this browser session only.
    const token = () => {
        try { return localStorage.getItem('pb_token') || sessionStorage.getItem('pb_token'); }
        catch (_) {
            try { return sessionStorage.getItem('pb_token'); } catch (_) { return null; }
        }
    };
    const clearToken = () => {
        try { localStorage.removeItem('pb_token'); } catch (_) { /* Storage may be disabled. */ }
        try { sessionStorage.removeItem('pb_token'); } catch (_) { /* Storage may be disabled. */ }
    };

    async function request(method, path, body) {
        const headers = { Accept: 'application/json' };
        if (body !== undefined) headers['Content-Type'] = 'application/json';
        if (token()) headers.Authorization = `Bearer ${token()}`;

        let res;
        try {
            res = await fetch(base + path, {
                method,
                headers,
                body: body !== undefined ? JSON.stringify(body) : undefined
            });
        } catch (err) {
            const e = new Error('The server is not reachable. Check that the backend is running.');
            e.network = true;
            throw e;
        }

        const data = res.status === 204 ? null : await res.json().catch(() => null);

        if (!res.ok) {
            // An expired or invalid token: drop it so the next page asks for sign-in again.
            if (res.status === 401 && token()) clearToken();

            let message = (data && data.message) || `Request failed (${res.status})`;
            if (data && data.fields) {
                message += ': ' + Object.values(data.fields).join(', ');
            }
            const error = new Error(message);
            error.status = res.status;
            error.code = data && data.code;
            throw error;
        }
        return data;
    }

    /* Multipart upload. The browser sets the boundary header, so Content-Type is not set here. */
    async function upload(path, files) {
        const form = new FormData();
        Array.from(files).forEach((f) => form.append('files', f));
        const headers = {};
        if (token()) headers.Authorization = `Bearer ${token()}`;

        let res;
        try {
            res = await fetch(base + path, { method: 'POST', headers, body: form });
        } catch (err) {
            throw new Error('The server is not reachable. Check that the backend is running.');
        }
        const data = await res.json().catch(() => null);
        if (!res.ok) {
            if (res.status === 401 && token()) clearToken();
            let message = (data && data.message) || `Upload failed (${res.status})`;
            if (data && data.fields) message += ': ' + Object.values(data.fields).join(', ');
            throw new Error(message);
        }
        return data;
    }

    return {
        setToken(value, remember) {
            clearToken();
            if (!value) return;
            try { (remember ? localStorage : sessionStorage).setItem('pb_token', value); }
            catch (_) {
                if (remember) {
                    try { sessionStorage.setItem('pb_token', value); return; } catch (_) { /* Fall through. */ }
                }
                throw new Error('Browser storage is unavailable. Enable site storage to stay signed in.');
            }
        },
        hasToken: () => !!token(),

        auth: {
            register: (payload) => request('POST', '/auth/register', payload),
            verify: (payload) => request('POST', '/auth/verify', payload),
            resendVerification: (identifier) => request('POST', '/auth/resend-verification', { identifier }),
            login: (payload) => request('POST', '/auth/login', payload),
            forgotPassword: (identifier) => request('POST', '/auth/forgot-password', { identifier }),
            resetPassword: (payload) => request('POST', '/auth/reset-password', payload),
            logout: () => request('POST', '/auth/logout')
        },
        customer: {
            me: () => request('GET', '/customers/me'),
            update: (body) => request('PUT', '/customers/me', body),
            changePassword: (currentPassword, newPassword) =>
                request('POST', '/customers/me/password', { currentPassword, newPassword })
        },
        pricing: {
            active: () => request('GET', '/pricing/active')
        },
        stations: {
            list: () => request('GET', '/stations'),
            get: (id) => request('GET', `/stations/${id}`),
            nearby: (lat, lng, radiusKm) => request('GET', `/stations/nearby?lat=${lat}&lng=${lng}&radiusKm=${radiusKm}`),
            availability: (id) => request('GET', `/stations/${id}/availability`)
        },
        reservations: {
            create: (stationId, durationMinutes) => request('POST', '/reservations', { stationId, durationMinutes: durationMinutes || 15 }),
            active: () => request('GET', '/reservations/active'),
            get: (id) => request('GET', `/reservations/${id}`),
            cancel: (id) => request('DELETE', `/reservations/${id}`)
        },
        rentals: {
            start: (reservationId) => request('POST', '/rentals/start', { reservationId }),
            active: () => request('GET', '/rentals/active'),
            history: () => request('GET', '/rentals'),
            get: (id) => request('GET', `/rentals/${id}`)
        },
        admin: {
            returns: (status = 'PENDING_REVIEW') => request('GET', `/admin/returns?status=${status}`),
            approve: (id, notes) => request('POST', `/admin/returns/${id}/approve`, { notes: notes || null }),
            reject: (id, reason) => request('POST', `/admin/returns/${id}/reject`, { notes: reason }),
            manualReview: (id, notes) => request('POST', `/admin/returns/${id}/manual-review`, { notes: notes || null }),
            stations: () => request('GET', '/admin/stations'),
            createStation: (body) => request('POST', '/admin/stations', body),
            updateStation: (id, body) => request('PUT', `/admin/stations/${id}`, body),
            addPowerBanks: (id, count) => request('POST', `/admin/stations/${id}/powerbanks`, { count }),
            pricing: () => request('GET', '/admin/pricing'),
            createPricing: (body) => request('POST', '/admin/pricing', body),
            activatePricing: (id) => request('POST', `/admin/pricing/${id}/activate`)
        },
        returns: {
            create: (rentalId, returnStationId) => request('POST', '/returns', { rentalId, returnStationId }),
            uploadImages: (id, files) => upload(`/returns/${id}/images`, files),
            get: (id) => request('GET', `/returns/${id}`),
            status: (id) => request('GET', `/returns/${id}/status`)
        }
    };
})();
