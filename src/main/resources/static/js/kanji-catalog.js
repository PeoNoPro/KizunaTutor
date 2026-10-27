// =========================================================================
// KANJI CATALOG & LEARNING SPACE TAB CONTROLLER (KIZUNATUTOR)
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

/**
 * 1. Chuyển đổi giữa các phân hệ: Bộ Thẻ Của Tôi (Mặc định) vs Luyện Viết vs Minigame Mưa Chữ vs Thư Viện Kanji
 */
function switchMainTab(tab) {
    if (window.IS_WORKSPACE_UNLOCKED === false || document.getElementById('lockedWorkspaceScreen')) {
        return;
    }
    const secPractice = document.getElementById('sectionPracticeTab');
    const secLibrary = document.getElementById('sectionLibraryTab');
    const secFolders = document.getElementById('sectionFoldersTab');
    const secGame = document.getElementById('sectionGameTab');

    // Ẩn tất cả các khu vực làm việc
    if (secPractice) secPractice.style.display = 'none';
    if (secLibrary) secLibrary.style.display = 'none';
    if (secFolders) secFolders.style.display = 'none';
    if (secGame) secGame.style.display = 'none';

    // Cập nhật Breadcrumbs và Tiêu đề trang chuẩn Master Layout
    updatePageHeaderForTab(tab);

    const url = new URL(window.location.href);

    if (tab === 'library') {
        if (secLibrary) secLibrary.style.display = 'block';
        url.searchParams.set('tab', 'library');
        window.history.replaceState({}, '', url.toString());

        if (!isLibLoaded) {
            loadKanjiCatalog(0);
        }
    } else if (tab === 'practice') {
        if (secPractice) secPractice.style.display = 'block';
        url.searchParams.set('tab', 'practice');
        window.history.replaceState({}, '', url.toString());

        // Đồng bộ kích thước và vẽ lại nét cho Kanji Canvas ngay khi phân hệ hiển thị
        setTimeout(() => {
            if (typeof window.setupKanjiCanvasDpi === 'function') {
                window.setupKanjiCanvasDpi();
                if (typeof window.redrawKanjiCanvas === 'function') {
                    window.redrawKanjiCanvas();
                }
            }
        }, 50);

        // Kiểm tra nạp thanh duyệt chữ theo bộ thẻ nếu có folderId
        if (typeof kfmSetupPracticeFolderStrip === 'function') {
            kfmSetupPracticeFolderStrip();
        }
    } else if (tab === 'game') {
        if (secGame) secGame.style.display = 'block';
        url.searchParams.set('tab', 'game');
        window.history.replaceState({}, '', url.toString());

        if (typeof initKanjiRainUI === 'function') {
            initKanjiRainUI();
        }
    } else {
        // Mặc định là phân hệ trung tâm: BỘ THẺ CỦA TÔI
        if (secFolders) secFolders.style.display = 'block';
        url.searchParams.set('tab', 'folders');
        window.history.replaceState({}, '', url.toString());

        if (typeof initKanjiFolderManager === 'function') {
            initKanjiFolderManager();
        }
    }
}

/**
 * Điều hướng quay lại giao diện tra cứu chữ Hán (Tìm Kanji)
 */
function backToKanjiSearch() {
    if (window.IS_WORKSPACE_UNLOCKED === false || document.getElementById('lockedWorkspaceScreen')) {
        return;
    }
    if (typeof openHwSearchModal === 'function') {
        openHwSearchModal();
        return;
    }
    if (window.history.length > 1 && document.referrer && !document.referrer.includes('tab=practice')) {
        window.history.back();
        return;
    }
    window.location.href = '/kanji?action=hwSearch';
}
window.backToKanjiSearch = backToKanjiSearch;

/**
 * Cập nhật động Tiêu đề trang, Breadcrumbs & Action Buttons theo phân hệ đang chọn
 */
function updatePageHeaderForTab(tab) {
    const breadcrumbCurrent = document.getElementById('breadcrumbCurrentSection');
    const pageHeading = document.getElementById('pageMainHeading');
    const pageSubtitle = document.getElementById('pageMainSubtitle');
    const pageHeaderActions = document.getElementById('pageHeaderActions');

    if (!pageHeading) return;

    if (tab === 'practice') {
        if (breadcrumbCurrent) breadcrumbCurrent.textContent = 'Luyện viết nét chữ';
        // 1. Chuẩn hóa tiêu đề: Loại bỏ (Kanji Canvas)
        pageHeading.textContent = '✍️ Luyện Viết Chữ Hán';
        if (pageSubtitle) pageSubtitle.textContent = 'Tự luyện viết chữ Kanji tương tác trên ô Mễ, hỗ trợ nét mờ chuẩn và nhận xét thông minh từ Kizuna Sensei.';
        if (pageHeaderActions) {
            const urlParams = new URLSearchParams(window.location.search);
            const fromParam = urlParams.get('from');
            const folderIdParam = urlParams.get('folderId');
            const sessionFrom = sessionStorage.getItem('kizuna_practice_from');

            // Nút "Về bộ thẻ của tôi" CHỈ ĐƯỢC HIỂN THỊ khi bắt đầu từ Bộ thẻ của tôi
            const isFromDeck = fromParam === 'deck' || !!folderIdParam || sessionFrom === 'deck';
            const isFromSearch = fromParam === 'search' || sessionFrom === 'search';

            // 1. Ẩn hoàn toàn cụm tra cứu (input, nút Tra cứu, nút Vẽ nét tra cứu) khi vào từ Bộ Thẻ
            const searchBarForm = document.getElementById('practiceSearchBarForm');
            if (searchBarForm) {
                searchBarForm.style.display = isFromDeck ? 'none' : 'flex';
            }

            if (isFromDeck) {
                pageHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-outline" id="btnBackToFolders" onclick="switchMainTab('folders')">
                        <span>🔙</span> <span>Về Bộ Thẻ Của Tôi</span>
                    </button>
                `;
            } else if (isFromSearch) {
                pageHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-outline" id="btnBackToSearch" onclick="backToKanjiSearch()">
                        <span>🔙</span> <span>Quay Lại Tra Cứu</span>
                    </button>
                `;
            } else {
                // Tra cứu tự do / truy cập độc lập
                pageHeaderActions.innerHTML = `
                    <button type="button" class="btn btn-outline" id="btnBackToSearch" onclick="backToKanjiSearch()">
                        <span>🔙</span> <span>Quay Lại Tra Cứu</span>
                    </button>
                `;
            }
        }
    } else if (tab === 'game') {
        if (breadcrumbCurrent) breadcrumbCurrent.textContent = 'Mưa chữ Hán';
        pageHeading.textContent = '🎮 Thử Thách Gõ Phím: Mưa Chữ Hán';
        if (pageSubtitle) pageSubtitle.textContent = 'Rèn luyện tốc độ nhận diện mặt chữ và gõ phím Hiragana phản xạ cao theo bộ thẻ đã chọn.';
        if (pageHeaderActions) {
            pageHeaderActions.innerHTML = `
                <button type="button" class="btn btn-secondary" onclick="openGameConfigModal()">
                    <span>⚙️</span> <span>Thiết Lập Ván Học</span>
                </button>
                <button type="button" class="btn btn-outline" onclick="switchMainTab('folders')">
                    <span>🔙</span> <span>Về Bộ Thẻ Của Tôi</span>
                </button>
            `;
        }
    } else if (tab === 'library') {
        if (breadcrumbCurrent) breadcrumbCurrent.textContent = 'Thư viện Kanji N5 - N1';
        pageHeading.textContent = '📚 Thư Viện Chữ Hán JLPT';
        if (pageSubtitle) pageSubtitle.textContent = 'Tra cứu danh mục Hán tự hơn 6.600 chữ chuẩn nét KanjiVG theo các cấp độ JLPT từ N5 đến N1.';
        if (pageHeaderActions) {
            pageHeaderActions.innerHTML = `
                <button type="button" class="btn btn-outline" onclick="openHwSearchModal()">
                    <span>🖌️</span> <span>Vẽ Nét Tra Cứu</span>
                </button>
                <button type="button" class="btn btn-outline" onclick="switchMainTab('folders')">
                    <span>🔙</span> <span>Về Bộ Thẻ Của Tôi</span>
                </button>
            `;
        }
    } else {
        // Trung tâm quản lý Bộ Thẻ
        if (breadcrumbCurrent) breadcrumbCurrent.textContent = 'Bộ thẻ của tôi';
        pageHeading.textContent = '📁 Bộ Thẻ Của Tôi';
        if (pageSubtitle) pageSubtitle.textContent = 'Trung tâm quản lý từ vựng và chọn chế độ luyện tập: Luyện viết nét Shodo hoặc Bắn chữ phản xạ.';
        if (pageHeaderActions) {
            pageHeaderActions.innerHTML = `
                <button type="button" class="btn btn-primary" onclick="kfmOpenCreateModal()">
                    <span>➕</span> <span>Tạo Bộ Thẻ Mới</span>
                </button>
            `;
        }
    }
}

// 2. Chọn cấp độ học JLPT (ALL, N5, N4, N3, N2, N1)
function onSelectJlptLevel(level) {
    libCurrentLevel = level;

    // Cập nhật giao diện nút cấp độ active
    const btnGroup = document.getElementById('libLevelFilterGroup');
    if (btnGroup) {
        btnGroup.querySelectorAll('.level-filter-btn').forEach(btn => {
            if (btn.getAttribute('data-level') === level) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }

    libCurrentPage = 0;
    loadKanjiCatalog(0);
}

// 3. Nhập từ khóa tìm kiếm (Debounce 300ms)
function onLibKeywordInput(value) {
    libKeyword = value.trim();
    clearTimeout(libDebounceTimer);
    libDebounceTimer = setTimeout(() => {
        libCurrentPage = 0;
        loadKanjiCatalog(0);
    }, 300);
}

// 4. Nhập số nét vẽ lọc
function onLibStrokeCountInput(value) {
    libStrokeCount = value ? value.trim() : '';
    clearTimeout(libDebounceTimer);
    libDebounceTimer = setTimeout(() => {
        libCurrentPage = 0;
        loadKanjiCatalog(0);
    }, 300);
}

// 5. Đặt lại toàn bộ bộ lọc về mặc định
function onResetLibFilters() {
    libCurrentLevel = 'ALL';
    libKeyword = '';
    libStrokeCount = '';
    libCurrentPage = 0;

    const inputKw = document.getElementById('libFilterKeyword');
    if (inputKw) inputKw.value = '';
    const inputStrokes = document.getElementById('libFilterStrokes');
    if (inputStrokes) inputStrokes.value = '';

    const btnGroup = document.getElementById('libLevelFilterGroup');
    if (btnGroup) {
        btnGroup.querySelectorAll('.level-filter-btn').forEach(btn => {
            if (btn.getAttribute('data-level') === 'ALL') {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });
    }

    loadKanjiCatalog(0);
}

// 6. Tải dữ liệu chữ Hán từ REST API backend (/api/kanji/catalog)
async function loadKanjiCatalog(page = 0) {
    const grid = document.getElementById('libKanjiGrid');
    const info = document.getElementById('libPaginationInfo');
    const btns = document.getElementById('libPaginationBtns');
    if (!grid) return;

    grid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 48px 0; color: var(--color-text-muted);">
            <div style="font-size: 32px; margin-bottom: 8px;">⏳</div>
            <div>Đang tải dữ liệu chữ Hán từ thư viện...</div>
        </div>
    `;

    try {
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', libPageSize);
        if (libCurrentLevel && libCurrentLevel !== 'ALL') {
            params.set('jlpt', libCurrentLevel);
            params.set('level', libCurrentLevel);
        }
        if (libKeyword) {
            params.set('keyword', libKeyword);
        }
        if (libStrokeCount) {
            params.set('strokeCount', libStrokeCount);
        }

        const resp = await fetch(`/api/kanji/catalog?${params.toString()}`);
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
        const data = await resp.json();

        libCurrentPage = (typeof data.currentPage === 'number') ? data.currentPage : (data.number || 0);
        libTotalPages = data.totalPages || 1;
        libTotalElements = data.totalElements || 0;
        isLibLoaded = true;

        const items = data.items || data.content || [];
        renderKanjiCatalogGrid(items);
        renderPagination(libCurrentPage, libTotalPages, libTotalElements);
    } catch (err) {
        console.error('Lỗi khi tải danh sách chữ Hán:', err);
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 40px 0; color: #DC2626;">
                Không thể kết nối đến máy chủ để tải thư viện chữ Hán. Vui lòng thử lại sau!
            </div>
        `;
    }
}

// 7. Hiển thị lưới Card chữ Hán theo chuẩn Figma
function renderKanjiCatalogGrid(items) {
    const grid = document.getElementById('libKanjiGrid');
    if (!grid) return;

    if (!items || items.length === 0) {
        grid.innerHTML = `
            <div style="grid-column: 1/-1; text-align: center; padding: 48px 20px; color: var(--color-text-muted); background: #FFFFFF; border: 1.5px dashed var(--color-border); border-radius: var(--radius-lg);">
                <div style="font-size: 38px; margin-bottom: 8px;">🔍</div>
                <h4 style="color: var(--color-primary); margin: 0 0 6px 0; font-size: 16px;">Không tìm thấy chữ Hán nào phù hợp</h4>
                <p style="margin: 0; font-size: 13px;">Hãy thử nới lỏng từ khóa hoặc chọn cấp độ JLPT khác.</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = items.map(k => {
        const jlpt = k.jlpt || k.jlptLevel || 'N5';
        const hanViet = k.hanViet || '—';
        const strokes = k.strokeCount ? `${k.strokeCount} nét` : '—';
        const meaning = k.meaning || 'Chưa có ý nghĩa cụ thể.';
        const char = k.kanjiChar || k.kanjiCharacter || '?';
        const kanjiId = k.id || k.kanjiId || '';

        return `
            <div class="kanji-catalog-card" title="Bấm để luyện viết chữ ${char}">
                <div class="card-top">
                    <span class="char-glyph">${escapeHtml(char)}</span>
                    <span class="hw-tile-jlpt">${escapeHtml(jlpt)}</span>
                </div>
                <div class="hanviet-label">${escapeHtml(hanViet)}</div>
                <div class="meta-line">Số nét: ${escapeHtml(strokes)}</div>
                <div class="meaning-line" title="${escapeHtml(meaning)}">${escapeHtml(meaning)}</div>
                <div style="display: flex; gap: 6px; margin-top: auto;">
                    <a href="/kanji?charQuery=${encodeURIComponent(char)}&tab=practice" class="btn-start-practice" style="flex: 1;" title="Luyện viết chữ ${char} trên ô Mễ">
                        <span>✍️</span> <span>Luyện viết</span>
                    </a>
                    <button type="button" class="btn-start-practice" style="padding: 5px 8px; color: var(--color-primary);" title="Thêm vào bộ thẻ cá nhân" onclick="promptAddKanjiToFolder('${kanjiId}', '${escapeHtml(char)}', event)">
                        <span>➕</span>
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// 8. Hiển thị thanh phân trang & tổng số lượng
function renderPagination(page, totalPages, totalElements) {
    const info = document.getElementById('libPaginationInfo');
    const btns = document.getElementById('libPaginationBtns');
    if (!info || !btns) return;

    info.innerHTML = `Hiển thị trang <strong>${page + 1}</strong> / <strong>${totalPages}</strong> (Tổng cộng <strong>${totalElements}</strong> chữ Hán)`;

    if (totalPages <= 1) {
        btns.innerHTML = '';
        return;
    }

    let buttonsHtml = '';

    buttonsHtml += `
        <button type="button" class="tool-btn" ${page === 0 ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : ''} onclick="loadKanjiCatalog(${page - 1})">
            ◀ Trước
        </button>
    `;

    const startP = Math.max(0, page - 2);
    const endP = Math.min(totalPages - 1, page + 2);

    for (let p = startP; p <= endP; p++) {
        const isActive = p === page;
        buttonsHtml += `
            <button type="button" class="tool-btn ${isActive ? 'active' : ''}" style="min-width: 32px; justify-content: center;" onclick="loadKanjiCatalog(${p})">
                ${p + 1}
            </button>
        `;
    }

    buttonsHtml += `
        <button type="button" class="tool-btn" ${page >= totalPages - 1 ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : ''} onclick="loadKanjiCatalog(${page + 1})">
            Sau ▶
        </button>
    `;

    btns.innerHTML = buttonsHtml;
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

// Tự động kích hoạt tab dựa theo URL param (?tab=...) khi tải trang
// MẶC ĐỊNH LÀ BỘ THẺ CỦA TÔI ('folders') THEO YÊU CẦU TỐI GIẢN LEARNING SPACE
document.addEventListener('DOMContentLoaded', () => {
    // Nếu phân hệ đang bị khóa (Locked Screen Workspace), không nạp dữ liệu hay hiển thị nội dung tab
    if (window.IS_WORKSPACE_UNLOCKED === false || document.getElementById('lockedWorkspaceScreen')) {
        return;
    }
    const urlParams = new URLSearchParams(window.location.search);
    const tab = urlParams.get('tab') || 'folders';
    switchMainTab(tab);

    // Nếu có action=game trên URL, tự động chuyển sang chế độ minigame
    if (urlParams.get('action') === 'game') {
        setTimeout(() => {
            if (typeof openGameConfigModal === 'function') {
                openGameConfigModal();
            }
        }, 300);
    }

    // Nếu có action=hwSearch trên URL, tự động mở modal vẽ nét tra cứu chữ Hán
    if (urlParams.get('action') === 'hwSearch') {
        setTimeout(() => {
            if (typeof openHwSearchModal === 'function') {
                openHwSearchModal();
            }
        }, 300);
    }
});
