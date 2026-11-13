/**
 * KizunaTutor - Trợ Lý Bóc Tách Đọc Hiểu & Ngữ Pháp AI (Reading Assistant)
 * Tích hợp AI Dokkai Sensei, VOICEVOX Audio Synthesizer, Flashcard Decks 1-Click
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { showToast } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';

// 1. Khởi tạo Layout Học viên Dùng Chung
initLearnerShell({
  activeMenu: 'ai-reading',
  pageTitle: 'Bóc Tách Đọc Hiểu AI 📖',
  badge: 'Trợ lý đọc hiểu Dokkai'
});

// Các đoạn văn bản mẫu
const SAMPLE_PRESETS = {
  n5: '私は毎朝7時に起きて、公園を散歩します。天気がいい日は富士山がきれいに見えます。途中でパン屋に寄って、焼きたてのパンとコーヒーを買います。家に帰ってから朝ご飯を食べます。',
  n4: '先週の日曜日、友達と一緒に京都へ旅行に行きました。新幹線で2時間くらいかかりました。金閣寺を見たり、抹茶アイスを食べたりして、とても楽しかったです。雨が降っても着物を着て写真を撮りました。',
  n3: '日本語が上手になるように、毎朝NHKのやさしいニュースを聞くことにしています。分からない単語があってもすぐに諦めないで、文脈から意味を推測するように努力しています。継続することが一番大切だと思います。'
};

// Trạng thái ứng dụng
let currentAnalysisResult = null;
let userDecksList = [];
let pendingSaveWord = null;
let isFuriganaHidden = false;

// DOM Elements
const inputEl = document.getElementById('readingInputText');
const charCountEl = document.getElementById('charCountLabel');
const selectJlptEl = document.getElementById('selectJlptTarget');
const btnClearEl = document.getElementById('btnClearInput');
const btnAnalyzeEl = document.getElementById('btnAnalyzeReading');

const emptyStateEl = document.getElementById('readingEmptyState');
const loadingStateEl = document.getElementById('readingLoadingState');
const resultContainerEl = document.getElementById('readingResultContainer');

const badgeEstimatedLevelEl = document.getElementById('badgeEstimatedLevel');
const interactiveReadingBodyEl = document.getElementById('interactiveReadingBody');
const textOverallSummaryEl = document.getElementById('textOverallSummary');
const btnToggleFuriganaEl = document.getElementById('btnToggleFurigana');
const furiganaToggleTextEl = document.getElementById('furiganaToggleText');
const btnSpeakFullEl = document.getElementById('btnSpeakFullArticle');

const countGrammarsEl = document.getElementById('countGrammars');
const grammarListContainerEl = document.getElementById('grammarListContainer');
const countVocabsEl = document.getElementById('countVocabs');
const vocabularyGridContainerEl = document.getElementById('vocabularyGridContainer');
const sentenceTranslationsContainerEl = document.getElementById('sentenceTranslationsContainer');

const modalSaveEl = document.getElementById('modalSaveToDeck');
const btnCloseModalEl = document.getElementById('btnCloseSaveModal');
const btnCancelModalEl = document.getElementById('btnCancelModal');
const btnConfirmSaveCardEl = document.getElementById('btnConfirmSaveCard');
const selectTargetDeckEl = document.getElementById('selectTargetDeck');
const modalTargetWordEl = document.getElementById('modalTargetWord');
const modalTargetReadingEl = document.getElementById('modalTargetReading');
const modalTargetMeaningEl = document.getElementById('modalTargetMeaning');

// =========================================================================
// 2. KHỞI TẠO & LẮNG NGHE SỰ KIỆN
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  checkVoicevoxStatus();
  fetchUserDecks();
});

function setupEventListeners() {
  // Đếm ký tự khi gõ
  if (inputEl) {
    inputEl.addEventListener('input', () => {
      const len = inputEl.value.length;
      charCountEl.textContent = `${len} / 3000 ký tự`;
      if (len > 3000) {
        charCountEl.classList.add('text-rose-600', 'font-bold');
      } else {
        charCountEl.classList.remove('text-rose-600', 'font-bold');
      }
    });
  }

  // Nút xóa nội dung
  if (btnClearEl) {
    btnClearEl.addEventListener('click', () => {
      inputEl.value = '';
      inputEl.dispatchEvent(new Event('input'));
      inputEl.focus();
    });
  }

  // Các nút chọn bài mẫu
  document.querySelectorAll('.btn-sample-preset').forEach(btn => {
    btn.addEventListener('click', () => {
      const presetKey = btn.dataset.preset;
      if (SAMPLE_PRESETS[presetKey]) {
        inputEl.value = SAMPLE_PRESETS[presetKey];
        inputEl.dispatchEvent(new Event('input'));
        if (selectJlptEl) {
          selectJlptEl.value = presetKey.toUpperCase();
        }
        showToast(`Đã nạp văn bản mẫu ${presetKey.toUpperCase()}`, 'info');
      }
    });
  });

  // Nút Phân Tích
  if (btnAnalyzeEl) {
    btnAnalyzeEl.addEventListener('click', executeReadingAnalysis);
  }

  // Nút Bật/Tắt Furigana
  if (btnToggleFuriganaEl) {
    btnToggleFuriganaEl.addEventListener('click', () => {
      isFuriganaHidden = !isFuriganaHidden;
      if (interactiveReadingBodyEl) {
        interactiveReadingBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
      }
      furiganaToggleTextEl.textContent = isFuriganaHidden ? 'Hiện Furigana' : 'Ẩn Furigana';
      showToast(isFuriganaHidden ? 'Đã ẩn Furigana để bạn tự kiểm tra đọc' : 'Đã hiện Furigana', 'info');
    });
  }

  // Nút đọc toàn bài bằng VOICEVOX
  if (btnSpeakFullEl) {
    btnSpeakFullEl.addEventListener('click', () => {
      if (currentAnalysisResult && currentAnalysisResult.originalText) {
        btnSpeakFullEl.classList.add('ring-2', 'ring-rose-400');
        voicevoxClient.speak(currentAnalysisResult.originalText).finally(() => {
          btnSpeakFullEl.classList.remove('ring-2', 'ring-rose-400');
        });
      }
    });
  }

  // Modal đóng
  if (btnCloseModalEl) btnCloseModalEl.addEventListener('click', closeModal);
  if (btnCancelModalEl) btnCancelModalEl.addEventListener('click', closeModal);
  if (btnConfirmSaveCardEl) btnConfirmSaveCardEl.addEventListener('click', saveWordToDeck);
}

// =========================================================================
// 3. KIỂM TRA TRẠNG THÁI VOICEVOX ENGINE
// =========================================================================
async function checkVoicevoxStatus() {
  const badgeEl = document.getElementById('voicevoxStatusBadge');
  const textEl = document.getElementById('voicevoxStatusText');
  if (!badgeEl || !textEl) return;

  const isOnline = await voicevoxClient.checkOnline();
  if (isOnline) {
    badgeEl.className = 'inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold self-start md:self-auto';
    textEl.textContent = 'VOICEVOX Sẵn sàng (Chuẩn giọng Tokyo)';
  } else {
    badgeEl.className = 'inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs font-semibold self-start md:self-auto';
    textEl.textContent = 'Web Speech API (VOICEVOX chưa chạy)';
  }
}

// =========================================================================
// 4. GỌI API PHÂN TÍCH ĐỌC HIỂU
// =========================================================================
async function executeReadingAnalysis() {
  const text = (inputEl.value || '').trim();
  if (!text) {
    showToast('Vui lòng nhập đoạn văn bản tiếng Nhật cần phân tích.', 'warning');
    inputEl.focus();
    return;
  }

  // Đổi trạng thái UI
  emptyStateEl.classList.add('hidden');
  resultContainerEl.classList.add('hidden');
  loadingStateEl.classList.remove('hidden');
  btnAnalyzeEl.disabled = true;
  btnAnalyzeEl.classList.add('opacity-70', 'cursor-not-allowed');

  const payload = {
    text: text,
    targetJlpt: selectJlptEl ? selectJlptEl.value : 'ALL',
    includeFurigana: true
  };

  try {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/learner/reading/analyze', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      throw new Error(`Mã lỗi máy chủ: ${res.status}`);
    }

    const data = await res.json();
    currentAnalysisResult = data;
    renderAnalysisResult(data);
    showToast('Phân tích đọc hiểu thành công!', 'success');
  } catch (err) {
    console.error('[AI Reading] Lỗi phân tích:', err);
    showToast('Không thể phân tích văn bản. Vui lòng thử lại sau.', 'error');
    emptyStateEl.classList.remove('hidden');
  } finally {
    loadingStateEl.classList.add('hidden');
    btnAnalyzeEl.disabled = false;
    btnAnalyzeEl.classList.remove('opacity-70', 'cursor-not-allowed');
  }
}

// =========================================================================
// 5. HIỂN THỊ KẾT QUẢ PHÂN TÍCH RA GIAO DIỆN
// =========================================================================
function renderAnalysisResult(data) {
  resultContainerEl.classList.remove('hidden');

  // Cấp độ ước tính
  if (badgeEstimatedLevelEl) {
    const level = data.estimatedLevel || 'N5';
    badgeEstimatedLevelEl.textContent = `JLPT ${level}`;
  }

  // Tóm tắt ý chính
  if (textOverallSummaryEl) {
    textOverallSummaryEl.textContent = data.overallSummary || 'Không có tóm tắt.';
  }

  // 1. Render Bài đọc có Furigana
  if (interactiveReadingBodyEl) {
    interactiveReadingBodyEl.innerHTML = data.rubyHtml || escapeHtml(data.originalText);
    interactiveReadingBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
  }

  // 2. Render Ngữ pháp
  const grammars = data.detectedGrammars || [];
  countGrammarsEl.textContent = grammars.length;
  grammarListContainerEl.innerHTML = '';

  if (grammars.length === 0) {
    grammarListContainerEl.innerHTML = '<div class="text-xs text-slate-400 italic p-3 text-center">Không phát hiện cấu trúc ngữ pháp đặc biệt.</div>';
  } else {
    grammars.forEach(g => {
      const card = document.createElement('div');
      card.className = 'p-3 rounded-lg border border-[#E2E8F0] bg-white hover:border-[#F43F5E] transition space-y-1.5 shadow-2xs';

      const levelColor = getJlptBadgeColor(g.jlptLevel);

      card.innerHTML = `
        <div class="flex items-center justify-between gap-2 flex-wrap">
          <div class="flex items-center gap-2">
            <span class="inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold ${levelColor}">
              ${escapeHtml(g.jlptLevel || 'JLPT')}
            </span>
            <span class="text-sm font-bold text-[#0F172A] font-japanese">${escapeHtml(g.pattern || '')}</span>
          </div>
          <div class="flex items-center gap-1.5">
            <button type="button" class="btn-speak-grammar text-slate-400 hover:text-[#9F1239] transition p-1 cursor-pointer" title="Nghe phát âm">
              <span class="material-symbols-outlined text-[16px]">volume_up</span>
            </button>
            <a href="/grammar?keyword=${encodeURIComponent(g.pattern || '')}" target="_blank"
               class="text-[11px] text-[#9F1239] hover:underline font-semibold flex items-center gap-0.5">
              <span>Kho ngữ pháp</span>
              <span class="material-symbols-outlined text-[13px]">open_in_new</span>
            </a>
          </div>
        </div>
        <div class="text-xs font-semibold text-[#831843]">${escapeHtml(g.meaning || '')}</div>
        ${g.matchedText ? `<div class="text-[11px] text-slate-500 font-japanese bg-[#F8FAFC] p-1.5 rounded border border-[#E2E8F0]">Đoạn khớp: <strong class="text-slate-700">「${escapeHtml(g.matchedText)}」</strong></div>` : ''}
        <p class="text-xs text-slate-600 leading-relaxed">${escapeHtml(g.explanation || '')}</p>
      `;

      // Gắn sự kiện nghe phát âm
      const btnSpeak = card.querySelector('.btn-speak-grammar');
      if (btnSpeak) {
        btnSpeak.addEventListener('click', () => {
          voicevoxClient.speak(g.matchedText || g.pattern);
        });
      }

      grammarListContainerEl.appendChild(card);
    });
  }

  // 3. Render Từ vựng trọng tâm
  const vocabs = data.vocabularies || [];
  countVocabsEl.textContent = vocabs.length;
  vocabularyGridContainerEl.innerHTML = '';

  if (vocabs.length === 0) {
    vocabularyGridContainerEl.innerHTML = '<div class="col-span-2 text-xs text-slate-400 italic p-3 text-center">Không có từ vựng tiêu biểu.</div>';
  } else {
    vocabs.forEach(v => {
      const card = document.createElement('div');
      card.className = 'p-3 rounded-lg border border-[#E2E8F0] bg-white hover:border-[#9F1239] transition flex flex-col justify-between space-y-2 shadow-2xs';

      card.innerHTML = `
        <div>
          <div class="flex items-center justify-between gap-1">
            <span class="text-base font-bold font-japanese text-[#0F172A]">${escapeHtml(v.kanji || '')}</span>
            <span class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">${escapeHtml(v.jlpt || 'N5')}</span>
          </div>
          <div class="text-xs text-slate-500 font-japanese">
            ${escapeHtml(v.reading || '')}
            ${v.hanViet ? `<span class="text-[11px] font-semibold text-[#831843] ml-1">(${escapeHtml(v.hanViet)})</span>` : ''}
          </div>
          <div class="text-xs text-slate-800 font-medium mt-1 leading-snug">${escapeHtml(v.meaning || '')}</div>
        </div>

        <div class="pt-2 border-t border-[#F1F5F9] flex items-center justify-between">
          <button type="button" class="btn-speak-vocab text-xs text-slate-500 hover:text-[#9F1239] font-medium flex items-center gap-1 cursor-pointer transition">
            <span class="material-symbols-outlined text-[15px]">volume_up</span>
            <span>Nghe</span>
          </button>
          <button type="button" class="btn-save-flashcard px-2 py-1 rounded bg-[#FFE5EC] hover:bg-rose-100 text-[#831843] text-[11px] font-bold border border-[#FBCFE8] flex items-center gap-1 cursor-pointer transition">
            <span class="material-symbols-outlined text-[14px]">style</span>
            <span>+ Flashcard</span>
          </button>
        </div>
      `;

      // Gắn sự kiện nghe phát âm
      const btnSpeak = card.querySelector('.btn-speak-vocab');
      if (btnSpeak) {
        btnSpeak.addEventListener('click', () => {
          voicevoxClient.speak(v.kanji || v.reading);
        });
      }

      // Gắn sự kiện lưu flashcard
      const btnSave = card.querySelector('.btn-save-flashcard');
      if (btnSave) {
        btnSave.addEventListener('click', () => {
          openSaveModal(v);
        });
      }

      vocabularyGridContainerEl.appendChild(card);
    });
  }

  // 4. Render Dịch từng câu
  const translations = data.sentenceTranslations || [];
  sentenceTranslationsContainerEl.innerHTML = '';
  if (translations.length === 0) {
    sentenceTranslationsContainerEl.innerHTML = '<div class="text-xs text-slate-400 italic">Chưa có bản dịch từng câu.</div>';
  } else {
    translations.forEach((sent, idx) => {
      const p = document.createElement('p');
      p.className = 'p-2 rounded bg-[#F8FAFC] border border-[#E2E8F0]';
      p.innerHTML = `<span class="font-bold text-[#831843] mr-1">[${idx + 1}]</span> ${escapeHtml(sent)}`;
      sentenceTranslationsContainerEl.appendChild(p);
    });
  }
}

function getJlptBadgeColor(level) {
  switch (level) {
    case 'N5': return 'bg-emerald-100 text-emerald-800';
    case 'N4': return 'bg-sky-100 text-sky-800';
    case 'N3': return 'bg-amber-100 text-amber-800';
    case 'N2': return 'bg-orange-100 text-orange-800';
    case 'N1': return 'bg-rose-100 text-rose-800';
    default: return 'bg-slate-100 text-slate-800';
  }
}

// =========================================================================
// 6. TÍCH HỢP LƯU TỪ VỰNG VÀO FLASHCARD DECK
// =========================================================================
async function fetchUserDecks() {
  try {
    const headers = {};
    const token = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/learner/decks', { headers });
    if (res.ok) {
      userDecksList = await res.json();
    }
  } catch (err) {
    console.debug('[AI Reading] Chưa lấy được danh sách deck:', err);
  }
}

function openSaveModal(word) {
  pendingSaveWord = word;
  modalTargetWordEl.textContent = word.kanji || word.reading;
  modalTargetReadingEl.textContent = `${word.reading} ${word.hanViet ? `(${word.hanViet})` : ''}`;
  modalTargetMeaningEl.textContent = word.meaning;

  selectTargetDeckEl.innerHTML = '';
  if (userDecksList.length === 0) {
    const opt = document.createElement('option');
    opt.value = '__CREATE_NEW__';
    opt.textContent = '✨ Tạo bộ thẻ mới: "Từ vựng bài đọc AI"';
    selectTargetDeckEl.appendChild(opt);
  } else {
    userDecksList.forEach(deck => {
      const opt = document.createElement('option');
      opt.value = deck.id;
      opt.textContent = `${deck.title} (${(deck.cards || []).length} thẻ)`;
      selectTargetDeckEl.appendChild(opt);
    });
    const createOpt = document.createElement('option');
    createOpt.value = '__CREATE_NEW__';
    createOpt.textContent = '➕ Tạo bộ thẻ mới...';
    selectTargetDeckEl.appendChild(createOpt);
  }

  modalSaveEl.classList.remove('hidden');
  modalSaveEl.classList.add('flex');
}

function closeModal() {
  modalSaveEl.classList.add('hidden');
  modalSaveEl.classList.remove('flex');
  pendingSaveWord = null;
}

async function saveWordToDeck() {
  if (!pendingSaveWord) return;

  const deckId = selectTargetDeckEl.value;
  const token = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const newCard = {
    id: 'card_' + Date.now(),
    term: pendingSaveWord.kanji || pendingSaveWord.reading,
    reading: pendingSaveWord.reading || '',
    meaning: pendingSaveWord.meaning || '',
    hanViet: pendingSaveWord.hanViet || '',
    example: '',
    exampleMeaning: '',
    createdAt: new Date().toISOString()
  };

  try {
    btnConfirmSaveCardEl.disabled = true;
    btnConfirmSaveCardEl.textContent = 'Đang lưu...';

    if (deckId === '__CREATE_NEW__') {
      // 1. Tạo deck mới
      const newDeckPayload = {
        title: 'Từ vựng bài đọc AI - ' + new Date().toLocaleDateString('vi-VN'),
        description: 'Tự động tạo từ tính năng Bóc tách bài đọc AI',
        color: 'rose',
        cards: [newCard]
      };

      const res = await fetch('/api/learner/decks', {
        method: 'POST',
        headers,
        body: JSON.stringify(newDeckPayload)
      });

      if (!res.ok) throw new Error('Không thể tạo bộ thẻ mới');
      const createdDeck = await res.json();
      userDecksList.push(createdDeck);
      showToast('Đã tạo bộ thẻ mới và lưu từ thành công!', 'success');
    } else {
      // 2. Thêm vào deck đã chọn
      const targetDeck = userDecksList.find(d => String(d.id) === String(deckId));
      if (!targetDeck) throw new Error('Không tìm thấy bộ thẻ đã chọn');

      const existingCards = targetDeck.cards || [];
      existingCards.push(newCard);

      const updatePayload = {
        title: targetDeck.title,
        description: targetDeck.description,
        color: targetDeck.color || 'blue',
        cards: existingCards
      };

      const res = await fetch(`/api/learner/decks/${deckId}`, {
        method: 'PUT',
        headers,
        body: JSON.stringify(updatePayload)
      });

      if (!res.ok) throw new Error('Không thể cập nhật bộ thẻ');
      targetDeck.cards = existingCards;
      showToast(`Đã thêm từ vào bộ thẻ "${targetDeck.title}"!`, 'success');
    }

    closeModal();
  } catch (err) {
    console.error('[AI Reading] Lỗi lưu flashcard:', err);
    showToast('Lỗi lưu vào Flashcard: ' + err.message, 'error');
  } finally {
    btnConfirmSaveCardEl.disabled = false;
    btnConfirmSaveCardEl.textContent = 'Lưu vào bộ thẻ';
  }
}

function escapeHtml(text) {
  if (text === null || text === undefined) return '';
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
