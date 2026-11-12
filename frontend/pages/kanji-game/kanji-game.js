/**
 * KizunaTutor - Game Mưa Chữ Hán (Kanji Rain Typing)
 * Feature-Based Architecture: frontend/pages/kanji-game/
 * Tuân thủ nghiêm ngặt FRONTEND_RULES.md (Zero mock/fake data)
 * State Machine: 'LOBBY' | 'PLAYING' | 'GAMEOVER'
 */

import { debounce, showToast } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { apiClient, extractKanaReadingsFromText } from '/js/apiClient.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'kanji-game',
  pageTitle: 'Game Mưa chữ Hán',
  badge: 'Đấu trường phản xạ'
});

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
    ctx.save();

    // 1. Há»™p bao quanh máº·t chá»¯ (Rounded Glass Card)
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

    // Táº¯t bÃ³ng Ä‘á»• Ä‘á»ƒ váº½ chá»¯ sáº¯c nÃ©t
    ctx.shadowColor = 'transparent';

    if (showAnswerHint) {
      // 2. Váº½ máº·t chá»¯ HÃ¡n to á»Ÿ ná»­a trÃªn khi cÃ³ gá»£i Ã½ Ä‘Ã¡p Ã¡n
      ctx.fillStyle = '#FFFFFF';
      ctx.font = '900 28px "Noto Serif JP", serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(this.character, this.x + this.width / 2, this.y + 26);

      // 3. Huy hiá»‡u cÃ¡ch Ä‘á»c gá»£i Ã½ (Primary Reading Badge)
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
    } else {
      // Khi áº©n Ä‘Ã¡p Ã¡n: Váº½ máº·t chá»¯ HÃ¡n to á»Ÿ chÃ­nh giá»¯a tháº» card
      ctx.fillStyle = '#FFFFFF';
      ctx.font = '900 32px "Noto Serif JP", serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(this.character, this.x + this.width / 2, this.y + this.height / 2);
    }

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
    this.isTargeted = false; // Trạng thái đang được người chơi nhắm gõ

    this.width = 68;
    this.height = 76;
    const padding = 15;
    this.x = Math.random() * (Math.max(100, canvasWidth - this.width - padding * 2)) + padding;
    this.y = -this.height;
    
    // Tốc độ cơ bản: 35 đến 60 px/giây nhân theo hệ số người chơi chọn
    this.baseSpeed = (Math.random() * 25 + 35) * speedMultiplier;

    // Tổng hợp tất cả các chìa khóa tìm kiếm (Hiragana, Katakana, Romaji, Hán-Việt có/không dấu)
    this.searchKeys = [];
    const pool = [...this.readings];
    if (this.primaryReading) pool.push(this.primaryReading);
    if (this.hanViet) pool.push(this.hanViet);

    pool.forEach(raw => {
      if (!raw) return;
      const norm = normalizeInput(raw);
      if (norm) {
        this.searchKeys.push(norm);
        const rom = hiraganaToRomaji(norm);
        if (rom) this.searchKeys.push(rom);
      }
      const noAcc = removeAccents(raw.trim().toLowerCase());
      if (noAcc) this.searchKeys.push(noAcc);
      const rawLower = raw.trim().toLowerCase();
      if (rawLower) this.searchKeys.push(rawLower);
    });

    this.searchKeys = Array.from(new Set(this.searchKeys.filter(Boolean)));
  }

  update(dt) {
    this.y += this.baseSpeed * dt;
  }

  draw(ctx) {
    ctx.save();

    const radius = 12;

    if (this.isTargeted) {
      // 1. LÀM NỔI BẬT CHỮ ĐANG GÕ (SHODO CRIMSON + NEON GLOW)
      ctx.shadowColor = '#F43F5E';
      ctx.shadowBlur = 18;
      ctx.shadowOffsetY = 0;

      // Nền thẻ đỏ rượu đậm, viền hồng sáng nổi bật
      ctx.fillStyle = '#881337';
      ctx.strokeStyle = '#FFE4E6';
      ctx.lineWidth = 3;

      // Phóng to nhẹ thẻ (scale 1.08) để nhô lên so với các chữ khác
      const scaleW = this.width * 1.08;
      const scaleH = this.height * 1.08;
      const offsetX = (scaleW - this.width) / 2;
      const offsetY = (scaleH - this.height) / 2;

      ctx.beginPath();
      ctx.roundRect(this.x - offsetX, this.y - offsetY, scaleW, scaleH, radius);
      ctx.fill();
      ctx.stroke();

      // Viền phụ tăng chiều sâu
      ctx.strokeStyle = '#FB7185';
      ctx.lineWidth = 1;
      ctx.stroke();
    } else {
      // Thẻ bình thường: mờ nhẹ nếu đang có 1 chữ khác được nhắm gõ
      const hasAnyTarget = fallingItems.some(it => it.isTargeted);
      if (hasAnyTarget) {
        ctx.globalAlpha = 0.55;
      }

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
    }

    // Tắt bóng đổ để vẽ chữ sắc nét
    ctx.shadowColor = 'transparent';

    if (showAnswerHint) {
      // 2. Vẽ mặt chữ Hán
      ctx.fillStyle = this.isTargeted ? '#FFF1F2' : '#FFFFFF';
      ctx.font = this.isTargeted ? '900 30px "Noto Serif JP", serif' : '900 28px "Noto Serif JP", serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(this.character, this.x + this.width / 2, this.y + 26);

      // 3. Huy hiệu cách đọc gợi ý (Primary Reading Badge)
      const badgeY = this.y + this.height - 18;
      if (this.isTargeted) {
        ctx.fillStyle = '#FFE4E6';
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.roundRect(this.x + 4, badgeY - 10, this.width - 8, 20, 6);
        ctx.fill();
        ctx.stroke();

        ctx.fillStyle = '#9F1239';
        ctx.font = 'bold 12px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(this.primaryReading || this.hanViet, this.x + this.width / 2, badgeY);
      } else {
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
      }
    } else {
      // Khi ẩn đáp án: Vẽ mặt chữ Hán to ở chính giữa thẻ card
      ctx.fillStyle = this.isTargeted ? '#FFE4E6' : '#FFFFFF';
      ctx.font = this.isTargeted ? '900 34px "Noto Serif JP", serif' : '900 32px "Noto Serif JP", serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(this.character, this.x + this.width / 2, this.y + this.height / 2);
    }

    ctx.restore();
  }
}
// =========================================================================
// 4. QUẢN LÝ TRẠNG THÁI CHÍNH (STATE MACHINE: LOBBY | PLAYING | GAMEOVER)
// =========================================================================
let currentGameState = 'LOBBY';

// DOM References - Screens
const screenLobby = document.getElementById('screenLobby');
const screenPlaying = document.getElementById('screenPlaying');
const screenGameOver = document.getElementById('screenGameOver');

// DOM References - Screen 1: Lobby
const btnStartFromLobby = document.getElementById('btnStartFromLobby');
const selectGameSource = document.getElementById('selectGameSource');
const optgroupFlashcardDecks = document.getElementById('optgroupFlashcardDecks');
const deckInfoBanner = document.getElementById('deckInfoBanner');
const deckInfoTitle = document.getElementById('deckInfoTitle');
const deckInfoCount = document.getElementById('deckInfoCount');
const durationBtns = document.querySelectorAll('.duration-btn');
const speedBtns = document.querySelectorAll('.speed-btn');
const hintBtns = document.querySelectorAll('.hint-btn');
const leaderboardList = document.getElementById('leaderboardList');

// DOM References - Screen 2: Gameplay
const canvas = document.getElementById('kanjiRainCanvas');
const ctx = canvas?.getContext('2d');
const inputField = document.getElementById('kanjiGameInput');
const liveTypingEcho = document.getElementById('liveTypingEcho');
const btnClearInput = document.getElementById('btnClearInput');
const btnQuitGame = document.getElementById('btnQuitGame');
const btnToggleSound = document.getElementById('btnToggleSound');
const iconSoundState = document.getElementById('iconSoundState');

// HUD
const hudScoreText = document.getElementById('hudScoreText');
const hudComboBadge = document.getElementById('hudComboBadge');
const hudTimeText = document.getElementById('hudTimeText');
const hudWordsCount = document.getElementById('hudWordsCount');
const hudAccuracy = document.getElementById('hudAccuracy');

// Quit Confirmation Modal
const quitConfirmModal = document.getElementById('quitConfirmModal');
const btnCancelQuit = document.getElementById('btnCancelQuit');
const btnConfirmQuit = document.getElementById('btnConfirmQuit');

// DOM References - Screen 3: Game Over
const resultBanner = document.getElementById('resultBanner');
const resultIcon = document.getElementById('resultIcon');
const resultTitle = document.getElementById('resultTitle');
const resultSubtitle = document.getElementById('resultSubtitle');
const resultRankTitle = document.getElementById('resultRankTitle');
const resultFinalScore = document.getElementById('resultFinalScore');
const resultFinalWords = document.getElementById('resultFinalWords');
const resultFinalAccuracy = document.getElementById('resultFinalAccuracy');
const resultFinalMistakes = document.getElementById('resultFinalMistakes');
const resultMistakesSection = document.getElementById('resultMistakesSection');
const resultMistakesList = document.getElementById('resultMistakesList');
const btnResultPlayAgain = document.getElementById('btnResultPlayAgain');
const btnResultChangeSettings = document.getElementById('btnResultChangeSettings');

// Sound Effects Engine
const sounds = new SoundEffects();

// Game State Variables
let isRunning = false;
let gamePool = [];
let fallingItems = [];
let particles = [];
let lastTimestamp = 0;
let spawnTimer = 0;
let spawnInterval = 2.4; // giây

let selectedDuration = 60; // giây (60, 180, 300)
let remainingTime = 60;
let speedMultiplier = 1; // 1 (chill), 1.5 (standard), 2.2 (blitz)
let showAnswerHint = false; // Máº·c Ä‘á»‹nh khÃ´ng hiá»ƒn thá»‹ Ä‘Ã¡p Ã¡n (theo yÃªu cáº§u thá»­ thÃ¡ch)

let score = 0;
let combo = 0;
let maxCombo = 0;
let wordsTyped = 0;
let totalKeystrokes = 0;
let totalMistakes = 0;
let mistakeMap = new Map(); // KanjiId -> { char, reading, count, typed }

/**
 * Điều phối luồng chuyển đổi trạng thái giữa 3 màn hình
 * @param {'LOBBY' | 'PLAYING' | 'GAMEOVER'} state
 */
function setGameState(state) {
  currentGameState = state;

  if (screenLobby) screenLobby.classList.toggle('hidden', state !== 'LOBBY');
  if (screenPlaying) screenPlaying.classList.toggle('hidden', state !== 'PLAYING');
  if (screenGameOver) screenGameOver.classList.toggle('hidden', state !== 'GAMEOVER');

  // Đóng modal xác nhận thoát nếu đang mở
  quitConfirmModal?.classList.add('hidden');
  quitConfirmModal?.classList.remove('flex');

  if (state === 'PLAYING') {
    resizeCanvas();
    setTimeout(() => {
      if (inputField) {
        inputField.value = '';
        inputField.focus();
      }
    }, 60);
  } else if (state === 'LOBBY') {
    isRunning = false;
    loadLeaderboard();
  }
}

// =========================================================================
// 5. RESIZE & CANVAS RESPONSIVENESS
// =========================================================================
function resizeCanvas() {
  if (!canvas || !ctx) return;
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
const ROMAJI_TO_HIRAGANA = {
  'a': 'あ', 'i': 'い', 'u': 'う', 'e': 'え', 'o': 'お',
  'ka': 'か', 'ki': 'き', 'ku': 'く', 'ke': 'け', 'ko': 'こ',
  'sa': 'さ', 'shi': 'し', 'si': 'し', 'su': 'す', 'se': 'せ', 'so': 'そ',
  'ta': 'た', 'chi': 'ち', 'ti': 'ち', 'tsu': 'つ', 'tu': 'つ', 'te': 'て', 'to': 'と',
  'na': 'な', 'ni': 'に', 'nu': 'ぬ', 'ne': 'ね', 'no': 'の',
  'ha': 'は', 'hi': 'ひ', 'fu': 'ふ', 'hu': 'ふ', 'he': 'へ', 'ho': 'ほ',
  'ma': 'ま', 'mi': 'み', 'mu': 'む', 'me': 'め', 'mo': 'も',
  'ya': 'や', 'yu': 'ゆ', 'yo': 'よ',
  'ra': 'ら', 'ri': 'り', 'ru': 'る', 're': 'れ', 'ro': 'ろ',
  'wa': 'わ', 'wo': 'を', 'nn': 'ん', "n'": 'ん',
  'ga': 'が', 'gi': 'ぎ', 'gu': 'ぐ', 'ge': 'げ', 'go': 'ご',
  'za': 'ざ', 'ji': 'じ', 'zi': 'じ', 'zu': 'ず', 'ze': 'ぜ', 'zo': 'ぞ',
  'da': 'だ', 'di': 'ぢ', 'du': 'づ', 'de': 'で', 'do': 'ど',
  'ba': 'ば', 'bi': 'び', 'bu': 'ぶ', 'be': 'べ', 'bo': 'ぼ',
  'pa': 'ぱ', 'pi': 'ぴ', 'pu': 'ぷ', 'pe': 'ぺ', 'po': 'ぽ',
  'kya': 'きゃ', 'kyu': 'きゅ', 'kyo': 'きょ',
  'sha': 'しゃ', 'shu': 'しゅ', 'sho': 'しょ', 'sya': 'しゃ', 'syu': 'しゅ', 'syo': 'しょ',
  'cha': 'ちゃ', 'chu': 'ちゅ', 'cho': 'ちょ', 'tya': 'ちゃ', 'tyu': 'ちゅ', 'tyo': 'ちょ',
  'nya': 'にゃ', 'nyu': 'にゅ', 'nyo': 'にょ',
  'hya': 'ひゃ', 'hyu': 'ひゅ', 'hyo': 'ひょ',
  'mya': 'みゃ', 'myu': 'みゅ', 'myo': 'みょ',
  'rya': 'りゃ', 'ryu': 'りゅ', 'ryo': 'りょ',
  'gya': 'ぎゃ', 'gyu': 'ぎゅ', 'gyo': 'ぎょ',
  'ja': 'じゃ', 'ju': 'じゅ', 'jo': 'じょ', 'jya': 'じゃ', 'jyu': 'じゅ', 'jyo': 'じょ',
  'zya': 'じゃ', 'zyu': 'じゅ', 'zyo': 'じょ',
  'bya': 'びゃ', 'byu': 'びゅ', 'byo': 'びょ',
  'pya': 'ぴゃ', 'pyu': 'ぴゅ', 'pyo': 'ぴょ'
};

const HIRAGANA_TO_ROMAJI = {
  'あ':'a','い':'i','う':'u','え':'e','お':'o',
  'か':'ka','き':'ki','く':'ku','け':'ke','こ':'ko',
  'さ':'sa','し':'shi','す':'su','せ':'se','そ':'so',
  'た':'ta','ち':'chi','つ':'tsu','て':'te','と':'to',
  'な':'na','に':'ni','ぬ':'nu','ね':'ne','の':'no',
  'は':'ha','ひ':'hi','ふ':'fu','へ':'he','ほ':'ho',
  'ま':'ma','み':'mi','む':'mu','め':'me','も':'mo',
  'や':'ya','ゆ':'yu','よ':'yo',
  'ら':'ra','り':'ri','る':'ru','れ':'re','ろ':'ro',
  'わ':'wa','を':'wo','ん':'n',
  'が':'ga','ぎ':'gi','ぐ':'gu','げ':'ge','ご':'go',
  'ざ':'za','じ':'ji','ず':'zu','ぜ':'ze','ぞ':'zo',
  'だ':'da','ぢ':'ji','づ':'zu','で':'de','ど':'do',
  'ば':'ba','び':'bi','ぶ':'bu','べ':'be','ぼ':'bo',
  'ぱ':'pa','ぴ':'pi','ぷ':'pu','ぺ':'pe','ぽ':'po',
  'きゃ':'kya','きゅ':'kyu','きょ':'kyo',
  'しゃ':'sha','しゅ':'shu','しょ':'sho',
  'ちゃ':'cha','ちゅ':'chu','ちょ':'cho',
  'にゃ':'nya','にゅ':'nyu','にょ':'nyo',
  'ひゃ':'hya','ひゅ':'hyu','ひょ':'hyo',
  'みゃ':'mya','みゅ':'myu','みょ':'myo',
  'りゃ':'rya','りゅ':'ryu','りょ':'ryo',
  'ぎゃ':'gya','ぎゅ':'gyu','ぎょ':'gyo',
  'じゃ':'ja','じゅ':'ju','じょ':'jo',
  'びゃ':'bya','びゅ':'byu','びょ':'byo',
  'ぴゃ':'pya','ぴゅ':'pyu','ぴょ':'pyo'
};

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

function romajiToHiragana(romaji) {
  if (!romaji) return '';
  let str = romaji.toLowerCase().trim();
  let result = '';
  let i = 0;
  while (i < str.length) {
    if (i + 1 < str.length && str[i] === str[i + 1] && !'aeiou'.includes(str[i]) && str[i] !== 'n') {
      result += 'っ';
      i++;
      continue;
    }
    let matched = false;
    for (let len = 4; len >= 1; len--) {
      if (i + len <= str.length) {
        const sub = str.slice(i, i + len);
        if (ROMAJI_TO_HIRAGANA[sub]) {
          result += ROMAJI_TO_HIRAGANA[sub];
          i += len;
          matched = true;
          break;
        }
      }
    }
    if (!matched) {
      if (str[i] === 'n' && (i + 1 === str.length || !'aeiouy'.includes(str[i + 1]))) {
        result += 'ん';
      } else {
        result += str[i];
      }
      i++;
    }
  }
  return result;
}

function hiraganaToRomaji(hira) {
  if (!hira) return '';
  let str = katakanaToHiragana(hira.toLowerCase().trim());
  let res = '';
  let i = 0;
  while (i < str.length) {
    if (str[i] === 'っ') {
      if (i + 1 < str.length) {
        const nextSub = str.slice(i + 1, i + 3);
        const nextRom = HIRAGANA_TO_ROMAJI[nextSub] || HIRAGANA_TO_ROMAJI[str[i + 1]] || '';
        if (nextRom) {
          res += nextRom[0];
        }
      }
      i++;
      continue;
    }
    if (i + 1 < str.length) {
      const sub2 = str.slice(i, i + 2);
      if (HIRAGANA_TO_ROMAJI[sub2]) {
        res += HIRAGANA_TO_ROMAJI[sub2];
        i += 2;
        continue;
      }
    }
    const sub1 = str[i];
    if (HIRAGANA_TO_ROMAJI[sub1]) {
      res += HIRAGANA_TO_ROMAJI[sub1];
    } else {
      res += sub1;
    }
    i++;
  }
  return res;
}
// =========================================================================
// 7. TRUY VẤN DỮ LIỆU CHỮ HÁN CHO TRẬN ĐẤU (ZERO MOCK DATA)
// =========================================================================
/**
 * Bóc tách sạch mọi cách đọc Kana hợp lệ (Hiragana/Katakana) và âm Hán-Việt từ thẻ 2 mặt hoặc đa cột,
 * loại bỏ hoàn toàn phần nghĩa tiếng Việt / dấu phân cách (•, -, ,) phía sau.
 */
function mapDeckCardToGameEntry(c, index) {
  const character = String(c.char || c.front || c.word || '?').trim();
  const meaning = String(c.meaning || c.back || '').trim();
  const explicitHanViet = String(c.hanViet || '').trim();

  // Gom các cụm Kana thuần túy từ trường đọc riêng lẫn mặt sau (VD: "あおい • Xanh lam" -> "あおい")
  const kanaPool = [
    ...extractKanaReadingsFromText(c.reading || ''),
    ...extractKanaReadingsFromText(c.furigana || ''),
    ...extractKanaReadingsFromText(c.kunyomi || ''),
    ...extractKanaReadingsFromText(c.onyomi || ''),
    ...extractKanaReadingsFromText(meaning)
  ];

  // Đề xuất 3 (Fallback): nếu thẻ chưa có Kana ở mặt sau, lấy Kana từ mặt trước (VD: あめ, こんにちは)
  if (kanaPool.length === 0) {
    kanaPool.push(...extractKanaReadingsFromText(character));
  }

  const normalizedKanaReadings = Array.from(new Set(
    kanaPool.map(k => normalizeInput(k)).filter(Boolean)
  ));

  // Gom âm Hán Việt (tách theo dấu phẩy / gạch ngang nếu có nhiều âm)
  const hvTokens = explicitHanViet
    ? explicitHanViet.split(/[,;/|•]+/).map(s => s.trim()).filter(Boolean)
    : [];

  // Bổ sung nhận diện từ viết HOA toàn bộ trong mặt sau thẻ từ điển (VD: "セイ, ショウ, あお, THANH, Xanh")
  if (hvTokens.length === 0 && meaning) {
    meaning.split(/[,;/•\n]+/).forEach(part => {
      const cleaned = part.trim();
      if (/^[A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯẠ-Ỹ\s]{2,20}$/.test(cleaned)) {
        hvTokens.push(cleaned);
      }
    });
  }

  const hvReadings = [];
  hvTokens.forEach(hv => {
    const low = hv.toLowerCase();
    const noAcc = removeAccents(low);
    if (low) hvReadings.push(low);
    if (noAcc) hvReadings.push(noAcc);
  });

  // Fallback cuối cùng nếu thẻ không có cả Kana lẫn Hán-Việt (thẻ thuần nghĩa)
  const fallbackReadings = [];
  if (normalizedKanaReadings.length === 0 && hvReadings.length === 0) {
    const firstMeaningSegment = meaning.split(/[•\-/,;(\n]/)[0].trim().toLowerCase();
    if (firstMeaningSegment) {
      fallbackReadings.push(firstMeaningSegment, removeAccents(firstMeaningSegment));
    }
    if (character) {
      fallbackReadings.push(character.toLowerCase());
    }
  }

  const allReadings = Array.from(new Set([
    ...normalizedKanaReadings,
    ...hvReadings,
    ...fallbackReadings
  ].filter(Boolean)));

  const primaryReading = normalizedKanaReadings[0] || hvTokens[0] || allReadings[0] || '';

  return {
    kanjiId: `deck-card-${index}`,
    character,
    hanViet: hvTokens[0] || explicitHanViet,
    meaning,
    jlptLevel: c.jlpt || c.level || 'N5',
    primaryReading,
    readings: allReadings
  };
}

async function loadGameData() {
  const source = selectGameSource.value;

  // Nếu người dùng chọn một bộ thẻ cá nhân từ Flashcards
  if (source.startsWith('deck:')) {
    const deckId = source.replace('deck:', '');
    let decks = [];
    try {
      decks = await apiClient.getDecks();
    } catch (e) {
      const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
      try { decks = rawDecks ? JSON.parse(rawDecks) : []; } catch (_) { decks = []; }
    }

    const targetDeck = decks.find(d => String(d.id) === String(deckId));
    if (targetDeck && targetDeck.cards && targetDeck.cards.length > 0) {
      gamePool = targetDeck.cards.map((c, i) => mapDeckCardToGameEntry(c, i));
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

// Nạp danh sách bộ thẻ cá nhân vào dropdown và cập nhật banner nếu có
async function populatePersonalDecksDropdown() {
  let decks = [];
  try {
    decks = await apiClient.getDecks();
  } catch (e) {
    const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
    try { decks = rawDecks ? JSON.parse(rawDecks) : []; } catch (_) { decks = []; }
  }

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

function updateDeckInfoBanner() {
  const urlParams = new URLSearchParams(window.location.search);
  const targetDeckId = urlParams.get('deckId') || urlParams.get('id');
  const sourceWrapper = document.getElementById('sourceSelectionWrapper');
  if (targetDeckId) {
    if (sourceWrapper) sourceWrapper.classList.add('hidden');
    if (deckInfoBanner) deckInfoBanner.classList.add('hidden');
    return;
  }

  const source = selectGameSource ? selectGameSource.value : '';
  if (source.startsWith('deck:')) {
    const deckId = source.replace('deck:', '');
    const rawDecks = localStorage.getItem('kizuna_flashcard_decks');
    let decks = [];
    try { decks = rawDecks ? JSON.parse(rawDecks) : []; } catch (e) { decks = []; }
    const deck = decks.find(d => String(d.id) === String(deckId));
    if (deck) {
      if (deckInfoBanner) deckInfoBanner.classList.remove('hidden');
      if (deckInfoTitle) deckInfoTitle.textContent = deck.title || 'Bộ Thẻ Cá Nhân';
      if (deckInfoCount) deckInfoCount.textContent = `${(deck.cards || []).length} thẻ`;
      return;
    }
  }
  if (deckInfoBanner) deckInfoBanner.classList.add('hidden');
}

// =========================================================================
// 8. BẮT ĐẦU VÁN ĐẤU (GAME INITIALIZATION & LOOP)
// =========================================================================
async function startGame() {
  const hasData = await loadGameData();
  if (!hasData || gamePool.length === 0) return;

  // Chuyển sang màn hình chơi game
  setGameState('PLAYING');

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
  if (gamePool.length === 0 || !canvas) return;
  const randIndex = Math.floor(Math.random() * gamePool.length);
  const data = gamePool[randIndex];
  const rect = canvas.getBoundingClientRect();
  const item = new FallingKanji(data, rect.width, speedMultiplier);
  fallingItems.push(item);
}

function gameLoop(timestamp) {
  if (!isRunning || currentGameState !== 'PLAYING') return;

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

  // 2. Cập nhật & Vẽ các chữ đang rơi (Vẽ chữ đang gõ đè lên trên cùng)
  const normalItems = [];
  let targetItem = null;
  for (let i = fallingItems.length - 1; i >= 0; i--) {
    const item = fallingItems[i];
    item.update(dt);
    if (item.isTargeted) {
      targetItem = item;
    } else {
      normalItems.push(item);
    }

    // Kiểm tra chạm vạch Deadline
    if (item.y + item.height >= dangerY) {
      endGame(false, item);
      return;
    }
  }

  // Vẽ các chữ bình thường trước
  for (let i = 0; i < normalItems.length; i++) {
    normalItems[i].draw(ctx);
  }
  // Vẽ chữ đang nhắm tới sau cùng để luôn hiển thị nổi bật trên bề mặt
  if (targetItem) {
    targetItem.draw(ctx);
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
/**
 * Cập nhật trạng thái target của từng chữ rơi dựa trên tiền tố người chơi đang gõ
 */
function updateTargetKanji() {
  const rawValue = inputField?.value?.trim() || '';
  if (!rawValue) {
    for (let i = 0; i < fallingItems.length; i++) {
      fallingItems[i].isTargeted = false;
    }
    return null;
  }

  const inputNorm = normalizeInput(rawValue);
  const inputNoAccent = removeAccents(inputNorm);
  const inputRomHira = romajiToHiragana(inputNorm);

  let bestTarget = null;
  let maxY = -999;

  for (let i = 0; i < fallingItems.length; i++) {
    const item = fallingItems[i];
    item.isTargeted = false;

    const isMatchPrefix = item.searchKeys && item.searchKeys.some(k => {
      return k.startsWith(inputNorm) ||
             k.startsWith(inputNoAccent) ||
             (inputRomHira && k.startsWith(inputRomHira));
    });

    if (isMatchPrefix && item.y > maxY) {
      maxY = item.y;
      bestTarget = item;
    }
  }

  if (bestTarget) {
    bestTarget.isTargeted = true;
  }
  return bestTarget;
}

function checkTypingMatch(isEnterCommit = false) {
  if (!inputField) return;
  const rawValue = inputField.value;
  const trimmed = rawValue.trim();
  const inputNorm = normalizeInput(trimmed);
  const inputNoAccent = removeAccents(inputNorm);
  const inputRomHira = romajiToHiragana(inputNorm);

  if (liveTypingEcho) {
    liveTypingEcho.textContent = trimmed || '...';
  }

  // Luôn cập nhật chữ đang nhắm tới theo thời gian thực
  updateTargetKanji();

  if (!inputNorm) {
    for (let i = 0; i < fallingItems.length; i++) {
      fallingItems[i].isTargeted = false;
    }
    return;
  }

  // 1. Tìm chữ rơi ở vị trí thấp nhất khớp chính xác
  let lowestMatchIndex = -1;
  let maxMatchY = -999;

  for (let i = 0; i < fallingItems.length; i++) {
    const item = fallingItems[i];
    const isMatched = item.searchKeys && item.searchKeys.some(k => {
      return k === inputNorm || 
             k === inputNoAccent || 
             (inputRomHira && k === inputRomHira);
    });

    if (isMatched && item.y > maxMatchY) {
      maxMatchY = item.y;
      lowestMatchIndex = i;
    }
  }

  // 2. NẾU KHỚP: Tiêu diệt chữ + Cộng điểm + TỰ ĐỘNG LÀM SẠCH Ô GÕ
  if (lowestMatchIndex >= 0) {
    const hitItem = fallingItems[lowestMatchIndex];

    sounds.playDing();
    for (let p = 0; p < 20; p++) {
      particles.push(new SparkleParticle(hitItem.x + hitItem.width / 2, hitItem.y + hitItem.height / 2));
    }

    fallingItems.splice(lowestMatchIndex, 1);

    combo++;
    if (combo > maxCombo) maxCombo = combo;
    const comboBonus = (combo - 1) * 20;
    score += (100 + comboBonus);
    wordsTyped++;

    // Tự động làm sạch ô gõ ngay lập tức
    inputField.value = '';
    if (liveTypingEcho) liveTypingEcho.textContent = '...';

    // Xóa target sau khi nổ
    for (let i = 0; i < fallingItems.length; i++) {
      fallingItems[i].isTargeted = false;
    }

    updateHud();
    return;
  }

  // 3. NẾU BẤM ENTER HOẶC SPACE MÀ KHÔNG KHỚP: Ghi nhận sai + Rung đỏ + TỰ ĐỘNG LÀM SẠCH Ô GÕ
  if (isEnterCommit && trimmed.length > 0) {
    sounds.playBuzz();
    combo = 0;
    totalMistakes++;

    if (fallingItems.length > 0) {
      const lowestItem = fallingItems.reduce((prev, curr) => (curr.y > prev.y ? curr : prev));
      const entry = mistakeMap.get(lowestItem.character) || {
        kanjiId: lowestItem.id,
        char: lowestItem.character,
        reading: lowestItem.primaryReading,
        meaning: lowestItem.meaning,
        hanViet: lowestItem.hanViet,
        count: 0,
        typed: trimmed
      };
      entry.count++;
      entry.typed = trimmed;
      mistakeMap.set(lowestItem.character, entry);
    }

    // Hiệu ứng rung đỏ báo lỗi
    inputField.classList.add('shake-error');
    setTimeout(() => {
      inputField.classList.remove('shake-error');
    }, 260);

    // TỰ ĐỘNG LÀM SẠCH Ô GÕ ĐỂ NGƯỜI DÙNG TIẾP TỤC KHÔNG CẦN BACKSPACE
    inputField.value = '';
    if (liveTypingEcho) liveTypingEcho.textContent = '...';

    for (let i = 0; i < fallingItems.length; i++) {
      fallingItems[i].isTargeted = false;
    }

    updateHud();
  }
}
// =========================================================================
// 11. KẾT THÚC TRẬN ĐẤU (GAME OVER / RESULT SCREEN)
// =========================================================================
async function endGame(isWon, failedKanji = null) {
  isRunning = false;

  const totalAttempts = wordsTyped + totalMistakes;
  const accuracy = totalAttempts > 0 ? Number(((wordsTyped / totalAttempts) * 100).toFixed(1)) : 100.0;
  const durationPlayed = Math.round(selectedDuration - remainingTime);

  if (isWon) {
    sounds.playVictory();
    if (resultIcon) {
      resultIcon.textContent = '🏆';
      resultIcon.className = 'w-16 h-16 rounded-2xl mx-auto flex items-center justify-center text-3xl bg-amber-50 text-amber-600 border border-amber-200 shadow-sm';
    }
    if (resultTitle) resultTitle.textContent = 'Bảo Vệ Thành Công!';
    if (resultSubtitle) resultSubtitle.textContent = 'Bạn đã sống sót qua toàn bộ thời gian thử thách';
  } else {
    sounds.playGameOver();
    if (resultIcon) {
      resultIcon.textContent = '💥';
      resultIcon.className = 'w-16 h-16 rounded-2xl mx-auto flex items-center justify-center text-3xl bg-rose-50 text-rose-600 border border-rose-200 shadow-sm';
    }
    if (resultTitle) resultTitle.textContent = 'Chữ Đã Chạm Vạch Ranh Giới!';
    if (resultSubtitle) resultSubtitle.textContent = failedKanji ? `Chữ "${failedKanji.character}" (${failedKanji.primaryReading}) đã chạm vạch đỏ!` : 'Chữ Hán đã chạm vào vạch nguy hiểm';
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
  if (resultRankTitle) resultRankTitle.textContent = rank;

  if (resultFinalScore) resultFinalScore.textContent = score;
  if (resultFinalWords) resultFinalWords.textContent = `${wordsTyped} chữ`;
  if (resultFinalAccuracy) resultFinalAccuracy.textContent = `${accuracy}%`;
  if (resultFinalMistakes) resultFinalMistakes.textContent = totalMistakes;

  // Hiển thị danh sách từ sai
  const mistakesArray = Array.from(mistakeMap.values());
  if (failedKanji && !mistakeMap.has(failedKanji.character)) {
    mistakesArray.unshift({
      kanjiId: failedKanji.id,
      char: failedKanji.character,
      reading: failedKanji.primaryReading,
      meaning: failedKanji.meaning,
      hanViet: failedKanji.hanViet,
      count: 1,
      typed: 'Chạm vạch đỏ'
    });
  }

  if (mistakesArray.length > 0) {
    resultMistakesSection?.classList.remove('hidden');
    if (resultMistakesList) {
      resultMistakesList.innerHTML = mistakesArray.map(m => `
        <div class="flex items-center justify-between p-2.5 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] text-xs">
          <div class="flex items-center gap-2.5">
            <span class="w-9 h-9 rounded-lg bg-white border border-[#ECE8DE] flex items-center justify-center font-japanese font-bold text-lg text-[#0F172A] shadow-2xs">
              ${m.char}
            </span>
            <div>
              <div class="flex items-center gap-1.5">
                <span class="font-bold text-[#006194] text-xs">${m.reading}</span>
                ${m.hanViet ? `<span class="text-[10px] text-slate-500 font-semibold">• Hán-Việt: ${m.hanViet}</span>` : ''}
              </div>
              <span class="text-[10px] text-[#64748B] block">${m.meaning ? `Nghĩa: ${m.meaning} • ` : ''}Lỗi: ${m.typed || 'Bỏ lỡ'} (${m.count} lần)</span>
            </div>
          </div>
          <div class="flex items-center gap-1.5 shrink-0">
            <a href="/shodo?char=${encodeURIComponent(m.char)}"
               class="px-2.5 py-1.5 rounded-lg bg-white border border-[#CBD5E1] hover:bg-slate-50 text-[#1E293B] font-semibold text-[11px] no-underline transition shadow-2xs flex items-center gap-1">
              <span>🖌️</span>
              <span>Luyện viết</span>
            </a>
            <a href="/kanji-dictionary?q=${encodeURIComponent(m.char)}"
               class="px-2.5 py-1.5 rounded-lg bg-white border border-[#CBD5E1] hover:bg-slate-50 text-[#1E293B] font-semibold text-[11px] no-underline transition shadow-2xs flex items-center gap-1">
              <span>📚</span>
              <span>Từ điển</span>
            </a>
          </div>
        </div>
      `).join('');
    }
  } else {
    resultMistakesSection?.classList.add('hidden');
  }

  // Chuyển State Machine sang Màn hình 3 (Báo cáo kết quả)
  setGameState('GAMEOVER');

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
              <div class="flex items-center gap-2.5">
                <span class="w-6 text-center font-bold text-sm">${medal}</span>
                <div>
                  <div class="font-bold text-[#0F172A]">${item.titleRank || 'Chiến Binh'}</div>
                  <div class="text-[10px] text-[#64748B]">${item.wordsTypedCount} chữ • Chính xác ${item.accuracy}%</div>
                </div>
              </div>
              <span class="font-extrabold text-[#831843] text-sm">${item.score} đ</span>
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
    <div class="py-6 text-center text-xs text-[#64748B] space-y-2">
      <span class="material-symbols-outlined text-3xl text-slate-300">hourglass_empty</span>
      <p>Chưa có phiên chơi nào được lưu.</p>
    </div>
  `;
}

// =========================================================================
// 13. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN
// =========================================================================
document.addEventListener('DOMContentLoaded', async () => {
  await populatePersonalDecksDropdown();
  loadLeaderboard();

  // Tự động chọn bộ thẻ từ URL param (?deckId=... hoặc ?id=...)
  const urlParams = new URLSearchParams(window.location.search);
  const targetDeckId = urlParams.get('deckId') || urlParams.get('id');
  const sourceSelectionWrapper = document.getElementById('sourceSelectionWrapper');

  if (targetDeckId) {
    if (selectGameSource) {
      const deckVal = `deck:${targetDeckId}`;
      const exists = Array.from(selectGameSource.options).some(o => o.value === deckVal);
      if (exists) {
        selectGameSource.value = deckVal;
      } else {
        const opt = document.createElement('option');
        opt.value = deckVal;
        opt.textContent = `Bộ Thẻ Cá Nhân`;
        selectGameSource.appendChild(opt);
        selectGameSource.value = deckVal;
      }
    }
    // Ẩn hoàn toàn khối chọn kho chữ Hán & banner khi đã chọn từ trang trước
    if (sourceSelectionWrapper) sourceSelectionWrapper.classList.add('hidden');
    if (deckInfoBanner) deckInfoBanner.classList.add('hidden');
  } else {
    updateDeckInfoBanner();
    selectGameSource?.addEventListener('change', updateDeckInfoBanner);
  }

  // Màn hình 1 (Lobby): Nút Bắt đầu chơi ngay
  btnStartFromLobby?.addEventListener('click', startGame);

  // Màn hình 2 (Playing): Thoát trận & Modal xác nhận
  btnQuitGame?.addEventListener('click', () => {
    quitConfirmModal?.classList.remove('hidden');
    quitConfirmModal?.classList.add('flex');
  });

  btnCancelQuit?.addEventListener('click', () => {
    quitConfirmModal?.classList.add('hidden');
    quitConfirmModal?.classList.remove('flex');
    inputField?.focus();
  });

  btnConfirmQuit?.addEventListener('click', () => {
    quitConfirmModal?.classList.add('hidden');
    quitConfirmModal?.classList.remove('flex');
    isRunning = false;
    setGameState('LOBBY');
  });

  // Nút xóa nhanh input
  btnClearInput?.addEventListener('click', () => {
    if (inputField) {
      inputField.value = '';
      inputField.focus();
    }
    if (liveTypingEcho) liveTypingEcho.textContent = '...';
  });

  // Màn hình 3 (Result): Chơi lại & Thay đổi cài đặt
  btnResultPlayAgain?.addEventListener('click', startGame);
  btnResultChangeSettings?.addEventListener('click', () => {
    setGameState('LOBBY');
  });

  // Xử lý ô nhập dữ liệu gõ phím (Hỗ trợ tự động làm sạch + Space/Enter)
  inputField?.addEventListener('input', () => {
    // Nếu người dùng vừa gõ phím cách (Space) -> xem như kết thúc từ để commit
    if (inputField.value.endsWith(' ')) {
      inputField.value = inputField.value.trim();
      checkTypingMatch(true);
      return;
    }
    checkTypingMatch(false);
  });

  inputField?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
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
        ? 'material-symbols-outlined text-[18px]'
        : 'material-symbols-outlined text-[18px] text-rose-500';
    }
    showToast(sounds.enabled ? 'Đã bật âm thanh trò chơi.' : 'Đã tắt âm thanh.', 'info');
  });

  // Chọn thời lượng (60s, 180s, 300s)
  durationBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      durationBtns.forEach(b => {
        b.classList.remove('active', 'bg-[#9F1239]', 'text-white', 'shadow-xs');
        b.classList.add('bg-white', 'text-[#1E293B]', 'border-[#CBD5E1]');
      });
      btn.classList.add('active', 'bg-[#9F1239]', 'text-white', 'shadow-xs');
      btn.classList.remove('bg-white', 'text-[#1E293B]', 'border-[#CBD5E1]');
      selectedDuration = parseInt(btn.dataset.duration, 10) || 60;
      remainingTime = selectedDuration;
      updateHud();
    });
  });

  // Chọn tốc độ rơi (chill, standard, blitz)
  speedBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      speedBtns.forEach(b => {
        b.classList.remove('active', 'bg-[#9F1239]', 'text-white', 'shadow-xs');
        b.classList.add('bg-white', 'text-[#1E293B]', 'border-[#CBD5E1]');
      });
      btn.classList.add('active', 'bg-[#9F1239]', 'text-white', 'shadow-xs');
      btn.classList.remove('bg-white', 'text-[#1E293B]', 'border-[#CBD5E1]');
      speedMultiplier = parseFloat(btn.dataset.speed) || 1;
    });
  });

  // Khởi động ở màn hình Lobby
  // Chọn chế độ hiển thị đáp án (ẩn / hiện gợi ý)
  hintBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      hintBtns.forEach(b => {
        b.classList.remove('active', 'bg-[#9F1239]', 'text-white', 'shadow-xs');
        b.classList.add('bg-white', 'text-[#1E293B]', 'border-[#CBD5E1]');
      });
      btn.classList.add('active', 'bg-[#9F1239]', 'text-white', 'shadow-xs');
      btn.classList.remove('bg-white', 'text-[#1E293B]', 'border-[#CBD5E1]');
      showAnswerHint = btn.dataset.hint === 'true';
    });
  });
  setGameState('LOBBY');
});
