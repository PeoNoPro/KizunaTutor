/**
 * KizunaTutor - Trò Chơi Ghép Thẻ (Quizlet Match Game Engine)
 * URL Route: /learner/flashcards-match?id={deckId} hoặc /match?id={deckId}
 */

import { showToast } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khởi tạo Shell học viên
initLearnerShell({
  activeMenu: 'flashcards',
  pageTitle: 'Trò Chơi Ghép Thẻ (Quizlet Match)',
  badge: 'Phản xạ siêu tốc'
});

const STORAGE_KEY = 'kizuna_flashcard_decks';
let currentDeckId = null;
let currentDeck = null;
let allDeckCards = [];

// Trạng thái bàn chơi
let timerInterval = null;
let startTime = null;
let penaltyTime = 0;
let selectedCards = [];
let matchedPairsCount = 0;
let totalPairs = 6;
let isLocked = false;

// Tiện ích an toàn chuỗi
function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// 2. Tải dữ liệu bộ thẻ
async function loadMatchGame() {
  const urlParams = new URLSearchParams(window.location.search);
  currentDeckId = urlParams.get('id') || urlParams.get('deckId');

  if (!currentDeckId) {
    const parts = window.location.pathname.split('/').filter(Boolean);
    const lastPart = parts[parts.length - 1];
    if (lastPart && !['flashcards-match', 'match'].includes(lastPart)) {
      currentDeckId = lastPart;
    }
  }

  if (!currentDeckId) {
    showToast('Không tìm thấy mã bộ thẻ để chơi ghép thẻ!', 'error');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1500);
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
    console.debug('Lỗi đọc localStorage flashcards:', e);
  }

  // 2. Nếu chưa có hoặc chưa có thẻ, nạp qua REST API Backend /api/learner/decks/{id}
  if (!currentDeck || !currentDeck.cards || currentDeck.cards.length === 0) {
    try {
      const res = await fetch(`/api/learner/decks/${encodeURIComponent(currentDeckId)}`).catch(() => null);
      if (res && res.ok) {
        const d = await res.json();
        if (d && (d.title || d.id)) {
          currentDeck = {
            id: String(d.id || currentDeckId),
            title: d.title || d.folderName || 'Bộ thẻ',
            cards: (d.cards || d.items || []).map(c => ({
              char: c.char || c.kanjiChar || c.front || c.character || '',
              meaning: c.meaning || c.back || ''
            }))
          };
        }
      }
    } catch (e) {
      console.debug('Lỗi API backend /api/learner/decks:', e);
    }
  }

  if (!currentDeck) {
    showToast('Không tìm thấy dữ liệu bộ thẻ tương ứng!', 'error');
    setTimeout(() => { window.location.href = '/learner/flashcards'; }, 1500);
    return;
  }

  // Cập nhật tiêu đề bộ thẻ
  const deckTitleEl = document.getElementById('matchDeckTitle');
  if (deckTitleEl) {
    deckTitleEl.innerHTML = `<span>🧩</span> <span>Bộ thẻ: ${escapeHtml(currentDeck.title || 'Học tập')}</span>`;
  }
  document.title = `Ghép Thẻ: ${currentDeck.title || 'Flashcard'} - KizunaTutor`;

  allDeckCards = Array.isArray(currentDeck.cards) ? [...currentDeck.cards] : [];

  // Hiển thị kỷ lục tốt nhất nếu có
  loadBestRecord();

  // Kiểm tra số lượng thẻ tối thiểu
  if (allDeckCards.length < 2) {
    document.getElementById('matchNotEnoughState')?.classList.remove('hidden');
    document.getElementById('matchCardGrid')?.classList.add('hidden');
    return;
  }

  document.getElementById('matchNotEnoughState')?.classList.add('hidden');
  document.getElementById('matchCardGrid')?.classList.remove('hidden');

  startNewMatchSession();
}

// 3. Khởi tạo một phiên chơi mới (Chọn ngẫu nhiên 6 cặp và xáo trộn)
function startNewMatchSession() {
  clearInterval(timerInterval);
  selectedCards = [];
  matchedPairsCount = 0;
  isLocked = false;
  penaltyTime = 0;

  const timerBadge = document.getElementById('matchTimerBadge');
  if (timerBadge) timerBadge.textContent = '0.0s';
  document.getElementById('matchVictoryModal')?.classList.add('hidden');

  // Chọn ngẫu nhiên tối đa 6 cặp thẻ (tổng cộng 12 mảnh)
  const count = Math.min(6, allDeckCards.length);
  const pickedCards = pickRandomItems(allDeckCards, count);
  totalPairs = pickedCards.length;

  let tiles = [];
  pickedCards.forEach((c, index) => {
    const frontText = c.char || c.front || c.kanjiChar || c.question || '—';
    let backText = c.meaning || c.back || c.answer || '—';

    // Nếu là thẻ ghép kiểu mới có On / Kun / Nghĩa: Lấy phần giải nghĩa tiếng Việt tinh gọn để hiển thị vừa vặn trong ô
    if (backText.includes(' / ')) {
      const parts = backText.split(' / ').map(s => s.trim()).filter(Boolean);
      // Ưu tiên lấy phần nghĩa tiếng Việt (thường nằm cuối hoặc không bắt đầu bằng On:/Kun:)
      const cleanMeaning = parts.find(p => !p.startsWith('On:') && !p.startsWith('Kun:') && !p.startsWith('Âm Hán:')) || parts[parts.length - 1];
      backText = cleanMeaning || backText;
    }

    tiles.push({
      id: `q_${index}`,
      pairId: index,
      text: frontText,
      type: 'front',
      isKanji: frontText.length <= 2
    });

    tiles.push({
      id: `a_${index}`,
      pairId: index,
      text: backText,
      type: 'back',
      isKanji: false
    });
  });

  // Trộn đều 12 mảnh thẻ
  tiles = shuffleArray(tiles);

  // Render lưới bàn chơi
  renderMatchGrid(tiles);

  // Bắt đầu bấm giờ
  startTimer();
}

// 4. Render 12 ô thẻ lên bàn chơi
function renderMatchGrid(tiles) {
  const grid = document.getElementById('matchCardGrid');
  if (!grid) return;
  grid.innerHTML = '';

  tiles.forEach(tile => {
    const tileEl = document.createElement('div');
    const kanjiClass = tile.isKanji ? 'font-japanese text-2xl sm:text-3xl font-bold' : 'text-xs sm:text-sm font-semibold';
    
    tileEl.className = `match-tile p-3 sm:p-4 bg-white border border-2 border-[#0F172A] shadow-[2px_2px_0px_0px_#0F172A] rounded-xl shadow-xs flex items-center justify-center text-center cursor-pointer select-none transition-all duration-150 min-h-[90px] sm:min-h-[110px] text-[#0F172A] hover:bg-[#FAF8F2] hover:shadow-sm leading-relaxed break-words ${kanjiClass}`;
    tileEl.dataset.id = tile.id;
    tileEl.dataset.pairId = tile.pairId;
    tileEl.dataset.type = tile.type;
    tileEl.textContent = tile.text;

    tileEl.addEventListener('click', () => handleTileClick(tileEl));
    grid.appendChild(tileEl);
  });
}

// 5. Xử lý khi nhấp vào thẻ
function handleTileClick(el) {
  if (isLocked) return;
  if (el.classList.contains('is-matched')) return;
  if (el.classList.contains('is-selected')) return;

  // Đánh dấu thẻ được chọn
  el.classList.add('is-selected', 'border-[#831843]', 'bg-[#FFE5EC]', 'text-[#831843]', 'ring-2', 'ring-[#831843]/30', 'scale-[1.02]');
  selectedCards.push(el);

  if (selectedCards.length === 2) {
    checkMatch();
  }
}

// 6. Kiểm tra ghép đôi
function checkMatch() {
  isLocked = true;
  const [first, second] = selectedCards;

  const isSamePair = String(first.dataset.pairId) === String(second.dataset.pairId);
  const isDifferentType = first.dataset.type !== second.dataset.type;

  if (isSamePair && isDifferentType) {
    // ==================== GHÉP ĐÚNG ====================
    first.classList.remove('border-[#4255FF]', 'bg-[#F6F7FB]', 'ring-2', 'ring-[#4255FF]/20');
    second.classList.remove('border-[#4255FF]', 'bg-[#F6F7FB]', 'ring-2', 'ring-[#4255FF]/20');

    first.classList.add('border-emerald-500', 'bg-emerald-50', 'text-emerald-800', 'animate-success');
    second.classList.add('border-emerald-500', 'bg-emerald-50', 'text-emerald-800', 'animate-success');

    // Phát âm từ vựng tiếng Nhật khi ghép đúng
    const frontTile = first.dataset.type === 'front' ? first : second;
    if (frontTile && frontTile.textContent) {
      voicevoxClient.speak(frontTile.textContent);
    }

    setTimeout(() => {
      first.classList.add('opacity-0', 'pointer-events-none', 'is-matched');
      second.classList.add('opacity-0', 'pointer-events-none', 'is-matched');
      selectedCards = [];
      isLocked = false;
      matchedPairsCount++;

      if (matchedPairsCount === totalPairs) {
        finishGame();
      }
    }, 280);

  } else {
    // ==================== GHÉP SAI ====================
    // Phạt thêm 1 giây vào đồng hồ
    penaltyTime += 1;
    showPenaltyNotice();

    first.classList.remove('border-[#4255FF]', 'bg-[#F6F7FB]', 'ring-2', 'ring-[#4255FF]/20');
    second.classList.remove('border-[#4255FF]', 'bg-[#F6F7FB]', 'ring-2', 'ring-[#4255FF]/20');

    first.classList.add('border-[#BA1A1A]', 'bg-rose-50', 'text-[#BA1A1A]', 'animate-shake');
    second.classList.add('border-[#BA1A1A]', 'bg-rose-50', 'text-[#BA1A1A]', 'animate-shake');

    setTimeout(() => {
      first.classList.remove('is-selected', 'border-[#BA1A1A]', 'bg-rose-50', 'text-[#BA1A1A]', 'animate-shake', 'scale-[1.02]');
      second.classList.remove('is-selected', 'border-[#BA1A1A]', 'bg-rose-50', 'text-[#BA1A1A]', 'animate-shake', 'scale-[1.02]');
      selectedCards = [];
      isLocked = false;
    }, 500);
  }
}

// 7. Đồng hồ bấm giờ (Stopwatch)
function startTimer() {
  startTime = performance.now();
  timerInterval = setInterval(() => {
    const currentElapsed = (performance.now() - startTime) / 1000 + penaltyTime;
    const badge = document.getElementById('matchTimerBadge');
    if (badge) {
      badge.textContent = `${currentElapsed.toFixed(1)}s`;
    }
  }, 100);
}

// 8. Hiệu ứng cộng thời gian phạt
function showPenaltyNotice() {
  const badge = document.getElementById('matchTimerBadge');
  if (badge) {
    badge.classList.add('bg-rose-100', 'text-rose-600', 'border-rose-300');
    setTimeout(() => {
      badge.classList.remove('bg-rose-100', 'text-rose-600', 'border-rose-300');
    }, 400);
  }
}

// 9. Hoàn thành ván đấu (Victory Modal)
function finishGame() {
  clearInterval(timerInterval);
  const totalSeconds = parseFloat(((performance.now() - startTime) / 1000 + penaltyTime).toFixed(1));
  
  const finalTimeEl = document.getElementById('finalRecordTime');
  if (finalTimeEl) {
    finalTimeEl.textContent = `${totalSeconds}s`;
  }

  // Kiểm tra kỷ lục cá nhân
  const recordKey = `kizuna_match_best_${currentDeckId}`;
  let isNewRecord = false;
  try {
    const previousBest = localStorage.getItem(recordKey);
    if (!previousBest || totalSeconds < parseFloat(previousBest)) {
      localStorage.setItem(recordKey, String(totalSeconds));
      isNewRecord = true;
    }
  } catch (e) {}

  const victoryRecordLabel = document.getElementById('victoryRecordLabel');
  if (victoryRecordLabel) {
    if (isNewRecord) {
      victoryRecordLabel.classList.remove('hidden');
    } else {
      victoryRecordLabel.classList.add('hidden');
    }
  }

  loadBestRecord();

  const victoryModal = document.getElementById('matchVictoryModal');
  if (victoryModal) {
    victoryModal.classList.remove('hidden');
    victoryModal.classList.add('flex');
  }
}

// 10. Tải kỷ lục tốt nhất
function loadBestRecord() {
  if (!currentDeckId) return;
  const recordKey = `kizuna_match_best_${currentDeckId}`;
  const bestBadge = document.getElementById('bestRecordBadge');
  const container = document.getElementById('bestRecordContainer');

  try {
    const best = localStorage.getItem(recordKey);
    if (best && bestBadge && container) {
      bestBadge.textContent = `${best}s`;
      container.classList.remove('hidden');
      container.classList.add('flex');
    }
  } catch (e) {}
}

// Tiện ích xáo trộn mảng (Fisher-Yates)
function shuffleArray(arr) {
  const cloned = [...arr];
  for (let i = cloned.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [cloned[i], cloned[j]] = [cloned[j], cloned[i]];
  }
  return cloned;
}

// Chọn ngẫu nhiên k phần tử
function pickRandomItems(arr, count) {
  const shuffled = shuffleArray(arr);
  return shuffled.slice(0, count);
}

// Điều hướng quay lại
function exitMatchGame() {
  clearInterval(timerInterval);
  if (currentDeckId) {
    window.location.href = `/learner/deck-detail?id=${encodeURIComponent(currentDeckId)}`;
  } else {
    window.location.href = '/learner/flashcards';
  }
}

// 11. Khởi tạo DOM Ready
document.addEventListener('DOMContentLoaded', () => {
  loadMatchGame();

  // Nút rời khỏi
  document.getElementById('btnExitMatch')?.addEventListener('click', exitMatchGame);
  document.getElementById('btnBackToDeck')?.addEventListener('click', exitMatchGame);
  document.getElementById('btnBackToDeckFromEmpty')?.addEventListener('click', exitMatchGame);

  // Nút chơi lại ván mới
  document.getElementById('btnRestartMatch')?.addEventListener('click', () => {
    startNewMatchSession();
  });
});
