/**
 * KizunaTutor - Từ điển Tra cứu Hán tự Joyo (Kanji Dictionary)
 * Feature-Based Architecture: frontend/pages/kanji-dictionary/
 * Tích hợp sâu với Flashcards và Luyện viết Shodo Canvas
 * Tuân thủ nghiêm ngặt FRONTEND_RULES.md (Zero mock/fake data)
 */

import { debounce, showToast } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'kanji-dictionary',
  pageTitle: 'Từ điển Hán tự Joyo',
  badge: 'Công cụ rèn luyện'
});

// Khởi tạo trạng thái
let currentPage = 0;
const pageSize = 24;
let totalPages = 1;
let currentItems = [];
let selectedKanjiForModal = null;

// Tham chiếu DOM chính
const searchInput = document.getElementById('searchKanjiInput');
const clearSearchBtn = document.getElementById('btnClearSearchInput');
const jlptSelect = document.getElementById('selectJlptLevel');
const strokeSelect = document.getElementById('selectStrokeCount');
const totalCountBadge = document.getElementById('kanjiTotalCountBadge');
const pageInfoText = document.getElementById('pageInfoText');
const loadingState = document.getElementById('dictLoadingState');
const emptyState = document.getElementById('dictEmptyState');
const kanjiGrid = document.getElementById('kanjiGrid');
const paginationContainer = document.getElementById('paginationContainer');
const btnPrevPage = document.getElementById('btnPrevPage');
const btnNextPage = document.getElementById('btnNextPage');
const pageIndicator = document.getElementById('paginationPageIndicator');
const btnResetFilters = document.getElementById('btnResetFilters');

// Tham chiếu Modal Thêm Flashcard
const addToFlashcardModal = document.getElementById('addToFlashcardModal');
const btnCloseFlashcardModal = document.getElementById('btnCloseFlashcardModal');
const btnCancelAddToDeck = document.getElementById('btnCancelAddToDeck');
const btnConfirmAddToDeck = document.getElementById('btnConfirmAddToDeck');
const selectTargetDeck = document.getElementById('selectTargetDeck');
const noDeckWarning = document.getElementById('noDeckWarning');
const modalPreviewChar = document.getElementById('modalPreviewChar');
const modalPreviewHanViet = document.getElementById('modalPreviewHanViet');
const modalPreviewJlpt = document.getElementById('modalPreviewJlpt');
const modalPreviewMeaning = document.getElementById('modalPreviewMeaning');

// Helper định dạng màu sắc huy hiệu JLPT
function getJlptBadgeClass(jlpt) {
  const level = (jlpt || '').toUpperCase();
  switch (level) {
    case 'N5':
      return 'bg-[#FFE5EC] text-[#831843] border-[#FBCFE8]';
    case 'N4':
      return 'bg-sky-50 text-sky-700 border-sky-200';
    case 'N3':
      return 'bg-amber-50 text-amber-700 border-amber-200';
    case 'N2':
      return 'bg-emerald-50 text-emerald-700 border-emerald-200';
    case 'N1':
      return 'bg-purple-50 text-purple-700 border-purple-200';
    default:
      return 'bg-gray-100 text-gray-700 border-gray-200';
  }
}

// Chuyển đổi trạng thái hiển thị đang tải (Loading State)
function showLoading(isLoading) {
  if (isLoading) {
    loadingState?.classList.remove('hidden');
    kanjiGrid?.classList.add('hidden');
    emptyState?.classList.add('hidden');
    paginationContainer?.classList.add('hidden');
  } else {
    loadingState?.classList.add('hidden');
  }
}

// Hiển thị danh sách thẻ chữ Hán lên giao diện
function renderGrid(items, totalElements) {
  if (totalCountBadge) {
    totalCountBadge.textContent = `${totalElements} chữ Hán`;
  }
  if (pageInfoText) {
    pageInfoText.textContent = `Trang ${currentPage + 1} / ${totalPages}`;
  }

  // Cập nhật các nút phân trang
  if (pageIndicator) {
    pageIndicator.textContent = `${currentPage + 1} / ${totalPages}`;
  }
  if (btnPrevPage) {
    btnPrevPage.disabled = (currentPage <= 0);
  }
  if (btnNextPage) {
    btnNextPage.disabled = (currentPage >= totalPages - 1);
  }

  // Trường hợp không có dữ liệu thực tế (Empty State chuẩn)
  if (!items || items.length === 0) {
    kanjiGrid.innerHTML = '';
    kanjiGrid.classList.add('hidden');
    emptyState?.classList.remove('hidden');
    paginationContainer?.classList.add('hidden');
    return;
  }

  emptyState?.classList.add('hidden');
  kanjiGrid.classList.remove('hidden');
  if (totalPages > 1) {
    paginationContainer?.classList.remove('hidden');
  } else {
    paginationContainer?.classList.add('hidden');
  }

  kanjiGrid.innerHTML = items.map(item => {
    const char = item.kanjiChar || '';
    const hanViet = item.hanViet || 'Chưa có';
    const meaning = item.meaning || 'Chưa cập nhật ý nghĩa';
    const stroke = item.strokeCount ? `${item.strokeCount} nét` : '—';
    const jlpt = item.jlpt || 'N5';
    const onyomi = item.onyomi || '—';
    const kunyomi = item.kunyomi || '—';
    const jlptBadgeClass = getJlptBadgeClass(jlpt);

    return `
      <div class="bg-white border border-[#ECE8DE] hover:border-[#006194] rounded-2xl p-4 shadow-sm hover:shadow-md transition-all duration-200 flex flex-col justify-between group">
        <div>
          <!-- Header thẻ: Chữ to & Cấp độ / Số nét -->
          <div class="flex items-start justify-between gap-2 mb-2">
            <div class="w-14 h-14 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] group-hover:border-[#006194]/30 flex items-center justify-center shrink-0">
              <span class="text-3xl font-bold font-japanese text-[#0F2545]">${char}</span>
            </div>
            <div class="flex flex-col items-end gap-1.5">
              <span class="px-2 py-0.5 text-[10px] font-bold rounded-full border ${jlptBadgeClass}">
                ${jlpt}
              </span>
              <span class="text-[11px] font-medium text-[#68737D] bg-[#FAF8F2] px-2 py-0.5 rounded-md border border-[#ECE8DE]">
                ${stroke}
              </span>
            </div>
          </div>

          <!-- Âm Hán Việt & Ý nghĩa -->
          <div class="space-y-1 mb-3">
            <h4 class="font-bold text-sm text-[#006194] uppercase tracking-wide truncate">
              ${hanViet}
            </h4>
            <p class="text-xs text-[#3F4850] line-clamp-2 min-h-[32px] leading-relaxed">
              ${meaning}
            </p>
          </div>

          <!-- Âm On / Âm Kun -->
          <div class="text-[11px] text-[#68737D] space-y-1 pt-2.5 border-t border-[#ECE8DE] bg-[#FAF8F2]/60 -mx-4 -mb-1 px-4 py-2 rounded-b-xl">
            <div class="truncate">
              <span class="font-semibold text-[#0F2545]">On:</span>
              <span class="text-[#3F4850] ml-1">${onyomi}</span>
            </div>
            <div class="truncate">
              <span class="font-semibold text-[#0F2545]">Kun:</span>
              <span class="text-[#3F4850] ml-1">${kunyomi}</span>
            </div>
          </div>
        </div>

        <!-- Các nút liên kết hành động 1-Click -->
        <div class="flex items-center gap-2 pt-3 mt-3 border-t border-[#ECE8DE]">
          <a href="/shodo?kanji=${encodeURIComponent(char)}" 
             class="flex-1 py-1.5 px-2 text-center text-xs font-semibold rounded-xl bg-[#FAF8F2] hover:bg-[#F2EFE6] text-[#0F2545] border border-[#ECE8DE] transition flex items-center justify-center gap-1 no-underline"
             title="Mở bảng Shodo luyện viết từng nét chuẩn của chữ này">
            <span class="material-symbols-outlined text-[15px]">draw</span>
            <span>Luyện viết</span>
          </a>

          <button type="button" 
                  data-action="open-save-flashcard" 
                  data-char="${char}"
                  class="flex-1 py-1.5 px-2 text-center text-xs font-semibold rounded-xl bg-sky-50 hover:bg-sky-100 text-[#006194] border border-sky-200 transition flex items-center justify-center gap-1 cursor-pointer"
                  title="Nhặt chữ này vào bộ thẻ Flashcards cá nhân">
            <span class="material-symbols-outlined text-[15px]">bookmark_add</span>
            <span>Lưu thẻ</span>
          </button>
        </div>
      </div>
    `;
  }).join('');

  // Gắn sự kiện mở modal lưu Flashcard
  kanjiGrid.querySelectorAll('[data-action="open-save-flashcard"]').forEach(btn => {
    btn.addEventListener('click', () => {
      const char = btn.getAttribute('data-char');
      const item = currentItems.find(x => x.kanjiChar === char);
      if (item) {
        openAddToFlashcardModal(item);
      }
    });
  });
}

// Truy vấn danh sách Hán tự từ Backend API
async function fetchKanjiList() {
  const keyword = (searchInput?.value || '').trim();
  const rawJlpt = jlptSelect?.value || '';
  const jlpt = (rawJlpt && rawJlpt !== 'ALL') ? rawJlpt : '';
  const stroke = strokeSelect?.value || '';

  showLoading(true);

  // Cập nhật hiển thị nút xóa ô tìm kiếm
  if (clearSearchBtn) {
    if (keyword.length > 0) {
      clearSearchBtn.classList.remove('hidden');
    } else {
      clearSearchBtn.classList.add('hidden');
    }
  }

  let url = `/api/learner/kanji/search?page=${currentPage}&size=${pageSize}`;
  if (keyword) url += `&keyword=${encodeURIComponent(keyword)}`;
  if (jlpt) url += `&jlpt=${encodeURIComponent(jlpt)}`;
  if (stroke) url += `&strokeCount=${encodeURIComponent(stroke)}`;

  try {
    let res = await fetch(url);
    if (!res.ok && res.status === 404) {
      // Dự phòng endpoint công khai /api/kanji/search
      url = url.replace('/api/learner/kanji/search', '/api/kanji/search');
      res = await fetch(url);
    }

    if (res.ok) {
      const data = await res.json();
      currentItems = data.items || [];
      const totalElements = data.totalElements || 0;
      totalPages = Math.max(1, data.totalPages || 1);
      renderGrid(currentItems, totalElements);
    } else {
      currentItems = [];
      totalPages = 1;
      renderGrid([], 0);
    }
  } catch (err) {
    console.debug('Lỗi kết nối tra cứu Hán tự:', err);
    currentItems = [];
    totalPages = 1;
    renderGrid([], 0);
  } finally {
    showLoading(false);
  }
}

// Mở Modal Lưu vào Flashcards
function openAddToFlashcardModal(kanjiItem) {
  selectedKanjiForModal = kanjiItem;

  // Cập nhật thông tin preview trong Modal
  if (modalPreviewChar) modalPreviewChar.textContent = kanjiItem.kanjiChar || '—';
  if (modalPreviewHanViet) modalPreviewHanViet.textContent = kanjiItem.hanViet || 'HÁN TỰ';
  if (modalPreviewJlpt) {
    modalPreviewJlpt.textContent = kanjiItem.jlpt || 'N5';
    modalPreviewJlpt.className = `px-2 py-0.2 rounded-full text-[10px] font-bold border ${getJlptBadgeClass(kanjiItem.jlpt)}`;
  }
  if (modalPreviewMeaning) modalPreviewMeaning.textContent = kanjiItem.meaning || 'Chưa cập nhật ý nghĩa';

  // Nạp danh sách bộ thẻ từ LocalStorage (chuẩn kizuna_flashcard_decks)
  const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
  let decks = [];
  try {
    decks = rawDecks ? JSON.parse(rawDecks) : [];
  } catch (e) {
    decks = [];
  }

  if (!decks || decks.length === 0) {
    selectTargetDeck.innerHTML = '<option value="">(Chưa có bộ thẻ)</option>';
    selectTargetDeck.disabled = true;
    noDeckWarning?.classList.remove('hidden');
    if (btnConfirmAddToDeck) btnConfirmAddToDeck.disabled = true;
  } else {
    selectTargetDeck.disabled = false;
    noDeckWarning?.classList.add('hidden');
    if (btnConfirmAddToDeck) btnConfirmAddToDeck.disabled = false;

    selectTargetDeck.innerHTML = decks.map(d => {
      const cardCount = d.cards ? d.cards.length : 0;
      return `<option value="${d.id}">${d.title} (${cardCount} thẻ)</option>`;
    }).join('');
  }

  addToFlashcardModal.classList.remove('hidden');
  addToFlashcardModal.classList.add('flex');
}

// Đóng Modal Thêm Flashcard
function closeAddToFlashcardModal() {
  addToFlashcardModal.classList.add('hidden');
  addToFlashcardModal.classList.remove('flex');
  selectedKanjiForModal = null;
}

// Lưu Hán tự vào bộ thẻ đã chọn
function confirmSaveToDeck() {
  if (!selectedKanjiForModal) return;

  const targetDeckId = selectTargetDeck.value;
  if (!targetDeckId) {
    showToast('Vui lòng chọn một bộ thẻ trước khi lưu.', 'error');
    return;
  }

  const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
  let decks = [];
  try {
    decks = rawDecks ? JSON.parse(rawDecks) : [];
  } catch (e) {
    decks = [];
  }

  const targetDeck = decks.find(d => String(d.id) === String(targetDeckId));
  if (!targetDeck) {
    showToast('Bộ thẻ không tồn tại hoặc đã bị xóa.', 'error');
    return;
  }

  targetDeck.cards = targetDeck.cards || [];

  // Kiểm tra trùng lặp thẻ trong bộ
  const isDuplicate = targetDeck.cards.some(c => c.char === selectedKanjiForModal.kanjiChar);
  if (isDuplicate) {
    showToast(`Chữ "${selectedKanjiForModal.kanjiChar}" đã có sẵn trong bộ thẻ này.`, 'info');
    closeAddToFlashcardModal();
    return;
  }

  // Thêm thẻ mới theo cấu trúc chuẩn
  targetDeck.cards.push({
    char: selectedKanjiForModal.kanjiChar,
    hanViet: selectedKanjiForModal.hanViet || '',
    meaning: selectedKanjiForModal.meaning || '',
    onyomi: selectedKanjiForModal.onyomi || '',
    kunyomi: selectedKanjiForModal.kunyomi || '',
    jlpt: selectedKanjiForModal.jlpt || 'N5'
  });

  // Lưu lại vào LocalStorage
  localStorage.setItem('kizuna_flashcard_decks', JSON.stringify(decks));

  closeAddToFlashcardModal();
  showToast(`Đã lưu chữ "${selectedKanjiForModal.kanjiChar}" vào bộ thẻ "${targetDeck.title}".`, 'success');
}

// Khởi tạo các sự kiện tương tác
document.addEventListener('DOMContentLoaded', () => {
  // Đọc tham số tìm kiếm từ URL query parameters nếu có (ví dụ: ?keyword=日 hoặc ?char=日)
  const urlParams = new URLSearchParams(window.location.search);
  const initialKeyword = urlParams.get('keyword') || urlParams.get('char') || urlParams.get('kanji');
  const initialJlpt = urlParams.get('jlpt');

  if (initialKeyword && searchInput) {
    searchInput.value = initialKeyword;
  }
  if (initialJlpt && jlptSelect) {
    jlptSelect.value = initialJlpt;
  }

  // Bắt đầu tra cứu ban đầu
  fetchKanjiList();

  // Tìm kiếm với Debounce (300ms)
  const debouncedSearch = debounce(() => {
    currentPage = 0;
    fetchKanjiList();
  }, 300);

  searchInput?.addEventListener('input', debouncedSearch);

  // Nút xóa nhanh ô tìm kiếm
  clearSearchBtn?.addEventListener('click', () => {
    if (searchInput) searchInput.value = '';
    currentPage = 0;
    fetchKanjiList();
  });

  // Lọc theo JLPT
  jlptSelect?.addEventListener('change', () => {
    currentPage = 0;
    fetchKanjiList();
  });

  // Lọc theo số nét
  strokeSelect?.addEventListener('change', () => {
    currentPage = 0;
    fetchKanjiList();
  });

  // Nút xóa bộ lọc ở Empty State
  btnResetFilters?.addEventListener('click', () => {
    if (searchInput) searchInput.value = '';
    if (jlptSelect) jlptSelect.value = 'ALL';
    if (strokeSelect) strokeSelect.value = '';
    currentPage = 0;
    fetchKanjiList();
  });

  // Phân trang
  btnPrevPage?.addEventListener('click', () => {
    if (currentPage > 0) {
      currentPage--;
      fetchKanjiList();
      window.scrollTo({ top: 300, behavior: 'smooth' });
    }
  });

  btnNextPage?.addEventListener('click', () => {
    if (currentPage < totalPages - 1) {
      currentPage++;
      fetchKanjiList();
      window.scrollTo({ top: 300, behavior: 'smooth' });
    }
  });

  // Sự kiện Modal Flashcard
  btnCloseFlashcardModal?.addEventListener('click', closeAddToFlashcardModal);
  btnCancelAddToDeck?.addEventListener('click', closeAddToFlashcardModal);
  btnConfirmAddToDeck?.addEventListener('click', confirmSaveToDeck);

  // Đóng modal khi bấm ra ngoài lớp phủ
  addToFlashcardModal?.addEventListener('click', (e) => {
    if (e.target === addToFlashcardModal) {
      closeAddToFlashcardModal();
    }
  });
});
