// Kanji Canvas Drawing Engine & Shodo AI Sensei Integration for KizunaTutor
document.addEventListener('DOMContentLoaded', () => {
    // 0. LÀM SẠCH CHUỖI RÁC ]> HOẶC DTD TRONG KHUNG SVG PREVIEW (TRIỆT TIÊU LỖI ]>)
    const svgPreviewEl = document.querySelector('.svg-preview');
    if (svgPreviewEl) {
        const rawHtml = svgPreviewEl.innerHTML;
        if (rawHtml.includes(']>') || rawHtml.includes('<?xml') || rawHtml.includes('<!DOCTYPE')) {
            const svgStartIdx = rawHtml.indexOf('<svg');
            const svgEndIdx = rawHtml.lastIndexOf('</svg>');
            if (svgStartIdx !== -1 && svgEndIdx !== -1) {
                svgPreviewEl.innerHTML = rawHtml.substring(svgStartIdx, svgEndIdx + 6);
            }
        }
    }

    const canvas = document.getElementById('kanjiCanvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let isDrawing = false;
    let strokes = [];
    let currentStroke = [];
    let isGuidelineVisible = false;
    let guideAnimTimer = null;
    let parsedStandardPaths = [];

    // 1. THIẾT LẬP KÍCH THƯỚC CANVAS CHUẨN ĐỘ PHÂN GIẢI CAO (High-DPI)
    function setupCanvasDpi() {
        const rect = canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 2;
        canvas.width = rect.width * dpr;
        canvas.height = rect.height * dpr;
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.scale(dpr, dpr);

        ctx.lineWidth = 6;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.strokeStyle = '#1B2A4A'; // Indigo Aizome
    }

    setupCanvasDpi();
    window.addEventListener('resize', () => {
        setupCanvasDpi();
        redrawAllStrokes();
    });

    function getPos(e) {
        const r = canvas.getBoundingClientRect();
        const clientX = e.touches ? e.touches[0].clientX : e.clientX;
        const clientY = e.touches ? e.touches[0].clientY : e.clientY;
        return {
            x: clientX - r.left,
            y: clientY - r.top,
            t: Date.now()
        };
    }

    function startDraw(e) {
        isDrawing = true;
        const pos = getPos(e);
        currentStroke = [pos];
        ctx.beginPath();
        ctx.moveTo(pos.x, pos.y);
        if (e.cancelable) e.preventDefault();
    }

    function draw(e) {
        if (!isDrawing) return;
        const pos = getPos(e);
        currentStroke.push(pos);
        ctx.lineTo(pos.x, pos.y);
        ctx.stroke();
        if (e.cancelable) e.preventDefault();
    }

    function stopDraw() {
        if (!isDrawing) return;
        isDrawing = false;
        if (currentStroke.length > 0) {
            strokes.push(currentStroke);
            currentStroke = [];
            updateStrokeCountDisplay();
        }
    }

    // Sự kiện chuột & cảm ứng
    canvas.addEventListener('mousedown', startDraw);
    canvas.addEventListener('mousemove', draw);
    window.addEventListener('mouseup', stopDraw);

    canvas.addEventListener('touchstart', startDraw, { passive: false });
    canvas.addEventListener('touchmove', draw, { passive: false });
    window.addEventListener('touchend', stopDraw);

    // =========================================================================
    // 2. VẼ LẠI TOÀN BỘ CÁC NÉT (REDRAW), HOÀN TÁC & XÓA BẢNG
    // =========================================================================
    function redrawAllStrokes() {
        const rect = canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 2;
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.scale(dpr, dpr);

        ctx.lineWidth = 6;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.strokeStyle = '#1B2A4A';

        for (const stroke of strokes) {
            if (stroke.length < 2) continue;
            ctx.beginPath();
            ctx.moveTo(stroke[0].x, stroke[0].y);
            for (let i = 1; i < stroke.length; i++) {
                ctx.lineTo(stroke[i].x, stroke[i].y);
            }
            ctx.stroke();
        }

        updateStrokeCountDisplay();
    }

    function undoLastStroke() {
        if (strokes.length > 0) {
            strokes.pop();
            redrawAllStrokes();
        }
    }

    function clearCanvas() {
        strokes = [];
        currentStroke = [];
        isDrawing = false;
        redrawAllStrokes();
    }

    const btnClearCanvas = document.getElementById('btnClearCanvas');
    if (btnClearCanvas) btnClearCanvas.addEventListener('click', clearCanvas);

    const btnUndoStroke = document.getElementById('btnUndoStroke');
    if (btnUndoStroke) btnUndoStroke.addEventListener('click', undoLastStroke);

    function updateStrokeCountDisplay() {
        const countEl = document.getElementById('drawnStrokeCount');
        const targetEl = document.getElementById('targetStrokeCount');
        const targetCount = targetEl ? parseInt(targetEl.textContent.trim(), 10) : 0;

        if (countEl) {
            countEl.textContent = strokes.length;
            if (targetCount > 0 && strokes.length === targetCount) {
                countEl.className = 'stroke-count-pill match';
            } else if (strokes.length > 0) {
                countEl.className = 'stroke-count-pill diff';
            } else {
                countEl.className = 'stroke-count-pill';
            }
        }
    }

    // =========================================================================
    // 3. CHUẨN BỊ LỚP NÉT MỜ (GHOST GUIDELINE) & KANJIVG SVG PARSING
    // =========================================================================
    const guideSvg = document.getElementById('canvasGuidelineSvg');
    const targetCharEl = document.getElementById('targetKanjiChar');
    const currentKanjiChar = targetCharEl ? targetCharEl.textContent.trim() : '愛';

    async function prepareCanvasGuideline() {
        if (!guideSvg) return;
        guideSvg.innerHTML = '';
        parsedStandardPaths = [];

        try {
            let svgXml = null;
            // 1. Ưu tiên đọc trực tiếp từ thẻ script template được nhúng sẵn từ server
            const rawSvgScript = document.getElementById('kanjiSvgRaw');
            if (rawSvgScript && rawSvgScript.textContent.trim().length > 50) {
                svgXml = rawSvgScript.textContent.trim();
            }

            // 2. Fallback gọi API backend nếu script rỗng
            if (!svgXml) {
                const res = await fetch(`/api/kanji/${encodeURIComponent(currentKanjiChar)}`);
                if (res.ok) {
                    const data = await res.json();
                    svgXml = data.svgData;
                }
            }

            if (svgXml) {
                const parser = new DOMParser();
                const doc = parser.parseFromString(svgXml, 'image/svg+xml');
                const paths = doc.querySelectorAll('path[id*="-s"]');
                const texts = doc.querySelectorAll('text');

                let bgPaths = '';
                let animPaths = '';
                let numTexts = '';

                paths.forEach((p, idx) => {
                    const d = p.getAttribute('d');
                    parsedStandardPaths.push(d);

                    // Lớp nét mờ tĩnh màu xám bạc
                    bgPaths += `<path d="${d}" stroke="#94A3B8" stroke-width="4.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />`;
                    // Lớp nét động màu đỏ Akane phục vụ hoạt họa xem lại bút thuận
                    animPaths += `<path id="guide-anim-p-${idx}" d="${d}" stroke="#C84B31" stroke-width="5.5" fill="none" stroke-linecap="round" stroke-linejoin="round" style="opacity: 0;" />`;
                });

                texts.forEach((t) => {
                    const tr = t.getAttribute('transform');
                    const txt = t.textContent;
                    numTexts += `<text transform="${tr}" fill="#E11D48" font-size="8.5" font-family="sans-serif" font-weight="bold">${txt}</text>`;
                });

                guideSvg.innerHTML = `
                    <g id="guideBgGroup">${bgPaths}</g>
                    <g id="guideAnimGroup">${animPaths}</g>
                    <g id="guideNumGroup" style="opacity: 0.9;">${numTexts}</g>
                `;
            }
        } catch (err) {
            console.warn('Lỗi phân tích nét mờ KanjiVG:', err);
        }
    }

    prepareCanvasGuideline();

    // =========================================================================
    // 4. CHỨC NĂNG 1: ẨN / HIỆN CÁC NÉT MỜ (TOGGLE GUIDELINE)
    // =========================================================================
    const btnToggleGuideline = document.getElementById('btnToggleGuideline');

    function toggleCanvasGuideline() {
        if (!guideSvg || !btnToggleGuideline) return;
        isGuidelineVisible = !isGuidelineVisible;

        if (isGuidelineVisible) {
            guideSvg.style.display = 'block';
            btnToggleGuideline.innerHTML = '👁️ Ẩn nét mờ';
            btnToggleGuideline.classList.add('active');
        } else {
            guideSvg.style.display = 'none';
            btnToggleGuideline.innerHTML = '👁️ Hiện nét mờ';
            btnToggleGuideline.classList.remove('active');
        }
    }

    if (btnToggleGuideline) {
        btnToggleGuideline.addEventListener('click', toggleCanvasGuideline);
    }

    // =========================================================================
    // 5. CHỨC NĂNG 2: XEM LẠI CÁCH VIẾT / THỨ TỰ BÚT THUẬN (REPLAY ANIMATION)
    // =========================================================================
    const btnAnimateGuide = document.getElementById('btnAnimateGuide');

    function replayCanvasGuideAnimation() {
        if (!guideSvg) return;

        // Tự động bật nét mờ nếu đang bị ẩn
        guideSvg.style.display = 'block';
        if (btnToggleGuideline) {
            btnToggleGuideline.innerHTML = '👁️ Ẩn nét mờ';
            btnToggleGuideline.classList.add('active');
            isGuidelineVisible = true;
        }

        if (guideAnimTimer) {
            clearTimeout(guideAnimTimer);
            guideAnimTimer = null;
        }

        const animPaths = guideSvg.querySelectorAll('path[id^="guide-anim-p-"]');
        if (!animPaths || animPaths.length === 0) return;

        // Reset toàn bộ các nét animated
        animPaths.forEach(p => {
            p.style.opacity = '0';
            p.style.transition = 'none';
        });

        let currentIdx = 0;

        function animateNext() {
            if (currentIdx < animPaths.length) {
                const p = animPaths[currentIdx];
                const len = p.getTotalLength() || 100;
                p.style.strokeDasharray = `${len} ${len}`;
                p.style.strokeDashoffset = `${len}`;
                p.style.opacity = '1';
                p.getBoundingClientRect(); // trigger reflow
                p.style.transition = 'stroke-dashoffset 0.45s ease-in-out';
                p.style.strokeDashoffset = '0';

                currentIdx++;
                guideAnimTimer = setTimeout(animateNext, 550);
            } else {
                // Giữ lại 1.2 giây sau khi phát xong rồi mờ dần
                guideAnimTimer = setTimeout(() => {
                    animPaths.forEach(p => {
                        p.style.transition = 'opacity 0.6s ease';
                        p.style.opacity = '0';
                    });
                }, 1200);
            }
        }

        animateNext();
    }

    if (btnAnimateGuide) {
        btnAnimateGuide.addEventListener('click', replayCanvasGuideAnimation);
    }

    // =========================================================================
    // 6. CHỨC NĂNG 3: HOÀN TẤT NÉT / TỰ ĐỘNG VẼ NÉT TIẾP THEO (COMPLETE STROKE)
    // =========================================================================
    const btnCompleteStroke = document.getElementById('btnCompleteStroke');

    function completeNextStroke() {
        if (!guideSvg) return;
        const animPaths = guideSvg.querySelectorAll('path[id^="guide-anim-p-"]');
        if (!animPaths || animPaths.length === 0) {
            alert('Chưa tải được dữ liệu nét chuẩn của chữ này.');
            return;
        }

        const nextStrokeIdx = strokes.length;
        if (nextStrokeIdx >= animPaths.length) {
            alert('🎉 Bạn đã vẽ đủ tất cả các nét chuẩn của chữ này rồi!');
            return;
        }

        const targetPath = animPaths[nextStrokeIdx];
        const totalLen = targetPath.getTotalLength();
        if (!totalLen || totalLen <= 0) return;

        // Lấy tỷ lệ co giãn giữa viewBox 109x109 và kích thước logic của Canvas
        const rect = canvas.getBoundingClientRect();
        const scaleX = rect.width / 109.0;
        const scaleY = rect.height / 109.0;

        // Lấy mẫu 25 điểm cách đều dọc theo đường cong SVG của nét chuẩn
        const sampledPoints = [];
        const sampleSteps = 24;
        for (let i = 0; i <= sampleSteps; i++) {
            const pt = targetPath.getPointAtLength((i / sampleSteps) * totalLen);
            sampledPoints.push({
                x: pt.x * scaleX,
                y: pt.y * scaleY,
                t: Date.now() + i * 15
            });
        }

        // Đẩy nét vào danh sách strokes
        strokes.push(sampledPoints);

        // Hiệu ứng vẽ nét mượt mà lên canvas
        animateSingleStrokeToCanvas(sampledPoints, () => {
            updateStrokeCountDisplay();
        });
    }

    function animateSingleStrokeToCanvas(strokePoints, onComplete) {
        if (!strokePoints || strokePoints.length < 2) return;
        let step = 1;

        ctx.strokeStyle = '#1B2A4A';
        ctx.lineWidth = 6;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';

        function drawNextStep() {
            if (step < strokePoints.length) {
                ctx.beginPath();
                ctx.moveTo(strokePoints[step - 1].x, strokePoints[step - 1].y);
                ctx.lineTo(strokePoints[step].x, strokePoints[step].y);
                ctx.stroke();
                step++;
                requestAnimationFrame(drawNextStep);
            } else {
                if (typeof onComplete === 'function') onComplete();
            }
        }

        drawNextStep();
    }

    if (btnCompleteStroke) {
        btnCompleteStroke.addEventListener('click', completeNextStroke);
    }

    // =========================================================================
    // 7. CẤU HÌNH KHÓA AI (GEMINI / OPENAI) & KIỂM TRA TRẠNG THÁI SERVER
    // =========================================================================
    const btnAiKeySettings = document.getElementById('btnAiKeySettings');
    const aiKeyModal = document.getElementById('aiKeyModal');
    const btnCloseAiModal = document.getElementById('btnCloseAiModal');
    const btnSaveApiKey = document.getElementById('btnSaveApiKey');
    const btnClearSavedApiKey = document.getElementById('btnClearSavedApiKey');
    const modalApiKeyInput = document.getElementById('modalApiKeyInput');
    const modalAiProvider = document.getElementById('modalAiProvider');
    const aiStatusDot = document.getElementById('aiStatusDot');
    const aiStatusText = document.getElementById('aiStatusText');

    let serverHasKey = false;

    async function checkAiStatus() {
        try {
            const res = await fetch('/api/kanji/ai-status');
            if (res.ok) {
                const data = await res.json();
                serverHasKey = !!data.hasServerKey;
            }
        } catch (e) {
            console.log('Không thể kiểm tra server ai status:', e);
        }

        const localKey = localStorage.getItem('kizuna_ai_api_key');
        const localProvider = localStorage.getItem('kizuna_ai_provider') || 'GEMINI';

        if (localKey && localKey.trim()) {
            aiStatusDot.className = 'ai-status-dot active';
            aiStatusText.textContent = `AI Sẵn Sàng (${localProvider} - Khóa Người Dùng)`;
            if (modalApiKeyInput) modalApiKeyInput.value = localKey;
            if (modalAiProvider) modalAiProvider.value = localProvider;
        } else if (serverHasKey) {
            aiStatusDot.className = 'ai-status-dot active';
            aiStatusText.textContent = 'AI Sẵn Sàng (Khóa Hệ Thống)';
        } else {
            aiStatusDot.className = 'ai-status-dot';
            aiStatusText.textContent = 'Chưa Cài Đặt AI Key';
        }
    }

    checkAiStatus();

    if (btnAiKeySettings && aiKeyModal) {
        btnAiKeySettings.addEventListener('click', () => {
            const localKey = localStorage.getItem('kizuna_ai_api_key') || '';
            const localProvider = localStorage.getItem('kizuna_ai_provider') || 'GEMINI';
            if (modalApiKeyInput) modalApiKeyInput.value = localKey;
            if (modalAiProvider) modalAiProvider.value = localProvider;
            aiKeyModal.style.display = 'flex';
        });
    }

    if (btnCloseAiModal && aiKeyModal) {
        btnCloseAiModal.addEventListener('click', () => {
            aiKeyModal.style.display = 'none';
        });
    }

    if (btnSaveApiKey) {
        btnSaveApiKey.addEventListener('click', () => {
            const key = modalApiKeyInput.value.trim();
            const provider = modalAiProvider.value;
            if (key) {
                localStorage.setItem('kizuna_ai_api_key', key);
                localStorage.setItem('kizuna_ai_provider', provider);
                alert('🎉 Đã lưu và kích hoạt khóa AI thành công!');
            } else {
                localStorage.removeItem('kizuna_ai_api_key');
                localStorage.setItem('kizuna_ai_provider', provider);
                alert('Đã xóa khóa cá nhân. Hệ thống sẽ sử dụng khóa máy chủ hoặc từ điển Topo.');
            }
            aiKeyModal.style.display = 'none';
            checkAiStatus();
        });
    }

    if (btnClearSavedApiKey) {
        btnClearSavedApiKey.addEventListener('click', () => {
            localStorage.removeItem('kizuna_ai_api_key');
            if (modalApiKeyInput) modalApiKeyInput.value = '';
            alert('Đã xóa khóa API lưu trên trình duyệt.');
            checkAiStatus();
        });
    }

    window.addEventListener('click', (e) => {
        if (e.target === aiKeyModal) {
            aiKeyModal.style.display = 'none';
        }
    });

    // =========================================================================
    // 8. CHẤM ĐIỂM NÉT VẼ AI (CALL /api/kanji/grade)
    // =========================================================================
    const btnGrade = document.getElementById('btnGradeCanvas');
    const aiLoadingOverlay = document.getElementById('aiLoadingOverlay');
    const evaluationResultModal = document.getElementById('evaluationResultModal');
    const btnCloseResultModal = document.getElementById('btnCloseResultModal');
    const btnDoneResult = document.getElementById('btnDoneResult');
    const btnRetryWrite = document.getElementById('btnRetryWrite');

    if (btnGrade) {
        btnGrade.addEventListener('click', async () => {
            if (strokes.length === 0) {
                alert('✍️ Bạn chưa vẽ nét nào trên bảng! Hãy dùng chuột hoặc bút cảm ứng để viết chữ vào ô Mễ trước khi chấm điểm.');
                return;
            }

            const kanji = currentKanjiChar;
            const rect = canvas.getBoundingClientRect();
            const canvasLogicalW = Math.round(rect.width || 270);
            const canvasLogicalH = Math.round(rect.height || 270);

            const localApiKey = localStorage.getItem('kizuna_ai_api_key') || '';
            const localProvider = localStorage.getItem('kizuna_ai_provider') || 'GEMINI';

            let canvasImageData = '';
            try {
                canvasImageData = canvas.toDataURL('image/png');
            } catch (err) {
                console.log('Không thể export canvas toDataURL:', err);
            }

            const payload = {
                kanji: kanji,
                userStrokes: strokes.length,
                strokeCoordinatesJson: JSON.stringify(strokes),
                canvasWidth: canvasLogicalW,
                canvasHeight: canvasLogicalH,
                apiKey: localApiKey,
                provider: localProvider,
                canvasImageData: canvasImageData
            };

            if (aiLoadingOverlay) aiLoadingOverlay.style.display = 'flex';
            btnGrade.disabled = true;

            try {
                const response = await fetch('/api/kanji/grade', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) {
                    throw new Error(`Máy chủ phản hồi mã lỗi ${response.status}`);
                }

                const result = await response.json();
                renderEvaluationResult(result);
            } catch (error) {
                console.error('Lỗi khi chấm điểm nét vẽ:', error);
                alert(`⚠️ Không thể chấm điểm nét vẽ: ${error.message}. Vui lòng thử lại!`);
            } finally {
                if (aiLoadingOverlay) aiLoadingOverlay.style.display = 'none';
                btnGrade.disabled = false;
            }
        });
    }

    function renderEvaluationResult(res) {
        if (!evaluationResultModal) return;

        const resKanjiChar = document.getElementById('resKanjiChar');
        if (resKanjiChar) resKanjiChar.textContent = res.kanji || '—';

        const resTotalScore = document.getElementById('resTotalScore');
        if (resTotalScore) resTotalScore.textContent = res.totalScorePercent != null ? res.totalScorePercent : 0;

        const resGradeBadge = document.getElementById('resGradeBadge');
        if (resGradeBadge) resGradeBadge.textContent = res.grade || 'Đạt chuẩn';

        const resGradeTitle = document.getElementById('resGradeTitle');
        if (resGradeTitle) resGradeTitle.textContent = res.grade || 'Kết Quả Đánh Giá Nét Chữ';

        const resOverallFeedback = document.getElementById('resOverallFeedback');
        if (resOverallFeedback) resOverallFeedback.textContent = res.feedback || '';

        const resAiFeedback = document.getElementById('resAiFeedback');
        if (resAiFeedback) {
            resAiFeedback.textContent = res.aiFeedback || 'Hệ thống đã hoàn tất chấm điểm nét vẽ.';
        }

        if (res.strokeCriterion) {
            const crit1Score = document.getElementById('crit1Score');
            const crit1Bar = document.getElementById('crit1Bar');
            if (crit1Score) crit1Score.textContent = `${res.strokeCriterion.score}/${res.strokeCriterion.maxScore}đ (${res.strokeCriterion.percentage}%)`;
            if (crit1Bar) crit1Bar.style.width = `${res.strokeCriterion.percentage}%`;
        }

        if (res.balanceCriterion) {
            const crit2Score = document.getElementById('crit2Score');
            const crit2Bar = document.getElementById('crit2Bar');
            if (crit2Score) crit2Score.textContent = `${res.balanceCriterion.score}/${res.balanceCriterion.maxScore}đ (${res.balanceCriterion.percentage}%)`;
            if (crit2Bar) crit2Bar.style.width = `${res.balanceCriterion.percentage}%`;
        }

        if (res.trajectoryCriterion) {
            const crit3Score = document.getElementById('crit3Score');
            const crit3Bar = document.getElementById('crit3Bar');
            if (crit3Score) crit3Score.textContent = `${res.trajectoryCriterion.score}/${res.trajectoryCriterion.maxScore}đ (${res.trajectoryCriterion.percentage}%)`;
            if (crit3Bar) crit3Bar.style.width = `${res.trajectoryCriterion.percentage}%`;
        }

        const boxDeductions = document.getElementById('boxDeductions');
        const resDeductionsList = document.getElementById('resDeductionsList');
        if (boxDeductions && resDeductionsList) {
            if (res.deductions && res.deductions.length > 0) {
                boxDeductions.style.display = 'block';
                resDeductionsList.innerHTML = res.deductions.map(d => `
                    <div class="deduction-item">
                        <span class="badge-penalty">${d.penalty}đ</span>
                        <div>
                            <strong>${d.title}:</strong> ${d.description}
                        </div>
                    </div>
                `).join('');
            } else {
                boxDeductions.style.display = 'none';
                resDeductionsList.innerHTML = '';
            }
        }

        const boxTips = document.getElementById('boxTips');
        const resTipsList = document.getElementById('resTipsList');
        if (boxTips && resTipsList) {
            if (res.actionableTips && res.actionableTips.length > 0) {
                resTipsList.innerHTML = res.actionableTips.map(t => `
                    <div class="tip-item">
                        <span>${t.icon || '💡'}</span>
                        <div>
                            <strong>${t.target}:</strong> ${t.tipText}
                        </div>
                    </div>
                `).join('');
            } else {
                resTipsList.innerHTML = '<div class="tip-item"><span>🌸</span><div>Hãy tiếp tục giữ vững phong độ viết chữ!</div></div>';
            }
        }

        evaluationResultModal.style.display = 'flex';
    }

    function closeResultModal() {
        if (evaluationResultModal) evaluationResultModal.style.display = 'none';
    }

    if (btnCloseResultModal) btnCloseResultModal.addEventListener('click', closeResultModal);
    if (btnDoneResult) btnDoneResult.addEventListener('click', closeResultModal);

    if (btnRetryWrite) {
        btnRetryWrite.addEventListener('click', () => {
            closeResultModal();
            clearCanvas();
        });
    }

    window.addEventListener('click', (e) => {
        if (e.target === evaluationResultModal) {
            closeResultModal();
        }
    });
});
