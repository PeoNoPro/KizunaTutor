/**
 * ====================================================================================================
 * KIZUNA TUTOR - MINIGAME "BẮN CHỮ KANJI" UI CONTROLLER & FOLDER MANAGER
 * ====================================================================================================
 * Quản lý tương tác người dùng, mở Modal cấu hình, gọi REST API lấy thư mục & nạp vào game engine.
 * ====================================================================================================
 */

let activeKanjiRainGame = null;
let currentLoadedFolders = [];
let selectedPlayFolderId = null;

/**
 * 1. Khởi tạo giao diện khi học viên chuyển sang Tab Game
 */
async function initKanjiRainUI() {
    await fetchUserFoldersForSelect();
    
    // Nếu chưa từng mở game, tự động mở modal cấu hình để người chơi chọn thời gian & bộ thẻ
    const hasActiveGame = activeKanjiRainGame && activeKanjiRainGame.isRunning;
    if (!hasActiveGame) {
        openGameConfigModal();
    }
}

/**
 * 2. Gọi API lấy danh sách Thư mục Kanji của người dùng
 */
async function fetchUserFoldersForSelect() {
    const selectElem = document.getElementById('selectGameFolder');
    if (!selectElem) return;

    try {
        const response = await fetch('/api/user/kanji-folders');
        if (response.ok) {
            currentLoadedFolders = await response.json();
            renderFolderSelectOptions(selectElem, currentLoadedFolders);
        } else {
            console.warn('Không tải được danh sách thư mục:', response.status);
        }
    } catch (err) {
        console.error('Lỗi khi fetch danh sách folders:', err);
    }
}

/**
 * Điền danh sách thư mục vào thẻ <select>
 */
function renderFolderSelectOptions(selectElem, folders) {
    selectElem.innerHTML = '';

    if (!folders || folders.length === 0) {
        const opt = document.createElement('option');
        opt.value = '';
        opt.textContent = '🌸 Bộ Thẻ Kanji Mặc Định (N5 Căn Bản)';
        selectElem.appendChild(opt);
        return;
    }

    folders.forEach(f => {
        const opt = document.createElement('option');
        opt.value = f.folderId;
        opt.textContent = `${f.folderName} (${f.kanjiCount} chữ)`;
        if (f.isDefault) {
            opt.selected = true;
            selectedPlayFolderId = f.folderId;
        }
        selectElem.appendChild(opt);
    });

    if (!selectedPlayFolderId && folders.length > 0) {
        selectedPlayFolderId = folders[0].folderId;
    }
}

/**
 * 3. Mở Modal Cấu Hình Trước Khi Chơi (Pre-Game Modal)
 */
function openGameConfigModal() {
    const modal = document.getElementById('gameConfigModal');
    if (modal) {
        modal.style.display = 'flex';
        const folderBadge = document.getElementById('gameConfigSelectedFolderBadge');
        if (folderBadge) folderBadge.parentElement.style.display = 'none';
        const selectContainer = document.getElementById('gameConfigSelectFolderContainer');
        if (selectContainer) selectContainer.style.display = 'block';
        fetchUserFoldersForSelect();
    }
}

/**
 * 3b. Mở Modal Cấu Hình với bộ thẻ đã chọn sẵn từ phân hệ "Bộ Thẻ Của Tôi"
 * Bỏ qua bước chọn thư mục, học viên chỉ cần chọn thời lượng và bấm Bắt đầu
 */
async function openGameConfigModalForFolder(folderId, folderName, kanjiCount) {
    const modal = document.getElementById('gameConfigModal');
    if (!modal) return;

    modal.style.display = 'flex';
    selectedPlayFolderId = folderId;

    // Hiển thị trực quan thông tin bộ thẻ đang luyện
    const folderBadge = document.getElementById('gameConfigSelectedFolderBadge');
    if (folderBadge) {
        folderBadge.innerHTML = `<span style="font-size: 16px;">📁</span> <strong>${escapeHtml(folderName || 'Bộ Thẻ Đang Luyện')}</strong> <span style="font-size: 12px; color: var(--color-text-secondary); margin-left: 6px;">(${kanjiCount || 0} chữ)</span>`;
        folderBadge.parentElement.style.display = 'block';
    }

    // Ẩn dropdown chọn thư mục vì đã bind sẵn
    const selectContainer = document.getElementById('gameConfigSelectFolderContainer');
    if (selectContainer) {
        selectContainer.style.display = 'none';
    }

    await fetchUserFoldersForSelect();
    const selectElem = document.getElementById('selectGameFolder');
    if (selectElem) {
        selectElem.value = folderId;
    }
}

function closeGameConfigModal() {
    const modal = document.getElementById('gameConfigModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

/**
 * 4. BẮT ĐẦU CHƠI MINIGAME:
 * Lấy thời gian đã chọn, nạp dữ liệu Hiragana chuẩn hóa từ API và khởi chạy Canvas Engine
 */
async function startKanjiRainGame() {
    const selectElem = document.getElementById('selectGameFolder');
    const folderId = selectElem ? selectElem.value : null;

    // Lấy thời gian chơi đã chọn từ Radio Buttons (60s, 180s, 300s)
    let selectedDuration = 60;
    const radioDuration = document.querySelector('input[name="gameDurationRadio"]:checked');
    if (radioDuration) {
        selectedDuration = parseInt(radioDuration.value, 10) || 60;
    }

    // Hiển thị trạng thái đang tải dữ liệu
    const btnStart = document.getElementById('btnStartGameSubmit');
    if (btnStart) {
        btnStart.disabled = true;
        btnStart.textContent = '⏳ Đang nạp dữ liệu chữ Hán...';
    }

    try {
        // Gọi API lấy danh sách chữ kèm các cách đọc Hiragana chuẩn hóa
        const url = folderId 
            ? `/api/user/kanji-folders/${folderId}/play-data`
            : `/api/user/kanji-folders/default/play-data`;
        
        let kanjiData = [];
        try {
            const resp = await fetch(url);
            if (resp.ok) {
                kanjiData = await resp.json();
            }
        } catch (e) {
            console.warn('Fallback sang API tra cứu chữ cơ bản:', e);
        }

        // Fallback nếu folder rỗng hoặc API chưa trả về
        if (!kanjiData || kanjiData.length === 0) {
            kanjiData = getClientFallbackKanjiList();
        }

        // Đóng modal cấu hình
        closeGameConfigModal();

        // Khởi tạo Game Engine nếu chưa có
        if (!activeKanjiRainGame) {
            activeKanjiRainGame = new KanjiRainGame({
                canvasId: 'kanjiRainCanvas',
                inputId: 'kanjiGameInput',
                displayInputId: 'liveTypingEcho',
                targetDuration: selectedDuration,
                folderId: folderId,
                onScoreUpdate: updateGameScoreDashboard,
                onGameOver: showGameOverModal,
                onGameWin: showGameWinModal
            });
        } else {
            activeKanjiRainGame.targetDuration = selectedDuration;
            activeKanjiRainGame.folderId = folderId;
        }

        // Nạp data và bắt đầu
        activeKanjiRainGame.loadKanjiData(kanjiData);
        activeKanjiRainGame.start();

        // Cập nhật tên bộ thẻ đang chơi trên thanh tiêu đề
        const titleBadge = document.getElementById('currentPlayingFolderBadge');
        if (titleBadge && selectElem && selectElem.selectedIndex >= 0) {
            titleBadge.textContent = selectElem.options[selectElem.selectedIndex].text;
        }

    } catch (err) {
        console.error('Lỗi khi khởi chạy minigame:', err);
        alert('Có lỗi khi nạp dữ liệu ván đấu. Vui lòng thử lại!');
    } finally {
        if (btnStart) {
            btnStart.disabled = false;
            btnStart.textContent = '🚀 BẮT ĐẦU CHƠI NGAY';
        }
    }
}

/**
 * 5. Cập nhật các chỉ số lên thanh Dashboard (Score, Timer, Accuracy, Combo)
 */
function updateGameScoreDashboard(stats) {
    const elScore = document.getElementById('gameScoreDisplay');
    const elTimer = document.getElementById('gameTimerDisplay');
    const elWords = document.getElementById('gameWordsDisplay');
    const elAccuracy = document.getElementById('gameAccuracyDisplay');
    const elCombo = document.getElementById('gameComboDisplay');

    if (elScore) elScore.textContent = stats.score.toLocaleString();
    if (elTimer) elTimer.textContent = stats.timeString;
    if (elWords) elWords.textContent = `${stats.wordsTyped} từ`;
    if (elAccuracy) elAccuracy.textContent = `${stats.accuracy}%`;

    if (elCombo) {
        if (stats.combo > 1) {
            elCombo.style.display = 'inline-flex';
            elCombo.textContent = `🔥 Chuỗi liên hoàn: x${stats.combo}!`;
        } else {
            elCombo.style.display = 'none';
        }
    }
}

/**
 * 6. HIỂN THỊ MODAL GAME OVER (THUA CUỘC DO CHẠM DEADLINE)
 */
function showGameOverModal(result) {
    const modal = document.getElementById('gameOverModal');
    if (!modal) return;

    // Highlight chữ Kanji gây thua cuộc
    const failedChar = result.failedKanjiCharacter || (result.failedKanjiData ? result.failedKanjiData.character : '❓');
    const elChar = document.getElementById('goFailedChar');
    const elReading = document.getElementById('goFailedReading');
    const elHanViet = document.getElementById('goFailedHanViet');
    const elMeaning = document.getElementById('goFailedMeaning');

    if (elChar) elChar.textContent = failedChar;
    if (result.failedKanjiData) {
        if (elReading) elReading.textContent = result.failedKanjiData.primaryReading || (result.failedKanjiData.readings || []).join(', ');
        if (elHanViet) elHanViet.textContent = result.failedKanjiData.hanViet || 'HÁN TỰ';
        if (elMeaning) elMeaning.textContent = result.failedKanjiData.meaning || 'Chưa có ý nghĩa cụ thể';
    }

    // Thống kê phiên
    const elWords = document.getElementById('goStatWords');
    const elScore = document.getElementById('goStatScore');
    const elAccuracy = document.getElementById('goStatAccuracy');
    if (elWords) elWords.textContent = `${result.wordsTypedCount} từ`;
    if (elScore) elScore.textContent = result.score.toLocaleString();
    if (elAccuracy) elAccuracy.textContent = `${result.accuracy}%`;

    // Danh sách các từ còn đang rơi
    const elRemaining = document.getElementById('goRemainingList');
    if (elRemaining && result.remainingKanjis) {
        elRemaining.innerHTML = '';
        result.remainingKanjis.slice(0, 8).forEach(item => {
            const tag = document.createElement('span');
            tag.style.cssText = 'background: #F1F5F9; border: 1px solid #CBD5E1; border-radius: 6px; padding: 3px 8px; font-size: 13px; color: #1E293B;';
            tag.textContent = `${item.character} (${item.reading || item.hanViet})`;
            elRemaining.appendChild(tag);
        });
    }

    modal.style.display = 'flex';
}

function closeGameOverModal() {
    const modal = document.getElementById('gameOverModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

/**
 * 7. HIỂN THỊ MODAL CHIẾN THẮNG (YOU WIN - SỐNG SÓT ĐẾN HẾT GIỜ)
 */
function showGameWinModal(result) {
    const modal = document.getElementById('gameWinModal');
    if (!modal) return;

    const elWords = document.getElementById('winStatWords');
    const elScore = document.getElementById('winStatScore');
    const elAccuracy = document.getElementById('winStatAccuracy');
    const elRank = document.getElementById('winRankTitle');

    if (elWords) elWords.textContent = `${result.wordsTypedCount} từ`;
    if (elScore) elScore.textContent = result.score.toLocaleString();
    if (elAccuracy) elAccuracy.textContent = `${result.accuracy}%`;

    // Tính danh hiệu 100% Tiếng Việt
    let title = 'Tân Binh Luyện Chữ 🌸';
    if (result.wordsTypedCount >= 40 && result.accuracy >= 92) {
        title = 'Bậc Thầy Chữ Hán 🏆';
    } else if (result.wordsTypedCount >= 25 && result.accuracy >= 85) {
        title = 'Cao Thủ Gõ Phím ⚔️';
    } else if (result.wordsTypedCount >= 12) {
        title = 'Học Giả Tiến Bộ 📜';
    }
    if (elRank) elRank.textContent = title;

    modal.style.display = 'flex';
}

function closeGameWinModal() {
    const modal = document.getElementById('gameWinModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

/**
 * Chơi lại ván mới cùng cấu hình hiện tại
 */
function restartKanjiGame() {
    closeGameOverModal();
    closeGameWinModal();
    startKanjiRainGame();
}

/**
 * Dừng chơi và mở modal cấu hình chọn lại bộ thẻ khác
 */
function exitToGameSetup() {
    closeGameOverModal();
    closeGameWinModal();
    if (activeKanjiRainGame) {
        activeKanjiRainGame.stop();
    }
    openGameConfigModal();
}

/**
 * 8. QUẢN LÝ BỘ THẺ KANJI (CHUYỂN SANG TAB BỘ THẺ TOÀN DIỆN)
 */
function openFolderManagerModal() {
    if (typeof switchMainTab === 'function') {
        switchMainTab('folders');
    } else {
        const modal = document.getElementById('folderManagerModal');
        if (modal) {
            modal.style.display = 'flex';
            loadFoldersIntoManagerList();
        }
    }
}

function closeFolderManagerModal() {
    const modal = document.getElementById('folderManagerModal');
    if (modal) {
        modal.style.display = 'none';
        fetchUserFoldersForSelect(); // Cập nhật lại dropdown game
    }
}

/**
 * Tải danh sách thư mục hiển thị trong bảng quản lý
 */
async function loadFoldersIntoManagerList() {
    const listElem = document.getElementById('folderManagerItemsList');
    if (!listElem) return;

    listElem.innerHTML = '<div style="text-align: center; padding: 20px; color: #64748B;">Đang tải danh sách thư mục...</div>';

    try {
        const resp = await fetch('/api/user/kanji-folders');
        if (resp.ok) {
            const folders = await resp.json();
            if (folders.length === 0) {
                listElem.innerHTML = '<div style="text-align: center; padding: 20px; color: #64748B;">Chưa có thư mục cá nhân nào. Hãy bấm nút tạo mới phía dưới!</div>';
                return;
            }

            listElem.innerHTML = '';
            folders.forEach(f => {
                const card = document.createElement('div');
                card.style.cssText = 'background: #FFFFFF; border: 1px solid #E2E8F0; border-radius: 8px; padding: 12px 16px; margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 1px 3px rgba(0,0,0,0.04);';

                card.innerHTML = `
                    <div>
                        <div style="font-weight: 700; color: #1E293B; font-size: 15px; display: flex; align-items: center; gap: 8px;">
                            <span style="display: inline-block; width: 12px; height: 12px; border-radius: 50%; background-color: ${f.color || '#EF4444'};"></span>
                            <span>${f.folderName}</span>
                        </div>
                        <div style="font-size: 13px; color: #64748B; margin-top: 4px;">
                            ${f.description || 'Chưa có mô tả'} • <strong style="color: #2563EB;">${f.kanjiCount} chữ Kanji</strong>
                        </div>
                    </div>
                    <div style="display: flex; gap: 6px;">
                        <button type="button" class="btn-outline" onclick="selectFolderToPlay('${f.folderId}')" style="padding: 6px 12px; font-size: 12px;">
                            🎮 Bắn Chữ
                        </button>
                        <button type="button" class="btn-outline" onclick="deleteUserFolder('${f.folderId}')" style="padding: 6px 10px; font-size: 12px; color: #DC2626; border-color: #FECACA;">
                            🗑️ Xóa
                        </button>
                    </div>
                `;
                listElem.appendChild(card);
            });
        }
    } catch (err) {
        listElem.innerHTML = '<div style="color: #DC2626; text-align: center; padding: 20px;">Lỗi khi tải danh sách thư mục!</div>';
    }
}

/**
 * Chọn thư mục để bắt đầu chơi ngay từ modal quản lý
 */
function selectFolderToPlay(folderId) {
    selectedPlayFolderId = folderId;
    closeFolderManagerModal();
    openGameConfigModal();
    const selectElem = document.getElementById('selectGameFolder');
    if (selectElem) {
        selectElem.value = folderId;
    }
}

/**
 * Xóa một thư mục bộ thẻ
 */
async function deleteUserFolder(folderId) {
    if (!confirm('Bạn có chắc chắn muốn xóa thư mục này không? Các chữ Hán bên trong sẽ không bị xóa khỏi từ điển.')) {
        return;
    }

    try {
        const resp = await fetch(`/api/user/kanji-folders/${folderId}`, {
            method: 'DELETE'
        });
        if (resp.ok) {
            loadFoldersIntoManagerList();
        } else {
            alert('Không thể xóa thư mục này.');
        }
    } catch (err) {
        console.error('Lỗi khi xóa folder:', err);
    }
}

/**
 * Tạo thư mục mới từ form modal
 */
async function submitCreateNewFolder(event) {
    event.preventDefault();
    const nameInput = document.getElementById('newFolderNameInput');
    const descInput = document.getElementById('newFolderDescInput');
    const colorInput = document.getElementById('newFolderColorInput');

    const folderName = nameInput ? nameInput.value.trim() : '';
    if (!folderName) {
        alert('Vui lòng nhập tên thư mục!');
        return;
    }

    const payload = {
        folderName: folderName,
        description: descInput ? descInput.value.trim() : '',
        color: colorInput ? colorInput.value : '#EF4444',
        kanjiIds: []
    };

    try {
        const resp = await fetch('/api/user/kanji-folders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (resp.ok) {
            if (nameInput) nameInput.value = '';
            if (descInput) descInput.value = '';
            loadFoldersIntoManagerList();
            alert('Đã tạo thư mục mới thành công! Bạn có thể thêm chữ Kanji vào thư mục từ danh sách từ điển.');
        } else {
            alert('Tạo thư mục thất bại, vui lòng thử lại.');
        }
    } catch (err) {
        console.error('Lỗi tạo thư mục:', err);
    }
}

/**
 * Dữ liệu fallback offline trong trường hợp mạng lỗi để game luôn chơi được
 */
function getClientFallbackKanjiList() {
    return [
        { kanjiId: '1', character: '日', hanViet: 'NHẬT', meaning: 'Mặt trời, ngày', readings: ['ひ', 'にち', 'び', 'か'], primaryReading: 'ひ' },
        { kanjiId: '2', character: '月', hanViet: 'NGUYỆT', meaning: 'Mặt trăng, tháng', readings: ['つき', 'げつ', 'がつ'], primaryReading: 'つき' },
        { kanjiId: '3', character: '水', hanViet: 'THỦY', meaning: 'Nước', readings: ['みず', 'すい'], primaryReading: 'みず' },
        { kanjiId: '4', character: '火', hanViet: 'HỎA', meaning: 'Lửa', readings: ['ひ', 'か'], primaryReading: 'ひ' },
        { kanjiId: '5', character: '木', hanViet: 'MỘC', meaning: 'Cây', readings: ['き', 'もく', 'ぼく'], primaryReading: 'き' },
        { kanjiId: '6', character: '金', hanViet: 'KIM', meaning: 'Vàng, tiền', readings: ['かね', 'きん'], primaryReading: 'かね' },
        { kanjiId: '7', character: '土', hanViet: 'THỔ', meaning: 'Đất', readings: ['つち', 'ど', 'と'], primaryReading: 'つち' },
        { kanjiId: '8', character: '人', hanViet: 'NHÂN', meaning: 'Người', readings: ['ひと', 'じん', 'にん'], primaryReading: 'ひと' },
        { kanjiId: '9', character: '山', hanViet: 'SƠN', meaning: 'Núi', readings: ['やま', 'さん'], primaryReading: 'やま' },
        { kanjiId: '10', character: '川', hanViet: 'XUYÊN', meaning: 'Sông', readings: ['かわ', 'せん'], primaryReading: 'かわ' },
        { kanjiId: '11', character: '学', hanViet: 'HỌC', meaning: 'Học tập', readings: ['まなぶ', 'がく'], primaryReading: 'まなぶ' },
        { kanjiId: '12', character: '校', hanViet: 'HIỆU', meaning: 'Trường học', readings: ['こう'], primaryReading: 'こう' },
        { kanjiId: '13', character: '先', hanViet: 'TIÊN', meaning: 'Trước, đi trước', readings: ['さき', 'せん'], primaryReading: 'さき' },
        { kanjiId: '14', character: '生', hanViet: 'SINH', meaning: 'Sống, sinh ra', readings: ['いきる', 'うまれる', 'なま', 'せい'], primaryReading: 'いきる' },
        { kanjiId: '15', character: '行', hanViet: 'HÀNH', meaning: 'Đi', readings: ['いく', 'ゆく', 'こう', 'ぎょう'], primaryReading: 'いく' },
        { kanjiId: '16', character: '来', hanViet: 'LAI', meaning: 'Đến', readings: ['くる', 'きたる', 'らい'], primaryReading: 'くる' }
    ];
}

// Gán ra window
window.initKanjiRainUI = initKanjiRainUI;
window.openGameConfigModal = openGameConfigModal;
window.closeGameConfigModal = closeGameConfigModal;
window.startKanjiRainGame = startKanjiRainGame;
window.restartKanjiGame = restartKanjiGame;
window.exitToGameSetup = exitToGameSetup;
window.openFolderManagerModal = openFolderManagerModal;
window.closeFolderManagerModal = closeFolderManagerModal;
window.submitCreateNewFolder = submitCreateNewFolder;
window.selectFolderToPlay = selectFolderToPlay;
window.deleteUserFolder = deleteUserFolder;


/**
 * 9. Tính năng thêm nhanh chữ Kanji vào bộ thẻ được quản lý bởi kanji-folder-manager.js
 */
