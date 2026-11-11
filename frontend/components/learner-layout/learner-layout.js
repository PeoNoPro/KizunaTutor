/**
 * KizunaTutor - Khung Giao Diện Học Viên Dùng Chung (Learner Shared Layout)
 * Cung cấp Sidebar, Header và Footer đồng bộ cho toàn bộ các màn hình của Học Viên
 * Tuân thủ nghiêm ngặt FRONTEND_RULES.md
 */

import { initSidebarRouter, LEARNER_NAV_ACTIVE, LEARNER_NAV_IDLE } from '/js/sidebarRouter.js';
import { brandLogoHtml } from '/components/brand-logo/brand-logo.js';
import { initLearningToolsGuard } from '/js/learning-tools-access.js';

// =========================================================================
// 1. CẤU HÌNH DANH MỤC MENU HỌC VIÊN
// =========================================================================
export const LEARNER_MENU_GROUPS = [
  {
    groupTitle: 'Bàn học tập',
    items: [
      {
        key: 'dashboard',
        title: 'Tổng quan học tập',
        url: '/learner/dashboard',
        icon: 'grid_view',
        matcher: ['/learner/dashboard', '/learner', '/dashboard']
      },
      {
        // FE-02: học viên xem và cập nhật hồ sơ cá nhân. Mở từ menu tài khoản, không hiện trong danh sách sidebar.
        key: 'profile',
        title: 'Hồ sơ cá nhân',
        url: '/pages/learner-profile/',
        icon: 'person',
        matcher: ['/pages/learner-profile'],
        hiddenInSidebar: true
      }
    ]
  },
  {
    groupTitle: 'Gia sư & lớp học',
    items: [
      {
        key: 'requests',
        title: 'Lịch học của tôi',
        url: '/learner/connections',
        icon: 'calendar_month',
        matcher: ['/learner/connections', '/learner/bookings', '/pages/learner-connections']
      },
      {
        // Yêu cầu học thử và kết nối trực tiếp học viên đã gửi tới gia sư
        key: 'my-requests',
        title: 'Yêu cầu của tôi',
        url: '/pages/learner-requests/',
        icon: 'pending_actions',
        matcher: ['/learner/requests', '/pages/learner-requests', '/connection-requests', '/pages/connection-requests']
      },
      {
        key: 'my-courses',
        title: 'Lớp học của tôi',
        url: '/pages/my-courses/',
        icon: 'school',
        matcher: ['/my-courses', '/pages/my-courses']
      }
    ]
  },
  {
    groupTitle: 'Học tập & rèn luyện',
    items: [
      {
        key: 'kanji-dictionary',
        title: 'Tra cứu chữ Hán',
        url: '/kanji-dictionary',
        icon: 'menu_book',
        matcher: ['/kanji-dictionary', '/dictionary', '/kanji']
      },
      {
        key: 'handwriting',
        title: 'Tra cứu viết tay',
        url: '/handwriting',
        icon: 'draw',
        matcher: ['/handwriting', '/handwriting-search', '/ohr']
      },
      {
        key: 'learning-support',
        title: 'Tra cứu từ vựng',
        url: '/vocabulary',
        icon: 'translate',
        matcher: ['/vocabulary', '/vocab', '/learning-support', '/learner/learning-support', '/learner/vocabulary']
      },
      {
        key: 'grammar',
        title: 'Ngữ pháp & AI Sensei',
        url: '/grammar',
        icon: 'auto_stories',
        matcher: ['/grammar', '/openjlpt']
      },
      {
        key: 'shodo',
        title: 'Phòng luyện viết Shodo',
        url: '/shodo',
        icon: 'brush',
        matcher: ['/shodo', '/shodo-practice', '/kanji-practice']
      },
      {
        key: 'kana',
        title: 'Đấu trường gõ Kana',
        url: '/kana',
        icon: 'keyboard',
        matcher: ['/kana', '/kana-quiz']
      },
      {
        key: 'ai-writing',
        title: 'Chữa câu & Viết AI',
        url: '/ai-writing',
        icon: 'edit_note',
        matcher: ['/ai-writing', '/sakubun']
      },
      {
        key: 'flashcards',
        title: 'Bộ thẻ của tôi',
        url: '/flashcards',
        icon: '🗂️',
        matcher: ['/flashcards', '/decks', '/learner/flashcards', '/learner/deck-detail', '/deck-detail']
      }
    ]
  },
  {
    groupTitle: 'Hỗ trợ',
    items: [
      {
        key: 'support',
        title: 'Yêu cầu hỗ trợ',
        url: '/learner/support/tickets',
        icon: 'support_agent',
        matcher: ['/learner/support/tickets', '/pages/support-ticket']
      },
      {
        key: 'policies',
        title: 'Chính sách',
        url: '/learner/policies',
        icon: 'policy',
        matcher: ['/learner/policies', '/pages/polic']
      }
    ]
  }
];

// =========================================================================
// 2. HELPER: XÁC ĐỊNH MENU ACTIVE DỰA VÀO ĐƯỜNG DẪN HIỆN TẠI
// =========================================================================
export const PAGE_META_MAP = {
  'dashboard': { title: 'Tổng quan học tập', badge: 'Bàn học tập' },
  'profile': { title: 'Hồ sơ cá nhân', badge: 'Bàn học tập' },
  'tutors': { title: 'Tìm gia sư Kizuna', badge: 'Gia sư & lớp học' },
  'requests': { title: 'Thời khóa biểu & Lịch học', badge: 'Gia sư & lớp học' },
  'my-courses': { title: 'Lớp học của tôi', badge: 'Gia sư & lớp học' },
  'kanji-dictionary': { title: 'Tra cứu chữ Hán', badge: 'Học tập & rèn luyện' },
  'handwriting': { title: 'Tra cứu chữ Hán (Viết tay)', badge: 'Học tập & rèn luyện' },
  'shodo': { title: 'Phòng luyện viết Shodo', badge: 'Học tập & rèn luyện' },
  'kana': { title: 'Đấu trường gõ Kana', badge: 'Học tập & rèn luyện' },
  'flashcards': { title: 'Bộ thẻ của tôi', badge: 'Học tập & rèn luyện' },
  'grammar': { title: 'Ngữ pháp & AI Sensei', badge: 'Học tập & rèn luyện' },
  'ai-writing': { title: 'Luyện viết & Chữa câu AI', badge: 'Học tập & rèn luyện' },
  'learning-support': { title: 'Tra cứu từ vựng', badge: 'Học tập & rèn luyện' },
  'vocabulary': { title: 'Tra cứu từ vựng', badge: 'Học tập & rèn luyện' },
  'kanji-game': { title: 'Game Mưa chữ Hán', badge: 'Học tập & rèn luyện' }
};

export function detectActiveMenu(customKey = null) {
  if (customKey) return customKey;
  const currentPath = window.location.pathname.replace(/\/+$/, '') || '/';
  
  for (const group of LEARNER_MENU_GROUPS) {
    for (const item of group.items) {
      if (item.key === customKey) return item.key;
      if (item.url.replace(/\/+$/, '') === currentPath) return item.key;
      if (item.matcher && item.matcher.some(m => currentPath.startsWith(m))) {
        return item.key;
      }
    }
  }
  return 'dashboard';
}

// =========================================================================
// 3. HELPER: LẤY THÔNG TIN HỌC VIÊN
// =========================================================================
export function getLearnerProfile() {
  try {
    const raw = localStorage.getItem('kizuna_user');
    const user = raw ? JSON.parse(raw) : null;
    const name = user?.fullName || user?.name || user?.email?.split('@')[0] || 'Học viên';
    const initials = name
      .split(' ')
      .filter(Boolean)
      .slice(-2)
      .map(part => part[0]?.toUpperCase())
      .join('') || 'HV';
    return { name, initials, email: user?.email || '', roleName: 'Học viên' };
  } catch (e) {
    return { name: 'Học viên', initials: 'HV', email: '', roleName: 'Học viên' };
  }
}

// =========================================================================
// 4. RENDER HTML SIDEBAR HỌC VIÊN (ĐỒNG BỘ KIỂU SIDEBAR GIA SƯ — components/tutor-layout)
// =========================================================================
// Màu viết bằng mã hex (khớp components/tutor-layout/tailwind-theme.js) vì các trang học viên
// không nạp theme Tailwind của gia sư; nhờ vậy sidebar hiển thị giống nhau trên mọi trang.

function escapeAttr(text) {
  return String(text == null ? '' : text)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/** Nạp phông Plus Jakarta Sans và bộ icon Material Symbols nếu trang chưa có (sidebar cần cả hai). */
export function ensureLearnerLayoutAssets() {
  if (typeof document === 'undefined') return;
  const fonts = [
    ['learnerFontJakarta', 'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap'],
    ['learnerFontSymbols', 'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200']
  ];
  fonts.forEach(([id, href]) => {
    if (document.getElementById(id)) return;
    if (id === 'learnerFontSymbols' && document.querySelector('link[href*="Material+Symbols+Outlined"]')) return;
    const link = document.createElement('link');
    link.id = id;
    link.rel = 'stylesheet';
    link.href = href;
    document.head.appendChild(link);
  });
}

export function getLearnerSidebarHtml(activeKey) {
  ensureLearnerLayoutAssets();
  const currentActive = detectActiveMenu(activeKey);
  const profile = getLearnerProfile();

  const menuSectionsHtml = LEARNER_MENU_GROUPS.map(group => {
    const itemsHtml = group.items.filter(item => !item.hiddenInSidebar).map(item => {
      const isActive = item.key === currentActive;
      return `
        <a href="${item.url}"
           data-menu-key="${item.key}"
           ${(item.key === 'tutors' || item.key === 'requests') ? 'data-no-spa' : 'data-spa-link'}
           class="${isActive ? LEARNER_NAV_ACTIVE : LEARNER_NAV_IDLE}" ${isActive ? 'aria-current="page"' : ''}>
          <span class="material-symbols-outlined text-[20px] shrink-0">${item.icon}</span>
          <span class="truncate">${item.title}</span>
        </a>
      `;
    }).join('');

    return `
      <div class="flex flex-col gap-1">
        <div class="px-3 pt-1 pb-1 text-[11px] leading-[14px] font-semibold text-[#707881] uppercase tracking-wider">${group.groupTitle}</div>
        ${itemsHtml}
      </div>
    `;
  }).join('');

  return `
    <!-- Learner Sidebar (cùng kiểu sidebar khu vực gia sư) -->
    <aside id="learnerSidebar" style="font-family: 'Plus Jakarta Sans', system-ui, sans-serif;"
           class="w-[270px] min-w-[260px] max-w-[280px] bg-white shadow-[0_1px_8px_rgba(0,0,0,0.04)] h-screen sticky top-0 flex flex-col justify-between shrink-0 z-40 transition-transform duration-200 ease-in-out -translate-x-full md:translate-x-0 select-none fixed md:static inset-y-0 left-0 overflow-hidden">

      <div class="flex flex-col min-h-0 flex-1">
        <!-- Logo -->
        <div class="px-6 pt-6 pb-4 flex items-start justify-between">
          ${brandLogoHtml({ href: '/home', subtitle: 'Không gian học viên' })}
          <button type="button" id="btnCloseSidebarMobile" class="md:hidden p-1.5 rounded-lg text-[#707881] hover:text-[#131b2e] hover:bg-[#eaedff] transition-colors" title="Đóng menu" aria-label="Đóng menu">
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <!-- Danh sách menu -->
        <nav class="flex flex-col gap-3 px-4 mt-1 pb-2 flex-1 overflow-y-auto scrollbar-none" aria-label="Điều hướng học viên">
          ${menuSectionsHtml}
        </nav>
      </div>

      <!-- Menu tài khoản ở góc dưới bên trái (giống gia sư) -->
      <div class="mx-4 my-4 relative" id="learnerUserMenu">
        <div id="learnerUserMenuPanel" role="menu" aria-label="Tài khoản"
             class="hidden absolute left-0 right-0 bottom-[calc(100%+8px)] rounded-xl bg-white shadow-lg ring-1 ring-black/5 p-1.5 flex-col z-50">
          <a href="/pages/learner-profile/" role="menuitem" class="flex items-center gap-2 px-3 py-2 text-[14px] font-medium rounded-lg text-[#131b2e] hover:bg-[#eaedff] no-underline">
            <span class="material-symbols-outlined text-[18px]">person</span><span>Hồ sơ cá nhân</span>
          </a>
          <a href="/home" data-no-spa role="menuitem" class="flex items-center gap-2 px-3 py-2 text-[14px] font-medium rounded-lg text-[#131b2e] hover:bg-[#eaedff] no-underline">
            <span class="material-symbols-outlined text-[18px]">home</span><span>Về trang chủ</span>
          </a>
          <div class="h-px bg-[#eaedff] my-1"></div>
          <button type="button" role="menuitem" id="btnLogoutSidebar" class="w-full flex items-center gap-2 px-3 py-2 text-[14px] font-medium rounded-lg text-[#ba1a1a] hover:bg-[#ffdad6] text-left">
            <span class="material-symbols-outlined text-[18px]">logout</span><span>Đăng xuất</span>
          </button>
        </div>
        <button id="learnerUserMenuButton" type="button" aria-haspopup="menu" aria-expanded="false"
                class="w-full flex items-center gap-3 p-3 rounded-xl bg-[#f2f3ff] hover:bg-[#eaedff] transition-colors text-left">
          <span id="sidebarUserAvatar" class="w-10 h-10 shrink-0 rounded-full bg-[#cce5ff] text-[#004b73] flex items-center justify-center text-[12px] font-semibold ring-2 ring-[#006194]/20">${escapeAttr(profile.initials)}</span>
          <span class="min-w-0 flex-1">
            <span id="sidebarLearnerName" class="block text-[14px] font-semibold text-[#131b2e] leading-tight truncate" title="${escapeAttr(profile.name)}">${escapeAttr(profile.name)}</span>
            <span class="block text-[11px] leading-[14px] font-medium text-[#3f4850] truncate">Học viên</span>
          </span>
          <span class="material-symbols-outlined text-[18px] text-[#707881] transition-transform" data-chevron>expand_less</span>
        </button>
      </div>
    </aside>

    <!-- Mobile Sidebar Backdrop Overlay -->
    <div id="learnerSidebarBackdrop" class="fixed inset-0 bg-black/30 z-30 hidden md:hidden transition-opacity"></div>
  `;
}

// =========================================================================
// 5. RENDER HTML HEADER HỌC VIÊN
// =========================================================================
export function getLearnerHeaderHtml(options = {}) {
  ensureLearnerLayoutAssets();
  return `
    <!-- Thanh trên chỉ hiện trên màn hình nhỏ để mở sidebar (giống khu vực gia sư) -->
    <header class="md:hidden h-14 bg-white/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.04)] sticky top-0 z-30 px-4 flex items-center gap-3" style="font-family: 'Plus Jakarta Sans', system-ui, sans-serif;">
      <button type="button" id="btnOpenSidebarMobile" class="p-2 rounded-lg text-[#3f4850] hover:bg-[#eaedff] transition-colors cursor-pointer" title="Mở menu" aria-label="Mở menu">
        <span class="material-symbols-outlined text-[22px]">menu</span>
      </button>
      <a href="/learner/dashboard" class="text-[20px] leading-7 font-semibold no-underline">
        <span class="text-[#0F2545]">Kizuna</span><span class="text-[#006194]">Tutor</span>
      </a>
    </header>
  `;
}

// =========================================================================
// 6. RENDER HTML FOOTER HỌC VIÊN
// =========================================================================
export function getLearnerFooterHtml() {
  return `
    <footer class="mt-auto pt-6 pb-6 border-t border-[#E2E8F0] text-center text-[13px] text-[#64748B] font-sans">
      KizunaTutor &copy; 2026 &bull; Đồng hành cùng bạn trên từng nét vẽ Kanji 🌸
    </footer>
  `;
}

// =========================================================================
// 7. BIND SỰ KIỆN: MOBILE SIDEBAR & ĐĂNG XUẤT
// =========================================================================
export function bindLearnerLayoutEvents() {
  const sidebar = document.getElementById('learnerSidebar');
  const backdrop = document.getElementById('learnerSidebarBackdrop');
  const btnOpen = document.getElementById('btnOpenSidebarMobile');
  const btnClose = document.getElementById('btnCloseSidebarMobile');
  const btnLogout = document.getElementById('btnLogoutSidebar');

  const openSidebar = () => {
    sidebar?.classList.remove('-translate-x-full');
    backdrop?.classList.remove('hidden');
    document.body.classList.add('overflow-hidden', 'md:overflow-auto');
  };

  const closeSidebar = () => {
    sidebar?.classList.add('-translate-x-full');
    backdrop?.classList.add('hidden');
    document.body.classList.remove('overflow-hidden', 'md:overflow-auto');
  };

  btnOpen?.addEventListener('click', openSidebar);
  btnClose?.addEventListener('click', closeSidebar);
  backdrop?.addEventListener('click', closeSidebar);

  // Xử lý phím ESC để đóng sidebar trên mobile
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !sidebar?.classList.contains('-translate-x-full')) {
      closeSidebar();
    }
  });

  // Menu tài khoản góc dưới trái: bấm để mở/đóng (giống sidebar gia sư)
  const menuRoot = document.getElementById('learnerUserMenu');
  const menuButton = document.getElementById('learnerUserMenuButton');
  const menuPanel = document.getElementById('learnerUserMenuPanel');
  if (menuRoot && menuButton && menuPanel) {
    const setMenuOpen = (open) => {
      menuPanel.classList.toggle('hidden', !open);
      menuPanel.classList.toggle('flex', open);
      menuButton.setAttribute('aria-expanded', open ? 'true' : 'false');
      menuButton.querySelector('[data-chevron]')?.classList.toggle('rotate-180', open);
    };
    menuButton.addEventListener('click', (e) => {
      e.stopPropagation();
      setMenuOpen(menuPanel.classList.contains('hidden'));
    });
    document.addEventListener('click', (e) => {
      if (!menuRoot.contains(e.target)) setMenuOpen(false);
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') setMenuOpen(false);
    });
  }

  // Xử lý Đăng xuất tập trung
  btnLogout?.addEventListener('click', () => {
    localStorage.removeItem('kizuna_token');
    localStorage.removeItem('kizuna_user');
    sessionStorage.clear();
    window.location.href = '/login';
  });
}

// =========================================================================
// 8. TỰ ĐỘNG KHỞI TẠO KHUNG BẢO VỆ TOÀN DIỆN (WRAPPER SHELL)
// =========================================================================
/**
 * Khóa rung giật thanh cuộn toàn cục và chống tràn màn hình ngang
 */
export function injectGlobalLayoutStyles() {
  if (typeof document === 'undefined') return;
  if (document.getElementById('kizunaGlobalLayoutStyles')) return;
  const styleEl = document.createElement('style');
  styleEl.id = 'kizunaGlobalLayoutStyles';
  styleEl.textContent = `
    /* 1. Khóa ổn định diện tích thanh cuộn - Chống giật khung ngang khi đổi trang */
    html {
      scrollbar-gutter: stable both-edges;
      box-sizing: border-box;
    }
    *, *::before, *::after {
      box-sizing: inherit;
    }
    body {
      margin: 0;
      padding: 0;
      overflow-x: hidden !important;
      background-color: #F8FAFC;
      min-height: 100vh;
    }

    /* 2. Dự trù diện tích cho Sidebar trước khi JS chạy để triệt tiêu 100% Layout Shift (CLS = 0) */
    #learnerSidebarContainer {
      width: 270px;
      min-width: 260px;
      max-width: 280px;
      flex-shrink: 0;
      height: 100vh;
    }
    @media (max-width: 767px) {
      #learnerSidebarContainer {
        display: none;
      }
    }

    /* 3. Ép trình duyệt tối ưu vẽ đồ họa GPU cho Sidebar */
    #learnerSidebar {
      transform: translateZ(0);
      backface-visibility: hidden;
      will-change: transform;
    }

    /* 4. Vùng nội dung chính cuộn độc lập, không làm ảnh hưởng thanh cuộn ngoài */
    main {
      contain: paint;
    }

    /* 5. Khử chớp nháy font chữ (FOIT) */
    * {
      font-display: swap;
    }

    .scrollbar-none::-webkit-scrollbar {
      display: none;
    }
    .scrollbar-none {
      -ms-overflow-style: none;
      scrollbar-width: none;
    }
  `;
  document.head.appendChild(styleEl);
}

/**
 * Tự động bọc toàn bộ nội dung của trang học viên vào bộ khung chung
 * @param {Object} options
 * @param {string} options.activeMenu - Key của menu đang mở (vd: 'kanji-dictionary', 'shodo', 'flashcards', 'kanji-game')
 * @param {string} options.pageTitle - Tiêu đề trang hiển thị trên header
 * @param {string} options.badge - Nhãn phụ (vd: 'Công cụ rèn luyện')
 * @param {string} options.containerId - ID vùng nội dung (mặc định 'learner-content' hoặc bọc body)
 * @param {string} options.rightContent - Nội dung HTML tùy biến bổ sung ở góc phải header
 */
export function initLearnerShell(options = {}) {
  // 0. Bổ sung style chống rung giật thanh cuộn & chống tràn ngay lập tức
  injectGlobalLayoutStyles();

  // Khởi tạo router điều hướng mượt mà không reload trang (Dynamic Shell Router)
  initSidebarRouter();

  // Khóa công cụ học khi học viên chưa vào lớp (chạy cả khi trang được nạp lại qua router SPA)
  initLearningToolsGuard();

  // Kiểm tra chống trùng lặp: Nếu sidebar đã được render, tuyệt đối không tạo thêm
  if (document.getElementById('learnerSidebar')) {
    return;
  }

  const activeMenu = options.activeMenu || detectActiveMenu();
  const pageTitle = options.pageTitle || '';
  const badge = options.badge || '';
  const rightContent = options.rightContent || '';

  // 1. Nếu trang đã có vùng placeholder cấu trúc sẵn
  const sidebarContainer = document.getElementById('learnerSidebarContainer');
  const headerContainer = document.getElementById('learnerHeaderContainer');
  const footerContainer = document.getElementById('learnerFooterContainer');

  if (sidebarContainer || headerContainer || footerContainer) {
    if (sidebarContainer) {
      const tempWrapper = document.createElement('div');
      tempWrapper.innerHTML = getLearnerSidebarHtml(activeMenu);
      while (tempWrapper.firstChild) {
        sidebarContainer.parentNode.insertBefore(tempWrapper.firstChild, sidebarContainer);
      }
      sidebarContainer.remove();
    }

    if (headerContainer) {
      const tempWrapper = document.createElement('div');
      tempWrapper.innerHTML = getLearnerHeaderHtml({ pageTitle, badge, rightContent });
      while (tempWrapper.firstChild) {
        headerContainer.parentNode.insertBefore(tempWrapper.firstChild, headerContainer);
      }
      headerContainer.remove();
    }

    if (footerContainer) {
      const tempWrapper = document.createElement('div');
      tempWrapper.innerHTML = getLearnerFooterHtml();
      while (tempWrapper.firstChild) {
        footerContainer.parentNode.insertBefore(tempWrapper.firstChild, footerContainer);
      }
      footerContainer.remove();
    }

    bindLearnerLayoutEvents();
    return;
  }

  // 2. Chỉ tự động bọc nếu được chỉ định rõ ràng qua wrapBody
  if (options.wrapBody) {
    const targetEl = document.getElementById(options.containerId) || document.querySelector('main') || document.body;
    const originalHtml = targetEl.innerHTML;
    const shellHtml = `
      <div class="flex h-screen overflow-hidden bg-white">
        ${getLearnerSidebarHtml(activeMenu)}
        <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
          ${getLearnerHeaderHtml({ pageTitle, badge, rightContent })}
          <main class="flex-1 overflow-y-auto p-4 sm:p-6 bg-white flex flex-col">
            <div class="max-w-[1240px] w-full mx-auto flex-1">
              ${originalHtml}
            </div>
            ${getLearnerFooterHtml()}
          </main>
        </div>
      </div>
    `;

    targetEl.innerHTML = shellHtml;
    bindLearnerLayoutEvents();
  }
}

// Alias thân thiện
export const mountLearnerLayout = initLearnerShell;

// =========================================================================
// 9. CUSTOM WEB COMPONENT: <kizuna-learner-layout>
// =========================================================================
if (typeof customElements !== 'undefined' && !customElements.get('kizuna-learner-layout')) {
  class KizunaLearnerLayoutElement extends HTMLElement {
    connectedCallback() {
      if (this.dataset.rendered === 'true') return;
      this.dataset.rendered = 'true';

      const activeMenu = this.getAttribute('active') || detectActiveMenu();
      const pageTitle = this.getAttribute('page-title') || '';
      const badge = this.getAttribute('badge') || '';
      const rightContent = this.getAttribute('right-content') || '';

      // Thu gom toàn bộ child nodes hiện có để bảo toàn DOM & Event Listener
      const fragment = document.createDocumentFragment();
      while (this.firstChild) {
        fragment.appendChild(this.firstChild);
      }

      // Tạo cấu trúc shell
      const wrapper = document.createElement('div');
      wrapper.className = 'flex h-screen overflow-hidden bg-white';
      wrapper.innerHTML = `
        ${getLearnerSidebarHtml(activeMenu)}
        <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
          ${getLearnerHeaderHtml({ pageTitle, badge, rightContent })}
          <main class="flex-1 overflow-y-auto p-4 sm:p-6 bg-white flex flex-col">
            <div id="learnerContentSlot" class="max-w-[1240px] w-full mx-auto flex-1 space-y-6"></div>
            ${getLearnerFooterHtml()}
          </main>
        </div>
      `;

      // Đưa các child nodes vào vùng slot
      const slot = wrapper.querySelector('#learnerContentSlot');
      if (slot) {
        slot.appendChild(fragment);
      }

      this.appendChild(wrapper);
      bindLearnerLayoutEvents();
      initLearningToolsGuard();
    }
  }

  customElements.define('kizuna-learner-layout', KizunaLearnerLayoutElement);
}

// =========================================================================
// 10. TỰ ĐỘNG KHỞI TẠO NẾU CÓ CÁC THẺ PLACEHOLDER TRÊN TRANG
// =========================================================================
export function autoInitLearnerLayout() {
  const sidebarContainer = document.getElementById('learnerSidebarContainer');
  const headerContainer = document.getElementById('learnerHeaderContainer');
  const footerContainer = document.getElementById('learnerFooterContainer');

  if (sidebarContainer || headerContainer || footerContainer) {
    const activeKey = detectActiveMenu();
    const meta = PAGE_META_MAP[activeKey] || { title: 'Không gian học viên', badge: 'Học tập' };
    initLearnerShell({
      activeMenu: activeKey,
      pageTitle: meta.title,
      badge: meta.badge
    });
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', autoInitLearnerLayout);
} else {
  autoInitLearnerLayout();
}


