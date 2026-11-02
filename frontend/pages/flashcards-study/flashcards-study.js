/**
 * KizunaTutor - Trang Luyện Tập Flashcard 3D Độc Lập
 * URL Route: /flashcards-study?id={deckId}
 */

import { showToast } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khởi tạo Shell học viên
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Luyện Tập Flashcard 3D',
  badge: 'Bộ nhớ siêu tốc'
});

const STORAGE_KEY = 'kizuna_flashcard_decks';
let currentDeckId = null;
let currentDeck = null;
let originalCards = [];
let cards = [];
let currentIndex = 0;
let isFlipped = false;

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
function speakJapanese(text) {
  if (!text) return;
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
async function loadStudyDeck() {
  const urlParams = new URLSearchParams(window.location.search);
  currentDeckId = urlParams.get('id') || urlParams.get('deckId');

  if (!currentDeckId) {
    const parts = window.location.pathname.split('/').filter(Boolean);
    if (parts.length > 0 && parts[parts.length - 1] !== 'flashcards-study') {
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
            jlpt: c.jlptLevel || c.jlpt || 'Flashcard'
          }))
        };
      }
    } catch (e) {
      console.debug('Lỗi API backend:', e);
    }
  }

  if (!currentDeck) {
    showToast('Không tìm thấy dữ liệu bộ thẻ tương ứng!', 'error');
    setTimeout(() => { window.location.href = '/flashcards'; }, 1500);
    return;
  }

  // Cập nhật tiêu đề bộ thẻ
  document.getElementById('studyDeckTitle').textContent = currentDeck.title || 'Bộ Thẻ';
  document.title = `Luyện Tập: ${currentDeck.title} - KizunaTutor`;

  originalCards = Array.isArray(currentDeck.cards) ? [...currentDeck.cards] : [];
  cards = [...originalCards];

  if (cards.length === 0) {
    document.getElementById('emptyDeckState').classList.remove('hidden');
    document.getElementById('studyAreaContainer').classList.add('hidden');
    document.getElementById('studyProgressBadge').textContent = 'Thẻ 0 / 0';
    document.getElementById('studyProgressBar').style.width = '0%';
    return;
  }

  document.getElementById('emptyDeckState').classList.add('hidden');
  document.getElementById('studyAreaContainer').classList.remove('hidden');

  currentIndex = 0;
  isFlipped = false;
  renderThumbnails();
  renderCard();
}

// 5. Hiển thị thẻ hiện tại
function renderCard() {
  if (cards.length === 0) return;
  if (currentIndex < 0) currentIndex = 0;
  if (currentIndex >= cards.length) currentIndex = cards.length - 1;

  const card = cards[currentIndex];
  const total = cards.length;

  // Trả về mặt trước khi đổi thẻ
  isFlipped = false;
  const cardElement = document.getElementById('flashcardElement');
  if (cardElement) {
    cardElement.classList.remove('is-flipped');
  }

  // Tiến độ
  document.getElementById('studyProgressBadge').textContent = `Thẻ ${currentIndex + 1} / ${total}`;
  const pct = Math.round(((currentIndex + 1) / total) * 100);
  document.getElementById('studyProgressBar').style.width = `${pct}%`;

  // Mặt trước
  document.getElementById('cardLevelBadgeFront').textContent = card.jlpt || 'Flashcard';
  document.getElementById('cardCharFront').textContent = card.char || '—';
  const hvFront = document.getElementById('cardHanVietFront');
  if (hvFront) {
    hvFront.textContent = card.hanViet ? card.hanViet.toUpperCase() : '';
  }

  // Mặt sau
  document.getElementById('cardLevelBadgeBack').textContent = card.jlpt || 'Đáp Án';
  document.getElementById('cardCharBack').textContent = card.char || '—';
  const hvBack = document.getElementById('cardHanVietBack');
  if (hvBack) {
    if (card.hanViet) {
      hvBack.textContent = card.hanViet.toUpperCase();
      hvBack.classList.remove('hidden');
    } else {
      hvBack.classList.add('hidden');
    }
  }

  document.getElementById('cardMeaningBack').textContent = card.meaning || '—';

  // Âm On/Kun (nếu có)
  const readingsContainer = document.getElementById('cardReadingsContainer');
  const onEl = document.getElementById('cardOnyomi');
  const kunEl = document.getElementById('cardKunyomi');
  const hasOnKun = (card.onyomi && card.onyomi !== '—') || (card.kunyomi && card.kunyomi !== '—');

  if (hasOnKun && readingsContainer) {
    readingsContainer.classList.remove('hidden');
    if (onEl) onEl.textContent = `On: ${card.onyomi || '—'}`;
    if (kunEl) kunEl.textContent = `Kun: ${card.kunyomi || '—'}`;
  } else if (readingsContainer) {
    readingsContainer.classList.add('hidden');
  }

  // Nút Shodo Canvas nếu là chữ đơn (Kanji)
  const isKanji = typeof card.char === 'string' && card.char.length === 1 && /[\u4e00-\u9faf]/.test(card.char);
  const shodoContainer = document.getElementById('shodoLinkContainer');
  const shodoBtn = document.getElementById('btnShodoPractice');
  if (isKanji && shodoContainer && shodoBtn) {
    shodoContainer.classList.remove('hidden');
    shodoBtn.href = `/shodo?kanji=${encodeURIComponent(card.char)}`;
  } else if (shodoContainer) {
    shodoContainer.classList.add('hidden');
  }

  // Đánh dấu thumbnail đang chọn
  highlightActiveThumbnail();
}

// 6. Lật thẻ 3D
function flipCard() {
  const cardElement = document.getElementById('flashcardElement');
  if (!cardElement) return;
  isFlipped = !isFlipped;
  cardElement.classList.toggle('is-flipped', isFlipped);
}

// 7. Thẻ tiếp theo
function nextCard() {
  if (cards.length === 0) return;
  if (currentIndex < cards.length - 1) {
    currentIndex++;
  } else {
    currentIndex = 0;
    showToast('Đã xem hết bộ thẻ! Quay lại từ thẻ đầu tiên.', 'info');
  }
  renderCard();
}

// 8. Thẻ trước đó
function prevCard() {
  if (cards.length === 0) return;
  if (currentIndex > 0) {
    currentIndex--;
  } else {
    currentIndex = cards.length - 1;
  }
  renderCard();
}

// 9. Xáo trộn thứ tự thẻ (Fisher-Yates)
function shuffleCards() {
  if (cards.length <= 1) return;
  for (let i = cards.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [cards[i], cards[j]] = [cards[j], cards[i]];
  }
  currentIndex = 0;
  renderThumbnails();
  renderCard();
  showToast('Đã xáo trộn thứ tự các thẻ trong bộ!', 'success');
}

// 10. Bắt đầu lại
function resetProgress() {
  cards = [...originalCards];
  currentIndex = 0;
  renderThumbnails();
  renderCard();
  showToast('Đã bắt đầu lại từ thẻ đầu tiên!', 'info');
}

// 11. Render thanh thumbnails
function renderThumbnails() {
  const container = document.getElementById('thumbnailsContainer');
  const countEl = document.getElementById('thumbnailCount');
  if (!container) return;

  if (countEl) countEl.textContent = `${cards.length} thẻ`;

  container.innerHTML = cards.map((c, idx) => `
    <button type="button" data-idx="${idx}"
            class="thumbnail-btn shrink-0 w-8 h-8 rounded border text-xs font-japanese font-bold flex items-center justify-center transition cursor-pointer ${idx === currentIndex ? 'border-slate-900 bg-slate-900 text-white shadow-xs' : 'border-slate-300 bg-slate-50 text-slate-700 hover:bg-slate-200'}"
            title="${escapeHtml(c.char)}: ${escapeHtml(c.meaning || c.hanViet || '')}">
      ${escapeHtml(c.char ? c.char.substring(0, 2) : String(idx + 1))}
    </button>
  `).join('');

  container.querySelectorAll('.thumbnail-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const idx = parseInt(btn.dataset.idx, 10);
      if (!isNaN(idx)) {
        currentIndex = idx;
        renderCard();
      }
    });
  });
}

function highlightActiveThumbnail() {
  const container = document.getElementById('thumbnailsContainer');
  if (!container) return;
  container.querySelectorAll('.thumbnail-btn').forEach(btn => {
    const idx = parseInt(btn.dataset.idx, 10);
    if (idx === currentIndex) {
      btn.className = 'thumbnail-btn shrink-0 w-8 h-8 rounded border border-slate-900 bg-slate-900 text-white font-japanese font-bold flex items-center justify-center shadow-xs cursor-pointer';
      btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    } else {
      btn.className = 'thumbnail-btn shrink-0 w-8 h-8 rounded border border-slate-300 bg-slate-50 text-slate-700 font-japanese font-bold flex items-center justify-center hover:bg-slate-200 cursor-pointer';
    }
  });
}

// 12. Điều hướng quay lại
function goBack() {
  if (currentDeckId) {
    window.location.href = `/flashcards-detail?id=${encodeURIComponent(currentDeckId)}`;
  } else {
    window.location.href = '/flashcards';
  }
}

// 13. DOM Ready & Gán sự kiện
document.addEventListener('DOMContentLoaded', () => {
  loadStudyDeck();

  // Nút quay lại
  document.getElementById('btnBack')?.addEventListener('click', goBack);
  document.getElementById('btnEmptyBack')?.addEventListener('click', goBack);

  // Điều khiển thẻ
  document.getElementById('flashcardElement')?.addEventListener('click', flipCard);
  document.getElementById('btnFlipCard')?.addEventListener('click', flipCard);
  document.getElementById('btnNextCard')?.addEventListener('click', nextCard);
  document.getElementById('btnPrevCard')?.addEventListener('click', prevCard);

  // Phát âm
  document.getElementById('btnSpeakFront')?.addEventListener('click', (e) => {
    e.stopPropagation();
    if (cards[currentIndex]) speakJapanese(cards[currentIndex].char);
  });
  document.getElementById('btnSpeakBack')?.addEventListener('click', (e) => {
    e.stopPropagation();
    if (cards[currentIndex]) speakJapanese(cards[currentIndex].char);
  });

  // Xáo trộn & Reset
  document.getElementById('btnShuffle')?.addEventListener('click', shuffleCards);
  document.getElementById('btnResetProgress')?.addEventListener('click', resetProgress);

  // Điều hướng bằng bàn phím
  document.addEventListener('keydown', (e) => {
    // Tránh can thiệp khi đang gõ vào input
    if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return;

    if (e.code === 'Space') {
      e.preventDefault();
      flipCard();
    } else if (e.code === 'ArrowRight') {
      e.preventDefault();
      nextCard();
    } else if (e.code === 'ArrowLeft') {
      e.preventDefault();
      prevCard();
    }
  });
});
