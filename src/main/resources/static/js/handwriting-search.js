/**
 * KizunaTutor - Module Tra Cứu Chữ Hán Bằng Nét Vẽ Viết Tay Thời Gian Thực (Real-time Handwriting Kanji Search)
 * Tích hợp kiến trúc Hybrid:
 * 1. Google Input Tools Handwriting API (nhận diện tức thì < 150ms sau mỗi nét vẽ)
 * 2. Backend Kizuna Spring Boot Enrichment & Vector Topology Matching Engine (Metadata đầy đủ + Offline Fallback)
 */

let hwCanvas = null;
let hwCtx = null;
let hwStrokes = [];         // Mảng chứa các nét: mỗi nét là mảng [{x, y, t}]
let hwCurrentStroke = [];   // Nét đang vẽ dở
let hwIsDrawing = false;
let hwDebounceTimer = null;
let hwCurrentCandidates = [];
let hwSelectedKanji = null;

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Chuẩn hóa và làm sạch chuỗi SVG KanjiVG:
 * Cắt bỏ <?xml ... ?>, <!-- ... -->, <!DOCTYPE ... [ ... ]>
 */
function cleanKanjiVgSvg(rawSvg) {
    if (!rawSvg) return '';
    const svgStartIdx = rawSvg.indexOf('<svg');
    const svgEndIdx = rawSvg.lastIndexOf('</svg>');
    if (svgStartIdx !== -1 && svgEndIdx !== -1) {
        return rawSvg.substring(svgStartIdx, svgEndIdx + 6);
    }
    return rawSvg
        .replace(/<\?xml[\s\S]*?\?>/gi, '')
        .replace(/<!--[\s\S]*?-->/g, '')
        .replace(/<!DOCTYPE[\s\S]*?\]>/gi, '')
        .replace(/<!DOCTYPE[^>]*>/gi, '')
        .trim();
}

function initHandwritingSearch() {
    hwCanvas = document.getElementById('hwSearchCanvas');
    if (!hwCanvas) return;
    hwCtx = hwCanvas.getContext('2d');

    const rect = hwCanvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    const displayWidth = (rect.width > 0) ? rect.width : 280;
    const displayHeight = (rect.height > 0) ? rect.height : 280;

    hwCanvas.width = Math.round(displayWidth * dpr);
    hwCanvas.height = Math.round(displayHeight * dpr);
    hwCanvas.style.width = displayWidth + 'px';
    hwCanvas.style.height = displayHeight + 'px';
    hwCanvas.style.touchAction = 'none';

    hwCtx.setTransform(1, 0, 0, 1, 0, 0);
    hwCtx.scale(dpr, dpr);

    // Gắn sự kiện chuột & cảm ứng (đảm bảo không bị duplicate)
    hwCanvas.removeEventListener('mousedown', hwOnMouseDown);
    hwCanvas.removeEventListener('mousemove', hwOnMouseMove);
    window.removeEventListener('mouseup', hwOnMouseUp);

    hwCanvas.removeEventListener('touchstart', hwOnTouchStart);
    hwCanvas.removeEventListener('touchmove', hwOnTouchMove);
    window.removeEventListener('touchend', hwOnTouchEnd);

    hwCanvas.addEventListener('mousedown', hwOnMouseDown);
    hwCanvas.addEventListener('mousemove', hwOnMouseMove);
    window.addEventListener('mouseup', hwOnMouseUp);

    hwCanvas.addEventListener('touchstart', hwOnTouchStart, { passive: false });
    hwCanvas.addEventListener('touchmove', hwOnTouchMove, { passive: false });
    window.addEventListener('touchend', hwOnTouchEnd);

    // Ủy quyền sự kiện (Event Delegation) duy nhất trên phần tử cha hwCandidateList
    const candidateContainer = document.getElementById('hwCandidateList');
    if (candidateContainer) {
        candidateContainer.onclick = function(e) {
            const tile = e.target.closest('.hw-candidate-tile');
            if (!tile) return;
            e.preventDefault();
            e.stopPropagation();
            const idxStr = tile.getAttribute('data-index');
            const index = idxStr !== null ? parseInt(idxStr, 10) : -1;
            const kanji = tile.getAttribute('data-kanji');
            openHwKanjiDetail(index >= 0 ? index : kanji, e);
        };
    }

    hwRedraw();
}

function getHwCoords(e) {
    if (!hwCanvas) return { x: 0, y: 0, t: Date.now() };
    const rect = hwCanvas.getBoundingClientRect();
    const clientX = (e.touches && e.touches.length > 0) ? e.touches[0].clientX : e.clientX;
    const clientY = (e.touches && e.touches.length > 0) ? e.touches[0].clientY : e.clientY;
    return {
        x: clientX - rect.left,
        y: clientY - rect.top,
        t: Date.now()
    };
}

function hwOnMouseDown(e) {
    if (e.button !== undefined && e.button !== 0) return;
    if (e.preventDefault) e.preventDefault();
    hwIsDrawing = true;
    const pt = getHwCoords(e);
    hwCurrentStroke = [pt];
    hwDrawCurrentSegment(pt, pt);
}

function hwOnMouseMove(e) {
    if (!hwIsDrawing) return;
    if (e.preventDefault) e.preventDefault();
    const pt = getHwCoords(e);
    if (hwCurrentStroke.length === 0) {
        hwCurrentStroke.push(pt);
        return;
    }
    const lastPt = hwCurrentStroke[hwCurrentStroke.length - 1];
    hwCurrentStroke.push(pt);
    hwDrawCurrentSegment(lastPt, pt);
}

function hwOnMouseUp() {
    if (!hwIsDrawing) return;
    hwIsDrawing = false;
    if (hwCurrentStroke.length > 0) {
        hwStrokes.push(hwCurrentStroke);
        hwCurrentStroke = [];
        hwUpdateStrokeCountBadge();
        hwTriggerDebouncedSearch();
    }
}

function hwOnTouchStart(e) {
    if (e.preventDefault) e.preventDefault();
    hwIsDrawing = true;
    const pt = getHwCoords(e);
    hwCurrentStroke = [pt];
    hwDrawCurrentSegment(pt, pt);
}

function hwOnTouchMove(e) {
    if (!hwIsDrawing) return;
    if (e.preventDefault) e.preventDefault();
    const pt = getHwCoords(e);
    if (hwCurrentStroke.length === 0) {
        hwCurrentStroke.push(pt);
        return;
    }
    const lastPt = hwCurrentStroke[hwCurrentStroke.length - 1];
    hwCurrentStroke.push(pt);
    hwDrawCurrentSegment(lastPt, pt);
}

function hwOnTouchEnd() {
    if (!hwIsDrawing) return;
    hwIsDrawing = false;
    if (hwCurrentStroke.length > 0) {
        hwStrokes.push(hwCurrentStroke);
        hwCurrentStroke = [];
        hwUpdateStrokeCountBadge();
        hwTriggerDebouncedSearch();
    }
}

function hwDrawCurrentSegment(p1, p2) {
    if (!hwCtx) return;
    hwCtx.save();
    hwCtx.strokeStyle = '#1B2A4A'; // Indigo Aizome
    hwCtx.lineWidth = 5;
    hwCtx.lineCap = 'round';
    hwCtx.lineJoin = 'round';

    hwCtx.beginPath();
    if (Math.abs(p1.x - p2.x) < 0.5 && Math.abs(p1.y - p2.y) < 0.5) {
        hwCtx.arc(p1.x, p1.y, 2.5, 0, Math.PI * 2);
        hwCtx.fillStyle = '#1B2A4A';
        hwCtx.fill();
        hwCtx.restore();
        return;
    }
    hwCtx.moveTo(p1.x, p1.y);
    hwCtx.lineTo(p2.x, p2.y);
    hwCtx.stroke();
    hwCtx.restore();
}

function hwRedraw() {
    if (!hwCanvas || !hwCtx) return;
    const rect = hwCanvas.getBoundingClientRect();
    const w = (rect.width > 0) ? rect.width : 280;
    const h = (rect.height > 0) ? rect.height : 280;

    // Xóa sạch canvas
    hwCtx.clearRect(0, 0, w, h);

    // 1. Vẽ nền ô Mễ (米字格)
    hwCtx.save();
    hwCtx.fillStyle = '#FFFFFF';
    hwCtx.fillRect(0, 0, w, h);

    // Khung ngoài
    hwCtx.strokeStyle = '#CBD5E1';
    hwCtx.lineWidth = 1.5;
    hwCtx.strokeRect(0, 0, w, h);

    // Lưới đứt đoạn ô Mễ
    hwCtx.strokeStyle = '#FCA5A5';
    hwCtx.lineWidth = 1;
    hwCtx.setLineDash([4, 4]);

    // 2 đường chéo
    hwCtx.beginPath();
    hwCtx.moveTo(0, 0); hwCtx.lineTo(w, h);
    hwCtx.moveTo(w, 0); hwCtx.lineTo(0, h);
    // Trục chữ thập
    hwCtx.moveTo(w / 2, 0); hwCtx.lineTo(w / 2, h);
    hwCtx.moveTo(0, h / 2); hwCtx.lineTo(w, h / 2);
    hwCtx.stroke();
    hwCtx.restore();

    // 2. Vẽ lại toàn bộ các nét đã vẽ
    if (hwStrokes.length > 0) {
        hwCtx.save();
        hwCtx.strokeStyle = '#1B2A4A';
        hwCtx.lineWidth = 5;
        hwCtx.lineCap = 'round';
        hwCtx.lineJoin = 'round';

        for (const stroke of hwStrokes) {
            if (!stroke || stroke.length === 0) continue;
            hwCtx.beginPath();
            hwCtx.moveTo(stroke[0].x, stroke[0].y);
            for (let i = 1; i < stroke.length; i++) {
                hwCtx.lineTo(stroke[i].x, stroke[i].y);
            }
            hwCtx.stroke();
        }
        hwCtx.restore();
    }
}

function hwUndoStroke() {
    if (hwStrokes.length > 0) {
        hwStrokes.pop();
        hwRedraw();
        hwUpdateStrokeCountBadge();
        if (hwStrokes.length > 0) {
            hwTriggerDebouncedSearch();
        } else {
            hwRenderEmptyState("Hãy viết nét đầu tiên vào bảng vẽ bên trái!");
        }
    }
}

function hwClearCanvas() {
    hwStrokes = [];
    hwCurrentStroke = [];
    if (hwDebounceTimer) clearTimeout(hwDebounceTimer);
    hwRedraw();
    hwUpdateStrokeCountBadge();
    hwRenderEmptyState("Hãy viết nét đầu tiên vào bảng vẽ bên trái!");
}

function hwUpdateStrokeCountBadge() {
    const el = document.getElementById('hwStrokeCountBadge');
    if (el) {
        el.innerText = `Số nét: ${hwStrokes.length}`;
    }
}

/**
 * Debounce nhận diện: Chờ 160ms sau nét vẽ cuối để gọi nhận diện
 */
function hwTriggerDebouncedSearch() {
    if (hwDebounceTimer) clearTimeout(hwDebounceTimer);
    if (hwStrokes.length === 0) return;

    hwShowLoadingState();
    hwDebounceTimer = setTimeout(() => {
        hwExecuteRecognition();
    }, 160);
}

/**
 * Thực thi nhận diện Hybrid:
 * 1. Gọi Google Input Tools Handwriting API (lấy candidates chữ Kanji trong 100ms)
 * 2. Gửi candidates về Backend Spring Boot để enrich ngữ nghĩa, Hán Việt, JLPT
 * 3. Nếu mạng lỗi, tự động fallback gọi Backend Vector Topology Engine!
 */
async function hwExecuteRecognition() {
    if (hwStrokes.length === 0) return;

    const rect = hwCanvas ? hwCanvas.getBoundingClientRect() : null;
    const w = (rect && rect.width > 0) ? rect.width : 280;
    const h = w;

    // Chuẩn bị Ink format cho Google Input Tools
    const ink = hwStrokes.map(stroke => {
        const xs = stroke.map(p => Math.round(p.x));
        const ys = stroke.map(p => Math.round(p.y));
        const ts = stroke.map((p, idx) => idx * 10);
        return [xs, ys, ts];
    });

    const googlePayload = {
        app_version: 0.4,
        api_level: "537.36",
        device: "5",
        input_type: "0",
        options: "enable_pre_space",
        requests: [
            {
                writing_guide: {
                    writing_area_width: Math.round(w),
                    writing_area_height: Math.round(h)
                },
                ink: ink,
                language: "ja"
            }
        ]
    };

    let googleCandidates = [];
    try {
        const gRes = await fetch('https://inputtools.google.com/request?itc=ja-t-i0-handwrit&app=translate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(googlePayload)
        });

        if (gRes.ok) {
            const gData = await gRes.json();
            if (gData && gData[0] === 'SUCCESS' && gData[1] && gData[1][0] && gData[1][0][1]) {
                googleCandidates = gData[1][0][1];
            }
        }
    } catch (err) {
        console.warn("Google Input Tools offline, chuyển sang Backend Topology Engine:", err);
    }

    // Đóng gói dữ liệu gửi Backend KizunaTutor
    const backendPayload = {
        strokeCount: hwStrokes.length,
        strokes: hwStrokes.map(stroke => stroke.map(p => ({ x: p.x, y: p.y }))),
        candidates: googleCandidates,
        limit: 12
    };

    try {
        const bRes = await fetch('/api/kanji/search-by-strokes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(backendPayload)
        });

        if (!bRes.ok) throw new Error("Backend search failed");
        const bData = await bRes.json();
        hwRenderCandidateResults(bData);
    } catch (err) {
        console.error("Lỗi tìm kiếm chữ Hán:", err);
        if (googleCandidates.length > 0) {
            hwRenderCandidateResults({
                matches: googleCandidates.slice(0, 10).map((c, i) => ({
                    character: c,
                    hanViet: 'HÁN TỰ',
                    meaning: 'Nhận diện viết tay',
                    totalStrokes: hwStrokes.length,
                    jlpt: 'N/A',
                    similarityScore: (1 - i * 0.05).toFixed(2)
                })),
                sourceEngine: 'GOOGLE_HANDWRITING_RAW'
            });
        } else {
            hwRenderEmptyState("Không tìm thấy kết quả phù hợp. Hãy điều chỉnh nét vẽ hoặc xóa viết lại!");
        }
    }
}

function hwShowLoadingState() {
    const listEl = document.getElementById('hwCandidateList');
    if (!listEl) return;
    listEl.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 36px 16px; color: #64748b;">
            <div class="ai-spinner" style="margin: 0 auto 10px auto; width: 32px; height: 32px;"></div>
            <p style="margin: 0; font-size: 13.5px; font-weight: 600; color: var(--color-primary, #1B2A4A);">Đang nhận diện nét vẽ thời gian thực...</p>
        </div>
    `;
}

function hwRenderEmptyState(msg) {
    const listEl = document.getElementById('hwCandidateList');
    if (!listEl) return;
    listEl.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 48px 20px; background: #FAF9F6; border: 1.5px dashed #CBD5E1; border-radius: 14px;">
            <div style="font-size: 38px; margin-bottom: 8px;">🖌️</div>
            <h5 style="margin: 0 0 6px 0; color: #1B2A4A; font-size: 15px; font-weight: 700;">Bảng Nhận Diện Viết Tay Sẵn Sàng</h5>
            <p style="margin: 0; color: #64748B; font-size: 13px;">${msg || 'Hãy vẽ từng nét chữ Hán vào ô vuông bên trái. Kết quả gợi ý sẽ hiển thị tức thì theo thời gian thực.'}</p>
        </div>
    `;

    const engineBadge = document.getElementById('hwEngineBadge');
    if (engineBadge) {
        engineBadge.style.display = 'none';
    }
}

function hwRenderCandidateResults(data) {
    const listEl = document.getElementById('hwCandidateList');
    if (!listEl) return;

    const matches = data.matches || [];
    hwCurrentCandidates = matches;

    const engineBadge = document.getElementById('hwEngineBadge');
    if (engineBadge) {
        engineBadge.style.display = 'none';
    }

    if (matches.length === 0) {
        hwRenderEmptyState("Chưa tìm thấy chữ Hán tương đồng. Hãy viết thêm các nét tiếp theo!");
        return;
    }

    listEl.innerHTML = matches.map((item, index) => {
        const percent = Math.round((item.similarityScore || 0.9) * 100);
        return `
            <div class="hw-candidate-tile" data-index="${index}" data-kanji="${escapeHtml(item.character)}" title="Bấm để xem chi tiết và luyện viết">
                <div class="hw-tile-top">
                    <span class="hw-tile-char">${escapeHtml(item.character)}</span>
                    <span class="hw-tile-score">${percent}%</span>
                </div>
                <div class="hw-tile-body">
                    <strong class="hw-tile-hanviet">${escapeHtml(item.hanViet || 'HÁN TỰ')}</strong>
                    <p class="hw-tile-meaning" title="${escapeHtml(item.meaning || '')}">${escapeHtml(item.meaning || 'Chữ Hán tra cứu')}</p>
                </div>
                <div class="hw-tile-footer">
                    <span class="hw-tile-strokes">${item.totalStrokes || '?'} nét</span>
                    <span class="hw-tile-jlpt">${escapeHtml(item.jlpt || 'N/A')}</span>
                </div>
            </div>
        `;
    }).join('');
}

/**
 * Mở Modal xem chi tiết chữ Hán khi click vào Candidate Tile
 */
async function openHwKanjiDetail(indexOrChar, event) {
    if (event) {
        event.stopPropagation();
    }

    let item = null;
    if (typeof indexOrChar === 'number') {
        item = hwCurrentCandidates[indexOrChar];
    } else if (typeof indexOrChar === 'string') {
        item = hwCurrentCandidates.find(c => c.character === indexOrChar) || { character: indexOrChar };
    }

    if (!item) return;

    hwSelectedKanji = item;
    const modal = document.getElementById('hwDetailModal');
    if (!modal) return;

    const charEl = document.getElementById('hwModalChar');
    if (charEl) charEl.innerText = item.character || '?';

    const hvEl = document.getElementById('hwModalHanViet');
    if (hvEl) hvEl.innerText = item.hanViet || 'HÁN TỰ';

    const meanEl = document.getElementById('hwModalMeaning');
    if (meanEl) meanEl.innerText = item.meaning || 'Chữ Hán tra cứu';

    const strEl = document.getElementById('hwModalStrokes');
    if (strEl) strEl.innerText = `${item.totalStrokes || '?'} nét`;

    const jlptEl = document.getElementById('hwModalJlpt');
    if (jlptEl) jlptEl.innerText = item.jlpt || 'N/A';

    const onEl = document.getElementById('hwModalOnyomi');
    if (onEl) onEl.innerText = item.onyomi || '-';

    const kunEl = document.getElementById('hwModalKunyomi');
    if (kunEl) kunEl.innerText = item.kunyomi || '-';

    // Nạp SVG nét vẽ chuẩn KanjiVG
    const svgBox = document.getElementById('hwModalSvgBox');
    if (svgBox) {
        if (item.svgSolution && item.svgSolution.includes('<svg')) {
            svgBox.innerHTML = cleanKanjiVgSvg(item.svgSolution);
        } else {
            svgBox.innerHTML = '<span style="color:#94a3b8; font-size:12px;">Đang tải hình nét chuẩn...</span>';
            fetch(`/api/kanji/${encodeURIComponent(item.character)}`)
                .then(r => r.json())
                .then(q => {
                    if (q && q.svgData) {
                        svgBox.innerHTML = cleanKanjiVgSvg(q.svgData);
                    } else {
                        svgBox.innerHTML = `<span style="font-size: 56px; font-family: 'Noto Serif JP', serif; color: #1B2A4A;">${item.character}</span>`;
                    }
                })
                .catch(() => {
                    svgBox.innerHTML = `<span style="font-size: 56px; font-family: 'Noto Serif JP', serif; color: #1B2A4A;">${item.character}</span>`;
                });
        }
    }

    modal.style.display = 'flex';
}

function closeHwDetailModal(event) {
    if (event) event.stopPropagation();
    const modal = document.getElementById('hwDetailModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

/**
 * Chuyển thẳng sang trang Luyện viết chữ Kanji với chữ vừa tra cứu
 */
function practiceSelectedHwKanji(event) {
    if (event) event.stopPropagation();
    if (!hwSelectedKanji || !hwSelectedKanji.character) return;
    const char = hwSelectedKanji.character;
    sessionStorage.setItem('kizuna_practice_from', 'search');
    window.location.href = `/kanji?charQuery=${encodeURIComponent(char)}&tab=practice&from=search`;
}

function copyHwSelectedChar(event) {
    if (event) event.stopPropagation();
    if (!hwSelectedKanji || !hwSelectedKanji.character) return;
    navigator.clipboard.writeText(hwSelectedKanji.character)
        .then(() => alert(`Đã sao chép chữ "${hwSelectedKanji.character}" vào bộ nhớ tạm!`))
        .catch(() => alert(`Ký tự: ${hwSelectedKanji.character}`));
}

// Mở & Đóng Modal Tra Cứu Viết Tay Toàn Cảnh
function openHwSearchModal() {
    const modal = document.getElementById('hwSearchModal');
    if (modal) {
        modal.style.display = 'flex';
        initHandwritingSearch();
        // Delay 60ms để layout modal hiển thị hoàn tất trước khi re-calibrate rect
        setTimeout(() => {
            initHandwritingSearch();
        }, 60);
    }
}

function closeHwSearchModal(event) {
    if (event) event.stopPropagation();
    const modal = document.getElementById('hwSearchModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// Khởi chạy khi DOM sẵn sàng
function setupHwSearchDomBindings() {
    const btnOpen = document.getElementById('btnOpenHwSearchModal');
    if (btnOpen) {
        btnOpen.addEventListener('click', (e) => {
            e.preventDefault();
            openHwSearchModal();
        });
    }

    const btnClose = document.getElementById('btnCloseHwSearchModal');
    if (btnClose) {
        btnClose.addEventListener('click', closeHwSearchModal);
    }

    // Đóng khi click ngoài backdrop
    const modal = document.getElementById('hwSearchModal');
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeHwSearchModal(e);
            }
        });
    }

    // Đóng bằng phím ESC
    window.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            const detailModal = document.getElementById('hwDetailModal');
            if (detailModal && detailModal.style.display === 'flex') {
                closeHwDetailModal(e);
                return;
            }
            if (modal && modal.style.display === 'flex') {
                closeHwSearchModal(e);
            }
        }
    });

    initHandwritingSearch();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupHwSearchDomBindings);
} else {
    setupHwSearchDomBindings();
}

// Xuất các hàm điều khiển ra Window toàn cục
window.initHandwritingSearch = initHandwritingSearch;
window.openHwSearchModal = openHwSearchModal;
window.closeHwSearchModal = closeHwSearchModal;
window.hwUndoStroke = hwUndoStroke;
window.hwClearCanvas = hwClearCanvas;
window.hwExecuteRecognition = hwExecuteRecognition;
window.openHwKanjiDetail = openHwKanjiDetail;
window.closeHwDetailModal = closeHwDetailModal;
window.practiceSelectedHwKanji = practiceSelectedHwKanji;
window.copyHwSelectedChar = copyHwSelectedChar;
