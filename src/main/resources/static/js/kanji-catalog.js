// =========================================================================
// KANJI CATALOG & JLPT LEVEL LIBRARY BROWSER FOR LEARNERS
// KizunaTutor - Danh Sách Chữ Hán Theo Cấp Độ N (N5 - N1) Dành Cho Học Viên
// =========================================================================

let libCurrentPage = 0;
let libPageSize = 24;
let libTotalPages = 1;
let libTotalElements = 0;
let libCurrentLevel = 'ALL';
let libKeyword = '';
let libStrokeCount = '';
let libDebounceTimer = null;
let isLibLoaded = false;

// 1. Chuyển đổi giữa 2 Tab: Luyện Viết (Canvas) vs Danh Sách Kanji (N5 - N1)
function switchMainTab(tab) {
    const secPractice = document.getElementById('sectionPracticeTab');
    const secLibrary = document.getElementById('sectionLibraryTab');
    const btnPractice = document.getElementById('tabBtnPractice');
    const btnLibrary = document.getElementById('tabBtnLibrary');

    if (tab === 'library') {
        if (secPractice) secPractice.style.display = 'none';
        if (secLibrary) secLibrary.style.display = 'block';
        if (btnPractice) btnPractice.classList.remove('active');
        if (btnLibrary) btnLibrary.classList.add('active');

        // Cập nhật URL query param không reload trang
        const url = new URL(window.location.href);
        url.searchParams.set('tab', 'library');
        window.history.replaceState({}, '', url.toString());

        if (!isLibLoaded) {
            loadKanjiCatalog(0);
        }
    } else {
        if (secPractice) secPractice.style.display = 'block';
        if (secLibrary) secLibrary.style.display = 'none';
        if (btnPractice) btnPractice.classList.add('active');
        if (btnLibrary) btnLibrary.classList.remove('active');

        const url = new URL(window.location.href);
        url.searchParams.delete('tab');
        window.history.replaceState({}, '', url.toString());
    }
}

// 2. Chọn cấp độ học JLPT (ALL, N5, N4, N3, N2, N1)
function onSelectJlptLevel(level) {
    libCurrentLevel = level;

    // Cập nhật giao diện nút cấp độ active
    const pills = document.querySelectorAll('.level-filter-btn');
    pills.forEach(btn => {
        if (btn.getAttribute('data-level') === level) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    loadKanjiCatalog(0);
}

// 3. Nhập từ khóa tìm kiếm (Debounce 300ms)
function onLibKeywordInput(val) {
    libKeyword = (val || '').trim();
    clearTimeout(libDebounceTimer);
    libDebounceTimer = setTimeout(() => {
        loadKanjiCatalog(0);
    }, 300);
}

// 4. Nhập số nét vẽ lọc chữ
function onLibStrokeCountInput(val) {
    libStrokeCount = (val || '').trim();
    clearTimeout(libDebounceTimer);
    libDebounceTimer = setTimeout(() => {
        loadKanjiCatalog(0);
    }, 300);
}

// 5. Đặt lại bộ lọc về mặc định
function onResetLibFilters() {
    libCurrentLevel = 'ALL';
    libKeyword = '';
    libStrokeCount = '';

    const kwInput = document.getElementById('libFilterKeyword');
    if (kwInput) kwInput.value = '';

    const strokesInput = document.getElementById('libFilterStrokes');
    if (strokesInput) strokesInput.value = '';

    const pills = document.querySelectorAll('.level-filter-btn');
    pills.forEach(btn => {
        if (btn.getAttribute('data-level') === 'ALL') {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });

    loadKanjiCatalog(0);
}

// 6. Tải dữ liệu chữ Hán từ Backend qua API phân trang
async function loadKanjiCatalog(page = 0) {
    libCurrentPage = page;
    const grid = document.getElementById('libKanjiGrid');
    if (!grid) return;

    // Hiển thị skeleton loading
    renderCatalogSkeleton();

    try {
        const params = new URLSearchParams();
        params.append('page', libCurrentPage);
        params.append('size', libPageSize);
        if (libCurrentLevel && libCurrentLevel !== 'ALL') {
            params.append('jlpt', libCurrentLevel);
        }
        if (libKeyword) {
            params.append('keyword', libKeyword);
        }
        if (libStrokeCount) {
            params.append('strokeCount', libStrokeCount);
        }

        const res = await fetch(`/api/kanji/search?${params.toString()}`);
        if (!res.ok) throw new Error('Không thể tải danh sách Kanji từ hệ thống.');

        const data = await res.json();
        const items = data.items || [];
        libTotalElements = data.totalElements || 0;
        libTotalPages = data.totalPages || 1;
        libCurrentPage = data.currentPage || 0;
        isLibLoaded = true;

        renderCatalogGrid(items);
        renderCatalogPagination();
    } catch (err) {
        console.error('Lỗi tải danh mục Kanji:', err);
        grid.innerHTML = `
            <div style="grid-column: 1/-1; padding: 40px 20px; text-align: center; color: #EF4444;">
                <div style="font-size: 32px; margin-bottom: 8px;">⚠️</div>
                <div style="font-weight: 600;">Không thể tải dữ liệu chữ Hán: ${escapeHtml(err.message)}</div>
                <button type="button" class="tool-btn" onclick="loadKanjiCatalog(0)" style="margin-top: 10px;">Thử lại</button>
            </div>
        `;
    }
}

// 7. Hiển thị khung chờ Skeleton
function renderCatalogSkeleton() {
    const grid = document.getElementById('libKanjiGrid');
    if (!grid) return;
    grid.innerHTML = Array.from({ length: 12 }).map(() => `
        <div class="kanji-catalog-card" style="opacity: 0.6; pointer-events: none; min-height: 140px;">
            <div style="height: 36px; background: #E2E8F0; border-radius: 6px; margin-bottom: 10px;"></div>
            <div style="height: 14px; background: #E2E8F0; border-radius: 4px; width: 65%; margin-bottom: 8px;"></div>
            <div style="height: 12px; background: #E2E8F0; border-radius: 4px; width: 85%;"></div>
        </div>
    `).join('');
}

// 8. Render lưới thẻ chữ Hán
function renderCatalogGrid(items) {
    const grid = document.getElementById('libKanjiGrid');
    if (!grid) return;

    if (!items || items.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; padding: 50px 20px; text-align: center; color: var(--color-text-muted); background: #FAF9F6; border: 1.5px dashed var(--color-border); border-radius: var(--radius-md);">
                <div style="font-size: 40px; margin-bottom: 8px;">🔍</div>
                <h4 style="margin: 0 0 6px 0; color: var(--color-primary); font-size: 16px;">Không tìm thấy chữ Kanji phù hợp</h4>
                <p style="margin: 0; font-size: 13px;">Hãy thử tìm với từ khóa khác hoặc chuyển sang cấp độ học khác.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = items.map(k => {
        const jlptClass = k.jlpt ? `badge-jlpt-${k.jlpt.toLowerCase()}` : '';
        return `
            <div class="kanji-catalog-card" onclick="selectKanjiForPractice('${escapeHtml(k.kanjiChar)}')" title="Bấm để luyện viết chữ ${escapeHtml(k.kanjiChar)}">
                <div class="card-top">
                    <span class="char-glyph">${escapeHtml(k.kanjiChar)}</span>
                    <span class="hw-tile-jlpt ${jlptClass}">${escapeHtml(k.jlpt || 'N5')}</span>
                </div>
                <div class="hanviet-label">${escapeHtml(k.hanViet || 'HÁN TỰ')}</div>
                <div class="meta-line">${k.strokeCount || '?'} nét • On: ${escapeHtml(k.onyomi || '-')}</div>
                <div class="meaning-line" title="${escapeHtml(k.meaning || '')}">${escapeHtml(k.meaning || 'Chữ Hán hệ thống')}</div>
                <div class="btn-start-practice">
                    <span>✍️</span> <span>Luyện viết</span>
                </div>
            </div>
        `;
    }).join('');
}

// 9. Render thanh phân trang
function renderCatalogPagination() {
    const infoEl = document.getElementById('libPaginationInfo');
    const btnsEl = document.getElementById('libPaginationBtns');
    if (!infoEl || !btnsEl) return;

    if (libTotalElements === 0) {
        infoEl.innerText = '0 chữ Hán';
        btnsEl.innerHTML = '';
        return;
    }

    const startIdx = libCurrentPage * libPageSize + 1;
    const endIdx = Math.min((libCurrentPage + 1) * libPageSize, libTotalElements);
    infoEl.innerHTML = `Hiển thị <strong>${startIdx} - ${endIdx}</strong> trên tổng số <strong>${libTotalElements}</strong> chữ Kanji`;

    let html = '';
    // Nút Trước
    html += `
        <button type="button" class="tool-btn" ${libCurrentPage <= 0 ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : ''} onclick="loadKanjiCatalog(${libCurrentPage - 1})">
            ◀ Trước
        </button>
    `;

    // Tính toán dải số trang hiển thị (tối đa 5 nút)
    const maxButtons = 5;
    let startPage = Math.max(0, libCurrentPage - Math.floor(maxButtons / 2));
    let endPage = Math.min(libTotalPages - 1, startPage + maxButtons - 1);
    if (endPage - startPage < maxButtons - 1) {
        startPage = Math.max(0, endPage - maxButtons + 1);
    }

    if (startPage > 0) {
        html += `<button type="button" class="tool-btn" onclick="loadKanjiCatalog(0)">1</button>`;
        if (startPage > 1) {
            html += `<span style="padding: 0 4px; color: var(--color-text-muted);">...</span>`;
        }
    }

    for (let p = startPage; p <= endPage; p++) {
        const isActive = (p === libCurrentPage);
        html += `
            <button type="button" class="tool-btn ${isActive ? 'active' : ''}" onclick="loadKanjiCatalog(${p})">
                ${p + 1}
            </button>
        `;
    }

    if (endPage < libTotalPages - 1) {
        if (endPage < libTotalPages - 2) {
            html += `<span style="padding: 0 4px; color: var(--color-text-muted);">...</span>`;
        }
        html += `<button type="button" class="tool-btn" onclick="loadKanjiCatalog(${libTotalPages - 1})">${libTotalPages}</button>`;
    }

    // Nút Sau
    html += `
        <button type="button" class="tool-btn" ${libCurrentPage >= libTotalPages - 1 ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : ''} onclick="loadKanjiCatalog(${libCurrentPage + 1})">
            Sau ▶
        </button>
    `;

    btnsEl.innerHTML = html;
}

// 10. Chọn chữ để luyện viết -> Chuyển sang Tab Luyện Viết với chữ đó
function selectKanjiForPractice(kanjiChar) {
    if (!kanjiChar) return;
    // Chuyển trang đến /kanji?charQuery={kanjiChar} để tải đúng chữ và nét vẽ vào Canvas
    window.location.href = `/kanji?charQuery=${encodeURIComponent(kanjiChar)}`;
}

// Helper escape HTML
function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Tự động kích hoạt tab dựa theo URL param (?tab=library) khi tải trang
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const tab = urlParams.get('tab');
    if (tab === 'library') {
        switchMainTab('library');
    }
});
