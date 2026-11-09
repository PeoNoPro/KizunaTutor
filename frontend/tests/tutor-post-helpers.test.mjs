import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validatePostDraft, submitLabel } from '../js/tutor-post-helpers.js';

const valid = {
  title: 'Luyện thi JLPT N3 cấp tốc',
  description: 'Giới thiệu '.repeat(10),
  teachingFormat: 'ONLINE',
  hourlyRate: 250000
};

test('bản nháp hợp lệ không có lỗi', () => {
  assert.deepEqual(validatePostDraft(valid), []);
});

test('báo lỗi tiếng Việt cho tiêu đề, giới thiệu, hình thức và học phí', () => {
  const errors = validatePostDraft({ title: 'Ngắn', description: 'Ngắn', teachingFormat: '', hourlyRate: 5000 });
  assert.ok(errors.includes('Tiêu đề tối thiểu 10 ký tự'));
  assert.ok(errors.includes('Giới thiệu tối thiểu 50 ký tự'));
  assert.ok(errors.includes('Chọn hình thức dạy'));
  assert.ok(errors.includes('Học phí mỗi giờ từ 10.000 đ đến 10.000.000 đ'));
  errors.forEach((e) => assert.notEqual(e, e.toUpperCase(), 'không viết in hoa toàn bộ'));
});

test('giới hạn trên khớp backend', () => {
  assert.ok(validatePostDraft({ ...valid, title: 'x'.repeat(151) }).includes('Tiêu đề tối đa 150 ký tự'));
  assert.ok(validatePostDraft({ ...valid, description: 'x'.repeat(3001) }).includes('Giới thiệu tối đa 3.000 ký tự'));
  assert.equal(validatePostDraft({ ...valid, hourlyRate: 10000001 }).length, 1);
  assert.deepEqual(validatePostDraft({ ...valid, hourlyRate: 10000 }), []);
});

test('nút gửi chỉ hiện khi được sửa: "Gửi duyệt" lần đầu, "Gửi lại" khi được yêu cầu bổ sung', () => {
  assert.equal(submitLabel(true, null), 'Gửi duyệt');
  assert.equal(submitLabel(true, { reviewStatus: 'NEEDS_INFO' }), 'Gửi lại');
  assert.equal(submitLabel(false, { reviewStatus: 'PENDING' }), null);
  assert.equal(submitLabel(false, { reviewStatus: 'APPROVED' }), null);
});
