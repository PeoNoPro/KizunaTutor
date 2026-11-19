import test from 'node:test';
import assert from 'node:assert/strict';
import {
  fileAssignmentRow,
  exerciseRow,
  countBuckets,
  filterRows,
  sortRows,
  nextLessonDeadline,
  quickDeadline,
  ymdLocal
} from '../js/assignment-overview-helpers.js';

const cls = { spaceId: 's1', title: 'Lớp N4', learnerName: 'Trần Minh Anh' };
const file = (over) => ({ assignmentId: 'a1', title: 'Ngữ pháp', dueDate: '2026-11-20T10:00:00Z', totalPoints: 10, opened: true, ...over });
const ex = (over) => ({ exerciseId: 'e1', title: 'Kanji 3', status: 'PUBLISHED', state: 'OPEN', questionCount: 20, maxScore: 40,
  attemptsUsed: 0, maxAttempts: 2, pendingGrading: false, bestScore: null, inProgressAttemptId: null, closesAt: null, ...over });

test('bài nộp file: cần chấm / chờ / quá hạn / đã chấm', () => {
  const review = fileAssignmentRow(cls, file({ submissionState: 'LATE', canGrade: true }));
  assert.equal(review.bucket, 'review');
  assert.equal(review.late, true);
  assert.match(review.action.href, /grade=a1/);
  assert.equal(fileAssignmentRow(cls, file({ submissionState: 'ON_TIME', canGrade: false })).bucket, 'waiting');
  assert.match(fileAssignmentRow(cls, file({ submissionState: 'ON_TIME', canGrade: false })).need, /chấm được sau hạn/);
  assert.equal(fileAssignmentRow(cls, file({ submissionState: 'NOT_SUBMITTED' })).bucket, 'waiting');
  assert.equal(fileAssignmentRow(cls, file({ submissionState: 'OVERDUE_NOT_SUBMITTED' })).bucket, 'overdue');
  const graded = fileAssignmentRow(cls, file({ submissionState: 'GRADED', grade: 8.5 }));
  assert.equal(graded.bucket, 'graded');
  assert.match(graded.need, /8,5 \/ 10/);
});

test('bài làm trực tiếp: nháp bỏ qua, chờ chấm, đang làm, đóng đề chưa làm, đã chấm', () => {
  assert.equal(exerciseRow(cls, ex({ status: 'DRAFT' })), null);
  const review = exerciseRow(cls, ex({ pendingGrading: true, attemptsUsed: 1 }));
  assert.equal(review.bucket, 'review');
  assert.match(review.action.href, /tab=bai-lam/);
  assert.equal(exerciseRow(cls, ex({ inProgressAttemptId: 'x', attemptsUsed: 1 })).bucket, 'waiting');
  assert.equal(exerciseRow(cls, ex({ state: 'CLOSED' })).bucket, 'overdue');
  assert.equal(exerciseRow(cls, ex({ state: 'NOT_OPEN' })).need, 'Chưa tới giờ mở đề');
  const graded = exerciseRow(cls, ex({ attemptsUsed: 1, bestScore: 32 }));
  assert.equal(graded.bucket, 'graded');
  assert.match(graded.need, /32 \/ 40 · 1\/2 lượt/);
});

test('đếm, lọc (bỏ dấu tiếng Việt), sắp xếp theo hạn', () => {
  const rows = [
    fileAssignmentRow(cls, file({ submissionState: 'LATE', canGrade: true, dueDate: '2026-11-21T00:00:00Z' })),
    exerciseRow(cls, ex({ pendingGrading: true, closesAt: '2026-11-19T00:00:00Z' })),
    exerciseRow({ ...cls, spaceId: 's2', title: 'Lớp N2', learnerName: 'Phạm Tôn Hải' }, ex({ exerciseId: 'e2', title: 'Đọc hiểu' }))
  ];
  assert.deepEqual(countBuckets(rows), { review: 2, waiting: 1, overdue: 0, graded: 0 });
  assert.equal(filterRows(rows, { bucket: 'review' }).length, 2);
  assert.equal(filterRows(rows, { kind: 'file' }).length, 1);
  assert.equal(filterRows(rows, { spaceId: 's2' }).length, 1);
  assert.equal(filterRows(rows, { keyword: 'doc hieu' }).length, 1);
  assert.equal(filterRows(rows, { keyword: 'TON HAI' }).length, 1);
  assert.deepEqual(sortRows(filterRows(rows, { bucket: 'review' }), 'review').map((r) => r.kind), ['online', 'file']);
});

test('hạn "trước buổi học tới" và hạn nhanh', () => {
  const now = new Date('2026-11-18T09:00:00');
  const sessions = [
    { spaceId: 's1', date: '2026-11-18', startTime: '08:00' },
    { spaceId: 's1', date: '2026-11-23', startTime: '08:00' },
    { spaceId: 's1', date: '2026-11-20', startTime: '19:30' },
    { spaceId: 's2', date: '2026-11-19', startTime: '07:00' }
  ];
  const due = nextLessonDeadline(sessions, 's1', now);
  assert.equal(due.getDate(), 20);
  assert.equal(due.getHours(), 19);
  assert.equal(due.getMinutes(), 29);
  assert.equal(nextLessonDeadline(sessions, 's3', now), null);
  const plus3 = quickDeadline(3, now);
  assert.equal(plus3.getDate(), 21);
  assert.equal(plus3.getHours(), 23);
  assert.equal(plus3.getMinutes(), 59);
  assert.equal(ymdLocal(now), '2026-11-18');
});
