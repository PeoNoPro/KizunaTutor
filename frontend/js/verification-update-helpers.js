/**
 * KizunaTutor - Quy tắc cập nhật hồ sơ xác minh sau khi đã phê duyệt (dùng chung giao diện + test).
 * - Thông tin liên hệ (SĐT, địa chỉ, giới thiệu, kinh nghiệm) lưu ngay.
 * - Đổi JLPT / bổ sung giấy tờ / gỡ giấy tờ đã xác minh: gửi yêu cầu cập nhật để cán bộ học vụ thẩm định;
 *   hồ sơ vẫn "Đã phê duyệt". Giấy tờ đề nghị gỡ chỉ bị xóa khi cán bộ chấp nhận.
 * - Giấy tờ đã xác minh không sửa / gỡ trực tiếp; khi đang có yêu cầu chờ thẩm định thì khóa phần giấy tờ.
 */

/** Trường sửa được khi hồ sơ đã phê duyệt (họ tên, ngày sinh gắn với giấy tờ tùy thân; JLPT đổi qua yêu cầu). */
export const APPROVED_EDITABLE_FIELDS = ['phone', 'address', 'experienceYears', 'bio'];

export const MSG_NO_CHANGES =
  'Chưa có thay đổi cần thẩm định. Hãy chọn trình độ JLPT mới, tải lên giấy tờ mới hoặc chọn giấy tờ cần gỡ.';
export const MSG_REMOVE_KEEPS_REQUIRED =
  'Hồ sơ cần giữ ít nhất một giấy tờ tùy thân và một chứng chỉ năng lực tiếng Nhật. Hãy tải lên giấy tờ thay thế trước khi gỡ.';

/** FE-16: "HH:mm ngày dd/MM/yyyy" theo giờ máy người dùng; giá trị rỗng hoặc sai trả "—". */
export function formatResubmitTime(value) {
  const d = value ? new Date(value) : null;
  if (!d || Number.isNaN(d.getTime())) return '—';
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())} ngày ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

/**
 * FE-16: lời nhắc cho hồ sơ bị từ chối. Hết thời gian chờ (editable) thì mời sửa và nộp lại với giấy tờ mới;
 * còn chờ thì nêu thời điểm được nộp lại.
 */
export function rejectedProfileHint(data) {
  if (data && data.editable) {
    return 'Hồ sơ đã bị từ chối trước đó. Bạn có thể sửa thông tin, tải lên giấy tờ mới thay cho giấy tờ không được chấp nhận và nộp lại hồ sơ.';
  }
  if (data && data.resubmitAvailableAt) {
    return `Hồ sơ không được chấp nhận. Bạn có thể chỉnh sửa và nộp lại hồ sơ từ ${formatResubmitTime(data.resubmitAvailableAt)}. Vui lòng xem lý do bên dưới để chuẩn bị.`;
  }
  return 'Hồ sơ không được chấp nhận. Vui lòng liên hệ bộ phận hỗ trợ nếu bạn cần trao đổi thêm.';
}

/** FE-16: ghi chú chỉ xem của hồ sơ bị từ chối còn trong thời gian chờ. */
export function rejectedReadOnlyNote(data) {
  return data && data.resubmitAvailableAt
    ? `Chỉ xem — được chỉnh sửa và nộp lại từ ${formatResubmitTime(data.resubmitAvailableAt)}`
    : 'Chỉ xem — hồ sơ đã bị từ chối';
}

const IDENTITY_TYPES = ['IDENTITY_DOCUMENT'];
const JAPANESE_TYPES = ['JLPT_CERTIFICATE', 'OTHER_JAPANESE_CERTIFICATE'];

export function hasPendingUpdate(data) {
  return Boolean(data && data.profileUpdate && data.profileUpdate.status === 'PENDING');
}

/** Được thêm giấy tờ mới: hồ sơ đang sửa được, hoặc đã phê duyệt và không có yêu cầu chờ. */
export function canUploadDocuments(data) {
  if (!data) return false;
  if (data.editable) return true;
  return Boolean(data.updatable) && !hasPendingUpdate(data);
}

/** Được đổi loại / thay tệp / gỡ trực tiếp một giấy tờ. */
export function documentChangeable(data, doc) {
  if (!data || !doc) return false;
  if (data.editable) return true;
  return canUploadDocuments(data) && doc.status !== 'VERIFIED';
}

/** Hồ sơ đã phê duyệt: giấy tờ đã xác minh chỉ gỡ được bằng cách đề nghị gỡ trong yêu cầu cập nhật. */
export function canRequestRemoval(data, doc) {
  return Boolean(data && doc && data.updatable && !hasPendingUpdate(data) && doc.status === 'VERIFIED');
}

/** Giấy tờ mới tải lên (chờ thẩm định, còn xem được tệp) sẽ được gửi kèm yêu cầu cập nhật. */
export function newDocumentCount(data) {
  return ((data && data.documents) || []).filter((d) => d.status === 'PENDING' && d.fileAvailable).length;
}

/**
 * Sau khi gỡ các giấy tờ removedIds, hồ sơ (giấy tờ đã xác minh còn lại + giấy tờ mới) còn đủ
 * giấy tờ tùy thân và chứng chỉ năng lực tiếng Nhật không. Trả về thông báo lỗi hoặc null.
 */
export function removalError(data, removedIds) {
  const removed = new Set(removedIds || []);
  if (removed.size === 0) return null;
  const remaining = ((data && data.documents) || []).filter((d) => !removed.has(d.id) && d.fileAvailable
    && (d.status === 'VERIFIED' || d.status === 'PENDING'));
  const has = (types) => remaining.some((d) => types.includes(d.type));
  return has(IDENTITY_TYPES) && has(JAPANESE_TYPES) ? null : MSG_REMOVE_KEEPS_REQUIRED;
}

/**
 * Kiểm tra trước khi gửi yêu cầu: phải đổi JLPT, có giấy tờ mới hoặc có giấy tờ đề nghị gỡ.
 * Trả về thông báo lỗi hoặc null.
 */
export function profileUpdateError(data, requestedJlpt, note, removedIds = []) {
  if ((note || '').trim().length > 1000) return 'Ghi chú tối đa 1.000 ký tự.';
  const current = (data && data.profile && data.profile.highestJlptLevel) || '';
  const jlptChanged = Boolean(requestedJlpt) && requestedJlpt !== current;
  const removedCount = (removedIds || []).length;
  if (!jlptChanged && newDocumentCount(data) === 0 && removedCount === 0) {
    return MSG_NO_CHANGES;
  }
  return removalError(data, removedIds);
}
