/**
 * KizunaTutor - Trang Luyện Tập Flashcard 3D Độc Lập
 * URL Route: /flashcards-study?id={deckId}
 * Hỗ trợ đa năng: Thẻ Kanji Từ Điển, Thẻ Thêm Tay & Thẻ Excel 2 Cột
 */

import { showToast } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
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

// 3. Tải dữ liệu bộ thẻ
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
            char: c.character || c.kanjiChar || c.char || c.front || '',
            hanViet: c.hanViet || '',
            meaning: c.meaning || c.back || '',
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
  const titleEl = document.getElementById('studyDeckTitle');
  if (titleEl) {
    titleEl.textContent = currentDeck.title || 'Bộ Thẻ';
  }
  document.title = `Luyện Tập: ${currentDeck.title || 'Flashcard'} - KizunaTutor`;

  originalCards = Array.isArray(currentDeck.cards) ? [...currentDeck.cards] : [];
  cards = [...originalCards];

  const emptyState = document.getElementById('emptyDeckState');
  const studyArea = document.getElementById('studyAreaContainer');
  const progressBadge = document.getElementById('studyProgressBadge');
  const progressBar = document.getElementById('studyProgressBar');

  if (cards.length === 0) {
    if (emptyState) emptyState.classList.remove('hidden');
    if (studyArea) studyArea.classList.add('hidden');
    if (progressBadge) progressBadge.textContent = 'Thẻ 0 / 0';
    if (progressBar) progressBar.style.width = '0%';
    return;
  }

  if (emptyState) emptyState.classList.add('hidden');
  if (studyArea) studyArea.classList.remove('hidden');

  currentIndex = 0;
  isFlipped = false;
  renderCard();
}

// 4. Hiển thị thẻ hiện tại với cơ chế tự thích ứng đa năng (Adaptive Card Rendering)
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

  // Cập nhật tiến độ
  const progressBadge = document.getElementById('studyProgressBadge');
  if (progressBadge) {
    progressBadge.textContent = `Thẻ ${currentIndex + 1} / ${total}`;
  }
  const progressBar = document.getElementById('studyProgressBar');
  if (progressBar) {
    const pct = Math.round(((currentIndex + 1) / total) * 100);
    progressBar.style.width = `${pct}%`;
  }

  // ==================== A. MẶT TRƯỚC (FRONT) ====================
  const frontText = card.char || card.front || card.kanjiChar || card.question || '—';
  const cardCharEl = document.getElementById('studyCardChar');
  if (cardCharEl) {
    cardCharEl.textContent = frontText;
    // Tự động co giãn cỡ chữ mặt trước
    if (frontText.length <= 2) {
      cardCharEl.className = 'text-6xl sm:text-7xl font-bold text-[#0F172A] font-japanese leading-none block';
    } else if (frontText.length <= 12) {
      cardCharEl.className = 'text-2xl sm:text-3xl font-bold text-[#0F172A] font-japanese leading-tight break-words block';
    } else {
      cardCharEl.className = 'text-base sm:text-lg font-bold text-[#0F172A] leading-snug break-words block';
    }
  }

  const levelBadge = document.getElementById('studyCardLevel');
  if (levelBadge) {
    levelBadge.textContent = card.jlpt || 'Flashcard';
  }

  const stepEl = document.getElementById('studyCardStep');
  if (stepEl) {
    stepEl.textContent = `${currentIndex + 1} / ${total}`;
  }

  // ==================== B. MẶT SAU (BACK) ====================
  const charBackEl = document.getElementById('studyCardCharBack');
  if (charBackEl) {
    charBackEl.textContent = frontText;
  }

  // Âm Hán Việt (chỉ hiện khi có)
  const hvEl = document.getElementById('studyCardHanViet');
  if (hvEl) {
    if (card.hanViet && card.hanViet !== '—' && card.hanViet !== 'HÁN TỰ') {
      hvEl.textContent = card.hanViet.toUpperCase();
      hvEl.classList.remove('hidden');
    } else {
      hvEl.textContent = '';
      hvEl.classList.add('hidden');
    }
  }

  // Chuẩn bị nội dung mặt sau (Back Content)
  let backText = card.meaning || card.back || card.answer || '';

  // Tương thích ngược: Nếu thẻ cũ chưa ghép On/Kun vào meaning nhưng có trường riêng
  if ((card.onyomi && card.onyomi !== '—' && !backText.includes('On:')) ||
      (card.kunyomi && card.kunyomi !== '—' && !backText.includes('Kun:'))) {
    const parts = [];
    if (card.onyomi && card.onyomi !== '—' && card.onyomi !== '-') parts.push(`On: ${card.onyomi}`);
    if (card.kunyomi && card.kunyomi !== '—' && card.kunyomi !== '-') parts.push(`Kun: ${card.kunyomi}`);
    if (backText) parts.push(backText);
    backText = parts.join(' / ');
  }

  const meaningContainer = document.getElementById('studyCardMeaningContainer');
  if (meaningContainer) {
    if (backText.includes(' / ')) {
      // Định dạng phân tách bằng dấu gạch chéo: On / Kun / Nghĩa
      const items = backText.split(' / ').map(s => s.trim()).filter(Boolean);
      meaningContainer.innerHTML = items.map(item => {
        if (item.startsWith('On:')) {
          return `
            <div class="flex items-start gap-2 bg-[#F8FAFC] border border-[#E2E8F0] rounded-lg p-2">
              <span class="text-[11px] font-bold text-slate-500 uppercase tracking-wider shrink-0 mt-0.5">Onyomi:</span>
              <span class="text-xs sm:text-sm font-japanese font-bold text-[#0F172A] leading-relaxed break-words">${escapeHtml(item.replace(/^On:\s*/i, ''))}</span>
            </div>
          `;
        }
        if (item.startsWith('Kun:')) {
          return `
            <div class="flex items-start gap-2 bg-[#F8FAFC] border border-[#E2E8F0] rounded-lg p-2">
              <span class="text-[11px] font-bold text-slate-500 uppercase tracking-wider shrink-0 mt-0.5">Kunyomi:</span>
              <span class="text-xs sm:text-sm font-japanese font-bold text-[#831843] leading-relaxed break-words">${escapeHtml(item.replace(/^Kun:\s*/i, ''))}</span>
            </div>
          `;
        }
        if (item.startsWith('Âm Hán:') || item.startsWith('Hán Việt:')) {
          return `
            <div class="flex items-start gap-2 bg-[#F8FAFC] border border-[#E2E8F0] rounded-lg p-2">
              <span class="text-[11px] font-bold text-slate-500 uppercase tracking-wider shrink-0 mt-0.5">Hán Việt:</span>
              <span class="text-xs sm:text-sm font-bold text-[#831843] uppercase tracking-wide leading-relaxed break-words">${escapeHtml(item.replace(/^(Âm Hán|Hán Việt):\s*/i, ''))}</span>
            </div>
          `;
        }
        return `
          <div class="p-2 bg-white rounded-lg border border-slate-100">
            <span class="text-[11px] font-bold text-slate-500 block mb-0.5">Nghĩa tiếng Việt:</span>
            <div class="text-xs sm:text-sm font-semibold text-[#0F172A] leading-relaxed break-words">${escapeHtml(item)}</div>
          </div>
        `;
      }).join('');
    } else {
      // Định dạng thẻ thường (thêm tay, Excel thông thường)
      meaningContainer.innerHTML = `
        <div class="p-3 bg-[#F8FAFC] border border-[#E2E8F0] rounded-lg">
          <span class="text-[11px] font-bold text-slate-500 block mb-1">Đáp án / Giải nghĩa:</span>
          <div class="text-sm sm:text-base font-semibold text-[#0F172A] leading-relaxed break-words">${escapeHtml(backText || 'Chưa có giải nghĩa')}</div>
        </div>
      `;
    }
  }

  // Nút Shodo Canvas: chỉ xuất hiện khi mặt trước đúng là 1 chữ Kanji đơn lẻ
  const isKanji = typeof frontText === 'string' && frontText.trim().length === 1 && /[\u4e00-\u9faf]/.test(frontText.trim());
  const shodoBtn = document.getElementById('btnStudyPracticeShodo');
  if (shodoBtn) {
    if (isKanji) {
      shodoBtn.classList.remove('hidden');
      shodoBtn.href = `/shodo?char=${encodeURIComponent(frontText.trim())}`;
    } else {
      shodoBtn.classList.add('hidden');
    }
  }
}

// 5. Lật thẻ 3D
function flipCard() {
  const cardElement = document.getElementById('flashcardElement');
  if (!cardElement) return;
  isFlipped = !isFlipped;
  cardElement.classList.toggle('is-flipped', isFlipped);
}

// 6. Thẻ tiếp theo
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

// 7. Thẻ trước đó
function prevCard() {
  if (cards.length === 0) return;
  if (currentIndex > 0) {
    currentIndex--;
  } else {
    currentIndex = cards.length - 1;
  }
  renderCard();
}

// 8. Xáo trộn thứ tự thẻ (Fisher-Yates)
function shuffleCards() {
  if (cards.length <= 1) return;
  for (let i = cards.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [cards[i], cards[j]] = [cards[j], cards[i]];
  }
  currentIndex = 0;
  renderCard();
  showToast('Đã xáo trộn thứ tự các thẻ trong bộ!', 'success');
}

// 9. Bắt đầu lại
function resetProgress() {
  cards = [...originalCards];
  currentIndex = 0;
  renderCard();
  showToast('Đã bắt đầu lại từ thẻ đầu tiên!', 'info');
}

// 10. Phát âm từ vựng (VOICEVOX Engine & Fallback TTS)
async function speakCurrentCard() {
  if (cards.length === 0) return;
  const card = cards[currentIndex];
  const text = card.char || card.front || card.kanjiChar || card.question || '';
  if (!text) return;

  const btnSpeaker = document.getElementById('btnStudyAudio') || document.getElementById('btnSpeakerStudy');
  if (btnSpeaker) {
    btnSpeaker.classList.add('scale-110', 'text-[#831843]', 'ring-2', 'ring-[#831843]');
  }

  try {
    await voicevoxClient.speak(text);
  } finally {
    if (btnSpeaker) {
      setTimeout(() => {
        btnSpeaker.classList.remove('scale-110', 'text-[#831843]', 'ring-2', 'ring-[#831843]');
      }, 400);
    }
  }
}

// 11. Điều hướng quay lại
function goBack() {
  if (currentDeckId) {
    window.location.href = `/learner/deck-detail?id=${encodeURIComponent(currentDeckId)}`;
  } else {
    window.location.href = '/learner/flashcards';
  }
}

// 12. DOM Ready & Gán sự kiện
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
  const audioBtn = document.getElementById('btnStudyAudio') || document.getElementById('btnSpeakerStudy');
  audioBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    speakCurrentCard();
  });

  // Xáo trộn & Reset
  document.getElementById('btnShuffle')?.addEventListener('click', shuffleCards);
  document.getElementById('btnResetProgress')?.addEventListener('click', resetProgress);

  // Điều hướng bằng bàn phím
  document.addEventListener('keydown', (e) => {
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
