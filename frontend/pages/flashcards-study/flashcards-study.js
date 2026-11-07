/**
 * KizunaTutor - Trang Luyá»‡n Táº­p Flashcard 3D Äá»™c Láº­p
 * URL Route: /flashcards-study?id={deckId}
 * Há»— trá»£ Ä‘a nÄƒng: Tháº» Kanji Tá»« Äiá»ƒn, Tháº» ThÃªm Tay & Tháº» Excel 2 Cá»™t
 */

import { showToast } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khá»Ÿi táº¡o Shell há»c viÃªn
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Luyá»‡n Táº­p Flashcard 3D',
  badge: 'Bá»™ nhá»› siÃªu tá»‘c'
});

const STORAGE_KEY = 'kizuna_flashcard_decks';
let currentDeckId = null;
let currentDeck = null;
let originalCards = [];
let cards = [];
let currentIndex = 0;
let isFlipped = false;

// 2. Tiá»‡n Ã­ch An ToÃ n Chuá»—i
function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// 3. Táº£i dá»¯ liá»‡u bá»™ tháº»
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
    showToast('KhÃ´ng tÃ¬m tháº¥y mÃ£ Ä‘á»‹nh danh bá»™ tháº»!', 'error');
    setTimeout(() => { window.location.href = '/flashcards'; }, 1500);
    return;
  }

  // 1. TÃ¬m trong LocalStorage
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const decks = JSON.parse(raw);
      if (Array.isArray(decks)) {
        currentDeck = decks.find(d => String(d.id) === String(currentDeckId));
      }
    }
  } catch (e) {
    console.debug('Lá»—i Ä‘á»c localStorage:', e);
  }

  // 2. Náº¿u chÆ°a cÃ³, thá»­ tÃ¬m qua REST API Backend
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
            onyomi: c.onyomi || 'â€”',
            kunyomi: c.kunyomi || 'â€”',
            jlpt: c.jlptLevel || c.jlpt || 'Flashcard'
          }))
        };
      }
    } catch (e) {
      console.debug('Lá»—i API backend:', e);
    }
  }

  if (!currentDeck) {
    showToast('KhÃ´ng tÃ¬m tháº¥y dá»¯ liá»‡u bá»™ tháº» tÆ°Æ¡ng á»©ng!', 'error');
    setTimeout(() => { window.location.href = '/flashcards'; }, 1500);
    return;
  }

  // Cáº­p nháº­t tiÃªu Ä‘á» bá»™ tháº»
  const titleEl = document.getElementById('studyDeckTitle');
  if (titleEl) {
    titleEl.textContent = currentDeck.title || 'Bá»™ Tháº»';
  }
  document.title = `Luyá»‡n Táº­p: ${currentDeck.title || 'Flashcard'} - KizunaTutor`;

  originalCards = Array.isArray(currentDeck.cards) ? [...currentDeck.cards] : [];
  cards = [...originalCards];

  const emptyState = document.getElementById('emptyDeckState');
  const studyArea = document.getElementById('studyAreaContainer');
  const progressBadge = document.getElementById('studyProgressBadge');
  const progressBar = document.getElementById('studyProgressBar');

  if (cards.length === 0) {
    if (emptyState) emptyState.classList.remove('hidden');
    if (studyArea) studyArea.classList.add('hidden');
    if (progressBadge) progressBadge.textContent = 'Tháº» 0 / 0';
    if (progressBar) progressBar.style.width = '0%';
    return;
  }

  if (emptyState) emptyState.classList.add('hidden');
  if (studyArea) studyArea.classList.remove('hidden');

  currentIndex = 0;
  isFlipped = false;
  renderCard();
}

// 4. Hiá»ƒn thá»‹ tháº» hiá»‡n táº¡i vá»›i cÆ¡ cháº¿ tá»± thÃ­ch á»©ng Ä‘a nÄƒng (Adaptive Card Rendering)
function renderCard() {
  if (cards.length === 0) return;
  if (currentIndex < 0) currentIndex = 0;
  if (currentIndex >= cards.length) currentIndex = cards.length - 1;

  const card = cards[currentIndex];
  const total = cards.length;

  // Tráº£ vá» máº·t trÆ°á»›c khi Ä‘á»•i tháº»
  isFlipped = false;
  const cardElement = document.getElementById('flashcardElement');
  if (cardElement) {
    cardElement.classList.remove('is-flipped');
  }

  // Cáº­p nháº­t tiáº¿n Ä‘á»™
  const progressBadge = document.getElementById('studyProgressBadge');
  if (progressBadge) {
    progressBadge.textContent = `Tháº» ${currentIndex + 1} / ${total}`;
  }
  const progressBar = document.getElementById('studyProgressBar');
  if (progressBar) {
    const pct = Math.round(((currentIndex + 1) / total) * 100);
    progressBar.style.width = `${pct}%`;
  }

  // ==================== A. Máº¶T TRÆ¯á»šC (FRONT) ====================
  const frontText = card.char || card.front || card.kanjiChar || card.question || 'â€”';
  const cardCharEl = document.getElementById('studyCardChar');
  if (cardCharEl) {
    cardCharEl.textContent = frontText;
    // Tá»± Ä‘á»™ng co giÃ£n cá»¡ chá»¯ máº·t trÆ°á»›c
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

  // ==================== B. Máº¶T SAU (BACK) ====================
  const charBackEl = document.getElementById('studyCardCharBack');
  if (charBackEl) {
    charBackEl.textContent = frontText;
  }

  // Ã‚m HÃ¡n Viá»‡t (chá»‰ hiá»‡n khi cÃ³)
  const hvEl = document.getElementById('studyCardHanViet');
  if (hvEl) {
    if (card.hanViet && card.hanViet !== 'â€”' && card.hanViet !== 'HÃN Tá»°') {
      hvEl.textContent = card.hanViet.toUpperCase();
      hvEl.classList.remove('hidden');
    } else {
      hvEl.textContent = '';
      hvEl.classList.add('hidden');
    }
  }

  // Chuáº©n bá»‹ ná»™i dung máº·t sau (Back Content)
  let backText = card.meaning || card.back || card.answer || '';

  // TÆ°Æ¡ng thÃ­ch ngÆ°á»£c: Náº¿u tháº» cÅ© chÆ°a ghÃ©p On/Kun vÃ o meaning nhÆ°ng cÃ³ trÆ°á»ng riÃªng
  if ((card.onyomi && card.onyomi !== 'â€”' && !backText.includes('On:')) ||
      (card.kunyomi && card.kunyomi !== 'â€”' && !backText.includes('Kun:'))) {
    const parts = [];
    if (card.onyomi && card.onyomi !== 'â€”' && card.onyomi !== '-') parts.push(`On: ${card.onyomi}`);
    if (card.kunyomi && card.kunyomi !== 'â€”' && card.kunyomi !== '-') parts.push(`Kun: ${card.kunyomi}`);
    if (backText) parts.push(backText);
    backText = parts.join(' / ');
  }

  const meaningContainer = document.getElementById('studyCardMeaningContainer');
  if (meaningContainer) {
    if (backText.includes(' / ')) {
      // Äá»‹nh dáº¡ng phÃ¢n tÃ¡ch báº±ng dáº¥u gáº¡ch chÃ©o: On / Kun / NghÄ©a
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
        if (item.startsWith('Ã‚m HÃ¡n:') || item.startsWith('HÃ¡n Viá»‡t:')) {
          return `
            <div class="flex items-start gap-2 bg-[#F8FAFC] border border-[#E2E8F0] rounded-lg p-2">
              <span class="text-[11px] font-bold text-slate-500 uppercase tracking-wider shrink-0 mt-0.5">HÃ¡n Viá»‡t:</span>
              <span class="text-xs sm:text-sm font-bold text-[#831843] uppercase tracking-wide leading-relaxed break-words">${escapeHtml(item.replace(/^(Ã‚m HÃ¡n|HÃ¡n Viá»‡t):\s*/i, ''))}</span>
            </div>
          `;
        }
        return `
          <div class="p-2 bg-white rounded-lg border border-slate-100">
            <span class="text-[11px] font-bold text-slate-500 block mb-0.5">NghÄ©a tiáº¿ng Viá»‡t:</span>
            <div class="text-xs sm:text-sm font-semibold text-[#0F172A] leading-relaxed break-words">${escapeHtml(item)}</div>
          </div>
        `;
      }).join('');
    } else {
      // Äá»‹nh dáº¡ng tháº» thÆ°á»ng (thÃªm tay, Excel thÃ´ng thÆ°á»ng)
      meaningContainer.innerHTML = `
        <div class="p-3 bg-[#F8FAFC] border border-[#E2E8F0] rounded-lg">
          <span class="text-[11px] font-bold text-slate-500 block mb-1">ÄÃ¡p Ã¡n / Giáº£i nghÄ©a:</span>
          <div class="text-sm sm:text-base font-semibold text-[#0F172A] leading-relaxed break-words">${escapeHtml(backText || 'ChÆ°a cÃ³ giáº£i nghÄ©a')}</div>
        </div>
      `;
    }
  }

  // NÃºt Shodo Canvas: chá»‰ xuáº¥t hiá»‡n khi máº·t trÆ°á»›c Ä‘Ãºng lÃ  1 chá»¯ Kanji Ä‘Æ¡n láº»
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

// 5. Láº­t tháº» 3D
function flipCard() {
  const cardElement = document.getElementById('flashcardElement');
  if (!cardElement) return;
  isFlipped = !isFlipped;
  cardElement.classList.toggle('is-flipped', isFlipped);
}

// 6. Tháº» tiáº¿p theo
function nextCard() {
  if (cards.length === 0) return;
  if (currentIndex < cards.length - 1) {
    currentIndex++;
  } else {
    currentIndex = 0;
    showToast('ÄÃ£ xem háº¿t bá»™ tháº»! Quay láº¡i tá»« tháº» Ä‘áº§u tiÃªn.', 'info');
  }
  renderCard();
}

// 7. Tháº» trÆ°á»›c Ä‘Ã³
function prevCard() {
  if (cards.length === 0) return;
  if (currentIndex > 0) {
    currentIndex--;
  } else {
    currentIndex = cards.length - 1;
  }
  renderCard();
}

// 8. XÃ¡o trá»™n thá»© tá»± tháº» (Fisher-Yates)
function shuffleCards() {
  if (cards.length <= 1) return;
  for (let i = cards.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [cards[i], cards[j]] = [cards[j], cards[i]];
  }
  currentIndex = 0;
  renderCard();
  showToast('ÄÃ£ xÃ¡o trá»™n thá»© tá»± cÃ¡c tháº» trong bá»™!', 'success');
}

// 9. Báº¯t Ä‘áº§u láº¡i
function resetProgress() {
  cards = [...originalCards];
  currentIndex = 0;
  renderCard();
  showToast('ÄÃ£ báº¯t Ä‘áº§u láº¡i tá»« tháº» Ä‘áº§u tiÃªn!', 'info');
}

// 10. Phát âm từ vựng (VOICEVOX Engine & Fallback TTS)
async function speakCurrentCard() {
  if (cards.length === 0) return;
  const card = cards[currentIndex];
  const text = card.char || card.front || card.kanjiChar || card.question || '';
  if (!text) return;

  const btnSpeaker = document.getElementById('btnSpeakerStudy');
  if (btnSpeaker) {
    btnSpeaker.classList.add('scale-110', 'text-[#831843]');
  }

  try {
    await voicevoxClient.speak(text);
  } finally {
    if (btnSpeaker) {
      setTimeout(() => btnSpeaker.classList.remove('scale-110', 'text-[#831843]'), 400);
    }
  }
}

// 11. Äiá»u hÆ°á»›ng quay láº¡i
function goBack() {
  if (currentDeckId) {
    window.location.href = `/learner/deck-detail?id=${encodeURIComponent(currentDeckId)}`;
  } else {
    window.location.href = '/learner/flashcards';
  }
}

// 12. DOM Ready & GÃ¡n sá»± kiá»‡n
document.addEventListener('DOMContentLoaded', () => {
  loadStudyDeck();

  // NÃºt quay láº¡i
  document.getElementById('btnBack')?.addEventListener('click', goBack);
  document.getElementById('btnEmptyBack')?.addEventListener('click', goBack);

  // Äiá»u khiá»ƒn tháº»
  document.getElementById('flashcardElement')?.addEventListener('click', flipCard);
  document.getElementById('btnFlipCard')?.addEventListener('click', flipCard);
  document.getElementById('btnNextCard')?.addEventListener('click', nextCard);
  document.getElementById('btnPrevCard')?.addEventListener('click', prevCard);

  // PhÃ¡t Ã¢m
  document.getElementById('btnStudyAudio')?.addEventListener('click', (e) => {
    e.stopPropagation();
    speakCurrentCard();
  });

  // XÃ¡o trá»™n & Reset
  document.getElementById('btnShuffle')?.addEventListener('click', shuffleCards);
  document.getElementById('btnResetProgress')?.addEventListener('click', resetProgress);

  // Äiá»u hÆ°á»›ng báº±ng bÃ n phÃ­m
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
