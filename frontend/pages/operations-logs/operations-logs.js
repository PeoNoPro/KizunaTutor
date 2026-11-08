import { OPERATIONS_BASE as base, api, byId, text, date, option, paging, query, emptyState, badge, avatar, createOperationsPage } from '../../js/pages/operations.js';

const state = {page: 0, catalogLoaded: false};

const SOURCE_TONES = {'Nhân sự': 'info', 'Sự cố': 'warning', 'Cấu hình': 'success'};

/** Mỗi dòng nhật ký là một câu: "<Người> <hành động> <đối tượng>", kèm nhóm, kết quả, thời gian; lý do (nếu có) ở dưới. */
function row(event) {
    const item = text('li', '', 'log-item'), body = text('div', '', 'log-body'), sentence = text('p', '', 'log-sentence');
    const target = event.incidentId ? text('a', event.targetLabel) : text('strong', event.targetLabel || '');
    if (event.incidentId) target.href = '/pages/operations-incident/?id=' + encodeURIComponent(event.incidentId);
    sentence.append(text('strong', event.actorDisplayName), document.createTextNode(' · ' + event.actionLabel + (event.targetLabel ? ': ' : '')), target);
    const tags = text('div', '', 'log-tags');
    tags.append(badge(event.sourceLabel, SOURCE_TONES[event.sourceLabel] || 'neutral'));
    // Cấu hình ghi "trước → sau" nên hiện dạng chữ; sự cố hiện trạng thái dạng nhãn màu.
    if (event.resultLabel && event.resultLabel.includes('→')) tags.append(text('span', event.resultLabel, 'log-change'));
    else if (event.resultLabel && event.resultLabel !== 'Đã lưu') tags.append(badge(event.resultLabel));
    body.append(sentence, tags);
    if (event.reason) body.append(text('p', event.reason, 'log-reason'));
    item.append(avatar(event.actorDisplayName), body, text('time', date(event.occurredAt), 'log-time'));
    return item;
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
    paging('log', data, state.page);
}

const page = createOperationsPage(loadLogs);
page.bind('logFilters', 'submit', async () => { state.page = 0; await loadLogs(); });
page.bind('logPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await loadLogs(); });
page.bind('logNext', 'click', async () => { state.page++; await loadLogs(); });
page.start();
