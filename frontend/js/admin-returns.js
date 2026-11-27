'use strict';

document.addEventListener('DOMContentLoaded', () => {
    if (!PBAdmin.requireStaffPage()) return;
    document.getElementById('status').addEventListener('change', load);
    document.getElementById('returns').addEventListener('click', act);
    load();
});

async function load() {
    const status = document.getElementById('status').value;
    const body = document.getElementById('returns');
    try {
        const rows = await PB.api.admin.returns(status);
        document.getElementById('queue-title').textContent = `${rows.length} return${rows.length === 1 ? '' : 's'} in queue`;
        body.innerHTML = rows.length ? rows.map(renderReturn).join('')
            : '<tr><td colspan="6" class="admin-empty">No returns in this queue.</td></tr>';
    } catch (error) {
        PBAdmin.message(error.message, true);
        body.innerHTML = `<tr><td colspan="6" class="admin-empty">${PBAdmin.esc(error.message)}</td></tr>`;
    }
}

function renderReturn(item) {
    const open = item.status === 'PENDING_REVIEW' || item.status === 'REQUIRES_MANUAL_REVIEW';
    const actions = open
        ? `<button class="admin-action secondary" data-action="approve" data-id="${Number(item.id)}" type="button">Approve</button>
           <button class="admin-action secondary" data-action="manual" data-id="${Number(item.id)}" type="button">Manual</button>
           <button class="admin-action secondary" data-action="reject" data-id="${Number(item.id)}" type="button">Reject</button>`
        : '<span class="admin-pill">Decided</span>';
    return `<tr>
        <td><strong>${PBAdmin.esc(item.reference)}</strong><small>#${Number(item.id)}</small></td>
        <td>${PBAdmin.esc(item.powerBankSerial)}</td>
        <td>${PBAdmin.esc(item.returnStation)}</td>
        <td>${Number(item.photoCount) || 0}</td>
        <td>${item.durationMinutes ?? '—'} min<small>${PB.money(item.finalCost)}</small></td>
        <td>${actions}</td>
    </tr>`;
}

async function act(event) {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const id = Number(button.dataset.id);
    const action = button.dataset.action;
    let notes = null;
    if (action === 'reject') {
        notes = prompt('Reason for rejecting this return:');
        if (!notes?.trim()) return;
    } else if (action === 'manual') {
        notes = prompt('Optional note for manual review:') || null;
    }
    if (!confirm(`${action === 'approve' ? 'Approve' : action === 'reject' ? 'Reject' : 'Send'} this return?`)) return;

    button.disabled = true;
    try {
        if (action === 'approve') await PB.api.admin.approve(id);
        else if (action === 'reject') await PB.api.admin.reject(id, notes);
        else await PB.api.admin.manualReview(id, notes);
        PBAdmin.message('Return review updated.');
        await load();
    } catch (error) {
        PBAdmin.message(error.message, true);
        button.disabled = false;
    }
}
