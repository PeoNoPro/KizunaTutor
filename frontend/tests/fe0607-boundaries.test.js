// FE-06 / FE-07: kiểm tra giá trị biên của các quy tắc phía trình duyệt (khớp requirement và máy chủ).
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  validateClassroomTitle,
  validateCourseFields,
  validateMaterialFile,
  validateMaterialFiles,
  validateMaterialFields,
  validateScheduleSlot,
  validateScheduleList,
  startTimeOptions,
  endTimeOptions,
  MAX_MATERIAL_SIZE_BYTES,
  MAX_FILES_PER_UPLOAD,
  MAX_TOTAL_SESSIONS,
  SESSION_DURATION_MESSAGE
} from '../js/classroom-helpers.js';
import { validateExerciseSettings } from '../js/exercise-helpers.js';
import { validateImageFile } from '../js/exercise-image.js';
import { validateAudioFile, AUDIO_MAX_BYTES, MAX_PLAYS_OPTIONS } from '../js/exercise-audio.js';

const MB = 1024 * 1024;
const file = (name, size) => ({ name, size });

// ---------------------------------------------------------------- FE-06: tạo lớp

test('FR-6.1.2 tên lớp: bắt buộc, tối đa 150 ký tự (biên 150 / 151)', () => {
  assert.equal(validateClassroomTitle('a'.repeat(150)), null);
  assert.match(validateClassroomTitle('a'.repeat(151)), /150/);
  assert.match(validateClassroomTitle('   '), /nhập tên lớp/);
  assert.equal(validateClassroomTitle('  N5 tối  '), null);
});

test('FR-6.1.2 tổng số buổi 1 – 500 và ngày bắt đầu từ hôm nay', () => {
  const today = '2026-11-20';
  assert.deepEqual(validateCourseFields({ startDate: today, totalSessions: 1, today }), {});
  assert.deepEqual(validateCourseFields({ startDate: today, totalSessions: MAX_TOTAL_SESSIONS, today }), {});
  assert.match(validateCourseFields({ startDate: today, totalSessions: 501, today }).totalSessions, /500/);
  assert.ok(validateCourseFields({ startDate: today, totalSessions: 0, today }).totalSessions);
  assert.ok(validateCourseFields({ startDate: today, totalSessions: '2.5', today }).totalSessions);
  assert.ok(validateCourseFields({ startDate: today, totalSessions: '', today }).totalSessions);
  assert.match(validateCourseFields({ startDate: '2026-11-19', totalSessions: 10, today }).startDate, /hôm nay/);
  assert.ok(validateCourseFields({ startDate: '', totalSessions: 10, today }).startDate);
});

test('FR-6.1.2 lịch hằng tuần: ít nhất 1 buổi, dài 60 – 120 phút (biên 59 / 60 / 120 / 121)', () => {
  const slot = (startTime, endTime, dayOfWeek = 2) => ({ dayOfWeek, startTime, endTime });
  assert.equal(validateScheduleSlot(slot('18:00', '19:00')), null);
  assert.equal(validateScheduleSlot(slot('18:00', '20:00')), null);
  assert.equal(validateScheduleSlot(slot('18:00', '18:59')), SESSION_DURATION_MESSAGE);
  assert.equal(validateScheduleSlot(slot('18:00', '20:01')), SESSION_DURATION_MESSAGE);
  assert.match(validateScheduleSlot(slot('19:00', '18:00')), /sau giờ bắt đầu/);
  assert.match(validateScheduleSlot(slot('18:00', '19:00', 0)), /thứ/);
  assert.match(validateScheduleSlot(slot('18:00', '19:00', 8)), /thứ/);
  assert.match(validateScheduleList([]), /ít nhất một buổi/);
  assert.equal(validateScheduleList([slot('18:00', '19:30'), slot('19:30', '21:00')]), null);
});

test('FR-6.1.2 chọn giờ: định dạng 24 giờ, mọi mốc cách nhau 30 phút', () => {
  const starts = startTimeOptions();
  assert.ok(starts.length > 0);
  for (const t of starts) {
    assert.match(t, /^([01]\d|2[0-3]):[0-5]\d$/);
    assert.equal(Number(t.slice(3)) % 30, 0, t);
  }
  for (const t of endTimeOptions('07:00')) assert.equal(Number(t.slice(3)) % 30, 0, t);
  assert.ok(starts.includes('13:00') && starts.includes('20:30'));
});

// ---------------------------------------------------------------- FE-06: tài liệu

test('FR-6.4.1 tệp tài liệu: PDF/DOC/DOCX/PNG/JPG/MP3, tối đa 25 MB (biên 25 MB / 25 MB + 1 byte)', () => {
  for (const name of ['a.pdf', 'b.DOC', 'c.docx', 'd.png', 'e.jpg', 'f.jpeg', 'g.mp3']) {
    assert.equal(validateMaterialFile(file(name, 1024)), null, name);
  }
  assert.equal(validateMaterialFile(file('a.pdf', MAX_MATERIAL_SIZE_BYTES)), null);
  assert.ok(validateMaterialFile(file('a.pdf', MAX_MATERIAL_SIZE_BYTES + 1)));
  assert.ok(validateMaterialFile(file('a.exe', 1024)));
  assert.ok(validateMaterialFile(file('a.pdf', 0)));
  assert.equal(MAX_MATERIAL_SIZE_BYTES, 25 * MB);
});

test('FR-6.4.1 số tệp mỗi tài liệu 1 – 5, tính cả tệp đang có khi sửa', () => {
  const five = Array.from({ length: 5 }, (_, i) => file(`t${i}.pdf`, 10));
  assert.equal(MAX_FILES_PER_UPLOAD, 5);
  assert.equal(validateMaterialFiles(five, { required: true }), null);
  assert.match(validateMaterialFiles([...five, file('t5.pdf', 10)], { required: true }), /Tối đa 5/);
  assert.match(validateMaterialFiles([file('x.pdf', 10)], { existing: 5 }), /Tối đa 5/);
  assert.match(validateMaterialFiles([], { required: true }), /ít nhất một tệp/);
});

test('FR-6.4.1 tiêu đề tài liệu bắt buộc, tối đa 150 ký tự; mô tả tối đa 1000 ký tự', () => {
  assert.deepEqual(validateMaterialFields({ title: 'a'.repeat(150), description: 'd'.repeat(1000) }), {});
  assert.ok(validateMaterialFields({ title: 'a'.repeat(151) }).title);
  assert.ok(validateMaterialFields({ title: '' }).title);
  assert.ok(validateMaterialFields({ title: 'x', description: 'd'.repeat(1001) }).description);
});

// ---------------------------------------------------------------- FE-07: soạn đề

test('FR-7.2.2 ảnh câu hỏi tối đa 5 MB, file nghe tối đa 25 MB (biên đúng giới hạn / vượt 1 byte)', () => {
  assert.equal(validateImageFile(file('a.png', 5 * MB)), ''); // module ảnh trả '' khi hợp lệ
  assert.ok(validateImageFile(file('a.png', 5 * MB + 1)));
  assert.ok(validateImageFile(file('a.bmp', 10)));
  assert.equal(validateAudioFile(file('a.ogg', AUDIO_MAX_BYTES)), null);
  assert.equal(validateAudioFile(file('a.webm', 10)), null);
  assert.ok(validateAudioFile(file('a.mp3', AUDIO_MAX_BYTES + 1)));
  assert.deepEqual(MAX_PLAYS_OPTIONS, [1, 2, 3, 4, 5]);
});

test('FR-7.2.3 cài đặt bài: thời gian làm bài 1 – 600 phút hoặc không giới hạn, số lượt theo máy chủ (1 – 20)', () => {
  const now = new Date('2026-11-20T10:00:00');
  const base = { title: 'Bài 1', instructions: '', opensLocal: '', closesLocal: '', timeLimit: 30, unlimited: false, maxAttempts: 1 };
  assert.deepEqual(validateExerciseSettings(base, { now }), {});
  assert.deepEqual(validateExerciseSettings({ ...base, timeLimit: 600, maxAttempts: 20 }, { now }), {});
  assert.ok(validateExerciseSettings({ ...base, timeLimit: 0 }, { now }).timeLimitMinutes);
  assert.ok(validateExerciseSettings({ ...base, timeLimit: 601 }, { now }).timeLimitMinutes);
  assert.deepEqual(validateExerciseSettings({ ...base, timeLimit: '', unlimited: true }, { now }), {});
  assert.ok(validateExerciseSettings({ ...base, maxAttempts: 0 }, { now }).maxAttempts);
  assert.ok(validateExerciseSettings({ ...base, maxAttempts: 21 }, { now }).maxAttempts);
  assert.ok(validateExerciseSettings({ ...base, title: 'a'.repeat(151) }, { now }).title);
  // Giờ đóng đề phải sau giờ mở và ở tương lai
  assert.ok(validateExerciseSettings({ ...base, opensLocal: '2026-11-21T10:00', closesLocal: '2026-11-21T09:00' }, { now }).closesAt);
  assert.ok(validateExerciseSettings({ ...base, closesLocal: '2026-11-20T09:00' }, { now }).closesAt);
  assert.deepEqual(validateExerciseSettings({ ...base, closesLocal: '2026-11-20T09:00' }, { now, checkCloseFuture: false }), {});
});
