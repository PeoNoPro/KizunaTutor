/**
 * KizunaTutor - Thanh phân trang dùng chung.
 * Luôn hiển thị: "Hiển thị a - b", chọn số dòng mỗi trang và các nút số trang
 * (kể cả khi chỉ có 1 trang).
 *
 *   import { renderPagination } from '/components/pagination/pagination.js';
 *   renderPagination($('pager'), { page, size, totalPages, total }, {
 *     onPage: (p) => load(p), onSize: (s) => { pageSize = s; load(0); }
 *   });
 *
 * Nhận được cả hai dạng phản hồi của backend: { totalElements } hoặc { totalItems }.
 */

export const PAGE_SIZE_OPTIONS = [10, 20, 50];

/**
 * Tính trạng thái phân trang (không đụng DOM): trang hiện tại, khoảng "a - b" và các số trang cần hiện
 * (trang đầu, trang cuối, trang hiện tại và hai trang kề).
 */
export function paginationState(data = {}, sizes = PAGE_SIZE_OPTIONS) {
  const total = Number(data.total ?? data.totalElements ?? data.totalItems ?? 0);
  const size = Number(data.size) || sizes[0];
  const totalPages = Math.max(1, Number(data.totalPages) || Math.ceil(total / size) || 1);
  const current = Math.min(Math.max(0, Number(data.page) || 0), totalPages - 1);
  const from = total === 0 ? 0 : current * size + 1;
  const to = Math.min(total, (current + 1) * size);
  const pages = [...new Set([0, totalPages - 1, current - 1, current, current + 1])]
    .filter((p) => p >= 0 && p < totalPages)
    .sort((a, b) => a - b);
  return { total, size, totalPages, current, from, to, pages };
}

export function renderPagination(container, data, { onPage, onSize, sizes = PAGE_SIZE_OPTIONS } = {}) {
  if (!container) return;
  const { size, totalPages, current, from, to, pages } = paginationState(data, sizes);

  const btn = (label, target, disabled, active = false, aria = '') => `
    <button type="button" data-page="${target}" ${disabled ? 'disabled' : ''} ${aria ? `aria-label="${aria}"` : ''} ${active ? 'aria-current="page"' : ''}
      class="h-8 min-w-[2rem] px-2.5 rounded-lg text-label-sm flex items-center justify-center transition-colors
             ${active ? 'bg-primary text-on-primary shadow-sm' : 'bg-surface-container-lowest text-on-surface hover:bg-surface-container'}
             ${disabled && !active ? 'opacity-40 cursor-not-allowed hover:bg-surface-container-lowest' : ''}">${label}</button>`;

  let buttons = btn('<span class="material-symbols-outlined text-[18px]">chevron_left</span>', current - 1, current === 0, false, 'Trang trước');
  let last = -1;
  pages.forEach((p) => {
    if (last >= 0 && p - last > 1) buttons += '<span class="px-1 text-outline text-label-sm">…</span>';
    buttons += btn(String(p + 1), p, p === current, p === current, `Trang ${p + 1}`);
    last = p;
  });
  buttons += btn('<span class="material-symbols-outlined text-[18px]">chevron_right</span>', current + 1, current >= totalPages - 1, false, 'Trang sau');

  const sizeSelect = onSize ? `
    <label class="inline-flex items-center gap-1.5 text-body-sm text-on-surface-variant">
      <select data-page-size aria-label="Số dòng mỗi trang"
              class="h-8 pl-2 pr-7 rounded-lg bg-surface-container-lowest text-on-surface text-label-sm outline-none cursor-pointer focus:ring-2 focus:ring-primary/20">
        ${sizes.map((s) => `<option value="${s}" ${s === size ? 'selected' : ''}>${s}</option>`).join('')}
      </select>
      / trang
    </label>` : '';

  container.innerHTML = `
    <div class="w-full flex flex-col sm:flex-row items-center justify-between gap-3">
      <div class="flex flex-wrap items-center gap-x-4 gap-y-2">
        <span class="text-body-sm text-outline">Hiển thị <strong class="text-on-surface font-semibold">${from} - ${to}</strong></span>
        ${sizeSelect}
      </div>
      <nav class="flex items-center gap-1.5" aria-label="Phân trang">${buttons}</nav>
    </div>`;

  container.querySelectorAll('button[data-page]').forEach((b) => b.addEventListener('click', () => {
    if (!b.disabled && onPage) onPage(Number(b.dataset.page));
  }));
  container.querySelector('[data-page-size]')?.addEventListener('change', (e) => onSize(Number(e.target.value)));
}
