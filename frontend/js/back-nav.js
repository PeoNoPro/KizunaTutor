/**
 * KizunaTutor - FE-06/FE-07 Nút "Quay lại" về đúng trang vừa đứng.
 * Lấy trang trước (document.referrer) nếu cùng web và không phải trang đăng nhập; không có thì dùng trang cha (fallback).
 * Hàm thuần nhận tham số để kiểm thử được.
 */

const SKIP = ['/login', '/register', '/forgot-password'];

/**
 * @param {string} fallback trang cha khi không biết trang trước (mở thẳng link, tải lại trang…)
 * @param {{referrer?: string, here?: {origin: string, pathname: string, search: string}}} [env]
 * @returns {string} đường dẫn (pathname + search) để quay lại
 */
export function previousPage(fallback, env = {}) {
  const here = env.here || (typeof window !== 'undefined' ? window.location : null);
  const referrer = env.referrer ?? (typeof document !== 'undefined' ? document.referrer : '');
  try {
    if (!here || !referrer) return fallback;
    const ref = new URL(referrer);
    if (ref.origin !== here.origin) return fallback;
    const path = ref.pathname.replace(/\/+$/, '') || '/';
    if (SKIP.some((p) => path.startsWith(p))) return fallback;
    // Cùng trang (vd tải lại) → không coi là "trang trước"
    if (path === (here.pathname.replace(/\/+$/, '') || '/') && ref.search === here.search) return fallback;
    return ref.pathname + ref.search;
  } catch (e) {
    return fallback;
  }
}

/** Tên dễ hiểu của một trang trong phần lớp học / bài tập để ghi trên nút "Quay lại …". */
export function pageLabel(url) {
  let u;
  try {
    u = new URL(url, 'http://x');
  } catch (e) {
    return 'Trang trước';
  }
  const path = u.pathname.replace(/\/+$/, '');
  const p = u.searchParams;
  if (path.startsWith('/tutor/classrooms') || path.startsWith('/pages/tutor-classrooms')) {
    if (p.get('space')) return 'Lớp học';
    if (p.get('view') === 'assignments') return 'Quản lý bài tập';
    if (p.get('view') === 'materials') return 'Tài liệu học tập';
    return 'Học viên của tôi';
  }
  if (path.includes('my-courses')) {
    if (p.get('space')) return 'Lớp học';
    if (p.get('view') === 'assignments') return 'Bài tập của tôi';
    if (p.get('view') === 'materials') return 'Tài liệu học tập';
    return 'Lớp học của tôi';
  }
  if (path.includes('exercise-take')) return 'Bài làm';
  if (path.includes('exercise-editor')) return 'Soạn bài';
  if (path.includes('tutor-schedule') || path.startsWith('/tutor/schedule')) return 'Lịch giảng dạy';
  if (path.includes('dashboard')) return 'Tổng quan';
  return 'Trang trước';
}
