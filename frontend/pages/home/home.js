/**
 * KizunaTutor - Home Page Logic
 * Tải danh sách gia sư thực tế từ REST API Backend và khối câu hỏi thường gặp.
 */

import { API } from '../../js/api.js';
import {
  escapeHtml,
  getTutorInitials,
  selectDisplayTutors
} from './home-helpers.js';

/** Thẻ gia sư nổi bật: ảnh vuông lớn, tên và môn dạy bên dưới; 5 thẻ một hàng trên màn hình rộng */
const FEATURED_COUNT = 5;
/* Flex căn giữa: đủ 5 thẻ thì lấp hàng, ít hơn thì dồn vào giữa thay vì để trống bên phải */
const TUTOR_CARDS_CLASS = 'flex flex-wrap justify-center gap-6';
const TUTOR_CARD_WIDTH = 'w-[calc(50%-0.75rem)] sm:w-[calc(33.333%-1rem)] lg:w-[calc(20%-1.2rem)]';

function renderTutorSkeletonCards(count = FEATURED_COUNT) {
  return Array.from({ length: count }, () => `
    <div class="animate-pulse ${TUTOR_CARD_WIDTH}">
      <div class="aspect-square rounded-element bg-line"></div>
      <div class="h-4 bg-line rounded-inner w-3/4 mt-3"></div>
      <div class="h-3 bg-line rounded-inner w-1/2 mt-2"></div>
    </div>
  `).join('');
}

function renderTutorCard(tutor) {
  const profileUrl = tutor.tutorCode
    ? `/gs/${encodeURIComponent(tutor.tutorCode)}`
    : `/pages/tutor-profile/?id=${encodeURIComponent(tutor.tutorId)}`;
  const fullName = escapeHtml(tutor.fullName || 'Gia sư KizunaTutor');
  const initials = escapeHtml(getTutorInitials(tutor.fullName));
  // Dòng phụ: môn dạy đầu tiên; chưa khai báo thì dùng trình độ JLPT
  const firstSubject = Array.isArray(tutor.specialties) && tutor.specialties.length ? tutor.specialties[0] : '';
  const subtitle = escapeHtml(firstSubject
    || (tutor.highestJlptLevel ? `Tiếng Nhật ${tutor.highestJlptLevel}` : 'Gia sư tiếng Nhật'));

  return `
    <a href="${profileUrl}" class="group block ${TUTOR_CARD_WIDTH} focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 rounded-element">
      <div class="aspect-square rounded-element overflow-hidden bg-primary-light border border-line">
        ${tutor.avatarUrl
          ? `<img class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-medium" src="${escapeHtml(tutor.avatarUrl)}" alt="${fullName}" loading="lazy">`
          : `<div class="w-full h-full flex items-center justify-center text-primary text-display-2">${initials}</div>`}
      </div>
      <h3 class="mt-3 px-2.5 text-heading-2 text-fg truncate group-hover:text-primary transition-colors duration-fast">${fullName}</h3>
      <p class="mt-1 px-2.5 text-body text-fg-muted truncate">${subtitle}</p>
    </a>
  `;
}

function renderEmptyOrErrorState({ isError = false } = {}) {
  return `
    <div class="col-span-full bg-card rounded-container border border-line shadow-low p-6 sm:p-8 grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
      <div class="lg:col-span-8 space-y-3">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full ${isError ? 'bg-warning-muted text-warning' : 'bg-primary-light text-primary'} text-caption font-semibold">
          <span class="material-symbols-outlined text-[16px]" aria-hidden="true">${isError ? 'cloud_off' : 'person_search'}</span>
          <span>${isError ? 'Tạm thời chưa tải được danh sách gia sư trực tuyến' : 'Chưa có hồ sơ gia sư tiêu biểu công khai'}</span>
        </div>
        <h3 class="text-heading-2 text-fg">
          ${isError
            ? 'Kết nối tới danh sách gia sư đang gián đoạn'
            : 'Hệ thống đang sẵn sàng đón nhận những gia sư đầu tiên'}
        </h3>
        <p class="text-body text-fg-muted max-w-2xl">
          ${isError
            ? 'Bạn có thể thử tải lại danh sách gia sư ngay bên dưới, chuyển sang trang tìm kiếm gia sư hoặc trải nghiệm ngay kho công cụ luyện viết Kanji, từ vựng và ngữ pháp JLPT.'
            : 'Khi gia sư hoàn tất xác thực chứng chỉ và được cán bộ học vụ duyệt bài đăng, hồ sơ sẽ hiển thị trực tiếp tại đây. Trong lúc chờ đợi, bạn có thể khám phá kho công cụ tự học hoặc đăng ký làm gia sư.'}
        </p>
        <div class="flex flex-wrap items-center gap-3 pt-1">
          ${isError ? `
            <button type="button" id="retry-featured-tutors-btn" class="px-5 py-2.5 rounded-element bg-primary hover:bg-primary-hover text-on-primary text-body font-semibold inline-flex items-center gap-1.5 transition-colors duration-fast cursor-pointer">
              <span class="material-symbols-outlined text-[16px]" aria-hidden="true">refresh</span>
              <span>Thử tải lại danh sách</span>
            </button>
          ` : ''}
          <a href="/tutors" class="px-5 py-2.5 rounded-element bg-card hover:bg-surface-muted text-fg border border-line-strong text-body font-semibold inline-flex items-center gap-1.5 transition-colors duration-fast">
            <span class="material-symbols-outlined text-[16px] text-primary" aria-hidden="true">search</span>
            <span>Mở trang Tìm gia sư</span>
          </a>
          <a href="/register" class="px-5 py-2.5 rounded-element bg-card hover:bg-surface-muted text-primary border border-line-strong text-body font-semibold inline-flex items-center gap-1.5 transition-colors duration-fast">
            <span class="material-symbols-outlined text-[16px]" aria-hidden="true">school</span>
            <span>Ứng tuyển làm gia sư</span>
          </a>
        </div>
      </div>

      <div class="lg:col-span-4 bg-surface-muted rounded-element p-5 space-y-3">
        <p class="text-caption font-semibold text-fg">Gợi ý bắt đầu ngay không cần chờ:</p>
        <div class="divide-y divide-line">
          <a href="/shodo" class="flex items-center justify-between py-2.5 text-body font-semibold text-fg hover:text-primary transition-colors duration-fast">
            <span class="flex items-center gap-2">
              <span class="font-kanji text-primary" aria-hidden="true">筆</span>
              <span>Phòng luyện viết Shodo</span>
            </span>
            <span class="material-symbols-outlined text-[16px] text-primary" aria-hidden="true">arrow_forward</span>
          </a>
          <a href="/handwriting" class="flex items-center justify-between py-2.5 text-body font-semibold text-fg hover:text-primary transition-colors duration-fast">
            <span class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[17px] text-primary" aria-hidden="true">draw</span>
              <span>Tra Kanji viết tay AI</span>
            </span>
            <span class="material-symbols-outlined text-[16px] text-primary" aria-hidden="true">arrow_forward</span>
          </a>
          <a href="/grammar" class="flex items-center justify-between py-2.5 text-body font-semibold text-fg hover:text-primary transition-colors duration-fast">
            <span class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[17px] text-primary" aria-hidden="true">menu_book</span>
              <span>Cẩm nang ngữ pháp JLPT</span>
            </span>
            <span class="material-symbols-outlined text-[16px] text-primary" aria-hidden="true">arrow_forward</span>
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
    titleEl.textContent = 'Gia sư tiếng Nhật 1-kèm-1';
    subtitleEl.textContent = 'Kiểm duyệt bởi cán bộ học vụ';
  }
}

async function loadFeaturedTutors() {
  const featuredContainer = document.getElementById('featured-tutors-grid');
  const noteEl = document.getElementById('featured-tutors-note');
  if (!featuredContainer) return;

  featuredContainer.className = TUTOR_CARDS_CLASS;
  featuredContainer.innerHTML = renderTutorSkeletonCards();

  const [featuredRes, searchRes] = await Promise.allSettled([
    API.tutors.getFeatured(),
    API.tutors.search({ page: 0, size: FEATURED_COUNT, sortBy: 'rating' })
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

  const { tutors, hasFeatured, usedFallbackSearch } = selectDisplayTutors(featuredList, searchList, FEATURED_COUNT);

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
  initFaqAccordion();
  loadFeaturedTutors();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initHome);
} else {
  initHome();
}
