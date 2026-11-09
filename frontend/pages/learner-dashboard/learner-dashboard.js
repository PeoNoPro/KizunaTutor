/**
 * KizunaTutor - Không Gian Học Viên (Learner Dashboard Controller)
 * Feature-Based Architecture: frontend/pages/learner-dashboard/
 * Đồng bộ với layout và tính năng trong legacy repository KizunaTutor
 * Tuân thủ FRONTEND_RULES.md (Zero mock/fake data)
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { showToast } from '/js/main.js';
import { API } from '/js/api.js';

// Khởi tạo khung Layout dùng chung cho Học Viên
initLearnerShell({
  activeMenu: 'dashboard',
  pageTitle: 'Tổng quan học tập',
  badge: 'Bàn học tập'
});

// DOM Elements
const bannerLearnerName = document.getElementById('bannerLearnerName');
const statKanjiCount = document.getElementById('statKanjiCount');
const statKanjiSubtext = document.getElementById('statKanjiSubtext');
const statAccuracy = document.getElementById('statAccuracy');
const statAccuracySubtext = document.getElementById('statAccuracySubtext');
const recentActivityList = document.getElementById('recentActivityList');
const statTutorStatus = document.getElementById('statTutorStatus');
const dashboardBookingsList = document.getElementById('dashboardBookingsList');

// =========================================================================
// 1. NẠP THÔNG TIN HỌC VIÊN TỪ LOCALSTORAGE / SESSION
// =========================================================================
function loadLearnerProfile() {
  const rawUser = localStorage.getItem('kizuna_user');
  let user = null;
  try {
    user = rawUser ? JSON.parse(rawUser) : null;
  } catch (e) {
    user = null;
  }

  const displayName = user?.fullName || user?.name || user?.email?.split('@')[0] || 'Học viên';
  
  const sidebarLearnerName = document.getElementById('sidebarLearnerName');
  const sidebarUserAvatar = document.getElementById('sidebarUserAvatar');

  if (sidebarLearnerName) sidebarLearnerName.textContent = displayName;
  if (bannerLearnerName) bannerLearnerName.textContent = displayName;

  const initials = displayName
    .split(' ')
    .filter(Boolean)
    .slice(-2)
    .map(part => part[0]?.toUpperCase())
    .join('') || 'HV';
  if (sidebarUserAvatar) sidebarUserAvatar.textContent = initials;
  const heroAvatar = document.getElementById('heroLearnerAvatar');
  if (heroAvatar) heroAvatar.textContent = initials;

  // Ngày hôm nay trên khối lời chào (giống dashboard gia sư)
  const todayText = document.getElementById('todayText');
  if (todayText) {
    const text = new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
    todayText.textContent = text.charAt(0).toUpperCase() + text.slice(1);
  }
}

// =========================================================================
// 2. TỔNG HỢP TIẾN ĐỘ THẬT: SỐ CHỮ KANJI TRONG BỘ THẺ CỦA HỌC VIÊN
// =========================================================================
function loadRealFlashcardStats() {
  const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
  let decks = [];
  try {
    decks = rawDecks ? JSON.parse(rawDecks) : [];
  } catch (e) {
    decks = [];
  }

  let totalCards = 0;
  decks.forEach(d => {
    totalCards += (d.cards || []).length;
  });

  if (statKanjiCount) {
    statKanjiCount.textContent = `${totalCards} chữ`;
  }
  if (statKanjiSubtext) {
    statKanjiSubtext.textContent = `${decks.length} bộ thẻ cá nhân đã tạo`;
  }
}

// =========================================================================
// 3. TẢI HOẠT ĐỘNG VÀ ĐIỂM SỐ GAME MƯA CHỮ TỪ BACKEND API
// =========================================================================
async function loadRecentSessions() {
  try {
    const res = await fetch('/api/learner/minigame/sessions');
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        // Tìm kỷ lục điểm cao nhất
        let highestScore = 0;
        data.forEach(s => {
          if (s.score && s.score > highestScore) highestScore = s.score;
        });

        if (statAccuracy) {
          statAccuracy.textContent = `${highestScore} điểm`;
        }
        if (statAccuracySubtext) {
          statAccuracySubtext.textContent = `Qua ${data.length} ván gõ phản xạ`;
        }

        // Hiển thị tối đa 4 hoạt động gần nhất
        if (recentActivityList) {
          recentActivityList.innerHTML = data.slice(0, 4).map(item => {
            const dateStr = item.playedAt ? new Date(item.playedAt).toLocaleDateString('vi-VN', {
              hour: '2-digit',
              minute: '2-digit',
              day: '2-digit',
              month: '2-digit'
            }) : 'Vừa xong';

            const statusClass = item.isWon
              ? 'bg-[#1E8B3F]/10 text-[#1E8B3F]'
              : 'bg-[#ffdad6] text-[#93000a]';
            const statusText = item.isWon ? 'Chiến thắng' : 'Chạm đáy';

            return `
              <div class="flex items-center justify-between gap-3 p-3 rounded-xl bg-[#f2f3ff]">
                <div class="flex items-center gap-3 min-w-0">
                  <div class="w-10 h-10 rounded-xl ${item.isWon ? 'bg-[#ffdcc3] text-[#8d4b00]' : 'bg-[#dce1ff] text-[#4059aa]'} flex items-center justify-center shrink-0">
                    <span class="material-symbols-outlined text-[20px]">${item.isWon ? 'emoji_events' : 'rainy'}</span>
                  </div>
                  <div class="min-w-0">
                    <div class="text-[14px] font-semibold text-[#131b2e] truncate">${item.titleRank || 'Chiến Binh Luyện Chữ'}</div>
                    <div class="text-[11px] leading-[14px] font-medium text-[#3f4850] mt-0.5 truncate">
                      Bắn trúng ${item.wordsTypedCount} chữ • Chính xác ${item.accuracy}% • ${dateStr}
                    </div>
                  </div>
                </div>
                <div class="flex items-center gap-2 shrink-0">
                  <span class="px-2 py-0.5 rounded-full text-[11px] font-semibold ${statusClass}">
                    ${statusText}
                  </span>
                  <span class="font-semibold text-[#006194] text-[14px]">
                    ${item.score} đ
                  </span>
                </div>
              </div>
            `;
          }).join('');
        }
        return;
      }
    }
  } catch (err) {
    console.debug('Chưa tải được lịch sử minigame:', err);
  }

  // Nếu chưa có phiên luyện tập nào
  if (recentActivityList) {
    recentActivityList.innerHTML = `
      <div class="py-8 text-center text-[13px] text-[#3f4850] flex flex-col items-center gap-2">
        <span class="material-symbols-outlined text-4xl text-[#bfc7d2]">school</span>
        <p>Chưa có phiên luyện tập nào được ghi nhận. Hãy bắt đầu học ngay nhé!</p>
      </div>
    `;
  }
}

// =========================================================================
// 3.5. TẢI DANH SÁCH LỊCH HỌC THỬ 1-1 GẦN ĐÂY CỦA HỌC VIÊN
// =========================================================================
async function loadTrialBookings() {
  if (!dashboardBookingsList) return;
  try {
    const res = await API.trialBookings.getMyRequests('ALL', 0, 4);
    const items = res?.items || res?.content || res?.data?.items || res?.data?.content || (Array.isArray(res) ? res : []);
    
    if (items.length === 0) {
      dashboardBookingsList.innerHTML = `
        <div class="py-6 text-center text-[13px] text-[#3f4850] flex flex-col items-center gap-2">
          <span class="material-symbols-outlined text-4xl text-[#bfc7d2]">event_available</span>
          <p>Chưa có lịch học thử nào. Đặt lịch với gia sư để bắt đầu học nhé!</p>
          <a href="/pages/tutor-discovery/" class="inline-flex items-center gap-1 px-4 py-2 rounded-xl bg-[#006194] text-white text-[14px] font-medium hover:bg-[#007bb9] no-underline mt-1">
            <span>Tìm gia sư</span>
            <span class="material-symbols-outlined text-[14px]">arrow_forward</span>
          </a>
        </div>
      `;
      return;
    }

    dashboardBookingsList.innerHTML = items.map(item => {
      const isOnline = item.learningMode === 'ONLINE';
      const avatarSrc = item.partnerAvatarUrl || 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=120&q=80';
      const pad = n => String(n).padStart(2, '0');
      const DAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
      let timeStr = 'Chưa định dạng';
      if (item.scheduledStartTime) {
        const d = new Date(item.scheduledStartTime);
        timeStr = `${pad(d.getHours())}:${pad(d.getMinutes())} • ${DAYS[d.getDay()]}, ${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
      }

      let statusBadge = '';
      if (item.bookingStatus === 'PENDING') {
        statusBadge = '<span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#ffdcc3] text-[#8d4b00]">Chờ phản hồi</span>';
      } else if (item.bookingStatus === 'CONFIRMED') {
        statusBadge = '<span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#1E8B3F]/10 text-[#1E8B3F]">Đã xác nhận</span>';
      } else {
        statusBadge = `<span class="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-[#eaedff] text-[#3f4850]">${item.statusLabel || 'Đã đóng'}</span>`;
      }

      return `
        <div class="flex items-center justify-between gap-3 p-3 rounded-xl bg-[#f2f3ff]">
          <div class="flex items-center gap-3 min-w-0">
            <img src="${avatarSrc}" alt="${item.partnerName || 'Gia sư'}" class="w-10 h-10 rounded-xl object-cover ring-2 ring-[#006194]/10 shrink-0">
            <div class="min-w-0">
              <div class="text-[14px] font-semibold text-[#131b2e] truncate">${item.partnerName || 'Gia sư Kizuna'}</div>
              <div class="text-[11px] leading-[14px] font-medium text-[#3f4850] flex items-center gap-1.5 mt-0.5">
                <span class="material-symbols-outlined text-[13px] text-[#006194]">schedule</span>
                <span>${timeStr}</span>
                <span>•</span>
                <span>${isOnline ? 'Google Meet' : 'Trực tiếp'}</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-2 shrink-0">
            ${statusBadge}
            <a href="/learner/connections" class="p-1 rounded-lg hover:bg-white text-[#707881] hover:text-[#006194] transition-colors" title="Xem chi tiết">
              <span class="material-symbols-outlined text-[18px]">chevron_right</span>
            </a>
          </div>
        </div>
      `;
    }).join('');

  } catch (err) {
    console.debug('Chưa tải được lịch học thử trên dashboard:', err);
    dashboardBookingsList.innerHTML = `
      <div class="py-4 text-center text-[13px] text-[#3f4850]">
        Chưa có lịch hẹn nào.
      </div>
    `;
  }
}

// =========================================================================
// 4. XỬ LÝ ĐĂNG XUẤT
// =========================================================================
function handleLogout() {
  localStorage.removeItem('kizuna_token');
  localStorage.removeItem('kizuna_user');
  sessionStorage.clear();
  showToast('Đã đăng xuất thành công.', 'info');
  setTimeout(() => {
    window.location.href = '/login';
  }, 400);
}

// =========================================================================
// 5. KHỞI TẠO DOM VÀ SỰ KIỆN
// =========================================================================
function initDashboard() {
  loadLearnerProfile();
  loadRealFlashcardStats();
  loadRecentSessions();
  loadTrialBookings();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initDashboard);
} else {
  initDashboard();
}
