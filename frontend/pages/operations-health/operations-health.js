import { OPERATIONS_BASE as base, api, byId, text, date, notice, emptyState, createOperationsPage } from '../../js/pages/operations.js';

async function loadHealth() {
    const data = await api(base + '/health'); byId('healthComponents').replaceChildren();
    if (!data.components.length) {
        notice('Chưa có kết quả quan sát.');
        byId('healthComponents').append(emptyState('Chưa có kết quả quan sát', 'Hệ thống chưa ghi nhận lần đo nào. Vui lòng quay lại sau ít phút.'));
    } else notice('Đã tải kết quả quan sát gần nhất.');
    data.components.forEach(row => {
        const card = text('article', '', 'operations-card');
        card.append(text('h2', row.label), text('p', row.stale ? 'Chưa xác định · Dữ liệu quá hạn' : row.statusLabel),
            text('p', 'Lần đo: ' + date(row.observedAt) + (row.responseTimeMs == null ? '' : ' · ' + row.responseTimeMs + ' mili giây')), text('p', row.reasonLabel));
        if (row.stale || row.statusLabel !== 'Hoạt động') {
            const link = text('a', 'Ghi nhận sự cố'); link.href = '/pages/operations-incidents/?component=' + encodeURIComponent(row.componentCode); card.append(link);
        }
        byId('healthComponents').append(card);
    });
}

const page = createOperationsPage(loadHealth);
page.bind('refreshHealth', 'click', loadHealth);
page.start();
setInterval(() => { if (!document.hidden) page.run(loadHealth); }, 30000);
