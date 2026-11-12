/** Đặt trang hỗ trợ và chính sách của học viên vào cùng khung sidebar với dashboard học viên. */
import {fetchAPI} from '../api.js';
import {getLearnerSidebarHtml,getLearnerHeaderHtml,bindLearnerLayoutEvents} from '../../components/learner-layout/learner-layout.js';

/** Router của dashboard chỉ thay ruột <main>, nên trang có script khởi tạo riêng phải tải lại đầy đủ. */
export function reloadIfInsideLearnerShell() {
  if (!document.getElementById('learnerSidebar')) return false;
  location.reload();
  return true;
}

export function mountLearnerFrame(active, displayName) {
  if (document.getElementById('learnerSidebar')) return;
  // learner-frame-utilities.css: class Tailwind của sidebar học viên mới mà bản biên dịch sẵn chưa có (ô logo, mục đang chọn...).
  ['/styles/learner-frame.css', '/styles/learner-frame-utilities.css'].forEach(href => {
    if (document.querySelector(`link[href="${href}"]`)) return;
    const css = document.createElement('link'); css.rel = 'stylesheet'; css.href = href; document.head.append(css);
  });
  document.querySelectorAll('body > header, body > footer').forEach(node => node.remove());
  const main = document.querySelector('main');
  const column = document.createElement('div'); column.className = 'learner-frame-body';
  column.innerHTML = getLearnerHeaderHtml();
  main.replaceWith(column); column.append(main);
  const sidebar = document.createElement('template'); sidebar.innerHTML = getLearnerSidebarHtml(active);
  column.before(sidebar.content);
  document.body.classList.add('learner-frame');
  const name = displayName?.replace(/\(.*?\)/g, '').trim();
  if (name) {
    const label = document.getElementById('sidebarLearnerName'); label.textContent = name; label.title = name;
    document.getElementById('sidebarUserAvatar').textContent = name.split(/\s+/).slice(-2).map(word => word[0]).join('').toLocaleUpperCase('vi-VN');
  }
  bindLearnerLayoutEvents();
}

/** Khách và gia sư giữ nguyên header công khai của trang chính sách. */
export async function mountLearnerFrameIfLearner(active) {
  try {
    const account = await fetchAPI('/v1/tickets/me');
    if (account.viewRole === 'learner') mountLearnerFrame(active, account.displayName);
  } catch { /* Phiên khách không có khung học viên. */ }
}
