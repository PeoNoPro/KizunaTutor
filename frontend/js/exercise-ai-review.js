/**
 * KizunaTutor - FE-15: chèn gợi ý chấm của AI vào màn chấm bài làm trực tiếp (tab "Bài làm của học viên", FE-07).
 * Trang FE-07 gọi attachAiSuggestions(...) ở cuối renderAttemptDetail; module này không đổi logic chấm của FE-07:
 * nút "Áp dụng gợi ý" chỉ điền ô điểm / nhận xét, gia sư vẫn phải bấm "Lưu điểm".
 */

import { API } from './api.js';
import { escapeHtml, friendlyErrorMessage } from './classroom-helpers.js';
import { reviewsByQuestion, aiSuggestionView, shouldPoll, canRetry, applyValues, isRecentSubmission, isTypedQuestion } from './exercise-ai-review-helpers.js';

const POLL_MS = 15000;
/** Tự tải lại tối đa 20 lần (khoảng 5 phút) mỗi lần mở lượt. */
const MAX_POLLS = 20;
const TONE_CLASS = {
  ai: 'border-primary/40 bg-primary-fixed/40',
  info: 'border-outline-variant bg-surface-container-low',
  warn: 'border-amber-300 bg-amber-50'
};

let pollTimer = null;

function blockHtml(view) {
  const cls = TONE_CLASS[view.tone] || TONE_CLASS.info;
  return `
    <div class="rounded-lg border ${cls} px-3 py-2 space-y-1" data-ai-block>
      <p class="flex flex-wrap items-center gap-2">
        <span class="material-symbols-outlined text-[18px] text-primary" aria-hidden="true">auto_awesome</span>
        <span class="font-semibold">${escapeHtml(view.title || 'AI gợi ý chấm')}</span>
        ${view.canApply ? '<button type="button" class="kz-btn px-2.5 py-1 ml-auto" data-ai-apply>Áp dụng gợi ý</button>' : ''}
      </p>
      ${view.text ? `<p class="whitespace-pre-line [overflow-wrap:anywhere] text-on-surface-variant">${escapeHtml(view.text)}</p>` : ''}
    </div>`;
}

/**
 * @param {HTMLElement} box     khối chi tiết lượt (#attemptDetail của FE-07)
 * @param {string} spaceId
 * @param {string} exerciseId
 * @param {object} attempt     lượt làm bài (AttemptView của FE-07)
 */
export async function attachAiSuggestions(box, spaceId, exerciseId, attempt, polls = 0) {
  clearTimeout(pollTimer);
  if (!box || !attempt || attempt.status === 'IN_PROGRESS') return;
  if (!(attempt.questions || []).some(isTypedQuestion)) return;

  let reviews;
  try {
    reviews = await API.exerciseAiReviews.list(spaceId, exerciseId, attempt.attemptId);
  } catch (err) {
    // Chức năng AI chưa sẵn sàng (chưa tạo bảng, mất kết nối): giữ nguyên màn chấm tay của FE-07
    console.debug('Không tải được gợi ý chấm của AI:', err);
    return;
  }
  if (!box.isConnected) return;
  render(box, spaceId, exerciseId, attempt, reviews, polls);
}

function render(box, spaceId, exerciseId, attempt, reviews, polls = 0) {
  box.querySelectorAll('[data-ai-block], [data-ai-retry-bar]').forEach((el) => el.remove());
  const byQuestion = reviewsByQuestion(reviews);
  const views = [];
  (attempt.questions || []).forEach((q) => {
    const view = aiSuggestionView(q, byQuestion.get(q.questionId), attempt.status, isRecentSubmission(attempt.submittedAt));
    if (!view) return;
    views.push(view);
    const li = box.querySelector(`[data-grade="${CSS.escape(q.questionId)}"]`);
    const scoreGrid = li && li.querySelector('[data-score]')?.closest('div');
    if (!li || !scoreGrid) return;
    scoreGrid.insertAdjacentHTML('beforebegin', blockHtml(view));
    const applyBtn = li.querySelector('[data-ai-apply]');
    applyBtn?.addEventListener('click', () => {
      const commentInput = li.querySelector('[data-comment]');
      const values = applyValues(view, commentInput ? commentInput.value : '');
      if (!values) return;
      li.querySelector('[data-score]').value = values.score;
      if (commentInput) commentInput.value = values.comment;
      applyBtn.textContent = 'Đã áp dụng — bấm Lưu điểm để lưu';
      applyBtn.disabled = true;
    });
  });

  if (canRetry(views)) {
    const bar = document.createElement('div');
    bar.dataset.aiRetryBar = 'true';
    bar.className = 'kz-card p-4 flex flex-wrap items-center gap-3';
    bar.innerHTML = `
      <p class="flex-1 min-w-0 text-on-surface-variant">Có câu tự luận AI chưa chấm được.</p>
      <button type="button" class="kz-btn" data-ai-retry>Chấm lại bằng AI</button>
      <p class="field-error hidden w-full" data-ai-retry-error></p>`;
    const list = box.querySelector('ol');
    (list && list.parentNode ? list.parentNode : box).insertBefore(bar, list);
    bar.querySelector('[data-ai-retry]').addEventListener('click', async (event) => {
      const btn = event.currentTarget;
      btn.disabled = true;
      try {
        const updated = await API.exerciseAiReviews.retry(spaceId, exerciseId, attempt.attemptId);
        if (box.isConnected) render(box, spaceId, exerciseId, attempt, updated, 0);
      } catch (err) {
        const msg = bar.querySelector('[data-ai-retry-error]');
        msg.textContent = friendlyErrorMessage(err);
        msg.classList.remove('hidden');
        btn.disabled = false;
      }
    });
  }

  if (shouldPoll(views) && polls < MAX_POLLS) {
    pollTimer = setTimeout(() => {
      if (box.isConnected) attachAiSuggestions(box, spaceId, exerciseId, attempt, polls + 1);
    }, POLL_MS);
  }
}
