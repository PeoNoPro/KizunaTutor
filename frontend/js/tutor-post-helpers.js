/**
 * KizunaTutor - Hàm thuần cho trang "Bài đăng của tôi" (không đụng DOM, test được bằng node --test).
 * Giới hạn khớp TutorPostService (backend): tiêu đề 10–150, giới thiệu 50–3000, học phí 10.000–10.000.000 đ.
 */

export const POST_LIMITS = Object.freeze({
  titleMin: 10,
  titleMax: 150,
  descriptionMin: 50,
  descriptionMax: 3000,
  rateMin: 10000,
  rateMax: 10000000
});

/** Danh sách lỗi (tiếng Việt) của bản nháp bài đăng; rỗng nếu hợp lệ. */
export function validatePostDraft(p) {
  const errors = [];
  const title = (p?.title || '').trim();
  const description = (p?.description || '').trim();
  if (title.length < POST_LIMITS.titleMin) errors.push('Tiêu đề tối thiểu 10 ký tự');
  if (title.length > POST_LIMITS.titleMax) errors.push('Tiêu đề tối đa 150 ký tự');
  if (description.length < POST_LIMITS.descriptionMin) errors.push('Giới thiệu tối thiểu 50 ký tự');
  if (description.length > POST_LIMITS.descriptionMax) errors.push('Giới thiệu tối đa 3.000 ký tự');
  if (!p?.teachingFormat) errors.push('Chọn hình thức dạy');
  const rate = Number(p?.hourlyRate);
  if (!(rate >= POST_LIMITS.rateMin && rate <= POST_LIMITS.rateMax)) errors.push('Học phí mỗi giờ từ 10.000 đ đến 10.000.000 đ');
  return errors;
}

/**
 * Gia sư chỉ được soạn/gửi khi chưa có bài hoặc cán bộ học vụ yêu cầu bổ sung.
 * Trả về nhãn nút gửi tương ứng, hoặc null nếu không được gửi (bài đang chờ duyệt, đã duyệt, bị từ chối).
 */
export function submitLabel(editable, post) {
  if (!editable) return null;
  return post ? 'Gửi lại' : 'Gửi duyệt';
}
