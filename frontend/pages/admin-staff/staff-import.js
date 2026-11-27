import {api, byId, text, downloadFile, emptyState} from '../../js/pages/operations.js';
import {API_CONFIG} from '../../js/config/api.config.js';

const base = '/admin/operations/staff/import';
const dialog = byId('importDialog');
const state = {rows: [], created: 0, busy: false};

/** Tạo lô có thể lâu hơn ngân sách 4,5 giây của fetchAPI, nên dùng hạn riêng để không báo lỗi giả khi máy chủ vẫn đang lưu. */
async function send(path, init) {
  const csrf = await api('/admin/operations/csrf');
  let response;
  try {
    response = await fetch(API_CONFIG.BASE_URL + path, {...init, method: 'POST', credentials: 'include', headers: {...init.headers, [csrf.headerName]: csrf.token}, signal: AbortSignal.timeout(30000)});
  } catch {
    throw new Error('Chưa nhận được phản hồi từ hệ thống. Hãy tải lại danh sách để xem tài khoản đã được tạo hay chưa trước khi thử lại.');
  }
  const result = await response.json().catch(() => ({}));
  if (!response.ok || result.success === false) {
    throw new Error(result.fieldErrors?.length ? result.fieldErrors.map(item => item.message).join(' ') : result.message || 'Chưa thực hiện được thao tác. Vui lòng thử lại.');
  }
  return result.data;
}

function message(value, error = false) {
  const node = byId('importNotice'); node.textContent = value; node.className = error ? 'portal-dialog-error' : 'staff-dialog-notice';
}

function resetPreview() {
  state.rows = []; byId('importPreview').replaceChildren(); byId('importPreview').hidden = true;
  byId('confirmImport').hidden = true;
}

function renderPreview(data) {
  const table = document.createElement('table'), head = document.createElement('tr'), body = document.createElement('tbody');
  // Kết quả đứng ngay sau số dòng để màn hình hẹp vẫn thấy lỗi mà không cần cuộn ngang.
  ['Dòng', 'Kết quả kiểm tra', 'Họ và tên', 'Email', 'Vai trò'].forEach(label => { const th = text('th', label); th.scope = 'col'; head.append(th); });
  const thead = document.createElement('thead'); thead.append(head);
  data.rows.forEach(row => {
    const line = document.createElement('tr'), status = text('td', row.errors.length ? row.errors.map(error => error.message).join(' ') : 'Hợp lệ', row.errors.length ? 'staff-import-error' : '');
    line.append(text('td', row.rowNumber), status);
    [row.fullName, row.email, row.role].forEach(value => line.append(text('td', value)));
    body.append(line);
  });
  const caption = text('caption', 'Kết quả kiểm tra tệp nhập nhân viên', 'sr-only');
  table.append(caption, thead, body);
  byId('importPreview').replaceChildren(table); byId('importPreview').hidden = false;
  const ready = data.errorCount === 0;
  byId('confirmImport').hidden = !ready;
  byId('confirmImport').textContent = 'Xác nhận tạo ' + data.total + ' tài khoản';
  state.rows = ready ? data.rows.map(({rowNumber, fullName, email, phone, role}) => ({rowNumber, fullName, email, phone, role})) : [];
  if (ready) message('Tất cả ' + data.total + ' dòng hợp lệ. Khi xác nhận, hệ thống tạo tài khoản và gửi mật khẩu tạm vào email của từng người.');
  else message('Có ' + data.errorCount + ' trong ' + data.total + ' dòng cần sửa. Chưa tạo tài khoản nào; hãy sửa tệp rồi kiểm tra lại.', true);
}

async function guarded(action) {
  if (state.busy) return;
  state.busy = true; ['checkImport', 'confirmImport', 'downloadTemplate'].forEach(id => { byId(id).disabled = true; });
  try { await action(); }
  catch (error) { message(error.message || 'Chưa thực hiện được thao tác. Vui lòng thử lại.', true); }
  finally { state.busy = false; ['checkImport', 'confirmImport', 'downloadTemplate'].forEach(id => { byId(id).disabled = false; }); }
}

async function check() {
  const file = byId('importFile').files[0];
  resetPreview();
  if (!file) { message('Vui lòng chọn tệp Excel.', true); return; }
  message('Đang kiểm tra tệp…');
  const form = new FormData(); form.append('file', file);
  renderPreview(await send(base + '/preview', {body: form}));
}

async function confirm() {
  message('Đang tạo tài khoản…');
  try {
    const data = await send(base + '/confirm', {headers: {'Content-Type': 'application/json'}, body: JSON.stringify({rows: state.rows})});
    state.created += data.created;
    resetPreview(); byId('importFile').value = '';
    const list = text('ul'); data.emails.forEach(email => list.append(text('li', email)));
    byId('importPreview').replaceChildren(list); byId('importPreview').hidden = false;
    message('Đã tạo ' + data.created + ' tài khoản, ' + data.pendingEmails + ' email đang chờ gửi. Đóng hộp thoại để xem trạng thái gửi trong danh sách; thư thất bại có thể gửi lại ở Xem chi tiết.');
  } catch (error) {
    // Dữ liệu cũ không còn đáng tin nên buộc kiểm tra lại tệp.
    resetPreview(); throw error;
  }
}

byId('importStaff').addEventListener('click', () => {
  state.created = 0; byId('importFile').value = ''; resetPreview(); message(''); dialog.showModal();
});
byId('importFile').addEventListener('change', () => { resetPreview(); message(''); });
byId('downloadTemplate').addEventListener('click', () => guarded(async () => {
  await downloadFile(base + '/template', 'mau-nhap-nhan-vien.xlsx'); message('Đã tải tệp mẫu.');
}));
byId('checkImport').addEventListener('click', () => guarded(check));
byId('importForm').addEventListener('submit', event => { event.preventDefault(); guarded(confirm); });
byId('closeImport').addEventListener('click', () => { if (!state.busy) dialog.close(); });
dialog.addEventListener('cancel', event => { if (state.busy) event.preventDefault(); });
dialog.addEventListener('close', () => {
  if (state.created) window.dispatchEvent(new CustomEvent('staff-imported', {detail: state.created}));
  state.created = 0;
});
