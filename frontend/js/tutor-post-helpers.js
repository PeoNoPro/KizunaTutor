/**
 * KizunaTutor - Hàm thuần cho trang "Bài đăng của tôi" (không đụng DOM, test được bằng node --test).
 * Giới hạn khớp TutorPostService (backend): tiêu đề 10–150, giới thiệu 50–3000,
 * bảng học phí ít nhất 1 mục, mỗi môn học / cấp độ một mức, mỗi mức 10.000–10.000.000 đ.
 */

export const POST_LIMITS = Object.freeze({
  titleMin: 10,
  titleMax: 150,
  descriptionMin: 50,
  descriptionMax: 3000,
  rateMin: 10000,
  rateMax: 10000000
});

/** Kiểm tra bảng học phí [{ level, hourlyRate }]; trả về danh sách lỗi tiếng Việt. */
export function validatePrices(prices) {
  const errors = [];
  if (!Array.isArray(prices) || prices.length === 0) {
    return ['Thêm ít nhất một mức học phí theo môn học hoặc cấp độ'];
  }
  if (prices.some((p) => !p.level)) errors.push('Chọn môn học hoặc cấp độ cho từng mức học phí');
  const levels = prices.map((p) => p.level).filter(Boolean);
  if (new Set(levels).size !== levels.length) errors.push('Mỗi môn học hoặc cấp độ chỉ được đặt một mức học phí');
  const badRate = prices.some((p) => {
    const rate = Number(p.hourlyRate);
    return !(rate >= POST_LIMITS.rateMin && rate <= POST_LIMITS.rateMax);
  });
  if (badRate) errors.push('Học phí mỗi giờ từ 10.000 đ đến 10.000.000 đ');
  return errors;
}

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
  return errors.concat(validatePrices(p?.prices));
}

/**
 * Các dòng bảng giá để điền vào form từ nội dung bài đăng.
 * Bài cũ chỉ có một mức giá chung (chưa có bảng giá): trả về một dòng chưa chọn môn, giữ nguyên mức giá.
 */
export function priceRowsFromContent(content) {
  if (content && Array.isArray(content.prices) && content.prices.length > 0) {
    return content.prices.map((p) => ({ level: p.level, hourlyRate: p.hourlyRate }));
  }
  if (content && content.hourlyRate != null) return [{ level: '', hourlyRate: content.hourlyRate }];
  return [{ level: '', hourlyRate: '' }];
}

/**
 * Bài đã duyệt (hoặc bản cập nhật bị từ chối nhưng vẫn còn bản đang hiển thị): gia sư được cập nhật,
 * nhưng phải bấm "Chỉnh sửa bài đăng" trước để tránh sửa nhầm.
 */
export function isUpdateMode(editable, post) {
  return Boolean(editable && post && (post.reviewStatus === 'APPROVED' || post.reviewStatus === 'REJECTED'));
}

/**
 * Nhãn nút gửi, hoặc null nếu không được gửi (bài đang chờ duyệt, bài đầu tiên bị từ chối).
 * "Gửi duyệt" lần đầu, "Gửi lại" khi được yêu cầu bổ sung, "Gửi bản cập nhật" khi sửa bài đã duyệt.
 */
export function submitLabel(editable, post) {
  if (!editable) return null;
  if (!post) return 'Gửi duyệt';
  return isUpdateMode(editable, post) ? 'Gửi bản cập nhật' : 'Gửi lại';
}
