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
  
  if (sidebarLearnerName) sidebarLearnerName.textContent = displayName;
  if (bannerLearnerName) bannerLearnerName.textContent = displayName;

  if (sidebarUserAvatar) {
    const initials = displayName
      .split(' ')
      .filter(Boolean)
      .slice(-2)
      .map(part => part[0]?.toUpperCase())
      .join('') || 'HV';
    sidebarUserAvatar.textContent = initials;
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
              ? 'bg-emerald-50 text-emerald-700 border-emerald-200' 
              : 'bg-rose-50 text-rose-700 border-rose-200';
            const statusText = item.isWon ? 'Chiến thắng' : 'Chạm đáy';

            return `
              <div class="flex items-center justify-between p-3 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] text-xs">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-lg bg-white border border-[#ECE8DE] flex items-center justify-center text-lg font-bold">
                    ${item.isWon ? '🏆' : '🌧️'}
                  </div>
                  <div>
                    <div class="font-bold text-[#0F2545]">${item.titleRank || 'Chiến Binh Luyện Chữ'}</div>
                    <div class="text-[11px] text-[#68737D] mt-0.5">
                      Bắn trúng ${item.wordsTypedCount} chữ • Chính xác ${item.accuracy}% • ${dateStr}
                    </div>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <span class="px-2 py-0.5 rounded-full text-[10px] font-bold border ${statusClass}">
                    ${statusText}
                  </span>
                  <span class="font-extrabold text-[#006194] text-sm">
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
      <div class="py-8 text-center text-xs text-[#68737D] space-y-2">
        <span class="material-symbols-outlined text-4xl text-slate-300">school</span>
        <p>Chưa có phiên luyện tập nào được ghi nhận. Hãy bắt đầu học ngay nhé!</p>
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
