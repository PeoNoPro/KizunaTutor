/**
 * KizunaTutor - Home Page Logic
 * Tải danh sách gia sư thực tế từ REST API Backend, điều hướng bộ lọc nhanh,
 * quản lý lộ trình JLPT N5-N1 tương tác và khối câu hỏi thường gặp.
 */

import { API } from '../../js/api.js';
import {
  buildDiscoveryUrl,
  escapeHtml,
  getJlptTrackData,
  getTutorInitials,
  selectDisplayTutors
} from './home-helpers.js';

/** Thẻ gia sư nổi bật: chỉ ảnh lớn và tên bên dưới; cả thẻ là liên kết tới hồ sơ. */
const TUTOR_CARDS_CLASS = 'flex flex-wrap justify-center gap-5 sm:gap-6';
const TUTOR_CARD_WIDTH = 'w-[calc(50%-10px)] sm:w-44 lg:w-52';

function renderTutorSkeletonCards(count = 4) {
  return Array.from({ length: count }, () => `
    <div class="${TUTOR_CARD_WIDTH} flex flex-col items-center animate-pulse">
      <div class="w-full aspect-square rounded-2xl bg-[#ECE7DC]"></div>
      <div class="h-4 bg-[#ECE7DC] rounded w-2/3 mt-3"></div>
    </div>
  `).join('');
}

function renderTutorCard(tutor) {
  const profileUrl = tutor.tutorCode
    ? `/gs/${encodeURIComponent(tutor.tutorCode)}`
    : `/pages/tutor-profile/?id=${encodeURIComponent(tutor.tutorId)}`;
  const fullName = escapeHtml(tutor.fullName || 'Gia sư KizunaTutor');
  const initials = escapeHtml(getTutorInitials(tutor.fullName));

  // alt rỗng: tên đã hiển thị ngay bên dưới ảnh, trình đọc màn hình không cần đọc hai lần
  return `
    <a href="${profileUrl}" class="${TUTOR_CARD_WIDTH} group flex flex-col items-center text-center focus:outline-none">
      <div class="w-full aspect-square rounded-2xl overflow-hidden bg-[#E0EFF7] border border-[#E5DECB] shadow-sm transition-all group-hover:-translate-y-1 group-hover:shadow-[0_14px_30px_-14px_rgba(15,37,69,0.35)] group-focus-visible:ring-2 group-focus-visible:ring-[#006194]">
        ${tutor.avatarUrl
          ? `<img class="w-full h-full object-cover" src="${escapeHtml(tutor.avatarUrl)}" alt="" loading="lazy">`
          : `<div class="w-full h-full flex items-center justify-center text-[#006194] font-bold text-4xl">${initials}</div>`}
      </div>
      <h3 class="mt-3 w-full font-bold text-[#0F2545] text-sm sm:text-base truncate group-hover:text-primary transition-colors">${fullName}</h3>
    </a>
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

  featuredContainer.className = TUTOR_CARDS_CLASS;
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

  featuredContainer.className = TUTOR_CARDS_CLASS;
  featuredContainer.innerHTML = tutors.map(renderTutorCard).join('');
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

function initScrollReveal() {
  const items = document.querySelectorAll('.reveal');
  if (!('IntersectionObserver' in window) || items.length === 0) return;

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add('is-visible');
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });

  document.documentElement.classList.add('js-reveal');
  items.forEach((el) => observer.observe(el));
}

function initHome() {
  initScrollReveal();
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
