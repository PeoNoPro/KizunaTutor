import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  reviewsByQuestion, aiSuggestionView, shouldPoll, canRetry, applyValues, isRecentSubmission
} from '../js/exercise-ai-review-helpers.js';

const manual = { questionId: 'q1', points: 5, needsManualGrading: true, score: null };

test('chỉ câu tự luận chờ gia sư chấm mới có khối AI', () => {
  assert.equal(aiSuggestionView({ ...manual, needsManualGrading: false }, undefined, 'SUBMITTED'), null);
  assert.equal(aiSuggestionView(manual, undefined, 'IN_PROGRESS'), null);
  assert.equal(aiSuggestionView(null, undefined, 'SUBMITTED'), null);
});

test('chưa có gợi ý: chờ AI nếu lượt mới nộp; gia sư đã chấm hoặc lượt cũ thì không hiện', () => {
  assert.equal(aiSuggestionView(manual, undefined, 'SUBMITTED').state, 'waiting');
  assert.equal(aiSuggestionView({ ...manual, score: 3 }, undefined, 'GRADED'), null);
  assert.equal(aiSuggestionView({ ...manual, score: 3 }, undefined, 'SUBMITTED'), null);
  assert.equal(aiSuggestionView(manual, undefined, 'SUBMITTED', false), null);
});

test('AI đã gợi ý: hiện điểm / điểm tối đa và cho áp dụng', () => {
  const view = aiSuggestionView(manual, { status: 'DONE', suggestedScore: 3.5, feedback: 'Tốt' }, 'SUBMITTED');
  assert.equal(view.state, 'done');
  assert.equal(view.title, 'AI gợi ý: 3,5 / 5 điểm');
  assert.equal(view.canApply, true);
  assert.equal(view.score, 3.5);
});

test('điểm gợi ý được kẹp trong 0…điểm câu', () => {
  assert.equal(aiSuggestionView(manual, { status: 'DONE', suggestedScore: 9 }, 'SUBMITTED').score, 5);
  assert.equal(aiSuggestionView(manual, { status: 'DONE', suggestedScore: -1 }, 'SUBMITTED').score, 0);
});

test('AI chưa chấm được / đang chấm', () => {
  const failed = aiSuggestionView(manual, { status: 'FAILED' }, 'SUBMITTED');
  assert.equal(failed.state, 'failed');
  assert.match(failed.text, /chấm tay/);
  assert.equal(aiSuggestionView(manual, { status: 'PENDING' }, 'SUBMITTED').state, 'pending');
});

test('tự tải lại khi còn câu đang chờ; nút chấm lại khi có câu lỗi', () => {
  assert.equal(shouldPoll([{ state: 'done' }, { state: 'waiting' }]), true);
  assert.equal(shouldPoll([{ state: 'done' }, { state: 'failed' }]), false);
  assert.equal(canRetry([{ state: 'done' }, { state: 'failed' }]), true);
  assert.equal(canRetry([{ state: 'pending' }]), false);
});

test('áp dụng gợi ý không ghi đè nhận xét gia sư đã viết', () => {
  const view = aiSuggestionView(manual, { status: 'DONE', suggestedScore: 4, feedback: 'Nhận xét AI' }, 'SUBMITTED');
  assert.deepEqual(applyValues(view, ''), { score: '4', comment: 'Nhận xét AI' });
  assert.deepEqual(applyValues(view, 'Của gia sư'), { score: '4', comment: 'Của gia sư' });
  assert.equal(applyValues(aiSuggestionView(manual, { status: 'FAILED' }, 'SUBMITTED'), ''), null);
});

test('gom gợi ý theo câu, bỏ phần tử hỏng', () => {
  const map = reviewsByQuestion([{ questionId: 'a', status: 'DONE' }, null, {}]);
  assert.equal(map.size, 1);
  assert.equal(map.get('a').status, 'DONE');
  assert.equal(reviewsByQuestion(null).size, 0);
});

test('lượt nộp trong 7 ngày', () => {
  const now = Date.parse('2026-11-19T00:00:00Z');
  assert.equal(isRecentSubmission('2026-11-12T00:00:00Z', now), true);
  assert.equal(isRecentSubmission('2026-11-11T23:59:00Z', now), false);
  assert.equal(isRecentSubmission(null, now), false);
});

test('câu điền khuyết đang chờ chấm có khối AI; câu máy đã chấm xong và câu trắc nghiệm thì không', () => {
  const blank = { questionId: 'q2', type: 'FILL_BLANK', points: 3, needsManualGrading: true, score: null, autoScore: 0 };
  assert.equal(aiSuggestionView({ ...blank, needsManualGrading: false, score: 3 }, undefined, 'SUBMITTED'), null,
    'điền khuyết khớp đáp án đã chốt điểm');
  assert.equal(aiSuggestionView(blank, undefined, 'SUBMITTED').state, 'waiting');
  assert.equal(aiSuggestionView(blank, { status: 'DONE', suggestedScore: 3, feedback: 'ok' }, 'SUBMITTED').title,
    'AI gợi ý: 3 / 3 điểm');
  assert.equal(aiSuggestionView(blank, undefined, 'GRADED'), null, 'lượt cũ đã có điểm cuối');
  assert.equal(aiSuggestionView({ ...blank, type: 'SINGLE_CHOICE' }, undefined, 'SUBMITTED'), null);
});
