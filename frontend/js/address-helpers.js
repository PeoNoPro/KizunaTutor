/**
 * KizunaTutor - FE-06 Địa chỉ học trực tiếp: ghép / tách chuỗi "địa chỉ cụ thể, Phường/Xã, Tỉnh/Thành phố".
 * Lớp học lưu địa chỉ trong một cột chữ (location_details), nên địa chỉ được ghép thành chuỗi chuẩn khi lưu
 * và tách ngược lại khi sửa. Hàm thuần, không đụng DOM (có test).
 */

/** Bỏ dấu, chữ thường, gộp khoảng trắng — để so tên không phân biệt dấu / hoa thường. */
export function plainText(s) {
  return String(s || '')
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D')
    .toLowerCase().replace(/\s+/g, ' ').trim();
}

const PROVINCE_PREFIX = /^(thanh pho|tinh)\s+/;
const WARD_PREFIX = /^(phuong|xa|dac khu|thi tran)\s+/;

/** Tên không kèm "Tỉnh"/"Thành phố"/"Phường"/"Xã" (đã bỏ dấu) — để khớp cả khi người dùng gõ thiếu tiền tố. */
function core(name, prefix) {
  return plainText(name).replace(prefix, '');
}

/** "Số 12, ngõ 34 Phố Huế" + ward + province → "Số 12, ngõ 34 Phố Huế, Phường Hai Bà Trưng, Thành phố Hà Nội". */
export function composeAddress({ detail, ward, province }) {
  return [detail, ward, province].map((s) => String(s || '').trim().replace(/^,+|,+$/g, '').trim())
    .filter(Boolean).join(', ');
}

/**
 * Tách chuỗi địa chỉ đã lưu (hoặc địa chỉ học viên gõ tự do) theo danh mục hành chính.
 * @param {string} text
 * @param {Array<[number, string, Array<[number, string]>]>} provinces danh mục [mã, tên tỉnh, [[mã, tên phường]]]
 * @returns {{provinceCode: number|null, wardCode: number|null, detail: string, recognized: boolean}}
 *   recognized = nhận ra được cả tỉnh và phường.
 */
export function parseAddress(text, provinces) {
  const raw = String(text || '').trim();
  const result = { provinceCode: null, wardCode: null, detail: raw, recognized: false };
  if (!raw || !Array.isArray(provinces)) return result;
  const parts = raw.split(',').map((s) => s.trim()).filter(Boolean);
  if (!parts.length) return result;

  // 1) Tỉnh: phần cuối khớp tên tỉnh (có hoặc không có tiền tố)
  let pIndex = -1;
  let province = null;
  for (let i = parts.length - 1; i >= 0 && !province; i -= 1) {
    const c = core(parts[i], PROVINCE_PREFIX);
    province = provinces.find((p) => core(p[1], PROVINCE_PREFIX) === c) || null;
    if (province) pIndex = i;
  }
  if (!province) return result;
  result.provinceCode = province[0];

  // 2) Phường: phần ngay trước tỉnh khớp tên phường của tỉnh đó
  const before = parts.slice(0, pIndex);
  if (before.length) {
    const last = before[before.length - 1];
    const exact = plainText(last);
    const c = core(last, WARD_PREFIX);
    const ward = province[2].find((w) => plainText(w[1]) === exact)
      || province[2].find((w) => core(w[1], WARD_PREFIX) === c);
    if (ward) {
      result.wardCode = ward[0];
      before.pop();
      result.recognized = true;
    }
  }
  result.detail = before.join(', ');
  return result;
}

/**
 * Kiểm tra 3 ô địa chỉ học trực tiếp.
 * @returns {{province?: string, ward?: string, detail?: string}} lỗi theo ô, rỗng nếu hợp lệ
 */
export function validateAddressParts({ provinceCode, wardCode, detail }) {
  const errors = {};
  if (!provinceCode) errors.province = 'Vui lòng chọn tỉnh / thành phố.';
  if (!wardCode) errors.ward = 'Vui lòng chọn phường / xã trong danh sách.';
  const d = String(detail || '').trim();
  if (!d) errors.detail = 'Vui lòng nhập số nhà, ngõ, đường.';
  else if (d.length > 300) errors.detail = 'Địa chỉ cụ thể tối đa 300 ký tự.';
  return errors;
}

/** Link bản đồ cho địa chỉ (mở Google Maps tìm theo chuỗi). */
export function mapLink(address) {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(String(address || ''))}`;
}
