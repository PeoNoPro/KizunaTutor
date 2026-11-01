/**
 * KizunaTutor - Quản lý & Luyện tập Bộ thẻ ghi nhớ Flashcards
 * Feature-Based architecture module, tuân thủ FRONTEND_RULES.md
 */

import { showToast, debounce } from '/js/main.js';

// =========================================================================
// 1. DỮ LIỆU KHỞI TẠO BAN ĐẦU (SEED DATA CHO HỌC VIÊN & KHÁCH)
// =========================================================================
const DEFAULT_SAMPLE_DECKS = [
  {
    id: 'deck-n5-essential',
    title: 'Hán tự N5 cơ bản thiết yếu',
    description: 'Tập hợp các chữ Hán căn bản đầu đời tiếng Nhật (Nhật, Bổn, Nhân, Học...)',
    color: 'blue',
    cards: [
      { char: '日', hanViet: 'NHẬT', meaning: 'Mặt trời, ngày', onyomi: 'ニチ, ジツ', kunyomi: 'ひ, -び', jlpt: 'N5' },
      { char: '本', hanViet: 'BỔN / BẢN', meaning: 'Sách, gốc rễ', onyomi: 'ホン', kunyomi: 'もと', jlpt: 'N5' },
      { char: '人', hanViet: 'NHÂN', meaning: 'Con người, nhân loại', onyomi: 'ジン, ニン', kunyomi: 'ひと', jlpt: 'N5' },
      { char: '学', hanViet: 'HỌC', meaning: 'Học tập, nghiên cứu', onyomi: 'ガク', kunyomi: 'まな.ぶ', jlpt: 'N5' },
      { char: '先', hanViet: 'TIÊN', meaning: 'Trước, đi trước', onyomi: 'セン', kunyomi: 'さき', jlpt: 'N5' },
      { char: '生', hanViet: 'SINH', meaning: 'Sinh sống, sinh ra', onyomi: 'セイ, ショウ', kunyomi: 'い.きる, う.まれる', jlpt: 'N5' }
    ]
  },
  {
    id: 'deck-n4-nature',
    title: 'Hán tự N4 chuyên đề Thiên nhiên',
    description: 'Các chữ Hán liên quan đến sông núi, thời tiết, vạn vật tự nhiên',
    color: 'green',
    cards: [
      { char: '山', hanViet: 'SƠN / SAN', meaning: 'Núi non', onyomi: 'サン', kunyomi: 'やま', jlpt: 'N5' },
      { char: '川', hanViet: 'XUYÊN', meaning: 'Dòng sông', onyomi: 'セン', kunyomi: 'かわ', jlpt: 'N5' },
      { char: '雨', hanViet: 'VŨ', meaning: 'Cơn mưa', onyomi: 'ウ', kunyomi: 'あめ', jlpt: 'N5' },
      { char: '空', hanViet: 'KHÔNG', meaning: 'Bầu trời, khoảng trống', onyomi: 'クウ', kunyomi: 'そら, あ.く', jlpt: 'N4' },
      { char: '海', hanViet: 'HẢI', meaning: 'Biển cả', onyomi: 'カイ', kunyomi: 'うみ', jlpt: 'N4' }
    ]
  },
  {
    id: 'deck-kizuna-shodo',
    title: 'Hán tự Thư pháp & Triết lý Kizuna',
    description: 'Các chữ Hán mang triết lý nhân sinh sâu sắc và vẻ đẹp thư pháp',
    color: 'pink',
    cards: [
      { char: '絆', hanViet: 'BÁN', meaning: 'Sợi dây gắn kết bền vững (Kizuna)', onyomi: 'ハン', kunyomi: 'きずな', jlpt: 'N1' },
      { char: '愛', hanViet: 'ÁI', meaning: 'Tình yêu thương, quý trọng', onyomi: 'アイ', kunyomi: 'いと.しい', jlpt: 'N3' },
      { char: '心', hanViet: 'TÂM', meaning: 'Trái tim, tấm lòng', onyomi: 'シン', kunyomi: 'こころ', jlpt: 'N4' },
      { char: '道', hanViet: 'ĐẠO', meaning: 'Con đường, đạo lý', onyomi: 'ドウ', kunyomi: 'みち', jlpt: 'N4' },
      { char: '和', hanViet: 'HÒA', meaning: 'Hòa thuận, phong cách Nhật Bản', onyomi: 'ワ', kunyomi: 'やわ.らぐ', jlpt: 'N3' }
    ]
  }
];

// =========================================================================
// 2. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
const STORAGE_KEY = 'kizuna_flashcard_decks';
let userDecks = [];
let activeDeck = null;
let currentCardIndex = 0;
let isFlipped = false;
let selectedColor = 'blue';

// =========================================================================
// 3. QUẢN LÝ LƯU TRỮ (LOCAL STORAGE & REST API SYNC)
// =========================================================================
function loadDecks() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      userDecks = JSON.parse(raw);
    } else {
      userDecks = [...DEFAULT_SAMPLE_DECKS];
      saveDecks();
    }
  } catch (e) {
    userDecks = [...DEFAULT_SAMPLE_DECKS];
  }
}

function saveDecks() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(userDecks));
  } catch (e) {
    console.debug('Không thể ghi localStorage:', e);
  }
}

// =========================================================================
// 4. RENDER DANH SÁCH BỘ THẺ
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

  if (filtered.length === 0) {
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

    // Lấy 4 chữ Hán tiêu biểu làm preview
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
              ${deck.description || 'Chưa có mô tả cho bộ thẻ này.'}
            </p>
          </div>

          <!-- Dải xem trước các chữ Hán -->
          <div class="p-3 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl flex items-center justify-between">
            <span class="text-xs text-[#68737D] font-medium">Chữ tiêu biểu:</span>
            <span class="font-japanese font-bold text-base text-[#0F2545] tracking-widest">
              ${previewChars || '—'}
            </span>
          </div>
        </div>

        <div class="pt-4 mt-4 border-t border-[#ECE8DE] flex items-center justify-between gap-2">
          <button type="button" data-action="open-deck" data-id="${deck.id}"
                  class="flex-1 py-2 px-3 rounded-xl bg-[#006194] hover:bg-[#004B73] text-white text-xs font-bold flex items-center justify-center gap-1.5 transition cursor-pointer shadow-xs">
            <span class="material-symbols-outlined text-[16px]">style</span>
            <span>Lật thẻ ôn tập</span>
          </button>
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;

  // Gắn sự kiện cho các nút hành động
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
// 5. CHẾ ĐỘ LẬT THẺ FLASHCARD 3D & ÔN TẬP
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
  if (!activeDeck || !activeDeck.cards || activeDeck.cards.length === 0) {
    document.getElementById('studyCardChar').textContent = '📭';
    document.getElementById('studyCardProgressText').textContent = 'Chưa có thẻ';
    document.getElementById('studyCardHanViet').textContent = 'TRỐNG';
    document.getElementById('studyCardMeaning').textContent = 'Hãy nhấn nút "Thêm chữ" ở trên để nạp chữ vào bộ thẻ nhé.';
    document.getElementById('studyCardOnyomi').textContent = '—';
    document.getElementById('studyCardKunyomi').textContent = '—';
    document.getElementById('btnStudyPracticeShodo').classList.add('hidden');
    return;
  }

  document.getElementById('btnStudyPracticeShodo').classList.remove('hidden');

  // Đảm bảo không lật mặt sau khi chuyển thẻ mới
  const cardEl = document.getElementById('flashcardElement');
  cardEl.classList.remove('is-flipped');
  isFlipped = false;

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

  // Nút Luyện viết chữ này -> chuyển sang Shodo practice
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
    currentCardIndex = 0; // Vòng lại thẻ đầu tiên
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
  saveDecks();
  renderCurrentCard();
  renderMiniGrid();
  showToast(`Đã xóa chữ [${removed[0]?.char}] khỏi bộ thẻ.`, 'info');
}

// =========================================================================
// 6. TẠO & XÓA BỘ THẺ (CRUD LOGIC)
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
  saveDecks();
  closeCreateDeckModal();
  renderDecks();
  showToast(`Đã tạo thành công bộ thẻ "${title}".`, 'success');
}

function deleteDeck(deckId) {
  const target = userDecks.find(d => d.id === deckId);
  if (!target) return;

  if (confirm(`Bạn có chắc chắn muốn xóa bộ thẻ "${target.title}" không?`)) {
    userDecks = userDecks.filter(d => d.id !== deckId);
    saveDecks();
    renderDecks();
    showToast(`Đã xóa bộ thẻ "${target.title}".`, 'info');
  }
}

// =========================================================================
// 7. THÊM CHỮ MỚI VÀO BỘ THẺ
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

  saveDecks();
  closeAddCardModal();
  currentCardIndex = activeDeck.cards.length - 1;
  renderCurrentCard();
  renderMiniGrid();
  showToast(`Đã thêm chữ [${char}] vào bộ thẻ thành công.`, 'success');
}

// =========================================================================
// 8. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadDecks();
  renderDecks();

  // Tìm kiếm bộ thẻ
  const searchInput = document.getElementById('inputSearchDecks');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      renderDecks(e.target.value);
    }, 250));
  }

  // Mở / Đóng Modal tạo bộ thẻ
  document.getElementById('btnOpenCreateDeck')?.addEventListener('click', openCreateDeckModal);
  document.getElementById('btnEmptyCreateDeck')?.addEventListener('click', openCreateDeckModal);
  document.getElementById('btnCloseCreateDeckModal')?.addEventListener('click', closeCreateDeckModal);
  document.getElementById('btnCancelDeck')?.addEventListener('click', closeCreateDeckModal);
  document.getElementById('btnSaveDeck')?.addEventListener('click', saveNewDeck);

  // Chọn màu bộ thẻ
  document.querySelectorAll('#deckColorPicker button').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedColor = btn.dataset.color;
      highlightSelectedColor();
    });
  });

  // Chế độ lật thẻ Flashcard
  document.getElementById('flashcardElement')?.addEventListener('click', flipCurrentCard);
  document.getElementById('btnFlipCard')?.addEventListener('click', flipCurrentCard);
  document.getElementById('btnNextCard')?.addEventListener('click', nextCard);
  document.getElementById('btnPrevCard')?.addEventListener('click', prevCard);
  document.getElementById('btnCloseStudyModal')?.addEventListener('click', closeStudyModal);

  // Thêm chữ vào bộ thẻ
  document.getElementById('btnOpenAddCardModal')?.addEventListener('click', openAddCardModal);
  document.getElementById('btnCloseAddCardModal')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnCancelAddCard')?.addEventListener('click', closeAddCardModal);
  document.getElementById('btnSubmitAddCard')?.addEventListener('click', submitNewCard);
});
