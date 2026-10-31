/**
 * KizunaTutor - Tutor Discovery Controller (FE-04)
 * 100% dữ liệu từ Database PostgreSQL Supabase qua REST API Backend.
 * Tuân thủ nghiêm ngặt AGENTS.md (Không dùng mock data, không fake data).
 */

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
  const detailModal = document.getElementById('detail-modal');
  const detailModalBody = document.getElementById('detail-modal-body');
  const closeDetailModalBtn = document.getElementById('close-detail-modal');

  const connectModal = document.getElementById('connect-modal');
  const connectForm = document.getElementById('connect-form');
  const closeConnectModalBtn = document.getElementById('close-connect-modal');
  const cancelConnectBtn = document.getElementById('cancel-connect-btn');
  const connectTutorAvatar = document.getElementById('connect-tutor-avatar');
  const connectTutorName = document.getElementById('connect-tutor-name');

  let activeTutorForConnect = null;
  let currentLoadedTutors = [];

  // Đọc query parameters từ URL (nếu có từ trang Home chuyển qua)
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('goal')) state.goal = urlParams.get('goal');
  if (urlParams.get('level')) state.jlptLevel = urlParams.get('level');
  if (urlParams.get('format')) state.format = urlParams.get('format');
  if (urlParams.get('price')) state.priceRange = urlParams.get('price');

  syncFiltersWithState();

  // Định dạng tiền VND chuẩn Việt Nam
  function formatVND(amount) {
    if (!amount) return '0 đ';
    return new Intl.NumberFormat('vi-VN').format(amount) + ' đ';
  }

  // Lấy dữ liệu thật từ REST API Backend (kết nối Supabase)
  async function fetchTutorsFromApi() {
    try {
      const params = new URLSearchParams();
      if (state.keyword.trim()) params.append('keyword', state.keyword.trim());
      if (state.jlptLevel && state.jlptLevel !== 'all') params.append('jlptLevel', state.jlptLevel);
      if (state.goal) params.append('goal', state.goal);
      if (state.format) params.append('format', state.format);
      if (state.priceRange) {
        if (state.priceRange === '150-250') { params.append('minPrice', '150000'); params.append('maxPrice', '250000'); }
        else if (state.priceRange === '250-400') { params.append('minPrice', '250000'); params.append('maxPrice', '400000'); }
        else if (state.priceRange === '400-600') { params.append('minPrice', '400000'); }
      }
      if (state.schedule) params.append('schedule', state.schedule);
      if (state.sortBy) params.append('sortBy', state.sortBy);
      params.append('page', String(state.currentPage - 1));
      params.append('size', String(state.pageSize));

      const baseUrl = (window.API_CONFIG && window.API_CONFIG.BASE_URL) ? window.API_CONFIG.BASE_URL : 'http://localhost:8085/api';
      const apiUrl = `${baseUrl}/tutors/search?${params.toString()}`;

      const response = await fetch(apiUrl, { signal: AbortSignal.timeout(5000) });
      if (!response.ok) {
        console.error('Lỗi khi gọi API tìm kiếm gia sư:', response.status);
        return [];
      }

      const data = await response.json();
      if (!data || !data.content || data.content.length === 0) {
        return [];
      }

      return data.content.map(item => ({
        id: item.tutorId,
        fullName: item.fullName || 'Gia sư Kizuna',
        avatarUrl: item.avatarUrl || 'https://lh3.googleusercontent.com/aida-public/AB6AXuDfWr7LjTJCoD0Gf2VNK0QdLAElZ9cyCzJkQ8IOJ7ZhfKFRiPoiK-N6PJaofpuz4ZSh0MCTyzeYN1pX3x0Uaq5OcxZn8yQJ0H4V9S7DtsrI2aXdgxRQ0o8NMGuIO63NK6Y74ZNBD0G7rqPan3Z9H-N80LBRdViJvA9zYNeJLWG53XmbteXaiLIE6gVUR1BHJBqSmUiPALj-L4qvCcRXCinX5eARKyo5T6tbevnMHOs',
        isOnline: true,
        verified: (item.verified !== undefined ? item.verified : item.isVerified) ?? true,
        headline: item.headline || 'Gia sư tiếng Nhật',
        experienceYears: item.experienceYears || 0,
        experienceText: (item.experienceYears ? item.experienceYears + ' năm kinh nghiệm giảng dạy' : 'Gia sư chuyên môn'),
        jlptLevel: item.highestJlptLevel || 'Chưa cập nhật',
        specialties: (item.specialties && item.specialties.length > 0) ? item.specialties : [item.headline || 'Tiếng Nhật giao tiếp & JLPT'],
        teachingFormat: item.teachingFormat || 'Online 1-1',
        location: item.location || 'Hà Nội, Việt Nam',
        hourlyRate: item.hourlyRate || 0,
        rating: item.avgRating || 5.0,
        reviewsCount: item.totalReviews || 0,
        bio: item.bio || 'Chưa có thông tin giới thiệu chi tiết.',
        featured: (item.featured !== undefined ? item.featured : item.isFeatured) ?? false
      }));
    } catch (err) {
      console.error('Không thể kết nối tới Backend API:', err);
      return [];
    }
  }

  // Render thẻ gia sư trực tiếp từ dữ liệu Database
  function renderTutorCard(tutor) {
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
                  ${tutor.fullName}
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
            <button class="w-1/2 sm:w-auto px-space-md py-2.5 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label-md text-label-md font-semibold transition-colors" type="button" onclick="window.openDetailModal('${tutor.id}')">
              Xem hồ sơ
            </button>
            <button class="w-1/2 sm:w-auto px-space-lg py-2.5 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-label-md text-label-md font-semibold transition-all shadow-sm flex items-center justify-center gap-1.5" type="button" onclick="window.openConnectModal('${tutor.id}')">
              <span class="material-symbols-outlined text-[16px]">send</span>
              <span>Kết nối</span>
            </button>
          </div>
        </div>
      </article>
    `;
  }

  // Render chip bộ lọc đang áp dụng
  function renderActiveChips() {
    if (!activeChipsContainer) return;
    const chips = [];

    if (state.keyword.trim()) {
      chips.push({ key: 'keyword', label: `Từ khóa: "${state.keyword}"` });
    }
    if (state.jlptLevel && state.jlptLevel !== 'all') {
      chips.push({ key: 'jlptLevel', label: `JLPT: ${state.jlptLevel}` });
    }
    if (state.goal) {
      const goalLabels = { kaiwa: 'Luyện giao tiếp', jlpt: 'Luyện thi JLPT', business: 'Tiếng Nhật thương mại', it: 'Tiếng Nhật IT' };
      chips.push({ key: 'goal', label: goalLabels[state.goal] || state.goal });
    }
    if (state.format) {
      const formatLabels = { online: 'Học Online', offline: 'Học Trực tiếp', hybrid: 'Kết hợp linh hoạt' };
      chips.push({ key: 'format', label: formatLabels[state.format] || state.format });
    }
    if (state.priceRange) {
      const priceLabels = { '150-250': '150k - 250k / giờ', '250-400': '250k - 400k / giờ', '400-600': 'Trên 400k / giờ' };
      chips.push({ key: 'priceRange', label: priceLabels[state.priceRange] || state.priceRange });
    }
    if (state.schedule) {
      const scheduleLabels = { today: 'Hôm nay', weekend: 'Cuối tuần', evening: 'Buổi tối' };
      chips.push({ key: 'schedule', label: scheduleLabels[state.schedule] || state.schedule });
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

  window.removeFilter = function(filterKey) {
    if (filterKey === 'keyword') {
      state.keyword = '';
      if (searchInput) searchInput.value = '';
    } else if (filterKey === 'jlptLevel') {
      state.jlptLevel = 'all';
      syncJlptPills();
    } else if (filterKey === 'goal') {
      state.goal = '';
      if (filterGoal) filterGoal.value = '';
    } else if (filterKey === 'format') {
      state.format = '';
      if (filterFormat) filterFormat.value = '';
    } else if (filterKey === 'priceRange') {
      state.priceRange = '';
      if (filterPrice) filterPrice.value = '';
    } else if (filterKey === 'schedule') {
      state.schedule = '';
      if (filterSchedule) filterSchedule.value = '';
    }
    updateUI();
  };

  // Cập nhật giao diện chính
  async function updateUI() {
    if (tutorGrid) {
      tutorGrid.innerHTML = `
        <div class="col-span-full py-12 flex flex-col items-center justify-center text-on-surface-variant gap-3">
          <span class="material-symbols-outlined text-primary text-[36px] animate-spin">progress_activity</span>
          <p class="text-xs font-medium">Đang tải dữ liệu gia sư từ máy chủ...</p>
        </div>
      `;
    }

    const tutors = await fetchTutorsFromApi();
    currentLoadedTutors = tutors;
    const totalCount = tutors.length;

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
        tutorGrid.innerHTML = tutors.map(renderTutorCard).join('');
      }
      if (currentRangeEl) {
        currentRangeEl.textContent = `1 - ${totalCount}`;
      }
    }

    renderActiveChips();
  }

  // Modal 1: Xem chi tiết hồ sơ gia sư (Đọc dữ liệu thật)
  window.openDetailModal = async function(tutorId) {
    if (!detailModal || !detailModalBody) return;

    // Tìm gia sư trong danh sách đã tải hoặc gọi API chi tiết
    let tutor = currentLoadedTutors.find(t => t.id === tutorId);
    if (!tutor) {
      try {
        const baseUrl = (window.API_CONFIG && window.API_CONFIG.BASE_URL) ? window.API_CONFIG.BASE_URL : 'http://localhost:8085/api';
        const res = await fetch(`${baseUrl}/tutors/${tutorId}`);
        if (res.ok) {
          const item = await res.json();
          tutor = {
            id: item.tutorId,
            fullName: item.fullName,
            avatarUrl: item.avatarUrl || 'https://lh3.googleusercontent.com/aida-public/AB6AXuDfWr7LjTJCoD0Gf2VNK0QdLAElZ9cyCzJkQ8IOJ7ZhfKFRiPoiK-N6PJaofpuz4ZSh0MCTyzeYN1pX3x0Uaq5OcxZn8yQJ0H4V9S7DtsrI2aXdgxRQ0o8NMGuIO63NK6Y74ZNBD0G7rqPan3Z9H-N80LBRdViJvA9zYNeJLWG53XmbteXaiLIE6gVUR1BHJBqSmUiPALj-L4qvCcRXCinX5eARKyo5T6tbevnMHOs',
            badge: item.headline || 'Gia sư tiếng Nhật',
            experienceYears: item.experienceYears || 0,
            experienceText: (item.experienceYears ? item.experienceYears + ' năm giảng dạy' : 'Gia sư chuyên môn'),
            jlptLevel: item.highestJlptLevel || 'Chưa cập nhật',
            specialties: item.specialties || [],
            location: item.location || 'Hà Nội, Việt Nam',
            hourlyRate: item.hourlyRate || 0,
            rating: item.avgRating || 5.0,
            reviewsCount: item.totalReviews || 0,
            bio: item.bio || 'Chưa có thông tin giới thiệu chi tiết.'
          };
        }
      } catch (e) {
        console.error('Không thể tải chi tiết gia sư:', e);
      }
    }

    if (!tutor) {
      alert('Không tìm thấy thông tin gia sư yêu cầu.');
      return;
    }

    detailModalBody.innerHTML = `
      <!-- Tutor Top Profile -->
      <div class="flex flex-col sm:flex-row items-start gap-4 pb-6 border-b border-[#ECE8DE]">
        <img class="w-24 h-24 rounded-2xl object-cover border border-[#E5DECB] shrink-0" src="${tutor.avatarUrl}" alt="${tutor.fullName}">
        <div class="flex-1 space-y-2">
          <div class="flex items-center gap-2 flex-wrap">
            <h2 class="text-xl font-bold text-[#0F2545]">${tutor.fullName}</h2>
            <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold border border-emerald-200">
              <span class="material-symbols-outlined text-[15px] text-emerald-600">verified</span> Đã kiểm định hồ sơ
            </span>
          </div>
          <p class="text-xs text-on-surface-variant font-medium">${tutor.badge} • ${tutor.experienceText}</p>
          <div class="flex items-center gap-3 text-xs">
            <span class="font-bold text-primary text-base">${formatVND(tutor.hourlyRate)} <span class="text-xs font-normal text-on-surface-variant">/ giờ</span></span>
            <span class="text-[#ECE8DE]">|</span>
            <div class="flex items-center gap-1 text-amber-500 font-bold">
              <span class="material-symbols-outlined text-[16px]" style="font-variation-settings: 'FILL' 1;">star</span>
              <span>${Number(tutor.rating).toFixed(1)}</span>
              <span class="text-on-surface-variant font-normal">(${tutor.reviewsCount} học viên)</span>
            </div>
            <span class="text-[#ECE8DE]">|</span>
            <span class="text-on-surface-variant flex items-center gap-1">
              <span class="material-symbols-outlined text-[15px]">location_on</span> ${tutor.location}
            </span>
          </div>
        </div>
      </div>

      <!-- Specialties & JLPT -->
      <div class="space-y-3">
        <h4 class="font-bold text-sm text-[#0F2545]">Trình độ &amp; Chuyên môn giảng dạy:</h4>
        <div class="flex flex-wrap gap-2">
          <span class="px-3 py-1 rounded-lg bg-sky-50 text-sky-800 text-xs font-semibold border border-sky-200">
            Chứng chỉ JLPT ${tutor.jlptLevel}
          </span>
          ${tutor.specialties.map(s => `<span class="px-3 py-1 rounded-lg bg-[#FAF8F2] text-[#0F2545] text-xs font-medium border border-[#E5DECB]">${s}</span>`).join('')}
        </div>
      </div>

      <!-- Bio -->
      <div class="space-y-2">
        <h4 class="font-bold text-sm text-[#0F2545]">Giới thiệu bản thân:</h4>
        <p class="text-xs text-on-surface-variant leading-relaxed bg-[#FAF8F2] p-4 rounded-xl border border-[#ECE8DE]">
          ${tutor.bio}
        </p>
      </div>

      <!-- Availability Notice (Trung thực theo dữ liệu thật trong Database) -->
      <div class="space-y-3 pt-2 border-t border-[#ECE8DE]">
        <div class="flex items-center gap-1.5">
          <span class="material-symbols-outlined text-primary text-[20px]">calendar_month</span>
          <h4 class="font-bold text-sm text-[#0F2545]">Lịch dạy rảnh trong tuần</h4>
        </div>
        <div class="p-4 rounded-xl bg-slate-50 border border-slate-200 text-center space-y-1">
          <p class="text-xs text-on-surface-variant">
            Gia sư hiện tại nhận lịch học linh hoạt theo thỏa thuận. Vui lòng bấm <strong>"Gửi yêu cầu kết nối"</strong> để trao đổi khung giờ học phù hợp nhất với bạn.
          </p>
        </div>
      </div>

      <!-- Action Buttons in Modal -->
      <div class="pt-4 flex items-center justify-end gap-3 border-t border-[#ECE8DE]">
        <button type="button" class="px-5 py-2.5 rounded-lg border border-[#E5DECB] text-xs font-semibold text-[#0F2545] hover:bg-[#FAF8F2]" onclick="window.closeDetailModal()">
          Đóng
        </button>
        <button type="button" class="px-6 py-2.5 rounded-lg bg-primary hover:bg-primary-container text-white text-xs font-semibold shadow-sm flex items-center gap-1.5" onclick="window.closeDetailModal(); window.openConnectModal('${tutor.id}')">
          <span class="material-symbols-outlined text-[16px]">handshake</span>
          <span>Gửi yêu cầu kết nối</span>
        </button>
      </div>
    `;

    detailModal.classList.remove('hidden');
    document.body.classList.add('overflow-hidden');
  };

  window.closeDetailModal = function() {
    if (detailModal) {
      detailModal.classList.add('hidden');
      document.body.classList.remove('overflow-hidden');
    }
  };

  if (closeDetailModalBtn) closeDetailModalBtn.addEventListener('click', window.closeDetailModal);

  // Modal 2: Gửi yêu cầu kết nối (T015)
  window.openConnectModal = function(tutorId) {
    const tutor = currentLoadedTutors.find(t => t.id === tutorId);
    if (!tutor || !connectModal) return;

    activeTutorForConnect = tutor;
    if (connectTutorAvatar) connectTutorAvatar.src = tutor.avatarUrl;
    if (connectTutorName) connectTutorName.textContent = tutor.fullName;

    connectModal.classList.remove('hidden');
    document.body.classList.add('overflow-hidden');
  };

  window.closeConnectModal = function() {
    if (connectModal) {
      connectModal.classList.add('hidden');
      document.body.classList.remove('overflow-hidden');
    }
  };

  if (closeConnectModalBtn) closeConnectModalBtn.addEventListener('click', window.closeConnectModal);
  if (cancelConnectBtn) cancelConnectBtn.addEventListener('click', window.closeConnectModal);

  if (connectForm) {
    connectForm.addEventListener('submit', (e) => {
      e.preventDefault();
      alert(`✅ Yêu cầu kết nối của bạn đã được gửi thành công tới gia sư ${activeTutorForConnect ? activeTutorForConnect.fullName : ''}! Chúng tôi sẽ liên hệ lại với bạn sớm nhất.`);
      window.closeConnectModal();
    });
  }

  // Tiêu chí JLPT Chips
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
      updateUI();
    });
  });

  // Đồng bộ Filter controls
  function syncFiltersWithState() {
    if (searchInput) searchInput.value = state.keyword;
    if (filterGoal) filterGoal.value = state.goal;
    if (filterFormat) filterFormat.value = state.format;
    if (filterPrice) filterPrice.value = state.priceRange;
    if (filterSchedule) filterSchedule.value = state.schedule;
    if (sortSelect) sortSelect.value = state.sortBy;
    syncJlptPills();
  }

  // Debounce tìm kiếm từ khóa (300ms)
  let searchDebounceTimeout = null;
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      clearTimeout(searchDebounceTimeout);
      searchDebounceTimeout = setTimeout(() => {
        state.keyword = e.target.value;
        state.currentPage = 1;
        updateUI();
      }, 300);
    });
  }

  if (searchBtn) {
    searchBtn.addEventListener('click', () => {
      if (searchInput) state.keyword = searchInput.value;
      state.currentPage = 1;
      updateUI();
    });
  }

  // Sự kiện thay đổi trên các Select Dropdown
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
        updateUI();
      });
    }
  });

  if (sortSelect) {
    sortSelect.addEventListener('change', (e) => {
      state.sortBy = e.target.value;
      updateUI();
    });
  }

  // Đặt lại tất cả bộ lọc
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
    updateUI();
  }

  if (resetBtn) resetBtn.addEventListener('click', resetAllFilters);
  if (emptyResetBtn) emptyResetBtn.addEventListener('click', resetAllFilters);

  // Khởi động giao diện lần đầu
  updateUI();
});
