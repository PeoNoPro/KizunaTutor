/**
 * KizunaTutor - Lưới thời khóa biểu tuần dùng chung (kiểu lịch theo phút)
 * Dùng cho: Lịch giảng dạy của gia sư (tutor-schedule) và Thời khóa biểu của học viên (learner-connections).
 *
 * - 24 giờ, mỗi giờ cao bằng nhau (HOUR_HEIGHT px); thẻ buổi học đặt đúng phút bắt đầu, cao theo thời lượng.
 * - Buổi học trùng giờ trong cùng ngày được chia cột (không đè nhau).
 * - Giờ đêm (22:00–06:00) tô nhạt; cột hôm nay có vạch đỏ ở phút hiện tại.
 * - Tự cuộn tới 07:00 hoặc buổi học sớm nhất trong tuần nếu sớm hơn.
 * Hàm thuần giao diện, không gọi API.
 */

export const HOUR_HEIGHT = 56;
const DEFAULT_SCROLL_HOUR = 7;
const DAY_MINUTES = 24 * 60;

const THEMES = {
  trial: { box: 'bg-sky-50 border-sky-300 text-sky-950', bar: 'bg-sky-600' },
  official: { box: 'bg-emerald-50 border-emerald-300 text-emerald-950', bar: 'bg-emerald-600' }
};

function pad(n) {
  return String(n).padStart(2, '0');
}

function ymd(d) {
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

function isNightHour(h) {
  return h < 6 || h >= 22;
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
 * Buổi học chính thức (một dòng thời khóa biểu FE-06 đã gắn ngày cụ thể trong tuần) → sự kiện trên lưới.
 * @param {object} o {dateYmd, startTime "HH:mm", endTime "HH:mm", partnerName, classTitle}
 */
export function officialEvent(o, href = null) {
  const start = hhmmToMinutes(o.startTime);
  const end = hhmmToMinutes(o.endTime);
  if (start === null || end === null) return null;
  return {
    dateYmd: o.dateYmd, start, end: Math.max(end, start + 15), kind: 'official',
    name: o.partnerName || '', label: 'Lớp chính thức', subtitle: o.classTitle || '', href
  };
}

/** Chia cột cho các buổi trùng giờ trong cùng một ngày. */
function layoutLanes(events) {
  const sorted = [...events].sort((a, b) => (a.start - b.start) || (b.end - a.end));
  let cluster = [];
  let clusterEnd = -1;
  const flush = () => {
    const laneEnds = [];
    cluster.forEach(ev => {
      let lane = laneEnds.findIndex(end => end <= ev.start);
      if (lane < 0) {
        lane = laneEnds.length;
        laneEnds.push(ev.end);
      } else {
        laneEnds[lane] = ev.end;
      }
      ev.lane = lane;
    });
    cluster.forEach(ev => { ev.lanes = laneEnds.length; });
    cluster = [];
  };
  sorted.forEach(ev => {
    if (cluster.length && ev.start >= clusterEnd) {
      flush();
      clusterEnd = -1;
    }
    cluster.push(ev);
    clusterEnd = Math.max(clusterEnd, ev.end);
  });
  flush();
  return sorted;
}

function eventHtml(ev) {
  const theme = THEMES[ev.kind] || THEMES.trial;
  const top = (ev.start / 60) * HOUR_HEIGHT;
  const height = Math.max(((ev.end - ev.start) / 60) * HOUR_HEIGHT, 22) - 2;
  const width = 100 / (ev.lanes || 1);
  const left = (ev.lane || 0) * width;
  const time = `${minutesToHhmm(ev.start)} – ${minutesToHhmm(ev.end)}`;
  const tooltip = [ev.label, time, ev.name, ev.subtitle].filter(Boolean).join(' · ');
  const tag = ev.href ? 'a' : 'div';
  const hrefAttr = ev.href ? ` href="${escapeHtml(ev.href)}"` : '';
  // Thẻ ngắn (dưới ~45 phút) hiện 1 dòng gọn; thẻ dài hiện tên / giờ / loại buổi + tên lớp
  const body = height < 40
    ? `<p class="text-[11px] font-semibold leading-tight truncate">${escapeHtml(minutesToHhmm(ev.start))} · ${escapeHtml(ev.name || ev.label)}</p>`
    : `<p class="text-[11px] font-bold leading-tight truncate">${escapeHtml(ev.name || ev.label)}</p>
       <p class="text-[10px] font-semibold leading-tight opacity-80">${escapeHtml(time)}</p>
       ${height >= 58 ? `<p class="text-[10px] leading-tight truncate mt-0.5"><span class="font-bold">${escapeHtml(ev.label)}</span>${ev.subtitle ? ` · ${escapeHtml(ev.subtitle)}` : ''}</p>` : ''}`;
  return `
    <${tag}${hrefAttr} title="${escapeHtml(tooltip)}"
      class="absolute rounded-lg border ${theme.box} overflow-hidden pl-2.5 pr-1.5 py-1 text-left no-underline shadow-xs hover:shadow-md hover:z-20 transition-shadow"
      style="top:${top + 1}px;height:${height}px;left:calc(${left}% + 2px);width:calc(${width}% - 4px);">
      <span class="absolute left-0 top-0 bottom-0 w-1 ${theme.bar}"></span>
      ${body}
    </${tag}>`;
}

/**
 * Vẽ thân lưới tuần.
 * @param {{bodyEl: HTMLElement, scrollEl: HTMLElement|null, monday: Date, events: Array}} opts
 *   events: kết quả của trialEvent / officialEvent (bỏ qua phần tử null)
 */
export function renderWeekTimeGrid({ bodyEl, scrollEl, monday, events }) {
  if (!bodyEl) return;
  const valid = (events || []).filter(Boolean);
  const now = new Date();
  const todayYmd = ymd(now);
  const nowMinutes = now.getHours() * 60 + now.getMinutes();
  const hourLines = `background-image:repeating-linear-gradient(to bottom, transparent 0, transparent ${HOUR_HEIGHT - 1}px, rgba(148,163,184,0.25) ${HOUR_HEIGHT - 1}px, rgba(148,163,184,0.25) ${HOUR_HEIGHT}px);`;

  const timeColumn = `
    <div class="sticky left-0 z-10 border-r border-slate-200 bg-white">
      ${Array.from({ length: 24 }, (_, h) => `
        <div class="relative text-[11px] font-bold text-right pr-2 pt-1 ${isNightHour(h) ? 'bg-slate-50 text-slate-400' : 'text-slate-500'}"
             style="height:${HOUR_HEIGHT}px">${pad(h)}:00</div>`).join('')}
    </div>`;

  const dayColumns = Array.from({ length: 7 }, (_, i) => {
    const date = new Date(monday.getTime() + i * 86400000);
    const dateYmd = ymd(date);
    const isToday = dateYmd === todayYmd;
    const dayEvents = layoutLanes(valid.filter(ev => ev.dateYmd === dateYmd));
    return `
      <div class="relative border-r last:border-r-0 border-slate-200 ${isToday ? 'bg-sky-50/40' : ''}"
           style="height:${24 * HOUR_HEIGHT}px;${hourLines}">
        <div class="absolute inset-x-0 top-0 bg-slate-100/60 pointer-events-none" style="height:${6 * HOUR_HEIGHT}px"></div>
        <div class="absolute inset-x-0 bg-slate-100/60 pointer-events-none" style="top:${22 * HOUR_HEIGHT}px;height:${2 * HOUR_HEIGHT}px"></div>
        ${dayEvents.map(eventHtml).join('')}
        ${isToday ? `
          <div class="absolute inset-x-0 z-10 border-t-2 border-red-500 pointer-events-none" style="top:${(nowMinutes / 60) * HOUR_HEIGHT}px">
            <span class="absolute -left-1 -top-[5px] w-2 h-2 rounded-full bg-red-500"></span>
          </div>` : ''}
      </div>`;
  }).join('');

  bodyEl.innerHTML = `<div class="grid grid-cols-8">${timeColumn}${dayColumns}</div>`;

  if (scrollEl) {
    const earliest = valid.length ? Math.floor(Math.min(...valid.map(ev => ev.start)) / 60) : DEFAULT_SCROLL_HOUR;
    scrollEl.scrollTop = Math.min(DEFAULT_SCROLL_HOUR, earliest) * HOUR_HEIGHT;
  }
}
