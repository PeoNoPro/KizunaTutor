/**
 * KizunaTutor - Tổng quan học vụ: gộp hồ sơ xác thực gia sư (FE-03) và bài đăng tuyển học viên (FE-09)
 * thành một danh sách đơn (hàm thuần, dùng chung giao diện + test).
 */

/** Trạng thái lọc của Tổng quan → tham số trạng thái của từng API. */
export const STATUS_PARAMS = {
  PENDING: { verification: 'PENDING', post: 'PENDING' },
  NEEDS_INFO: { verification: 'ADDITIONAL_INFO_REQUIRED', post: 'NEEDS_INFO' },
  APPROVED: { verification: 'APPROVED', post: 'APPROVED' },
  REJECTED: { verification: 'REJECTED', post: 'REJECTED' },
  ALL: { verification: 'ALL', post: 'ALL' }
};

/** Trạng thái từng loại → nhóm màu chung trên bảng. */
const STATUS_TONE = {
  PENDING: 'PENDING',
  PROFILE_UPDATE: 'PENDING',
  ADDITIONAL_INFO_REQUIRED: 'NEEDS_INFO',
  NEEDS_INFO: 'NEEDS_INFO',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
};

export function toneOf(status) {
  return STATUS_TONE[status] || 'PENDING';
}

/** Một dòng hồ sơ xác thực (VerificationQueueItemResponse) → dòng chung. */
export function fromVerification(item) {
  const update = item.status === 'PROFILE_UPDATE';
  return {
    type: 'VERIFICATION',
    typeLabel: update ? 'Cập nhật hồ sơ' : 'Hồ sơ xác thực',
    id: item.tutorId,
    tutorName: item.fullName,
    contact: [item.email, item.phone].filter(Boolean).join(' · '),
    title: item.highestJlptLevel ? `JLPT ${item.highestJlptLevel}` : 'Chưa khai báo JLPT',
    subtitle: `Kinh nghiệm ${item.experienceYears ?? 0} năm`,
    submittedAt: item.submittedAt || null,
    status: item.status,
    tone: toneOf(item.status),
    statusLabel: item.statusLabel,
    href: `/academic/verifications/detail?tutorId=${encodeURIComponent(item.tutorId)}`,
    actionLabel: item.status === 'PENDING' || update ? 'Thẩm định' : 'Xem hồ sơ'
  };
}

/** Một dòng bài đăng (ReviewItemView) → dòng chung. Bài đăng xem / duyệt ở trang Duyệt bài đăng. */
export function fromPost(item) {
  const draft = item.draft || {};
  const isUpdate = item.reviewStatus === 'PENDING' && Boolean(item.published);
  return {
    type: 'POST',
    typeLabel: isUpdate ? 'Bài đăng (bản cập nhật)' : 'Bài đăng',
    id: item.postId,
    tutorName: item.tutorName,
    contact: item.tutorEmail || '',
    title: draft.title || 'Bài đăng chưa có tiêu đề',
    subtitle: [item.highestJlptLevel ? `JLPT ${item.highestJlptLevel}` : '', draft.teachingFormatLabel || ''].filter(Boolean).join(' · '),
    submittedAt: item.submittedAt || null,
    status: item.reviewStatus,
    tone: toneOf(item.reviewStatus),
    statusLabel: item.reviewStatusLabel,
    href: '/academic/posts',
    actionLabel: item.reviewStatus === 'PENDING' ? 'Duyệt bài' : 'Xem bài'
  };
}

/** Gộp hai danh sách, sắp theo ngày gửi (đơn chưa có ngày gửi luôn ở cuối). */
export function mergeRows(verifications, posts, sort = 'oldest') {
  const rows = [...(verifications || []).map(fromVerification), ...(posts || []).map(fromPost)];
  const time = (r) => {
    const t = Date.parse(r.submittedAt || '');
    return Number.isFinite(t) ? t : null;
  };
  return rows.sort((a, b) => {
    const ta = time(a);
    const tb = time(b);
    if (ta === null && tb === null) return 0;
    if (ta === null) return 1;
    if (tb === null) return -1;
    return sort === 'newest' ? tb - ta : ta - tb;
  });
}

/**
 * Số liệu 4 thẻ: cộng hai loại đơn, kèm chi tiết từng loại.
 * v = VerificationSummaryResponse (có thể null nếu lỗi), p = ReviewSummaryView của bài đăng (có thể null).
 */
export function kpiCounts(v, p) {
  const vs = v || {};
  const ps = p || {};
  const make = (tutors, posts) => ({ total: (tutors || 0) + (posts || 0), tutors: tutors || 0, posts: posts || 0 });
  return {
    pending: make((vs.pendingCount || 0) + (vs.profileUpdatePendingCount || 0), ps.pendingCount),
    needsInfo: make(vs.needsInfoCount, ps.needsInfoCount),
    approved: make(vs.approvedCount, ps.approvedCount),
    rejected: make(vs.rejectedCount, ps.rejectedCount)
  };
}

/** "2 hồ sơ · 1 bài đăng" (bỏ phần bằng 0; cả hai bằng 0 thì chuỗi rỗng). */
export function breakdownText(count) {
  const parts = [];
  if (count.tutors) parts.push(`${count.tutors} hồ sơ`);
  if (count.posts) parts.push(`${count.posts} bài đăng`);
  return parts.join(' · ');
}
