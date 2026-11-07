/**
 * Quản lý báo sự cố kỹ thuật cho Admin từ một yêu cầu hỗ trợ được chuyển lên.
 * Sự cố vào danh sách của Admin ở trạng thái "Chưa xử lý"; Quản lý theo dõi ở trang "Sự cố đã báo".
 */
import {fetchAPI} from '../api.js';
import {showToast} from '../main.js';

const BASE = '/admin/operations/reported-incidents';
const SEVERITIES = [['NORMAL', 'Bình thường'], ['LOW', 'Thấp'], ['HIGH', 'Cao'], ['CRITICAL', 'Nghiêm trọng']];
const api = (path = '', options = {}) => fetchAPI(BASE + path, {csrfPath: BASE + '/csrf', ...options});

function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}

/** Mô tả điền sẵn: nội dung yêu cầu và những gì CS đã xử lý, để Admin không phải hỏi lại. */
function draftDescription(ticket) {
  const lines = [`Từ yêu cầu hỗ trợ "${ticket.subject}" (${ticket.categoryLabel}) của ${ticket.requesterDisplayName}.`];
  if (ticket.description) lines.push('', 'Người dùng mô tả:', ticket.description);
  if (ticket.escalation?.reason) lines.push('', 'Lý do Nhân viên hỗ trợ chuyển lên:', ticket.escalation.reason);
  if (ticket.escalation?.handlingSummary) lines.push('', 'Đã xử lý:', ticket.escalation.handlingSummary);
  lines.push('', 'Mã yêu cầu hỗ trợ: ' + ticket.ticketId);
  return lines.join('\n');
}

function openDialog(ticket) {
  const dialog = element('dialog', undefined, 'staff-dialog portal-dialog');
  dialog.setAttribute('aria-labelledby', 'incidentReportTitle');
  const form = element('form'); form.noValidate = true;
  const head = element('div', undefined, 'portal-dialog-head'), title = element('h2', 'Báo sự cố kỹ thuật cho Admin');
  title.id = 'incidentReportTitle';
  const close = element('button', 'Đóng', 'portal-dialog-close'); close.type = 'button';
  head.append(title, close);
  const body = element('div', undefined, 'portal-dialog-body');
  body.append(element('p', 'Admin sẽ nhận và xử lý sự cố. Bạn theo dõi trạng thái ở mục "Sự cố đã báo".'));

  const field = (label, control) => { const wrap = element('label', label); wrap.append(control); body.append(wrap); return control; };
  const subject = element('input'); subject.name = 'subject'; subject.maxLength = 200; subject.value = ticket.subject.slice(0, 200);
  field('Tiêu đề sự cố', subject);
  const severity = element('select'); severity.name = 'severity';
  SEVERITIES.forEach(([value, label]) => severity.append(new Option(label, value)));
  field('Mức độ', severity);
  const components = element('fieldset'), legend = element('legend', 'Phần bị ảnh hưởng');
  components.append(legend, element('p', 'Không rõ thì để "Tiến trình ứng dụng".', 'staff-help'));
  body.append(components);
  const description = element('textarea'); description.name = 'description'; description.rows = 8; description.maxLength = 10000; description.value = draftDescription(ticket);
  field('Mô tả', description);
  const error = element('p', '', 'staff-dialog-notice'); error.setAttribute('role', 'alert'); error.tabIndex = -1;
  body.append(error);

  const actions = element('div', undefined, 'portal-dialog-actions');
  const submit = element('button', 'Gửi cho Admin', 'primary'); submit.type = 'submit';
  const cancel = element('button', 'Hủy'); cancel.type = 'button';
  actions.append(submit, cancel);
  form.append(head, body, actions); dialog.append(form); document.body.append(dialog);

  let busy = false;
  const dismiss = () => { if (!busy) dialog.close(); };
  close.onclick = cancel.onclick = dismiss;
  dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
  dialog.addEventListener('close', () => dialog.remove());

  api('/components').then(options => {
    options.forEach(({value, label}) => {
      const item = element('label', undefined, 'incident-component'), box = element('input');
      box.type = 'checkbox'; box.name = 'components'; box.value = value; box.checked = value === 'APPLICATION';
      item.append(box, document.createTextNode(' ' + label)); components.append(item);
    });
  }).catch(failure => { error.textContent = failure.message; });

  form.onsubmit = async event => {
    event.preventDefault();
    if (busy) return;
    const chosen = [...form.querySelectorAll('input[name=components]:checked')].map(box => box.value);
    const problem = !subject.value.trim() ? 'Vui lòng nhập tiêu đề sự cố.' : !description.value.trim() ? 'Vui lòng nhập mô tả sự cố.' : !chosen.length ? 'Vui lòng chọn ít nhất một phần bị ảnh hưởng.' : '';
    if (problem) { error.textContent = problem; error.focus(); return; }
    busy = true; submit.disabled = cancel.disabled = close.disabled = true; submit.textContent = 'Đang gửi…'; error.textContent = '';
    try {
      await api('', {method: 'POST', body: {subject: subject.value.trim(), description: description.value.trim(), severity: severity.value, components: chosen}});
      busy = false; dialog.close();
      showToast('Đã báo sự cố cho Admin. Theo dõi ở mục "Sự cố đã báo".', 'success');
    } catch (failure) {
      error.textContent = failure.message; error.focus();
    } finally {
      busy = false; submit.disabled = cancel.disabled = close.disabled = false; submit.textContent = 'Gửi cho Admin';
    }
  };
  dialog.showModal(); subject.focus();
}

/** Thẻ đặt ở tab "Xử lý nội bộ" của Quản lý. */
export function incidentReportCard(ticket) {
  const card = element('section', undefined, 'support-card support-action-card');
  card.append(element('h3', 'Lỗi kỹ thuật của hệ thống?'), element('p', 'Nếu yêu cầu này do hệ thống lỗi, báo cho Admin để họ ghi nhận và khắc phục.'));
  const button = element('button', 'Báo sự cố kỹ thuật cho Admin'); button.type = 'button';
  button.onclick = () => openDialog(ticket);
  card.append(button);
  return card;
}
