/**
 * learningSupport.js - Điều khiển giao diện Học Từ Vựng Theo Chủ Đề Đời Sống (OpenJLPT)
 * Đảm bảo map đúng dữ liệu hiển thị, bắt sự kiện chuyển tab và không bị báo rỗng.
 */
let currentTopics = [];
let activeTopicIdentifier = null;
let currentVocabularies = [];
let currentPage = 0;
let totalPages = 1;
let currentSearchKeyword = '';
let selectedVocabForDeck = null;
let userDecks = [];

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
    loadUserDecks();
    setupEventListeners();
}

/**
 * 1. Nạp danh sách chủ đề từ API
 */
async function loadTopics() {
    const topicContainer = document.getElementById('topicPillsContainer');
    try {
        const res = await fetch('/api/learner/topics', { headers: getAuthHeaders() });
        if (!res.ok) throw new Error('Không thể tải danh sách chủ đề');
        currentTopics = await res.json();

        if (!currentTopics || currentTopics.length === 0) {
            topicContainer.innerHTML = '<div class="text-xs text-slate-500 py-2">Chưa có chủ đề nào được nạp.</div>';
            return;
        }

        topicContainer.innerHTML = currentTopics.map((topic, idx) => {
            const isFirst = idx === 0;
            // Lưu cả ID lẫn mã Code trên nút bấm
            const topicKey = topic.code || topic.id;
            return `
                <button type="button" 
                        onclick="selectTopic('${topicKey}')"
                        id="topicBtn_${topicKey}"
                        data-topic-key="${topicKey}"
                        class="topic-pill-btn px-3.5 py-2 rounded-[5px] border border-black font-itim text-xs font-bold transition flex items-center gap-1.5 shadow-[2px_2px_0px_0px_#0F172A] active:translate-y-0.5 cursor-pointer whitespace-nowrap ${
                            isFirst ? 'bg-[#E0399A] text-white' : 'bg-white text-black hover:bg-slate-50'
                        }">
                    <span>${topic.icon || '🌸'}</span>
                    <span>${escapeHtml(topic.titleVn)}</span>
                    <span class="text-[10px] opacity-80 font-normal">(${topic.vocabCount || 10})</span>
                </button>
            `;
        }).join('');

        // Tự động kích hoạt chủ đề đầu tiên
        if (currentTopics.length > 0) {
            const firstKey = currentTopics[0].code || currentTopics[0].id;
            selectTopic(firstKey);
        }
    } catch (err) {
        console.error('Lỗi loadTopics:', err);
        KizunaToast.danger('Có lỗi khi tải danh sách chủ đề học tập.');
    }
}

/**
 * 2. Chọn chủ đề và nạp danh sách từ vựng
 */
async function selectTopic(topicIdentifier) {
    activeTopicIdentifier = topicIdentifier;
    currentPage = 0;
    currentSearchKeyword = '';
    const searchInput = document.getElementById('searchVocabInput');
    if (searchInput) searchInput.value = '';

    // Cập nhật giao diện nút tab đang chọn
    document.querySelectorAll('.topic-pill-btn').forEach(btn => {
        btn.classList.remove('bg-[#E0399A]', 'text-white');
        btn.classList.add('bg-white', 'text-black');
    });

    const activeBtn = document.getElementById(`topicBtn_${topicIdentifier}`) || 
                      document.querySelector(`[data-topic-key="${topicIdentifier}"]`);
    if (activeBtn) {
        activeBtn.classList.remove('bg-white', 'text-black');
        activeBtn.classList.add('bg-[#E0399A]', 'text-white');
    }

    // Hiển thị tiêu đề & mô tả chủ đề
    const topic = currentTopics.find(t => t.code === topicIdentifier || t.id === topicIdentifier);
    if (topic) {
        const titleEl = document.getElementById('activeTopicTitle');
        const descEl = document.getElementById('activeTopicDesc');
        if (titleEl) titleEl.innerHTML = `${topic.icon || '🌸'} ${escapeHtml(topic.titleVn)} <span class="text-xs text-slate-500 font-normal">(${escapeHtml(topic.titleJp || '')})</span>`;
        if (descEl) descEl.textContent = topic.description || 'Học từ vựng giao tiếp thực tế.';
    }

    await loadVocabularies();
}

/**
 * 3. Tải danh sách từ vựng theo chủ đề & từ khóa
 */
async function loadVocabularies() {
    if (!activeTopicIdentifier) return;
    const grid = document.getElementById('vocabGrid');
    const emptyState = document.getElementById('vocabEmptyState');
    grid.innerHTML = '<div class="col-span-full py-16 text-center text-slate-500 font-meta text-xs animate-pulse">🌸 Đang nạp danh sách từ vựng từ OpenJLPT...</div>';
    emptyState.classList.add('hidden');

    try {
        const query = currentSearchKeyword ? `&search=${encodeURIComponent(currentSearchKeyword)}` : '';
        const url = `/api/learner/topics/${encodeURIComponent(activeTopicIdentifier)}/vocabularies?page=${currentPage}&size=24${query}`;
        const res = await fetch(url, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`Lỗi HTTP ${res.status}`);
        const data = await res.json();

        // Xử lý linh hoạt cả mảng trực tiếp lẫn Spring Page { content: [...] }
        if (Array.isArray(data)) {
            currentVocabularies = data;
            totalPages = 1;
        } else {
            currentVocabularies = data.content || data.items || [];
            totalPages = data.totalPages || 1;
        }

        if (currentVocabularies.length === 0) {
            grid.innerHTML = '';
            emptyState.classList.remove('hidden');
            renderPagination(0, 0);
            return;
        }

        renderVocabGrid(currentVocabularies);
        renderPagination(currentPage, totalPages);
    } catch (err) {
        console.error('Lỗi loadVocabularies:', err);
        grid.innerHTML = `<div class="col-span-full py-8 text-center text-xs text-rose-600 bg-rose-50 border border-black rounded-[5px]">⚠️ Không thể tải từ vựng: ${err.message}. Đang thử kết nối lại...</div>`;
    }
}

/**
 * 4. Render danh sách thẻ từ vựng chuẩn Retro-Brutalist
 */
function renderVocabGrid(vocabs) {
    const grid = document.getElementById('vocabGrid');
    grid.innerHTML = vocabs.map(v => {
        const kanjis = v.kanjiCharacters || [];
        const hasKanji = kanjis.length > 0;

        return `
            <div class="card-retro bg-white rounded-[5px] border border-black p-3.5 shadow-[3px_3px_0px_0px_#0F172A] flex flex-col justify-between hover:translate-x-[-1px] hover:translate-y-[-1px] hover:shadow-[4px_4px_0px_0px_#0F172A] transition duration-150">
                <div>
                    <!-- Header Thẻ: Chữ Hán, JLPT badge & nút phát âm -->
                    <div class="flex items-start justify-between gap-2 pb-2 border-b border-black/10">
                        <div class="min-w-0">
                            <!-- Chữ Hán kèm Furigana -->
                            <ruby class="text-xl sm:text-2xl font-bold font-serif text-black leading-snug">
                                ${escapeHtml(v.word)}
                                <rt class="text-[10px] text-pink-600 font-normal font-sans">${escapeHtml(v.furigana || '')}</rt>
                            </ruby>
                            <div class="flex items-center gap-1.5 mt-0.5 text-[11px] text-slate-500 font-meta">
                                <span class="italic text-slate-700 font-semibold">${escapeHtml(v.romaji || '')}</span>
                                ${v.hanViet ? `<span class="text-slate-400">•</span><span class="text-amber-800 font-bold font-itim uppercase">${escapeHtml(v.hanViet)}</span>` : ''}
                            </div>
                        </div>
                        <div class="flex flex-col items-end gap-1.5 shrink-0">
                            <span class="badge-nhai px-1.5 py-0.5 text-[10px] font-bold font-meta border border-black rounded-[4px] ${getJlptColorClass(v.jlptLevel)}">
                                ${escapeHtml(v.jlptLevel || 'N5')}
                            </span>
                            <button type="button" 
                                    onclick="speakJapanese('${escapeJs(v.word)}')"
                                    class="w-7 h-7 rounded-[4px] border border-black bg-white hover:bg-slate-100 flex items-center justify-center text-xs shadow-[1.5px_1.5px_0px_0px_#0F172A] active:scale-95 cursor-pointer"
                                    title="Nghe phát âm chuẩn giọng Nhật">
                                🔊
                            </button>
                        </div>
                    </div>

                    <!-- Phần Nghĩa Tiếng Việt -->
                    <div class="py-2">
                        <p class="text-xs font-bold text-[#14532D] font-body leading-relaxed">
                            👉 ${escapeHtml(v.meaningVn)}
                        </p>
                    </div>

                    <!-- Câu Ví Dụ Ngữ Cảnh -->
                    ${v.exampleJp ? `
                        <div class="bg-[#FAF9F6] border border-black/10 rounded-[4px] p-2 mb-2 text-[11px] leading-relaxed">
                            <div class="font-serif text-slate-800 flex items-center gap-1">
                                <span class="text-slate-400">💬</span>
                                <span>${escapeHtml(v.exampleJp)}</span>
                            </div>
                            ${v.exampleVn ? `<div class="text-slate-500 text-[10px] font-body mt-0.5 pl-4">${escapeHtml(v.exampleVn)}</div>` : ''}
                        </div>
                    ` : ''}
                </div>

                <!-- Footer Thao Tác: Cất vào bộ thẻ & Luyện viết Shodo -->
                <div class="pt-2 border-t border-black/10 grid ${hasKanji ? 'grid-cols-2' : 'grid-cols-1'} gap-1.5">
                    <button type="button" 
                            onclick="openAddToDeckModal('${v.id}')"
                            class="btn-retro text-xs py-1.5 px-2 bg-white hover:bg-amber-50 text-black flex items-center justify-center gap-1 rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold transition active:translate-y-0.5 cursor-pointer truncate">
                        <span>📁</span>
                        <span>Cất Vào Thẻ</span>
                    </button>

                    ${hasKanji ? `
                        ${kanjis.length === 1 ? `
                            <a href="/learner/practice-canvas?char=${encodeURIComponent(kanjis[0])}" 
                               class="btn-retro text-xs py-1.5 px-2 bg-[#15803D] hover:bg-[#166534] text-white flex items-center justify-center gap-1 rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] no-underline font-itim font-bold transition active:translate-y-0.5 truncate"
                               title="Luyện viết nét Shodo chữ [${kanjis[0]}]">
                                <span>🖌️</span>
                                <span>Viết [${escapeHtml(kanjis[0])}]</span>
                            </a>
                        ` : `
                            <button type="button" 
                                    onclick="openKanjiPickerModal('${escapeJs(v.word)}', ${JSON.stringify(kanjis).replace(/"/g, '&quot;')})"
                                    class="btn-retro text-xs py-1.5 px-2 bg-[#15803D] hover:bg-[#166534] text-white flex items-center justify-center gap-1 rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold transition active:translate-y-0.5 cursor-pointer truncate"
                                    title="Chọn chữ Kanji để luyện viết Shodo">
                                <span>🖌️</span>
                                <span>Luyện Viết (${kanjis.length})</span>
                            </button>
                        `}
                    ` : ''}
                </div>
            </div>
        `;
    }).join('');
}

/**
 * 5. Điều hướng phân trang
 */
function renderPagination(page, total) {
    const bar = document.getElementById('vocabPaginationBar');
    if (!bar) return;
    if (total <= 1) {
        bar.classList.add('hidden');
        return;
    }
    bar.classList.remove('hidden');
    document.getElementById('vocabPageInfo').textContent = `Trang ${page + 1} / ${total}`;
    document.getElementById('vocabPrevBtn').disabled = (page <= 0);
    document.getElementById('vocabNextBtn').disabled = (page >= total - 1);
}

function prevVocabPage() {
    if (currentPage > 0) {
        currentPage--;
        loadVocabularies();
    }
}

function nextVocabPage() {
    if (currentPage < totalPages - 1) {
        currentPage++;
        loadVocabularies();
    }
}

/**
 * 6. Phát âm chuẩn giọng Nhật
 */
function speakJapanese(text) {
    if (!('speechSynthesis' in window)) {
        KizunaToast.warning('Trình duyệt của bạn chưa hỗ trợ phát âm âm thanh.');
        return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'ja-JP';
    utterance.rate = 0.85;
    window.speechSynthesis.speak(utterance);
}

/**
 * 7. Tải danh sách bộ thẻ người dùng
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
    const select = document.getElementById('saveDeckSelect');
    if (!select) return;
    if (userDecks.length === 0) {
        select.innerHTML = '<option value="">-- Chưa có bộ thẻ (Nhập tên bên dưới để tạo) --</option>';
        return;
    }
    select.innerHTML = '<option value="">-- Chọn bộ thẻ có sẵn --</option>' + 
        userDecks.map(d => `<option value="${d.folderId}">${escapeHtml(d.folderName)} (🌸 ${d.kanjiCount || 0} chữ)</option>`).join('');
}

/**
 * 8. Mở Modal Cất Vào Bộ Thẻ
 */
function openAddToDeckModal(vocabId) {
    const vocab = currentVocabularies.find(v => String(v.id) === String(vocabId));
    if (!vocab) return;
    selectedVocabForDeck = vocab;

    document.getElementById('modalDeckVocabWord').textContent = vocab.word;
    document.getElementById('modalDeckVocabMeaning').textContent = vocab.meaningVn;
    const kanjiList = vocab.kanjiCharacters || [];
    document.getElementById('modalDeckVocabKanjis').textContent = kanjiList.length > 0 ? kanjiList.join(', ') : 'Không có chữ Hán';

    document.getElementById('newDeckNameInput').value = '';
    renderDeckSelectOptions();

    document.getElementById('modalAddToDeck').style.display = 'flex';
}

function closeAddToDeckModal() {
    document.getElementById('modalAddToDeck').style.display = 'none';
    selectedVocabForDeck = null;
}

/**
 * 9. Gửi yêu cầu lưu từ vựng vào bộ thẻ
 */
async function submitAddVocabToDeck(e) {
    e.preventDefault();
    if (!selectedVocabForDeck) return;

    const deckId = document.getElementById('saveDeckSelect').value;
    const newDeckName = document.getElementById('newDeckNameInput').value.trim();

    if (!deckId && !newDeckName) {
        KizunaToast.warning('Vui lòng chọn bộ thẻ có sẵn hoặc nhập tên bộ thẻ mới bạn nhé! 🌸');
        return;
    }

    const btn = document.getElementById('btnSubmitAddToDeck');
    btn.disabled = true;
    btn.textContent = 'Đang lưu...';

    try {
        const payload = {
            deckId: deckId ? deckId : null,
            newDeckName: newDeckName ? newDeckName : null
        };

        const res = await fetch(`/api/learner/topics/vocabularies/${selectedVocabForDeck.id}/add-to-deck`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errJson = await res.json().catch(() => ({}));
            throw new Error(errJson.message || 'Lỗi khi lưu từ vựng vào bộ thẻ');
        }

        const data = await res.json();
        closeAddToDeckModal();
        KizunaToast.success(data.message || 'Đã lưu từ vựng vào bộ thẻ thành công! 🌸');

        loadUserDecks();
    } catch (err) {
        console.error('Lỗi submitAddVocabToDeck:', err);
        KizunaToast.danger(err.message || 'Có lỗi kết nối máy chủ khi lưu vào bộ thẻ.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span>💾</span><span>Lưu Ngay</span>';
    }
}

/**
 * 10. Multi-Kanji Picker Modal
 */
function openKanjiPickerModal(word, kanjis) {
    document.getElementById('pickerWordTitle').textContent = `Từ vựng: ${word}`;
    const list = document.getElementById('pickerKanjiList');
    list.innerHTML = kanjis.map(ch => `
        <a href="/learner/practice-canvas?char=${encodeURIComponent(ch)}" 
           class="p-3 bg-white border border-black rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-between hover:bg-pink-50 transition active:scale-95 no-underline text-black">
            <div class="flex items-center gap-3">
                <span class="text-3xl font-bold font-serif text-[#15803D]">${escapeHtml(ch)}</span>
                <span class="text-xs font-itim font-semibold">Tập viết nét chữ [${escapeHtml(ch)}]</span>
            </div>
            <span class="btn-retro text-xs py-1 px-2.5 bg-[#15803D] text-white rounded-[4px] border border-black shadow-[1px_1px_0px_0px_#000]">
                Vào Vẽ 🖌️
            </span>
        </a>
    `).join('');

    document.getElementById('modalKanjiPicker').style.display = 'flex';
}

function closeKanjiPickerModal() {
    document.getElementById('modalKanjiPicker').style.display = 'none';
}

function setupEventListeners() {
    const searchInput = document.getElementById('searchVocabInput');
    if (searchInput) {
        let debounceTimer = null;
        searchInput.addEventListener('input', (e) => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                currentSearchKeyword = e.target.value.trim();
                currentPage = 0;
                loadVocabularies();
            }, 300);
        });
    }
}

function getJlptColorClass(level) {
    switch (level) {
        case 'N5': return 'bg-emerald-100 text-emerald-800';
        case 'N4': return 'bg-sky-100 text-sky-800';
        case 'N3': return 'bg-amber-100 text-amber-800';
        case 'N2': return 'bg-orange-100 text-orange-800';
        case 'N1': return 'bg-rose-100 text-rose-800';
        default: return 'bg-slate-100 text-slate-800';
    }
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
