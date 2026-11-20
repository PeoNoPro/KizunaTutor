/**
 * KizunaTutor - FE-15: hiển thị gợi ý chấm của AI cho câu tự luận (hàm thuần, dùng chung giao diện + test).
 * AI chỉ gợi ý; điểm chính thức do gia sư lưu bằng nút "Lưu điểm" của FE-07.
 */

/** Câu học viên tự gõ (điền khuyết, tự luận): AI gợi ý, gia sư duyệt điểm. */
export const TYPED_QUESTION_TYPES = ['FILL_BLANK', 'WRITTEN'];

/**
 * Câu có khối gợi ý AI: câu FE-07 đánh dấu chờ chấm (needsManualGrading — tự luận, điền khuyết có ô không đáp án
 * hoặc học viên điền khác đáp án). Câu điền khuyết máy đã chấm xong (khớp đáp án / bỏ trống) không cần AI.
 */
export function isTypedQuestion(question) {
  return Boolean(question && question.needsManualGrading
    && (!question.type || TYPED_QUESTION_TYPES.includes(question.type)));
}

/** Gợi ý theo câu: Map questionId → review. */
export function reviewsByQuestion(reviews) {
  const map = new Map();
  (Array.isArray(reviews) ? reviews : []).forEach((r) => {
    if (r && r.questionId) map.set(r.questionId, r);
  });
  return map;
}

function formatPoints(value) {
  const n = Number(value);
  if (!Number.isFinite(n)) return '0';
  return String(Math.round(n * 100) / 100).replace('.', ',');
}

/**
 * Cách hiển thị khối AI của một câu.
 * - question: câu trong lượt (FE-07): { questionId, type, points, needsManualGrading, score }
 * - review: gợi ý từ API hoặc undefined (chưa tới lượt AI chấm)
 * - attemptStatus: trạng thái lượt (IN_PROGRESS / SUBMITTED / GRADED)
 * - recent: lượt nộp trong 7 ngày gần đây (AI chỉ tự chấm các lượt này)
 * Trả null nếu câu không cần khối AI.
 */
export function aiSuggestionView(question, review, attemptStatus, recent = true) {
  if (!question || !isTypedQuestion(question) || attemptStatus === 'IN_PROGRESS') return null;
  if (!review) {
    // Lượt đã có điểm cuối, hoặc gia sư đã chấm câu này trước khi AI chạy → AI bỏ qua
    if (attemptStatus === 'GRADED') return null;
    if (question.needsManualGrading && question.score !== null && question.score !== undefined) return null;
    if (!recent) return null;
    return { state: 'waiting', tone: 'info', text: 'AI sẽ chấm câu này trong ít phút.', canApply: false };
  }
  if (review.status === 'DONE') {
    const score = Number(review.suggestedScore);
    const max = Number(question.points);
    const safeScore = Number.isFinite(score) ? Math.min(Math.max(score, 0), Number.isFinite(max) ? max : score) : 0;
    return {
      state: 'done',
      tone: 'ai',
      title: `AI gợi ý: ${formatPoints(safeScore)} / ${formatPoints(question.points)} điểm`,
      text: review.feedback || '',
      canApply: true,
      score: safeScore,
      comment: review.feedback || ''
    };
  }
  if (review.status === 'FAILED') {
    return {
      state: 'failed',
      tone: 'warn',
      text: 'AI chưa chấm được câu này. Bạn có thể chấm tay hoặc chấm lại bằng AI.',
      canApply: false
    };
  }
  return { state: 'pending', tone: 'info', text: 'AI đang chấm câu này…', canApply: false };
}

/** AI chỉ tự chấm lượt nộp trong 7 ngày (khớp backend). */
export const AI_WINDOW_DAYS = 7;

export function isRecentSubmission(submittedAt, now = Date.now()) {
  const t = Date.parse(submittedAt || "");
  return Number.isFinite(t) && now - t <= AI_WINDOW_DAYS * 24 * 3600 * 1000;
}

/** Còn câu đang chờ AI (đang chấm hoặc chưa tới lượt) → trang nên tự tải lại. */
export function shouldPoll(views) {
  return (views || []).some((v) => v && (v.state === 'pending' || v.state === 'waiting'));
}

/** Có câu AI chưa chấm được → hiện nút "Chấm lại bằng AI". */
export function canRetry(views) {
  return (views || []).some((v) => v && v.state === 'failed');
}

/**
 * Giá trị điền vào ô khi bấm "Áp dụng gợi ý": luôn điền điểm; chỉ điền nhận xét khi ô nhận xét đang trống
 * (không ghi đè nhận xét gia sư đã viết).
 */
export function applyValues(view, currentComment) {
  if (!view || !view.canApply) return null;
  const keepComment = typeof currentComment === 'string' && currentComment.trim() !== '';
  return {
    score: String(view.score),
    comment: keepComment ? currentComment : (view.comment || '').slice(0, 5000)
  };
}
