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
  pageTitle: 'Luyện Đọc Hiểu AI 📖',
  badge: 'Trợ lý đọc hiểu Dokkai'
});

// Các đoạn văn bản mẫu
const SAMPLE_PRESETS = {
  n5: '私は毎朝7時に起きて、公園を散歩します。天気がいい日は富士山がきれいに見えます。途中でパン屋に寄って、焼きたてのパンとコーヒーを買います。家に帰ってから朝ご飯を食べます。',
  tokyo: '先週の日曜日、私は友達と東京へ行きました。朝9時に駅で友達に会いました。それから、電車で浅草へ行きました。浅草は人がたくさんいましたが、とてもにぎやかで面白かったです。私たちは大きいお寺を見たり、写真を撮ったりしました。昼ごはんに美味しいラーメンを食べました。午後、デパートで家族にお土産を買いました。少し疲れましたが、とても楽しかったです。また友達と東京へ行きたいです。',
  n4: '先週の日曜日、友達と一緒に京都へ旅行に行きました。新幹線で2時間くらいかかりました。金閣寺を見たり、抹茶アイスを食べたりして、とても楽しかったです。雨が降っても着物を着て写真を撮りました。',
  n3: '日本語が上手になるように、毎朝NHKのやさしいニュースを聞くことにしています。分からない単語があってもすぐに諦めないで、文脈から意味を推測するように努力しています。継続することが一番大切だと思います。',
  tsang: '「人生最大の贈り物は友情で, 私はもう手に入れました!」 ツァン君は私の同じ大学です。 彼は背が高くて、ちょっとスリムで目が明るくて、かっこい人です。 初めて出会った相手は建築大学の入学手続きをした時でした。彼は私を声を掛けました。 それから、毎日一緒に大学に行ったり、ハノイの様々な素晴らしい観光地に行ったりしました。春夏秋冬のいい記念を作って来ました。 そばにいるとき、彼はいつでも笑います。私にツァン君の笑みは一番素晴らしものです。彼は我慢強いや強気を教えてくれました。 今まで、私たちはお二人のこと知っているのは10年間です。これからもずっといい友達になりたいです。 ツァン君に：「これからは初めてのように手伝ってくれたいです」。'
};

// Trạng thái ứng dụng
let currentAnalysisResult = null;
let userDecksList = [];
let pendingSaveWord = null;
let isFuriganaHidden = false;
let dokkaiAnswers = {};
let dokkaiSubmitted = false;

// DOM Elements: Điều hướng 2 màn hình
const inputScreenEl = document.getElementById('readingInputScreen');
const workspaceScreenEl = document.getElementById('readingWorkspaceScreen');
const btnBackToInputEl = document.getElementById('btnBackToInput');

// DOM Elements: Màn hình 1 (Input Studio)
const inputEl = document.getElementById('readingInputText');
const charCountEl = document.getElementById('charCountLabel');
const selectJlptEl = document.getElementById('selectJlptTarget');
const btnClearEl = document.getElementById('btnClearInput');
const btnAnalyzeEl = document.getElementById('btnAnalyzeReading');
const loadingStateEl = document.getElementById('readingLoadingState');

// DOM Elements: Màn hình 2 (Reader Workspace)
const badgeEstimatedLevelEl = document.getElementById('badgeEstimatedLevel');
const interactiveReadingBodyEl = document.getElementById('interactiveReadingBody');
const textOverallSummaryEl = document.getElementById('textOverallSummary');
const btnToggleFuriganaEl = document.getElementById('btnToggleFurigana');
const furiganaToggleTextEl = document.getElementById('furiganaToggleText');
const btnSpeakFullEl = document.getElementById('btnSpeakFullArticle');

// DOM Elements: Thử thách Đọc hiểu Dokkai (Tab 1)
const countDokkaiQuestionsEl = document.getElementById('countDokkaiQuestions');
const cleanReadingPassageBodyEl = document.getElementById('cleanReadingPassageBody');
const btnToggleCleanFuriganaEl = document.getElementById('btnToggleCleanFurigana');
const cleanFuriganaToggleTextEl = document.getElementById('cleanFuriganaToggleText');
const dokkaiQuestionsContainerEl = document.getElementById('dokkaiQuestionsContainer');
const dokkaiScoreSummaryEl = document.getElementById('dokkaiScoreSummary');
const btnSubmitDokkaiEl = document.getElementById('btnSubmitDokkai');
const btnSubmitDokkaiTextEl = document.getElementById('btnSubmitDokkaiText');

const countGrammarsEl = document.getElementById('countGrammars');
const grammarListContainerEl = document.getElementById('grammarListContainer');
const countVocabsEl = document.getElementById('countVocabs');
const vocabularyGridContainerEl = document.getElementById('vocabularyGridContainer');
const sentenceTranslationsContainerEl = document.getElementById('sentenceTranslationsContainer');

// Tabs & Tab Panels
const tabButtons = document.querySelectorAll('.reading-tab-btn');
const tabPanels = {
  challenge: document.getElementById('tabPanelChallenge'),
  reader: document.getElementById('tabPanelReader'),
  vocab: document.getElementById('tabPanelVocab'),
  grammar: document.getElementById('tabPanelGrammar'),
  translation: document.getElementById('tabPanelTranslation')
};

// Modal Lưu Flashcard
const modalSaveEl = document.getElementById('modalSaveToDeck');
const btnCloseModalEl = document.getElementById('btnCloseSaveModal');
const btnCancelModalEl = document.getElementById('btnCancelModal');
const btnConfirmSaveCardEl = document.getElementById('btnConfirmSaveCard');
const selectTargetDeckEl = document.getElementById('selectTargetDeck');
const modalTargetWordEl = document.getElementById('modalTargetWord');
const modalTargetReadingEl = document.getElementById('modalTargetReading');
const modalTargetMeaningEl = document.getElementById('modalTargetMeaning');

// Tooltip Tương Tác Nổi (Watanoc Interactive Tooltip Elements)
const tooltipEl = document.getElementById('readingFloatingTooltip');
const tooltipArrowEl = document.getElementById('tooltipArrow');
const tooltipBadgeTypeEl = document.getElementById('tooltipBadgeType');
const tooltipBadgeJlptEl = document.getElementById('tooltipBadgeJlpt');
const tooltipHeadwordEl = document.getElementById('tooltipHeadword');
const tooltipReadingHanVietEl = document.getElementById('tooltipReadingHanViet');
const tooltipMeaningEl = document.getElementById('tooltipMeaning');
const tooltipExtraEl = document.getElementById('tooltipExtra');
const btnTooltipSpeakEl = document.getElementById('btnTooltipSpeak');
const btnTooltipCloseEl = document.getElementById('btnTooltipClose');
const btnTooltipSaveFlashcardEl = document.getElementById('btnTooltipSaveFlashcard');
const btnTooltipDetailLinkEl = document.getElementById('btnTooltipDetailLink');
const tooltipDetailLinkTextEl = document.getElementById('tooltipDetailLinkText');

// State cho Interactive Tokens & Tooltip
let activeTokensMap = new Map();
let currentActiveTokenEl = null;
let currentTooltipToken = null;
let tooltipShowTimer = null;
let tooltipHideTimer = null;

// =========================================================================
// 1.5. BỘ ĐIỀU KHIỂN CHUYỂN MÀN HÌNH & CHUYỂN TABS (NAVIGATION CONTROLLER)
// =========================================================================

function switchScreen(screenName) {
  hideTooltip();
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

function switchTab(tabKey) {
  hideTooltip();

  // Đổi trạng thái hiển thị của các nút tab
  tabButtons.forEach(btn => {
    if (btn.dataset.tab === tabKey) {
      btn.classList.add('active', 'border-slate-300');
      btn.classList.remove('text-slate-600');
    } else {
      btn.classList.remove('active', 'border-slate-300');
      btn.classList.add('text-slate-600');
    }
  });

  // Ẩn/Hiện các panel tương ứng
  Object.keys(tabPanels).forEach(key => {
    const panel = tabPanels[key];
    if (panel) {
      if (key === tabKey) {
        panel.classList.remove('hidden');
      } else {
        panel.classList.add('hidden');
      }
    }
  });
}

// =========================================================================
// 2. KHỞI TẠO & LẮNG NGHE SỰ KIỆN
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  checkVoicevoxStatus();
  fetchUserDecks();
});

function setupEventListeners() {
  // Nút quay lại màn hình nhập liệu
  if (btnBackToInputEl) {
    btnBackToInputEl.addEventListener('click', () => {
      switchScreen('input');
    });
  }

  // Sự kiện chuyển các Tabs ở Màn hình 2
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.dataset.tab;
      if (tab) switchTab(tab);
    });
  });

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
          if (presetKey === 'tsang') {
            selectJlptEl.value = 'ALL';
          } else {
            selectJlptEl.value = presetKey.toUpperCase();
          }
        }
        showToast(`Đã nạp văn bản mẫu thành công`, 'info');
      }
    });
  });

  // Nút Phân Tích
  if (btnAnalyzeEl) {
    btnAnalyzeEl.addEventListener('click', executeReadingAnalysis);
  }

  // Nút Bật/Tắt Furigana trên thanh công cụ Workspace
  if (btnToggleFuriganaEl) {
    btnToggleFuriganaEl.addEventListener('click', () => {
      isFuriganaHidden = !isFuriganaHidden;
      syncFuriganaUI();
      showToast(isFuriganaHidden ? 'Đã ẩn Furigana để bạn tự đọc' : 'Đã hiện Furigana hỗ trợ đọc', 'info');
    });
  }

  // Nút Bật/Tắt Furigana trên Khung bài đọc sạch (Tab 1)
  if (btnToggleCleanFuriganaEl) {
    btnToggleCleanFuriganaEl.addEventListener('click', () => {
      isFuriganaHidden = !isFuriganaHidden;
      syncFuriganaUI();
      showToast(isFuriganaHidden ? 'Đã ẩn Furigana để bạn tự đọc' : 'Đã hiện Furigana hỗ trợ đọc', 'info');
    });
  }

  // Nút Nộp bài & Chấm điểm Đọc hiểu
  if (btnSubmitDokkaiEl) {
    btnSubmitDokkaiEl.addEventListener('click', handleDokkaiSubmit);
  }

  // Nút Đọc Toàn Bài
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

  // Sự kiện cho Interactive Tooltip (Hover Bridge & Actions)
  if (tooltipEl) {
    tooltipEl.addEventListener('mouseenter', () => {
      clearTimeout(tooltipHideTimer);
    });
    tooltipEl.addEventListener('mouseleave', () => {
      tooltipHideTimer = setTimeout(() => {
        hideTooltip();
      }, 250);
    });
  }

  if (btnTooltipCloseEl) {
    btnTooltipCloseEl.addEventListener('click', (e) => {
      e.stopPropagation();
      hideTooltip();
    });
  }

  if (btnTooltipSpeakEl) {
    btnTooltipSpeakEl.addEventListener('click', (e) => {
      e.stopPropagation();
      if (!currentTooltipToken) return;
      const t = currentTooltipToken;
      const textToSpeak = (t.type === 'vocab' || t.type === 'kanji')
        ? (t.data.surfaceText || t.data.kanji || t.data.reading)
        : (t.data.matchedText || t.data.pattern);
      btnTooltipSpeakEl.classList.add('ring-2', 'ring-rose-400');
      voicevoxClient.speak(textToSpeak).finally(() => {
        btnTooltipSpeakEl.classList.remove('ring-2', 'ring-rose-400');
      });
    });
  }

  if (btnTooltipSaveFlashcardEl) {
    btnTooltipSaveFlashcardEl.addEventListener('click', (e) => {
      e.stopPropagation();
      if (currentTooltipToken && (currentTooltipToken.type === 'vocab' || currentTooltipToken.type === 'kanji')) {
        openSaveModal(currentTooltipToken.data);
      }
    });
  }

  // Đóng Tooltip khi click ra ngoài vùng bài đọc
  document.addEventListener('click', (e) => {
    if (tooltipEl && !tooltipEl.contains(e.target) && !e.target.closest('.reading-token')) {
      hideTooltip();
    }
  });

  // Tự động căn lại vị trí Tooltip khi cuộn trang
  window.addEventListener('scroll', () => {
    if (currentActiveTokenEl && tooltipEl && !tooltipEl.classList.contains('hidden')) {
      positionTooltip(currentActiveTokenEl);
    }
  }, { passive: true });
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

  // Chuyển sang màn hình Loading
  switchScreen('loading');
  btnAnalyzeEl.disabled = true;
  btnAnalyzeEl.classList.add('opacity-70', 'cursor-not-allowed');

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

    // Chuyển sang Màn hình Không gian đọc và mở tab Thử thách Đọc hiểu mặc định
    switchScreen('workspace');
    switchTab('challenge');
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
    btnAnalyzeEl.disabled = false;
    btnAnalyzeEl.classList.remove('opacity-70', 'cursor-not-allowed');
  }
}

// =========================================================================
// 5. HIỂN THỊ KẾT QUẢ PHÂN TÍCH RA GIAO DIỆN
// =========================================================================
function renderAnalysisResult(data) {
  // Cấp độ ước tính
  if (badgeEstimatedLevelEl) {
    const level = data.estimatedLevel || 'N5';
    badgeEstimatedLevelEl.textContent = `JLPT ${level}`;
  }

  // Tóm tắt ý chính
  if (textOverallSummaryEl) {
    textOverallSummaryEl.textContent = data.overallSummary || 'Không có tóm tắt.';
  }

  // 0. Render Thử thách Đọc hiểu Dokkai (Tab 1)
  renderDokkaiChallenge(data);

  // 1. Render Bài đọc có Furigana & Annotated Tokens (Mô hình Watanoc)
  if (interactiveReadingBodyEl) {
    const annotatedHtml = buildAnnotatedReadingHtml(
      data.rubyHtml,
      data.originalText,
      data.vocabularies || [],
      data.detectedGrammars || []
    );
    interactiveReadingBodyEl.innerHTML = annotatedHtml;
    interactiveReadingBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
    attachTokenEventListeners();
  }

  // 2. Render Ngữ pháp
  const grammars = data.detectedGrammars || [];
  if (countGrammarsEl) countGrammarsEl.textContent = grammars.length;
  if (grammarListContainerEl) {
    grammarListContainerEl.innerHTML = '';

    if (grammars.length === 0) {
      grammarListContainerEl.innerHTML = '<div class="col-span-2 text-xs text-slate-400 italic p-6 text-center bg-slate-50 rounded-xl">Không phát hiện cấu trúc ngữ pháp đặc biệt trong bài đọc này.</div>';
    } else {
      grammars.forEach(g => {
        const card = document.createElement('div');
        card.className = 'p-3.5 rounded-xl border border-[#E2E8F0] bg-white hover:border-[#6366F1] transition space-y-2 shadow-2xs cursor-pointer';

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
          ${g.matchedText ? `<div class="text-[11px] text-slate-600 font-japanese bg-[#F8FAFC] p-2 rounded-lg border border-[#E2E8F0]">Đoạn khớp: <strong class="text-indigo-800 font-semibold">「${escapeHtml(g.matchedText)}」</strong></div>` : ''}
          <p class="text-xs text-slate-600 leading-relaxed">${escapeHtml(g.explanation || '')}</p>
          <div class="text-[11px] text-indigo-600 font-semibold pt-1 flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">arrow_circle_right</span>
            <span>Click để xem trong bài đọc</span>
          </div>
        `;

        // Gắn sự kiện nghe phát âm
        const btnSpeak = card.querySelector('.btn-speak-grammar');
        if (btnSpeak) {
          btnSpeak.addEventListener('click', async (e) => {
            e.stopPropagation();
            await voicevoxClient.speakWithButton(g.matchedText || g.pattern, btnSpeak);
          });
        }

        // Tương tác 2 chiều: click card danh sách sẽ chuyển về tab Bài đọc và scroll highlight token
        card.addEventListener('click', (e) => {
          if (e.target.closest('button') || e.target.closest('a')) return;
          switchTab('reader');
          const token = interactiveReadingBodyEl.querySelector(`.reading-token[data-token-id="${g.id}"]`);
          if (token) {
            token.scrollIntoView({ behavior: 'smooth', block: 'center' });
            showTooltipForToken(token, g.id);
          }
        });

        grammarListContainerEl.appendChild(card);
      });
    }
  }

  // 3. Render Từ vựng trọng tâm
  const vocabs = data.vocabularies || [];
  if (countVocabsEl) countVocabsEl.textContent = vocabs.length;
  if (vocabularyGridContainerEl) {
    vocabularyGridContainerEl.innerHTML = '';

    if (vocabs.length === 0) {
      vocabularyGridContainerEl.innerHTML = '<div class="col-span-3 text-xs text-slate-400 italic p-6 text-center bg-slate-50 rounded-xl">Không có từ vựng tiêu biểu trong bài đọc này.</div>';
    } else {
      vocabs.forEach(v => {
        const card = document.createElement('div');
        card.className = 'p-3.5 rounded-xl border border-[#E2E8F0] bg-white hover:border-[#9F1239] transition flex flex-col justify-between space-y-2.5 shadow-2xs cursor-pointer';

        card.innerHTML = `
          <div>
            <div class="flex items-center justify-between gap-1">
              <span class="text-base font-bold font-japanese text-[#0F172A]">${escapeHtml(v.kanji || v.surfaceText || '')}</span>
              <span class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-slate-100 text-slate-600">${escapeHtml(v.jlpt || 'N5')}</span>
            </div>
            <div class="text-xs text-slate-500 font-japanese mt-0.5">
              ${escapeHtml(v.reading || '')}
              ${v.hanViet ? `<span class="text-[11px] font-semibold text-[#831843] ml-1">(${escapeHtml(v.hanViet)})</span>` : ''}
            </div>
            <div class="text-xs text-slate-800 font-medium mt-1.5 leading-snug">${escapeHtml(v.meaning || '')}</div>
          </div>

          <div class="pt-2 border-t border-[#F1F5F9] flex items-center justify-between">
            <button type="button" class="btn-speak-vocab text-xs text-slate-500 hover:text-[#9F1239] font-medium flex items-center gap-1 cursor-pointer transition">
              <span class="material-symbols-outlined text-[15px]">volume_up</span>
              <span>Nghe</span>
            </button>
            <button type="button" class="btn-save-flashcard px-2.5 py-1 rounded-lg bg-[#FFE5EC] hover:bg-rose-100 text-[#831843] text-[11px] font-bold border border-[#FBCFE8] flex items-center gap-1 cursor-pointer transition">
              <span class="material-symbols-outlined text-[14px]">style</span>
              <span>+ Flashcard</span>
            </button>
          </div>
        `;

        // Gắn sự kiện nghe phát âm
        const btnSpeak = card.querySelector('.btn-speak-vocab');
        if (btnSpeak) {
          btnSpeak.addEventListener('click', async (e) => {
            e.stopPropagation();
            await voicevoxClient.speakWithButton(v.surfaceText || v.kanji || v.reading, btnSpeak);
          });
        }

        // Gắn sự kiện lưu flashcard
        const btnSave = card.querySelector('.btn-save-flashcard');
        if (btnSave) {
          btnSave.addEventListener('click', (e) => {
            e.stopPropagation();
            openSaveModal(v);
          });
        }

        // Tương tác 2 chiều: click card chuyển về tab Bài đọc và scroll highlight token
        card.addEventListener('click', (e) => {
          if (e.target.closest('button')) return;
          switchTab('reader');
          const token = interactiveReadingBodyEl.querySelector(`.reading-token[data-token-id="${v.id}"]`);
          if (token) {
            token.scrollIntoView({ behavior: 'smooth', block: 'center' });
            showTooltipForToken(token, v.id);
          }
        });

        vocabularyGridContainerEl.appendChild(card);
      });
    }
  }

  // 4. Render Dịch từng câu (Bản dịch song ngữ)
  const translations = data.sentenceTranslations || [];
  if (sentenceTranslationsContainerEl) {
    sentenceTranslationsContainerEl.innerHTML = '';
    if (translations.length === 0) {
      sentenceTranslationsContainerEl.innerHTML = '<div class="text-xs text-slate-400 italic p-6 text-center bg-slate-50 rounded-xl">Chưa có bản dịch từng câu cho đoạn văn này.</div>';
    } else {
      translations.forEach((sent, idx) => {
        const row = document.createElement('div');
        row.className = 'p-3 rounded-xl bg-white border border-[#E2E8F0] shadow-2xs flex items-start gap-3';
        row.innerHTML = `
          <span class="inline-flex items-center justify-center w-6 h-6 rounded-lg bg-rose-100 text-[#831843] text-xs font-bold shrink-0 mt-0.5">${idx + 1}</span>
          <div class="text-xs sm:text-sm text-slate-700 leading-relaxed">${escapeHtml(sent)}</div>
        `;
        sentenceTranslationsContainerEl.appendChild(row);
      });
    }
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
// 5.1. BỘ PHÂN TÍCH TƯƠNG TÁC (WATANOC INTERACTIVE TOKENS & FLOATING TOOLTIP)
// =========================================================================

/**
 * Xây dựng đoạn mã HTML tương tác (Watanoc Interactive Tokens)
 * Tự động bọc các từ vựng và cấu trúc ngữ pháp thành các token có gạch chân màu sắc
 */
function buildAnnotatedReadingHtml(rubyHtml, originalText, vocabularies, grammars) {
  let workingHtml = rubyHtml || escapeHtml(originalText || '');
  if (!workingHtml.trim()) return '';

  activeTokensMap.clear();

  // 1. Phân loại ứng viên ngữ pháp và ứng viên từ vựng / Hán tự
  const grammarCandidates = [];
  const wordCandidates = [];

  // Thêm ngữ pháp
  grammars.forEach((g, idx) => {
    const id = g.id || `g_${idx}`;
    g.id = id;
    let matchTarget = (g.matchedText || '').trim();
    if (!matchTarget && g.pattern) {
      const cleanPattern = g.pattern.replace(/^[〜~～\s]+/, '').replace(/[〜~～\s]+$/, '').trim();
      if (!cleanPattern.includes('〜') && !cleanPattern.includes('~') && !cleanPattern.includes('～')) {
        matchTarget = cleanPattern;
      }
    }
    if (matchTarget) {
      grammarCandidates.push({
        id: id,
        type: 'grammar',
        text: matchTarget,
        data: g
      });
    }
  });

  // Thêm từ vựng & chữ Hán
  vocabularies.forEach((v, idx) => {
    const id = v.id || `v_${idx}`;
    v.id = id;
    const matchTarget = (v.surfaceText || v.kanji || v.reading || '').trim();
    if (matchTarget) {
      const isKanjiOnly = v.wordType === 'KANJI' || (v.kanji && v.kanji.length === 1 && /[\u4E00-\u9FAF]/.test(v.kanji));
      wordCandidates.push({
        id: id,
        type: isKanjiOnly ? 'kanji' : 'vocab',
        text: matchTarget,
        data: v
      });
    }
  });

  // Đăng ký toàn bộ vào activeTokensMap
  grammarCandidates.forEach(cand => activeTokensMap.set(cand.id, cand));
  wordCandidates.forEach(cand => activeTokensMap.set(cand.id, cand));

  // Sắp xếp ưu tiên độ dài giảm dần
  grammarCandidates.sort((a, b) => b.text.length - a.text.length);
  wordCandidates.sort((a, b) => b.text.length - a.text.length);

  // Bước 1: Trích xuất các khối ngữ pháp thành placeholder ___GRAMMAR_i___
  const grammarPlaceholders = [];
  grammarCandidates.forEach(cand => {
    const regex = createRubyAwareRegex(cand.text);
    if (!regex) return;

    workingHtml = workingHtml.replace(regex, (matchedStr) => {
      if (matchedStr.includes('___GRAMMAR_') || matchedStr.includes('___WORD_')) return matchedStr;
      const ph = `___GRAMMAR_${grammarPlaceholders.length}___`;
      grammarPlaceholders.push({
        placeholder: ph,
        id: cand.id,
        type: cand.type,
        html: matchedStr,
        label: cand.text
      });
      return ph;
    });
  });

  // Bước 2: Bọc từ vựng & chữ Hán cả trong workingHtml lẫn bên trong nội dung của các khối ngữ pháp
  const wordPlaceholders = [];

  function replaceWordsInString(str) {
    let result = str;
    wordCandidates.forEach(cand => {
      const regex = createRubyAwareRegex(cand.text);
      if (!regex) return;

      result = result.replace(regex, (matchedStr) => {
        if (matchedStr.includes('___GRAMMAR_') || matchedStr.includes('___WORD_')) return matchedStr;
        const ph = `___WORD_${wordPlaceholders.length}___`;
        wordPlaceholders.push({
          placeholder: ph,
          id: cand.id,
          type: cand.type,
          html: matchedStr,
          label: cand.text
        });
        return ph;
      });
    });
    return result;
  }

  // Quét từ vựng trong văn bản chính ngoài ngữ pháp
  workingHtml = replaceWordsInString(workingHtml);

  // Quét từ vựng lồng bên trong các đoạn ngữ pháp
  grammarPlaceholders.forEach(item => {
    item.html = replaceWordsInString(item.html);
  });

  // Bước 3: Phục hồi các placeholder từ vựng thành thẻ span
  wordPlaceholders.forEach(item => {
    const spanTag = `<span class="reading-token token-${item.type}" data-token-id="${escapeHtml(item.id)}" tabindex="0" role="button" aria-label="${escapeHtml(item.label)}">${item.html}</span>`;
    workingHtml = workingHtml.replaceAll(item.placeholder, spanTag);
    // Đồng thời phục hồi trong các đoạn ngữ pháp
    grammarPlaceholders.forEach(gItem => {
      gItem.html = gItem.html.replaceAll(item.placeholder, spanTag);
    });
  });

  // Bước 4: Phục hồi các placeholder ngữ pháp thành thẻ span
  grammarPlaceholders.forEach(item => {
    const spanTag = `<span class="reading-token token-grammar" data-token-id="${escapeHtml(item.id)}" tabindex="0" role="button" aria-label="${escapeHtml(item.label)}">${item.html}</span>`;
    workingHtml = workingHtml.replaceAll(item.placeholder, spanTag);
  });

  return workingHtml;
}

/**
 * Sinh biểu thức Regex thông minh nhận diện từ vựng kể cả khi bị bọc thẻ <ruby>
 */
function createRubyAwareRegex(phrase) {
  if (!phrase) return null;
  const clean = phrase.replace(/^[〜~～\s]+/, '').replace(/[〜~～\s]+$/, '');
  if (!clean) return null;

  const escClean = clean.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const wholeRuby = `<ruby>\\s*${escClean}\\s*<rt>[^<]*<\\/rt>\\s*<\\/ruby>`;

  // Từng ký tự kèm thẻ ruby tùy chọn cho các từ ghép kanji + okurigana
  const chars = Array.from(clean);
  const flexibleParts = chars.map(ch => {
    const escCh = ch.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return `(?:<ruby>\\s*)?${escCh}(?:\\s*<rt>[^<]*<\\/rt>\\s*<\\/ruby>)?`;
  });
  const flexible = flexibleParts.join('(?:<rt>[^<]*<\\/rt>\\s*<\\/ruby>\\s*<ruby>)?');

  const combined = `(?:${wholeRuby}|${flexible}|${escClean})`;
  return new RegExp(combined, 'g');
}

/**
 * Hiển thị Tooltip tương tác cho một Token
 */
function showTooltipForToken(tokenEl, tokenId) {
  const token = activeTokensMap.get(tokenId);
  if (!token || !tooltipEl) return;

  currentActiveTokenEl = tokenEl;
  currentTooltipToken = token;

  // Xóa active-token cũ và kích hoạt cho token này
  document.querySelectorAll('.reading-token').forEach(el => el.classList.remove('active-token'));
  tokenEl.classList.add('active-token');

  // Đổ dữ liệu vào Tooltip
  if (token.type === 'vocab' || token.type === 'kanji') {
    const v = token.data;
    if (token.type === 'kanji') {
      tooltipBadgeTypeEl.textContent = 'Hán tự';
      tooltipBadgeTypeEl.className = 'px-1.5 py-0.5 rounded text-[10px] font-extrabold uppercase tracking-wide bg-emerald-100 text-emerald-800 border border-emerald-200';
    } else {
      tooltipBadgeTypeEl.textContent = 'Từ vựng';
      tooltipBadgeTypeEl.className = 'px-1.5 py-0.5 rounded text-[10px] font-extrabold uppercase tracking-wide bg-rose-100 text-rose-800 border border-rose-200';
    }
    tooltipBadgeJlptEl.textContent = `JLPT ${v.jlpt || 'N5'}`;
    tooltipHeadwordEl.textContent = v.kanji || v.surfaceText || v.reading;

    let sub = escapeHtml(v.reading || '');
    if (v.hanViet) {
      sub += ` <span class="font-sans font-bold text-[#831843] ml-1">(${escapeHtml(v.hanViet)})</span>`;
    }
    if (v.wordType) {
      sub += ` <span class="text-slate-400 font-sans text-[11px] ml-1">• ${escapeHtml(v.wordType)}</span>`;
    }
    tooltipReadingHanVietEl.innerHTML = sub;
    tooltipMeaningEl.textContent = v.meaning || 'Chưa có ý nghĩa.';

    tooltipExtraEl.classList.add('hidden');

    btnTooltipSaveFlashcardEl.classList.remove('hidden');
    btnTooltipDetailLinkEl.href = `/kanji-dictionary?keyword=${encodeURIComponent(v.kanji || v.surfaceText || v.reading)}`;
    tooltipDetailLinkTextEl.textContent = token.type === 'kanji' ? 'Chi tiết Hán tự' : 'Từ điển Hán tự';
  } else if (token.type === 'grammar') {
    const g = token.data;
    tooltipBadgeTypeEl.textContent = 'Ngữ pháp';
    tooltipBadgeTypeEl.className = 'px-1.5 py-0.5 rounded text-[10px] font-extrabold uppercase tracking-wide bg-indigo-100 text-indigo-800 border border-indigo-200';
    tooltipBadgeJlptEl.textContent = `JLPT ${g.jlptLevel || 'N5'}`;
    tooltipHeadwordEl.textContent = g.pattern || '';

    tooltipReadingHanVietEl.innerHTML = g.matchedText
      ? `<span class="text-slate-600 font-japanese font-medium">Khớp: 「${escapeHtml(g.matchedText)}」</span>`
      : '';
    tooltipMeaningEl.textContent = g.meaning || 'Chưa có ý nghĩa.';

    if (g.explanation) {
      tooltipExtraEl.textContent = g.explanation;
      tooltipExtraEl.classList.remove('hidden');
    } else {
      tooltipExtraEl.classList.add('hidden');
    }

    btnTooltipSaveFlashcardEl.classList.add('hidden');
    btnTooltipDetailLinkEl.href = `/grammar?keyword=${encodeURIComponent(g.pattern || '')}`;
    tooltipDetailLinkTextEl.textContent = 'Kho ngữ pháp';
  }

  // Định vị trí và hiển thị
  positionTooltip(tokenEl);
  tooltipEl.classList.remove('hidden', 'opacity-0', 'pointer-events-none');
}

/**
 * Định vị trí của Tooltip nổi trên hoặc dưới Token một cách thông minh
 */
function positionTooltip(tokenEl) {
  if (!tooltipEl || !tokenEl) return;

  const rect = tokenEl.getBoundingClientRect();
  const tooltipRect = tooltipEl.getBoundingClientRect();

  const tooltipWidth = tooltipRect.width || 320;
  const tooltipHeight = tooltipRect.height || 180;

  // Tính vị trí X căn giữa token
  let left = rect.left + (rect.width / 2) - (tooltipWidth / 2);
  // Đảm bảo không tràn viền trái/phải màn hình
  const margin = 12;
  if (left < margin) left = margin;
  if (left + tooltipWidth > window.innerWidth - margin) {
    left = window.innerWidth - tooltipWidth - margin;
  }

  // Tính vị trí Y: ưu tiên nổi lên TRÊN token
  let top = rect.top - tooltipHeight - 12;
  let placeBelow = false;

  // Nếu không đủ chỗ phía trên, lật xuống PHÍA DƯỚI token
  if (top < margin) {
    top = rect.bottom + 12;
    placeBelow = true;
  }

  tooltipEl.style.left = `${Math.round(left)}px`;
  tooltipEl.style.top = `${Math.round(top)}px`;

  // Căn vị trí mũi tên (arrow notch)
  if (tooltipArrowEl) {
    const arrowX = rect.left + (rect.width / 2) - left - 6;
    tooltipArrowEl.style.left = `${Math.max(16, Math.min(tooltipWidth - 28, arrowX))}px`;
    if (placeBelow) {
      tooltipArrowEl.style.top = '-6px';
      tooltipArrowEl.style.bottom = 'auto';
      tooltipArrowEl.style.borderTop = '2px solid #1E293B';
      tooltipArrowEl.style.borderLeft = '2px solid #1E293B';
      tooltipArrowEl.style.borderBottom = 'none';
      tooltipArrowEl.style.borderRight = 'none';
    } else {
      tooltipArrowEl.style.top = 'auto';
      tooltipArrowEl.style.bottom = '-6px';
      tooltipArrowEl.style.borderBottom = '2px solid #1E293B';
      tooltipArrowEl.style.borderRight = '2px solid #1E293B';
      tooltipArrowEl.style.borderTop = 'none';
      tooltipArrowEl.style.borderLeft = 'none';
    }
  }
}

/**
 * Ẩn Tooltip tương tác
 */
function hideTooltip() {
  if (!tooltipEl) return;
  tooltipEl.classList.add('opacity-0', 'pointer-events-none');
  setTimeout(() => {
    if (tooltipEl.classList.contains('opacity-0')) {
      tooltipEl.classList.add('hidden');
    }
  }, 160);

  if (currentActiveTokenEl) {
    currentActiveTokenEl.classList.remove('active-token');
    currentActiveTokenEl = null;
  }
  currentTooltipToken = null;
}

/**
 * Gắn sự kiện Hover & Click cho các Tokens trong bài đọc
 */
function attachTokenEventListeners() {
  const tokenEls = interactiveReadingBodyEl.querySelectorAll('.reading-token');
  tokenEls.forEach(tokenEl => {
    const tokenId = tokenEl.getAttribute('data-token-id');

    // 1. Hover vào Token (Desktop)
    tokenEl.addEventListener('mouseenter', (e) => {
      e.stopPropagation();
      clearTimeout(tooltipHideTimer);
      tooltipShowTimer = setTimeout(() => {
        showTooltipForToken(tokenEl, tokenId);
      }, 100);
    });

    // 2. Rời chuột khỏi Token
    tokenEl.addEventListener('mouseleave', (e) => {
      e.stopPropagation();
      clearTimeout(tooltipShowTimer);
      tooltipHideTimer = setTimeout(() => {
        hideTooltip();
      }, 250);
    });

    // 3. Click vào Token (Mobile & Touch Devices)
    tokenEl.addEventListener('click', (e) => {
      e.stopPropagation();
      clearTimeout(tooltipHideTimer);
      showTooltipForToken(tokenEl, tokenId);
    });
  });
}

// =========================================================================
// 6. XỬ LÝ LƯU FLASHCARD
// =========================================================================
async function fetchUserDecks() {
  try {
    const token = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
    const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

    const res = await fetch('/api/learner/flashcards/decks', { headers });
    if (res.ok) {
      userDecksList = await res.json();
      populateDeckSelect();
    }
  } catch (e) {
    console.warn('[AI Reading] Không tải được danh sách Decks:', e);
  }
}

function populateDeckSelect() {
  if (!selectTargetDeckEl) return;
  selectTargetDeckEl.innerHTML = '';

  if (!userDecksList || userDecksList.length === 0) {
    selectTargetDeckEl.innerHTML = '<option value="default">Bộ thẻ mặc định (Tự động tạo)</option>';
    return;
  }

  const lastDeckId = localStorage.getItem('kizuna_last_selected_deck_id');

  userDecksList.forEach(deck => {
    const opt = document.createElement('option');
    const val = String(deck.id || deck.deckId);
    opt.value = val;
    opt.textContent = `${deck.name || 'Bộ thẻ'} (${deck.cardCount || 0} thẻ)`;
    if (lastDeckId && (val === String(lastDeckId) || deck.name === lastDeckId)) {
      opt.selected = true;
    }
    selectTargetDeckEl.appendChild(opt);
  });

  if (lastDeckId && userDecksList.some(d => String(d.id || d.deckId) === String(lastDeckId))) {
    selectTargetDeckEl.value = lastDeckId;
  }
}

function openSaveModal(vocab) {
  pendingSaveWord = vocab;
  if (modalTargetWordEl) modalTargetWordEl.textContent = vocab.kanji || vocab.surfaceText || vocab.reading;
  if (modalTargetReadingEl) modalTargetReadingEl.textContent = `${vocab.reading || ''} ${vocab.hanViet ? `(${vocab.hanViet})` : ''}`;
  if (modalTargetMeaningEl) modalTargetMeaningEl.textContent = vocab.meaning || '';

  if (modalSaveEl) {
    modalSaveEl.classList.remove('hidden');
    modalSaveEl.classList.add('flex');
  }
}

function closeModal() {
  pendingSaveWord = null;
  if (modalSaveEl) {
    modalSaveEl.classList.add('hidden');
    modalSaveEl.classList.remove('flex');
  }
}

async function saveWordToDeck() {
  if (!pendingSaveWord) return;

  const deckId = selectTargetDeckEl ? selectTargetDeckEl.value : 'default';
  btnConfirmSaveCardEl.disabled = true;
  btnConfirmSaveCardEl.textContent = 'Đang lưu...';

  const cardPayload = {
    frontText: pendingSaveWord.kanji || pendingSaveWord.surfaceText || pendingSaveWord.reading,
    frontSubText: pendingSaveWord.reading,
    backMeaning: pendingSaveWord.meaning,
    hanViet: pendingSaveWord.hanViet,
    jlptLevel: pendingSaveWord.jlpt || 'N5',
    deckId: deckId !== 'default' ? deckId : null
  };

  try {
    const token = localStorage.getItem('kizuna_token') || localStorage.getItem('access_token');
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/learner/flashcards/cards', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(cardPayload)
    });

    if (!res.ok) {
      throw new Error(`Mã lỗi: ${res.status}`);
    }

    showToast(`Đã thêm từ「${cardPayload.frontText}」vào bộ thẻ thành công!`, 'success');
    if (cardPayload.deckId) {
      try { localStorage.setItem('kizuna_last_selected_deck_id', String(cardPayload.deckId)); } catch (e) {}
    }
    closeModal();
  } catch (err) {
    console.error('[AI Reading] Lỗi lưu Flashcard:', err);
    showToast('Lưu Flashcard thất bại. Vui lòng thử lại.', 'error');
  } finally {
    btnConfirmSaveCardEl.disabled = false;
    btnConfirmSaveCardEl.textContent = 'Lưu vào bộ thẻ';
  }
}

// =========================================================================
// 7. HÀM TIỆN ÍCH ESCAPE HTML
// =========================================================================
function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// =========================================================================
// 8. PHÂN HỆ THỬ THÁCH ĐỌC HIỂU DOKKAI AI (TAB 1)
// =========================================================================

function syncFuriganaUI() {
  if (interactiveReadingBodyEl) interactiveReadingBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
  if (cleanReadingPassageBodyEl) cleanReadingPassageBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
  if (furiganaToggleTextEl) furiganaToggleTextEl.textContent = isFuriganaHidden ? 'Hiện Furigana' : 'Ẩn Furigana';
  if (cleanFuriganaToggleTextEl) cleanFuriganaToggleTextEl.textContent = isFuriganaHidden ? 'Hiện Furigana' : 'Ẩn Furigana';
}

function getQuestionTypeBadge(type) {
  switch (type) {
    case 'MAIN_IDEA':
      return '<span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-rose-100 text-[#831843] border border-rose-200">Ý CHÍNH (要旨)</span>';
    case 'REASON':
      return '<span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-amber-100 text-amber-800 border border-amber-200">LÝ DO (理由)</span>';
    case 'DETAIL':
      return '<span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-sky-100 text-sky-800 border border-sky-200">CHI TIẾT (詳細)</span>';
    case 'REFERENCE':
      return '<span class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-purple-100 text-purple-800 border border-purple-200">CHỈ ĐỊNH (指示語)</span>';
    default:
      return '<span class="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-700">ĐỌC HIỂU (読解)</span>';
  }
}

function renderDokkaiChallenge(data) {
  const questions = data.comprehensionQuestions || [];
  dokkaiAnswers = {};
  dokkaiSubmitted = false;

  if (countDokkaiQuestionsEl) {
    countDokkaiQuestionsEl.textContent = questions.length;
  }
  const countDokkaiQuestionsHeaderEl = document.getElementById('countDokkaiQuestionsHeader');
  if (countDokkaiQuestionsHeaderEl) {
    countDokkaiQuestionsHeaderEl.textContent = questions.length;
  }

  // 1. Render bài đọc sạch ở cột trái (chỉ giữ Ruby furigana chuẩn)
  if (cleanReadingPassageBodyEl) {
    cleanReadingPassageBodyEl.innerHTML = data.rubyHtml || escapeHtml(data.originalText || '');
    cleanReadingPassageBodyEl.classList.toggle('hide-furigana', isFuriganaHidden);
  }

  // Ẩn bảng điểm cũ
  if (dokkaiScoreSummaryEl) {
    dokkaiScoreSummaryEl.classList.add('hidden');
    dokkaiScoreSummaryEl.innerHTML = '';
  }

  if (btnSubmitDokkaiEl) {
    btnSubmitDokkaiEl.disabled = false;
    btnSubmitDokkaiEl.classList.remove('opacity-60', 'cursor-not-allowed');
  }
  if (btnSubmitDokkaiTextEl) {
    btnSubmitDokkaiTextEl.textContent = 'Nộp bài & Chấm điểm AI';
  }

  // 2. Render danh sách câu hỏi ở cột phải
  if (!dokkaiQuestionsContainerEl) return;
  dokkaiQuestionsContainerEl.innerHTML = '';

  if (questions.length === 0) {
    dokkaiQuestionsContainerEl.innerHTML = `
      <div class="ls-card p-6 text-center space-y-3 bg-amber-50/60 border border-amber-200">
        <span class="material-symbols-outlined text-amber-600 text-[36px]">psychology_alt</span>
        <div class="space-y-1">
          <h4 class="text-sm font-bold text-slate-800">Chưa có câu hỏi đọc hiểu cho bài này</h4>
          <p class="text-xs text-slate-500">Đoạn văn quá ngắn hoặc chưa kết nối AI để tự động tạo câu hỏi trắc nghiệm.</p>
        </div>
        <button type="button" class="btn-goto-reader px-4 py-2 rounded-lg bg-[#9F1239] text-white text-xs font-bold shadow-xs hover:bg-[#BE185D] transition cursor-pointer">
          Chuyển sang xem bóc tách từ vựng & ngữ pháp
        </button>
      </div>
    `;
    const btnGoto = dokkaiQuestionsContainerEl.querySelector('.btn-goto-reader');
    if (btnGoto) {
      btnGoto.addEventListener('click', () => switchTab('reader'));
    }
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

    const typeBadge = getQuestionTypeBadge(q.questionType);

    let optionsHtml = '';
    const letters = ['A', 'B', 'C', 'D'];
    (q.options || []).forEach((opt, optIdx) => {
      const letter = letters[optIdx] || String(optIdx + 1);
      optionsHtml += `
        <label class="dokkai-option-item flex items-start gap-3 p-3 rounded-xl border border-slate-200 bg-white hover:border-[#9F1239] hover:bg-rose-50/30 cursor-pointer transition select-none group" data-opt-index="${optIdx}">
          <input type="radio" name="dokkai_q_${q.id}" value="${optIdx}" class="mt-0.5 text-[#9F1239] focus:ring-[#9F1239] cursor-pointer" />
          <div class="flex-1 text-xs sm:text-sm font-japanese leading-relaxed text-slate-800 group-hover:text-slate-900">
            <span class="font-bold text-slate-500 font-sans mr-1">${letter}.</span> ${escapeHtml(opt)}
          </div>
        </label>
      `;
    });

    card.innerHTML = `
      <div class="flex items-center justify-between gap-2 border-b border-slate-100 pb-2">
        <div class="flex items-center gap-2">
          <span class="w-6 h-6 rounded-full bg-[#9F1239] text-white text-xs font-bold flex items-center justify-center shrink-0">
            ${qIndex + 1}
          </span>
          <span class="text-xs font-bold text-slate-800">Câu hỏi ${qIndex + 1}</span>
        </div>
        ${typeBadge}
      </div>

      <div class="text-xs sm:text-sm font-bold text-slate-900 font-japanese leading-relaxed">
        ${escapeHtml(q.question || '')}
      </div>

      <div class="space-y-2 pt-1" id="optionsGroup_${q.id}">
        ${optionsHtml}
      </div>

      <!-- Khung lời giải phân tích bẫy (ẩn trước khi nộp bài) -->
      <div class="dokkai-explanation-box hidden p-3.5 rounded-xl text-xs space-y-2.5">
        <!-- Rendered after submit -->
      </div>
    `;

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
              ${isPassed ? 'Rất tốt! Bạn đã nắm bắt chính xác ý tứ và quan điểm của bài đọc.' : 'Cố gắng lên! Hãy xem giải thích và đối chiếu dẫn chứng trong bài để rút kinh nghiệm.'}
            </p>
          </div>
        </div>
        <button type="button" id="btnNextToDeepAnalysis" class="px-3.5 py-1.5 rounded-lg bg-white border border-slate-300 hover:border-[#9F1239] text-slate-700 hover:text-[#9F1239] text-xs font-bold shadow-2xs cursor-pointer flex items-center justify-center gap-1 shrink-0 transition">
          <span>Xem bóc tách sâu</span>
          <span class="material-symbols-outlined text-[15px]">arrow_forward</span>
        </button>
      </div>
    `;

    const btnNext = dokkaiScoreSummaryEl.querySelector('#btnNextToDeepAnalysis');
    if (btnNext) {
      btnNext.addEventListener('click', () => switchTab('reader'));
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

function highlightEvidenceInCleanPassage(quote) {
  if (!cleanReadingPassageBodyEl || !quote) return;
  const cleanQuote = quote.trim().replace(/[。！？\s]+$/, '');
  if (!cleanQuote) return;

  // Khôi phục bài đọc sạch ban đầu
  cleanReadingPassageBodyEl.innerHTML = (currentAnalysisResult && currentAnalysisResult.rubyHtml)
    ? currentAnalysisResult.rubyHtml
    : escapeHtml((currentAnalysisResult && currentAnalysisResult.originalText) || '');

  const passageHtml = cleanReadingPassageBodyEl.innerHTML;
  let highlighted = false;

  // 1. Sử dụng Regex thấu hiểu Ruby để tìm và bọc <mark> mà không làm mất furigana
  const rubyRegex = createRubyAwareRegex(cleanQuote);
  if (rubyRegex && rubyRegex.test(passageHtml)) {
    cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(rubyRegex, (matched) => `<mark class="evidence-highlight">${matched}</mark>`);
    highlighted = true;
  } else if (passageHtml.includes(cleanQuote)) {
    // 2. Dự phòng khi câu không có thẻ ruby
    cleanReadingPassageBodyEl.innerHTML = passageHtml.replace(cleanQuote, `<mark class="evidence-highlight">${cleanQuote}</mark>`);
    highlighted = true;
  } else {
    // 3. Dự phòng cắt cụm từ con ngắn hơn nếu câu quá dài hoặc lệch dấu
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

  const highlightEl = cleanReadingPassageBodyEl.querySelector('.evidence-highlight');
  if (highlightEl) {
    highlightEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
    showToast('Đã đánh dấu câu văn làm dẫn chứng trong bài đọc', 'info');
  } else {
    showToast('Dẫn chứng: 「' + cleanQuote + '」', 'info');
  }
}
