/**
 * KizunaTutor - Đấu trường gõ chữ Kana (Hiragana & Katakana)
 * Module Feature-Based độc lập tuân thủ FRONTEND_RULES.md
 */

import { showToast } from '/js/main.js';

// =========================================================================
// 1. TỪ ĐIỂN DỮ LIỆU KANA TOÀN DIỆN (HIRAGANA & KATAKANA)
// =========================================================================
const KANA_RAW_DEFINITIONS = [
  // --- 1. KANA CHÍNH (MAIN KANA - 46 KÝ TỰ) ---
  { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'あ', k: 'ア', r: 'a', aliases: ['a'] },
  { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'い', k: 'イ', r: 'i', aliases: ['i'] },
  { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'う', k: 'ウ', r: 'u', aliases: ['u'] },
  { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'え', k: 'エ', r: 'e', aliases: ['e'] },
  { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'お', k: 'オ', r: 'o', aliases: ['o'] },

  { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'か', k: 'カ', r: 'ka', aliases: ['ka'] },
  { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'き', k: 'キ', r: 'ki', aliases: ['ki'] },
  { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'く', k: 'ク', r: 'ku', aliases: ['ku'] },
  { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'け', k: 'ケ', r: 'ke', aliases: ['ke'] },
  { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'こ', k: 'コ', r: 'ko', aliases: ['ko'] },

  { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'さ', k: 'サ', r: 'sa', aliases: ['sa'] },
  { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'し', k: 'シ', r: 'shi', aliases: ['shi', 'si'] },
  { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'す', k: 'ス', r: 'su', aliases: ['su'] },
  { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'せ', k: 'セ', r: 'se', aliases: ['se'] },
  { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'そ', k: 'ソ', r: 'so', aliases: ['so'] },

  { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'た', k: 'タ', r: 'ta', aliases: ['ta'] },
  { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'ち', k: 'チ', r: 'chi', aliases: ['chi', 'ti'] },
  { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'つ', k: 'ツ', r: 'tsu', aliases: ['tsu', 'tu'] },
  { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'て', k: 'テ', r: 'te', aliases: ['te'] },
  { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'と', k: 'ト', r: 'to', aliases: ['to'] },

  { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'な', k: 'ナ', r: 'na', aliases: ['na'] },
  { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'に', k: 'ニ', r: 'ni', aliases: ['ni'] },
  { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'ぬ', k: 'ヌ', r: 'nu', aliases: ['nu'] },
  { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'ね', k: 'ネ', r: 'ne', aliases: ['ne'] },
  { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'の', k: 'ノ', r: 'no', aliases: ['no'] },

  { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'は', k: 'ハ', r: 'ha', aliases: ['ha'] },
  { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'ひ', k: 'ヒ', r: 'hi', aliases: ['hi'] },
  { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'ふ', k: 'フ', r: 'fu', aliases: ['fu', 'hu'] },
  { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'へ', k: 'ヘ', r: 'he', aliases: ['he'] },
  { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'ほ', k: 'ホ', r: 'ho', aliases: ['ho'] },

  { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'ま', k: 'マ', r: 'ma', aliases: ['ma'] },
  { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'み', k: 'ミ', r: 'mi', aliases: ['mi'] },
  { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'む', k: 'ム', r: 'mu', aliases: ['mu'] },
  { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'め', k: 'メ', r: 'me', aliases: ['me'] },
  { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'も', k: 'モ', r: 'mo', aliases: ['mo'] },

  { rowKey: 'ya', rowLabel: 'Hàng ya', cat: 'main', h: 'や', k: 'ヤ', r: 'ya', aliases: ['ya'] },
  { rowKey: 'ya', rowLabel: 'Hàng ya', cat: 'main', h: 'ゆ', k: 'ユ', r: 'yu', aliases: ['yu'] },
  { rowKey: 'ya', rowLabel: 'Hàng ya', cat: 'main', h: 'よ', k: 'ヨ', r: 'yo', aliases: ['yo'] },

  { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'ら', k: 'ラ', r: 'ra', aliases: ['ra'] },
  { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'り', k: 'リ', r: 'ri', aliases: ['ri'] },
  { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'る', k: 'ル', r: 'ru', aliases: ['ru'] },
  { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'れ', k: 'レ', r: 're', aliases: ['re'] },
  { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'ろ', k: 'ロ', r: 'ro', aliases: ['ro'] },

  { rowKey: 'wa', rowLabel: 'Hàng wa', cat: 'main', h: 'わ', k: 'ワ', r: 'wa', aliases: ['wa'] },
  { rowKey: 'wa', rowLabel: 'Hàng wa', cat: 'main', h: 'を', k: 'ヲ', r: 'wo', aliases: ['wo', 'o'] },
  { rowKey: 'wa', rowLabel: 'Hàng wa', cat: 'main', h: 'ん', k: 'ン', r: 'n', aliases: ['n', 'nn'] },

  // --- 2. BIẾN ÂM DAKUTEN & HANDAKUTEN (25 KÝ TỰ) ---
  { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'が', k: 'ガ', r: 'ga', aliases: ['ga'] },
  { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'ぎ', k: 'ギ', r: 'gi', aliases: ['gi'] },
  { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'ぐ', k: 'グ', r: 'gu', aliases: ['gu'] },
  { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'げ', k: 'ゲ', r: 'ge', aliases: ['ge'] },
  { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'ご', k: 'ゴ', r: 'go', aliases: ['go'] },

  { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ざ', k: 'ザ', r: 'za', aliases: ['za'] },
  { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'じ', k: 'ジ', r: 'ji', aliases: ['ji', 'zi'] },
  { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ず', k: 'ズ', r: 'zu', aliases: ['zu'] },
  { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ぜ', k: 'ゼ', r: 'ze', aliases: ['ze'] },
  { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ぞ', k: 'ゾ', r: 'zo', aliases: ['zo'] },

  { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'だ', k: 'ダ', r: 'da', aliases: ['da'] },
  { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'ぢ', k: 'ヂ', r: 'ji', aliases: ['ji', 'di'] },
  { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'づ', k: 'ヅ', r: 'zu', aliases: ['zu', 'du'] },
  { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'で', k: 'デ', r: 'de', aliases: ['de'] },
  { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'ど', k: 'ド', r: 'do', aliases: ['do'] },

  { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'ば', k: 'バ', r: 'ba', aliases: ['ba'] },
  { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'び', k: 'ビ', r: 'bi', aliases: ['bi'] },
  { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'ぶ', k: 'ブ', r: 'bu', aliases: ['bu'] },
  { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'べ', k: 'ベ', r: 'be', aliases: ['be'] },
  { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'ぼ', k: 'ボ', r: 'bo', aliases: ['bo'] },

  { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぱ', k: 'パ', r: 'pa', aliases: ['pa'] },
  { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぴ', k: 'ピ', r: 'pi', aliases: ['pi'] },
  { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぷ', k: 'プ', r: 'pu', aliases: ['pu'] },
  { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぺ', k: 'ペ', r: 'pe', aliases: ['pe'] },
  { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぽ', k: 'ポ', r: 'po', aliases: ['po'] },

  // --- 3. ÂM GHÉP (YOUON - 33 KÝ TỰ) ---
  { rowKey: 'kya', rowLabel: 'Hàng kya', cat: 'combination', h: 'きゃ', k: 'キャ', r: 'kya', aliases: ['kya'] },
  { rowKey: 'kya', rowLabel: 'Hàng kya', cat: 'combination', h: 'きゅ', k: 'キュ', r: 'kyu', aliases: ['kyu'] },
  { rowKey: 'kya', rowLabel: 'Hàng kya', cat: 'combination', h: 'きょ', k: 'キョ', r: 'kyo', aliases: ['kyo'] },

  { rowKey: 'sha', rowLabel: 'Hàng sha', cat: 'combination', h: 'しゃ', k: 'シャ', r: 'sha', aliases: ['sha', 'sya'] },
  { rowKey: 'sha', rowLabel: 'Hàng sha', cat: 'combination', h: 'しゅ', k: 'シュ', r: 'shu', aliases: ['shu', 'syu'] },
  { rowKey: 'sha', rowLabel: 'Hàng sha', cat: 'combination', h: 'しょ', k: 'ショ', r: 'sho', aliases: ['sho', 'syo'] },

  { rowKey: 'cha', rowLabel: 'Hàng cha', cat: 'combination', h: 'ちゃ', k: 'チャ', r: 'cha', aliases: ['cha', 'tya'] },
  { rowKey: 'cha', rowLabel: 'Hàng cha', cat: 'combination', h: 'ちゅ', k: 'チュ', r: 'chu', aliases: ['chu', 'tyu'] },
  { rowKey: 'cha', rowLabel: 'Hàng cha', cat: 'combination', h: 'ちょ', k: 'チョ', r: 'cho', aliases: ['cho', 'tyo'] },

  { rowKey: 'nya', rowLabel: 'Hàng nya', cat: 'combination', h: 'にゃ', k: 'ニャ', r: 'nya', aliases: ['nya'] },
  { rowKey: 'nya', rowLabel: 'Hàng nya', cat: 'combination', h: 'にゅ', k: 'ニュ', r: 'nyu', aliases: ['nyu'] },
  { rowKey: 'nya', rowLabel: 'Hàng nya', cat: 'combination', h: 'にょ', k: 'ニョ', r: 'nyo', aliases: ['nyo'] },

  { rowKey: 'hya', rowLabel: 'Hàng hya', cat: 'combination', h: 'ひゃ', k: 'ヒャ', r: 'hya', aliases: ['hya'] },
  { rowKey: 'hya', rowLabel: 'Hàng hya', cat: 'combination', h: 'ひゅ', k: 'ヒュ', r: 'hyu', aliases: ['hyu'] },
  { rowKey: 'hya', rowLabel: 'Hàng hya', cat: 'combination', h: 'ひょ', k: 'ヒョ', r: 'hyo', aliases: ['hyo'] },

  { rowKey: 'mya', rowLabel: 'Hàng mya', cat: 'combination', h: 'みゃ', k: 'ミャ', r: 'mya', aliases: ['mya'] },
  { rowKey: 'mya', rowLabel: 'Hàng mya', cat: 'combination', h: 'みゅ', k: 'ミュ', r: 'myu', aliases: ['myu'] },
  { rowKey: 'mya', rowLabel: 'Hàng mya', cat: 'combination', h: 'みょ', k: 'ミョ', r: 'myo', aliases: ['myo'] },

  { rowKey: 'rya', rowLabel: 'Hàng rya', cat: 'combination', h: 'りゃ', k: 'リャ', r: 'rya', aliases: ['rya'] },
  { rowKey: 'rya', rowLabel: 'Hàng rya', cat: 'combination', h: 'りゅ', k: 'リュ', r: 'ryu', aliases: ['ryu'] },
  { rowKey: 'rya', rowLabel: 'Hàng rya', cat: 'combination', h: 'りょ', k: 'リョ', r: 'ryo', aliases: ['ryo'] },

  { rowKey: 'gya', rowLabel: 'Hàng gya', cat: 'combination', h: 'ぎゃ', k: 'ギャ', r: 'gya', aliases: ['gya'] },
  { rowKey: 'gya', rowLabel: 'Hàng gya', cat: 'combination', h: 'ぎゅ', k: 'ギュ', r: 'gyu', aliases: ['gyu'] },
  { rowKey: 'gya', rowLabel: 'Hàng gya', cat: 'combination', h: 'ぎょ', k: 'ギョ', r: 'gyo', aliases: ['gyo'] },

  { rowKey: 'ja', rowLabel: 'Hàng ja', cat: 'combination', h: 'じゃ', k: 'ジャ', r: 'ja', aliases: ['ja', 'zya', 'jya'] },
  { rowKey: 'ja', rowLabel: 'Hàng ja', cat: 'combination', h: 'じゅ', k: 'ジュ', r: 'ju', aliases: ['ju', 'zyu', 'jyu'] },
  { rowKey: 'ja', rowLabel: 'Hàng ja', cat: 'combination', h: 'じょ', k: 'ジョ', r: 'jo', aliases: ['jo', 'zyo', 'jyo'] },

  { rowKey: 'bya', rowLabel: 'Hàng bya', cat: 'combination', h: 'びゃ', k: 'ビャ', r: 'bya', aliases: ['bya'] },
  { rowKey: 'bya', rowLabel: 'Hàng bya', cat: 'combination', h: 'びゅ', k: 'ビュ', r: 'byu', aliases: ['byu'] },
  { rowKey: 'bya', rowLabel: 'Hàng bya', cat: 'combination', h: 'びょ', k: 'ビョ', r: 'byo', aliases: ['byo'] },

  { rowKey: 'pya', rowLabel: 'Hàng pya', cat: 'combination', h: 'ぴゃ', k: 'ピャ', r: 'pya', aliases: ['pya'] },
  { rowKey: 'pya', rowLabel: 'Hàng pya', cat: 'combination', h: 'ぴゅ', k: 'ピュ', r: 'pyu', aliases: ['pyu'] },
  { rowKey: 'pya', rowLabel: 'Hàng pya', cat: 'combination', h: 'ぴょ', k: 'ピョ', r: 'pyo', aliases: ['pyo'] }
];

// =========================================================================
// 2. TRẠNG THÁI ỨNG DỤNG (APPLICATION STATE)
// =========================================================================
let currentScript = 'hiragana'; // 'hiragana' | 'katakana'
const selectedGroups = new Set(['main']);
const selectedKanaSet = new Set();

let activeQuizList = [];
let currentIndex = 0;
let quizResults = [];

// =========================================================================
// 3. XỬ LÝ DỮ LIỆU & BỘ CHỌN KANA (SELECTION LOGIC)
// =========================================================================

function getKanaItems(scriptType = currentScript) {
  return KANA_RAW_DEFINITIONS.map(def => ({
    rowKey: def.rowKey,
    rowLabel: def.rowLabel,
    category: def.cat,
    character: scriptType === 'hiragana' ? def.h : def.k,
    romaji: def.r,
    aliases: def.aliases || [def.r]
  }));
}

function recalculateSelectedKana() {
  selectedKanaSet.clear();
  const allItems = getKanaItems(currentScript);

  allItems.forEach(item => {
    if (selectedGroups.has(item.category)) {
      selectedKanaSet.add(item.character);
    }
  });

  updateSelectionCountBadge();
  renderSelectionGrid();
}

function updateSelectionCountBadge() {
  const badge = document.getElementById('selectedKanaCount');
  if (badge) badge.textContent = selectedKanaSet.size;

  const warning = document.getElementById('selectionWarning');
  if (warning) {
    if (selectedKanaSet.size === 0) {
      warning.classList.remove('hidden');
    } else {
      warning.classList.add('hidden');
    }
  }
}

function setScriptType(script) {
  currentScript = script;
  const tabHiragana = document.getElementById('tabHiragana');
  const tabKatakana = document.getElementById('tabKatakana');

  if (script === 'hiragana') {
    tabHiragana.className = 'py-3 px-4 font-bold text-sm flex items-center justify-center gap-2 rounded-xl border border-[#FBCFE8] bg-[#FFE5EC] text-[#831843] transition shadow-sm cursor-pointer';
    tabKatakana.className = 'py-3 px-4 font-semibold text-sm flex items-center justify-center gap-2 rounded-xl border border-transparent bg-transparent text-[#68737D] hover:bg-[#FAF8F2] hover:text-[#0F2545] transition cursor-pointer';
  } else {
    tabKatakana.className = 'py-3 px-4 font-bold text-sm flex items-center justify-center gap-2 rounded-xl border border-sky-300 bg-sky-100 text-[#006194] transition shadow-sm cursor-pointer';
    tabHiragana.className = 'py-3 px-4 font-semibold text-sm flex items-center justify-center gap-2 rounded-xl border border-transparent bg-transparent text-[#68737D] hover:bg-[#FAF8F2] hover:text-[#0F2545] transition cursor-pointer';
  }

  recalculateSelectedKana();
}

function toggleKanaGroup(groupKey) {
  const btnMap = {
    main: 'btnGroupMain',
    dakuten: 'btnGroupDakuten',
    combination: 'btnGroupCombination'
  };

  const btn = document.getElementById(btnMap[groupKey]);

  if (selectedGroups.has(groupKey)) {
    selectedGroups.delete(groupKey);
    if (btn) {
      btn.className = 'px-3 py-1.5 text-xs font-semibold text-[#3F4850] bg-white border border-[#ECE8DE] rounded-xl hover:bg-[#FAF8F2] transition flex items-center gap-1.5 cursor-pointer';
      btn.innerHTML = btn.innerText.replace('✓', '').trim();
    }
  } else {
    selectedGroups.add(groupKey);
    if (btn) {
      btn.className = 'px-3 py-1.5 text-xs font-semibold text-[#831843] bg-[#FFE5EC] hover:bg-[#FCE7F3] border border-[#FBCFE8] rounded-xl transition flex items-center gap-1.5 cursor-pointer';
      const label = btn.innerText.replace('✓', '').trim();
      btn.innerHTML = `<span>✓</span><span>${label}</span>`;
    }
  }

  recalculateSelectedKana();
}

function selectAllGroups() {
  selectedGroups.add('main');
  selectedGroups.add('dakuten');
  selectedGroups.add('combination');

  ['main', 'dakuten', 'combination'].forEach(g => {
    const btn = document.getElementById(g === 'main' ? 'btnGroupMain' : (g === 'dakuten' ? 'btnGroupDakuten' : 'btnGroupCombination'));
    if (btn) {
      btn.className = 'px-3 py-1.5 text-xs font-semibold text-[#831843] bg-[#FFE5EC] hover:bg-[#FCE7F3] border border-[#FBCFE8] rounded-xl transition flex items-center gap-1.5 cursor-pointer';
      const label = btn.innerText.replace('✓', '').trim();
      btn.innerHTML = `<span>✓</span><span>${label}</span>`;
    }
  });

  recalculateSelectedKana();
}

function clearAllGroups() {
  selectedGroups.clear();
  ['main', 'dakuten', 'combination'].forEach(g => {
    const btn = document.getElementById(g === 'main' ? 'btnGroupMain' : (g === 'dakuten' ? 'btnGroupDakuten' : 'btnGroupCombination'));
    if (btn) {
      btn.className = 'px-3 py-1.5 text-xs font-semibold text-[#3F4850] bg-white border border-[#ECE8DE] rounded-xl hover:bg-[#FAF8F2] transition flex items-center gap-1.5 cursor-pointer';
      btn.innerHTML = btn.innerText.replace('✓', '').trim();
    }
  });

  recalculateSelectedKana();
}

function toggleSingleKana(char) {
  if (selectedKanaSet.has(char)) {
    selectedKanaSet.delete(char);
  } else {
    selectedKanaSet.add(char);
  }
  updateSelectionCountBadge();
  renderSelectionGrid();
}

function toggleRowKana(rowKey) {
  const rowItems = getKanaItems(currentScript).filter(it => it.rowKey === rowKey);
  const allSelected = rowItems.every(it => selectedKanaSet.has(it.character));

  rowItems.forEach(it => {
    if (allSelected) {
      selectedKanaSet.delete(it.character);
    } else {
      selectedKanaSet.add(it.character);
    }
  });

  updateSelectionCountBadge();
  renderSelectionGrid();
}

function renderSelectionGrid() {
  const container = document.getElementById('kanaSelectionContainer');
  if (!container) return;

  const allItems = getKanaItems(currentScript);
  const rowMap = new Map();

  allItems.forEach(item => {
    if (!rowMap.has(item.rowKey)) {
      rowMap.set(item.rowKey, {
        rowKey: item.rowKey,
        rowLabel: item.rowLabel,
        category: item.category,
        items: []
      });
    }
    rowMap.get(item.rowKey).items.push(item);
  });

  let html = '';

  rowMap.forEach(group => {
    const rowAllActive = group.items.every(it => selectedKanaSet.has(it.character));
    const rowAnyActive = group.items.some(it => selectedKanaSet.has(it.character));

    html += `
      <div class="p-3.5 bg-white border border-[#ECE8DE] rounded-2xl flex flex-col md:flex-row md:items-center justify-between gap-3 shadow-xs">
        <div class="flex items-center gap-3 shrink-0">
          <button type="button" 
                  data-action="toggle-row" data-row="${group.rowKey}"
                  class="text-xs py-1.5 px-3 rounded-lg font-bold border transition flex items-center gap-1.5 cursor-pointer ${
                    rowAllActive 
                      ? 'bg-[#FFE5EC] border-[#FBCFE8] text-[#831843]' 
                      : (rowAnyActive ? 'bg-[#FAF8F2] border-[#ECE8DE] text-[#0F2545]' : 'bg-white border-[#ECE8DE] text-[#68737D] hover:bg-[#FAF8F2]')
                  }">
            <span>${rowAllActive ? '✓' : '＋'}</span>
            <span>${group.rowLabel}</span>
          </button>
        </div>

        <div class="flex flex-wrap items-center gap-2 flex-1 md:justify-end">
    `;

    group.items.forEach(it => {
      const isSelected = selectedKanaSet.has(it.character);
      html += `
        <button type="button" 
                data-action="toggle-kana" data-char="${it.character}"
                class="w-12 h-12 flex flex-col items-center justify-center rounded-xl border transition cursor-pointer select-none ${
                  isSelected 
                    ? 'bg-[#FFE5EC] border-[#FBCFE8] text-[#831843] font-bold shadow-xs' 
                    : 'bg-white border-[#ECE8DE] text-[#3F4850] hover:bg-[#FAF8F2]'
                }">
          <span class="text-base leading-none font-bold">${it.character}</span>
          <span class="text-[10px] opacity-70 mt-0.5 font-mono">${it.romaji}</span>
        </button>
      `;
    });

    html += `
        </div>
      </div>
    `;
  });

  container.innerHTML = html;

  // Gắn sự kiện click
  container.querySelectorAll('[data-action="toggle-row"]').forEach(btn => {
    btn.addEventListener('click', () => toggleRowKana(btn.dataset.row));
  });

  container.querySelectorAll('[data-action="toggle-kana"]').forEach(btn => {
    btn.addEventListener('click', () => toggleSingleKana(btn.dataset.char));
  });
}

// =========================================================================
// 4. ĐẤU TRƯỜNG GÕ ROMAJI (LIVE QUIZ ARENA)
// =========================================================================

function shuffleArray(arr) {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function startQuiz(customItems = null) {
  let pool = [];

  if (customItems && customItems.length > 0) {
    pool = customItems;
  } else {
    const all = getKanaItems(currentScript);
    pool = all.filter(it => selectedKanaSet.has(it.character));
  }

  if (pool.length === 0) {
    showToast('Vui lòng chọn ít nhất một chữ Kana để bắt đầu thử tài.', 'error');
    return;
  }

  activeQuizList = shuffleArray(pool);
  currentIndex = 0;
  quizResults = [];

  document.getElementById('viewSetup').classList.add('hidden');
  document.getElementById('viewScorecard').classList.add('hidden');
  document.getElementById('viewArena').classList.remove('hidden');

  updateArenaProgress();
  renderArenaGrid();
  focusTargetCard(0);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function updateArenaProgress() {
  const total = activeQuizList.length;
  const completed = quizResults.length;
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0;

  const countEl = document.getElementById('arenaProgressCount');
  const percentEl = document.getElementById('arenaProgressPercent');
  const barEl = document.getElementById('arenaProgressBar');

  if (countEl) countEl.textContent = `${completed} / ${total} chữ`;
  if (percentEl) percentEl.textContent = `${percent}%`;
  if (barEl) barEl.style.width = `${percent}%`;
}

function renderArenaGrid() {
  const grid = document.getElementById('kanaQuizGrid');
  if (!grid) return;

  let html = '';

  activeQuizList.forEach((it, idx) => {
    html += `
      <div id="tileCard_${idx}" 
           class="kana-tile flex flex-col items-center justify-between p-2 sm:p-2.5 h-28 sm:h-32 text-center"
           data-index="${idx}">
        <div class="text-[10px] text-[#68737D] font-mono">#${idx + 1}</div>
        <div class="text-2xl sm:text-3xl font-bold my-auto text-[#0F2545] font-japanese">${it.character}</div>
        <div class="w-full">
          <input type="text" 
                 id="romajiInput_${idx}" 
                 data-index="${idx}"
                 placeholder="..."
                 autocomplete="off"
                 autocorrect="off"
                 autocapitalize="off"
                 spellcheck="false"
                 class="w-full text-center text-xs sm:text-sm font-semibold py-1 px-1 rounded-lg border border-[#ECE8DE] bg-[#FAF8F2] focus:bg-white focus:border-[#006194] focus:outline-none transition">
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;

  // Gắn sự kiện cho các input
  activeQuizList.forEach((_, idx) => {
    const input = document.getElementById(`romajiInput_${idx}`);
    const card = document.getElementById(`tileCard_${idx}`);

    if (input) {
      input.addEventListener('input', (e) => handleRomajiInput(idx, e.target.value.trim().toLowerCase(), false));
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          handleRomajiInput(idx, input.value.trim().toLowerCase(), true);
        }
      });
      input.addEventListener('focus', () => {
        highlightActiveCard(idx);
      });
    }

    if (card) {
      card.addEventListener('click', () => {
        if (input && !input.disabled) input.focus();
      });
    }
  });
}

function highlightActiveCard(idx) {
  document.querySelectorAll('.kana-tile').forEach(c => c.classList.remove('active-target'));
  const target = document.getElementById(`tileCard_${idx}`);
  if (target) target.classList.add('active-target');
}

function focusTargetCard(idx) {
  highlightActiveCard(idx);
  const input = document.getElementById(`romajiInput_${idx}`);
  if (input && !input.disabled) {
    input.focus();
    input.select();
  }
}

function handleRomajiInput(idx, val, isEnterPressed) {
  if (!val) return;

  const item = activeQuizList[idx];
  const isMatch = item.aliases.includes(val);

  const card = document.getElementById(`tileCard_${idx}`);
  const input = document.getElementById(`romajiInput_${idx}`);

  // Nếu gõ trúng hoặc bấm Enter kiểm tra
  if (isMatch) {
    card.classList.remove('tile-wrong', 'shake-card', 'active-target');
    card.classList.add('tile-correct');
    input.value = item.romaji;
    input.disabled = true;

    // Ghi nhận kết quả
    recordResult(idx, item, true, val);
    updateArenaProgress();

    // Tự động chuyển sang thẻ tiếp theo
    findAndFocusNext(idx);
  } else if (isEnterPressed) {
    // Nhấn Enter mà sai
    card.classList.add('tile-wrong', 'shake-card');
    showToast(`Chưa chính xác: âm của 「${item.character}」 là "${item.romaji}"`, 'info');

    setTimeout(() => {
      card.classList.remove('shake-card');
    }, 400);

    recordResult(idx, item, false, val);
    updateArenaProgress();
    findAndFocusNext(idx);
  }
}

function recordResult(idx, item, isCorrect, enteredVal) {
  // Tránh ghi nhận 2 lần cho 1 card
  const existingIdx = quizResults.findIndex(r => r.quizIndex === idx);
  const record = { quizIndex: idx, item, isCorrect, enteredVal };

  if (existingIdx >= 0) {
    quizResults[existingIdx] = record;
  } else {
    quizResults.push(record);
  }

  // Nếu đã làm hết toàn bộ thẻ
  if (quizResults.length === activeQuizList.length) {
    setTimeout(() => {
      finishQuiz();
    }, 500);
  }
}

function findAndFocusNext(currentIdx) {
  const nextIdx = activeQuizList.findIndex((_, i) => {
    const input = document.getElementById(`romajiInput_${i}`);
    return input && !input.disabled && i > currentIdx;
  });

  if (nextIdx !== -1) {
    focusTargetCard(nextIdx);
  } else {
    // Tìm từ đầu xem còn ô nào chưa làm
    const remainingIdx = activeQuizList.findIndex((_, i) => {
      const input = document.getElementById(`romajiInput_${i}`);
      return input && !input.disabled;
    });

    if (remainingIdx !== -1) {
      focusTargetCard(remainingIdx);
    }
  }
}

// =========================================================================
// 5. BẢNG TỔNG KẾT & PHÂN TÍCH ĐIỂM (SCORECARD)
// =========================================================================

function finishQuiz() {
  // Thu thập các thẻ chưa hoàn thành coi như sai
  activeQuizList.forEach((it, idx) => {
    if (!quizResults.some(r => r.quizIndex === idx)) {
      quizResults.push({ quizIndex: idx, item: it, isCorrect: false, enteredVal: '' });
    }
  });

  const total = activeQuizList.length;
  const correctCount = quizResults.filter(r => r.isCorrect).length;
  const accuracy = total > 0 ? Math.round((correctCount / total) * 100) : 0;

  document.getElementById('viewArena').classList.add('hidden');
  document.getElementById('viewScorecard').classList.remove('hidden');

  // Cập nhật thông số
  const accText = document.getElementById('scoreAccuracyText');
  const countText = document.getElementById('scoreCountText');
  const pill = document.getElementById('scoreBadgePill');
  const comment = document.getElementById('scoreSenseiComment');

  if (accText) accText.textContent = `${accuracy}%`;
  if (countText) countText.textContent = `${correctCount} / ${total} chữ`;

  if (pill) {
    if (accuracy === 100) {
      pill.textContent = 'Hoàn hảo tuyệt đối';
      pill.className = 'inline-flex items-center px-3 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200';
    } else if (accuracy >= 80) {
      pill.textContent = 'Xuất sắc';
      pill.className = 'inline-flex items-center px-3 py-0.5 rounded-full text-xs font-semibold bg-sky-50 text-sky-700 border border-sky-200';
    } else if (accuracy >= 50) {
      pill.textContent = 'Khá tốt';
      pill.className = 'inline-flex items-center px-3 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200';
    } else {
      pill.textContent = 'Cần ôn luyện thêm';
      pill.className = 'inline-flex items-center px-3 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200';
    }
  }

  if (comment) {
    if (accuracy === 100) {
      comment.textContent = 'Bạn gõ chữ rất chuẩn xác và phản xạ tuyệt vời. Tiếp tục duy trì phong độ nhé!';
    } else if (accuracy >= 80) {
      comment.textContent = 'Kết quả rất ấn tượng! Chỉ một vài âm gõ nhầm nhẹ, bạn hoàn toàn có thể đạt 100% ở lượt tiếp theo.';
    } else {
      comment.textContent = 'Đừng nản lòng nhé! Luyện gõ thêm vài lần sẽ giúp các ký tự Kana in sâu vào phản xạ của bạn.';
    }
  }

  renderRowBreakdown();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function renderRowBreakdown() {
  const container = document.getElementById('rowBreakdownContainer');
  if (!container) return;

  const rowMap = new Map();

  quizResults.forEach(res => {
    const it = res.item;
    if (!rowMap.has(it.rowKey)) {
      rowMap.set(it.rowKey, {
        rowKey: it.rowKey,
        rowLabel: it.rowLabel,
        results: []
      });
    }
    rowMap.get(it.rowKey).results.push(res);
  });

  let html = '';

  rowMap.forEach(group => {
    const totalInRow = group.results.length;
    const correctInRow = group.results.filter(r => r.isCorrect).length;
    const isPerfect = correctInRow === totalInRow;

    html += `
      <div class="p-4 bg-white flex flex-col md:flex-row md:items-center justify-between gap-3 border border-[#ECE8DE] rounded-2xl shadow-xs">
        <div class="space-y-1.5 flex-1">
          <div class="flex items-center gap-2">
            <span class="font-bold text-sm text-[#0F2545]">${group.rowLabel}</span>
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${
              isPerfect ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-rose-50 text-rose-700 border border-rose-200'
            }">
              ${correctInRow}/${totalInRow} chữ (${Math.round((correctInRow / totalInRow) * 100)}%)
            </span>
          </div>

          <div class="flex flex-wrap gap-2 pt-1">
    `;

    group.results.forEach(r => {
      const it = r.item;
      const isOk = r.isCorrect;
      html += `
        <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-xl border text-xs font-semibold ${
          isOk ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-rose-50 border-rose-200 text-rose-700'
        }">
          <span>${isOk ? '◯' : '✕'}</span>
          <span class="font-bold font-japanese">${it.character}</span>
          <span class="font-mono text-[10px] opacity-75">(${it.romaji})</span>
        </div>
      `;
    });

    html += `
          </div>
        </div>

        <div class="shrink-0 flex items-center">
          <button type="button" 
                  data-action="retry-row" data-row="${group.rowKey}"
                  class="text-xs py-1.5 px-3 font-semibold flex items-center gap-1.5 border border-[#ECE8DE] rounded-xl bg-white hover:bg-[#FAF8F2] text-[#0F2545] transition cursor-pointer">
            <span class="material-symbols-outlined text-[15px]">refresh</span>
            <span>Ôn lại hàng này</span>
          </button>
        </div>
      </div>
    `;
  });

  container.innerHTML = html;

  container.querySelectorAll('[data-action="retry-row"]').forEach(btn => {
    btn.addEventListener('click', () => {
      const items = getKanaItems(currentScript).filter(it => it.rowKey === btn.dataset.row);
      if (items.length > 0) {
        startQuiz(items);
      }
    });
  });
}

function backToSetup() {
  document.getElementById('viewArena').classList.add('hidden');
  document.getElementById('viewScorecard').classList.add('hidden');
  document.getElementById('viewSetup').classList.remove('hidden');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// =========================================================================
// 6. KHỞI TẠO DOM & GẮN SỰ KIỆN TOÀN CỤC (EVENT LISTENERS)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  // Tabs
  document.getElementById('tabHiragana')?.addEventListener('click', () => setScriptType('hiragana'));
  document.getElementById('tabKatakana')?.addEventListener('click', () => setScriptType('katakana'));

  // Quick Groups
  document.getElementById('btnGroupMain')?.addEventListener('click', () => toggleKanaGroup('main'));
  document.getElementById('btnGroupDakuten')?.addEventListener('click', () => toggleKanaGroup('dakuten'));
  document.getElementById('btnGroupCombination')?.addEventListener('click', () => toggleKanaGroup('combination'));
  document.getElementById('btnGroupAll')?.addEventListener('click', selectAllGroups);
  document.getElementById('btnGroupClear')?.addEventListener('click', clearAllGroups);

  // CTA
  document.getElementById('btnStartQuiz')?.addEventListener('click', () => startQuiz());
  document.getElementById('btnBackToSetup')?.addEventListener('click', backToSetup);
  document.getElementById('btnFinishQuiz')?.addEventListener('click', finishQuiz);
  document.getElementById('btnRetryQuiz')?.addEventListener('click', () => startQuiz());
  document.getElementById('btnScorecardNewSetup')?.addEventListener('click', backToSetup);

  // Khởi tạo bảng ban đầu
  selectedGroups.clear();
  selectedGroups.add('main');
  recalculateSelectedKana();
});
