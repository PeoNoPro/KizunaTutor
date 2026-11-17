import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  countBlanks,
  splitSegments,
  splitList,
  defaultQuestion,
  validateQuestion,
  questionPayload,
  totalPoints,
  previewView,
  isAnswered,
  describeResponse,
  describeCorrect,
  formatCountdown,
  formatScore,
  validateExerciseSettings,
  exerciseStateLabel,
  QUESTION_TYPES
} from '../js/exercise-helpers.js';

test('đủ 8 dạng câu, nhãn trạng thái tiếng Việt', () => {
  assert.equal(QUESTION_TYPES.length, 8);
  assert.equal(exerciseStateLabel('DRAFT'), 'Nháp');
  assert.equal(exerciseStateLabel('NOT_OPEN'), 'Chưa mở');
  assert.equal(exerciseStateLabel('CLOSED'), 'Đã đóng');
});

test('chỗ trống ___ và danh sách phân cách bằng ;', () => {
  assert.equal(countBlanks('私は ___ を ____ 。'), 2);
  assert.deepEqual(splitSegments('A ___ B ___ C'), ['A ', ' B ', ' C']);
  assert.deepEqual(splitList(' たべます ; 食べます;;'), ['たべます', '食べます']);
});

test('kiểm tra câu trước khi giao: báo đúng lỗi từng dạng', () => {
  const single = defaultQuestion('SINGLE_CHOICE');
  single.prompt = 'Đề';
  single.data.options = ['a', 'b'];
  assert.equal(validateQuestion(single), 'chưa chọn đáp án đúng.');
  single.data.correct = 1;
  assert.equal(validateQuestion(single), null);

  const fill = defaultQuestion('FILL_BLANK');
  assert.match(validateQuestion(fill), /___/);
  fill.data.text = 'Tôi ___ cơm';
  assert.equal(validateQuestion(fill), 'chỗ trống 1 chưa có đáp án.');
  fill.data.answers = [['ăn']];
  assert.equal(validateQuestion(fill), null);

  const essay = { ...defaultQuestion('WRITTEN'), prompt: 'Viết' };
  assert.match(validateQuestion(essay, { autoMode: true }), /Gia sư chấm/);
  assert.equal(validateQuestion(essay, { autoMode: false }), null);

  const match = { ...defaultQuestion('MATCHING'), prompt: 'Nối' };
  assert.equal(validateQuestion(match), 'có cặp nối đang để trống.');
  assert.match(validateQuestion({ ...single, points: 0 }), /điểm/);
});

test('dữ liệu gửi máy chủ khớp số chỗ trống, tổng điểm', () => {
  const q = { type: 'WORD_BANK', prompt: '', points: '1,5', data: { text: '___ は ___', answers: ['これ', 'ペン', 'thừa'], distractors: [' あれ ', ''] } };
  const p = questionPayload(q);
  assert.deepEqual(p.data.answers, ['これ', 'ペン']);
  assert.deepEqual(p.data.distractors, ['あれ']);
  assert.equal(p.points, 1.5);
  assert.equal(totalPoints([{ points: 2 }, { points: '1,5' }, { points: '' }]), 3.5);
});

test('xem thử không lộ đáp án và xáo trộn cột phải / thứ tự', () => {
  const v = previewView({ type: 'SINGLE_CHOICE', data: { options: ['a', 'b'], correct: 1 } });
  assert.equal(v.correct, undefined);
  const m = previewView({ type: 'MATCHING', data: { pairs: [{ left: '1', right: 'A' }, { left: '2', right: 'B' }, { left: '3', right: 'C' }] } });
  assert.deepEqual(m.lefts, ['1', '2', '3']);
  assert.notDeepEqual(m.rights, ['A', 'B', 'C']);
  const o = previewView({ type: 'ORDERING', data: { items: ['x', 'y'] } });
  assert.deepEqual(o.items, ['y', 'x']);
});

test('đã trả lời chưa và mô tả câu trả lời / đáp án', () => {
  assert.equal(isAnswered('SINGLE_CHOICE', { choice: 0 }), true);
  assert.equal(isAnswered('FILL_BLANK', { blanks: ['a', ''] }), false);
  assert.equal(isAnswered('WRITTEN', { text: '  ' }), false);
  assert.equal(isAnswered('TRUE_FALSE', null), false);
  const view = { options: ['ほん', 'き'] };
  assert.equal(describeResponse('SINGLE_CHOICE', view, { choice: 1 }), 'き');
  assert.equal(describeResponse('MULTI_CHOICE', view, {}), 'Chưa trả lời');
  assert.equal(describeCorrect('SINGLE_CHOICE', view, { correct: 0 }), 'ほん');
  assert.equal(describeCorrect('FILL_BLANK', {}, { answers: [['たべます', '食べます']] }), '(1) たべます / 食べます');
  assert.equal(describeCorrect('MATCHING', {}, { pairs: [{ left: '犬', right: 'dog' }] }), '犬 → dog');
});

test('đồng hồ đếm ngược và định dạng điểm', () => {
  assert.equal(formatCountdown(65), '01:05');
  assert.equal(formatCountdown(3725), '1:02:05');
  assert.equal(formatCountdown(-3), '00:00');
  assert.equal(formatScore('7.50'), '7,5');
  assert.equal(formatScore(null), '');
});

test('cài đặt bài: đóng đề sau mở đề, thời gian làm bài, số lượt', () => {
  const now = new Date('2026-11-10T10:00:00');
  const ok = { title: 'Bài', opensLocal: '2026-11-11T08:00', closesLocal: '2026-11-12T08:00', unlimited: false, timeLimit: '30', maxAttempts: '2' };
  assert.deepEqual(validateExerciseSettings(ok, { now }), {});
  assert.ok(validateExerciseSettings({ ...ok, closesLocal: '2026-11-11T07:00' }, { now }).closesAt);
  assert.ok(validateExerciseSettings({ ...ok, timeLimit: '0' }, { now }).timeLimitMinutes);
  assert.deepEqual(validateExerciseSettings({ ...ok, unlimited: true, timeLimit: '' }, { now }), {});
  assert.ok(validateExerciseSettings({ ...ok, maxAttempts: '21' }, { now }).maxAttempts);
  assert.ok(validateExerciseSettings({ ...ok, title: '  ' }, { now }).title);
});
