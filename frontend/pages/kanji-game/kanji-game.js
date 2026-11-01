/**
 * KizunaTutor - Game Mưa Chữ Hán (Kanji Rain Typing)
 * Feature-Based Architecture: frontend/pages/kanji-game/
 * Tuân thủ nghiêm ngặt FRONTEND_RULES.md (Zero mock/fake data)
 */

import { debounce, showToast } from '/js/main.js';

// =========================================================================
// 1. WEB AUDIO API SYNTHESIZER (Tự động phát âm không cần file MP3 bên ngoài)
// =========================================================================
class SoundEffects {
  constructor() {
    this.ctx = null;
    this.enabled = true;
  }

  init() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) {
        this.ctx = new AudioCtx();
      }
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  playDing() {
    if (!this.enabled) return;
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sine';
    osc.frequency.setValueAtTime(880, now);
    osc.frequency.exponentialRampToValueAtTime(1760, now + 0.1);

    gain.gain.setValueAtTime(0.2, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.35);

    osc.connect(gain);
    gain.connect(this.ctx.destination);

    osc.start(now);
    osc.stop(now + 0.35);
  }

  playBuzz() {
    if (!this.enabled) return;
    this.init();
    if (!this.ctx) return;

    const now = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(130, now);
    osc.frequency.linearRampToValueAtTime(85, now + 0.18);

    gain.gain.setValueAtTime(0.18, now);
    gain.gain.linearRampToValueAtTime(0.01, now + 0.18);

    osc.connect(gain);
    gain.connect(this.ctx.destination);

    osc.start(now);
    osc.stop(now + 0.18);
  }

  playVictory() {
    if (!this.enabled) return;
    this.init();
    if (!this.ctx) return;

    const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
    const now = this.ctx.currentTime;

    notes.forEach((freq, idx) => {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      const start = now + idx * 0.12;

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, start);

      gain.gain.setValueAtTime(0.25, start);
      gain.gain.exponentialRampToValueAtTime(0.001, start + 0.45);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start(start);
      osc.stop(start + 0.45);
    });
  }

  playGameOver() {
    if (!this.enabled) return;
    this.init();
    if (!this.ctx) return;

    const notes = [329.63, 311.13, 293.66, 277.18]; // E4, D#4, D4, C#4
    const now = this.ctx.currentTime;

    notes.forEach((freq, idx) => {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      const start = now + idx * 0.14;

      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(freq, start);

      gain.gain.setValueAtTime(0.2, start);
      gain.gain.linearRampToValueAtTime(0.01, start + 0.28);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start(start);
      osc.stop(start + 0.28);
    });
  }
}

// =========================================================================
// 2. HỆ THỐNG HẠT NỔ PHÁO HOA KHI BẮN TRÚNG (SPARKLE PARTICLES)
// =========================================================================
class SparkleParticle {
  constructor(x, y, color = '#38BDF8') {
    this.x = x;
    this.y = y;
    this.color = color;
    const angle = Math.random() * Math.PI * 2;
    const speed = Math.random() * 5 + 2;
    this.vx = Math.cos(angle) * speed;
    this.vy = Math.sin(angle) * speed;
    this.alpha = 1;
    this.decay = Math.random() * 0.03 + 0.02;
    this.radius = Math.random() * 3 + 2;
  }

  update() {
    this.x += this.vx;
    this.y += this.vy;
    this.alpha -= this.decay;
  }

  draw(ctx) {
    if (this.alpha <= 0) return;
    ctx.save();
    ctx.globalAlpha = Math.max(0, this.alpha);
    ctx.fillStyle = this.color;
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }
}

// =========================================================================
// 3. THỂ HIỆN CHỮ HÁN RƠI TRÊN CANVAS (FALLING KANJI)
// =========================================================================
class FallingKanji {
  constructor(data, canvasWidth, speedMultiplier = 1) {
    this.id = data.kanjiId || `k-${Date.now()}`;
    this.character = data.character || '日';
    this.readings = data.readings || [];
    this.primaryReading = data.primaryReading || (this.readings.length > 0 ? this.readings[0] : '');
    this.hanViet = data.hanViet || '';
    this.meaning = data.meaning || '';
    this.jlpt = data.jlptLevel || 'N5';

    this.width = 68;
    this.height = 76;
    const padding = 15;
    this.x = Math.random() * (Math.max(100, canvasWidth - this.width - padding * 2)) + padding;
    this.y = -this.height;
    
    // Tốc độ cơ bản: 35 đến 60 px/giây nhân theo hệ số người chơi chọn
    this.baseSpeed = (Math.random() * 25 + 35) * speedMultiplier;
  }

  update(dt) {
    this.y += this.baseSpeed * dt;
  }

  draw(ctx) {
    ctx.save();

    // 1. Hộp bao quanh mặt chữ (Rounded Glass Card)
    const radius = 12;
    ctx.shadowColor = 'rgba(0, 0, 0, 0.4)';
    ctx.shadowBlur = 10;
    ctx.shadowOffsetY = 4;

    ctx.fillStyle = 'rgba(30, 41, 59, 0.88)';
    ctx.strokeStyle = '#38BDF8';
    ctx.lineWidth = 1.5;

    ctx.beginPath();
    ctx.roundRect(this.x, this.y, this.width, this.height, radius);
    ctx.fill();
    ctx.stroke();

    // Tắt bóng đổ để vẽ chữ sắc nét
    ctx.shadowColor = 'transparent';

    // 2. Vẽ mặt chữ Hán to ở giữa
    ctx.fillStyle = '#FFFFFF';
    ctx.font = '900 28px "Noto Serif JP", serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(this.character, this.x + this.width / 2, this.y + 26);

    // 3. Huy hiệu cách đọc gợi ý (Primary Reading Badge)
    const badgeY = this.y + this.height - 18;
    ctx.fillStyle = 'rgba(14, 165, 233, 0.25)';
    ctx.strokeStyle = 'rgba(56, 189, 248, 0.6)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.roundRect(this.x + 6, badgeY - 10, this.width - 12, 18, 6);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = '#BAE6FD';
    ctx.font = 'bold 11px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(this.primaryReading || this.hanViet, this.x + this.width / 2, badgeY - 1);

    ctx.restore();
  }
}

// =========================================================================
// 4. BỘ ĐIỀU KHIỂN CHÍNH TRÒ CHƠI (GAME ENGINE)
// =========================================================================
const sounds = new SoundEffects();

// DOM References
const canvas = document.getElementById('kanjiRainCanvas');
const ctx = canvas.getContext('2d');
const inputField = document.getElementById('kanjiGameInput');
const liveTypingEcho = document.getElementById('liveTypingEcho');
const startOverlay = document.getElementById('gameStartOverlay');
const btnStartTrigger = document.getElementById('btnStartGameTrigger');
const btnRestart = document.getElementById('btnRestartGame');
const btnToggleSound = document.getElementById('btnToggleSound');
const iconSoundState = document.getElementById('iconSoundState');

// HUD
const hudScoreText = document.getElementById('hudScoreText');
const hudComboBadge = document.getElementById('hudComboBadge');
const hudTimeText = document.getElementById('hudTimeText');
const hudWordsCount = document.getElementById('hudWordsCount');
const hudAccuracy = document.getElementById('hudAccuracy');

// Settings
const selectGameSource = document.getElementById('selectGameSource');
const optgroupFlashcardDecks = document.getElementById('optgroupFlashcardDecks');
const durationBtns = document.querySelectorAll('.duration-btn');
const speedBtns = document.querySelectorAll('.speed-btn');

// Result Modal
const resultModal = document.getElementById('gameResultModal');
const btnCloseResultModal = document.getElementById('btnCloseResultModal');
const btnPlayAgain = document.getElementById('btnPlayAgain');
const modalResultBanner = document.getElementById('modalResultBanner');
const modalResultIcon = document.getElementById('modalResultIcon');
const modalResultTitle = document.getElementById('modalResultTitle');
const modalResultSubtitle = document.getElementById('modalResultSubtitle');
const modalRankTitle = document.getElementById('modalRankTitle');
const modalFinalScore = document.getElementById('modalFinalScore');
const modalFinalWords = document.getElementById('modalFinalWords');
const modalFinalAccuracy = document.getElementById('modalFinalAccuracy');
const modalFinalMistakes = document.getElementById('modalFinalMistakes');
const modalMistakesSection = document.getElementById('modalMistakesSection');
const modalMistakesList = document.getElementById('modalMistakesList');
const leaderboardList = document.getElementById('leaderboardList');

// Game State
let isRunning = false;
let gamePool = [];
let fallingItems = [];
let particles = [];
let lastTimestamp = 0;
let spawnTimer = 0;
let spawnInterval = 2.4; // giây

let selectedDuration = 60; // giây
let remainingTime = 60;
let speedMultiplier = 1;

let score = 0;
let combo = 0;
let maxCombo = 0;
let wordsTyped = 0;
let totalKeystrokes = 0;
let totalMistakes = 0;
let mistakeMap = new Map(); // KanjiId -> { char, reading, count, typed }

// =========================================================================
// 5. RESIZE & CANVAS RESPONSIVENESS
// =========================================================================
function resizeCanvas() {
  const rect = canvas.getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  ctx.scale(dpr, dpr);
}
window.addEventListener('resize', resizeCanvas);

// =========================================================================
// 6. TIỆN ÍCH CHUYỂN ĐỔI CHUỖI GÕ (KATAKANA, HIRAGANA, ROMAJI)
// =========================================================================
function katakanaToHiragana(str) {
  if (!str) return '';
  let res = '';
  for (let i = 0; i < str.length; i++) {
    const code = str.charCodeAt(i);
    if (code >= 0x30A1 && code <= 0x30F6) {
      res += String.fromCharCode(code - 0x60);
    } else {
      res += str[i];
    }
  }
  return res;
}

function removeAccents(str) {
  if (!str) return '';
  return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D');
}

function normalizeInput(str) {
  if (!str) return '';
  const trimmed = str.trim().toLowerCase();
  return katakanaToHiragana(trimmed);
}

// =========================================================================
// 7. TRUY VẤN DỮ LIỆU CHỮ HÁN CHO TRẬN ĐẤU (ZERO MOCK DATA)
// =========================================================================
async function loadGameData() {
  const source = selectGameSource.value;

  // Nếu người dùng chọn một bộ thẻ cá nhân từ Flashcards
  if (source.startsWith('deck:')) {
    const deckId = source.replace('deck:', '');
    const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
    let decks = [];
    try { decks = rawDecks ? JSON.parse(rawDecks) : []; } catch (e) { decks = []; }

    const targetDeck = decks.find(d => String(d.id) === String(deckId));
    if (targetDeck && targetDeck.cards && targetDeck.cards.length > 0) {
      gamePool = targetDeck.cards.map((c, i) => ({
        kanjiId: `deck-card-${i}`,
        character: c.char,
        hanViet: c.hanViet || '',
        meaning: c.meaning || '',
        jlptLevel: c.jlpt || 'N5',
        primaryReading: c.kunyomi || c.onyomi || c.hanViet || '',
        readings: [
          normalizeInput(c.kunyomi || ''),
          normalizeInput(c.onyomi || ''),
          removeAccents(c.hanViet || '').toLowerCase(),
          (c.hanViet || '').toLowerCase()
        ].filter(Boolean)
      }));
      return true;
    } else {
      showToast('Bộ thẻ này hiện chưa có chữ Hán nào. Vui lòng chọn bộ khác hoặc kho từ điển.', 'error');
      return false;
    }
  }

  // Ngược lại: tải dữ liệu thực tế từ Backend API
  try {
    const jlptParam = source === 'ALL' ? '' : `&jlpt=${encodeURIComponent(source)}`;
    const res = await fetch(`/api/learner/minigame/play-data?limit=40${jlptParam}`);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        gamePool = data;
        return true;
      }
    }
  } catch (err) {
    console.debug('Lỗi khi tải dữ liệu game từ server:', err);
  }

  showToast('Chưa có dữ liệu chữ Hán cho cấp độ này. Hãy thử cấp độ khác.', 'info');
  return false;
}

// Nạp danh sách bộ thẻ cá nhân vào dropdown
function populatePersonalDecksDropdown() {
  const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
  let decks = [];
  try { decks = rawDecks ? JSON.parse(rawDecks) : []; } catch (e) { decks = []; }

  if (!optgroupFlashcardDecks) return;
  optgroupFlashcardDecks.innerHTML = '';

  if (decks.length > 0) {
    decks.forEach(d => {
      const opt = document.createElement('option');
      opt.value = `deck:${d.id}`;
      opt.textContent = `🗂️ ${d.title} (${(d.cards || []).length} thẻ)`;
      optgroupFlashcardDecks.appendChild(opt);
    });
  } else {
    const opt = document.createElement('option');
    opt.value = '';
    opt.disabled = true;
    opt.textContent = '(Chưa có bộ thẻ cá nhân)';
    optgroupFlashcardDecks.appendChild(opt);
  }
}

// =========================================================================
// 8. BẮT ĐẦU VÁN ĐẤU (GAME INITIALIZATION & LOOP)
// =========================================================================
async function startGame() {
  const hasData = await loadGameData();
  if (!hasData || gamePool.length === 0) return;

  // Khởi tạo các thông số trận đấu
  resizeCanvas();
  isRunning = true;
  fallingItems = [];
  particles = [];
  lastTimestamp = performance.now();
  spawnTimer = 0;
  remainingTime = selectedDuration;

  score = 0;
  combo = 0;
  maxCombo = 0;
  wordsTyped = 0;
  totalKeystrokes = 0;
  totalMistakes = 0;
  mistakeMap.clear();

  updateHud();

  startOverlay?.classList.add('hidden');
  resultModal?.classList.add('hidden');
  resultModal?.classList.remove('flex');

  if (inputField) {
    inputField.value = '';
    inputField.focus();
  }
  if (liveTypingEcho) {
    liveTypingEcho.textContent = '...';
  }

  // Thả chữ đầu tiên ngay lập tức
  spawnKanji();

  requestAnimationFrame(gameLoop);
}

function spawnKanji() {
  if (gamePool.length === 0) return;
  const randIndex = Math.floor(Math.random() * gamePool.length);
  const data = gamePool[randIndex];
  const rect = canvas.getBoundingClientRect();
  const item = new FallingKanji(data, rect.width, speedMultiplier);
  fallingItems.push(item);
}

function gameLoop(timestamp) {
  if (!isRunning) return;

  const dt = Math.min((timestamp - lastTimestamp) / 1000, 0.1);
  lastTimestamp = timestamp;

  // Cập nhật đồng hồ đếm ngược
  remainingTime -= dt;
  if (remainingTime <= 0) {
    remainingTime = 0;
    endGame(true); // Thắng ván đấu (hết giờ an toàn)
    return;
  }

  // Cập nhật sinh chữ rơi theo nhịp
  spawnTimer += dt;
  const currentInterval = Math.max(1.2, spawnInterval - (wordsTyped / 30) * 0.8);
  if (spawnTimer >= currentInterval) {
    spawnTimer = 0;
    spawnKanji();
  }

  // Vẽ Canvas
  const rect = canvas.getBoundingClientRect();
  const width = rect.width;
  const height = rect.height;

  ctx.clearRect(0, 0, width, height);

  // 1. Vẽ Vạch Đỏ Nguy Hiểm (Danger Line at Bottom)
  const dangerY = height - 42;
  ctx.save();
  ctx.strokeStyle = 'rgba(239, 68, 68, 0.75)';
  ctx.lineWidth = 2;
  ctx.setLineDash([6, 6]);
  ctx.beginPath();
  ctx.moveTo(0, dangerY);
  ctx.lineTo(width, dangerY);
  ctx.stroke();

  // Nhãn vạch nguy hiểm
  ctx.fillStyle = 'rgba(239, 68, 68, 0.9)';
  ctx.font = 'bold 10px sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText('RANH GIỚI BẢO VỆ', width - 12, dangerY - 6);
  ctx.restore();

  // 2. Cập nhật & Vẽ các chữ đang rơi
  for (let i = fallingItems.length - 1; i >= 0; i--) {
    const item = fallingItems[i];
    item.update(dt);
    item.draw(ctx);

    // Kiểm tra chạm vạch Deadline
    if (item.y + item.height >= dangerY) {
      // Game Over: Chữ chạm vạch nguy hiểm!
      endGame(false, item);
      return;
    }
  }

  // 3. Cập nhật & Vẽ hạt pháo hoa
  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i];
    p.update();
    p.draw(ctx);
    if (p.alpha <= 0) {
      particles.splice(i, 1);
    }
  }

  updateHud();

  requestAnimationFrame(gameLoop);
}

// =========================================================================
// 9. CẬP NHẬT HUD & TÍNH ĐỘ CHÍNH XÁC
// =========================================================================
function updateHud() {
  if (hudScoreText) hudScoreText.textContent = score;

  if (hudComboBadge) {
    if (combo >= 2) {
      hudComboBadge.textContent = `Combo x${combo}`;
      hudComboBadge.classList.remove('hidden');
    } else {
      hudComboBadge.classList.add('hidden');
    }
  }

  if (hudTimeText) {
    const mins = Math.floor(Math.max(0, remainingTime) / 60);
    const secs = Math.floor(Math.max(0, remainingTime) % 60);
    hudTimeText.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  if (hudWordsCount) hudWordsCount.textContent = `${wordsTyped} chữ`;

  if (hudAccuracy) {
    const totalAttempts = wordsTyped + totalMistakes;
    const acc = totalAttempts > 0 ? Math.round((wordsTyped / totalAttempts) * 100) : 100;
    hudAccuracy.textContent = `${acc}%`;
  }
}

// =========================================================================
// 10. XỬ LÝ SỰ KIỆN GÕ PHÍM & SO KHỚP CHỮ
// =========================================================================
function checkTypingMatch(isEnterCommit = false) {
  const rawValue = inputField.value;
  const inputNorm = normalizeInput(rawValue);
  const inputNoAccent = removeAccents(inputNorm);

  if (liveTypingEcho) {
    liveTypingEcho.textContent = rawValue || '...';
  }

  if (!inputNorm) return;

  // Tìm chữ rơi ở vị trí thấp nhất khớp với cách gõ
  let lowestMatchIndex = -1;
  let maxMatchY = -999;

  for (let i = 0; i < fallingItems.length; i++) {
    const item = fallingItems[i];
    const isMatched = item.readings.some(r => {
      const normR = normalizeInput(r);
      const noAccentR = removeAccents(normR);
      return normR === inputNorm || noAccentR === inputNoAccent || r.toLowerCase() === inputNorm;
    });

    if (isMatched && item.y > maxMatchY) {
      maxMatchY = item.y;
      lowestMatchIndex = i;
    }
  }

  // Nếu tìm thấy chữ khớp!
  if (lowestMatchIndex >= 0) {
    const hitItem = fallingItems[lowestMatchIndex];

    // Phát âm thanh & tạo hiệu ứng hạt nổ pháo hoa
    sounds.playDing();
    for (let p = 0; p < 18; p++) {
      particles.push(new SparkleParticle(hitItem.x + hitItem.width / 2, hitItem.y + hitItem.height / 2));
    }

    // Xóa chữ khỏi danh sách rơi
    fallingItems.splice(lowestMatchIndex, 1);

    // Điểm cộng & Combo
    combo++;
    if (combo > maxCombo) maxCombo = combo;
    const comboBonus = (combo - 1) * 20;
    score += (100 + comboBonus);
    wordsTyped++;

    inputField.value = '';
    if (liveTypingEcho) liveTypingEcho.textContent = '...';

    updateHud();
    return;
  }

  // Nếu bấm Enter mà không khớp bất kỳ chữ nào -> Tính là gõ sai
  if (isEnterCommit && rawValue.trim().length > 0) {
    sounds.playBuzz();
    combo = 0;
    totalMistakes++;

    // Ghi nhận chữ thấp nhất bị gõ sai
    if (fallingItems.length > 0) {
      const lowestItem = fallingItems.reduce((prev, curr) => (curr.y > prev.y ? curr : prev));
      const entry = mistakeMap.get(lowestItem.character) || {
        kanjiId: lowestItem.id,
        char: lowestItem.character,
        reading: lowestItem.primaryReading,
        count: 0,
        typed: rawValue
      };
      entry.count++;
      entry.typed = rawValue;
      mistakeMap.set(lowestItem.character, entry);
    }

    inputField.classList.add('border-rose-500');
    setTimeout(() => inputField.classList.remove('border-rose-500'), 250);

    inputField.value = '';
    if (liveTypingEcho) liveTypingEcho.textContent = '...';

    updateHud();
  }
}

// =========================================================================
// 11. KẾT THÚC TRẬN ĐẤU & LƯU PHIÊN (GAME OVER / VICTORY)
// =========================================================================
async function endGame(isWon, failedKanji = null) {
  isRunning = false;

  const totalAttempts = wordsTyped + totalMistakes;
  const accuracy = totalAttempts > 0 ? Number(((wordsTyped / totalAttempts) * 100).toFixed(1)) : 100.0;
  const durationPlayed = Math.round(selectedDuration - remainingTime);

  if (isWon) {
    sounds.playVictory();
    modalResultIcon.textContent = '🏆';
    modalResultIcon.className = 'w-16 h-16 rounded-2xl mx-auto flex items-center justify-center text-3xl bg-amber-50 text-amber-600 border border-amber-200 shadow-sm';
    modalResultTitle.textContent = 'Bảo Vệ Thành Công!';
    modalResultSubtitle.textContent = 'Bạn đã sống sót qua toàn bộ thời gian thử thách';
  } else {
    sounds.playGameOver();
    modalResultIcon.textContent = '💥';
    modalResultIcon.className = 'w-16 h-16 rounded-2xl mx-auto flex items-center justify-center text-3xl bg-rose-50 text-rose-600 border border-rose-200 shadow-sm';
    modalResultTitle.textContent = 'Chữ Đã Chạm Đáy!';
    modalResultSubtitle.textContent = failedKanji ? `Chữ "${failedKanji.character}" (${failedKanji.primaryReading}) đã chạm vạch ranh giới!` : 'Chữ Hán đã chạm vào vạch nguy hiểm';
  }

  // Danh hiệu đạt được
  let rank = 'Tân Binh Luyện Chữ 🌱';
  if (!isWon) {
    rank = 'Chiến Binh Kiên Trì 🛡️';
  } else if (wordsTyped >= 45 && accuracy >= 95.0) {
    rank = 'Bậc Thầy Chữ Hán 👑';
  } else if (wordsTyped >= 30 && accuracy >= 90.0) {
    rank = 'Cao Thủ Gõ Phím ⚡';
  } else if (wordsTyped >= 15 && accuracy >= 80.0) {
    rank = 'Học Giả Tiến Bộ 📖';
  }
  if (modalRankTitle) modalRankTitle.textContent = rank;

  if (modalFinalScore) modalFinalScore.textContent = score;
  if (modalFinalWords) modalFinalWords.textContent = `${wordsTyped} chữ`;
  if (modalFinalAccuracy) modalFinalAccuracy.textContent = `${accuracy}%`;
  if (modalFinalMistakes) modalFinalMistakes.textContent = totalMistakes;

  // Hiển thị danh sách từ sai
  const mistakesArray = Array.from(mistakeMap.values());
  if (failedKanji && !mistakeMap.has(failedKanji.character)) {
    mistakesArray.unshift({
      kanjiId: failedKanji.id,
      char: failedKanji.character,
      reading: failedKanji.primaryReading,
      count: 1,
      typed: 'Chạm đáy'
    });
  }

  if (mistakesArray.length > 0) {
    modalMistakesSection?.classList.remove('hidden');
    modalMistakesList.innerHTML = mistakesArray.map(m => `
      <div class="flex items-center justify-between p-2 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] text-xs">
        <div class="flex items-center gap-2">
          <span class="w-8 h-8 rounded-lg bg-white border border-[#ECE8DE] flex items-center justify-center font-japanese font-bold text-base text-[#0F2545]">
            ${m.char}
          </span>
          <div>
            <span class="font-bold text-[#006194]">${m.reading}</span>
            <span class="text-[10px] text-[#68737D] block">Sai ${m.count} lần</span>
          </div>
        </div>
        <div class="flex items-center gap-1.5">
          <a href="/shodo?kanji=${encodeURIComponent(m.char)}" 
             class="px-2 py-1 rounded-lg bg-white border border-[#ECE8DE] hover:bg-[#F2EFE6] text-[#0F2545] font-semibold text-[11px] no-underline">
            Luyện viết
          </a>
        </div>
      </div>
    `).join('');
  } else {
    modalMistakesSection?.classList.add('hidden');
  }

  resultModal?.classList.remove('hidden');
  resultModal?.classList.add('flex');

  // Gửi lưu kết quả lên Backend API
  try {
    const payload = {
      durationSeconds: durationPlayed,
      targetDurationSeconds: selectedDuration,
      wordsTypedCount: wordsTyped,
      totalMistakes: totalMistakes,
      accuracy: accuracy,
      score: score,
      isWon: isWon,
      failedKanjiCharacter: failedKanji ? failedKanji.character : null,
      mistakes: mistakesArray.map(m => ({
        kanjiCharacter: m.char,
        reading: m.reading,
        mistakeCount: m.count,
        typedInput: m.typed
      }))
    };

    await fetch('/api/learner/minigame/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    // Cập nhật lại Bảng xếp hạng
    loadLeaderboard();
  } catch (err) {
    console.debug('Lỗi khi lưu phiên chơi lên backend:', err);
  }
}

// =========================================================================
// 12. TẢI BẢNG XẾP HẠNG TỪ BACKEND
// =========================================================================
async function loadLeaderboard() {
  if (!leaderboardList) return;
  try {
    const res = await fetch('/api/learner/minigame/leaderboard');
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        leaderboardList.innerHTML = data.slice(0, 5).map((item, idx) => {
          const medal = idx === 0 ? '🥇' : (idx === 1 ? '🥈' : (idx === 2 ? '🥉' : `#${idx + 1}`));
          return `
            <div class="flex items-center justify-between p-2.5 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] text-xs">
              <div class="flex items-center gap-2">
                <span class="w-6 text-center font-bold text-sm">${medal}</span>
                <div>
                  <div class="font-bold text-[#0F2545]">${item.titleRank || 'Chiến Binh'}</div>
                  <div class="text-[10px] text-[#68737D]">${item.wordsTypedCount} chữ • Chính xác ${item.accuracy}%</div>
                </div>
              </div>
              <span class="font-extrabold text-[#006194] text-sm">${item.score} đ</span>
            </div>
          `;
        }).join('');
        return;
      }
    }
  } catch (e) {
    console.debug('Lỗi fetch leaderboard:', e);
  }

  leaderboardList.innerHTML = `
    <div class="py-6 text-center text-xs text-[#68737D] space-y-2">
      <span class="material-symbols-outlined text-3xl text-slate-300">hourglass_empty</span>
      <p>Chưa có phiên chơi nào được lưu.</p>
    </div>
  `;
}

// =========================================================================
// 13. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  resizeCanvas();
  populatePersonalDecksDropdown();
  loadLeaderboard();

  // Bắt đầu game khi nhấn nút lớp phủ
  btnStartTrigger?.addEventListener('click', startGame);

  // Chơi lại ván mới
  btnRestart?.addEventListener('click', startGame);
  btnPlayAgain?.addEventListener('click', () => {
    resultModal?.classList.add('hidden');
    resultModal?.classList.remove('flex');
    startGame();
  });

  // Đóng Modal kết quả
  btnCloseResultModal?.addEventListener('click', () => {
    resultModal?.classList.add('hidden');
    resultModal?.classList.remove('flex');
  });

  // Xử lý ô nhập dữ liệu gõ phím
  inputField?.addEventListener('input', () => {
    checkTypingMatch(false);
  });

  inputField?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      checkTypingMatch(true);
    }
  });

  // Nút bật/tắt âm thanh
  btnToggleSound?.addEventListener('click', () => {
    sounds.enabled = !sounds.enabled;
    if (iconSoundState) {
      iconSoundState.textContent = sounds.enabled ? 'volume_up' : 'volume_off';
      iconSoundState.className = sounds.enabled 
        ? 'material-symbols-outlined text-[20px]' 
        : 'material-symbols-outlined text-[20px] text-rose-500';
    }
    showToast(sounds.enabled ? 'Đã bật âm thanh trò chơi.' : 'Đã tắt âm thanh.', 'info');
  });

  // Chọn thời lượng
  durationBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      durationBtns.forEach(b => {
        b.classList.remove('active', 'bg-[#006194]', 'text-white', 'font-bold');
        b.classList.add('bg-[#FAF8F2]', 'text-[#3F4850]', 'font-semibold');
      });
      btn.classList.add('active', 'bg-[#006194]', 'text-white', 'font-bold');
      btn.classList.remove('bg-[#FAF8F2]', 'text-[#3F4850]', 'font-semibold');
      selectedDuration = parseInt(btn.dataset.duration, 10) || 60;
      remainingTime = selectedDuration;
      updateHud();
    });
  });

  // Chọn tốc độ rơi
  speedBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      speedBtns.forEach(b => {
        b.classList.remove('active', 'bg-[#006194]', 'text-white', 'font-bold');
        b.classList.add('bg-[#FAF8F2]', 'text-[#3F4850]', 'font-semibold');
      });
      btn.classList.add('active', 'bg-[#006194]', 'text-white', 'font-bold');
      btn.classList.remove('bg-[#FAF8F2]', 'text-[#3F4850]', 'font-semibold');
      speedMultiplier = parseFloat(btn.dataset.speed) || 1;
    });
  });
});
