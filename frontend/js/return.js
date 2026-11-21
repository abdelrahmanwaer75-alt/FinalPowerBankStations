'use strict';

/* Return: pick a station with a free slot, confirm, then add photos on the rentals page.
   Costs come from the server. The final total is set when the return is created. */

const rentalId = Number(new URLSearchParams(location.search).get('rental'));
let rental = null;
let stations = [];
let selected = null;

document.addEventListener('DOMContentLoaded', async () => {
    PB.updateSignInLink();
    if (!PB.requireLogin({ inPlace: true, title: 'Sign in to return your PowerBank', message: 'Sign in to choose a return station and complete your return.' })) return;

    document.getElementById('confirm').addEventListener('click', confirmReturn);

    try {
        const active = await PB.api.rentals.active();
        rental = active.find((r) => r.id === rentalId) || active[0];
        if (!rental) return show('empty');

        stations = await PB.loadStations();
        render();
    } catch (err) {
        showBanner(err.message);
        show('empty');
    }
});

function render() {
    show('content');
    setText('unit', rental.powerBankSerial);
    setText('ref', rental.reference);
    setText('started', PB.dateTime(rental.startTime));
    setText('rate', `${PB.rateMoney(rental.pricePerMinute)} / min`);
    setText('total', PB.money(rental.cost));
    updateDuration();
    setInterval(updateDuration, 1000);

    const list = document.getElementById('stations');
    list.innerHTML = stations.map((s) => {
        const free = s.returnSlots > 0;
        return `
            <li class="${free ? '' : 'disabled'}" data-id="${s.id}">
                <span class="radio" aria-hidden="true"></span>
                <div style="flex:1">
                    <strong>${PB.esc(s.name)}</strong>
                    <small>${PB.esc(s.address)}</small>
                </div>
                <span class="${free ? 'free' : 'full'}">${free ? s.returnSlots + ' free slots' : 'No free slots'}</span>
            </li>`;
    }).join('') || '<li class="disabled"><small>No stations are available.</small></li>';

    list.querySelectorAll('li[data-id]').forEach((li) => {
        const station = stations.find((s) => s.id === Number(li.dataset.id));
        li.addEventListener('click', () => {
            if (!station || station.returnSlots <= 0) return;
            selected = station;
            list.querySelectorAll('li').forEach((x) => x.classList.toggle('selected', x === li));
            document.getElementById('confirm').disabled = false;
        });
    });
}

function updateDuration() {
    if (!rental) return;
    const seconds = Math.max(0, Math.floor((Date.now() - new Date(rental.startTime)) / 1000));
    const h = String(Math.floor(seconds / 3600)).padStart(2, '0');
    const m = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0');
    const s = String(seconds % 60).padStart(2, '0');
    setText('duration', `${h}:${m}:${s}`);
}

async function confirmReturn() {
    if (!rental || !selected) return;
    if (!confirm(`Return the PowerBank to ${selected.name}?`)) return;
    const btn = document.getElementById('confirm');
    btn.disabled = true;
    try {
        const ret = await PB.api.returns.create(rental.id, selected.id);
        PB.toast(`Returned. Total ${PB.money(ret.finalCost)}. Add photos to finish.`);
        location.href = 'return-photos.html?return=' + ret.id;
    } catch (err) {
        PB.toast(err.message, true);
        btn.disabled = false;
    }
}

function show(id) {
    ['content', 'empty'].forEach((x) => { document.getElementById(x).hidden = x !== id; });
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
