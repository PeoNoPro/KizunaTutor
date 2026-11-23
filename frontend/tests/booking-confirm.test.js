import test from 'node:test';
import assert from 'node:assert/strict';
import { parseBookingParams, buildBookingQuery } from '../js/booking-form.js';

const NOW = new Date('2026-11-18T10:00:00+07:00');

test('học thử: đọc giờ bắt đầu và đề xuất ngoài lịch', () => {
  const p = parseBookingParams('?type=trial&tutorId=t1&start=2026-11-20T18:30&propose=1', NOW);
  assert.deepEqual(p, {
    type: 'trial', tutorId: 't1', start: '2026-11-20T18:30',
    scheduledStartTime: '2026-11-20T18:30:00+07:00', offSchedule: true
  });
});

test('học thử: từ chối giờ đã qua, sai định dạng hoặc thiếu gia sư', () => {
  assert.equal(parseBookingParams('?type=trial&tutorId=t1&start=2026-11-18T09:00', NOW), null);
  assert.equal(parseBookingParams('?type=trial&tutorId=t1&start=abc', NOW), null);
  assert.equal(parseBookingParams('?type=trial&start=2026-11-20T18:30', NOW), null);
});

test('kết nối: chỉ đọc thứ + giờ bắt đầu (độ dài ca do gia sư chọn)', () => {
  const p = parseBookingParams('?type=connect&tutorId=t1&slots=2-18:00,1-09:30', NOW);
  assert.deepEqual(p.slots, [
    { dayOfWeek: 2, startTime: '18:00' },
    { dayOfWeek: 1, startTime: '09:30' }
  ]);
});

test('kết nối: từ chối ca sai định dạng, còn kèm độ dài kiểu cũ, hoặc không có ca', () => {
  assert.equal(parseBookingParams('?type=connect&tutorId=t1&slots=8-18:00', NOW), null);
  assert.equal(parseBookingParams('?type=connect&tutorId=t1&slots=2-25:00', NOW), null);
  assert.equal(parseBookingParams('?type=connect&tutorId=t1&slots=2-18:00-90', NOW), null);
  assert.equal(parseBookingParams('?type=connect&tutorId=t1&slots=', NOW), null);
  assert.equal(parseBookingParams('?type=other&tutorId=t1', NOW), null);
});

test('buildBookingQuery và parseBookingParams khớp nhau (popup → URL → trang xác nhận)', () => {
  const trial = { type: 'trial', tutorId: 't1', start: '2026-11-20T18:30', offSchedule: false };
  const p1 = parseBookingParams('?' + buildBookingQuery(trial), NOW);
  assert.equal(p1.start, trial.start);
  assert.equal(p1.offSchedule, false);

  const connect = { type: 'connect', tutorId: 't1', slots: [{ dayOfWeek: 1, startTime: '09:30' }] };
  const p2 = parseBookingParams('?' + buildBookingQuery(connect), NOW);
  assert.deepEqual(p2.slots, connect.slots);
});
