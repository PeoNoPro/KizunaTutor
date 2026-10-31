/**
 * grammarHandbook.js - Điều khiển Cẩm Nang Ngữ Pháp OpenJLPT & Kizuna AI Sensei Đặt Câu
 */
let currentLevel = 'N5';
let currentGrammarRules = [];
let currentPage = 0;
let totalPages = 1;
let currentSearchKeyword = '';
let selectedRuleForAi = null;
let selectedRuleForDeck = null;
let userDecks = [];

document.addEventListener('DOMContentLoaded', () => {
    initGrammarHandbook();
});

function getAuthHeaders() {
    const token = localStorage.getItem('access_token') || localStorage.getItem('kizuna_token');
    const headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

async function initGrammarHandbook() {
    await loadLevelCounts();
    await loadGrammarRules();
    loadUserDecks();
    setupEventListeners();
}

/**
 * 1. Nạp thống kê số lượng mẫu câu theo 5 cấp độ JLPT
 */
async function loadLevelCounts() {
    try {
        const res = await fetch('/api/learner/grammar/levels', { headers: getAuthHeaders() });
        if (res.ok) {
            const counts = await res.json();
            for (const [lvl, count] of Object.entries(counts)) {
                const countBadge = document.getElementById(`countBadge_${lvl}`);
                if (countBadge) countBadge.textContent = count;
            }
        }
    } catch (e) {
        console.warn('Lỗi loadLevelCounts:', e);
    }
}

/**
 * 2. Chọn cấp độ JLPT (N5 -> N1)
 */
function selectLevel(lvl) {
    currentLevel = lvl.toUpperCase();
    currentPage = 0;
    currentSearchKeyword = '';
    const searchInput = document.getElementById('searchGrammarInput');
    if (searchInput) searchInput.value = '';

    // Cập nhật giao diện tabs
    document.querySelectorAll('.grammar-level-tab').forEach(tab => {
        tab.classList.remove('bg-[#E0399A]', 'text-white');
        tab.classList.add('bg-white', 'text-black');
    });
    const activeTab = document.getElementById(`tabBtn_${currentLevel}`);
    if (activeTab) {
        activeTab.classList.remove('bg-white', 'text-black');
        activeTab.classList.add('bg-[#E0399A]', 'text-white');
    }

    loadGrammarRules();
}

/**
 * 3. Tải danh sách mẫu câu từ API
 */
async function loadGrammarRules() {
    const grid = document.getElementById('grammarGrid');
    const emptyState = document.getElementById('grammarEmptyState');
    grid.innerHTML = '<div class="col-span-full py-16 text-center text-slate-500 font-meta text-xs animate-pulse">📖 Đang nạp danh sách cấu trúc ngữ pháp từ OpenJLPT...</div>';
    emptyState.classList.add('hidden');

    try {
        const query = currentSearchKeyword ? `&keyword=${encodeURIComponent(currentSearchKeyword)}` : '';
        const url = `/api/learner/grammar?level=${currentLevel}&page=${currentPage}&size=12${query}`;
        const res = await fetch(url, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`Lỗi HTTP ${res.status}`);
        const data = await res.json();

        currentGrammarRules = data.content || [];
        totalPages = data.totalPages || 1;

        if (currentGrammarRules.length === 0) {
            grid.innerHTML = '';
            emptyState.classList.remove('hidden');
            renderPagination(0, 0);
            return;
        }

        renderGrammarGrid(currentGrammarRules);
        renderPagination(currentPage, totalPages);
    } catch (err) {
        console.error('Lỗi loadGrammarRules:', err);
        grid.innerHTML = `<div class="col-span-full py-8 text-center text-xs text-rose-600 bg-rose-50 border border-black rounded-[5px]">⚠️ Không thể tải dữ liệu ngữ pháp: ${err.message}</div>`;
    }
}

/**
 * 4. Render danh sách thẻ ngữ pháp chuẩn Retro-Brutalist
 */
function renderGrammarGrid(rules) {
    const grid = document.getElementById('grammarGrid');
    grid.innerHTML = rules.map(r => {
        const examples = r.examples || [];
        const firstEx = examples.length > 0 ? examples[0] : null;

        return `
            <div class="card-retro bg-white rounded-[5px] border border-black p-4 shadow-[3px_3px_0px_0px_#0F172A] flex flex-col justify-between hover:translate-x-[-1px] hover:translate-y-[-1px] hover:shadow-[4px_4px_0px_0px_#0F172A] transition duration-150">
                <div>
                    <!-- Header Thẻ: Mẫu câu, Level badge & Nút phát âm -->
                    <div class="flex items-start justify-between gap-2 pb-2.5 border-b border-black/10">
                        <div class="min-w-0">
                            <h3 class="text-xl sm:text-2xl font-bold font-serif text-black leading-snug">
                                ${escapeHtml(r.pattern)}
                            </h3>
                            <div class="text-[11px] text-slate-500 font-meta italic">
                                ${escapeHtml(r.romaji || '')}
                            </div>
                        </div>
                        <div class="flex items-center gap-1.5 shrink-0">
                            <span class="badge-nhai px-2 py-0.5 text-[10px] font-bold font-meta border border-black rounded-[4px] ${getLevelBadgeColor(r.level)}">
                                🌸 ${escapeHtml(r.level || 'N5')}
                            </span>
                            <button type="button" 
                                    onclick="speakJapanese('${escapeJs(r.pattern.replace(/〜|~/g, ''))}')"
                                    class="w-7 h-7 rounded-[4px] border border-black bg-white hover:bg-slate-100 flex items-center justify-center text-xs shadow-[1.5px_1.5px_0px_0px_#0F172A] active:scale-95 cursor-pointer"
                                    title="Nghe phát âm">
                                🔊
                            </button>
                        </div>
                    </div>

                    <!-- Khối Công Thức Kết Hợp (Formation Formula) -->
                    ${r.formation ? `
                        <div class="mt-2.5 p-2 bg-[#FAF9F6] border border-black/15 rounded-[4px] text-xs font-mono leading-relaxed">
                            <span class="text-slate-400 font-bold font-meta text-[10px] uppercase">Công thức:</span>
                            <span class="text-indigo-900 font-bold ml-1">${escapeHtml(translateFormation(r.formation))}</span>
                        </div>
                    ` : ''}

                    <!-- Giải Thích Ý Nghĩa 100% Tiếng Việt -->
                    <div class="py-2.5">
                        <p class="text-xs font-bold text-[#14532D] font-body leading-relaxed">
                            👉 ${escapeHtml(r.meaningVn || ('Cấu trúc ngữ pháp ' + r.pattern))}
                        </p>
                        ${r.notes ? `
                            <p class="text-[11px] text-slate-500 font-body mt-1 leading-normal italic">
                                💡 ${escapeHtml(r.notes)}
                            </p>
                        ` : ''}
                    </div>

                    <!-- Câu Ví Dụ Tiêu Biểu 100% Tiếng Việt -->
                    ${firstEx ? `
                        <div class="bg-amber-50/50 border border-amber-900/15 rounded-[4px] p-2.5 mb-2.5 text-xs leading-relaxed">
                            <div class="font-serif text-slate-900 flex items-start justify-between gap-1">
                                <div>
                                    <span class="text-amber-800 font-bold mr-1">💬</span>
                                    <span>${renderFuriganaHtml(firstEx.furigana || firstEx.ja)}</span>
                                </div>
                                <button type="button" 
                                        onclick="speakJapanese('${escapeJs(firstEx.ja)}')"
                                        class="shrink-0 text-slate-400 hover:text-black p-0.5" 
                                        title="Nghe phát âm cả câu">
                                    🔊
                                </button>
                            </div>
                            <div class="text-slate-600 text-[11px] font-body mt-1 pl-4">
                                ${escapeHtml(getDisplayExampleVn(firstEx))}
                            </div>
                        </div>
                    ` : ''}
                </div>

                <!-- Footer Thao Tác: AI Đặt Câu, Cất vào bộ thẻ -->
                <div class="pt-2.5 border-t border-black/10 grid grid-cols-2 gap-2">
                    <button type="button" 
                            onclick="openAiSentenceModal('${r.id}')"
                            class="btn-retro text-xs py-1.5 px-2 bg-[#7C3AED] hover:bg-[#6D28D9] text-white flex items-center justify-center gap-1 rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold transition active:translate-y-0.5 cursor-pointer truncate">
                        <span>🤖</span>
                        <span>AI Đặt Câu</span>
                    </button>

                    <button type="button" 
                            onclick="openAddGrammarToDeckModal('${r.id}')"
                            class="btn-retro text-xs py-1.5 px-2 bg-white hover:bg-slate-50 text-black flex items-center justify-center gap-1 rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold transition active:translate-y-0.5 cursor-pointer truncate">
                        <span>📁</span>
                        <span>Cất Vào Thẻ</span>
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

/**
 * 5. Mở Modal AI Sensei Đặt Câu Theo Ngữ Cảnh
 */
function openAiSentenceModal(ruleId) {
    const rule = currentGrammarRules.find(r => r.id === ruleId);
    if (!rule) return;
    selectedRuleForAi = rule;

    document.getElementById('aiModalPatternTitle').textContent = `Mẫu câu: ${rule.pattern}`;
    document.getElementById('aiModalPatternDesc').textContent = rule.meaningVn || ('Cấu trúc ngữ pháp ' + rule.pattern);
    document.getElementById('aiCustomContextInput').value = '';
    
    // Nạp khóa cá nhân đã lưu nếu có
    const keyInput = document.getElementById('aiCustomApiKeyInput');
    if (keyInput) {
        keyInput.value = localStorage.getItem('kizuna_gemini_api_key') || '';
    }

    // Ẩn vùng kết quả cũ
    const resultBox = document.getElementById('aiSentenceResultBox');
    resultBox.classList.add('hidden');

    document.getElementById('modalAiSentence').style.display = 'flex';
}

function saveCustomApiKey() {
    const input = document.getElementById('aiCustomApiKeyInput');
    if (!input) return;
    const key = input.value.trim();
    if (key) {
        localStorage.setItem('kizuna_gemini_api_key', key);
        KizunaToast.success('Đã lưu khóa cá nhân thành công! ✨');
    } else {
        localStorage.removeItem('kizuna_gemini_api_key');
        KizunaToast.info('Đã xóa khóa cá nhân. Hệ thống sẽ dùng bộ mẫu thông minh chuẩn.');
    }
}

function closeAiSentenceModal() {
    document.getElementById('modalAiSentence').style.display = 'none';
    selectedRuleForAi = null;
}

/**
 * 6. Kích hoạt gọi Gemini AI đặt câu theo ngữ cảnh
 */
async function generateAiSentenceWithContext(contextPreset) {
    if (!selectedRuleForAi) return;

    let targetContext = contextPreset;
    if (!targetContext) {
        targetContext = document.getElementById('aiCustomContextInput').value.trim();
        if (!targetContext) {
            KizunaToast.warning('Vui lòng chọn ngữ cảnh có sẵn hoặc nhập ngữ cảnh bạn mong muốn nhé! 🌸');
            return;
        }
    }

    const resultBox = document.getElementById('aiSentenceResultBox');
    const loadingBox = document.getElementById('aiSentenceLoadingBox');
    
    resultBox.classList.add('hidden');
    loadingBox.classList.remove('hidden');

    try {
        const clientApiKey = localStorage.getItem('kizuna_gemini_api_key') || '';
        const payload = {
            pattern: selectedRuleForAi.pattern,
            level: selectedRuleForAi.level,
            meaning: selectedRuleForAi.meaningVn || ('Cấu trúc ngữ pháp ' + selectedRuleForAi.pattern),
            context: targetContext,
            apiKey: clientApiKey
        };

        const res = await fetch('/api/learner/grammar/ai-generate', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error('Không thể tạo câu từ AI Sensei');
        const data = await res.json();

        loadingBox.classList.add('hidden');
        resultBox.classList.remove('hidden');

        document.getElementById('aiResContextLabel').textContent = `Ngữ cảnh: [${data.context}]`;
        document.getElementById('aiResJapaneseHtml').innerHTML = data.furiganaHtml || data.japaneseSentence;
        document.getElementById('aiResVietnamese').textContent = data.vietnameseMeaning;
        document.getElementById('aiResNuanceNote').textContent = data.nuanceNote || 'Dùng câu này rất tự nhiên và đúng văn phong giao tiếp Nhật Bản.';

        // Nút nghe phát âm câu do AI tạo
        const speakBtn = document.getElementById('aiResSpeakBtn');
        speakBtn.onclick = () => speakJapanese(data.japaneseSentence);

        // Khối chữ Kanji để luyện viết Shodo
        const kanjiContainer = document.getElementById('aiResKanjiActions');
        const kanjis = data.kanjiCharacters || [];
        if (kanjis.length > 0) {
            kanjiContainer.innerHTML = kanjis.map(ch => `
                <a href="/learner/practice-canvas?char=${encodeURIComponent(ch)}" 
                   class="btn-retro text-[11px] py-1 px-2 bg-[#15803D] text-white rounded-[4px] border border-black shadow-[1.5px_1.5px_0px_0px_#000] no-underline flex items-center gap-1 active:translate-y-0.5">
                    <span>🖌️</span> Tập viết chữ [${escapeHtml(ch)}]
                </a>
            `).join('');
        } else {
            kanjiContainer.innerHTML = '';
        }

        KizunaToast.success(`AI Sensei đã tạo câu cho ngữ cảnh "${data.context}" thành công! ✨`);
    } catch (err) {
        console.error('Lỗi generateAiSentenceWithContext:', err);
        loadingBox.classList.add('hidden');
        KizunaToast.danger('Có lỗi khi kết nối AI Sensei. Đang tải cấu trúc dự phòng.');
    }
}

/**
 * 7. Điều hướng phân trang
 */
function renderPagination(page, total) {
    const bar = document.getElementById('grammarPaginationBar');
    if (!bar) return;
    if (total <= 1) {
        bar.classList.add('hidden');
        return;
    }
    bar.classList.remove('hidden');
    document.getElementById('grammarPageInfo').textContent = `Trang ${page + 1} / ${total}`;
    document.getElementById('grammarPrevBtn').disabled = (page <= 0);
    document.getElementById('grammarNextBtn').disabled = (page >= total - 1);
}

function prevGrammarPage() {
    if (currentPage > 0) {
        currentPage--;
        loadGrammarRules();
    }
}

function nextGrammarPage() {
    if (currentPage < totalPages - 1) {
        currentPage++;
        loadGrammarRules();
    }
}

/**
 * 8. Quản lý lưu vào bộ thẻ Flashcard
 */
async function loadUserDecks() {
    try {
        const res = await fetch('/api/learner/decks', { headers: getAuthHeaders() });
        if (res.ok) {
            userDecks = await res.json();
            renderDeckSelectOptions();
        }
    } catch (e) {
        console.warn('Lỗi loadUserDecks:', e);
    }
}

function renderDeckSelectOptions() {
    const select = document.getElementById('saveGrammarDeckSelect');
    if (!select) return;
    if (userDecks.length === 0) {
        select.innerHTML = '<option value="">-- Chưa có bộ thẻ (Nhập tên bên dưới để tạo) --</option>';
        return;
    }
    select.innerHTML = '<option value="">-- Chọn bộ thẻ có sẵn --</option>' + 
        userDecks.map(d => `<option value="${d.folderId}">${escapeHtml(d.folderName)} (🌸 ${d.kanjiCount || 0} chữ)</option>`).join('');
}

function openAddGrammarToDeckModal(ruleId) {
    const rule = currentGrammarRules.find(r => r.id === ruleId);
    if (!rule) return;
    selectedRuleForDeck = rule;

    document.getElementById('modalGrammarPatternWord').textContent = rule.pattern;
    document.getElementById('modalGrammarMeaning').textContent = rule.meaningVn || ('Cấu trúc ' + rule.pattern);
    document.getElementById('modalGrammarFormation').textContent = translateFormation(rule.formation) || 'Cấu trúc câu chuẩn JLPT';
    document.getElementById('newGrammarDeckNameInput').value = '';
    renderDeckSelectOptions();

    document.getElementById('modalAddGrammarToDeck').style.display = 'flex';
}

function closeAddGrammarToDeckModal() {
    document.getElementById('modalAddGrammarToDeck').style.display = 'none';
    selectedRuleForDeck = null;
}

async function submitAddGrammarToDeck(e) {
    e.preventDefault();
    if (!selectedRuleForDeck) return;

    const deckId = document.getElementById('saveGrammarDeckSelect').value;
    const newDeckName = document.getElementById('newGrammarDeckNameInput').value.trim();

    if (!deckId && !newDeckName) {
        KizunaToast.warning('Vui lòng chọn bộ thẻ có sẵn hoặc nhập tên mới bạn nhé! 🌸');
        return;
    }

    const btn = document.getElementById('btnSubmitAddGrammarToDeck');
    btn.disabled = true;
    btn.textContent = 'Đang lưu...';

    try {
        // Tự động gom chữ Kanji có trong câu ví dụ của ngữ pháp để nạp vào bộ thẻ
        let kanjiChar = '文';
        if (selectedRuleForDeck.examples && selectedRuleForDeck.examples.length > 0) {
            const jaText = selectedRuleForDeck.examples[0].ja;
            for (const ch of jaText) {
                if (ch >= '\u4e00' && ch <= '\u9faf') {
                    kanjiChar = ch;
                    break;
                }
            }
        }

        let targetDeckId = deckId;
        if (!targetDeckId && newDeckName) {
            const createRes = await fetch('/api/learner/decks', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ deckName: newDeckName, description: 'Tạo từ Cẩm nang ngữ pháp' })
            });
            if (createRes.ok) {
                const created = await createRes.json();
                targetDeckId = created.id || created.folderId;
            }
        }

        if (targetDeckId) {
            await fetch(`/api/learner/decks/${targetDeckId}/cards`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({
                    character: kanjiChar,
                    meaning: `Ngữ pháp: ${selectedRuleForDeck.pattern} (${selectedRuleForDeck.meaningVn || selectedRuleForDeck.meaning})`
                })
            });
        }

        closeAddGrammarToDeckModal();
        KizunaToast.success(`Đã cất cấu trúc [${selectedRuleForDeck.pattern}] vào bộ thẻ thành công! 🌸`);
        loadUserDecks();
    } catch (err) {
        console.error('Lỗi submitAddGrammarToDeck:', err);
        KizunaToast.danger('Có lỗi khi cất ngữ pháp vào bộ thẻ.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span>💾</span><span>Lưu Ngay</span>';
    }
}

/**
 * 9. Phát âm âm thanh
 */
function speakJapanese(text) {
    if (!('speechSynthesis' in window)) {
        KizunaToast.warning('Trình duyệt của bạn chưa hỗ trợ phát âm âm thanh.');
        return;
    }
    window.speechSynthesis.cancel();
    const clean = text.replace(/〜|~/g, '').trim();
    const utterance = new SpeechSynthesisUtterance(clean);
    utterance.lang = 'ja-JP';
    utterance.rate = 0.85;
    window.speechSynthesis.speak(utterance);
}

function setupEventListeners() {
    const searchInput = document.getElementById('searchGrammarInput');
    if (searchInput) {
        let debounceTimer = null;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                currentSearchKeyword = e.target.value.trim();
                currentPage = 0;
                loadGrammarRules();
            }, 300);
        });
    }
}

/**
 * Tiện ích chuyển định dạng {Chữ|Furigana} thành <ruby>
 */
function renderFuriganaHtml(text) {
    if (!text) return '';
    return text.replace(/\{([^|]+)\|([^}]+)\}/g, '<ruby class="font-bold">$1<rt class="text-[10px] text-pink-600 font-normal">$2</rt></ruby>');
}

function getLevelBadgeColor(level) {
    switch (level) {
        case 'N5': return 'bg-emerald-100 text-emerald-800';
        case 'N4': return 'bg-sky-100 text-sky-800';
        case 'N3': return 'bg-amber-100 text-amber-800';
        case 'N2': return 'bg-orange-100 text-orange-800';
        case 'N1': return 'bg-rose-100 text-rose-800';
        default: return 'bg-slate-100 text-slate-800';
    }
}

function translateFormation(formation) {
    if (!formation) return '';
    return formation
        .replace(/\bNoun\b/g, 'Danh từ (N)')
        .replace(/\bVerb-te form\b|\bVerb-te\b/gi, 'Động từ thể て (V-te)')
        .replace(/\bVerb-nai stem\b/gi, 'Động từ thể ない bỏ ない')
        .replace(/\bVerb-nai form\b|\bVerb-nai\b/gi, 'Động từ thể ない (V-nai)')
        .replace(/\bVerb-dictionary form\b|\bVerb-dict\b|\bVerb-dictionary\b/gi, 'Động từ thể từ điển (V-ru)')
        .replace(/\bVerb-casual\b/gi, 'Động từ thể thông thường (V ngắn)')
        .replace(/\bVerb-plain\b/gi, 'Động từ thể thông thường (V ngắn)')
        .replace(/\bVerb-stem\b/gi, 'Động từ thể ます bỏ ます')
        .replace(/\bVerb-ta form\b|\bVerb-ta\b/gi, 'Động từ thể た (quá khứ)')
        .replace(/\bVerb-potential\b/gi, 'Động từ thể khả năng')
        .replace(/\bVerb-passive\b/gi, 'Động từ thể bị động')
        .replace(/\bVerb-causative\b/gi, 'Động từ thể sai khiến')
        .replace(/\bVerb-volitional\b/gi, 'Động từ thể ý chí')
        .replace(/\bVerb-conditional\b|\bVerb-ba\b/gi, 'Động từ thể điều kiện (ば)')
        .replace(/\bVerb\b/g, 'Động từ (V)')
        .replace(/\bi-adjective\b|\bi-Adj\b|\bI-adjective\b/gi, 'Tính từ đuôi い (A-i)')
        .replace(/\bna-adjective\b|\bna-Adj\b|\bNa-adjective\b/gi, 'Tính từ đuôi な (A-na)')
        .replace(/\bAdjective\b/gi, 'Tính từ (A)')
        .replace(/\bSentence\b/gi, 'Mệnh đề / Câu')
        .replace(/\bPhrase\b/gi, 'Cụm từ')
        .replace(/\bClause\b/gi, 'Mệnh đề')
        .replace(/\bPlain form\b/gi, 'Thể thông thường')
        .replace(/\bPolite form\b/gi, 'Thể lịch sự')
        .replace(/\bCounters\b|\bCounter\b/gi, 'Số từ + Lượng từ')
        .replace(/\bQuestion word\b/gi, 'Từ để hỏi');
}

function getDisplayExampleVn(ex) {
    if (!ex) return 'Câu ví dụ minh họa ngữ cảnh.';
    if (ex.vn && !isPureEnglish(ex.vn)) {
        return ex.vn;
    }
    // Bản dịch cơ bản nếu chuỗi chưa có tiếng Việt
    return 'Ví dụ mẫu: Câu đàm thoại sinh hoạt thực tế trong đời sống Nhật Bản.';
}

function isPureEnglish(str) {
    if (!str) return false;
    const vietnameseRegex = /[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i;
    return !vietnameseRegex.test(str) && /^[a-zA-Z0-9\s.,!?'"()\-]+$/.test(str.trim());
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function escapeJs(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '\\"');
}
