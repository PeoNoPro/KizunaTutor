/**
 * KizunaTutor - Khung Giao Diện Học Viên Dùng Chung (Learner Shared Layout)
 * Cung cấp Sidebar Dropdown/Accordion, Header và Footer đồng bộ cho toàn bộ màn hình Học Viên
 * Tuân thủ nghiêm ngặt FRONTEND_RULES.md
 */

import {
  initSidebarRouter,
  LEARNER_NAV_ACTIVE,
  LEARNER_NAV_IDLE,
  LEARNER_SUBNAV_ACTIVE,
  LEARNER_SUBNAV_IDLE
} from '/js/sidebarRouter.js';
import { brandLogoHtml } from '/components/brand-logo/brand-logo.js';
import { initLearningToolsGuard } from '/js/learning-tools-access.js';

// =========================================================================
// 1. CẤU HÌNH DANH MỤC MENU HỌC VIÊN (4 TRỤ CỘT TRI THỨC CỐT LÕI)
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
        // Yêu cầu học thử và kết nối trực tiếp học viên đã gửi tới gia sư
        key: 'my-requests',
        title: 'Yêu cầu kết nối',
        url: '/pages/learner-requests/',
        icon: 'pending_actions',
        matcher: ['/learner/requests', '/pages/learner-requests', '/connection-requests', '/pages/connection-requests']
      },
      {
        key: 'requests',
        title: 'Lịch học',
        url: '/learner/connections',
        icon: 'calendar_month',
        matcher: ['/learner/connections', '/learner/bookings', '/connection-requests', '/pages/connection-requests', '/pages/learner-connections']
      },
      {
        key: 'my-courses',
        title: 'Lớp học của tôi',
        url: '/pages/my-courses/',
        icon: 'school',
        matcher: ['/my-courses', '/pages/my-courses']
      },
      {
        // FE-06: tài liệu học tập của mọi lớp (cùng trang Lớp học của tôi, ?view=materials)
        key: 'my-materials',
        title: 'Tài liệu học tập',
        url: '/pages/my-courses/?view=materials',
        icon: 'menu_book',
        query: { view: 'materials' },
        paths: ['/pages/my-courses', '/learner/my-courses', '/my-courses'],
        noSpa: true
      },
      {
        // FE-07: bài tập của mọi lớp (cùng trang Lớp học của tôi, ?view=assignments)
        key: 'my-assignments',
        title: 'Bài tập của tôi',
        url: '/pages/my-courses/?view=assignments',
        icon: 'assignment',
        query: { view: 'assignments' },
        paths: ['/pages/my-courses', '/learner/my-courses', '/my-courses'],
        noSpa: true
      }
    ]
  },
  {
    groupTitle: 'Hỗ trợ học tập',
    items: [
      // 1. Dropdown: Từ vựng & Chữ Hán (Moji・Goi)
      {
        key: 'group-vocab',
        title: 'Từ vựng & Chữ Hán',
        icon: 'translate',
        isDropdown: true,
        children: [
          {
            key: 'learning-support',
            title: 'Tra cứu từ vựng',
            url: '/vocabulary',
            icon: 'search',
            matcher: ['/vocabulary', '/vocab', '/learning-support', '/learner/learning-support', '/learner/vocabulary']
          },
          {
            key: 'kanji-dictionary',
            title: 'Tra cứu Hán tự & Bút thuận',
            url: '/kanji-dictionary',
            icon: 'menu_book',
            matcher: ['/kanji-dictionary', '/dictionary', '/kanji', '/handwriting', '/handwriting-search', '/ohr']
          }
        ]
      },

      // 2. Ngữ pháp JLPT (Bunpou - Độc lập chuẩn JLPT)
      {
        key: 'grammar',
        title: 'Tra cứu ngữ pháp JLPT',
        url: '/grammar',
        icon: 'library_books',
        matcher: ['/grammar', '/openjlpt']
      },

      // 3. Đọc hiểu JLPT (Dokkai - Độc lập chuẩn JLPT)
      {
        key: 'ai-reading',
        title: 'Luyện đọc hiểu văn bản',
        url: '/reading',
        icon: 'chrome_reader_mode',
        matcher: ['/reading', '/ai-reading', '/learner/reading', '/learner/ai-reading']
      },

      // 4. Dropdown: Luyện tập & Thực hành (Gom toàn bộ các hoạt động rèn luyện)
      {
        key: 'group-practice',
        title: 'Luyện tập & Thực hành',
        icon: 'psychology',
        isDropdown: true,
        children: [
          {
            key: 'practice',
            title: 'Luyện đề thi JLPT',
            url: '/learner/practice',
            icon: 'quiz',
            matcher: ['/learner/practice', '/learner/practice/', '/pages/practice', '/pages/practice-attempt', '/pages/practice-history']
          },
          {
            key: 'ai-writing',
            title: 'Luyện viết & Chữa câu AI',
            url: '/ai-writing',
            icon: 'edit_note',
            matcher: ['/ai-writing', '/sakubun']
          },
          {
            key: 'shodo',
            title: 'Luyện viết nét chữ Hán',
            url: '/shodo',
            icon: 'brush',
            matcher: ['/shodo', '/shodo-practice', '/kanji-practice']
          },
          {
            key: 'kana',
            title: 'Luyện gõ bảng chữ cái',
            url: '/kana',
            icon: 'keyboard',
            matcher: ['/kana', '/kana-quiz']
          },
          {
            key: 'kanji-game',
            title: 'Luyện phản xạ chữ Hán',
            url: '/kanji-game',
            icon: 'videogame_asset',
            matcher: ['/kanji-game', '/game', '/rain']
          },
          {
            key: 'flashcards',
            title: 'Bộ thẻ ghi nhớ Flashcards',
            url: '/flashcards',
            icon: 'style',
            matcher: [
              '/flashcards',
              '/decks',
              '/learner/flashcards',
              '/learner/deck-detail',
              '/deck-detail',
              '/pages/flashcards',
              '/flashcards-study',
              '/learner/flashcards-study',
              '/flashcards-match',
              '/learner/flashcards-match'
            ]
          }
        ]
      }
    ]
  },
  {
    groupTitle: 'Hệ thống & trợ giúp',
    items: [
      {
        key: 'notifications',
        title: 'Thông báo',
        url: '/pages/notifications/',
        icon: 'notifications',
        matcher: ['/pages/notifications', '/pages/notification-preferences']
      },
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
        matcher: ['/learner/policies', '/pages/policies', '/pages/policy-document']
      }
    ]
  }
];

// =========================================================================
// 2. HELPER: XÁC ĐỊNH MENU ACTIVE DỰA VÀO ĐƯỜNG DẪN
// =========================================================================
export const PAGE_META_MAP = {
  'dashboard': { title: 'Tổng quan học tập', badge: 'Bàn học tập' },
  'profile': { title: 'Hồ sơ cá nhân', badge: 'Bàn học tập' },
  'requests': { title: 'Lịch học', badge: 'Gia sư & lớp học' },
  'my-requests': { title: 'Yêu cầu kết nối', badge: 'Gia sư & lớp học' },
  'my-courses': { title: 'Lớp học của tôi', badge: 'Gia sư & lớp học' },
  'my-materials': { title: 'Tài liệu học tập', badge: 'Gia sư & lớp học' },
  'my-assignments': { title: 'Bài tập của tôi', badge: 'Gia sư & lớp học' },
  'learning-support': { title: 'Tra cứu từ vựng', badge: 'Hỗ trợ học tập' },
  'vocabulary': { title: 'Tra cứu từ vựng', badge: 'Hỗ trợ học tập' },
  'kanji-dictionary': { title: 'Từ điển chữ Hán', badge: 'Hỗ trợ học tập' },
  'handwriting': { title: 'Bảng vẽ nhận diện nét', badge: 'Hỗ trợ học tập' },
  'grammar': { title: 'Kho ngữ pháp JLPT', badge: 'Hỗ trợ học tập' },
  'ai-writing': { title: 'Luyện viết & Chữa câu AI', badge: 'Hỗ trợ học tập' },
  'ai-reading': { title: 'Luyện đọc hiểu văn bản', badge: 'Hỗ trợ học tập' },
  'reading': { title: 'Luyện đọc hiểu văn bản', badge: 'Hỗ trợ học tập' },
  'practice': { title: 'Luyện đề thi JLPT', badge: 'Hỗ trợ học tập' },
  'shodo': { title: 'Luyện viết nét chữ Hán', badge: 'Hỗ trợ học tập' },
  'kana': { title: 'Luyện gõ bảng chữ cái', badge: 'Hỗ trợ học tập' },
  'kanji-game': { title: 'Luyện phản xạ chữ Hán', badge: 'Hỗ trợ học tập' },
  'flashcards': { title: 'Bộ thẻ ghi nhớ Flashcards', badge: 'Hỗ trợ học tập' },
  'notifications': { title: 'Thông báo', badge: 'Hệ thống & trợ giúp' },
  'support': { title: 'Trung tâm hỗ trợ', badge: 'Hệ thống & trợ giúp' },
  'policies': { title: 'Chính sách & Điều khoản', badge: 'Hệ thống & trợ giúp' }
};

export function detectActiveMenu(customKey = null) {
  if (customKey) return customKey;
  const currentPath = window.location.pathname.replace(/\/+$/, '') || '/';

  // Mục dùng chung trang với mục khác, phân biệt bằng tham số (vd /pages/my-courses/?view=materials): xét trước
  const params = new URLSearchParams(window.location.search);
  for (const group of LEARNER_MENU_GROUPS) {
    for (const item of group.items) {
      if (!item.query) continue;
      const samePage = (item.paths || [item.url.split('?')[0].replace(/\/+$/, '')]).includes(currentPath);
      if (samePage && Object.entries(item.query).every(([k, v]) => params.get(k) === v)) return item.key;
    }
  }

  for (const group of LEARNER_MENU_GROUPS) {
    for (const item of group.items) {
      if (item.isDropdown && item.children) {
        for (const child of item.children) {
          if (child.key === customKey) return child.key;
          if (child.url.replace(/\/+$/, '') === currentPath) return child.key;
          if (child.matcher && child.matcher.some(m => currentPath.startsWith(m))) {
            return child.key;
          }
        }
      } else {
        if (item.key === customKey) return item.key;
        if (item.query) continue;
        if (item.url.replace(/\/+$/, '') === currentPath) return item.key;
        if (item.matcher && item.matcher.some(m => currentPath.startsWith(m))) {
          return item.key;
        }
      }
    }
  }
  return 'dashboard';
}

function isDropdownGroupActive(dropdownItem, currentActive) {
  if (!dropdownItem.children) return false;
  return dropdownItem.children.some(child => child.key === currentActive);
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
    return { name, initials, email: user?.email || '', avatarUrl: user?.avatarUrl || '', roleName: 'Học viên' };
  } catch (e) {
    return { name: 'Học viên', initials: 'HV', email: '', avatarUrl: '', roleName: 'Học viên' };
  }
}

function escapeAttr(text) {
  return String(text || '').replace(/"/g, '&quot;');
}

// =========================================================================
// 4. TỰ ĐỘNG NẠP ASSETS
// =========================================================================
function ensureLearnerLayoutAssets() {
  if (typeof document === 'undefined') return;

  if (!document.querySelector('link[href*="material-symbols"]')) {
    const iconLink = document.createElement('link');
    iconLink.rel = 'stylesheet';
    iconLink.href = 'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200';
    document.head.appendChild(iconLink);
  }

  if (!document.querySelector('script[src*="tailwindcss"]') && !window.tailwind) {
    const twScript = document.createElement('script');
    twScript.src = 'https://cdn.tailwindcss.com';
    document.head.appendChild(twScript);
  }
}

// =========================================================================
// 5. RENDER SIDEBAR HỌC VIÊN
// =========================================================================
export function getLearnerSidebarHtml(activeKey) {
  ensureLearnerLayoutAssets();
  const currentActive = detectActiveMenu(activeKey);
  const profile = getLearnerProfile();

  const menuSectionsHtml = LEARNER_MENU_GROUPS.map(group => {
    const itemsHtml = group.items.filter(item => !item.hiddenInSidebar).map(item => {
      // Trường hợp 1: Menu cha dạng Dropdown / Accordion (Từ vựng, Ngữ pháp, Thực hành & Luyện đề)
      if (item.isDropdown && item.children) {
        const isGroupOpen = isDropdownGroupActive(item, currentActive);

        const subItemsHtml = item.children.map(child => {
          const isChildActive = child.key === currentActive;
          return `
            <a href="${child.url}"
               data-menu-key="${child.key}"
               data-spa-link
               class="${isChildActive ? LEARNER_SUBNAV_ACTIVE : LEARNER_SUBNAV_IDLE}"
               ${isChildActive ? 'aria-current="page"' : ''}>
              <span class="material-symbols-outlined text-[16px] shrink-0 text-slate-500">${child.icon}</span>
              <span class="truncate">${child.title}</span>
            </a>
          `;
        }).join('');

        return `
          <div class="nav-dropdown-group flex flex-col gap-0.5" data-dropdown-key="${item.key}">
            <button type="button"
                    class="nav-dropdown-toggle w-full flex items-center justify-between px-3 py-2 rounded-lg text-[13.5px] leading-5 font-semibold transition-colors cursor-pointer select-none text-left ${isGroupOpen ? 'text-[#007bb9] bg-[#f0f4ff]' : 'text-[#3f4850] hover:bg-[#e2e7ff] hover:text-[#131b2e]'}"
                    aria-expanded="${isGroupOpen ? 'true' : 'false'}"
                    aria-controls="menu-${item.key}">
              <span class="flex items-center gap-2.5 truncate">
                <span class="material-symbols-outlined text-[19px] shrink-0 ${isGroupOpen ? 'text-[#007bb9]' : 'text-[#555f6d]'}">${item.icon}</span>
                <span class="truncate">${item.title}</span>
              </span>
              <span class="material-symbols-outlined text-[18px] text-[#707881] transition-transform duration-200 shrink-0 ${isGroupOpen ? 'rotate-180' : ''}" data-chevron>expand_more</span>
            </button>
            <div id="menu-${item.key}" class="nav-dropdown-menu flex flex-col gap-1 pl-2.5 ml-3 my-0.5 border-l-2 border-[#CBD5E1] transition-all duration-200 ${isGroupOpen ? '' : 'hidden'}">
              ${subItemsHtml}
            </div>
          </div>
        `;
      }

      // Trường hợp 2: Menu đơn lẻ độc lập (Dashboard, Lớp học, Đọc hiểu, Bộ thẻ của tôi)
      const isActive = item.key === currentActive;
      return `
        <a href="${item.url}"
           data-menu-key="${item.key}"
           ${(item.noSpa || item.key === 'tutors' || item.key === 'requests' || item.key === 'my-requests' || item.key === 'notifications' || item.key === 'support' || item.key === 'policies') ? 'data-no-spa' : 'data-spa-link'}
           class="${isActive ? LEARNER_NAV_ACTIVE : LEARNER_NAV_IDLE}" ${isActive ? 'aria-current="page"' : ''}>
          <span class="material-symbols-outlined text-[20px] shrink-0">${item.icon}</span>
          <span class="truncate">${item.title}</span>
        </a>
      `;
    }).join('');

    return `
      <div class="mb-4">
        <p class="px-3 mb-1.5 text-[11px] font-semibold text-[#707881] uppercase tracking-wider select-none">${group.groupTitle}</p>
        <div class="space-y-1">
          ${itemsHtml}
        </div>
      </div>
    `;
  }).join('');

  return `
    <aside id="learnerSidebar"
           class="w-64 bg-[#f8f9ff] border-r border-[#d9e3f6] flex flex-col shrink-0 fixed inset-y-0 left-0 z-40
                  transition-transform duration-200 md:static md:translate-x-0 -translate-x-full"
           aria-label="Thanh điều hướng chính">
      <div class="flex flex-col min-h-0 flex-1">
        <!-- Logo -->
        <div class="px-6 pt-6 pb-4 flex items-start justify-between">
          ${brandLogoHtml({ href: '/home', subtitle: 'Không gian học viên', className: 'min-w-0' })}
          <div class="flex items-center gap-1">
            <button type="button" data-notification-bell class="p-1.5 rounded-lg text-[#3f4850] hover:text-[#131b2e] hover:bg-[#eaedff] transition-colors" aria-label="Thông báo"><span class="material-symbols-outlined text-[22px]">notifications</span></button>
            <button type="button" id="btnCloseSidebarMobile" class="md:hidden p-1.5 rounded-lg text-[#707881] hover:text-[#131b2e] hover:bg-[#eaedff] transition-colors" title="Đóng menu" aria-label="Đóng menu">
              <span class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>
        </div>

        <!-- Danh sách menu -->
        <nav class="flex-1 px-4 py-2 overflow-y-auto space-y-4">
          ${menuSectionsHtml}
        </nav>
      </div>

      <!-- Menu người dùng dưới cùng -->
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
          <button type="button" id="btnLogoutSidebar" role="menuitem"
                  class="w-full flex items-center gap-2 px-3 py-2 text-[14px] font-medium rounded-lg text-[#ba1a1a] hover:bg-[#ffdad6]/40 transition-colors text-left">
            <span class="material-symbols-outlined text-[18px]">logout</span><span>Đăng xuất</span>
          </button>
        </div>

        <button type="button" id="btnUserMenuToggle" aria-haspopup="menu" aria-expanded="false"
                class="w-full flex items-center justify-between p-2 rounded-xl hover:bg-[#eaedff] transition-colors text-left">
          <div class="flex items-center gap-3 min-w-0">
            <div class="relative w-10 h-10 rounded-full bg-[#007bb9] flex items-center justify-center text-white font-semibold text-sm shrink-0 overflow-hidden">
              ${escapeAttr(profile.initials)}
              ${profile.avatarUrl
                ? `<img src="${escapeAttr(profile.avatarUrl)}" alt="" class="absolute inset-0 w-full h-full object-cover" onerror="this.remove()">`
                : ''}
            </div>
            <div class="min-w-0">
              <p class="text-[14px] font-semibold text-[#131b2e] truncate">${escapeAttr(profile.name)}</p>
              <p class="text-[12px] text-[#707881] truncate">${escapeAttr(profile.roleName)}</p>
            </div>
          </div>
          <span class="material-symbols-outlined text-[20px] text-[#707881] shrink-0" data-user-menu-chevron>unfold_more</span>
        </button>
      </div>
    </aside>
    <div id="learnerSidebarBackdrop"
         class="fixed inset-0 bg-black/40 z-30 hidden md:hidden transition-opacity"
         aria-hidden="true"></div>
  `;
}

// =========================================================================
// 6. RENDER HEADER VÀ FOOTER
// =========================================================================
export function getLearnerHeaderHtml({ pageTitle = '', badge = '', rightContent = '' }) {
  return `
    <header class="h-16 border-b border-[#d9e3f6] bg-white flex items-center justify-between px-4 sm:px-6 shrink-0 z-10">
      <div class="flex items-center gap-3">
        <button type="button" id="btnOpenSidebarMobile" class="md:hidden p-2 rounded-lg text-[#3f4850] hover:bg-[#eaedff]" title="Mở menu" aria-label="Mở menu">
          <span class="material-symbols-outlined text-[24px]">menu</span>
        </button>
        <div>
          <h1 class="text-lg sm:text-xl font-bold text-[#131b2e] leading-tight">${pageTitle}</h1>
          ${badge ? `<span class="inline-block text-[11px] font-medium text-[#006194] bg-[#d3e4ff] px-2 py-0.5 rounded-full mt-0.5">${badge}</span>` : ''}
        </div>
      </div>
      <div class="flex items-center gap-2">
        ${rightContent}
      </div>
    </header>
  `;
}

export function getLearnerFooterHtml() {
  return `
    <footer class="mt-auto py-6 border-t border-[#e2e7ff] text-center text-[12px] text-[#707881]">
      <p>© 2026 KizunaTutor. Đồng hành cùng giấc mơ Nhật ngữ của bạn.</p>
    </footer>
  `;
}

// =========================================================================
// 7. BIND SỰ KIỆN: MOBILE SIDEBAR, DROPDOWNS & ĐĂNG XUẤT
// =========================================================================
export function bindLearnerLayoutEvents() {
  // FE-12: số thông báo chưa đọc cạnh mục Thông báo.
  import('/js/pages/notification-badge.js').then(module => module.attachNotificationBadges()).catch(() => {});

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

  // Bắt sự kiện click các nút Dropdown Toggle trên Sidebar
  const dropdownToggles = document.querySelectorAll('#learnerSidebar .nav-dropdown-toggle');
  dropdownToggles.forEach(btn => {
    if (btn.__kizunaDropdownBound) return;
    btn.__kizunaDropdownBound = true;

    btn.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      const parentGroup = btn.closest('.nav-dropdown-group');
      if (!parentGroup) return;

      const menu = parentGroup.querySelector('.nav-dropdown-menu');
      const chevron = btn.querySelector('[data-chevron]');
      if (!menu) return;

      const isExpanded = !menu.classList.contains('hidden');

      if (isExpanded) {
        menu.classList.add('hidden');
        btn.setAttribute('aria-expanded', 'false');
        btn.classList.remove('text-[#007bb9]', 'bg-[#f0f4ff]');
        btn.classList.add('text-[#3f4850]');
        chevron?.classList.remove('rotate-180');
      } else {
        menu.classList.remove('hidden');
        btn.setAttribute('aria-expanded', 'true');
        btn.classList.add('text-[#007bb9]', 'bg-[#f0f4ff]');
        btn.classList.remove('text-[#3f4850]');
        chevron?.classList.add('rotate-180');
      }
    });
  });

  // User menu panel toggle
  const userMenuBtn = document.getElementById('btnUserMenuToggle');
  const userMenuPanel = document.getElementById('learnerUserMenuPanel');
  if (userMenuBtn && userMenuPanel && !userMenuBtn.__kizunaBound) {
    userMenuBtn.__kizunaBound = true;
    userMenuBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isHidden = userMenuPanel.classList.toggle('hidden');
      userMenuBtn.setAttribute('aria-expanded', String(!isHidden));
    });

    document.addEventListener('click', (e) => {
      if (!userMenuPanel.classList.contains('hidden') && !e.target.closest('#learnerUserMenu')) {
        userMenuPanel.classList.add('hidden');
        userMenuBtn.setAttribute('aria-expanded', 'false');
      }
    });
  }

  // Logout handler
  if (btnLogout && !btnLogout.__kizunaBound) {
    btnLogout.__kizunaBound = true;
    btnLogout.addEventListener('click', () => {
      localStorage.removeItem('kizuna_token');
      localStorage.removeItem('kizuna_user');
      window.location.href = '/pages/login/';
    });
  }
}

// =========================================================================
// 8. GLOBAL LAYOUT STYLES & INITIALIZATION
// =========================================================================
export function injectGlobalLayoutStyles() {
  if (typeof document === 'undefined') return;
  if (document.getElementById('kizunaGlobalLayoutStyles')) return;
  const styleEl = document.createElement('style');
  styleEl.id = 'kizunaGlobalLayoutStyles';
  styleEl.textContent = `
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

    #learnerSidebar {
      transform: translateZ(0);
      backface-visibility: hidden;
      will-change: transform;
    }

    main {
      contain: paint;
    }

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
 */
export function initLearnerShell(options = {}) {
  injectGlobalLayoutStyles();
  initSidebarRouter();
  initLearningToolsGuard();

  if (document.getElementById('learnerSidebar')) {
    return;
  }

  const activeMenu = options.activeMenu || detectActiveMenu();
  const pageTitle = options.pageTitle || '';
  const badge = options.badge || '';
  const rightContent = options.rightContent || '';

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

      const fragment = document.createDocumentFragment();
      while (this.firstChild) {
        fragment.appendChild(this.firstChild);
      }

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
