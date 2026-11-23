/**
 * KizunaTutor - Home Page Logic
 * Tải danh sách gia sư thực tế từ REST API Backend và khối câu hỏi thường gặp.
 */

import { API } from '../../js/api.js';
import {
  escapeHtml,
  getTutorInitials
} from './home-helpers.js';

/** Dải gia sư: carousel trượt theo trang, mỗi trang 5 / 3 / 2 thẻ (desktop / tablet / điện thoại) */
// shortcut: chỉ lấy tối đa 50 gia sư cho carousel, khi hệ thống đông hơn thì chuyển sang tải thêm theo trang
const ALL_TUTORS_LIMIT = 50;
const SLIDE_INTERVAL_MS = 3500;
const SLIDE_TRANSITION = 'transform 500ms ease-in-out';
const TUTOR_CARDS_CLASS = 'w-full max-w-[1100px] mx-auto overflow-hidden';
/* Bề rộng thẻ do JS tính (--card-w) để một trang vừa đúng số thẻ; khoảng cách dùng gap của hàng (--card-gap) */
const TUTOR_CARD_WIDTH = 'shrink-0 w-[var(--card-w,200px)]';

/** Số thẻ mỗi trang: 5 trên desktop, 3 trên tablet, 2 trên điện thoại */
function cardsPerPage() {
  if (window.matchMedia('(min-width: 1024px)').matches) return 5;
  if (window.matchMedia('(min-width: 640px)').matches) return 3;
  return 2;
}

/** Khoảng cách giữa thẻ: 24px desktop/tablet, 16px điện thoại */
function cardGap() {
  return window.matchMedia('(min-width: 640px)').matches ? 24 : 16;
}

/** Đặt --card-w / --card-gap để khung chứa vừa đúng một trang thẻ */
function fitCards(container) {
  const perPage = cardsPerPage();
  const gap = cardGap();
  container.style.setProperty('--card-gap', `${gap}px`);
  container.style.setProperty('--card-w', `${(container.clientWidth - (perPage - 1) * gap) / perPage}px`);
  return { perPage, gap };
}

function renderTutorSkeletonCards(count = cardsPerPage()) {
  return Array.from({ length: count }, () => `
    <div class="animate-pulse ${TUTOR_CARD_WIDTH}">
      <div class="aspect-square rounded-[6px] bg-line"></div>
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
    <a href="${profileUrl}" class="group block ${TUTOR_CARD_WIDTH} bg-white rounded-[6px] focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2">
      <div class="aspect-square rounded-[6px] overflow-hidden bg-primary-light">
        ${tutor.avatarUrl
          ? `<img class="w-full h-full object-cover" src="${escapeHtml(tutor.avatarUrl)}" alt="${fullName}" loading="lazy">`
          : `<div class="w-full h-full flex items-center justify-center text-primary text-display-2">${initials}</div>`}
      </div>
      <h3 class="mt-2.5 px-1 text-[15px] leading-5 font-semibold text-fg truncate group-hover:text-primary transition-colors duration-fast">${fullName}</h3>
      <p class="mt-0.5 px-1 pb-2 text-[13px] leading-[18px] text-fg-muted truncate">${subtitle}</p>
    </a>
  `;
}

function renderEmptyOrErrorState({ isError = false } = {}) {
  return `
    <div class="col-span-full bg-card rounded-container border border-line shadow-low p-6 sm:p-8 grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
      <div class="lg:col-span-8 space-y-3">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full ${isError ? 'bg-warning-muted text-warning' : 'bg-primary-light text-primary'} text-caption font-semibold">
          <span class="material-symbols-outlined text-[16px]" aria-hidden="true">${isError ? 'cloud_off' : 'person_search'}</span>
          <span>${isError ? 'Tạm thời chưa tải được danh sách gia sư trực tuyến' : 'Chưa có hồ sơ gia sư công khai'}</span>
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

async function loadFeaturedTutors() {
  const featuredContainer = document.getElementById('featured-tutors-grid');
  if (!featuredContainer) return;

  featuredContainer.className = TUTOR_CARDS_CLASS;
  fitCards(featuredContainer);
  featuredContainer.innerHTML = `<div class="flex justify-center gap-[var(--card-gap)]">${renderTutorSkeletonCards()}</div>`;

  // Mọi gia sư đang hiển thị công khai (đã xác minh, gói hội viên còn hạn, bài đăng đã duyệt), xếp theo đánh giá
  let tutors;
  try {
    const res = await API.tutors.search({ page: 0, size: ALL_TUTORS_LIMIT, sortBy: 'rating' });
    tutors = Array.isArray(res?.content) ? res.content : [];
  } catch (err) {
    featuredContainer.className = 'grid grid-cols-1 gap-5';
    featuredContainer.innerHTML = renderEmptyOrErrorState({ isError: true });
    const retryBtn = document.getElementById('retry-featured-tutors-btn');
    if (retryBtn) retryBtn.addEventListener('click', loadFeaturedTutors);
    return;
  }

  if (tutors.length === 0) {
    featuredContainer.className = 'grid grid-cols-1 gap-5';
    featuredContainer.innerHTML = renderEmptyOrErrorState({ isError: false });
    return;
  }

  featuredContainer.className = TUTOR_CARDS_CLASS;
  initTutorCarousel(featuredContainer, tutors);
}

let carouselCleanup = null;

/**
 * Carousel trượt theo trang: cứ 3,5 giây cả hàng dịch sang trái đúng một trang (translateX, 500ms ease-in-out).
 * Lặp vô hạn: gắn bản sao các thẻ đầu vào cuối hàng; trượt vào vùng bản sao xong thì tắt transition và
 * nhảy về vị trí tương ứng ở đầu (nội dung y hệt nên không thấy giật). Vị trí tính theo modulo số gia sư nên
 * mỗi lần luôn trượt đúng một trang kể cả khi số gia sư không chia hết cho số thẻ mỗi trang.
 * Rê chuột thì dừng, rời chuột chạy tiếp; ẩn tab thì dừng (visibilitychange); đổi kích thước thì tính lại.
 */
function initTutorCarousel(container, tutors) {
  if (carouselCleanup) carouselCleanup();
  const count = tutors.length;
  let track;
  let perPage;
  let gap;
  let offset = 0; // chỉ số thẻ đang nằm ở mép trái, luôn trong [0, count)
  let timer = null;
  let hovering = false;

  const cardStep = () => (container.clientWidth + gap) / perPage; // bề rộng một thẻ + khoảng cách
  const moveTo = (index, animate, dragPx = 0) => {
    track.style.transition = animate ? SLIDE_TRANSITION : 'none';
    track.style.transform = `translateX(${-index * cardStep() + dragPx}px)`;
  };

  const build = () => {
    ({ perPage, gap } = fitCards(container));
    offset = 0;
    const looping = count > perPage;
    // Bản sao đủ để trang bắt đầu ở bất kỳ vị trí < count cũng đủ thẻ: 2 × perPage − 1 thẻ đầu (lặp vòng)
    const clones = looping ? Array.from({ length: 2 * perPage - 1 }, (_, i) => tutors[i % count]) : [];
    const cloneHtml = clones.map((t) => renderTutorCard(t).replace('<a ', '<a tabindex="-1" aria-hidden="true" ')).join('');
    container.innerHTML = `<div class="flex gap-[var(--card-gap)] ${looping ? '' : 'justify-center'}">${tutors.map(renderTutorCard).join('')}${cloneHtml}</div>`;
    track = container.firstElementChild;
    moveTo(0, false);
  };

  /** Trượt có hiệu ứng tới vị trí target; nếu target nằm ở vùng bản sao thì trượt xong nhảy về vị trí y hệt ở đầu hàng */
  const goTo = (target) => {
    moveTo(target, true);
    offset = target % count;
    if (target !== offset) {
      const done = (e) => {
        if (e.target !== track) return; // bỏ qua transition của thẻ con (đổi màu chữ khi rê chuột)
        track.removeEventListener('transitionend', done);
        moveTo(offset, false);
      };
      track.addEventListener('transitionend', done);
    }
  };

  const next = () => {
    if (hovering || dragging || document.hidden) return;
    goTo(offset + perPage);
  };

  // ---- Vuốt (cảm ứng) / kéo chuột: kéo quá 50px thì sang trang sau / trước, chưa đủ thì bật về chỗ cũ ----
  const SWIPE_THRESHOLD = 50;
  let dragging = false;
  let dragStartX = 0;
  let dragStartY = 0;
  let dragDx = 0;
  let dragBase = 0;
  let dragMoved = false;
  let horizontal = null; // null = chưa rõ hướng; false = người dùng đang cuộn dọc trang

  const onPointerDown = (e) => {
    if (count <= perPage || (e.pointerType === 'mouse' && e.button !== 0)) return;
    dragging = true;
    dragMoved = false;
    horizontal = null;
    dragStartX = e.clientX;
    dragStartY = e.clientY;
    dragDx = 0;
    stop();
    // Ở trang đầu thì đổi sang vị trí y hệt trong vùng bản sao, để kéo sang phải (về trang trước) vẫn có thẻ
    dragBase = offset < perPage ? offset + count : offset;
    moveTo(dragBase, false);
  };

  const onPointerMove = (e) => {
    if (!dragging) return;
    const dx = e.clientX - dragStartX;
    const dy = e.clientY - dragStartY;
    if (horizontal === null && (Math.abs(dx) > 6 || Math.abs(dy) > 6)) {
      horizontal = Math.abs(dx) > Math.abs(dy);
      if (horizontal) container.setPointerCapture?.(e.pointerId);
    }
    if (!horizontal) return;
    dragMoved = true;
    dragDx = Math.max(-container.clientWidth, Math.min(container.clientWidth, dx));
    moveTo(dragBase, false, dragDx);
  };

  const onPointerUp = () => {
    if (!dragging) return;
    dragging = false;
    if (dragDx <= -SWIPE_THRESHOLD) goTo(dragBase + perPage);
    else if (dragDx >= SWIPE_THRESHOLD) goTo(dragBase - perPage);
    else if (dragDx !== 0) goTo(dragBase);
    else moveTo(offset, false); // chỉ chạm / bấm, không kéo: về đúng vị trí, không cần hiệu ứng
    if (!hovering) start();
  };

  // Vừa kéo xong thì không tính là bấm mở hồ sơ gia sư
  const onClickCapture = (e) => {
    if (dragMoved) {
      e.preventDefault();
      e.stopPropagation();
      dragMoved = false;
    }
  };
  const onDragStart = (e) => e.preventDefault();

  const stop = () => { clearInterval(timer); timer = null; };
  const start = () => {
    stop();
    if (count > perPage && !document.hidden && !hovering) timer = setInterval(next, SLIDE_INTERVAL_MS);
  };

  const onEnter = () => { hovering = true; stop(); };
  const onLeave = () => { hovering = false; start(); };
  const onVisibility = () => (document.hidden ? stop() : start());
  let resizeTimer = null;
  const onResize = () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      if (cardsPerPage() !== perPage) {
        build();
      } else {
        ({ perPage, gap } = fitCards(container));
        moveTo(offset, false);
      }
      start();
    }, 150);
  };

  build();
  // Cho trình duyệt tự cuộn dọc trang, còn kéo ngang do carousel xử lý
  container.classList.add('touch-pan-y', 'select-none', 'cursor-grab');
  container.addEventListener('mouseenter', onEnter);
  container.addEventListener('mouseleave', onLeave);
  container.addEventListener('pointerdown', onPointerDown);
  container.addEventListener('pointermove', onPointerMove);
  container.addEventListener('pointerup', onPointerUp);
  container.addEventListener('pointercancel', onPointerUp);
  container.addEventListener('click', onClickCapture, true);
  container.addEventListener('dragstart', onDragStart);
  document.addEventListener('visibilitychange', onVisibility);
  window.addEventListener('resize', onResize);
  start();

  carouselCleanup = () => {
    stop();
    container.removeEventListener('mouseenter', onEnter);
    container.removeEventListener('mouseleave', onLeave);
    container.removeEventListener('pointerdown', onPointerDown);
    container.removeEventListener('pointermove', onPointerMove);
    container.removeEventListener('pointerup', onPointerUp);
    container.removeEventListener('pointercancel', onPointerUp);
    container.removeEventListener('click', onClickCapture, true);
    container.removeEventListener('dragstart', onDragStart);
    document.removeEventListener('visibilitychange', onVisibility);
    window.removeEventListener('resize', onResize);
  };
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
