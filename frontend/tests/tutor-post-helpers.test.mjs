import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  validatePostDraft, validatePrices, priceRowsFromContent, submitLabel, isUpdateMode
} from '../js/tutor-post-helpers.js';

const valid = {
  title: 'Luyện thi JLPT N3 cấp tốc',
  description: 'Giới thiệu '.repeat(10),
  teachingFormat: 'ONLINE',
  prices: [
    { level: 'JLPT_N3', sessionMinutes: 120, pricePerSession: 480000 },
    { level: 'KAIWA', sessionMinutes: 60, pricePerSession: 180000 }
  ]
};

test('bản nháp hợp lệ không có lỗi', () => {
  assert.deepEqual(validatePostDraft(valid), []);
});

test('báo lỗi tiếng Việt cho tiêu đề, giới thiệu, hình thức và học phí', () => {
  const errors = validatePostDraft({ title: 'Ngắn', description: 'Ngắn', teachingFormat: '',
    prices: [{ level: 'JLPT_N3', sessionMinutes: 90, pricePerSession: 5000 }] });
  assert.ok(errors.includes('Tiêu đề tối thiểu 10 ký tự'));
  assert.ok(errors.includes('Giới thiệu tối thiểu 50 ký tự'));
  assert.ok(errors.includes('Chọn hình thức dạy'));
  assert.ok(errors.includes('Học phí mỗi buổi từ 10.000 đ đến 10.000.000 đ'));
  errors.forEach((e) => assert.notEqual(e, e.toUpperCase(), 'không viết in hoa toàn bộ'));
});

test('giới hạn trên khớp backend', () => {
  assert.ok(validatePostDraft({ ...valid, title: 'x'.repeat(151) }).includes('Tiêu đề tối đa 150 ký tự'));
  assert.ok(validatePostDraft({ ...valid, description: 'x'.repeat(3001) }).includes('Giới thiệu tối đa 3.000 ký tự'));
  assert.deepEqual(validatePrices([{ level: 'JLPT_N1', sessionMinutes: 90, pricePerSession: 10000 }]), []);
  assert.equal(validatePrices([{ level: 'JLPT_N1', sessionMinutes: 90, pricePerSession: 10000001 }]).length, 1);
});

test('bảng học phí: bắt buộc, chọn môn cho từng dòng, không trùng môn', () => {
  assert.deepEqual(validatePrices([]), ['Thêm ít nhất một mức học phí theo môn học hoặc cấp độ']);
  assert.ok(validatePrices([{ level: '', sessionMinutes: 90, pricePerSession: 200000 }])
    .includes('Chọn môn học hoặc cấp độ cho từng mức học phí'));
  assert.ok(validatePrices([
    { level: 'JLPT_N3', sessionMinutes: 90, pricePerSession: 200000 },
    { level: 'JLPT_N3', sessionMinutes: 90, pricePerSession: 250000 }
  ]).includes('Mỗi môn học hoặc cấp độ chỉ được đặt một mức học phí'));
  assert.ok(validatePrices([{ level: 'KAIWA', sessionMinutes: 60, pricePerSession: '' }])
    .includes('Học phí mỗi buổi từ 10.000 đ đến 10.000.000 đ'));
});

test('thời lượng mỗi buổi chỉ nhận 60 / 90 / 120 phút', () => {
  const msg = 'Chọn thời lượng mỗi buổi (60, 90 hoặc 120 phút) cho từng môn học';
  assert.ok(validatePrices([{ level: 'KAIWA', sessionMinutes: '', pricePerSession: 180000 }]).includes(msg));
  assert.ok(validatePrices([{ level: 'KAIWA', sessionMinutes: 100, pricePerSession: 180000 }]).includes(msg));
  assert.deepEqual(validatePrices([{ level: 'KAIWA', sessionMinutes: '60', pricePerSession: '180000' }]), []);
});

test('điền form: dùng bảng giá nếu có, dữ liệu cũ quy ra buổi 90 phút, bài mới một dòng trống', () => {
  assert.deepEqual(priceRowsFromContent({ prices: [
    { level: 'KAIWA', levelLabel: 'Giao tiếp (Kaiwa)', hourlyRate: 180000, sessionMinutes: 60, pricePerSession: 180000 }
  ] }), [{ level: 'KAIWA', sessionMinutes: 60, pricePerSession: 180000 }]);
  // Phần tử cũ chỉ có giá theo giờ
  assert.deepEqual(priceRowsFromContent({ prices: [{ level: 'JLPT_N5', hourlyRate: 200000 }] }),
    [{ level: 'JLPT_N5', sessionMinutes: 90, pricePerSession: 300000 }]);
  // Bài rất cũ chỉ có một mức giá chung
  assert.deepEqual(priceRowsFromContent({ prices: [], hourlyRate: 200000 }),
    [{ level: '', sessionMinutes: 90, pricePerSession: 300000 }]);
  assert.deepEqual(priceRowsFromContent(null), [{ level: '', sessionMinutes: 90, pricePerSession: '' }]);
});

test('nút gửi chỉ hiện khi được sửa: "Gửi duyệt" lần đầu, "Gửi lại" khi được yêu cầu bổ sung', () => {
  assert.equal(submitLabel(true, null), 'Gửi duyệt');
  assert.equal(submitLabel(true, { reviewStatus: 'NEEDS_INFO' }), 'Gửi lại');
  assert.equal(submitLabel(false, { reviewStatus: 'PENDING' }), null);
  assert.equal(submitLabel(false, { reviewStatus: 'APPROVED' }), null);
});

test('bài đã duyệt: được cập nhật qua nút "Chỉnh sửa bài đăng", nút gửi là "Gửi bản cập nhật"', () => {
  assert.equal(isUpdateMode(true, { reviewStatus: 'APPROVED' }), true);
  assert.equal(isUpdateMode(true, { reviewStatus: 'REJECTED' }), true);
  assert.equal(isUpdateMode(true, { reviewStatus: 'NEEDS_INFO' }), false);
  assert.equal(isUpdateMode(true, null), false);
  assert.equal(isUpdateMode(false, { reviewStatus: 'APPROVED' }), false);
  assert.equal(submitLabel(true, { reviewStatus: 'APPROVED' }), 'Gửi bản cập nhật');
  assert.equal(submitLabel(true, { reviewStatus: 'REJECTED' }), 'Gửi bản cập nhật');
});
