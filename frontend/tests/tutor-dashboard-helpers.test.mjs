import { test } from 'node:test';
import assert from 'node:assert/strict';
import { todayLessons, lessonState, listOf, ymd } from '../js/tutor-dashboard-helpers.js';

// 17/11/2026 10:00 giờ máy (lịch so theo ngày giờ máy, giống trình duyệt)
const now = new Date(2026, 10, 17, 10, 0);
const at = (h, m) => new Date(2026, 10, 17, h, m).toISOString();

test('chỉ lấy buổi của lớp trong ngày hôm nay, sắp theo giờ bắt đầu', () => {
  const sessions = [
    { date: '2026-11-17', startTime: '14:00', endTime: '15:30', classTitle: 'N3', partnerName: 'An', number: 2, totalSessions: 8, spaceId: 's1' },
    { date: '2026-11-17', startTime: '08:00', endTime: '09:00', classTitle: 'N4', partnerName: 'Bình', extra: true },
    { date: '2026-11-18', startTime: '07:00', endTime: '08:00', classTitle: 'Ngày mai' }
  ];
  const lessons = todayLessons(sessions, [], now);
  assert.deepEqual(lessons.map((l) => l.title), ['N4', 'N3']);
  assert.equal(lessons[0].note, 'Buổi học thêm');
  assert.equal(lessons[1].note, 'Buổi 2/8');
  assert.equal(lessons[1].href, '/tutor/classrooms?space=s1');
});

test('buổi học thử: chỉ tính buổi đã xác nhận trong ngày, giờ kết thúc theo thời lượng khi thiếu', () => {
  const trials = [
    { bookingStatus: 'CONFIRMED', scheduledStartTime: at(9, 0), durationMinutes: 50, partnerName: 'Chi', learningMode: 'ONLINE' },
    { bookingStatus: 'PENDING', scheduledStartTime: at(11, 0) },
    { bookingStatus: 'CANCELLED', scheduledStartTime: at(12, 0) },
    { bookingStatus: 'RESCHEDULED', scheduledStartTime: new Date(2026, 10, 18, 9, 0).toISOString() }
  ];
  const lessons = todayLessons([], trials, now);
  assert.equal(lessons.length, 1);
  assert.deepEqual([lessons[0].start, lessons[0].end, lessons[0].kind, lessons[0].note], ['09:00', '09:50', 'Học thử', 'Trực tuyến']);
});

test('gộp lớp học và học thử theo thứ tự giờ', () => {
  const lessons = todayLessons(
    [{ date: ymd(now), startTime: '13:00', endTime: '14:00', classTitle: 'N2' }],
    [{ bookingStatus: 'COMPLETED', scheduledStartTime: at(7, 30), scheduledEndTime: at(8, 20) }],
    now);
  assert.deepEqual(lessons.map((l) => l.start), ['07:30', '13:00']);
});

test('trạng thái buổi so với giờ hiện tại', () => {
  assert.equal(lessonState({ start: '08:00', end: '09:00' }, '10:00').label, 'Đã kết thúc');
  assert.equal(lessonState({ start: '09:30', end: '10:30' }, '10:00').label, 'Đang diễn ra');
  assert.equal(lessonState({ start: '10:00', end: '11:00' }, '10:00').label, 'Đang diễn ra');
  assert.equal(lessonState({ start: '14:00', end: '15:00' }, '10:00').label, 'Sắp diễn ra');
});

test('đọc danh sách từ các dạng phản hồi API', () => {
  assert.deepEqual(listOf([1]), [1]);
  assert.deepEqual(listOf({ items: [2] }), [2]);
  assert.deepEqual(listOf({ data: { content: [3] } }), [3]);
  assert.deepEqual(listOf(null), []);
});
