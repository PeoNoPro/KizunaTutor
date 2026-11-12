import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  periodOf,
  officialEvent,
  trialEvent,
  dayDateLabel,
  weekRange,
  PERIODS
} from '../js/week-time-grid.js';
import { validateCourseFields, courseSummaryText } from '../js/classroom-helpers.js';

test('chỉ có 2 ca Sáng / Tối, xếp theo giờ bắt đầu (mốc 17:00)', () => {
  assert.deepEqual(PERIODS.map((p) => p.label), ['Sáng', 'Tối']);
  assert.equal(periodOf(0), 'morning');
  assert.equal(periodOf(6 * 60), 'morning');
  assert.equal(periodOf(16 * 60), 'morning'); // 16:00–19:00 vẫn ở ca Sáng
  assert.equal(periodOf(16 * 60 + 59), 'morning');
  assert.equal(periodOf(17 * 60), 'evening');
  assert.equal(periodOf(23 * 60 + 30), 'evening');
});

test('khoảng ngày của tuần để tải buổi học', () => {
  assert.deepEqual(weekRange(new Date(2026, 10, 16)), { from: '2026-11-16', to: '2026-11-22' });
  assert.equal(dayDateLabel('2026-11-22'), 'Chủ nhật 22/11');
});

const base = {
  spaceId: 'lop-1', classTitle: 'Lớp N4', partnerName: 'Yamada Kenji', scheduleId: 's-thu', extraId: null,
  originalDate: '2026-11-19', date: '2026-11-19', startTime: '06:00', endTime: '07:00',
  moved: false, extra: false, number: 3, totalSessions: 24
};

test('thẻ buổi học ghi số buổi của khóa', () => {
  const ev = officialEvent(base, '/lop');
  assert.equal(ev.dateYmd, '2026-11-19');
  assert.equal(ev.label, 'Lớp chính thức');
  assert.equal(ev.note, 'Buổi 3/24');
  assert.equal(ev.href, '/lop');
  assert.equal(officialEvent({ ...base, totalSessions: null }).note, 'Buổi 3');
});

test('buổi đã dời ghi rõ ngày gốc; buổi thêm có nhãn riêng; thẻ bấm được không có đường dẫn', () => {
  const moved = officialEvent({ ...base, moved: true, date: '2026-11-21', startTime: '18:00', endTime: '19:00' }, '/lop', { onClick: true });
  assert.equal(moved.dateYmd, '2026-11-21');
  assert.equal(moved.label, 'Lớp chính thức · đã dời');
  assert.equal(moved.note, 'Buổi 3/24 · Đã dời từ Thứ Năm 19/11');
  assert.equal(moved.href, null);
  assert.equal(moved.clickable, true);
  const extra = officialEvent({ ...base, extra: true, scheduleId: null, extraId: 'x1' });
  assert.equal(extra.label, 'Buổi học thêm');
});

test('thẻ học thử cùng cấu trúc với thẻ học chính thức, khác loại (màu)', () => {
  const t = trialEvent({ scheduledStartTime: new Date(2026, 10, 22, 10, 30).toISOString(), durationMinutes: 50,
    bookingStatus: 'CONFIRMED', partnerName: 'Yamada Kenji' });
  const off = officialEvent(base);
  assert.equal(t.kind, 'trial');
  assert.equal(off.kind, 'official');
  for (const key of ['dateYmd', 'start', 'end', 'name', 'label']) {
    assert.ok(key in t && key in off, key);
  }
  assert.equal(t.end - t.start, 50);
});

test('kiểm tra ngày bắt đầu và tổng số buổi của khóa', () => {
  const today = '2026-11-18';
  assert.deepEqual(validateCourseFields({ startDate: '2026-11-18', totalSessions: '24', today }), {});
  assert.match(validateCourseFields({ startDate: '', totalSessions: '24', today }).startDate, /ngày bắt đầu/);
  assert.match(validateCourseFields({ startDate: '2026-11-17', totalSessions: '24', today }).startDate, /từ hôm nay/);
  assert.match(validateCourseFields({ startDate: today, totalSessions: '', today }).totalSessions, /Vui lòng nhập/);
  assert.equal(validateCourseFields({ startDate: today, totalSessions: '0', today }).totalSessions, 'Khóa học cần có ít nhất 1 buổi.');
  assert.equal(validateCourseFields({ startDate: today, totalSessions: '2.5', today }).totalSessions, 'Số buổi phải là số tròn, ví dụ 24.');
  assert.equal(validateCourseFields({ startDate: today, totalSessions: '501', today }).totalSessions, 'Mỗi khóa học có tối đa 500 buổi.');
  // Lịch 2 buổi mỗi tuần, chưa học buổi nào: tối thiểu 2 buổi
  assert.equal(validateCourseFields({ startDate: today, totalSessions: '1', weeklySlots: 2, today }).totalSessions,
    'Lịch học có 2 buổi mỗi tuần nên tổng số buổi cần từ 2 buổi trở lên.');
  assert.deepEqual(validateCourseFields({ startDate: today, totalSessions: '2', weeklySlots: 2, today }), {});
  // Đã học 45 buổi: chỉ được sửa thành 45 trở lên; không đổi ngày bắt đầu thì không kiểm tra ngày
  assert.equal(validateCourseFields({ totalSessions: '44', minTotal: 45, weeklySlots: 2, checkStartDate: false, today }).totalSessions,
    'Khóa học đã học 45 buổi nên tổng số buổi cần từ 45 buổi trở lên.');
  assert.deepEqual(validateCourseFields({ totalSessions: '45', minTotal: 45, weeklySlots: 2, checkStartDate: false, today }), {});
});

test('tóm tắt tiến độ khóa học', () => {
  assert.equal(courseSummaryText({ totalSessions: 24, startedSessions: 5, courseStartDate: '2026-11-02', courseEndDate: '2027-02-18' }),
    'Đã học 5/24 buổi · bắt đầu 02/11/2026 · dự kiến kết thúc 18/02/2027');
  assert.match(courseSummaryText({ totalSessions: null }), /lặp lại hằng tuần/);
});
