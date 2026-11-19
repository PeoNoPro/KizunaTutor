/**
 * FE-13: vẽ câu hỏi luyện đề (dùng chung cho xem trước của học vụ và bài làm/kết quả của học viên).
 * Câu hỏi luôn ở dạng chữ có furigana; ảnh minh họa hiện bằng thẻ <img> với đường dẫn đã ký.
 */
import { el, richText, mediaUrl, SECTION_LABELS } from './practice-common.js';

/** Đoạn văn / ảnh dùng chung của một nhóm câu (đọc hiểu). */
export function renderGroup(question) {
  const box = el('div', undefined, 'flex flex-col gap-3');
  if (question.passage) box.append(richText('div', question.passage, 'pr-passage pr-jp'));
  if (question.groupImageUrl) { const img = el('img', undefined, 'pr-figure'); img.src = mediaUrl(question.groupImageUrl); img.alt = 'Hình của đoạn đọc'; img.loading = 'lazy'; box.append(img); }
  return box;
}

/**
 * Một câu hỏi.
 * options: { showAnswers, chosen, onChoose(index), editable(question) }
 * - onChoose có thì các lựa chọn bấm được (đang làm bài).
 * - showAnswers thì tô đáp án đúng (xanh), lựa chọn sai của người làm (đỏ), kèm giải thích và lời thoại.
 */
export function renderQuestion(question, options = {}) {
  const card = el('article', undefined, 'pr-question'); card.dataset.questionId = question.questionId;
  const stem = el('div', undefined, 'pr-question-stem pr-jp');
  stem.append(el('span', String(question.number), 'pr-number'));
  stem.append(question.stem ? richText('div', question.stem) : el('div', question.section === 'LISTENING' ? 'Nghe và chọn câu trả lời đúng.' : '', 'text-[15px] text-[#6b7580]'));
  card.append(stem);
  if (question.imageUrl) { const img = el('img', undefined, 'pr-figure'); img.src = mediaUrl(question.imageUrl); img.alt = 'Hình của câu ' + question.number; img.loading = 'lazy'; card.append(img); }

  const list = el('div', undefined, 'pr-options pr-jp');
  question.options.forEach((text, index) => {
    const value = index + 1, button = el('button', undefined, 'pr-option'); button.type = 'button';
    button.append(el('span', String(value), 'pr-option-mark'), richText('span', text));
    button.setAttribute('aria-pressed', String(options.chosen === value));
    if (options.showAnswers) {
      if (value === question.correct) button.dataset.result = 'correct';
      else if (options.chosen === value) button.dataset.result = 'wrong';
    }
    if (options.onChoose && !options.showAnswers) button.onclick = () => options.onChoose(value, button);
    else button.disabled = true;
    list.append(button);
  });
  card.append(list);

  if (options.showAnswers && (question.explanation || question.script)) {
    const box = el('div', undefined, 'pr-explain');
    if (question.explanation) { box.append(el('strong', 'Giải thích')); box.append(richText('div', question.explanation, 'pr-jp')); }
    if (question.script) {
      const details = el('details'); details.append(el('summary', 'Lời thoại phần nghe'), richText('div', question.script, 'pr-script pr-jp'));
      box.append(details);
    }
    card.append(box);
  }
  if (options.editable) {
    const actions = el('div', undefined, 'flex justify-end'), edit = el('button', 'Sửa câu', 'pr-btn'); edit.type = 'button';
    edit.onclick = () => options.editable(question); actions.append(edit); card.append(actions);
  }
  return card;
}

/** Cả một mondai (xem trước của học vụ): tiêu đề, hướng dẫn, đoạn văn nhóm, các câu. */
export function renderMondai(mondai, options = {}) {
  const box = el('section', undefined, 'pr-card pr-mondai');
  const head = el('div', undefined, 'pr-mondai-head');
  const chip = el('span', SECTION_LABELS[mondai.section] || mondai.sectionLabel, 'pr-chip'); chip.dataset.section = mondai.section;
  head.append(chip, el('h3', `問題${mondai.number}`, 'pr-mondai-title'), el('span', `${mondai.questions.length} câu`, 'text-[13px] text-[#6b7580]'));
  box.append(head);
  if (mondai.instruction) box.append(richText('div', mondai.instruction, 'pr-instruction pr-jp'));
  let group = null;
  for (const question of mondai.questions) {
    if (question.groupId && question.groupId !== group) { group = question.groupId; if (question.passage || question.groupImageUrl) box.append(renderGroup(question)); }
    box.append(renderQuestion({ ...question, section: mondai.section }, { showAnswers: options.showAnswers, editable: options.editable }));
  }
  return box;
}
