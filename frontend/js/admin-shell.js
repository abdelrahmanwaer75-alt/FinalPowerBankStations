'use strict';
window.PBAdmin = window.PBAdmin || {};
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.admin-brand').forEach((brand) => { brand.href = '../customer/index.html'; });
    try {
        const theme = localStorage.getItem('pb_theme');
        if (theme === 'dark' || theme === 'light') document.documentElement.dataset.theme = theme;
    } catch (_) { /* Keep the default light theme if storage is unavailable. */ }
});
PBAdmin.requireStaffPage = function () {
    if (!PB.api.hasToken()) {
        const page = location.pathname.split('/').pop() || 'admin-dashboard.html';
        const destination = `../customer/login.html?mode=admin&next=${encodeURIComponent(`../admin/${page}`)}`;
        location.href = destination;
        return false;
    }
    return true;
};
PBAdmin.message = function (text, error = false) { const banner = document.getElementById('admin-banner'); if (!banner) return; banner.textContent = text; banner.hidden = false; banner.classList.toggle('error', error); };
PBAdmin.esc = (value) => PB.esc(value ?? '—');
