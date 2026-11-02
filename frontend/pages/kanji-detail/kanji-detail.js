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

// 2. Tiện ích An Toàn Chuỗi & Làm sạch SVG
function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/**
 * Chuẩn hóa và làm sạch chuỗi SVG KanjiVG:
 * - Cắt bỏ XML declaration, comments, DOCTYPE (nguyên nhân gây lỗi hiển thị ']>')
 * - Loại bỏ nhóm StrokeNumbers (nguyên nhân gây xuất hiện các số thứ tự nét 1, 2, 3...)
 */
function cleanKanjiVgSvg(rawSvg) {
  if (!rawSvg) return '';
  const svgStartIdx = rawSvg.indexOf('<svg');
  const svgEndIdx = rawSvg.lastIndexOf('</svg>');
  let svg = '';
  if (svgStartIdx !== -1 && svgEndIdx !== -1) {
    svg = rawSvg.substring(svgStartIdx, svgEndIdx + 6);
  } else {
    svg = rawSvg
      .replace(/<\?xml[\s\S]*?\?>/gi, '')
      .replace(/<!--[\s\S]*?-->/g, '')
      .replace(/<!DOCTYPE[\s\S]*?\]>/gi, '')
      .replace(/<!DOCTYPE[^>]*>/gi, '')
      .trim();
  }
  // Loại bỏ nhóm ký tự số thứ tự nét vẽ StrokeNumbers
  svg = svg.replace(/<g[^>]*id="[^"]*StrokeNumbers[^"]*"[\s\S]*?<\/g>/gi, '');
  return svg;
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
  const headerKanjiCharEl = document.getElementById('headerKanjiChar');
  if (headerKanjiCharEl) headerKanjiCharEl.textContent = char;
  const headerHanVietEl = document.getElementById('headerHanViet');
  if (headerHanVietEl) headerHanVietEl.textContent = `(${hanViet})`;

  // Khung bên trái
  const svgBox = document.getElementById('kanjiSvgContainer');
  if (currentKanjiData.svgData && svgBox) {
    svgBox.innerHTML = cleanKanjiVgSvg(currentKanjiData.svgData);
    const svgEl = svgBox.querySelector('svg');
    if (svgEl) {
      svgEl.setAttribute('width', '100%');
      svgEl.setAttribute('height', '100%');
    }
  } else {
    const dispEl = document.getElementById('kanjiDisplayChar');
    if (dispEl) dispEl.textContent = char;
  }

  const elHanViet = document.getElementById('detailHanViet');
  if (elHanViet) elHanViet.textContent = hanViet;
  const elBadgeJlpt = document.getElementById('badgeJlpt');
  if (elBadgeJlpt) elBadgeJlpt.textContent = `JLPT: ${jlpt}`;
  const elBadgeStrokes = document.getElementById('badgeStrokes');
  if (elBadgeStrokes) elBadgeStrokes.textContent = `${strokes} nét`;
  const elBadgeRadical = document.getElementById('badgeRadical');
  if (elBadgeRadical) elBadgeRadical.textContent = `Bộ: ${radical}`;

  // Link Shodo
  const shodoBtn = document.getElementById('btnShodoPractice');
  if (shodoBtn && char && char !== '—') {
    shodoBtn.href = `/shodo?char=${encodeURIComponent(char)}`;
  }

  // Khung bên phải
  const elMeaning = document.getElementById('detailMeaning');
  if (elMeaning) elMeaning.textContent = meaning;
  const elOnyomi = document.getElementById('detailOnyomi');
  if (elOnyomi) elOnyomi.textContent = onyomi;
  const elKunyomi = document.getElementById('detailKunyomi');
  if (elKunyomi) elKunyomi.textContent = kunyomi;
  const elUnicodeHex = document.getElementById('detailUnicodeHex');
  if (elUnicodeHex) elUnicodeHex.textContent = hex;
  const elStrokeCount = document.getElementById('detailStrokeCount');
  if (elStrokeCount) elStrokeCount.textContent = `${strokes} nét`;
  const elRadical = document.getElementById('detailRadical');
  if (elRadical) elRadical.textContent = radical;
  const elJlpt = document.getElementById('detailJlpt');
  if (elJlpt) elJlpt.textContent = jlpt;
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
