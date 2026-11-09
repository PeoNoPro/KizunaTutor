/**
 * Đặt trang hỗ trợ của gia sư vào cùng khung sidebar + thanh trên với dashboard gia sư
 * (components/tutor-layout), để chuyển từ dashboard sang "Yêu cầu hỗ trợ" không bị đổi giao diện.
 */
import {mountTutorLayout,setTutorIdentity} from '../../components/tutor-layout/tutor-layout.js';

export async function mountTutorFrame(displayName) {
  if (document.getElementById('tutorSidebar')) return;
  // Utilities and tutor theme are precompiled in support-utilities.css; never wait for a runtime CDN compiler.
  if (!document.querySelector('link[href="/styles/tutor-theme.css"]')) {
    const css = document.createElement('link'); css.rel = 'stylesheet'; css.href = '/styles/tutor-theme.css'; document.head.append(css);
  }
  document.querySelectorAll('body > header, body > footer').forEach(node => node.remove());
  document.body.classList.add('tutor-frame');
  const sidebar = document.createElement('div'); sidebar.id = 'tutorSidebar';
  const column = document.createElement('div'); column.className = 'lg:pl-72 flex flex-col min-h-screen tutor-frame-body';
  const topbar = document.createElement('div'); topbar.id = 'tutorTopbar';
  const main = document.querySelector('main');
  main.replaceWith(column); column.append(topbar, main);
  column.before(sidebar);
  mountTutorLayout('support');
  setTutorIdentity({ fullName: displayName?.replace(/\(.*?\)/g, '').trim() });
}
