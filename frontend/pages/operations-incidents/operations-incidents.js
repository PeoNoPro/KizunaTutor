import { OPERATIONS_BASE as base, api, byId, text, notice, option, paging, query, incidentCards, downloadFile, createOperationsPage } from '../../js/pages/operations.js';

const params = new URLSearchParams(location.search);
const state = {page: 0, catalogLoaded: false, recurrenceOf: null, applied: ''};

async function catalog() {
    const [components, admins] = await Promise.all([api(base + '/components'), api(base + '/assignees')]);
    components.forEach(row => {
        option(byId('filterComponent'), row.value, row.label);
        const label = text('label', ''); const input = document.createElement('input'); input.type = 'checkbox'; input.value = row.value;
        input.name = 'component'; input.checked = params.get('component') === row.value;
        label.append(input, document.createTextNode(' ' + row.label)); byId('createComponents').append(label);
    });
    admins.forEach(row => option(byId('filterAssignee'), row.value, row.label));
    state.recurrenceOf = params.get('recurrenceOf'); byId('recurrenceNote').hidden = !state.recurrenceOf;
    if (params.has('component') || params.has('recurrenceOf')) byId('createIncidentPanel').showModal();
}

async function loadIncidents() {
    const filters = query(byId('incidentFilters')); filters.set('page', state.page);
    const data = await api(base + '/incidents?' + filters);
    filters.delete('page'); state.applied = filters.toString();
    incidentCards(byId('incidentList'), data.items); paging('incident', data, state.page);
    byId('incidentPage').textContent = 'Trang ' + (state.page + 1); notice('Đã tải danh sách sự cố.');
}

async function load() {
    if (!state.catalogLoaded) { await catalog(); state.catalogLoaded = true; }
    await loadIncidents();
}

async function createIncident() {
    const payload = {subject: byId('incidentSubject').value, description: byId('incidentDescription').value,
        severity: byId('incidentSeverity').value, components: [...document.querySelectorAll('input[name=component]:checked')].map(n => n.value)};
    if (state.recurrenceOf) payload.recurrenceOf = state.recurrenceOf;
    byId('incidentCreateNotice').textContent = '';
    try {
        const data = await api(base + '/incidents', 'POST', payload);
        location.assign('/pages/operations-incident/?id=' + encodeURIComponent(data.incidentId));
    } catch (error) {
        // Thông báo chung nằm phía sau hộp thoại nên hiện thêm lỗi ngay trong biểu mẫu.
        byId('incidentCreateNotice').textContent = error.message; byId('incidentCreateNotice').focus();
        throw error;
    }
}

/** Xuất đúng bộ lọc của danh sách đang hiển thị, kể cả khi ô lọc đã đổi nhưng chưa bấm Lọc. */
async function exportIncidents() {
    notice('Đang tạo tệp Excel…');
    const saved = await downloadFile(base + '/incidents/export?' + state.applied, 'su-co-ky-thuat.xlsx');
    notice(saved ? 'Đã tạo tệp Excel gồm toàn bộ sự cố phù hợp bộ lọc.' : 'Không có sự cố phù hợp bộ lọc để xuất. Chưa tạo tệp nào.');
}

const page = createOperationsPage(load);
page.bind('incidentCreateForm', 'submit', createIncident);
byId('openCreateIncident').addEventListener('click', () => byId('createIncidentPanel').showModal());
['closeCreateIncident', 'cancelCreateIncident'].forEach(id => byId(id).addEventListener('click', () => byId('createIncidentPanel').close()));
page.bind('exportIncidents', 'click', exportIncidents);
page.bind('incidentFilters', 'submit', async () => { state.page = 0; await loadIncidents(); });
page.bind('incidentPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await loadIncidents(); });
page.bind('incidentNext', 'click', async () => { state.page++; await loadIncidents(); });
page.start();
