'use strict';

(() => {
    const returnId = Number(new URLSearchParams(location.search).get('return'));
    const $ = (selector) => document.querySelector(selector);
    let firstLoad = true;

    document.addEventListener('DOMContentLoaded', async () => {
        PB.updateSignInLink();
        if (!PB.requireLogin({
            inPlace: true,
            title: 'Sign in to see the return review',
            message: 'Sign in to view the status and details of this return.'
        })) return;
        if (!returnId) {
            $('#empty').hidden = false;
            return;
        }
        await loadReturn();
        window.setInterval(loadReturn, 20000);
    });

    async function loadReturn() {
        try {
            render(await PB.api.returns.get(returnId));
            firstLoad = false;
        } catch (error) {
            if (error.status === 404) $('#empty').hidden = false;
            else if (firstLoad) {
                $('#banner').textContent = error.message;
                $('#banner').hidden = false;
            }
        }
    }

    function render(ret) {
        $('#content').hidden = false;
        $('#subtitle').textContent = `${ret.reference} · returned at ${ret.returnStationName}`;
        const status = ret.status.replaceAll('_', ' ').toLowerCase();
        $('#status').textContent = status;
        $('#status').className = `review-status ${ret.status.toLowerCase()}`;
        $('#receipt').href = `return-confirmation.html?return=${ret.id}`;

        const copy = ret.status === 'APPROVED'
            ? 'The return was approved. The device can be placed back into available inventory.'
            : ret.status === 'REJECTED'
                ? 'This return was rejected. Contact support if you need more information.'
                : ret.status === 'REQUIRES_MANUAL_REVIEW'
                    ? 'A staff member is reviewing this return manually.'
                    : 'Your return is waiting for staff review. This page checks the saved status automatically.';
        $('#outcome').textContent = copy;
        $('#photos').innerHTML = ret.images.length
            ? ret.images.map((image) => `<li><span>${PB.esc(image.originalName)}</span><small>${PB.esc(image.mimeType)} · ${Math.ceil(image.fileSize / 1024)} KB</small></li>`).join('')
            : '<li><span>No photos have been submitted yet.</span></li>';
    }
})();
