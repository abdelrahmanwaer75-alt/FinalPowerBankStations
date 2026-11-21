'use strict';

(() => {
    const returnId = Number(new URLSearchParams(location.search).get('return'));
    const maxPhotos = 6, maxBytes = 5 * 1024 * 1024;
    let ret, selected = [];
    const $ = (selector) => document.querySelector(selector);
    const message = (text, error = false) => { const b = $('#banner'); b.textContent = text; b.hidden = false; b.classList.toggle('error', error); };
    const show = (id) => { $(id).hidden = false; };
    const esc = (value) => PB.esc(value ?? '—');
    document.addEventListener('DOMContentLoaded', async () => {
        PB.updateSignInLink(); if (!PB.requireLogin({ inPlace: true, title: 'Sign in to upload return photos', message: 'Sign in to add photos and follow the return review.' })) return;
        if (!returnId) { show('#empty'); return; }
        try { ret = await PB.api.returns.get(returnId); render(); } catch (error) { error.status === 404 ? show('#empty') : message(error.message, true); }
    });
    function render() {
        show('#content');
        $('#return-summary').textContent = `${ret.reference} · ${ret.returnStationName}`;
        $('#status').textContent = ret.status.replaceAll('_', ' ').toLowerCase(); $('#status').className = `review-status ${ret.status.toLowerCase()}`;
        $('#return-details').innerHTML = [['Return reference', ret.reference], ['Station', ret.returnStationName], ['Returned', PB.dateTime(ret.returnedAt)], ['Photos already saved', `${ret.images.length} / ${maxPhotos}`]].map(([label, value]) => `<li><span>${esc(label)}</span><strong>${esc(value)}</strong></li>`).join('');
        $('#continue').href = `return-inspection.html?return=${ret.id}`;
        $('#files').addEventListener('change', (event) => select([...event.target.files]));
        $('.upload-zone').addEventListener('dragover', (event) => event.preventDefault());
        $('.upload-zone').addEventListener('drop', (event) => { event.preventDefault(); select([...event.dataTransfer.files]); });
        $('#upload').addEventListener('click', upload);
    }
    function select(files) {
        const invalid = files.find((file) => !['image/jpeg', 'image/png'].includes(file.type) || file.size > maxBytes);
        if (invalid) return message(`${invalid.name} must be a JPG or PNG and no larger than 5 MB.`, true);
        if (ret.images.length + files.length > maxPhotos) return message(`Only ${maxPhotos} photos are allowed for one return.`, true);
        selected = files; previews();
    }
    function previews() {
        $('#previews').innerHTML = selected.map((file, index) => `<article class="preview"><img alt="Preview of ${esc(file.name)}" src="${URL.createObjectURL(file)}"><span class="file-name">${esc(file.name)}</span><button type="button" data-remove="${index}" aria-label="Remove ${esc(file.name)}">×</button></article>`).join('');
        $('#previews').querySelectorAll('[data-remove]').forEach((button) => button.addEventListener('click', () => { selected.splice(Number(button.dataset.remove), 1); previews(); }));
    }
    async function upload() {
        if (!selected.length) return message('Choose at least one photo before submitting.', true);
        const button = $('#upload'); button.disabled = true;
        try { await PB.api.returns.uploadImages(ret.id, selected); location.href = `return-inspection.html?return=${ret.id}`; }
        catch (error) { message(error.message, true); button.disabled = false; }
    }
})();
