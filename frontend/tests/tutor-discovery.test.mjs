import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  buildMetricTriplet,
  buildPreviewPanelHtml,
  buildTutorCardHtml,
  computePreviewOffsetPx,
  escapeHtml,
  formatHourlyRateInfo,
  getShortDisplayName,
  getTutorInitials,
  getTutorProfileUrl,
  matchTutorTimeFilter,
  normalizeTutorRecord,
  summarizeAvailabilities
} from '../pages/tutor-discovery/tutor-discovery-helpers.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const frontendDir = path.resolve(__dirname, '..');

test('normalizeTutorRecord ánh xạ dữ liệu thật từ API và không tạo trường giả', () => {
  const normalized = normalizeTutorRecord({
    tutorId: 'tutor-01',
    tutorCode: 10025,
    fullName: 'Nguyễn Minh Anh',
    avatarUrl: 'https://example.com/a.jpg',
    verified: true,
    featured: true,
    headline: 'Gia sư cũ',
    postTitle: 'Luyện thi JLPT N2 thực chiến',
    bio: 'Có 6 năm giảng dạy tiếng Nhật giao tiếp và luyện thi JLPT.',
    highestJlptLevel: 'N1',
    experienceYears: 6,
    hourlyRate: 280000,
    avgRating: 4.9,
    totalReviews: 12,
    studentCount: 6,
    lessonCount: 24,
    location: 'Cầu Giấy, Hà Nội',
    teachingFormat: 'Online 1-1',
    specialties: ['JLPT N2', 'Kaiwa'],
    availabilities: [
      { dayOfWeek: 2, dayName: 'Thứ hai', startTime: '19:00', endTime: '21:00' }
    ]
  });

  assert.equal(normalized.id, 'tutor-01');
  assert.equal(normalized.tutorCode, 10025);
  assert.equal(normalized.headline, 'Luyện thi JLPT N2 thực chiến');
  assert.equal(normalized.verified, true);
  assert.equal(normalized.isFeatured, true);
  assert.equal(normalized.rating, 4.9);
  assert.equal(normalized.reviewsCount, 12);
  assert.equal(normalized.studentCount, 6);
  assert.equal(normalized.lessonCount, 24);
  assert.equal(normalized.availabilities.length, 1);
  assert.equal('isOnline' in normalized, false);

  const legacyFlags = normalizeTutorRecord({ isVerified: true, isFeatured: true });
  assert.equal(legacyFlags.verified, true);
  assert.equal(legacyFlags.isFeatured, true);
});

test('buildMetricTriplet chỉ dùng dữ liệu thật từ DB (đánh giá, số học viên, số buổi học)', () => {
  const fullMetrics = buildMetricTriplet({
    rating: 4.86,
    reviewsCount: 8,
    studentCount: 5,
    lessonCount: 19
  });
  assert.deepEqual(fullMetrics, [
    { value: '4.9 ★', label: '8 đánh giá' },
    { value: '5', label: 'Học viên' },
    { value: '19', label: 'Buổi học' }
  ]);

  const minimalMetrics = buildMetricTriplet({
    rating: 0,
    reviewsCount: 0,
    studentCount: 0,
    lessonCount: 0
  });
  assert.deepEqual(minimalMetrics, [
    { value: 'Mới', label: 'Chưa có đánh giá' },
    { value: '0', label: 'Học viên' },
    { value: '0', label: 'Buổi học' }
  ]);
});

test('formatHourlyRateInfo, summarizeAvailabilities và các hàm tên gia sư hoạt động chính xác', () => {
  const rate = formatHourlyRateInfo(250000);
  assert.match(rate.primary, /^250[.,]000đ$/);
  assert.equal(rate.unit, '60 phút/giờ');

  const contactRate = formatHourlyRateInfo(0);
  assert.equal(contactRate.primary, 'Liên hệ');
  assert.equal(contactRate.unit, 'Thỏa thuận học phí');

  assert.equal(getShortDisplayName('Nguyễn Thị Minh Anh'), 'Minh Anh');
  assert.equal(getShortDisplayName('Kenji'), 'Kenji');
  assert.equal(getTutorInitials('Nguyễn Thị Minh Anh'), 'NA');
  assert.equal(getTutorProfileUrl({ tutorCode: 10012, id: 'abc' }), '/gs/10012');
  assert.equal(getTutorProfileUrl({ id: 'abc' }), '/pages/tutor-profile/?id=abc');

  const slots = summarizeAvailabilities([
    { dayName: 'Thứ hai', startTime: '19:00:00', endTime: '21:00:00' },
    { dayName: 'Thứ tư', startTime: '20:00', endTime: '21:30' },
    { dayName: '', startTime: '08:00', endTime: '09:00' }
  ], 2);
  assert.equal(slots.length, 2);
  assert.deepEqual(slots[0], { dayName: 'Thứ hai', timeRange: '19:00 - 21:00' });
  assert.deepEqual(slots[1], { dayName: 'Thứ tư', timeRange: '20:00 - 21:30' });
});

test('buildTutorCardHtml dựng thẻ ngang tiếng Việt an toàn, không inline style/onclick hay dữ liệu giả', () => {
  const longBio = 'Kinh nghiệm luyện thi JLPT N3-N1 và giao tiếp tiếng Nhật công sở cho kỹ sư công nghệ thông tin, giáo trình cá nhân hóa theo mục tiêu từng học viên.';
  const html = buildTutorCardHtml({
    id: 'uuid-123',
    tutorCode: 10099,
    fullName: 'Trần <script>alert(1)</script> Thảo',
    avatarUrl: '',
    verified: true,
    isFeatured: true,
    headline: 'Chuyên luyện Kaiwa & JLPT N2',
    experienceYears: 4,
    jlptLevel: 'N1',
    specialties: ['Kaiwa', 'JLPT N2'],
    teachingFormat: 'Online 1-1',
    location: 'Quận 1, TP. Hồ Chí Minh',
    hourlyRate: 300000,
    rating: 5,
    reviewsCount: 4,
    studentCount: 3,
    lessonCount: 14,
    bio: longBio,
    availabilities: [{ dayName: 'Thứ ba', startTime: '19:00', endTime: '20:30' }]
  }, { isSelected: true });

  assert.ok(html.includes('aria-selected="true"'));
  assert.ok(html.includes('Trần &lt;script&gt;alert(1)&lt;/script&gt; Thảo'));
  assert.ok(!html.includes('<script>'));
  assert.ok(html.includes('Đặt lịch học thử'));
  assert.ok(!html.includes('calendar_add_on'));
  assert.ok(html.includes('bg-[#006194]'));
  assert.ok(!html.includes('bg-[#FF85B3]'));
  assert.ok(html.includes('Kết nối với gia sư'));
  assert.ok(!html.includes('<span>Xem hồ sơ</span>'));
  assert.ok(html.includes('4 đánh giá'));
  assert.ok(html.includes('>3</div>'));
  assert.ok(html.includes('>Học viên</div>'));
  assert.ok(html.includes('>14</div>'));
  assert.ok(html.includes('>Buổi học</div>'));
  assert.ok(!html.includes('>Kinh nghiệm</div>'));
  assert.ok(!html.includes('>Lịch rảnh/tuần</div>'));
  assert.ok(html.includes('Xem thêm'));
  assert.ok(html.includes('Gia sư tiêu biểu'));
  assert.ok(html.includes('Đã xác thực'));
  assert.ok(html.includes('Đang mở 1 khung giờ trống trong tuần'));
  assert.ok(!html.includes('open_in_new'), 'Không hiển thị nút icon mở hồ sơ phụ ở góc phải thẻ');
  assert.ok(!/\bstyle\s*=/.test(html), 'Không dùng inline style=');
  assert.ok(!/\bonclick\s*=/.test(html), 'Không dùng inline onclick=');
  assert.equal(escapeHtml('<b>hi</b>'), '&lt;b&gt;hi&lt;/b&gt;');
});

test('buildPreviewPanelHtml tối giản chỉ giữ ảnh đại diện, Xem lịch và Xem hồ sơ gia sư', () => {
  const htmlWithAvatar = buildPreviewPanelHtml({
    id: 't1',
    tutorCode: 10005,
    fullName: 'Lê Thu Trang',
    avatarUrl: 'https://example.com/trang.jpg',
    verified: true,
    headline: 'Gia sư tiếng Nhật IT',
    jlptLevel: 'N1',
    teachingFormat: 'Online 1-1',
    location: 'Hà Nội',
    hourlyRate: 250000,
    rating: 4.8,
    reviewsCount: 5,
    availabilities: [
      { dayName: 'Thứ năm', startTime: '18:30', endTime: '20:00' }
    ]
  });

  assert.ok(htmlWithAvatar.includes('src="https://example.com/trang.jpg"'));
  assert.ok(htmlWithAvatar.includes('/gs/10005#weekly-booking-calendar-section'));
  assert.ok(htmlWithAvatar.includes('href="/gs/10005"'));
  assert.ok(htmlWithAvatar.includes('Xem lịch'));
  assert.ok(htmlWithAvatar.includes('Xem hồ sơ gia sư'));
  assert.ok(!htmlWithAvatar.includes('JLPT'), 'Không nhồi thêm chi tiết JLPT trong khung preview');
  assert.ok(!htmlWithAvatar.includes('250'), 'Không nhồi thêm giá tiền trong khung preview');
  assert.ok(!htmlWithAvatar.includes('Thứ năm'), 'Không nhồi thêm lịch chi tiết trong khung preview');

  const htmlFallbackAvatar = buildPreviewPanelHtml({
    id: 't2',
    fullName: 'Phạm Bảo',
    avatarUrl: ''
  });
  assert.ok(htmlFallbackAvatar.includes('PB'));
  assert.ok(htmlFallbackAvatar.includes('/pages/tutor-profile/?id=t2#weekly-booking-calendar-section'));
  assert.ok(htmlFallbackAvatar.includes('Xem lịch'));
  assert.ok(htmlFallbackAvatar.includes('Xem hồ sơ gia sư'));
});

test('computePreviewOffsetPx tính vị trí trượt khớp thẻ đang chọn và giới hạn trong khung danh sách', () => {
  assert.equal(computePreviewOffsetPx({
    cardTopRelativeToAside: 0,
    asideTopInViewport: 240,
    asideHeight: 1000,
    panelHeight: 280
  }), 0);

  assert.equal(computePreviewOffsetPx({
    cardTopRelativeToAside: 260,
    asideTopInViewport: 240,
    asideHeight: 1000,
    panelHeight: 280
  }), 260);

  // Thẻ đang cuộn chạm mép trên viewport -> trượt giữ khoảng cách stickyTopPx an toàn
  assert.equal(computePreviewOffsetPx({
    cardTopRelativeToAside: 120,
    asideTopInViewport: -120,
    asideHeight: 1000,
    panelHeight: 280,
    stickyTopPx: 96
  }), 216);

  // Chạm cuối danh sách -> kẹp ở asideHeight - panelHeight
  assert.equal(computePreviewOffsetPx({
    cardTopRelativeToAside: 880,
    asideTopInViewport: -200,
    asideHeight: 1000,
    panelHeight: 280
  }), 720);
});

test('matchTutorTimeFilter lọc đúng theo khung giờ và ngày học từ lịch rảnh DB', () => {
  const tutor = {
    availabilities: [
      { dayOfWeek: 6, startTime: '18:30', endTime: '20:30' }, // Thứ 6 buổi tối
      { dayOfWeek: 1, startTime: '09:00', endTime: '11:00' }  // Chủ nhật buổi sáng
    ]
  };
  const emptyTutor = { availabilities: [] };

  // Không chọn bộ lọc thời gian -> luôn khớp
  assert.equal(matchTutorTimeFilter(emptyTutor, [], []), true);
  assert.equal(matchTutorTimeFilter(tutor, new Set(), new Set()), true);

  // Khi có chọn bộ lọc nhưng gia sư không có lịch -> không khớp
  assert.equal(matchTutorTimeFilter(emptyTutor, ['morning'], []), false);
  assert.equal(matchTutorTimeFilter(emptyTutor, [], [2]), false);

  // Chỉ chọn khung giờ
  assert.equal(matchTutorTimeFilter(tutor, ['morning'], []), true);
  assert.equal(matchTutorTimeFilter(tutor, ['evening'], []), true);
  assert.equal(matchTutorTimeFilter(tutor, ['afternoon'], []), false);

  // Chỉ chọn ngày học (1 = CN, 2..7 = T2..T7)
  assert.equal(matchTutorTimeFilter(tutor, [], [6]), true);
  assert.equal(matchTutorTimeFilter(tutor, [], [1]), true);
  assert.equal(matchTutorTimeFilter(tutor, [], [2, 3]), false);

  // Kết hợp cả khung giờ và ngày học trên cùng 1 ca rảnh
  assert.equal(matchTutorTimeFilter(tutor, ['morning'], [1]), true);
  assert.equal(matchTutorTimeFilter(tutor, ['evening'], [6]), true);
  assert.equal(matchTutorTimeFilter(tutor, ['morning'], [6]), false);
});

test('hợp đồng HTML/JS trang tutor-discovery dùng component chung, bộ lọc 1 hàng ngang và không ALL-CAPS hay inline', async () => {
  const html = await readFile(path.join(frontendDir, 'pages/tutor-discovery/index.html'), 'utf8');
  const controllerJs = await readFile(path.join(frontendDir, 'pages/tutor-discovery/tutor-discovery.js'), 'utf8');

  assert.ok(html.includes('id="header-placeholder"'));
  assert.ok(html.includes('id="footer-placeholder"'));
  assert.ok(!/<header[\s>]/i.test(html));
  assert.ok(!/<footer[\s>]/i.test(html));

  assert.ok(html.includes('id="filter-pill-bar"'));
  assert.ok(html.includes('id="popover-schedule"'));
  assert.ok(html.includes('id="reset-filter-btn"'));
  for (const labelText of [
    'Khung giờ học',
    'Buổi sáng (00:00 - 12:00)',
    'Buổi chiều (12:00 - 18:00)',
    'Buổi tối (18:00 - 24:00)',
    'Ngày học',
    'Thứ 2',
    'Thứ 3',
    'Thứ 4',
    'Thứ 5',
    'Thứ 6',
    'Thứ 7',
    'Chủ nhật'
  ]) {
    assert.ok(html.includes(labelText), `Thiếu nhãn bộ lọc thời gian: ${labelText}`);
  }
  assert.ok(!html.includes('id="advanced-filter-drawer"'), 'Không còn ngăn kéo bộ lọc nhiều tầng cũ');
  assert.ok(!html.includes('schedule-matrix-cell'), 'Không còn ma trận T2..CN dạng lưới cũ');
  assert.ok(!html.includes('data-dropdown="more"'), 'Không còn mục bộ lọc Khác');
  assert.ok(!html.includes('id="filter-tutor-level"'));
  assert.ok(!html.includes('id="filter-verified-only"'));
  assert.ok(!controllerJs.includes('matchTutorLevel'));
  assert.ok(html.includes('lg:flex-auto'), 'Các nút lọc tự giãn đều chiếm hết khoảng trống ngang');
  assert.ok(controllerJs.includes('filter-pill-btn w-full h-10'), 'Giữ kích thước giãn rộng khi đổi trạng thái nút lọc');

  assert.ok(html.includes('id="results-layout"'));
  assert.ok(html.includes('items-stretch'));
  assert.ok(html.includes('id="tutor-grid"'));
  assert.ok(html.includes('id="tutor-preview-aside"'));
  assert.ok(html.includes('id="tutor-preview-panel"'));
  assert.ok(html.includes('transition: transform 320ms cubic-bezier(0.22, 1, 0.36, 1)'));
  assert.ok(controllerJs.includes('syncPreviewPanelPosition'));
  assert.ok(html.includes('overflow-x-clip'), 'Tránh overflow-hidden làm hỏng bố cục');

  for (const forbiddenCaps of ['TỈNH / THÀNH', 'MÔN HỌC / KỸ NĂNG', 'HỌC PHÍ / BUỔI', 'HÌNH THỨC', 'TRÌNH ĐỘ GIA SƯ', 'KẾT NỐI TẬN TÂM']) {
    assert.ok(!html.includes(forbiddenCaps), `Không dùng chữ ALL-CAPS: ${forbiddenCaps}`);
  }

  for (const doc of [html, controllerJs]) {
    assert.ok(!/\bstyle\s*=/.test(doc), 'Không dùng thuộc tính style= nội tuyến');
    assert.ok(!/\bonclick\s*=/.test(doc), 'Không dùng thuộc tính onclick= nội tuyến');
    assert.ok(!/\bROLE_[A-Z_]+\b/.test(doc), 'Không để lộ mã vai trò nội bộ ROLE_*');
  }
});

test('trang tutor-profile tự động cuộn tới lịch rảnh tuần sau khi render dữ liệu từ DB và không hiển thị thẻ mã hay đã xác thực', async () => {
  const profileJs = await readFile(path.join(frontendDir, 'pages/tutor-profile/tutor-profile.js'), 'utf8');
  assert.ok(profileJs.includes('id="weekly-booking-calendar-section"'));
  assert.ok(profileJs.includes("window.location.hash === '#weekly-booking-calendar-section'"));
  assert.ok(profileJs.includes('scheduleSection.scrollIntoView'));
  assert.ok(!profileJs.includes('Mã: #'), 'Không hiển thị thẻ Mã gia sư ở đầu hồ sơ');
  assert.ok(!profileJs.includes('Đã xác thực hồ sơ'), 'Không hiển thị thẻ Đã xác thực hồ sơ ở đầu hồ sơ');
  assert.ok(
    profileJs.indexOf('id="open-booking-btn"') < profileJs.indexOf('id="open-connect-btn"'),
    'Nút Đặt lịch học thử 1-1 nằm phía trên nút Kết nối với gia sư'
  );
  const ctaBlockStart = profileJs.indexOf('<!-- Hành động chính (CTA) -->');
  const ctaBlockEnd = profileJs.indexOf('<!-- Cam kết đảm bảo quyền lợi -->');
  assert.ok(ctaBlockStart !== -1 && ctaBlockEnd > ctaBlockStart);
  const ctaBlock = profileJs.slice(ctaBlockStart, ctaBlockEnd);
  assert.ok(!ctaBlock.includes('calendar_add_on'), 'Không hiển thị icon calendar_add_on trên nút Đặt lịch học thử 1-1');
  assert.ok(!ctaBlock.includes('handshake'), 'Không hiển thị icon handshake trên nút Kết nối với gia sư');
});
