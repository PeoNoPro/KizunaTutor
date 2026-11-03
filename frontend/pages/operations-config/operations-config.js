import { OPERATIONS_BASE as base, api, byId, text, date, notice, paging, emptyState, createOperationsPage } from '../../js/pages/operations.js';

const state = {historyPage: 0, configDirty: false, version: null, currentConfig: null};

async function loadConfig() {
    const data = await api(base + '/booking-policy'); state.version = data.version; state.currentConfig = data;
    byId('configForm').hidden = !data.editable;
    if (!data.editable) { notice(data.message); return; }
    if (!state.configDirty) {
        byId('durationMinutes').value = data.durationMinutes; byId('minAdvanceHours').value = data.minAdvanceHours;
    }
    byId('configEffective').textContent = 'Hiện hành: ' + data.durationMinutes + ' phút; đặt trước ' + data.minAdvanceHours + ' giờ. Hiệu lực: ' + date(data.effectiveAt);
    notice(state.configDirty ? 'Đã tải giá trị hiện hành. Hãy đối chiếu với nội dung đang nhập trước khi lưu.' : 'Đã tải cấu hình hiện hành.');
    const history = await api(base + '/booking-policy/history?page=' + state.historyPage);
    byId('configHistory').replaceChildren();
    if (!history.items.length) byId('configHistory').append(emptyState('Chưa có thay đổi cấu hình', 'Lịch sử sẽ hiện ở đây sau lần lưu cấu hình đầu tiên.'));
    history.items.forEach(row => byId('configHistory').append(text('p', row.actorDisplayName + ' · ' + date(row.occurredAt) + '\n' + row.before.durationMinutes + ' → ' + row.after.durationMinutes + ' phút; ' + row.before.minAdvanceHours + ' → ' + row.after.minAdvanceHours + ' giờ\nLý do: ' + row.reason)));
    paging('configHistory', history, state.historyPage);
}

async function saveConfig() {
    const payload = {expectedVersion: state.version, durationMinutes: Number(byId('durationMinutes').value), minAdvanceHours: Number(byId('minAdvanceHours').value), reason: byId('configReason').value};
    if (!confirm('Thời lượng: ' + state.currentConfig.durationMinutes + ' → ' + payload.durationMinutes + ' phút. Đặt trước: ' + state.currentConfig.minAdvanceHours + ' → ' + payload.minAdvanceHours + ' giờ. Áp dụng cho lượt đặt mới?')) return;
    await api(base + '/booking-policy', 'PUT', payload); state.configDirty = false; byId('configReason').value = '';
    await loadConfig(); notice('Đã lưu cấu hình. Các lịch đã tạo được giữ nguyên.');
}

const page = createOperationsPage(loadConfig);
page.bind('configForm', 'submit', saveConfig);
byId('configForm').addEventListener('input', () => { state.configDirty = true; });
page.bind('configHistoryPrev', 'click', async () => { state.historyPage = Math.max(0, state.historyPage - 1); await loadConfig(); });
page.bind('configHistoryNext', 'click', async () => { state.historyPage++; await loadConfig(); });
page.start();
