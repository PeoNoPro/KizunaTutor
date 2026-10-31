/**
 * grammarHandbook.js - Điều khiển Cẩm Nang Ngữ Pháp OpenJLPT & Kizuna AI Sensei Đặt Câu
 * Áp dụng cơ chế Safe Fetch: AbortController chống Race Condition & MemoryCache chống Empty State Flicker
 */
let currentLevel = 'N5';
let masterGrammarList = [];    // Master Data Protection: Bảo toàn danh sách gốc
let displayedGrammarList = []; // Danh sách đang hiển thị
let currentGrammarRules = [];  // Tương thích ngược
let currentPage = 0;
let totalPages = 1;
let totalElements = 0;
let currentSearchKeyword = '';
let selectedRuleForAi = null;
let selectedRuleForDeck = null;
let userDecks = [];

// Quản lý Request & Cache chống Race Condition
let grammarAbortController = null;
const grammarMemoryCache = new Map(); // Key: `${level}_${page}_${keyword}`

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

function getActiveLevel() {
    const levelSelect = document.getElementById('grammarLevelSelect');
    if (levelSelect && levelSelect.value) {
        const val = String(levelSelect.value).trim().toUpperCase();
        if (val) return val;
    }
    return currentLevel || 'N5';
}

async function initGrammarHandbook() {
    await loadLevelCounts();
    await loadGrammarRules(0);
    setupEventListeners();

    // Kiểm tra URL xem có yêu cầu xem chi tiết mẫu câu không (?id=...)
    const urlParams = new URLSearchParams(window.location.search);
    const initialRuleId = urlParams.get('id');
    if (initialRuleId) {
        viewGrammarDetail(null, initialRuleId);
    }

    // Lắng nghe sự kiện Back/Forward của trình duyệt để chuyển đổi mượt mà
    window.addEventListener('popstate', (e) => {
        const params = new URLSearchParams(window.location.search);
        const ruleId = params.get('id');
        if (ruleId) {
            viewGrammarDetail(null, ruleId);
        } else {
            const listView = document.getElementById('grammarListView');
            const detailView = document.getElementById('grammarDetailView');
            if (listView && detailView) {
                detailView.classList.add('hidden');
                listView.classList.remove('hidden');
            }
        }
    });
}

/**
 * 1. Nạp thống kê số lượng mẫu câu theo 5 cấp độ JLPT
 */
async function loadLevelCounts() {
    try {
        const res = await fetch('/api/learner/grammar/levels', { headers: getAuthHeaders() });
        if (res.ok) {
            const counts = await res.json();
            const levelNames = {
                'N5': '🌱 JLPT N5',
                'N4': '🌿 JLPT N4',
                'N3': '🎋 JLPT N3',
                'N2': '🌸 JLPT N2',
                'N1': '⛩️ JLPT N1'
            };
            const levelSelect = document.getElementById('grammarLevelSelect');
            if (levelSelect) {
                const activeLvl = getActiveLevel();
                levelSelect.innerHTML = Object.entries(counts).map(([lvl, count]) => {
                    const normLvl = String(lvl || '').trim().toUpperCase();
                    const name = levelNames[normLvl] || `JLPT ${normLvl}`;
                    return `<option value="${normLvl}" ${normLvl === activeLvl ? 'selected' : ''}>${name} (${count} mẫu câu)</option>`;
                }).join('');
                levelSelect.value = activeLvl;
                currentLevel = activeLvl;
            }
        }
    } catch (e) {
        console.warn('Lỗi loadLevelCounts:', e);
    }
}

/**
 * 2. Chọn cấp độ JLPT (N5 -> N1) - Chuẩn hóa kiểu & chống Race Condition
 */
function selectLevel(lvl) {
    const safeLevel = String(lvl || 'N5').trim().toUpperCase();
    currentLevel = safeLevel;
    currentPage = 0;
    currentSearchKeyword = '';

    // Xóa trắng ô tìm kiếm khi đổi filter để tránh bộ lọc cũ làm rỗng kết quả
    const searchInput = document.getElementById('searchGrammarInput');
    if (searchInput) searchInput.value = '';

    const levelSelect = document.getElementById('grammarLevelSelect');
    if (levelSelect && levelSelect.value !== currentLevel) {
        levelSelect.value = currentLevel;
    }

    loadGrammarRules(0);
}

/**
 * 3. Tải danh sách mẫu câu từ API với AbortController và MemoryCache an toàn
 */
async function loadGrammarRules(targetPage) {
    if (typeof targetPage === 'number') {
        currentPage = Math.max(0, targetPage);
    }

    const grid = document.getElementById('grammarGrid');
    const emptyState = document.getElementById('grammarEmptyState');
    if (!grid) return;

    currentLevel = getActiveLevel();
    currentPage = Math.max(0, Number(currentPage) || 0);

    // 1. Hủy ngay request cũ đang dở dang nếu người dùng click chuyển filter liên tục
    if (grammarAbortController) {
        grammarAbortController.abort();
    }
    grammarAbortController = new AbortController();

    // 2. Tạo cache key chuẩn hóa
    const cleanKeyword = String(currentSearchKeyword || '').trim().toLowerCase();
    const cacheKey = `${currentLevel}_p${currentPage}_kw${cleanKeyword}`;

    // 3. Kiểm tra bộ nhớ đệm Cache: Nếu có dữ liệu hợp lệ, render ngay tức thì (0ms latency, zero flicker)
    if (grammarMemoryCache.has(cacheKey)) {
        const cached = grammarMemoryCache.get(cacheKey);
        if (cached && Array.isArray(cached.rules) && cached.rules.length > 0) {
            masterGrammarList = cached.rules;
            displayedGrammarList = cached.rules;
            currentGrammarRules = cached.rules;
            totalPages = Number(cached.totalPages) || 1;
            totalElements = Number(cached.totalElements) || displayedGrammarList.length;
            renderGrammarGrid(displayedGrammarList);
            renderPagination(currentPage, totalPages, totalElements);
            if (emptyState) emptyState.classList.add('hidden');
            return;
        }
    }

    // 4. Hiển thị Skeleton / Loading trạng thái chờ
    grid.innerHTML = '<div class="col-span-full py-16 text-center text-slate-500 font-meta text-xs animate-pulse">📖 Đang nạp danh sách cấu trúc ngữ pháp từ OpenJLPT...</div>';
    if (emptyState) emptyState.classList.add('hidden');

    try {
        const query = cleanKeyword ? `&keyword=${encodeURIComponent(cleanKeyword)}` : '';
        const url = `/api/learner/grammar?level=${encodeURIComponent(currentLevel)}&page=${currentPage}&size=12${query}`;
        
        const res = await fetch(url, {
            headers: getAuthHeaders(),
            signal: grammarAbortController.signal
        });

        if (!res.ok) throw new Error(`Lỗi HTTP ${res.status}`);
        const data = await res.json();

        const rules = Array.isArray(data.content) ? data.content : [];
        const totalPgs = Number(data.totalPages) || 1;
        const totalElems = Number(data.totalElements) || rules.length;

        // 5. Lưu vào Cache nếu có dữ liệu
        if (rules.length > 0) {
            grammarMemoryCache.set(cacheKey, {
                rules: rules,
                totalPages: totalPgs,
                totalElements: totalElems
            });
        }

        masterGrammarList = rules;
        displayedGrammarList = rules;
        currentGrammarRules = rules;
        totalPages = totalPgs;
        totalElements = totalElems;

        // 6. Xử lý trạng thái rỗng
        if (displayedGrammarList.length === 0) {
            grid.innerHTML = '';
            if (emptyState) emptyState.classList.remove('hidden');
            renderPagination(0, 0, 0);
            return;
        }

        // 7. Render danh sách thẻ ngữ pháp
        renderGrammarGrid(displayedGrammarList);
        renderPagination(currentPage, totalPages, totalElements);

    } catch (err) {
        // Bỏ qua lỗi nếu do chủ động abort khi chuyển filter nhanh
        if (err.name === 'AbortError') return;

        console.error('Lỗi loadGrammarRules:', err);
        grid.innerHTML = `<div class="col-span-full py-8 text-center text-xs text-rose-600 bg-rose-50 border border-black rounded-[5px]">⚠️ Không thể tải dữ liệu ngữ pháp: ${escapeHtml(err.message)}</div>`;
    }
}

/**
 * 4. Render danh sách thẻ ngữ pháp chuẩn Retro-Brutalist
 * Theo yêu cầu người dùng:
 * - Bên ngoài chỉ hiển thị: Mẫu ngữ pháp + Romaji + Badge JLPT + Nút nghe + Nghĩa tiếng Việt.
 * - Loại bỏ nút AI đặt câu và nút cất vào bộ thẻ.
 * - Khi bấm vào mẫu câu / card sẽ chuyển sang trang giao diện chi tiết riêng biệt (không dùng chung giao diện với bên ngoài).
 */
function renderGrammarGrid(rules) {
    const grid = document.getElementById('grammarGrid');
    if (!grid) return;

    grid.innerHTML = rules.map(r => {
        const cleanPat = cleanGrammarPattern(r.pattern);
        const cleanRom = cleanGrammarRomaji(r.romaji);

        return `
            <div class="card-retro bg-white rounded-[5px] border border-black p-4 shadow-[3px_3px_0px_0px_#0F172A] flex flex-col justify-between hover:translate-x-[-1px] hover:translate-y-[-1px] hover:shadow-[4px_4px_0px_0px_#0F172A] transition duration-150 cursor-pointer"
                 onclick="viewGrammarDetail(event, '${r.id}')"
                 title="Bấm để xem chi tiết công thức và câu ví dụ">
                <div class="space-y-2.5">
                    <!-- VÙNG TIÊU ĐỀ & MẪU NGỮ PHÁP (ĐÃ LOẠI BỎ CHÚ THÍCH TIẾNG ANH) -->
                    <div class="flex items-start justify-between gap-2 pb-2 border-b border-black/10">
                        <div class="min-w-0">
                            <h3 class="text-xl sm:text-2xl font-bold font-serif text-black hover:text-[#E0399A] transition leading-snug flex items-center gap-1.5">
                                <span>${escapeHtml(cleanPat)}</span>
                            </h3>
                            <div class="text-[11px] text-slate-500 font-meta italic">
                                ${escapeHtml(cleanRom)}
                            </div>
                        </div>
                        <div class="flex items-center gap-1.5 shrink-0" onclick="event.stopPropagation()">
                            <span class="badge-nhai px-2 py-0.5 text-[10px] font-bold font-meta border border-black rounded-[4px] ${getLevelBadgeColor(r.level)}">
                                🌸 ${escapeHtml(r.level || 'N5')}
                            </span>
                            <button type="button" 
                                    onclick="playAudioGrammar(event, '${escapeJs(cleanPat.replace(/〜|~/g, ''))}')"
                                    class="w-7 h-7 rounded-[4px] border border-black bg-white hover:bg-slate-100 flex items-center justify-center text-xs shadow-[1.5px_1.5px_0px_0px_#0F172A] active:scale-95 cursor-pointer"
                                    title="Nghe phát âm">
                                🔊
                            </button>
                        </div>
                    </div>

                    <!-- CHỈ HIỂN THỊ MẪU CÂU + NGHĨA Ở BÊN NGOÀI -->
                    <div class="pt-1">
                        <p class="text-xs font-bold text-[#14532D] font-body leading-relaxed">
                            👉 ${escapeHtml(r.meaningVn || ('Cấu trúc ngữ pháp ' + cleanPat))}
                        </p>
                    </div>
                </div>

                <!-- Nút chuyển sang trang giao diện chi tiết -->
                <div class="pt-3 mt-3 border-t border-black/10 flex items-center justify-between text-xs shrink-0">
                    <span class="text-[11px] font-bold text-[#E0399A] font-meta flex items-center gap-1">
                        📖 Chi tiết công thức &amp; ví dụ
                    </span>
                    <button type="button" 
                            onclick="viewGrammarDetail(event, '${r.id}')"
                            class="px-2.5 py-1 bg-white hover:bg-[#FFE5EC] text-black border border-black rounded-[4px] shadow-[1.5px_1.5px_0px_0px_#0F172A] font-itim font-bold transition active:translate-y-0.5 cursor-pointer">
                        Xem chi tiết ➔
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

/**
 * Điều hướng mở trang giao diện chi tiết riêng biệt cho mẫu ngữ pháp
 */
async function viewGrammarDetail(event, ruleId) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    const targetId = typeof event === 'string' ? event : ruleId;
    if (!targetId) return;

    // Tìm trong master data đã nạp
    let rule = (masterGrammarList && masterGrammarList.length > 0 ? masterGrammarList : displayedGrammarList)
        .find(r => String(r.id) === String(targetId));

    if (rule) {
        showGrammarDetailView(rule);
        return;
    }

    // Nếu chưa có trong danh sách hiện tại (truy cập link trực tiếp), nạp từ API
    await fetchGrammarDetailById(targetId);
}

/**
 * Nạp chi tiết mẫu ngữ pháp từ Backend API
 */
async function fetchGrammarDetailById(ruleId) {
    const detailContent = document.getElementById('grammarDetailContent');
    const listView = document.getElementById('grammarListView');
    const detailView = document.getElementById('grammarDetailView');

    if (listView && detailView) {
        listView.classList.add('hidden');
        detailView.classList.remove('hidden');
    }

    if (detailContent) {
        detailContent.innerHTML = `
            <div class="p-8 text-center text-slate-500 bg-white border border-black rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A]">
                <div class="animate-spin inline-block w-6 h-6 border-2 border-current border-t-transparent text-[#E0399A] rounded-full mb-2"></div>
                <p class="text-xs font-bold font-meta">Đang nạp chi tiết ngữ pháp...</p>
            </div>
        `;
    }

    try {
        const res = await fetch(`/api/learner/grammar/${encodeURIComponent(ruleId)}`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error('Không thể tải chi tiết ngữ pháp');
        const rule = await res.json();
        showGrammarDetailView(rule);
    } catch (err) {
        console.error('Lỗi nạp chi tiết ngữ pháp:', err);
        if (detailContent) {
            detailContent.innerHTML = `
                <div class="p-6 bg-rose-50 border border-black rounded-[5px] text-center text-rose-700 text-xs shadow-[2px_2px_0px_0px_#0F172A]">
                    ⚠️ Không thể tải thông tin chi tiết của mẫu câu này. Vui lòng thử lại sau!
                </div>
            `;
        }
    }
}

/**
 * Hiển thị giao diện trang chi tiết riêng biệt (Dedicated Detail View)
 */
function showGrammarDetailView(rule) {
    const listView = document.getElementById('grammarListView');
    const detailView = document.getElementById('grammarDetailView');
    const detailContent = document.getElementById('grammarDetailContent');
    const bcLevel = document.getElementById('detailBreadcrumbLevel');
    const bcPattern = document.getElementById('detailBreadcrumbPattern');

    if (bcLevel) bcLevel.textContent = `JLPT ${rule.level || 'N5'}`;
    if (bcPattern) bcPattern.textContent = cleanGrammarPattern(rule.pattern) || 'Mẫu ngữ pháp';

    // Cập nhật URL trình duyệt thân thiện
    try {
        const newUrl = `${window.location.pathname}?id=${encodeURIComponent(rule.id)}`;
        if (window.location.search !== `?id=${encodeURIComponent(rule.id)}`) {
            window.history.pushState({ id: rule.id }, '', newUrl);
        }
    } catch (e) {
        console.warn('History pushState warning:', e);
    }

    if (detailContent) {
        renderGrammarDetailPage(rule);
    }

    if (listView && detailView) {
        listView.classList.add('hidden');
        detailView.classList.remove('hidden');
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * Render toàn bộ nội dung chi tiết vào trang chi tiết riêng
 */
function renderGrammarDetailPage(rule) {
    const container = document.getElementById('grammarDetailContent');
    if (!container) return;

    const cleanPat = cleanGrammarPattern(rule.pattern);
    const cleanRom = cleanGrammarRomaji(rule.romaji);
    const examples = rule.examples || [];

    container.innerHTML = `
        <!-- THẺ HERO MẪU NGỮ PHÁP (ĐÃ LOẠI BỎ CHÚ THÍCH TIẾNG ANH) -->
        <div class="bg-white border border-black rounded-[5px] p-5 shadow-[4px_4px_0px_0px_#0F172A]">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-black/10">
                <div>
                    <div class="flex items-center gap-2">
                        <span class="badge-nhai px-2.5 py-1 text-xs font-bold font-meta border border-black rounded-[4px] ${getLevelBadgeColor(rule.level)}">
                            🌸 JLPT ${escapeHtml(rule.level || 'N5')}
                        </span>
                        <span class="text-xs text-slate-500 font-meta italic">
                            ${escapeHtml(cleanRom)}
                        </span>
                    </div>
                    <h1 class="text-2xl sm:text-3xl font-bold font-serif text-black mt-2 leading-tight">
                        ${escapeHtml(cleanPat)}
                    </h1>
                </div>
                <button type="button" 
                        onclick="playAudioGrammar(event, '${escapeJs(cleanPat.replace(/〜|~/g, ''))}')"
                        class="px-4 py-2 bg-white hover:bg-slate-100 text-[#0F172A] border border-black rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold flex items-center gap-2 self-start sm:self-auto cursor-pointer">
                    <span class="text-base">🔊</span>
                    <span>Nghe Phát Âm Mẫu Câu</span>
                </button>
            </div>

            <!-- Ý NGHĨA CỐT LÕI TIẾNG VIỆT 100% -->
            <div class="mt-4 p-4 bg-[#F0FDF4] border-2 border-[#15803D] rounded-[5px] shadow-[2px_2px_0px_0px_#15803D]">
                <div class="text-[11px] font-bold font-meta text-[#15803D] uppercase tracking-wide">
                    📌 Ý Nghĩa Tiếng Việt:
                </div>
                <p class="text-sm sm:text-base font-bold text-[#14532D] font-body mt-1 leading-relaxed">
                    👉 ${escapeHtml(rule.meaningVn || rule.meaning || ('Cấu trúc ngữ pháp ' + cleanPat))}
                </p>
            </div>
        </div>

        <!-- KHỐI CÔNG THỨC KẾT HỢP (FORMATION FORMULA) -->
        <div class="bg-white border border-black rounded-[5px] p-5 shadow-[4px_4px_0px_0px_#0F172A] space-y-3">
            <h2 class="text-sm font-bold font-meta uppercase text-slate-800 flex items-center gap-2 pb-2 border-b border-black/10">
                <span>📐</span>
                <span>Công Thức Kết Hợp Cấu Trúc</span>
            </h2>
            <div class="p-3.5 bg-[#FAF9F6] border border-black/20 rounded-[5px] text-xs sm:text-sm font-mono leading-relaxed text-indigo-950 font-bold">
                ${escapeHtml(translateFormation(rule.formation) || 'Đang cập nhật công thức chi tiết...')}
            </div>

            ${rule.notes ? `
                <div class="p-3 bg-amber-50 border border-amber-600/30 rounded-[5px] text-xs text-slate-700 font-body leading-relaxed flex items-start gap-2">
                    <span class="text-amber-800 font-bold text-sm">💡</span>
                    <div>
                        <span class="font-bold text-amber-900">Ghi chú &amp; Lưu ý:</span>
                        <p class="mt-0.5">${escapeHtml(rule.notes)}</p>
                    </div>
                </div>
            ` : ''}
        </div>

        <!-- KHỐI DANH SÁCH CÂU VÍ DỤ MINH HỌA (100% TIẾNG VIỆT) -->
        <div class="bg-white border border-black rounded-[5px] p-5 shadow-[4px_4px_0px_0px_#0F172A] space-y-4">
            <div class="flex items-center justify-between pb-2 border-b border-black/10">
                <h2 class="text-sm font-bold font-meta uppercase text-slate-800 flex items-center gap-2">
                    <span>💬</span>
                    <span>Các Câu Ví Dụ Minh Họa (${examples.length} câu)</span>
                </h2>
                <span class="text-[11px] font-bold text-slate-500 font-meta">Đọc Furigana &amp; Nghe Audio</span>
            </div>

            ${examples.length === 0 ? `
                <p class="text-xs text-slate-500 italic py-4 text-center">Chưa có câu ví dụ minh họa cho mẫu câu này.</p>
            ` : `
                <div class="space-y-3">
                    ${examples.map((ex, idx) => `
                        <div class="bg-[#FAF9F6] border border-black/15 rounded-[5px] p-3.5 space-y-2 hover:border-black transition">
                            <div class="flex items-start justify-between gap-3">
                                <div class="space-y-1">
                                    <div class="text-xs font-bold text-pink-700 font-meta">
                                        Ví dụ #${idx + 1}
                                    </div>
                                    <div class="text-sm sm:text-base font-serif text-slate-900 leading-relaxed">
                                        ${renderFuriganaHtml(ex.furigana || ex.ja)}
                                    </div>
                                </div>
                                <button type="button" 
                                        onclick="playAudioGrammar(event, '${escapeJs(ex.ja)}')"
                                        class="w-8 h-8 rounded-[4px] border border-black bg-white hover:bg-slate-100 flex items-center justify-center text-xs shadow-[1.5px_1.5px_0px_0px_#0F172A] shrink-0 active:scale-95 cursor-pointer"
                                        title="Nghe câu ví dụ">
                                    🔊
                                </button>
                            </div>
                            <div class="text-xs font-body text-slate-700 pt-1.5 border-t border-dashed border-black/15 pl-2 border-l-2 border-[#15803D]">
                                👉 <span class="font-bold text-[#14532D]">${escapeHtml(getDisplayExampleVn(ex))}</span>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `}
        </div>

        <!-- NÚT QUAY LẠI CHÂN TRANG CHI TIẾT -->
        <div class="pt-2 flex justify-start">
            <button type="button" 
                    onclick="closeGrammarDetail(event)"
                    class="text-xs py-2 px-4 bg-white hover:bg-slate-100 text-[#0F172A] border border-black rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold flex items-center gap-1.5 active:translate-y-0.5 cursor-pointer">
                <span>⬅️</span>
                <span>Quay Lại Danh Sách Ngữ Pháp</span>
            </button>
        </div>
    `;
}

/**
 * Đóng giao diện chi tiết, quay trở về danh sách ngữ pháp
 */
function closeGrammarDetail(event) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    const listView = document.getElementById('grammarListView');
    const detailView = document.getElementById('grammarDetailView');

    try {
        if (window.location.search) {
            window.history.pushState({}, '', window.location.pathname);
        }
    } catch (e) {
        console.warn('History pushState warning:', e);
    }

    if (listView && detailView) {
        detailView.classList.add('hidden');
        listView.classList.remove('hidden');
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
}

/**
 * 7. Điều hướng phân trang chuẩn Retro-Brutalist (đồng bộ với Kho Từ Vựng)
 */
function renderPagination(curPage, totalPgs, totalElems) {
    const bar = document.getElementById('grammarPaginationBar');
    const summary = document.getElementById('grammarPaginationSummary');
    const buttonsContainer = document.getElementById('grammarPaginationButtons');
    if (!bar || !buttonsContainer) return;

    if (totalPgs <= 1 && totalElems <= 12) {
        bar.classList.add('hidden');
        return;
    }
    bar.classList.remove('hidden');

    const start = totalElems === 0 ? 0 : (curPage * 12) + 1;
    const end = Math.min((curPage + 1) * 12, totalElems);
    if (summary) {
        summary.textContent = `Hiển thị ${start} - ${end} trên tổng số ${Number(totalElems).toLocaleString('vi-VN')} mẫu câu`;
    }

    buttonsContainer.innerHTML = '';

    // Nút Trang trước
    const prevBtn = document.createElement('button');
    prevBtn.type = 'button';
    prevBtn.disabled = curPage === 0;
    prevBtn.className = `px-2.5 py-1 rounded-[5px] border border-black text-xs font-bold shadow-[1.5px_1.5px_0px_0px_#0F172A] transition ${
        curPage === 0 ? 'opacity-40 cursor-not-allowed bg-slate-100 text-slate-400' : 'bg-white hover:bg-slate-100 text-slate-800 active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none'
    }`;
    prevBtn.innerHTML = '← Trước';
    prevBtn.onclick = (e) => {
        if (e) { e.preventDefault(); e.stopPropagation(); }
        prevGrammarPage();
    };
    buttonsContainer.appendChild(prevBtn);

    // Danh sách số trang (tối đa 5 nút)
    const maxBtns = 5;
    let startPage = Math.max(0, curPage - 2);
    let endPage = Math.min(totalPgs - 1, startPage + maxBtns - 1);
    if (endPage - startPage + 1 < maxBtns) {
        startPage = Math.max(0, endPage - maxBtns + 1);
    }

    for (let p = startPage; p <= endPage; p++) {
        const pBtn = document.createElement('button');
        pBtn.type = 'button';
        pBtn.className = `w-7 h-7 rounded-[5px] border border-black text-xs font-bold shadow-[1.5px_1.5px_0px_0px_#0F172A] transition ${
            p === curPage ? 'bg-black text-white' : 'bg-white hover:bg-slate-100 text-slate-800 active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none'
        }`;
        pBtn.textContent = p + 1;
        pBtn.onclick = (e) => {
            if (e) { e.preventDefault(); e.stopPropagation(); }
            goToGrammarPage(p);
        };
        buttonsContainer.appendChild(pBtn);
    }

    // Nút Trang sau
    const nextBtn = document.createElement('button');
    nextBtn.type = 'button';
    nextBtn.disabled = curPage >= totalPgs - 1;
    nextBtn.className = `px-2.5 py-1 rounded-[5px] border border-black text-xs font-bold shadow-[1.5px_1.5px_0px_0px_#0F172A] transition ${
        curPage >= totalPgs - 1 ? 'opacity-40 cursor-not-allowed bg-slate-100 text-slate-400' : 'bg-white hover:bg-slate-100 text-slate-800 active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none'
    }`;
    nextBtn.innerHTML = 'Sau →';
    nextBtn.onclick = (e) => {
        if (e) { e.preventDefault(); e.stopPropagation(); }
        nextGrammarPage();
    };
    buttonsContainer.appendChild(nextBtn);
}

function goToGrammarPage(p) {
    if (p < 0 || p >= totalPages || p === currentPage) return;
    currentPage = p;
    loadGrammarRules();
    const grid = document.getElementById('grammarGrid');
    if (grid) grid.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function prevGrammarPage() {
    if (currentPage > 0) {
        currentPage--;
        loadGrammarRules();
        const grid = document.getElementById('grammarGrid');
        if (grid) grid.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

function nextGrammarPage() {
    if (currentPage < totalPages - 1) {
        currentPage++;
        loadGrammarRules();
        const grid = document.getElementById('grammarGrid');
        if (grid) grid.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

/**
 * 8. Quản lý lưu vào bộ thẻ Flashcard (Tạm thời tắt theo yêu cầu giao diện tối giản)
 */
async function loadUserDecks() {
    // Tạm thời vô hiệu hóa
}

function openAddGrammarToDeckModal(event, ruleId) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    if (window.KizunaToast) KizunaToast.info('Tính năng thêm ngữ pháp vào bộ thẻ đang tạm đóng để nâng cấp.');
}

function handleSaveGrammar(event, ruleId) {
    openAddGrammarToDeckModal(event, ruleId);
}

function closeAddGrammarToDeckModal(event) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
}

async function submitAddGrammarToDeck(e) {
    if (e) {
        if (typeof e.preventDefault === 'function') e.preventDefault();
        if (typeof e.stopPropagation === 'function') e.stopPropagation();
    }
}

/**
 * 9. Phát âm âm thanh (Play Audio & SpeechSynthesis)
 * Có chặn triệt để Event Bubbling & Form Submission
 */
function playAudioGrammar(event, text) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    const targetText = typeof event === 'string' ? event : text;
    speakJapanese(event, targetText);
}

function speakJapanese(event, text) {
    if (event && typeof event.preventDefault === 'function') {
        event.preventDefault();
        event.stopPropagation();
    }
    const targetText = typeof event === 'string' ? event : text;
    if (!targetText) return;

    if (!('speechSynthesis' in window)) {
        if (window.KizunaToast) KizunaToast.warning('Trình duyệt của bạn chưa hỗ trợ phát âm âm thanh.');
        return;
    }
    window.speechSynthesis.cancel();
    const clean = String(targetText).replace(/〜|~/g, '').trim();
    const utterance = new SpeechSynthesisUtterance(clean);
    utterance.lang = 'ja-JP';
    utterance.rate = 0.85;
    window.speechSynthesis.speak(utterance);
}

function handleGrammarSearchSubmit(event) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    const searchInput = document.getElementById('searchGrammarInput');
    currentSearchKeyword = searchInput ? searchInput.value.trim() : '';
    currentPage = 0;
    loadGrammarRules(0);
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
                loadGrammarRules(0);
            }, 300);
        });
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                e.stopPropagation();
                handleGrammarSearchSubmit(e);
            }
        });
    }

    const filterForm = document.getElementById('grammarFilterForm');
    if (filterForm) {
        filterForm.addEventListener('submit', (e) => {
            e.preventDefault();
            e.stopPropagation();
            handleGrammarSearchSubmit(e);
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
    return 'Ví dụ mẫu: Câu đàm thoại sinh hoạt thực tế trong đời sống Nhật Bản.';
}

function isPureEnglish(str) {
    if (!str) return false;
    const vietnameseRegex = /[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i;
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

/**
 * Loại bỏ các chú thích tiếng Anh thừa thãi trong ngoặc tròn như: （subject）, (subject), （time）, etc.
 */
function cleanGrammarPattern(str) {
    if (!str) return '';
    return String(str).replace(/（[a-zA-Z\s/_\-]+）|\([a-zA-Z\s/_\-]+\)/g, '').trim();
}

function cleanGrammarRomaji(str) {
    if (!str) return '';
    return String(str).replace(/（[a-zA-Z\s/_\-]+）|\([a-zA-Z\s/_\-]+\)/g, '').trim();
}
