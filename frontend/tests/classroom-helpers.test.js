import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  classroomStatusLabel,
  fileTypeLabel,
  formatDateVi,
  fileExtension,
  validateMaterialFile,
  validateMaterialFields,
  validateClassroomTitle,
  friendlyErrorMessage,
  escapeHtml,
  MAX_MATERIAL_SIZE_BYTES,
  INVALID_FILE_MESSAGE,
  UNSUPPORTED_FORMAT_MESSAGE,
  isMembershipLockedError,
  notifyUnlessMembershipGate
} from '../js/classroom-helpers.js';

test('nhãn trạng thái lớp bằng tiếng Việt, không lộ enum', () => {
  assert.equal(classroomStatusLabel('ACTIVE'), 'Đang hoạt động');
  assert.equal(classroomStatusLabel('PAUSED'), 'Tạm dừng');
  assert.equal(classroomStatusLabel('COMPLETED'), 'Đã hoàn thành');
  assert.equal(classroomStatusLabel('CLOSED'), 'Đã đóng');
  assert.equal(classroomStatusLabel('WHATEVER'), 'Không xác định');
  assert.equal(classroomStatusLabel(undefined), 'Không xác định');
});

test('nhãn loại tệp', () => {
  assert.equal(fileTypeLabel('PDF'), 'Tài liệu PDF');
  assert.equal(fileTypeLabel('MP3'), 'Âm thanh MP3');
  assert.equal(fileTypeLabel(null), 'Tệp tài liệu');
});

test('định dạng ngày vi-VN và an toàn với giá trị rỗng', () => {
  assert.equal(formatDateVi('2026-11-03T10:00:00Z'), '03/11/2026');
  assert.equal(formatDateVi(null), '');
  assert.equal(formatDateVi('không phải ngày'), '');
});

test('đuôi tệp', () => {
  assert.equal(fileExtension('Bai.PDF'), 'pdf');
  assert.equal(fileExtension('a.b.jpeg'), 'jpeg');
  assert.equal(fileExtension('khongduoi'), '');
  assert.equal(fileExtension('cham.'), '');
});

test('kiểm tệp phía trình duyệt', () => {
  assert.equal(validateMaterialFile({ name: 'a.pdf', size: 100 }), null);
  assert.equal(validateMaterialFile({ name: 'a.mp3', size: MAX_MATERIAL_SIZE_BYTES }), null);
  assert.equal(validateMaterialFile({ name: 'a.pdf', size: MAX_MATERIAL_SIZE_BYTES + 1 }), INVALID_FILE_MESSAGE);
  assert.equal(validateMaterialFile({ name: 'a.mp4', size: 100 }), UNSUPPORTED_FORMAT_MESSAGE);
  assert.match(validateMaterialFile({ name: 'a.pdf', size: 0 }), /không có nội dung/);
  assert.equal(validateMaterialFile(null), 'Vui lòng chọn tệp tài liệu.');
  assert.equal(validateMaterialFile(null, { required: false }), null);
});

test('kiểm trường tài liệu', () => {
  assert.deepEqual(validateMaterialFields({ title: 'Bài 1', description: '' }), {});
  assert.ok(validateMaterialFields({ title: '   ' }).title);
  assert.ok(validateMaterialFields({ title: 'a'.repeat(151) }).title);
  assert.equal(validateMaterialFields({ title: 'a'.repeat(150) }).title, undefined);
  assert.ok(validateMaterialFields({ title: 'x', description: 'a'.repeat(1001) }).description);
});

test('kiểm tên lớp', () => {
  assert.equal(validateClassroomTitle('Lớp N4'), null);
  assert.ok(validateClassroomTitle(''));
  assert.ok(validateClassroomTitle('a'.repeat(151)));
});

test('thông điệp lỗi thân thiện, không chứa mã HTTP', () => {
  assert.equal(friendlyErrorMessage({ userMessage: 'Lớp học không còn hoạt động.' }), 'Lớp học không còn hoạt động.');
  for (const status of [401, 403, 404, 500, undefined]) {
    const msg = friendlyErrorMessage({ status });
    assert.ok(msg.length > 0);
    assert.doesNotMatch(msg, /\d{3}|HTTP|error/i);
  }
  assert.ok(friendlyErrorMessage(new TypeError('Failed to fetch')).startsWith('Không thể kết nối'));
});

test('escape HTML chống chèn mã', () => {
  assert.equal(escapeHtml('<img src=x onerror="a">'), '&lt;img src=x onerror=&quot;a&quot;&gt;');
  assert.equal(escapeHtml(null), '');
});

import { decodeJwtPayload, resolveRole, roleDeniedHtml, CLASSROOM_HOME_BY_ROLE } from '../js/classroom-helpers.js';

function fakeJwt(payload) {
  const b64url = (obj) => Buffer.from(JSON.stringify(obj), 'utf8').toString('base64')
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64url({ alg: 'HS256' })}.${b64url(payload)}.chu-ky`;
}

test('đọc vai trò từ token (kể cả tên tiếng Việt), dự phòng bằng kizuna_user', () => {
  const token = fakeJwt({ role: 'ROLE_TUTOR', fullName: 'Yamada Kenji (Gia Sư)' });
  assert.equal(decodeJwtPayload(token).fullName, 'Yamada Kenji (Gia Sư)');
  assert.equal(resolveRole(token, null), 'ROLE_TUTOR');
  assert.equal(resolveRole(fakeJwt({ sub: 'a' }), JSON.stringify({ role: 'ROLE_LEARNER' })), 'ROLE_LEARNER');
  assert.equal(resolveRole('khong-phai-token', '{hỏng'), null);
  assert.equal(resolveRole(null, null), null);
});

test('token ưu tiên hơn kizuna_user khi hai nguồn khác nhau', () => {
  const token = fakeJwt({ role: 'ROLE_LEARNER' });
  assert.equal(resolveRole(token, JSON.stringify({ role: 'ROLE_TUTOR' })), 'ROLE_LEARNER');
});

test('thông báo sai vai trò có nút về đúng trang, không lộ mã vai trò', () => {
  const html = roleDeniedHtml('Quản lý lớp học', 'gia sư', 'ROLE_LEARNER');
  assert.ok(html.includes(CLASSROOM_HOME_BY_ROLE.ROLE_LEARNER));
  assert.ok(html.includes('Lớp học của tôi'));
  assert.doesNotMatch(html, /ROLE_/);
  const adminHtml = roleDeniedHtml('Lớp học của tôi', 'học viên', 'ROLE_ADMIN');
  assert.ok(!adminHtml.includes('/pages/'));
  assert.ok(adminHtml.includes('Đăng nhập bằng tài khoản khác'));
});

import {
  dayOfWeekLabel,
  shortTime,
  durationLabel,
  sortSchedules,
  summarizeSchedules,
  validateScheduleSlot,
  sessionDurationError,
  minutesToTime,
  startTimeOptions,
  endTimeOptions,
  pickEndTime,
  validateScheduleList
} from '../js/classroom-helpers.js';

test('nhãn thứ và giờ của lịch học', () => {
  assert.equal(dayOfWeekLabel(1), 'Thứ Hai');
  assert.equal(dayOfWeekLabel(7), 'Chủ nhật');
  assert.equal(dayOfWeekLabel(9), '');
  assert.equal(shortTime('19:00:00'), '19:00');
  assert.equal(shortTime('7:05'), '07:05');
  assert.equal(durationLabel('19:00', '20:30'), '1 giờ 30 phút');
  assert.equal(durationLabel('19:00', '19:45'), '45 phút');
});

test('sắp xếp và tóm tắt lịch học theo thứ rồi giờ', () => {
  const list = [
    { dayOfWeek: 5, startTime: '18:00', endTime: '19:00' },
    { dayOfWeek: 1, startTime: '19:00', endTime: '20:30' }
  ];
  assert.deepEqual(sortSchedules(list).map((s) => s.dayOfWeek), [1, 5]);
  assert.equal(summarizeSchedules(list), 'T2 19:00–20:30 · T6 18:00–19:00');
  assert.equal(summarizeSchedules([]), '');
});

test('kiểm tra một buổi học khớp quy tắc máy chủ', () => {
  assert.equal(validateScheduleSlot({ dayOfWeek: 1, startTime: '19:00', endTime: '20:30' }), null);
  assert.match(validateScheduleSlot({ dayOfWeek: null, startTime: '19:00', endTime: '20:00' }), /chọn thứ/);
  assert.match(validateScheduleSlot({ dayOfWeek: 2, startTime: '', endTime: '20:00' }), /giờ bắt đầu/);
  assert.match(validateScheduleSlot({ dayOfWeek: 2, startTime: '20:00', endTime: '19:00' }), /sau giờ bắt đầu/);
  assert.match(validateScheduleSlot({ dayOfWeek: 2, startTime: '19:00', endTime: '19:55' }), /60 phút đến 120 phút/);
  assert.match(validateScheduleSlot({ dayOfWeek: 2, startTime: '08:00', endTime: '10:05' }), /60 phút đến 120 phút/);
  assert.equal(validateScheduleSlot({ dayOfWeek: 2, startTime: '08:00', endTime: '09:00' }), null);
  assert.equal(validateScheduleSlot({ dayOfWeek: 2, startTime: '08:00', endTime: '10:00' }), null);
});

test('chọn giờ: 24 giờ, nấc 30 phút, giờ kết thúc cách 60 – 120 phút', () => {
  assert.equal(minutesToTime(450), '07:30');
  const starts = startTimeOptions();
  assert.equal(starts[0], '06:00');
  assert.equal(starts.at(-1), '22:00');
  assert.ok(starts.includes('07:00') && starts.includes('07:30') && !starts.includes('07:15'));
  assert.ok(starts.every((t) => /^\d{2}:(00|30)$/.test(t)));
  // Giờ cũ lệch nấc vẫn được giữ để không bị đổi ngầm
  assert.ok(startTimeOptions('18:45').includes('18:45'));

  assert.deepEqual(endTimeOptions('19:00'), ['20:00', '20:30', '21:00']);
  assert.deepEqual(endTimeOptions('22:00'), ['23:00', '23:30']);
  assert.deepEqual(endTimeOptions(''), []);
  assert.deepEqual(endTimeOptions('18:00', '19:15'), ['19:00', '19:15', '19:30', '20:00']);
  assert.deepEqual(endTimeOptions('18:00', '18:45'), ['19:00', '19:30', '20:00']);

  assert.equal(pickEndTime('19:00'), '20:30');
  assert.equal(pickEndTime('20:00', '18:00', '20:00'), '22:00');
  assert.equal(pickEndTime('22:00'), '23:30');
});

test('thời lượng buổi khi dời / thêm buổi: 60 – 120 phút', () => {
  assert.equal(sessionDurationError('18:00', '19:30'), null);
  assert.equal(sessionDurationError('18:00', '19:00'), null);
  assert.equal(sessionDurationError('18:00', '20:00'), null);
  assert.match(sessionDurationError('18:00', '18:45'), /60 phút đến 120 phút/);
  assert.match(sessionDurationError('18:00', '20:30'), /60 phút đến 120 phút/);
  assert.equal(sessionDurationError('', '20:00'), null);
});

test('danh sách buổi học: bắt buộc có, không trùng giờ', () => {
  assert.match(validateScheduleList([]), /ít nhất một buổi/);
  assert.equal(validateScheduleList([
    { dayOfWeek: 3, startTime: '09:00', endTime: '10:00' },
    { dayOfWeek: 3, startTime: '10:00', endTime: '11:00' }
  ]), null);
  assert.match(validateScheduleList([
    { dayOfWeek: 3, startTime: '09:00', endTime: '10:00' },
    { dayOfWeek: 3, startTime: '09:30', endTime: '11:00' }
  ]), /trùng giờ/);
});

import { validateMaterialFiles, formatFileSize, MAX_FILES_PER_UPLOAD } from '../js/classroom-helpers.js';

test('nhiều tệp: tối đa 5, có DOC/DOCX, báo lỗi kèm tên tệp', () => {
  assert.equal(validateMaterialFiles([], { required: false }), null);
  assert.match(validateMaterialFiles([], { required: true }), /ít nhất một tệp/);
  assert.equal(validateMaterialFiles([{ name: 'a.docx', size: 100 }, { name: 'b.doc', size: 100 }, { name: 'c.pdf', size: 100 }]), null);
  const many = Array.from({ length: MAX_FILES_PER_UPLOAD + 1 }, (_, i) => ({ name: `f${i}.pdf`, size: 10 }));
  assert.match(validateMaterialFiles(many), /Tối đa 5 tệp/);
  assert.match(validateMaterialFiles([{ name: 'a.pdf', size: 10 }], { existing: 5 }), /Tối đa 5 tệp/);
  assert.match(validateMaterialFiles([{ name: 'virus.exe', size: 10 }]), /^Tệp "virus\.exe": /);
  // Báo MỌI tệp lỗi, mỗi tệp một dòng
  const many2 = validateMaterialFiles([{ name: 'ok.pdf', size: 10 }, { name: 'a.exe', size: 10 }, { name: 'to.docx', size: 26 * 1024 * 1024 }]);
  assert.deepEqual(many2.split('\n').map((l) => l.slice(0, 14)), ['Tệp "a.exe": Đ', 'Tệp "to.docx":']);
  assert.match(validateMaterialFiles([{ name: 'to.pdf', size: 26 * 1024 * 1024 }]), /25 MB/);
});

test('định dạng dung lượng tệp', () => {
  assert.equal(formatFileSize(512), '1 KB');
  assert.equal(formatFileSize(350 * 1024), '350 KB');
  assert.equal(formatFileSize(1.5 * 1024 * 1024), '1,5 MB');
});

import { courseCardLines } from '../js/classroom-helpers.js';

test('thẻ lớp: thời gian học theo ngày bắt đầu khóa, lớp cũ dùng ngày mở lớp', () => {
  const c = { courseStartDate: '2026-11-20', courseEndDate: '2027-01-15', totalSessions: 16, startedSessions: 3, createdAt: '2026-11-12T08:00:00Z' };
  assert.deepEqual(courseCardLines(c), { period: '20/11/2026 → dự kiến 15/01/2027', progress: 'Đã học 3/16 buổi' });
  assert.deepEqual(courseCardLines({ createdAt: '2026-11-12T08:00:00Z' }), { period: 'Bắt đầu 12/11/2026', progress: null });
  assert.equal(courseCardLines({ courseStartDate: '2026-11-20', totalSessions: 4 }).progress, 'Đã học 0/4 buổi');
});

const MEMBERSHIP_403 = { status: 403, message: 'Bạn cần đăng ký gói hội viên còn hiệu lực để sử dụng chức năng này.' };
const fakeDoc = (hasGate) => ({ querySelector: (sel) => (hasGate && sel === '[data-membership-gate]' ? {} : null) });
const wait = (ms) => new Promise((r) => setTimeout(r, ms));

test('nhận đúng lỗi 403 "cần gói hội viên"; lỗi khác không tính', () => {
  assert.equal(isMembershipLockedError(MEMBERSHIP_403), true);
  assert.equal(isMembershipLockedError({ status: 403, message: 'Bạn không có quyền truy cập lớp học này.' }), false);
  assert.equal(isMembershipLockedError({ status: 409, message: 'Gói hội viên ...' }), false);
  assert.equal(isMembershipLockedError(null), false);
});

test('lỗi thường hiện thông báo ngay; lỗi gói hội viên chỉ hiện khi trang không có thẻ khóa', async () => {
  let shown = 0;
  notifyUnlessMembershipGate({ status: 500, message: 'Lỗi' }, () => shown++, { doc: fakeDoc(true), waitMs: 5 });
  assert.equal(shown, 1);

  notifyUnlessMembershipGate(MEMBERSHIP_403, () => shown++, { doc: fakeDoc(true), waitMs: 5 });
  await wait(20);
  assert.equal(shown, 1, 'đã có thẻ khóa thì không hiện toast trùng');

  notifyUnlessMembershipGate(MEMBERSHIP_403, () => shown++, { doc: fakeDoc(false), waitMs: 5 });
  await wait(20);
  assert.equal(shown, 2, 'không có thẻ khóa thì vẫn báo cho gia sư');
});
