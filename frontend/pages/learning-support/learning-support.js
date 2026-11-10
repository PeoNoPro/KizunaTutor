/**
 * KizunaTutor - Trang Học & Tra Cứu Từ Vựng Theo Chủ Đề (Learning Support)
 * Hỗ trợ 3 chế độ xem: 🎯 Chuẩn thi JLPT | 🈸 Trọng tâm Hán tự | 🔤 Trọng tâm Kana
 */
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';
import { showToast } from '/js/main.js';
import { apiClient } from '/js/apiClient.js';
import { voicevoxClient } from '/js/voicevoxClient.js';

let pendingVocabToSave = null;
let lastSelectedVocabDeckId = '';

let currentTopics = [
  { id: 'JLPT_N5', code: 'JLPT_N5', titleVn: 'JLPT N5 Căn bản', icon: '🌱', count: '650' },
  { id: 'JLPT_N4', code: 'JLPT_N4', titleVn: 'JLPT N4 Sơ cấp', icon: '🌿', count: '750' },
  { id: 'JLPT_N3', code: 'JLPT_N3', titleVn: 'JLPT N3 Trung cấp', icon: '🎋', count: '1.200' },
  { id: 'JLPT_N2', code: 'JLPT_N2', titleVn: 'JLPT N2 Cao cấp', icon: '🌸', count: '1.800' },
  { id: 'JLPT_N1', code: 'JLPT_N1', titleVn: 'JLPT N1 Thượng cấp', icon: '⛩️', count: '2.400' },
  { id: 'LIFE_DAILY', code: 'LIFE_DAILY', titleVn: 'Giao tiếp hàng ngày', icon: '💬', count: '450' },
  { id: 'WORKPLACE', code: 'WORKPLACE', titleVn: 'Tiếng Nhật công sở', icon: '💼', count: '380' }
];

let activeTopic = 'JLPT_N5';
let vocabList = [];
let currentViewMode = localStorage.getItem('kizuna_vocab_view_mode') || 'jlpt'; // 'jlpt' | 'kanji' | 'kana'

// Danh sách các từ vựng ưu tiên hiển thị Hiragana / Katakana ở trình độ sơ cấp (JLPT N5/N4)
// theo đúng quy chuẩn khảo thí JLPT và giáo trình Minna no Nihongo
const KANA_PREFER_SET = new Set([
  // Đại từ, từ chỉ định, nghi vấn từ
  'これ', 'それ', 'あれ', 'どれ', 'ここ', 'そこ', 'あそこ', 'どこ',
  'こちら', 'そちら', 'あちら', 'どちら', 'こっち', 'そっち', 'あっち', 'どっち',
  'だれ', 'どなた', 'いつ', 'いくら', 'いくつ', 'どう', 'どんな', 'どうして', 'なぜ', 'なに', 'なん',
  'あなた', 'あの', 'この', 'その', 'どの', 'ぼく', 'わたし',
  // Lời chào, phó từ, liên từ
  'ありがとう', 'こんにちは', 'こんばんは', 'おはよう', 'さようなら', 'すみません', 'ごめんなさい',
  'どうぞ', 'どうも', 'たくさん', 'あまり', 'とても', 'たいへん', 'すこし', 'ちょっと', 'ちょうど',
  'だんだん', 'そろそろ', 'いつも', 'ときどき', 'たぶん', 'ぜんぜん', 'もっと', 'もう', 'まだ',
  'いっしょに', 'まっすぐ', 'ゆっくり', 'よく', 'すぐ', 'また', 'そして', 'それから', 'でも',
  // Danh từ thường viết bằng Kana ở N5/N4 (Chữ Hán thuộc N2/N1)
  'あめ', 'つくえ', 'いす', 'くつ', 'くつした', 'めがね', 'さいふ', 'かぎ', 'かばん', 'はこ',
  'りんご', 'みかん', 'たまご', 'ごはん', 'おちゃ', 'みず', 'くだもの', 'おかし', 'ねこ', 'いぬ',
  'とり', 'さかな', 'へや', 'にわ', 'うち', 'いえ', 'まち', 'くるま', 'バス', 'タクシー', 'パン',
  'テレビ', 'ラジオ', 'カメラ', 'コーヒー', 'トイレ', 'シャワー', 'ベッド', 'シャツ', 'ネクタイ',
  // Động từ thường viết bằng Kana ở sơ cấp
  'やる', 'くれる', 'もらう', 'あげる', 'いる', 'ある', 'あびる', 'できる',
  // Tính từ thường viết bằng Kana ở sơ cấp
  'いい', 'よい', 'きれい', 'きれいな', 'かわいい', 'おいしい', 'まずい', 'すごい'
]);

function boot() {
  initLearnerShell({
    activeMenu: 'learning-support',
    pageTitle: 'Tra cứu từ vựng',
    badge: 'Học tập & rèn luyện'
  });

  initPage();
  initViewModeToggles();
  initVocabDeckModal();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', boot);
} else {
  boot();
}

async function initPage() {
  await loadTopics();
  renderTopicPills();
  await loadVocabularies();

  document.getElementById('searchVocabInput')?.addEventListener('input', (e) => {
    filterVocab(e.target.value);
  });
}

/**
 * Khởi tạo bộ nút chuyển chế độ xem: Chuẩn JLPT / Chữ Hán / Chữ Kana
 */
function initViewModeToggles() {
  updateViewModeButtons();

  document.querySelectorAll('[data-view-mode]').forEach(btn => {
    btn.addEventListener('click', () => {
      const mode = btn.dataset.viewMode;
      if (mode && mode !== currentViewMode) {
        currentViewMode = mode;
        try {
          localStorage.setItem('kizuna_vocab_view_mode', mode);
        } catch (e) {}
        updateViewModeButtons();
        renderVocabGrid(vocabList);

        const modeNames = {
          jlpt: '🎯 Chuẩn thi JLPT (Tự động thích ứng Kana/Kanji)',
          kanji: '🈸 Trọng tâm Hán tự (Ưu tiên mặt chữ Hán)',
          kana: '🔤 Trọng tâm Kana (Ưu tiên Hiragana / Katakana)'
        };
        showToast(`Đã chuyển chế độ: ${modeNames[mode] || mode}`, 'info');
      }
    });
  });
}

function updateViewModeButtons() {
  const modes = ['jlpt', 'kanji', 'kana'];
  modes.forEach(m => {
    const btn = document.querySelector(`[data-view-mode="${m}"]`);
    if (btn) {
      if (m === currentViewMode) {
        btn.className = 'px-2.5 py-1 text-xs font-bold rounded-md transition cursor-pointer bg-white text-[#831843] shadow-xs';
      } else {
        btn.className = 'px-2.5 py-1 text-xs font-medium rounded-md transition cursor-pointer text-[#64748B] hover:text-[#0F172A]';
      }
    }
  });
}

/**
 * Kiểm tra xem từ vựng nên hiển thị to bằng Kana hay Kanji theo chuẩn thi JLPT
 */
function shouldDisplayKana(v) {
  if (currentViewMode === 'kana') return true;
  if (currentViewMode === 'kanji') return false;

  // Chế độ 'jlpt':
  const word = (v.word || '').trim();
  const reading = (v.reading || v.furigana || '').trim();
  const level = (v.level || '').toUpperCase();

  // 1. Nếu từ vốn là thuần Kana (word == reading hoặc không có Hán Việt)
  if (word === reading || !v.hanViet || v.hanViet.trim() === '') return true;

  // 2. Nếu là từ mượn Katakana hoặc từ thuần Hiragana
  if (/^[ぁ-んァ-ヶー・\s]+$/.test(word)) return true;

  // 3. Nếu là N5 hoặc N4 và nằm trong tập từ vựng ưu tiên Kana
  if (level === 'N5' || level === 'N4' || !level || level === 'JLPT') {
    if (KANA_PREFER_SET.has(reading) || KANA_PREFER_SET.has(word)) {
      return true;
    }
  }

  return false;
}

async function loadTopics() {
  try {
    const res = await fetch('/api/learner/topics');
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        currentTopics = data;
      }
    }
  } catch (e) {
    // Sử dụng danh mục mặc định
  }
}

function renderTopicPills() {
  const container = document.getElementById('topicPillsContainer');
  if (!container) return;

  container.innerHTML = currentTopics.map(t => {
    const topicKey = String(t.code || t.id || '');
    const isActive = topicKey === activeTopic;
    return `
      <button type="button"
              data-topic="${topicKey}"
              class="px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap shrink-0 transition flex items-center gap-1.5 cursor-pointer ${
                isActive
                  ? 'bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]'
                  : 'bg-white border border-[#CBD5E1] text-[#1E293B] hover:bg-slate-50'
              }">
        <span>${t.icon || '📚'}</span>
        <span>${t.titleVn}</span>
      </button>
    `;
  }).join('');

  container.querySelectorAll('button[data-topic]').forEach(btn => {
    btn.addEventListener('click', () => {
      activeTopic = btn.dataset.topic;
      const matched = currentTopics.find(t => String(t.code || t.id) === activeTopic);
      const badge = document.getElementById('activeTopicBadge');
      if (badge && matched) {
        badge.textContent = matched.titleVn;
      }
      const searchInput = document.getElementById('searchVocabInput');
      if (searchInput) searchInput.value = '';
      renderTopicPills();
      loadVocabularies();
    });
  });

  const countSummary = document.getElementById('topicCountSummary');
  if (countSummary) {
    countSummary.textContent = `${currentTopics.length} chủ đề tuyển chọn`;
  }
}

async function loadVocabularies() {
  const grid = document.getElementById('vocabGrid');
  const emptyState = document.getElementById('vocabEmptyState');
  if (!grid) return;

  grid.innerHTML = '<div class="col-span-full py-10 text-center text-xs text-slate-400">Đang tải danh sách từ vựng...</div>';

  try {
    const res = await fetch(`/api/learner/vocabularies?topic=${encodeURIComponent(activeTopic)}&limit=3500`);
    if (res.ok) {
      const data = await res.json();
      vocabList = Array.isArray(data) ? data : (data.content || data.items || []);
    } else {
      vocabList = [];
    }
  } catch (e) {
    vocabList = [];
  }

  if (!vocabList || vocabList.length === 0) {
    vocabList = getSampleVocab(activeTopic);
  }

  renderVocabGrid(vocabList);
}

function renderVocabGrid(items) {
  const grid = document.getElementById('vocabGrid');
  const emptyState = document.getElementById('vocabEmptyState');
  if (!grid) return;

  if (!items || items.length === 0) {
    grid.innerHTML = '';
    emptyState?.classList.remove('hidden');
    return;
  }

  emptyState?.classList.add('hidden');

  grid.innerHTML = items.map(v => {
    const isKanaMain = shouldDisplayKana(v);
    const mainText = isKanaMain ? (v.reading || v.furigana || v.word) : (v.word || v.kanji);
    const hasKanji = Boolean(v.word && v.reading && v.word !== v.reading && v.hanViet && v.hanViet.trim() !== '');

    let subLineHtml = '';
    if (isKanaMain) {
      if (hasKanji) {
        subLineHtml = `
          <div class="flex items-center gap-1.5 mt-0.5 text-xs text-[#64748B] font-mono truncate" title="Chữ Hán tham khảo">
            <span class="text-slate-400">Hán tự:</span>
            <span class="font-bold text-[#0F172A] font-japanese">${escapeHtml(v.word)}</span>
            <span class="text-slate-300">•</span>
            <span class="text-[#831843] font-bold uppercase">${escapeHtml(v.hanViet)}</span>
          </div>
        `;
      } else {
        subLineHtml = `
          <div class="flex items-center gap-1.5 mt-0.5 text-xs text-[#64748B] font-mono truncate">
            <span>${v.romaji ? escapeHtml(v.romaji) : 'Từ thuần Kana'}</span>
          </div>
        `;
      }
    } else {
      subLineHtml = `
        <div class="flex items-center gap-1.5 mt-0.5 text-xs text-[#64748B] font-mono truncate">
          <span>${escapeHtml(v.furigana || v.reading || '')}</span>
          ${v.hanViet ? `<span class="text-slate-300">•</span><span class="text-[#831843] font-bold uppercase">${escapeHtml(v.hanViet)}</span>` : ''}
        </div>
      `;
    }

    return `
      <div class="bg-white border border-[#E2E8F0] rounded-xl p-3.5 hover:border-[#CBD5E1] transition-all flex flex-col justify-between h-full min-w-0 group">
        <div class="space-y-2.5 flex-1 min-w-0">
          <div class="flex items-start justify-between gap-2 pb-2.5 border-b border-[#E2E8F0]">
            <div class="min-w-0">
              <div class="text-xl md:text-2xl font-bold font-japanese text-[#0F172A] leading-snug truncate" title="${escapeHtml(mainText)}">
                ${escapeHtml(mainText)}
              </div>
              ${subLineHtml}
            </div>
            <span class="px-2.5 py-0.5 text-[11px] font-bold rounded-full bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8] shrink-0">
              ${escapeHtml(v.level || 'JLPT')}
            </span>
          </div>

          <div class="text-xs md:text-sm font-semibold text-[#0F172A] leading-relaxed break-words">
            ${escapeHtml(v.meaning || v.meaningVn || 'Đang cập nhật')}
          </div>

          ${v.example ? `
            <div class="p-2.5 bg-[#F8FAFC] border border-[#E2E8F0] rounded-xl text-xs space-y-1 break-words">
              <div class="font-japanese font-medium text-[#0F172A]">${escapeHtml(v.example)}</div>
              <div class="text-[#64748B] italic">${escapeHtml(v.exampleMeaning || '')}</div>
            </div>
          ` : ''}
        </div>

        <!-- Cụm nút hành động bám sát đáy (mt-auto) -->
        <div class="pt-3 mt-auto border-t border-[#E2E8F0] flex items-center justify-between gap-2">
          <button type="button"
                  data-action="speak-vocab"
                  data-word="${escapeHtml(v.word || v.kanji || v.reading)}"
                  class="flex-1 py-1.5 px-3 rounded-lg border border-[#CBD5E1] bg-white hover:bg-[#FFE5EC] hover:border-[#FBCFE8] text-[#0F172A] hover:text-[#831843] text-xs font-semibold flex items-center justify-center gap-1.5 transition cursor-pointer"
                  title="Nghe phát âm từ vựng">
            <span class="material-symbols-outlined text-[16px] text-[#831843]">volume_up</span>
            <span>Phát âm</span>
          </button>
          <button type="button"
                  data-action="save-vocab"
                  data-word="${escapeHtml(isKanaMain ? (v.reading || v.word) : (v.word || v.reading))}"
                  data-reading="${escapeHtml(v.furigana || v.reading || '')}"
                  data-meaning="${escapeHtml(v.meaning || v.meaningVn || '')}"
                  data-level="${escapeHtml(v.level || 'JLPT')}"
                  class="p-1.5 rounded-lg border border-[#CBD5E1] bg-white hover:bg-[#FFE5EC] text-[#64748B] hover:text-[#831843] transition cursor-pointer"
                  title="Cất vào bộ thẻ Flashcards">
            <span class="material-symbols-outlined text-[18px]">bookmark_add</span>
          </button>
          <button type="button"
                  onclick="navigator.clipboard?.writeText('${escapeHtml(v.word || v.kanji)}'); showToast('Đã sao chép từ vựng.', 'info');"
                  class="p-1.5 rounded-lg border border-[#CBD5E1] bg-white hover:bg-slate-50 text-[#64748B] hover:text-[#0F172A] transition cursor-pointer"
                  title="Sao chép từ vựng">
            <span class="material-symbols-outlined text-[18px]">content_copy</span>
          </button>
        </div>
      </div>
    `;
  }).join('');

  // Gắn sự kiện cho các nút hành động
  grid.querySelectorAll('[data-action="speak-vocab"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      speakWord(btn.dataset.word);
    });
  });

  grid.querySelectorAll('[data-action="save-vocab"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      openVocabSaveDeckModal(btn.dataset.word, btn.dataset.reading, btn.dataset.meaning, btn.dataset.level);
    });
  });
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

function speakWord(text) {
  if (!text) return;
  try {
    voicevoxClient.speak(text);
  } catch (e) {
    console.debug('Lỗi phát âm từ vựng:', e);
  }
}

function initVocabDeckModal() {
  const modal = document.getElementById('vocabSaveToDeckModal');
  const closeBtn = document.getElementById('btnCloseVocabDeckModal');
  const cancelBtn = document.getElementById('btnCancelVocabDeckModal');
  const form = document.getElementById('formSaveVocabToDeck');
  const selectEl = document.getElementById('modalVocabDeckSelect');
  const newWrapper = document.getElementById('newVocabDeckInputWrapper');
  const newNameInput = document.getElementById('modalNewVocabDeckNameInput');

  if (!modal || !form || !selectEl) return;

  closeBtn?.addEventListener('click', closeVocabDeckModal);
  cancelBtn?.addEventListener('click', closeVocabDeckModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeVocabDeckModal();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
      closeVocabDeckModal();
    }
  });

  selectEl.addEventListener('change', () => {
    if (selectEl.value === '__NEW__') {
      newWrapper?.classList.remove('hidden');
      newNameInput?.focus();
    } else {
      newWrapper?.classList.add('hidden');
    }
  });

  form.addEventListener('submit', submitSaveVocabToDeck);
}

async function openVocabSaveDeckModal(word, reading, meaning, level = 'N5') {
  if (!word) return;
  pendingVocabToSave = { word, reading: reading || '', meaning: meaning || '', level: level || 'N5' };

  const wordEl = document.getElementById('modalVocabWord');
  const readingEl = document.getElementById('modalVocabReading');
  const meaningEl = document.getElementById('modalVocabMeaning');
  const levelEl = document.getElementById('modalVocabLevel');

  if (wordEl) wordEl.textContent = word;
  if (readingEl) readingEl.textContent = (reading && reading !== word) ? `(${reading})` : '';
  if (meaningEl) meaningEl.textContent = meaning || 'Chưa cập nhật nghĩa';
  if (levelEl) levelEl.textContent = level || 'N5';

  const modal = document.getElementById('vocabSaveToDeckModal');
  if (modal) {
    modal.classList.remove('hidden');
    modal.classList.add('flex');
  }

  await loadVocabDeckOptions();
}

function closeVocabDeckModal() {
  const modal = document.getElementById('vocabSaveToDeckModal');
  if (modal) {
    modal.classList.add('hidden');
    modal.classList.remove('flex');
  }
}

async function loadVocabDeckOptions() {
  const selectEl = document.getElementById('modalVocabDeckSelect');
  const newWrapper = document.getElementById('newVocabDeckInputWrapper');
  const newNameInput = document.getElementById('modalNewVocabDeckNameInput');
  if (!selectEl) return;

  selectEl.innerHTML = '<option value="">Đang tải danh sách bộ thẻ...</option>';
  if (newWrapper) newWrapper.classList.add('hidden');
  if (newNameInput) newNameInput.value = '';

  let decks = [];
  try {
    decks = await apiClient.getDecks();
  } catch (err) {
    console.error('Lỗi tải danh sách bộ thẻ:', err);
  }

  if (!Array.isArray(decks) || decks.length === 0) {
    selectEl.innerHTML = `
      <option value="__DEFAULT_VOCAB__">Từ Vựng Sổ Tay (Tạo mặc định • 0 thẻ)</option>
      <option value="__NEW__">➕ Tạo bộ thẻ mới...</option>
    `;
    return;
  }

  const optionsHtml = decks.map(d => {
    const count = Array.isArray(d.cards) ? d.cards.length : 0;
    return `<option value="${escapeHtml(d.id)}">${escapeHtml(d.title || 'Bộ thẻ')} (${count} thẻ)</option>`;
  }).join('');

  selectEl.innerHTML = optionsHtml + '<option value="__NEW__">➕ Tạo bộ thẻ mới...</option>';

  if (lastSelectedVocabDeckId && decks.some(d => String(d.id) === String(lastSelectedVocabDeckId))) {
    selectEl.value = lastSelectedVocabDeckId;
  }
}

async function submitSaveVocabToDeck(e) {
  e.preventDefault();
  if (!pendingVocabToSave) return;

  const selectEl = document.getElementById('modalVocabDeckSelect');
  const newNameInput = document.getElementById('modalNewVocabDeckNameInput');
  const submitBtn = document.getElementById('btnConfirmSaveVocabDeck');
  if (!selectEl) return;

  const selectedValue = selectEl.value;
  const { word, reading, meaning, level } = pendingVocabToSave;

  if (selectedValue === '__NEW__' && (!newNameInput || !newNameInput.value.trim())) {
    showToast('Vui lòng nhập tên bộ thẻ mới.', 'error');
    newNameInput?.focus();
    return;
  }

  const defaultBtnHtml = submitBtn ? submitBtn.innerHTML : '';
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span>Đang lưu...</span>';
  }

  try {
    const decks = await apiClient.getDecks();
    const backText = reading ? `${reading} • ${meaning}` : meaning;
    const newCard = {
      id: 'card_' + Date.now(),
      char: word,
      front: word,
      meaning: backText,
      back: backText,
      reading: reading || '',
      level: level || 'N5',
      createdAt: new Date().toISOString()
    };

    if (selectedValue === '__NEW__' || selectedValue === '__DEFAULT_VOCAB__') {
      const title = selectedValue === '__NEW__' ? newNameInput.value.trim() : 'Từ Vựng Sổ Tay';
      const created = await apiClient.createDeck({
        id: 'deck_vocab_' + Date.now(),
        title,
        description: 'Bộ thẻ từ vựng lưu từ chuyên mục Tra Cứu Từ Vựng',
        color: 'emerald',
        cards: [newCard]
      });
      lastSelectedVocabDeckId = created.id;
      showToast(`Đã lưu "${word}" vào bộ thẻ mới "${created.title}"!`, 'success');
    } else {
      const targetDeck = decks.find(d => String(d.id) === String(selectedValue));
      if (!targetDeck) {
        showToast('Không tìm thấy bộ thẻ đã chọn.', 'error');
        return;
      }

      const cards = Array.isArray(targetDeck.cards) ? [...targetDeck.cards] : [];
      const exists = cards.some(c => (c.char || c.front || c.word) === word);
      if (exists) {
        showToast(`Từ "${word}" đã có sẵn trong bộ thẻ "${targetDeck.title}".`, 'info');
        closeVocabDeckModal();
        return;
      }

      cards.push(newCard);
      await apiClient.updateDeck(targetDeck.id, {
        title: targetDeck.title,
        description: targetDeck.description || '',
        color: targetDeck.color || 'blue',
        cards
      });
      lastSelectedVocabDeckId = targetDeck.id;
      showToast(`Đã lưu "${word}" vào bộ thẻ "${targetDeck.title}" thành công!`, 'success');
    }

    closeVocabDeckModal();
  } catch (err) {
    console.error('Lỗi lưu từ vựng vào bộ thẻ:', err);
    showToast('Không thể lưu từ vựng vào bộ thẻ lúc này.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = defaultBtnHtml;
    }
  }
}

function filterVocab(kw) {
  const query = (kw || '').trim().toLowerCase();
  if (!query) {
    renderVocabGrid(vocabList);
    return;
  }
  const filtered = vocabList.filter(v => {
    return (v.word && v.word.toLowerCase().includes(query)) ||
           (v.kanji && v.kanji.toLowerCase().includes(query)) ||
           (v.furigana && v.furigana.toLowerCase().includes(query)) ||
           (v.reading && v.reading.toLowerCase().includes(query)) ||
           (v.meaning && v.meaning.toLowerCase().includes(query)) ||
           (v.meaningVn && v.meaningVn.toLowerCase().includes(query)) ||
           (v.hanViet && v.hanViet.toLowerCase().includes(query));
  });
  renderVocabGrid(filtered);
}

function getSampleVocab(topic) {
  return [
    { word: '勉強', kanji: '勉強', furigana: 'べんきょう', hanViet: 'MIỄN CƯỠNG', level: 'N5', meaning: 'Học tập, học hành', example: '毎日日本語を勉強します。', exampleMeaning: 'Mỗi ngày tôi đều học tiếng Nhật.' },
    { word: '友達', kanji: '友達', furigana: 'ともだち', hanViet: 'HỮU ĐẠT', level: 'N5', meaning: 'Bạn bè, người bạn', example: '友達と遊びに行きます。', exampleMeaning: 'Tôi đi chơi cùng với bạn bè.' },
    { word: '時間', kanji: '時間', furigana: 'じかん', hanViet: 'THỜI GIAN', level: 'N5', meaning: 'Thời gian, giờ giấc', example: '時間がありません。', exampleMeaning: 'Tôi không có thời gian.' },
    { word: '仕事', kanji: '仕事', furigana: 'しごと', hanViet: 'SĨ SỰ', level: 'N5', meaning: 'Công việc, nghề nghiệp', example: '明日は仕事があります。', exampleMeaning: 'Ngày mai tôi có công việc.' },
    { word: '家族', kanji: '家族', furigana: 'かぞく', hanViet: 'GIA TỘC', level: 'N5', meaning: 'Gia đình', example: '私の家族は4人です。', exampleMeaning: 'Gia đình tôi gồm có 4 người.' },
    { word: '約束', kanji: '約束', furigana: 'やくそく', hanViet: 'ƯỚC THÚC', level: 'N4', meaning: 'Lời hứa, cuộc hẹn', example: '友達と約束があります。', exampleMeaning: 'Tôi có cuộc hẹn với bạn bè.' }
  ];
}
