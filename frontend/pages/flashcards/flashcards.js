/**
 * KizunaTutor - Quản lý & Luyện tập Bộ thẻ ghi nhớ Flashcards
 * Feature-Based architecture module, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast, debounce } from '/js/main.js';

// =========================================================================
// 1. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
const STORAGE_KEY = 'kizuna_flashcard_decks';
let userDecks = [];
let activeDeck = null;
let currentCardIndex = 0;
let isFlipped = false;
let selectedColor = 'blue';

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

  const colorBorderMap = {
    blue: 'border-[#006194]',
    pink: 'border-[#831843]',
    green: 'border-[#059669]',
    amber: 'border-[#D97706]',
    indigo: 'border-[#4059AA]'
  };

  const colorBgPillMap = {
    blue: 'bg-sky-50 text-[#006194] border-sky-200',
    pink: 'bg-[#FFE5EC] text-[#831843] border-[#FBCFE8]',
    green: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    amber: 'bg-amber-50 text-amber-700 border-amber-200',
    indigo: 'bg-indigo-50 text-indigo-700 border-indigo-200'
  };

  let html = '';

  filtered.forEach(deck => {
    const cardCount = deck.cards ? deck.cards.length : 0;
    const borderClass = colorBorderMap[deck.color] || 'border-[#006194]';
    const pillClass = colorBgPillMap[deck.color] || 'bg-sky-50 text-[#006194] border-sky-200';
    const previewChars = (deck.cards || []).slice(0, 4).map(c => c.char).join(' ');

    html += `
      <div class="bg-white border border-[#ECE8DE] rounded-2xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between border-t-4 ${borderClass} group">
        <div class="space-y-3">
          <div class="flex items-start justify-between gap-2">
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border ${pillClass}">
              ${cardCount} chữ Hán
            </span>
            <div class="flex items-center gap-1 opacity-70 group-hover:opacity-100 transition">
              <button type="button" data-action="delete-deck" data-id="${deck.id}" 
                      class="text-[#68737D] hover:text-rose-600 p-1 rounded-lg" title="Xóa bộ thẻ này">
                <span class="material-symbols-outlined text-[18px]">delete</span>
              </button>
            </div>
          </div>

          <div>
            <h3 class="font-bold text-base md:text-lg text-[#0F2545] group-hover:text-[#006194] transition">
              ${deck.title}
            </h3>
            <p class="text-xs text-[#68737D] mt-1 line-clamp-2">
              ${deck.description || 'Chưa có ghi chú mô tả cho bộ thẻ này.'}
            </p>
          </div>

          <div class="p-3 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl flex items-center justify-between">
            <span class="text-xs text-[#68737D] font-medium">Chữ trong bộ:</span>
            <span class="font-japanese font-bold text-base text-[#0F2545] tracking-widest">
              ${previewChars || '(Chưa có chữ nào)'}
            </span>
          </div>
        </div>

        <div class="pt-4 mt-4 border-t border-[#ECE8DE] flex items-center justify-between gap-2">
          <button type="button" data-action="open-deck" data-id="${deck.id}"
                  class="flex-1 py-2 px-3 rounded-xl bg-[#006194] hover:bg-[#004B73] text-white text-xs font-bold flex items-center justify-center gap-1.5 transition cursor-pointer shadow-xs">
            <span class="material-symbols-outlined text-[16px]">style</span>
            <span>Xem &amp; Lật thẻ</span>
          </button>
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;

  grid.querySelectorAll('[data-action="open-deck"]').forEach(btn => {
    btn.addEventListener('click', () => openStudyModal(btn.dataset.id));
  });

  grid.querySelectorAll('[data-action="delete-deck"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      deleteDeck(btn.dataset.id);
    });
  });
}

// =========================================================================
// 4. CHẾ ĐỘ LẬT THẺ FLASHCARD 3D
// =========================================================================
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
  selectedColor = 'blue';
  highlightSelectedColor();
}

function closeCreateDeckModal() {
  document.getElementById('modalCreateDeck').classList.add('hidden');
}

function highlightSelectedColor() {
  document.querySelectorAll('#deckColorPicker button').forEach(btn => {
    if (btn.dataset.color === selectedColor) {
      btn.classList.add('ring-2', 'ring-offset-2', 'ring-[#006194]');
    } else {
      btn.classList.remove('ring-2', 'ring-offset-2', 'ring-[#006194]');
    }
  });
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
    color: selectedColor,
    cards: []
  };

  userDecks.unshift(newDeck);
  saveLocalDecks();
  closeCreateDeckModal();
  renderDecks();
  showToast(`Đã tạo thành công bộ thẻ "${title}".`, 'success');
}

function deleteDeck(deckId) {
  const target = userDecks.find(d => d.id === deckId);
  if (!target) return;

  if (confirm(`Bạn có chắc chắn muốn xóa bộ thẻ "${target.title}" không?`)) {
    userDecks = userDecks.filter(d => d.id !== deckId);
    saveLocalDecks();
    renderDecks();
    showToast(`Đã xóa bộ thẻ "${target.title}".`, 'info');
  }
}

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
// 7. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
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

  document.querySelectorAll('#deckColorPicker button').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedColor = btn.dataset.color;
      highlightSelectedColor();
    });
  });

  document.getElementById('flashcardElement')?.addEventListener('click', flipCurrentCard);
  document.getElementById('btnFlipCard')?.addEventListener('click', flipCurrentCard);
  document.getElementById('btnNextCard')?.addEventListener('click', nextCard);
  document.getElementById('btnPrevCard')?.addEventListener('click', prevCard);
  document.getElementById('btnCloseStudyModal')?.addEventListener('click', closeStudyModal);

  document.getElementById('btnOpenAddCardModal')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnCloseAddCardModal')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnCancelAddCard')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnSubmitAddCard')?.addEventListener('click', submitNewCard);
});
