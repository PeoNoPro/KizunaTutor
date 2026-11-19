/**
 * KizunaTutor - Hàm thuần cho trang "Bài đăng của tôi" (không đụng DOM, test được bằng node --test).
 * Giới hạn khớp TutorPostService (backend): tiêu đề 10–150, giới thiệu 50–3000,
 * bảng học phí ít nhất 1 mục, mỗi môn học / cấp độ một mức; mỗi mức có thời lượng một buổi (60 / 90 / 120 phút)
 * và giá một buổi 10.000–10.000.000 đ.
 */

export const POST_LIMITS = Object.freeze({
  titleMin: 10,
  titleMax: 150,
  descriptionMin: 50,
  descriptionMax: 3000,
  rateMin: 10000,
  rateMax: 10000000
});

/** Thời lượng một buổi gia sư được chọn (phút), khớp PostPriceItem.SESSION_MINUTES ở backend. */
export const SESSION_MINUTES = Object.freeze([60, 90, 120]);

/** Thời lượng mặc định khi dữ liệu cũ chưa có (khớp migration 024_price_items_session.sql). */
const DEFAULT_SESSION_MINUTES = 90;

/** Kiểm tra bảng học phí [{ level, sessionMinutes, pricePerSession }]; trả về danh sách lỗi tiếng Việt. */
export function validatePrices(prices) {
  const errors = [];
  if (!Array.isArray(prices) || prices.length === 0) {
    return ['Thêm ít nhất một mức học phí theo môn học hoặc cấp độ'];
  }
  if (prices.some((p) => !p.level)) errors.push('Chọn môn học hoặc cấp độ cho từng mức học phí');
  const levels = prices.map((p) => p.level).filter(Boolean);
  if (new Set(levels).size !== levels.length) errors.push('Mỗi môn học hoặc cấp độ chỉ được đặt một mức học phí');
  if (prices.some((p) => !SESSION_MINUTES.includes(Number(p.sessionMinutes)))) {
    errors.push('Chọn thời lượng mỗi buổi (60, 90 hoặc 120 phút) cho từng môn học');
  }
  const badRate = prices.some((p) => {
    const rate = p.pricePerSession === '' || p.pricePerSession == null ? NaN : Number(p.pricePerSession);
    return !(rate >= POST_LIMITS.rateMin && rate <= POST_LIMITS.rateMax);
  });
  if (badRate) errors.push('Học phí mỗi buổi từ 10.000 đ đến 10.000.000 đ');
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

/** Giá một giờ quy ra giá một buổi (dữ liệu cũ chỉ có giá theo giờ). */
function perSessionFromHourly(hourlyRate, minutes) {
  return hourlyRate == null || hourlyRate === '' ? '' : Math.round(Number(hourlyRate) * minutes / 60);
}

/**
 * Các dòng bảng giá để điền vào form từ nội dung bài đăng: { level, sessionMinutes, pricePerSession }.
 * Bài cũ chỉ có một mức giá chung (chưa có bảng giá): trả về một dòng chưa chọn môn, giá quy ra buổi 90 phút.
 */
export function priceRowsFromContent(content) {
  if (content && Array.isArray(content.prices) && content.prices.length > 0) {
    return content.prices.map((p) => {
      const minutes = p.sessionMinutes || DEFAULT_SESSION_MINUTES;
      return {
        level: p.level,
        sessionMinutes: minutes,
        pricePerSession: p.pricePerSession != null ? p.pricePerSession : perSessionFromHourly(p.hourlyRate, minutes)
      };
    });
  }
  if (content && content.hourlyRate != null) {
    return [{ level: '', sessionMinutes: DEFAULT_SESSION_MINUTES,
      pricePerSession: perSessionFromHourly(content.hourlyRate, DEFAULT_SESSION_MINUTES) }];
  }
  return [{ level: '', sessionMinutes: DEFAULT_SESSION_MINUTES, pricePerSession: '' }];
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
