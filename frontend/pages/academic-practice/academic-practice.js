/** FE-13: Ngân hàng đề (cán bộ học vụ) — danh sách đề, tải tệp mẫu, tạo đề từ Excel. */
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { showToast } from '/js/main.js';
import { api, el, monthLabel, dateTime, download, showProblems } from '/js/pages/practice-common.js';

mountAcademicLayout('practice', 'Ngân hàng đề');
const $ = id => document.getElementById(id);
const detailUrl = id => '/academic/practice/detail?id=' + encodeURIComponent(id);

async function load() {
  const params = new URLSearchParams([...new FormData($('filters'))].filter(([, v]) => v));
  const rows = $('examRows');
  try {
    const exams = await api('/academic/practice/exams' + (params.size ? '?' + params : ''));
    $('listNotice').hidden = true;
    if (!exams.length) { rows.replaceChildren(row([el('td', 'Chưa có đề nào. Bấm "Tạo đề từ Excel" để bắt đầu.', 'pr-empty')], 6)); return; }
    rows.replaceChildren(...exams.map(exam => {
      const tr = el('tr'); tr.dataset.href = detailUrl(exam.examId);
      const title = el('td'), link = el('a', exam.title, 'ap-title'); link.href = detailUrl(exam.examId);
      title.append(link, el('div', monthLabel(exam.examMonth), 'ap-sub'));
      const listening = el('td');
      if (!exam.listeningCount) listening.append(el('span', '—', 'ap-sub'));
      else {
        listening.append(el('div', exam.hasAudio ? 'Đã có file nghe' : 'Chưa có file nghe', exam.hasAudio ? '' : 'ap-warn'));
        listening.append(el('div', `${exam.listeningCount} câu nghe`, 'ap-sub'));
      }
      const status = el('td'), badge = el('span', exam.statusLabel, 'pr-badge'); badge.dataset.status = exam.status; status.append(badge);
      tr.append(title, el('td', exam.level), el('td', String(exam.questionCount)), listening, status, el('td', dateTime(exam.updatedAt), 'ap-sub'));
      tr.onclick = event => { if (!event.target.closest('a')) location.assign(tr.dataset.href); };
      return tr;
    }));
  } catch (error) {
    rows.replaceChildren(row([el('td', '', 'pr-empty')], 6));
    $('listNotice').textContent = error.message; $('listNotice').hidden = false;
  }
}
function row(cells, span) { const tr = el('tr'); cells[0].colSpan = span; tr.append(...cells); return tr; }

$('filters').addEventListener('change', load);
$('downloadTemplate').addEventListener('click', async () => {
  try { await download('/academic/practice/template', 'mau-de-luyen-thi.xlsx'); } catch (error) { showToast(error.message, 'error'); }
});
const dialog = $('createDialog');
$('openCreate').addEventListener('click', () => { $('createForm').reset(); $('createNotice').hidden = true; dialog.showModal(); });
dialog.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => dialog.close()));
$('createForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.target, submit = $('createSubmit'), body = new FormData();
  body.append('file', form.elements.file.files[0]);
  [...form.elements.images.files].forEach(file => body.append('images', file));
  submit.disabled = true; submit.textContent = 'Đang kiểm tra…'; $('createNotice').hidden = true;
  try {
    const exam = await api('/academic/practice/exams', { method: 'POST', body, timeoutMs: 120000 });
    showToast('Đã tạo đề ở trạng thái Nháp. Hãy kiểm tra lại rồi đăng.', 'success');
    location.assign(detailUrl(exam.examId));
  } catch (error) { showProblems($('createNotice'), error); }
  finally { submit.disabled = false; submit.textContent = 'Kiểm tra và tạo đề'; }
});
load();
