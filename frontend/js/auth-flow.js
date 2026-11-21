'use strict';

(() => {
    try {
        const theme = localStorage.getItem('pb_theme');
        if (theme === 'dark' || theme === 'light') document.documentElement.dataset.theme = theme;
    } catch (_) { /* Keep the default light theme if storage is unavailable. */ }
    const $ = (selector) => document.querySelector(selector);
    const query = new URLSearchParams(location.search);
    const adminReturn = query.get('return') === 'admin';
    const identifier = $('#identifier');
    const requestedIdentifier = query.get('identifier') || '';
    if (identifier) identifier.value = requestedIdentifier;
    if (adminReturn) {
        document.querySelectorAll('header nav a').forEach((link) => {
            link.href = 'login.html?mode=admin';
            link.textContent = 'Staff sign in';
        });
        document.querySelectorAll('.flow-actions a').forEach((link) => {
            if (link.href.includes('forgot-password.html')) link.href = 'forgot-password.html?return=admin';
            else if (link.href.includes('login.html')) link.href = 'login.html?mode=admin';
        });
    }
    const verifyDestination = $('#verify-destination');
    if (verifyDestination) {
        const at = requestedIdentifier.indexOf('@');
        const masked = at > 1
            ? `${requestedIdentifier.slice(0, 2)}${'•'.repeat(Math.min(6, at - 2))}${requestedIdentifier.slice(at)}`
            : requestedIdentifier.replace(/.(?=.{3})/g, '•');
        verifyDestination.textContent = masked || 'No destination was provided. Return to registration and try again.';
    }

    function createCodeInputs() {
        const row = $('#code-row');
        if (!row) return () => '';
        row.innerHTML = Array.from({ length: 6 }, (_, index) => `<input aria-label="Digit ${index + 1}" inputmode="numeric" autocomplete="one-time-code" maxlength="1" pattern="[0-9]">`).join('');
        const inputs = [...row.querySelectorAll('input')];
        inputs.forEach((input, index) => {
            input.addEventListener('input', () => {
                input.value = input.value.replace(/\D/g, '').slice(-1);
                if (input.value && inputs[index + 1]) inputs[index + 1].focus();
            });
            input.addEventListener('keydown', (event) => {
                if (event.key === 'Backspace' && !input.value && inputs[index - 1]) inputs[index - 1].focus();
            });
            input.addEventListener('paste', (event) => {
                const digits = (event.clipboardData.getData('text').match(/\d/g) || []).slice(0, 6);
                if (!digits.length) return;
                event.preventDefault(); digits.forEach((digit, i) => { if (inputs[i]) inputs[i].value = digit; });
                (inputs[Math.min(digits.length, 6) - 1] || inputs[0]).focus();
            });
        });
        return () => inputs.map((input) => input.value).join('');
    }
    const getCode = createCodeInputs();
    const setMessage = (id, message, error = false) => { const el = $(id); if (el) { el.textContent = message; el.classList.toggle('error', error); el.classList.toggle('success', !error); } };
    const validIdentifier = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value) || /^(?=(?:\D*\d){7,20}$)\+?[0-9 ()-]{7,20}$/.test(value);

    $('#verify-form')?.addEventListener('submit', async (event) => {
        event.preventDefault(); const id = identifier.value.trim(); const code = getCode();
        if (!validIdentifier(id) || code.length !== 6) return setMessage('#verify-message', 'Enter your email or phone number and all six digits.', true);
        const button = $('#verify-submit'); button.disabled = true;
        try { const result = await PB.api.auth.verify({ identifier: id, code }); PB.api.setToken(result.token, true); location.href = 'dashboard.html'; }
        catch (error) { setMessage('#verify-message', error.message, true); button.disabled = false; }
    });
    const resend = $('#resend');
    function startResendCooldown(seconds = 60) {
        if (!resend) return;
        const until = Date.now() + seconds * 1000;
        try { localStorage.setItem(`pb_verify_resend_until:${identifier.value.trim().toLowerCase()}`, String(until)); } catch (_) { /* Timer still works for this page. */ }
        resend.disabled = true;
        resend.setAttribute('aria-disabled', 'true');
        resend.classList.add('is-disabled');
        const tick = () => {
            const remaining = Math.max(0, Math.ceil((until - Date.now()) / 1000));
            if (!remaining) {
                clearInterval(timer);
                resend.disabled = false;
                resend.removeAttribute('aria-disabled');
                resend.classList.remove('is-disabled');
                resend.textContent = 'Resend code';
            } else resend.textContent = `Resend code in ${remaining}s`;
        };
        tick();
        const timer = setInterval(tick, 1000);
    }

    if (resend && identifier?.value) {
        let until = 0;
        try { until = Number(localStorage.getItem(`pb_verify_resend_until:${identifier.value.trim().toLowerCase()}`)) || 0; } catch (_) { /* Start a fresh page timer. */ }
        const remaining = Math.ceil((until - Date.now()) / 1000);
        startResendCooldown(remaining > 0 ? remaining : 60);
    } else if (resend) {
        resend.disabled = true;
        resend.textContent = 'Return to registration';
    }

    resend?.addEventListener('click', async () => {
        const link = $('#resend');
        if (link.disabled || link.getAttribute('aria-disabled') === 'true') return;
        const id = identifier.value.trim();
        if (!validIdentifier(id)) return setMessage('#verify-message', 'The verification destination is missing. Return to registration and try again.', true);
        link.setAttribute('aria-disabled', 'true');
        link.disabled = true;
        try {
            const result = await PB.api.auth.resendVerification(id);
            setMessage('#verify-message', result.message);
            startResendCooldown(60);
        }
        catch (error) {
            setMessage('#verify-message', error.message, true);
            if (error.status === 429) {
                const match = error.message.match(/wait (\d+) seconds/i);
                startResendCooldown(match ? Number(match[1]) : 60);
            }
            else { link.disabled = false; link.removeAttribute('aria-disabled'); }
        }
    });
    $('#forgot-form')?.addEventListener('submit', async (event) => {
        event.preventDefault(); const id = identifier.value.trim();
        if (!validIdentifier(id)) return setMessage('#forgot-message', 'Enter a valid email address or phone number.', true);
        const button = event.submitter; button.disabled = true;
        try {
            const result = await PB.api.auth.forgotPassword(id);
            setMessage('#forgot-message', result.message);
            const returnParam = adminReturn ? '&return=admin' : '';
            setTimeout(() => { location.href = `reset-password.html?identifier=${encodeURIComponent(id)}${returnParam}`; }, 700);
        }
        catch (error) { setMessage('#forgot-message', error.message, true); button.disabled = false; }
    });
    $('#reset-form')?.addEventListener('submit', async (event) => {
        event.preventDefault(); const id = identifier.value.trim(); const code = getCode(); const password = $('#new-password').value; const confirm = $('#confirm-password').value;
        if (!validIdentifier(id) || code.length !== 6) return setMessage('#reset-message', 'Enter your email or phone number and all six digits.', true);
        if (password !== confirm) return setMessage('#reset-message', 'The new passwords do not match.', true);
        if (password.length < 8 || password.length > 72 || !/(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])/.test(password)) return setMessage('#reset-message', 'Use 8 to 72 characters with upper and lower case letters, a number, and a special character.', true);
        const button = event.submitter; button.disabled = true;
        try {
            const result = await PB.api.auth.resetPassword({ identifier: id, code, newPassword: password });
            setMessage('#reset-message', result.message);
            setTimeout(() => { location.href = adminReturn ? 'login.html?mode=admin' : 'login.html'; }, 800);
        }
        catch (error) { setMessage('#reset-message', error.message, true); button.disabled = false; }
    });
})();
