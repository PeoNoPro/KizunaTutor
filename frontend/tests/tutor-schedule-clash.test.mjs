import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isSlotClashing } from '../js/tutor-schedule-helpers.js';

test('isSlotClashing phát hiện trùng lịch học cố định của lớp (classSchedules)', () => {
  const tutor = {
    classSchedules: [
      { dayOfWeek: 6, startTime: '18:00', endTime: '19:00' } // Thứ 6: 18:00 - 19:00
    ],
    bookedSlots: []
  };

  // Ca 18:00 - 18:30 (1080 -> 1110) trùng với 18:00 - 19:00
  assert.equal(isSlotClashing(tutor, 6, '13/11/2026', 1080, 30), true);

  // Ca 18:30 - 19:00 (1110 -> 1140) trùng với 18:00 - 19:00
  assert.equal(isSlotClashing(tutor, 6, '13/11/2026', 1110, 30), true);

  // Ca 19:00 - 19:30 (1140 -> 1170) không trùng
  assert.equal(isSlotClashing(tutor, 6, '13/11/2026', 1140, 30), false);

  // Ca 19:30 - 20:00 (1170 -> 1200) không trùng
  assert.equal(isSlotClashing(tutor, 6, '13/11/2026', 1170, 30), false);

  // Thứ khác (ví dụ Thứ 7: dayOfWeek = 7) không bị ảnh hưởng
  assert.equal(isSlotClashing(tutor, 7, '14/11/2026', 1080, 30), false);
});

test('isSlotClashing phát hiện trùng buổi học thử đã được đặt trước theo ngày cụ thể (bookedSlots)', () => {
  const tutor = {
    classSchedules: [],
    bookedSlots: [
      { date: '13/11/2026', startTime: '19:00', endTime: '19:30' }
    ]
  };

  // Ngày 13/11/2026 lúc 19:00 bị trùng
  assert.equal(isSlotClashing(tutor, 6, '13/11/2026', 1140, 30), true);

  // Cùng thứ 6 nhưng ngày 20/11/2026 không có lịch đặt nên rảnh
  assert.equal(isSlotClashing(tutor, 6, '20/11/2026', 1140, 30), false);
});

test('isSlotClashing an toàn khi gia sư chưa có lịch học hay ca bận nào', () => {
  const tutor = {
    classSchedules: [],
    bookedSlots: []
  };

  assert.equal(isSlotClashing(tutor, 6, '13/11/2026', 1080, 30), false);
  assert.equal(isSlotClashing(null, 6, '13/11/2026', 1080, 30), false);
});

