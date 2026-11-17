/**
 * KizunaTutor - Quy tắc cập nhật hồ sơ xác minh sau khi đã phê duyệt (dùng chung giao diện + test).
 * - Thông tin liên hệ (SĐT, địa chỉ, giới thiệu, kinh nghiệm) lưu ngay.
 * - Đổi JLPT / bổ sung giấy tờ: gửi yêu cầu cập nhật để cán bộ học vụ thẩm định; hồ sơ vẫn "Đã phê duyệt".
 * - Giấy tờ đã xác minh không sửa / gỡ được; khi đang có yêu cầu chờ thẩm định thì khóa phần giấy tờ.
 */

/** Trường sửa được khi hồ sơ đã phê duyệt (họ tên, ngày sinh gắn với giấy tờ tùy thân; JLPT đổi qua yêu cầu). */
export const APPROVED_EDITABLE_FIELDS = ['phone', 'address', 'experienceYears', 'bio'];

export function hasPendingUpdate(data) {
  return Boolean(data && data.profileUpdate && data.profileUpdate.status === 'PENDING');
}

/** Được thêm giấy tờ mới: hồ sơ đang sửa được, hoặc đã phê duyệt và không có yêu cầu chờ. */
export function canUploadDocuments(data) {
  if (!data) return false;
  if (data.editable) return true;
  return Boolean(data.updatable) && !hasPendingUpdate(data);
}

/** Được đổi loại / thay tệp / gỡ một giấy tờ. */
export function documentChangeable(data, doc) {
  if (!data || !doc) return false;
  if (data.editable) return true;
  return canUploadDocuments(data) && doc.status !== 'VERIFIED';
}

/** Giấy tờ mới tải lên (chờ thẩm định, còn xem được tệp) sẽ được gửi kèm yêu cầu cập nhật. */
export function newDocumentCount(data) {
  return ((data && data.documents) || []).filter((d) => d.status === 'PENDING' && d.fileAvailable).length;
}

/** Kiểm tra trước khi gửi yêu cầu: phải đổi JLPT hoặc có giấy tờ mới. Trả về thông báo lỗi hoặc null. */
export function profileUpdateError(data, requestedJlpt, note) {
  if ((note || '').trim().length > 1000) return 'Ghi chú tối đa 1.000 ký tự.';
  const current = (data && data.profile && data.profile.highestJlptLevel) || '';
  const jlptChanged = Boolean(requestedJlpt) && requestedJlpt !== current;
  if (!jlptChanged && newDocumentCount(data) === 0) {
    return 'Chưa có thay đổi cần thẩm định. Hãy chọn trình độ JLPT mới hoặc tải lên giấy tờ mới.';
  }
  return null;
}
