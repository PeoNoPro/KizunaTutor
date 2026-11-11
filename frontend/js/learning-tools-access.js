/**
 * KizunaTutor - Khóa công cụ học khi học viên chưa được gia sư thêm vào lớp học.
 *
 * Quy tắc (backend kiểm tra lại ở LearningToolAccessWebConfig, giao diện chỉ để hướng dẫn):
 * - Mở khóa khi học viên có ít nhất một lớp ACTIVE hoặc PAUSED.
 * - Luôn mở: Tra cứu chữ Hán, Tra cứu viết tay, Đấu trường gõ Kana.
 * - Bị khóa: Từ vựng, Ngữ pháp & AI Sensei, Shodo, Chữa câu AI, Bộ thẻ (flashcard), Game Mưa chữ Hán.
 *
 * Được learner-layout cài một lần: khóa mục trên sidebar, chặn bấm vào link công cụ bị khóa,
 * và thay nội dung <main> bằng màn hình hướng dẫn khi mở thẳng đường dẫn công cụ.
 */

import { API } from './api.js';
import { LOCKED_MENU_KEYS, isLockedToolPath } from './learning-tools-paths.js';

export { LOCKED_MENU_KEYS, isLockedToolPath, normalizePath } from './learning-tools-paths.js';

const LOCK_COPY = {
  // Học viên đã đăng nhập nhưng chưa thuộc lớp nào
  noClass: {
    title: 'Công cụ này mở khóa khi bạn vào lớp học',
    text: 'Hãy kết nối với một gia sư. Khi gia sư thêm bạn vào lớp, toàn bộ công cụ học tập sẽ được mở.',
    actions: [
      { href: '/tutors', icon: 'person_search', label: 'Tìm gia sư', primary: true },
      { href: '/learner/connections', icon: 'calendar_month', label: 'Yêu cầu kết nối của tôi' }
    ]
  },
  // Khách chưa đăng nhập (hoặc phiên đã hết hạn)
  login: {
    title: 'Đăng nhập để dùng công cụ này',
    text: 'Công cụ học dành cho học viên đã được gia sư thêm vào lớp. Hãy đăng nhập hoặc tạo tài khoản học viên để bắt đầu.',
    actions: [
      { href: '/login', icon: 'login', label: 'Đăng nhập', primary: true },
      { href: '/register', icon: 'person_add', label: 'Tạo tài khoản' }
    ]
  }
};
const CACHE_MS = 60 * 1000;

let cached = null;
let cachedAt = 0;
let installed = false;

/**
 * Trạng thái mở khóa (lưu tạm 1 phút). Khách chưa đăng nhập: allowed = false, loginRequired = true.
 * Lỗi mạng/máy chủ thì coi như mở để không chặn nhầm; backend vẫn tự trả 401/403.
 */
export async function getLearningToolAccess({ refresh = false } = {}) {
  if (!refresh && cached && Date.now() - cachedAt < CACHE_MS) return cached;
  try {
    cached = await API.learningTools.getAccess();
    cachedAt = Date.now();
  } catch (err) {
    cached = { allowed: true, openClassCount: 0, loginRequired: false };
    cachedAt = 0;
  }
  return cached;
}

// ----------------------------------------------------------------- Giao diện

function buildLockPanel({ compact = false, loginRequired = false } = {}) {
  const copy = loginRequired ? LOCK_COPY.login : LOCK_COPY.noClass;
  const panel = document.createElement('section');
  panel.setAttribute('data-learning-tools-lock', '');
  panel.className = compact
    ? 'w-full max-w-md bg-white rounded-2xl p-6 shadow-xl text-center'
    : 'w-full max-w-xl mx-auto my-10 bg-white rounded-2xl p-8 border border-[#E2E8F0] shadow-sm text-center';
  panel.innerHTML = `
    <div class="mx-auto w-14 h-14 rounded-2xl bg-[#eaedff] text-[#006194] flex items-center justify-center">
      <span class="material-symbols-outlined text-[30px]">lock</span>
    </div>
    <h2 class="mt-4 text-[18px] leading-7 font-semibold text-[#0F2545]"></h2>
    <p class="mt-2 text-[14px] leading-[22px] text-[#3f4850]"></p>
    <div data-lock-actions class="mt-5 flex flex-wrap justify-center gap-2"></div>
    <p class="mt-5 pt-4 border-t border-[#eaedff] text-[13px] leading-5 text-[#3f4850]">
      Bạn vẫn dùng được miễn phí:
      <a href="/kanji-dictionary" class="text-[#006194] font-medium">Tra cứu chữ Hán</a>,
      <a href="/handwriting" class="text-[#006194] font-medium">Tra cứu viết tay</a> và
      <a href="/kana" class="text-[#006194] font-medium">Đấu trường gõ Kana</a>.
    </p>
  `;
  panel.querySelector('h2').textContent = copy.title;
  panel.querySelector('p').textContent = copy.text;
  const actions = panel.querySelector('[data-lock-actions]');
  copy.actions.forEach((action) => {
    const link = document.createElement('a');
    link.href = action.href;
    link.setAttribute('data-no-spa', '');
    link.className = action.primary
      ? 'inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#006194] text-white text-[14px] font-medium hover:bg-[#007bb9] no-underline'
      : 'inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-[#eaedff] text-[#006194] text-[14px] font-medium hover:bg-[#e2e7ff] no-underline';
    const icon = document.createElement('span');
    icon.className = 'material-symbols-outlined text-[18px]';
    icon.textContent = action.icon;
    const label = document.createElement('span');
    label.textContent = action.label;
    link.append(icon, label);
    actions.appendChild(link);
  });
  return panel;
}

/** Thay nội dung <main> bằng màn hình hướng dẫn (giữ footer nếu có). */
export function renderLockedMain(access = cached || {}, main = document.querySelector('main')) {
  if (!main) return;
  main.querySelector('[data-learning-tools-lock]')?.remove();
  const footer = main.querySelector('footer');
  main.replaceChildren(buildLockPanel({ loginRequired: Boolean(access.loginRequired) }));
  if (footer) main.appendChild(footer);
}

function closeLockModal() {
  document.getElementById('learningToolsLockModal')?.remove();
}

/** Hộp thoại khi bấm vào công cụ bị khóa (không chuyển trang). */
export function showLockModal(access = cached || {}) {
  closeLockModal();
  const overlay = document.createElement('div');
  overlay.id = 'learningToolsLockModal';
  overlay.className = 'fixed inset-0 z-[60] bg-black/40 flex items-center justify-center p-4';
  overlay.setAttribute('role', 'dialog');
  overlay.setAttribute('aria-modal', 'true');
  const panel = buildLockPanel({ compact: true, loginRequired: Boolean(access.loginRequired) });
  const close = document.createElement('button');
  close.type = 'button';
  close.className = 'mt-4 text-[13px] text-[#707881] hover:text-[#131b2e]';
  close.textContent = 'Để sau';
  close.addEventListener('click', closeLockModal);
  panel.appendChild(close);
  overlay.appendChild(panel);
  overlay.addEventListener('click', (e) => { if (e.target === overlay) closeLockModal(); });
  document.body.appendChild(overlay);
}

/** Thêm biểu tượng khóa vào các mục công cụ trên sidebar. */
export function markSidebarLocks(allowed) {
  const sidebar = document.getElementById('learnerSidebar');
  if (!sidebar) return;
  LOCKED_MENU_KEYS.forEach((key) => {
    const link = sidebar.querySelector(`a[data-menu-key="${key}"]`);
    if (!link) return;
    link.querySelector('[data-lock-icon]')?.remove();
    link.toggleAttribute('data-tool-locked', !allowed);
    if (!allowed) {
      const icon = document.createElement('span');
      icon.setAttribute('data-lock-icon', '');
      icon.className = 'material-symbols-outlined text-[16px] ml-auto shrink-0 opacity-70';
      icon.textContent = 'lock';
      icon.title = 'Mở khóa khi bạn vào lớp học';
      link.appendChild(icon);
    }
  });
}

/** Banner trạng thái trên dashboard (container có id learningToolsAccessBanner). */
export function renderAccessBanner(container, access) {
  if (!container) return;
  container.replaceChildren();
  container.classList.remove('hidden');
  const box = document.createElement('div');
  const icon = document.createElement('span');
  icon.className = 'material-symbols-outlined text-[22px] shrink-0';
  const text = document.createElement('p');
  text.className = 'text-[14px] leading-[22px] flex-1 min-w-0';
  if (access.allowed) {
    box.className = 'w-full rounded-2xl px-5 py-3.5 bg-[#059669]/10 text-[#047857] flex items-center gap-3';
    icon.textContent = 'lock_open';
    text.textContent = access.openClassCount > 0
      ? `Bạn đang học ${access.openClassCount} lớp, toàn bộ công cụ học tập đã được mở.`
      : 'Toàn bộ công cụ học tập đã được mở.';
    box.append(icon, text);
  } else {
    box.className = 'w-full rounded-2xl px-5 py-3.5 bg-[#eaedff] text-[#004b73] flex flex-wrap items-center gap-3';
    icon.textContent = 'lock';
    text.textContent = 'Kết nối với gia sư để mở khóa Bộ thẻ, Ngữ pháp, Từ vựng, Shodo và Chữa câu AI.';
    const cta = document.createElement('a');
    cta.href = '/tutors';
    cta.setAttribute('data-no-spa', '');
    cta.className = 'px-4 py-2 rounded-xl bg-[#006194] text-white text-[13px] font-medium hover:bg-[#007bb9] no-underline';
    cta.textContent = 'Tìm gia sư';
    box.append(icon, text, cta);
  }
  container.appendChild(box);
}

// ----------------------------------------------------------------- Cài đặt

async function guardCurrentPage() {
  const access = await getLearningToolAccess();
  markSidebarLocks(access.allowed);
  if (!access.allowed && isLockedToolPath(window.location.pathname)) {
    renderLockedMain(access);
  }
  return access;
}

/** Bắt bấm vào link công cụ bị khóa (sidebar hoặc nút trong trang) trước khi router SPA xử lý. */
async function handleLockedLinkClick(event) {
  const link = event.target.closest?.('a[href]');
  if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey) return;
  let url;
  try {
    url = new URL(link.getAttribute('href'), window.location.origin);
  } catch (e) {
    return;
  }
  if (url.origin !== window.location.origin || !isLockedToolPath(url.pathname)) return;
  // Khách chưa đăng nhập: biết ngay, không cần chờ API.
  const access = hasToken() ? cached : { allowed: false, loginRequired: true };
  // Đã đăng nhập: chỉ chặn khi đã có kết quả "bị khóa" trong bộ nhớ tạm; nếu lọt qua,
  // trang đích vẫn tự hiện màn hình khóa và backend vẫn trả 403.
  if (access && access.allowed === false) {
    event.preventDefault();
    event.stopImmediatePropagation();
    showLockModal(access);
  }
}

function hasToken() {
  try {
    return Boolean(localStorage.getItem('kizuna_token'));
  } catch (e) {
    return false;
  }
}

/** Gọi mỗi lần trang học viên khởi tạo; phần lắng nghe sự kiện chỉ cài một lần. */
export function initLearningToolsGuard() {
  if (typeof document === 'undefined') return Promise.resolve(null);
  if (!installed) {
    installed = true;
    document.addEventListener('click', handleLockedLinkClick, true);
    window.addEventListener('popstate', () => { guardCurrentPage(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeLockModal(); });
  }
  return guardCurrentPage();
}
