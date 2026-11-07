import { OPERATIONS_BASE as base, api, byId, text, date, paging, emptyState, badge, createOperationsPage } from '../../js/pages/operations.js';

/** Quản lý xem các sự cố kỹ thuật mình đã báo cho Admin: trạng thái, người phụ trách và nội dung đã gửi. */
const state = {page: 0};
const STATUS_TONES = {'Chưa xử lý': 'warning', 'Đang xử lý': 'info', 'Đã khắc phục, chờ đóng': 'success', 'Đã đóng': 'neutral'};

function row(incident) {
    const item = text('article', '', 'portal-row'), info = text('div', '', 'portal-row-info');
    const meta = text('div', '', 'portal-row-meta');
    meta.append(badge(incident.severityLabel), text('span', 'Báo lúc ' + date(incident.createdAt)), text('span', 'Phụ trách: ' + incident.assigneeLabel));
    const detail = text('details'), summary = text('summary', 'Nội dung đã gửi');
    detail.append(summary, text('p', incident.description, 'support-preserve'), text('p', 'Phần bị ảnh hưởng: ' + incident.components.join(', ')));
    info.append(text('strong', incident.subject, 'portal-row-title'), meta, detail);
    const status = text('div'); status.append(badge(incident.statusLabel, STATUS_TONES[incident.statusLabel]));
    item.append(info, status);
    return item;
}

async function load() {
    const data = await api(base + '/reported-incidents?page=' + state.page);
    const list = byId('reportedList');
    list.replaceChildren();
    if (!data.items.length) list.append(emptyState('Bạn chưa báo sự cố nào', 'Khi một yêu cầu hỗ trợ do hệ thống lỗi, mở tab "Xử lý nội bộ" và chọn "Báo sự cố kỹ thuật cho Admin".'));
    else {
        const head = text('div', '', 'portal-row-head'); ['Sự cố', 'Trạng thái'].forEach(label => head.append(text('span', label)));
        list.append(head, ...data.items.map(row));
    }
    byId('reportedPage').textContent = 'Trang ' + (state.page + 1);
    paging('reported', data, state.page);
}

const page = createOperationsPage(load);
page.bind('reportedPrev', 'click', async () => { state.page = Math.max(0, state.page - 1); await load(); });
page.bind('reportedNext', 'click', async () => { state.page++; await load(); });
page.start();
