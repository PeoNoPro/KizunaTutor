import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  accountStatusTone, emptyMessage, validateStatusForm, statusConsequence, orDash, STATUS_FILTERS
} from '../js/manager-users-helpers.js';

test('màu trạng thái tài khoản', () => {
  assert.equal(accountStatusTone('ACTIVE'), 'success');
  assert.equal(accountStatusTone('PENDING'), 'warning');
  assert.equal(accountStatusTone('SUSPENDED'), 'danger');
  assert.equal(accountStatusTone('BANNED'), 'danger');
  assert.equal(accountStatusTone('LẠ'), 'neutral');
});

test('thông báo rỗng: có điều kiện tìm theo UC-20 AF-1, không có thì theo nhóm', () => {
  assert.equal(emptyMessage('TUTOR', { keyword: ' an ' }), 'Không tìm thấy người dùng phù hợp với điều kiện tìm kiếm.');
  assert.equal(emptyMessage('LEARNER', { status: 'BANNED' }), 'Không tìm thấy người dùng phù hợp với điều kiện tìm kiếm.');
  assert.equal(emptyMessage('LEARNER'), 'Chưa có học viên nào đăng ký.');
  assert.equal(emptyMessage('TUTOR', { keyword: '   ' }), 'Chưa có gia sư nào đăng ký.');
});

test('biểu mẫu hợp lệ: cắt khoảng trắng, lời nhắn trống gửi null, kèm trạng thái hiện tại', () => {
  const r = validateStatusForm({ status: 'SUSPENDED', currentStatus: 'ACTIVE', reason: '  Vi phạm  ', message: '   ' });
  assert.equal(r.valid, true);
  assert.deepEqual(r.payload, { status: 'SUSPENDED', expectedStatus: 'ACTIVE', reason: 'Vi phạm', message: null });
});

test('biểu mẫu lỗi: thiếu trạng thái, thiếu lý do, trùng trạng thái, quá dài', () => {
  assert.deepEqual(validateStatusForm({ currentStatus: 'ACTIVE' }).errors, {
    status: 'Vui lòng chọn trạng thái mới.', reason: 'Vui lòng nhập lý do nội bộ.'
  });
  assert.equal(validateStatusForm({ status: 'ACTIVE', currentStatus: 'ACTIVE', reason: 'x' }).errors.status, 'Tài khoản đã ở trạng thái này.');
  const long = validateStatusForm({ status: 'BANNED', currentStatus: 'ACTIVE', reason: 'x'.repeat(1001), message: 'y'.repeat(1001) });
  assert.equal(long.valid, false);
  assert.ok(long.errors.reason.includes('1.000'));
  assert.ok(long.errors.message.includes('1.000'));
});

test('câu hệ quả theo trạng thái mới', () => {
  assert.match(statusConsequence('SUSPENDED'), /đăng xuất ngay/);
  assert.match(statusConsequence('BANNED'), /không thể đăng nhập nữa/);
  assert.match(statusConsequence('ACTIVE'), /như bình thường/);
  assert.equal(statusConsequence(''), '');
});

test('giá trị trống hiện "Chưa cập nhật"; nhãn không viết hoa toàn bộ', () => {
  assert.equal(orDash(null), 'Chưa cập nhật');
  assert.equal(orDash('  '), 'Chưa cập nhật');
  assert.equal(orDash(0), '0');
  STATUS_FILTERS.forEach(({ label }) => assert.notEqual(label, label.toUpperCase()));
});
