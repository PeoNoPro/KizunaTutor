import { OPERATIONS_BASE as base, api, byId, text, date, notice, option, paging, emptyState, createOperationsPage } from '../../js/pages/operations.js';

const incidentId = new URLSearchParams(location.search).get('id') || '';
const state = {eventPage: 0, version: null};
const actionLabels = {claim: 'Nhận xử lý', progress: 'Ghi diễn biến', resolve: 'Đánh dấu đã giải quyết', resume: 'Tiếp tục xử lý', close: 'Đóng sự cố', release: 'Trả lại danh sách chờ'};

function commandFields() {
    const fields = {progress: ['detail'], resolve: ['result', 'verification'], resume: ['reason'], release: ['reason']};
    const visible = fields[byId('commandAction').value] || [];
    document.querySelectorAll('[data-field]').forEach(n => {
        n.hidden = !visible.includes(n.dataset.field);
        n.querySelectorAll('input,select,textarea').forEach(input => { input.required = !n.hidden; });
    });
}

async function loadIncident() {
    const data = await api(base + '/incidents/' + encodeURIComponent(incidentId)); state.version = data.version;
    const card = byId('incidentDetail'); card.replaceChildren(text('h2', data.subject), text('p', data.description), text('p', data.statusLabel + ' · ' + data.severityLabel),
        text('p', 'Thành phần: ' + data.components.join(', ')), text('p', 'Phụ trách: ' + data.assigneeLabel), text('p', 'Ghi nhận: ' + data.reporterLabel + ' · ' + date(data.createdAt)));
    const previous = byId('commandAction').value; byId('commandAction').replaceChildren();
    data.allowedActions.forEach(a => option(byId('commandAction'), a, actionLabels[a]));
    if (data.allowedActions.includes(previous)) byId('commandAction').value = previous;
    byId('incidentActions').hidden = !data.allowedActions.length; commandFields();
    byId('recurrenceLink').hidden = data.allowedActions.length > 0;
    byId('recurrenceLink').href = '/pages/operations-incidents/?recurrenceOf=' + encodeURIComponent(data.incidentId);
    const history = await api(base + '/incidents/' + encodeURIComponent(incidentId) + '/events?page=' + state.eventPage);
    byId('incidentEvents').replaceChildren();
    if (!history.items.length) byId('incidentEvents').append(emptyState('Chưa có lịch sử xử lý', 'Các thao tác trên sự cố này sẽ được ghi lại tại đây.'));
    history.items.forEach(row => {
        const details = [row.actionLabel + ' · ' + row.actorDisplayName + ' · ' + date(row.occurredAt), row.detail, row.reason, row.verification,
            row.after.statusLabel, 'Phụ trách: ' + row.after.assigneeLabel].filter(Boolean);
        byId('incidentEvents').append(text('p', details.join('\n'), 'operations-event'));
    });
    paging('event', history, state.eventPage); notice('Đã tải chi tiết và lịch sử sự cố.');
}

async function sendCommand() {
    const action = byId('commandAction').value;
    if (action === 'close' && !confirm('Bạn đã xác minh phục hồi và muốn đóng sự cố?')) return;
    const payload = {expectedVersion: state.version};
    const fields = {progress: ['detail'], resolve: ['result', 'verification'], resume: ['reason'], release: ['reason']};
    const inputs = {reason: 'commandReason', detail: 'commandDetail', result: 'commandResult', verification: 'commandVerification'};
    (fields[action] || []).forEach(k => { payload[k] = byId(inputs[k]).value; });
    await api(base + '/incidents/' + encodeURIComponent(incidentId) + '/' + action, 'POST', payload);
    byId('incidentCommandForm').reset(); await loadIncident(); notice('Đã lưu thao tác và lịch sử xử lý.');
}

const page = createOperationsPage(loadIncident);
page.bind('incidentCommandForm', 'submit', sendCommand);
byId('commandAction').addEventListener('change', commandFields);
page.bind('eventPrev', 'click', async () => { state.eventPage = Math.max(0, state.eventPage - 1); await loadIncident(); });
page.bind('eventNext', 'click', async () => { state.eventPage++; await loadIncident(); });
page.start();
