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
  const r = validateStatusForm({ status: 'SUSPENDED', currentStatus: 'ACTIVE', message: '   ' });
  assert.equal(r.valid, true);
  assert.deepEqual(r.payload, { status: 'SUSPENDED', expectedStatus: 'ACTIVE', message: null });
});

test('biểu mẫu lỗi: thiếu trạng thái, trùng trạng thái, lời nhắn quá dài', () => {
  assert.deepEqual(validateStatusForm({ currentStatus: 'ACTIVE' }).errors, { status: 'Vui lòng chọn trạng thái mới.' });
  assert.equal(validateStatusForm({ status: 'SUSPENDED', currentStatus: 'ACTIVE' }).valid, true);
  assert.equal(validateStatusForm({ status: 'ACTIVE', currentStatus: 'ACTIVE' }).errors.status, 'Tài khoản đã ở trạng thái này.');
  const long = validateStatusForm({ status: 'BANNED', currentStatus: 'ACTIVE', message: 'y'.repeat(1001) });
  assert.equal(long.valid, false);
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

test('lastLoginInfo: thời gian không đăng nhập dễ đọc, đánh dấu lâu không hoạt động từ 30 ngày', async () => {
  const { lastLoginInfo, INACTIVE_DAYS } = await import('../js/manager-users-helpers.js');
  const now = new Date('2026-11-23T10:00:00Z');
  const ago = (ms) => new Date(now.getTime() - ms).toISOString();
  const MIN = 60000, DAY = 86400000;
  assert.deepEqual(lastLoginInfo(null, now), { text: 'Chưa ghi nhận', idle: false });
  assert.deepEqual(lastLoginInfo('không phải ngày', now), { text: 'Chưa ghi nhận', idle: false });
  assert.equal(lastLoginInfo(ago(20 * 1000), now).text, 'Vừa xong');
  assert.equal(lastLoginInfo(ago(15 * MIN), now).text, '15 phút trước');
  assert.equal(lastLoginInfo(ago(3 * 60 * MIN), now).text, '3 giờ trước');
  assert.deepEqual(lastLoginInfo(ago(12 * DAY), now), { text: '12 ngày trước', idle: false });
  assert.deepEqual(lastLoginInfo(ago(INACTIVE_DAYS * DAY), now), { text: '30 ngày trước', idle: true });
  assert.equal(lastLoginInfo(ago(75 * DAY), now).text, '2 tháng trước');
  assert.equal(lastLoginInfo(ago(400 * DAY), now).text, '1 năm trước');
});

test('lockAction: đang hoạt động thì Tạm khóa, đang bị khóa thì Mở lại, chờ kích hoạt thì không có nút', async () => {
  const { lockAction } = await import('../js/manager-users-helpers.js');
  assert.deepEqual(lockAction('ACTIVE'), { target: 'SUSPENDED', label: 'Tạm khóa tài khoản', confirm: 'Xác nhận tạm khóa', tone: 'danger' });
  assert.equal(lockAction('SUSPENDED').target, 'ACTIVE');
  assert.equal(lockAction('SUSPENDED').label, 'Mở lại tài khoản');
  assert.equal(lockAction('BANNED').target, 'ACTIVE');
  assert.equal(lockAction('PENDING'), null);
  assert.equal(lockAction(undefined), null);
});
