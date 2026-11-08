import { OPERATIONS_BASE as base, api, byId, query, notice, paging, incidentCards, createOperationsPage } from '../../js/pages/operations.js';

// Danh sách sự cố theo kỳ; ba nhóm (phát sinh, khắc phục, chưa đóng) là bộ lọc thay cho khối thống kê.
const state = {reportPage: 0, cutoff: null};
const COUNT_LABELS = {created: 'phát sinh trong kỳ', resolved: 'được khắc phục trong kỳ', open: 'chưa đóng đến cuối kỳ'};

async function loadReport() {
    const params = query(byId('reportFilters'));
    params.set('page', state.reportPage);
    if (state.cutoff) params.set('cutoff', state.cutoff);
    const data = await api(base + '/reports/incidents?' + params); state.cutoff = data.cutoff;
    // Giữ ngày cụ thể của kỳ mặc định để lần chuyển trang sau không bị lệch qua nửa đêm.
    byId('reportFilters').elements.startDate.value = data.startDate; byId('reportFilters').elements.endDate.value = data.endDate;
    const metric = byId('reportMetric').value;
    byId('reportCount').textContent = (data.totalElements ?? data.items.length) + ' sự cố ' + COUNT_LABELS[metric];
    // Báo cáo chỉ trả tên và trạng thái; lấy thêm thao tác được phép của từng sự cố (tối đa 20 dòng một trang).
    const items = await Promise.all(data.items.map(item => api(base + '/incidents/' + encodeURIComponent(item.incidentId)).catch(() => item)));
    incidentCards(byId('reportItems'), items, runCommand);
    paging('report', data, state.reportPage);
}

function runCommand(row, action, payload) {
    page.run(async () => {
        await api(base + '/incidents/' + encodeURIComponent(row.incidentId) + '/' + action, 'POST', payload);
        await loadReport(); notice('Đã lưu thao tác cho sự cố ' + row.subject + '.');
    });
}

const page = createOperationsPage(loadReport);
page.bind('reportFilters', 'submit', async () => { state.reportPage = 0; state.cutoff = null; await loadReport(); });
byId('reportMetric').addEventListener('change', () => page.run(async () => { state.reportPage = 0; state.cutoff = null; await loadReport(); }));
page.bind('reportPrev', 'click', async () => { state.reportPage = Math.max(0, state.reportPage - 1); await loadReport(); });
page.bind('reportNext', 'click', async () => { state.reportPage++; await loadReport(); });
page.start();
