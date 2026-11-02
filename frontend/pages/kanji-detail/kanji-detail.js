/**
 * KizunaTutor - Trang Chi Tiết Chữ Hán Độc Lập
 * URL Route: /kanji-detail?char=...
 */

import { showToast } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// 1. Khởi tạo Shell học viên
initLearnerShell({
  activeMenu: 'dictionary',
  pageTitle: 'Chi Tiết Chữ Hán',
  badge: 'Tra cứu chuyên sâu'
});

const STORAGE_KEY = 'kizuna_flashcard_decks';
let currentChar = null;
let currentKanjiData = null;

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

// 4. Tải dữ liệu chữ Hán
async function loadKanjiDetail() {
  const urlParams = new URLSearchParams(window.location.search);
  currentChar = urlParams.get('char') || urlParams.get('kanji') || urlParams.get('c');
  const kanjiId = urlParams.get('id');

  if (!currentChar && !kanjiId) {
    showToast('Không tìm thấy thông tin chữ Hán cần tra cứu!', 'error');
    setTimeout(() => { window.location.href = '/handwriting'; }, 1500);
    return;
  }

  // 1. Gọi Backend API
  try {
    let url = currentChar
      ? `/api/learner/kanji/detail?char=${encodeURIComponent(currentChar)}`
      : `/api/learner/kanji/detail?id=${encodeURIComponent(kanjiId)}`;

    let res = await fetch(url).catch(() => null);
    if (!res || !res.ok) {
      let fbUrl = currentChar
        ? `/api/kanji/detail?char=${encodeURIComponent(currentChar)}`
        : `/api/kanji/detail?id=${encodeURIComponent(kanjiId)}`;
      res = await fetch(fbUrl).catch(() => null);
    }

    if (res && res.ok) {
      currentKanjiData = await res.json();
    } else if (currentChar) {
      // Thử fallback qua search
      const sRes = await fetch(`/api/learner/kanji/search?keyword=${encodeURIComponent(currentChar)}&size=1`).catch(() => null);
      if (sRes && sRes.ok) {
        const sData = await sRes.json();
        currentKanjiData = (sData.items || []).find(x => (x.kanjiChar === currentChar || x.character === currentChar)) || sData.items[0];
      }
    }
  } catch (e) {
    console.debug('Lỗi nạp API chi tiết:', e);
  }

  // 2. Dự phòng kiểm tra localStorage 'selected_kanji_detail'
  if (!currentKanjiData && currentChar) {
    try {
      const cached = JSON.parse(localStorage.getItem('selected_kanji_detail') || '{}');
      if (cached && (cached.character === currentChar || cached.kanjiChar === currentChar)) {
        currentKanjiData = cached;
      }
    } catch (e) {}
  }

  if (!currentKanjiData && currentChar) {
    currentKanjiData = {
      kanjiCharacter: currentChar,
      kanjiChar: currentChar,
      character: currentChar,
      hanViet: 'HÁN TỰ',
      meaning: 'Chữ Hán tra cứu',
      onyomi: '—',
      kunyomi: '—',
      strokeCount: 1,
      radical: '—',
      jlptLevel: 'N/A'
    };
  }

  renderDetailView();
}

// 5. Hiển thị chi tiết chữ Hán
function renderDetailView() {
  if (!currentKanjiData) return;

  const char = currentKanjiData.kanjiCharacter || currentKanjiData.kanjiChar || currentKanjiData.character || currentChar || '—';
  const hanViet = (currentKanjiData.hanViet || 'HÁN TỰ').toUpperCase();
  const meaning = currentKanjiData.meaning || currentKanjiData.vietnameseMeaning || 'Chưa cập nhật nghĩa chi tiết.';
  const onyomi = currentKanjiData.onyomi || '—';
  const kunyomi = currentKanjiData.kunyomi || '—';
  const strokes = currentKanjiData.strokeCount || currentKanjiData.totalStrokes || '—';
  const radical = currentKanjiData.radical || '—';
  const jlpt = currentKanjiData.jlptLevel || currentKanjiData.jlpt || 'Tự do';
  const hex = currentKanjiData.unicodeHex ? `U+${currentKanjiData.unicodeHex.toUpperCase()}` : (char && char !== '—' ? `U+${char.codePointAt(0).toString(16).toUpperCase()}` : '—');

  // Cập nhật tiêu đề trang
  document.title = `Chữ Hán: ${char} (${hanViet}) - KizunaTutor`;
  document.getElementById('headerKanjiChar').textContent = char;
  document.getElementById('headerHanViet').textContent = `(${hanViet})`;

  // Khung bên trái
  const svgBox = document.getElementById('kanjiSvgContainer');
  if (currentKanjiData.svgData && svgBox) {
    svgBox.innerHTML = currentKanjiData.svgData;
    const svgEl = svgBox.querySelector('svg');
    if (svgEl) {
      svgEl.setAttribute('width', '100%');
      svgEl.setAttribute('height', '100%');
    }
  } else {
    document.getElementById('kanjiDisplayChar').textContent = char;
  }

  document.getElementById('detailHanViet').textContent = hanViet;
  document.getElementById('badgeJlpt').textContent = `JLPT: ${jlpt}`;
  document.getElementById('badgeStrokes').textContent = `${strokes} nét`;
  document.getElementById('badgeRadical').textContent = `Bộ: ${radical}`;

  // Link Shodo
  const shodoBtn = document.getElementById('btnShodoPractice');
  if (shodoBtn && char && char !== '—') {
    shodoBtn.href = `/shodo?char=${encodeURIComponent(char)}`;
  }

  // Khung bên phải
  document.getElementById('detailMeaning').textContent = meaning;
  document.getElementById('detailOnyomi').textContent = onyomi;
  document.getElementById('detailKunyomi').textContent = kunyomi;
  document.getElementById('detailUnicodeHex').textContent = hex;
  document.getElementById('detailStrokeCount').textContent = `${strokes} nét`;
  document.getElementById('detailRadical').textContent = radical;
  document.getElementById('detailJlpt').textContent = jlpt;
}

// 6. Modal Cất Vào Bộ Thẻ Flashcard
async function openSaveToDeckModal() {
  const select = document.getElementById('selectDeckDestination');
  if (!select) return;

  const char = currentKanjiData?.kanjiCharacter || currentKanjiData?.kanjiChar || currentChar || '—';
  const hanViet = (currentKanjiData?.hanViet || 'HÁN TỰ').toUpperCase();

  document.getElementById('modalDeckChar').textContent = char;
  document.getElementById('modalDeckHanViet').textContent = hanViet;

  let options = [];

  // Lấy danh sách từ LocalStorage
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const decks = JSON.parse(raw);
      if (Array.isArray(decks)) {
        decks.forEach(d => {
          options.push({ name: d.title, count: (d.cards ? d.cards.length : 0) });
        });
      }
    }
  } catch (e) {}

  if (options.length === 0) {
    options = [{ name: 'Bộ Thẻ Cá Nhân', count: 0 }];
  }

  select.innerHTML = options.map(o => `<option value="${escapeHtml(o.name)}">${escapeHtml(o.name)} (${o.count} thẻ)</option>`).join('');
  document.getElementById('modalAddToDeck').classList.remove('hidden');
}

function closeSaveToDeckModal() {
  document.getElementById('modalAddToDeck').classList.add('hidden');
}

function confirmSaveToDeck() {
  const targetDeckName = document.getElementById('selectDeckDestination')?.value || 'Bộ Thẻ Cá Nhân';
  const char = currentKanjiData?.kanjiCharacter || currentKanjiData?.kanjiChar || currentChar;
  if (!char) return;

  const hanViet = (currentKanjiData?.hanViet || 'HÁN TỰ').toUpperCase();
  const meaning = currentKanjiData?.meaning || currentKanjiData?.vietnameseMeaning || 'Chữ Hán';
  const onyomi = currentKanjiData?.onyomi || '—';
  const kunyomi = currentKanjiData?.kunyomi || '—';
  const jlpt = currentKanjiData?.jlptLevel || currentKanjiData?.jlpt || 'N5';

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    let decks = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(decks)) decks = [];

    let deck = decks.find(d => d.title === targetDeckName);
    if (!deck) {
      deck = {
        id: 'deck_' + Date.now(),
        title: targetDeckName,
        description: 'Bộ thẻ học viên tạo từ tra cứu',
        cards: [],
        color: 'blue'
      };
      decks.push(deck);
    }

    deck.cards = deck.cards || [];
    const existingIdx = deck.cards.findIndex(c => (c.char || c.kanjiChar) === char);
    const newCard = {
      char: char,
      kanjiChar: char,
      hanViet: hanViet,
      meaning: meaning,
      onyomi: onyomi,
      kunyomi: kunyomi,
      jlpt: jlpt
    };

    if (existingIdx >= 0) {
      deck.cards[existingIdx] = { ...deck.cards[existingIdx], ...newCard };
    } else {
      deck.cards.push(newCard);
    }

    localStorage.setItem(STORAGE_KEY, JSON.stringify(decks));
    closeSaveToDeckModal();
    showToast(`Đã cất chữ [${char}] vào bộ thẻ "${targetDeckName}" thành công!`, 'success');
  } catch (e) {
    showToast('Không thể lưu thẻ: ' + e.message, 'error');
  }
}

// 7. DOM Ready & Gán sự kiện
document.addEventListener('DOMContentLoaded', () => {
  loadKanjiDetail();

  // Nút quay lại
  document.getElementById('btnBackToList')?.addEventListener('click', () => {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      window.location.href = '/handwriting';
    }
  });

  // Sao chép chữ Hán
  document.getElementById('btnCopyChar')?.addEventListener('click', async () => {
    const char = currentKanjiData?.kanjiCharacter || currentKanjiData?.kanjiChar || currentChar;
    if (!char) return;
    try {
      await navigator.clipboard.writeText(char);
      showToast(`Đã sao chép chữ [${char}] vào bộ nhớ tạm!`, 'success');
    } catch (e) {
      showToast('Không thể sao chép chữ: ' + e.message, 'error');
    }
  });

  // Phát âm
  document.getElementById('btnSpeak')?.addEventListener('click', () => {
    const char = currentKanjiData?.kanjiCharacter || currentKanjiData?.kanjiChar || currentChar;
    if (char) speakJapanese(char);
  });

  // Cất vào bộ thẻ
  document.getElementById('btnSaveToDeck')?.addEventListener('click', openSaveToDeckModal);
  document.getElementById('btnCloseSaveDeckModal')?.addEventListener('click', closeSaveToDeckModal);
  document.getElementById('btnCancelSaveDeck')?.addEventListener('click', closeSaveToDeckModal);
  document.getElementById('btnConfirmSaveDeck')?.addEventListener('click', confirmSaveToDeck);
});
