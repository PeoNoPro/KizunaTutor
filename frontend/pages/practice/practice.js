/**
 * FE-13: trang Luyện đề của học viên — hộp chọn (cấp độ, đề, hình thức, phần thi) và lịch sử thi.
 * Khung học viên có thể nạp lại trang này mà không tải lại toàn bộ (thay <main>), nên mọi thứ khởi tạo ngay khi nạp.
 */
import { api, el, dateTime, SECTION_LABELS, SECTION_ORDER, monthLabel } from '/js/pages/practice-common.js';

// Khung học viên chỉ chép nội dung <main> khi chuyển trang, nên dấu nhận diện nằm ở thẻ bọc bên trong.
const root = document.querySelector('[data-practice-page="landing"]');
if (root) init(root);

function init(root) {
  const $ = id => root.querySelector('#' + id);
  const dialog = $('startDialog'), form = $('startForm');
  let options = null, historyPage = 0;
  const attemptUrl = id => '/learner/practice/attempt?id=' + encodeURIComponent(id);

  // ---- Hộp chọn
  const sectionBox = $('sectionChoices');
  SECTION_ORDER.forEach(code => {
    const label = el('label', undefined, 'pr-choice'), input = el('input'); input.type = 'checkbox'; input.name = 'section'; input.value = code; input.checked = true;
    label.append(input, document.createTextNode(SECTION_LABELS[code]), el('small', '')); sectionBox.append(label);
  });

  function levelData() { return options?.levels.find(l => l.level === form.elements.level.value); }

  function refresh() {
    const data = levelData(), select = form.elements.examId;
    const selected = select.value;
    select.replaceChildren(...(data?.exams.length ? data.exams.map(exam => { const o = el('option', exam.title + (exam.examMonth ? ' (' + monthLabel(exam.examMonth) + ')' : '')); o.value = exam.examId; return o; }) : [el('option', 'Chưa có đề')]));
    if ([...select.options].some(o => o.value === selected)) select.value = selected;
    select.disabled = !data?.exams.length;
    const exam = data?.exams.find(e => e.examId === select.value);
    // Số câu từng phần của đề đang chọn; phần đề không có thì không chọn được.
    sectionBox.querySelectorAll('label').forEach(label => {
      const code = label.querySelector('input').value, count = exam?.counts[code] ?? 0;
      label.querySelector('small').textContent = count + ' câu';
      label.querySelector('input').disabled = !count;
      if (!count) label.querySelector('input').checked = false;
    });
    if (form.elements.all.checked) sectionBox.querySelectorAll('input:not(:disabled)').forEach(input => { input.checked = true; });
    $('startSubmit').disabled = !exam;
  }

  form.addEventListener('change', event => {
    if (event.target.name === 'all') sectionBox.querySelectorAll('input:not(:disabled)').forEach(input => { input.checked = event.target.checked; });
    if (event.target.name === 'section') form.elements.all.checked = [...sectionBox.querySelectorAll('input:not(:disabled)')].every(input => input.checked);
    refresh();
  });

  $('openStart').addEventListener('click', async () => {
    $('startNotice').hidden = true;
    try { if (!options) options = await api('/learner/practice/options'); refresh(); dialog.showModal(); }
    catch (error) { alert(error.message); }
  });
  dialog.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => dialog.close()));

  form.addEventListener('submit', async event => {
    event.preventDefault();
    const f = form.elements, sections = [...sectionBox.querySelectorAll('input:checked')].map(input => input.value);
    if (!sections.length) { $('startNotice').textContent = 'Hãy chọn ít nhất một phần thi.'; $('startNotice').hidden = false; return; }
    const body = { level: f.level.value, examId: f.examId.value, wrongOnly: f.wrongOnly.value === 'true', sections };
    $('startSubmit').disabled = true; $('startSubmit').textContent = 'Đang chuẩn bị đề…';
    try { const result = await api('/learner/practice/attempts', { method: 'POST', body }); location.assign(attemptUrl(result.attemptId)); }
    catch (error) { $('startNotice').textContent = error.message; $('startNotice').hidden = false; }
    finally { $('startSubmit').disabled = false; $('startSubmit').textContent = 'Bắt đầu làm bài'; }
  });

  // ---- Lịch sử thi
  async function loadHistory(append = false) {
    const box = $('historyList');
    try {
      const data = await api(`/learner/practice/attempts?page=${historyPage}&size=10`);
      if (!append) box.replaceChildren();
      if (!data.items.length && !append) box.append(el('p', 'Bạn chưa làm bài nào. Bấm "Bắt đầu luyện ngay" để làm bài đầu tiên.', 'pr-empty'));
      data.items.forEach(item => {
        const link = el('a', undefined, 'pl-attempt'); link.href = attemptUrl(item.attemptId);
        const info = el('div');
        info.append(el('strong', `${item.level} · ${item.examTitle || item.modeLabel}${item.wrongOnly ? ' · Làm lại câu sai' : ''}`),
          el('small', `${item.sectionLabels.join(', ')} · ${dateTime(item.startedAt)}`));
        const score = el('div', undefined, 'pl-score');
        if (item.submitted) { score.append(document.createTextNode(`${item.correctCount}/${item.questionCount}`)); score.append(el('small', 'câu đúng')); }
        else score.append(el('span', 'Đang làm', 'pr-badge'));
        link.append(info, score, el('span', item.submitted ? 'Xem lại ›' : 'Làm tiếp ›', 'pr-btn pl-go'));
        box.append(link);
      });
      $('historyCount').textContent = data.totalElements ? data.totalElements + ' lần làm' : '';
      $('historyMore').hidden = (historyPage + 1) * data.size >= data.totalElements;
    } catch (error) { box.replaceChildren(el('p', error.message, 'pr-notice')); }
  }
  $('historyMore').addEventListener('click', () => { historyPage++; loadHistory(true); });
  loadHistory();
}
