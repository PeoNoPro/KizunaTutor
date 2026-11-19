/**
 * FE-13: Chi tiết đề (cán bộ học vụ) — xem trước như học viên thấy, sửa từng câu, sửa thông tin đề,
 * tải file nghe (thi theo đề phát cả file), đăng / ẩn / xóa đề nháp, nhập lại Excel.
 */
import { mountAcademicLayout } from '/components/academic-layout/academic-layout.js';
import { showToast } from '/js/main.js';
import { api, el, mediaUrl, monthLabel, uploadRaw, furiganaToggle, showProblems, askConfirm } from '/js/pages/practice-common.js';
import { renderMondai } from '/js/pages/practice-render.js';

mountAcademicLayout('practice', 'Chi tiết đề');
const $ = id => document.getElementById(id);
const examId = new URLSearchParams(location.search).get('id');
let exam = null, editing = null;

function notice(message) { $('pageNotice').textContent = message; $('pageNotice').hidden = !message; }

async function load() {
  if (!examId) { notice('Thiếu mã đề.'); return; }
  try { exam = await api('/academic/practice/exams/' + encodeURIComponent(examId)); render(); notice(''); }
  catch (error) { notice(error.message); }
}

function render() {
  document.title = exam.title + ' - Ngân hàng đề';
  $('examTitle').textContent = exam.title;
  $('examStatus').textContent = exam.statusLabel; $('examStatus').dataset.status = exam.status;
  const total = exam.mondai.reduce((sum, m) => sum + m.questions.length, 0);
  $('examMeta').textContent = [exam.level, monthLabel(exam.examMonth), total + ' câu', exam.used ? 'đã có học viên làm' : 'chưa có học viên làm'].filter(Boolean).join(' · ');
  $('publishButton').hidden = exam.status === 'PUBLISHED';
  $('publishButton').lastChild.textContent = exam.status === 'HIDDEN' ? 'Đăng lại' : 'Đăng cho học viên';
  $('hideButton').hidden = exam.status !== 'PUBLISHED';
  $('deleteButton').hidden = !(exam.status === 'DRAFT' && !exam.publishedAt && !exam.used);
  $('reimportButton').hidden = !(exam.status === 'DRAFT' && !exam.used);

  const form = $('infoForm').elements;
  form.title.value = exam.title; form.level.value = exam.level; form.examMonth.value = exam.examMonth || '';
  form.source.value = exam.source || ''; form.minutesVocab.value = exam.minutesVocab; form.minutesGrammar.value = exam.minutesGrammar; form.minutesListening.value = exam.minutesListening;

  const player = $('audioPlayer');
  if (exam.audio) {
    $('audioInfo').textContent = `${exam.audio.name} · ${(exam.audio.sizeBytes / 1e6).toFixed(1)} MB`;
    const url = mediaUrl(exam.audio.url); if (player.dataset.fileId !== String(exam.audio.fileId)) { player.src = url; player.dataset.fileId = exam.audio.fileId; }
    player.hidden = false;
  } else { $('audioInfo').textContent = 'Chưa có file nghe.'; player.hidden = true; player.removeAttribute('src'); }

  const preview = $('preview');
  preview.replaceChildren(...(exam.mondai.length ? exam.mondai.map(m => renderMondai(m, { showAnswers: true, editable: question => openQuestion(question) })) : [el('p', 'Đề chưa có câu hỏi.', 'pr-empty pr-card')]));
}

$('infoForm').addEventListener('submit', async event => {
  event.preventDefault();
  const f = event.target.elements;
  try {
    exam = await api('/academic/practice/exams/' + examId, { method: 'PUT', body: {
      version: exam.version, title: f.title.value, level: f.level.value, examMonth: f.examMonth.value, source: f.source.value,
      minutesVocab: Number(f.minutesVocab.value), minutesGrammar: Number(f.minutesGrammar.value), minutesListening: Number(f.minutesListening.value) } });
    render(); showToast('Đã lưu thông tin đề.', 'success');
  } catch (error) { showToast(error.status === 409 ? 'Đề vừa được thay đổi ở nơi khác. Hãy tải lại trang.' : error.message, 'error'); }
});

$('audioUpload').addEventListener('click', async () => {
  const file = $('audioFile').files[0], button = $('audioUpload');
  if (!file) { showToast('Hãy chọn file nghe trước.', 'error'); return; }
  button.disabled = true; button.textContent = `Đang tải ${(file.size / 1e6).toFixed(1)} MB…`;
  try { exam = await uploadRaw(`/academic/practice/exams/${examId}/audio`, file); render(); $('audioFile').value = ''; showToast('Đã tải file nghe.', 'success'); }
  catch (error) { showToast(error.message, 'error'); }
  finally { button.disabled = false; button.textContent = 'Tải file nghe lên'; }
});

async function command(path, done, method = 'POST') {
  try { const result = await api(`/academic/practice/exams/${examId}${path}`, { method }); if (result) { exam = result; render(); } showToast(done, 'success'); return true; }
  catch (error) { showToast(error.message, 'error'); return false; }
}
$('publishButton').addEventListener('click', () => command('/publish', 'Đã đăng đề. Học viên đã có thể luyện.'));
$('hideButton').addEventListener('click', async () => {
  if (await askConfirm({ title: 'Ẩn đề này?', message: 'Học viên sẽ không chọn được đề này nữa, lịch sử điểm vẫn giữ.', ok: 'Ẩn đề', iconName: 'visibility_off' })) command('/hide', 'Đã ẩn đề.');
});
$('deleteButton').addEventListener('click', async () => {
  if (!await askConfirm({ title: 'Xóa đề nháp?', message: 'Đề nháp này cùng toàn bộ câu hỏi, ảnh và file nghe sẽ bị xóa, không khôi phục được.', ok: 'Xóa đề', danger: true })) return;
  if (await command('', 'Đã xóa đề nháp.', 'DELETE')) location.assign('/academic/practice');
});

// ---- Sửa một câu
function openQuestion(question) {
  editing = question;
  const f = $('questionForm').elements;
  $('questionTitle').textContent = `Sửa câu ${question.number} – ${question.sectionLabel}, mondai ${question.mondaiNumber}`;
  f.stem.value = question.stem || '';
  [1, 2, 3, 4].forEach(i => { f['o' + i].value = question.options[i - 1] || ''; });
  f.correct.value = String(question.correct); f.explanation.value = question.explanation || ''; f.script.value = question.script || '';
  renderImageEditors(question);
  // Đề đã có học viên làm: không đổi đáp án đúng (bài đã nộp chấm theo đáp án cũ); máy chủ cũng chặn đổi số lựa chọn.
  f.correct.disabled = !!exam.used;
  $('questionNotice').hidden = !exam.used;
  if (exam.used) $('questionNotice').textContent = 'Đề đã có học viên làm: chỉ sửa được nội dung chữ, không đổi đáp án đúng hay số lựa chọn.';
  $('questionDialog').showModal();
}
/** Ảnh trong hộp Sửa câu: ảnh của câu, và ảnh dùng chung của đoạn đọc (nếu câu thuộc một đoạn). Đổi/bỏ ảnh lưu ngay. */
function renderImageEditors(question) {
  const box = $('imageEditors'); box.replaceChildren();
  const editors = [['question', 'Ảnh của câu', question.imageUrl]];
  if (question.groupId) editors.push(['group', 'Ảnh dùng chung của đoạn đọc (áp dụng cho mọi câu trong đoạn)', question.groupImageUrl]);
  for (const [target, label, url] of editors) {
    const wrap = el('div', undefined, 'pr-field');
    wrap.append(el('span', label));
    if (url) { const img = el('img', undefined, 'pr-figure'); img.src = mediaUrl(url); img.alt = label; img.style.maxHeight = '180px'; img.style.objectFit = 'contain'; wrap.append(img); }
    else wrap.append(el('span', 'Chưa có ảnh.', 'ad-help'));
    const input = el('input'); input.type = 'file'; input.accept = 'image/png,image/jpeg,image/webp';
    const row = el('div', undefined, 'flex flex-wrap gap-2');
    const upload = el('button', url ? 'Đổi ảnh' : 'Thêm ảnh', 'pr-btn'); upload.type = 'button';
    upload.onclick = () => { if (!input.files[0]) { showToast('Hãy chọn ảnh trước.', 'error'); return; } saveImage(question, target, input.files[0]); };
    row.append(input, upload);
    if (url) { const remove = el('button', 'Bỏ ảnh', 'pr-btn pr-btn-danger'); remove.type = 'button'; remove.onclick = async () => { if (await askConfirm({ title: 'Bỏ ảnh này?', message: 'Câu hỏi sẽ không còn hiện ảnh này nữa.', ok: 'Bỏ ảnh', danger: true })) saveImage(question, target, null); }; row.append(remove); }
    wrap.append(row); box.append(wrap);
  }
}
async function saveImage(question, target, file) {
  const body = new FormData(); if (file) body.append('file', file);
  try {
    exam = await api(`/academic/practice/exams/${examId}/questions/${question.questionId}/image?target=${target}`, { method: 'POST', body });
    render();
    const fresh = exam.mondai.flatMap(m => m.questions.map(q => ({ ...q, section: m.section, sectionLabel: m.sectionLabel, mondaiNumber: m.number }))).find(q => q.questionId === question.questionId);
    if (fresh) { editing = fresh; renderImageEditors(fresh); }
    showToast(file ? 'Đã lưu ảnh.' : 'Đã bỏ ảnh.', 'success');
  } catch (error) { $('questionNotice').textContent = error.message; $('questionNotice').hidden = false; }
}

$('questionForm').addEventListener('submit', async event => {
  event.preventDefault();
  const f = event.target.elements, options = [1, 2, 3, 4].map(i => f['o' + i].value.trim());
  while (options.length && !options[options.length - 1]) options.pop();
  try {
    exam = await api(`/academic/practice/exams/${examId}/questions/${editing.questionId}`, { method: 'PUT', body: {
      version: exam.version, stem: f.stem.value, options, correct: Number(f.correct.value), // ô bị khóa vẫn đọc được giá trị cũ
      explanation: f.explanation.value, script: f.script.value } });
    $('questionDialog').close(); render(); showToast('Đã lưu câu.', 'success');
  } catch (error) { $('questionNotice').textContent = error.message; $('questionNotice').hidden = false; }
});

// ---- Nhập lại Excel
$('reimportButton').addEventListener('click', () => { $('reimportForm').reset(); $('reimportNotice').hidden = true; $('reimportDialog').showModal(); });
$('reimportForm').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.target, submit = $('reimportSubmit'), body = new FormData();
  body.append('file', form.elements.file.files[0]); [...form.elements.images.files].forEach(file => body.append('images', file));
  submit.disabled = true; submit.textContent = 'Đang kiểm tra và nhập…'; $('reimportNotice').hidden = true;
  try { exam = await api(`/academic/practice/exams/${examId}/import`, { method: 'POST', body, timeoutMs: 120000 }); $('reimportDialog').close(); render(); showToast('Đã nhập lại đề.', 'success'); }
  catch (error) { showProblems($('reimportNotice'), error); }
  finally { submit.disabled = false; submit.textContent = 'Kiểm tra và nhập lại'; }
});

document.querySelectorAll('dialog [data-close]').forEach(button => button.addEventListener('click', () => button.closest('dialog').close()));
furiganaToggle($('preview'), $('furiganaToggle'));
load();
