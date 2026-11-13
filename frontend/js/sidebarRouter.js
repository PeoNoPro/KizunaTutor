/**
 * KizunaTutor - Dynamic Shell Navigation Router (sidebarRouter.js)
 * Chuyển trang mượt mà không reload toàn bộ màn hình, loại bỏ hiện tượng giật cục & chớp trắng.
 */

// Lớp CSS mục sidebar học viên (đồng bộ kiểu sidebar gia sư); learner-layout.js dùng chung hai hằng này.
export const LEARNER_NAV_BASE = 'nav-item flex items-center gap-3 px-3 py-2.5 rounded-lg text-[14px] leading-5 transition-colors no-underline';
export const LEARNER_NAV_ACTIVE = `${LEARNER_NAV_BASE} bg-[#007bb9] text-[#fdfcff] font-semibold shadow-sm`;
export const LEARNER_NAV_IDLE = `${LEARNER_NAV_BASE} text-[#3f4850] font-medium hover:bg-[#e2e7ff] hover:text-[#131b2e]`;

// Lớp CSS mục menu con cấp 2 (Dropdown / Accordion sub-items)
export const LEARNER_SUBNAV_BASE = 'nav-sub-item flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px] leading-5 transition-colors no-underline';
export const LEARNER_SUBNAV_ACTIVE = `${LEARNER_SUBNAV_BASE} bg-[#007bb9] text-[#fdfcff] font-semibold shadow-sm`;
export const LEARNER_SUBNAV_IDLE = `${LEARNER_SUBNAV_BASE} text-[#555f6d] font-medium hover:bg-[#e2e7ff] hover:text-[#131b2e]`;

let isNavigating = false;
let currentPathname = typeof window !== 'undefined' ? window.location.pathname : '';

/**
 * Khởi tạo trình lắng nghe điều hướng mượt mà cho thanh Sidebar học viên
 */
export function initSidebarRouter() {
  if (typeof window === 'undefined') return;
  if (window.__kizunaSidebarRouterInitialized) return;
  window.__kizunaSidebarRouterInitialized = true;
  currentPathname = window.location.pathname;

  // Lắng nghe sự kiện click trên toàn bộ link của Sidebar
  document.addEventListener('click', handleSidebarLinkClick);

  // Lắng nghe nút Back/Forward của trình duyệt
  window.addEventListener('popstate', handlePopState);
}

async function handleSidebarLinkClick(e) {
  // Bỏ qua nếu là nút toggle mở/đóng dropdown menu (xử lý riêng trong learner-layout.js)
  if (e.target.closest('.nav-dropdown-toggle')) return;

  const link = e.target.closest('#learnerSidebar a[href], a[data-spa-link]');
  if (!link) return;

  const href = link.getAttribute('href');
  if (!href || href.startsWith('#') || href.startsWith('javascript:') || href.startsWith('mailto:') || href.startsWith('tel:')) {
    return;
  }

  // Bỏ qua nếu là link mở tab mới hoặc link ngoài domain
  if (link.target === '_blank' || href.startsWith('http://') || href.startsWith('https://')) {
    const currentOrigin = window.location.origin;
    if (!href.startsWith(currentOrigin)) return;
  }

  // Bỏ qua các trang độc lập (Full-page / Đầy đủ giao diện): Tìm gia sư, Yêu cầu kết nối, Trang chủ
  if (
    link.hasAttribute('data-no-spa') ||
    href.includes('/pages/tutor-discovery') ||
    href.includes('/tutors') ||
    href.includes('/pages/connection-requests') ||
    href.includes('/connection-requests') ||
    href.includes('/pages/home')
  ) {
    return;
  }

  const currentPath = window.location.pathname + window.location.search;
  const targetUrl = new URL(href, window.location.origin).pathname + new URL(href, window.location.origin).search;

  if (currentPath === targetUrl) {
    e.preventDefault();
    return;
  }

  e.preventDefault();
  navigateTo(targetUrl, link, true);
}

/**
 * Điều hướng mượt đến trang đích không làm giật khung Sidebar
 */
export async function navigateTo(targetUrl, activeLink = null, pushHistory = true) {
  if (isNavigating) return;
  isNavigating = true;

  const currentMain = document.querySelector('main');
  const sidebar = document.getElementById('learnerSidebar');

  // 1. Cập nhật trạng thái Active trên Sidebar ngay lập tức (Zero Latency)
  if (sidebar) {
    const allLinks = sidebar.querySelectorAll('a[data-menu-key]');
    allLinks.forEach(l => {
      const isSub = l.classList.contains('nav-sub-item');
      l.className = isSub ? LEARNER_SUBNAV_IDLE : LEARNER_NAV_IDLE;
      l.removeAttribute('aria-current');
    });

    const activeEl = activeLink || sidebar.querySelector(`a[href="${targetUrl}"], a[href="${targetUrl.split('?')[0]}"]`);
    if (activeEl) {
      const isSub = activeEl.classList.contains('nav-sub-item');
      activeEl.className = isSub ? LEARNER_SUBNAV_ACTIVE : LEARNER_NAV_ACTIVE;
      activeEl.setAttribute('aria-current', 'page');

      // Tự động mở dropdown cha nếu menu con đang active
      const parentDropdown = activeEl.closest('.nav-dropdown-menu');
      if (parentDropdown) {
        parentDropdown.classList.remove('hidden');
        const parentBtn = parentDropdown.previousElementSibling;
        if (parentBtn && parentBtn.classList.contains('nav-dropdown-toggle')) {
          parentBtn.setAttribute('aria-expanded', 'true');
          parentBtn.classList.add('text-[#007bb9]', 'bg-[#f0f4ff]');
          parentBtn.classList.remove('text-[#3f4850]');
          const chevron = parentBtn.querySelector('[data-chevron]');
          if (chevron) chevron.classList.add('rotate-180');
        }
      }
    }

    // Đóng sidebar nếu đang mở trên mobile
    const backdrop = document.getElementById('learnerSidebarBackdrop');
    if (backdrop && !backdrop.classList.contains('hidden')) {
      sidebar.classList.add('-translate-x-full');
      backdrop.classList.add('hidden');
      document.body.classList.remove('overflow-hidden', 'md:overflow-auto');
    }
  }

  // 2. Thêm hiệu ứng mờ nhẹ vùng nội dung chính (chống chớp trắng và giật giụa)
  if (currentMain) {
    currentMain.style.transition = 'opacity 0.1s ease-out';
    currentMain.style.opacity = '0.5';
  }

  try {
    // 3. Tải HTML trang đích
    const res = await fetch(targetUrl, {
      headers: { 'X-Requested-With': 'KizunaShell' }
    });

    if (!res.ok) {
      window.location.href = targetUrl;
      return;
    }

    const htmlText = await res.text();
    const parser = new DOMParser();
    const doc = parser.parseFromString(htmlText, 'text/html');

    const newMain = doc.querySelector('main');
    if (!newMain || !currentMain) {
      // Fallback nếu trang đích không cùng layout
      window.location.href = targetUrl;
      return;
    }

    // 4. Đồng bộ tiêu đề trang
    if (doc.title) {
      document.title = doc.title;
    }

    // 5. Thay thế nội dung main
    currentMain.innerHTML = newMain.innerHTML;
    currentMain.className = newMain.className;

    // 6. Cập nhật URL trình duyệt
    if (pushHistory) {
      window.history.pushState({ kizunaPath: targetUrl }, '', targetUrl);
    }
    currentPathname = targetUrl;

    // 7. Nạp và thực thi script trang đích (Dynamic Module Loading)
    const scripts = Array.from(doc.querySelectorAll('script'));
    for (const s of scripts) {
      const src = s.getAttribute('src');
      if (src && (src.includes('/pages/') || src.includes('.js'))) {
        // Tránh nạp lại các script hệ thống dùng chung đã có trên window
        if (src.includes('sidebarRouter.js') || src.includes('learner-layout.js')) {
          continue;
        }

        try {
          const bustSrc = src.includes('?') ? `${src}&_ts=${Date.now()}` : `${src}?_ts=${Date.now()}`;
          await import(bustSrc);
        } catch (scriptErr) {
          console.debug('[SidebarRouter] Tái kích hoạt script trang đích:', src, scriptErr.message);
        }
      }
    }

    // 8. Kích hoạt sự kiện kizuna:page-mounted cho trang đích lắng nghe
        // Bổ sung dispatch DOMContentLoaded để các trang SPA có thể khởi tạo lại các event listener
    document.dispatchEvent(new Event('DOMContentLoaded'));

    window.dispatchEvent(new CustomEvent('kizuna:page-mounted', {
      detail: { url: targetUrl }
    }));

    // Cuộn mượt lên đầu trang
    currentMain.scrollTop = 0;
    window.scrollTo({ top: 0, behavior: 'instant' });

  } catch (err) {
    console.warn('[SidebarRouter] Fallback sang điều hướng trình duyệt tiêu chuẩn:', err);
    window.location.href = targetUrl;
  } finally {
    if (currentMain) {
      currentMain.style.opacity = '1';
    }
    isNavigating = false;
  }
}

/**
 * Xử lý nút Back/Forward trên trình duyệt
 */
function handlePopState(e) {
  const targetUrl = window.location.pathname + window.location.search;
  navigateTo(targetUrl, null, false);
}

export default {
  initSidebarRouter,
  navigateTo,
  LEARNER_NAV_ACTIVE,
  LEARNER_NAV_IDLE,
  LEARNER_SUBNAV_ACTIVE,
  LEARNER_SUBNAV_IDLE
};
