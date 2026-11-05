import { OPERATIONS_BASE as base, api, byId, text, date, notice, option, paging, query, emptyState, createOperationsPage } from '../../js/pages/operations.js';

const state = {page: 0, catalogLoaded: false};

function row(event) {
    const line = document.createElement('tr');
    const target = text('td', event.targetLabel);
    if (event.incidentId) {
        const link = text('a', event.targetLabel); link.href = '/pages/operations-incident/?id=' + encodeURIComponent(event.incidentId);
        target.replaceChildren(link);
    }
    line.append(text('td', date(event.occurredAt)), text('td', event.actorDisplayName), text('td', event.sourceLabel), text('td', event.actionLabel), target, text('td', event.resultLabel), text('td', event.reason || 'Không có'));
    return line;
}

async function loadLogs() {
    if (!state.catalogLoaded) {
        (await api(base + '/assignees')).forEach(admin => option(byId('logActor'), admin.value, admin.label));
        state.catalogLoaded = true;
    }
    const params = query(byId('logFilters')); params.set('page', state.page);
    const data = await api(base + '/audit-log?' + params);
    byId('logRows').replaceChildren(...data.items.map(row));
    byId('logEmpty').replaceChildren();
    if (!data.items.length) byId('logEmpty').append(emptyState('Chưa có nhật ký phù hợp', 'Vui lòng thử điều chỉnh bộ lọc hoặc quay lại sau khi có thao tác mới.'));
    byId('logPage').textContent = 'Trang ' + (state.page + 1);
    paging('log', data, state.page); notice('Đã tải ' + data.totalElements + ' dòng nhật ký.');
}

const page = createOperationsPage(loadLogs);
page.bind('logFilters', 'submit', async () => { state.page = 0; await loadLogs(); });
page.bind('logPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await loadLogs(); });
page.bind('logNext', 'click', async () => { state.page++; await loadLogs(); });
page.start();
