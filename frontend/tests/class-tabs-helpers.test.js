import test from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeTab,
  groupByWeek,
  weekStart,
  tutorStatus,
  learnerStatus,
  countStatuses,
  sortByDeadline,
  timeLeftLabel,
  fileKind,
  filterMaterials,
  countMaterialKinds,
  matchesKeyword,
  isRecent,
  nextSessionOf
} from '../js/class-tabs-helpers.js';

// Thứ Năm 19/11/2026 10:00 giờ máy
const NOW = new Date(2026, 10, 19, 10, 0, 0);

test('tab hợp lệ, mặc định Tổng quan', () => {
  assert.equal(normalizeTab('bai-tap'), 'bai-tap');
  assert.equal(normalizeTab('xyz'), 'tong-quan');
  assert.equal(normalizeTab(null), 'tong-quan');
});

test('tuần bắt đầu từ Thứ Hai', () => {
  const ws = weekStart(NOW);
  assert.equal(ws.getDay(), 1);
  assert.equal(ws.getDate(), 16);
});

test('nhóm theo tuần: tuần này, tuần trước, khoảng ngày, rồi theo tháng; mới nhất trước', () => {
  const at = (y, m, d) => new Date(y, m - 1, d, 9).toISOString();
  const items = [
    { id: 'a', at: at(2026, 11, 18) },
    { id: 'b', at: at(2026, 11, 10) },
    { id: 'c', at: at(2026, 11, 3) },
    { id: 'd', at: at(2026, 9, 20) },
    { id: 'e', at: null },
    { id: 'f', at: at(2026, 11, 16) }
  ];
  const groups = groupByWeek(items, (x) => x.at, NOW);
  assert.deepEqual(groups.map((g) => g.key), ['w0', 'w1', 'w2', 'm2026-09', 'none']);
  assert.ok(groups[0].label.startsWith('Tuần này · 16/11 – 22/11'));
  assert.ok(groups[1].label.startsWith('Tuần trước · 09/11 – 15/11'));
  assert.equal(groups[2].label, '02/11 – 08/11');
  assert.equal(groups[3].label, 'Tháng 9/2026');
  assert.deepEqual(groups[0].items.map((x) => x.id), ['a', 'f']);
});

test('trạng thái bài với gia sư', () => {
  assert.equal(tutorStatus({ kind: 'online', e: { status: 'DRAFT' } }), 'draft');
  assert.equal(tutorStatus({ kind: 'online', e: { status: 'PUBLISHED', pendingGrading: true, state: 'OPEN' } }), 'review');
  assert.equal(tutorStatus({ kind: 'online', e: { status: 'PUBLISHED', state: 'CLOSED' } }), 'done');
  assert.equal(tutorStatus({ kind: 'online', e: { status: 'PUBLISHED', state: 'OPEN' } }), 'open');
  assert.equal(tutorStatus({ kind: 'file', a: { submissionState: 'LATE', canGrade: true } }), 'review');
  assert.equal(tutorStatus({ kind: 'file', a: { submissionState: 'ON_TIME', canGrade: false } }), 'open');
  assert.equal(tutorStatus({ kind: 'file', a: { submissionState: 'GRADED' } }), 'done');
  assert.equal(tutorStatus({ kind: 'file', a: { submissionState: 'NOT_SUBMITTED' } }), 'open');
});

test('trạng thái bài với học viên và đếm', () => {
  const items = [
    { kind: 'online', e: { canStart: true, attemptsUsed: 0 } },
    { kind: 'online', e: { inProgressAttemptId: 'x', attemptsUsed: 1 } },
    { kind: 'online', e: { pendingGrading: true, attemptsUsed: 1 } },
    { kind: 'online', e: { bestScore: 8, attemptsUsed: 1, canStart: true } },
    { kind: 'file', a: { submissionState: 'NOT_SUBMITTED' } },
    { kind: 'file', a: { submissionState: 'LATE' } },
    { kind: 'file', a: { submissionState: 'GRADED' } },
    { kind: 'file', a: { submissionState: 'OVERDUE_NOT_SUBMITTED' } }
  ];
  assert.deepEqual(items.map(learnerStatus), ['todo', 'todo', 'waiting', 'graded', 'todo', 'waiting', 'graded', 'other']);
  assert.deepEqual(countStatuses(items, learnerStatus), { all: 8, todo: 3, waiting: 2, graded: 2, other: 1 });
});

test('bài cần làm: hạn gần nhất trước, không hạn ở cuối; nhãn thời gian còn lại', () => {
  const items = [
    { kind: 'file', a: { dueDate: null }, id: 1 },
    { kind: 'online', e: { closesAt: '2026-11-25T10:00:00Z' }, id: 2 },
    { kind: 'file', a: { dueDate: '2026-11-21T10:00:00Z' }, id: 3 }
  ];
  assert.deepEqual(sortByDeadline(items).map((x) => x.id), [3, 2, 1]);
  assert.equal(timeLeftLabel(new Date(NOW.getTime() + 4 * 86400000 + 3600000).toISOString(), NOW), 'còn 4 ngày');
  assert.equal(timeLeftLabel(new Date(NOW.getTime() + 5 * 3600000).toISOString(), NOW), 'còn 5 giờ');
  assert.equal(timeLeftLabel(new Date(NOW.getTime() - 1000).toISOString(), NOW), '');
  assert.equal(timeLeftLabel(null, NOW), '');
});

test('tài liệu: loại tệp, lọc theo loại và từ khóa không dấu, đếm', () => {
  assert.equal(fileKind({ fileType: 'PDF' }), 'pdf');
  assert.equal(fileKind({ fileType: 'DOCX' }), 'word');
  assert.equal(fileKind({ fileName: 'bang-kanji.PNG' }), 'image');
  assert.equal(fileKind({ fileType: 'MP3' }), 'audio');
  const mats = [
    { title: 'Ngữ pháp tuần 12', files: [{ fileType: 'PDF', fileName: 'np.pdf' }, { fileType: 'DOCX', fileName: 'bt.docx' }] },
    { title: 'Nghe hiểu bài 11', files: [{ fileType: 'MP3', fileName: 'nghe.mp3' }] }
  ];
  assert.equal(filterMaterials(mats, { kind: 'word' }).length, 1);
  assert.equal(filterMaterials(mats, { keyword: 'ngu phap' })[0].title, 'Ngữ pháp tuần 12');
  assert.equal(filterMaterials(mats, { keyword: 'nghe.mp3' }).length, 1);
  assert.deepEqual(countMaterialKinds(mats), { all: 2, pdf: 1, word: 1, audio: 1 });
  assert.ok(matchesKeyword('Kanji N4 – Bài 3', 'bai 3'));
});

test('tài liệu mới trong 7 ngày; buổi tới gần nhất của đúng lớp', () => {
  assert.equal(isRecent(new Date(NOW.getTime() - 2 * 86400000).toISOString(), NOW), true);
  assert.equal(isRecent(new Date(NOW.getTime() - 9 * 86400000).toISOString(), NOW), false);
  const sessions = [
    { spaceId: 'A', date: '2026-11-19', startTime: '08:00', endTime: '09:00' },
    { spaceId: 'B', date: '2026-11-19', startTime: '19:00', endTime: '20:30' },
    { spaceId: 'A', date: '2026-11-24', startTime: '19:00', endTime: '20:30', number: 13 },
    { spaceId: 'A', date: '2026-11-20', startTime: '19:00', endTime: '20:30', number: 12 }
  ];
  assert.equal(nextSessionOf(sessions, 'A', NOW).number, 12);
  assert.equal(nextSessionOf(sessions, 'C', NOW), null);
});
