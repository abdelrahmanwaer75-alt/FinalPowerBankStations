'use strict';

/* Profile: reads and updates the signed-in customer's details, language, theme, and password.
   Every change is validated on the server. The form shows the server's message when a change is refused. */

let profile = null;

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();

    if (!PB.api.hasToken()) {
        show('empty');
        return;
    }

    document.getElementById('profile-form').addEventListener('submit', saveProfile);
    document.getElementById('password-form').addEventListener('submit', changePassword);
    document.getElementById('logout').addEventListener('click', logout);
    document.querySelectorAll('[data-lang]').forEach((b) => b.addEventListener('click', () => savePrefs({ languagePreference: b.dataset.lang })));
    document.querySelectorAll('[data-theme]').forEach((b) => b.addEventListener('click', () => savePrefs({ themePreference: b.dataset.theme })));

    try {
        profile = await PB.api.customer.me();
        render();
    } catch (err) {
        if (err.status === 401) {
            show('empty');
        } else {
            showBanner(err.message);
        }
    }
});

function render() {
    show('content');
    show('head');
    document.getElementById('full-name').value = profile.fullName || '';
    document.getElementById('email').value = profile.email || '';
    document.getElementById('phone').value = profile.phone || '';
    setText('name', profile.fullName);
    setText('meta', profile.email);
    setText('since', profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '—');
    setText('avatar', initials(profile.fullName));
    markSeg('[data-lang]', 'lang', profile.languagePreference);
    markSeg('[data-theme]', 'theme', profile.themePreference);
}

async function saveProfile(event) {
    event.preventDefault();
    const err = document.getElementById('profile-error');
    const fullName = document.getElementById('full-name').value.trim();
    const phone = document.getElementById('phone').value.trim();

    if (!fullName) return setError(err, 'Please enter your full name.');
    if (phone && !/^\+?[0-9 ()-]{7,20}$/.test(phone)) return setError(err, 'Enter a valid mobile number.');

    const btn = document.getElementById('save-profile');
    btn.disabled = true;
    try {
        profile = await PB.api.customer.update({
            fullName,
            phone: phone || null,
            languagePreference: profile.languagePreference,
            themePreference: profile.themePreference
        });
        render();
        setError(err, 'Saved.', true);
        PB.toast('Profile updated.');
    } catch (e) {
        setError(err, e.message);
    } finally {
        btn.disabled = false;
    }
}

async function savePrefs(patch) {
    try {
        profile = await PB.api.customer.update({
            fullName: profile.fullName,
            phone: profile.phone || null,
            languagePreference: patch.languagePreference || profile.languagePreference,
            themePreference: patch.themePreference || profile.themePreference
        });
        render();
        PB.toast('Preferences saved.');
    } catch (e) {
        PB.toast(e.message, true);
    }
}

async function changePassword(event) {
    event.preventDefault();
    const err = document.getElementById('password-error');
    const current = document.getElementById('current-password').value;
    const next = document.getElementById('new-password').value;
    const confirm = document.getElementById('confirm-password').value;

    if (!current) return setError(err, 'Enter your current password.');
    if (next.length < 8) return setError(err, 'The new password needs at least 8 characters.');
    if (next !== confirm) return setError(err, 'The new passwords do not match.');

    try {
        await PB.api.customer.changePassword(current, next);
        ['current-password', 'new-password', 'confirm-password'].forEach((id) => { document.getElementById(id).value = ''; });
        setError(err, 'Password updated.', true);
        PB.toast('Password updated.');
    } catch (e) {
        setError(err, e.message);
    }
}

async function logout() {
    try { await PB.api.auth.logout(); } catch (_) { /* local sign-out still applies */ }
    PB.api.setToken(null);
    location.href = 'index.html';
}

function markSeg(selector, attr, value) {
    document.querySelectorAll(selector).forEach((b) => {
        const key = attr === 'lang' ? b.dataset.lang : b.dataset.theme;
        b.classList.toggle('on', key === value);
    });
}

function initials(name) {
    return (name || '--').split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('') || '--';
}

function setError(el, message, ok = false) {
    el.textContent = message;
    el.classList.toggle('ok', ok);
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
