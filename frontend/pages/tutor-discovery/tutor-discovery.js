/**
 * KizunaTutor - Tutor Discovery Controller (FE-04)
 * Quản lý tìm kiếm, lọc đa tiêu chí, phân trang, và đồng bộ URL hai chiều (Two-Way URL Sync).
 * Gọi dữ liệu 100% từ Database Supabase qua REST API (/js/api.js).
 */

import { API } from '../../js/api.js';
import { formatVND, debounce } from '../../js/main.js';
import { openModal, closeModal } from '../../components/modal/modal.js';

document.addEventListener('DOMContentLoaded', () => {
  // 1. Trạng thái tìm kiếm trung tâm (State)
  const state = {
    keyword: '',
    location: '',
    jlptLevel: 'all',
    goal: '',
    format: '',
    priceRange: '',
    schedule: '',
    sortBy: 'rating',
    currentPage: 1,
    pageSize: 6
  };

  // Whitelist các tham số hợp lệ
  const VALID_JLPT = ['N1', 'N2', 'N3', 'N4', 'N5', 'all'];
  const VALID_GOALS = ['kaiwa', 'jlpt', 'business', 'it'];
  const VALID_FORMATS = ['online', 'offline', 'hybrid'];
  const VALID_PRICES = ['150-250', '250-400', '400-600'];
  const VALID_SCHEDULES = ['today', 'weekend', 'evening'];
  const VALID_SORTS = ['rating', 'price_asc', 'price_desc', 'exp', 'newest'];

  // DOM Elements
  const searchInput = document.getElementById('search-input');
  const searchBtn = document.getElementById('search-button');
  const jlptPills = document.querySelectorAll('.jlpt-chip');
  const filterLocation = document.getElementById('filter-location');
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
  const paginationNav = document.getElementById('pagination-nav');
  const currentRangeEl = document.getElementById('current-range');
  const totalCountEl = document.getElementById('total-count-display');

  // Modals DOM Elements
  const connectForm = document.getElementById('connect-form');
  const closeConnectModalBtn = document.getElementById('close-connect-modal');
  const cancelConnectBtn = document.getElementById('cancel-connect-btn');
  const connectTutorAvatar = document.getElementById('connect-tutor-avatar');
  const connectTutorName = document.getElementById('connect-tutor-name');

  let activeTutorForConnect = null;
  let currentLoadedTutors = [];
  let isPopStateHandling = false;
  let currentRequestId = 0;

  // 2. Kiểm định tham số URL theo Whitelist (T005)
  function validateUrlParams(params) {
    const validated = {};

    const q = params.get('q') || params.get('keyword');
    if (q && q.trim()) validated.keyword = q.trim();

    const loc = params.get('location') || params.get('loc') || params.get('address');
    if (loc && loc.trim()) validated.location = loc.trim();

    const jlpt = params.get('jlpt') || params.get('level');
    if (jlpt && VALID_JLPT.includes(jlpt)) validated.jlptLevel = jlpt;
    else if (jlpt) validated.jlptLevel = 'all';

    const goal = params.get('goal');
    if (goal && VALID_GOALS.includes(goal)) validated.goal = goal;

    const format = params.get('format');
    if (format && VALID_FORMATS.includes(format)) validated.format = format;

    const price = params.get('price');
    if (price && VALID_PRICES.includes(price)) validated.priceRange = price;

    const schedule = params.get('schedule');
    if (schedule && VALID_SCHEDULES.includes(schedule)) validated.schedule = schedule;

    const sort = params.get('sort') || params.get('sortBy');
    if (sort && VALID_SORTS.includes(sort)) validated.sortBy = sort;

    const page = parseInt(params.get('page'), 10);
    if (!isNaN(page) && page >= 1) validated.currentPage = page;

    return validated;
  }

  // 3. Khôi phục State từ URL Query Params (T006)
  function syncStateFromUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    const validated = validateUrlParams(urlParams);

    state.keyword = validated.keyword || '';
    state.location = validated.location || '';
    state.jlptLevel = validated.jlptLevel || 'all';
    state.goal = validated.goal || '';
    state.format = validated.format || '';
    state.priceRange = validated.priceRange || '';
    state.schedule = validated.schedule || '';
    state.sortBy = validated.sortBy || 'rating';
    state.currentPage = validated.currentPage || 1;

    syncFiltersWithState();
  }

  // 4. Đồng bộ State lên các UI Controls
  function syncFiltersWithState() {
    if (searchInput) searchInput.value = state.keyword;
    if (filterLocation) filterLocation.value = state.location;
    if (filterGoal) filterGoal.value = state.goal;
    if (filterFormat) filterFormat.value = state.format;
    if (filterPrice) filterPrice.value = state.priceRange;
    if (filterSchedule) filterSchedule.value = state.schedule;
    if (sortSelect) sortSelect.value = state.sortBy;
    syncJlptPills();
  }

  // Đồng bộ giao diện Pill JLPT (Hiện xanh nổi bật khi được chọn)
  function syncJlptPills() {
    jlptPills.forEach(pill => {
      const level = pill.getAttribute('data-level');
      if (level === state.jlptLevel) {
        pill.className = 'jlpt-chip px-space-md py-1 rounded-full bg-[#006194] text-white border border-[#006194] font-label-sm text-label-sm font-semibold transition-all shadow-sm cursor-pointer';
      } else {
        pill.className = 'jlpt-chip px-space-md py-1 rounded-full bg-white border border-[#E5DECB] hover:bg-[#FAF8F2] text-[#0F2545] font-label-sm text-label-sm transition-colors cursor-pointer';
      }
    });
  }

  // 5. Cập nhật URL từ State với PushState hoặc ReplaceState (T007)
  function updateUrlFromState(shouldPush = true) {
    if (isPopStateHandling) return;

    const params = new URLSearchParams();

    if (state.keyword && state.keyword.trim()) params.set('q', state.keyword.trim());
    if (state.location && state.location.trim()) params.set('location', state.location.trim());
    if (state.jlptLevel && state.jlptLevel !== 'all') params.set('jlpt', state.jlptLevel);
    if (state.goal) params.set('goal', state.goal);
    if (state.format) params.set('format', state.format);
    if (state.priceRange) params.set('price', state.priceRange);
    if (state.schedule) params.set('schedule', state.schedule);
    if (state.sortBy && state.sortBy !== 'rating') params.set('sort', state.sortBy);
    if (state.currentPage && state.currentPage > 1) params.set('page', state.currentPage);

    const queryString = params.toString();
    const currentPath = window.location.pathname;
    const basePath = (currentPath.startsWith('/tutors') || currentPath.startsWith('/pages/tutor-discovery'))
      ? currentPath
      : '/tutors';

    const newUrl = queryString ? `${basePath}?${queryString}` : basePath;
    const currentRelativeUrl = window.location.pathname + window.location.search;

    if (newUrl === currentRelativeUrl) return;

    if (shouldPush) {
      window.history.pushState({}, '', newUrl);
    } else {
      window.history.replaceState({}, '', newUrl);
    }
  }

  // 6. Lắng nghe sự kiện Back/Forward của trình duyệt (T008)
  window.addEventListener('popstate', () => {
    isPopStateHandling = true;
    syncStateFromUrl();
  loadLocationOptions();
    loadTutors();
    isPopStateHandling = false;
  });

  // 7. Gọi REST API nạp dữ liệu gia sư thật & Chống Race Condition (T023)
  async function loadTutors() {
    const requestId = ++currentRequestId;

    // Trạng thái Loading
    if (tutorGrid) {
      tutorGrid.innerHTML = `
        <div class="col-span-full py-16 flex flex-col items-center justify-center text-on-surface-variant gap-3">
          <span class="material-symbols-outlined text-primary text-[40px] animate-spin">progress_activity</span>
          <p class="text-sm font-medium">Đang tải danh sách gia sư từ máy chủ...</p>
        </div>
      `;
    }
    if (emptyState) emptyState.classList.add('hidden');

    try {
      const searchParams = {
        keyword: state.keyword,
        location: state.location,
        jlptLevel: state.jlptLevel,
        goal: state.goal,
        format: state.format,
        schedule: state.schedule,
        sortBy: state.sortBy,
        page: state.currentPage - 1,
        size: state.pageSize
      };

      // Ánh xạ khoảng học phí VNĐ (T017 / T3.5)
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

      // Nếu có request mới hơn đã được phát đi, hủy bỏ kết quả cũ (Chống Race Condition)
      if (requestId !== currentRequestId) return;

      const items = (response && response.content) ? response.content : [];
      const totalElements = (response && response.totalElements !== undefined) ? response.totalElements : items.length;
      const totalPages = (response && response.totalPages !== undefined) ? response.totalPages : 1;

      // Xử lý tình huống biên: Nếu trang hiện tại vượt quá tổng số trang, đưa về trang 1
      if (totalPages > 0 && state.currentPage > totalPages) {
        state.currentPage = 1;
        updateUrlFromState(false);
        loadTutors();
        return;
      }

      currentLoadedTutors = items.map(item => ({
        id: item.tutorId,
        tutorCode: item.tutorCode,
        fullName: item.fullName || 'Gia sư Kizuna',
        avatarUrl: item.avatarUrl || 'https://lh3.googleusercontent.com/aida-public/AB6AXuDfWr7LjTJCoD0Gf2VNK0QdLAElZ9cyCzJkQ8IOJ7ZhfKFRiPoiK-N6PJaofpuz4ZSh0MCTyzeYN1pX3x0Uaq5OcxZn8yQJ0H4V9S7DtsrI2aXdgxRQ0o8NMGuIO63NK6Y74ZNBD0G7rqPan3Z9H-N80LBRdViJvA9zYNeJLWG53XmbteXaiLIE6gVUR1BHJBqSmUiPALj-L4qvCcRXCinX5eARKyo5T6tbevnMHOs',
        isOnline: true,
        verified: (item.verified !== undefined ? item.verified : item.isVerified) ?? true,
        // Tiêu đề bài đăng đã được cán bộ học vụ duyệt (nếu có)
        headline: item.postTitle || item.headline || 'Gia sư tiếng Nhật',
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

      if (resultsCount) resultsCount.textContent = totalElements;
      if (totalCountEl) totalCountEl.textContent = totalElements;

      if (totalElements === 0) {
        // Trạng thái Rỗng (Empty State - MSG01)
        if (tutorGrid) tutorGrid.innerHTML = '';
        if (emptyState) emptyState.classList.remove('hidden');
        if (paginationSection) paginationSection.classList.add('hidden');
      } else {
        // Trạng thái Thành công (Success)
        if (emptyState) emptyState.classList.add('hidden');
        if (tutorGrid) {
          tutorGrid.innerHTML = currentLoadedTutors.map(renderTutorCard).join('');
        }
        renderPagination(totalPages, state.currentPage, totalElements, state.pageSize);
      }
    } catch (err) {
      if (requestId !== currentRequestId) return;
      console.error('Không thể kết nối tải dữ liệu gia sư:', err);

      // Trạng thái Lỗi mạng (Error State) thân thiện kèm nút Thử lại
      if (tutorGrid) {
        tutorGrid.innerHTML = `
          <div class="col-span-full p-12 bg-white rounded-2xl border border-red-200 text-center space-y-4 max-w-lg mx-auto my-6 shadow-sm">
            <div class="w-16 h-16 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto">
              <span class="material-symbols-outlined text-[32px]">cloud_off</span>
            </div>
            <div class="space-y-1">
              <h3 class="text-lg font-bold text-[#0F2545]">Không thể kết nối đến máy chủ</h3>
              <p class="text-xs text-on-surface-variant max-w-sm mx-auto leading-relaxed">
                Đã có sự cố khi tải danh sách gia sư. Vui lòng kiểm tra lại kết nối và thử lại.
              </p>
            </div>
            <button id="retry-search-btn" type="button" class="px-5 py-2.5 rounded-lg bg-primary hover:bg-primary-container text-white text-xs font-semibold transition-colors inline-flex items-center gap-1.5 shadow-sm">
              <span class="material-symbols-outlined text-[16px]">refresh</span>
              <span>Thử lại</span>
            </button>
          </div>
        `;
        const retryBtn = document.getElementById('retry-search-btn');
        if (retryBtn) retryBtn.addEventListener('click', () => loadTutors());
      }
      if (emptyState) emptyState.classList.add('hidden');
      if (paginationSection) paginationSection.classList.add('hidden');
    }

    renderActiveChips();
  }

  // 8. Render thẻ gia sư với link dynamic route /gs/:tutorCode (T022)
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
            <button class="w-1/2 sm:w-auto px-space-md py-2.5 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-label-md text-label-md font-semibold transition-all shadow-sm flex items-center justify-center gap-1.5 cursor-pointer" type="button" onclick="window.handleOpenConnect('${tutor.id}')">
              <span class="material-symbols-outlined text-[16px]">calendar_add_on</span>
              <span>Đặt lịch học thử</span>
            </button>
          </div>
        </div>
      </article>
    `;
  }

  // 9. Hiện thực phân trang hai chiều Frontend ↔ Backend (T021)
  function renderPagination(totalPages, currentPage, totalElements, pageSize) {
    if (!paginationSection || !paginationNav) return;

    if (totalElements === 0) {
      paginationSection.classList.add('hidden');
      return;
    }

    paginationSection.classList.remove('hidden');

    const totalPagesCount = Math.max(1, totalPages);
    const start = (currentPage - 1) * pageSize + 1;
    const end = Math.min(currentPage * pageSize, totalElements);
    if (currentRangeEl) currentRangeEl.textContent = `${start} - ${end}`;
    if (totalCountEl) totalCountEl.textContent = totalElements;

    let navHtml = `
      <button class="px-space-md py-2 rounded-lg bg-white border border-[#E5DECB] hover:bg-[#FAF8F2] text-[#3F4850] text-label-sm font-label-sm font-semibold transition-colors flex items-center gap-1 ${currentPage <= 1 ? 'pointer-events-none opacity-50 cursor-not-allowed' : 'cursor-pointer'}" 
              type="button" 
              data-page="${currentPage - 1}"
              ${currentPage <= 1 ? 'disabled' : ''}>
        <span class="material-symbols-outlined text-[16px]">chevron_left</span>
        <span class="hidden sm:inline">Trước</span>
      </button>
    `;

    for (let i = 1; i <= totalPagesCount; i++) {
      if (i === currentPage) {
        navHtml += `
          <button class="w-9 h-9 rounded-lg bg-primary text-white font-label-sm text-label-sm font-semibold shadow-sm cursor-default" type="button">${i}</button>
        `;
      } else {
        navHtml += `
          <button class="w-9 h-9 rounded-lg bg-white border border-[#E5DECB] hover:bg-[#FAF8F2] text-[#3F4850] font-label-sm text-label-sm font-semibold transition-colors cursor-pointer" type="button" data-page="${i}">${i}</button>
        `;
      }
    }

    navHtml += `
      <button class="px-space-md py-2 rounded-lg bg-white border border-[#E5DECB] hover:bg-[#FAF8F2] text-[#3F4850] text-label-sm font-label-sm font-semibold transition-colors flex items-center gap-1 ${currentPage >= totalPagesCount ? 'pointer-events-none opacity-50 cursor-not-allowed' : 'cursor-pointer'}" 
              type="button" 
              data-page="${currentPage + 1}"
              ${currentPage >= totalPagesCount ? 'disabled' : ''}>
        <span class="hidden sm:inline">Tiếp theo</span>
        <span class="material-symbols-outlined text-[16px]">chevron_right</span>
      </button>
    `;

    paginationNav.innerHTML = navHtml;

    paginationNav.querySelectorAll('button[data-page]').forEach(btn => {
      btn.addEventListener('click', () => {
        const targetPage = parseInt(btn.getAttribute('data-page'), 10);
        if (targetPage >= 1 && targetPage <= totalPagesCount && targetPage !== state.currentPage) {
          state.currentPage = targetPage;
          updateUrlFromState(true);
          loadTutors();
          window.scrollTo({ top: 320, behavior: 'smooth' });
        }
      });
    });
  }

  // 10. Chip bộ lọc đang áp dụng & Xóa từng chip đồng bộ URL (T019)
  function renderActiveChips() {
    if (!activeChipsContainer) return;
    const chips = [];

    if (state.keyword.trim()) chips.push({ key: 'keyword', label: `Từ khóa: "${state.keyword}"` });
    if (state.location && state.location.trim()) chips.push({ key: 'location', label: `Khu vực: "${state.location}"` });
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
        <button type="button" class="hover:text-error ml-1 text-[14px] leading-none cursor-pointer" onclick="window.removeFilter('${chip.key}')">×</button>
      </span>
    `).join('');
  }

  window.removeFilter = function (key) {
    if (key === 'keyword') { state.keyword = ''; if (searchInput) searchInput.value = ''; }
    if (key === 'location') { state.location = ''; if (filterLocation) filterLocation.value = ''; }
    if (key === 'jlptLevel') { state.jlptLevel = 'all'; syncJlptPills(); }
    if (key === 'goal') { state.goal = ''; if (filterGoal) filterGoal.value = ''; }
    if (key === 'format') { state.format = ''; if (filterFormat) filterFormat.value = ''; }
    if (key === 'priceRange') { state.priceRange = ''; if (filterPrice) filterPrice.value = ''; }
    if (key === 'schedule') { state.schedule = ''; if (filterSchedule) filterSchedule.value = ''; }
    state.currentPage = 1;
    updateUrlFromState(true);
    loadTutors();
  };

  // 11. Đặt lịch học thử 1-1 với gia sư (Phương án 1)
  window.handleOpenConnect = function (tutorId) {
    const tutor = currentLoadedTutors.find(t => t.id === tutorId);
    if (!tutor) return;

    const targetUrl = (tutor.tutorCode)
      ? `/gs/${tutor.tutorCode}?connect=true`
      : `/pages/tutor-profile/?id=${tutor.id}&connect=true`;

    window.location.href = targetUrl;
  };

  // 12. Gắn sự kiện người dùng tương tác với bộ lọc (T015, T016, T017, T018, T020)

  // Sự kiện Pill JLPT (T016)
  jlptPills.forEach(pill => {
    pill.addEventListener('click', () => {
      state.jlptLevel = pill.getAttribute('data-level');
      state.currentPage = 1;
      syncJlptPills();
      updateUrlFromState(true);
      loadTutors();
    });
  });

  // Sự kiện Debounce ô tìm kiếm 300ms + replaceState (T015)
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      state.keyword = e.target.value;
      state.currentPage = 1;
      updateUrlFromState(false);
      loadTutors();
    }, 300));
  }

  if (searchBtn) {
    searchBtn.addEventListener('click', () => {
      if (searchInput) state.keyword = searchInput.value;
      state.currentPage = 1;
      updateUrlFromState(true);
      loadTutors();
    });
  }

  // Sự kiện 5 dropdown bộ lọc (Khu vực, Mục tiêu, Hình thức, Học phí, Lịch rảnh)
  [
    { el: filterLocation, prop: 'location' },
    { el: filterGoal, prop: 'goal' },
    { el: filterFormat, prop: 'format' },
    { el: filterPrice, prop: 'priceRange' },
    { el: filterSchedule, prop: 'schedule' }
  ].forEach(({ el, prop }) => {
    if (el) {
      el.addEventListener('change', (e) => {
        state[prop] = e.target.value;
        state.currentPage = 1;
        updateUrlFromState(true);
        loadTutors();
      });
    }
  });

  // Sự kiện dropdown sắp xếp (T018)
  if (sortSelect) {
    sortSelect.addEventListener('change', (e) => {
      state.sortBy = e.target.value;
      updateUrlFromState(true);
      loadTutors();
    });
  }

  // Hàm đặt lại toàn bộ bộ lọc và làm sạch URL (T020)
  function resetAllFilters() {
    state.keyword = '';
    state.location = '';
    state.jlptLevel = 'all';
    state.goal = '';
    state.format = '';
    state.priceRange = '';
    state.schedule = '';
    state.sortBy = 'rating';
    state.currentPage = 1;
    syncFiltersWithState();
    updateUrlFromState(true);
    loadTutors();
  }

  if (resetBtn) resetBtn.addEventListener('click', resetAllFilters);
  if (emptyResetBtn) emptyResetBtn.addEventListener('click', resetAllFilters);

  // 12.1 Nạp danh sách các tỉnh/thành phố thực tế từ database của các gia sư (FE-04)
  async function loadLocationOptions() {
    try {
      const locations = await API.tutors.getLocations();
      if (!filterLocation) return;

      filterLocation.innerHTML = '<option value="">Tất cả khu vực</option>';
      if (Array.isArray(locations) && locations.length > 0) {
        locations.forEach(loc => {
          const opt = document.createElement('option');
          opt.value = loc;
          opt.textContent = loc;
          filterLocation.appendChild(opt);
        });
      }

      if (state.location) {
        filterLocation.value = state.location;
      }
    } catch (err) {
      console.warn('Lỗi khi nạp danh sách địa điểm gia sư:', err);
    }
  }

  // 13. Khởi tạo trang: Khôi phục State từ URL ngay khi vào trang và tải dữ liệu
  syncStateFromUrl();
  loadLocationOptions();
  loadTutors();
});
