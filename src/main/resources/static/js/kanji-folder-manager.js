/**
 * ====================================================================================================
 * KIZUNA TUTOR - QUẢN LÝ BỘ THẺ HỌC TẬP CHỮ HÁN (KANJI FOLDER WORKSPACE & MANAGER)
 * ====================================================================================================
 * Cung cấp đầy đủ các tính năng:
 * 1. Hiển thị danh sách bộ thẻ của người dùng (tự do CRUD, không áp đặt thư mục mặc định).
 * 2. Tạo mới bộ thẻ từ số lượng 0 chữ.
 * 3. Đổi tên, chỉnh sửa mô tả, màu sắc bộ thẻ (PUT /api/user/kanji-folders/{id}).
 * 4. Xóa bất kỳ bộ thẻ nào kèm hộp thoại xác nhận an toàn (DELETE /api/user/kanji-folders/{id}).
 * 5. Xem chi tiết danh sách các chữ Hán trong bộ thẻ (GET /api/user/kanji-folders/{id}).
 * 6. Tìm kiếm & Thêm chữ Kanji trực tiếp tại chỗ (In-place Search & Add with Auto-complete Dropdown).
 * 7. Xóa chữ Kanji khỏi bộ thẻ (DELETE /api/user/kanji-folders/{id}/items/{kanjiId}).
 * 8. Nạp ngẫu nhiên theo cấp độ JLPT (N1-N5) với cơ chế tự động loại trừ các chữ đã có và chống kẹt nút (Lock Flag).
 * 9. Chuyển thẳng sang Minigame Mưa Chữ Hán hoặc Bảng Luyện Viết với bộ thẻ đã chọn.
 * ====================================================================================================
 */

let kfmFolders = [];
let kfmActiveFolderId = null;
let kfmActiveFolderDetail = null;
let kfmSearchDebounceTimer = null;
let kfmIsAddingRandom = false; // Cờ khóa trạng thái chống bấm lặp (Lock Flag)

// Quản lý Phân trang & Tìm kiếm bộ thẻ cột trái
let kfmFolderSearchQuery = '';
let kfmFolderCurrentPage = 0;
const kfmFolderPageSize = 8;
let kfmFolderDebounceTimer = null;

function kfmOnFolderSearchInput(val) {
    clearTimeout(kfmFolderDebounceTimer);
    kfmFolderDebounceTimer = setTimeout(() => {
        kfmFolderSearchQuery = (val || '').trim().toLowerCase();
        kfmFolderCurrentPage = 0;
        kfmRenderFolderList();
    }, 200);
}

/**
 * 1. Khởi tạo Không Gian Quản Lý Bộ Thẻ
 */
async function initKanjiFolderManager() {
    await kfmLoadAllFolders();
}

/**
 * 2. Tải toàn bộ danh sách bộ thẻ của người dùng
 */
async function kfmLoadAllFolders(selectFolderId = null) {
    const listContainer = document.getElementById('kfmFolderList');
    if (!listContainer) return;

    listContainer.innerHTML = `
        <div style="text-align: center; padding: 32px 16px; color: var(--color-text-muted);">
            <div style="font-size: 28px; margin-bottom: 8px;">⏳</div>
            <div style="font-size: 13.5px; font-weight: 600;">Đang tải danh sách bộ thẻ...</div>
        </div>
    `;

    try {
        const resp = await fetch('/api/user/kanji-folders');
        if (resp.ok) {
            kfmFolders = await resp.json();
            kfmRenderFolderList(kfmFolders);

            // Cập nhật tổng số bộ thẻ lên tiêu đề
            const countBadge = document.getElementById('kfmTotalFoldersCount');
            if (countBadge) {
                countBadge.textContent = `${kfmFolders.length} bộ thẻ`;
            }

            // Chọn thư mục chỉ định hoặc thư mục đầu tiên (nếu có)
            if (selectFolderId && kfmFolders.some(f => f.folderId === selectFolderId)) {
                await kfmSelectFolder(selectFolderId);
            } else if (kfmFolders.length > 0) {
                await kfmSelectFolder(kfmFolders[0].folderId);
            } else {
                kfmActiveFolderId = null;
                kfmActiveFolderDetail = null;
                kfmRenderEmptyFolderWorkspace();
            }
        } else {
            listContainer.innerHTML = `
                <div style="text-align: center; padding: 24px; color: #DC2626;">
                    Không thể nạp danh sách bộ thẻ. Vui lòng tải lại trang!
                </div>
            `;
        }
    } catch (err) {
        console.error('Lỗi khi nạp danh sách bộ thẻ:', err);
        listContainer.innerHTML = `
            <div style="text-align: center; padding: 24px; color: #DC2626;">
                Lỗi kết nối máy chủ khi nạp bộ thẻ.
            </div>
        `;
    }
}

/**
 * 3. Render danh sách các Card thư mục ở cột trái (Tối giản Card, chỉ giữ lại Icon, Tên, Ghi chú, Số lượng chữ & Phân trang)
 */
function kfmRenderFolderList(foldersInput) {
    if (Array.isArray(foldersInput)) {
        kfmFolders = foldersInput;
    }
    const listContainer = document.getElementById('kfmFolderList');
    const paginationContainer = document.getElementById('kfmFolderPagination');
    if (!listContainer) return;

    // Lọc theo từ khóa tìm kiếm (tên hoặc ghi chú)
    const filtered = kfmFolders.filter(f => {
        if (!kfmFolderSearchQuery) return true;
        const name = (f.folderName || '').toLowerCase();
        const desc = (f.description || '').toLowerCase();
        return name.includes(kfmFolderSearchQuery) || desc.includes(kfmFolderSearchQuery);
    });

    if (!filtered || filtered.length === 0) {
        if (kfmFolders.length === 0) {
            listContainer.innerHTML = `
                <div style="text-align: center; padding: 24px 16px; color: var(--color-text-muted); background: #FAF9F6; border: 1.5px dashed var(--color-border); border-radius: var(--radius-md);">
                    <div style="font-size: 32px; margin-bottom: 8px;">📂</div>
                    <div style="font-weight: 700; color: #1A202C; margin-bottom: 4px;">Chưa Có Bộ Thẻ Nào</div>
                    <div style="font-size: 12.5px;">Bấm nút "+ Tạo Mới" để bắt đầu bộ sưu tập đầu tiên của bạn!</div>
                </div>
            `;
        } else {
            listContainer.innerHTML = `
                <div style="text-align: center; padding: 20px 12px; color: var(--color-text-muted); background: #FAF9F6; border: 1px dashed var(--color-border); border-radius: var(--radius-md);">
                    <div style="font-size: 24px; margin-bottom: 6px;">🔍</div>
                    <div style="font-size: 13px; font-weight: 600; color: var(--color-text);">Không tìm thấy bộ thẻ</div>
                    <div style="font-size: 12px; margin-top: 2px;">Không có kết quả khớp với "${escapeHtml(kfmFolderSearchQuery)}"</div>
                </div>
            `;
        }
        if (paginationContainer) paginationContainer.innerHTML = '';
        return;
    }

    const totalPages = Math.ceil(filtered.length / kfmFolderPageSize) || 1;
    if (kfmFolderCurrentPage >= totalPages) kfmFolderCurrentPage = totalPages - 1;
    if (kfmFolderCurrentPage < 0) kfmFolderCurrentPage = 0;

    const startIndex = kfmFolderCurrentPage * kfmFolderPageSize;
    const pageFolders = filtered.slice(startIndex, startIndex + kfmFolderPageSize);

    listContainer.innerHTML = pageFolders.map(f => {
        const isActive = f.folderId === kfmActiveFolderId;
        const color = f.color || '#4F46E5';

        return `
            <div class="kfm-folder-card ${isActive ? 'kfm-active' : ''}" 
                 onclick="kfmSelectFolder('${f.folderId}')" 
                 id="kfmFolderCard_${f.folderId}"
                 title="Bấm để xem và quản lý bộ thẻ ${escapeHtml(f.folderName)}"
                 style="background: #FFFFFF; border: 1px solid ${isActive ? color : 'var(--color-border)'}; border-left: 4px solid ${color}; border-radius: 8px; padding: 12px 14px; margin-bottom: 8px; cursor: pointer; transition: all 0.15s ease; box-shadow: ${isActive ? '0 2px 8px rgba(79,70,229,0.08)' : 'none'};">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px;">
                    <div style="display: flex; align-items: flex-start; gap: 10px; flex: 1; min-width: 0;">
                        <span style="font-size: 20px; line-height: 1.2;">📁</span>
                        <div style="min-width: 0; flex: 1;">
                            <div style="font-size: 13.5px; font-weight: 700; color: var(--color-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                ${escapeHtml(f.folderName)}
                            </div>
                            <div style="font-size: 12px; color: var(--color-text-muted); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                                ${escapeHtml(f.description || 'Chưa có ghi chú')}
                            </div>
                        </div>
                    </div>
                    <span style="font-size: 11px; font-weight: 700; background: var(--color-surface-muted); color: var(--color-text-secondary); padding: 2px 8px; border-radius: 9999px; white-space: nowrap; flex-shrink: 0;">
                        ${f.kanjiCount || 0} chữ
                    </span>
                </div>
            </div>
        `;
    }).join('');

    kfmRenderFolderPagination(totalPages, filtered.length);
}

/**
 * Điều khiển phân trang danh sách bộ thẻ cột trái
 */
function kfmRenderFolderPagination(totalPages, totalItems) {
    const container = document.getElementById('kfmFolderPagination');
    if (!container) return;

    if (totalPages <= 1) {
        container.innerHTML = `
            <span style="font-size: 11px; color: var(--color-text-muted);">
                Tổng cộng <strong>${totalItems}</strong> bộ thẻ
            </span>
        `;
        return;
    }

    let html = '';

    // Nút Trang trước
    const prevDisabled = kfmFolderCurrentPage <= 0;
    html += `
        <button type="button" class="btn btn-outline" 
                ${prevDisabled ? 'disabled style="opacity: 0.35; cursor: not-allowed; padding: 2px 6px; font-size: 11px; min-width: 26px; height: 26px;"' : 'style="padding: 2px 6px; font-size: 11px; min-width: 26px; height: 26px;"'} 
                onclick="kfmGoToFolderPage(${kfmFolderCurrentPage - 1})">
            ◀
        </button>
    `;

    // Hiển thị các số trang
    for (let p = 0; p < totalPages; p++) {
        const isActive = p === kfmFolderCurrentPage;
        html += `
            <button type="button" class="btn ${isActive ? 'btn-primary' : 'btn-outline'}" 
                    onclick="kfmGoToFolderPage(${p})"
                    style="padding: 2px 6px; font-size: 11px; min-width: 26px; height: 26px; ${isActive ? 'font-weight: 700;' : ''}">
                ${p + 1}
            </button>
        `;
    }

    // Nút Trang sau
    const nextDisabled = kfmFolderCurrentPage >= totalPages - 1;
    html += `
        <button type="button" class="btn btn-outline" 
                ${nextDisabled ? 'disabled style="opacity: 0.35; cursor: not-allowed; padding: 2px 6px; font-size: 11px; min-width: 26px; height: 26px;"' : 'style="padding: 2px 6px; font-size: 11px; min-width: 26px; height: 26px;"'} 
                onclick="kfmGoToFolderPage(${kfmFolderCurrentPage + 1})">
            ▶
        </button>
    `;

    container.innerHTML = html;
}

function kfmGoToFolderPage(page) {
    kfmFolderCurrentPage = page;
    kfmRenderFolderList();
}

/**
 * 4. Render Empty State ở cột phải khi người dùng chưa có bộ thẻ nào
 */
function kfmRenderEmptyFolderWorkspace() {
    const detailContainer = document.getElementById('kfmFolderDetailWorkspace');
    if (!detailContainer) return;

    detailContainer.innerHTML = `
        <div style="background: #FFFFFF; border: 2px dashed #CBD5E0; border-radius: var(--radius-lg); padding: 60px 24px; text-align: center; box-shadow: 0 4px 0 #E2E8F0;">
            <div style="font-size: 54px; margin-bottom: 16px;">🌱</div>
            <h3 style="font-size: 20px; font-weight: 800; color: #1A202C; margin: 0 0 8px 0;">
                Chưa có bộ thẻ nào được tạo
            </h3>
            <p style="font-size: 14px; color: var(--color-text-muted); max-width: 460px; margin: 0 auto 20px auto; line-height: 1.5;">
                Bạn có toàn quyền tạo mới các bộ thẻ theo bài học, chủ đề hoặc cấp độ thi JLPT của riêng mình. Hãy bắt đầu ngay!
            </p>
            <button type="button" class="btn btn-primary" onclick="kfmOpenCreateModal()" style="padding: 10px 20px; font-size: 14px;">
                <span>➕</span> <span>Tạo Bộ Thẻ Đầu Tiên</span>
            </button>
        </div>
    `;
}

/**
 * 5. Chọn một thư mục để xem và thao tác ở cột phải
 */
async function kfmSelectFolder(folderId) {
    kfmActiveFolderId = folderId;

    // Cập nhật viền active ở cột trái
    document.querySelectorAll('.kfm-folder-card').forEach(card => {
        card.classList.remove('kfm-active');
        card.style.borderColor = '#E2E8F0';
        card.style.boxShadow = '0 1px 3px rgba(0,0,0,0.02)';
    });

    const activeCard = document.getElementById(`kfmFolderCard_${folderId}`);
    const selectedFolder = kfmFolders.find(f => f.folderId === folderId);
    if (activeCard && selectedFolder) {
        activeCard.classList.add('kfm-active');
        activeCard.style.borderColor = selectedFolder.color || '#58CC02';
        activeCard.style.boxShadow = '0 4px 12px rgba(0,0,0,0.08)';
    }

    const detailContainer = document.getElementById('kfmFolderDetailWorkspace');
    if (!detailContainer) return;

    detailContainer.innerHTML = `
        <div style="text-align: center; padding: 60px 20px; color: var(--color-text-muted);">
            <div style="font-size: 32px; margin-bottom: 8px;">⏳</div>
            <div style="font-size: 14px; font-weight: 700;">Đang nạp danh sách chữ Hán trong bộ thẻ...</div>
        </div>
    `;

    try {
        const resp = await fetch(`/api/user/kanji-folders/${folderId}`);
        if (resp.ok) {
            kfmActiveFolderDetail = await resp.json();
            kfmRenderFolderDetailWorkspace(kfmActiveFolderDetail);
        } else {
            detailContainer.innerHTML = `
                <div style="padding: 40px; text-align: center; color: #DC2626;">
                    Không tìm thấy thông tin chi tiết cho bộ thẻ này!
                </div>
            `;
        }
    } catch (err) {
        console.error('Lỗi khi tải chi tiết thư mục:', err);
        detailContainer.innerHTML = `
            <div style="padding: 40px; text-align: center; color: #DC2626;">
                Lỗi tải thông tin bộ thẻ. Vui lòng thử lại!
            </div>
        `;
    }
}

/**
 * 6. Render chi tiết bộ thẻ đang chọn ở cột phải (Bao gồm cụm chức năng gộp JLPT & In-place Search)
 */
function kfmRenderFolderDetailWorkspace(folder) {
    const detailContainer = document.getElementById('kfmFolderDetailWorkspace');
    if (!detailContainer) return;

    const items = folder.items || [];
    const color = folder.color || '#58CC02';

    detailContainer.innerHTML = `
        <!-- HEADER BỘ THẺ ĐANG CHỌN -->
        <div style="background: #FFFFFF; border: 1px solid var(--color-border); border-radius: var(--radius-xl); padding: 20px 24px; margin-bottom: 20px; box-shadow: var(--shadow-xs);">
            <div style="display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 14px;">
                <div style="display: flex; align-items: center; gap: 14px;">
                    <div style="width: 48px; height: 48px; border-radius: var(--radius-md); background: ${color}22; border: 2px solid ${color}; display: flex; align-items: center; justify-content: center; font-size: 24px;">
                        📂
                    </div>
                    <div>
                        <div style="display: flex; align-items: center; gap: 10px;">
                            <h2 style="font-size: 20px; font-weight: 800; color: var(--color-primary); margin: 0;">${escapeHtml(folder.folderName)}</h2>
                            <span style="font-size: 11.5px; font-weight: 700; background: var(--color-surface-muted); color: var(--color-secondary); padding: 3px 10px; border-radius: var(--radius-full);">
                                ${items.length} chữ Hán
                            </span>
                        </div>
                        <p style="font-size: 13px; color: var(--color-text-secondary); margin: 4px 0 0 0;">
                            ${escapeHtml(folder.description || 'Chưa có ghi chú mục tiêu học tập')}
                        </p>
                    </div>
                </div>

                <!-- CỤM 2 NÚT HÀNH ĐỘNG LUYỆN TẬP CHÍNH (DUAL PRACTICE MODE ACTIONS) -->
                <div style="display: flex; gap: 10px; flex-wrap: wrap; align-items: center;">
                    <button type="button" class="btn btn-primary" onclick="kfmPracticeFolder('${folder.folderId}')" style="padding: 10px 18px; font-size: 13.5px;" title="Luyện viết nét toàn bộ chữ trong bộ thẻ này trên ô Mễ">
                        <span>✍️</span> <span>Luyện Viết Nét Chữ</span>
                    </button>
                    <button type="button" class="btn btn-secondary" onclick="kfmPlayMinigameWithThisFolder('${folder.folderId}')" style="padding: 10px 18px; font-size: 13.5px;" title="Rèn phản xạ gõ phím Hiragana với bộ chữ này">
                        <span>🎮</span> <span>Mưa Chữ Hán</span>
                    </button>
                    <button type="button" class="btn btn-outline" onclick="kfmOpenEditModal('${folder.folderId}')" style="padding: 10px 14px; font-size: 13px;">
                        <span>✏️</span> <span>Sửa</span>
                    </button>
                    <button type="button" class="btn btn-outline" onclick="kfmDeleteFolder('${folder.folderId}')" style="padding: 10px 14px; font-size: 13px; color: var(--color-danger); border-color: #FECACA;">
                        <span>🗑️</span> <span>Xóa</span>
                    </button>
                </div>
            </div>
        </div>

        <!-- KHUNG TÌM KIẾM & THÊM CHỮ KANJI MỚI VÀO BỘ THẺ -->
        <div style="background: #FFFFFF; border: 2px solid #E2E8F0; border-radius: var(--radius-lg); padding: 18px 22px; margin-bottom: 24px; box-shadow: 0 4px 0 #E2E8F0;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 10px;">
                <label style="font-size: 14px; font-weight: 800; color: #1A202C; display: flex; align-items: center; gap: 8px;">
                    <span>🔍</span> <span>Thêm Chữ Hán Mới Vào Bộ Thẻ:</span>
                </label>
                
                <!-- Cụm chức năng gộp duy nhất: Dropdown JLPT N1-N5 + Nút Nạp Ngẫu Nhiên 10 Chữ -->
                <div style="display: flex; align-items: center; gap: 8px;">
                    <select id="kfmSelectRandomJlpt" style="padding: 6px 12px; border: 2px solid #CBD5E0; border-radius: var(--radius-md); font-size: 13px; font-weight: 700; background: #FFFFFF; color: #1A202C; outline: none; cursor: pointer;">
                        <option value="N5" selected>JLPT: N5</option>
                        <option value="N4">JLPT: N4</option>
                        <option value="N3">JLPT: N3</option>
                        <option value="N2">JLPT: N2</option>
                        <option value="N1">JLPT: N1</option>
                    </select>
                    <button type="button" id="btnKfmAddRandom" class="btn btn-outline" onclick="kfmAddRandomKanjis()" style="padding: 6px 14px; font-size: 12.5px; font-weight: 600;" title="Nạp ngẫu nhiên 10 chữ cấp độ này (tự loại trừ các chữ đã có trong bộ)">
                        <span>🎲</span> <span>+10 Chữ Ngẫu Nhiên</span>
                    </button>
                </div>
            </div>

            <div style="position: relative;">
                <input type="text" 
                       id="kfmSearchKanjiInput" 
                       class="vocapy-input" 
                       placeholder="Gõ Kanji (vd: 日, 愛), âm Hán Việt (vd: NHẬT, ÁI), Romaji/Hiragana hoặc nghĩa tiếng Việt..." 
                       oninput="kfmHandleSearchInput(event)"
                       autocomplete="off"
                       style="width: 100%; padding: 12px 16px; border: 2px solid #CBD5E0; border-radius: var(--radius-md); font-size: 14px; outline: none; background: #F8FAFC;">
                
                <!-- Dropdown Auto-complete kết quả tìm kiếm -->
                <div id="kfmSearchResultsDropdown" style="display: none; position: absolute; top: calc(100% + 6px); left: 0; right: 0; background: #FFFFFF; border: 2px solid #CBD5E0; border-radius: var(--radius-md); box-shadow: 0 12px 30px rgba(0,0,0,0.18); z-index: 1000; max-height: 320px; overflow-y: auto; padding: 6px;">
                </div>
            </div>
            <div style="font-size: 12px; color: var(--color-text-muted); margin-top: 8px;">
                💡 <em>Mẹo: Kết quả tìm kiếm sẽ tự động loại trừ các chữ đã có trong bộ thẻ. Bạn có thể bấm "+ Thêm" nhiều từ liên tiếp mà không bị đóng ô tìm kiếm!</em>
            </div>
        </div>

        <!-- DANH SÁCH CÁC CHỮ KANJI HIỆN CÓ TRONG BỘ THẺ -->
        <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 14px;">
            <div style="font-size: 16px; font-weight: 800; color: #1A202C; display: flex; align-items: center; gap: 8px;">
                <span>📖</span> <span>Các Chữ Đang Học Trong Bộ:</span>
                <span id="kfmCurrentFolderKanjiCountBadge" style="font-size: 13px; font-weight: 700; color: ${color}; background: ${color}15; padding: 2px 10px; border-radius: var(--radius-full);">
                    ${items.length} chữ
                </span>
            </div>
            <span style="font-size: 12.5px; color: var(--color-text-muted);">
                Sắp xếp theo thứ tự thêm vào bộ
            </span>
        </div>

        <div id="kfmKanjiCardsGrid" class="kfm-kanji-grid">
            ${items.length === 0 ? `
                <div id="kfmEmptyItemsPlaceholder" style="grid-column: 1/-1; text-align: center; padding: 50px 20px; background: #FAF9F6; border: 2px dashed #CBD5E0; border-radius: var(--radius-lg);">
                    <div style="font-size: 42px; margin-bottom: 10px;">🌱</div>
                    <h3 style="margin: 0 0 6px 0; font-size: 17px; font-weight: 800; color: #1A202C;">Bộ Thẻ Này Chưa Có Chữ Kanji Nào</h3>
                    <p style="margin: 0 0 16px 0; font-size: 13.5px; color: var(--color-text-muted);">
                        Hãy gõ vào ô tìm kiếm ở trên hoặc bấm "+10 Chữ Ngẫu Nhiên" để nạp chữ vào bộ ngay!
                    </p>
                </div>
            ` : items.map((k, idx) => kfmBuildKanjiCardHtml(k, idx + 1, folder.folderId)).join('')}
        </div>
    `;
}

/**
 * Helper: Tạo HTML cho 1 thẻ chữ Kanji trong lưới
 */
function kfmBuildKanjiCardHtml(k, indexNumber, folderId) {
    return `
        <div class="kfm-kanji-item-card" id="kfmKanjiCardItem_${k.id}" style="background: #FFFFFF; border: 2px solid #E2E8F0; border-radius: var(--radius-md); padding: 14px; position: relative; display: flex; flex-direction: column; justify-content: space-between; box-shadow: 0 2px 0 #E2E8F0; transition: transform 0.15s ease;">
            <div>
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px;">
                    <span style="font-size: 11px; font-weight: 700; color: #718096;">#${indexNumber}</span>
                    <span style="font-size: 10.5px; font-weight: 800; background: #E0E7FF; color: #4338CA; padding: 1px 6px; border-radius: 4px;">
                        ${escapeHtml(k.jlpt || 'N5')}
                    </span>
                </div>

                <div style="text-align: center; margin: 6px 0;">
                    <span style="font-size: 46px; font-family: 'Noto Serif JP', 'Hiragino Mincho Pro', 'Yu Mincho', serif; font-weight: 700; color: #1A202C; line-height: 1;">
                        ${escapeHtml(k.kanjiChar)}
                    </span>
                </div>

                <div style="text-align: center; margin-bottom: 6px;">
                    <span style="font-size: 13.5px; font-weight: 800; color: #7C3AED; letter-spacing: 0.5px;">
                        ${escapeHtml(k.hanViet || 'HÁN TỰ')}
                    </span>
                </div>

                <div style="font-size: 12px; color: #475569; margin-bottom: 4px;">
                    <strong>Kun:</strong> ${escapeHtml(k.kunyomi || '-')}
                </div>
                <div style="font-size: 12px; color: #475569; margin-bottom: 6px;">
                    <strong>On:</strong> ${escapeHtml(k.onyomi || '-')}
                </div>

                <div style="font-size: 12px; color: #64748B; background: #F8FAFC; padding: 6px 8px; border-radius: 6px; margin-bottom: 10px; min-height: 34px; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;" title="${escapeHtml(k.meaning || '')}">
                    ${escapeHtml(k.meaning || 'Chưa có chú giải')}
                </div>
            </div>

            <div style="display: flex; gap: 6px; border-top: 1px dashed #E2E8F0; padding-top: 8px;">
                <button type="button" class="btn btn-secondary" onclick="kfmPracticeKanji('${escapeHtml(k.kanjiChar)}')" style="flex: 1; padding: 6px 4px; font-size: 11.5px;">
                    ✍️ Viết
                </button>
                <button type="button" class="btn btn-outline" onclick="kfmRemoveKanjiFromFolder('${folderId}', '${k.id}', '${escapeHtml(k.kanjiChar)}')" title="Xóa chữ này khỏi bộ thẻ" style="padding: 6px 10px; font-size: 11.5px; color: #DC2626; border-color: #FECACA;">
                    🗑️
                </button>
            </div>
        </div>
    `;
}

/**
 * 7. Xử lý tìm kiếm chữ Kanji theo thời gian thực (Debounce 300ms & Exclude các chữ đã có)
 */
function kfmHandleSearchInput(event) {
    const query = event.target.value.trim();
    const dropdown = document.getElementById('kfmSearchResultsDropdown');
    if (!dropdown) return;

    if (kfmSearchDebounceTimer) {
        clearTimeout(kfmSearchDebounceTimer);
    }

    if (!query) {
        dropdown.style.display = 'none';
        dropdown.innerHTML = '';
        return;
    }

    // Debounce chuẩn 300ms
    kfmSearchDebounceTimer = setTimeout(async () => {
        dropdown.style.display = 'block';
        dropdown.innerHTML = '<div style="padding: 14px; text-align: center; color: #64748B;">⏳ Đang tìm kiếm chữ Hán...</div>';

        try {
            const url = `/api/kanji/search?keyword=${encodeURIComponent(query)}&excludeFolderId=${kfmActiveFolderId || ''}&size=8`;
            const resp = await fetch(url);

            if (resp.ok) {
                const data = await resp.json();
                const items = data.items || [];

                if (items.length === 0) {
                    dropdown.innerHTML = '<div style="padding: 14px; text-align: center; color: #64748B;">Không tìm thấy chữ Hán nào phù hợp hoặc chữ này đã có trong bộ thẻ!</div>';
                    return;
                }

                dropdown.innerHTML = items.map(k => `
                    <div style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border-bottom: 1px solid #F1F5F9; border-radius: 6px; transition: background 0.15s ease;" onmouseenter="this.style.background='#F8FAFC'" onmouseleave="this.style.background='transparent'">
                        <div style="display: flex; align-items: center; gap: 14px; flex: 1; min-width: 0;">
                            <span style="font-size: 32px; font-family: 'Noto Serif JP', serif; font-weight: 700; color: #1A202C; min-width: 40px; text-align: center;">
                                ${escapeHtml(k.kanjiChar)}
                            </span>
                            <div style="min-width: 0;">
                                <div style="display: flex; align-items: center; gap: 6px;">
                                    <span style="font-weight: 800; color: #7C3AED; font-size: 14px;">${escapeHtml(k.hanViet || 'HÁN TỰ')}</span>
                                    <span style="font-size: 10.5px; background: #E0E7FF; color: #3730A3; padding: 1px 6px; border-radius: 4px; font-weight: 700;">${escapeHtml(k.jlpt || 'N5')}</span>
                                </div>
                                <div style="font-size: 11.5px; color: #64748B; margin-top: 2px;">
                                    <strong>On:</strong> ${escapeHtml(k.onyomi || '-')} | <strong>Kun:</strong> ${escapeHtml(k.kunyomi || '-')}
                                </div>
                                <div style="font-size: 12px; color: #334155; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                    ${escapeHtml(k.meaning || 'Chưa có nghĩa')}
                                </div>
                            </div>
                        </div>
                        <button type="button" 
                                id="btnKfmAdd_${k.id}" 
                                class="btn btn-primary" 
                                onclick='kfmAddKanjiToCurrentFolder(${JSON.stringify(k).replace(/'/g, "&apos;")}, this)' 
                                style="padding: 6px 14px; font-size: 12px; font-weight: 700; flex-shrink: 0; margin-left: 10px;">
                            ➕ Thêm
                        </button>
                    </div>
                `).join('');
            } else {
                dropdown.innerHTML = '<div style="padding: 12px; text-align: center; color: #DC2626;">Lỗi khi tìm kiếm chữ!</div>';
            }
        } catch (err) {
            console.error('Lỗi search kanji:', err);
            dropdown.innerHTML = '<div style="padding: 12px; text-align: center; color: #DC2626;">Lỗi kết nối máy chủ!</div>';
        }
    }, 300);
}

/**
 * 8. Thêm 1 chữ Kanji vào bộ thẻ đang chọn (Optimistic UI & Giữ ô tìm kiếm mở)
 */
async function kfmAddKanjiToCurrentFolder(kanji, btnElement) {
    if (!kfmActiveFolderId) {
        kfmShowToast('Vui lòng chọn một bộ thẻ trước khi thêm chữ!', 'error');
        return;
    }

    if (btnElement) {
        btnElement.disabled = true;
        btnElement.textContent = '⏳ Đang thêm...';
    }

    try {
        const resp = await fetch(`/api/user/kanji-folders/${kfmActiveFolderId}/items`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ kanjiId: kanji.id })
        });

        if (resp.ok) {
            kfmShowToast(`Đã thêm chữ 【${kanji.kanjiChar}】 vào bộ thẻ!`, 'success');

            // Cập nhật trạng thái nút đã bấm trong dropdown
            if (btnElement) {
                btnElement.textContent = '✅ Đã thêm';
                btnElement.classList.remove('btn-primary');
                btnElement.classList.add('btn-outline');
            }

            // Xóa placeholder rỗng nếu có
            const emptyPlaceholder = document.getElementById('kfmEmptyItemsPlaceholder');
            if (emptyPlaceholder) emptyPlaceholder.remove();

            // Optimistic UI: Append trực tiếp thẻ mới vào lưới mà không tải lại trang
            const grid = document.getElementById('kfmKanjiCardsGrid');
            if (grid && !document.getElementById(`kfmKanjiCardItem_${kanji.id}`)) {
                const currentCount = grid.querySelectorAll('.kfm-kanji-item-card').length;
                const newCardHtml = kfmBuildKanjiCardHtml(kanji, currentCount + 1, kfmActiveFolderId);
                grid.insertAdjacentHTML('beforeend', newCardHtml);
            }

            // Cập nhật các badge số lượng chữ
            kfmIncrementCurrentFolderCounters();
        } else {
            kfmShowToast('Không thể thêm chữ vào bộ thẻ. Vui lòng thử lại!', 'error');
            if (btnElement) {
                btnElement.disabled = false;
                btnElement.textContent = '➕ Thêm';
            }
        }
    } catch (err) {
        console.error('Lỗi khi thêm chữ vào bộ thẻ:', err);
        kfmShowToast('Lỗi kết nối máy chủ khi thêm chữ!', 'error');
        if (btnElement) {
            btnElement.disabled = false;
            btnElement.textContent = '➕ Thêm';
        }
    }
}

/**
 * 9. Cụm chức năng gộp: Nạp ngẫu nhiên 10 chữ theo cấp độ JLPT đã chọn (Chống kẹt nút bằng try/finally)
 */
async function kfmAddRandomKanjis() {
    if (!kfmActiveFolderId) {
        kfmShowToast('Vui lòng chọn một bộ thẻ trước!', 'error');
        return;
    }

    if (kfmIsAddingRandom) return; // Khóa ngăn chặn bấm đúp khi đang xử lý

    const selectJlpt = document.getElementById('kfmSelectRandomJlpt');
    const selectedJlpt = selectJlpt ? selectJlpt.value : 'N5';
    const btn = document.getElementById('btnKfmAddRandom');

    kfmIsAddingRandom = true;
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span>⏳ Đang nạp...</span>';
    }

    kfmShowToast(`Đang tìm và nạp ngẫu nhiên 10 chữ cấp độ ${selectedJlpt}...`, 'info');

    try {
        const resp = await fetch(`/api/user/kanji-folders/${kfmActiveFolderId}/add-random`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                jlpt: selectedJlpt,
                quantity: 10
            })
        });

        if (resp.ok) {
            const data = await resp.json();
            if (data.addedCount === 0) {
                kfmShowToast(data.message || `Kho chữ ${selectedJlpt} đã được thêm hết vào bộ thẻ này!`, 'info');
            } else {
                kfmShowToast(data.message || `Đã nạp thành công ${data.addedCount} chữ ${selectedJlpt} vào bộ thẻ!`, 'success');
                // Nạp lại chi tiết bộ thẻ để render danh sách mới đầy đủ
                await kfmSelectFolder(kfmActiveFolderId);
                await kfmRefreshFolderCounters();
            }
        } else {
            kfmShowToast('Lỗi khi nạp ngẫu nhiên chữ vào bộ thẻ!', 'error');
        }
    } catch (err) {
        console.error('Lỗi nạp ngẫu nhiên chữ:', err);
        kfmShowToast('Lỗi kết nối máy chủ khi nạp chữ!', 'error');
    } finally {
        // Luôn luôn mở khóa cờ và khôi phục nút bất kể thành công hay thất bại (Ngăn ngừa lỗi liệt nút)
        kfmIsAddingRandom = false;
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<span>🎲</span> <span>+10 Chữ Ngẫu Nhiên</span>';
        }
    }
}

/**
 * 10. Tăng số lượng chữ hiển thị trên UI khi thêm chữ thành công
 */
function kfmIncrementCurrentFolderCounters() {
    const badge = document.getElementById('kfmCurrentFolderKanjiCountBadge');
    if (badge) {
        const grid = document.getElementById('kfmKanjiCardsGrid');
        const count = grid ? grid.querySelectorAll('.kfm-kanji-item-card').length : 0;
        badge.textContent = `${count} chữ`;
    }

    const folderCard = document.getElementById(`kfmFolderCard_${kfmActiveFolderId}`);
    if (folderCard) {
        const countSpan = folderCard.querySelector('span[style*="border-radius: var(--radius-full)"]');
        if (countSpan) {
            const current = parseInt(countSpan.textContent) || 0;
            countSpan.textContent = `${current + 1} chữ`;
        }
    }
}

/**
 * 11. Xóa 1 chữ Kanji khỏi bộ thẻ
 */
async function kfmRemoveKanjiFromFolder(folderId, kanjiId, kanjiChar) {
    if (!confirm(`Bạn có chắc chắn muốn xóa chữ 【${kanjiChar}】 khỏi bộ thẻ này không?`)) {
        return;
    }

    try {
        const resp = await fetch(`/api/user/kanji-folders/${folderId}/items/${kanjiId}`, {
            method: 'DELETE'
        });

        if (resp.ok) {
            kfmShowToast(`Đã xóa chữ 【${kanjiChar}】 khỏi bộ thẻ!`, 'info');
            await kfmSelectFolder(folderId);
            await kfmRefreshFolderCounters();
        } else {
            kfmShowToast('Không thể xóa chữ khỏi bộ thẻ!', 'error');
        }
    } catch (err) {
        console.error('Lỗi khi xóa chữ khỏi bộ thẻ:', err);
        kfmShowToast('Lỗi kết nối máy chủ khi xóa chữ!', 'error');
    }
}

/**
 * 12. Chế độ 1: Luyện Viết Nét Chữ (Kanji Canvas) theo bộ thẻ đã chọn
 * Tự động nạp toàn bộ danh sách chữ của riêng bộ thẻ này vào Shodo Canvas
 */
async function kfmPracticeFolder(folderId) {
    let folder = kfmActiveFolderDetail;
    if (!folder || folder.folderId !== folderId) {
        try {
            const resp = await fetch(`/api/user/kanji-folders/${folderId}`);
            if (resp.ok) {
                folder = await resp.json();
            }
        } catch (e) {
            console.error('Lỗi khi nạp dữ liệu bộ thẻ:', e);
        }
    }

    const items = (folder && folder.items) ? folder.items : [];
    if (items.length === 0) {
        if (typeof kfmShowToast === 'function') {
            kfmShowToast('Bộ thẻ này chưa có chữ nào! Hãy tìm và thêm chữ vào bộ trước khi luyện viết.', 'warning');
        } else {
            alert('Bộ thẻ này chưa có chữ nào! Hãy tìm và thêm chữ vào bộ trước khi luyện viết.');
        }
        return;
    }

    // Lưu danh sách chữ vào Session Storage để điều hướng tiếp tục
    sessionStorage.setItem('kfmPracticeSet', JSON.stringify({
        folderId: folder.folderId,
        folderName: folder.folderName,
        kanjis: items.map(it => it.kanjiChar),
        currentIndex: 0
    }));
    sessionStorage.setItem('kizuna_practice_from', 'deck');

    // Chuyển sang màn hình Canvas với chữ đầu tiên của bộ thẻ
    const firstChar = items[0].kanjiChar;
    window.location.href = `/kanji?charQuery=${encodeURIComponent(firstChar)}&tab=practice&folderId=${folder.folderId}&from=deck`;
}

/**
 * 12b. Chuyển sang Tab Luyện Viết với một chữ Hán đơn lẻ
 */
function kfmPracticeKanji(kanjiChar) {
    sessionStorage.setItem('kizuna_practice_from', 'deck');
    const folderId = kfmActiveFolderDetail ? kfmActiveFolderDetail.folderId : '';
    const folderParam = folderId ? `&folderId=${folderId}` : '';
    window.location.href = `/kanji?charQuery=${encodeURIComponent(kanjiChar)}&tab=practice${folderParam}&from=deck`;
}

/**
 * 13. Chế độ 2: Minigame Mưa Chữ Hán theo bộ thẻ đã chọn
 * Tự động bind folderId và mở modal chuẩn bị chỉ với bước chọn thời gian
 */
async function kfmPlayMinigameWithThisFolder(folderId) {
    let folder = kfmActiveFolderDetail;
    if (!folder || folder.folderId !== folderId) {
        folder = kfmFolders.find(f => f.folderId === folderId);
        if (!folder) {
            try {
                const resp = await fetch(`/api/user/kanji-folders/${folderId}`);
                if (resp.ok) folder = await resp.json();
            } catch (e) {}
        }
    }

    const folderName = folder ? folder.folderName : 'Bộ Thẻ Đang Chọn';
    const kanjiCount = folder ? (folder.kanjiCount || (folder.items ? folder.items.length : 0)) : 0;

    // Chuyển sang Tab Game
    if (typeof switchMainTab === 'function') {
        switchMainTab('game');
    }

    // Mở Modal cấu hình đã bind sẵn folderId
    setTimeout(() => {
        if (typeof openGameConfigModalForFolder === 'function') {
            openGameConfigModalForFolder(folderId, folderName, kanjiCount);
        } else if (typeof openGameConfigModal === 'function') {
            openGameConfigModal(folderId);
        }
    }, 150);
}

/**
 * 13b. Hiển thị thanh duyệt chữ trong bộ thẻ trên màn hình Canvas Shodo
 */
async function kfmSetupPracticeFolderStrip() {
    const stripContainer = document.getElementById('practiceFolderStripContainer');
    if (!stripContainer) return;

    const urlParams = new URLSearchParams(window.location.search);
    const folderId = urlParams.get('folderId');
    const fromParam = urlParams.get('from');
    const currentChar = urlParams.get('charQuery') || (document.getElementById('targetKanjiChar') ? document.getElementById('targetKanjiChar').textContent.trim() : '');

    if (!folderId || fromParam === 'search') {
        stripContainer.style.display = 'none';
        return;
    }

    try {
        const resp = await fetch(`/api/user/kanji-folders/${folderId}`);
        if (!resp.ok) return;
        const folder = await resp.json();
        const items = folder.items || [];
        if (items.length === 0) return;

        stripContainer.style.display = 'block';

        // Ẩn thanh tra cứu khi đang trong chế độ luyện tập theo bộ thẻ
        const searchBarForm = document.getElementById('practiceSearchBarForm');
        if (searchBarForm) searchBarForm.style.display = 'none';

        const titleEl = document.getElementById('practiceFolderTitle');
        if (titleEl) titleEl.textContent = folder.folderName;

        let currIdx = items.findIndex(it => it.kanjiChar === currentChar);
        if (currIdx === -1) currIdx = 0;

        const progressEl = document.getElementById('practiceFolderProgress');
        if (progressEl) progressEl.textContent = `Chữ ${currIdx + 1}/${items.length}`;

        const pillsContainer = document.getElementById('practiceFolderKanjiPills');
        if (pillsContainer) {
            pillsContainer.innerHTML = items.map((it, idx) => {
                const isCurr = idx === currIdx;
                return `
                    <a href="/kanji?charQuery=${encodeURIComponent(it.kanjiChar)}&tab=practice&folderId=${folderId}&from=deck" 
                       id="practiceKanjiPill-${idx}"
                       class="tool-btn ${isCurr ? 'active' : ''}" 
                       data-idx="${idx}"
                       onclick="if(typeof kfmScrollActivePillToCenter==='function') kfmScrollActivePillToCenter(this.parentElement, this);"
                       style="padding: 5px 11px; font-size: 13.5px; font-weight: 700; text-decoration: none; min-width: 32px; justify-content: center; border-radius: var(--radius-md); transition: all 0.2s ease; ${isCurr ? 'background: #EEF2FF; border: 2px solid var(--color-primary); color: var(--color-primary); box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.18); transform: scale(1.08); font-weight: 800;' : ''}">
                        ${escapeHtml(it.kanjiChar)}
                    </a>
                `;
            }).join('');

            // Tự động cuộn chữ Kanji đang active vào giữa tầm nhìn thanh cuộn ngang
            kfmScrollActivePillToCenter(pillsContainer);
        }

        const prevBtn = document.getElementById('btnPrevFolderKanji');
        if (prevBtn) {
            if (currIdx > 0) {
                prevBtn.disabled = false;
                prevBtn.style.opacity = '1';
                prevBtn.onclick = () => {
                    const prevChar = items[currIdx - 1].kanjiChar;
                    window.location.href = `/kanji?charQuery=${encodeURIComponent(prevChar)}&tab=practice&folderId=${folderId}&from=deck`;
                };
            } else {
                prevBtn.disabled = true;
                prevBtn.style.opacity = '0.5';
            }
        }

        const nextBtn = document.getElementById('btnNextFolderKanji');
        if (nextBtn) {
            if (currIdx < items.length - 1) {
                nextBtn.disabled = false;
                nextBtn.style.opacity = '1';
                nextBtn.onclick = () => {
                    const nextChar = items[currIdx + 1].kanjiChar;
                    window.location.href = `/kanji?charQuery=${encodeURIComponent(nextChar)}&tab=practice&folderId=${folderId}&from=deck`;
                };
            } else {
                nextBtn.disabled = true;
                nextBtn.style.opacity = '0.5';
            }
        }
    } catch (e) {
        console.warn('Lỗi khi nạp thanh duyệt chữ bộ thẻ:', e);
    }
}

/**
 * 13c. Tự động căn chỉnh mượt mà chữ Kanji đang active vào chính giữa thanh cuộn ngang (Center Alignment)
 */
function kfmScrollActivePillToCenter(container, targetElement) {
    if (!container) container = document.getElementById('practiceFolderKanjiPills');
    if (!container) return;

    setTimeout(() => {
        const activePill = targetElement || container.querySelector('.tool-btn.active');
        if (!activePill) return;

        // 1. scrollIntoView chuẩn W3C với inline: 'center'
        if (typeof activePill.scrollIntoView === 'function') {
            activePill.scrollIntoView({
                behavior: 'smooth',
                block: 'nearest',
                inline: 'center'
            });
        }

        // 2. Fallback tính toán độ lệch thủ công scrollLeft
        const targetScrollLeft = activePill.offsetLeft - (container.clientWidth / 2) + (activePill.clientWidth / 2);
        container.scrollTo({
            left: Math.max(0, targetScrollLeft),
            behavior: 'smooth'
        });
    }, 120);
}

/**
 * 14. Cập nhật lại số lượng chữ trên danh sách thẻ bên trái
 */
async function kfmRefreshFolderCounters() {
    try {
        const resp = await fetch('/api/user/kanji-folders');
        if (resp.ok) {
            kfmFolders = await resp.json();
            kfmRenderFolderList(kfmFolders);
        }
    } catch (err) {
        console.warn('Lỗi refresh counters:', err);
    }
}

/**
 * 15. HỘP THOẠI TẠO BỘ THẺ MỚI (MODAL)
 */
function kfmOpenCreateModal() {
    const modal = document.getElementById('kfmCreateFolderModal');
    if (modal) {
        modal.style.display = 'flex';
        const nameInput = document.getElementById('kfmNewFolderName');
        if (nameInput) {
            nameInput.value = '';
            nameInput.focus();
        }
        const descInput = document.getElementById('kfmNewFolderDesc');
        if (descInput) descInput.value = '';
    }
}

function kfmCloseCreateModal() {
    const modal = document.getElementById('kfmCreateFolderModal');
    if (modal) modal.style.display = 'none';
}

async function kfmSubmitCreateFolder(event) {
    event.preventDefault();
    const nameInput = document.getElementById('kfmNewFolderName');
    const descInput = document.getElementById('kfmNewFolderDesc');
    const folderName = nameInput ? nameInput.value.trim() : '';
    if (!folderName) {
        kfmShowToast('Vui lòng nhập tên cho bộ thẻ!', 'error');
        return;
    }
    const payload = {
        folderName: folderName,
        description: descInput ? descInput.value.trim() : '',
        color: '#4F46E5',
        kanjiIds: []
    };

    try {
        const resp = await fetch('/api/user/kanji-folders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (resp.ok) {
            const created = await resp.json();
            kfmCloseCreateModal();
            kfmShowToast(`Đã tạo bộ thẻ 【${folderName}】 thành công!`, 'success');
            await kfmLoadAllFolders(created.folderId);
        } else {
            kfmShowToast('Tạo bộ thẻ thất bại. Vui lòng thử lại!', 'error');
        }
    } catch (err) {
        console.error('Lỗi khi tạo bộ thẻ:', err);
        kfmShowToast('Lỗi kết nối máy chủ!', 'error');
    }
}

/**
 * 16. HỘP THOẠI CHỈNH SỬA BỘ THẺ (MODAL)
 */
function kfmOpenEditModal(folderId) {
    const folder = kfmFolders.find(f => f.folderId === folderId);
    if (!folder) return;

    const modal = document.getElementById('kfmEditFolderModal');
    if (modal) {
        modal.style.display = 'flex';
        document.getElementById('kfmEditFolderId').value = folder.folderId;
        document.getElementById('kfmEditFolderName').value = folder.folderName;
        document.getElementById('kfmEditFolderDesc').value = folder.description || '';

        const colorRadio = document.querySelector(`input[name="kfmEditFolderColor"][value="${folder.color}"]`);
        if (colorRadio) {
            colorRadio.checked = true;
        } else {
            const defaultRadio = document.querySelector('input[name="kfmEditFolderColor"]');
            if (defaultRadio) defaultRadio.checked = true;
        }
    }
}

function kfmCloseEditModal() {
    const modal = document.getElementById('kfmEditFolderModal');
    if (modal) modal.style.display = 'none';
}

async function kfmSubmitEditFolder(event) {
    event.preventDefault();
    const folderId = document.getElementById('kfmEditFolderId').value;
    const folderName = document.getElementById('kfmEditFolderName').value.trim();
    const desc = document.getElementById('kfmEditFolderDesc').value.trim();
    const colorRadio = document.querySelector('input[name="kfmEditFolderColor"]:checked');

    if (!folderName) {
        kfmShowToast('Vui lòng nhập tên bộ thẻ!', 'error');
        return;
    }

    const payload = {
        folderName: folderName,
        description: desc,
        color: colorRadio ? colorRadio.value : '#58CC02'
    };

    try {
        const resp = await fetch(`/api/user/kanji-folders/${folderId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (resp.ok) {
            kfmCloseEditModal();
            kfmShowToast('Đã lưu thay đổi bộ thẻ thành công!', 'success');
            await kfmLoadAllFolders(folderId);
        } else {
            kfmShowToast('Không thể cập nhật bộ thẻ!', 'error');
        }
    } catch (err) {
        console.error('Lỗi khi cập nhật bộ thẻ:', err);
        kfmShowToast('Lỗi kết nối máy chủ!', 'error');
    }
}

/**
 * 17. Xóa bất kỳ bộ thẻ nào kèm hộp thoại xác nhận
 */
async function kfmDeleteFolder(folderId) {
    const folder = kfmFolders.find(f => f.folderId === folderId);
    const folderName = folder ? folder.folderName : 'này';

    if (!confirm(`Bạn có chắc chắn muốn xóa bộ thẻ 【${folderName}】 không? Toàn bộ danh sách chữ trong bộ sẽ bị xóa khỏi bộ thẻ này (không xóa khỏi từ điển).`)) {
        return;
    }

    try {
        const resp = await fetch(`/api/user/kanji-folders/${folderId}`, {
            method: 'DELETE'
        });

        if (resp.ok) {
            kfmShowToast(`Đã xóa bộ thẻ 【${folderName}】 thành công!`, 'info');
            await kfmLoadAllFolders();
        } else {
            kfmShowToast('Không thể xóa bộ thẻ!', 'error');
        }
    } catch (err) {
        console.error('Lỗi khi xóa bộ thẻ:', err);
        kfmShowToast('Lỗi kết nối máy chủ khi xóa bộ thẻ!', 'error');
    }
}

/**
 * 18. HỘP THOẠI TOÀN CỤC: THÊM CHỮ KANJI VÀO BỘ THẺ TỪ BẤT KỲ ĐÂU
 */
async function promptAddKanjiToFolder(kanjiId, kanjiChar, event) {
    if (event) event.stopPropagation();

    const modal = document.getElementById('kfmQuickAddToFolderModal');
    if (!modal) return;

    modal.style.display = 'flex';
    document.getElementById('kfmQuickAddTargetChar').textContent = kanjiChar;
    document.getElementById('kfmQuickAddKanjiId').value = kanjiId;

    const listElem = document.getElementById('kfmQuickAddFolderChoices');
    listElem.innerHTML = '<div style="padding: 16px; text-align: center; color: #64748B;">Đang nạp danh sách bộ thẻ...</div>';

    try {
        if (!kfmFolders || kfmFolders.length === 0) {
            const resp = await fetch('/api/user/kanji-folders');
            if (resp.ok) kfmFolders = await resp.json();
        }

        if (!kfmFolders || kfmFolders.length === 0) {
            listElem.innerHTML = `
                <div style="padding: 16px; text-align: center; color: #64748B;">
                    Bạn chưa có bộ thẻ nào. Hãy tạo một bộ thẻ mới trước!
                </div>
            `;
            return;
        }

        listElem.innerHTML = kfmFolders.map(f => `
            <div style="display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; border: 1.5px solid #E2E8F0; border-radius: var(--radius-md); margin-bottom: 8px; transition: background 0.15s ease;" onmouseenter="this.style.background='#F8FAFC'" onmouseleave="this.style.background='transparent'">
                <div style="display: flex; align-items: center; gap: 10px;">
                    <span style="font-size: 18px;">📁</span>
                    <div>
                        <div style="font-weight: 700; color: #1A202C; font-size: 14px;">${escapeHtml(f.folderName)}</div>
                        <div style="font-size: 11.5px; color: #718096;">${f.kanjiCount || 0} chữ hiện có</div>
                    </div>
                </div>
                <button type="button" class="btn btn-primary" onclick="kfmExecuteQuickAdd('${f.folderId}', '${kanjiId}', '${escapeHtml(kanjiChar)}')" style="padding: 6px 12px; font-size: 12px;">
                    ➕ Thêm Vào Bộ Này
                </button>
            </div>
        `).join('');
    } catch (err) {
        listElem.innerHTML = '<div style="padding: 16px; text-align: center; color: #DC2626;">Lỗi khi tải bộ thẻ!</div>';
    }
}

function closeQuickAddToFolderModal() {
    const modal = document.getElementById('kfmQuickAddToFolderModal');
    if (modal) modal.style.display = 'none';
}

async function kfmExecuteQuickAdd(folderId, kanjiId, kanjiChar) {
    try {
        const resp = await fetch(`/api/user/kanji-folders/${folderId}/items`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ kanjiId: kanjiId })
        });

        if (resp.ok) {
            closeQuickAddToFolderModal();
            kfmShowToast(`Đã thêm chữ 【${kanjiChar}】 vào bộ thẻ thành công!`, 'success');
            await kfmRefreshFolderCounters();
        } else {
            kfmShowToast('Không thể thêm chữ vào bộ thẻ!', 'error');
        }
    } catch (err) {
        console.error('Lỗi khi thêm chữ:', err);
        kfmShowToast('Lỗi kết nối máy chủ!', 'error');
    }
}

/**
 * 19. Hiển thị thông báo Toast 3D VocaPy
 */
function kfmShowToast(message, type = 'success') {
    let container = document.getElementById('kfmToastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'kfmToastContainer';
        container.style.cssText = 'position: fixed; top: 24px; right: 24px; z-index: 99999; display: flex; flex-direction: column; gap: 10px; pointer-events: none;';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    const bgColor = type === 'success' ? '#DCFCE7' : (type === 'error' ? '#FEE2E2' : '#E0F2FE');
    const borderColor = type === 'success' ? '#22C55E' : (type === 'error' ? '#EF4444' : '#0284C7');
    const textColor = type === 'success' ? '#166534' : (type === 'error' ? '#991B1B' : '#075985');
    const icon = type === 'success' ? '✅' : (type === 'error' ? '⚠️' : 'ℹ️');

    toast.style.cssText = `
        background: ${bgColor};
        border: 2px solid ${borderColor};
        border-radius: var(--radius-md);
        padding: 12px 18px;
        color: ${textColor};
        font-weight: 700;
        font-size: 13.5px;
        box-shadow: 0 6px 16px rgba(0,0,0,0.12);
        display: flex;
        align-items: center;
        gap: 10px;
        min-width: 280px;
        max-width: 420px;
        pointer-events: auto;
        transform: translateY(-10px);
        opacity: 0;
        transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
    `;

    toast.innerHTML = `
        <span style="font-size: 18px;">${icon}</span>
        <span style="flex: 1;">${escapeHtml(message)}</span>
    `;

    container.appendChild(toast);

    requestAnimationFrame(() => {
        toast.style.transform = 'translateY(0)';
        toast.style.opacity = '1';
    });

    setTimeout(() => {
        toast.style.transform = 'translateY(-10px)';
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 250);
    }, 3200);
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Đóng dropdown tìm kiếm khi click ra ngoài
document.addEventListener('click', (e) => {
    const dropdown = document.getElementById('kfmSearchResultsDropdown');
    const input = document.getElementById('kfmSearchKanjiInput');
    if (dropdown && input && !dropdown.contains(e.target) && e.target !== input) {
        dropdown.style.display = 'none';
    }
});

// Xuất các hàm ra phạm vi Window
window.initKanjiFolderManager = initKanjiFolderManager;
window.kfmSelectFolder = kfmSelectFolder;
window.kfmOpenCreateModal = kfmOpenCreateModal;
window.kfmCloseCreateModal = kfmCloseCreateModal;
window.kfmSubmitCreateFolder = kfmSubmitCreateFolder;
window.kfmOpenEditModal = kfmOpenEditModal;
window.kfmCloseEditModal = kfmCloseEditModal;
window.kfmSubmitEditFolder = kfmSubmitEditFolder;
window.kfmDeleteFolder = kfmDeleteFolder;
window.kfmHandleSearchInput = kfmHandleSearchInput;
window.kfmAddKanjiToCurrentFolder = kfmAddKanjiToCurrentFolder;
window.kfmRemoveKanjiFromFolder = kfmRemoveKanjiFromFolder;
window.kfmAddRandomKanjis = kfmAddRandomKanjis;
window.kfmPracticeKanji = kfmPracticeKanji;
window.kfmPlayMinigameWithThisFolder = kfmPlayMinigameWithThisFolder;
window.promptAddKanjiToFolder = promptAddKanjiToFolder;
window.closeQuickAddToFolderModal = closeQuickAddToFolderModal;
window.kfmExecuteQuickAdd = kfmExecuteQuickAdd;
window.kfmOnFolderSearchInput = kfmOnFolderSearchInput;
window.kfmGoToFolderPage = kfmGoToFolderPage;
window.kfmShowToast = kfmShowToast;
window.kfmScrollActivePillToCenter = kfmScrollActivePillToCenter;
