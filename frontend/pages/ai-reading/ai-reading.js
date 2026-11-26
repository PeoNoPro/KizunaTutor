/**
 * KizunaTutor - Trợ Lý Luyện Đọc Hiểu Tiếng Nhật AI (Pure Dokkai Studio)
 * Cấu trúc trực quan: Văn bản đọc hiểu ở trên, Bộ câu hỏi trắc nghiệm Dokkai ở dưới.
 * Hỗ trợ Furigana chuẩn trên đầu chữ Hán, 4 tùy chọn tập trung trên thanh công cụ Top Bar,
 * bản dịch tham khảo tiếng Việt đối chiếu và phát âm giọng đọc Tokyo VOICEVOX.
 */

import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { showToast } from '/js/main.js';
import { voicevoxClient } from '/js/voicevoxClient.js';
import {
  splitJapaneseSentences,
  buildTranslationSentences,
  findBestMatchingSentence,
} from '/js/reading-translation-helpers.js';

// 1. Khởi tạo Layout Học viên Dùng Chung
initLearnerShell({
  activeMenu: 'ai-reading',
  pageTitle: 'Luyện đọc hiểu văn bản',
  badge: 'Đọc hiểu'
});

// Các đoạn văn bản mẫu tiện lợi
const SAMPLE_PRESETS = {
  n5: '私は毎朝7時に起きて、公園を散歩します。天気がいい日は富士山がきれいに見えます。途中でパン屋に寄って、焼きたてのパンとコーヒーを買います。家に帰ってから朝ご飯を食べます。',
  tokyo: '先週の日曜日、私は友達と東京へ行きました。朝9時に駅で友達に会いました。それから、電車で浅草へ行きました。浅草は人がたくさんいましたが、とてもにぎやかで面白かったです。私たちは大きいお寺を見たり、写真を撮ったりしました。昼ごはんに美味しいラーメンを食べました。午後、デパートで家族にお土産を買いました。少し疲れましたが、とても楽しかったです。また友達と東京へ行きたいです。',
  n4: '先週の日曜日、友達と一緒に京都へ旅行に行きました。新幹線で2時間くらいかかりました。金閣寺を見たり、抹茶アイスを食べたりして、とても楽しかったです。雨が降っても着物を着て写真を撮りました。',
  n3: '日本語が上手になるように、毎朝NHKのやさしいニュースを聞くことにしています。分からない単語があってもすぐに諦めないで、文脈から意味を推測するように努力しています。継続することが一番大切だと思います。',
  nature: '私の国には、美しい自然がたくさんあります。特に、山や海、川などの景色がとてもきれいです。私が一番好きな場所は、海の近くにある小さな町です。そこへ行くと、青い海と白い砂浜が広がっていて、心が落ち着きます。朝になると、太陽が海から昇り、空が少しずつ明るくなっていきます。その景色は本当にすばらしく、何時間でも見ていられるほどです。また、夕方には空が赤やオレンジ色に変わり、とてもロマンチックな雰囲気になります。忙しい毎日を忘れて、自然の美しさを感じることができます。機会があれば、ぜひ皆さんにもこの場所を訪れてほしいです。'
};

// Trạng thái ứng dụng (Tất cả các tác vụ trợ giúp đều mặc định ẨN để học viên tự luyện đọc tiếng Nhật)
let currentAnalysisResult = null;
let isFuriganaHidden = true; // Mặc định ẩn Furigana
let isSummaryVisible = false; // Mặc định ẩn ý chính bài đọc
let isTranslationVisible = false; // Mặc định ẩn bản dịch tham khảo tiếng Việt
let dokkaiAnswers = {};
let dokkaiSubmitted = false;

// DOM Elements: Điều hướng 2 màn hình
let inputScreenEl = null;
let workspaceScreenEl = null;
let btnBackToInputEl = null;

// DOM Elements: Màn hình 1 (Input Studio)
let inputEl = null;
let charCountEl = null;
let selectJlptEl = null;
let btnClearEl = null;
let btnAnalyzeEl = null;
let loadingStateEl = null;

// DOM Elements: Màn hình 2 (Dokkai Studio Top Toolbar)
let badgeEstimatedLevelEl = null;
let btnToggleFuriganaEl = null;
let furiganaToggleTextEl = null;
let btnToggleSummaryEl = null;
let summaryToggleTextEl = null;
let btnToggleTranslationEl = null;
let translationToggleTextEl = null;
let btnSpeakFullEl = null;

// DOM Elements: Đoạn văn đọc hiểu (Trên)
let cleanReadingPassageBodyEl = null;
let overallSummaryBoxEl = null;
let textOverallSummaryEl = null;
let cleanPassageTranslationContainerEl = null;
let fullTranslationBodyEl = null;

// DOM Elements: Bộ câu hỏi Đọc hiểu Dokkai (Dưới)
let countDokkaiQuestionsHeaderEl = null;
let dokkaiQuestionsContainerEl = null;
let dokkaiScoreSummaryEl = null;
let btnSubmitDokkaiEl = null;
let btnSubmitDokkaiTextEl = null;

// =========================================================================
// 1. LÀM MỚI TOÀN BỘ THAM CHIẾU DOM (TRÁNH DETACHED DOM TRONG SPA ROUTER)
// =========================================================================
function refreshReadingDomReferences() {
  inputScreenEl = document.getElementById('readingInputScreen');
  workspaceScreenEl = document.getElementById('readingWorkspaceScreen');
  btnBackToInputEl = document.getElementById('btnBackToInput');

  inputEl = document.getElementById('readingInputText');
  charCountEl = document.getElementById('charCountLabel');
  selectJlptEl = document.getElementById('selectJlptTarget');
  btnClearEl = document.getElementById('btnClearInput');
  btnAnalyzeEl = document.getElementById('btnAnalyzeReading');
  loadingStateEl = document.getElementById('readingLoadingState');

  badgeEstimatedLevelEl = document.getElementById('badgeEstimatedLevel');
  btnToggleFuriganaEl = document.getElementById('btnToggleFurigana');
  furiganaToggleTextEl = document.getElementById('furiganaToggleText');
  btnToggleSummaryEl = document.getElementById('btnToggleSummary');
  summaryToggleTextEl = document.getElementById('summaryToggleText');
  btnToggleTranslationEl = document.getElementById('btnToggleTranslation');
  translationToggleTextEl = document.getElementById('translationToggleText');
  btnSpeakFullEl = document.getElementById('btnSpeakFullArticle');

  cleanReadingPassageBodyEl = document.getElementById('cleanReadingPassageBody');
  overallSummaryBoxEl = document.getElementById('overallSummaryBox');
  textOverallSummaryEl = document.getElementById('textOverallSummary');
  cleanPassageTranslationContainerEl = document.getElementById('cleanPassageTranslationContainer');
  fullTranslationBodyEl = document.getElementById('fullTranslationBody');

  countDokkaiQuestionsHeaderEl = document.getElementById('countDokkaiQuestionsHeader');
  dokkaiQuestionsContainerEl = document.getElementById('dokkaiQuestionsContainer');
  dokkaiScoreSummaryEl = document.getElementById('dokkaiScoreSummary');
  btnSubmitDokkaiEl = document.getElementById('btnSubmitDokkai');
  btnSubmitDokkaiTextEl = document.getElementById('btnSubmitDokkaiText');
}

// =========================================================================
// 2. ĐIỀU KHIỂN CHUYỂN MÀN HÌNH
// =========================================================================
function switchScreen(screenName) {
  if (screenName === 'input') {
    if (inputScreenEl) inputScreenEl.classList.remove('hidden');
    if (workspaceScreenEl) workspaceScreenEl.classList.add('hidden');
    if (loadingStateEl) loadingStateEl.classList.add('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } else if (screenName === 'workspace') {
    if (inputScreenEl) inputScreenEl.classList.add('hidden');
    if (workspaceScreenEl) workspaceScreenEl.classList.remove('hidden');
    if (loadingStateEl) loadingStateEl.classList.add('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  } else if (screenName === 'loading') {
    if (inputScreenEl) inputScreenEl.classList.add('hidden');
    if (workspaceScreenEl) workspaceScreenEl.classList.add('hidden');
    if (loadingStateEl) loadingStateEl.classList.remove('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

// =========================================================================
// 3. KHỞI TẠO & LẮNG NGHE SỰ KIỆN
// =========================================================================
function initReadingPage() {
  refreshReadingDomReferences();
  switchScreen('input');
  setupEventListeners();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initReadingPage);
} else {
  initReadingPage();
}

window.addEventListener('kizuna:page-mounted', (e) => {
  const url = e?.detail?.url || window.location.pathname;
  if (url.includes('reading')) {
    initReadingPage();
  }
});

function setupEventListeners() {
  const guardEl = btnAnalyzeEl || document.getElementById('btnAnalyzeReading');
  if (guardEl && guardEl.dataset.readingEventsBound === 'true') {
    return;
  }
  if (guardEl) {
    guardEl.dataset.readingEventsBound = 'true';
  }

  // Nút chữ I để bật/tắt modal hướng dẫn 3 bước học nhanh
  const btnToggleGuideEl = document.getElementById('btnToggleGuide');
  const btnCloseGuideEl = document.getElementById('btnCloseGuide');
  const btnAckGuideEl = document.getElementById('btnAckGuide');
  const readingGuideModalEl = document.getElementById('readingGuideModal');

  if (btnToggleGuideEl && readingGuideModalEl) {
    btnToggleGuideEl.addEventListener('click', () => {
      readingGuideModalEl.classList.remove('hidden');
    });
  }
  if (btnCloseGuideEl && readingGuideModalEl) {
    btnCloseGuideEl.addEventListener('click', () => {
      readingGuideModalEl.classList.add('hidden');
    });
  }
  if (btnAckGuideEl && readingGuideModalEl) {
    btnAckGuideEl.addEventListener('click', () => {
      readingGuideModalEl.classList.add('hidden');
    });
  }
  if (readingGuideModalEl) {
    readingGuideModalEl.addEventListener('click', (e) => {
      if (e.target === readingGuideModalEl) {
        readingGuideModalEl.classList.add('hidden');
      }
    });
  }
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && readingGuideModalEl && !readingGuideModalEl.classList.contains('hidden')) {
      readingGuideModalEl.classList.add('hidden');
    }
  });

  // Nút quay lại màn hình nhập liệu
  if (btnBackToInputEl) {
    btnBackToInputEl.addEventListener('click', () => {
      switchScreen('input');
    });
  }

  // Đếm ký tự khi gõ
  if (inputEl && charCountEl) {
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
      if (inputEl) {
        inputEl.value = '';
        inputEl.dispatchEvent(new Event('input'));
        inputEl.focus();
      }
    });
  }

  // Các nút chọn bài mẫu
  document.querySelectorAll('.btn-sample-preset').forEach(btn => {
    btn.addEventListener('click', () => {
      const presetKey = btn.dataset.preset;
      if (SAMPLE_PRESETS[presetKey] && inputEl) {
        inputEl.value = SAMPLE_PRESETS[presetKey];
        inputEl.dispatchEvent(new Event('input'));
        if (selectJlptEl) {
          if (presetKey === 'n5' || presetKey === 'tokyo') selectJlptEl.value = 'N5';
          else if (presetKey === 'n4' || presetKey === 'nature') selectJlptEl.value = 'N4';
          else if (presetKey === 'n3') selectJlptEl.value = 'N3';
          else selectJlptEl.value = 'ALL';
        }
        showToast('Đã nạp đoạn văn mẫu thành công!', 'info');
      }
    });
  });

  // Nút Bắt đầu phân tích đọc hiểu AI
  if (btnAnalyzeEl) {
    btnAnalyzeEl.addEventListener('click', executeReadingAnalysis);
  }

  // 1. Tùy chọn Ẩn/Hiện Furigana trên Top Bar
  if (btnToggleFuriganaEl) {
    btnToggleFuriganaEl.addEventListener('click', () => {
      isFuriganaHidden = !isFuriganaHidden;
      syncFuriganaUI();
      showToast(isFuriganaHidden ? 'Đã ẩn Furigana' : 'Đã hiện Furigana', 'info');
    });
  }

  // 2. Tùy chọn Ẩn/Hiện Ý chính bài đọc trên Top Bar
  if (btnToggleSummaryEl) {
    btnToggleSummaryEl.addEventListener('click', () => {
      isSummaryVisible = !isSummaryVisible;
      if (overallSummaryBoxEl) {
        overallSummaryBoxEl.classList.toggle('hidden', !isSummaryVisible);
      }
      if (summaryToggleTextEl) {
        summaryToggleTextEl.textContent = isSummaryVisible ? 'Ẩn ý chính' : 'Hiện ý chính';
      }
      btnToggleSummaryEl.classList.toggle('bg-amber-100', isSummaryVisible);
      btnToggleSummaryEl.classList.toggle('border-amber-400', isSummaryVisible);
      if (isSummaryVisible && overallSummaryBoxEl) {
        overallSummaryBoxEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
      showToast(isSummaryVisible ? 'Đã mở ý chính bài đọc' : 'Đã ẩn ý chính bài đọc', 'info');
    });
  }

  // 3. Tùy chọn Ẩn/Hiện Bản dịch tham khảo tiếng Việt trên Top Bar
  if (btnToggleTranslationEl) {
    btnToggleTranslationEl.addEventListener('click', () => {
      isTranslationVisible = !isTranslationVisible;
      if (cleanPassageTranslationContainerEl) {
        cleanPassageTranslationContainerEl.classList.toggle('hidden', !isTranslationVisible);
      }
      if (translationToggleTextEl) {
        translationToggleTextEl.textContent = isTranslationVisible ? 'Ẩn bản dịch' : 'Hiện bản dịch';
      }
      btnToggleTranslationEl.classList.toggle('bg-indigo-100', isTranslationVisible);
      btnToggleTranslationEl.classList.toggle('border-indigo-400', isTranslationVisible);
      if (isTranslationVisible && cleanPassageTranslationContainerEl) {
        cleanPassageTranslationContainerEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      }
      showToast(isTranslationVisible ? 'Đã mở bản dịch tham khảo' : 'Đã ẩn bản dịch tham khảo', 'info');
    });
  }

  // 4. Tùy chọn Nghe toàn bài bằng VOICEVOX trên Top Bar
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

  // Nút Nộp bài Đọc hiểu Dokkai
  if (btnSubmitDokkaiEl) {
    btnSubmitDokkaiEl.addEventListener('click', handleDokkaiSubmit);
  }

  // Bấm vào câu trong bản dịch đoạn văn để nhảy tới câu tiếng Nhật tương ứng
  if (fullTranslationBodyEl) {
    fullTranslationBodyEl.addEventListener('click', (e) => {
      const span = e.target.closest('[data-sentence-index]');
      if (!span) return;
      const idx = Number.parseInt(span.dataset.sentenceIndex, 10);
      if (Number.isNaN(idx)) return;

      fullTranslationBodyEl.querySelectorAll('.translation-sentence-link').forEach((el) => {
        el.classList.remove('bg-amber-100', 'text-amber-950', 'font-medium');
      });
      span.classList.add('bg-amber-100', 'text-amber-950', 'font-medium');

      const jpSentences = splitJapaneseSentences(currentAnalysisResult?.originalText || '');
      const targetJp = jpSentences[idx] || jpSentences[0] || '';
      if (targetJp) {
        highlightEvidenceInCleanPassage(targetJp);
      }
    });
  }
}

// Đồng bộ trạng thái hiển thị Furigana
function syncFuriganaUI() {
  if (cleanReadingPassageBodyEl) {
    cleanReadingPassageBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
  }
  if (furiganaToggleTextEl) {
    furiganaToggleTextEl.textContent = isFuriganaHidden ? 'Hiện Furigana' : 'Ẩn Furigana';
  }
  if (btnToggleFuriganaEl) {
    btnToggleFuriganaEl.classList.toggle('bg-rose-100', !isFuriganaHidden);
    btnToggleFuriganaEl.classList.toggle('border-rose-400', !isFuriganaHidden);
    btnToggleFuriganaEl.classList.toggle('text-rose-900', !isFuriganaHidden);
    const iconEl = btnToggleFuriganaEl.querySelector('.material-symbols-outlined');
    if (iconEl) {
      iconEl.textContent = isFuriganaHidden ? 'visibility_off' : 'visibility';
    }
  }
}

// =========================================================================
// 4. GỌI API PHÂN TÍCH ĐỌC HIỂU
// =========================================================================
async function executeReadingAnalysis() {
  if (!inputEl) return;
  const text = (inputEl.value || '').trim();
  if (!text) {
    showToast('Vui lòng nhập đoạn văn bản tiếng Nhật cần phân tích.', 'warning');
    inputEl.focus();
    return;
  }

  // Chuyển sang màn hình Loading
  switchScreen('loading');
  if (btnAnalyzeEl) {
    btnAnalyzeEl.disabled = true;
    btnAnalyzeEl.classList.add('opacity-70', 'cursor-not-allowed');
  }

  const payload = {
    text: text,
    targetJlpt: selectJlptEl ? selectJlptEl.value : 'ALL',
    includeFurigana: true
  };

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 25000);

  try {
    const headers = { 'Content-Type': 'application/json' };
    const token = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/learner/reading/analyze', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload),
      signal: controller.signal
    });

    if (!res.ok) {
      throw new Error(`Mã lỗi máy chủ: ${res.status}`);
    }

    const data = await res.json();
    currentAnalysisResult = data;
    renderAnalysisResult(data);

    // Chuyển sang Không gian đọc hiểu Dokkai
    switchScreen('workspace');
    showToast('Phân tích đọc hiểu thành công!', 'success');
  } catch (err) {
    console.error('[AI Reading] Lỗi phân tích:', err);
    if (err.name === 'AbortError') {
      showToast('Quá thời gian phản hồi từ máy chủ (25s). Vui lòng thử lại với đoạn văn ngắn hơn.', 'error');
    } else {
      showToast('Không thể phân tích văn bản. Vui lòng thử lại sau.', 'error');
    }
    switchScreen('input');
  } finally {
    clearTimeout(timeoutId);
    if (btnAnalyzeEl) {
      btnAnalyzeEl.disabled = false;
      btnAnalyzeEl.classList.remove('opacity-70', 'cursor-not-allowed');
    }
  }
}

// =========================================================================
// 5. HIỂN THỊ KẾT QUẢ ĐỌC HIỂU DOKKAI RA GIAO DIỆN
// =========================================================================
function renderAnalysisResult(data) {
  // Cấp độ ước tính
  if (badgeEstimatedLevelEl) {
    const level = data.estimatedLevel || 'N5';
    badgeEstimatedLevelEl.textContent = `JLPT ${level}`;
  }

  // Tóm tắt ý chính
  if (textOverallSummaryEl) {
    textOverallSummaryEl.textContent = data.overallSummary || 'Không có tóm tắt ý chính.';
  }

  // 1. Render bài đọc sạch ở trên cùng (Ruby Furigana chuẩn trên đầu chữ Hán)
  let renderedRuby = data.rubyHtml;
  if (!renderedRuby || !renderedRuby.includes('<ruby>')) {
    // Dự phòng tự động bổ sung thẻ ruby từ danh sách từ vựng nếu rubyHtml thiếu
    let baseText = escapeHtml(data.originalText || '');
    if (Array.isArray(data.vocabularies) && data.vocabularies.length > 0) {
      data.vocabularies.forEach(v => {
        const kanji = v.kanji || v.surfaceText;
        const reading = v.reading;
        if (kanji && reading && kanji !== reading && /[\u4e00-\u9faf]/.test(kanji) && baseText.includes(kanji)) {
          baseText = baseText.replaceAll(kanji, `<ruby>${kanji}<rt>${reading}</rt></ruby>`);
        }
      });
    }
    renderedRuby = baseText;
    data.rubyHtml = renderedRuby;
  }

  if (cleanReadingPassageBodyEl) {
    cleanReadingPassageBodyEl.innerHTML = renderedRuby;
    cleanReadingPassageBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
  }

  // 2. Render bản dịch tiếng Việt toàn bài dạng đoạn văn liền mạch (bấm từng câu để đối chiếu)
  if (fullTranslationBodyEl) {
    const sentences = buildTranslationSentences(data.sentenceTranslations, data.fullTranslation);
    if (sentences.length > 0) {
      const spansHtml = sentences
        .map((sentence, idx) => `<span class="translation-sentence-link cursor-pointer hover:bg-amber-100/80 rounded px-0.5 transition-colors" data-sentence-index="${idx}" title="Bấm để xem câu tiếng Nhật tương ứng">${escapeHtml(sentence)}</span>`)
        .join(' ');
      fullTranslationBodyEl.innerHTML = `<p class="leading-relaxed font-sans text-slate-800">${spansHtml}</p>`;
    } else {
      const fallbackText = data.overallSummary || 'Chưa có bản dịch toàn bài.';
      fullTranslationBodyEl.innerHTML = `<p class="leading-relaxed font-sans text-slate-800">${escapeHtml(fallbackText)}</p>`;
    }
  }

  // Khởi tạo trạng thái mặc định ẨN HẾT cho toàn bộ các tác vụ hỗ trợ
  isFuriganaHidden = true;
  isSummaryVisible = false;
  isTranslationVisible = false;

  syncFuriganaUI();
  if (overallSummaryBoxEl) overallSummaryBoxEl.classList.add('hidden');
  if (cleanPassageTranslationContainerEl) cleanPassageTranslationContainerEl.classList.add('hidden');
  if (summaryToggleTextEl) summaryToggleTextEl.textContent = 'Hiện ý chính';
  if (translationToggleTextEl) translationToggleTextEl.textContent = 'Hiện bản dịch';
  if (furiganaToggleTextEl) furiganaToggleTextEl.textContent = 'Hiện Furigana';
  if (btnToggleSummaryEl) {
    btnToggleSummaryEl.classList.remove('bg-amber-100', 'border-amber-400');
  }
  if (btnToggleTranslationEl) {
    btnToggleTranslationEl.classList.remove('bg-indigo-100', 'border-indigo-400');
  }
  if (btnToggleFuriganaEl) {
    btnToggleFuriganaEl.classList.remove('bg-rose-100', 'border-rose-400');
  }

  // 3. Render bộ câu hỏi Đọc hiểu Dokkai ở dưới (Không chứa tag loại câu hỏi)
  renderDokkaiChallenge(data);
}

// =========================================================================
// 6. RENDER BỘ CÂU HỎI TRẮC NGHIỆM DOKKAI (KHÔNG CHỨA TAG RƯỜM RÀ)
// =========================================================================
function renderDokkaiChallenge(data) {
  const questions = data.comprehensionQuestions || [];
  dokkaiAnswers = {};
  dokkaiSubmitted = false;

  if (countDokkaiQuestionsHeaderEl) {
    countDokkaiQuestionsHeaderEl.textContent = questions.length;
  }

  // Ẩn bảng điểm cũ
  if (dokkaiScoreSummaryEl) {
    dokkaiScoreSummaryEl.classList.add('hidden');
    dokkaiScoreSummaryEl.innerHTML = '';
  }

  // Khôi phục nút Nộp bài
  if (btnSubmitDokkaiEl) {
    btnSubmitDokkaiEl.disabled = false;
    btnSubmitDokkaiEl.classList.remove('opacity-60', 'cursor-not-allowed');
  }
  if (btnSubmitDokkaiTextEl) {
    btnSubmitDokkaiTextEl.textContent = 'Nộp bài & Chấm điểm AI';
  }

  if (!dokkaiQuestionsContainerEl) return;
  dokkaiQuestionsContainerEl.innerHTML = '';

  if (questions.length === 0) {
    dokkaiQuestionsContainerEl.innerHTML = `
      <div class="ls-card p-6 text-center space-y-3 bg-amber-50/60 border border-amber-200">
        <span class="material-symbols-outlined text-amber-600 text-[36px]">psychology_alt</span>
        <div class="space-y-1">
          <h4 class="text-sm font-bold text-slate-800">Chưa có câu hỏi đọc hiểu cho bài này</h4>
          <p class="text-xs text-slate-500">Đoạn văn quá ngắn hoặc mô hình AI chưa tạo được câu hỏi trắc nghiệm phù hợp.</p>
        </div>
      </div>
    `;
    if (btnSubmitDokkaiEl && btnSubmitDokkaiEl.parentElement) {
      btnSubmitDokkaiEl.parentElement.classList.add('hidden');
    }
    return;
  }

  if (btnSubmitDokkaiEl && btnSubmitDokkaiEl.parentElement) {
    btnSubmitDokkaiEl.parentElement.classList.remove('hidden');
  }

  questions.forEach((q, qIndex) => {
    const card = document.createElement('div');
    card.className = 'ls-card p-4 space-y-3 border border-slate-200 dokkai-question-card';
    card.dataset.questionId = q.id;

    let optionsHtml = '';
    const letters = ['A', 'B', 'C', 'D'];
    (q.options || []).forEach((opt, optIdx) => {
      const letter = letters[optIdx] || String(optIdx + 1);
      const viOpt = (q.optionsVi && q.optionsVi[optIdx]) ? q.optionsVi[optIdx] : '';
      optionsHtml += `
        <label class="dokkai-option-item flex items-start gap-3 p-3 rounded-xl border border-slate-200 bg-white hover:border-[#9F1239] hover:bg-rose-50/30 cursor-pointer transition select-none group" data-opt-index="${optIdx}">
          <input type="radio" name="dokkai_q_${q.id}" value="${optIdx}" class="mt-0.5 text-[#9F1239] focus:ring-[#9F1239] cursor-pointer" />
          <div class="flex-1 text-xs sm:text-sm leading-relaxed">
            <div class="font-japanese text-slate-800 group-hover:text-slate-900 font-medium">
              <span class="font-bold text-slate-500 font-sans mr-1">${letter}.</span> ${escapeHtml(opt)}
            </div>
            ${viOpt ? `
              <div class="dokkai-translation-text text-[11px] text-slate-500 font-sans mt-1 ml-4 hidden">
                ${escapeHtml(viOpt)}
              </div>
            ` : ''}
          </div>
        </label>
      `;
    });

    const viQuestion = q.questionVi || '';
    const hasQuestionTranslation = Boolean(viQuestion || (q.optionsVi && q.optionsVi.some(Boolean)));

    // Header câu hỏi tinh gọn: Chỉ hiển thị Câu hỏi số thứ tự + nút dịch riêng cho từng câu
    card.innerHTML = `
      <div class="flex items-center justify-between gap-2 border-b border-slate-100 pb-2">
        <div class="flex items-center gap-2">
          <span class="w-6 h-6 rounded-full bg-[#9F1239] text-white text-xs font-bold flex items-center justify-center shrink-0">
            ${qIndex + 1}
          </span>
          <span class="text-xs font-bold text-slate-800">Câu hỏi ${qIndex + 1}</span>
        </div>
        ${hasQuestionTranslation ? `
          <button type="button"
                  class="btn-toggle-question-translation px-2.5 py-1 text-[11px] font-semibold rounded-md border border-[#CBD5E1] bg-white text-slate-600 hover:text-indigo-700 hover:border-indigo-300 hover:bg-indigo-50/40 transition cursor-pointer flex items-center gap-1 shadow-2xs"
                  aria-expanded="false"
                  title="Bật/Tắt bản dịch tiếng Việt cho câu hỏi này">
            <span class="material-symbols-outlined text-[14px] text-indigo-600">translate</span>
            <span class="question-translation-toggle-text">Hiện dịch</span>
          </button>
        ` : ''}
      </div>

      <div class="space-y-1">
        <div class="text-xs sm:text-sm font-bold text-slate-900 font-japanese leading-relaxed">
          ${escapeHtml(q.question || '')}
        </div>
        ${viQuestion ? `
          <div class="dokkai-translation-text text-xs text-indigo-700/80 font-sans font-medium italic hidden">
            💡 ${escapeHtml(viQuestion)}
          </div>
        ` : ''}
      </div>

      <div class="space-y-2 pt-1" id="optionsGroup_${q.id}">
        ${optionsHtml}
      </div>

      <!-- Khung lời giải phân tích bẫy (ẩn trước khi nộp bài) -->
      <div class="dokkai-explanation-box hidden p-3.5 rounded-xl text-xs space-y-2.5">
        <!-- Rendered after submit -->
      </div>
    `;

    // Nút bật/tắt dịch riêng cho câu hỏi này
    const btnToggleQTrans = card.querySelector('.btn-toggle-question-translation');
    if (btnToggleQTrans) {
      let isQuestionTranslationShown = false;
      btnToggleQTrans.addEventListener('click', () => {
        isQuestionTranslationShown = !isQuestionTranslationShown;
        card.querySelectorAll('.dokkai-translation-text').forEach((el) => {
          el.classList.toggle('hidden', !isQuestionTranslationShown);
        });
        const labelEl = btnToggleQTrans.querySelector('.question-translation-toggle-text');
        if (labelEl) {
          labelEl.textContent = isQuestionTranslationShown ? 'Ẩn dịch' : 'Hiện dịch';
        }
        btnToggleQTrans.setAttribute('aria-expanded', String(isQuestionTranslationShown));
        btnToggleQTrans.classList.toggle('bg-indigo-50', isQuestionTranslationShown);
        btnToggleQTrans.classList.toggle('border-indigo-300', isQuestionTranslationShown);
        btnToggleQTrans.classList.toggle('text-indigo-700', isQuestionTranslationShown);
      });
    }

    // Lắng nghe sự kiện chọn radio
    const radioInputs = card.querySelectorAll(`input[name="dokkai_q_${q.id}"]`);
    radioInputs.forEach(radio => {
      radio.addEventListener('change', () => {
        dokkaiAnswers[q.id] = parseInt(radio.value, 10);
        card.querySelectorAll('.dokkai-option-item').forEach(lbl => {
          lbl.classList.remove('border-[#9F1239]', 'bg-rose-50/50');
        });
        radio.closest('.dokkai-option-item').classList.add('border-[#9F1239]', 'bg-rose-50/50');
      });
    });

    dokkaiQuestionsContainerEl.appendChild(card);
  });
}

// =========================================================================
// 7. XỬ LÝ CHẤM ĐIỂM & ĐỐI CHIẾU DẪN CHỨNG
// =========================================================================
function handleDokkaiSubmit() {
  if (!currentAnalysisResult || !currentAnalysisResult.comprehensionQuestions) return;
  const questions = currentAnalysisResult.comprehensionQuestions;
  if (questions.length === 0) return;

  const answeredCount = Object.keys(dokkaiAnswers).length;
  if (answeredCount < questions.length && !dokkaiSubmitted) {
    const unAnswered = questions.length - answeredCount;
    if (!confirm(`Bạn còn ${unAnswered} câu chưa trả lời. Bạn có chắc chắn muốn nộp bài để xem chấm điểm và phân tích không?`)) {
      return;
    }
  }

  dokkaiSubmitted = true;
  let correctCount = 0;

  questions.forEach(q => {
    const userAns = dokkaiAnswers[q.id];
    const isCorrect = userAns === q.correctOptionIndex;
    if (isCorrect) correctCount++;

    const card = dokkaiQuestionsContainerEl.querySelector(`.dokkai-question-card[data-question-id="${q.id}"]`);
    if (!card) return;

    card.querySelectorAll('input[type="radio"]').forEach(r => { r.disabled = true; });

    card.querySelectorAll('.dokkai-option-item').forEach(lbl => {
      const idx = parseInt(lbl.dataset.optIndex, 10);
      lbl.classList.remove('border-[#9F1239]', 'bg-rose-50/50', 'hover:border-[#9F1239]', 'cursor-pointer');
      lbl.classList.add('cursor-default');

      if (idx === q.correctOptionIndex) {
        lbl.classList.add('border-emerald-500', 'bg-emerald-50/80', 'text-emerald-900', 'font-semibold');
        lbl.insertAdjacentHTML('beforeend', '<span class="material-symbols-outlined text-emerald-600 text-[18px] ml-auto shrink-0">check_circle</span>');
      } else if (idx === userAns && !isCorrect) {
        lbl.classList.add('border-rose-500', 'bg-rose-50/80', 'text-rose-900');
        lbl.insertAdjacentHTML('beforeend', '<span class="material-symbols-outlined text-rose-600 text-[18px] ml-auto shrink-0">cancel</span>');
      }
    });

    const explBox = card.querySelector('.dokkai-explanation-box');
    if (explBox) {
      explBox.classList.remove('hidden');
      explBox.className = `dokkai-explanation-box p-3.5 rounded-xl text-xs space-y-2 border ${
        isCorrect ? 'bg-emerald-50/60 border-emerald-200 text-emerald-900' : 'bg-rose-50/60 border-rose-200 text-slate-800'
      }`;

      explBox.innerHTML = `
        <div class="flex items-center justify-between gap-2 border-b border-slate-200/60 pb-1.5 flex-wrap">
          <span class="font-bold flex items-center gap-1 ${isCorrect ? 'text-emerald-700' : 'text-rose-700'}">
            <span class="material-symbols-outlined text-[16px]">${isCorrect ? 'check_circle' : 'error'}</span>
            <span>${isCorrect ? 'Chính xác!' : 'Chưa chính xác'}</span>
          </span>
          ${q.evidenceQuote ? `
            <button type="button" class="btn-show-evidence px-2.5 py-1 rounded-md bg-white border border-slate-200 text-slate-700 hover:text-[#9F1239] hover:border-[#9F1239] text-[11px] font-bold flex items-center gap-1 cursor-pointer transition shadow-2xs" data-evidence="${escapeHtml(q.evidenceQuote)}">
              <span class="material-symbols-outlined text-[14px] text-amber-500">search_check</span>
              <span>🔍 Xem dẫn chứng trong bài</span>
            </button>
          ` : ''}
        </div>
        <p class="leading-relaxed text-slate-700">${escapeHtml(q.explanation || 'Không có giải thích chi tiết.')}</p>
      `;

      const btnEvidence = explBox.querySelector('.btn-show-evidence');
      if (btnEvidence) {
        btnEvidence.addEventListener('click', () => {
          highlightEvidenceInCleanPassage(q.evidenceQuote);
        });
      }
    }
  });

  // Hiển thị card điểm số
  if (dokkaiScoreSummaryEl) {
    dokkaiScoreSummaryEl.classList.remove('hidden');
    const total = questions.length;
    const percent = Math.round((correctCount / total) * 100);
    const isPassed = percent >= 60;

    dokkaiScoreSummaryEl.className = `ls-card p-4 space-y-2 border-2 ${
      isPassed ? 'border-emerald-300 bg-emerald-50/50' : 'border-amber-300 bg-amber-50/50'
    }`;

    dokkaiScoreSummaryEl.innerHTML = `
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div class="flex items-center gap-2.5">
          <span class="material-symbols-outlined ${isPassed ? 'text-emerald-600' : 'text-amber-600'} text-[28px] shrink-0">
            ${isPassed ? 'military_tech' : 'psychology'}
          </span>
          <div>
            <h4 class="text-sm font-bold text-slate-900">Kết quả Đọc hiểu: ${correctCount} / ${total} câu đúng (${percent}%)</h4>
            <p class="text-[11px] text-slate-500">
              ${isPassed ? 'Rất tốt! Bạn đã nắm bắt chính xác ý tứ và quan điểm của bài đọc.' : 'Cố gắng lên! Hãy xem giải thích và bấm "Xem dẫn chứng trong bài" để hiểu rõ hơn.'}
            </p>
          </div>
        </div>
        <button type="button" id="btnRestartDokkai" class="px-3.5 py-1.5 rounded-lg bg-white border border-slate-300 hover:border-[#9F1239] text-slate-700 hover:text-[#9F1239] text-xs font-bold shadow-2xs cursor-pointer flex items-center justify-center gap-1 shrink-0 transition">
          <span class="material-symbols-outlined text-[15px]">refresh</span>
          <span>Làm lại bài đọc</span>
        </button>
      </div>
    `;

    const btnRestart = dokkaiScoreSummaryEl.querySelector('#btnRestartDokkai');
    if (btnRestart) {
      btnRestart.addEventListener('click', () => {
        if (currentAnalysisResult) {
          renderDokkaiChallenge(currentAnalysisResult);
          window.scrollTo({ top: 0, behavior: 'smooth' });
          showToast('Đã làm mới bộ câu hỏi đọc hiểu', 'info');
        }
      });
    }
    dokkaiScoreSummaryEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  if (btnSubmitDokkaiEl) {
    btnSubmitDokkaiEl.disabled = true;
    btnSubmitDokkaiEl.classList.add('opacity-60', 'cursor-not-allowed');
  }
  if (btnSubmitDokkaiTextEl) {
    btnSubmitDokkaiTextEl.textContent = 'Đã nộp bài & Có lời giải chi tiết';
  }
  showToast(`Đã chấm điểm: ${correctCount}/${questions.length} câu đúng!`, correctCount === questions.length ? 'success' : 'info');
}

// Đánh dấu dẫn chứng trong văn bản bài đọc và tự động cuộn lên
function highlightEvidenceInCleanPassage(quote) {
  if (!cleanReadingPassageBodyEl || !quote) return;
  const cleanQuote = quote.trim().replace(/^[「『"'\s]+/, '').replace(/[」』"'。！？\s]+$/, '');
  if (!cleanQuote) return;

  // Khôi phục bài đọc sạch ban đầu
  cleanReadingPassageBodyEl.innerHTML = (currentAnalysisResult && currentAnalysisResult.rubyHtml)
    ? currentAnalysisResult.rubyHtml
    : escapeHtml((currentAnalysisResult && currentAnalysisResult.originalText) || '');

  const passageHtml = cleanReadingPassageBodyEl.innerHTML;

  // 1. Sử dụng Regex thấu hiểu Ruby để tìm và bọc <mark> mà không làm mất furigana
  const rubyRegex = createRubyAwareRegex(cleanQuote);
  let highlighted = false;

  if (rubyRegex && rubyRegex.test(passageHtml)) {
    cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(rubyRegex, (matched) => `<mark class="evidence-highlight">${matched}</mark>`);
    highlighted = true;
  } else if (passageHtml.includes(cleanQuote)) {
    // 2. Dự phòng khi câu không có thẻ ruby
    cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(cleanQuote, `<mark class="evidence-highlight">${cleanQuote}</mark>`);
    highlighted = true;
  } else {
    // 3. Dự phòng tìm câu khớp nhất trong văn bản gốc (khi AI lược chữ hoặc thêm ...)
    const bestSentence = findBestMatchingSentence(cleanQuote, (currentAnalysisResult && currentAnalysisResult.originalText) || '');
    if (bestSentence) {
      const bestClean = bestSentence.trim().replace(/^[「『"'\s]+/, '').replace(/[」』"'。！？\s]+$/, '');
      const bestRegex = createRubyAwareRegex(bestClean);
      if (bestRegex && bestRegex.test(passageHtml)) {
        cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(bestRegex, (matched) => `<mark class="evidence-highlight">${matched}</mark>`);
        highlighted = true;
      } else if (bestClean && passageHtml.includes(bestClean)) {
        cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(bestClean, `<mark class="evidence-highlight">${bestClean}</mark>`);
        highlighted = true;
      }
    }

    // 4. Dự phòng cắt theo dấu phẩy hoặc cụm từ con ngắn hơn
    if (!highlighted) {
      const clauses = cleanQuote.split(/[、,，…・]/).map(c => c.trim()).filter(c => c.length >= 4);
      for (const clause of clauses) {
        const clauseRegex = createRubyAwareRegex(clause);
        if (clauseRegex && clauseRegex.test(passageHtml)) {
          cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(clauseRegex, (matched) => `<mark class="evidence-highlight">${matched}</mark>`);
          highlighted = true;
          break;
        }
      }
    }

    if (!highlighted) {
      for (let len = Math.min(cleanQuote.length, 16); len >= 6; len -= 2) {
        const subQuote = cleanQuote.slice(0, len);
        const subRegex = createRubyAwareRegex(subQuote);
        if (subRegex && subRegex.test(passageHtml)) {
          cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(subRegex, (matched) => `<mark class="evidence-highlight">${matched}</mark>`);
          highlighted = true;
          break;
        }
      }
    }
  }

  // Duy trì trạng thái ẩn/hiện Furigana theo thiết lập hiện tại của người dùng
  cleanReadingPassageBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);

  // Cuộn trang mượt mà lên vị trí dẫn chứng trên bài đọc (chỉ highlight trong bài, không hiện toast)
  const highlightEl = cleanReadingPassageBodyEl.querySelector('.evidence-highlight');
  if (highlightEl) {
    highlightEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
  } else {
    cleanReadingPassageBodyEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }
}

// Regex thông minh nhận diện từ/câu kể cả khi bị bọc thẻ <ruby>
function createRubyAwareRegex(phrase) {
  if (!phrase) return null;
  const clean = phrase.replace(/^[〜~～「『"'\s]+/, '').replace(/[〜~～」』"'。！？\s]+$/, '');
  if (!clean) return null;

  const chars = Array.from(clean);
  if (chars.length === 0) return null;

  const interTag = '(?:\\s*<rt>[^<]*<\\/rt>|\\s*<[^>]+>)*\\s*';
  const parts = chars.map((ch) => {
    const esc = ch.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    if (ch === '\n' || ch === '\r') {
      return '(?:<br\\s*\\/?>|\\s)*';
    }
    if (ch === '、' || ch === '。' || ch === '，' || ch === '．' || ch === ' ' || ch === '　') {
      return '(?:' + esc + '|[、。，．\\s])*';
    }
    return esc;
  });

  const pattern = '(?:<ruby[^>]*>\\s*)?' + parts.join(interTag) + '(?:\\s*<rt>[^<]*<\\/rt>\\s*<\\/ruby>)?';
  try {
    return new RegExp(pattern, 'g');
  } catch (e) {
    return null;
  }
}

// Hàm tiện ích escape HTML
function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
