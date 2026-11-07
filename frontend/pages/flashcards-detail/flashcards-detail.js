/**
 * KizunaTutor - Trang Chi Tiết Bộ Thẻ Flashcard Độc Lập
 * Master-Detail Route Architecture: /learner/deck-detail?id=... (hoặc /flashcards-detail?id=...)
 * REST API Client (Zero-Reload) & Tích Hợp KizunaToast
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
    String(c.meaning || '').toLowerCase().includes(q) ||
    String(c.hanViet || '').toLowerCase().includes(q)
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
        <!-- Cột trước: Từ vựng / Câu hỏi -->
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <span class="w-6 h-6 rounded bg-slate-100 border border-[#E2E8F0] flex items-center justify-center text-[10px] font-mono text-slate-500 font-bold shrink-0">
            ${originalIdx + 1}
          </span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-japanese font-bold text-base text-[#0F172A] truncate" title="${escapeHtml(c.char)}">
                ${escapeHtml(c.char)}
              </span>
            </div>
            ${c.hanViet ? `<span class="text-[11px] font-semibold text-[#831843] uppercase tracking-wider">${escapeHtml(c.hanViet)}</span>` : ''}
          </div>
        </div>

        <!-- Cột sau: Nghĩa / Đáp án -->
        <div class="flex-1 min-w-0 text-xs text-[#334155] sm:border-l sm:border-[#E2E8F0] sm:pl-3">
          <span class="font-medium leading-relaxed">${escapeHtml(c.meaning || '—')}</span>
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
  const char = (document.getElementById('inputEditChar').value || '').trim();
  const meaning = (document.getElementById('inputEditMeaning').value || '').trim();

  if (!char) {
    KizunaToast.warning('Vui lòng nhập nội dung mặt trước.');
    return;
  }

  currentCards[editingCardIndex].char = char;
  currentCards[editingCardIndex].meaning = meaning || '—';

  try {
    await apiClient.updateDeck(currentDeckId, { cards: currentCards });
    KizunaToast.success('Đã cập nhật thông tin thẻ!');
  } catch (err) {
    console.error(err);
    KizunaToast.error('Lỗi khi cập nhật thẻ!');
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

// 11. Đọc file Excel 2 cột (Cột 1: Câu hỏi / Từ vựng, Cột 2: Đáp án / Nghĩa)
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
        if (!row || row.length < 2) return;
        const col1 = String(row[0] || '').trim();
        const col2 = String(row[1] || '').trim();

        // Bỏ qua dòng tiêu đề nếu chứa từ khóa
        const lowerCol1 = col1.toLowerCase();
        if (index === 0 && (
          lowerCol1.includes('câu hỏi') || 
          lowerCol1.includes('mặt trước') || 
          lowerCol1.includes('question') || 
          lowerCol1.includes('front') || 
          lowerCol1.includes('thuật ngữ') || 
          lowerCol1.includes('từ vựng')
        )) {
          return;
        }

        if (col1) {
          newCards.push({
            char: col1,
            hanViet: '',
            meaning: col2 || '—',
            onyomi: '—',
            kunyomi: '—',
            jlpt: 'Excel'
          });
        }
      });

      if (newCards.length === 0) {
        KizunaToast.warning('Không tìm thấy dữ liệu hợp lệ! Đảm bảo Cột 1 là Câu hỏi, Cột 2 là Đáp án.');
        return;
      }

      currentCards.push(...newCards);
      await apiClient.addCards(currentDeckId, newCards);
      closeAddCardModal();
      renderOverview();
      renderCardsList();
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
    ["Câu hỏi / Từ vựng (Mặt trước)", "Đáp án / Nghĩa (Mặt sau)"],
    ["桜 (さくら)", "Hoa anh đào - Biểu tượng của mùa xuân Nhật Bản"],
    ["勉強 (べんきょう)", "Học tập, rèn luyện kiến thức"],
    ["先生 (せんせい)", "Thầy cô giáo, giáo viên"],
    ["愛 (あい)", "Tình yêu thương, ái tình, trân quý"],
    ["ありがとうございます", "Cảm ơn bạn rất nhiều (lịch sự)"],
    ["こんにちは", "Xin chào (lời chào ban ngày)"]
  ];
  const ws = XLSX.utils.aoa_to_sheet(sampleData);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Flashcards");
  XLSX.writeFile(wb, "mau-the-kizuna-2cot.xlsx");
  KizunaToast.success('Đã tải xuống file Excel mẫu 2 cột!');
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
  <meta charset="utf-8">
  <title>In Bộ Thẻ - ${escapeHtml(currentDeck.title)}</title>
  <style>
    @page { size: A4; margin: 12mm; }
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; color: #0F172A; background: #FFF; margin: 0; padding: 0; }
    .toolbar { padding: 10px 14px; background: #F1F5F9; margin-bottom: 16px; border-radius: 6px; display: flex; gap: 10px; }
    .btn { padding: 7px 16px; background: #006194; color: white; border: 1px solid #0F172A; border-radius: 4px; font-weight: bold; font-size: 12px; cursor: pointer; }
    .print-header { border-bottom: 2px solid #0F172A; padding-bottom: 8px; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: flex-end; }
    .print-header h1 { margin: 0; font-size: 18px; font-weight: 800; }
    .print-header p { margin: 4px 0 0; font-size: 12px; color: #64748B; }
    .print-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
    .print-card { border: 1.5px dashed #0F172A; border-radius: 6px; padding: 12px 14px; page-break-inside: avoid; display: flex; flex-direction: column; justify-content: space-between; min-height: 110px; background: #FAFAFA; box-sizing: border-box; }
    .card-front { font-size: 16px; font-weight: bold; color: #0F2545; margin-bottom: 6px; border-bottom: 1px solid #E2E8F0; padding-bottom: 6px; }
    .card-back { font-size: 12px; color: #334155; line-height: 1.4; }
    .card-footer { display: flex; justify-content: space-between; font-size: 10px; color: #94A3B8; margin-top: 8px; padding-top: 4px; }
    @media print { .no-print { display: none !important; } body { background: white; } }
  </style>
</head>
<body>
  <div class="toolbar no-print">
    <button class="btn" onclick="window.print()">🖨️ In Ngay (Print / Lưu PDF)</button>
    <button class="btn" style="background:#64748B;" onclick="window.close()">Đóng Cửa Sổ</button>
  </div>
  <div class="print-header">
    <div>
      <h1>🌸 ${escapeHtml(currentDeck.title)}</h1>
      <p>${escapeHtml(currentDeck.description || 'Bộ thẻ Kizuna Flashcards')} • Tổng số: ${currentCards.length} thẻ</p>
    </div>
    <div style="font-size: 11px; font-weight: bold; color: #64748B;">KizunaTutor Flashcards</div>
  </div>
  <div class="print-grid">
    ${currentCards.map((c, i) => `
      <div class="print-card">
        <div>
          <div class="card-front">${escapeHtml(c.char || '')}</div>
          <div class="card-back">${escapeHtml(c.meaning || c.hanViet || '')}</div>
        </div>
        <div class="card-footer">
          <span>Kizuna Flashcards</span>
          <span>#${i + 1}</span>
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

  // Import Excel từ trang chi tiết
  document.getElementById('btnTriggerDetailExcel')?.addEventListener('click', () => {
    document.getElementById('inputDetailExcel')?.click();
  });
  document.getElementById('inputDetailExcel')?.addEventListener('change', handleExcelImport);
  document.getElementById('btnQuickExcelImport')?.addEventListener('click', () => {
    document.getElementById('inputDetailExcel')?.click();
  });
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
