import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  buildDiscoveryUrl,
  escapeHtml,
  getGridColumnsClass,
  getJlptTrackData,
  getTutorInitials,
  sanitizePublicLocation,
  selectDisplayTutors
} from '../pages/home/home-helpers.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const frontendDir = path.resolve(__dirname, '..');

test('buildDiscoveryUrl tạo query string sạch khớp bộ lọc tutor-discovery', () => {
  assert.equal(buildDiscoveryUrl({}), '/tutors');
  assert.equal(
    buildDiscoveryUrl({ keyword: '  Kaiwa  ', jlpt: 'N3', goal: 'it', format: 'online', price: '250-400' }),
    '/tutors?q=Kaiwa&jlpt=N3&goal=it&format=online&price=250-400'
  );
  assert.equal(buildDiscoveryUrl({ location: 'Hà Nội' }), '/tutors?location=H%C3%A0+N%E1%BB%99i');
});

test('selectDisplayTutors ưu tiên gia sư tiêu biểu, bù bằng gia sư thật đã duyệt và không trùng lặp', () => {
  const featured = [
    { tutorId: 't-1', fullName: 'Nguyễn Minh Anh' },
    { tutorId: 't-2', fullName: 'Trần Nhật Huy' }
  ];
  const searched = [
    { tutorId: 't-2', fullName: 'Trần Nhật Huy' },
    { tutorId: 't-3', fullName: 'Lê Thu Trang' },
    { tutorId: 't-4', fullName: 'Phạm Quang Bảo' },
    { tutorId: 't-5', fullName: 'Đỗ Hải Yến' }
  ];

  const merged = selectDisplayTutors(featured, searched, 4);
  assert.equal(merged.hasFeatured, true);
  assert.equal(merged.usedFallbackSearch, true);
  assert.deepEqual(merged.tutors.map((t) => t.tutorId), ['t-1', 't-2', 't-3', 't-4']);
  assert.equal(merged.tutors[0]._sourceTag, 'featured');
  assert.equal(merged.tutors[2]._sourceTag, 'top-rated');

  const empty = selectDisplayTutors([], [], 4);
  assert.equal(empty.tutors.length, 0);
  assert.equal(empty.hasFeatured, false);
  assert.equal(empty.usedFallbackSearch, false);
});

test('sanitizePublicLocation tuân thủ GB-15: giữ Quận/Huyện & Tỉnh/Thành, loại bỏ số nhà/tên đường/GPS', () => {
  assert.equal(sanitizePublicLocation('Quận 1, TP. Hồ Chí Minh'), 'Quận 1, TP. Hồ Chí Minh');
  assert.equal(sanitizePublicLocation('Số nhà 15 ngõ 2 Xuân Thủy, Cầu Giấy, Hà Nội'), 'Cầu Giấy, Hà Nội');
  assert.equal(sanitizePublicLocation('123 Đường Láng, Đống Đa, Hà Nội'), 'Đống Đa, Hà Nội');
  assert.equal(sanitizePublicLocation('21.0285, 105.8542'), '');
  assert.equal(sanitizePublicLocation(''), '');
});

test('tiện ích giao diện trang chủ: chữ viết tắt tên, lớp lưới thích ứng, dữ liệu JLPT và escapeHtml', () => {
  assert.equal(getTutorInitials('Nguyễn Thu Hà'), 'NH');
  assert.equal(getTutorInitials('Kenji'), 'KE');
  assert.equal(getTutorInitials(''), 'GS');

  assert.equal(getGridColumnsClass(1), 'grid-cols-1 lg:grid-cols-2');
  assert.equal(getGridColumnsClass(2), 'grid-cols-1 md:grid-cols-2');
  assert.equal(getGridColumnsClass(3), 'grid-cols-1 md:grid-cols-3');
  assert.equal(getGridColumnsClass(4), 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4');

  assert.equal(getJlptTrackData('n2').level, 'N2');
  assert.equal(getJlptTrackData('invalid').level, 'N3');

  assert.equal(escapeHtml('<script>alert("x")</script>'), '&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;');
});

test('hợp đồng HTML/JS trang chủ tuân thủ component dùng chung, đầy đủ anchor và không rò rỉ nội bộ', async () => {
  const html = await readFile(path.join(frontendDir, 'pages/home/index.html'), 'utf8');
  const homeJs = await readFile(path.join(frontendDir, 'pages/home/home.js'), 'utf8');

  // Bắt buộc dùng placeholder cho Header & Footer dùng chung
  assert.ok(html.includes('id="header-placeholder"'));
  assert.ok(html.includes('id="footer-placeholder"'));
  assert.ok(!/<header[\s>]/i.test(html), 'Không hardcode thẻ <header> vào pages/home/index.html');
  assert.ok(!/<footer[\s>]/i.test(html), 'Không hardcode thẻ <footer> vào pages/home/index.html');

  // Phải có đủ các anchor mà Header & Footer trỏ tới
  for (const anchor of ['id="kham-pha-gia-su"', 'id="chuyen-de"', 'id="cong-cu-hoc"', 'id="cach-hoat-dong"', 'id="tro-thanh-gia-su"', 'id="ho-tro"']) {
    assert.ok(html.includes(anchor), `Thiếu anchor ${anchor} trên trang chủ`);
  }

  // Không dùng inline style/onclick, không dùng uppercase, không lộ mã nội bộ hay số liệu bịa 450+
  for (const [label, content] of [['index.html', html], ['home.js', homeJs]]) {
    assert.ok(!/\sonclick\s*=|\sstyle\s*=/i.test(content), `${label} không được dùng thuộc tính nội tuyến onclick/style`);
    assert.ok(!/\buppercase\b/.test(content), `${label} không được dùng class uppercase trên UI tiếng Việt`);
    assert.ok(!/ROLE_|BAD_CREDENTIALS|NullPointerException|450\+/.test(content), `${label} không được chứa mã nội bộ hoặc số liệu tĩnh 450+`);
  }
});
