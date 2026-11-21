'use strict';

document.addEventListener('DOMContentLoaded', () => {
    try {
        const theme = localStorage.getItem('pb_theme');
        if (theme === 'dark' || theme === 'light') document.documentElement.dataset.theme = theme;
    } catch (_) { /* Keep the default light theme if storage is unavailable. */ }
    const form = document.getElementById('admin-login-form');
    const password = document.getElementById('admin-password');
    const toggle = document.getElementById('admin-password-toggle');
    const submit = document.getElementById('admin-login-submit');
    const message = document.getElementById('admin-login-message');
    toggle.addEventListener('click', () => {
        const show = password.type === 'password';
        password.type = show ? 'text' : 'password';
        toggle.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
    });
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const identifier = document.getElementById('admin-identifier').value.trim();
        const secret = password.value;
        const emailOrPhone = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(identifier) || /^(?=(?:\D*\d){7,20}$)\+?[0-9 ()-]{7,20}$/.test(identifier);
        document.getElementById('admin-identifier-error').classList.toggle('show', !emailOrPhone);
        document.getElementById('admin-password-error').classList.toggle('show', !secret);
        message.textContent = '';
        message.className = 'form-message';
        if (!emailOrPhone || !secret) return;

        submit.disabled = true;
        submit.textContent = 'Checking staff access…';
        try {
            const result = await PB.api.auth.login({ email: identifier, password: secret });
            PB.api.setToken(result.token, document.getElementById('admin-remember').checked);
            await PB.api.admin.stations();
            location.href = 'admin-dashboard.html';
        } catch (error) {
            PB.api.setToken(null);
            message.textContent = error.code === 'NOT_AUTHORIZED' || error.status === 403
                ? 'This account does not have an active staff role. Contact your administrator.'
                : error.code === 'BAD_CREDENTIALS'
                    ? 'Incorrect email or phone number, or password.'
                    : error.message;
            message.classList.add('error');
            submit.disabled = false;
            submit.textContent = 'Sign in to admin →';
        }
    });
});
