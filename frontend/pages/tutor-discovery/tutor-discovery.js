/**
 * KizunaTutor - Tutor Discovery Controller (FE-04)
 * Quản lý tìm kiếm, lọc đa tiêu chí, phân trang, và đồng bộ URL hai chiều (Two-Way URL Sync).
 * Gọi dữ liệu 100% từ Database Supabase qua REST API (/js/api.js).
 */

import { API } from '../../js/api.js';
import { debounce } from '../../js/main.js';
import {
  escapeHtml,
  getTutorProfileUrl,
  normalizeTutorRecord,
  buildTutorCardHtml,
  buildPreviewPanelHtml,
  computePreviewOffsetPx,
  matchTutorTimeFilter
} from './tutor-discovery-helpers.js';

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
    tutorLevel: '',
    verifiedOnly: false,
    selectedShifts: new Set(),
    selectedDays: new Set(),
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
  const VALID_SHIFTS = ['morning', 'afternoon', 'evening'];
  const VALID_SORTS = ['rating', 'price_asc', 'price_desc', 'exp', 'newest'];

  // DOM Elements
  const searchInput = document.getElementById('search-input');
  const searchBtn = document.getElementById('search-button');
  const filterPillBar = document.getElementById('filter-pill-bar');
  const locationOptionsList = document.getElementById('location-options-list');
  const sortSelect = document.getElementById('sort-select');
  const resetBtn = document.getElementById('reset-filter-btn');
  const emptyResetBtn = document.getElementById('empty-reset-btn');
  const filterTutorLevel = document.getElementById('filter-tutor-level');
  const filterVerifiedOnly = document.getElementById('filter-verified-only');
  const shiftCheckboxes = document.querySelectorAll('.schedule-shift-checkbox');
  const dayCheckboxes = document.querySelectorAll('.schedule-day-checkbox');
  const btnClearScheduleSlots = document.getElementById('btn-clear-schedule-slots');
  const resultsLayout = document.getElementById('results-layout');
  const tutorCardsCol = document.getElementById('tutor-cards-col');
  const tutorGrid = document.getElementById('tutor-grid');
  const tutorPreviewAside = document.getElementById('tutor-preview-aside');
  const tutorPreviewPanel = document.getElementById('tutor-preview-panel');
  const emptyState = document.getElementById('empty-state');
  const resultsCount = document.getElementById('total-tutors-count');
  const activeChipsRow = document.getElementById('active-chips-row');
  const activeChipsContainer = document.getElementById('active-filter-chips');
  const paginationSection = document.getElementById('pagination-section');
  const paginationNav = document.getElementById('pagination-nav');
  const currentRangeEl = document.getElementById('current-range');
  const totalCountEl = document.getElementById('total-count-display');

  let currentLoadedTutors = [];
  let currentDisplayedTutors = [];
  let activePreviewTutorId = null;
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
    if (format && (VALID_FORMATS.includes(format) || format === 'all')) validated.format = format;

    const price = params.get('price');
    if (price && VALID_PRICES.includes(price)) validated.priceRange = price;

    const schedule = params.get('schedule');
    if (schedule && VALID_SCHEDULES.includes(schedule)) validated.schedule = schedule;

    const tutorLevel = params.get('tutorLevel');
    if (tutorLevel && tutorLevel.trim()) validated.tutorLevel = tutorLevel.trim();

    const verified = params.get('verified');
    if (verified === 'true') validated.verifiedOnly = true;

    const shiftsParam = params.get('shifts');
    if (shiftsParam && shiftsParam.trim()) {
      validated.selectedShifts = new Set(
        shiftsParam.split(',').map(s => s.trim()).filter(s => VALID_SHIFTS.includes(s))
      );
    }

    const daysParam = params.get('days');
    if (daysParam && daysParam.trim()) {
      validated.selectedDays = new Set(
        daysParam.split(',').map(d => parseInt(d.trim(), 10)).filter(d => d >= 1 && d <= 7)
      );
    }

    // Hỗ trợ tham số cũ ?slots=2-morning nếu người dùng truy cập link đã lưu
    const slots = params.get('slots');
    if (slots && slots.trim()) {
      validated.selectedShifts = validated.selectedShifts || new Set();
      validated.selectedDays = validated.selectedDays || new Set();
      slots.split(',').forEach(token => {
        const [dStr, shiftKey] = token.trim().split('-');
        const dayNum = parseInt(dStr, 10);
        if (dayNum >= 1 && dayNum <= 7) validated.selectedDays.add(dayNum);
        if (VALID_SHIFTS.includes(shiftKey)) validated.selectedShifts.add(shiftKey);
      });
    }

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
    state.tutorLevel = validated.tutorLevel || '';
    state.verifiedOnly = validated.verifiedOnly || false;
    state.selectedShifts = validated.selectedShifts || new Set();
    state.selectedDays = validated.selectedDays || new Set();
    state.sortBy = validated.sortBy || 'rating';
    state.currentPage = validated.currentPage || 1;

    syncFiltersWithState();
  }

  function closeAllDropdowns() {
    document.querySelectorAll('[data-dropdown-panel]').forEach(panel => {
      panel.classList.add('hidden');
    });
    document.querySelectorAll('[data-dropdown-trigger]').forEach(btn => {
      btn.setAttribute('aria-expanded', 'false');
    });
  }

  function toggleDropdown(name) {
    const targetPanel = document.querySelector(`[data-dropdown-panel="${name}"]`);
    const targetTrigger = document.querySelector(`[data-dropdown-trigger="${name}"]`);
    if (!targetPanel || !targetTrigger) return;
    const isCurrentlyHidden = targetPanel.classList.contains('hidden');
    closeAllDropdowns();
    if (isCurrentlyHidden) {
      targetPanel.classList.remove('hidden');
      targetTrigger.setAttribute('aria-expanded', 'true');
    }
  }

  function setPillStyle(key, labelText, isActive) {
    const labelEl = document.querySelector(`[data-pill-label="${key}"]`);
    const triggerBtn = document.querySelector(`[data-dropdown-trigger="${key}"]`);
    if (labelEl) labelEl.textContent = labelText;
    if (triggerBtn) {
      triggerBtn.className = isActive
        ? 'filter-pill-btn h-9 px-3.5 rounded-full bg-[#EBF4FF] border border-[#006194] text-[#004B73] text-[13px] font-semibold inline-flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap'
        : 'filter-pill-btn h-9 px-3.5 rounded-full bg-white border border-[#E5DECB] hover:border-[#0F2545] text-[#0F2545] text-[13px] font-medium inline-flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap';
    }
  }

  function syncOptionHighlight(filterType, activeValue) {
    document.querySelectorAll(`.filter-option-btn[data-filter-type="${filterType}"]`).forEach(btn => {
      const isSelected = String(btn.getAttribute('data-value') || '') === String(activeValue || '');
      btn.classList.toggle('bg-[#EBF4FF]', isSelected);
      btn.classList.toggle('text-[#004B73]', isSelected);
      btn.classList.toggle('font-semibold', isSelected);
      btn.classList.toggle('text-[#0F2545]', !isSelected);
    });
  }

  // 4. Đồng bộ State lên thanh bộ lọc 1 hàng ngang & các popover
  function syncFiltersWithState() {
    if (searchInput) searchInput.value = state.keyword;
    if (filterTutorLevel) filterTutorLevel.value = state.tutorLevel;
    if (filterVerifiedOnly) filterVerifiedOnly.checked = state.verifiedOnly;
    if (sortSelect) sortSelect.value = state.sortBy;

    const goalLabels = {
      kaiwa: 'Kaiwa',
      jlpt: 'Luyện thi JLPT',
      business: 'Thương mại',
      it: 'IT & BrSE'
    };
    setPillStyle('goal', state.goal ? (goalLabels[state.goal] || state.goal) : 'Môn học', Boolean(state.goal));
    syncOptionHighlight('goal', state.goal);

    const hasJlpt = Boolean(state.jlptLevel && state.jlptLevel !== 'all');
    setPillStyle('jlpt', hasJlpt ? `JLPT ${state.jlptLevel}` : 'Trình độ JLPT', hasJlpt);
    syncOptionHighlight('jlpt', state.jlptLevel || 'all');

    const timeCount = state.selectedShifts.size + state.selectedDays.size;
    const legacyScheduleMap = { today: 'Hôm nay', weekend: 'Cuối tuần', evening: 'Buổi tối' };
    const scheduleLabel = timeCount > 0
      ? `Thời gian học (${timeCount})`
      : (state.schedule ? (legacyScheduleMap[state.schedule] || state.schedule) : 'Thời gian học');
    setPillStyle('schedule', scheduleLabel, timeCount > 0 || Boolean(state.schedule));

    shiftCheckboxes.forEach(cb => {
      cb.checked = state.selectedShifts.has(cb.value);
    });
    dayCheckboxes.forEach(cb => {
      cb.checked = state.selectedDays.has(parseInt(cb.value, 10));
    });
    if (btnClearScheduleSlots) {
      btnClearScheduleSlots.classList.toggle('hidden', timeCount === 0);
    }

    const formatLabels = { online: 'Online 1-1', offline: 'Tại nhà', hybrid: 'Linh hoạt' };
    const hasFormat = Boolean(state.format && state.format !== 'all');
    setPillStyle('format', hasFormat ? (formatLabels[state.format] || state.format) : 'Hình thức học', hasFormat);
    syncOptionHighlight('format', hasFormat ? state.format : '');

    setPillStyle('location', state.location || 'Địa điểm', Boolean(state.location));
    syncOptionHighlight('location', state.location);

    const priceLabels = { '150-250': '150k - 250k', '250-400': '250k - 400k', '400-600': 'Trên 400k' };
    setPillStyle('price', state.priceRange ? (priceLabels[state.priceRange] || state.priceRange) : 'Học phí', Boolean(state.priceRange));
    syncOptionHighlight('priceRange', state.priceRange);

    const moreCount = (state.tutorLevel ? 1 : 0) + (state.verifiedOnly ? 1 : 0);
    setPillStyle('more', moreCount > 0 ? `Khác (${moreCount})` : 'Khác', moreCount > 0);
  }

  // 5. Cập nhật URL từ State với PushState hoặc ReplaceState (T007)
  function updateUrlFromState(shouldPush = true) {
    if (isPopStateHandling) return;

    const params = new URLSearchParams();

    if (state.keyword && state.keyword.trim()) params.set('q', state.keyword.trim());
    if (state.location && state.location.trim()) params.set('location', state.location.trim());
    if (state.jlptLevel && state.jlptLevel !== 'all') params.set('jlpt', state.jlptLevel);
    if (state.goal) params.set('goal', state.goal);
    if (state.format && state.format !== 'all') params.set('format', state.format);
    if (state.priceRange) params.set('price', state.priceRange);
    if (state.schedule) params.set('schedule', state.schedule);
    if (state.tutorLevel) params.set('tutorLevel', state.tutorLevel);
    if (state.verifiedOnly) params.set('verified', 'true');
    if (state.selectedShifts.size > 0) params.set('shifts', Array.from(state.selectedShifts).join(','));
    if (state.selectedDays.size > 0) params.set('days', Array.from(state.selectedDays).join(','));
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

  function setSplitLayoutMode(mode) {
    if (resultsLayout) {
      resultsLayout.classList.toggle('hidden', mode === 'empty');
    }
    if (tutorCardsCol) {
      if (mode === 'error') {
        tutorCardsCol.classList.remove('lg:col-span-8');
        tutorCardsCol.classList.add('lg:col-span-12');
      } else {
        tutorCardsCol.classList.remove('lg:col-span-12');
        tutorCardsCol.classList.add('lg:col-span-8');
      }
    }
    if (tutorPreviewAside) {
      tutorPreviewAside.classList.toggle('lg:hidden', mode === 'empty' || mode === 'error');
    }
    if (emptyState) {
      if (mode === 'empty') {
        emptyState.classList.remove('hidden');
        emptyState.classList.add('flex');
      } else {
        emptyState.classList.add('hidden');
        emptyState.classList.remove('flex');
      }
    }
  }

  let previewPositionRaf = null;

  function renderPreviewSkeleton() {
    if (!tutorPreviewPanel) return;
    tutorPreviewPanel.style.transform = 'translate3d(0, 0, 0)';
    tutorPreviewPanel.innerHTML = `
      <div class="bg-white border border-[#E5DECB] rounded-[12px] p-3.5 shadow-sm space-y-3 animate-pulse">
        <div class="w-full aspect-[16/10] rounded-[8px] bg-[#EFECE4]"></div>
        <div class="grid grid-cols-2 gap-2.5">
          <div class="h-10 rounded-[8px] bg-[#FAF8F2] border border-[#EFECE4]"></div>
          <div class="h-10 rounded-[8px] bg-[#FAF8F2] border border-[#EFECE4]"></div>
        </div>
      </div>
    `;
  }

  function syncPreviewPanelPosition() {
    if (previewPositionRaf !== null) {
      cancelAnimationFrame(previewPositionRaf);
    }
    previewPositionRaf = requestAnimationFrame(() => {
      previewPositionRaf = null;
      if (!tutorPreviewPanel || !tutorPreviewAside || !tutorGrid) return;
      if (tutorPreviewAside.offsetParent === null) {
        tutorPreviewPanel.style.transform = '';
        return;
      }

      const cards = Array.from(tutorGrid.querySelectorAll('[data-tutor-card]'));
      if (cards.length === 0) {
        tutorPreviewPanel.style.transform = 'translate3d(0, 0, 0)';
        return;
      }

      const activeCard = cards.find(c => c.getAttribute('data-tutor-id') === String(activePreviewTutorId)) || cards[0];
      const cardRect = activeCard.getBoundingClientRect();
      const asideRect = tutorPreviewAside.getBoundingClientRect();
      const offsetY = computePreviewOffsetPx({
        cardTopRelativeToAside: cardRect.top - asideRect.top,
        asideTopInViewport: asideRect.top,
        asideHeight: tutorPreviewAside.offsetHeight,
        panelHeight: tutorPreviewPanel.offsetHeight,
        stickyTopPx: 96
      });

      tutorPreviewPanel.style.transform = `translate3d(0, ${offsetY}px, 0)`;
    });
  }

  function setActivePreviewTutor(tutorId) {
    const matched = currentDisplayedTutors.find(t => String(t.id) === String(tutorId));
    if (!matched) return;
    const isChanged = String(activePreviewTutorId) !== String(matched.id);
    activePreviewTutorId = matched.id;

    if (tutorGrid) {
      tutorGrid.querySelectorAll('[data-tutor-card]').forEach(cardEl => {
        const isCurrent = cardEl.getAttribute('data-tutor-id') === String(matched.id);
        cardEl.setAttribute('aria-selected', isCurrent ? 'true' : 'false');
        cardEl.classList.toggle('tutor-card-selected', isCurrent);
        cardEl.classList.toggle('border-[#0F2545]', isCurrent);
        cardEl.classList.toggle('ring-1', isCurrent);
        cardEl.classList.toggle('ring-[#0F2545]/15', isCurrent);
        cardEl.classList.toggle('shadow-[0_10px_28px_rgba(15,37,69,0.10)]', isCurrent);
        cardEl.classList.toggle('border-[#E5DECB]', !isCurrent);
      });
    }

    if (tutorPreviewPanel && (isChanged || !tutorPreviewPanel.innerHTML.trim())) {
      tutorPreviewPanel.innerHTML = buildPreviewPanelHtml(matched);
    }

    syncPreviewPanelPosition();
  }

  // 7. Gọi REST API nạp dữ liệu gia sư thật & Chống Race Condition (T023)
  async function loadTutors() {
    const requestId = ++currentRequestId;

    // Trạng thái Loading
    setSplitLayoutMode('loading');
    renderPreviewSkeleton();
    if (tutorGrid) {
      tutorGrid.innerHTML = `
        <div class="py-16 flex flex-col items-center justify-center text-on-surface-variant gap-3 bg-white border border-[#E5DECB] rounded-[12px]">
          <span class="material-symbols-outlined text-primary text-[40px] animate-spin">progress_activity</span>
          <p class="text-sm font-medium">Đang tải danh sách gia sư từ máy chủ...</p>
        </div>
      `;
    }

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

      currentLoadedTutors = items.map(normalizeTutorRecord);

      // Áp dụng bộ lọc nâng cao (Trình độ gia sư, Hồ sơ xác thực, Lịch rảnh ô ma trận)
      let displayTutors = currentLoadedTutors;

      if (state.verifiedOnly) {
        displayTutors = displayTutors.filter(t => t.verified);
      }

      if (state.tutorLevel) {
        displayTutors = displayTutors.filter(t => matchTutorLevel(t, state.tutorLevel));
      }

      if (state.selectedShifts.size > 0 || state.selectedDays.size > 0) {
        displayTutors = displayTutors.filter(t => matchTutorTimeFilter(t, state.selectedShifts, state.selectedDays));
      }

      currentDisplayedTutors = displayTutors;
      const hasClientFilters = Boolean(state.verifiedOnly || state.tutorLevel || state.selectedShifts.size > 0 || state.selectedDays.size > 0);
      const totalMatched = hasClientFilters ? displayTutors.length : totalElements;
      const effectiveTotalPages = hasClientFilters
        ? Math.max(1, Math.ceil(totalMatched / state.pageSize))
        : totalPages;

      if (resultsCount) resultsCount.textContent = totalMatched;
      if (totalCountEl) totalCountEl.textContent = totalMatched;

      if (displayTutors.length === 0 || totalMatched === 0) {
        // Trạng thái Rỗng (Empty State - MSG01)
        if (tutorGrid) tutorGrid.innerHTML = '';
        if (tutorPreviewPanel) tutorPreviewPanel.innerHTML = '';
        if (paginationSection) paginationSection.classList.add('hidden');
        setSplitLayoutMode('empty');
      } else {
        // Trạng thái Thành công (Success)
        setSplitLayoutMode('results');
        const hasCurrentActive = displayTutors.some(t => String(t.id) === String(activePreviewTutorId));
        if (!hasCurrentActive) {
          activePreviewTutorId = displayTutors[0].id;
        }
        if (tutorGrid) {
          tutorGrid.innerHTML = displayTutors
            .map(t => buildTutorCardHtml(t, { isSelected: String(t.id) === String(activePreviewTutorId) }))
            .join('');
        }
        const selectedTutor = displayTutors.find(t => String(t.id) === String(activePreviewTutorId)) || displayTutors[0];
        if (tutorPreviewPanel) {
          tutorPreviewPanel.innerHTML = buildPreviewPanelHtml(selectedTutor);
        }
        syncPreviewPanelPosition();
        renderPagination(effectiveTotalPages, state.currentPage, totalMatched, state.pageSize);
      }
    } catch (err) {
      if (requestId !== currentRequestId) return;
      console.error('Không thể kết nối tải dữ liệu gia sư:', err);

      currentDisplayedTutors = [];
      activePreviewTutorId = null;
      if (tutorPreviewPanel) tutorPreviewPanel.innerHTML = '';
      if (paginationSection) paginationSection.classList.add('hidden');
      setSplitLayoutMode('error');

      // Trạng thái Lỗi mạng (Error State) thân thiện kèm nút Thử lại
      if (tutorGrid) {
        tutorGrid.innerHTML = `
          <div class="p-12 bg-white rounded-2xl border border-red-200 text-center space-y-4 max-w-lg mx-auto my-6 shadow-sm">
            <div class="w-16 h-16 rounded-full bg-red-50 text-red-600 flex items-center justify-center mx-auto">
              <span class="material-symbols-outlined text-[32px]">cloud_off</span>
            </div>
            <div class="space-y-1">
              <h3 class="text-lg font-bold text-[#0F2545]">Không thể kết nối đến máy chủ</h3>
              <p class="text-xs text-on-surface-variant max-w-sm mx-auto leading-relaxed">
                Đã có sự cố khi tải danh sách gia sư. Vui lòng kiểm tra lại kết nối và thử lại.
              </p>
            </div>
            <button id="retry-search-btn" type="button" class="px-5 py-2.5 rounded-lg bg-primary hover:bg-primary-container text-white text-xs font-semibold transition-colors inline-flex items-center gap-1.5 shadow-sm cursor-pointer">
              <span class="material-symbols-outlined text-[16px]">refresh</span>
              <span>Thử lại</span>
            </button>
          </div>
        `;
        const retryBtn = document.getElementById('retry-search-btn');
        if (retryBtn) retryBtn.addEventListener('click', () => loadTutors());
      }
    }

    renderActiveChips();
  }

  function openConnectForTutor(tutorId) {
    const tutor = currentLoadedTutors.find(t => String(t.id) === String(tutorId));
    if (!tutor) return;
    const baseUrl = getTutorProfileUrl(tutor);
    const separator = baseUrl.includes('?') ? '&' : '?';
    window.location.href = `${baseUrl}${separator}connect=true`;
  }

  if (tutorGrid) {
    tutorGrid.addEventListener('mouseover', (e) => {
      const card = e.target.closest('[data-tutor-card]');
      if (!card || !tutorGrid.contains(card)) return;
      const tid = card.getAttribute('data-tutor-id');
      if (tid) {
        setActivePreviewTutor(tid);
      }
    });

    tutorGrid.addEventListener('focusin', (e) => {
      const card = e.target.closest('[data-tutor-card]');
      if (!card || !tutorGrid.contains(card)) return;
      const tid = card.getAttribute('data-tutor-id');
      if (tid) {
        setActivePreviewTutor(tid);
      }
    });

    tutorGrid.addEventListener('click', (e) => {
      const connectBtn = e.target.closest('[data-action="connect"]');
      if (connectBtn) {
        e.preventDefault();
        openConnectForTutor(connectBtn.getAttribute('data-tutor-id'));
        return;
      }

      const toggleBioBtn = e.target.closest('[data-action="toggle-bio"]');
      if (toggleBioBtn) {
        e.preventDefault();
        const card = toggleBioBtn.closest('[data-tutor-card]');
        const bioEl = card ? card.querySelector('[data-bio-text]') : null;
        if (bioEl) {
          const isExpanded = toggleBioBtn.getAttribute('aria-expanded') === 'true';
          bioEl.classList.toggle('line-clamp-2', isExpanded);
          toggleBioBtn.setAttribute('aria-expanded', isExpanded ? 'false' : 'true');
          toggleBioBtn.textContent = isExpanded ? 'Xem thêm' : 'Thu gọn';
          syncPreviewPanelPosition();
        }
        return;
      }

      const card = e.target.closest('[data-tutor-card]');
      if (card) {
        const tid = card.getAttribute('data-tutor-id');
        if (tid) setActivePreviewTutor(tid);
      }
    });

    window.addEventListener('resize', debounce(() => {
      syncPreviewPanelPosition();
    }, 100));
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

  // Khớp trình độ chuyên môn gia sư
  function matchTutorLevel(tutor, level) {
    if (!level) return true;
    const text = `${tutor.fullName || ''} ${tutor.headline || ''} ${tutor.bio || ''} ${(tutor.specialties || []).join(' ')}`.toLowerCase();
    switch (level) {
      case 'teacher':
        return /giáo viên|chuyên nghiệp|sư phạm/i.test(text);
      case 'lecturer':
        return /giảng viên|đại học|thạc sĩ|tiến sĩ/i.test(text);
      case 'pedagogy':
        return /sư phạm|cử nhân/i.test(text);
      case 'overseas':
        return /du học|nhật bản|tokyo|kyoto|osaka/i.test(text);
      case 'native':
        return /bản xứ|người nhật|sensei|yamada|suzuki|tanaka|sato/i.test(text);
      case 'student':
        return /sinh viên/i.test(text);
      default:
        return true;
    }
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
    if (state.format && state.format !== 'all') {
      const map = { online: 'Học Online', offline: 'Tại nhà', hybrid: 'Linh hoạt' };
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
    if (state.tutorLevel) {
      const levelMap = {
        teacher: 'Giáo viên',
        lecturer: 'Giảng viên',
        pedagogy: 'Sư phạm',
        overseas: 'Du học sinh',
        native: 'Bản xứ (Sensei)',
        student: 'Sinh viên'
      };
      chips.push({ key: 'tutorLevel', label: `Trình độ: ${levelMap[state.tutorLevel] || state.tutorLevel}` });
    }
    if (state.verifiedOnly) {
      chips.push({ key: 'verifiedOnly', label: 'Hồ sơ đã xác thực' });
    }
    const shiftLabels = {
      morning: 'Buổi sáng',
      afternoon: 'Buổi chiều',
      evening: 'Buổi tối'
    };
    const dayLabels = {
      1: 'Chủ nhật',
      2: 'Thứ 2',
      3: 'Thứ 3',
      4: 'Thứ 4',
      5: 'Thứ 5',
      6: 'Thứ 6',
      7: 'Thứ 7'
    };

    state.selectedShifts.forEach(s => {
      chips.push({ key: `shift:${s}`, label: `Khung giờ: ${shiftLabels[s] || s}` });
    });
    state.selectedDays.forEach(d => {
      chips.push({ key: `day:${d}`, label: `Ngày học: ${dayLabels[d] || `Thứ ${d}`}` });
    });

    if (activeChipsRow) {
      activeChipsRow.classList.toggle('hidden', chips.length === 0);
      activeChipsRow.classList.toggle('flex', chips.length > 0);
    }

    if (chips.length === 0) {
      activeChipsContainer.innerHTML = '';
      return;
    }

    activeChipsContainer.innerHTML = chips.map(chip => `
      <span class="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-surface-container text-primary text-xs font-semibold border border-surface-variant">
        <span>${escapeHtml(chip.label)}</span>
        <button type="button" class="hover:text-error ml-1 text-[14px] leading-none cursor-pointer" data-remove-filter="${escapeHtml(chip.key)}" aria-label="Bỏ lọc ${escapeHtml(chip.label)}">×</button>
      </span>
    `).join('');
  }

  function removeFilterByKey(key) {
    if (key === 'keyword') state.keyword = '';
    if (key === 'location') state.location = '';
    if (key === 'jlptLevel') state.jlptLevel = 'all';
    if (key === 'goal') state.goal = '';
    if (key === 'format') state.format = '';
    if (key === 'priceRange') state.priceRange = '';
    if (key === 'schedule') state.schedule = '';
    if (key === 'tutorLevel') state.tutorLevel = '';
    if (key === 'verifiedOnly') state.verifiedOnly = false;
    if (key.startsWith('shift:')) state.selectedShifts.delete(key.slice(6));
    if (key.startsWith('day:')) state.selectedDays.delete(parseInt(key.slice(4), 10));

    syncFiltersWithState();
    state.currentPage = 1;
    updateUrlFromState(true);
    loadTutors();
  }

  if (activeChipsContainer) {
    activeChipsContainer.addEventListener('click', (e) => {
      const removeBtn = e.target.closest('[data-remove-filter]');
      if (!removeBtn) return;
      const key = removeBtn.getAttribute('data-remove-filter');
      if (key) removeFilterByKey(key);
    });
  }

  // 12. Gắn sự kiện cho thanh bộ lọc 1 hàng ngang & các popover

  if (filterPillBar) {
    filterPillBar.addEventListener('click', (e) => {
      const trigger = e.target.closest('[data-dropdown-trigger]');
      if (trigger) {
        e.stopPropagation();
        const name = trigger.getAttribute('data-dropdown-trigger');
        if (name) toggleDropdown(name);
        return;
      }

      const optionBtn = e.target.closest('.filter-option-btn');
      if (optionBtn) {
        const filterType = optionBtn.getAttribute('data-filter-type');
        const value = optionBtn.getAttribute('data-value') || '';
        if (filterType === 'goal') state.goal = value;
        else if (filterType === 'jlpt') state.jlptLevel = value || 'all';
        else if (filterType === 'format') state.format = value;
        else if (filterType === 'location') state.location = value;
        else if (filterType === 'priceRange') state.priceRange = value;

        closeAllDropdowns();
        syncFiltersWithState();
        state.currentPage = 1;
        updateUrlFromState(true);
        loadTutors();
      }
    });
  }

  document.addEventListener('click', (e) => {
    if (!e.target.closest('.filter-dropdown')) {
      closeAllDropdowns();
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeAllDropdowns();
    }
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

  // Sự kiện dropdown Trình độ gia sư (trong popover Khác)
  if (filterTutorLevel) {
    filterTutorLevel.addEventListener('change', (e) => {
      state.tutorLevel = e.target.value;
      syncFiltersWithState();
      state.currentPage = 1;
      updateUrlFromState(true);
      loadTutors();
    });
  }

  // Sự kiện checkbox Hồ sơ xác thực (trong popover Khác)
  if (filterVerifiedOnly) {
    filterVerifiedOnly.addEventListener('change', (e) => {
      state.verifiedOnly = e.target.checked;
      syncFiltersWithState();
      state.currentPage = 1;
      updateUrlFromState(true);
      loadTutors();
    });
  }

  // Sự kiện checkbox Khung giờ học & Ngày học (trong popover Thời gian học)
  shiftCheckboxes.forEach(cb => {
    cb.addEventListener('change', () => {
      if (cb.checked) state.selectedShifts.add(cb.value);
      else state.selectedShifts.delete(cb.value);
      syncFiltersWithState();
      state.currentPage = 1;
      updateUrlFromState(true);
      loadTutors();
    });
  });

  dayCheckboxes.forEach(cb => {
    cb.addEventListener('change', () => {
      const dayVal = parseInt(cb.value, 10);
      if (cb.checked) state.selectedDays.add(dayVal);
      else state.selectedDays.delete(dayVal);
      syncFiltersWithState();
      state.currentPage = 1;
      updateUrlFromState(true);
      loadTutors();
    });
  });

  // Sự kiện nút Bỏ chọn thời gian học
  if (btnClearScheduleSlots) {
    btnClearScheduleSlots.addEventListener('click', () => {
      state.selectedShifts.clear();
      state.selectedDays.clear();
      syncFiltersWithState();
      state.currentPage = 1;
      updateUrlFromState(true);
      loadTutors();
    });
  }

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
    state.tutorLevel = '';
    state.verifiedOnly = false;
    state.selectedShifts.clear();
    state.selectedDays.clear();
    state.sortBy = 'rating';
    state.currentPage = 1;
    closeAllDropdowns();
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
      if (!locationOptionsList) return;

      locationOptionsList.innerHTML = '';
      const allBtn = document.createElement('button');
      allBtn.type = 'button';
      allBtn.className = 'filter-option-btn w-full text-left px-3 py-2 rounded-xl text-[13px] text-[#0F2545] hover:bg-[#FAF8F2] transition-colors cursor-pointer';
      allBtn.setAttribute('data-filter-type', 'location');
      allBtn.setAttribute('data-value', '');
      allBtn.textContent = 'Tất cả khu vực';
      locationOptionsList.appendChild(allBtn);

      if (Array.isArray(locations) && locations.length > 0) {
        locations.forEach(loc => {
          const optBtn = document.createElement('button');
          optBtn.type = 'button';
          optBtn.className = 'filter-option-btn w-full text-left px-3 py-2 rounded-xl text-[13px] text-[#0F2545] hover:bg-[#FAF8F2] transition-colors cursor-pointer';
          optBtn.setAttribute('data-filter-type', 'location');
          optBtn.setAttribute('data-value', loc);
          optBtn.textContent = loc;
          locationOptionsList.appendChild(optBtn);
        });
      }

      syncFiltersWithState();
    } catch (err) {
      console.warn('Lỗi khi nạp danh sách địa điểm gia sư:', err);
    }
  }

  // 13. Khởi tạo trang: Khôi phục State từ URL ngay khi vào trang và tải dữ liệu
  syncStateFromUrl();
  loadLocationOptions();
  loadTutors();
});
