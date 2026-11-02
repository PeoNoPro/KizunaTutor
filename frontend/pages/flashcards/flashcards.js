/**
 * KizunaTutor - Quản lý & Luyện tập Bộ thẻ ghi nhớ Flashcards
 * Feature-Based architecture module, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast, debounce } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Bộ thẻ Flashcards',
  badge: 'Bộ nhớ siêu tốc'
});

// =========================================================================
// 1. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
const STORAGE_KEY = 'kizuna_flashcard_decks';
let userDecks = [];
let activeDeck = null;
let currentCardIndex = 0;
let isFlipped = false;

// =========================================================================
// 2. QUẢN LÝ LƯU TRỮ THỰC TẾ (LOCAL STORAGE & BACKEND API)
// =========================================================================
async function loadDecks() {
  try {
    // 1. Thử lấy từ REST API Backend nếu có kết nối
    const res = await fetch('/api/user/kanji-folders').catch(() => null);
    if (res && res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        userDecks = data.map(d => ({
          id: String(d.id || d.folderId),
          title: d.folderName || d.title,
          description: d.description || '',
          color: d.color || 'blue',
          cards: (d.items || d.cards || []).map(c => ({
            char: c.character || c.kanjiChar || c.char,
            hanViet: c.hanViet || 'HÁN TỰ',
            meaning: c.meaning || '',
            onyomi: c.onyomi || '—',
            kunyomi: c.kunyomi || '—',
            jlpt: c.jlptLevel || c.jlpt || 'JLPT'
          }))
        }));
        saveLocalDecks();
        renderDecks();
        return;
      }
    }
  } catch (e) {
    console.debug('Không kết nối được API Backend, kiểm tra bộ nhớ cục bộ:', e);
  }

  // 2. Lấy dữ liệu thực tế do người dùng tự tạo từ LocalStorage (Tuyệt đối không chèn fake data)
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      // Lọc bỏ bất kỳ bộ thẻ mẫu cũ nào nếu từng tồn tại
      userDecks = Array.isArray(parsed) 
        ? parsed.filter(d => !String(d.id).startsWith('deck-n5-') && !String(d.id).startsWith('deck-n4-') && !String(d.id).startsWith('deck-kizuna-'))
        : [];
    } else {
      userDecks = [];
    }
  } catch (e) {
    userDecks = [];
  }

  saveLocalDecks();
  renderDecks();
}

function saveLocalDecks() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(userDecks));
  } catch (e) {
    console.debug('Không thể lưu localStorage:', e);
  }
}

function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// =========================================================================
// 3. RENDER DANH SÁCH BỘ THẺ (HIỂN THỊ EMPTY STATE NẾU CHƯA CÓ DỮ LIỆU)
// =========================================================================
function renderDecks(filterQuery = '') {
  const grid = document.getElementById('deckGrid');
  const emptyState = document.getElementById('deckEmptyState');
  const countBadge = document.getElementById('deckCountBadge');

  if (!grid || !emptyState) return;

  const q = filterQuery.trim().toLowerCase();
  const filtered = userDecks.filter(d => 
    d.title.toLowerCase().includes(q) || (d.description && d.description.toLowerCase().includes(q))
  );

  if (countBadge) {
    countBadge.textContent = `${filtered.length} bộ thẻ`;
  }

  // Khi chưa có dữ liệu thực tế hoặc tìm kiếm không khớp -> Hiển thị Empty State chuẩn
  if (filtered.length === 0) {
    grid.innerHTML = '';
    grid.classList.add('hidden');
    emptyState.classList.remove('hidden');
    return;
  }

  emptyState.classList.add('hidden');
  grid.classList.remove('hidden');

  let html = '';

  filtered.forEach(deck => {
    const cardCount = deck.cards ? deck.cards.length : 0;

    html += `
      <div class="deck-card relative flex flex-col justify-between p-4 bg-white border border-[#0F172A] rounded-[5px] shadow-[3px_3px_0px_0px_#0F172A] hover:-translate-y-0.5 transition-transform group" data-deck-id="${deck.id}">
        <!-- Phần thông tin thư mục -->
        <div>
          <div class="flex items-start justify-between gap-2">
            <h3 class="font-['Itim'] text-base font-bold text-slate-900 truncate flex-1 cursor-pointer hover:text-[#006194] transition" 
                title="Nhấn để chọn cách học bộ thẻ: ${escapeHtml(deck.title)}"
                data-action="prompt-study" data-id="${deck.id}">
              🌸 ${escapeHtml(deck.title)}
            </h3>
            
            <!-- Nút Thao Tác Thêm (Dropdown Trigger) -->
            <div class="relative dropdown-container shrink-0">
              <button type="button" 
                      data-action="toggle-menu" data-id="${deck.id}"
                      class="w-7 h-7 flex items-center justify-center border border-[#0F172A] rounded-[4px] bg-slate-100 hover:bg-slate-200 text-xs shadow-[1px_1px_0px_0px_#0F172A] cursor-pointer transition font-bold"
                      title="Tùy chọn khác">
                ⋮
              </button>

              <!-- Dropdown Menu Nội dung -->
              <div id="menu-${deck.id}" class="deck-dropdown-menu hidden absolute right-0 top-8 w-48 bg-white border border-[#0F172A] rounded-[5px] shadow-[3px_3px_0px_0px_#0F172A] z-30 py-1 text-xs">
                <button type="button" data-action="print-deck" data-id="${deck.id}" class="w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center gap-2 cursor-pointer text-slate-800">
                  <span>🖨️</span> In bộ thẻ (PDF)
                </button>
                <button type="button" data-action="duplicate-deck" data-id="${deck.id}" class="w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center gap-2 cursor-pointer text-slate-800">
                  <span>📋</span> Nhân bản bộ thẻ
                </button>
                <button type="button" data-action="merge-deck" data-id="${deck.id}" class="w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center gap-2 border-t border-slate-100 cursor-pointer text-slate-800">
                  <span>🔗</span> Gộp với bộ thẻ khác
                </button>
              </div>
            </div>
          </div>

          <!-- Mô tả thư mục -->
          <p class="text-xs text-slate-600 mt-1 line-clamp-2" title="${escapeHtml(deck.description || '')}">
            ${escapeHtml(deck.description || 'Chưa có ghi chú mô tả cho bộ thẻ này.')}
          </p>
          <div class="mt-2 text-[11px] font-semibold text-pink-600 flex items-center gap-1.5">
            <span>🏷️</span>
            <span>${cardCount} thẻ học</span>
          </div>
        </div>

        <!-- Cụm nút hành động chính ở chân thẻ -->
        <div class="grid grid-cols-3 gap-2 mt-4 pt-3 border-t border-slate-200">
          <!-- Nút Học & Lật Thẻ -->
          <button type="button" 
                  data-action="prompt-study" data-id="${deck.id}" 
                  class="py-1.5 px-2 bg-[#006194] hover:bg-[#004B73] text-white text-xs font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center gap-1 cursor-pointer truncate"
                  title="Chọn cách học bộ thẻ (Lật thẻ 3D hoặc Minigame bắn chữ)">
            <span>👁️</span> Học Thẻ
          </button>

          <!-- Nút Chỉnh Sửa -->
          <button type="button" 
                  data-action="edit-deck" data-id="${deck.id}" 
                  class="py-1.5 px-2 bg-white hover:bg-slate-50 text-slate-800 text-xs font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center gap-1 cursor-pointer truncate"
                  title="Chỉnh sửa & nạp Excel">
            <span>✏️</span> Sửa
          </button>

          <!-- Nút Xóa -->
          <button type="button" 
                  data-action="delete-deck" data-id="${deck.id}" 
                  class="py-1.5 px-2 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center gap-1 cursor-pointer truncate"
                  title="Xóa bộ thẻ này">
            <span>🗑️</span> Xóa
          </button>
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;

  grid.querySelectorAll('[data-action="prompt-study"]').forEach(btn => {
    btn.addEventListener('click', () => promptStudyDeck(btn.dataset.id));
  });

  grid.querySelectorAll('[data-action="edit-deck"]').forEach(btn => {
    btn.addEventListener('click', () => openEditDeckModal(btn.dataset.id));
  });

  grid.querySelectorAll('[data-action="delete-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      handleDeleteDeck(btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="toggle-menu"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      toggleDeckMenu(e, btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="print-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      handlePrintDeck(btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="duplicate-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      handleDuplicateDeck(btn.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="merge-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      closeAllMenus();
      openMergeModal(btn.dataset.id);
    });
  });
}

function toggleDeckMenu(event, deckId) {
  event.stopPropagation();
  const targetMenu = document.getElementById(`menu-${deckId}`);
  if (!targetMenu) return;

  const isHidden = targetMenu.classList.contains('hidden');
  closeAllMenus();
  if (isHidden) {
    targetMenu.classList.remove('hidden');
  }
}

function closeAllMenus() {
  document.querySelectorAll('.deck-dropdown-menu').forEach(m => m.classList.add('hidden'));
}

// =========================================================================
// 4. CHỌN CHẾ ĐỘ HỌC & CHẾ ĐỘ LẬT THẺ FLASHCARD 3D
// =========================================================================
let selectedStudyDeckId = null;

function promptStudyDeck(deckId) {
  const deck = userDecks.find(d => d.id === deckId);
  if (!deck) return;
  selectedStudyDeckId = deckId;

  document.getElementById('chooseStudyDeckTitle').textContent = deck.title;
  document.getElementById('chooseStudyDeckCount').textContent = `${(deck.cards || []).length} thẻ học`;
  document.getElementById('modalChooseStudyMode').classList.remove('hidden');
}

function closeChooseStudyModeModal() {
  document.getElementById('modalChooseStudyMode').classList.add('hidden');
  selectedStudyDeckId = null;
}

function openStudyModal(deckId) {
  const deck = userDecks.find(d => d.id === deckId);
  if (!deck) return;

  activeDeck = deck;
  currentCardIndex = 0;
  isFlipped = false;

  const modal = document.getElementById('modalStudyDeck');
  document.getElementById('studyDeckTitle').textContent = deck.title;
  document.getElementById('studyDeckSubtitle').textContent = `${(deck.cards || []).length} chữ Hán trong bộ thẻ`;

  modal.classList.remove('hidden');
  renderCurrentCard();
  renderMiniGrid();
}

function closeStudyModal() {
  document.getElementById('modalStudyDeck').classList.add('hidden');
  activeDeck = null;
  renderDecks();
}

function renderCurrentCard() {
  const cardEl = document.getElementById('flashcardElement');
  cardEl.classList.remove('is-flipped');
  isFlipped = false;

  if (!activeDeck || !activeDeck.cards || activeDeck.cards.length === 0) {
    document.getElementById('studyCardChar').textContent = '📭';
    document.getElementById('studyCardProgressText').textContent = '0 / 0';
    document.getElementById('studyCardHanViet').textContent = 'CHƯA CÓ CHỮ HÁN';
    document.getElementById('studyCardMeaning').textContent = 'Bộ thẻ này hiện chưa có chữ Hán nào. Vui lòng bấm "Thêm chữ" ở góc trên để bổ sung.';
    document.getElementById('studyCardOnyomi').textContent = '—';
    document.getElementById('studyCardKunyomi').textContent = '—';
    document.getElementById('btnStudyPracticeShodo').classList.add('hidden');
    return;
  }

  document.getElementById('btnStudyPracticeShodo').classList.remove('hidden');

  const card = activeDeck.cards[currentCardIndex];
  const total = activeDeck.cards.length;

  document.getElementById('studyCardProgressText').textContent = `Thẻ ${currentCardIndex + 1} / ${total}`;
  document.getElementById('studyCardLevel').textContent = card.jlpt || 'JLPT';
  document.getElementById('studyCardChar').textContent = card.char;
  document.getElementById('studyCardCharBack').textContent = card.char;
  document.getElementById('studyCardHanViet').textContent = card.hanViet || 'HÁN VIỆT';
  document.getElementById('studyCardMeaning').textContent = card.meaning || 'Chưa cập nhật nghĩa';
  document.getElementById('studyCardOnyomi').textContent = card.onyomi || '—';
  document.getElementById('studyCardKunyomi').textContent = card.kunyomi || '—';

  const shodoBtn = document.getElementById('btnStudyPracticeShodo');
  shodoBtn.href = `/shodo?kanji=${encodeURIComponent(card.char)}`;
}

function flipCurrentCard() {
  const cardEl = document.getElementById('flashcardElement');
  isFlipped = !isFlipped;
  cardEl.classList.toggle('is-flipped', isFlipped);
}

function nextCard() {
  if (!activeDeck || !activeDeck.cards || activeDeck.cards.length === 0) return;
  if (currentCardIndex < activeDeck.cards.length - 1) {
    currentCardIndex++;
  } else {
    currentCardIndex = 0;
    showToast('Đã xem hết bộ thẻ! Quay lại từ thẻ đầu tiên.', 'info');
  }
  renderCurrentCard();
}

function prevCard() {
  if (!activeDeck || !activeDeck.cards || activeDeck.cards.length === 0) return;
  if (currentCardIndex > 0) {
    currentCardIndex--;
  } else {
    currentCardIndex = activeDeck.cards.length - 1;
  }
  renderCurrentCard();
}

function renderMiniGrid() {
  const container = document.getElementById('deckCardsMiniGrid');
  const countEl = document.getElementById('miniGridCount');
  if (!container || !activeDeck) return;

  const cards = activeDeck.cards || [];
  if (countEl) countEl.textContent = `${cards.length} chữ`;

  if (cards.length === 0) {
    container.innerHTML = '<span class="text-xs text-[#68737D] italic">Chưa có chữ Hán nào trong bộ thẻ này.</span>';
    return;
  }

  let html = '';
  cards.forEach((c, idx) => {
    const isCurrent = idx === currentCardIndex;
    html += `
      <div class="relative group/mini">
        <button type="button" data-index="${idx}"
                class="w-10 h-10 rounded-xl border font-japanese font-bold text-sm transition flex items-center justify-center cursor-pointer ${
                  isCurrent 
                    ? 'bg-[#FFE5EC] border-[#FBCFE8] text-[#831843] shadow-xs' 
                    : 'bg-[#FAF8F2] border-[#ECE8DE] text-[#0F2545] hover:bg-white'
                }">
          ${c.char}
        </button>
        <button type="button" data-action="remove-card" data-index="${idx}"
                class="hidden group-hover/mini:flex absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-rose-600 text-white text-[10px] items-center justify-center cursor-pointer shadow-xs"
                title="Xóa chữ này khỏi bộ thẻ">
          &times;
        </button>
      </div>
    `;
  });

  container.innerHTML = html;

  container.querySelectorAll('button[data-index]').forEach(b => {
    b.addEventListener('click', () => {
      currentCardIndex = parseInt(b.dataset.index, 10);
      renderCurrentCard();
      renderMiniGrid();
    });
  });

  container.querySelectorAll('[data-action="remove-card"]').forEach(b => {
    b.addEventListener('click', (e) => {
      e.stopPropagation();
      const idx = parseInt(b.dataset.index, 10);
      removeCardFromDeck(idx);
    });
  });
}

function removeCardFromDeck(index) {
  if (!activeDeck || !activeDeck.cards) return;
  const removed = activeDeck.cards.splice(index, 1);
  if (currentCardIndex >= activeDeck.cards.length) {
    currentCardIndex = Math.max(0, activeDeck.cards.length - 1);
  }
  saveLocalDecks();
  renderCurrentCard();
  renderMiniGrid();
  showToast(`Đã xóa chữ [${removed[0]?.char}] khỏi bộ thẻ.`, 'info');
}

// =========================================================================
// 5. TẠO & XÓA BỘ THẺ (CRUD LOGIC)
// =========================================================================
function openCreateDeckModal() {
  document.getElementById('modalCreateDeck').classList.remove('hidden');
  document.getElementById('deckNameInput').value = '';
  document.getElementById('deckDescInput').value = '';
}

function closeCreateDeckModal() {
  document.getElementById('modalCreateDeck').classList.add('hidden');
}

function saveNewDeck() {
  const nameInput = document.getElementById('deckNameInput');
  const descInput = document.getElementById('deckDescInput');
  const title = (nameInput.value || '').trim();
  const desc = (descInput.value || '').trim();

  if (!title) {
    showToast('Vui lòng nhập tên cho bộ thẻ nhé.', 'error');
    nameInput.focus();
    return;
  }

  const newDeck = {
    id: `deck-${Date.now()}`,
    title: title,
    description: desc,
    color: 'blue',
    cards: []
  };

  userDecks.unshift(newDeck);
  saveLocalDecks();
  closeCreateDeckModal();
  renderDecks();
  showToast(`Đã tạo thành công bộ thẻ "${title}".`, 'success');
}

async function handleDeleteDeck(deckId) {
  const target = userDecks.find(d => d.id === deckId);
  if (!target) return;

  if (!confirm(`Bạn có chắc chắn muốn xóa bộ thẻ "${target.title}" không?`)) {
    return;
  }

  // Cố gắng xóa trên backend API nếu có kết nối
  try {
    await fetch(`/api/user/kanji-folders/${encodeURIComponent(deckId)}`, {
      method: 'DELETE'
    }).catch(() => null);
  } catch (e) {
    console.debug('Lỗi khi gọi API xóa folder:', e);
  }

  userDecks = userDecks.filter(d => d.id !== deckId);
  saveLocalDecks();
  renderDecks();
  showToast(`Đã xóa bộ thẻ "${target.title}".`, 'info');
}

const deleteDeck = handleDeleteDeck;

// =========================================================================
// 6. THÊM CHỮ MỚI VÀO BỘ THẺ
// =========================================================================
function openAddCardModal() {
  document.getElementById('modalAddCard').classList.remove('hidden');
  document.getElementById('inputNewKanjiChar').value = '';
  document.getElementById('inputNewHanViet').value = '';
  document.getElementById('inputNewMeaning').value = '';
}

function closeAddCardModal() {
  document.getElementById('modalAddCard').classList.add('hidden');
}

function submitNewCard() {
  if (!activeDeck) return;

  const charInput = document.getElementById('inputNewKanjiChar');
  const hanVietInput = document.getElementById('inputNewHanViet');
  const meaningInput = document.getElementById('inputNewMeaning');

  const char = (charInput.value || '').trim().charAt(0);
  const hanViet = (hanVietInput.value || '').trim().toUpperCase();
  const meaning = (meaningInput.value || '').trim();

  if (!char) {
    showToast('Vui lòng nhập ký tự chữ Hán.', 'error');
    charInput.focus();
    return;
  }

  activeDeck.cards = activeDeck.cards || [];
  activeDeck.cards.push({
    char: char,
    hanViet: hanViet || 'HÁN TỰ',
    meaning: meaning || 'Chưa cập nhật nghĩa',
    onyomi: '—',
    kunyomi: '—',
    jlpt: 'Tự do'
  });

  saveLocalDecks();
  closeAddCardModal();
  currentCardIndex = activeDeck.cards.length - 1;
  renderCurrentCard();
  renderMiniGrid();
  showToast(`Đã thêm chữ [${char}] vào bộ thẻ thành công.`, 'success');
}

// =========================================================================
// 7. CHỨC NĂNG MỞ RỘNG (IN THẺ, NHÂN BẢN, GỘP BỘ THẺ)
// =========================================================================

// 1. In bộ thẻ (Print Mode A4 / Xuất PDF)
function handlePrintDeck(deckId) {
  const deck = userDecks.find(d => d.id === deckId);
  if (!deck) return;
  const cards = deck.cards || [];
  if (cards.length === 0) {
    showToast('Bộ thẻ này chưa có thẻ học nào để in!', 'warning');
    return;
  }

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    showToast('Vui lòng cho phép popup trên trình duyệt để mở giao diện in ấn.', 'error');
    return;
  }

  const printHtml = `
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <title>In Bộ Thẻ - ${escapeHtml(deck.title)}</title>
  <style>
    @page {
      size: A4;
      margin: 12mm;
    }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      margin: 0;
      padding: 0;
      color: #0F172A;
      background: #FFF;
    }
    .toolbar {
      padding: 10px 14px;
      background: #F1F5F9;
      margin-bottom: 16px;
      border-radius: 6px;
      display: flex;
      gap: 10px;
    }
    .btn {
      padding: 7px 16px;
      background: #006194;
      color: white;
      border: 1px solid #0F172A;
      border-radius: 4px;
      font-weight: bold;
      font-size: 12px;
      cursor: pointer;
    }
    .print-header {
      border-bottom: 2px solid #0F172A;
      padding-bottom: 8px;
      margin-bottom: 16px;
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
    }
    .print-header h1 {
      margin: 0;
      font-size: 18px;
      font-weight: 800;
    }
    .print-header p {
      margin: 4px 0 0;
      font-size: 12px;
      color: #64748B;
    }
    .print-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
    }
    .print-card {
      border: 1.5px dashed #0F172A;
      border-radius: 6px;
      padding: 12px 14px;
      page-break-inside: avoid;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      min-height: 110px;
      background: #FAFAFA;
      box-sizing: border-box;
    }
    .card-front {
      font-size: 16px;
      font-weight: bold;
      color: #0F2545;
      margin-bottom: 6px;
      border-bottom: 1px solid #E2E8F0;
      padding-bottom: 6px;
    }
    .card-back {
      font-size: 12px;
      color: #334155;
      line-height: 1.4;
    }
    .card-footer {
      display: flex;
      justify-content: space-between;
      font-size: 10px;
      color: #94A3B8;
      margin-top: 8px;
      padding-top: 4px;
    }
    @media print {
      .no-print { display: none !important; }
      body { background: white; }
    }
  </style>
</head>
<body>
  <div class="toolbar no-print">
    <button class="btn" onclick="window.print()">🖨️ In Ngay (Print / Lưu PDF)</button>
    <button class="btn" style="background:#64748B;" onclick="window.close()">Đóng Cửa Sổ</button>
  </div>
  <div class="print-header">
    <div>
      <h1>🌸 ${escapeHtml(deck.title)}</h1>
      <p>${escapeHtml(deck.description || 'Bộ thẻ Kizuna Flashcards')} • Tổng số: ${cards.length} thẻ</p>
    </div>
    <div style="font-size: 11px; font-weight: bold; color: #64748B;">KizunaTutor Flashcards</div>
  </div>
  <div class="print-grid">
    ${cards.map((c, i) => `
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

// 2. Nhân bản bộ thẻ (Duplicate Deck)
function handleDuplicateDeck(deckId) {
  const target = userDecks.find(d => d.id === deckId);
  if (!target) return;

  if (!confirm(`Bạn có muốn nhân bản bộ thẻ "${target.title}" kèm toàn bộ thẻ con không?`)) return;

  const duplicatedDeck = {
    id: `deck-${Date.now()}`,
    title: `${target.title} (Bản sao)`,
    description: target.description || '',
    color: target.color || 'blue',
    cards: (target.cards || []).map(c => ({ ...c }))
  };

  userDecks.unshift(duplicatedDeck);
  saveLocalDecks();
  renderDecks();
  showToast(`Đã nhân bản bộ thẻ "${duplicatedDeck.title}" thành công!`, 'success');
}

// 3. Gộp bộ thẻ (Merge Decks)
let sourceMergeDeckId = null;

function openMergeModal(deckId) {
  const sourceDeck = userDecks.find(d => d.id === deckId);
  if (!sourceDeck) return;

  const otherDecks = userDecks.filter(d => d.id !== deckId);
  if (otherDecks.length === 0) {
    showToast('Bạn cần có ít nhất 2 bộ thẻ để có thể thực hiện gộp thẻ!', 'warning');
    return;
  }

  sourceMergeDeckId = deckId;
  document.getElementById('mergeSourceDeckTitle').textContent = sourceDeck.title;
  document.getElementById('mergeSourceDeckCount').textContent = `${(sourceDeck.cards || []).length} thẻ học`;

  const select = document.getElementById('mergeTargetDeckSelect');
  select.innerHTML = otherDecks.map(d => `
    <option value="${d.id}">${escapeHtml(d.title)} (${(d.cards || []).length} thẻ)</option>
  `).join('');

  document.getElementById('checkDeleteSourceAfterMerge').checked = false;
  document.getElementById('mergeDeckModal').classList.remove('hidden');
}

function closeMergeModal() {
  document.getElementById('mergeDeckModal').classList.add('hidden');
  sourceMergeDeckId = null;
}

function executeMergeDecks() {
  if (!sourceMergeDeckId) return;
  const select = document.getElementById('mergeTargetDeckSelect');
  const targetDeckId = select.value;
  const sourceDeck = userDecks.find(d => d.id === sourceMergeDeckId);
  const targetDeck = userDecks.find(d => d.id === targetDeckId);

  if (!sourceDeck || !targetDeck) return;

  const shouldDeleteSource = document.getElementById('checkDeleteSourceAfterMerge').checked;
  const sourceCards = sourceDeck.cards || [];

  if (sourceCards.length === 0) {
    showToast('Bộ thẻ nguồn không có thẻ nào để gộp!', 'warning');
    closeMergeModal();
    return;
  }

  // Chuyển thẻ sang bộ đích
  targetDeck.cards = (targetDeck.cards || []).concat(sourceCards.map(c => ({ ...c })));

  if (shouldDeleteSource) {
    userDecks = userDecks.filter(d => d.id !== sourceMergeDeckId);
  }

  saveLocalDecks();
  renderDecks();
  closeMergeModal();
  showToast(`Đã gộp ${sourceCards.length} thẻ sang bộ "${targetDeck.title}" thành công!`, 'success');
}

// =========================================================================
// 8. MODAL CHỈNH SỬA BỘ THẺ & IMPORT EXCEL 2 CỘT
// =========================================================================
let activeEditingDeck = null;
let editingCards = [];

function openEditDeckModal(deckId) {
  const deck = userDecks.find(d => d.id === deckId);
  if (!deck) return;

  activeEditingDeck = deck;
  editingCards = (deck.cards || []).map(c => ({ ...c }));

  document.getElementById('editDeckNameInput').value = deck.title || '';
  document.getElementById('editDeckDescInput').value = deck.description || '';
  document.getElementById('quickAddFrontInput').value = '';
  document.getElementById('quickAddBackInput').value = '';

  renderEditingCardsList();
  document.getElementById('editDeckModal').classList.remove('hidden');
}

function closeEditDeckModal() {
  document.getElementById('editDeckModal').classList.add('hidden');
  activeEditingDeck = null;
  editingCards = [];
}

function renderEditingCardsList() {
  const container = document.getElementById('deckCardsListContainer');
  const countBadge = document.getElementById('deckCardCountBadge');
  if (!container) return;

  if (countBadge) {
    countBadge.textContent = `${editingCards.length} thẻ`;
  }

  if (editingCards.length === 0) {
    container.innerHTML = `
      <div class="p-6 text-center text-slate-400 text-xs border border-dashed border-slate-300 rounded-[4px] bg-slate-50">
        Chưa có thẻ nào trong bộ. Hãy thêm từ file Excel hoặc nhập nhanh ở trên nhé!
      </div>
    `;
    return;
  }

  container.innerHTML = editingCards.map((c, idx) => `
    <div class="flex items-center justify-between gap-2 p-2 bg-white border border-slate-200 rounded-[4px] text-xs hover:border-[#0F172A] transition">
      <div class="flex-1 min-w-0 grid grid-cols-1 sm:grid-cols-2 gap-1.5">
        <div class="font-bold text-slate-900 truncate font-japanese" title="${escapeHtml(c.char)}">
          ${escapeHtml(c.char)}
        </div>
        <div class="text-slate-600 truncate" title="${escapeHtml(c.meaning || c.hanViet || '')}">
          ${escapeHtml(c.meaning || c.hanViet || '—')}
        </div>
      </div>
      <button type="button" data-action="remove-editing-card" data-index="${idx}"
              class="w-6 h-6 flex items-center justify-center rounded border border-rose-200 text-rose-600 hover:bg-rose-50 cursor-pointer shrink-0 font-bold"
              title="Xóa thẻ này">
        ✕
      </button>
    </div>
  `).join('');

  container.querySelectorAll('[data-action="remove-editing-card"]').forEach(btn => {
    btn.addEventListener('click', () => {
      const idx = parseInt(btn.dataset.index, 10);
      editingCards.splice(idx, 1);
      renderEditingCardsList();
    });
  });
}

function quickAddCardToEditList() {
  const frontInput = document.getElementById('quickAddFrontInput');
  const backInput = document.getElementById('quickAddBackInput');
  const front = (frontInput.value || '').trim();
  const back = (backInput.value || '').trim();

  if (!front) {
    showToast('Vui lòng nhập nội dung mặt trước (câu hỏi/thuật ngữ).', 'error');
    frontInput.focus();
    return;
  }

  editingCards.push({
    char: front,
    hanViet: '',
    meaning: back || '—',
    onyomi: '—',
    kunyomi: '—',
    jlpt: 'Tự do'
  });

  frontInput.value = '';
  backInput.value = '';
  renderEditingCardsList();
  showToast(`Đã thêm thẻ [${front}] vào danh sách.`, 'success');
}

/**
 * Đọc file Excel 2 cột (Cột 1: Câu hỏi, Cột 2: Đáp án)
 */
function handleExcelUpload(event) {
  const file = event.target.files[0];
  if (!file) return;

  if (typeof XLSX === 'undefined') {
    showToast('Thư viện Excel đang được tải, vui lòng kiểm tra kết nối mạng và thử lại!', 'error');
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
      
      // Chuyển sheet thành mảng 2 chiều
      const rawRows = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
      const newCards = [];

      rawRows.forEach((row, index) => {
        if (!row || row.length < 2) return;
        const col1 = String(row[0] || '').trim();
        const col2 = String(row[1] || '').trim();

        // Bỏ qua dòng tiêu đề nếu chứa chữ câu hỏi, mặt trước, question, front, thuật ngữ
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

      editingCards = editingCards.concat(newCards);
      renderEditingCardsList();
      showToast(`Đã đọc thành công ${newCards.length} thẻ từ file Excel!`, 'success');
    } catch (err) {
      console.error(err);
      showToast('Lỗi khi đọc file Excel. Vui lòng kiểm tra lại định dạng file!', 'error');
    } finally {
      event.target.value = '';
    }
  };
  reader.readAsArrayBuffer(file);
}

function downloadSampleExcel() {
  if (typeof XLSX === 'undefined') {
    showToast('Thư viện Excel chưa sẵn sàng, vui lòng thử lại sau giây lát!', 'warning');
    return;
  }
  const sampleData = [
    ["Câu hỏi (Mặt trước)", "Đáp án (Mặt sau)"],
    ["桜 (さくら)", "Hoa anh đào - Biểu tượng của mùa xuân Nhật Bản"],
    ["勉強 (べんきょう)", "Học tập, rèn luyện kiến thức"],
    ["先生 (せんせい)", "Thầy cô giáo, giáo viên, bác sĩ"],
    ["愛 (あい)", "Tình yêu thương, ái tình, trân quý"],
    ["ありがとうございます", "Cảm ơn bạn rất nhiều (lịch sự)"],
    ["こんにちは", "Xin chào (lời chào ban ngày)"]
  ];
  const ws = XLSX.utils.aoa_to_sheet(sampleData);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, "Flashcards");
  XLSX.writeFile(wb, "Kizuna_Flashcard_Mau.xlsx");
  showToast('Đã tải xuống file Excel mẫu 2 cột thành công!', 'success');
}

function saveDeckChanges() {
  if (!activeEditingDeck) return;
  const nameInput = document.getElementById('editDeckNameInput');
  const descInput = document.getElementById('editDeckDescInput');
  const title = (nameInput.value || '').trim();
  const desc = (descInput.value || '').trim();

  if (!title) {
    showToast('Vui lòng nhập tên bộ thẻ nhé.', 'error');
    nameInput.focus();
    return;
  }

  activeEditingDeck.title = title;
  activeEditingDeck.description = desc;
  activeEditingDeck.cards = [...editingCards];

  saveLocalDecks();
  renderDecks();
  closeEditDeckModal();
  showToast(`Đã lưu thay đổi bộ thẻ "${title}" thành công!`, 'success');
}

// =========================================================================
// 9. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadDecks();

  const searchInput = document.getElementById('inputSearchDecks');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderDecks(e.target.value);
    }, 250));
  }

  document.getElementById('btnOpenCreateDeck')?.addEventListener('click', openCreateDeckModal);
  document.getElementById('btnEmptyCreateDeck')?.addEventListener('click', openCreateDeckModal);
  document.getElementById('btnCloseCreateDeckModal')?.addEventListener('click', closeCreateDeckModal);
  document.getElementById('btnCancelDeck')?.addEventListener('click', closeCreateDeckModal);
  document.getElementById('btnSaveDeck')?.addEventListener('click', saveNewDeck);

  document.getElementById('flashcardElement')?.addEventListener('click', flipCurrentCard);
  document.getElementById('btnFlipCard')?.addEventListener('click', flipCurrentCard);
  document.getElementById('btnNextCard')?.addEventListener('click', nextCard);
  document.getElementById('btnPrevCard')?.addEventListener('click', prevCard);
  document.getElementById('btnCloseStudyModal')?.addEventListener('click', closeStudyModal);

  document.getElementById('btnOpenAddCardModal')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnCloseAddCardModal')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnCancelAddCard')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnSubmitAddCard')?.addEventListener('click', submitNewCard);

  // Choose Study Mode Modal Events
  document.getElementById('btnCloseChooseStudyModeModal')?.addEventListener('click', closeChooseStudyModeModal);
  document.getElementById('btnCancelChooseStudyMode')?.addEventListener('click', closeChooseStudyModeModal);
  document.getElementById('btnSelectModeFlashcard')?.addEventListener('click', () => {
    const deckId = selectedStudyDeckId;
    closeChooseStudyModeModal();
    if (deckId) openStudyModal(deckId);
  });
  document.getElementById('btnSelectModeKanjiGame')?.addEventListener('click', () => {
    const deckId = selectedStudyDeckId;
    closeChooseStudyModeModal();
    if (deckId) {
      window.location.href = `/kanji-game?deckId=${encodeURIComponent(deckId)}`;
    }
  });

  // Edit Deck Modal Events
  document.getElementById('btnCloseEditDeckModal')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnCancelEditDeck')?.addEventListener('click', closeEditDeckModal);
  document.getElementById('btnSaveDeckChanges')?.addEventListener('click', saveDeckChanges);
  document.getElementById('btnTriggerExcelUpload')?.addEventListener('click', () => {
    document.getElementById('excelFileInput')?.click();
  });
  document.getElementById('excelFileInput')?.addEventListener('change', handleExcelUpload);
  document.getElementById('btnDownloadSampleExcel')?.addEventListener('click', downloadSampleExcel);
  document.getElementById('btnQuickAddCard')?.addEventListener('click', quickAddCardToEditList);

  // Merge Modal Events
  document.getElementById('btnCloseMergeModal')?.addEventListener('click', closeMergeModal);
  document.getElementById('btnCancelMerge')?.addEventListener('click', closeMergeModal);
  document.getElementById('btnConfirmMerge')?.addEventListener('click', executeMergeDecks);

  // Global click to close dropdown menus
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.dropdown-container')) {
      closeAllMenus();
    }
  });
});
