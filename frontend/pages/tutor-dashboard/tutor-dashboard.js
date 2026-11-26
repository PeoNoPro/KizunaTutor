/**
 * KizunaTutor - Trang tổng quan của gia sư (bố cục theo thiết kế Stitch "Tutor Dashboard").
 * Chưa được phê duyệt: tiến trình xác minh + hoạt động gần đây (dữ liệu từ /api/tutor/verification).
 * Đã phê duyệt: yêu cầu kết nối, lịch giảng dạy hôm nay (buổi của lớp + buổi học thử đã xác nhận) và truy cập nhanh.
 */

import { API, ApiError } from '/js/api.js';
import { mountTutorLayout, setTutorIdentity } from '/components/tutor-layout/tutor-layout.js';
import { hm, ymd, listOf, todayLessons, lessonState } from '/js/tutor-dashboard-helpers.js';
import { statusBadge, badge } from '/js/membership-format.js';

const STATUS_VIEW = {
  UNVERIFIED: {
    icon: 'badge', color: 'text-on-surface', note: 'Chưa nộp hồ sơ xác minh', action: 'Bắt đầu xác minh',
  },
  PENDING: {
    icon: 'hourglass_top', color: 'text-secondary', note: 'Đang chờ cán bộ học vụ thẩm định', action: 'Xem hồ sơ đã nộp',
  },
  ADDITIONAL_INFO_REQUIRED: {
    icon: 'edit_note', color: 'text-tertiary', note: 'Cần bổ sung thông tin', action: 'Bổ sung hồ sơ',
  },
  APPROVED: {
    icon: 'verified', color: 'text-success', note: 'Đủ điều kiện nhận học viên', action: 'Xem hồ sơ xác minh',
  },
  REJECTED: {
    icon: 'block', color: 'text-error', note: 'Quyết định cuối cùng, không thể nộp lại', action: 'Xem lý do từ chối',
  }
};

const $ = (id) => document.getElementById(id);

function setVisible(el, visible) {
  el.classList.toggle('hidden', !visible);
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text == null ? '' : String(text);
  return div.innerHTML;
}

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}

function initialsOf(name) {
  const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'GS';
  return ((parts[0][0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}

function renderClock() {
  const now = new Date();
  const day = new Intl.DateTimeFormat('vi-VN', { weekday: 'long' }).format(now);
  $('todayText').textContent = `${day.charAt(0).toUpperCase()}${day.slice(1)}, ${formatDate(now)}`;
  $('clockText').textContent = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')} (GMT+7)`;
}

/**
 * Bốn bước của tiến trình xác minh, tính từ trạng thái thật.
 * state: 'done' | 'current' | 'todo' | 'failed'
 */
function progressSteps(data) {
  const s = data.status;
  const infoReady = !data.editable || (data.missingRequirements || []).length === 0;
  const submittedOk = ['PENDING', 'APPROVED', 'REJECTED'].includes(s);
  return [
    {
      title: 'Hoàn thiện hồ sơ',
      note: infoReady ? 'Đã đủ thông tin và giấy tờ' : `Còn thiếu ${(data.missingRequirements || []).length} mục`,
      state: infoReady ? 'done' : 'current'
    },
    {
      title: 'Nộp hồ sơ',
      note: s === 'ADDITIONAL_INFO_REQUIRED' ? 'Cần nộp lại sau khi bổ sung'
        : submittedOk ? `Đã nộp ${formatDate(data.submittedAt)}` : 'Chưa nộp',
      state: submittedOk ? 'done' : (infoReady ? 'current' : 'todo')
    },
    {
      title: 'Thẩm định',
      note: s === 'PENDING' ? 'Cán bộ học vụ đang xem xét' : (s === 'APPROVED' || s === 'REJECTED') ? 'Đã thẩm định' : 'Chờ nộp hồ sơ',
      state: s === 'PENDING' ? 'current' : (s === 'APPROVED' || s === 'REJECTED') ? 'done' : 'todo'
    },
    {
      title: 'Kết quả',
      note: s === 'APPROVED' ? `Đã phê duyệt ${formatDate(data.verifiedAt)}` : s === 'REJECTED' ? 'Không được chấp nhận' : 'Chưa có kết quả',
      state: s === 'APPROVED' ? 'done' : s === 'REJECTED' ? 'failed' : 'todo'
    }
  ];
}

const STEP_STYLE = {
  done: { box: 'bg-primary-fixed', icon: 'check_circle', iconColor: 'text-primary' },
  current: { box: 'bg-tertiary-fixed ring-2 ring-tertiary/40', icon: 'radio_button_checked', iconColor: 'text-tertiary' },
  todo: { box: 'bg-surface-container-low', icon: 'radio_button_unchecked', iconColor: 'text-outline' },
  failed: { box: 'bg-error-container', icon: 'cancel', iconColor: 'text-error' }
};

function renderProgress(data) {
  $('progressSteps').innerHTML = progressSteps(data).map((step, i) => {
    const st = STEP_STYLE[step.state];
    return `
      <li class="p-3 rounded-xl ${st.box}">
        <div class="flex items-center gap-1.5">
          <span class="material-symbols-outlined text-[20px] ${st.iconColor}">${st.icon}</span>
          <span class="text-caption text-on-surface-variant">Bước ${i + 1}</span>
        </div>
        <p class="text-label-md font-semibold text-on-surface mt-1">${step.title}</p>
        <p class="text-caption text-on-surface-variant mt-0.5">${escapeHtml(step.note)}</p>
      </li>`;
  }).join('');

  const fb = data.latestFeedback;
  const missing = data.editable ? (data.missingRequirements || []) : [];
  // Chỉ hiện khung chi tiết khi có phản hồi hoặc mục còn thiếu; trạng thái chung đã có ở thẻ số liệu và lời chào.
  let html = '';
  if (fb && fb.content) {
    html = `
      <p class="text-label-sm ${data.status === 'REJECTED' ? 'text-error' : 'text-tertiary'}">${escapeHtml(fb.kindLabel || 'Phản hồi')} từ cán bộ học vụ${fb.createdAt ? ` · ${formatDate(fb.createdAt)}` : ''}</p>
      <p class="text-body-md text-on-surface mt-1 whitespace-pre-line">${escapeHtml(fb.content)}</p>`;
  } else if (missing.length) {
    html = `
      <p class="text-label-sm text-tertiary">Hồ sơ còn thiếu</p>
      <ul class="list-disc pl-5 mt-1 text-body-sm text-on-surface">${missing.map((m) => `<li>${escapeHtml(m)}</li>`).join('')}</ul>`;
  }
  $('progressDetail').innerHTML = html;
  setVisible($('progressDetail'), Boolean(html));
}

/** Hoạt động gần đây dựng từ các mốc thật của hồ sơ xác minh. */
function buildActivities(data) {
  const items = [];
  if (data.latestFeedback && data.latestFeedback.createdAt) {
    items.push({
      at: data.latestFeedback.createdAt,
      dot: data.status === 'REJECTED' ? 'bg-error' : 'bg-tertiary',
      text: `Cán bộ học vụ đã gửi phản hồi: <strong>${escapeHtml(data.latestFeedback.kindLabel || 'Phản hồi')}</strong>.`
    });
  }
  if (data.verifiedAt) {
    items.push({ at: data.verifiedAt, dot: 'bg-success', text: 'Hồ sơ xác minh của bạn đã được <strong>phê duyệt</strong>.' });
  }
  if (data.submittedAt) {
    items.push({ at: data.submittedAt, dot: 'bg-primary', text: 'Bạn đã nộp hồ sơ xác minh.' });
  }
  (data.documents || []).forEach((doc) => {
    if (doc.uploadedAt) {
      items.push({ at: doc.uploadedAt, dot: 'bg-secondary', text: `Bạn đã tải lên giấy tờ <strong>${escapeHtml(doc.title)}</strong>.` });
    }
  });
  return items
    .sort((a, b) => new Date(b.at) - new Date(a.at))
    .slice(0, 5);
}

function render(data) {
  const view = STATUS_VIEW[data.status] || STATUS_VIEW.UNVERIFIED;
  const profile = data.profile || {};
  const name = profile.fullName || 'gia sư';

  setTutorIdentity({ fullName: profile.fullName, jlptLevel: profile.highestJlptLevel, statusLabel: data.statusLabel });

  // Lời chào
  $('tutorName').textContent = name;
  $('heroAvatar').textContent = initialsOf(name);
  const verified = data.status === 'APPROVED';
  $('heroVerifiedMark').classList.toggle('hidden', !verified);
  $('heroVerifiedMark').classList.toggle('flex', verified);
  $('heroEligibleBadge').classList.toggle('hidden', !verified);
  $('heroEligibleBadge').classList.toggle('flex', verified);
  const hasLevel = Boolean(profile.highestJlptLevel);
  $('heroLevelBadge').classList.toggle('hidden', !hasLevel);
  $('heroLevelBadge').classList.toggle('flex', hasLevel);
  $('heroLevelText').textContent = hasLevel ? `JLPT ${profile.highestJlptLevel}` : '';
  $('heroActionText').textContent = view.action;

  renderProgress(data);

  // Hoạt động gần đây
  const activities = buildActivities(data);
  setVisible($('activityEmpty'), activities.length === 0);
  $('activityList').classList.toggle('hidden', activities.length === 0);
  $('activityList').innerHTML = activities.length === 0 ? '' : `
    <div class="absolute left-2.5 top-2 bottom-2 w-0.5 bg-surface-container-high"></div>
    ${activities.map((a) => `
      <div class="relative flex items-start gap-3">
        <span class="absolute -left-6 top-1 w-3 h-3 rounded-full ${a.dot} ring-4 ring-surface-container-lowest"></span>
        <div>
          <p class="text-body-sm text-on-surface">${a.text}</p>
          <span class="text-caption text-on-surface-variant">${formatDate(a.at)}</span>
        </div>
      </div>`).join('')}`;

}

async function loadTrialBookings() {
  const container = $('trialBookingsContainer');
  const emptyEl = $('trialBookingsEmpty');
  const badgeEl = $('trialPendingBadge');
  const countEl = $('trialPendingCount');

  try {
    const res = await API.trialBookings.getMyRequests('PENDING', 0, 6);
    const items = res?.items || res?.content || res?.data?.items || res?.data?.content || (Array.isArray(res) ? res : []);
    const pendingCount = items.length;

    if (countEl) countEl.textContent = pendingCount;
    if (badgeEl) {
      badgeEl.classList.toggle('hidden', pendingCount === 0);
      badgeEl.classList.toggle('inline-flex', pendingCount > 0);
    }

    if (!container) return;

    if (pendingCount === 0) {
      if (emptyEl) emptyEl.classList.remove('hidden');
      container.innerHTML = '';
      return;
    }

    if (emptyEl) emptyEl.classList.add('hidden');
    container.innerHTML = items.map(item => {
      const isOnline = item.learningMode === 'ONLINE';
      const avatarSrc = item.partnerAvatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80';
      
      let timeStr = 'Chưa định dạng';
      if (item.scheduledStartTime) {
        const d = new Date(item.scheduledStartTime);
        const pad = n => String(n).padStart(2, '0');
        const DAY_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
        timeStr = `${pad(d.getHours())}:${pad(d.getMinutes())} • ${DAY_NAMES[d.getDay()]}, ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
      }

      return `
        <div class="p-4 rounded-xl bg-surface-container-low border border-outline-variant hover:border-primary/40 transition-all flex flex-col justify-between space-y-3">
          <div class="flex items-start gap-3">
            <img src="${avatarSrc}" alt="${escapeHtml(item.partnerName)}" class="w-10 h-10 rounded-xl object-cover ring-1 ring-outline-variant shrink-0">
            <div class="min-w-0 flex-1">
              <div class="flex items-center gap-1.5">
                <h4 class="text-label-md font-bold text-on-surface truncate">${escapeHtml(item.partnerName || 'Học viên')}</h4>
                <span class="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-primary-fixed text-primary shrink-0">Học viên</span>
              </div>
              <p class="text-caption text-on-surface-variant mt-0.5">${item.durationMinutes || 50} phút • Học thử 0đ</p>
            </div>
          </div>

          <div class="text-caption text-on-surface-variant space-y-1 bg-surface-container-lowest p-2.5 rounded-lg border border-outline-variant/60">
            <div class="flex items-center gap-1.5 text-on-surface font-medium">
              <span class="material-symbols-outlined text-[15px] text-primary">schedule</span>
              <span>${timeStr}</span>
            </div>
            <div class="flex items-center gap-1.5 text-on-surface-variant">
              <span class="material-symbols-outlined text-[15px]">${isOnline ? 'laptop_chromebook' : 'pin_drop'}</span>
              <span>${isOnline ? 'Trực tuyến (Google Meet)' : 'Trực tiếp tại địa chỉ hẹn'}</span>
            </div>
            ${item.currentLevel ? `
              <div class="flex items-center gap-1.5 text-tertiary">
                <span class="material-symbols-outlined text-[15px]">psychology</span>
                <span>Trình độ: ${escapeHtml(item.currentLevel)}</span>
              </div>
            ` : ''}
          </div>

          <a href="/tutor/connections" class="w-full py-2 px-3 rounded-lg bg-primary hover:bg-primary-container text-on-primary text-label-sm font-semibold transition-all flex items-center justify-center gap-1 shadow-xs">
            <span>Phản hồi yêu cầu</span>
            <span class="material-symbols-outlined text-[15px]">arrow_forward</span>
          </a>
        </div>
      `;
    }).join('');

  } catch (err) {
    console.warn('Lỗi tải danh sách yêu cầu học thử trên dashboard:', err);
    if (emptyEl) emptyEl.classList.remove('hidden');
  }
}

/** Lối tắt tới các chức năng giảng dạy (trang hội viên tự hiện màn hình mua gói khi chưa có gói). */
const QUICK_LINKS = [
  { label: 'Lịch giảng dạy', icon: 'calendar_month', href: '/tutor/schedule' },
  { label: 'Yêu cầu kết nối', icon: 'group_add', href: '/tutor/connections' },
  { label: 'Học viên của tôi', icon: 'school', href: '/tutor/classrooms?view=students' },
  { label: 'Tài liệu học tập', icon: 'menu_book', href: '/tutor/classrooms?view=materials' },
  { label: 'Quản lý bài tập', icon: 'assignment', href: '/tutor/classrooms?view=assignments' },
  { label: 'Bài đăng tuyển học viên', icon: 'campaign', href: '/tutor/post' }
];

function renderQuickLinks() {
  $('quickLinks').innerHTML = QUICK_LINKS.map((link) => `
    <a href="${link.href}" class="p-3 rounded-xl bg-surface-container-low hover:bg-surface-container-high transition-colors flex items-center gap-2.5 min-w-0">
      <span class="w-9 h-9 rounded-lg bg-primary-fixed text-primary flex items-center justify-center shrink-0">
        <span class="material-symbols-outlined text-[20px]">${link.icon}</span>
      </span>
      <span class="text-label-md text-on-surface truncate">${link.label}</span>
    </a>`).join('');
}

async function loadTodaySchedule() {
  const now = new Date();
  const today = ymd(now);
  setVisible($('todayScheduleError'), false);
  setVisible($('todayScheduleEmpty'), false);
  $('todaySchedule').innerHTML = '<li class="h-14 rounded-xl bg-surface-container-low animate-pulse"></li>';
  const [sessionsRes, trialsRes] = await Promise.allSettled([
    API.classrooms.sessions(today, today),
    API.trialBookings.getMyRequests('ALL', 0, 100)
  ]);
  if (sessionsRes.status === 'rejected' && trialsRes.status === 'rejected') {
    $('todaySchedule').innerHTML = '';
    const err = sessionsRes.reason;
    $('todayScheduleError').textContent = err instanceof ApiError && err.message
      ? err.message : 'Không tải được lịch giảng dạy hôm nay. Vui lòng thử lại sau.';
    setVisible($('todayScheduleError'), true);
    return;
  }
  const lessons = todayLessons(
    sessionsRes.status === 'fulfilled' ? listOf(sessionsRes.value) : [],
    trialsRes.status === 'fulfilled' ? listOf(trialsRes.value) : [],
    now);
  $('todayScheduleNote').textContent = lessons.length
    ? `${lessons.length} buổi dạy trong ngày ${formatDate(now)}`
    : `Ngày ${formatDate(now)}`;
  setVisible($('todayScheduleEmpty'), lessons.length === 0);
  const nowHm = hm(now);
  $('todaySchedule').innerHTML = lessons.map((l) => {
    const st = lessonState(l, nowHm);
    return `
      <li>
        <a href="${l.href}" class="flex items-center gap-4 p-3 rounded-xl bg-surface-container-low hover:bg-surface-container-high transition-colors ${st.dim ? 'opacity-70' : ''}">
          <div class="w-20 shrink-0 text-center">
            <p class="text-title-sm font-bold text-primary">${escapeHtml(l.start)}</p>
            <p class="text-caption text-on-surface-variant">${l.end ? `đến ${escapeHtml(l.end)}` : ''}</p>
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-1.5">
              <p class="text-label-md font-semibold text-on-surface truncate">${escapeHtml(l.title)}</p>
              <span class="px-1.5 py-0.5 rounded text-[10px] font-semibold bg-secondary-fixed text-on-secondary-fixed shrink-0">${l.kind}</span>
            </div>
            <p class="text-caption text-on-surface-variant mt-0.5 truncate">${escapeHtml([l.partner, l.note].filter(Boolean).join(' • '))}</p>
          </div>
          <span class="px-2 py-0.5 rounded-full text-caption font-semibold shrink-0 ${st.cls}">${st.label}</span>
        </a>
      </li>`;
  }).join('');
}

/** Gói hội viên đang dùng (FE-09, UC-97) — trước đây ở trang Gói hội viên. */
async function loadMembership() {
  const block = $('membershipBlock');
  try {
    const mine = await API.tutorMembership.getMine();
    $('membershipStateBadge').innerHTML = mine.tutorStateLabel
      ? badge(mine.tutorStateLabel, mine.tutorState === 'ACTIVE_PAID' ? 'success' : 'neutral') : '';
    const current = mine.current;
    if (current) {
      block.innerHTML = `
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-3 rounded-xl bg-surface-container-low p-4">
          <div>
            <p class="text-title-md font-semibold text-on-surface">${escapeHtml(current.planName)}</p>
            <p class="text-body-sm text-on-surface-variant mt-1">Từ ${formatDate(current.startDate)} đến ${formatDate(current.endDate)}</p>
          </div>
          <div class="flex items-center gap-3 flex-wrap">
            ${statusBadge(current.status, current.statusLabel)}
            <span class="text-body-md text-on-surface">Còn <strong>${current.daysLeft}</strong> ngày</span>
          </div>
        </div>
        ${mine.expiresOn && mine.expiresOn !== current.endDate
          ? `<p class="text-body-sm text-on-surface-variant mt-3">Tính cả các gói đã gia hạn, gói của bạn có hiệu lực đến <strong>${formatDate(mine.expiresOn)}</strong>.</p>` : ''}`;
      $('membershipLinkText').textContent = 'Gia hạn / nâng cấp gói';
    } else {
      const hadBefore = (mine.history || []).length > 0;
      block.innerHTML = `
        <div class="rounded-xl bg-surface-container-low p-5 text-center">
          <span class="material-symbols-outlined text-[32px] text-outline">${hadBefore ? 'event_busy' : 'workspace_premium'}</span>
          <p class="text-title-sm font-semibold text-on-surface mt-1">${hadBefore ? 'Gói hội viên của bạn đã hết hạn' : 'Bạn chưa có gói hội viên'}</p>
          <p class="text-body-sm text-on-surface-variant mt-1">${hadBefore ? 'Gia hạn để tiếp tục nhận học viên và dùng đầy đủ tính năng giảng dạy.' : 'Đăng ký gói để bắt đầu nhận học viên.'}</p>
        </div>`;
      $('membershipLinkText').textContent = hadBefore ? 'Gia hạn gói' : 'Xem các gói';
    }
    const upcoming = mine.upcoming || [];
    setVisible($('membershipUpcoming'), upcoming.length > 0);
    $('membershipUpcomingList').innerHTML = upcoming.map((m) => `
      <li class="flex items-center justify-between gap-3 rounded-lg bg-surface-container-low px-3 py-2">
        <span class="text-body-sm text-on-surface">${escapeHtml(m.planName)} · ${formatDate(m.startDate)} – ${formatDate(m.endDate)}</span>
        ${statusBadge(m.status, m.statusLabel)}
      </li>`).join('');
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    block.innerHTML = '<p class="text-body-sm text-on-surface-variant">Chưa tải được thông tin gói hội viên. Vui lòng tải lại trang.</p>';
  }
}

async function load() {
  setVisible($('loadingState'), true);
  setVisible($('errorState'), false);
  setVisible($('contentState'), false);
  try {
    const data = await API.tutorVerification.getMine();
    render(data);
    const approved = data.status === 'APPROVED';
    // Chưa phê duyệt: giữ tiến trình xác minh, ẩn yêu cầu kết nối.
    // Đã phê duyệt: ẩn tiến trình xác minh và hoạt động gần đây, thay bằng lịch dạy hôm nay + truy cập nhanh.
    setVisible($('trialBookingsSection'), approved);
    setVisible($('verificationSection'), !approved);
    setVisible($('approvedSection'), approved);
    if (approved) {
      renderQuickLinks();
      await Promise.all([loadTrialBookings(), loadTodaySchedule(), loadMembership()]);
    }
    setVisible($('contentState'), true);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return;
    $('errorMessage').textContent = err instanceof ApiError && err.status === 404
      ? 'Không tìm thấy hồ sơ gia sư của tài khoản này. Vui lòng liên hệ bộ phận hỗ trợ.'
      : (err instanceof ApiError ? err.message : 'Hệ thống đang bận, bạn vui lòng thử lại sau.');
    setVisible($('errorState'), true);
  } finally {
    setVisible($('loadingState'), false);
  }
}

mountTutorLayout('dashboard');
renderClock();
setInterval(renderClock, 30000);

document.addEventListener('DOMContentLoaded', () => {
  $('retryButton').addEventListener('click', load);
  load();
});
