import { mountAdminShell } from '../../components/admin-shell/admin-shell.js';
mountAdminShell();
/**
 * KizunaTutor - Helper dùng chung cho các trang quản trị vận hành (FE-11) và trang chính sách.
 * Mỗi trang giữ logic riêng trong pages/<ten-trang>/<ten-trang>.js; file này chỉ chứa phần lặp lại.
 */
import { fetchAPI } from '../api.js';
import { API_CONFIG } from '../config/api.config.js';
import { showToast } from '../main.js';

export const OPERATIONS_BASE = '/admin/operations';

export const byId = id => document.getElementById(id);

export function api(path, method = 'GET', payload) {
    return fetchAPI(path, {method, body: payload});
}

/** Tải tệp từ API; trả false khi máy chủ báo không có dữ liệu (204) để không báo tải thành công giả. */
export async function downloadFile(path, filename) {
    let response;
    try {
        response = await fetch(API_CONFIG.BASE_URL + path, {credentials: 'include', signal: AbortSignal.timeout(30000)});
    } catch {
        throw new Error('Chưa kết nối được hệ thống. Vui lòng thử lại sau.');
    }
    if (response.status === 204) return false;
    if (!response.ok) {
        const result = await response.json().catch(() => ({}));
        const error = new Error(result.message || 'Chưa tạo được tệp. Vui lòng thử lại.');
        error.status = response.status; throw error;
    }
    const url = URL.createObjectURL(await response.blob()), link = document.createElement('a');
    link.href = url; link.download = filename; document.body.append(link); link.click(); link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    return true;
}

/** Hiển thị ô báo cáo: số có phân cách, thời gian ISO theo định dạng Việt Nam, trống giữ trống. */
export function formatReportCell(value) {
    if (value == null) return '';
    if (typeof value === 'number') return new Intl.NumberFormat('vi-VN').format(value);
    if (Array.isArray(value)) { const [y, m, d, h = 0, min = 0] = value; return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}, ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`; }
    if (/^\d{4}-\d{2}-\d{2}T/.test(value)) { const [d, t] = value.split('T'); return `${d.split('-').reverse().join('/')}, ${t.slice(0, 5)}`; }
    return String(value);
}

/** Mở hộp thoại in của trình duyệt để lưu PDF; dùng phông của trình duyệt nên tiếng Việt giữ đúng dấu. */
export function printTable(title, subtitle, headers, rows) {
    const frame = document.createElement('iframe');
    frame.setAttribute('aria-hidden', 'true'); frame.style.cssText = 'position:fixed;width:0;height:0;border:0';
    document.body.append(frame);
    const doc = frame.contentDocument, style = doc.createElement('style'), table = doc.createElement('table'), head = doc.createElement('tr'), body = doc.createElement('tbody'), thead = doc.createElement('thead');
    doc.documentElement.lang = 'vi'; doc.title = title;
    style.textContent = '@page{size:A4 landscape;margin:12mm}body{font-family:Arial,sans-serif;font-size:11px;color:#0f2545}h1{font-size:18px;margin:0 0 4px}p{margin:0 0 12px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #d5ccb6;padding:4px 6px;text-align:left;overflow-wrap:anywhere}th{background:#f7f4ec}thead{display:table-header-group}tr{break-inside:avoid}';
    doc.head.append(style);
    const heading = doc.createElement('h1'), note = doc.createElement('p'); heading.textContent = title; note.textContent = subtitle + ' · ' + rows.length + ' dòng';
    headers.forEach(label => { const th = doc.createElement('th'); th.textContent = label; head.append(th); });
    rows.forEach(row => { const line = doc.createElement('tr'); row.forEach(value => { const td = doc.createElement('td'); td.textContent = formatReportCell(value); line.append(td); }); body.append(line); });
    thead.append(head); table.append(thead, body); doc.body.append(heading, note, table);
    frame.contentWindow.addEventListener('afterprint', () => frame.remove(), {once: true});
    frame.contentWindow.focus(); frame.contentWindow.print();
}

export function text(tag, value, className) {
    const node = document.createElement(tag);
    node.textContent = value == null ? '' : String(value);
    if (className) node.className = className;
    return node;
}

export function date(value) {
    if (!value) return 'Chưa có lần đo';
    const parts = Object.fromEntries(new Intl.DateTimeFormat('en-GB', {day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone: 'Asia/Ho_Chi_Minh'}).formatToParts(new Date(value)).map(part => [part.type, part.value]));
    return `${parts.day}/${parts.month}/${parts.year}, ${parts.hour}:${parts.minute}`;
}

export function notice(message, error = false) {
    const node = byId('operationsNotice'); node.textContent = message; node.dataset.error = String(error);
    if (error) node.focus();
}

export function option(select, value, label) {
    const node = text('option', label); node.value = value; select.append(node);
}

export function paging(prefix, data, page) {
    byId(prefix + 'Prev').disabled = page <= 0;
    byId(prefix + 'Next').disabled = (page + 1) * data.size >= data.totalElements;
}

export function query(form) {
    return new URLSearchParams([...new FormData(form)].filter(([, v]) => v !== ''));
}

/** Card trạng thái rỗng theo mẫu FRONTEND_RULES.md mục 7.2. */
export function emptyState(title, message) {
    const box = text('div', '', 'operations-empty-state text-center py-16 bg-white rounded-2xl border border-[#ECE8DE] p-8');
    const admin = document.body.classList.contains('admin-dashboard');
    const icon = text('span', admin ? '○' : 'sentiment_dissatisfied', admin ? 'operations-empty-icon' : 'material-symbols-outlined text-5xl text-[#68737D] mb-3');
    icon.setAttribute('aria-hidden', 'true');
    box.append(icon, text('h3', title, 'text-base font-bold text-[#0F2545]'), text('p', message, 'text-sm text-[#68737D] mt-1'));
    return box;
}

export function incidentCards(container, rows) {
    container.replaceChildren();
    if (!rows.length) { container.append(emptyState('Chưa có sự cố phù hợp', 'Vui lòng thử điều chỉnh lại bộ lọc hoặc quay lại sau ít phút.')); return; }
    const head = text('div', '', 'portal-row-head'); ['Thông tin', 'Trạng thái', 'Thao tác'].forEach(label => head.append(text('span', label))); container.append(head);
    rows.forEach(row => {
        const href = '/pages/operations-incident/?id=' + encodeURIComponent(row.incidentId);
        const item = text('article', '', 'portal-row'), info = text('div', '', 'portal-row-info');
        const link = text('a', row.subject, 'portal-row-title'); link.href = href;
        info.append(link, text('p', 'Mức độ ' + row.severityLabel.toLocaleLowerCase('vi-VN') + (row.assigneeLabel ? ' · Phụ trách: ' + row.assigneeLabel : '')));
        const open = text('a', 'Xem chi tiết', 'portal-link-button'); open.href = href; open.setAttribute('aria-label', 'Xem chi tiết sự cố ' + row.subject);
        item.append(info, text('span', row.statusLabel, 'portal-row-status'), open);
        container.append(item);
    });
}

function setupFieldValidation() {
    document.querySelectorAll('input,textarea,select').forEach(input => {
        const max = input.getAttribute('maxlength');
        if (max) { input.dataset.maxCodePoints = max; input.removeAttribute('maxlength'); }
        input.addEventListener('input', () => {
            input.setCustomValidity(''); input.removeAttribute('aria-invalid');
            if (input.dataset.maxCodePoints && Array.from(input.value).length > Number(input.dataset.maxCodePoints))
                input.setCustomValidity('Nội dung vượt quá ' + input.dataset.maxCodePoints + ' ký tự cho phép.');
            else if (input.required && !input.value.trim()) input.setCustomValidity('Vui lòng nhập thông tin cho trường này.');
        });
        input.addEventListener('invalid', () => {
            input.setAttribute('aria-invalid', 'true');
            if (!input.validationMessage || input.validity.valueMissing) input.setCustomValidity('Vui lòng nhập thông tin cho trường này.');
            else if (input.validity.rangeOverflow || input.validity.rangeUnderflow || input.validity.stepMismatch)
                input.setCustomValidity('Vui lòng nhập số nguyên trong khoảng được hướng dẫn.');
        });
    });
}

/**
 * Khởi tạo khung chung của một trang: đánh dấu menu đang mở, chạy thao tác có trạng thái tải/lỗi,
 * nút "Thử tải lại" và kiểm tra ô nhập. `load` là hàm tải dữ liệu chính của trang.
 */
export function createOperationsPage(load) {
    const body = document.body;
    let busy = false;
    document.querySelectorAll('.operations-navigation a').forEach(a => { if (new URL(a.href).pathname === location.pathname) a.setAttribute('aria-current', 'page'); });
    async function run(action) {
        if (busy) return;
        busy = true; byId('operationsContent').setAttribute('aria-busy', 'true');
        document.querySelectorAll('button[type=submit]').forEach(b => { b.disabled = true; });
        try {
            await action();
            byId('retryLoad').hidden = true;
            body.dataset.ready = 'true'; body.dataset.error = 'false';
            performance.mark('operations-rendered');
            window.dispatchEvent(new Event('operations-rendered'));
        } catch (error) {
            if (error.status === 401) { location.assign('/login'); return; }
            body.dataset.error = 'true';
            notice(error.status ? error.message : 'Chưa kết nối được hệ thống. Vui lòng thử lại sau.', true);
            if (error.status === 409) notice(error.message + ' Nội dung đang nhập được giữ lại; chọn Thử tải lại để đối chiếu dữ liệu mới.', true);
            byId('retryLoad').hidden = false;
            showToast(byId('operationsNotice').textContent, 'error');
        } finally {
            busy = false; byId('operationsContent').setAttribute('aria-busy', 'false');
            document.querySelectorAll('button[type=submit]').forEach(b => { b.disabled = false; });
        }
    }
    const bind = (id, event, handler) => byId(id)?.addEventListener(event, e => {
        if (event === 'submit') e.preventDefault();
        run(() => handler(e));
    });
    bind('retryLoad', 'click', load);
    return {run, bind, start() { setupFieldValidation(); run(load); }};
}
