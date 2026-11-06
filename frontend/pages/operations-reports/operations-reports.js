import { OPERATIONS_BASE as base, api, byId, text, date, notice, paging, query, incidentCards, createOperationsPage } from '../../js/pages/operations.js';

const state = {reportPage: 0, metric: null, cutoff: null};

async function loadReport() {
    const params = query(byId('reportFilters'));
    if (state.metric) { params.set('metric', state.metric); params.set('page', state.reportPage); if (state.cutoff) params.set('cutoff', state.cutoff); }
    const data = await api(base + '/reports/incidents?' + params); state.cutoff = data.cutoff;
    // Preserve the concrete default period so a later drilldown cannot cross midnight.
    byId('reportFilters').elements.startDate.value = data.startDate; byId('reportFilters').elements.endDate.value = data.endDate;
    const day = value => value.split('-').reverse().join('/');
    byId('reportCutoff').textContent = 'Kỳ ' + day(data.startDate) + ' đến ' + day(data.endDate) + ' · Chốt: ' + date(data.cutoff);
    byId('reportSummary').replaceChildren();
    const metrics = [['created', 'Mới ghi nhận', data.createdCount, 'Được tạo trong kỳ.'], ['resolved', 'Đã giải quyết', data.resolvedCount, 'Có lần giải quyết trong kỳ; mỗi sự cố tính một lần.'], ['open', 'Còn mở cuối kỳ', data.openAtCutoff, 'Chưa đóng tại thời điểm chốt, gồm sự cố từ trước kỳ.']];
    metrics.forEach(([key, label, count, definition]) => {
        const button = text('button', label + ': ' + count); button.type = 'button';
        button.addEventListener('click', () => page.run(async () => { state.metric = key; state.reportPage = 0; await loadReport(); }));
        byId('reportSummary').append(button, text('p', definition));
        const groups = data.breakdowns[key];
        Object.entries(groups).forEach(([group, values]) => byId('reportSummary').append(text('p', (group === 'status' ? 'Theo trạng thái: ' : 'Theo mức độ: ') + (Object.entries(values).map(([k, v]) => k + ': ' + v).join('; ') || 'Không có sự cố'))));
    });
    byId('reportItems').replaceChildren();
    if (state.metric) { incidentCards(byId('reportItems'), data.items); paging('report', data, state.reportPage); }
    byId('reportPrev').hidden = !state.metric; byId('reportNext').hidden = !state.metric; notice('Đã tổng hợp từ lịch sử sự cố.');
}

const page = createOperationsPage(loadReport);
page.bind('reportFilters', 'submit', async () => { state.metric = null; state.cutoff = null; await loadReport(); });
page.bind('reportPrev', 'click', async () => { state.reportPage = Math.max(0, state.reportPage - 1); await loadReport(); });
page.bind('reportNext', 'click', async () => { state.reportPage++; await loadReport(); });
page.start();
