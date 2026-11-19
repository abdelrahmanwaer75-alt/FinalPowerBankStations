'use strict';

/* Shared header and preferences (theme and language).
   Pages add <div id="app-header" data-active="map"></div> and load this file. */

window.PB = window.PB || {};

PB.prefs = (function () {
    const KEY = 'pb_prefs';

    function read() {
        try { return JSON.parse(localStorage.getItem(KEY)) || {}; } catch { return {}; }
    }

    function write(next) {
        localStorage.setItem(KEY, JSON.stringify(next));
    }

    return {
        get: () => ({ theme: 'light', lang: 'en', ...read() }),
        set(patch) {
            write({ ...read(), ...patch });
            apply();
        }
    };

    function apply() {
        const p = PB.prefs.get();
        document.documentElement.dataset.theme = p.theme;
        document.documentElement.lang = p.lang;
        document.documentElement.dir = p.lang === 'ar' ? 'rtl' : 'ltr';
    }
})();

PB.prefs.set({}); // apply saved preferences before the page paints

PB.renderHeader = function (activeKey) {
    const mount = document.getElementById('app-header');
    if (!mount) return;

    const links = [
        { key: 'map', label: 'Map', href: 'map.html' },
        { key: 'rentals', label: 'My Rental', href: 'rentals.html' },
        { key: 'history', label: 'History', href: 'rentals.html' },
        { key: 'profile', label: 'Profile', href: 'login.html' }
    ];

    const p = PB.prefs.get();

    mount.innerHTML = `
        <header class="app-header">
            <div class="app-header__left">
                <a class="brand" href="index.html">
                    <span class="brand-mark" aria-hidden="true">⚡</span>
                    <span class="brand-text">
                        <strong>PowerBank Stations</strong>
                        <small><span class="dot"></span> Network Active</small>
                    </span>
                </a>
                <nav class="app-nav" aria-label="Main navigation">
                    ${links.map((l) => `<a href="${l.href}" class="${l.key === activeKey ? 'active' : ''}">${l.label}</a>`).join('')}
                </nav>
            </div>
            <div class="app-header__right">
                <div class="lang-switch" role="group" aria-label="Language">
                    <button type="button" data-lang="en" class="${p.lang === 'en' ? 'is-active' : ''}">EN</button>
                    <span aria-hidden="true">|</span>
                    <button type="button" data-lang="ar" class="${p.lang === 'ar' ? 'is-active' : ''}">العربية</button>
                </div>
                <button type="button" class="icon-btn" id="theme-toggle" aria-label="Toggle dark mode">
                    ${p.theme === 'dark' ? '☀' : '☾'}
                </button>
            </div>
        </header>`;

    mount.querySelectorAll('[data-lang]').forEach((btn) => {
        btn.addEventListener('click', () => {
            PB.prefs.set({ lang: btn.dataset.lang });
            PB.renderHeader(activeKey);
        });
    });

    document.getElementById('theme-toggle').addEventListener('click', () => {
        const next = PB.prefs.get().theme === 'dark' ? 'light' : 'dark';
        PB.prefs.set({ theme: next });
        PB.renderHeader(activeKey);
    });
};

document.addEventListener('DOMContentLoaded', () => {
    const active = document.getElementById('app-header')?.dataset.active || '';
    PB.renderHeader(active);
});
