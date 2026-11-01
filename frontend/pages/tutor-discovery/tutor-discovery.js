/**
 * KizunaTutor - Tutor Discovery Controller (FE-04)
 * Gọi dữ liệu 100% từ Database Supabase qua API module (/js/api.js).
 */

import { API } from '../../js/api.js';
import { formatVND, debounce } from '../../js/main.js';
import { openModal, closeModal } from '../../components/modal/modal.js';

document.addEventListener('DOMContentLoaded', () => {
  // Trạng thái tìm kiếm (State)
  const state = {
    keyword: '',
    jlptLevel: 'all',
    goal: '',
    format: '',
    priceRange: '',
    schedule: '',
    sortBy: 'rating',
    currentPage: 1,
    pageSize: 10
  };

  // DOM Elements
  const searchInput = document.getElementById('search-input');
  const searchBtn = document.getElementById('search-button');
  const jlptPills = document.querySelectorAll('.jlpt-chip');
  const filterGoal = document.getElementById('filter-goal');
  const filterFormat = document.getElementById('filter-format');
  const filterPrice = document.getElementById('filter-price');
  const filterSchedule = document.getElementById('filter-schedule');
  const sortSelect = document.getElementById('sort-select');
  const resetBtn = document.getElementById('reset-filter-btn');
  const emptyResetBtn = document.getElementById('empty-reset-btn');
  const tutorGrid = document.getElementById('tutor-grid');
  const emptyState = document.getElementById('empty-state');
  const resultsCount = document.getElementById('total-tutors-count');
  const activeChipsContainer = document.getElementById('active-filter-chips');
  const paginationSection = document.getElementById('pagination-section');
  const currentRangeEl = document.getElementById('current-range');
  const totalCountEl = document.getElementById('total-count-display');

  // Modals DOM Elements
  const detailModalBody = document.getElementById('detail-modal-body');
  const closeDetailModalBtn = document.getElementById('close-detail-modal');
  const connectForm = document.getElementById('connect-form');
  const closeConnectModalBtn = document.getElementById('close-connect-modal');
  const cancelConnectBtn = document.getElementById('cancel-connect-btn');
  const connectTutorAvatar = document.getElementById('connect-tutor-avatar');
  const connectTutorName = document.getElementById('connect-tutor-name');

  let activeTutorForConnect = null;
  let currentLoadedTutors = [];

  // Đọc tham số URL từ Home Page
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('goal')) state.goal = urlParams.get('goal');
  if (urlParams.get('level')) state.jlptLevel = urlParams.get('level');
  if (urlParams.get('format')) state.format = urlParams.get('format');
  if (urlParams.get('price')) state.priceRange = urlParams.get('price');

  syncFiltersWithState();

  // Gọi REST API lấy dữ liệu thật từ Database
  async function loadTutors() {
    if (tutorGrid) {
      tutorGrid.innerHTML = `
        <div class="col-span-full py-12 flex flex-col items-center justify-center text-on-surface-variant gap-3">
          <span class="material-symbols-outlined text-primary text-[36px] animate-spin">progress_activity</span>
          <p class="text-xs font-medium">Đang tải danh sách gia sư từ máy chủ...</p>
        </div>
      `;
    }

    try {
      const searchParams = {
        keyword: state.keyword,
        jlptLevel: state.jlptLevel,
        goal: state.goal,
        format: state.format,
        schedule: state.schedule,
        sortBy: state.sortBy,
        page: state.currentPage - 1,
        size: state.pageSize
      };

      if (state.priceRange === '150-250') {
        searchParams.minPrice = 150000;
        searchParams.maxPrice = 250000;
      } else if (state.priceRange === '250-400') {
        searchParams.minPrice = 250000;
        searchParams.maxPrice = 400000;
      } else if (state.priceRange === '400-600') {
        searchParams.minPrice = 400000;
      }

      const response = await API.tutors.search(searchParams);
      const items = (response && response.content) ? response.content : [];
      
      currentLoadedTutors = items.map(item => ({
        id: item.tutorId,
        tutorCode: item.tutorCode,
        fullName: item.fullName || 'Gia sư Kizuna',
        avatarUrl: item.avatarUrl || 'https://lh3.googleusercontent.com/aida-public/AB6AXuDfWr7LjTJCoD0Gf2VNK0QdLAElZ9cyCzJkQ8IOJ7ZhfKFRiPoiK-N6PJaofpuz4ZSh0MCTyzeYN1pX3x0Uaq5OcxZn8yQJ0H4V9S7DtsrI2aXdgxRQ0o8NMGuIO63NK6Y74ZNBD0G7rqPan3Z9H-N80LBRdViJvA9zYNeJLWG53XmbteXaiLIE6gVUR1BHJBqSmUiPALj-L4qvCcRXCinX5eARKyo5T6tbevnMHOs',
        isOnline: true,
        verified: (item.verified !== undefined ? item.verified : item.isVerified) ?? true,
        headline: item.headline || 'Gia sư tiếng Nhật',
        experienceYears: item.experienceYears || 0,
        experienceText: (item.experienceYears ? item.experienceYears + ' năm giảng dạy chuyên sâu' : 'Gia sư chuyên môn'),
        jlptLevel: item.highestJlptLevel || 'N1',
        specialties: (item.specialties && item.specialties.length > 0) ? item.specialties : [item.headline || 'Luyện thi JLPT & Kaiwa'],
        teachingFormat: item.teachingFormat || 'Online 1-1',
        location: item.location || 'Hà Nội, Việt Nam',
        hourlyRate: item.hourlyRate || 0,
        rating: item.avgRating || 5.0,
        reviewsCount: item.totalReviews || 0,
        bio: item.bio || 'Chưa có thông tin giới thiệu chi tiết.'
      }));

      const totalCount = currentLoadedTutors.length;
      if (resultsCount) resultsCount.textContent = totalCount;
      if (totalCountEl) totalCountEl.textContent = totalCount;

      if (totalCount === 0) {
        if (tutorGrid) tutorGrid.innerHTML = '';
        if (emptyState) emptyState.classList.remove('hidden');
        if (paginationSection) paginationSection.classList.add('hidden');
      } else {
        if (emptyState) emptyState.classList.add('hidden');
        if (paginationSection) paginationSection.classList.remove('hidden');
        if (tutorGrid) {
          tutorGrid.innerHTML = currentLoadedTutors.map(renderTutorCard).join('');
        }
        if (currentRangeEl) {
          currentRangeEl.textContent = `1 - ${totalCount}`;
        }
      }
    } catch (err) {
      console.error('Không thể tải dữ liệu gia sư:', err);
      if (tutorGrid) tutorGrid.innerHTML = '';
      if (emptyState) emptyState.classList.remove('hidden');
    }

    renderActiveChips();
  }

  // Render thẻ gia sư
  function renderTutorCard(tutor) {
    const profileUrl = tutor.tutorCode ? `/gs/${tutor.tutorCode}` : `/pages/tutor-profile/?id=${tutor.id}`;
    return `
      <article class="bg-white border border-[#E5DECB] rounded-xl p-space-md md:p-space-lg shadow-[0_1px_4px_rgba(15,37,69,0.04)] hover:shadow-md transition-all duration-200 flex flex-col justify-between group">
        <div class="space-y-space-md">
          <!-- Header info: Avatar & Main credentials -->
          <div class="flex items-start gap-space-md">
            <div class="relative shrink-0">
              <img class="w-20 h-20 rounded-full object-cover shadow-sm ring-2 ring-surface-container-lowest" alt="${tutor.fullName}" src="${tutor.avatarUrl}">
              ${tutor.isOnline ? '<span class="absolute bottom-0 right-0 w-4 h-4 rounded-full bg-emerald-500 ring-2 ring-surface-container-lowest" title="Đang trực tuyến"></span>' : ''}
            </div>
            <div class="flex-1 min-w-0 space-y-1">
              <div class="flex items-center justify-between gap-2">
                <h3 class="font-title-md text-title-md font-bold text-on-surface group-hover:text-primary transition-colors truncate">
                  <a href="${profileUrl}">${tutor.fullName}</a>
                </h3>
                <div class="flex items-center gap-1 text-amber-500 font-bold shrink-0 text-sm">
                  <span class="material-symbols-outlined text-[18px]" style="font-variation-settings: 'FILL' 1;">star</span>
                  <span>${Number(tutor.rating).toFixed(1)}</span>
                  <span class="text-on-surface-variant font-normal text-xs">(${tutor.reviewsCount})</span>
                </div>
              </div>

              <!-- Badges -->
              <div class="flex flex-wrap items-center gap-1.5 pt-0.5">
                ${tutor.verified ? `
                  <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-sky-50 text-sky-800 text-[11px] font-semibold border border-sky-200">
                    <span class="material-symbols-outlined text-[14px] text-sky-600">verified</span>
                    <span>Đã xác thực CCCD & Bằng cấp</span>
                  </span>
                ` : ''}
              </div>

              <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-surface-container-low text-primary text-xs font-semibold max-w-full">
                <span class="material-symbols-outlined text-[15px] shrink-0">badge</span>
                <span class="truncate">${tutor.headline}</span>
              </div>
            </div>
          </div>

          <!-- JLPT & Experience Tags -->
          <div class="flex flex-wrap items-center gap-1.5">
            <span class="px-2.5 py-0.5 rounded bg-surface-container text-primary font-semibold text-xs border border-surface-variant">
              JLPT ${tutor.jlptLevel}
            </span>
            <span class="text-xs text-on-surface-variant flex items-center gap-1">
              <span class="material-symbols-outlined text-[15px] text-primary">history_edu</span>
              <span>${tutor.experienceText}</span>
            </span>
          </div>

          <!-- Highlights (Specialties & Format) -->
          <div class="space-y-1.5 text-xs text-on-surface-variant bg-[#FAF8F2] p-2.5 rounded-lg border border-[#ECE8DE]">
            <div class="flex items-start gap-1.5">
              <span class="material-symbols-outlined text-primary text-[16px] shrink-0">psychology</span>
              <span class="line-clamp-2"><strong class="text-on-surface">Chuyên môn:</strong> ${tutor.specialties.join(', ')}.</span>
            </div>
            <div class="flex items-center gap-1.5">
              <span class="material-symbols-outlined text-primary text-[16px] shrink-0">computer</span>
              <span><strong class="text-on-surface">Hình thức:</strong> ${tutor.teachingFormat}</span>
            </div>
          </div>
        </div>

        <!-- Footer Card: Pricing & Actions -->
        <div class="pt-space-md mt-space-md flex flex-col sm:flex-row sm:items-center justify-between gap-space-sm border-t border-[#f0ebe0]">
          <div class="flex flex-col">
            <span class="font-caption text-caption text-on-surface-variant">Học phí đề xuất:</span>
            <div class="flex items-baseline gap-1">
              <span class="font-title-md text-title-md font-bold text-primary">${formatVND(tutor.hourlyRate)}</span>
              <span class="font-caption text-caption text-on-surface-variant">/ giờ</span>
            </div>
          </div>
          <div class="flex items-center gap-space-xs w-full sm:w-auto">
            <a href="${profileUrl}" class="w-1/2 sm:w-auto px-space-md py-2.5 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label-md text-label-md font-semibold transition-colors text-center inline-flex items-center justify-center">
              Xem hồ sơ
            </a>
            <button class="w-1/2 sm:w-auto px-space-lg py-2.5 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-label-md text-label-md font-semibold transition-all shadow-sm flex items-center justify-center gap-1.5" type="button" onclick="window.handleOpenConnect('${tutor.id}')">
              <span class="material-symbols-outlined text-[16px]">send</span>
              <span>Kết nối</span>
            </button>
          </div>
        </div>
      </article>
    `;
  }

  // Chip bộ lọc đang áp dụng
  function renderActiveChips() {
    if (!activeChipsContainer) return;
    const chips = [];

    if (state.keyword.trim()) chips.push({ key: 'keyword', label: `Từ khóa: "${state.keyword}"` });
    if (state.jlptLevel && state.jlptLevel !== 'all') chips.push({ key: 'jlptLevel', label: `JLPT: ${state.jlptLevel}` });
    if (state.goal) {
      const map = { kaiwa: 'Luyện giao tiếp', jlpt: 'Luyện thi JLPT', business: 'Tiếng Nhật thương mại', it: 'Tiếng Nhật IT' };
      chips.push({ key: 'goal', label: map[state.goal] || state.goal });
    }
    if (state.format) {
      const map = { online: 'Học Online', offline: 'Học Trực tiếp', hybrid: 'Kết hợp' };
      chips.push({ key: 'format', label: map[state.format] || state.format });
    }
    if (state.priceRange) {
      const map = { '150-250': '150k - 250k / giờ', '250-400': '250k - 400k / giờ', '400-600': 'Trên 400k / giờ' };
      chips.push({ key: 'priceRange', label: map[state.priceRange] || state.priceRange });
    }
    if (state.schedule) {
      const map = { today: 'Hôm nay', weekend: 'Cuối tuần', evening: 'Buổi tối' };
      chips.push({ key: 'schedule', label: map[state.schedule] || state.schedule });
    }

    if (chips.length === 0) {
      activeChipsContainer.innerHTML = '';
      return;
    }

    activeChipsContainer.innerHTML = chips.map(chip => `
      <span class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-surface-container text-primary text-xs font-semibold border border-surface-variant">
        <span>${chip.label}</span>
        <button type="button" class="hover:text-error ml-1 text-[14px] leading-none" onclick="window.removeFilter('${chip.key}')">×</button>
      </span>
    `).join('');
  }

  window.removeFilter = function(key) {
    if (key === 'keyword') { state.keyword = ''; if (searchInput) searchInput.value = ''; }
    if (key === 'jlptLevel') { state.jlptLevel = 'all'; syncJlptPills(); }
    if (key === 'goal') { state.goal = ''; if (filterGoal) filterGoal.value = ''; }
    if (key === 'format') { state.format = ''; if (filterFormat) filterFormat.value = ''; }
    if (key === 'priceRange') { state.priceRange = ''; if (filterPrice) filterPrice.value = ''; }
    if (key === 'schedule') { state.schedule = ''; if (filterSchedule) filterSchedule.value = ''; }
    loadTutors();
  };

  // Mở modal kết nối
  window.handleOpenConnect = function(tutorId) {
    const tutor = currentLoadedTutors.find(t => t.id === tutorId);
    if (!tutor) return;
    activeTutorForConnect = tutor;
    if (connectTutorAvatar) connectTutorAvatar.src = tutor.avatarUrl;
    if (connectTutorName) connectTutorName.textContent = tutor.fullName;
    openModal('connect-modal');
  };

  if (closeConnectModalBtn) closeConnectModalBtn.addEventListener('click', () => closeModal('connect-modal'));
  if (cancelConnectBtn) cancelConnectBtn.addEventListener('click', () => closeModal('connect-modal'));

  if (connectForm) {
    connectForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      try {
        await API.connectionRequests.create({
          tutorId: activeTutorForConnect?.id,
          message: connectForm.querySelector('textarea')?.value || ''
        });
        alert(`✅ Yêu cầu kết nối của bạn đã được gửi thành công tới gia sư ${activeTutorForConnect?.fullName}!`);
        closeModal('connect-modal');
      } catch (err) {
        alert('Đã có lỗi xảy ra khi gửi yêu cầu. Vui lòng thử lại sau.');
      }
    });
  }

  // Đồng bộ pills JLPT
  function syncJlptPills() {
    jlptPills.forEach(pill => {
      const level = pill.getAttribute('data-level');
      if (level === state.jlptLevel) {
        pill.classList.remove('bg-[#FAF8F2]', 'text-on-surface-variant', 'border-[#E5DECB]');
        pill.classList.add('bg-primary', 'text-white', 'border-primary', 'shadow-sm');
      } else {
        pill.classList.remove('bg-primary', 'text-white', 'border-primary', 'shadow-sm');
        pill.classList.add('bg-[#FAF8F2]', 'text-on-surface-variant', 'border-[#E5DECB]');
      }
    });
  }

  jlptPills.forEach(pill => {
    pill.addEventListener('click', () => {
      state.jlptLevel = pill.getAttribute('data-level');
      state.currentPage = 1;
      syncJlptPills();
      loadTutors();
    });
  });

  function syncFiltersWithState() {
    if (searchInput) searchInput.value = state.keyword;
    if (filterGoal) filterGoal.value = state.goal;
    if (filterFormat) filterFormat.value = state.format;
    if (filterPrice) filterPrice.value = state.priceRange;
    if (filterSchedule) filterSchedule.value = state.schedule;
    if (sortSelect) sortSelect.value = state.sortBy;
    syncJlptPills();
  }

  // Debounce nhập liệu ô tìm kiếm 300ms
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      state.keyword = e.target.value;
      state.currentPage = 1;
      loadTutors();
    }, 300));
  }

  if (searchBtn) {
    searchBtn.addEventListener('click', () => {
      if (searchInput) state.keyword = searchInput.value;
      state.currentPage = 1;
      loadTutors();
    });
  }

  [
    { el: filterGoal, prop: 'goal' },
    { el: filterFormat, prop: 'format' },
    { el: filterPrice, prop: 'priceRange' },
    { el: filterSchedule, prop: 'schedule' }
  ].forEach(({ el, prop }) => {
    if (el) {
      el.addEventListener('change', (e) => {
        state[prop] = e.target.value;
        state.currentPage = 1;
        loadTutors();
      });
    }
  });

  if (sortSelect) {
    sortSelect.addEventListener('change', (e) => {
      state.sortBy = e.target.value;
      loadTutors();
    });
  }

  function resetAllFilters() {
    state.keyword = '';
    state.jlptLevel = 'all';
    state.goal = '';
    state.format = '';
    state.priceRange = '';
    state.schedule = '';
    state.sortBy = 'rating';
    state.currentPage = 1;
    syncFiltersWithState();
    loadTutors();
  }

  if (resetBtn) resetBtn.addEventListener('click', resetAllFilters);
  if (emptyResetBtn) emptyResetBtn.addEventListener('click', resetAllFilters);

  // Khởi động
  loadTutors();
});

