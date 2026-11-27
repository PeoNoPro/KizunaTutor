/**
 * KizunaTutor - Không Gian Học Viên (Learner Dashboard Controller)
 * Ưu tiên theo trạng thái học viên: buổi học tiếp theo → việc cần để ý → lịch 7 ngày → lớp học → luyện tập.
 * Tuân thủ FRONTEND_RULES.md (Zero mock/fake data)
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { API } from '/js/api.js';
import { getLearningToolAccess, renderAccessBanner } from '/js/learning-tools-access.js';

initLearnerShell({
  activeMenu: 'dashboard',
  pageTitle: 'Tổng quan học tập',
  badge: 'Bàn học tập'
});

const $ = (id) => document.getElementById(id);
const DAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
const OPEN_TRIAL = ['CONFIRMED', 'RESCHEDULED'];
const pad = (n) => String(n).padStart(2, '0');
const ymd = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const hm = (d) => `${pad(d.getHours())}:${pad(d.getMinutes())}`;
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const safeUrl = (u) => (u && /^https?:\/\//i.test(u) ? u : '');
const listOf = (res) => res?.items || res?.content || res?.data?.items || res?.data?.content || (Array.isArray(res) ? res : []);

// =========================================================================
// 1. LỜI CHÀO
// =========================================================================
function loadLearnerProfile() {
  let user = null;
  try { user = JSON.parse(localStorage.getItem('kizuna_user') || 'null'); } catch (e) { user = null; }
  const name = user?.fullName || user?.name || user?.email?.split('@')[0] || 'Học viên';
  $('bannerLearnerName').textContent = name;
  const today = new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
  $('todayText').textContent = today.charAt(0).toUpperCase() + today.slice(1);
}

// =========================================================================
// 2. LỊCH HỌC: GỘP BUỔI CỦA LỚP + BUỔI HỌC THỬ ĐÃ CHỐT THÀNH MỘT DANH SÁCH SỰ KIỆN
// =========================================================================
function classEvents(sessions) {
  return sessions.map((s) => ({
    start: new Date(`${s.date}T${s.startTime}`),
    end: new Date(`${s.date}T${s.endTime}`),
    title: s.classTitle || 'Buổi học',
    partner: s.partnerName,
    note: s.totalSessions ? `Buổi ${s.number}/${s.totalSessions}` : (s.extra ? 'Buổi học thêm' : ''),
    mode: s.learningMode,
    link: s.meetingLink,
    location: s.locationDetails,
    trial: false
  }));
}

function trialEvents(trials) {
  return trials
    .filter((t) => OPEN_TRIAL.includes(t.bookingStatus) && t.scheduledStartTime)
    .map((t) => ({
      start: new Date(t.scheduledStartTime),
      end: new Date(t.scheduledEndTime || t.scheduledStartTime),
      title: 'Buổi học thử',
      partner: t.partnerName,
      note: t.durationMinutes ? `${t.durationMinutes} phút` : '',
      mode: t.learningMode,
      link: t.meetingLink,
      location: t.locationDetails,
      trial: true
    }));
}

function relativeDay(d) {
  const diff = Math.round((new Date(ymd(d)) - new Date(ymd(new Date()))) / 86400000);
  if (diff === 0) return 'Hôm nay';
  if (diff === 1) return 'Ngày mai';
  return `${DAYS[d.getDay()]}, ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
}

function countdown(start) {
  const mins = Math.round((start - Date.now()) / 60000);
  if (mins <= 0) return 'Đang diễn ra';
  if (mins < 60) return `Còn ${mins} phút`;
  if (mins < 24 * 60) return `Còn ${Math.floor(mins / 60)} giờ ${mins % 60 ? `${mins % 60} phút` : ''}`.trim();
  return `Còn ${Math.round(mins / 1440)} ngày`;
}

function renderNextSession(events, hasClass) {
  const card = $('nextSessionCard');
  const next = events.find((e) => e.end > Date.now());
  if (!next) {
    card.innerHTML = hasClass
      ? `<div>
           <p class="text-[13px] font-medium text-white/80">Buổi học tiếp theo</p>
           <h2 class="text-[22px] leading-8 font-semibold mt-1">Không có buổi học nào trong 7 ngày tới</h2>
           <p class="text-[14px] text-white/80 mt-1">Tranh thủ ôn lại bài với Flashcard hoặc luyện viết nhé.</p>
         </div>
         <div><a href="/pages/my-courses/" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-[#006194] text-[14px] font-semibold no-underline hover:bg-[#eaedff]">Xem lớp học<span class="material-symbols-outlined text-[18px]">arrow_forward</span></a></div>`
      : `<div>
           <p class="text-[13px] font-medium text-white/80">Bắt đầu học</p>
           <h2 class="text-[22px] leading-8 font-semibold mt-1">Bạn chưa có lịch học nào</h2>
           <p class="text-[14px] text-white/80 mt-1">Chọn một gia sư, đặt buổi học thử 30 phút miễn phí rồi học chính thức.</p>
         </div>
         <div><a href="/pages/tutor-discovery/" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-[#006194] text-[14px] font-semibold no-underline hover:bg-[#eaedff]"><span class="material-symbols-outlined text-[18px]">person_search</span>Tìm gia sư</a></div>`;
    return;
  }

  const link = safeUrl(next.link);
  const place = next.mode === 'OFFLINE'
    ? `<span class="flex items-center gap-1.5"><span class="material-symbols-outlined text-[18px]">location_on</span>${esc(next.location || 'Học trực tiếp')}</span>`
    : `<span class="flex items-center gap-1.5"><span class="material-symbols-outlined text-[18px]">videocam</span>Trực tuyến</span>`;
  const action = next.mode !== 'OFFLINE' && link
    ? `<a href="${esc(link)}" target="_blank" rel="noopener noreferrer" class="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white text-[#006194] text-[14px] font-semibold no-underline hover:bg-[#eaedff]"><span class="material-symbols-outlined text-[18px]">videocam</span>Vào phòng học</a>`
    : '';
  const detailHref = next.trial ? '/pages/learner-requests/?tab=trial' : '/learner/connections';

  card.innerHTML = `
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="text-[13px] font-medium text-white/80">${next.trial ? 'Buổi học thử tiếp theo' : 'Buổi học tiếp theo'}</p>
        <h2 class="text-[26px] leading-9 font-semibold mt-1">${relativeDay(next.start)} · ${hm(next.start)} – ${hm(next.end)}</h2>
        <p class="text-[15px] mt-1 truncate">${esc(next.title)}${next.partner ? ` · ${esc(next.partner)}` : ''}</p>
      </div>
      <span class="px-3 py-1 rounded-full bg-white/15 text-[12px] font-semibold shrink-0">${countdown(next.start)}</span>
    </div>
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div class="flex flex-wrap items-center gap-4 text-[13px] text-white/90">
        ${place}${next.note ? `<span>${esc(next.note)}</span>` : ''}
      </div>
      <div class="flex flex-wrap items-center gap-2">
        ${action}
        <a href="${detailHref}" class="inline-flex items-center gap-1 px-4 py-2.5 rounded-xl bg-white/10 text-white text-[14px] font-medium no-underline hover:bg-white/20">Xem lịch<span class="material-symbols-outlined text-[18px]">arrow_forward</span></a>
      </div>
    </div>`;
}

function renderWeekStrip(events) {
  const today = new Date();
  $('weekStrip').innerHTML = Array.from({ length: 7 }, (_, i) => {
    const day = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i);
    const key = ymd(day);
    const items = events.filter((e) => ymd(e.start) === key);
    const chips = items.map((e) => `
      <div class="rounded-lg px-2 py-1.5 ${e.trial ? 'bg-[#ffdcc3] text-[#6e3900]' : 'bg-[#cce5ff] text-[#004b73]'}" title="${esc(e.title)}${e.partner ? ` · ${esc(e.partner)}` : ''}">
        <div class="text-[12px] font-semibold">${hm(e.start)}</div>
        <div class="text-[11px] leading-[14px] truncate">${esc(e.trial ? 'Học thử' : e.title)}</div>
      </div>`).join('');
    return `
      <div class="rounded-xl p-2 min-h-[96px] flex flex-col gap-1.5 ${i === 0 ? 'bg-[#eaedff]' : 'bg-[#f7f8ff]'}">
        <div class="text-[12px] font-semibold ${i === 0 ? 'text-[#006194]' : 'text-[#3f4850]'}">${i === 0 ? 'Hôm nay' : DAYS[day.getDay()]} <span class="font-normal">${pad(day.getDate())}/${pad(day.getMonth() + 1)}</span></div>
        ${chips || '<div class="text-[11px] text-[#bfc7d2]">Trống</div>'}
      </div>`;
  }).join('');
}

function renderTodo(trials, connections) {
  const todo = [];
  trials.filter((t) => t.bookingStatus === 'PENDING').forEach((t) => {
    const when = t.scheduledStartTime ? new Date(t.scheduledStartTime) : null;
    todo.push({ icon: 'hourglass_top', tone: 'bg-[#ffdcc3] text-[#8d4b00]', href: '/pages/learner-requests/?tab=trial',
      title: `Chờ ${t.partnerName || 'gia sư'} nhận học thử`, sub: when ? `${relativeDay(when)} · ${hm(when)}` : 'Học thử' });
  });
  connections.filter((c) => c.status === 'PENDING').forEach((c) => {
    todo.push({ icon: 'group_add', tone: 'bg-[#dce1ff] text-[#4059aa]', href: '/pages/learner-requests/',
      title: `Chờ ${c.partnerName || c.tutorName || 'gia sư'} phản hồi`, sub: 'Yêu cầu kết nối' });
  });

  $('todoList').innerHTML = todo.length
    ? todo.slice(0, 4).map((t) => `
        <a href="${t.href}" class="flex items-center gap-3 p-2.5 rounded-xl hover:bg-[#f2f3ff] transition-colors no-underline">
          <span class="w-9 h-9 rounded-lg ${t.tone} flex items-center justify-center shrink-0"><span class="material-symbols-outlined text-[18px]">${t.icon}</span></span>
          <span class="min-w-0 flex-1">
            <span class="block text-[13px] font-semibold text-[#131b2e] truncate">${esc(t.title)}</span>
            <span class="block text-[12px] leading-4 text-[#3f4850] truncate">${esc(t.sub)}</span>
          </span>
        </a>`).join('') + (todo.length > 4 ? `<p class="text-[12px] text-[#3f4850] px-2.5">và ${todo.length - 4} yêu cầu khác</p>` : '')
    : `<div class="py-6 flex flex-col items-center gap-2 text-center text-[13px] text-[#3f4850]">
         <span class="material-symbols-outlined text-[32px] text-[#1E8B3F]">task_alt</span>
         <p>Không có yêu cầu nào đang chờ.</p>
       </div>`;
}

function renderClasses(classes) {
  const open = classes.filter((c) => c.status === 'ACTIVE' || c.status === 'PAUSED');
  $('myClassesList').innerHTML = open.length
    ? open.map((c) => {
        const total = c.totalSessions || 0;
        const pct = total ? Math.min(100, Math.round((c.startedSessions / total) * 100)) : 0;
        return `
          <a href="/pages/my-courses/" class="block p-3.5 rounded-xl bg-[#f2f3ff] hover:bg-[#eaedff] transition-colors no-underline">
            <div class="flex items-center justify-between gap-3">
              <div class="min-w-0">
                <div class="text-[14px] font-semibold text-[#131b2e] truncate">${esc(c.title || 'Lớp học')}</div>
                <div class="text-[12px] leading-4 text-[#3f4850] truncate">${esc(c.tutorName || '')}${c.learningMode === 'OFFLINE' ? ' · Trực tiếp' : ' · Trực tuyến'}</div>
              </div>
              ${c.status === 'PAUSED' ? '<span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#ffdcc3] text-[#8d4b00] shrink-0">Tạm dừng</span>' : ''}
            </div>
            ${total ? `
              <div class="mt-2.5 flex items-center gap-3">
                <div class="h-1.5 flex-1 rounded-full bg-white overflow-hidden"><div class="h-full rounded-full bg-[#006194]" style="width:${pct}%"></div></div>
                <span class="text-[12px] font-medium text-[#3f4850] shrink-0">${c.startedSessions}/${total} buổi</span>
              </div>` : ''}
          </a>`;
      }).join('')
    : `<div class="py-6 flex flex-col items-center gap-2 text-center text-[13px] text-[#3f4850]">
         <span class="material-symbols-outlined text-[32px] text-[#bfc7d2]">school</span>
         <p>Chưa có lớp học. Sau buổi học thử, gia sư sẽ tạo lớp cho bạn.</p>
       </div>`;
  return open.length > 0;
}

async function loadSchedule() {
  const today = new Date();
  const to = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 6);
  const [classes, sessions, trials, connections] = await Promise.all([
    API.classrooms.list().catch(() => []),
    API.classrooms.sessions(ymd(today), ymd(to)).catch(() => []),
    API.trialBookings.getMyRequests('ALL', 0, 20).then(listOf).catch(() => []),
    API.connectionRequests.getMyRequests('PENDING', 0, 10).then(listOf).catch(() => [])
  ]);

  const events = [...classEvents(sessions || []), ...trialEvents(trials)].sort((a, b) => a.start - b.start);
  const hasClass = renderClasses(classes || []);
  renderNextSession(events, hasClass);
  renderWeekStrip(events);
  renderTodo(trials, connections);
}

// =========================================================================
// 3. LUYỆN TẬP: CHỈ HIỆN SỐ KHI ĐÃ CÓ DỮ LIỆU, CHƯA CÓ THÌ MỜI BẮT ĐẦU
// =========================================================================
function loadFlashcardStats() {
  let decks = [];
  try { decks = JSON.parse(localStorage.getItem('kizuna_flashcard_decks') || '[]'); } catch (e) { decks = []; }
  const cards = decks.reduce((n, d) => n + (d.cards || []).length, 0);
  if (cards > 0) {
    $('statKanjiSubtext').textContent = `${cards} chữ trong ${decks.length} bộ thẻ`;
    $('statKanjiCta').textContent = 'Ôn tập';
  }
}

async function loadGameSessions() {
  let data = [];
  try {
    const res = await fetch('/api/learner/minigame/sessions');
    if (res.ok) data = await res.json();
  } catch (err) {
    console.debug('Chưa tải được lịch sử minigame:', err);
  }
  if (!Array.isArray(data) || data.length === 0) return;

  const best = Math.max(0, ...data.map((s) => s.score || 0));
  $('statAccuracySubtext').textContent = `Kỷ lục ${best} điểm · ${data.length} ván`;
  $('statAccuracyCta').textContent = 'Chơi tiếp';
  $('recentActivityList').innerHTML = `<p class="text-[12px] font-semibold text-[#3f4850]">Ván gần đây</p>` + data.slice(0, 3).map((item) => {
    const when = item.playedAt ? new Date(item.playedAt).toLocaleDateString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }) : 'Vừa xong';
    return `
      <div class="flex items-center justify-between gap-3 text-[12px] leading-4">
        <span class="text-[#3f4850] truncate">${esc(when)} · ${esc(item.wordsTypedCount)} chữ · ${esc(item.accuracy)}%</span>
        <span class="font-semibold ${item.isWon ? 'text-[#1E8B3F]' : 'text-[#006194]'} shrink-0">${esc(item.score)} đ</span>
      </div>`;
  }).join('');
}

// =========================================================================
// 4. KHỞI TẠO
// =========================================================================
async function loadLockBanner() {
  const access = await getLearningToolAccess();
  // Chỉ báo khi công cụ đang bị khóa; đã mở thì không cần chiếm chỗ đầu trang.
  if (!access.allowed) renderAccessBanner($('learningToolsAccessBanner'), access);
}

function initDashboard() {
  loadLockBanner();
  loadLearnerProfile();
  loadFlashcardStats();
  loadGameSessions();
  loadSchedule();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initDashboard);
} else {
  initDashboard();
}
