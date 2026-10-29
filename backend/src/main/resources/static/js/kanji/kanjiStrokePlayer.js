/**
 * ============================================================================
 * KANJI STROKE ORDER PLAYER (BỘ ĐIỀU KHIỂN HOẠT ẢNH BÚT THUẬN SIÊU MƯỢT 60FPS)
 * KizunaTutor Design System - Chuẩn Thư Pháp Nhaitopik
 * ============================================================================
 * Tối ưu hóa hiệu năng GPU 60fps/120fps:
 * 1. Pre-calculation: Đo và tính toán path.getTotalLength() DUY NHẤT 1 LẦN khi nạp chữ.
 *    Tuyệt đối không đo đạc (không gọi getTotalLength / getBoundingClientRect) khi đang chạy.
 * 2. GPU Hardware Acceleration: Dùng CSS transition: stroke-dashoffset {duration}ms cubic-bezier(...)
 *    kết hợp will-change: stroke-dashoffset và transform: translateZ(0).
 *    Giao phó toàn bộ việc nội suy chuyển động cho Compositor Thread của GPU.
 * 3. Layer Separation: Tách riêng 3 tầng độc lập:
 *    - Layer 1 (Tĩnh): Ô Mễ kẻ đứt nét.
 *    - Layer 2 (Động): SVG Stroke Player phát hoạt ảnh bút thuận.
 *    - Layer 3 (Tương tác): Canvas 2D để học viên tự luyện viết.
 * 4. Tự động Fade-out: Giữ 1.2s -> Mờ dần 0.5s -> Xóa sạch trả lại ô Mễ tinh khôi.
 * 5. Nạp động 100% chữ Kanji qua cơ chế 3 tầng (Cache -> API Backend -> KanjiVG CDN).
 * ============================================================================
 */

class KanjiStrokePlayer {
    /**
     * Chuyển đổi ký tự Kanji thành mã 5 chữ số hexa chuẩn của KanjiVG (Ví dụ: '忖' -> '05fd6')
     */
    static getKanjiUnicodeHex(char) {
        if (!char || typeof char !== 'string') return '00000';
        const codePoint = char.codePointAt(0);
        return codePoint.toString(16).padStart(5, '0').toLowerCase();
    }

    /**
     * Tải dữ liệu nét SVG động theo cơ chế Cache & Fallback 3 tầng
     */
    static async loadStrokeData(kanjiChar) {
        if (!kanjiChar || typeof kanjiChar !== 'string') return null;
        const char = kanjiChar.trim();

        // 1. Kiểm tra RAM Cache
        window.kanjiStrokeCache = window.kanjiStrokeCache || {};
        if (window.kanjiStrokeCache[char]) {
            return window.kanjiStrokeCache[char];
        }

        const unicodeHex = KanjiStrokePlayer.getKanjiUnicodeHex(char);

        // 2. Thử gọi API Backend theo ký tự
        try {
            const resp = await fetch(`/api/kanji/${encodeURIComponent(char)}`);
            if (resp.ok) {
                const data = await resp.json();
                if (data.svgData && data.svgData.length > 50) {
                    window.kanjiStrokeCache[char] = data.svgData;
                    return data.svgData;
                }
            }
        } catch (e) {}

        // 3. Thử gọi API Backend theo mã Unicode Hex
        try {
            const hexResp = await fetch(`/api/kanji/hex/${unicodeHex}`);
            if (hexResp.ok) {
                const hexData = await hexResp.json();
                if (hexData.svgData && hexData.svgData.length > 50) {
                    window.kanjiStrokeCache[char] = hexData.svgData;
                    return hexData.svgData;
                }
            }
        } catch (e) {}

        // 4. Fallback tải trực tiếp từ KanjiVG CDN theo mã Hex
        try {
            const cdnUrl = `https://cdn.jsdelivr.net/gh/KanjiVG/kanjivg@master/kanji/${unicodeHex}.svg`;
            const cdnResp = await fetch(cdnUrl);
            if (cdnResp.ok) {
                const svgText = await cdnResp.text();
                if (svgText && svgText.includes('<path')) {
                    window.kanjiStrokeCache[char] = svgText;
                    return svgText;
                }
            }
        } catch (e) {}

        return null;
    }

    /**
     * @param {Object} options Cấu hình khởi tạo
     * @param {SVGElement|string} options.target Phần tử SVG hoặc ID của SVG container
     * @param {number} [options.baseDuration=560] Thời gian vẽ chuẩn cho 1 nét (ms)
     * @param {number} [options.pauseBetweenStrokes=120] Nghỉ giữa 2 nét (ms)
     * @param {number} [options.speed=1.0] Tốc độ (0.5x, 1.0x, 1.5x)
     * @param {boolean} [options.autoFadeOutOnComplete=true] Tự động fade-out xóa nét sau khi vẽ xong
     * @param {number} [options.holdDuration=1200] Thời gian giữ nguyên chữ hoàn chỉnh trước khi fade-out (ms)
     * @param {number} [options.fadeOutDuration=500] Thời gian diễn ra hiệu ứng mờ dần (ms)
     * @param {string} [options.activeStrokeColor='#15803D'] Màu nét cọ đang vẽ (Uguisu-iro 15803D)
     * @param {string} [options.completedStrokeColor='#0F172A'] Màu mực nét đã vẽ xong (Mực đen than 0F172A)
     * @param {string} [options.ghostStrokeColor='#CBD5E1'] Màu nét bóng mờ xám (Slate 300)
     * @param {string} [options.numberBadgeColor='#15803D'] Màu huy hiệu số thứ tự nét (Uguisu-iro 15803D)
     * @param {Function} [options.onStateChange] Callback khi trạng thái đổi (idle, playing, paused, completed)
     * @param {Function} [options.onStrokeChange] Callback khi đang vẽ nét (current, total, phase)
     * @param {Function} [options.onComplete] Callback khi hoàn tất (total, autoFaded)
     */
    constructor(options = {}) {
        this.targetEl = typeof options.target === 'string'
            ? document.getElementById(options.target)
            : options.target;

        if (!this.targetEl) {
            console.error('[KanjiStrokePlayer] Không tìm thấy phần tử target SVG:', options.target);
            return;
        }

        this.baseDuration = options.baseDuration || 560;
        this.pauseBetween = options.pauseBetweenStrokes || 120;
        this.speed = options.speed || 1.0;

        this.autoFadeOutOnComplete = options.autoFadeOutOnComplete !== undefined ? options.autoFadeOutOnComplete : true;
        this.holdDuration = options.holdDuration || 1200; // Giữ 1.2s
        this.fadeOutDuration = options.fadeOutDuration || 500; // Mờ dần trong 0.5s

        this.activeColor = options.activeStrokeColor || '#15803D';
        this.completedColor = options.completedStrokeColor || '#0F172A';
        this.ghostColor = options.ghostStrokeColor || '#CBD5E1';
        this.badgeColor = options.numberBadgeColor || '#15803D';

        this.onStateChange = options.onStateChange || null;
        this.onStrokeChange = options.onStrokeChange || null;
        this.onComplete = options.onComplete || null;

        // Trạng thái nội bộ
        this.state = 'idle'; // 'idle' | 'playing' | 'paused' | 'completed'
        this.strokes = [];
        this.currentStrokeIndex = 0;
        this.pauseTimeoutId = null;
        this.strokeTimeoutId = null;
        this.fadeOutTimer = null;
        this.cleanupTimer = null;
        this.isGhostVisible = false;

        // Lớp đồ họa SVG phân cấp
        this.gridGroup = null;
        this.ghostGroup = null;
        this.animGroup = null;
        this.numberGroup = null;

        this.setupSvgContainer();
    }

    /**
     * Khởi tạo cấu trúc ViewBox và kích hoạt tăng tốc phần cứng GPU
     */
    setupSvgContainer() {
        this.targetEl.setAttribute('viewBox', '0 0 109 109');
        this.targetEl.setAttribute('preserveAspectRatio', 'xMidYMid meet');
        this.targetEl.innerHTML = '';

        // Kích hoạt GPU Compositing Layer riêng biệt
        this.targetEl.style.willChange = 'transform';
        this.targetEl.style.transform = 'translateZ(0)';
        this.targetEl.style.backfaceVisibility = 'hidden';

        // 1. Lớp lưới ô Mễ tự cách (米字格) viền đứt nét tĩnh (Layer 1)
        this.gridGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        this.gridGroup.setAttribute('id', 'ksp-rice-grid');
        this.renderRiceGrid();
        this.targetEl.appendChild(this.gridGroup);

        // 2. Lớp nét bóng mờ tĩnh xám nhạt (ghost layer)
        this.ghostGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        this.ghostGroup.setAttribute('id', 'ksp-ghost-layer');
        this.ghostGroup.style.display = 'none';
        this.ghostGroup.style.opacity = '0.35';
        this.targetEl.appendChild(this.ghostGroup);

        // 3. Lớp nét động hoạt họa GPU (Layer 2)
        this.animGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        this.animGroup.setAttribute('id', 'ksp-anim-layer');
        this.animGroup.style.willChange = 'opacity, transform';
        this.animGroup.style.transform = 'translateZ(0)';
        this.targetEl.appendChild(this.animGroup);

        // 4. Lớp huy hiệu đánh số thứ tự nét
        this.numberGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        this.numberGroup.setAttribute('id', 'ksp-number-layer');
        this.numberGroup.style.willChange = 'opacity';
        this.targetEl.appendChild(this.numberGroup);
    }

    renderRiceGrid() {
        this.gridGroup.innerHTML = `
            <rect x="4.5" y="4.5" width="100" height="100" fill="none" stroke="#E2E8F0" stroke-width="1.2" />
            <line x1="54.5" y1="4.5" x2="54.5" y2="104.5" stroke="#CBD5E1" stroke-width="0.8" stroke-dasharray="3,3" />
            <line x1="4.5" y1="54.5" x2="104.5" y2="54.5" stroke="#CBD5E1" stroke-width="0.8" stroke-dasharray="3,3" />
            <line x1="4.5" y1="4.5" x2="104.5" y2="104.5" stroke="#E2E8F0" stroke-width="0.75" stroke-dasharray="4,4" />
            <line x1="104.5" y1="4.5" x2="4.5" y2="104.5" stroke="#E2E8F0" stroke-width="0.75" stroke-dasharray="4,4" />
        `;
    }

    /**
     * Nạp dữ liệu nét và đo đạc độ dài nét DUY NHẤT 1 LẦN (Pre-calculation)
     * Tuyệt đối không đo đạc DOM khi hoạt ảnh đang phát.
     */
    loadSvg(svgXml) {
        this.stop();
        this.cancelFadeOut();

        this.strokes = [];
        this.ghostGroup.innerHTML = '';
        this.animGroup.innerHTML = '';
        this.numberGroup.innerHTML = '';
        this.currentStrokeIndex = 0;

        // Reset độ mờ
        this.animGroup.style.transition = 'none';
        this.numberGroup.style.transition = 'none';
        this.ghostGroup.style.transition = 'none';
        this.animGroup.style.opacity = '1';
        this.numberGroup.style.opacity = '1';

        if (!svgXml || typeof svgXml !== 'string' || svgXml.trim().length < 40) {
            this.setState('idle');
            return false;
        }

        try {
            const parser = new DOMParser();
            const doc = parser.parseFromString(svgXml, 'image/svg+xml');

            let pathNodes = doc.querySelectorAll('g[id*="StrokePaths"] path');
            if (pathNodes.length === 0) pathNodes = doc.querySelectorAll('path[id*="-s"]');
            if (pathNodes.length === 0) pathNodes = doc.querySelectorAll('path[d]');

            if (pathNodes.length === 0) return false;

            // Đọc vị trí các số thứ tự nét từ thẻ <text>
            const textNodes = doc.querySelectorAll('text');
            const strokeNumbersMap = [];
            textNodes.forEach(t => {
                const tr = t.getAttribute('transform');
                const val = t.textContent ? t.textContent.trim() : '';
                let x = null, y = null;

                if (tr && tr.includes('matrix')) {
                    const match = tr.match(/matrix\s*\(([^)]+)\)/);
                    if (match) {
                        const parts = match[1].split(/[\s,]+/).map(parseFloat);
                        if (parts.length >= 6) { x = parts[4]; y = parts[5]; }
                    }
                } else if (t.hasAttribute('x') && t.hasAttribute('y')) {
                    x = parseFloat(t.getAttribute('x'));
                    y = parseFloat(t.getAttribute('y'));
                }

                if (val && x !== null && y !== null) {
                    strokeNumbersMap.push({ num: parseInt(val, 10) || val, x, y });
                }
            });

            pathNodes.forEach((node, index) => {
                const d = node.getAttribute('d');
                if (!d || d.trim().length === 0) return;

                // 1. Nét bóng mờ tĩnh xám
                const ghostPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                ghostPath.setAttribute('d', d);
                ghostPath.setAttribute('fill', 'none');
                ghostPath.setAttribute('stroke', this.ghostColor);
                ghostPath.setAttribute('stroke-width', '4.8');
                ghostPath.setAttribute('stroke-linecap', 'round');
                ghostPath.setAttribute('stroke-linejoin', 'round');
                this.ghostGroup.appendChild(ghostPath);

                // 2. Nét động hoạt họa
                const animPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                animPath.setAttribute('d', d);
                animPath.setAttribute('fill', 'none');
                animPath.setAttribute('stroke', this.completedColor);
                animPath.setAttribute('stroke-width', '5.2');
                animPath.setAttribute('stroke-linecap', 'round');
                animPath.setAttribute('stroke-linejoin', 'round');
                animPath.classList.add('ksp-anim-path');

                // Kích hoạt tối ưu phần cứng GPU cho từng nét
                animPath.style.willChange = 'stroke-dashoffset';
                animPath.style.transform = 'translateZ(0)';
                animPath.style.backfaceVisibility = 'hidden';

                this.animGroup.appendChild(animPath);

                // Đo chiều dài nét DUY NHẤT 1 LẦN TRƯỚC KHI CHẠY (Pre-calculation)
                const len = Math.ceil(animPath.getTotalLength() || 100);
                animPath.style.strokeDasharray = `${len} ${len}`;
                animPath.style.strokeDashoffset = `${len}`;
                animPath.style.opacity = '1';

                // Xác định tọa độ huy hiệu số thứ tự nét
                const strokeNum = index + 1;
                let numX = 10, numY = 10;
                const matched = strokeNumbersMap.find(m => m.num === strokeNum);

                if (matched) {
                    numX = matched.x;
                    numY = matched.y;
                } else {
                    try {
                        const pt = animPath.getPointAtLength(0);
                        numX = Math.max(5, Math.min(100, pt.x - 3));
                        numY = Math.max(8, Math.min(102, pt.y - 2));
                    } catch (e) {
                        const mMatch = d.match(/M\s*([\d.]+)[,\s]+([\d.]+)/i);
                        if (mMatch) {
                            numX = parseFloat(mMatch[1]) - 3;
                            numY = parseFloat(mMatch[2]) - 2;
                        }
                    }
                }

                // Huy hiệu số thứ tự nét
                const badgeGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                badgeGroup.style.opacity = '0';
                badgeGroup.style.transition = 'opacity 0.2s ease';
                badgeGroup.style.pointerEvents = 'none';

                const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                circle.setAttribute('cx', (numX + 2.5).toFixed(1));
                circle.setAttribute('cy', (numY - 3).toFixed(1));
                circle.setAttribute('r', '4.2');
                circle.setAttribute('fill', '#FFFFFF');
                circle.setAttribute('stroke', this.badgeColor);
                circle.setAttribute('stroke-width', '0.9');

                const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                text.setAttribute('x', (numX + 2.5).toFixed(1));
                text.setAttribute('y', (numY - 1).toFixed(1));
                text.setAttribute('text-anchor', 'middle');
                text.setAttribute('fill', this.badgeColor);
                text.setAttribute('font-size', '6.8');
                text.setAttribute('font-family', 'ui-sans-serif, system-ui, sans-serif');
                text.setAttribute('font-weight', 'bold');
                text.textContent = strokeNum.toString();

                badgeGroup.appendChild(circle);
                badgeGroup.appendChild(text);
                this.numberGroup.appendChild(badgeGroup);

                this.strokes.push({
                    index,
                    length: len,
                    pathEl: animPath,
                    ghostEl: ghostPath,
                    badgeEl: badgeGroup
                });
            });

            this.setState('idle');
            return true;
        } catch (err) {
            console.error('[KanjiStrokePlayer] Lỗi nạp SVG:', err);
            return false;
        }
    }

    /**
     * Khởi động phát hoạt ảnh nét vẽ mượt mà 60fps/120fps bằng GPU Compositor
     */
    play() {
        if (this.strokes.length === 0) return;
        this.cancelFadeOut();

        // Khôi phục hiển thị layer
        this.animGroup.style.transition = 'none';
        this.numberGroup.style.transition = 'none';
        this.animGroup.style.opacity = '1';
        this.numberGroup.style.opacity = '1';

        if (this.state === 'completed') {
            this.reset();
        }

        if (this.state === 'playing') return;

        this.setState('playing');
        this.playNextStroke();
    }

    /**
     * Chạy tuần tự từng nét bằng CSS Transition trên luồng GPU Compositor
     * Loại bỏ hoàn toàn vòng lặp JavaScript tính tay từng pixel.
     */
    playNextStroke() {
        if (this.currentStrokeIndex >= this.strokes.length || this.state !== 'playing') {
            this.handlePlaybackComplete();
            return;
        }

        const current = this.strokes[this.currentStrokeIndex];
        const { pathEl, length, badgeEl } = current;

        // 1. Đổi màu nét đang vẽ (Xanh rêu Shodo hoặc Cam san hô)
        pathEl.setAttribute('stroke', this.activeColor);
        // 2. Hiện số thứ tự nét
        badgeEl.style.opacity = '1';

        if (this.onStrokeChange) {
            this.onStrokeChange(this.currentStrokeIndex + 1, this.strokes.length, 'drawing');
        }

        // 3. Tính thời gian vẽ dựa trên chiều dài nét (nét dài vẽ lâu hơn, nét ngắn vẽ nhanh hơn)
        // Hệ số tốc độ speed chia trực tiếp vào duration
        const baseDuration = Math.max(260, Math.min(750, length * 2.3));
        const duration = Math.round(baseDuration / this.speed);

        // 4. Kích hoạt GPU Transition mượt mà tuyệt đối (cubic-bezier Shodo chuẩn mực)
        pathEl.style.transition = `stroke-dashoffset ${duration}ms cubic-bezier(0.25, 0.1, 0.25, 1)`;

        // Kích hoạt khung hình vẽ nét trong rAF tiếp theo của màn hình
        requestAnimationFrame(() => {
            pathEl.style.strokeDashoffset = '0';
        });

        // 5. Lắng nghe transitionend để chuyển sang nét tiếp theo
        const onEnd = (e) => {
            if (e && e.target !== pathEl) return;
            if (e && e.propertyName && e.propertyName !== 'stroke-dashoffset') return;

            pathEl.removeEventListener('transitionend', onEnd);
            clearTimeout(this.strokeTimeoutId);
            this.strokeTimeoutId = null;

            // Nét vẽ xong chuyển sang mực đen mun thư pháp
            pathEl.setAttribute('stroke', this.completedColor);
            this.currentStrokeIndex++;

            if (this.onStrokeChange) {
                this.onStrokeChange(this.currentStrokeIndex, this.strokes.length, 'completed');
            }

            if (this.currentStrokeIndex < this.strokes.length && this.state === 'playing') {
                const pauseTime = Math.round(this.pauseBetween / this.speed);
                this.pauseTimeoutId = setTimeout(() => {
                    this.playNextStroke();
                }, pauseTime);
            } else {
                this.handlePlaybackComplete();
            }
        };

        pathEl.addEventListener('transitionend', onEnd);
        // Fallback timer an toàn đề phòng trình duyệt bỏ qua transitionend khi đổi tab
        this.strokeTimeoutId = setTimeout(onEnd, duration + 60);
    }

    pause() {
        if (this.state !== 'playing') return;

        if (this.pauseTimeoutId) {
            clearTimeout(this.pauseTimeoutId);
            this.pauseTimeoutId = null;
        }
        if (this.strokeTimeoutId) {
            clearTimeout(this.strokeTimeoutId);
            this.strokeTimeoutId = null;
        }

        this.setState('paused');
    }

    togglePlayPause() {
        if (this.state === 'playing') {
            this.pause();
        } else {
            this.play();
        }
    }

    replay() {
        this.stop();
        this.cancelFadeOut();
        this.reset();
        this.play();
    }

    stop() {
        if (this.pauseTimeoutId) {
            clearTimeout(this.pauseTimeoutId);
            this.pauseTimeoutId = null;
        }
        if (this.strokeTimeoutId) {
            clearTimeout(this.strokeTimeoutId);
            this.strokeTimeoutId = null;
        }
    }

    cancelFadeOut() {
        if (this.fadeOutTimer) {
            clearTimeout(this.fadeOutTimer);
            this.fadeOutTimer = null;
        }
        if (this.cleanupTimer) {
            clearTimeout(this.cleanupTimer);
            this.cleanupTimer = null;
        }
    }

    reset() {
        this.currentStrokeIndex = 0;
        this.strokes.forEach(s => {
            s.pathEl.style.transition = 'none';
            s.pathEl.style.strokeDashoffset = `${s.length}`;
            s.pathEl.setAttribute('stroke', this.completedColor);
            s.badgeEl.style.opacity = '0';
        });
        this.setState('idle');
    }

    setSpeed(speed) {
        if (typeof speed === 'number' && speed > 0.1 && speed <= 5.0) {
            this.speed = speed;
        }
    }

    /**
     * Bật/Tắt thủ công nét bóng mờ (Manual Toggle Ghost Guide)
     */
    toggleGhost(visible) {
        if (typeof visible === 'boolean') {
            this.isGhostVisible = visible;
        } else {
            this.isGhostVisible = !this.isGhostVisible;
        }

        this.ghostGroup.style.transition = 'opacity 0.25s ease';
        if (this.isGhostVisible) {
            this.ghostGroup.style.display = 'block';
            this.ghostGroup.style.opacity = '0.35'; // Nét xám nhạt mờ mờ để học viên đồ theo
        } else {
            this.ghostGroup.style.display = 'none';
            this.ghostGroup.style.opacity = '0';
        }
        return this.isGhostVisible;
    }

    /**
     * Xử lý khi hoàn thành tất cả các nét:
     * 1. Giữ nguyên 1.2s để học viên ghi nhớ hình thái chữ.
     * 2. Fade-out 0.5s mượt mà.
     * 3. Dọn sạch trả lại ô Mễ tinh khôi.
     */
    handlePlaybackComplete() {
        this.setState('completed');

        this.strokes.forEach(s => {
            s.pathEl.style.strokeDashoffset = '0';
            s.pathEl.setAttribute('stroke', this.completedColor);
            s.badgeEl.style.opacity = '0.9';
        });

        if (this.onComplete) {
            this.onComplete(this.strokes.length, false /* chưa fade out */);
        }

        if (this.autoFadeOutOnComplete) {
            // Bước 1: Giữ nguyên 1.2s
            this.fadeOutTimer = setTimeout(() => {
                // Bước 2: Hiệu ứng mờ dần trong 0.5s
                this.animGroup.style.transition = `opacity ${this.fadeOutDuration}ms ease-out`;
                this.numberGroup.style.transition = `opacity ${this.fadeOutDuration}ms ease-out`;
                this.ghostGroup.style.transition = `opacity ${this.fadeOutDuration}ms ease-out`;

                this.animGroup.style.opacity = '0';
                this.numberGroup.style.opacity = '0';
                this.ghostGroup.style.opacity = '0';

                // Bước 3: Dọn sạch nét vẽ, trả lại ô Mễ sạch sẽ
                this.cleanupTimer = setTimeout(() => {
                    this.reset();

                    this.animGroup.style.transition = 'none';
                    this.numberGroup.style.transition = 'none';
                    this.ghostGroup.style.transition = 'none';

                    this.animGroup.style.opacity = '1';
                    this.numberGroup.style.opacity = '1';
                    this.ghostGroup.style.opacity = '0.35';
                    this.ghostGroup.style.display = 'none';
                    this.isGhostVisible = false;

                    this.setState('idle');

                    if (this.onComplete) {
                        this.onComplete(this.strokes.length, true /* đã fade out sạch sẽ */);
                    }
                }, this.fadeOutDuration);
            }, this.holdDuration);
        }
    }

    setState(newState) {
        this.state = newState;
        if (this.onStateChange) {
            this.onStateChange(this.state);
        }
    }

    destroy() {
        this.stop();
        this.cancelFadeOut();
        if (this.targetEl) {
            this.targetEl.innerHTML = '';
        }
    }
}

window.KanjiStrokePlayer = KanjiStrokePlayer;
