/**
 * KizunaTutor - Home Page Logic
 * Tải danh sách gia sư thực tế từ REST API Backend, điều hướng bộ lọc nhanh,
 * quản lý lộ trình JLPT N5-N1 tương tác và khối câu hỏi thường gặp.
 */

import { API } from '../../js/api.js';
import { formatVND } from '../../js/main.js';
import {
  buildDiscoveryUrl,
  escapeHtml,
  getGridColumnsClass,
  getJlptTrackData,
  getTutorInitials,
  sanitizePublicLocation,
  selectDisplayTutors
} from './home-helpers.js';

function renderTutorSkeletonCards(count = 4) {
  return Array.from({ length: count }, () => `
    <div class="bg-white rounded-2xl p-6 border border-[#E5DECB] shadow-sm flex flex-col justify-between space-y-4 animate-pulse">
      <div class="space-y-4">
        <div class="flex items-start gap-3.5">
          <div class="w-14 h-14 rounded-2xl bg-[#ECE7DC] shrink-0"></div>
          <div class="flex-1 space-y-2 pt-1">
            <div class="h-4 bg-[#ECE7DC] rounded w-3/4"></div>
            <div class="h-3 bg-[#F2EDE2] rounded w-1/2"></div>
            <div class="h-3 bg-[#F2EDE2] rounded w-2/3"></div>
          </div>
        </div>
        <div class="flex gap-2">
          <div class="h-6 bg-[#F2EDE2] rounded-full w-16"></div>
          <div class="h-6 bg-[#F2EDE2] rounded-full w-24"></div>
        </div>
        <div class="space-y-2 pt-1">
          <div class="h-3 bg-[#F2EDE2] rounded w-full"></div>
          <div class="h-3 bg-[#F2EDE2] rounded w-5/6"></div>
        </div>
      </div>
      <div class="pt-4 border-t border-[#F0EBE0] flex items-center justify-between">
        <div class="h-5 bg-[#ECE7DC] rounded w-24"></div>
        <div class="h-9 bg-[#ECE7DC] rounded-xl w-24"></div>
      </div>
    </div>
  `).join('');
}

function renderTutorCard(tutor) {
  const profileUrl = tutor.tutorCode
    ? `/gs/${encodeURIComponent(tutor.tutorCode)}`
    : `/pages/tutor-profile/?id=${encodeURIComponent(tutor.tutorId)}`;
  const fullName = escapeHtml(tutor.fullName || 'Gia sư KizunaTutor');
  const headline = escapeHtml(tutor.postTitle || tutor.headline || 'Gia sư tiếng Nhật 1-1');
  const bio = escapeHtml(
    tutor.bio || 'Lộ trình cá nhân hóa theo mục tiêu JLPT và giao tiếp thực tế cùng học viên.'
  );
  const jlpt = escapeHtml(tutor.highestJlptLevel || 'N2');
  const safeLocation = escapeHtml(sanitizePublicLocation(tutor.location));
  const expYears = Number(tutor.experienceYears || 0);
  const reviewCount = Number(tutor.totalReviews || 0);
  const avgRating = Number(tutor.avgRating || 0).toFixed(1);
  const initials = escapeHtml(getTutorInitials(tutor.fullName));
  const isFeatured = tutor._sourceTag === 'featured';

  return `
    <article class="bg-white border border-[#E5DECB] rounded-2xl p-6 shadow-sm hover:shadow-[0_14px_30px_-14px_rgba(15,37,69,0.2)] hover:border-[#006194]/40 hover:-translate-y-0.5 transition-all flex flex-col justify-between group">
      <div class="space-y-4">
        <div class="flex items-start gap-3.5">
          ${tutor.avatarUrl
            ? `<img class="w-14 h-14 rounded-2xl object-cover border border-[#E5DECB] shrink-0 bg-[#FAF8F2]" src="${escapeHtml(tutor.avatarUrl)}" alt="${fullName}">`
            : `<div class="w-14 h-14 rounded-2xl bg-[#E0EFF7] text-[#006194] font-bold text-base flex items-center justify-center border border-[#CFE2EE] shrink-0">${initials}</div>`}
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between gap-2">
              <h3 class="font-bold text-[#0F2545] text-[15px] group-hover:text-primary transition-colors truncate">
                <a href="${profileUrl}">${fullName}</a>
              </h3>
              ${reviewCount > 0 ? `
                <div class="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-full bg-amber-50 border border-amber-200 text-amber-700 font-bold text-[11px] shrink-0">
                  <span class="material-symbols-outlined filled text-[14px] text-amber-500">star</span>
                  <span>${avgRating}</span>
                </div>
              ` : ''}
            </div>
            <p class="text-xs text-primary font-semibold truncate mt-0.5">${headline}</p>
            <div class="flex flex-wrap items-center gap-1.5 mt-2">
              <span class="px-2 py-0.5 rounded-md bg-sky-50 text-sky-900 text-[11px] font-bold border border-sky-200">
                JLPT ${jlpt}
              </span>
              ${tutor.verified || tutor.isVerified ? `
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-800 text-[11px] font-semibold border border-emerald-200">
                  <span class="material-symbols-outlined text-[13px]">verified</span>
                  <span>Đã xác thực</span>
                </span>
              ` : ''}
              ${isFeatured ? `
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-rose-50 text-[#9F1239] text-[11px] font-semibold border border-rose-200">
                  Tiêu biểu
                </span>
              ` : ''}
            </div>
          </div>
        </div>

        <p class="text-xs text-on-surface-variant line-clamp-2 leading-relaxed bg-[#FAF8F2] p-3.5 rounded-xl border border-[#ECE8DE]">
          ${bio}
        </p>

        <div class="flex flex-wrap items-center justify-between gap-2 text-[11px] text-[#52606D]">
          <span class="inline-flex items-center gap-1 font-medium">
            <span class="material-symbols-outlined text-[15px] text-[#006194]">work_history</span>
            <span>${expYears} năm kinh nghiệm</span>
          </span>
          ${safeLocation ? `
            <span class="inline-flex items-center gap-1 font-medium truncate max-w-[165px]">
              <span class="material-symbols-outlined text-[15px] text-[#006194]">location_on</span>
              <span class="truncate">${safeLocation}</span>
            </span>
          ` : `
            <span class="inline-flex items-center gap-1 font-medium">
              <span class="material-symbols-outlined text-[15px] text-[#006194]">laptop_chromebook</span>
              <span>Học 1-1 linh hoạt</span>
            </span>
          `}
        </div>
      </div>

      <div class="pt-4 mt-4 border-t border-[#ECE8DE] flex items-center justify-between gap-3">
        <div>
          <span class="text-[11px] text-on-surface-variant block">Học phí tham khảo</span>
          <span class="font-bold text-primary text-sm">
            ${formatVND(tutor.hourlyRate)}
            <span class="text-[11px] font-normal text-on-surface-variant">/ giờ</span>
          </span>
        </div>
        <a href="${profileUrl}" class="px-4 py-2 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-semibold shadow-sm transition-all shrink-0 inline-flex items-center gap-1">
          <span>Xem hồ sơ</span>
          <span class="material-symbols-outlined text-[15px]">arrow_forward</span>
        </a>
      </div>
    </article>
  `;
}

function renderSingleTutorCompanionCard() {
  return `
    <article class="bg-gradient-to-br from-[#0F2545] to-[#18355C] text-white rounded-2xl p-6 shadow-md flex flex-col justify-between space-y-5">
      <div class="space-y-3">
        <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-sky-200 text-xs font-semibold border border-white/15">
          <span class="material-symbols-outlined text-[15px]">explore</span>
          <span>Mở rộng tìm kiếm</span>
        </span>
        <h3 class="text-xl font-bold leading-snug">Khám phá toàn bộ hồ sơ gia sư hoặc đăng ký giảng dạy</h3>
        <p class="text-xs sm:text-sm text-slate-300 leading-relaxed">
          Sử dụng bộ lọc chi tiết theo tỉnh/thành, chứng chỉ JLPT, khung giờ rảnh thực tế hoặc gia nhập đội ngũ gia sư KizunaTutor.
        </p>
      </div>
      <div class="flex flex-wrap items-center gap-3 pt-2">
        <a href="/tutors" class="px-4 py-2.5 rounded-xl bg-white text-[#0F2545] hover:bg-slate-100 text-xs font-bold inline-flex items-center gap-1.5 transition-colors">
          <span>Mở bộ lọc đầy đủ</span>
          <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
        </a>
        <a href="/register" class="px-4 py-2.5 rounded-xl bg-amber-400 hover:bg-amber-300 text-[#0F2545] text-xs font-bold inline-flex items-center gap-1.5 transition-colors">
          <span>Trở thành gia sư</span>
        </a>
      </div>
    </article>
  `;
}

function renderEmptyOrErrorState({ isError = false } = {}) {
  return `
    <div class="col-span-full bg-white rounded-3xl border border-[#E5DECB] shadow-sm p-6 sm:p-8 lg:p-10 grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
      <div class="lg:col-span-8 space-y-3">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full ${isError ? 'bg-amber-50 text-amber-800 border-amber-200' : 'bg-sky-50 text-[#006194] border-sky-200'} border text-xs font-semibold">
          <span class="material-symbols-outlined text-[16px]">${isError ? 'cloud_off' : 'person_search'}</span>
          <span>${isError ? 'Tạm thời chưa tải được danh sách gia sư trực tuyến' : 'Chưa có hồ sơ gia sư tiêu biểu công khai'}</span>
        </div>
        <h3 class="text-xl font-bold text-[#0F2545]">
          ${isError
            ? 'Kết nối tới danh sách gia sư đang gián đoạn'
            : 'Hệ thống đang sẵn sàng đón nhận những gia sư đầu tiên'}
        </h3>
        <p class="text-sm text-on-surface-variant leading-relaxed max-w-2xl">
          ${isError
            ? 'Bạn có thể thử tải lại danh sách gia sư ngay bên dưới, chuyển sang trang tìm kiếm gia sư hoặc trải nghiệm ngay kho công cụ luyện viết Kanji, từ vựng và ngữ pháp JLPT.'
            : 'Khi gia sư hoàn tất xác thực chứng chỉ và được cán bộ học vụ duyệt bài đăng, hồ sơ sẽ hiển thị trực tiếp tại đây. Trong lúc chờ đợi, bạn có thể khám phá kho công cụ tự học hoặc đăng ký làm gia sư.'}
        </p>
        <div class="flex flex-wrap items-center gap-3 pt-1">
          ${isError ? `
            <button type="button" id="retry-featured-tutors-btn" class="px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-container text-white text-xs font-semibold inline-flex items-center gap-1.5 shadow-sm transition-colors cursor-pointer">
              <span class="material-symbols-outlined text-[16px]">refresh</span>
              <span>Thử tải lại danh sách</span>
            </button>
          ` : ''}
          <a href="/tutors" class="px-5 py-2.5 rounded-xl bg-[#FAF8F2] hover:bg-[#F1ECE0] text-[#0F2545] border border-[#DFD6C2] text-xs font-semibold inline-flex items-center gap-1.5 transition-colors">
            <span class="material-symbols-outlined text-[16px]">search</span>
            <span>Mở trang Tìm gia sư</span>
          </a>
          <a href="/register" class="px-5 py-2.5 rounded-xl bg-white hover:bg-slate-50 text-[#006194] border border-[#CFE2EE] text-xs font-semibold inline-flex items-center gap-1.5 transition-colors">
            <span class="material-symbols-outlined text-[16px]">school</span>
            <span>Ứng tuyển làm gia sư</span>
          </a>
        </div>
      </div>

      <div class="lg:col-span-4 bg-[#FAF8F2] rounded-2xl border border-[#ECE4D0] p-5 space-y-3">
        <p class="text-xs font-bold text-[#0F2545]">Gợi ý bắt đầu ngay không cần chờ:</p>
        <div class="space-y-2">
          <a href="/shodo" class="flex items-center justify-between p-3 rounded-xl bg-white border border-[#E5DECB] hover:border-[#006194] transition-colors text-xs font-semibold text-[#0F2545]">
            <span class="flex items-center gap-2">
              <span class="font-kanji text-base text-[#9F1239]">筆</span>
              <span>Phòng luyện viết Shodo</span>
            </span>
            <span class="material-symbols-outlined text-[16px] text-[#006194]">arrow_forward</span>
          </a>
          <a href="/handwriting" class="flex items-center justify-between p-3 rounded-xl bg-white border border-[#E5DECB] hover:border-[#006194] transition-colors text-xs font-semibold text-[#0F2545]">
            <span class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[17px] text-[#006194]">draw</span>
              <span>Tra Kanji viết tay AI</span>
            </span>
            <span class="material-symbols-outlined text-[16px] text-[#006194]">arrow_forward</span>
          </a>
          <a href="/grammar" class="flex items-center justify-between p-3 rounded-xl bg-white border border-[#E5DECB] hover:border-[#006194] transition-colors text-xs font-semibold text-[#0F2545]">
            <span class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[17px] text-[#006194]">menu_book</span>
              <span>Cẩm nang ngữ pháp JLPT</span>
            </span>
            <span class="material-symbols-outlined text-[16px] text-[#006194]">arrow_forward</span>
          </a>
        </div>
      </div>
    </div>
  `;
}

function updateHeroVerifiedBadge(totalElements) {
  const titleEl = document.getElementById('heroTutorBadgeTitle');
  const subtitleEl = document.getElementById('heroTutorBadgeSubtitle');
  if (!titleEl || !subtitleEl) return;

  if (typeof totalElements === 'number' && totalElements > 0) {
    titleEl.textContent = `${totalElements} hồ sơ gia sư`;
    subtitleEl.textContent = 'Đã duyệt & sẵn sàng dạy';
  } else {
    titleEl.textContent = 'Xác thực JLPT';
    subtitleEl.textContent = 'Kiểm duyệt bởi học vụ';
  }
}

async function loadFeaturedTutors() {
  const featuredContainer = document.getElementById('featured-tutors-grid');
  const noteEl = document.getElementById('featured-tutors-note');
  if (!featuredContainer) return;

  featuredContainer.className = 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5';
  featuredContainer.innerHTML = renderTutorSkeletonCards(4);

  const [featuredRes, searchRes] = await Promise.allSettled([
    API.tutors.getFeatured(),
    API.tutors.search({ page: 0, size: 4, sortBy: 'rating' })
  ]);

  const featuredOk = featuredRes.status === 'fulfilled';
  const searchOk = searchRes.status === 'fulfilled';

  if (!featuredOk && !searchOk) {
    updateHeroVerifiedBadge(null);
    featuredContainer.className = 'grid grid-cols-1 gap-5';
    featuredContainer.innerHTML = renderEmptyOrErrorState({ isError: true });
    if (noteEl) noteEl.textContent = 'Tạm gián đoạn kết nối dữ liệu';
    const retryBtn = document.getElementById('retry-featured-tutors-btn');
    if (retryBtn) retryBtn.addEventListener('click', loadFeaturedTutors);
    return;
  }

  const featuredList = featuredOk && Array.isArray(featuredRes.value) ? featuredRes.value : [];
  const searchPayload = searchOk && searchRes.value ? searchRes.value : {};
  const searchList = Array.isArray(searchPayload.content) ? searchPayload.content : [];
  const totalVerified = typeof searchPayload.totalElements === 'number' ? searchPayload.totalElements : searchList.length;

  updateHeroVerifiedBadge(totalVerified);

  const { tutors, hasFeatured, usedFallbackSearch } = selectDisplayTutors(featuredList, searchList, 4);

  if (tutors.length === 0) {
    featuredContainer.className = 'grid grid-cols-1 gap-5';
    featuredContainer.innerHTML = renderEmptyOrErrorState({ isError: false });
    if (noteEl) noteEl.textContent = 'Chưa có hồ sơ công khai';
    return;
  }

  if (noteEl) {
    if (hasFeatured && !usedFallbackSearch) {
      noteEl.textContent = `Đang hiển thị ${tutors.length} gia sư tiêu biểu`;
    } else if (hasFeatured && usedFallbackSearch) {
      noteEl.textContent = `Kết hợp gia sư tiêu biểu & đánh giá cao`;
    } else {
      noteEl.textContent = `Hiển thị ${tutors.length} gia sư đã duyệt nổi bật`;
    }
  }

  featuredContainer.className = `grid ${getGridColumnsClass(tutors.length)} gap-5 items-stretch`;
  let cardsHtml = tutors.map(renderTutorCard).join('');
  if (tutors.length === 1) {
    cardsHtml += renderSingleTutorCompanionCard();
  }
  featuredContainer.innerHTML = cardsHtml;
}

function initQuickSearchDock() {
  const keywordInput = document.getElementById('home-filter-keyword');
  const jlptSelect = document.getElementById('home-filter-jlpt');
  const goalSelect = document.getElementById('home-filter-goal');
  const formatSelect = document.getElementById('home-filter-format');
  const priceSelect = document.getElementById('home-filter-price');
  const searchBtn = document.getElementById('home-search-btn');

  const navigateWithFilters = () => {
    window.location.href = buildDiscoveryUrl({
      keyword: keywordInput ? keywordInput.value : '',
      jlpt: jlptSelect ? jlptSelect.value : '',
      goal: goalSelect ? goalSelect.value : '',
      format: formatSelect ? formatSelect.value : '',
      price: priceSelect ? priceSelect.value : ''
    });
  };

  if (searchBtn) {
    searchBtn.addEventListener('click', (event) => {
      event.preventDefault();
      navigateWithFilters();
    });
  }

  if (keywordInput) {
    keywordInput.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') {
        event.preventDefault();
        navigateWithFilters();
      }
    });
  }
}

function initJlptRoadmapTabs() {
  const tabButtons = document.querySelectorAll('[data-jlpt-tab]');
  if (tabButtons.length === 0) return;

  const markEl = document.getElementById('jlpt-detail-mark');
  const titleEl = document.getElementById('jlpt-detail-title');
  const summaryEl = document.getElementById('jlpt-detail-summary');
  const kanjiEl = document.getElementById('jlpt-detail-kanji');
  const vocabEl = document.getElementById('jlpt-detail-vocab');
  const outcomeEl = document.getElementById('jlpt-detail-outcome');
  const focusEl = document.getElementById('jlpt-detail-focus');
  const findTutorBtn = document.getElementById('jlpt-detail-tutor-btn');
  const practiceBtn = document.getElementById('jlpt-detail-tool-btn');

  const renderLevel = (level) => {
    const data = getJlptTrackData(level);
    tabButtons.forEach((btn) => {
      const isSelected = btn.getAttribute('data-jlpt-tab') === data.level;
      btn.setAttribute('aria-selected', isSelected ? 'true' : 'false');
      if (isSelected) {
        btn.className = 'jlpt-roadmap-tab px-4 py-2 rounded-xl bg-[#006194] text-white font-bold text-xs sm:text-sm shadow-sm transition-all cursor-pointer';
      } else {
        btn.className = 'jlpt-roadmap-tab px-4 py-2 rounded-xl bg-white text-[#0F2545] border border-[#E5DECB] hover:border-[#006194] hover:text-[#006194] font-semibold text-xs sm:text-sm transition-all cursor-pointer';
      }
    });

    if (markEl) markEl.textContent = data.kanjiMark;
    if (titleEl) titleEl.textContent = data.title;
    if (summaryEl) summaryEl.textContent = data.summary;
    if (kanjiEl) kanjiEl.textContent = data.kanjiCount;
    if (vocabEl) vocabEl.textContent = data.vocabCount;
    if (outcomeEl) outcomeEl.textContent = data.outcome;
    if (focusEl) focusEl.textContent = data.recommendedFocus;
    if (findTutorBtn) {
      findTutorBtn.href = buildDiscoveryUrl({ jlpt: data.level });
      const labelSpan = findTutorBtn.querySelector('[data-tutor-cta-text]');
      if (labelSpan) labelSpan.textContent = `Tìm gia sư luyện ${data.level}`;
    }
    if (practiceBtn) {
      practiceBtn.href = data.toolHref;
      const toolSpan = practiceBtn.querySelector('[data-tool-cta-text]');
      if (toolSpan) toolSpan.textContent = data.toolLabel;
    }
  };

  tabButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      renderLevel(btn.getAttribute('data-jlpt-tab'));
    });
  });

  renderLevel('N3');
}

function initFaqAccordion() {
  const faqList = document.getElementById('home-faq-list');
  if (!faqList) return;

  faqList.addEventListener('click', (event) => {
    const trigger = event.target.closest('[data-faq-trigger]');
    if (!trigger) return;

    const item = trigger.closest('[data-faq-item]');
    const panel = item ? item.querySelector('[data-faq-panel]') : null;
    const icon = trigger.querySelector('[data-faq-icon]');
    if (!panel) return;

    const isExpanded = trigger.getAttribute('aria-expanded') === 'true';

    faqList.querySelectorAll('[data-faq-item]').forEach((otherItem) => {
      const otherBtn = otherItem.querySelector('[data-faq-trigger]');
      const otherPanel = otherItem.querySelector('[data-faq-panel]');
      const otherIcon = otherBtn ? otherBtn.querySelector('[data-faq-icon]') : null;
      if (otherBtn && otherPanel) {
        otherBtn.setAttribute('aria-expanded', 'false');
        otherPanel.classList.add('hidden');
        if (otherIcon) otherIcon.classList.remove('rotate-180');
      }
    });

    if (!isExpanded) {
      trigger.setAttribute('aria-expanded', 'true');
      panel.classList.remove('hidden');
      if (icon) icon.classList.add('rotate-180');
    }
  });
}

function initHome() {
  initQuickSearchDock();
  initJlptRoadmapTabs();
  initFaqAccordion();
  loadFeaturedTutors();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initHome);
} else {
  initHome();
}
