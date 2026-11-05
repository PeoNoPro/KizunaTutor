import { OPERATIONS_BASE as base, api, byId, text, date, notice, paging, emptyState, createOperationsPage } from '../../js/pages/operations.js';

const state = {historyPage: 0, dirty: false, version: null, current: null};
const PATH = base + '/settings/attachment-limit';

async function load() {
    const data = await api(PATH); state.version = data.version; state.current = data.maxMegabytes;
    byId('attachmentLimit').max = data.ceilingMegabytes;
    byId('attachmentCeiling').textContent = 'Từ 1 đến ' + data.ceilingMegabytes + ' MB, đây là mức server đang tiếp nhận được.';
    if (!state.dirty) byId('attachmentLimit').value = data.maxMegabytes;
    byId('attachmentCurrent').textContent = 'Hiện hành: ' + data.maxMegabytes + ' MB mỗi tệp.';
    const history = await api(base + '/settings/history?page=' + state.historyPage);
    byId('configHistory').replaceChildren();
    if (!history.items.length) byId('configHistory').append(emptyState('Chưa có thay đổi cấu hình', 'Lịch sử sẽ hiện ở đây sau lần lưu đầu tiên.'));
    history.items.forEach(row => byId('configHistory').append(text('p', row.actorDisplayName + ' · ' + date(row.occurredAt) + '\n' + row.before + ' MB → ' + row.after + ' MB\nLý do: ' + row.reason)));
    paging('configHistory', history, state.historyPage);
    notice(state.dirty ? 'Đã tải giá trị hiện hành. Hãy đối chiếu với nội dung đang nhập trước khi lưu.' : 'Đã tải cấu hình hiện hành.');
}

async function save() {
    const maxMegabytes = Number(byId('attachmentLimit').value);
    if (!confirm('Đổi giới hạn tệp đính kèm hỗ trợ từ ' + state.current + ' MB thành ' + maxMegabytes + ' MB? Chỉ áp dụng cho lần tải mới, tệp đã lưu giữ nguyên.')) return;
    await api(PATH, 'PUT', {maxMegabytes, expectedVersion: state.version, reason: byId('configReason').value});
    state.dirty = false; byId('configReason').value = '';
    await load(); notice('Đã lưu cấu hình, có hiệu lực ngay với lần tải tệp tiếp theo.');
}

const page = createOperationsPage(load);
page.bind('configForm', 'submit', save);
byId('configForm').addEventListener('input', () => { state.dirty = true; });
page.bind('configHistoryPrev', 'click', async () => { state.historyPage = Math.max(0, state.historyPage - 1); await load(); });
page.bind('configHistoryNext', 'click', async () => { state.historyPage++; await load(); });
page.start();
