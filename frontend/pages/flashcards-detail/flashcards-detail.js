/**
 * KizunaTutor - Trang Chi Tiết Bộ Thẻ Flashcard Độc Lập
 * Master-Detail Route Architecture: /learner/deck-detail?id=... (hoặc /flashcards-detail?id=...)
 * REST API Client (Zero-Reload) & Tích Hợp KizunaToast & VOICEVOX AI
 */

import { debounce } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
import { apiClient, KizunaToast } from '/js/apiClient.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khởi tạo Shell học viên
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Chi Tiết Bộ Thẻ Flashcard',
  badge: 'Bộ nhớ siêu tốc'
});

const STORAGE_KEY = 'kizuna_flashcard_decks';
let currentDeck = null;
let currentDeckId = null;
let currentCards = [];
let editingCardIndex = -1;

// 2. Tiện ích An Toàn Chuỗi
function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// 3. Phát âm từ vựng qua VOICEVOX AI (fallback Web Speech API)
function speakWord(text) {
  if (!text) return;
  if (voicevoxClient && typeof voicevoxClient.speak === 'function') {
    voicevoxClient.speak(text).catch(() => fallbackSpeech(text));
  } else {
    fallbackSpeech(text);
  }
}

function fallbackSpeech(text) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'ja-JP';
  utterance.rate = 0.9;
  window.speechSynthesis.speak(utterance);
}

// 4. Tải dữ liệu bộ thẻ từ API (fallback localStorage)
async function loadDeckDetail() {
  const urlParams = new URLSearchParams(window.location.search);
  currentDeckId = urlParams.get('id') || urlParams.get('deckId') || 'deck_default';

  try {
    const deck = await apiClient.getDeck(currentDeckId);
    if (deck) {
      currentDeck = deck;
      currentCards = Array.isArray(deck.cards) ? deck.cards : [];
      renderOverview();
      renderCardsList();
      return;
    }
  } catch (err) {
    console.warn('API getDeck failed, checking localStorage fallback:', err);
  }

  // Fallback LocalStorage
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const decks = JSON.parse(raw);
      if (Array.isArray(decks)) {
        const found = decks.find(d => String(d.id) === String(currentDeckId));
        if (found) {
          currentDeck = found;
          currentCards = Array.isArray(found.cards) ? found.cards : [];
          renderOverview();
          renderCardsList();
          return;
        }
      }
    }
  } catch (e) {
    console.error('LocalStorage load failed:', e);
  }

  // Nếu hoàn toàn chưa có bộ thẻ -> Tạo bộ thẻ mặc định
  currentDeck = {
    id: currentDeckId,
    title: 'Bộ Thẻ Cá Nhân',
    description: 'Chưa có ghi chú mô tả cho bộ thẻ này.',
    cards: []
  };
  currentCards = [];
  renderOverview();
  renderCardsList();
}

// 5. Render Thông Tin Tổng Quan (Overview Panel)
function renderOverview() {
  const titleEl = document.getElementById('overviewTitle');
  const descEl = document.getElementById('overviewDesc');
  const countBadge = document.getElementById('badgeCardCount');

  if (titleEl) titleEl.textContent = currentDeck?.title || 'Bộ thẻ không tên';
  if (descEl) descEl.textContent = currentDeck?.description || 'Chưa có ghi chú mô tả.';
  if (countBadge) countBadge.textContent = `${currentCards.length} thẻ`;
}

// 6. Render Danh Sách Thẻ Con
function renderCardsList(filterQuery = '') {
  const container = document.getElementById('detailListContainer');
  const emptyState = document.getElementById('emptyCardsState');
  const countBadge = document.getElementById('filteredCountBadge');

  if (!container || !emptyState) return;

  const q = (filterQuery || '').trim().toLowerCase();
  const filtered = currentCards.filter(c => 
    String(c.char || '').toLowerCase().includes(q) ||
    String(c.meaning || '').toLowerCase().includes(q) ||
    String(c.hanViet || '').toLowerCase().includes(q)
  );

  if (countBadge) countBadge.textContent = `(${filtered.length} / ${currentCards.length} thẻ)`;

  if (filtered.length === 0) {
    container.innerHTML = '';
    container.classList.add('hidden');
    emptyState.classList.remove('hidden');
    return;
  }

  emptyState.classList.add('hidden');
  container.classList.remove('hidden');

  container.innerHTML = filtered.map((c) => {
    const originalIdx = currentCards.indexOf(c);
    const isKanji = (c.char && c.char.length === 1 && !c.char.match(/[a-zA-Z0-9\s]/));

    return `
      <div class="p-2.5 bg-white border border-[#0F172A] rounded-[6px] shadow-[2px_2px_0px_0px_#0F172A] hover:bg-[#FEF3C7] transition flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5 group">
        <!-- Cột trước: Từ vựng / Câu hỏi -->
        <div class="flex items-center gap-2.5 flex-1 min-w-0">
          <span class="w-6 h-6 rounded-[4px] bg-[#FEF3C7] border border-[#0F172A] flex items-center justify-center text-[10px] font-mono text-[#0F172A] font-bold shrink-0">
            ${originalIdx + 1}
          </span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-serif font-bold text-base text-[#0F172A] truncate" title="${escapeHtml(c.char)}">
                ${escapeHtml(c.char)}
              </span>
            </div>
            ${c.hanViet ? `<span class="text-[11px] font-semibold text-[#831843] uppercase tracking-wider">${escapeHtml(c.hanViet)}</span>` : ''}
          </div>
        </div>

        <!-- Cột sau: Nghĩa / Đáp án -->
        <div class="flex-1 min-w-0 text-xs text-[#0F172A] sm:border-l sm:border-[#0F172A]/15 sm:pl-3">
          <span class="font-medium leading-relaxed">${escapeHtml(c.meaning || '—')}</span>
        </div>

        <!-- Cụm nút thao tác thẻ: Phát âm, Luyện viết, Sửa, Xóa -->
        <div class="flex items-center gap-1.5 shrink-0 self-end sm:self-center">
          <button type="button" data-action="speak-card" data-text="${escapeHtml(c.char)}"
                  class="ls-btn ls-btn-default w-7 h-7 p-0"
                  title="Phát âm từ vựng">
            🔊
          </button>
          ${isKanji ? `
            <a href="/shodo?char=${encodeURIComponent(c.char)}" 
               class="ls-btn ls-btn-primary py-1 px-2 text-xs no-underline"
               title="Luyện viết chữ này trên Canvas Shodo">
              <span>✍️</span>
              <span>Luyện viết</span>
            </a>
          ` : ''}
          <button type="button" data-action="edit-card" data-index="${originalIdx}"
                  class="ls-btn ls-btn-default w-7 h-7 p-0"
                  title="Chỉnh sửa thẻ này">
            ✏️
          </button>
          <button type="button" data-action="delete-card" data-index="${originalIdx}"
                  class="ls-btn ls-btn-danger w-7 h-7 p-0"
                  title="Xóa thẻ này khỏi bộ">
            🗑️
          </button>
        </div>
      </div>
    `;
  }).join('');

  // Gắn sự kiện phát âm
  container.querySelectorAll('[data-action="speak-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      speakWord(btn.dataset.text);
    });
  });

  // Gắn sự kiện sửa thẻ lẻ
  container.querySelectorAll('[data-action="edit-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(btn.dataset.index, 10);
      openEditCardModal(idx);
    });
  });

  // Gắn sự kiện xóa thẻ lẻ
  container.querySelectorAll('[data-action="delete-card"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(btn.dataset.index, 10);
      deleteSingleCard(idx);
    });
  });
}

// 7. Xóa 1 thẻ lẻ qua API
async function deleteSingleCard(index) {
  if (index < 0 || index >= currentCards.length) return;
  const target = currentCards[index];
  if (!confirm(`Bạn có chắc muốn xóa thẻ "${target?.char}" khỏi bộ không?`)) return;

  const removed = currentCards.splice(index, 1);
  try {
    await apiClient.deleteCard(currentDeckId, index);
    KizunaToast.info(`Đã xóa thẻ [${removed[0]?.char}] khỏi bộ.`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể xóa thẻ qua API!');
  }

  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

// 8. Thêm thẻ mới thủ công
async function submitNewCard() {
  const charInput = document.getElementById('inputNewChar');
  const meaningInput = document.getElementById('inputNewMeaning');
  const char = (charInput.value || '').trim();
  const meaning = (meaningInput.value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lòng nhập nội dung câu hỏi / từ vựng mặt trước.');
    charInput.focus();
    return;
  }

  const newCard = {
    char: char,
    hanViet: '',
    meaning: meaning || '—',
    onyomi: '—',
    kunyomi: '—',
    jlpt: 'Tự do'
  };

  currentCards.push(newCard);

  try {
    await apiClient.addCards(currentDeckId, [newCard]);
    KizunaToast.success(`Đã thêm thẻ [${char}] vào bộ!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể lưu thẻ mới qua API!');
  }

  closeAddCardModal();
  renderOverview();
  renderCardsList();
}

function openAddCardModal() {
  document.getElementById('inputNewChar').value = '';
  document.getElementById('inputNewMeaning').value = '';
  document.getElementById('modalAddCard').classList.remove('hidden');
  document.getElementById('inputNewChar').focus();
}

function closeAddCardModal() {
  document.getElementById('modalAddCard').classList.add('hidden');
}

// 9. Sửa thẻ lẻ
function openEditCardModal(index) {
  if (index < 0 || index >= currentCards.length) return;
  editingCardIndex = index;
  const card = currentCards[index];

  document.getElementById('inputEditChar').value = card.char || '';
  document.getElementById('inputEditMeaning').value = card.meaning || '';
  document.getElementById('modalEditCard').classList.remove('hidden');
  document.getElementById('inputEditChar').focus();
}

function closeEditCardModal() {
  document.getElementById('modalEditCard').classList.add('hidden');
  editingCardIndex = -1;
}

async function saveEditCard() {
  if (editingCardIndex < 0 || editingCardIndex >= currentCards.length) return;
  const newChar = (document.getElementById('inputEditChar').value || '').trim();
  const newMeaning = (document.getElementById('inputEditMeaning').value || '').trim();

  if (!newChar) {
    KizunaToast.warning('Nội dung mặt trước không được để trống.');
    return;
  }

  currentCards[editingCardIndex].char = newChar;
  currentCards[editingCardIndex].meaning = newMeaning || '—';

  try {
    await apiClient.updateCard(currentDeckId, editingCardIndex, currentCards[editingCardIndex]);
    KizunaToast.success('Đã cập nhật thông tin thẻ!');
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể cập nhật thẻ qua API!');
  }

  closeEditCardModal();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

// 10. Chỉnh sửa thông tin bộ thẻ (Tên & Mô tả)
function openEditDeckModal() {
  document.getElementById('inputEditDeckName').value = currentDeck?.title || '';
  document.getElementById('inputEditDeckDesc').value = currentDeck?.description || '';
  document.getElementById('modalEditDeckInfo').classList.remove('hidden');
  document.getElementById('inputEditDeckName').focus();
}

function closeEditDeckModal() {
  document.getElementById('modalEditDeckInfo').classList.add('hidden');
}

async function saveEditDeck() {
  const newTitle = (document.getElementById('inputEditDeckName').value || '').trim();
  const newDesc = (document.getElementById('inputEditDeckDesc').value || '').trim();

  if (!newTitle) {
    KizunaToast.warning('Tên bộ thẻ không được để trống.');
    return;
  }

  currentDeck.title = newTitle;
  currentDeck.description = newDesc;

  try {
    await apiClient.updateDeck(currentDeckId, { title: newTitle, description: newDesc });
    KizunaToast.success('Đã cập nhật tên và mô tả bộ thẻ!');
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể cập nhật bộ thẻ qua API!');
  }

  closeEditDeckModal();
  renderOverview();
}

// 11. Xử lý Nạp File Excel 2 Cột (.xlsx, .xls)
function handleExcelImport(file) {
  if (!file) return;

  if (typeof XLSX === 'undefined') {
    KizunaToast.error('Thư viện đọc file Excel chưa sẵn sàng. Vui lòng thử lại sau vài giây!');
    return;
  }

  const reader = new FileReader();
  reader.onload = async function (e) {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const firstSheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[firstSheetName];
      const rows = XLSX.utils.sheet_to_json(worksheet, { header: 1 });

      if (!rows || rows.length === 0) {
        KizunaToast.warning('File Excel trống hoặc không có dữ liệu hợp lệ.');
        return;
      }

      const importedCards = [];
      for (let i = 0; i < rows.length; i++) {
        const row = rows[i];
        if (!row || row.length === 0) continue;

        let colFront = row[0] !== undefined ? String(row[0]).trim() : '';
        let colBack = row[1] !== undefined ? String(row[1]).trim() : '';

        // Bỏ qua dòng tiêu đề nếu có
        if (i === 0 && (colFront.toLowerCase().includes('mặt trước') || colFront.toLowerCase().includes('từ vựng') || colFront.toLowerCase().includes('front'))) {
          continue;
        }

        if (colFront) {
          importedCards.push({
            char: colFront,
            hanViet: '',
            meaning: colBack || '—',
            onyomi: '—',
            kunyomi: '—',
            jlpt: 'Tự do'
          });
        }
      }

      if (importedCards.length === 0) {
        KizunaToast.warning('Không tìm thấy dòng dữ liệu nào hợp lệ trong file Excel.');
        return;
      }

      currentCards.push(...importedCards);

      try {
        await apiClient.addCards(currentDeckId, importedCards);
        KizunaToast.success(`🎉 Đã nạp thành công ${importedCards.length} thẻ từ file Excel!`);
      } catch (err) {
        console.error(err);
        KizunaToast.error('Không thể lưu danh sách thẻ Excel vào cơ sở dữ liệu!');
      }

      closeAddCardModal();
      renderOverview();
      renderCardsList();

    } catch (err) {
      console.error('Lỗi khi đọc file Excel:', err);
      KizunaToast.error('Không thể đọc file Excel. Vui lòng kiểm tra lại định dạng file!');
    }
  };
  reader.readAsArrayBuffer(file);
}

// 12. Tải File Excel Mẫu
function downloadSampleExcel() {
  if (typeof XLSX === 'undefined') {
    KizunaToast.error('Thư viện Excel chưa sẵn sàng.');
    return;
  }

  const sampleData = [
    ['Mặt trước (Từ vựng / Câu hỏi)', 'Mặt sau (Đáp án / Giải nghĩa)'],
    ['桜 (さくら)', 'Hoa anh đào - Biểu tượng mùa xuân nước Nhật'],
    ['富士山 (ふじさん)', 'Núi Phú Sĩ - Ngọn núi lửa hùng vĩ cao nhất Nhật Bản'],
    ['ありがとう', 'Cảm ơn (Lời cảm ơn lịch sự thông dụng)'],
    ['絆 (きずな)', 'Sợi dây gắn kết, tình bạn bè, tình thầy trò sâu đậm'],
    ['こんにちは', 'Xin chào (Chào buổi trưa / chiều)']
  ];

  const ws = XLSX.utils.aoa_to_sheet(sampleData);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'MauTheKizuna');
  XLSX.writeFile(wb, 'KizunaTutor_MauNapTheFlashcard.xlsx');
  KizunaToast.info('Đã tải xuống file Excel mẫu.');
}

// 13. Khởi tạo & Gắn sự kiện (Event Listeners)
document.addEventListener('DOMContentLoaded', () => {
  loadDeckDetail();

  // 3 Chế độ rèn luyện bộ thẻ
  const btnStudy = document.getElementById('btnStartStudyFromDetail');
  if (btnStudy) {
    btnStudy.addEventListener('click', () => {
      window.location.href = `/flashcards-study?deckId=${encodeURIComponent(currentDeckId)}`;
    });
  }

  const btnMatch = document.getElementById('btnPlayMatchGame');
  if (btnMatch) {
    btnMatch.addEventListener('click', () => {
      window.location.href = `/flashcards-match?deckId=${encodeURIComponent(currentDeckId)}`;
    });
  }

  const btnRain = document.getElementById('btnPlayRainGame');
  if (btnRain) {
    btnRain.addEventListener('click', () => {
      window.location.href = `/kanji-game?deckId=${encodeURIComponent(currentDeckId)}`;
    });
  }

  // Thêm thẻ mới
  const btnOpenAdd = document.getElementById('btnOpenAddCardModal');
  if (btnOpenAdd) btnOpenAdd.addEventListener('click', openAddCardModal);

  const btnEmptyAdd = document.getElementById('btnEmptyAddCard');
  if (btnEmptyAdd) btnEmptyAdd.addEventListener('click', openAddCardModal);

  const btnCloseAdd = document.getElementById('btnCloseAddCardModal');
  if (btnCloseAdd) btnCloseAdd.addEventListener('click', closeAddCardModal);

  const btnCancelAdd = document.getElementById('btnCancelAddCard');
  if (btnCancelAdd) btnCancelAdd.addEventListener('click', closeAddCardModal);

  const btnSubmitAdd = document.getElementById('btnSubmitAddCard');
  if (btnSubmitAdd) btnSubmitAdd.addEventListener('click', submitNewCard);

  // Sửa thẻ
  const btnCloseEdit = document.getElementById('btnCloseEditCardModal');
  if (btnCloseEdit) btnCloseEdit.addEventListener('click', closeEditCardModal);

  const btnCancelEdit = document.getElementById('btnCancelEditCard');
  if (btnCancelEdit) btnCancelEdit.addEventListener('click', closeEditCardModal);

  const btnSaveEdit = document.getElementById('btnSaveEditCard');
  if (btnSaveEdit) btnSaveEdit.addEventListener('click', saveEditCard);

  // Sửa thông tin bộ thẻ
  const btnCloseEditDeck = document.getElementById('btnCloseEditDeckModal');
  if (btnCloseEditDeck) btnCloseEditDeck.addEventListener('click', closeEditDeckModal);

  const btnCancelEditDeck = document.getElementById('btnCancelEditDeck');
  if (btnCancelEditDeck) btnCancelEditDeck.addEventListener('click', closeEditDeckModal);

  const btnSaveEditDeck = document.getElementById('btnSaveEditDeck');
  if (btnSaveEditDeck) btnSaveEditDeck.addEventListener('click', saveEditDeck);

  // Tìm kiếm nội bộ
  const searchInput = document.getElementById('internalSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderCardsList(e.target.value);
    }, 200));
  }

  // Nạp file Excel từ ngoài Overview
  const inputDetailExcel = document.getElementById('inputDetailExcel');
  const btnTriggerDetailExcel = document.getElementById('btnTriggerDetailExcel');
  if (btnTriggerDetailExcel && inputDetailExcel) {
    btnTriggerDetailExcel.addEventListener('click', () => inputDetailExcel.click());
    inputDetailExcel.addEventListener('change', (e) => {
      if (e.target.files && e.target.files[0]) {
        handleExcelImport(e.target.files[0]);
        e.target.value = '';
      }
    });
  }

  // Nạp file Excel từ Modal Thêm Thẻ
  const btnQuickExcel = document.getElementById('btnQuickExcelImport');
  if (btnQuickExcel && inputDetailExcel) {
    btnQuickExcel.addEventListener('click', () => inputDetailExcel.click());
  }

  // Tải file mẫu
  const btnDownloadSample = document.getElementById('btnDownloadSample');
  if (btnDownloadSample) {
    btnDownloadSample.addEventListener('click', downloadSampleExcel);
  }
});
