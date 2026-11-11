import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  containsDetailedAddressOrGps,
  isTutorProfileIncomplete,
  roleLabel,
  statusBadgeHtml,
  statusLabel,
  validateAvatarFile,
  validateStatusUpdate,
  validateTutorForm
} from '../js/profile-utils.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const frontendDir = path.resolve(__dirname, '..');

test('chuyển ngữ vai trò và trạng thái tài khoản sang tiếng Việt, không lộ mã nội bộ', () => {
  assert.equal(roleLabel('ROLE_LEARNER'), 'Học viên');
  assert.equal(roleLabel('ROLE_TUTOR'), 'Gia sư');
  assert.equal(statusLabel('ACTIVE'), 'Đang hoạt động');
  assert.equal(statusLabel('SUSPENDED'), 'Tạm khóa');
  assert.equal(statusLabel('BANNED'), 'Đã cấm');

  const badge = statusBadgeHtml('SUSPENDED');
  assert.ok(badge.includes('Tạm khóa'));
  assert.ok(!badge.includes('SUSPENDED'));
  assert.ok(!badge.includes('ROLE_'));
});

test('GB-15: cho phép Quận/Huyện chung và chặn số nhà, tên đường hoặc tọa độ GPS', () => {
  assert.equal(containsDetailedAddressOrGps('Cầu Giấy'), false);
  assert.equal(containsDetailedAddressOrGps('Quận 1'), false);
  assert.equal(containsDetailedAddressOrGps('Thành phố Thủ Đức'), false);

  assert.equal(containsDetailedAddressOrGps('Số nhà 15 ngõ 2 Xuân Thủy'), true);
  assert.equal(containsDetailedAddressOrGps('123 Lê Lợi'), true);
  assert.equal(containsDetailedAddressOrGps('Đường Láng'), true);
  assert.equal(containsDetailedAddressOrGps('21.0285, 105.8542'), true);
});

test('kiểm tra biểu mẫu hồ sơ giảng dạy gia sư khớp quy tắc backend', () => {
  const valid = {
    fullName: 'Nguyễn Thu Hà',
    phone: '0912345678',
    avatarUrl: '/api/profile/avatar/ha.png',
    headline: 'Gia sư tiếng Nhật giao tiếp',
    bio: 'Có 4 năm kinh nghiệm luyện JLPT.',
    experienceYears: 4,
    hourlyRate: 250000,
    highestJlptLevel: 'N2',
    province: 'Hà Nội',
    addressDetail: 'Cầu Giấy'
  };
  assert.equal(validateTutorForm(valid).valid, true);

  const invalid = validateTutorForm({
    ...valid,
    headline: '   ',
    experienceYears: 60,
    hourlyRate: -1000,
    highestJlptLevel: 'N6',
    province: '',
    addressDetail: 'Số nhà 10 ngõ 3'
  });
  assert.equal(invalid.valid, false);
  assert.ok(invalid.errors.headline);
  assert.ok(invalid.errors.experienceYears);
  assert.ok(invalid.errors.hourlyRate);
  assert.ok(invalid.errors.highestJlptLevel);
  assert.ok(invalid.errors.province);
  assert.ok(invalid.errors.addressDetail);

  Object.values(invalid.errors).forEach((msg) => {
    assert.notEqual(msg, msg.toUpperCase(), 'Thông báo lỗi không viết hoa toàn bộ');
  });
});

test('kiểm tra cập nhật trạng thái tài khoản bắt buộc lý do 5-500 ký tự', () => {
  assert.equal(validateStatusUpdate({ status: 'SUSPENDED', reason: '   Ngắn  ' }).valid, false);
  assert.equal(validateStatusUpdate({ status: 'PENDING', reason: 'Lý do hợp lệ đủ dài' }).valid, false);
  assert.equal(validateStatusUpdate({ status: 'SUSPENDED', reason: 'Vi phạm quy định hủy lịch học' }).valid, true);
  assert.equal(validateStatusUpdate({ status: 'BANNED', reason: 'a'.repeat(501) }).valid, false);
});

test('kiểm tra phát hiện hồ sơ gia sư chưa đầy đủ và kiểm tra tệp ảnh đại diện', () => {
  assert.equal(isTutorProfileIncomplete({ headline: '', hourlyRate: 0 }), true);
  assert.equal(isTutorProfileIncomplete({ headline: 'Gia sư N2', hourlyRate: 200000 }), false);

  assert.ok(validateAvatarFile({ type: 'application/pdf', size: 1000 }));
  assert.ok(validateAvatarFile({ type: 'image/png', size: 6 * 1024 * 1024 }));
  assert.equal(validateAvatarFile({ type: 'image/jpeg', size: 1024 * 1024 }), '');
});

test('hợp đồng trang HTML/JS và định tuyến FE-02 không dùng mã nội bộ hay thuộc tính nội tuyến', async () => {
  const htmlPaths = [
    'pages/learner-profile/index.html',
    'pages/tutor-teaching-profile/index.html',
    'pages/manager-users/index.html'
  ];

  for (const rel of htmlPaths) {
    const html = await readFile(path.join(frontendDir, rel), 'utf8');
    assert.ok(html.includes('Plus Jakarta Sans') || html.includes('Plus+Jakarta+Sans'), `${rel} phải sử dụng font Plus Jakarta Sans`);
    assert.ok(!/\sonclick=/i.test(html), `${rel} không được dùng onclick nội tuyến`);
    assert.ok(!/\sstyle=/i.test(html), `${rel} không được dùng style nội tuyến`);
    assert.ok(!/\buppercase\b/.test(html), `${rel} không được dùng lớp Tailwind uppercase`);
  }

  const [apiJs, staffLayoutJs, serverJs] = await Promise.all([
    readFile(path.join(frontendDir, 'js/api.js'), 'utf8'),
    readFile(path.join(frontendDir, 'components/staff-layout/staff-layout.js'), 'utf8'),
    readFile(path.join(frontendDir, 'server.js'), 'utf8')
  ]);

  ['getTutorProfile', 'updateTutorProfile', 'getUsers', 'getUserDetail', 'updateUserStatus'].forEach((fn) => {
    assert.ok(apiJs.includes(`${fn}(`), `API.profile phải cung cấp hàm ${fn}`);
  });
  assert.ok(staffLayoutJs.includes("href: '/manager/users'"), 'staff-layout phải có liên kết Quản lý người dùng');
  assert.ok(serverJs.includes("'/manager/users': '/pages/manager-users/index.html'"), 'server.js phải định tuyến /manager/users');
});
