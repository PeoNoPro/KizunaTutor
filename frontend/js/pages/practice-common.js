/**
 * FE-13 Luyện đề JLPT — phần dùng chung cho trang học vụ (Ngân hàng đề) và học viên (Luyện đề).
 * - richText: hiện chữ có furigana {漢字|かんじ} (thẻ <ruby>), gạch chân __từ__, xuống dòng. Luôn thoát HTML trước.
 * - api / uploadRaw: gọi API kèm token đăng nhập.
 */
import { authRequest, ApiError } from '../api.js';
import { API_CONFIG } from '../config/api.config.js';

export const SECTION_LABELS = { VOCAB: 'Từ vựng', GRAMMAR_READING: 'Ngữ pháp – Đọc hiểu', LISTENING: 'Nghe' };
export const SECTION_ORDER = ['VOCAB', 'GRAMMAR_READING', 'LISTENING'];

export const api = (path, options) => authRequest(path, options);

/** Đường dẫn file nghe/ảnh do API trả (đã ký) → địa chỉ đầy đủ. */
export const mediaUrl = path => (path ? API_CONFIG.BASE_URL + path : '');

const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));

/** Chữ có furigana/gạch chân → HTML an toàn. */
export function richHtml(value) {
  let html = escapeHtml(value);
  html = html.replace(/\{([^{}|]+)\|([^{}]+)\}/g, (_, base, reading) => `<ruby>${base}<rt>${reading}</rt></ruby>`);
  html = html.replace(/__(.+?)__/g, '<u>$1</u>');
  return html.replace(/\n/g, '<br>');
}

export function richText(tag, value, className) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  node.innerHTML = richHtml(value);
  return node;
}

export function el(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined && text !== null) node.textContent = text;
  if (className) node.className = className;
  return node;
}

export function icon(name) { const node = el('span', name, 'material-symbols-outlined'); node.setAttribute('aria-hidden', 'true'); return node; }

/** "1:05" hoặc "1:02:30". */
export function clock(ms) {
  const total = Math.max(0, Math.round(ms / 1000)), h = Math.floor(total / 3600), m = Math.floor(total % 3600 / 60), s = total % 60;
  return (h ? h + ':' + String(m).padStart(2, '0') : String(m)) + ':' + String(s).padStart(2, '0');
}

export function dateTime(value) {
  if (!value) return '';
  return new Date(value).toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function monthLabel(value) { if (!value) return ''; const [y, m] = value.split('-'); return `Tháng ${Number(m)}/${y}`; }

/** Gửi thẳng nội dung tệp (file nghe lớn, không qua multipart). */
export async function uploadRaw(path, file, method = 'PUT') {
  let token = null;
  try { token = localStorage.getItem('kizuna_token'); } catch { /* bỏ qua */ }
  let response;
  try {
    // Tên file gửi trên đường dẫn: header riêng (X-File-Name) bị CORS của backend chặn.
    const url = API_CONFIG.BASE_URL + path + (path.includes('?') ? '&' : '?') + 'name=' + encodeURIComponent(file.name);
    response = await fetch(url, {
      method, body: file,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': file.type || 'audio/mpeg' },
      signal: AbortSignal.timeout(10 * 60 * 1000)
    });
  } catch { throw new ApiError({ message: 'Chưa tải được tệp lên. Kiểm tra kết nối rồi thử lại.' }); }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new ApiError({ status: response.status, message: data.message || 'Chưa tải được tệp lên.' });
  return data;
}

/** Tải tệp (ví dụ tệp Excel mẫu) kèm token. */
export async function download(path, filename) {
  const blob = await authRequest(path, { responseType: 'blob' });
  const url = URL.createObjectURL(blob), link = document.createElement('a');
  link.href = url; link.download = filename; document.body.append(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/** Bật/tắt furigana trên vùng chứa, nhớ lựa chọn của người dùng. */
export function furiganaToggle(container, button) {
  let on = true;
  try { on = localStorage.getItem('kizuna-practice-furigana') !== 'off'; } catch { /* bỏ qua */ }
  const apply = () => { container.classList.toggle('pr-hide-furigana', !on); button.setAttribute('aria-pressed', String(on)); button.lastChild.textContent = on ? 'Furigana: bật' : 'Furigana: tắt'; };
  button.onclick = () => { on = !on; try { localStorage.setItem('kizuna-practice-furigana', on ? 'on' : 'off'); } catch { /* bỏ qua */ } apply(); };
  apply();
}

/**
 * Hộp xác nhận ngay trên trang (thay confirm() của trình duyệt). Trả về true khi người dùng đồng ý; Esc hoặc "Quay lại" là false.
 * host: nơi gắn hộp — trang học viên truyền vùng [data-practice-page] để hộp dùng đúng bộ màu học viên.
 */
/** center: hộp căn giữa, không biểu tượng, hai nút bằng nhau (dùng khi làm bài). message có thể là chuỗi hoặc phần tử DOM. */
export function askConfirm({ title, message, ok = 'Đồng ý', cancel = 'Quay lại', danger = false, iconName, host = document.body, center = false }) {
  return new Promise(resolve => {
    const dialog = el('dialog', undefined, 'pr-dialog pr-confirm');
    if (danger) dialog.dataset.danger = '';
    if (center) dialog.dataset.center = '';
    dialog.setAttribute('aria-labelledby', 'prConfirmTitle');
    const body = el('div', undefined, 'pr-dialog-body'), head = el('div', undefined, 'pr-confirm-head'), text = el('div');
    const titleNode = el('h2', title); titleNode.id = 'prConfirmTitle';
    text.append(titleNode);
    if (message) { const p = el('p'); p.append(message); text.append(p); }
    if (!center) head.append(icon(iconName || (danger ? 'warning' : 'help')));
    head.append(text);
    const row = el('div', undefined, 'pr-confirm-row flex justify-end gap-2 flex-wrap');
    const no = el('button', cancel, 'pr-btn'), yes = el('button', ok, danger ? 'pr-btn pr-btn-danger-solid' : 'pr-btn pr-btn-primary');
    no.type = yes.type = 'button';
    let answer = false;
    no.onclick = () => dialog.close();
    yes.onclick = () => { answer = true; dialog.close(); };
    dialog.addEventListener('close', () => { dialog.remove(); resolve(answer); });
    row.append(no, yes); body.append(head, row); dialog.append(body); host.append(dialog);
    dialog.showModal(); yes.focus();
  });
}

/** Lỗi tệp đề (backend trả "details" theo từng dòng): hiện danh sách để học vụ sửa. */
export function showProblems(box, error) {
  box.replaceChildren(el('strong', error.message || 'Chưa lưu được.'));
  const details = Array.isArray(error.fields) ? error.fields : [];
  if (details.length) {
    const list = el('ul');
    details.slice(0, 50).forEach(p => list.append(el('li', (p.row ? 'Dòng ' + p.row + ': ' : '') + p.message)));
    if (details.length > 50) list.append(el('li', `… và ${details.length - 50} lỗi khác.`));
    box.append(list);
  }
  box.hidden = false;
}
