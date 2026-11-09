/**
 * KizunaTutor - Lưới thời khóa biểu tuần dùng chung (theo ca)
 * Dùng cho: Lịch giảng dạy của gia sư (tutor-schedule) và Thời khóa biểu của học viên (learner-connections).
 *
 * - Mỗi ngày chia 2 ca: Sáng (buổi BẮT ĐẦU trước 17:00) và Tối (bắt đầu từ 17:00). Không hiện mốc giờ cụ thể.
 *   Xếp ca theo giờ bắt đầu: buổi 16:00–19:00 vẫn nằm ở ca Sáng.
 * - Trong mỗi ô (ngày × ca), các buổi xếp theo giờ bắt đầu, mỗi buổi là một thẻ:
 *   tên người học/gia sư, giờ, loại buổi (+ tên lớp). Học thử và học chính thức cùng kiểu thẻ, khác màu.
 * - Buổi học chính thức lấy từ GET /api/classrooms/sessions/me (máy chủ đã sinh lịch cho đủ số buổi của khóa,
 *   áp dụng dời / hủy / thêm buổi riêng trong tuần).
 * Hàm thuần giao diện, không gọi API.
 */

const DAY_MINUTES = 24 * 60;

/** Ca trong ngày: from/to tính theo phút kể từ 00:00, xếp ca theo giờ bắt đầu của buổi. */
export const PERIODS = [
  { key: 'morning', label: 'Sáng', from: 0, to: 17 * 60 },
  { key: 'evening', label: 'Tối', from: 17 * 60, to: DAY_MINUTES }
];

const THEMES = {
  trial: { box: 'bg-sky-50 border-sky-300 text-sky-950 hover:bg-sky-100', bar: 'bg-sky-600' },
  official: { box: 'bg-emerald-50 border-emerald-300 text-emerald-950 hover:bg-emerald-100', bar: 'bg-emerald-600' }
};

const DAY_LABELS = ['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ nhật'];

function pad(n) {
  return String(n).padStart(2, '0');
}

export function ymd(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function hhmmToMinutes(value) {
  const m = /^(\d{1,2}):(\d{2})/.exec(String(value || ''));
  return m ? Number(m[1]) * 60 + Number(m[2]) : null;
}

function minutesToHhmm(min) {
  const m = Math.min(Math.max(min, 0), DAY_MINUTES);
  return `${pad(Math.floor(m / 60))}:${pad(m % 60)}`;
}

/** "HH:mm" từ chuỗi "HH:mm" hoặc "HH:mm:ss" */
export function shortHhmm(value) {
  const min = hhmmToMinutes(value);
  return min === null ? '' : minutesToHhmm(min);
}

/** Ca của một buổi theo phút bắt đầu: 'morning' (trước 17:00) hoặc 'evening'. */
export function periodOf(startMinutes) {
  const p = PERIODS.find(x => startMinutes >= x.from && startMinutes < x.to);
  return p ? p.key : PERIODS[PERIODS.length - 1].key;
}

/** Ngày "yyyy-MM-dd" → Date lúc 00:00 giờ máy. */
function parseYmd(value) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || ''));
  return m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : null;
}

/** "Thứ Năm 19/11" */
export function dayDateLabel(dateYmd) {
  const d = parseYmd(dateYmd);
  if (!d) return '';
  return `${DAY_LABELS[(d.getDay() + 6) % 7]} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
}

/** Ngày "yyyy-MM-dd" của Thứ Hai và Chủ nhật của tuần chứa {@code monday}. */
export function weekRange(monday) {
  const sunday = new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 6);
  return { from: ymd(monday), to: ymd(sunday) };
}

/**
 * Buổi học thử (bản ghi trial booking) → sự kiện trên lưới.
 * @param {object} b trial booking từ API (scheduledStartTime, scheduledEndTime, durationMinutes, bookingStatus, partnerName)
 * @param {string|null} href đường dẫn khi bấm vào thẻ (null = không bấm)
 */
export function trialEvent(b, href = null) {
  const s = new Date(b.scheduledStartTime);
  if (Number.isNaN(s.getTime())) return null;
  const e = b.scheduledEndTime ? new Date(b.scheduledEndTime) : new Date(s.getTime() + (b.durationMinutes || 50) * 60000);
  const start = s.getHours() * 60 + s.getMinutes();
  const end = ymd(e) === ymd(s) ? e.getHours() * 60 + e.getMinutes() : DAY_MINUTES;
  const label = b.bookingStatus === 'COMPLETED' ? 'Học thử · đã học'
    : b.bookingStatus === 'PENDING' ? 'Học thử · chờ duyệt'
      : b.bookingStatus === 'RESCHEDULED' ? 'Học thử · đã dời lịch' : 'Học thử';
  return {
    dateYmd: ymd(s), start, end: Math.max(end, start + 15), kind: 'trial',
    name: b.partnerName || '', label, subtitle: '', href
  };
}

/**
 * Buổi học chính thức (một phần tử của GET /api/classrooms/sessions/me) → sự kiện trên lưới.
 * @param {object} o {spaceId, classTitle, partnerName, scheduleId, extraId, originalDate, date, startTime, endTime,
 *                    moved, extra, number, totalSessions}
 * @param {string|null} href đường dẫn khi bấm (bỏ qua nếu truyền onClick)
 * @param {{onClick?: boolean}} opts onClick = true: thẻ là nút, bấm sẽ gọi onEventClick của renderWeekTimeGrid với dữ liệu o
 */
export function officialEvent(o, href = null, { onClick = false } = {}) {
  const start = hhmmToMinutes(o.startTime);
  const end = hhmmToMinutes(o.endTime);
  if (start === null || end === null) return null;
  const kindLabel = o.extra ? 'Buổi học thêm' : (o.moved ? 'Lớp chính thức · đã dời' : 'Lớp chính thức');
  const numberLabel = o.number ? `Buổi ${o.number}${o.totalSessions ? `/${o.totalSessions}` : ''}` : '';
  const notes = [numberLabel, o.moved && o.originalDate ? `Đã dời từ ${dayDateLabel(o.originalDate)}` : '']
    .filter(Boolean).join(' · ');
  return {
    dateYmd: o.date, start, end: Math.max(end, start + 15), kind: 'official',
    name: o.partnerName || '', label: kindLabel,
    subtitle: o.classTitle || '', note: notes,
    href: onClick ? null : href, clickable: onClick, data: o
  };
}

function eventHtml(ev, index) {
  const theme = THEMES[ev.kind] || THEMES.trial;
  const time = `${minutesToHhmm(ev.start)} – ${minutesToHhmm(ev.end)}`;
  const tooltip = [ev.label, time, ev.name, ev.subtitle, ev.note].filter(Boolean).join(' · ');
  const base = `relative block w-full rounded-lg border ${theme.box} overflow-hidden pl-3 pr-2 py-1.5 text-left no-underline shadow-xs transition-colors`;
  const inner = `
      <span class="absolute left-0 top-0 bottom-0 w-1 ${theme.bar}"></span>
      <p class="text-[12px] font-bold leading-tight truncate">${escapeHtml(ev.name || ev.label)}</p>
      <p class="text-[11px] font-semibold leading-tight opacity-80 mt-0.5">${escapeHtml(time)}</p>
      <p class="text-[10px] leading-tight truncate mt-0.5"><span class="font-bold">${escapeHtml(ev.label)}</span>${ev.subtitle ? ` · ${escapeHtml(ev.subtitle)}` : ''}</p>
      ${ev.note ? `<p class="text-[10px] leading-tight truncate mt-0.5 italic opacity-80">${escapeHtml(ev.note)}</p>` : ''}`;
  if (ev.clickable) {
    return `<button type="button" data-week-event="${index}" title="${escapeHtml(tooltip)} · Bấm để dời hoặc hủy buổi này" class="${base} cursor-pointer">${inner}</button>`;
  }
  if (ev.href) {
    return `<a href="${escapeHtml(ev.href)}" title="${escapeHtml(tooltip)}" class="${base}">${inner}</a>`;
  }
  return `<div title="${escapeHtml(tooltip)}" class="${base}">${inner}</div>`;
}

/**
 * Vẽ thân lưới tuần theo ca (Sáng / Tối).
 * @param {{bodyEl: HTMLElement, scrollEl?: HTMLElement|null, monday: Date, events: Array, onEventClick?: Function}} opts
 *   events: kết quả của trialEvent / officialEvent (bỏ qua phần tử null)
 *   onEventClick(data): gọi khi bấm thẻ được tạo với officialEvent(..., {onClick: true})
 */
export function renderWeekTimeGrid({ bodyEl, scrollEl, monday, events, onEventClick }) {
  if (!bodyEl) return;
  const valid = (events || []).filter(Boolean);
  const todayYmd = ymd(new Date());
  const days = Array.from({ length: 7 }, (_, i) => ymd(new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + i)));

  const rows = PERIODS.map(period => {
    const cells = days.map(dateYmd => {
      const list = valid
        .map((ev, index) => ({ ev, index }))
        .filter(({ ev }) => ev.dateYmd === dateYmd && periodOf(ev.start) === period.key)
        .sort((a, b) => (a.ev.start - b.ev.start) || (a.ev.end - b.ev.end));
      return `
        <div class="min-h-[132px] p-1.5 flex flex-col gap-1.5 border-r last:border-r-0 border-slate-200 ${dateYmd === todayYmd ? 'bg-sky-50/40' : ''}">
          ${list.map(({ ev, index }) => eventHtml(ev, index)).join('')}
        </div>`;
    }).join('');
    return `
      <div class="grid grid-cols-8 border-b last:border-b-0 border-slate-200">
        <div class="sticky left-0 z-10 bg-white border-r border-slate-200 flex items-center justify-center">
          <span class="text-[12px] font-bold text-slate-600">${escapeHtml(period.label)}</span>
        </div>
        ${cells}
      </div>`;
  }).join('');

  bodyEl.innerHTML = rows;
  if (scrollEl) scrollEl.scrollTop = 0;

  if (onEventClick) {
    bodyEl.querySelectorAll('[data-week-event]').forEach(btn => {
      btn.addEventListener('click', () => {
        const ev = valid[Number(btn.dataset.weekEvent)];
        if (ev) onEventClick(ev.data);
      });
    });
  }
}
