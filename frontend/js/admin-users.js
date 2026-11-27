'use strict';

let requestState = { canApprove: false, requests: [] };

document.addEventListener('DOMContentLoaded', () => {
    if (!PBAdmin.requireStaffPage()) return;
    document.getElementById('staff-request-form').addEventListener('submit', submitStaffRequest);
    document.getElementById('requests-table').addEventListener('click', reviewRequest);
    loadRequests();
});

async function loadRequests() {
    const body = document.getElementById('requests-table');
    try {
        requestState = await PB.api.admin.accountRequests();
        document.getElementById('request-scope').textContent = requestState.canApprove
            ? 'All requests are shown. You can approve or reject pending requests.'
            : 'Showing requests submitted from your account. The Super Admin makes the final decision.';
        document.getElementById('request-card').hidden = requestState.canApprove;
        if (!requestState.requests.length) {
            body.innerHTML = '<tr><td colspan="5">No staff account requests yet.</td></tr>';
            return;
        }
        body.innerHTML = requestState.requests.map((item) => {
            const pending = item.status === 'PENDING';
            const pillClass = item.status === 'APPROVED' ? '' : item.status === 'REJECTED' ? 'danger' : 'warning';
            const actions = requestState.canApprove && pending
                ? `<button class="admin-action" data-approve="${Number(item.id)}" type="button">Approve</button> <button class="admin-action secondary" data-reject="${Number(item.id)}" type="button">Reject</button>`
                : (item.createdUserId ? 'Account created' : '—');
            return `<tr>
                <td><strong>${PBAdmin.esc(item.fullName)}</strong><small>${PBAdmin.esc(item.email)}${item.phone ? ` · ${PBAdmin.esc(item.phone)}` : ''}</small><small>Requested ${formatDate(item.requestedAt)}</small></td>
                <td>${PBAdmin.esc(roleLabel(item.requestedRole))}</td>
                <td>${PBAdmin.esc(item.requestedBy)}</td>
                <td><span class="admin-pill ${pillClass}">${PBAdmin.esc(item.status)}</span>${item.reviewNotes ? `<small>${PBAdmin.esc(item.reviewNotes)}</small>` : ''}</td>
                <td>${actions}</td>
            </tr>`;
        }).join('');
    } catch (error) {
        body.innerHTML = `<tr><td colspan="5">${PBAdmin.esc(error.message)}</td></tr>`;
        PBAdmin.message(error.message, true);
    }
}

async function submitStaffRequest(event) {
    event.preventDefault();
    const button = document.getElementById('submit-request');
    button.disabled = true;
    const payload = {
        fullName: document.getElementById('staff-name').value.trim(),
        email: document.getElementById('staff-email').value.trim(),
        phone: document.getElementById('staff-phone').value.trim() || null,
        role: document.getElementById('staff-role').value
    };
    try {
        await PB.api.admin.createAccountRequest(payload);
        event.target.reset();
        PBAdmin.message('Request sent. The staff account will be created only after Super Admin approval.');
        await loadRequests();
    } catch (error) {
        PBAdmin.message(error.message, true);
    } finally {
        button.disabled = false;
    }
}

async function reviewRequest(event) {
    const approve = event.target.closest('[data-approve]');
    const reject = event.target.closest('[data-reject]');
    if (!approve && !reject) return;
    const button = approve || reject;
    const id = Number(button.dataset.approve || button.dataset.reject);
    let notes = null;
    if (approve) {
        if (!confirm('Approve this request and create the staff account? A password setup code will be sent to the requested email.')) return;
    } else {
        if (!confirm('Reject this staff account request?')) return;
        notes = prompt('Optional reason for rejection:');
        if (notes === null) return;
    }
    button.disabled = true;
    try {
        if (approve) {
            await PB.api.admin.approveAccountRequest(id);
            PBAdmin.message('Approved. The account is active and a password setup code was sent. If email delivery is disabled, check the backend console.');
        } else {
            await PB.api.admin.rejectAccountRequest(id, notes);
            PBAdmin.message('Request rejected.');
        }
        await loadRequests();
    } catch (error) {
        PBAdmin.message(error.message, true);
        button.disabled = false;
    }
}

function roleLabel(role) { return role === 'STATION_MANAGER' ? 'Station manager' : role === 'ADMIN' ? 'Admin' : role; }
function formatDate(value) {
    if (!value) return 'just now';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
}
