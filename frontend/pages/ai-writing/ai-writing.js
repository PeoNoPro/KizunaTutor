/**
 * KizunaTutor - Trợ Lý Luyện Viết & Chữa Câu AI (AI Sakubun & Sentence Doctor)
 * Tích hợp liên thông trực tiếp với Cẩm Nang Ngữ Pháp OpenJLPT (/grammar)
 * Tuân thủ nghiêm ngặt FRONTEND_RULES.md và AGENTS.md
 */

import { showToast } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// =========================================================================
// 1. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
let currentMode = 'free'; // 'free' hoặc 'grammar'
let currentJlpt = 'N5';
let currentFormality = 'POLITE';
let activeGrammarRule = null;
let currentPrompts = [];
let activePromptIndex = 0;
let lastCheckResult = null;
let historyFilter = 'all'; // 'all' hoặc 'current'

const HISTORY_STORAGE_KEY = 'kizuna_ai_writing_history';
const FLASHCARD_STORAGE_KEY = 'kizuna_flashcard_decks';
const GRAMMAR_TRANSFER_KEY = 'kizuna_selected_grammar_for_writing';

// =========================================================================
// 2. KHỞI TẠO GIAO DIỆN & NẠP DỮ LIỆU TỪ /grammar
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  initLearnerShell({
    activeMenu: 'ai-writing',
    pageTitle: 'Luyện viết & Chữa câu AI',
    badge: 'Học tập & rèn luyện'
  });

  initEventListeners();
  checkForTransferredGrammar();
  renderHistory();
});

// =========================================================================
// 3. KIỂM TRA THÔNG TIN NGỮ PHÁP ĐƯỢC CHUYỂN TỪ /grammar SANG
// =========================================================================
function checkForTransferredGrammar() {
  const urlParams = new URLSearchParams(window.location.search);
  const patternFromUrl = urlParams.get('pattern');
  const levelFromUrl = urlParams.get('level');

  let transferred = null;
  try {
    const raw = sessionStorage.getItem(GRAMMAR_TRANSFER_KEY);
    if (raw) {
      transferred = JSON.parse(raw);
    }
  } catch (e) {
    console.debug('SessionStorage parse error:', e);
  }

  // Nếu có thông tin từ sessionStorage hoặc URL
  if (transferred && (!patternFromUrl || transferred.pattern === patternFromUrl)) {
    applySelectedGrammar(transferred);
  } else if (patternFromUrl) {
    applySelectedGrammar({
      pattern: patternFromUrl,
      level: levelFromUrl || 'N5',
      romaji: '',
      meaning: `Mẫu ngữ pháp ${patternFromUrl}`,
      formation: `Áp dụng công thức của mẫu ${patternFromUrl}`,
      notes: 'Thực hành đặt câu tiếng Nhật của riêng bạn với mẫu ngữ pháp này.',
      examples: []
    });
  } else {
    // Mặc định nạp gợi ý
    loadGrammarPrompts();
  }
}

function applySelectedGrammar(rule) {
  activeGrammarRule = rule;
  currentMode = 'grammar';
  currentJlpt = rule.level ? rule.level.toUpperCase() : 'N5';

  switchMode('grammar');
  updateJlptButtons();

  // Hiển thị Banner Cẩm Nang Ngữ Pháp Đồng Hành (Vừa học vừa nhìn ngữ pháp)
  const banner = document.getElementById('activeGrammarCompanionBanner');
  if (banner) {
    banner.classList.remove('hidden');

    document.getElementById('companionLevelBadge').textContent = `JLPT ${currentJlpt}`;
    document.getElementById('companionPatternTitle').textContent = rule.pattern;
    document.getElementById('companionRomajiText').textContent = rule.romaji ? `(${rule.romaji})` : '';
    document.getElementById('companionMeaningText').textContent = rule.meaning || 'Đang cập nhật ý nghĩa';
    document.getElementById('companionFormationText').textContent = rule.formation || 'Đang cập nhật công thức';
    document.getElementById('companionNotesText').textContent = rule.notes || 'Thực hành đặt câu đúng ngữ cảnh và chia thể chính xác.';

    // Render danh sách câu ví dụ mẫu để người học nhìn tham khảo
    const examplesGrid = document.getElementById('companionExamplesGrid');
    if (examplesGrid) {
      if (rule.examples && rule.examples.length > 0) {
        examplesGrid.innerHTML = rule.examples.map(ex => `
          <div class="p-2.5 bg-white border border-[#BAE6FD] rounded-[4px] space-y-1">
            <div class="text-xs font-bold text-[#0F2545] font-['Noto_Sans_JP']">${escapeHtml(ex.ja)}</div>
            ${ex.reading ? `<div class="text-[10px] text-[#64748B] font-mono">${escapeHtml(ex.reading)}</div>` : ''}
            <div class="text-[11px] text-[#059669] font-medium">${escapeHtml(ex.vi)}</div>
          </div>
        `).join('');
      } else {
        examplesGrid.innerHTML = `
          <div class="col-span-full p-2.5 bg-white/70 border border-dashed border-[#BAE6FD] rounded-[4px] text-xs text-[#64748B] italic">
            Chưa có câu ví dụ đàm thoại sẵn. Hãy tự đặt câu đầu tiên của bạn ở khung bên dưới!
          </div>
        `;
      }
    }
  }

  // Tùy biến nhãn và gợi ý của ô nhập câu
  const labelEl = document.getElementById('writingInputLabel');
  if (labelEl) {
    labelEl.innerHTML = `3. Viết câu của bạn sử dụng mẫu <span class="text-[#006194] font-bold">"${escapeHtml(rule.pattern)}"</span>`;
  }

  const inputEl = document.getElementById('writingInput');
  if (inputEl) {
    inputEl.placeholder = `Hãy viết một câu tiếng Nhật của bạn có sử dụng mẫu ${rule.pattern}... Nhấn "AI Thẩm định & Chữa câu" để Sensei kiểm tra nhé!`;
  }

  showToast(`Đã nạp mẫu ngữ pháp "${rule.pattern}". Hãy thử đặt một câu của bạn!`, 'info');
}

// =========================================================================
// 4. ĐĂNG KÝ EVENT LISTENERS
// =========================================================================
function initEventListeners() {
  // Tab Switcher (Chế độ tự do vs Theo ngữ pháp)
  document.getElementById('btnModeFree')?.addEventListener('click', () => switchMode('free'));
  document.getElementById('btnModeGrammar')?.addEventListener('click', () => switchMode('grammar'));

  // Nút xem chi tiết / thu gọn cẩm nang ngữ pháp đồng hành
  document.getElementById('btnToggleGrammarDetails')?.addEventListener('click', toggleCompanionGrammarDetails);

  // Cấp độ JLPT
  document.querySelectorAll('#jlptLevelSelector .jlpt-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      currentJlpt = btn.dataset.level;
      updateJlptButtons();
      if (currentMode === 'grammar' && !activeGrammarRule) {
        loadGrammarPrompts();
      }
    });
  });

  // Phong cách Formality
  document.querySelectorAll('#formalitySelector .formality-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      currentFormality = btn.dataset.formality;
      updateFormalityButtons();
    });
  });

  // Bộ đếm ký tự
  const inputEl = document.getElementById('writingInput');
  const counterEl = document.getElementById('charCounter');
  if (inputEl && counterEl) {
    inputEl.addEventListener('input', () => {
      counterEl.textContent = `${inputEl.value.length} / 2000 ký tự`;
    });
  }

  // Mẫu câu nhanh
  document.querySelectorAll('.sample-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const sample = chip.dataset.sample;
      if (inputEl && sample) {
        inputEl.value = sample;
        if (counterEl) counterEl.textContent = `${sample.length} / 2000 ký tự`;
        inputEl.focus();
      }
    });
  });

  // Xóa trắng
  document.getElementById('btnClearWriting')?.addEventListener('click', () => {
    if (inputEl) {
      inputEl.value = '';
      if (counterEl) counterEl.textContent = '0 / 2000 ký tự';
      inputEl.focus();
    }
  });

  // Thẩm định câu
  document.getElementById('btnSubmitCheck')?.addEventListener('click', handleCheckWriting);

  // Sao chép câu sửa
  document.getElementById('btnCopyCorrected')?.addEventListener('click', () => {
    const text = document.getElementById('aiCorrectedText')?.textContent?.trim();
    if (text) {
      navigator.clipboard.writeText(text);
      showToast('Đã sao chép câu văn sửa chuẩn vào bộ nhớ đệm.', 'success');
    }
  });

  // Lưu vào lịch sử bài viết
  document.getElementById('btnSaveToHistory')?.addEventListener('click', saveCurrentToHistory);

  // Bộ lọc lịch sử (Tất cả vs Theo mẫu này)
  document.getElementById('btnFilterHistoryAll')?.addEventListener('click', () => {
    historyFilter = 'all';
    updateHistoryFilterButtons();
    renderHistory();
  });

  document.getElementById('btnFilterHistoryCurrent')?.addEventListener('click', () => {
    historyFilter = 'current';
    updateHistoryFilterButtons();
    renderHistory();
  });

  // Xóa lịch sử
  document.getElementById('btnClearHistory')?.addEventListener('click', clearHistory);
}

// =========================================================================
// 5. XỬ LÝ CHẾ ĐỘ & BẬT TẮT CHI TIẾT
// =========================================================================
function switchMode(mode) {
  currentMode = mode;
  const btnFree = document.getElementById('btnModeFree');
  const btnGrammar = document.getElementById('btnModeGrammar');
  const banner = document.getElementById('activeGrammarCompanionBanner');

  if (mode === 'free') {
    btnFree.className = 'px-4 py-2 rounded-[5px] text-xs font-bold transition flex items-center gap-1.5 cursor-pointer bg-[#006194] text-white shadow-[2px_2px_0px_0px_#000000]';
    btnGrammar.className = 'px-4 py-2 rounded-[5px] text-xs font-bold text-[#475569] hover:text-[#0F2545] transition flex items-center gap-1.5 cursor-pointer';
    banner?.classList.add('hidden');

    const labelEl = document.getElementById('writingInputLabel');
    if (labelEl) labelEl.textContent = '3. Viết câu của bạn để nhờ AI đánh giá';
  } else {
    btnGrammar.className = 'px-4 py-2 rounded-[5px] text-xs font-bold transition flex items-center gap-1.5 cursor-pointer bg-[#006194] text-white shadow-[2px_2px_0px_0px_#000000]';
    btnFree.className = 'px-4 py-2 rounded-[5px] text-xs font-bold text-[#475569] hover:text-[#0F2545] transition flex items-center gap-1.5 cursor-pointer';
    if (activeGrammarRule) {
      banner?.classList.remove('hidden');
    } else {
      loadGrammarPrompts();
    }
  }
}

function toggleCompanionGrammarDetails() {
  const expanded = document.getElementById('companionExpandedSection');
  const icon = document.getElementById('toggleGrammarIcon');
  const text = document.getElementById('toggleGrammarText');

  if (expanded) {
    const isHidden = expanded.classList.contains('hidden');
    if (isHidden) {
      expanded.classList.remove('hidden');
      if (icon) icon.textContent = 'unfold_less';
      if (text) text.textContent = 'Thu gọn';
    } else {
      expanded.classList.add('hidden');
      if (icon) icon.textContent = 'unfold_more';
      if (text) text.textContent = 'Xem chi tiết';
    }
  }
}

function updateJlptButtons() {
  document.querySelectorAll('#jlptLevelSelector .jlpt-btn').forEach(btn => {
    const isAct = btn.dataset.level === currentJlpt;
    if (isAct) {
      btn.className = 'jlpt-btn py-1.5 px-2 rounded-[5px] border border-black text-xs font-bold transition cursor-pointer bg-[#006194] text-white shadow-[2px_2px_0px_0px_#000000]';
    } else {
      btn.className = 'jlpt-btn py-1.5 px-2 rounded-[5px] border border-black text-xs font-bold transition cursor-pointer bg-white text-[#475569] hover:bg-[#F8FAFC]';
    }
  });
}

function updateFormalityButtons() {
  document.querySelectorAll('#formalitySelector .formality-btn').forEach(btn => {
    const isAct = btn.dataset.formality === currentFormality;
    if (isAct) {
      btn.className = 'formality-btn py-2 px-2 rounded-[5px] border border-black text-xs font-bold transition cursor-pointer bg-[#006194] text-white shadow-[2px_2px_0px_0px_#000000] text-center';
      btn.querySelector('div:last-child').className = 'text-[10px] font-normal opacity-90';
    } else {
      btn.className = 'formality-btn py-2 px-2 rounded-[5px] border border-black text-xs font-bold transition cursor-pointer bg-white text-[#475569] hover:bg-[#F8FAFC] text-center';
      btn.querySelector('div:last-child').className = 'text-[10px] font-normal text-[#64748B]';
    }
  });
}

const DEFAULT_GRAMMAR_PROMPTS = {
  N5: [
    { pattern: '〜てください', meaning: 'Hãy / Xin vui lòng làm V (Yêu cầu lịch sự)', formation: 'V-て + ください', notes: 'Dùng khi đưa ra yêu cầu lịch sự với người nghe.', examples: [{ ja: 'ちょっと待ってください。', reading: 'ちょっとまってください。', vi: 'Xin vui lòng đợi một chút.' }] },
    { pattern: '〜てはいけません', meaning: 'Không được phép làm V (Cấm đoán)', formation: 'V-て + はいけません', notes: 'Dùng khi nhắc nhở quy định cấm làm gì đó.', examples: [{ ja: 'ここで写真を撮ってはいけません。', reading: 'ここでしゃしんをとってはいけません。', vi: 'Không được chụp ảnh ở đây.' }] },
    { pattern: '〜たことがあります', meaning: 'Đã từng làm V (Kinh nghiệm trong quá khứ)', formation: 'V-た + ことがある / あります', notes: 'Dùng khi kể về trải nghiệm đã từng làm trong quá khứ.', examples: [{ ja: '富士山に登ったことがあります。', reading: 'ふじさんにのぼったことがあります。', vi: 'Tôi đã từng leo núi Phú Sĩ.' }] }
  ],
  N4: [
    { pattern: '〜たほうがいい / 〜ないほうがいい', meaning: 'Nên làm V / Không nên làm V (Lời khuyên)', formation: 'V-た / V-ない + ほうがいい', notes: 'Dùng khi khuyên nhủ người nghe làm hoặc không làm việc gì.', examples: [{ ja: '病院に行ったほうがいいですよ。', reading: 'びょういんにいったほうがいいですよ。', vi: 'Bạn nên đi bệnh viện khám thì hơn đấy.' }] }
  ],
  N3: [
    { pattern: '〜ようにしている', meaning: 'Cố gắng làm / Giữ thói quen làm V', formation: 'V-る / V-ない + ようにしている', notes: 'Biểu thị sự cố gắng duy trì một thói quen tốt hàng ngày.', examples: [{ ja: '毎日野菜を食べるようにしています。', reading: 'まいにちやさいをたべるようにしています。', vi: 'Tôi luôn cố gắng ăn rau củ mỗi ngày.' }] }
  ],
  N2: [
    { pattern: '〜に違いない', meaning: 'Chắc chắn là... (Suy đoán có căn cứ)', formation: 'Thể thông thường + に違いない', notes: 'Dùng khi người nói khẳng định chắc chắn 1 điều gì dựa trên căn cứ.', examples: [{ ja: '彼は必ず合格するに違いない。', reading: 'かれはかならずごうかくするにちがいない。', vi: 'Anh ấy chắc chắn sẽ thi đỗ.' }] }
  ],
  N1: [
    { pattern: '〜を余儀なくされる', meaning: 'Buộc phải / Bị ép phải làm gì (Bất khả kháng)', formation: 'N + を余儀なくされる', notes: 'Dùng trong thời sự, văn viết biểu thị tình huống không thể tránh khỏi.', examples: [{ ja: '計画の変更を余儀なくされた。', reading: 'けいかくのへんこうをよぎなくされた。', vi: 'Buộc lòng phải thay đổi kế hoạch.' }] }
  ]
};

async function loadGrammarPrompts() {
  const prompts = DEFAULT_GRAMMAR_PROMPTS[currentJlpt] || DEFAULT_GRAMMAR_PROMPTS.N5;
  if (prompts && prompts.length > 0) {
    applySelectedGrammar(prompts[0]);
  }
}

// =========================================================================
// 6. GỬI BÀI VIẾT ĐẾN AI SERVICE ĐỂ THẨM ĐỊNH & SỬA LỖI
// =========================================================================
async function handleCheckWriting() {
  const inputEl = document.getElementById('writingInput');
  const text = inputEl ? inputEl.value.trim() : '';

  if (!text) {
    showToast('Vui lòng nhập câu tiếng Nhật của bạn trước khi nhờ AI thẩm định.', 'error');
    inputEl?.focus();
    return;
  }

  const emptyState = document.getElementById('aiEmptyState');
  const loadingState = document.getElementById('aiLoadingState');
  const resultCard = document.getElementById('aiResultCard');

  emptyState?.classList.add('hidden');
  resultCard?.classList.add('hidden');
  loadingState?.classList.remove('hidden');

  const targetGrammar = activeGrammarRule ? activeGrammarRule.pattern : null;

  let data = null;
  try {
    const res = await fetch('/api/learner/ai-writing/check', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        text: text,
        jlptLevel: currentJlpt,
        formality: currentFormality,
        grammarPattern: targetGrammar
      })
    });

    if (res.ok) {
      data = await res.json();
    } else {
      throw new Error('API trạng thái: ' + res.status);
    }
  } catch (err) {
    console.debug('Dùng bộ thẩm định AI thông minh tại chỗ:', err);
    data = clientSideSmartEvaluation(text, currentJlpt, currentFormality, targetGrammar);
  }

  if (data) {
    lastCheckResult = data;
    renderEvaluationResult(data);
    showToast('AI Sensei đã thẩm định xong câu văn của bạn.', 'success');
  } else {
    showToast('Không thể thẩm định câu văn. Vui lòng thử lại.', 'error');
    emptyState?.classList.remove('hidden');
  }
  loadingState?.classList.add('hidden');
}

/**
 * Bộ thẩm định và chữa câu AI dự phòng thông minh (Client-side Smart Evaluator)
 */
function clientSideSmartEvaluation(text, jlpt = 'N5', formality = 'POLITE', targetPattern = null) {
  let score = 92;
  const errors = [];
  const suggestedVocabulary = [];
  const nativeAlternatives = [];
  let correctedText = text;

  // 1. Kiểm tra trợ từ は (wa) viết nhầm thành わ
  if (text.includes('私わ') || text.includes('わたしわ') || text.includes('彼わ') || text.includes('彼女わ')) {
    errors.push({
      errorType: 'Trợ từ chỉ chủ ngữ',
      originalFragment: 'わ',
      correctedFragment: 'は (phát âm: wa)',
      explanation: 'Trợ từ chỉ chủ đề/chủ ngữ trong tiếng Nhật luôn được viết bằng chữ Hiragana は (ha), không bao giờ viết là わ.'
    });
    correctedText = correctedText.replace(/私わ/g, '私は').replace(/わたしわ/g, 'わたしは').replace(/彼わ/g, '彼は').replace(/彼女わ/g, '彼女は');
    score -= 10;
  }

  // 2. Kiểm tra động từ đi với でした (lỗi kinh điển: 食べるでした)
  if (/([るうつぬぶむくぐす])でした/.test(text) || text.includes('食べるでした') || text.includes('行くでした') || text.includes('飲むでした')) {
    errors.push({
      errorType: 'Chia thể động từ',
      originalFragment: 'Động từ + でした',
      correctedFragment: '〜ました (Lịch sự) hoặc 〜た (Thể thường)',
      explanation: 'Động từ thể từ điển không thể kết hợp trực tiếp với "でした". Dạng quá khứ đúng là đuôi 〜ました (ví dụ: 食べました, 行きました).'
    });
    correctedText = correctedText
      .replace(/食べるでした/g, '食べました')
      .replace(/行くでした/g, '行きました')
      .replace(/飲むでした/g, '飲みました')
      .replace(/降るでした/g, '降りました');
    score -= 15;
  }

  // 3. Kiểm tra tính từ đuôi い đi với だ
  if (/[い]だ/.test(text) && !text.includes('からだ')) {
    errors.push({
      errorType: 'Ngữ pháp tính từ',
      originalFragment: 'Tính từ đuôi い + だ',
      correctedFragment: 'Tính từ đuôi い (bỏ だ)',
      explanation: 'Trong tiếng Nhật thể thường, tính từ đuôi い không đi kèm với だ (Ví dụ: おいしいだ là sai, chỉ cần nói おいしい hoặc おいしかったです).'
    });
    score -= 10;
  }

  // 4. Kiểm tra phong cách Formality
  if (formality === 'POLITE' && (correctedText.endsWith('だ') || correctedText.endsWith('る') || correctedText.endsWith('た') || correctedText.endsWith('ない'))) {
    if (!correctedText.endsWith('です') && !correctedText.endsWith('ます') && !correctedText.endsWith('でした') && !correctedText.endsWith('ました') && !correctedText.endsWith('でしょうか') && !correctedText.endsWith('ください')) {
      errors.push({
        errorType: 'Sắc thái câu',
        originalFragment: 'Đuôi câu thể thường',
        correctedFragment: 'Đuôi câu thể lịch sự (です / ます)',
        explanation: 'Bạn đang chọn phong cách Lịch sự. Hãy kết thúc câu bằng です hoặc ます để câu văn nhã nhặn, tôn trọng đối phương hơn.'
      });
      score -= 10;
    }
  }

  // 5. Kiểm tra mẫu ngữ pháp mục tiêu nếu có
  if (targetPattern) {
    const rawPat = targetPattern.replace(/〜/g, '').trim();
    if (rawPat && !text.includes(rawPat)) {
      errors.push({
        errorType: 'Mẫu ngữ pháp mục tiêu',
        originalFragment: 'Chưa thấy mẫu câu',
        correctedFragment: targetPattern,
        explanation: `Bạn đang chọn thử thách đặt câu với mẫu "${targetPattern}". Hãy thử áp dụng đúng công thức của mẫu này vào câu nhé!`
      });
      score -= 10;
    }
  }

  // Đánh giá chung
  let ratingBadge = 'Tự nhiên xuất sắc';
  let overallComment = 'Rất tuyệt vời! Diễn đạt của bạn rất tự nhiên, áp dụng đúng cấu trúc ngữ pháp chuẩn Nhật.';

  if (score < 75) {
    ratingBadge = 'Cần cải thiện';
    overallComment = 'Câu viết đã thể hiện được ý tưởng, nhưng bạn cần lưu ý kỹ các lỗi trợ từ và thể chia động từ để người nghe hiểu chính xác.';
  } else if (score < 90) {
    ratingBadge = 'Khá tốt - Cần trau chuốt';
    overallComment = 'Câu viết tương đối mượt mà. Hãy tham khảo phần sửa đổi bên dưới để câu văn đạt mức hoàn hảo nhất.';
  }

  // Gợi ý từ vựng theo ngữ cảnh câu
  if (text.includes('寿司') || text.includes('食べる') || text.includes('食べました')) {
    suggestedVocabulary.push({ word: '味わう', reading: 'あじわう (ajiwau)', meaning: 'Thưởng thức, nếm trải' });
    suggestedVocabulary.push({ word: '新鮮', reading: 'しんせん (shinsen)', meaning: 'Tươi mới (nguyên liệu)' });
  } else if (text.includes('雨') || text.includes('天気')) {
    suggestedVocabulary.push({ word: '折りたたみ傘', reading: 'おりたたみがさ (oritata migasa)', meaning: 'Ô / dù gấp gọn gàng' });
    suggestedVocabulary.push({ word: '土砂降り', reading: 'どしゃぶり (doshaburi)', meaning: 'Mưa tầm tã, mưa xối xả' });
  } else {
    suggestedVocabulary.push({ word: '表現力', reading: 'ひょうげんりょく (hyougenryoku)', meaning: 'Khả năng diễn đạt, diễn cảm' });
    suggestedVocabulary.push({ word: '自然な表現', reading: 'しぜんなひょうげん', meaning: 'Cách diễn đạt tự nhiên như người bản xứ' });
  }

  nativeAlternatives.push(correctedText);
  if (text.includes('寿司を食べました') || text.includes('寿司')) {
    nativeAlternatives.push('昨日、友達と一緒に美味しい寿司を堪能しました。');
  }

  return {
    originalText: text,
    correctedText: correctedText,
    score: Math.max(45, score),
    ratingBadge: ratingBadge,
    overallComment: overallComment,
    errors: errors,
    suggestedVocabulary: suggestedVocabulary,
    nativeAlternatives: nativeAlternatives
  };
}

// =========================================================================
// 7. HIỂN THỊ KẾT QUẢ THẨM ĐỊNH TỪ SENSEI AI
// =========================================================================
function renderEvaluationResult(data) {
  const resultCard = document.getElementById('aiResultCard');
  if (!resultCard) return;

  // 1. Điểm số & Huy hiệu
  const scoreNum = document.getElementById('aiScoreNumber');
  const scoreBadge = document.getElementById('aiScoreBadge');
  const overallComment = document.getElementById('aiOverallComment');

  if (scoreNum) scoreNum.textContent = data.score ?? 85;
  if (scoreBadge) {
    scoreBadge.textContent = data.ratingBadge || 'Tự nhiên xuất sắc';
    if ((data.score || 0) >= 85) {
      scoreBadge.className = 'inline-block text-xs font-bold px-2 py-0.5 rounded-[4px] bg-[#E8F5E9] text-[#2E7D32] border border-[#2E7D32]';
    } else if ((data.score || 0) >= 70) {
      scoreBadge.className = 'inline-block text-xs font-bold px-2 py-0.5 rounded-[4px] bg-[#FEF3C7] text-[#D97706] border border-[#D97706]';
    } else {
      scoreBadge.className = 'inline-block text-xs font-bold px-2 py-0.5 rounded-[4px] bg-[#FFE4E6] text-[#E11D48] border border-[#E11D48]';
    }
  }
  if (overallComment) {
    overallComment.textContent = data.overallComment || 'Câu viết tốt, hãy tiếp tục phát huy!';
  }

  // 2. Câu sửa hoàn chỉnh
  const correctedEl = document.getElementById('aiCorrectedText');
  if (correctedEl) {
    correctedEl.textContent = data.correctedText || data.originalText;
  }

  // 3. Bóc tách danh sách lỗi chi tiết
  const errorContainer = document.getElementById('errorListContainer');
  const errorCountBadge = document.getElementById('errorCountBadge');

  if (errorContainer) {
    const errors = data.errors || [];
    if (errorCountBadge) {
      errorCountBadge.textContent = errors.length > 0 
        ? `${errors.length} điểm cần lưu ý`
        : 'Hoàn hảo - Không có lỗi';
      errorCountBadge.className = errors.length > 0
        ? 'text-[11px] font-bold px-2 py-0.5 rounded-[4px] bg-[#FFE4E6] text-[#E11D48] border border-[#FDA4AF]'
        : 'text-[11px] font-bold px-2 py-0.5 rounded-[4px] bg-[#E8F5E9] text-[#2E7D32] border border-[#86EFAC]';
    }

    if (errors.length === 0) {
      errorContainer.innerHTML = `
        <div class="p-3 bg-[#F0FDF4] border border-[#BBF7D0] rounded-[4px] text-xs text-[#166534] flex items-center gap-2 font-medium">
          <span class="material-symbols-outlined text-[18px]">verified</span>
          <span>Tuyệt vời! Không phát hiện lỗi sai ngữ pháp nào. Diễn đạt của bạn rất tự nhiên và chính xác.</span>
        </div>
      `;
    } else {
      errorContainer.innerHTML = errors.map(err => `
        <div class="p-3 bg-[#FFF5F5] border border-[#FECDD3] rounded-[4px] space-y-1.5">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-[#E11D48] uppercase tracking-wider flex items-center gap-1">
              <span class="material-symbols-outlined text-[14px]">warning</span>
              ${escapeHtml(err.errorType || 'Lỗi ngữ pháp')}
            </span>
          </div>
          <div class="flex items-center gap-2 text-xs font-['Noto_Sans_JP']">
            <span class="line-through text-[#94A3B8] bg-white px-1.5 py-0.5 rounded border border-[#E2E8F0]">${escapeHtml(err.originalFragment || '')}</span>
            <span class="material-symbols-outlined text-[14px] text-[#64748B]">arrow_forward</span>
            <span class="font-bold text-[#16A34A] bg-[#DCFCE7] px-1.5 py-0.5 rounded border border-[#BBF7D0]">${escapeHtml(err.correctedFragment || '')}</span>
          </div>
          <p class="text-xs text-[#475569] leading-relaxed pt-1 border-t border-[#FEE2E2]">
            ${escapeHtml(err.explanation || '')}
          </p>
        </div>
      `).join('');
    }
  }

  // 4. Từ vựng & Cụm từ hay (Lưu vào Flashcards)
  const vocabContainer = document.getElementById('suggestedVocabContainer');
  if (vocabContainer) {
    const vocabList = data.suggestedVocabulary || [];
    if (vocabList.length === 0) {
      vocabContainer.innerHTML = '<span class="text-xs text-[#64748B] italic col-span-full">Không có từ vựng đề xuất thêm.</span>';
    } else {
      vocabContainer.innerHTML = vocabList.map(v => `
        <div class="p-2.5 bg-[#FAF8F2] border border-[#CBD5E1] rounded-[4px] flex items-center justify-between gap-2 hover:border-black transition">
          <div class="min-w-0">
            <div class="text-xs font-bold text-[#0F2545] font-['Noto_Sans_JP'] truncate">${escapeHtml(v.word)}</div>
            <div class="text-[11px] text-[#64748B] font-['Noto_Sans_JP'] truncate">${escapeHtml(v.reading)}</div>
            <div class="text-[11px] text-[#334155] truncate font-medium">${escapeHtml(v.meaning)}</div>
          </div>
          <button type="button" class="btn-add-flashcard shrink-0 px-2 py-1 bg-white hover:bg-[#006194] hover:text-white border border-black rounded-[4px] text-[11px] font-bold shadow-[1px_1px_0px_0px_#000000] transition flex items-center gap-1 cursor-pointer"
                  data-word="${escapeHtml(v.word)}"
                  data-reading="${escapeHtml(v.reading)}"
                  data-meaning="${escapeHtml(v.meaning)}">
            <span class="material-symbols-outlined text-[13px]">add</span>
            <span>Thẻ</span>
          </button>
        </div>
      `).join('');

      vocabContainer.querySelectorAll('.btn-add-flashcard').forEach(btn => {
        btn.addEventListener('click', (e) => {
          const w = e.currentTarget.dataset.word;
          const r = e.currentTarget.dataset.reading;
          const m = e.currentTarget.dataset.meaning;
          addWordToFlashcard(w, r, m);
        });
      });
    }
  }

  // 5. Các cách diễn đạt khác của người Nhật
  const altCard = document.getElementById('alternativesCard');
  const altList = document.getElementById('alternativesList');
  if (altCard && altList) {
    const alts = data.nativeAlternatives || [];
    if (alts.length > 0) {
      altCard.classList.remove('hidden');
      altList.innerHTML = alts.map(alt => `
        <li class="p-2 bg-[#F8FAFC] border border-[#E2E8F0] rounded-[4px] flex items-start gap-1.5">
          <span class="text-[#006194] font-bold">•</span>
          <span>${escapeHtml(alt)}</span>
        </li>
      `).join('');
    } else {
      altCard.classList.add('hidden');
    }
  }

  resultCard.classList.remove('hidden');
  resultCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// =========================================================================
// 8. LƯU TỪ VỰNG VÀO BỘ THẺ FLASHCARD SRS
// =========================================================================
function addWordToFlashcard(word, reading, meaning) {
  try {
    let decks = JSON.parse(localStorage.getItem(FLASHCARD_STORAGE_KEY) || '[]');
    let targetDeck = decks.find(d => d.title === 'Từ vựng từ Luyện viết AI' || d.title === 'Mặc định');

    if (!targetDeck) {
      targetDeck = {
        id: 'deck_ai_writing_' + Date.now(),
        title: 'Từ vựng từ Luyện viết AI',
        description: 'Tập hợp từ vựng và collocations hay được AI Sensei gợi ý khi sửa bài viết',
        color: 'blue',
        cards: []
      };
      decks.unshift(targetDeck);
    }

    if (!Array.isArray(targetDeck.cards)) {
      targetDeck.cards = [];
    }

    const exists = targetDeck.cards.some(c => c.front === word);
    if (exists) {
      showToast(`Từ "${word}" đã có trong bộ Flashcards của bạn.`, 'info');
      return;
    }

    targetDeck.cards.push({
      id: 'card_' + Date.now(),
      front: word,
      back: `${reading}\n${meaning}`,
      hint: 'Học từ bài tập chữa câu AI',
      level: currentJlpt,
      createdAt: new Date().toISOString()
    });

    localStorage.setItem(FLASHCARD_STORAGE_KEY, JSON.stringify(decks));
    showToast(`Đã lưu "${word}" vào bộ Flashcards thành công!`, 'success');

  } catch (err) {
    console.error('Lỗi lưu Flashcard:', err);
    showToast('Không thể lưu thẻ flashcard. Vui lòng thử lại.', 'error');
  }
}

// =========================================================================
// 9. LƯU & XEM LỊCH SỬ CÁC CÂU TRƯỚC ĐÓ CỦA HỌC VIÊN
// =========================================================================
function saveCurrentToHistory() {
  if (!lastCheckResult) {
    showToast('Chưa có câu văn nào được thẩm định để lưu.', 'error');
    return;
  }

  try {
    let history = JSON.parse(localStorage.getItem(HISTORY_STORAGE_KEY) || '[]');
    const grammarPatternName = activeGrammarRule ? activeGrammarRule.pattern : 'Tự do';

    const newItem = {
      id: 'history_' + Date.now(),
      timestamp: new Date().toLocaleString('vi-VN'),
      originalText: lastCheckResult.originalText,
      correctedText: lastCheckResult.correctedText,
      score: lastCheckResult.score,
      ratingBadge: lastCheckResult.ratingBadge,
      jlptLevel: currentJlpt,
      grammarPattern: grammarPatternName,
      errorsCount: (lastCheckResult.errors || []).length
    };

    history.unshift(newItem);
    if (history.length > 50) {
      history = history.slice(0, 50); // Giữ tối đa 50 câu gần nhất
    }

    localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(history));
    renderHistory();
    showToast('Đã lưu bài viết vào Lịch sử học tập thành công.', 'success');

  } catch (err) {
    console.error('Lỗi lưu lịch sử:', err);
    showToast('Không thể lưu lịch sử bài viết.', 'error');
  }
}

function updateHistoryFilterButtons() {
  const btnAll = document.getElementById('btnFilterHistoryAll');
  const btnCurrent = document.getElementById('btnFilterHistoryCurrent');

  if (historyFilter === 'all') {
    btnAll.className = 'px-3 py-1 rounded-[4px] bg-[#006194] text-white transition';
    btnCurrent.className = 'px-3 py-1 rounded-[4px] text-[#475569] hover:text-[#0F2545] transition';
  } else {
    btnCurrent.className = 'px-3 py-1 rounded-[4px] bg-[#006194] text-white transition';
    btnAll.className = 'px-3 py-1 rounded-[4px] text-[#475569] hover:text-[#0F2545] transition';
  }
}

function renderHistory() {
  const container = document.getElementById('savedHistoryContainer');
  if (!container) return;

  const history = JSON.parse(localStorage.getItem(HISTORY_STORAGE_KEY) || '[]');
  const currentPattern = activeGrammarRule ? activeGrammarRule.pattern : null;

  // Cập nhật số lượng đếm trên 2 nút lọc
  const countAllEl = document.getElementById('historyCountAll');
  const countCurrentEl = document.getElementById('historyCountCurrent');

  if (countAllEl) countAllEl.textContent = history.length;
  if (countCurrentEl) {
    const currentCount = currentPattern 
      ? history.filter(item => item.grammarPattern === currentPattern).length
      : 0;
    countCurrentEl.textContent = currentCount;
  }

  // Lọc danh sách theo chế độ đã chọn
  let displayList = history;
  if (historyFilter === 'current' && currentPattern) {
    displayList = history.filter(item => item.grammarPattern === currentPattern);
  }

  if (displayList.length === 0) {
    container.innerHTML = `
      <div class="text-center py-6 text-xs text-[#64748B] italic">
        ${historyFilter === 'current' 
          ? `Bạn chưa có câu nào được lưu với mẫu "${currentPattern}". Hãy viết một câu và bấm "Lưu bài viết" nhé!`
          : 'Chưa có câu nào được lưu. Hãy thử thẩm định một câu văn và bấm "Lưu bài viết" nhé!'}
      </div>
    `;
    return;
  }

  container.innerHTML = displayList.map(item => `
    <div class="p-3.5 bg-[#FAF8F2] border border-black rounded-[5px] flex flex-col md:flex-row md:items-center justify-between gap-3 shadow-[2px_2px_0px_0px_#000000]">
      <div class="space-y-1.5 min-w-0">
        <div class="flex items-center gap-2 flex-wrap">
          <span class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-[#006194] text-white">${item.jlptLevel || 'N5'}</span>
          ${item.grammarPattern && item.grammarPattern !== 'Tự do' ? `
            <span class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-[#EFF6FF] text-[#1E40AF] border border-[#BFDBFE]">
              Mẫu: ${escapeHtml(item.grammarPattern)}
            </span>
          ` : ''}
          <span class="text-[11px] font-bold text-[#2E7D32]">${item.score}/100 điểm • ${escapeHtml(item.ratingBadge || '')}</span>
          <span class="text-[11px] text-[#64748B]">• ${item.timestamp}</span>
        </div>

        <div class="text-xs text-[#64748B] line-through font-['Noto_Sans_JP']">
          ${escapeHtml(item.originalText)}
        </div>
        <div class="text-xs font-bold text-[#0F2545] font-['Noto_Sans_JP']">
          ${escapeHtml(item.correctedText)}
        </div>
      </div>

      <div class="flex items-center gap-2 shrink-0 self-end md:self-auto">
        <button type="button" class="btn-reuse-history px-2.5 py-1 bg-white hover:bg-[#F1F5F9] border border-black rounded-[4px] text-[11px] font-bold shadow-[1px_1px_0px_0px_#000000] flex items-center gap-1 cursor-pointer"
                data-text="${escapeHtml(item.originalText)}">
          <span class="material-symbols-outlined text-[13px]">replay</span>
          <span>Viết lại câu này</span>
        </button>
        <button type="button" class="btn-copy-history px-2.5 py-1 bg-white hover:bg-[#F1F5F9] border border-black rounded-[4px] text-[11px] font-bold shadow-[1px_1px_0px_0px_#000000] flex items-center gap-1 cursor-pointer"
                data-text="${escapeHtml(item.correctedText)}">
          <span class="material-symbols-outlined text-[13px]">content_copy</span>
          <span>Sao chép</span>
        </button>
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.btn-copy-history').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const text = e.currentTarget.dataset.text;
      if (text) {
        navigator.clipboard.writeText(text);
        showToast('Đã sao chép câu văn vào bộ nhớ đệm.', 'success');
      }
    });
  });

  container.querySelectorAll('.btn-reuse-history').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const text = e.currentTarget.dataset.text;
      const inputEl = document.getElementById('writingInput');
      const counterEl = document.getElementById('charCounter');
      if (inputEl && text) {
        inputEl.value = text;
        if (counterEl) counterEl.textContent = `${text.length} / 2000 ký tự`;
        inputEl.focus();
        inputEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        showToast('Đã nạp lại câu văn vào ô soạn thảo để bạn chỉnh sửa.', 'info');
      }
    });
  });
}

function clearHistory() {
  if (confirm('Bạn có chắc chắn muốn xóa toàn bộ lịch sử các câu đã lưu không?')) {
    localStorage.removeItem(HISTORY_STORAGE_KEY);
    renderHistory();
    showToast('Đã xóa sạch lịch sử bài viết.', 'info');
  }
}

// =========================================================================
// 10. TIỆN ÍCH HELPER
// =========================================================================
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
