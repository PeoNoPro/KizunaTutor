/**
 * KizunaTutor - FE-07 Bài làm trực tiếp: hàm dùng chung (không đụng giao diện, test được bằng node --test).
 * Dạng câu và dữ liệu khớp với máy chủ (ExerciseQuestionType / ExerciseQuestionEngine).
 * Chỗ trống trong câu điền khuyết / điền từ: gõ 3 dấu gạch dưới "___".
 */

export const BLANK_RE = /_{3,}/g;

export const QUESTION_TYPES = [
  { value: 'SINGLE_CHOICE', label: 'Trắc nghiệm 1 đáp án', hint: 'Chọn một đáp án đúng' },
  { value: 'MULTI_CHOICE', label: 'Trắc nghiệm nhiều đáp án', hint: 'Chọn tất cả đáp án đúng' },
  { value: 'TRUE_FALSE', label: 'Đúng / Sai', hint: 'Chọn Đúng hoặc Sai' },
  { value: 'FILL_BLANK', label: 'Điền khuyết', hint: 'Gõ từ vào chỗ trống' },
  { value: 'WORD_BANK', label: 'Điền từ có sẵn', hint: 'Kéo hoặc bấm chọn từ vào chỗ trống' },
  { value: 'MATCHING', label: 'Nối cột', hint: 'Chọn vế phải tương ứng với từng vế trái' },
  { value: 'ORDERING', label: 'Sắp xếp thứ tự', hint: 'Sắp xếp các mục theo thứ tự đúng' },
  { value: 'WRITTEN', label: 'Tự luận', hint: 'Viết câu trả lời' }
];

export function questionTypeLabel(type) {
  return QUESTION_TYPES.find((t) => t.value === type)?.label || 'Câu hỏi';
}

export function questionTypeHint(type) {
  return QUESTION_TYPES.find((t) => t.value === type)?.hint || '';
}

const STATE_LABELS = { DRAFT: 'Nháp', NOT_OPEN: 'Chưa mở', OPEN: 'Đang mở', CLOSED: 'Đã đóng' };
const STATE_TONES = { DRAFT: 'done', NOT_OPEN: 'paused', OPEN: 'active', CLOSED: 'done' };

export function exerciseStateLabel(state) {
  return STATE_LABELS[state] || 'Không xác định';
}

export function exerciseStateTone(state) {
  return STATE_TONES[state] || 'done';
}

export function attemptStatusLabel(status) {
  return { IN_PROGRESS: 'Đang làm', SUBMITTED: 'Chờ gia sư chấm', GRADED: 'Đã có điểm' }[status] || 'Không xác định';
}

export function gradingModeLabel(mode) {
  return mode === 'TUTOR' ? 'Gia sư chấm' : 'Hệ thống chấm tự động';
}

/** "10/11/2026 08:00" */
export function formatDateTime(value) {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  const p = (n) => String(n).padStart(2, '0');
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

/** Số điểm hiển thị: 7.50 → "7,5" */
export function formatScore(value) {
  if (value === null || value === undefined || value === '') return '';
  const n = Number(value);
  if (Number.isNaN(n)) return '';
  return String(Math.round(n * 100) / 100).replace('.', ',');
}

/** "mm:ss" hoặc "h:mm:ss" cho đồng hồ đếm ngược */
export function formatCountdown(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const p = (n) => String(n).padStart(2, '0');
  return h > 0 ? `${h}:${p(m)}:${p(sec)}` : `${p(m)}:${p(sec)}`;
}

export function countBlanks(text) {
  return (String(text || '').match(BLANK_RE) || []).length;
}

/** "A ___ B ___ C" → ["A ", " B ", " C"] */
export function splitSegments(text) {
  return String(text || '').split(BLANK_RE);
}

/** "a; b ;c" → ["a","b","c"] (đáp án được chấp nhận / từ gây nhiễu nhập trên một dòng) */
export function splitList(value) {
  return String(value || '').split(/[;\n]/).map((s) => s.trim()).filter(Boolean);
}

/** Dữ liệu mặc định khi gia sư chọn một dạng câu. */
export function defaultQuestion(type) {
  const base = { type, prompt: '', points: 1 };
  switch (type) {
    case 'SINGLE_CHOICE': return { ...base, data: { options: ['', ''], correct: null } };
    case 'MULTI_CHOICE': return { ...base, data: { options: ['', '', ''], correct: [] } };
    case 'TRUE_FALSE': return { ...base, data: { correct: null } };
    case 'FILL_BLANK': return { ...base, data: { text: '', answers: [] } };
    case 'WORD_BANK': return { ...base, data: { text: '', answers: [], distractors: [] } };
    case 'MATCHING': return { ...base, data: { pairs: [{ left: '', right: '' }, { left: '', right: '' }] } };
    case 'ORDERING': return { ...base, data: { items: ['', '', ''] } };
    case 'WRITTEN': return { ...base, data: { accepted: [], sample: '' } };
    default: return { ...base, data: {} };
  }
}

/**
 * Kiểm tra một câu trước khi giao (khớp quy tắc máy chủ). Trả câu lỗi tiếng Việt hoặc null.
 * @param {boolean} autoMode bài "Hệ thống chấm tự động": câu tự luận phải có đáp án được chấp nhận
 */
export function validateQuestion(q, { autoMode = true } = {}) {
  const d = q?.data || {};
  const points = Number(String(q?.points ?? '').replace(',', '.'));
  if (!q?.type) return 'chưa chọn dạng câu hỏi.';
  if (!Number.isFinite(points) || points <= 0 || points > 100) return 'điểm của câu phải lớn hơn 0 và tối đa 100.';
  const needsPrompt = !['FILL_BLANK', 'WORD_BANK'].includes(q.type);
  if (needsPrompt && !String(q.prompt || '').trim()) return 'chưa nhập đề bài.';
  const blank = (s) => !String(s ?? '').trim();
  switch (q.type) {
    case 'SINGLE_CHOICE':
    case 'MULTI_CHOICE': {
      const options = d.options || [];
      if (options.length < 2) return 'cần ít nhất 2 lựa chọn.';
      if (options.some(blank)) return 'có lựa chọn đang để trống.';
      if (q.type === 'SINGLE_CHOICE') {
        if (!(Number.isInteger(d.correct) && d.correct >= 0 && d.correct < options.length)) return 'chưa chọn đáp án đúng.';
      } else if (!(d.correct || []).length) return 'chưa chọn đáp án đúng.';
      return null;
    }
    case 'TRUE_FALSE':
      return typeof d.correct === 'boolean' ? null : 'chưa chọn Đúng hoặc Sai.';
    case 'FILL_BLANK': {
      const n = countBlanks(d.text);
      if (!n) return 'câu cần có ít nhất một chỗ trống (gõ ___ để tạo chỗ trống).';
      for (let i = 0; i < n; i += 1) {
        if (!((d.answers || [])[i] || []).some((a) => !blank(a))) return `chỗ trống ${i + 1} chưa có đáp án.`;
      }
      return null;
    }
    case 'WORD_BANK': {
      const n = countBlanks(d.text);
      if (!n) return 'câu cần có ít nhất một chỗ trống (gõ ___ để tạo chỗ trống).';
      for (let i = 0; i < n; i += 1) if (blank((d.answers || [])[i])) return 'có chỗ trống chưa có từ đúng.';
      return null;
    }
    case 'MATCHING': {
      const pairs = d.pairs || [];
      if (pairs.length < 2) return 'cần ít nhất 2 cặp nối.';
      if (pairs.some((p) => blank(p.left) || blank(p.right))) return 'có cặp nối đang để trống.';
      return null;
    }
    case 'ORDERING': {
      const items = d.items || [];
      if (items.length < 2) return 'cần ít nhất 2 mục để sắp xếp.';
      if (items.some(blank)) return 'có mục đang để trống.';
      return null;
    }
    case 'WRITTEN':
      if (autoMode && !(d.accepted || []).some((a) => !blank(a))) {
        return 'bài chấm tự động nên câu tự luận phải có đáp án được chấp nhận. Hãy nhập đáp án hoặc đổi cách chấm sang "Gia sư chấm".';
      }
      return null;
    default:
      return 'dạng câu hỏi không hợp lệ.';
  }
}

/** Chuẩn hóa dữ liệu câu gửi lên máy chủ (cắt số chỗ trống cho khớp, bỏ dòng trống thừa). */
export function questionPayload(q) {
  const d = q.data || {};
  const points = Number(String(q.points ?? '').replace(',', '.'));
  const data = { ...d };
  if (q.type === 'FILL_BLANK') {
    const n = countBlanks(d.text);
    data.answers = Array.from({ length: n }, (_, i) => ((d.answers || [])[i] || []).map((a) => String(a).trim()).filter(Boolean));
  }
  if (q.type === 'WORD_BANK') {
    const n = countBlanks(d.text);
    data.answers = Array.from({ length: n }, (_, i) => String((d.answers || [])[i] || '').trim());
    data.distractors = (d.distractors || []).map((s) => String(s).trim()).filter(Boolean);
  }
  if (q.type === 'WRITTEN') data.accepted = (d.accepted || []).map((s) => String(s).trim()).filter(Boolean);
  return { type: q.type, prompt: String(q.prompt || '').trim(), points: Number.isFinite(points) ? points : null, data };
}

/** Tổng điểm các câu */
export function totalPoints(questions) {
  return (questions || []).reduce((sum, q) => {
    const n = Number(String(q.points ?? '').replace(',', '.'));
    return sum + (Number.isFinite(n) ? n : 0);
  }, 0);
}

function shuffle(list) {
  const a = [...list];
  for (let i = a.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  if (a.length > 1 && a.every((x, i) => x === list[i])) a.push(a.shift());
  return a;
}

/** Đề cho học viên dựng từ dữ liệu gia sư (dùng cho "Xem thử"; khi làm thật máy chủ trả đề, không kèm đáp án). */
export function previewView(q) {
  const d = q.data || {};
  switch (q.type) {
    case 'SINGLE_CHOICE':
    case 'MULTI_CHOICE': return { options: d.options || [] };
    case 'FILL_BLANK': return { segments: splitSegments(d.text) };
    case 'WORD_BANK': return { segments: splitSegments(d.text), bank: shuffle([...(d.answers || []), ...(d.distractors || [])].filter(Boolean)) };
    case 'MATCHING': return { lefts: (d.pairs || []).map((p) => p.left), rights: shuffle((d.pairs || []).map((p) => p.right)) };
    case 'ORDERING': return { items: shuffle(d.items || []) };
    default: return {};
  }
}

/** Học viên đã trả lời câu này chưa (để tô màu bảng số câu, đếm câu chưa làm khi nộp). */
export function isAnswered(type, response) {
  if (!response) return false;
  const filled = (arr) => Array.isArray(arr) && arr.length > 0 && arr.every((x) => String(x ?? '').trim() !== '');
  switch (type) {
    case 'SINGLE_CHOICE': return Number.isInteger(response.choice);
    case 'MULTI_CHOICE': return Array.isArray(response.choices) && response.choices.length > 0;
    case 'TRUE_FALSE': return typeof response.value === 'boolean';
    case 'FILL_BLANK':
    case 'WORD_BANK': return filled(response.blanks);
    case 'MATCHING': return filled(response.matches);
    case 'ORDERING': return Array.isArray(response.order) && response.order.length > 0;
    case 'WRITTEN': return String(response.text || '').trim() !== '';
    default: return false;
  }
}

/** Câu trả lời dạng chữ để xem lại sau khi nộp. */
export function describeResponse(type, view, response) {
  if (!isAnswered(type, response)) return 'Chưa trả lời';
  const options = view?.options || [];
  switch (type) {
    case 'SINGLE_CHOICE': return options[response.choice] ?? '';
    case 'MULTI_CHOICE': return response.choices.map((i) => options[i]).filter((x) => x !== undefined).join(' · ');
    case 'TRUE_FALSE': return response.value ? 'Đúng' : 'Sai';
    case 'FILL_BLANK':
    case 'WORD_BANK': return response.blanks.map((b, i) => `(${i + 1}) ${b}`).join('   ');
    case 'MATCHING': return (view?.lefts || []).map((l, i) => `${l} → ${response.matches[i] || '…'}`).join('   ');
    case 'ORDERING': return response.order.join(' → ');
    case 'WRITTEN': return response.text;
    default: return '';
  }
}

/** Đáp án đúng dạng chữ (khi gia sư cho xem). */
export function describeCorrect(type, view, correct) {
  if (!correct) return '';
  const options = view?.options || [];
  switch (type) {
    case 'SINGLE_CHOICE': return options[correct.correct] ?? '';
    case 'MULTI_CHOICE': return (correct.correct || []).map((i) => options[i]).join(' · ');
    case 'TRUE_FALSE': return correct.correct ? 'Đúng' : 'Sai';
    case 'FILL_BLANK': return (correct.answers || []).map((a, i) => `(${i + 1}) ${(a || []).join(' / ')}`).join('   ');
    case 'WORD_BANK': return (correct.answers || []).map((a, i) => `(${i + 1}) ${a}`).join('   ');
    case 'MATCHING': return (correct.pairs || []).map((p) => `${p.left} → ${p.right}`).join('   ');
    case 'ORDERING': return (correct.items || []).join(' → ');
    case 'WRITTEN': {
      const accepted = (correct.accepted || []).join(' / ');
      return [accepted, correct.sample ? `Đáp án mẫu: ${correct.sample}` : ''].filter(Boolean).join(' · ');
    }
    default: return '';
  }
}

/** datetime-local ("2026-11-10T08:00") ↔ ISO */
export function toLocalInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}

export function fromLocalInput(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

/**
 * Kiểm tra phần cài đặt bài (khớp máy chủ). Trả {field: lỗi}.
 * @param {{title, instructions, opensLocal, closesLocal, timeLimit, unlimited, maxAttempts}} f
 */
export function validateExerciseSettings(f, { now = new Date(), checkCloseFuture = true } = {}) {
  const errors = {};
  const title = String(f.title || '').trim();
  if (!title) errors.title = 'Vui lòng nhập tiêu đề bài tập.';
  else if (title.length > 150) errors.title = 'Tiêu đề bài tập tối đa 150 ký tự.';
  if (String(f.instructions || '').trim().length > 5000) errors.instructions = 'Hướng dẫn làm bài tối đa 5000 ký tự.';
  const opens = f.opensLocal ? new Date(f.opensLocal) : null;
  const closes = f.closesLocal ? new Date(f.closesLocal) : null;
  if (opens && closes && !(opens < closes)) errors.closesAt = 'Thời gian đóng đề phải sau thời gian mở đề.';
  else if (closes && checkCloseFuture && !(closes > now)) errors.closesAt = 'Thời gian đóng đề phải ở tương lai.';
  if (!f.unlimited) {
    const t = Number(f.timeLimit);
    if (!Number.isInteger(t) || t < 1 || t > 600) errors.timeLimitMinutes = 'Thời gian làm bài phải từ 1 đến 600 phút.';
  }
  const m = Number(f.maxAttempts);
  if (!Number.isInteger(m) || m < 1 || m > 20) errors.maxAttempts = 'Số lượt làm bài phải từ 1 đến 20.';
  return errors;
}
