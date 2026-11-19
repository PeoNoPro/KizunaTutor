import test from 'node:test';
import assert from 'node:assert/strict';
import { composeAddress, parseAddress, validateAddressParts, plainText, mapLink } from '../js/address-helpers.js';
import { VN_PROVINCES } from '../js/vn-address-data.js';

test('danh mục hành chính sau 01/07/2025: 34 tỉnh, 3.321 phường xã, Hà Nội 126', () => {
  assert.equal(VN_PROVINCES.length, 34);
  assert.equal(VN_PROVINCES.reduce((n, p) => n + p[2].length, 0), 3321);
  const hn = VN_PROVINCES.find((p) => p[1] === 'Thành phố Hà Nội');
  assert.equal(hn[2].length, 126);
  assert.ok(hn[2].some((w) => w[1] === 'Phường Hai Bà Trưng'));
});

test('ghép địa chỉ: bỏ ô trống và dấu phẩy thừa', () => {
  assert.equal(composeAddress({ detail: ' Số 12, ngõ 34 Phố Huế, ', ward: 'Phường Hai Bà Trưng', province: 'Thành phố Hà Nội' }),
    'Số 12, ngõ 34 Phố Huế, Phường Hai Bà Trưng, Thành phố Hà Nội');
  assert.equal(composeAddress({ detail: '', ward: 'Phường Bạch Mai', province: 'Thành phố Hà Nội' }), 'Phường Bạch Mai, Thành phố Hà Nội');
});

test('tách ngược địa chỉ đã lưu ra tỉnh, phường, địa chỉ cụ thể', () => {
  const r = parseAddress('Số 12, ngõ 34 Phố Huế, Phường Hai Bà Trưng, Thành phố Hà Nội', VN_PROVINCES);
  const hn = VN_PROVINCES.find((p) => p[0] === r.provinceCode);
  assert.equal(hn[1], 'Thành phố Hà Nội');
  assert.equal(hn[2].find((w) => w[0] === r.wardCode)[1], 'Phường Hai Bà Trưng');
  assert.equal(r.detail, 'Số 12, ngõ 34 Phố Huế');
  assert.equal(r.recognized, true);
});

test('nhận cả khi người dùng gõ không dấu, thiếu "Thành phố" / "Phường"', () => {
  const r = parseAddress('12 ngo 34, hai ba trung, ha noi', VN_PROVINCES);
  assert.equal(r.recognized, true);
  assert.equal(r.detail, '12 ngo 34');
});

test('địa chỉ tự do không nhận ra được: giữ nguyên làm địa chỉ cụ thể', () => {
  const r = parseAddress('gần hồ Hoàn Kiếm, nhà 12 ngõ 34', VN_PROVINCES);
  assert.equal(r.recognized, false);
  assert.equal(r.provinceCode, null);
  assert.equal(r.detail, 'gần hồ Hoàn Kiếm, nhà 12 ngõ 34');
  // Có tỉnh nhưng không có phường hợp lệ: chọn sẵn tỉnh, phường để trống
  const p = parseAddress('nhà 5, Phường Không Có, Thành phố Hà Nội', VN_PROVINCES);
  assert.ok(p.provinceCode);
  assert.equal(p.wardCode, null);
  assert.equal(p.recognized, false);
  assert.equal(parseAddress('', VN_PROVINCES).detail, '');
});

test('kiểm tra 3 ô bắt buộc', () => {
  assert.deepEqual(validateAddressParts({ provinceCode: 1, wardCode: 4, detail: 'Số 1' }), {});
  const e = validateAddressParts({ provinceCode: null, wardCode: null, detail: '  ' });
  assert.ok(e.province && e.ward && e.detail);
  assert.ok(validateAddressParts({ provinceCode: 1, wardCode: 4, detail: 'x'.repeat(301) }).detail);
});

test('so tên không dấu và link bản đồ', () => {
  assert.equal(plainText('  Phường  Hai Bà Trưng '), 'phuong hai ba trung');
  assert.equal(plainText('Đống Đa'), 'dong da');
  assert.ok(mapLink('Số 1, Hà Nội').startsWith('https://www.google.com/maps/search/?api=1&query='));
});
