/**
 * KIZUNA KANA QUIZ - ĐẤU TRƯỜNG GÕ KANA (HIRAGANA & KATAKANA)
 * Phong cách Tofugu Kana Quiz kết hợp Design System Retro Nhaitopik
 * Hỗ trợ chọn nhanh Multi-Select độc lập và mở rộng cho cả Guest & Learner.
 */

(function () {
    'use strict';

    // =========================================================================
    // 1. BẢNG MÃ DỮ LIỆU KANA TOÀN DIỆN (DATA DICTIONARY MAPPING)
    // Bao gồm 46 Kana chính, 25 Biến âm (Dakuten/Handakuten), 33 Âm ghép (Youon)
    // =========================================================================
    const KANA_RAW_DEFINITIONS = [
        // --- 1. KANA CHÍNH (MAIN KANA - 46 KÝ TỰ) ---
        // Hàng A
        { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'あ', k: 'ア', r: 'a', aliases: ['a'] },
        { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'い', k: 'イ', r: 'i', aliases: ['i'] },
        { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'う', k: 'ウ', r: 'u', aliases: ['u'] },
        { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'え', k: 'エ', r: 'e', aliases: ['e'] },
        { rowKey: 'a', rowLabel: 'Hàng a', cat: 'main', h: 'お', k: 'オ', r: 'o', aliases: ['o'] },

        // Hàng KA
        { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'か', k: 'カ', r: 'ka', aliases: ['ka'] },
        { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'き', k: 'キ', r: 'ki', aliases: ['ki'] },
        { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'く', k: 'ク', r: 'ku', aliases: ['ku'] },
        { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'け', k: 'ケ', r: 'ke', aliases: ['ke'] },
        { rowKey: 'ka', rowLabel: 'Hàng ka', cat: 'main', h: 'こ', k: 'コ', r: 'ko', aliases: ['ko'] },

        // Hàng SA
        { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'さ', k: 'サ', r: 'sa', aliases: ['sa'] },
        { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'し', k: 'シ', r: 'shi', aliases: ['shi', 'si'] },
        { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'す', k: 'ス', r: 'su', aliases: ['su'] },
        { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'せ', k: 'セ', r: 'se', aliases: ['se'] },
        { rowKey: 'sa', rowLabel: 'Hàng sa', cat: 'main', h: 'そ', k: 'ソ', r: 'so', aliases: ['so'] },

        // Hàng TA
        { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'た', k: 'タ', r: 'ta', aliases: ['ta'] },
        { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'ち', k: 'チ', r: 'chi', aliases: ['chi', 'ti'] },
        { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'つ', k: 'ツ', r: 'tsu', aliases: ['tsu', 'tu'] },
        { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'て', k: 'テ', r: 'te', aliases: ['te'] },
        { rowKey: 'ta', rowLabel: 'Hàng ta', cat: 'main', h: 'と', k: 'ト', r: 'to', aliases: ['to'] },

        // Hàng NA
        { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'な', k: 'ナ', r: 'na', aliases: ['na'] },
        { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'に', k: 'ニ', r: 'ni', aliases: ['ni'] },
        { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'ぬ', k: 'ヌ', r: 'nu', aliases: ['nu'] },
        { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'ね', k: 'ネ', r: 'ne', aliases: ['ne'] },
        { rowKey: 'na', rowLabel: 'Hàng na', cat: 'main', h: 'の', k: 'ノ', r: 'no', aliases: ['no'] },

        // Hàng HA
        { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'は', k: 'ハ', r: 'ha', aliases: ['ha'] },
        { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'ひ', k: 'ヒ', r: 'hi', aliases: ['hi'] },
        { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'ふ', k: 'フ', r: 'fu', aliases: ['fu', 'hu'] },
        { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'へ', k: 'ヘ', r: 'he', aliases: ['he'] },
        { rowKey: 'ha', rowLabel: 'Hàng ha', cat: 'main', h: 'ほ', k: 'ホ', r: 'ho', aliases: ['ho'] },

        // Hàng MA
        { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'ま', k: 'マ', r: 'ma', aliases: ['ma'] },
        { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'み', k: 'ミ', r: 'mi', aliases: ['mi'] },
        { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'む', k: 'ム', r: 'mu', aliases: ['mu'] },
        { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'め', k: 'メ', r: 'me', aliases: ['me'] },
        { rowKey: 'ma', rowLabel: 'Hàng ma', cat: 'main', h: 'も', k: 'モ', r: 'mo', aliases: ['mo'] },

        // Hàng YA
        { rowKey: 'ya', rowLabel: 'Hàng ya', cat: 'main', h: 'や', k: 'ヤ', r: 'ya', aliases: ['ya'] },
        { rowKey: 'ya', rowLabel: 'Hàng ya', cat: 'main', h: 'ゆ', k: 'ユ', r: 'yu', aliases: ['yu'] },
        { rowKey: 'ya', rowLabel: 'Hàng ya', cat: 'main', h: 'よ', k: 'ヨ', r: 'yo', aliases: ['yo'] },

        // Hàng RA
        { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'ら', k: 'ラ', r: 'ra', aliases: ['ra'] },
        { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'り', k: 'リ', r: 'ri', aliases: ['ri'] },
        { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'る', k: 'ル', r: 'ru', aliases: ['ru'] },
        { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'れ', k: 'レ', r: 're', aliases: ['re'] },
        { rowKey: 'ra', rowLabel: 'Hàng ra', cat: 'main', h: 'ろ', k: 'ロ', r: 'ro', aliases: ['ro'] },

        // Hàng WA
        { rowKey: 'wa', rowLabel: 'Hàng wa', cat: 'main', h: 'わ', k: 'ワ', r: 'wa', aliases: ['wa'] },
        { rowKey: 'wa', rowLabel: 'Hàng wa', cat: 'main', h: 'を', k: 'ヲ', r: 'wo', aliases: ['wo', 'o'] },
        { rowKey: 'wa', rowLabel: 'Hàng wa', cat: 'main', h: 'ん', k: 'ン', r: 'n', aliases: ['n', 'nn'] },

        // --- 2. BIẾN ÂM DAKUTEN & HANDAKUTEN (25 KÝ TỰ) ---
        // Hàng GA
        { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'が', k: 'ガ', r: 'ga', aliases: ['ga'] },
        { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'ぎ', k: 'ギ', r: 'gi', aliases: ['gi'] },
        { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'ぐ', k: 'グ', r: 'gu', aliases: ['gu'] },
        { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'げ', k: 'ゲ', r: 'ge', aliases: ['ge'] },
        { rowKey: 'ga', rowLabel: 'Hàng ga (biến âm)', cat: 'dakuten', h: 'ご', k: 'ゴ', r: 'go', aliases: ['go'] },

        // Hàng ZA
        { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ざ', k: 'ザ', r: 'za', aliases: ['za'] },
        { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'じ', k: 'ジ', r: 'ji', aliases: ['ji', 'zi'] },
        { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ず', k: 'ズ', r: 'zu', aliases: ['zu'] },
        { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ぜ', k: 'ゼ', r: 'ze', aliases: ['ze'] },
        { rowKey: 'za', rowLabel: 'Hàng za (biến âm)', cat: 'dakuten', h: 'ぞ', k: 'ゾ', r: 'zo', aliases: ['zo'] },

        // Hàng DA
        { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'だ', k: 'ダ', r: 'da', aliases: ['da'] },
        { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'ぢ', k: 'ヂ', r: 'ji', aliases: ['ji', 'di'] },
        { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'づ', k: 'ヅ', r: 'zu', aliases: ['zu', 'du'] },
        { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'で', k: 'デ', r: 'de', aliases: ['de'] },
        { rowKey: 'da', rowLabel: 'Hàng da (biến âm)', cat: 'dakuten', h: 'ど', k: 'ド', r: 'do', aliases: ['do'] },

        // Hàng BA
        { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'ば', k: 'バ', r: 'ba', aliases: ['ba'] },
        { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'び', k: 'ビ', r: 'bi', aliases: ['bi'] },
        { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'ぶ', k: 'ブ', r: 'bu', aliases: ['bu'] },
        { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'べ', k: 'ベ', r: 'be', aliases: ['be'] },
        { rowKey: 'ba', rowLabel: 'Hàng ba (biến âm)', cat: 'dakuten', h: 'ぼ', k: 'ボ', r: 'bo', aliases: ['bo'] },

        // Hàng PA (Handakuten)
        { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぱ', k: 'パ', r: 'pa', aliases: ['pa'] },
        { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぴ', k: 'ピ', r: 'pi', aliases: ['pi'] },
        { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぷ', k: 'プ', r: 'pu', aliases: ['pu'] },
        { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぺ', k: 'ペ', r: 'pe', aliases: ['pe'] },
        { rowKey: 'pa', rowLabel: 'Hàng pa (bán biến âm)', cat: 'dakuten', h: 'ぽ', k: 'ポ', r: 'po', aliases: ['po'] },

        // --- 3. ÂM GHÉP YOUON (33 KÝ TỰ) ---
        // Hàng KYA
        { rowKey: 'kya', rowLabel: 'Hàng kya (âm ghép)', cat: 'youon', h: 'きゃ', k: 'キャ', r: 'kya', aliases: ['kya'] },
        { rowKey: 'kya', rowLabel: 'Hàng kya (âm ghép)', cat: 'youon', h: 'きゅ', k: 'キュ', r: 'kyu', aliases: ['kyu'] },
        { rowKey: 'kya', rowLabel: 'Hàng kya (âm ghép)', cat: 'youon', h: 'きょ', k: 'キョ', r: 'kyo', aliases: ['kyo'] },

        // Hàng SHA
        { rowKey: 'sha', rowLabel: 'Hàng sha (âm ghép)', cat: 'youon', h: 'しゃ', k: 'シャ', r: 'sha', aliases: ['sha', 'sya'] },
        { rowKey: 'sha', rowLabel: 'Hàng sha (âm ghép)', cat: 'youon', h: 'しゅ', k: 'シュ', r: 'shu', aliases: ['shu', 'syu'] },
        { rowKey: 'sha', rowLabel: 'Hàng sha (âm ghép)', cat: 'youon', h: 'しょ', k: 'ショ', r: 'sho', aliases: ['sho', 'syo'] },

        // Hàng CHA
        { rowKey: 'cha', rowLabel: 'Hàng cha (âm ghép)', cat: 'youon', h: 'ちゃ', k: 'チャ', r: 'cha', aliases: ['cha', 'tya'] },
        { rowKey: 'cha', rowLabel: 'Hàng cha (âm ghép)', cat: 'youon', h: 'ちゅ', k: 'チュ', r: 'chu', aliases: ['chu', 'tyu'] },
        { rowKey: 'cha', rowLabel: 'Hàng cha (âm ghép)', cat: 'youon', h: 'ちょ', k: 'チョ', r: 'cho', aliases: ['cho', 'tyo'] },

        // Hàng NYA
        { rowKey: 'nya', rowLabel: 'Hàng nya (âm ghép)', cat: 'youon', h: 'にゃ', k: 'ニャ', r: 'nya', aliases: ['nya'] },
        { rowKey: 'nya', rowLabel: 'Hàng nya (âm ghép)', cat: 'youon', h: 'にゅ', k: 'ニュ', r: 'nyu', aliases: ['nyu'] },
        { rowKey: 'nya', rowLabel: 'Hàng nya (âm ghép)', cat: 'youon', h: 'にょ', k: 'ニョ', r: 'nyo', aliases: ['nyo'] },

        // Hàng HYA
        { rowKey: 'hya', rowLabel: 'Hàng hya (âm ghép)', cat: 'youon', h: 'ひゃ', k: 'ヒャ', r: 'hya', aliases: ['hya'] },
        { rowKey: 'hya', rowLabel: 'Hàng hya (âm ghép)', cat: 'youon', h: 'ひゅ', k: 'ヒュ', r: 'hyu', aliases: ['hyu'] },
        { rowKey: 'hya', rowLabel: 'Hàng hya (âm ghép)', cat: 'youon', h: 'ひょ', k: 'ヒョ', r: 'hyo', aliases: ['hyo'] },

        // Hàng MYA
        { rowKey: 'mya', rowLabel: 'Hàng mya (âm ghép)', cat: 'youon', h: 'みゃ', k: 'ミャ', r: 'mya', aliases: ['mya'] },
        { rowKey: 'mya', rowLabel: 'Hàng mya (âm ghép)', cat: 'youon', h: 'みゅ', k: 'ミュ', r: 'myu', aliases: ['myu'] },
        { rowKey: 'mya', rowLabel: 'Hàng mya (âm ghép)', cat: 'youon', h: 'みょ', k: 'ミョ', r: 'myo', aliases: ['myo'] },

        // Hàng RYA
        { rowKey: 'rya', rowLabel: 'Hàng rya (âm ghép)', cat: 'youon', h: 'りゃ', k: 'リャ', r: 'rya', aliases: ['rya'] },
        { rowKey: 'rya', rowLabel: 'Hàng rya (âm ghép)', cat: 'youon', h: 'りゅ', k: 'リュ', r: 'ryu', aliases: ['ryu'] },
        { rowKey: 'rya', rowLabel: 'Hàng rya (âm ghép)', cat: 'youon', h: 'りょ', k: 'リョ', r: 'ryo', aliases: ['ryo'] },

        // Hàng GYA
        { rowKey: 'gya', rowLabel: 'Hàng gya (âm ghép)', cat: 'youon', h: 'ぎゃ', k: 'ギャ', r: 'gya', aliases: ['gya'] },
        { rowKey: 'gya', rowLabel: 'Hàng gya (âm ghép)', cat: 'youon', h: 'ぎゅ', k: 'ギュ', r: 'gyu', aliases: ['gyu'] },
        { rowKey: 'gya', rowLabel: 'Hàng gya (âm ghép)', cat: 'youon', h: 'ぎょ', k: 'ギョ', r: 'gyo', aliases: ['gyo'] },

        // Hàng JA
        { rowKey: 'ja', rowLabel: 'Hàng ja (âm ghép)', cat: 'youon', h: 'じゃ', k: 'ジャ', r: 'ja', aliases: ['ja', 'zya', 'jya'] },
        { rowKey: 'ja', rowLabel: 'Hàng ja (âm ghép)', cat: 'youon', h: 'じゅ', k: 'ジュ', r: 'ju', aliases: ['ju', 'zyu', 'jyu'] },
        { rowKey: 'ja', rowLabel: 'Hàng ja (âm ghép)', cat: 'youon', h: 'じょ', k: 'ジョ', r: 'jo', aliases: ['jo', 'zyo', 'jyo'] },

        // Hàng BYA
        { rowKey: 'bya', rowLabel: 'Hàng bya (âm ghép)', cat: 'youon', h: 'びゃ', k: 'ビャ', r: 'bya', aliases: ['bya'] },
        { rowKey: 'bya', rowLabel: 'Hàng bya (âm ghép)', cat: 'youon', h: 'びゅ', k: 'ビュ', r: 'byu', aliases: ['byu'] },
        { rowKey: 'bya', rowLabel: 'Hàng bya (âm ghép)', cat: 'youon', h: 'びょ', k: 'ビョ', r: 'byo', aliases: ['byo'] },

        // Hàng PYA
        { rowKey: 'pya', rowLabel: 'Hàng pya (âm ghép)', cat: 'youon', h: 'ぴゃ', k: 'ピャ', r: 'pya', aliases: ['pya'] },
        { rowKey: 'pya', rowLabel: 'Hàng pya (âm ghép)', cat: 'youon', h: 'ぴゅ', k: 'ピュ', r: 'pyu', aliases: ['pyu'] },
        { rowKey: 'pya', rowLabel: 'Hàng pya (âm ghép)', cat: 'youon', h: 'ぴょ', k: 'ピョ', r: 'pyo', aliases: ['pyo'] }
    ];

    // =========================================================================
    // 2. KHỞI TẠO BỘ DỮ LIỆU ĐƯỢC CHUẨN HÓA THEO SCRIPT TYPE
    // =========================================================================
    function getKanaItems(scriptType) {
        return KANA_RAW_DEFINITIONS.map((def, idx) => {
            const char = scriptType === 'katakana' ? def.k : def.h;
            return {
                id: `${scriptType}_${idx}_${def.r}`,
                scriptType: scriptType,
                category: def.cat,
                rowKey: def.rowKey,
                rowLabel: def.rowLabel,
                character: char,
                romaji: def.r,
                validRomaji: def.aliases.map(a => a.toLowerCase().trim())
            };
        });
    }

    // =========================================================================
    // 3. TRẠNG THÁI TOÀN CỤC CỦA QUIZ (APPLICATION STATE)
    // =========================================================================
    let currentScript = 'hiragana'; // 'hiragana' hoặc 'katakana'
    
    // Cấu trúc Set quản lý Multi-select nhóm nội dung: 'main', 'dakuten', 'youon'
    const selectedGroups = new Set(['main']);
    
    // Tập hợp ID các ký tự cụ thể đang được chọn
    let selectedCharIds = new Set();
    
    let quizDeck = [];
    let currentDeckIndex = 0;
    let correctCount = 0;
    let quizResults = [];
    let hasCompleted = false;

    // =========================================================================
    // 4. THUẬT TOÁN XÁO TRỘN NGẪU NHIÊN FISHER-YATES SHUFFLE
    // =========================================================================
    function shuffleArray(array) {
        const copy = [...array];
        for (let i = copy.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [copy[i], copy[j]] = [copy[j], copy[i]];
        }
        return copy;
    }

    // =========================================================================
    // 5. GIAI ĐOẠN 1: BỘ CHỌN NHANH MULTI-SELECT & MA TRẬN KÝ TỰ (SETUP PHASE)
    // =========================================================================

    /**
     * Bật/Tắt nhóm chữ khi click theo cơ chế Multi-select / Toggle độc lập
     * @param {string} groupKey 'main', 'dakuten', hoặc 'combination' / 'youon'
     */
    window.toggleKanaGroup = function (groupKey) {
        const key = (groupKey === 'combination') ? 'youon' : groupKey;
        const items = getKanaItems(currentScript).filter(it => it.category === key);

        if (selectedGroups.has(key)) {
            // TẮT: Hủy chọn nhóm và bớt các chữ thuộc nhóm này
            selectedGroups.delete(key);
            items.forEach(it => selectedCharIds.delete(it.id));
        } else {
            // BẬT: Thêm nhóm và cộng dồn các chữ thuộc nhóm này
            selectedGroups.add(key);
            items.forEach(it => selectedCharIds.add(it.id));
        }

        renderButtonStates();
        renderSelectionMatrix();
        updateSelectionCounter();
    };

    /**
     * Cập nhật CSS trạng thái của từng nút bấm theo chuẩn Design System Retro Nhaitopik (Slate-900 #0F172A)
     */
    function renderButtonStates() {
        const groupMapping = {
            'main': { id: 'btnGroupMain', label: 'Kana Chính (46 chữ)' },
            'dakuten': { id: 'btnGroupDakuten', label: 'Biến Âm (25 chữ)' },
            'youon': { id: 'btnGroupCombination', label: 'Âm Ghép (33 chữ)' }
        };

        for (const [key, config] of Object.entries(groupMapping)) {
            const btn = document.getElementById(config.id);
            if (!btn) continue;

            if (selectedGroups.has(key)) {
                btn.innerHTML = `<span>✓</span> <span>${config.label}</span>`;
                btn.className = "btn-primary px-2 py-0.5 text-xs font-bold text-white bg-[#E0399A] hover:bg-[#D02B89] border border-[#0F172A] rounded-md shadow-[1px_1px_0px_0px_#0F172A] transition-all flex items-center gap-1";
            } else {
                btn.innerHTML = `<span>${config.label}</span>`;
                btn.className = "btn-secondary px-2 py-0.5 text-xs font-semibold text-[#475569] bg-white border border-[#CBD5E1] rounded-md hover:bg-slate-50 transition-all flex items-center gap-1 shadow-sm";
            }
        }
    }

    /**
     * Tác vụ hàng loạt: Chọn toàn bộ bảng (bật sáng cả 3 nhóm)
     */
    window.selectAllGroups = function () {
        selectedGroups.add('main');
        selectedGroups.add('dakuten');
        selectedGroups.add('youon');

        const items = getKanaItems(currentScript);
        items.forEach(it => selectedCharIds.add(it.id));

        renderButtonStates();
        renderSelectionMatrix();
        updateSelectionCounter();
    };

    /**
     * Tác vụ hàng loạt: Bỏ chọn tất cả (tắt cả 3 nhóm, đưa về 0)
     */
    window.clearAllGroups = function () {
        selectedGroups.clear();
        selectedCharIds.clear();

        renderButtonStates();
        renderSelectionMatrix();
        updateSelectionCounter();
    };

    /**
     * Chuyển đổi giữa 2 tab Hiragana và Katakana
     */
    window.setScriptType = function (type) {
        if (currentScript === type) return;
        currentScript = type;

        const tabHira = document.getElementById('tabHiragana');
        const tabKata = document.getElementById('tabKatakana');
        if (type === 'hiragana') {
            tabHira.className = 'tab-script-hiragana active flex-1 py-1.5 px-3 font-sans font-bold text-xs flex items-center justify-center gap-1.5 border border-[#0F172A] rounded-lg bg-[#E0399A] text-white shadow-retro transition';
            tabKata.className = 'tab-script-katakana flex-1 py-1.5 px-3 font-sans font-bold text-xs flex items-center justify-center gap-1.5 border border-[#CBD5E1] rounded-lg bg-white text-[#475569] shadow-sm hover:bg-[#EFF6FF] hover:text-[#1D4ED8] transition';
        } else {
            tabKata.className = 'tab-script-katakana active flex-1 py-1.5 px-3 font-sans font-bold text-xs flex items-center justify-center gap-1.5 border border-[#0F172A] rounded-lg bg-[#E0399A] text-white shadow-retro transition';
            tabHira.className = 'tab-script-hiragana flex-1 py-1.5 px-3 font-sans font-bold text-xs flex items-center justify-center gap-1.5 border border-[#CBD5E1] rounded-lg bg-white text-[#475569] shadow-sm hover:bg-[#FDF2F8] hover:text-[#BE185D] transition';
        }

        recalculateSelectedKana();
    };

    /**
     * Tính toán lại danh sách ID ký tự dựa trên selectedGroups
     */
    function recalculateSelectedKana() {
        selectedCharIds.clear();
        const items = getKanaItems(currentScript);
        items.forEach(it => {
            if (selectedGroups.has(it.category)) {
                selectedCharIds.add(it.id);
            }
        });
        renderButtonStates();
        renderSelectionMatrix();
        updateSelectionCounter();
    }

    /**
     * Bật/Tắt cả một hàng
     */
    window.toggleRow = function (rowKey, isChecked) {
        const items = getKanaItems(currentScript).filter(it => it.rowKey === rowKey);
        items.forEach(it => {
            if (isChecked) {
                selectedCharIds.add(it.id);
            } else {
                selectedCharIds.delete(it.id);
            }
        });
        syncSelectedGroupsWithChars();
        renderSelectionMatrix();
        updateSelectionCounter();
    };

    /**
     * Bật/Tắt một ký tự đơn lẻ
     */
    window.toggleChar = function (charId) {
        if (selectedCharIds.has(charId)) {
            selectedCharIds.delete(charId);
        } else {
            selectedCharIds.add(charId);
        }
        syncSelectedGroupsWithChars();
        renderSelectionMatrix();
        updateSelectionCounter();
    };

    /**
     * Đồng bộ trạng thái các nút nhóm nếu người dùng tick lẻ trong ma trận
     */
    function syncSelectedGroupsWithChars() {
        const items = getKanaItems(currentScript);
        ['main', 'dakuten', 'youon'].forEach(cat => {
            const catItems = items.filter(it => it.category === cat);
            const allSelected = catItems.length > 0 && catItems.every(it => selectedCharIds.has(it.id));
            if (allSelected) {
                selectedGroups.add(cat);
            } else {
                selectedGroups.delete(cat);
            }
        });
        renderButtonStates();
    }

    /**
     * Cập nhật số lượng ký tự đã chọn và trạng thái nút Bắt Đầu
     */
    function updateSelectionCounter() {
        const count = selectedCharIds.size;
        const countEl = document.getElementById('selectedKanaCount');
        const btnStart = document.getElementById('btnStartQuiz');
        const warnEl = document.getElementById('selectionWarning');

        if (countEl) countEl.textContent = count;

        if (btnStart) {
            if (count > 0) {
                btnStart.disabled = false;
                btnStart.classList.remove('opacity-50', 'cursor-not-allowed');
                if (warnEl) warnEl.classList.add('hidden');
            } else {
                btnStart.disabled = true;
                btnStart.classList.add('opacity-50', 'cursor-not-allowed');
                if (warnEl) warnEl.classList.remove('hidden');
            }
        }
    }

    /**
     * Render lưới chọn chi tiết theo từng danh mục và từng hàng
     */
    function renderSelectionMatrix() {
        const container = document.getElementById('kanaSelectionContainer');
        if (!container) return;

        const items = getKanaItems(currentScript);

        const categories = [
            { key: 'main', title: '1. Kana Chính (46 chữ căn bản)', desc: 'Nền tảng quan trọng nhất của tiếng Nhật' },
            { key: 'dakuten', title: '2. Biến Âm Dakuten & Handakuten (25 chữ)', desc: 'Thêm dấu hai phẩy (tenten) hoặc dấu tròn (maru)' },
            { key: 'youon', title: '3. Âm Ghép Youon (33 chữ)', desc: 'Ghép với các âm nhỏ ya, yu, yo' }
        ];

        let html = '';

        categories.forEach(cat => {
            const catItems = items.filter(it => it.category === cat.key);
            const rowMap = new Map();
            catItems.forEach(it => {
                if (!rowMap.has(it.rowKey)) {
                    rowMap.set(it.rowKey, { label: it.rowLabel, items: [] });
                }
                rowMap.get(it.rowKey).items.push(it);
            });

            html += `
                <div class="card-nhai p-2 sm:p-2.5 bg-white space-y-1.5">
                    <div class="flex items-center justify-between border-b border-[#0F172A]/20 pb-1.5">
                        <div>
                            <h3 class="font-itim font-bold text-sm sm:text-base text-[#0F172A] flex items-center gap-1.5">
                                <span>🌸</span>
                                <span>${cat.title}</span>
                            </h3>
                            <p class="text-[11px] font-meta text-slate-500 mt-0.5">${cat.desc}</p>
                        </div>
                    </div>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-2 pt-0.5">
            `;

            rowMap.forEach((rowData, rKey) => {
                const allSelected = rowData.items.every(it => selectedCharIds.has(it.id));

                html += `
                    <div class="p-2 rounded-[5px] border border-[#0F172A]/70 bg-[#FAF9F6] flex flex-col justify-between space-y-1.5">
                        <div class="flex items-center justify-between">
                            <label class="flex items-center gap-1.5 cursor-pointer select-none">
                                <input type="checkbox" 
                                       class="checkbox-nhai rounded-[3px] accent-[#15803D]" 
                                       ${allSelected ? 'checked' : ''} 
                                       onchange="toggleRow('${rKey}', this.checked)">
                                <span class="font-itim font-bold text-xs sm:text-sm text-[#0F172A]">${rowData.label}</span>
                            </label>
                            <span class="text-[11px] font-meta text-slate-500">${rowData.items.length} chữ</span>
                        </div>
                        <div class="flex flex-wrap gap-1">
                `;

                rowData.items.forEach(it => {
                    const isChecked = selectedCharIds.has(it.id);
                    html += `
                        <button type="button" 
                                onclick="toggleChar('${it.id}')"
                                class="btn-nhai px-2 py-0.5 text-xs font-bold font-['Segoe_UI'] rounded-[4px] border border-[#0F172A] transition-all ${
                                    isChecked 
                                        ? 'bg-[#E0399A] text-white shadow-[1px_1px_0px_0px_#0F172A]' 
                                        : 'bg-white text-[#334155] hover:bg-slate-100 shadow-[1px_1px_0px_0px_#0F172A]'
                                }"
                                title="Romaji: ${it.romaji}">
                            <span class="text-xs sm:text-sm font-bold ${isChecked ? 'text-white' : 'text-[#0F172A]'}">${it.character}</span>
                            <span class="text-[10px] opacity-75 font-mono ml-0.5">${it.romaji}</span>
                        </button>
                    `;
                });

                html += `
                        </div>
                    </div>
                `;
            });

            html += `
                    </div>
                </div>
            `;
        });

        container.innerHTML = html;
    }

    // =========================================================================
    // 6. GIAI ĐOẠN 2: ĐẤU TRƯỜNG GÕ KANA TRỰC QUAN (LIVE QUIZ ARENA)
    // =========================================================================

    /**
     * Bắt đầu bài thi Quiz
     */
    window.startQuiz = function (customDeck = null) {
        if (customDeck && Array.isArray(customDeck) && customDeck.length > 0) {
            quizDeck = shuffleArray(customDeck);
        } else {
            const allItems = getKanaItems(currentScript);
            const chosen = allItems.filter(it => selectedCharIds.has(it.id));
            if (chosen.length === 0) {
                if (typeof KizunaDialog !== 'undefined') {
                    KizunaDialog.show({
                        icon: '🌸',
                        title: 'Chưa Chọn Ký Tự',
                        content: '<p class="text-xs sm:text-sm text-slate-700 font-medium">Vui lòng chọn ít nhất một ký tự Kana trong bảng để bắt đầu thử tài bạn nhé! ✨</p>',
                        actions: [{ label: 'Chọn Chữ Ngay', variant: 'primary' }]
                    });
                } else {
                    alert('Vui lòng chọn ít nhất một ký tự Kana để bắt đầu thử tài bạn nhé! 🎯');
                }
                return;
            }
            quizDeck = shuffleArray(chosen);
        }

        currentDeckIndex = 0;
        correctCount = 0;
        hasCompleted = false;
        quizResults = quizDeck.map(item => ({
            item: item,
            isCorrect: false,
            attempts: 0,
            userRomaji: ''
        }));

        document.getElementById('viewSetup').classList.add('hidden');
        document.getElementById('viewScorecard').classList.add('hidden');
        document.getElementById('viewArena').classList.remove('hidden');

        window.scrollTo({ top: 0, behavior: 'smooth' });

        renderQuizArena();
        updateArenaProgressBar();

        setTimeout(() => {
            const firstInput = document.getElementById('card-input-0');
            if (firstInput) firstInput.focus();
        }, 120);
    };

    /**
     * Render lưới các thẻ chữ Kana tương tác
     */
    function renderQuizArena() {
        const grid = document.getElementById('kanaQuizGrid');
        if (!grid) return;

        let html = '';

        quizDeck.forEach((it, idx) => {
            html += `
                <div id="card-box-${idx}" 
                     onclick="focusCardInput(${idx})"
                     class="card-nhai card-quiz-item p-1.5 sm:p-2 rounded-[5px] border border-[#0F172A] bg-white shadow-[2px_2px_0px_0px_#0F172A] flex flex-col items-center justify-between text-center transition-all duration-200 cursor-pointer min-h-[80px] sm:min-h-[88px] select-none hover:translate-y-[-1px]">
                    <div class="card-char-display text-2xl sm:text-3xl font-extrabold font-['Segoe_UI'] text-[#0F172A] transition-colors">
                        ${it.character}
                    </div>
                    <div class="w-full mt-1 flex flex-col items-center">
                        <input type="text" 
                               id="card-input-${idx}" 
                               data-idx="${idx}"
                               autocomplete="off" 
                               autocapitalize="off" 
                               spellcheck="false"
                               class="card-input-field w-full max-w-[58px] sm:max-w-[64px] text-center font-mono font-bold text-xs sm:text-sm py-0.5 border-b-2 border-[#0F172A] outline-none bg-transparent transition-all text-[#0F172A]"
                               placeholder="..."
                               onfocus="handleCardFocus(${idx})"
                               onblur="handleCardBlur(${idx})"
                               onkeydown="handleCardKeydown(event, ${idx})"
                               oninput="handleCardInput(event, ${idx})">
                        <div id="card-romaji-label-${idx}" class="hidden text-[10px] sm:text-xs font-mono font-bold text-[#86EFAC] mt-0.5">
                            ${it.romaji}
                        </div>
                    </div>
                </div>
            `;
        });

        grid.innerHTML = html;
    }

    /**
     * Xử lý Focus thẻ chữ: chuyển sang nền vàng dịu #FEF3C7, shadow 3px 3px #0F172A
     */
    window.handleCardFocus = function (idx) {
        const res = quizResults[idx];
        if (res && res.isCorrect) return;
        const cardBox = document.getElementById(`card-box-${idx}`);
        if (cardBox) {
            cardBox.classList.remove('bg-white', 'shadow-[2px_2px_0px_0px_#0F172A]');
            cardBox.classList.add('bg-[#FEF3C7]', 'shadow-[3px_3px_0px_0px_#0F172A]');
        }
    };

    /**
     * Xử lý Blur khỏi thẻ chữ: trả về nền trắng #FFFFFF, shadow 2px 2px #0F172A
     */
    window.handleCardBlur = function (idx) {
        const res = quizResults[idx];
        if (res && res.isCorrect) return;
        const cardBox = document.getElementById(`card-box-${idx}`);
        if (cardBox) {
            cardBox.classList.remove('bg-[#FEF3C7]', 'shadow-[3px_3px_0px_0px_#0F172A]');
            cardBox.classList.add('bg-white', 'shadow-[2px_2px_0px_0px_#0F172A]');
        }
    };

    /**
     * Bấm vào thẻ để focus ô input
     */
    window.focusCardInput = function (idx) {
        const input = document.getElementById(`card-input-${idx}`);
        if (input && !input.disabled) {
            input.focus();
            input.select();
        }
    };

    /**
     * Bắt sự kiện bàn phím khi gõ Romaji (Bấm ENTER để nộp)
     */
    window.handleCardKeydown = function (e, idx) {
        if (e.key === 'Enter') {
            e.preventDefault();
            const input = document.getElementById(`card-input-${idx}`);
            if (input) {
                verifyCardAnswer(idx, input.value);
            }
        }
    };

    /**
     * Hỗ trợ tự động kiểm tra ngay khi gõ đúng Romaji mà không cần bấm Enter
     */
    window.handleCardInput = function (e, idx) {
        const val = e.target.value.trim().toLowerCase();
        const it = quizDeck[idx];
        if (it && it.validRomaji.includes(val)) {
            verifyCardAnswer(idx, val);
        }
    };

    /**
     * Kiểm tra đáp án Romaji
     */
    function verifyCardAnswer(idx, rawInput) {
        const it = quizDeck[idx];
        const res = quizResults[idx];
        if (!it || !res || res.isCorrect) return;

        const val = (rawInput || '').trim().toLowerCase();
        const cardBox = document.getElementById(`card-box-${idx}`);
        const input = document.getElementById(`card-input-${idx}`);
        const romajiLabel = document.getElementById(`card-romaji-label-${idx}`);

        res.attempts++;
        res.userRomaji = val;

        if (it.validRomaji.includes(val)) {
            // === ĐÁP ÁN ĐÚNG ===
            res.isCorrect = true;
            correctCount++;

            if (cardBox) {
                cardBox.classList.remove('bg-white', 'bg-[#FEF3C7]', 'hover:translate-y-[-2px]', 'shadow-[3px_3px_0px_0px_#0F172A]');
                cardBox.classList.add('bg-[#15803D]', 'text-white', 'border-[#0F172A]', 'shadow-[2px_2px_0px_0px_#0F172A]');
                const charEl = cardBox.querySelector('.card-char-display');
                if (charEl) {
                    charEl.classList.remove('text-[#0F172A]', 'text-slate-900');
                    charEl.classList.add('text-white');
                }
            }

            if (input) {
                input.value = val;
                input.disabled = true;
                input.classList.add('hidden');
            }

            if (romajiLabel) {
                romajiLabel.classList.remove('hidden');
            }

            updateArenaProgressBar();

            const nextUnsolvedIndex = findNextUnsolvedCardIndex(idx);
            if (nextUnsolvedIndex !== -1) {
                setTimeout(() => {
                    const nextInput = document.getElementById(`card-input-${nextUnsolvedIndex}`);
                    if (nextInput) {
                        nextInput.focus();
                        nextInput.select();
                        nextInput.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
                    }
                }, 50);
            } else {
                hasCompleted = true;
                setTimeout(() => {
                    finishQuiz();
                }, 400);
            }
        } else {
            // === ĐÁP ÁN SAI (DỊU MẮT: HỒNG PHẤN/ĐỎ ĐẤT DỊU #FEE2E2 & #DC2626) ===
            if (cardBox) {
                cardBox.classList.remove('bg-[#FEF3C7]', 'bg-white');
                cardBox.classList.add('shake-card', 'bg-[#FEE2E2]', 'border-[#DC2626]');
                const charEl = cardBox.querySelector('.card-char-display');
                if (charEl) {
                    charEl.classList.add('text-[#991B1B]');
                }
                setTimeout(() => {
                    cardBox.classList.remove('shake-card', 'bg-[#FEE2E2]', 'border-[#DC2626]');
                    cardBox.classList.add('border-[#0F172A]');
                    if (charEl) {
                        charEl.classList.remove('text-[#991B1B]');
                    }
                    if (input) {
                        input.value = '';
                        input.focus();
                    }
                    window.handleCardFocus(idx);
                }, 400);
            }
        }
    }

    /**
     * Tìm vị trí thẻ chưa giải kế tiếp
     */
    function findNextUnsolvedCardIndex(startIndex) {
        for (let i = startIndex + 1; i < quizDeck.length; i++) {
            if (!quizResults[i].isCorrect) return i;
        }
        for (let i = 0; i <= startIndex; i++) {
            if (!quizResults[i].isCorrect) return i;
        }
        return -1;
    }

    /**
     * Cập nhật thanh tiến độ trong màn hình làm bài
     */
    function updateArenaProgressBar() {
        const total = quizDeck.length;
        const percent = total > 0 ? Math.round((correctCount / total) * 100) : 0;

        const countEl = document.getElementById('arenaProgressCount');
        const barEl = document.getElementById('arenaProgressBar');
        const percentEl = document.getElementById('arenaProgressPercent');

        if (countEl) countEl.textContent = `${correctCount} / ${total} chữ`;
        if (percentEl) percentEl.textContent = `${percent}%`;
        if (barEl) barEl.style.width = `${percent}%`;
    }

    // =========================================================================
    // 7. GIAI ĐOẠN 3: BẢNG TỔNG KẾT & PHÂN TÍCH CHI TIẾT (SCORECARD PHASE)
    // =========================================================================

    /**
     * Kết thúc bài thi, hiển thị bảng điểm và đồng bộ tiến độ học tập nếu đã đăng nhập
     */
    window.finishQuiz = function () {
        const total = quizDeck.length;
        const accuracy = total > 0 ? ((correctCount / total) * 100).toFixed(1) : '0.0';

        document.getElementById('viewSetup').classList.add('hidden');
        document.getElementById('viewArena').classList.add('hidden');
        document.getElementById('viewScorecard').classList.remove('hidden');

        window.scrollTo({ top: 0, behavior: 'smooth' });

        const scoreAccEl = document.getElementById('scoreAccuracyText');
        const scoreCountEl = document.getElementById('scoreCountText');
        const senseiCommentEl = document.getElementById('scoreSenseiComment');
        const scoreBadgeEl = document.getElementById('scoreBadgePill');

        if (scoreAccEl) scoreAccEl.textContent = `${accuracy}%`;
        if (scoreCountEl) scoreCountEl.textContent = `${correctCount} / ${total} chữ`;

        const numAcc = parseFloat(accuracy);
        if (numAcc >= 90) {
            if (senseiCommentEl) senseiCommentEl.textContent = 'Quá đỉnh! Gõ chuẩn như người bản xứ! Tốc độ ngón tay của bạn làm Sensei hoa cả mắt rồi! ✨';
            if (scoreBadgeEl) {
                scoreBadgeEl.textContent = 'Xuất Sắc! 🌟';
                scoreBadgeEl.className = 'badge-nhai bg-[#15803D] text-white text-xs px-2.5 py-1 font-bold';
            }
        } else if (numAcc >= 70) {
            if (senseiCommentEl) senseiCommentEl.textContent = 'Phong độ rất tốt! Bạn đã nắm chắc hầu hết các âm, chỉ vấp vài chữ biến âm xíu xiu thôi. Ôn lại chút là thành cao thủ ngay! 🎯';
            if (scoreBadgeEl) {
                scoreBadgeEl.textContent = 'Đạt Yêu Cầu 🌸';
                scoreBadgeEl.className = 'badge-nhai bg-[#E76F51] text-white text-xs px-2.5 py-1 font-bold';
            }
        } else {
            if (senseiCommentEl) senseiCommentEl.textContent = 'Đừng nản chí nhé! Bảng chữ cái lúc đầu hơi lú xíu. Bấm nút [Ôn Lại Hàng Này] để Sensei cùng bạn luyện tiếp nghen! 💡';
            if (scoreBadgeEl) {
                scoreBadgeEl.textContent = 'Cần Cố Gắng ⚡';
                scoreBadgeEl.className = 'badge-nhai bg-[#DC2626] text-white text-xs px-2.5 py-1 font-bold';
            }
        }

        renderRowBreakdown();

        // Hiển thị Bảng Tổng Kết Tương Tác KizunaDialog
        if (typeof KizunaDialog !== 'undefined') {
            const commentText = senseiCommentEl ? senseiCommentEl.textContent : 'Chúc mừng bạn đã hoàn thành bài thi!';
            KizunaDialog.show({
                icon: numAcc >= 80 ? '🎉' : '🎯',
                title: 'Bảng Tổng Kết Đấu Trường Kana',
                content: `
                    <div class="space-y-3">
                        <div class="flex items-center gap-3 p-3 bg-[#FAF9F6] border-2 border-[#0F172A] rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A]">
                            <span class="font-itim font-extrabold text-3xl sm:text-4xl ${numAcc >= 80 ? 'text-[#15803D]' : 'text-[#0F172A]'} leading-none shrink-0">${accuracy}%</span>
                            <div class="flex-1 min-w-0">
                                <div class="text-xs font-bold text-[#0F172A]">Hoàn thành: ${correctCount}/${total} chữ</div>
                                <div class="text-[11px] text-slate-600 mt-0.5">
                                    ${numAcc >= 90 ? 'Xuất Sắc! Gõ siêu tốc 🌟' : numAcc >= 70 ? 'Phong độ rất tốt! 🌸' : 'Cần rèn luyện thêm chút xíu 💡'}
                                </div>
                            </div>
                        </div>

                        <div class="p-2.5 bg-slate-50 border border-[#CBD5E1] rounded-[5px] space-y-1">
                            <div class="text-[11px] font-bold text-slate-700 flex items-center gap-1.5">
                                <span>🧑‍🏫</span>
                                <span>Nhận xét từ chuyên gia nè!:</span>
                            </div>
                            <p class="text-xs sm:text-sm text-slate-800 leading-relaxed italic font-sans">
                                "${commentText}"
                            </p>
                        </div>
                    </div>
                `,
                actions: [
                    {
                        label: '🔁 Làm Lại',
                        variant: 'secondary',
                        onClick: () => { retryCurrentQuiz(); }
                    },
                    {
                        label: '📊 Xem Chi Tiết Bảng Điểm',
                        variant: 'primary',
                        onClick: () => {}
                    }
                ]
            });
        }

        // Tự động gửi tiến độ về API nếu người dùng đã đăng nhập (Learner)
        try {
            const token = localStorage.getItem('access_token') || localStorage.getItem('kizuna_token');
            const headers = { 'Content-Type': 'application/json' };
            if (token) headers['Authorization'] = 'Bearer ' + token;

            fetch('/api/learner/progress/kana-quiz', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({
                    scriptType: currentScript,
                    totalCount: total,
                    correctCount: correctCount,
                    accuracy: numAcc,
                    categories: Array.from(selectedGroups)
                })
            }).catch(err => console.debug('Ghi nhận tiến độ ngầm:', err));
        } catch(e) {}
    };

    /**
     * Render bảng phân tích chi tiết từng hàng (Row-by-row Breakdown)
     */
    function renderRowBreakdown() {
        const container = document.getElementById('rowBreakdownContainer');
        if (!container) return;

        const rowGroups = new Map();

        quizResults.forEach(res => {
            const it = res.item;
            if (!rowGroups.has(it.rowKey)) {
                rowGroups.set(it.rowKey, {
                    rowKey: it.rowKey,
                    rowLabel: it.rowLabel,
                    category: it.category,
                    results: []
                });
            }
            rowGroups.get(it.rowKey).results.push(res);
        });

        let html = '';

        rowGroups.forEach((group, rKey) => {
            const totalInRow = group.results.length;
            const correctInRow = group.results.filter(r => r.isCorrect).length;
            const isFullRow = correctInRow === totalInRow;

            html += `
                <div class="card-nhai p-2 sm:p-2.5 bg-white flex flex-col md:flex-row md:items-center justify-between gap-2 border border-[#0F172A] shadow-[2px_2px_0px_0px_#0F172A]">
                    <div class="space-y-1 flex-1">
                        <div class="flex items-center gap-1.5">
                            <span class="font-itim font-bold text-xs sm:text-sm text-[#0F172A]">${group.rowLabel}</span>
                            <span class="badge-nhai ${isFullRow ? 'bg-[#15803D]' : 'bg-[#E76F51]'} text-white text-[10px] px-1.5 py-0.5">
                                ${correctInRow}/${totalInRow} chữ (${Math.round((correctInRow / totalInRow) * 100)}%)
                            </span>
                        </div>
                        <div class="flex flex-wrap gap-1.5 pt-0.5">
            `;

            group.results.forEach(r => {
                const it = r.item;
                const isPassed = r.isCorrect;
                html += `
                    <div class="flex items-center gap-1 px-2 py-0.5 rounded-[4px] border ${
                        isPassed 
                            ? 'bg-[#DCFCE7] border-[#16A34A] text-[#166534]' 
                            : 'bg-[#FEE2E2] border-[#DC2626] text-[#991B1B]'
                    } text-xs font-semibold">
                        <span>${isPassed ? '◯' : '✕'}</span>
                        <span class="font-bold font-['Segoe_UI'] text-xs sm:text-sm">${it.character}</span>
                        <span class="font-mono text-[10px] opacity-80">(${it.romaji})</span>
                    </div>
                `;
            });

            html += `
                        </div>
                    </div>
                    <div class="shrink-0 flex items-center">
                        <button type="button" 
                                onclick="retrySingleRow('${rKey}')" 
                                class="btn-nhai text-xs py-1.5 px-2.5 font-itim font-bold flex items-center gap-1 border border-[#0F172A] rounded-[5px] bg-[#FAF9F6] shadow-[1px_1px_0px_0px_#0F172A] active:translate-y-0.5 hover:bg-slate-100 text-[#0F172A]">
                            <span>↪️</span>
                            <span>Ôn Lại Hàng Này</span>
                        </button>
                    </div>
                </div>
            `;
        });

        container.innerHTML = html;
    }

    /**
     * Ôn luyện riêng một hàng cụ thể
     */
    window.retrySingleRow = function (rowKey) {
        const items = getKanaItems(currentScript).filter(it => it.rowKey === rowKey);
        if (items.length > 0) {
            startQuiz(items);
        }
    };

    /**
     * Làm lại bài quiz với chính bộ thẻ vừa chọn
     */
    window.retryCurrentQuiz = function () {
        startQuiz();
    };

    /**
     * Quay lại màn hình thiết lập (Bước 1)
     */
    window.backToSetup = function () {
        document.getElementById('viewArena').classList.add('hidden');
        document.getElementById('viewScorecard').classList.add('hidden');
        document.getElementById('viewSetup').classList.remove('hidden');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    // =========================================================================
    // 8. KHỞI TẠO TỰ ĐỘNG KHI TẢI TRANG (DOM READY)
    // =========================================================================
    document.addEventListener('DOMContentLoaded', () => {
        selectedGroups.clear();
        selectedGroups.add('main');
        recalculateSelectedKana();
    });

})();
