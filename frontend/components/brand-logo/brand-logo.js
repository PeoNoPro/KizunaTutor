/**
 * KizunaTutor - Logo thương hiệu dùng chung cho sidebar các dashboard, đồng bộ với logo trang chủ
 * (components/header/header.html): ảnh tròn có viền, chữ "Kizuna" navy + "Tutor" xanh, dòng phụ nhỏ.
 *
 * Dùng trong các khung có Tailwind (học viên, gia sư, học vụ, quản lý gói hội viên):
 *   import { brandLogoHtml } from '/components/brand-logo/brand-logo.js';
 *   `${brandLogoHtml({ href: '/home', subtitle: 'Không gian học viên' })}`
 * Khung quản trị (admin-shell, không có Tailwind) dùng cùng cấu trúc với CSS trong styles/staff-unified.css.
 */

export const BRAND_LOGO_SRC = '/assets/logos/kizuna-logo.png';

function escapeHtml(text) {
  return String(text == null ? '' : text)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

export function brandLogoHtml({ href = '/home', subtitle = 'Kết nối song ngữ Việt – Nhật', className = '' } = {}) {
  return `
    <a href="${escapeHtml(href)}" title="Về trang chủ KizunaTutor" data-no-spa class="flex items-center gap-2.5 no-underline ${escapeHtml(className)}">
      <img src="${BRAND_LOGO_SRC}" alt="Kizuna Logo" class="h-10 w-10 object-contain rounded-full shadow-sm border border-[#E5DECB] bg-white shrink-0">
      <span class="flex flex-col min-w-0">
        <span class="font-bold text-xl leading-none tracking-tight text-[#0F2545]">Kizuna<span class="text-[#006194]">Tutor</span></span>
        <span class="text-[10px] leading-[14px] text-[#68737D] font-medium tracking-wider mt-1 truncate">${escapeHtml(subtitle)}</span>
      </span>
    </a>`;
}
