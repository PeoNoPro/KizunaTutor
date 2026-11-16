/**
 * Khối "Liên hệ" trên thẻ yêu cầu học thử / kết nối.
 * Server chỉ trả partnerPhone / partnerEmail khi người xem được phép thấy:
 *  - gia sư: liên hệ học viên đã gửi kèm yêu cầu (trừ khi đã từ chối / hủy / hết hạn);
 *  - học viên: liên hệ gia sư sau khi gia sư đồng ý.
 */

function esc(text) {
  return String(text == null ? '' : text).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
}

/** "0912345678" → "0912 345 678" để dễ đọc. */
export function formatPhone(phone) {
  const p = String(phone || '');
  return /^\d{10}$/.test(p) ? `${p.slice(0, 4)} ${p.slice(4, 7)} ${p.slice(7)}` : p;
}

/**
 * Nhãn hình thức học của yêu cầu kết nối / lớp: "Học online" hoặc "Học trực tiếp · <địa điểm>".
 * @param {{ learningMode?: string, locationDetails?: string }} item
 */
export function learningModeHtml(item) {
  const offline = item && item.learningMode === 'OFFLINE';
  const text = offline ? `Học trực tiếp${item.locationDetails ? ` · ${item.locationDetails}` : ''}` : 'Học online';
  return `
    <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg ${offline ? 'bg-amber-50 border border-amber-200 text-amber-900' : 'bg-emerald-50 border border-emerald-200 text-emerald-800'} text-[11px] font-semibold [overflow-wrap:anywhere]">
      <span class="material-symbols-outlined text-[14px]">${offline ? 'home_pin' : 'videocam'}</span>${esc(text)}
    </div>`;
}

/**
 * @param {{ partnerPhone?: string, partnerEmail?: string }} item dữ liệu yêu cầu từ server
 * @param {string} title ví dụ "Liên hệ học viên", "Liên hệ gia sư"
 * @returns {string} HTML, rỗng nếu không có liên hệ để hiện
 */
export function contactHtml(item, title) {
  const phone = item && item.partnerPhone;
  const email = item && item.partnerEmail;
  if (!phone && !email) return '';
  const link = 'inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white ring-1 ring-inset ring-sky-200 text-[#006194] text-xs font-semibold hover:bg-sky-50 no-underline';
  return `
    <div class="flex flex-wrap items-center gap-2 p-3 rounded-xl bg-sky-50/70 border border-sky-200/70">
      <span class="text-xs font-bold text-[#0F2545] mr-1 inline-flex items-center gap-1">
        <span class="material-symbols-outlined text-[16px] text-[#006194]">contact_phone</span>${esc(title)}
      </span>
      ${phone ? `<a href="tel:${esc(phone)}" class="${link}"><span class="material-symbols-outlined text-[15px]">call</span><span class="tabular-nums select-all">${esc(formatPhone(phone))}</span></a>` : ''}
      ${email ? `<a href="mailto:${esc(email)}" class="${link} [overflow-wrap:anywhere]"><span class="material-symbols-outlined text-[15px]">mail</span><span class="select-all">${esc(email)}</span></a>` : ''}
    </div>`;
}
