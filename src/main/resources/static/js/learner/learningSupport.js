/**
 * learningSupport.js - Điều khiển giao diện Kho Từ Vựng OpenJLPT & Hỗ Trợ Học Tập
 * Chuẩn Retro-Brutalist Hard-Shadow Design System
 * Tích hợp Safe Fetch: AbortController chống Race Condition & MemoryCache chống rỗng giao diện khi đổi filter
 */
let currentTopics = [];
let activeTopicIdentifier = 'JLPT_N5'; // Mặc định mở N5 hoặc ALL_JLPT
let currentVocabularies = [];
let currentPage = 0;
let totalPages = 1;
let totalElements = 0;
let currentSearchKeyword = '';
let searchDebounceTimer = null;
let selectedVocabForDeck = null;
let userDecks = [];

// Quản lý Request & Cache chống Race Condition
let vocabAbortController = null;
const vocabMemoryCache = new Map(); // Key: `${topicKey}_p${page}_kw${keyword}`

document.addEventListener('DOMContentLoaded', () => {
    initLearningSupport();
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

async function initLearningSupport() {
    await loadTopics();
    setupEventListeners();

    // Kiểm tra URL xem có yêu cầu xem chi tiết từ vựng không (?id=...)
    const urlParams = new URLSearchParams(window.location.search);
    const initialVocabId = urlParams.get('id');
    if (initialVocabId) {
        viewVocabDetail(null, initialVocabId);
    }

    // Lắng nghe sự kiện Back/Forward của trình duyệt để chuyển đổi mượt mà
    window.addEventListener('popstate', (e) => {
        const params = new URLSearchParams(window.location.search);
        const vocabId = params.get('id');
        if (vocabId) {
            viewVocabDetail(null, vocabId);
        } else {
            const listView = document.getElementById('vocabListView');
            const detailView = document.getElementById('vocabDetailView');
            if (listView && detailView) {
                detailView.classList.add('hidden');
                listView.classList.remove('hidden');
            }
        }
    });
}

/**
 * 1. Nạp danh sách chủ đề & 5 cấp độ JLPT từ API
 */
async function loadTopics() {
    const topicSelect = document.getElementById('topicSelect');
    try {
        const res = await fetch('/api/learner/topics', { headers: getAuthHeaders() });
        if (!res.ok) throw new Error('Không thể tải danh sách chủ đề');
        currentTopics = await res.json();

        if (!currentTopics || currentTopics.length === 0) {
            if (topicSelect) topicSelect.innerHTML = '<option value="">Chưa có danh mục nào được nạp</option>';
            return;
        }

        if (topicSelect) {
            topicSelect.innerHTML = currentTopics.map((topic) => {
                const topicKey = String(topic.code || topic.id || '').trim();
                const countText = topic.vocabCount ? Number(topic.vocabCount).toLocaleString('vi-VN') : '0';
                return `<option value="${topicKey}">${topic.icon || '📚'} ${escapeHtml(topic.titleVn)} (${countText} từ)</option>`;
            }).join('');
        }

        // Tự động kích hoạt chủ đề đầu tiên (ALL_JLPT hoặc JLPT_N5)
        if (currentTopics.length > 0) {
            const firstKey = String(currentTopics[0].code || currentTopics[0].id || 'JLPT_N5').trim();
            if (topicSelect) topicSelect.value = firstKey;
            selectTopic(firstKey);
        }
    } catch (err) {
        console.error('Lỗi loadTopics:', err);
        if (window.KizunaToast) KizunaToast.danger('Có lỗi khi tải danh mục từ vựng.');
    }
}

/**
 * 2. Chọn chủ đề và nạp danh sách từ vựng - Chuẩn hóa String & Reset search
 */
async function selectTopic(topicIdentifier) {
    const safeTopic = String(topicIdentifier || 'JLPT_N5').trim();
    activeTopicIdentifier = safeTopic;
    currentPage = 0;
    currentSearchKeyword = '';

    // Xóa trắng ô tìm kiếm khi đổi chủ đề để tránh từ khóa cũ làm rỗng danh sách mới
    const searchInput = document.getElementById('searchVocabInput');
    if (searchInput) searchInput.value = '';

    const topicSelect = document.getElementById('topicSelect');
    if (topicSelect && topicSelect.value !== activeTopicIdentifier) {
        topicSelect.value = activeTopicIdentifier;
    }

    // Hiển thị tiêu đề & mô tả chủ đề ngay tức thì từ bộ nhớ
    const topic = currentTopics.find(t => 
        String(t.code || '').trim() === safeTopic || 
        String(t.id || '').trim() === safeTopic
    );

    if (topic) {
        const titleEl = document.getElementById('activeTopicTitle');
        const descEl = document.getElementById('activeTopicDesc');
        const countBadge = document.getElementById('activeTopicCountBadge');

        if (titleEl) titleEl.innerHTML = `${topic.icon || '📚'} ${escapeHtml(topic.titleVn)} <span class="text-xs text-slate-600 font-normal">(${escapeHtml(topic.titleJp || '')})</span>`;
        if (descEl) descEl.textContent = topic.description || 'Từ vựng tiếng Nhật chuẩn OpenJLPT.';
        if (countBadge) countBadge.textContent = `${Number(topic.vocabCount || 0).toLocaleString('vi-VN')} từ vựng`;
    }

    await loadVocabularies();
}

/**
 * 3. Tải danh sách từ vựng theo chủ đề với AbortController và MemoryCache an toàn
 */
async function loadVocabularies() {
    if (!activeTopicIdentifier) return;
    const grid = document.getElementById('vocabGrid');
    const emptyState = document.getElementById('vocabEmptyState');
    if (!grid) return;

    // 1. Hủy ngay request cũ đang chạy nếu người dùng chuyển filter liên tục
    if (vocabAbortController) {
        vocabAbortController.abort();
    }
    vocabAbortController = new AbortController();

    // 2. Tạo cache key chuẩn hóa
    const cleanKeyword = String(currentSearchKeyword || '').trim().toLowerCase();
    const cacheKey = `${activeTopicIdentifier}_p${currentPage}_kw${cleanKeyword}`;

    // 3. Kiểm tra bộ nhớ Cache: Nếu có dữ liệu hợp lệ, render ngay (0ms latency, zero flicker)
    if (vocabMemoryCache.has(cacheKey)) {
        const cached = vocabMemoryCache.get(cacheKey);
        if (cached && Array.isArray(cached.items) && cached.items.length > 0) {
            currentVocabularies = cached.items;
            totalPages = cached.totalPages || 1;
            totalElements = cached.totalElements || currentVocabularies.length;
            renderVocabGrid(currentVocabularies);
            renderPagination(currentPage, totalPages, totalElements);
            if (emptyState) emptyState.classList.add('hidden');
            return;
        }
    }

    // 4. Hiển thị Skeleton / Loading trạng thái chờ
    grid.innerHTML = '<div class="col-span-full py-16 text-center text-slate-600 font-mono text-xs animate-pulse">⏳ Đang nạp danh sách từ vựng từ kho OpenJLPT...</div>';
    if (emptyState) emptyState.classList.add('hidden');

    try {
        const query = cleanKeyword ? `&keyword=${encodeURIComponent(cleanKeyword)}` : '';
        const url = `/api/learner/topics/${encodeURIComponent(activeTopicIdentifier)}/vocabularies?page=${currentPage}&size=24${query}`;
        
        const res = await fetch(url, {
            headers: getAuthHeaders(),
            signal: vocabAbortController.signal
        });

        if (!res.ok) throw new Error(`Lỗi HTTP ${res.status}`);
        const data = await res.json();

        let items = [];
        let totalPgs = 1;
        let totalElems = 0;

        if (Array.isArray(data)) {
            items = data;
            totalPgs = 1;
            totalElems = data.length;
        } else {
            items = data.content || data.items || [];
            totalPgs = data.totalPages || 1;
            totalElems = data.totalElements != null ? data.totalElements : items.length;
        }

        // 5. Lưu vào Cache nếu có dữ liệu
        if (items.length > 0) {
            vocabMemoryCache.set(cacheKey, {
                items: items,
                totalPages: totalPgs,
                totalElements: totalElems
            });
        }

        currentVocabularies = items;
        totalPages = totalPgs;
        totalElements = totalElems;

        // 6. Xử lý trạng thái rỗng
        if (currentVocabularies.length === 0) {
            grid.innerHTML = '';
            if (emptyState) emptyState.classList.remove('hidden');
            renderPagination(0, 0, 0);
            return;
        }

        // 7. Render danh sách từ vựng
        renderVocabGrid(currentVocabularies);
        renderPagination(currentPage, totalPages, totalElements);

    } catch (err) {
        // Bỏ qua lỗi nếu do chủ động abort khi chuyển filter nhanh
        if (err.name === 'AbortError') return;

        console.error('Lỗi loadVocabularies:', err);
        grid.innerHTML = `<div class="col-span-full py-8 text-center text-xs text-rose-700 bg-rose-50 border border-black rounded-[5px]">⚠️ Không thể tải từ vựng: ${escapeHtml(err.message)}.</div>`;
    }
}

/**
 * 4. Render danh sách thẻ từ vựng chuẩn Retro-Brutalist
 * Theo yêu cầu người dùng:
 * - Bên ngoài chỉ hiển thị: Từ vựng + Furigana + Romaji / Hán Việt + Badge JLPT + Nút nghe + Nghĩa tiếng Việt.
 * - Loại bỏ nút thêm vào bộ thẻ và popup picker bên ngoài.
 * - Khi bấm vào thẻ từ vựng sẽ chuyển sang trang giao diện chi tiết riêng biệt (không dùng chung giao diện với bên ngoài).
 */
function renderVocabGrid(vocabs) {
    const grid = document.getElementById('vocabGrid');
    if (!grid) return;

    grid.innerHTML = vocabs.map(v => {
        const kanjis = v.kanjiCharacters || [];
        const posText = v.posVn || (v.pos && v.pos.length > 0 ? v.pos.join(', ') : 'Từ vựng');

        return `
            <div class="card-retro bg-white rounded-[5px] border border-black p-3.5 shadow-[3px_3px_0px_0px_#0F172A] flex flex-col justify-between hover:translate-x-[-1px] hover:translate-y-[-1px] hover:shadow-[4px_4px_0px_0px_#0F172A] transition duration-150 cursor-pointer"
                 onclick="viewVocabDetail(event, '${v.id}')"
                 title="Bấm để xem chi tiết từ vựng và câu ví dụ">
                <div class="space-y-2">
                    <!-- Header Thẻ: Chữ Hán, Cấp độ JLPT & Nút phát âm -->
                    <div class="flex items-start justify-between gap-2 pb-2 border-b border-black/10">
                        <div class="min-w-0">
                            <!-- Chữ Hán kèm Furigana -->
                            <ruby class="text-xl sm:text-2xl font-bold font-serif text-slate-900 leading-snug">
                                ${escapeHtml(v.word)}
                                <rt class="text-[11px] text-[#E0399A] font-bold font-sans">${escapeHtml(v.furigana || '')}</rt>
                            </ruby>
                            <div class="flex items-center gap-1.5 mt-0.5 text-xs text-slate-500 font-mono">
                                <span class="italic text-slate-700 font-semibold">${escapeHtml(v.romaji || '')}</span>
                                ${v.hanViet ? `<span class="text-slate-300">•</span><span class="text-amber-800 font-bold font-['Itim'] uppercase">${escapeHtml(v.hanViet)}</span>` : ''}
                            </div>
                        </div>
                        <div class="flex flex-col items-end gap-1.5 shrink-0" onclick="event.stopPropagation()">
                            <span class="px-2 py-0.5 text-[10px] font-bold border border-black rounded-[5px] shadow-[1px_1px_0px_0px_#0F172A] ${getJlptColorClass(v.jlptLevel)}">
                                ${escapeHtml(v.jlptLevel || 'N5')}
                            </span>
                            <button type="button" 
                                    onclick="speakJapanese('${escapeJs(v.word)}')"
                                    class="w-7 h-7 rounded-[5px] border border-black bg-white hover:bg-slate-100 flex items-center justify-center text-xs shadow-[1.5px_1.5px_0px_0px_#0F172A] active:translate-x-[0.5px] active:translate-y-[0.5px] active:shadow-none cursor-pointer"
                                    title="Nghe phát âm chuẩn giọng Nhật">
                                🔊
                            </button>
                        </div>
                    </div>

                    <!-- Huy hiệu từ loại -->
                    <div class="pt-0.5">
                        <span class="inline-block px-1.5 py-0.5 rounded-[3px] bg-slate-100 text-slate-700 border border-black text-[10px] font-semibold">
                            🏷️ ${escapeHtml(posText)}
                        </span>
                    </div>

                    <!-- Phần Nghĩa Tiếng Việt Cốt Lõi (100% Tiếng Việt, Không hiển thị Tiếng Anh) -->
                    <div class="py-1">
                        <p class="text-xs font-bold text-[#14532D] leading-relaxed">
                            👉 ${escapeHtml(getDisplayMeaningVn(v))}
                        </p>
                    </div>
                </div>

                <!-- Footer Thao Tác Chuyển Giao Diện Chi Tiết -->
                <div class="pt-2 mt-2 border-t border-black/10 flex items-center justify-between text-xs shrink-0">
                    <span class="text-[11px] font-bold text-[#E0399A] font-['Itim'] flex items-center gap-1">
                        📖 Chi tiết &amp; Ví dụ
                    </span>
                    <button type="button" 
                            onclick="viewVocabDetail(event, '${v.id}')"
                            class="px-2.5 py-1 bg-white hover:bg-[#FFE5EC] text-black border border-black rounded-[4px] shadow-[1.5px_1.5px_0px_0px_#0F172A] font-['Itim'] font-bold transition active:translate-y-0.5 cursor-pointer">
                        Xem chi tiết ➔
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

/**
 * Điều hướng mở trang giao diện chi tiết riêng biệt cho từ vựng
 */
async function viewVocabDetail(event, vocabId) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    const targetId = typeof event === 'string' ? event : vocabId;
    if (!targetId) return;

    // Tìm trong danh sách từ vựng hiện tại trước
    let vocab = currentVocabularies.find(v => String(v.id) === String(targetId));

    if (vocab) {
        showVocabDetailView(vocab);
        return;
    }

    // Nếu chưa có trong danh sách hiện tại (mở trực tiếp từ link ?id=...), nạp từ API
    await fetchVocabDetailById(targetId);
}

/**
 * Nạp chi tiết từ vựng từ Backend API
 */
async function fetchVocabDetailById(vocabId) {
    const detailContent = document.getElementById('vocabDetailContent');
    const listView = document.getElementById('vocabListView');
    const detailView = document.getElementById('vocabDetailView');

    if (listView && detailView) {
        listView.classList.add('hidden');
        detailView.classList.remove('hidden');
    }

    if (detailContent) {
        detailContent.innerHTML = `
            <div class="p-8 text-center text-slate-500 bg-white border border-black rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A]">
                <div class="animate-spin inline-block w-6 h-6 border-2 border-current border-t-transparent text-[#E0399A] rounded-full mb-2"></div>
                <p class="text-xs font-bold font-meta">Đang nạp chi tiết từ vựng...</p>
            </div>
        `;
    }

    try {
        const res = await fetch(`/api/learner/topics/vocabularies/${encodeURIComponent(vocabId)}`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error('Không thể tải chi tiết từ vựng');
        const vocab = await res.json();
        showVocabDetailView(vocab);
    } catch (err) {
        console.error('Lỗi nạp chi tiết từ vựng:', err);
        if (detailContent) {
            detailContent.innerHTML = `
                <div class="p-6 bg-rose-50 border border-black rounded-[5px] text-center text-rose-700 text-xs shadow-[2px_2px_0px_0px_#0F172A]">
                    ⚠️ Không thể tải thông tin chi tiết của từ vựng này. Vui lòng thử lại sau!
                </div>
            `;
        }
    }
}

/**
 * Hiển thị giao diện trang chi tiết riêng biệt (Dedicated Detail View)
 */
function showVocabDetailView(vocab) {
    const listView = document.getElementById('vocabListView');
    const detailView = document.getElementById('vocabDetailView');
    const detailContent = document.getElementById('vocabDetailContent');
    const bcTopic = document.getElementById('detailBreadcrumbTopic');
    const bcWord = document.getElementById('detailBreadcrumbWord');

    const activeTopic = currentTopics.find(t => 
        String(t.code || '').trim() === activeTopicIdentifier || 
        String(t.id || '').trim() === activeTopicIdentifier
    );

    if (bcTopic) bcTopic.textContent = activeTopic ? activeTopic.titleVn : `JLPT ${vocab.jlptLevel || 'N5'}`;
    if (bcWord) bcWord.textContent = vocab.word || 'Từ vựng';

    // Cập nhật URL trình duyệt thân thiện
    try {
        const newUrl = `${window.location.pathname}?id=${encodeURIComponent(vocab.id)}`;
        if (window.location.search !== `?id=${encodeURIComponent(vocab.id)}`) {
            window.history.pushState({ id: vocab.id }, '', newUrl);
        }
    } catch (e) {
        console.warn('History pushState warning:', e);
    }

    if (detailContent) {
        renderVocabDetailPage(vocab);
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
function renderVocabDetailPage(vocab) {
    const container = document.getElementById('vocabDetailContent');
    if (!container) return;

    const kanjis = vocab.kanjiCharacters || [];
    const examples = vocab.examples || [];
    const posText = vocab.posVn || (vocab.pos && vocab.pos.length > 0 ? vocab.pos.join(', ') : 'Từ vựng');

    container.innerHTML = `
        <!-- THẺ HERO TỪ VỰNG -->
        <div class="bg-white border border-black rounded-[5px] p-5 shadow-[4px_4px_0px_0px_#0F172A]">
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-black/10">
                <div>
                    <div class="flex items-center gap-2">
                        <span class="badge-nhai px-2.5 py-1 text-xs font-bold font-meta border border-black rounded-[4px] ${getJlptColorClass(vocab.jlptLevel)}">
                            🌸 JLPT ${escapeHtml(vocab.jlptLevel || 'N5')}
                        </span>
                        <span class="inline-block px-2 py-0.5 rounded-[3px] bg-slate-100 text-slate-700 border border-black text-[11px] font-semibold">
                            🏷️ ${escapeHtml(posText)}
                        </span>
                    </div>
                    <div class="mt-3">
                        <ruby class="text-3xl sm:text-4xl font-bold font-serif text-slate-900 leading-tight">
                            ${escapeHtml(vocab.word)}
                            <rt class="text-sm sm:text-base text-[#E0399A] font-bold font-sans">${escapeHtml(vocab.furigana || '')}</rt>
                        </ruby>
                    </div>
                    <div class="flex items-center gap-2 mt-1 text-sm text-slate-600 font-mono">
                        <span class="italic text-slate-800 font-bold">${escapeHtml(vocab.romaji || '')}</span>
                        ${vocab.hanViet ? `<span class="text-slate-300">•</span><span class="text-amber-900 font-bold font-['Itim'] uppercase text-base">${escapeHtml(vocab.hanViet)}</span>` : ''}
                    </div>
                </div>
                <button type="button" 
                        onclick="speakJapanese('${escapeJs(vocab.word)}')"
                        class="px-4 py-2 bg-white hover:bg-slate-100 text-[#0F172A] border border-black rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A] font-['Itim'] font-bold flex items-center gap-2 self-start sm:self-auto cursor-pointer">
                    <span class="text-base">🔊</span>
                    <span>Nghe Phát Âm Từ Vựng</span>
                </button>
            </div>

            <!-- Ý NGHĨA CỐT LÕI TIẾNG VIỆT 100% (LOẠI BỎ TOÀN BỘ TIẾNG ANH) -->
            <div class="mt-4 p-4 bg-[#F0FDF4] border-2 border-[#15803D] rounded-[5px] shadow-[2px_2px_0px_0px_#15803D]">
                <div class="text-[11px] font-bold font-meta text-[#15803D] uppercase tracking-wide">
                    📌 Ý Nghĩa Tiếng Việt:
                </div>
                <p class="text-base sm:text-lg font-bold text-[#14532D] font-body mt-1 leading-relaxed">
                    👉 ${escapeHtml(getDisplayMeaningVn(vocab))}
                </p>
            </div>
        </div>

        <!-- KHỐI CHỮ HÁN KANJI & TẬP VIẾT NÉT SHODO (NẾU CÓ CHỮ HÁN) -->
        ${kanjis.length > 0 ? `
            <div class="bg-white border border-black rounded-[5px] p-5 shadow-[4px_4px_0px_0px_#0F172A] space-y-3">
                <h2 class="text-sm font-bold font-meta uppercase text-slate-800 flex items-center gap-2 pb-2 border-b border-black/10">
                    <span>🖌️</span>
                    <span>Chữ Hán Trong Từ Vựng &amp; Luyện Viết Shodo (${kanjis.length} chữ)</span>
                </h2>
                <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                    ${kanjis.map(ch => `
                        <div class="flex items-center justify-between p-3 rounded-[5px] border border-black bg-[#FAF9F6] shadow-[2px_2px_0px_0px_#0F172A]">
                            <div class="flex items-center gap-3">
                                <span class="w-10 h-10 rounded-[5px] bg-white border border-black flex items-center justify-center font-serif text-2xl font-bold text-slate-900 shadow-[1px_1px_0px_0px_#0F172A]">
                                    ${escapeHtml(ch)}
                                </span>
                                <div>
                                    <div class="font-['Itim'] font-bold text-sm text-slate-900">Chữ Hán: ${escapeHtml(ch)}</div>
                                    <div class="text-[11px] text-slate-500">Tập viết theo thứ tự nét chuẩn</div>
                                </div>
                            </div>
                            <a href="/learner/practice-canvas?char=${encodeURIComponent(ch)}" 
                               class="text-xs py-1.5 px-3 bg-[#DCFCE7] hover:bg-[#BBF7D0] text-[#064E3B] rounded-[4px] border border-black shadow-[1.5px_1.5px_0px_0px_#000] no-underline font-['Itim'] font-bold flex items-center gap-1 active:translate-y-0.5 cursor-pointer">
                                <span>🖌️</span>
                                <span>Luyện nét</span>
                            </a>
                        </div>
                    `).join('')}
                </div>
            </div>
        ` : ''}

        <!-- KHỐI DANH SÁCH CÂU VÍ DỤ MINH HỌA (100% TIẾNG VIỆT) -->
        <div class="bg-white border border-black rounded-[5px] p-5 shadow-[4px_4px_0px_0px_#0F172A] space-y-4">
            <div class="flex items-center justify-between pb-2 border-b border-black/10">
                <h2 class="text-sm font-bold font-meta uppercase text-slate-800 flex items-center gap-2">
                    <span>💬</span>
                    <span>Câu Ví Dụ Minh Họa Ngữ Cảnh Thực Tế</span>
                </h2>
                <span class="text-[11px] font-bold text-slate-500 font-meta">Đọc Furigana &amp; Nghe Audio</span>
            </div>

            ${(examples.length === 0 && !vocab.exampleJp) ? `
                <p class="text-xs text-slate-500 italic py-4 text-center">Chưa có câu ví dụ minh họa cho từ vựng này.</p>
            ` : `
                <div class="space-y-3">
                    ${examples.length > 0 ? examples.map((ex, idx) => `
                        <div class="bg-[#FAF9F6] border border-black/15 rounded-[5px] p-3.5 space-y-2 hover:border-black transition">
                            <div class="flex items-start justify-between gap-3">
                                <div class="space-y-1">
                                    <div class="text-xs font-bold text-pink-700 font-meta">
                                        Ví dụ #${idx + 1}
                                    </div>
                                    <div class="text-sm sm:text-base font-serif text-slate-900 leading-relaxed">
                                        ${formatFuriganaRuby(ex.furigana || ex.ja)}
                                    </div>
                                </div>
                                <button type="button" 
                                        onclick="speakJapanese('${escapeJs(ex.ja)}')"
                                        class="w-8 h-8 rounded-[4px] border border-black bg-white hover:bg-slate-100 flex items-center justify-center text-xs shadow-[1.5px_1.5px_0px_0px_#0F172A] shrink-0 active:scale-95 cursor-pointer"
                                        title="Nghe câu ví dụ">
                                    🔊
                                </button>
                            </div>
                            <div class="text-xs font-body text-slate-700 pt-1.5 border-t border-dashed border-black/15 pl-2 border-l-2 border-[#15803D]">
                                👉 <span class="font-bold text-[#14532D]">${escapeHtml(getDisplayExampleVn(ex, vocab))}</span>
                            </div>
                        </div>
                    `).join('') : `
                        <div class="bg-[#FAF9F6] border border-black/15 rounded-[5px] p-3.5 space-y-2 hover:border-black transition">
                            <div class="flex items-start justify-between gap-3">
                                <div class="space-y-1">
                                    <div class="text-xs font-bold text-pink-700 font-meta">Ví dụ tiêu biểu</div>
                                    <div class="text-sm sm:text-base font-serif text-slate-900 leading-relaxed">
                                        ${escapeHtml(vocab.exampleJp)}
                                    </div>
                                </div>
                                <button type="button" 
                                        onclick="speakJapanese('${escapeJs(vocab.exampleJp)}')"
                                        class="w-8 h-8 rounded-[4px] border border-black bg-white hover:bg-slate-100 flex items-center justify-center text-xs shadow-[1.5px_1.5px_0px_0px_#0F172A] shrink-0 active:scale-95 cursor-pointer"
                                        title="Nghe câu ví dụ">
                                    🔊
                                </button>
                            </div>
                            <div class="text-xs font-body text-slate-700 pt-1.5 border-t border-dashed border-black/15 pl-2 border-l-2 border-[#15803D]">
                                👉 <span class="font-bold text-[#14532D]">${escapeHtml(getDisplayExampleVn(null, vocab))}</span>
                            </div>
                        </div>
                    `}
                </div>
            `}
        </div>

        <!-- NÚT QUAY LẠI CHÂN TRANG CHI TIẾT -->
        <div class="pt-2 flex justify-start">
            <button type="button" 
                    onclick="closeVocabDetail(event)"
                    class="text-xs py-2 px-4 bg-white hover:bg-slate-100 text-[#0F172A] border border-black rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A] font-['Itim'] font-bold flex items-center gap-1.5 active:translate-y-0.5 cursor-pointer">
                <span>⬅️</span>
                <span>Quay Lại Kho Từ Vựng</span>
            </button>
        </div>
    `;
}

/**
 * Đóng giao diện chi tiết, quay trở về danh sách từ vựng
 */
function closeVocabDetail(event) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    const listView = document.getElementById('vocabListView');
    const detailView = document.getElementById('vocabDetailView');

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
 * Format chuỗi Furigana Tatoeba dạng {漢字|かんじ} thành thẻ Ruby HTML
 */
function formatFuriganaRuby(text) {
    if (!text) return '';
    return text.replace(/\{([^|]+)\|([^}]+)\}/g, '<ruby class="font-bold text-slate-900">$1<rt class="text-[10px] text-pink-700 font-normal font-sans">$2</rt></ruby>');
}

function getJlptColorClass(level) {
    switch ((level || '').toUpperCase()) {
        case 'N5': return 'bg-[#DCFCE7] text-emerald-900';
        case 'N4': return 'bg-[#E0F2FE] text-sky-900';
        case 'N3': return 'bg-[#FEF3C7] text-amber-900';
        case 'N2': return 'bg-[#FFEDD5] text-orange-900';
        case 'N1': return 'bg-[#FFE4E6] text-rose-900';
        default: return 'bg-slate-100 text-slate-800';
    }
}

/**
 * 5. Điều khiển phân trang (Pagination)
 */
function renderPagination(curPage, totalPgs, totalElems) {
    const bar = document.getElementById('vocabPaginationBar');
    const summary = document.getElementById('vocabPaginationSummary');
    const buttonsContainer = document.getElementById('vocabPaginationButtons');

    if (!bar || !buttonsContainer) return;

    if (totalPgs <= 1 && totalElems <= 24) {
        bar.classList.add('hidden');
        return;
    }

    bar.classList.remove('hidden');

    const start = totalElems === 0 ? 0 : (curPage * 24) + 1;
    const end = Math.min((curPage + 1) * 24, totalElems);
    if (summary) {
        summary.textContent = `Hiển thị ${start} - ${end} trên tổng số ${Number(totalElems).toLocaleString('vi-VN')} từ`;
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
    prevBtn.onclick = () => { if (currentPage > 0) { currentPage--; loadVocabularies(); } };
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
        pBtn.onclick = () => { currentPage = p; loadVocabularies(); };
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
    nextBtn.onclick = () => { if (currentPage < totalPgs - 1) { currentPage++; loadVocabularies(); } };
    buttonsContainer.appendChild(nextBtn);
}

/**
 * 6. Phát âm tiếng Nhật Web Speech API
 */
function speakJapanese(text) {
    if (!('speechSynthesis' in window)) {
        if (window.KizunaToast) KizunaToast.warning('Trình duyệt chưa hỗ trợ bộ phát âm Speech API.');
        return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'ja-JP';
    utterance.rate = 0.9;
    window.speechSynthesis.speak(utterance);
}

/**
 * 7. Lắng nghe tìm kiếm với Debounce 300ms & Chặn Form Submit Reload
 */
function handleVocabSearchSubmit(event) {
    if (event) {
        if (typeof event.preventDefault === 'function') event.preventDefault();
        if (typeof event.stopPropagation === 'function') event.stopPropagation();
    }
    const searchInput = document.getElementById('searchVocabInput');
    currentSearchKeyword = searchInput ? searchInput.value.trim() : '';
    currentPage = 0;
    loadVocabularies();
}

function setupEventListeners() {
    const searchInput = document.getElementById('searchVocabInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                currentSearchKeyword = e.target.value.trim();
                currentPage = 0;
                loadVocabularies();
            }, 300);
        });
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                e.stopPropagation();
                handleVocabSearchSubmit(e);
            }
        });
    }

    const filterForm = document.getElementById('vocabFilterForm');
    if (filterForm) {
        filterForm.addEventListener('submit', (e) => {
            e.preventDefault();
            e.stopPropagation();
            handleVocabSearchSubmit(e);
        });
    }
}

/**
 * 8. Quản lý Modal Cất vào bộ thẻ Flashcard (Tạm thời tắt theo yêu cầu giao diện tối giản)
 */
async function loadUserDecks() {
    // Tạm thời vô hiệu hóa
}

function populateDeckSelect() {
    // Tạm thời vô hiệu hóa
}

function openAddToDeckModal(vocabId) {
    if (window.KizunaToast) KizunaToast.info('Tính năng thêm từ vựng vào bộ thẻ đang tạm đóng để nâng cấp.');
}

function closeAddToDeckModal() {
    // Tạm thời vô hiệu hóa
}

async function submitAddVocabToDeck(e) {
    if (e && typeof e.preventDefault === 'function') e.preventDefault();
}

/**
 * 9. Quản lý Modal Chọn chữ Kanji (Tích hợp trực tiếp vào view chi tiết riêng)
 */
function openKanjiPickerModal(word, kanjis) {
    // Tích hợp trực tiếp vào trang chi tiết riêng
}

function closeKanjiPickerModal() {
    // Tạm thời vô hiệu hóa
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function escapeJs(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '\\"');
}

/**
 * 10. Tiện ích bản địa hóa 100% tiếng Việt, loại bỏ hoàn toàn tiếng Anh
 */
function getDisplayMeaningVn(v) {
    if (!v) return 'Từ vựng tiếng Nhật chuẩn JLPT';
    // Ưu tiên meaningVn nếu là tiếng Việt thực sự
    if (v.meaningVn && !isPureEnglish(v.meaningVn)) {
        return v.meaningVn;
    }
    // Nếu meaningVn bị lẫn tiếng Anh hoặc chưa có, dùng nghĩa Hán tự
    if (v.hanViet) {
        return `Nghĩa Hán tự: ${v.hanViet}`;
    }
    return 'Từ vựng tiếng Nhật chuẩn OpenJLPT';
}

function getDisplayExampleVn(ex, vocab) {
    if (ex && ex.vn && !isPureEnglish(ex.vn) && ex.vn !== ex.en) {
        return ex.vn;
    }
    if (vocab && vocab.exampleVn && !isPureEnglish(vocab.exampleVn)) {
        return vocab.exampleVn;
    }
    return 'Câu ví dụ đàm thoại thực tế trong sinh hoạt đời sống Nhật Bản.';
}

function isPureEnglish(str) {
    if (!str) return false;
    const vietnameseRegex = /[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i;
    return !vietnameseRegex.test(str) && /^[a-zA-Z0-9\s.,!?'"()\-;:_]+$/.test(str.trim());
}
