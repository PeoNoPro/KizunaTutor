/**
 * KizunaTutor - Dynamic Shell Navigation Router (sidebarRouter.js)
 * Chuyển trang mượt mà không reload toàn bộ màn hình, loại bỏ hiện tượng giật cục & chớp trắng.
 */

// Lớp CSS mục sidebar học viên (đồng bộ kiểu sidebar gia sư); learner-layout.js dùng chung hai hằng này.
export const LEARNER_NAV_BASE = 'nav-item flex items-center gap-3 px-3 py-2.5 rounded-lg text-[14px] leading-5 transition-colors no-underline';
export const LEARNER_NAV_ACTIVE = `${LEARNER_NAV_BASE} bg-[#007bb9] text-[#fdfcff] font-semibold shadow-sm`;
export const LEARNER_NAV_IDLE = `${LEARNER_NAV_BASE} text-[#3f4850] font-medium hover:bg-[#e2e7ff] hover:text-[#131b2e]`;

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
      l.className = LEARNER_NAV_IDLE;
      l.removeAttribute('aria-current');
    });

    const activeEl = activeLink || sidebar.querySelector(`a[href="${targetUrl}"], a[href="${targetUrl.split('?')[0]}"]`);
    if (activeEl) {
      activeEl.className = LEARNER_NAV_ACTIVE;
      activeEl.setAttribute('aria-current', 'page');
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

    // 5. Nạp các thẻ <style> hoặc <link rel="stylesheet"> của trang mới vào <head> nếu chưa có
    const targetStyles = doc.querySelectorAll('head style, head link[rel="stylesheet"]');
    targetStyles.forEach(s => {
      if (s.tagName.toLowerCase() === 'link') {
        const href = s.getAttribute('href');
        if (href && !document.querySelector(`head link[href="${href}"]`)) {
          const newLink = document.createElement('link');
          newLink.rel = 'stylesheet';
          newLink.href = href;
          document.head.appendChild(newLink);
        }
      } else if (s.tagName.toLowerCase() === 'style') {
        const styleId = s.id || ('page-style-' + Math.random().toString(36).substring(2, 9));
        if (!document.getElementById(styleId)) {
          const newStyle = document.createElement('style');
          newStyle.id = styleId;
          newStyle.textContent = s.textContent;
          document.head.appendChild(newStyle);
        }
      }
    });

    // 6. Cập nhật URL trong History (chỉ khi do người dùng click điều hướng, không phải popstate)
    if (pushHistory) {
      window.history.pushState({ targetUrl }, '', targetUrl);
    }
    currentPathname = new URL(targetUrl, window.location.origin).pathname;

    // 7. Thay thế phần ruột <main>
    currentMain.innerHTML = newMain.innerHTML;
    currentMain.className = newMain.className;
    currentMain.scrollTop = 0;

    // 8. Đồng bộ các Modal ngoài <main> nếu trang mới có định nghĩa
    document.querySelectorAll('body > [id*="modal"], body > [id*="Modal"]').forEach(el => {
      if (!el.closest('#learnerSidebar')) {
        el.remove();
      }
    });
    const newModals = doc.querySelectorAll('body > [id*="modal"], body > [id*="Modal"]');
    newModals.forEach(m => {
      document.body.appendChild(m.cloneNode(true));
    });

    // 9. Chạy các module script của trang mới
    const moduleScripts = doc.querySelectorAll('script[type="module"][src]');
    for (const script of moduleScripts) {
      const src = script.getAttribute('src');
      if (src && !src.includes('main.js') && !src.includes('learner-layout.js')) {
        const cleanSrc = src.split('?')[0];
        try {
          await import(`${cleanSrc}?t=${Date.now()}`);
        } catch (scriptErr) {
          console.debug('Lỗi nạp module trang mới:', scriptErr);
        }
      }
    }

    // 9. Kích hoạt sự kiện để trang mới khởi tạo DOM & Event Listeners
    document.dispatchEvent(new Event('DOMContentLoaded'));
    window.dispatchEvent(new Event('load'));

  } catch (err) {
    console.debug('Shell navigation fallback to full reload:', err);
    window.location.href = targetUrl;
  } finally {
    if (currentMain) {
      currentMain.style.opacity = '1';
    }
    isNavigating = false;
  }
}

function handlePopState(e) {
  const newPathname = window.location.pathname;
  // Nếu chỉ thay đổi query param, hash hoặc trạng thái nội bộ trong cùng một trang (pathname không đổi)
  // thì để cho module của trang hiện tại tự xử lý điều hướng nội bộ
  if (newPathname === currentPathname) {
    return;
  }
  currentPathname = newPathname;
  const targetUrl = window.location.pathname + window.location.search;
  navigateTo(targetUrl, null, false);
}
