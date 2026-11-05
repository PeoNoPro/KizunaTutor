import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  submissionStateLabel,
  formatDueDate,
  formatPoints,
  toDateTimeLocalValue,
  fromDateTimeLocalValue,
  validateAssignmentForm,
  validateSubmission,
  validateGrade,
  parsePoints
} from '../js/assignment-helpers.js';

test('nhãn trạng thái nộp bài tiếng Việt, không lộ mã', () => {
  assert.equal(submissionStateLabel('NOT_SUBMITTED'), 'Chưa nộp');
  assert.equal(submissionStateLabel('ON_TIME'), 'Đúng hạn');
  assert.equal(submissionStateLabel('LATE'), 'Nộp muộn');
  assert.equal(submissionStateLabel('OVERDUE_NOT_SUBMITTED'), 'Quá hạn – chưa nộp');
  assert.equal(submissionStateLabel('GRADED'), 'Đã chấm');
  assert.equal(submissionStateLabel('XYZ'), 'Không xác định');
});

test('định dạng hạn nộp và điểm', () => {
  assert.equal(formatDueDate(null), 'Không có hạn nộp');
  assert.match(formatDueDate('2026-11-10T16:59:00Z'), /10\/11\/2026/);
  assert.equal(formatPoints(85.5), '85,5');
  assert.equal(formatPoints(null), '');
});

test('chuyển đổi datetime-local hai chiều', () => {
  const local = '2026-11-10T23:59';
  const iso = fromDateTimeLocalValue(local);
  assert.ok(iso.endsWith('Z'));
  assert.equal(toDateTimeLocalValue(iso), local);
  assert.equal(fromDateTimeLocalValue(''), null);
  assert.equal(toDateTimeLocalValue(null), '');
});

test('kiểm form bài tập', () => {
  const now = new Date('2026-11-10T10:00:00');
  assert.deepEqual(validateAssignmentForm({ title: 'Bài 1', dueLocal: '' }, { now }), {});
  assert.ok(validateAssignmentForm({ title: '  ' }, { now }).title);
  assert.ok(validateAssignmentForm({ title: 'a'.repeat(151) }, { now }).title);
  assert.ok(validateAssignmentForm({ title: 'B', description: 'a'.repeat(5001) }, { now }).description);
  assert.ok(validateAssignmentForm({ title: 'B', dueLocal: '2026-11-09T10:00' }, { now }).dueDate);
  assert.equal(validateAssignmentForm({ title: 'B', dueLocal: '2026-11-11T10:00' }, { now }).dueDate, undefined);
  assert.ok(validateAssignmentForm({ title: 'B', totalPoints: '0' }, { now }).totalPoints);
  assert.ok(validateAssignmentForm({ title: 'B', totalPoints: '1000' }, { now }).totalPoints);
  assert.equal(validateAssignmentForm({ title: 'B', totalPoints: '10,5' }, { now }).totalPoints, undefined);
});

test('sửa bài tập giữ nguyên hạn đã qua thì không báo lỗi', () => {
  const now = new Date('2026-11-10T10:00:00');
  const pastIso = new Date('2026-11-09T10:00:00').toISOString();
  const dueLocal = toDateTimeLocalValue(pastIso);
  assert.equal(validateAssignmentForm({ title: 'B', dueLocal }, { now, dueUnchangedIso: pastIso }).dueDate, undefined);
  assert.ok(validateAssignmentForm({ title: 'B', dueLocal: '2026-11-08T10:00' }, { now, dueUnchangedIso: pastIso }).dueDate);
});

test('kiểm bài nộp', () => {
  assert.ok(validateSubmission({ content: ' ', file: null }));
  assert.equal(validateSubmission({ content: 'Đáp án', file: null }), null);
  assert.equal(validateSubmission({ content: '', file: { name: 'a.pdf', size: 100 } }), null);
  assert.ok(validateSubmission({ content: '', file: { name: 'a.exe', size: 100 } }));
  assert.ok(validateSubmission({ content: 'a'.repeat(5001), file: null }));
});

test('kiểm điểm', () => {
  assert.equal(validateGrade('85,5', 100), null);
  assert.ok(validateGrade('', 100));
  assert.ok(validateGrade('-1', 100));
  assert.ok(validateGrade('101', 100));
  assert.equal(parsePoints('8,5'), 8.5);
  assert.equal(parsePoints(''), null);
});
