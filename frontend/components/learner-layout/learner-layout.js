/**
 * KizunaTutor - Khung Giao Diện Học Viên Dùng Chung (Learner Shared Layout)
 * Cung cấp Sidebar, Header và Footer đồng bộ cho toàn bộ các màn hình của Học Viên
 * Tuân thủ nghiêm ngặt FRONTEND_RULES.md
 */

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
        icon: '📊',
        matcher: ['/learner/dashboard', '/learner', '/dashboard']
      }
    ]
  },
  {
    groupTitle: 'Gia sư & lớp học',
    items: [
      {
        key: 'tutors',
        title: 'Tìm gia sư Kizuna',
        url: '/pages/tutor-discovery/',
        icon: '🔍',
        matcher: ['/tutors', '/pages/tutor-discovery']
      },
      {
        key: 'requests',
        title: 'Lịch học thử & ghép lớp',
        url: '/pages/connection-requests/',
        icon: '📅',
        matcher: ['/connection-requests', '/pages/connection-requests']
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
        icon: '📖',
        matcher: ['/kanji-dictionary', '/dictionary', '/kanji']
      },
      {
        key: 'handwriting',
        title: 'Tra cứu viết tay',
        url: '/handwriting',
        icon: '🖋️',
        matcher: ['/handwriting', '/handwriting-search', '/ohr']
      },
      {
        key: 'shodo',
        title: 'Phòng luyện viết Shodo',
        url: '/shodo',
        icon: '✏️',
        matcher: ['/shodo', '/shodo-practice', '/kanji-practice']
      },
      {
        key: 'kana',
        title: 'Đấu trường gõ Kana',
        url: '/kana',
        icon: '⚡',
        matcher: ['/kana', '/kana-quiz']
      },
      {
        key: 'flashcards',
        title: 'Bộ thẻ của tôi',
        url: '/flashcards',
        icon: '🗂️',
        matcher: ['/flashcards', '/decks']
      },
      {
        key: 'grammar',
        title: 'Ngữ pháp & AI Sensei',
        url: '/grammar',
        icon: '📖',
        matcher: ['/grammar', '/openjlpt']
      },
      {
        key: 'kanji-game',
        title: 'Game Mưa chữ Hán',
        url: '/kanji-game',
        icon: '🎮',
        matcher: ['/kanji-game', '/game', '/rain']
      }
    ]
  }
];

// =========================================================================
// 2. HELPER: XÁC ĐỊNH MENU ACTIVE DỰA VÀO ĐƯỜNG DẪN HIỆN TẠI
// =========================================================================
export const PAGE_META_MAP = {
  'dashboard': { title: 'Tổng quan học tập', badge: 'Bàn học tập' },
  'tutors': { title: 'Tìm gia sư Kizuna', badge: 'Gia sư & lớp học' },
  'requests': { title: 'Lịch học thử & ghép lớp', badge: 'Gia sư & lớp học' },
  'kanji-dictionary': { title: 'Tra cứu chữ Hán', badge: 'Học tập & rèn luyện' },
  'handwriting': { title: 'Tra cứu viết tay', badge: 'Học tập & rèn luyện' },
  'shodo': { title: 'Phòng luyện viết Shodo', badge: 'Học tập & rèn luyện' },
  'kana': { title: 'Đấu trường gõ Kana', badge: 'Học tập & rèn luyện' },
  'flashcards': { title: 'Bộ thẻ của tôi', badge: 'Học tập & rèn luyện' },
  'grammar': { title: 'Ngữ pháp & AI Sensei', badge: 'Học tập & rèn luyện' },
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
// 4. RENDER HTML SIDEBAR HỌC VIÊN (ĐỒNG BỘ CHUẨN XÁC VỚI SIDEBAR LEGACY)
// =========================================================================
export function getLearnerSidebarHtml(activeKey) {
  const currentActive = detectActiveMenu(activeKey);
  const profile = getLearnerProfile();

  const menuSectionsHtml = LEARNER_MENU_GROUPS.map(group => {
    const itemsHtml = group.items.map(item => {
      const isActive = item.key === currentActive;
      const activeClass = isActive
        ? 'bg-[#FFE5EC] border-l-[3px] border-l-[#831843] border border-[#FBCFE8] text-[#831843] font-semibold'
        : 'text-[#334155] hover:text-[#0F172A] hover:bg-[#F1F5F9] border border-transparent font-normal';

      return `
        <a href="${item.url}"
           data-menu-key="${item.key}"
           class="nav-item flex items-center space-x-2.5 px-3 py-2 text-[14px] rounded-[6px] transition-all mb-1 no-underline ${activeClass}">
          <span class="text-sm shrink-0">${item.icon}</span>
          <span class="text-inherit truncate">${item.title}</span>
        </a>
      `;
    }).join('');

    return `
      <div>
        <div class="sidebar-group-title text-[12px] font-semibold text-[#64748B] uppercase tracking-[0.05em] px-2.5 mb-1.5">${group.groupTitle}</div>
        ${itemsHtml}
      </div>
    `;
  }).join('');

  return `
    <!-- Learner Sidebar (Đồng bộ chuẩn 100% với legacy KizunaTutor) -->
    <aside id="learnerSidebar" class="w-[270px] min-w-[260px] max-w-[280px] bg-[#F8FAFC] border-r border-[#E2E8F0] h-screen sticky top-0 flex flex-col justify-between p-4 shrink-0 z-40 transition-transform duration-200 ease-in-out -translate-x-full md:translate-x-0 font-sans select-none fixed md:static inset-y-0 left-0 shadow-lg md:shadow-none">
      
      <!-- Khối Logo Thương Hiệu KizunaLearner Đẩy Lên Đầu Sidebar -->
      <div class="pb-3 mb-2 border-b border-[#E2E8F0] flex items-center justify-between">
        <a href="/learner/dashboard" class="flex items-center gap-2.5 no-underline group" title="KizunaTutor Học Viên">
          <div class="w-8 h-8 rounded-[8px] bg-[#1E293B] text-white font-bold flex items-center justify-center text-base group-hover:scale-105 transition-transform">
            絆
          </div>
          <div class="font-sans font-bold text-[18px] text-[#0F172A] tracking-tight">
            Kizuna<span class="text-[#831843] font-extrabold">Learner</span>
          </div>
        </a>
        <button type="button" id="btnCloseSidebarMobile" class="md:hidden p-1.5 rounded-[6px] text-slate-400 hover:text-slate-700 hover:bg-[#F1F5F9] transition-colors" title="Đóng menu">
          ✕
        </button>
      </div>

      <!-- Danh Sách Menu Điều Hướng -->
      <div class="space-y-3 flex-1 overflow-y-auto pr-1">
        ${menuSectionsHtml}
      </div>

      <!-- Khối Thông Tin Học Viên Dưới Cùng -->
      <div class="pt-3 mt-2 border-t border-[#E2E8F0] bg-[#F8FAFC]">
        <div class="flex items-center justify-between gap-2">
          <div class="flex items-center space-x-2.5 min-w-0">
            <div id="sidebarUserAvatar" class="w-8 h-8 rounded-[8px] bg-[#1E293B] text-white flex items-center justify-center font-bold text-xs shrink-0">
              ${profile.initials}
            </div>
            <div class="text-xs leading-tight min-w-0">
              <div id="sidebarLearnerName" class="font-semibold text-[#0F172A] truncate max-w-[95px]" title="${profile.name}">
                ${profile.name}
              </div>
              <div class="text-[11px] text-[#64748B] truncate">Học viên</div>
            </div>
          </div>
          <button type="button"
                  id="btnLogoutSidebar"
                  class="text-[13px] text-[#334155] hover:text-[#831843] hover:bg-[#FFE5EC] font-medium px-2.5 py-1.5 rounded-[6px] border border-[#CBD5E1] hover:border-[#FBCFE8] transition-all cursor-pointer flex items-center gap-1 shrink-0"
                  title="Đăng xuất khỏi tài khoản">
            <span>🚪</span>
            <span>Thoát</span>
          </button>
        </div>
      </div>
    </aside>

    <!-- Mobile Sidebar Backdrop Overlay -->
    <div id="learnerSidebarBackdrop" class="fixed inset-0 bg-slate-900/40 z-30 hidden md:hidden transition-opacity"></div>
  `;
}

// =========================================================================
// 5. RENDER HTML HEADER HỌC VIÊN
// =========================================================================
export function getLearnerHeaderHtml(options = {}) {
  const {
    pageTitle = '',
    badge = '',
    showBreadcrumbs = true,
    rightContent = ''
  } = options;

  const profile = getLearnerProfile();

  return `
    <header class="h-14 sm:h-16 bg-white border-b border-[#E2E8F0] sticky top-0 z-20 px-4 sm:px-6 flex items-center justify-between font-sans shadow-2xs">
      
      <!-- Left: Mobile Toggle & Page Info -->
      <div class="flex items-center gap-3 min-w-0">
        <button type="button" id="btnOpenSidebarMobile" class="md:hidden p-1.5 rounded-md border border-[#CBD5E1] text-[#334155] bg-white hover:bg-[#F8FAFC] transition-colors shrink-0" title="Mở danh mục menu">
          <span class="material-symbols-outlined text-[20px]">menu</span>
        </button>

        <!-- Brand snippet on mobile -->
        <a href="/learner/dashboard" class="md:hidden font-sans font-bold text-[17px] text-[#0F172A] tracking-tight shrink-0 no-underline">
          Kizuna<span class="text-[#831843] font-extrabold">Learner</span>
        </a>

        <!-- Desktop Breadcrumb / Title -->
        <div class="hidden md:flex items-center gap-2 text-sm text-[#64748B]">
          <a href="/learner/dashboard" class="hover:text-[#0F172A] no-underline font-medium flex items-center gap-1">
            <span class="material-symbols-outlined text-[16px]">school</span>
            <span>Không gian học viên</span>
          </a>
          ${pageTitle ? `
            <span>/</span>
            <span class="font-bold text-[#0F172A] truncate max-w-[280px]">${pageTitle}</span>
          ` : ''}
          ${badge ? `
            <span class="ml-1 text-[11px] font-bold px-2 py-0.5 rounded-full bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">
              ${badge}
            </span>
          ` : ''}
        </div>
      </div>

      <!-- Right: User snippet & Custom actions -->
      <div class="flex items-center gap-2 sm:gap-3 shrink-0">
        ${rightContent}

        <!-- Quick link to Dashboard (if on another page) -->
        <a href="/learner/dashboard" class="hidden sm:inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg border border-[#CBD5E1] text-[#334155] hover:bg-[#F8FAFC] transition-all no-underline" title="Về tổng quan học tập">
          <span class="material-symbols-outlined text-[16px]">home</span>
          <span>Bàn học</span>
        </a>

        <!-- User profile badge -->
        <div class="hidden lg:flex items-center gap-2 pl-2 border-l border-[#E2E8F0]">
          <div class="w-7 h-7 rounded-md bg-[#1E293B] text-white flex items-center justify-center font-bold text-xs shadow-2xs">
            ${profile.initials}
          </div>
          <span class="text-xs font-bold text-[#0F172A] max-w-[120px] truncate">${profile.name}</span>
        </div>

        <!-- Quick link home -->
        <a href="/pages/home/" class="text-xs font-semibold text-[#64748B] hover:text-[#0F172A] no-underline px-2 py-1 rounded hover:bg-[#F1F5F9] transition-colors" title="Trang chủ giới thiệu">
          Trang chủ
        </a>
      </div>
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
 * Tự động bọc toàn bộ nội dung của trang học viên vào bộ khung chung
 * @param {Object} options
 * @param {string} options.activeMenu - Key của menu đang mở (vd: 'kanji-dictionary', 'shodo', 'flashcards', 'kanji-game')
 * @param {string} options.pageTitle - Tiêu đề trang hiển thị trên header
 * @param {string} options.badge - Nhãn phụ (vd: 'Công cụ rèn luyện')
 * @param {string} options.containerId - ID vùng nội dung (mặc định 'learner-content' hoặc bọc body)
 * @param {string} options.rightContent - Nội dung HTML tùy biến bổ sung ở góc phải header
 */
export function initLearnerShell(options = {}) {
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


