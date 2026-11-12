/**
 * KizunaTutor - Trang Chi Tiết Bộ Thẻ Flashcard Độc Lập
 * Master-Detail Route Architecture: /learner/deck-detail?id=... (hoặc /flashcards-detail?id=...)
 * REST API Client (Zero-Reload) & Tích Hợp KizunaToast
 */

import { debounce } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
import { apiClient, KizunaToast, enrichFlashcardItem } from '/js/apiClient.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khởi tạo Shell học viên
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Chi Tiết Bộ Thẻ Flashcard',
  badge: 'Bộ nhớ siêu tốc'
});

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

// 3. Phát âm từ vựng tiếng Nhật (VOICEVOX AI & Web Speech API)
function speakWord(text) {
  if (!text) return;
  voicevoxClient.speak(text);
}

// 4. Tải dữ liệu bộ thẻ
async function loadDeckDetail() {
  const urlParams = new URLSearchParams(window.location.search);
  currentDeckId = urlParams.get('id');

  if (!currentDeckId) {
    // Thử lấy từ pathname nếu theo cấu trúc /learner/flashcards/:id
    const parts = window.location.pathname.split('/').filter(Boolean);
    if (parts.length > 0 && !parts[parts.length - 1].includes('detail')) {
      currentDeckId = parts[parts.length - 1];
    }
  }

  if (!currentDeckId) {
    KizunaToast.error('Không tìm thấy mã định danh bộ thẻ!');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1200);
    return;
  }

  try {
    currentDeck = await apiClient.getDeck(currentDeckId);
  } catch (err) {
    console.debug('Lỗi tải chi tiết bộ thẻ:', err);
  }

  if (!currentDeck) {
    KizunaToast.error('Không tìm thấy bộ thẻ tương ứng!');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1200);
    return;
  }

  currentCards = currentDeck.cards || [];
  renderOverview();
  renderCardsList();

  // Kiểm tra nếu có param ?edit=1 thì mở popup sửa
  if (urlParams.get('edit') === '1') {
    openEditDeckModal();
  }
}

// 5. Render Thông Tin Tổng Quan
function renderOverview() {
  if (!currentDeck) return;
  document.getElementById('overviewTitle').textContent = currentDeck.title || 'Chưa đặt tên';
  document.getElementById('overviewDesc').textContent = currentDeck.description || 'Chưa có ghi chú mô tả cho bộ thẻ này.';
  document.getElementById('badgeCardCount').textContent = `${currentCards.length} thẻ`;
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
    String(c.meaning || '').toLowerCase().includes(q)
  );

  countBadge.textContent = `(${filtered.length} / ${currentCards.length} thẻ)`;

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
      <div class="p-3 bg-white border border-[#E2E8F0] hover:border-[#CBD5E1] rounded-xl transition flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 group">
        <!-- Cột trước: Mặt trước (Từ vựng / Chữ Hán / Câu hỏi) -->
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <span class="w-6 h-6 rounded bg-slate-100 border border-[#E2E8F0] flex items-center justify-center text-[10px] font-mono text-slate-500 font-bold shrink-0">
            ${originalIdx + 1}
          </span>
          <div class="flex-1 min-w-0">
            <span class="font-japanese font-bold text-base text-[#0F172A] break-words" title="${escapeHtml(c.char || '')}">
              ${escapeHtml(c.char || '')}
            </span>
          </div>
        </div>

        <!-- Cột sau: Mặt sau (Nghĩa / Đáp án) -->
        <div class="flex-1 min-w-0 text-xs text-[#334155] sm:border-l sm:border-[#E2E8F0] sm:pl-3">
          <span class="font-medium leading-relaxed break-words">${escapeHtml(c.meaning || '—')}</span>
        </div>

        <!-- Cụm nút thao tác thẻ: Phát âm [ 🔊 ], Luyện viết, Sửa [ ✏️ ], Xóa [ 🗑️ ] -->
        <div class="flex items-center gap-1.5 shrink-0 self-end sm:self-center">
          <button type="button" data-action="speak-card" data-text="${escapeHtml(c.char)}"
                  class="w-7 h-7 flex items-center justify-center rounded-lg border border-[#CBD5E1] bg-white text-slate-700 hover:bg-slate-50 cursor-pointer text-xs transition"
                  title="Phát âm từ vựng">
            🔊
          </button>
          ${isKanji ? `
            <a href="/learner/practice-canvas?char=${encodeURIComponent(c.char)}" 
               class="py-1 px-2 text-[11px] font-semibold rounded-lg border border-[#FBCFE8] bg-[#FFE5EC] text-[#831843] hover:bg-[#FCE7F3] flex items-center gap-1 transition no-underline cursor-pointer"
               title="Luyện viết chữ này trên Canvas Shodo">
              <span>🖌️</span>
              <span>Luyện viết</span>
            </a>
          ` : ''}
          <button type="button" data-action="edit-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded-lg border border-[#CBD5E1] bg-white text-slate-700 hover:bg-slate-50 cursor-pointer font-semibold text-xs transition"
                  title="Chỉnh sửa thẻ này">
            ✏️
          </button>
          <button type="button" data-action="delete-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded-lg border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 cursor-pointer font-semibold text-xs transition"
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

// 8. Thêm thẻ mới thủ công (Chỉ gồm 2 trường: Mặt trước & Mặt sau)
async function submitNewCard() {
  const charInput = document.getElementById('inputNewChar');
  const meaningInput = document.getElementById('inputNewMeaning');

  const char = (charInput?.value || '').trim();
  const meaning = (meaningInput?.value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lòng nhập nội dung mặt trước.');
    charInput?.focus();
    return;
  }

  if (!meaning) {
    KizunaToast.warning('Vui lòng nhập nội dung mặt sau.');
    meaningInput?.focus();
    return;
  }

  const newCard = enrichFlashcardItem({
    char: char,
    meaning: meaning
  });

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
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

function openAddCardModal() {
  const charEl = document.getElementById('inputNewChar');
  const meaningEl = document.getElementById('inputNewMeaning');
  if (charEl) charEl.value = '';
  if (meaningEl) meaningEl.value = '';
  document.getElementById('modalAddCard')?.classList.remove('hidden');
  charEl?.focus();
}

function closeAddCardModal() {
  document.getElementById('modalAddCard')?.classList.add('hidden');
}

// 9. Sửa thẻ lẻ (Chỉ gồm 2 trường: Mặt trước & Mặt sau)
function openEditCardModal(index) {
  if (index < 0 || index >= currentCards.length) return;
  editingCardIndex = index;
  const card = currentCards[index];

  const charEl = document.getElementById('inputEditChar');
  const meaningEl = document.getElementById('inputEditMeaning');
  if (charEl) charEl.value = card.char || card.front || '';
  if (meaningEl) meaningEl.value = card.meaning || card.back || '';

  document.getElementById('modalEditCard')?.classList.remove('hidden');
  charEl?.focus();
}

function closeEditCardModal() {
  document.getElementById('modalEditCard')?.classList.add('hidden');
  editingCardIndex = -1;
}

async function saveEditCard() {
  if (editingCardIndex < 0 || editingCardIndex >= currentCards.length) return;
  const char = (document.getElementById('inputEditChar')?.value || '').trim();
  const meaning = (document.getElementById('inputEditMeaning')?.value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lòng nhập nội dung mặt trước.');
    return;
  }
  if (!meaning) {
    KizunaToast.warning('Vui lòng nhập nội dung mặt sau.');
    return;
  }

  currentCards[editingCardIndex] = enrichFlashcardItem({
    ...currentCards[editingCardIndex],
    char,
    meaning
  });

  try {
    await apiClient.updateDeck(currentDeckId, { cards: currentCards });
    KizunaToast.success('Đã cập nhật thông tin thẻ!');
  } catch (err) {
    console.error(err);
    KizunaToast.error('Không thể lưu thay đổi thẻ qua API!');
  }

  closeEditCardModal();
  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
}

// 10. Chỉnh sửa thông tin bộ thẻ (Tên & Mô tả)
function openEditDeckModal() {
  if (!currentDeck) return;
  document.getElementById('inputEditDeckName').value = currentDeck.title || '';
  document.getElementById('inputEditDeckDesc').value = currentDeck.description || '';
  document.getElementById('modalEditDeckInfo').classList.remove('hidden');
  document.getElementById('inputEditDeckName').focus();
}

function closeEditDeckModal() {
  document.getElementById('modalEditDeckInfo').classList.add('hidden');
}

async function saveEditDeckInfo() {
  const title = (document.getElementById('inputEditDeckName').value || '').trim();
  const desc = (document.getElementById('inputEditDeckDesc').value || '').trim();

  if (!title) {
    KizunaToast.warning('Vui lòng nhập tên bộ thẻ!');
    return;
  }

  currentDeck.title = title;
  currentDeck.description = desc;

  try {
    await apiClient.updateDeck(currentDeckId, { title, description: desc });
    KizunaToast.success(`Đã cập nhật bộ thẻ "${title}"!`);
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lỗi khi cập nhật thông tin bộ thẻ!');
  }

  renderOverview();
  closeEditDeckModal();
}

// 11. Đọc file Excel Đa Dụng 2 Cột (Cột 1: Mặt trước, Cột 2: Mặt sau)
function handleExcelImport(event) {
  const file = event.target.files[0];
  if (!file) return;

  if (typeof XLSX === 'undefined') {
    KizunaToast.error('Thư viện Excel đang tải, vui lòng thử lại sau giây lát!');
    event.target.value = '';
    return;
  }

  const reader = new FileReader();
  reader.onload = async function(e) {
    try {
      const data = new Uint8Array(e.target.result);
      const workbook = XLSX.read(data, { type: 'array' });
      const firstSheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[firstSheetName];
      const rawRows = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
      const newCards = [];

      rawRows.forEach((row, index) => {
        if (!row || row.length === 0) return;
        const col0 = String(row[0] || '').trim();
        const col1 = String(row[1] || '').trim();
        if (!col0 && !col1) return;

        // Bỏ qua dòng tiêu đề nếu chứa từ khóa thông dụng
        const lower0 = col0.toLowerCase();
        if (index === 0 && (
          lower0.includes('mặt trước') || 
          lower0.includes('câu hỏi') || 
          lower0.includes('chữ hán') || 
          lower0.includes('từ vựng') || 
          lower0.includes('front') || 
          lower0.includes('kanji') ||
          lower0.includes('thuật ngữ')
        )) {
          return;
        }

        // Hỗ trợ cả định dạng 3 cột (Mặt trước | Cách đọc Kana | Nghĩa) và 2 cột (Mặt trước | Mặt sau)
        if (row.length >= 3 && String(row[2] || '').trim()) {
          const readingCol = String(row[1] || '').trim();
          const meaningCol = String(row[2] || '').trim();
          newCards.push(enrichFlashcardItem({
            char: col0,
            reading: readingCol,
            meaning: readingCol ? `${readingCol} • ${meaningCol}` : meaningCol
          }));
        } else {
          newCards.push(enrichFlashcardItem({
            char: col0,
            meaning: col1 || '—'
          }));
        }
      });

      if (newCards.length === 0) {
        KizunaToast.warning('Không tìm thấy dữ liệu thẻ hợp lệ trong file Excel!');
        return;
      }

      currentCards.push(...newCards);
      await apiClient.addCards(currentDeckId, newCards);
      closeAddCardModal();
      renderOverview();
      renderCardsList(document.getElementById('internalSearchInput')?.value || '');
      KizunaToast.success(`Đã nạp thành công ${newCards.length} thẻ từ file Excel!`);
    } catch (err) {
      console.error(err);
      KizunaToast.error('Lỗi khi đọc file Excel. Vui lòng kiểm tra lại định dạng file!');
    } finally {
      event.target.value = '';
    }
  };
  reader.readAsArrayBuffer(file);
}

function downloadSampleExcel() {
  if (typeof XLSX === 'undefined') {
    KizunaToast.error('Thư viện Excel chưa sẵn sàng, vui lòng thử lại sau!');
    return;
  }
  const sampleData = [
    [
      "Mặt trước (Chữ Hán / Từ vựng / Câu hỏi)*",
      "Mặt sau (Giải nghĩa / Đáp án)*"
    ],
    ["日", "ニチ, ジツ, ひ, -び, -か, NHẬT, Mặt trời, ngày"],
    ["桜", "さくら, Hoa anh đào"],
    ["勉強", "べんきょう, Học tập, việc học"],
    ["こんにちは", "Xin chào (dùng ban ngày)"],
    ["犬", "いぬ, Con chó"],
    ["猫", "ねこ, Con mèo"],
    ["富士山", "ふじさん, Núi Phú Sĩ - ngọn núi cao nhất Nhật Bản"]
  ];

  const ws = XLSX.utils.aoa_to_sheet(sampleData);
  ws['!cols'] = [{ wch: 35 }, { wch: 60 }];
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Mau_Flashcard_2_Cot");
  XLSX.writeFile(wb, "KizunaTutor_Mau_Flashcard_2_Cot.xlsx");
  KizunaToast.info("Đã tải xuống file Excel mẫu 2 cột!");
}

// 12. In bộ thẻ (Print Mode A4 / Xuất PDF 2 mặt)
function printCurrentDeck() {
  if (!currentDeck || currentCards.length === 0) {
    KizunaToast.warning('Bộ thẻ này chưa có thẻ học nào để in!');
    return;
  }

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    KizunaToast.error('Vui lòng cho phép popup để mở trang in ấn.');
    return;
  }

  const printHtml = `
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>In Bộ Thẻ - ${escapeHtml(currentDeck.title || 'Flashcards')}</title>
  <link rel="stylesheet" href="/styles/variables.css">
  <style>
    @page { size: A4; margin: 10mm; }
    body { font-family: 'Be Vietnam Pro', system-ui, sans-serif; margin: 0; padding: 10px; color: #0F172A; }
    .page-header { text-align: center; margin-bottom: 20px; border-bottom: 2px solid #0F172A; padding-bottom: 10px; }
    .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
    .card { border: 2px solid #0F172A; border-radius: 8px; padding: 16px; page-break-inside: avoid; display: flex; flex-direction: column; justify-content: space-between; min-height: 140px; }
    .front { font-family: 'Shippori Mincho', serif; font-size: 28px; font-weight: bold; text-align: center; margin-bottom: 8px; }
    .details { font-size: 12px; color: #334155; border-top: 1px dashed #CBD5E1; padding-top: 8px; font-weight: 500; }
    @media print {
      .no-print { display: none !important; }
    }
  </style>
</head>
<body>
  <div class="no-print" style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center;">
    <div><strong>KizunaTutor</strong> - Xem trước trang in A4</div>
    <button onclick="window.print()" style="padding: 6px 16px; background: #0F172A; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold;">
      🖨️ In Ngay (Print / PDF)
    </button>
  </div>
  <div class="page-header">
    <h2 style="margin: 0 0 5px 0;">${escapeHtml(currentDeck.title)}</h2>
    <p style="margin: 0; font-size: 13px; color: #475569;">${escapeHtml(currentDeck.description || '')} • Tổng cộng ${currentCards.length} thẻ</p>
  </div>
  <div class="grid">
    ${currentCards.map(c => `
      <div class="card">
        <div>
          <div class="front">${escapeHtml(c.char)}</div>
        </div>
        <div class="details">
          <span>${escapeHtml(c.meaning || '—')}</span>
        </div>
      </div>
    `).join('')}
  </div>
</body>
</html>
  `;

  printWindow.document.open();
  printWindow.document.write(printHtml);
  printWindow.document.close();
  printWindow.focus();
}

// =========================================================================
// 13. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadDeckDetail();

  // Nút quay lại danh sách
  document.getElementById('btnBackToList')?.addEventListener('click', () => {
    window.location.href = '/learner/flashcards';
  });

  // Thanh công cụ tác vụ
  document.getElementById('btnStartStudy')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/flashcards-study?id=${encodeURIComponent(currentDeckId)}`;
    }
  });
  document.getElementById('btnStartStudyFromDetail')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/flashcards-study?id=${encodeURIComponent(currentDeckId)}`;
    }
  });
  document.getElementById('btnPlayMatchGame')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/learner/flashcards-match?id=${encodeURIComponent(currentDeckId)}`;
    }
  });
  document.getElementById('btnPlayRainGame')?.addEventListener('click', () => {
    if (currentDeckId) {
      window.location.href = `/kanji-game?deckId=${encodeURIComponent(currentDeckId)}`;
    } else {
      window.location.href = '/kanji-game';
    }
  });
  document.getElementById('btnPrintDeck')?.addEventListener('click', printCurrentDeck);
  document.getElementById('btnEditDeck')?.addEventListener('click', openEditDeckModal);

  // Tìm kiếm nội bộ
  const searchInput = document.getElementById('internalSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderCardsList(e.target.value);
    }, 200));
  }

  // Thêm thẻ thủ công
  document.getElementById('btnOpenAddCardModal')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnEmptyAddCard')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnCloseAddCardModal')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnCancelAddCard')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnSubmitAddCard')?.addEventListener('click', submitNewCard);

  // Import Excel từ bên trong modal Thêm Thẻ Mới (2 cột)
  document.getElementById('btnTriggerModalExcel')?.addEventListener('click', () => {
    document.getElementById('inputModalExcel')?.click();
  });
  document.getElementById('inputModalExcel')?.addEventListener('change', handleExcelImport);
  document.getElementById('btnDownloadSample')?.addEventListener('click', downloadSampleExcel);

  // Sửa thẻ con lẻ
  document.getElementById('btnCloseEditCardModal')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnCancelEditCard')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnSaveEditCard')?.addEventListener('click', saveEditCard);

  // Sửa thông tin bộ thẻ
  document.getElementById('btnCloseEditDeckModal')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnCancelEditDeck')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnSaveEditDeck')?.addEventListener('click', saveEditDeckInfo);
});
