/**
 * ====================================================================================================
 * KIZUNA TUTOR - MINIGAME "BẮN CHỮ KANJI" (KANJI RAIN TYPING ENGINE)
 * ====================================================================================================
 * Phiên bản: 1.0.0 (Vanilla JS ES6+, HTML5 Canvas, Web Audio API Synth)
 * 
 * ĐẶC ĐIỂM KỸ THUẬT:
 * 1. Cơ chế Game Loop mượt mà 60FPS bằng requestAnimationFrame và delta-time.
 * 2. Bộ tổng hợp âm thanh Web Audio API Synthesizer (Ding, Buzz, Win Fanfare, Game Over)
 *    không phụ thuộc vào bất kỳ file âm thanh MP3 bên ngoài nào.
 * 3. Xử lý bàn phím Tiếng Nhật thông minh (IME Composition Safe):
 *    Ngăn chặn việc so khớp nhầm khi người dùng đang soạn Romaji chưa commit sang Hiragana.
 * 4. Hệ thống hạt nổ Sparkle Particles lung linh khi gõ đúng chữ Kanji.
 * 5. Tự động tính toán độ chính xác và gửi dữ liệu Analytics về Backend REST API.
 * ====================================================================================================
 */

/**
 * 1. LỚP BỘ TỔNG HỢP ÂM THANH (WEB AUDIO API SYNTHESIZER)
 * Sử dụng OscillatorNode để tạo âm thanh trực tiếp từ tần số sóng (Sine, Triangle, Sawtooth).
 */
class SoundEffects {
    constructor() {
        this.ctx = null;
    }

    init() {
        if (!this.ctx) {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (AudioContext) {
                this.ctx = new AudioContext();
            }
        }
        if (this.ctx && this.ctx.state === 'suspended') {
            this.ctx.resume();
        }
    }

    /**
     * Âm thanh "Ding" thành công (khi gõ trúng chữ Kanji)
     * Tần số trượt từ 880Hz (A5) lên 1760Hz (A6) với decay dạng chuông ngân.
     */
    playDing() {
        this.init();
        if (!this.ctx) return;

        const now = this.ctx.currentTime;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(880, now);
        osc.frequency.exponentialRampToValueAtTime(1760, now + 0.1);

        gain.gain.setValueAtTime(0.25, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.35);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start(now);
        osc.stop(now + 0.35);
    }

    /**
     * Âm thanh "Buzz" cảnh báo gõ sai
     * Sử dụng sóng Sawtooth ở tần số thấp (130Hz -> 90Hz) tạo âm rung giật.
     */
    playBuzz() {
        this.init();
        if (!this.ctx) return;

        const now = this.ctx.currentTime;
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(130, now);
        osc.frequency.linearRampToValueAtTime(85, now + 0.18);

        gain.gain.setValueAtTime(0.2, now);
        gain.gain.linearRampToValueAtTime(0.01, now + 0.18);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start(now);
        osc.stop(now + 0.18);
    }

    /**
     * Âm thanh Chiến Thắng (Victory Fanfare)
     * Hợp âm Arpeggio vui nhộn gồm chuỗi nốt: C5 -> E5 -> G5 -> C6
     */
    playVictory() {
        this.init();
        if (!this.ctx) return;

        const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
        const now = this.ctx.currentTime;

        notes.forEach((freq, idx) => {
            const osc = this.ctx.createOscillator();
            const gain = this.ctx.createGain();
            const start = now + idx * 0.12;

            osc.type = 'triangle';
            osc.frequency.setValueAtTime(freq, start);

            gain.gain.setValueAtTime(0.3, start);
            gain.gain.exponentialRampToValueAtTime(0.001, start + 0.45);

            osc.connect(gain);
            gain.connect(this.ctx.destination);

            osc.start(start);
            osc.stop(start + 0.45);
        });
    }

    /**
     * Âm thanh Thất Bại (Game Over)
     * Chuỗi âm trầm giảm dần: E4 -> D#4 -> D4 -> C#4
     */
    playGameOver() {
        this.init();
        if (!this.ctx) return;

        const notes = [329.63, 311.13, 293.66, 277.18];
        const now = this.ctx.currentTime;

        notes.forEach((freq, idx) => {
            const osc = this.ctx.createOscillator();
            const gain = this.ctx.createGain();
            const start = now + idx * 0.14;

            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(freq, start);

            gain.gain.setValueAtTime(0.2, start);
            gain.gain.linearRampToValueAtTime(0.01, start + 0.28);

            osc.connect(gain);
            gain.connect(this.ctx.destination);

            osc.start(start);
            osc.stop(start + 0.28);
        });
    }
}

/**
 * 2. LỚP HỆ THỐNG HẠT LẤP LÁNH (SPARKLE PARTICLE SYSTEM)
 * Tạo hiệu ứng nổ pháo hoa hạt sáng màu xanh khi người chơi tiêu diệt chữ Kanji.
 */
class SparkleParticle {
    constructor(x, y, color) {
        this.x = x;
        this.y = y;
        this.color = color || '#10B981';
        this.radius = Math.random() * 3 + 2;
        // Tốc độ và góc bắn ngẫu nhiên 360 độ
        const angle = Math.random() * Math.PI * 2;
        const speed = Math.random() * 5 + 2;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.alpha = 1.0;
        this.decay = Math.random() * 0.03 + 0.02; // Tốc độ mờ dần
        this.gravity = 0.12; // Trọng lực nhẹ kéo hạt rơi xuống
    }

    update() {
        this.x += this.vx;
        this.y += this.vy;
        this.vy += this.gravity;
        this.alpha -= this.decay;
    }

    draw(ctx) {
        if (this.alpha <= 0) return;
        ctx.save();
        ctx.globalAlpha = Math.max(0, this.alpha);
        ctx.fillStyle = this.color;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
    }
}

/**
 * 3. THỰC THỂ CHỮ KANJI RƠI (FALLING KANJI ENTITY)
 * Quản lý tọa độ, tốc độ, mặt chữ, âm đọc, kích thước thẻ vẽ trên Canvas.
 */
class FallingKanji {
    constructor(kanjiData, canvasWidth, initialSpeed) {
        this.id = kanjiData.kanjiId;
        this.character = kanjiData.character;
        this.hanViet = kanjiData.hanViet || 'HÁN TỰ';
        this.meaning = kanjiData.meaning || '';
        this.readings = kanjiData.readings || []; // Danh sách cách đọc Hiragana hợp lệ
        this.primaryReading = kanjiData.primaryReading || ''; // Gợi ý hiển thị
        this.strokeCount = kanjiData.strokeCount || 1;

        // Kích thước Card thẻ chữ
        this.width = 86;
        this.height = 98;

        // Tọa độ X ngẫu nhiên (tránh vẽ sát mép biên màn hình)
        const minX = 25;
        const maxX = Math.max(minX, canvasWidth - this.width - 25);
        this.x = Math.floor(Math.random() * (maxX - minX + 1)) + minX;
        this.y = -this.height; // Bắt đầu từ phía trên ngoài màn hình

        this.speed = initialSpeed;
        this.mistakesCount = 0; // Số lần người chơi gõ sai chữ này
        this.flashRed = 0; // Hiệu ứng chớp đỏ khi bị gõ sai
    }

    update(dt, speedMultiplier) {
        // Tọa độ Y dịch chuyển theo vận tốc thực tế (pixel / second)
        this.y += this.speed * speedMultiplier * dt;
        if (this.flashRed > 0) {
            this.flashRed -= dt * 3;
            if (this.flashRed < 0) this.flashRed = 0;
        }
    }

    draw(ctx, deadlineY) {
        ctx.save();

        const x = this.x;
        const y = this.y;
        const w = this.width;
        const h = this.height;
        const radius = 10;

        // Kiểm tra mức độ nguy hiểm: Nếu cách deadline dưới 140px thì cảnh báo viền đỏ cam
        const distToDeadline = deadlineY - (y + h);
        const isDanger = distToDeadline < 140;

        // Vẽ đổ bóng mềm mại (Card Shadow)
        ctx.shadowColor = isDanger ? 'rgba(239, 68, 68, 0.35)' : 'rgba(15, 23, 42, 0.12)';
        ctx.shadowBlur = isDanger ? 14 : 8;
        ctx.shadowOffsetY = 4;

        // Nền Card thẻ bài
        ctx.fillStyle = this.flashRed > 0 
            ? `rgba(254, 226, 226, 0.95)` 
            : (isDanger ? '#FFF5F5' : '#FFFFFF');
        
        ctx.beginPath();
        ctx.roundRect(x, y, w, h, radius);
        ctx.fill();

        // Tắt bóng để vẽ viền & chữ sắc nét
        ctx.shadowColor = 'transparent';

        // Viền thẻ
        ctx.lineWidth = isDanger ? 2.5 : 1.5;
        ctx.strokeStyle = this.flashRed > 0 
            ? '#EF4444' 
            : (isDanger ? '#F87171' : '#E2E8F0');
        ctx.stroke();

        // 1. Mặt chữ Kanji chính (to, đậm nét)
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.font = 'bold 38px "Noto Sans JP", "Hiragino Kaku Gothic ProN", "Yu Gothic", sans-serif';
        ctx.fillStyle = isDanger ? '#DC2626' : '#1E293B';
        ctx.fillText(this.character, x + w / 2, y + 36);

        // 2. Đường kẻ phân cách nhẹ
        ctx.strokeStyle = '#F1F5F9';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(x + 10, y + 62);
        ctx.lineTo(x + w - 10, y + 62);
        ctx.stroke();

        // 3. Khung Badge hiển thị cách đọc Hiragana gợi ý
        const badgeY = y + 72;
        ctx.fillStyle = isDanger ? '#FEE2E2' : '#EFF6FF';
        ctx.beginPath();
        ctx.roundRect(x + 8, badgeY - 7, w - 16, 22, 6);
        ctx.fill();

        // Chữ Hiragana gợi ý
        ctx.font = 'bold 12px "Noto Sans JP", sans-serif';
        ctx.fillStyle = isDanger ? '#B91C1C' : '#2563EB';
        const displayReading = this.primaryReading || (this.readings.length > 0 ? this.readings[0] : '');
        ctx.fillText(displayReading, x + w / 2, badgeY + 4);

        // 4. Nếu chữ này từng bị gõ sai, vẽ chấm cảnh báo nhỏ
        if (this.mistakesCount > 0) {
            ctx.fillStyle = '#EF4444';
            ctx.beginPath();
            ctx.arc(x + w - 10, y + 10, 4, 0, Math.PI * 2);
            ctx.fill();
        }

        ctx.restore();
    }
}

/**
 * 4. CLASS ĐIỀU KHIỂN CHÍNH: KanjiRainGame
 * Quản lý vòng lặp Canvas, IME Input, logic va chạm, tính điểm và tương tác API.
 */
class KanjiRainGame {
    constructor(config) {
        // Cấu hình DOM Elements
        this.canvas = document.getElementById(config.canvasId || 'kanjiRainCanvas');
        this.ctx = this.canvas.getContext('2d');
        this.inputElem = document.getElementById(config.inputId || 'kanjiGameInput');
        this.displayInputElem = document.getElementById(config.displayInputId || 'liveTypingEcho');

        // Callback kết thúc
        this.onGameOver = config.onGameOver || (() => {});
        this.onGameWin = config.onGameWin || (() => {});
        this.onScoreUpdate = config.onScoreUpdate || (() => {});

        // Âm thanh
        this.sounds = new SoundEffects();

        // Trạng thái vòng lặp
        this.isRunning = false;
        this.isPaused = false;
        this.animationFrameId = null;
        this.lastTime = 0;

        // Cấu hình thời gian chơi & Tốc độ
        this.targetDuration = config.targetDuration || 60; // 60s, 180s, 300s
        this.remainingSeconds = this.targetDuration;
        this.elapsedSeconds = 0;
        this.speedMultiplier = 1.0; // Tăng 10% sau mỗi 20s
        this.baseSpeed = 42; // pixel / second ban đầu

        // Bộ đếm điểm số & Thống kê
        this.score = 0;
        this.wordsTypedCount = 0;
        this.totalMistakes = 0;
        this.combo = 0;
        this.maxCombo = 0;
        this.folderId = config.folderId || null;
        this.folderName = config.folderName || 'Thư mục Kanji';

        // Danh sách từ điển phục vụ game
        this.kanjiPool = [];
        this.fallingObjects = [];
        this.sparkles = [];
        this.mistakesMap = new Map(); // kanjiId -> { kanji, count, lastTyped }

        // Logic sinh chữ mới
        this.spawnTimer = 0;
        this.spawnInterval = 3.2; // Giây giữa các lần sinh chữ

        // Cấu hình vạch ranh giới (Deadline)
        this.deadlineOffsetFromBottom = 65; // Cách đáy canvas 65px

        // Cờ trạng thái IME tiếng Nhật (Input Method Editor)
        this.isComposing = false;
        this.screenFlashRed = 0;

        // Khởi tạo các listener
        this.setupEventListeners();
        this.resizeCanvas();
    }

    /**
     * Tự động căn chỉnh kích thước Canvas theo khung hiển thị (High-DPI / Retina Ready)
     */
    resizeCanvas() {
        if (!this.canvas) return;
        const rect = this.canvas.getBoundingClientRect();
        const dpr = window.devicePixelRatio || 1;

        // Giữ tỉ lệ chiều rộng thực tế của khung
        const width = rect.width || 800;
        const height = rect.height || 540;

        this.canvas.width = width * dpr;
        this.canvas.height = height * dpr;
        this.ctx.scale(dpr, dpr);

        this.logicalWidth = width;
        this.logicalHeight = height;
    }

    /**
     * Nạp dữ liệu danh sách chữ Kanji vào ván đấu
     */
    loadKanjiData(dataList) {
        if (!Array.isArray(dataList) || dataList.length === 0) {
            console.warn('KanjiRainGame: Dữ liệu Kanji rỗng!');
            this.kanjiPool = [];
            return;
        }
        this.kanjiPool = [...dataList];
    }

    /**
     * Đăng ký các sự kiện bàn phím, tương tác chuột và IME tiếng Nhật an toàn
     */
    setupEventListeners() {
        if (!this.inputElem) return;

        // 1. Tự động lấy lại focus khi click vào bất kỳ đâu trên Canvas
        if (this.canvas) {
            this.canvas.addEventListener('click', () => {
                this.inputElem.focus();
            });
        }

        // 2. Xử lý IME Composition (Gõ Hiragana chuẩn an toàn)
        this.inputElem.addEventListener('compositionstart', () => {
            this.isComposing = true;
        });

        this.inputElem.addEventListener('compositionupdate', (e) => {
            if (this.displayInputElem) {
                this.displayInputElem.textContent = e.data || this.inputElem.value;
            }
        });

        this.inputElem.addEventListener('compositionend', (e) => {
            this.isComposing = false;
            const committedText = e.data || this.inputElem.value;
            if (this.displayInputElem) {
                this.displayInputElem.textContent = committedText;
            }
            // Khi người dùng chốt từ Hiragana xong, lập tức kiểm tra so khớp
            this.evaluateInput(committedText.trim(), false);
        });

        // 3. Sự kiện gõ text thông thường
        this.inputElem.addEventListener('input', () => {
            const val = this.inputElem.value;
            if (this.displayInputElem) {
                this.displayInputElem.textContent = val;
            }
            // Chỉ kiểm tra ngay lập tức nếu KHÔNG ở trạng thái soạn thảo IME (non-composing)
            if (!this.isComposing) {
                this.evaluateInput(val.trim(), false);
            }
        });

        // 4. Nhấn phím Enter: Chốt từ hoặc xác nhận nộp
        this.inputElem.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                // Nếu đang gõ dở IME thì để IME chốt, không can thiệp
                if (this.isComposing) return;

                const val = this.inputElem.value.trim();
                if (val.length > 0) {
                    this.evaluateInput(val, true); // Chế độ Enter ép kiểm tra
                }
            }
        });

        // Tự động resize canvas khi thay đổi kích thước cửa sổ
        window.addEventListener('resize', () => {
            this.resizeCanvas();
        });
    }

    /**
     * Bắt đầu phiên chơi mới
     */
    start() {
        this.resizeCanvas();
        this.isRunning = true;
        this.isPaused = false;
        this.fallingObjects = [];
        this.sparkles = [];
        this.mistakesMap.clear();

        this.score = 0;
        this.wordsTypedCount = 0;
        this.totalMistakes = 0;
        this.combo = 0;
        this.maxCombo = 0;
        this.elapsedSeconds = 0;
        this.remainingSeconds = this.targetDuration;
        this.speedMultiplier = 1.0;
        this.spawnTimer = 0;

        // Sinh ngay 1 chữ đầu tiên
        this.spawnFallingKanji();

        // Focus vào ô input
        if (this.inputElem) {
            this.inputElem.value = '';
            this.inputElem.focus();
        }
        if (this.displayInputElem) {
            this.displayInputElem.textContent = '...';
        }

        this.lastTime = performance.now();
        if (this.animationFrameId) {
            cancelAnimationFrame(this.animationFrameId);
        }
        this.animationFrameId = requestAnimationFrame(this.gameLoop.bind(this));
    }

    /**
     * Dừng game
     */
    stop() {
        this.isRunning = false;
        if (this.animationFrameId) {
            cancelAnimationFrame(this.animationFrameId);
            this.animationFrameId = null;
        }
    }

    /**
     * Sinh một chữ Kanji ngẫu nhiên từ kho dữ liệu rơi từ trên xuống
     */
    spawnFallingKanji() {
        if (!this.kanjiPool || this.kanjiPool.length === 0) return;

        // Lấy ngẫu nhiên một chữ từ pool
        const randomIndex = Math.floor(Math.random() * this.kanjiPool.length);
        const kanjiData = this.kanjiPool[randomIndex];

        // Không sinh trùng chữ đang rơi trên màn hình nếu pool đủ lớn
        const isAlreadyFalling = this.fallingObjects.some(o => o.character === kanjiData.character);
        if (isAlreadyFalling && this.kanjiPool.length > 5) {
            // Thử chọn lại 1 lần
            const retryIdx = (randomIndex + 1) % this.kanjiPool.length;
            const newKanji = this.kanjiPool[retryIdx];
            this.fallingObjects.push(new FallingKanji(newKanji, this.logicalWidth, this.baseSpeed));
            return;
        }

        const falling = new FallingKanji(kanjiData, this.logicalWidth, this.baseSpeed);
        this.fallingObjects.push(falling);
    }

    /**
     * Thuật toán kiểm tra gõ phím & so khớp cách đọc Hiragana
     * 
     * @param {string} rawInput Chuỗi ký tự người dùng đã gõ
     * @param {boolean} isEnterPressed Có phải vừa nhấn phím Enter không
     */
    evaluateInput(rawInput, isEnterPressed) {
        if (!rawInput || rawInput.length === 0) return;

        // Chuẩn hóa input: chuyển chữ thường, đổi Katakana sang Hiragana nếu có
        const inputHira = this.normalizeInput(rawInput);
        if (!inputHira) return;

        // Tìm kiếm xem có chữ Kanji nào đang rơi khớp với cách đọc này không
        // Ưu tiên chữ đang ở vị trí Y lớn nhất (gần chạm Deadline nhất)
        let matchedIndex = -1;
        let lowestY = -9999;

        for (let i = 0; i < this.fallingObjects.length; i++) {
            const item = this.fallingObjects[i];
            const hasMatch = item.readings.some(r => r === inputHira);

            if (hasMatch && item.y > lowestY) {
                lowestY = item.y;
                matchedIndex = i;
            }
        }

        if (matchedIndex !== -1) {
            // ==========================================
            // TRƯỜNG HỢP: GÕ ĐÚNG KHỚP CHỮ KANJI!
            // ==========================================
            const destroyed = this.fallingObjects[matchedIndex];

            // 1. Tạo hiệu ứng pháo hoa hạt xanh (Sparkle particles)
            this.createExplosion(destroyed.x + destroyed.width / 2, destroyed.y + destroyed.height / 2);

            // 2. Phát âm thanh "Ding" thành công
            this.sounds.playDing();

            // 3. Xóa chữ khỏi danh sách đang rơi
            this.fallingObjects.splice(matchedIndex, 1);

            // 4. Cập nhật chỉ số
            this.wordsTypedCount++;
            this.combo++;
            if (this.combo > this.maxCombo) {
                this.maxCombo = this.combo;
            }

            // Điểm số: 100 điểm gốc + thưởng combo
            const comboBonus = Math.min(this.combo * 15, 150);
            this.score += (100 + comboBonus);

            // 5. Xóa sạch ô input để người chơi gõ từ tiếp theo
            this.clearInput();

            // Callback cập nhật UI ngoài
            this.notifyStatsUpdate();

        } else if (isEnterPressed) {
            // ==========================================
            // TRƯỜNG HỢP: NHẤN ENTER NHƯNG KHÔNG KHỚP TỪ NÀO (GÕ SAI)
            // ==========================================
            this.handleWrongInput(inputHira);
        }
    }

    /**
     * Xử lý khi người chơi gõ sai và ấn Enter
     */
    handleWrongInput(typedText) {
        this.totalMistakes++;
        this.combo = 0; // Đứt chuỗi combo

        // 1. Phát âm thanh "Buzz" cảnh báo
        this.sounds.playBuzz();

        // 2. Hiệu ứng rung ô input + chớp viền đỏ màn hình
        this.screenFlashRed = 0.25;
        this.shakeInputElement();

        // 3. Ghi nhận lỗi sai cho chữ đang ở gần vạch Deadline nhất
        if (this.fallingObjects.length > 0) {
            // Sắp xếp tìm chữ có Y lớn nhất
            let lowest = this.fallingObjects[0];
            for (let i = 1; i < this.fallingObjects.length; i++) {
                if (this.fallingObjects[i].y > lowest.y) {
                    lowest = this.fallingObjects[i];
                }
            }
            lowest.mistakesCount++;
            lowest.flashRed = 0.5;

            // Ghi vào mistakesMap
            const existing = this.mistakesMap.get(lowest.id) || {
                kanjiId: lowest.id,
                character: lowest.character,
                reading: lowest.primaryReading,
                count: 0,
                lastTyped: typedText
            };
            existing.count++;
            existing.lastTyped = typedText;
            this.mistakesMap.set(lowest.id, existing);
        }

        // 4. Xóa sạch ô input để gõ lại
        this.clearInput();
        this.notifyStatsUpdate();
    }

    /**
     * Xóa sạch giá trị ô input
     */
    clearInput() {
        if (this.inputElem) {
            this.inputElem.value = '';
        }
        if (this.displayInputElem) {
            this.displayInputElem.textContent = '...';
        }
    }

    /**
     * Hiệu ứng CSS rung lắc ô input khi gõ sai
     */
    shakeInputElement() {
        const target = this.displayInputElem || this.inputElem;
        if (!target) return;
        target.classList.remove('game-shake-anim');
        void target.offsetWidth; // Force reflow
        target.classList.add('game-shake-anim');
    }

    /**
     * Sinh 24 hạt sáng nổ tung quanh tâm chữ Kanji bị bắn trúng
     */
    createExplosion(centerX, centerY) {
        const colors = ['#10B981', '#34D399', '#6EE7B7', '#FBBF24', '#60A5FA'];
        for (let i = 0; i < 26; i++) {
            const col = colors[Math.floor(Math.random() * colors.length)];
            this.sparkles.push(new SparkleParticle(centerX, centerY, col));
        }
    }

    /**
     * Chuẩn hóa chuỗi gõ vào: Trim, chuyển Katakana sang Hiragana
     */
    normalizeInput(str) {
        if (!str) return '';
        let res = '';
        for (let i = 0; i < str.length; i++) {
            const c = str.charCodeAt(i);
            // Dải Katakana: 0x30A1 - 0x30F6 -> Hiragana tương ứng: c - 0x60
            if (c >= 0x30A1 && c <= 0x30F6) {
                res += String.fromCharCode(c - 0x60);
            } else {
                res += str.charAt(i);
            }
        }
        return res.trim().toLowerCase();
    }

    /**
     * Vòng lặp chính Game Loop (60FPS)
     */
    gameLoop(currentTime) {
        if (!this.isRunning) return;

        // Tính delta-time theo giây
        const dt = Math.min((currentTime - this.lastTime) / 1000, 0.1);
        this.lastTime = currentTime;

        if (!this.isPaused) {
            this.update(dt);
            this.render();
        }

        if (this.isRunning) {
            this.animationFrameId = requestAnimationFrame(this.gameLoop.bind(this));
        }
    }

    /**
     * Cập nhật logic game theo delta-time
     */
    update(dt) {
        this.elapsedSeconds += dt;
        this.remainingSeconds -= dt;

        // 1. Tốc độ rơi: Cứ mỗi 20 giây tăng tốc độ speed thêm 10%
        const speedStages = Math.floor(this.elapsedSeconds / 20);
        this.speedMultiplier = Math.pow(1.10, speedStages);

        // 2. Tần suất sinh chữ: Giảm dần thời gian chờ khi điểm số tăng
        const dynamicInterval = Math.max(1.4, this.spawnInterval - (this.score / 2000) * 1.0);
        this.spawnTimer += dt;
        if (this.spawnTimer >= dynamicInterval) {
            this.spawnTimer = 0;
            this.spawnFallingKanji();
        }

        // 3. Cập nhật vị trí các chữ Kanji đang rơi
        const deadlineY = this.logicalHeight - this.deadlineOffsetFromBottom;
        let failedKanji = null;

        for (let i = 0; i < this.fallingObjects.length; i++) {
            const item = this.fallingObjects[i];
            item.update(dt, this.speedMultiplier);

            // KIỂM TRA ĐIỀU KIỆN THUA (GAME OVER):
            // Tọa độ y + height của bất kỳ chữ nào >= vạch ranh giới (Deadline)
            if (item.y + item.height >= deadlineY) {
                failedKanji = item;
                break;
            }
        }

        // 4. Cập nhật hiệu ứng hạt sáng Sparkle
        for (let i = this.sparkles.length - 1; i >= 0; i--) {
            const p = this.sparkles[i];
            p.update();
            if (p.alpha <= 0) {
                this.sparkles.splice(i, 1);
            }
        }

        // 5. Cập nhật viền flash đỏ
        if (this.screenFlashRed > 0) {
            this.screenFlashRed -= dt * 2;
            if (this.screenFlashRed < 0) this.screenFlashRed = 0;
        }

        // Xử lý thua cuộc
        if (failedKanji) {
            this.triggerGameOver(failedKanji);
            return;
        }

        // KIỂM TRA ĐIỀU KIỆN THẮNG (YOU WIN):
        // Đồng hồ đếm ngược về 00:00 mà không chữ nào chạm vạch ranh giới
        if (this.remainingSeconds <= 0) {
            this.remainingSeconds = 0;
            this.triggerGameWin();
            return;
        }

        // Cập nhật thống kê định kỳ lên UI
        this.notifyStatsUpdate();
    }

    /**
     * Vẽ toàn bộ các thành phần lên HTML5 Canvas
     */
    render() {
        const ctx = this.ctx;
        const w = this.logicalWidth;
        const h = this.logicalHeight;

        // Xóa màn hình
        ctx.clearRect(0, 0, w, h);

        // 1. Vẽ nền Canvas gradient phong cách võ đạo Kizuna
        const bgGrad = ctx.createLinearGradient(0, 0, 0, h);
        bgGrad.addColorStop(0, '#F8FAFC');
        bgGrad.addColorStop(1, '#F1F5F9');
        ctx.fillStyle = bgGrad;
        ctx.fillRect(0, 0, w, h);

        // Vẽ lưới mờ trang trí tinh tế phong cách Nhật Bản
        this.renderBackgroundGrid(ctx, w, h);

        const deadlineY = h - this.deadlineOffsetFromBottom;

        // 2. Vẽ VÙNG NGUY HIỂM dưới vạch Deadline (Danger Zone)
        const dangerGrad = ctx.createLinearGradient(0, deadlineY, 0, h);
        dangerGrad.addColorStop(0, 'rgba(239, 68, 68, 0.18)');
        dangerGrad.addColorStop(1, 'rgba(239, 68, 68, 0.35)');
        ctx.fillStyle = dangerGrad;
        ctx.fillRect(0, deadlineY, w, h - deadlineY);

        // 3. Vẽ VẠCH RANH GIỚI (DEADLINE):
        // Đường line đỏ nét đứt cách đáy 65px
        ctx.save();
        ctx.strokeStyle = '#EF4444';
        ctx.lineWidth = 2.5;
        ctx.setLineDash([10, 8]);
        ctx.beginPath();
        ctx.moveTo(0, deadlineY);
        ctx.lineTo(w, deadlineY);
        ctx.stroke();

        // Chữ cảnh báo: Vạch ranh giới nguy hiểm
        ctx.setLineDash([]);
        ctx.font = 'bold 12px "Noto Sans JP", sans-serif';
        ctx.textAlign = 'right';
        ctx.fillStyle = '#DC2626';
        ctx.fillText('⚠️ VẠCH RANH GIỚI NGUY HIỂM (Chạm đáy sẽ dừng ván)', w - 24, deadlineY - 8);

        // Tốc độ hiện tại
        ctx.textAlign = 'left';
        ctx.font = '11px sans-serif';
        ctx.fillStyle = '#64748B';
        const speedText = `⚡ Nhịp Rơi: Mức ${this.speedMultiplier.toFixed(2)}x | Sau mỗi 20 giây tăng 10%`;
        ctx.fillText(speedText, 24, deadlineY - 8);
        ctx.restore();

        // 4. Vẽ các thực thể chữ Kanji đang rơi
        for (const item of this.fallingObjects) {
            item.draw(ctx, deadlineY);
        }

        // 5. Vẽ hiệu ứng hạt sáng nổ (Sparkles)
        for (const p of this.sparkles) {
            p.draw(ctx);
        }

        // 6. Hiệu ứng Flash đỏ viền màn hình khi gõ sai
        if (this.screenFlashRed > 0) {
            ctx.save();
            ctx.fillStyle = `rgba(239, 68, 68, ${this.screenFlashRed * 0.4})`;
            ctx.fillRect(0, 0, w, h);
            ctx.strokeStyle = `rgba(239, 68, 68, ${this.screenFlashRed})`;
            ctx.lineWidth = 12;
            ctx.strokeRect(0, 0, w, h);
            ctx.restore();
        }
    }

    /**
     * Vẽ lưới nền mờ nhẹ phong cách giấy kẻ truyền thống Nhật
     */
    renderBackgroundGrid(ctx, w, h) {
        ctx.save();
        ctx.strokeStyle = 'rgba(226, 232, 240, 0.4)';
        ctx.lineWidth = 1;
        const step = 45;
        for (let x = step; x < w; x += step) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, h);
            ctx.stroke();
        }
        for (let y = step; y < h; y += step) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(w, y);
            ctx.stroke();
        }
        ctx.restore();
    }

    /**
     * Kích hoạt Thua cuộc (Game Over)
     */
    triggerGameOver(failedKanji) {
        this.stop();
        this.sounds.playGameOver();

        const accuracy = this.calculateAccuracy();
        const mistakesList = Array.from(this.mistakesMap.values());

        const resultPayload = {
            folderId: this.folderId,
            durationSeconds: Math.round(this.elapsedSeconds),
            targetDurationSeconds: this.targetDuration,
            wordsTypedCount: this.wordsTypedCount,
            totalMistakes: this.totalMistakes,
            accuracy: accuracy,
            score: this.score,
            isWon: false,
            failedKanjiId: failedKanji.id,
            failedKanjiCharacter: failedKanji.character,
            failedKanjiData: failedKanji,
            remainingKanjis: this.fallingObjects.map(f => ({
                character: f.character,
                reading: f.primaryReading,
                hanViet: f.hanViet,
                meaning: f.meaning
            })),
            mistakes: mistakesList
        };

        // Tự động đóng gói gửi API lưu kết quả
        this.saveSessionAnalytics(resultPayload);

        // Gọi callback để hiển thị Modal Game Over
        if (typeof this.onGameOver === 'function') {
            this.onGameOver(resultPayload);
        }
    }

    /**
     * Kích hoạt Chiến Thắng (You Win)
     */
    triggerGameWin() {
        this.stop();
        this.sounds.playVictory();

        const accuracy = this.calculateAccuracy();
        const mistakesList = Array.from(this.mistakesMap.values());

        const resultPayload = {
            folderId: this.folderId,
            durationSeconds: this.targetDuration,
            targetDurationSeconds: this.targetDuration,
            wordsTypedCount: this.wordsTypedCount,
            totalMistakes: this.totalMistakes,
            accuracy: accuracy,
            score: this.score,
            isWon: true,
            failedKanjiId: null,
            failedKanjiCharacter: null,
            mistakes: mistakesList
        };

        // Tự động đóng gói gửi API lưu kết quả
        this.saveSessionAnalytics(resultPayload);

        // Gọi callback hiển thị Modal You Win
        if (typeof this.onGameWin === 'function') {
            this.onGameWin(resultPayload);
        }
    }

    /**
     * Tính toán tỷ lệ chính xác Accuracy %: words / (words + mistakes) * 100
     */
    calculateAccuracy() {
        const total = this.wordsTypedCount + this.totalMistakes;
        if (total === 0) return 100.0;
        const acc = (this.wordsTypedCount / total) * 100.0;
        return Math.round(acc * 10) / 10;
    }

    /**
     * Tự động gửi kết quả phân tích trận đấu về Backend REST API
     */
    async saveSessionAnalytics(payload) {
        try {
            const body = {
                folderId: payload.folderId,
                durationSeconds: payload.durationSeconds,
                targetDurationSeconds: payload.targetDurationSeconds,
                wordsTypedCount: payload.wordsTypedCount,
                totalMistakes: payload.totalMistakes,
                accuracy: payload.accuracy,
                score: payload.score,
                isWon: payload.isWon,
                failedKanjiId: payload.failedKanjiId,
                failedKanjiCharacter: payload.failedKanjiCharacter,
                mistakes: (payload.mistakes || []).map(m => ({
                    kanjiId: m.kanjiId,
                    kanjiCharacter: m.character || m.kanjiCharacter,
                    reading: m.reading,
                    mistakeCount: m.count || m.mistakeCount,
                    typedInput: m.lastTyped || m.typedInput
                }))
            };

            const response = await fetch('/api/user/minigame/sessions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });

            if (response.ok) {
                const data = await response.json();
                console.log('✅ Đã lưu kết quả Minigame lên hệ thống:', data);
            } else {
                console.warn('⚠️ Lưu kết quả Minigame thất bại:', response.status);
            }
        } catch (err) {
            console.error('Lỗi khi gửi API analytics minigame:', err);
        }
    }

    /**
     * Bắn thông tin thời gian, điểm số ra ngoài UI
     */
    notifyStatsUpdate() {
        if (typeof this.onScoreUpdate === 'function') {
            const minutes = Math.floor(Math.max(0, this.remainingSeconds) / 60);
            const seconds = Math.floor(Math.max(0, this.remainingSeconds) % 60);
            const formattedTime = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;

            this.onScoreUpdate({
                score: this.score,
                wordsTyped: this.wordsTypedCount,
                combo: this.combo,
                accuracy: this.calculateAccuracy(),
                timeString: formattedTime,
                remainingSeconds: this.remainingSeconds,
                speedMultiplier: this.speedMultiplier
            });
        }
    }
}

// Xuất class ra phạm vi window để các view/template dễ dàng gọi
window.KanjiRainGame = KanjiRainGame;
