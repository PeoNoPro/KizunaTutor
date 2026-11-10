/**
 * KizunaTutor - Menu người dùng ở góc phải thanh trên (tên + vai trò + avatar, bấm để mở menu thả xuống).
 * Dùng chung cho khu vực Gia sư và Cổng học vụ.
 *
 *   topbar.innerHTML = `... ${userMenuHtml({ prefix: 'tutor', name, roleLabel: 'Gia sư tiếng Nhật' })} ...`;
 *   bindUserMenu('tutor');
 */

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

export function initialsOf(name, fallback = 'KZ') {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return fallback;
  return ((parts[0][0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

export function logout() {
  try {
    localStorage.removeItem('kizuna_token');
    localStorage.removeItem('kizuna_user');
  } catch (e) {}
  window.location.href = '/login';
}

/**
 * HTML của nút người dùng và menu thả xuống. Các phần tử có id `${prefix}TopbarName`, `${prefix}TopbarAvatar`
 * để trang có thể cập nhật tên thật sau khi tải dữ liệu.
 */
export function userMenuHtml({ prefix, name, roleLabel, avatarClass = 'w-9 h-9', showHome = true, placement = 'top', cardClass, menuItems = [] }) {
  if (placement === 'sidebar') return sidebarUserMenuHtml({ prefix, name, roleLabel, showHome, cardClass, menuItems });
  const homeItem = showHome ? `
        <a href="/home" data-no-spa role="menuitem" class="flex items-center gap-2 px-3 py-2 text-label-md rounded-lg text-on-surface hover:bg-surface-container">
          <span class="material-symbols-outlined text-[18px]">home</span><span>Về trang chủ</span>
        </a>
        <div class="h-px bg-surface-container my-1"></div>` : '';
  return `
    <div class="relative" id="${prefix}UserMenu">
      <button id="${prefix}UserMenuButton" type="button" aria-haspopup="menu" aria-expanded="false"
              class="flex items-center gap-3 pl-2 pr-1.5 py-1 rounded-xl hover:bg-surface-container transition-colors">
        <span class="text-right hidden sm:block">
          <span id="${prefix}TopbarName" class="block text-label-md font-semibold text-on-surface leading-tight">${escapeHtml(name)}</span>
          <span class="block text-caption text-on-surface-variant">${escapeHtml(roleLabel)}</span>
        </span>
        <span id="${prefix}TopbarAvatar" class="${avatarClass} rounded-full bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center text-label-sm ring-2 ring-primary/20">${escapeHtml(initialsOf(name))}</span>
        <span class="material-symbols-outlined text-[18px] text-outline transition-transform" data-chevron>expand_more</span>
      </button>
      <div id="${prefix}UserMenuPanel" role="menu" aria-label="Tài khoản"
           class="hidden absolute right-0 top-[calc(100%+8px)] w-56 rounded-xl bg-surface-container-lowest shadow-lg ring-1 ring-black/5 p-1.5 flex-col z-50">
        <div class="px-3 py-2 sm:hidden">
          <p class="text-label-md font-semibold text-on-surface truncate" data-menu-name>${escapeHtml(name)}</p>
          <p class="text-caption text-on-surface-variant">${escapeHtml(roleLabel)}</p>
        </div>
        ${homeItem}
        <button type="button" role="menuitem" data-logout class="w-full flex items-center gap-2 px-3 py-2 text-label-md rounded-lg text-error hover:bg-error-container text-left">
          <span class="material-symbols-outlined text-[18px]">logout</span><span>Đăng xuất</span>
        </button>
      </div>
    </div>`;
}

/**
 * Biến thể đặt ở góc dưới bên trái sidebar: nút chiếm hết chiều ngang, menu mở lên phía trên.
 */
function sidebarUserMenuHtml({ prefix, name, roleLabel, showHome, cardClass = 'bg-surface-container-low hover:bg-surface-container', menuItems = [] }) {
  // Mục bổ sung (tùy chọn) đặt trên "Về trang chủ", ví dụ "Hồ sơ cá nhân" của gia sư.
  const extraItems = menuItems.map((item) => `
        <a href="${escapeHtml(item.href)}" role="menuitem" class="flex items-center gap-2 px-3 py-2 text-label-md rounded-lg text-on-surface hover:bg-surface-container">
          <span class="material-symbols-outlined text-[18px]">${escapeHtml(item.icon)}</span><span>${escapeHtml(item.label)}</span>
        </a>`).join('');
  const homeItem = (showHome ? `
        <a href="/home" data-no-spa role="menuitem" class="flex items-center gap-2 px-3 py-2 text-label-md rounded-lg text-on-surface hover:bg-surface-container">
          <span class="material-symbols-outlined text-[18px]">home</span><span>Về trang chủ</span>
        </a>` : '');
  const topItems = extraItems + homeItem;
  const menuTop = topItems ? `${topItems}
        <div class="h-px bg-surface-container my-1"></div>` : '';
  return `
    <div class="relative" id="${prefix}UserMenu">
      <div id="${prefix}UserMenuPanel" role="menu" aria-label="Tài khoản"
           class="hidden absolute left-0 right-0 bottom-[calc(100%+8px)] rounded-xl bg-surface-container-lowest shadow-lg ring-1 ring-black/5 p-1.5 flex-col z-50">
        ${menuTop}
        <button type="button" role="menuitem" data-logout class="w-full flex items-center gap-2 px-3 py-2 text-label-md rounded-lg text-error hover:bg-error-container text-left">
          <span class="material-symbols-outlined text-[18px]">logout</span><span>Đăng xuất</span>
        </button>
      </div>
      <button id="${prefix}UserMenuButton" type="button" aria-haspopup="menu" aria-expanded="false"
              class="w-full flex items-center gap-3 p-3 rounded-xl ${cardClass} transition-colors text-left">
        <span id="${prefix}TopbarAvatar" class="w-10 h-10 shrink-0 rounded-full bg-primary-fixed text-on-primary-fixed-variant flex items-center justify-center text-label-sm ring-2 ring-primary/20">${escapeHtml(initialsOf(name))}</span>
        <span class="min-w-0 flex-1">
          <span id="${prefix}TopbarName" class="block text-label-md font-semibold text-on-surface leading-tight truncate">${escapeHtml(name)}</span>
          <span id="${prefix}TopbarRole" class="block text-caption text-on-surface-variant truncate">${escapeHtml(roleLabel)}</span>
          <span id="${prefix}TopbarStatus" class="hidden text-caption text-primary truncate"></span>
        </span>
        <span class="material-symbols-outlined text-[18px] text-outline transition-transform" data-chevron>expand_less</span>
      </button>
    </div>`;
}

/** Cập nhật dòng vai trò và dòng trạng thái (chỉ có ở biến thể sidebar). */
export function setUserMenuDetails(prefix, { roleLabel, statusLabel } = {}) {
  const role = document.getElementById(`${prefix}TopbarRole`);
  if (role && roleLabel) role.textContent = roleLabel;
  const status = document.getElementById(`${prefix}TopbarStatus`);
  if (status) {
    status.textContent = statusLabel || '';
    status.classList.toggle('hidden', !statusLabel);
    status.classList.toggle('block', Boolean(statusLabel));
  }
}

export function bindUserMenu(prefix) {
  const root = document.getElementById(`${prefix}UserMenu`);
  const button = document.getElementById(`${prefix}UserMenuButton`);
  const panel = document.getElementById(`${prefix}UserMenuPanel`);
  if (!root || !button || !panel) return;

  const setOpen = (open) => {
    panel.classList.toggle('hidden', !open);
    panel.classList.toggle('flex', open);
    button.setAttribute('aria-expanded', open ? 'true' : 'false');
    button.querySelector('[data-chevron]')?.classList.toggle('rotate-180', open);
  };

  button.addEventListener('click', (event) => {
    event.stopPropagation();
    setOpen(panel.classList.contains('hidden'));
  });
  document.addEventListener('click', (event) => {
    if (!root.contains(event.target)) setOpen(false);
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') setOpen(false);
  });
  panel.querySelector('[data-logout]')?.addEventListener('click', logout);
}

/** Cập nhật tên hiển thị trên nút người dùng sau khi có dữ liệu thật. */
export function setUserMenuName(prefix, name) {
  const nameEl = document.getElementById(`${prefix}TopbarName`);
  if (nameEl) nameEl.textContent = name;
  const avatar = document.getElementById(`${prefix}TopbarAvatar`);
  if (avatar) avatar.textContent = initialsOf(name);
  const menuName = document.querySelector(`#${prefix}UserMenuPanel [data-menu-name]`);
  if (menuName) menuName.textContent = name;
}
