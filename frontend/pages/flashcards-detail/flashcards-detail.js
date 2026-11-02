/**
 * KizunaTutor - Trang Chi Tiết Bộ Thẻ Flashcard Độc Lập
 * Master-Detail Route Architecture: /flashcards-detail?id=...
 */

import { showToast, debounce } from '/js/main.js';
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
let activeCardIndex = 0;
let isFlipped = false;
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

// 3. Phát âm tiếng Nhật (Web Speech API)
function speakText(text) {
  if (!('speechSynthesis' in window)) {
    showToast('Trình duyệt không hỗ trợ phát âm tự động.', 'info');
    return;
  }
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'ja-JP';
  utterance.rate = 0.9;
  window.speechSynthesis.speak(utterance);
}

// 4. Tải dữ liệu bộ thẻ
async function loadDeckDetail() {
  const urlParams = new URLSearchParams(window.location.search);
  currentDeckId = urlParams.get('id');

  if (!currentDeckId) {
    // Thử lấy từ pathname nếu theo cấu trúc /learner/flashcards/:id
    const parts = window.location.pathname.split('/').filter(Boolean);
    if (parts.length > 0 && parts[parts.length - 1] !== 'flashcards-detail') {
      currentDeckId = parts[parts.length - 1];
    }
  }

  if (!currentDeckId) {
    showToast('Không tìm thấy mã định danh bộ thẻ!', 'error');
    setTimeout(() => { window.location.href = '/flashcards'; }, 1500);
    return;
  }

  // 1. Tìm trong LocalStorage
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const decks = JSON.parse(raw);
      if (Array.isArray(decks)) {
        currentDeck = decks.find(d => String(d.id) === String(currentDeckId));
      }
    }
  } catch (e) {
    console.debug('Lỗi đọc localStorage:', e);
  }

  // 2. Nếu chưa có, thử tìm qua REST API Backend
  if (!currentDeck) {
    try {
      const res = await fetch(`/api/user/kanji-folders/${encodeURIComponent(currentDeckId)}`).catch(() => null);
      if (res && res.ok) {
        const d = await res.json();
        currentDeck = {
          id: String(d.id || d.folderId),
          title: d.folderName || d.title,
          description: d.description || '',
          color: d.color || 'blue',
          cards: (d.items || d.cards || []).map(c => ({
            char: c.character || c.kanjiChar || c.char,
            hanViet: c.hanViet || '',
            meaning: c.meaning || '',
            onyomi: c.onyomi || '—',
            kunyomi: c.kunyomi || '—',
            jlpt: c.jlptLevel || c.jlpt || 'Tự do'
          }))
        };
      }
    } catch (e) {
      console.debug('Lỗi kết nối API Backend:', e);
    }
  }

  if (!currentDeck) {
    showToast('Không tìm thấy bộ thẻ tương ứng!', 'error');
    setTimeout(() => { window.location.href = '/flashcards'; }, 1500);
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
  const titleEl = document.getElementById('titleText');
  if (titleEl) titleEl.textContent = currentDeck.title || 'Bộ Thẻ';
  const idBadge = document.getElementById('identifierBadge');
  if (idBadge) idBadge.textContent = `#${currentDeck.id}`;
  document.getElementById('overviewTitle').textContent = currentDeck.title || 'Chưa đặt tên';
  document.getElementById('overviewDesc').textContent = currentDeck.description || 'Chưa có ghi chú mô tả cho bộ thẻ này.';
  const totalCardsEl = document.getElementById('overviewTotalCards');
  if (totalCardsEl) totalCardsEl.textContent = currentCards.length;
  document.getElementById('badgeCardCount').textContent = `${currentCards.length} thẻ`;

  // Chuẩn bị thông tin trong modal chọn cách học
  document.getElementById('chooseStudyDeckTitle').textContent = currentDeck.title;
  document.getElementById('chooseStudyDeckCount').textContent = `${currentCards.length} thẻ học`;
}

// 6. Render Danh Sách Thẻ Con
function renderCardsList(filterQuery = '') {
  const container = document.getElementById('detailListContainer');
  const emptyState = document.getElementById('emptyCardsState');
  const countBadge = document.getElementById('filteredCountBadge');

  if (!container || !emptyState) return;

  const q = filterQuery.trim().toLowerCase();
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

  container.innerHTML = filtered.map((c, idx) => {
    const originalIdx = currentCards.indexOf(c);
    const isKanji = (c.char && c.char.length === 1 && !c.char.match(/[a-zA-Z0-9\s]/));

    return `
      <div class="p-3 bg-white border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] hover:bg-slate-50 transition flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 group">
        <!-- Cột trước: Từ vựng / Câu hỏi -->
        <div class="flex items-center gap-3 flex-1 min-w-0">
          <span class="w-6 h-6 rounded bg-slate-100 border border-slate-300 flex items-center justify-center text-[10px] font-mono text-slate-500 font-bold shrink-0">
            ${originalIdx + 1}
          </span>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2">
              <span class="font-japanese font-bold text-base text-slate-900 truncate" title="${escapeHtml(c.char)}">
                ${escapeHtml(c.char)}
              </span>
            </div>
            ${c.hanViet ? `<span class="text-[11px] font-semibold text-[#006194] uppercase tracking-wider">${escapeHtml(c.hanViet)}</span>` : ''}
          </div>
        </div>

        <!-- Cột sau: Nghĩa / Đáp án -->
        <div class="flex-1 min-w-0 text-xs text-slate-700 sm:border-l sm:border-slate-200 sm:pl-3">
          <span class="font-medium leading-relaxed">${escapeHtml(c.meaning || '—')}</span>
        </div>

        <!-- Các nút thao tác lẻ -->
        <div class="flex items-center gap-1.5 shrink-0 self-end sm:self-center">
          ${isKanji ? `
            <a href="/shodo?kanji=${encodeURIComponent(c.char)}" 
               class="py-1 px-2 text-[11px] font-bold rounded border border-pink-300 bg-[#FFE5EC] text-[#831843] hover:bg-pink-100 flex items-center gap-1 transition no-underline"
               title="Luyện viết nét chữ này trên Canvas Shodo">
              <span>🖌️</span>
              <span>Luyện viết</span>
            </a>
          ` : ''}
          <button type="button" data-action="edit-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded border border-slate-300 text-slate-700 hover:bg-slate-100 cursor-pointer font-bold text-xs"
                  title="Chỉnh sửa thẻ này">
            ✏️
          </button>
          <button type="button" data-action="delete-card" data-index="${originalIdx}"
                  class="w-7 h-7 flex items-center justify-center rounded border border-rose-200 text-rose-600 hover:bg-rose-50 cursor-pointer font-bold text-xs"
                  title="Xóa thẻ này khỏi bộ">
            ✕
          </button>
        </div>
      </div>
    `;
  }).join('');

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

// 7. Xóa 1 thẻ lẻ
function deleteSingleCard(index) {
  if (index < 0 || index >= currentCards.length) return;
  const removed = currentCards.splice(index, 1);
  saveCurrentDeck();
  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
  showToast(`Đã xóa thẻ [${removed[0]?.char}] khỏi bộ.`, 'info');
}

// 8. Lưu bộ thẻ vào Storage và Backend
function saveCurrentDeck() {
  if (!currentDeck) return;
  currentDeck.cards = currentCards;

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    let allDecks = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(allDecks)) allDecks = [];

    const existingIdx = allDecks.findIndex(d => String(d.id) === String(currentDeck.id));
    if (existingIdx >= 0) {
      allDecks[existingIdx] = currentDeck;
    } else {
      allDecks.unshift(currentDeck);
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(allDecks));
  } catch (e) {
    console.debug('Lỗi lưu localStorage:', e);
  }
}

// 9. Modal Chỉnh Sửa Tên & Mô Tả Bộ Thẻ
function openEditDeckModal() {
  if (!currentDeck) return;
  document.getElementById('inputEditDeckName').value = currentDeck.title || '';
  document.getElementById('inputEditDeckDesc').value = currentDeck.description || '';
  document.getElementById('modalEditDeckInfo').classList.remove('hidden');
}

function closeEditDeckModal() {
  document.getElementById('modalEditDeckInfo').classList.add('hidden');
}

function saveEditDeckInfo() {
  const name = (document.getElementById('inputEditDeckName').value || '').trim();
  const desc = (document.getElementById('inputEditDeckDesc').value || '').trim();

  if (!name) {
    showToast('Tên bộ thẻ không được để trống.', 'error');
    return;
  }

  currentDeck.title = name;
  currentDeck.description = desc;
  saveCurrentDeck();
  renderOverview();
  closeEditDeckModal();
  showToast('Đã cập nhật thông tin bộ thẻ thành công!', 'success');
}

// 10. Modal Thêm Thẻ Mới Thủ Công
function openAddCardModal() {
  document.getElementById('inputNewChar').value = '';
  document.getElementById('inputNewMeaning').value = '';
  document.getElementById('modalAddCard').classList.remove('hidden');
}

function closeAddCardModal() {
  document.getElementById('modalAddCard').classList.add('hidden');
}

function submitNewCard() {
  const char = (document.getElementById('inputNewChar').value || '').trim();
  const meaning = (document.getElementById('inputNewMeaning').value || '').trim();

  if (!char || !meaning) {
    showToast('Vui lòng nhập đầy đủ mặt trước và mặt sau của thẻ.', 'error');
    return;
  }

  currentCards.push({
    char: char,
    hanViet: '',
    meaning: meaning,
    onyomi: '—',
    kunyomi: '—',
    jlpt: 'Tự do'
  });

  saveCurrentDeck();
  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
  closeAddCardModal();
  showToast(`Đã thêm thẻ [${char}] vào bộ thành công!`, 'success');
}

// 10.1 Modal Chỉnh Sửa Thẻ Học
function openEditCardModal(index) {
  if (index < 0 || index >= currentCards.length) return;
  editingCardIndex = index;
  const card = currentCards[index];
  document.getElementById('inputEditChar').value = card.char || '';
  document.getElementById('inputEditHanViet').value = card.hanViet || '';
  document.getElementById('inputEditMeaning').value = card.meaning || '';
  document.getElementById('modalEditCard').classList.remove('hidden');
}

function closeEditCardModal() {
  editingCardIndex = -1;
  document.getElementById('modalEditCard').classList.add('hidden');
}

function saveEditCard() {
  if (editingCardIndex < 0 || editingCardIndex >= currentCards.length) return;
  const char = (document.getElementById('inputEditChar').value || '').trim();
  const hanViet = (document.getElementById('inputEditHanViet').value || '').trim();
  const meaning = (document.getElementById('inputEditMeaning').value || '').trim();

  if (!char || !meaning) {
    showToast('Vui lòng nhập đầy đủ mặt trước và mặt sau của thẻ.', 'error');
    return;
  }

  currentCards[editingCardIndex].char = char;
  currentCards[editingCardIndex].hanViet = hanViet;
  currentCards[editingCardIndex].meaning = meaning;

  saveCurrentDeck();
  renderOverview();
  renderCardsList(document.getElementById('internalSearchInput')?.value || '');
  closeEditCardModal();
  showToast(`Đã cập nhật thẻ [${char}] thành công!`, 'success');
}

// 11. Modal Chọn Cách Học Thẻ
function openChooseStudyMode() {
  if (currentCards.length === 0) {
    showToast('Bộ thẻ này chưa có thẻ nào để học!', 'warning');
    return;
  }
  document.getElementById('modalChooseStudyMode').classList.remove('hidden');
}

function closeChooseStudyMode() {
  document.getElementById('modalChooseStudyMode').classList.add('hidden');
}

// 12. Chế Độ Lật Thẻ 3D Flashcard
function openStudy3D() {
  closeChooseStudyMode();
  if (currentCards.length === 0) return;

  activeCardIndex = 0;
  isFlipped = false;
  document.getElementById('studyDeckTitle').textContent = currentDeck.title;
  document.getElementById('studyDeckSubtitle').textContent = `${currentCards.length} thẻ trong bộ`;
  document.getElementById('modalStudyDeck').classList.remove('hidden');
  renderStudyCard();
}

function closeStudy3D() {
  document.getElementById('modalStudyDeck').classList.add('hidden');
}

function renderStudyCard() {
  const cardEl = document.getElementById('flashcardElement');
  cardEl.classList.remove('is-flipped');
  isFlipped = false;

  const card = currentCards[activeCardIndex];
  const total = currentCards.length;

  document.getElementById('studyCardProgressText').textContent = `Thẻ ${activeCardIndex + 1} / ${total}`;
  document.getElementById('studyCardLevel').textContent = card.jlpt || 'Flashcard';
  document.getElementById('studyCardChar').textContent = card.char || '—';
  document.getElementById('studyCardCharBack').textContent = card.char || '—';
  document.getElementById('studyCardHanViet').textContent = card.hanViet || '';
  document.getElementById('studyCardMeaning').textContent = card.meaning || '—';

  const isKanji = (card.char && card.char.length === 1 && !card.char.match(/[a-zA-Z0-9\s]/));
  const shodoBtn = document.getElementById('btnStudyPracticeShodo');
  if (isKanji) {
    shodoBtn.classList.remove('hidden');
    shodoBtn.href = `/shodo?kanji=${encodeURIComponent(card.char)}`;
  } else {
    shodoBtn.classList.add('hidden');
  }
}

function flipStudyCard() {
  const cardEl = document.getElementById('flashcardElement');
  isFlipped = !isFlipped;
  cardEl.classList.toggle('is-flipped', isFlipped);
}

function nextStudyCard() {
  if (activeCardIndex < currentCards.length - 1) {
    activeCardIndex++;
  } else {
    activeCardIndex = 0;
    showToast('Đã ôn hết bộ thẻ! Bắt đầu lại từ thẻ đầu tiên.', 'info');
  }
  renderStudyCard();
}

function prevStudyCard() {
  if (activeCardIndex > 0) {
    activeCardIndex--;
  } else {
    activeCardIndex = currentCards.length - 1;
  }
  renderStudyCard();
}

// 13. Import Excel 2 Cột (.xlsx, .xls)
function handleExcelUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  if (typeof XLSX === 'undefined') {
    showToast('Thư viện Excel đang tải, vui lòng kiểm tra kết nối mạng!', 'error');
    event.target.value = '';
    return;
  }

  const reader = new FileReader();
  reader.onload = function(e) {
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

        if (col1 && col2) {
          newCards.push({
            char: col1,
            hanViet: '',
            meaning: col2,
            onyomi: '—',
            kunyomi: '—',
            jlpt: 'Excel'
          });
        }
      });

      if (newCards.length === 0) {
        showToast('Không tìm thấy dữ liệu hợp lệ! Đảm bảo Cột 1 là Câu hỏi, Cột 2 là Đáp án.', 'warning');
        return;
      }

      currentCards = currentCards.concat(newCards);
      saveCurrentDeck();
      renderOverview();
      renderCardsList(document.getElementById('internalSearchInput')?.value || '');
      closeAddCardModal();
      showToast(`Đã nạp thành công ${newCards.length} thẻ mới từ file Excel!`, 'success');
    } catch (err) {
      console.error(err);
      showToast('Lỗi khi đọc file Excel. Vui lòng kiểm tra lại định dạng!', 'error');
    } finally {
      event.target.value = '';
    }
  };
  reader.readAsArrayBuffer(file);
}

// 14. Tải file Excel mẫu 2 cột
function downloadSampleExcel() {
  if (typeof XLSX === 'undefined') {
    showToast('Thư viện Excel chưa sẵn sàng, vui lòng thử lại sau!', 'warning');
    return;
  }
  const sampleData = [
    ["Câu hỏi (Mặt trước)", "Đáp án (Mặt sau)"],
    ["桜 (さくら)", "Hoa anh đào - Biểu tượng của mùa xuân Nhật Bản"],
    ["勉強 (べんきょう)", "Học tập, rèn luyện kiến thức"],
    ["先生 (せんせい)", "Thầy cô giáo, giáo viên, bác sĩ"],
    ["愛 (あい)", "Tình yêu thương, trân quý"],
    ["ありがとうございます", "Cảm ơn bạn rất nhiều (lịch sự)"],
    ["こんにちは", "Xin chào (lời chào ban ngày)"]
  ];
  const ws = XLSX.utils.aoa_to_sheet(sampleData);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Flashcards");
  XLSX.writeFile(wb, "Kizuna_Flashcard_Mau.xlsx");
  showToast('Đã tải xuống file Excel mẫu 2 cột!', 'success');
}

// 15. Xuất Excel bộ thẻ hiện tại
function exportCurrentDeckExcel() {
  if (currentCards.length === 0) {
    showToast('Bộ thẻ này chưa có thẻ nào để xuất file!', 'warning');
    return;
  }
  if (typeof XLSX === 'undefined') {
    showToast('Thư viện Excel chưa sẵn sàng!', 'warning');
    return;
  }

  const exportData = [
    ["Câu hỏi (Mặt trước)", "Đáp án (Mặt sau)", "Âm Hán Việt"]
  ];
  currentCards.forEach(c => {
    exportData.push([c.char || '', c.meaning || '', c.hanViet || '']);
  });

  const ws = XLSX.utils.aoa_to_sheet(exportData);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Cards");
  const fileName = `${currentDeck.title || 'Bo_The'}_Cards.xlsx`;
  XLSX.writeFile(wb, fileName);
  showToast(`Đã xuất file "${fileName}" thành công!`, 'success');
}

// 16. In ấn bộ thẻ (A4 Print / PDF)
function printDeck() {
  if (currentCards.length === 0) {
    showToast('Bộ thẻ chưa có thẻ nào để in!', 'warning');
    return;
  }

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    showToast('Vui lòng cho phép popup để mở trang in!', 'error');
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
    .card-footer { display: flex; justify-content: space-between; font-size: 10px; color: #94A3B8; margin-top: 8px; }
    @media print { .no-print { display: none !important; } }
  </style>
</head>
<body>
  <div class="toolbar no-print">
    <button class="btn" onclick="window.print()">🖨️ In Ngay (Print / PDF)</button>
    <button class="btn" style="background:#64748B;" onclick="window.close()">Đóng Cửa Sổ</button>
  </div>
  <div class="print-header">
    <div>
      <h1>🌸 ${escapeHtml(currentDeck.title)}</h1>
      <p>${escapeHtml(currentDeck.description || '')} • Tổng số: ${currentCards.length} thẻ</p>
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

// 17. Xóa toàn bộ bộ thẻ
async function deleteCurrentDeck() {
  if (!currentDeck) return;
  if (!confirm(`Bạn có chắc chắn muốn xóa toàn bộ bộ thẻ "${currentDeck.title}" không? Hành động này không thể hoàn tác.`)) {
    return;
  }

  try {
    await fetch(`/api/user/kanji-folders/${encodeURIComponent(currentDeck.id)}`, {
      method: 'DELETE'
    }).catch(() => null);
  } catch (e) {
    console.debug('Lỗi xóa API backend:', e);
  }

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      let allDecks = JSON.parse(raw);
      if (Array.isArray(allDecks)) {
        allDecks = allDecks.filter(d => String(d.id) !== String(currentDeck.id));
        localStorage.setItem(STORAGE_KEY, JSON.stringify(allDecks));
      }
    }
  } catch (e) {
    console.debug('Lỗi xóa localStorage:', e);
  }

  showToast(`Đã xóa bộ thẻ "${currentDeck.title}".`, 'info');
  setTimeout(() => {
    window.location.href = '/flashcards';
  }, 500);
}

// 18. DOM Ready & Gán sự kiện
document.addEventListener('DOMContentLoaded', () => {
  loadDeckDetail();

  // Nút quay lại danh sách
  document.getElementById('btnBackToList')?.addEventListener('click', () => {
    window.location.href = '/flashcards';
  });

  // Tác vụ chính: Bắt đầu học
  document.getElementById('btnPrimaryAction')?.addEventListener('click', openChooseStudyMode);

  // Chọn phương thức học
  document.getElementById('btnCloseChooseStudyModeModal')?.addEventListener('click', closeChooseStudyMode);
  document.getElementById('btnCancelChooseStudyMode')?.addEventListener('click', closeChooseStudyMode);
  document.getElementById('btnSelectModeFlashcard')?.addEventListener('click', openStudy3D);
  document.getElementById('btnSelectModeKanjiGame')?.addEventListener('click', () => {
    closeChooseStudyMode();
    window.location.href = `/kanji-game?deckId=${encodeURIComponent(currentDeckId)}`;
  });

  // 3D Study modal controls
  document.getElementById('flashcardElement')?.addEventListener('click', flipStudyCard);
  document.getElementById('btnFlipCard')?.addEventListener('click', flipStudyCard);
  document.getElementById('btnNextCard')?.addEventListener('click', nextStudyCard);
  document.getElementById('btnPrevCard')?.addEventListener('click', prevStudyCard);
  document.getElementById('btnCloseStudyModal')?.addEventListener('click', closeStudy3D);

  // Chỉnh sửa bộ thẻ
  document.getElementById('btnEditAction')?.addEventListener('click', openEditDeckModal);
  document.getElementById('btnCloseEditDeckModal')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnCancelEditDeck')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnSaveEditDeck')?.addEventListener('click', saveEditDeckInfo);

  // Xuất file & In
  const exportDropdown = document.getElementById('exportDropdownMenu');
  document.getElementById('btnExportMenuToggle')?.addEventListener('click', (e) => {
    e.stopPropagation();
    exportDropdown.classList.toggle('hidden');
  });
  document.addEventListener('click', () => {
    exportDropdown?.classList.add('hidden');
  });
  document.getElementById('btnExportExcel')?.addEventListener('click', () => {
    exportDropdown?.classList.add('hidden');
    exportCurrentDeckExcel();
  });
  document.getElementById('btnPrintDeck')?.addEventListener('click', () => {
    exportDropdown?.classList.add('hidden');
    printDeck();
  });

  // Xóa bộ thẻ
  document.getElementById('btnDeleteAction')?.addEventListener('click', deleteCurrentDeck);

  // Thêm thẻ lẻ
  document.getElementById('btnOpenAddCardModal')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnEmptyAddCard')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnCloseAddCardModal')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnCancelAddCard')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnSubmitAddCard')?.addEventListener('click', submitNewCard);

  // Sửa thẻ lẻ
  document.getElementById('btnCloseEditCardModal')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnCancelEditCard')?.addEventListener('click', closeEditCardModal);
  document.getElementById('btnSaveEditCard')?.addEventListener('click', saveEditCard);

  // Excel Nạp & Mẫu
  document.getElementById('btnQuickExcelImport')?.addEventListener('click', () => {
    document.getElementById('inputExcelFile')?.click();
  });
  document.getElementById('inputExcelFile')?.addEventListener('change', handleExcelUpload);
  document.getElementById('btnDownloadSample')?.addEventListener('click', downloadSampleExcel);

  // Tìm kiếm nội bộ
  const searchInput = document.getElementById('internalSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderCardsList(e.target.value);
    }, 200));
  }
});
