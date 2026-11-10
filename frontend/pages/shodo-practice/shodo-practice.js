/**
 * KizunaTutor - Phòng Luyện Viết Shodo Canvas AI
 * Module độc lập theo kiến trúc Feature-Based, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast } from '/js/main.js';
import { KizunaDialog } from '/js/common/dialog.js';
import { AgentMemoryService } from '/js/api/agentMemory.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'shodo',
  pageTitle: 'Phòng luyện viết Shodo',
  badge: 'Bút thuận AI'
});

// =========================================================================
// 1. TRẠNG THÁI TOÀN CỤC PHÒNG LUYỆN TẬP
// =========================================================================
let currentChar = null;
let strokePaths = []; // Mảng chứa dữ liệu path nét d từ KanjiVG
let activeStrokeFilter = null; // null = tất cả nét, số = nét cụ thể
let isGuidelineVisible = true;
let isPlayingAnimation = false;
let animTimer = null;

// Quản lý Canvas vẽ tay
let canvas = null;
let ctx = null;
let isDrawing = false;
let drawnStrokes = []; // Mảng các nét người dùng vẽ [[{x, y}, ...], ...]
let currentStroke = [];

// =========================================================================
// 2. TIỆN ÍCH CHUYỂN ĐỔI UNICODE SANG MÃ HEX KANJIVG
// =========================================================================
function getKanjiUnicodeHex(char) {
  if (!char || typeof char !== 'string') return null;
  const codePoint = char.codePointAt(0);
  return codePoint.toString(16).padStart(5, '0').toLowerCase();
}

// =========================================================================
// 3. TẢI DỮ LIỆU SVG NÉT CHỮ TỪ BACKEND / KANJIVG CDN
// =========================================================================
async function loadKanjiSvg(char) {
  if (!char || typeof char !== 'string') return [];
  const targetChar = char.trim().charAt(0);
  if (!targetChar) return [];

  const hex = getKanjiUnicodeHex(targetChar);

  // 1. Kiểm tra static file nội bộ /kanjivg/{hex}.svg trước (cực nhanh, không trễ)
  if (hex) {
    try {
      const localRes = await fetch(`/kanjivg/${hex}.svg`).catch(() => null);
      if (localRes && localRes.ok) {
        const svgText = await localRes.text();
        if (svgText && svgText.includes('<path')) {
          return parseKanjiSvgPaths(svgText);
        }
      }
    } catch (e) {}
  }

  // 2. Thử lấy từ Backend Database (PostgreSQL lưu sẵn KanjiVG SVG)
  try {
    let bRes = await fetch(`/api/learner/kanji/detail?char=${encodeURIComponent(targetChar)}`).catch(() => null);
    if (!bRes || !bRes.ok) {
      bRes = await fetch(`/api/kanji/${encodeURIComponent(targetChar)}`).catch(() => null);
    }
    if (bRes && bRes.ok) {
      const bData = await bRes.json();
      const svgContent = bData.svgData || (bData.data && bData.data.svgData);
      if (svgContent && svgContent.includes('<path')) {
        return parseKanjiSvgPaths(svgContent);
      }
    }
  } catch (e) {}

  // 3. Dự phòng tải từ KanjiVG CDN theo mã Hex
  if (hex) {
    const cdnUrl = `https://cdn.jsdelivr.net/gh/KanjiVG/kanjivg@master/kanji/${hex}.svg`;
    try {
      const res = await fetch(cdnUrl);
      if (res.ok) {
        const svgText = await res.text();
        return parseKanjiSvgPaths(svgText);
      }
    } catch (err) {
      console.debug('Không thể tải dữ liệu nét bút chuẩn cho chữ này:', err);
    }
  }
  return [];
}

function parseKanjiSvgPaths(svgText) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(svgText, 'image/svg+xml');
  const pathElements = Array.from(doc.querySelectorAll('path[d]'));

  // Lọc ra các path nét vẽ (bỏ các path số thứ tự)
  return pathElements
    .map(p => p.getAttribute('d'))
    .filter(d => d && !d.includes('kvg:type="number"'));
}

// =========================================================================
// 4. RENDER SVG NÉT BÚT THUẬN & HOẠT ẢNH BÚT MỰC
// =========================================================================
/**
 * Quản lý trạng thái bật / tắt nét mờ hướng dẫn
 */
let isGhostStrokeVisible = true;

function updateGhostToggleButton(btn, ghostLayer) {
  if (!btn) return;
  const labelEl = btn.querySelector('.label') || btn.querySelector('span:last-child');
  const iconEl = btn.querySelector('.icon') || btn.querySelector('span:first-child');

  if (isGhostStrokeVisible) {
    btn.classList.add('is-active', 'bg-[#FFE5EC]', 'text-[#831843]');
    btn.classList.remove('bg-white', 'text-[#0F172A]');
    if (iconEl) iconEl.textContent = '👁️';
    if (labelEl) labelEl.textContent = 'Ẩn nét mờ';
    btn.title = 'Nhấn để ẩn nét mờ hướng dẫn (tự nhớ mặt chữ để vẽ)';
    if (ghostLayer) {
      ghostLayer.style.opacity = '0.35';
      ghostLayer.style.visibility = 'visible';
    }
  } else {
    btn.classList.remove('is-active', 'bg-[#FFE5EC]', 'text-[#831843]');
    btn.classList.add('bg-white', 'text-[#0F172A]');
    if (iconEl) iconEl.textContent = '👁️';
    if (labelEl) labelEl.textContent = 'Hiện nét mờ';
    btn.title = 'Nhấn để hiện nét mờ hướng dẫn (tô theo nét)';
    if (ghostLayer) {
      ghostLayer.style.opacity = '0';
      ghostLayer.style.visibility = 'hidden';
    }
  }
}

function setupGhostStrokeToggle() {
  const toggleBtn = document.getElementById('btnToggleGuide') || document.getElementById('btnToggleGhost') || document.getElementById('btnToggleGhostStroke') || document.querySelector('[data-action="toggle-ghost"]');
  const ghostLayer = document.getElementById('kanjiGhostLayer') || document.getElementById('ghostStrokeLayer') || document.getElementById('ghostGuidelineGroup');

  if (!toggleBtn) return;

  updateGhostToggleButton(toggleBtn, ghostLayer);

  toggleBtn.onclick = (e) => {
    e.preventDefault();
    isGhostStrokeVisible = !isGhostStrokeVisible;

    const layer = document.getElementById('kanjiGhostLayer') || document.getElementById('ghostStrokeLayer') || document.getElementById('ghostGuidelineGroup');
    if (layer) {
      if (isGhostStrokeVisible) {
        layer.style.opacity = '0.35';
        layer.style.visibility = 'visible';
      } else {
        layer.style.opacity = '0';
        layer.style.visibility = 'hidden';
      }
    }

    updateGhostToggleButton(toggleBtn, layer);
    showToast(isGhostStrokeVisible ? 'Đã bật nét mờ hướng dẫn.' : 'Đã ẩn nét mờ hướng dẫn.', 'info');
  };
}

function renderGuidelineSvg() {
  const svg = document.getElementById('canvasGuidelineSvg');
  if (!svg) return;

  svg.innerHTML = '';

  // Lớp 1: Lớp nét mờ dẫn đường (Ghost / Guideline Layer)
  const ghostGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
  ghostGroup.id = 'kanjiGhostLayer';
  ghostGroup.setAttribute('id', 'kanjiGhostLayer');
  ghostGroup.classList.add('ghost-stroke-layer', 'kanji-ghost-layer');
  ghostGroup.style.opacity = isGhostStrokeVisible ? '0.35' : '0';
  ghostGroup.style.visibility = isGhostStrokeVisible ? 'visible' : 'hidden';
  ghostGroup.style.transition = 'opacity 0.15s ease-out';

  // Lớp 2: Lớp hoạt ảnh bút mực khi phát nét (Anim Strokes Layer)
  const animGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
  animGroup.id = 'animStrokesGroup';
  animGroup.classList.add('anim-stroke-layer');

  strokePaths.forEach((d, idx) => {
    const isFiltered = (activeStrokeFilter !== null && activeStrokeFilter !== idx);

    // 1. Nét xám mỏng mờ nằm dưới khung vẽ để học viên tô theo
    const ghostPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    ghostPath.setAttribute('d', d);
    ghostPath.setAttribute('fill', 'none');
    ghostPath.setAttribute('stroke', isFiltered ? '#CBD5E1' : '#475569');
    ghostPath.setAttribute('stroke-width', '4.5');
    ghostPath.setAttribute('stroke-linecap', 'round');
    ghostPath.setAttribute('stroke-linejoin', 'round');
    if (isFiltered) {
      ghostPath.setAttribute('opacity', '0.2');
    }
    ghostPath.id = 'ghostPath_' + idx;
    ghostGroup.appendChild(ghostPath);

    // 2. Lớp nét mô phỏng hoạt ảnh bút mực
    const animPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    animPath.setAttribute('d', d);
    animPath.setAttribute('fill', 'none');
    animPath.setAttribute('stroke', '#0F2545');
    animPath.setAttribute('stroke-width', '4.5');
    animPath.setAttribute('stroke-linecap', 'round');
    animPath.setAttribute('stroke-linejoin', 'round');
    animPath.classList.add('ksp-anim-path');
    animPath.id = 'strokePath_' + idx;

    animGroup.appendChild(animPath);

    // QUAN TRỌNG: transition = none để TUYỆT ĐỐI KHÔNG BỊ TUA NGƯỢC NÉT khi tải trang
    animPath.style.transition = 'none';
    const len = animPath.getTotalLength ? animPath.getTotalLength() : 200;
    animPath.style.strokeDasharray = `${len} ${len}`;
    animPath.style.strokeDashoffset = `${len}`;
  });

  svg.appendChild(ghostGroup);
  svg.appendChild(animGroup);

  const toggleBtn = document.getElementById('btnToggleGuide') || document.getElementById('btnToggleGhost') || document.getElementById('btnToggleGhostStroke') || document.querySelector('[data-action="toggle-ghost"]');
  if (toggleBtn) {
    updateGhostToggleButton(toggleBtn, ghostGroup);
  }
}

function resetAnimStrokes(fade = false, onComplete = null) {
  const animGroup = document.getElementById('animStrokesGroup');
  if (fade && animGroup) {
    animGroup.style.transition = 'opacity 0.35s ease-out';
    animGroup.style.opacity = '0';
    setTimeout(() => {
      strokePaths.forEach((_, idx) => {
        const p = document.getElementById('strokePath_' + idx);
        if (p) {
          const len = p.getTotalLength ? p.getTotalLength() : 200;
          p.style.transition = 'none';
          p.style.strokeDashoffset = `${len}`;
        }
      });
      animGroup.style.transition = 'none';
      animGroup.style.opacity = '1';
      if (typeof onComplete === 'function') onComplete();
    }, 350);
  } else {
    strokePaths.forEach((_, idx) => {
      const p = document.getElementById('strokePath_' + idx);
      if (p) {
        const len = p.getTotalLength ? p.getTotalLength() : 200;
        p.style.transition = 'none';
        p.style.strokeDashoffset = `${len}`;
      }
    });
    if (animGroup) {
      animGroup.style.transition = 'none';
      animGroup.style.opacity = '1';
    }
    if (typeof onComplete === 'function') onComplete();
  }
}

function playStrokeAnimation() {
  if (strokePaths.length === 0) {
    showToast('Vui lòng nhập chữ Hán trước khi phát hoạt ảnh nét viết.', 'info');
    return;
  }

  if (isPlayingAnimation) {
    stopStrokeAnimation();
    resetAnimStrokes(false);
    clearCanvas(false);
    return;
  }

  // Làm sạch nét vẽ tay trước khi bắt đầu phát hoạt ảnh nét
  clearCanvas(false);

  isPlayingAnimation = true;
  const playBtn = document.getElementById('btnPlayStroke') || document.querySelector('[data-action="play-stroke"]');
  if (playBtn) {
    const iconEl = playBtn.querySelector('.icon') || playBtn.querySelector('span:first-child');
    const labelEl = playBtn.querySelector('.label') || playBtn.querySelector('span:last-child');
    if (iconEl) iconEl.textContent = '⏸️';
    if (labelEl) labelEl.textContent = 'Tạm dừng';
    playBtn.classList.add('bg-amber-500', 'text-white');
    playBtn.classList.remove('bg-white', 'bg-[#FFE5EC]', 'text-[#831843]', 'text-[#0F172A]');
  }

  // Đặt lại toàn bộ nét về trạng thái ẩn
  resetAnimStrokes(false);

  let currentAnimIdx = 0;

  function animateNextStroke() {
    if (!isPlayingAnimation) {
      stopStrokeAnimation();
      return;
    }

    // Khi đã phát nét xong toàn bộ
    if (currentAnimIdx >= strokePaths.length) {
      // Dừng lại 800ms để người học quan sát trọn vẹn chữ hoàn chỉnh, sau đó làm sạch bảng trắng
      animTimer = setTimeout(() => {
        resetAnimStrokes(true, () => {
          clearCanvas(false);
          stopStrokeAnimation();
        });
      }, 800);
      return;
    }

    const p = document.getElementById('strokePath_' + currentAnimIdx);
    if (p) {
      p.style.transition = 'stroke-dashoffset 0.55s ease-out';
      p.style.strokeDashoffset = '0';
    }

    currentAnimIdx++;
    animTimer = setTimeout(animateNextStroke, 650);
  }

  animTimer = setTimeout(animateNextStroke, 150);
}

function stopStrokeAnimation() {
  isPlayingAnimation = false;
  if (animTimer) {
    clearTimeout(animTimer);
    animTimer = null;
  }

  const playBtn = document.getElementById('btnPlayStroke') || document.querySelector('[data-action="play-stroke"]');
  if (playBtn) {
    const iconEl = playBtn.querySelector('.icon') || playBtn.querySelector('span:first-child');
    const labelEl = playBtn.querySelector('.label') || playBtn.querySelector('span:last-child');
    if (iconEl) iconEl.textContent = '▷';
    if (labelEl) labelEl.textContent = 'Phát nét';
    playBtn.classList.remove('bg-amber-500', 'text-white');
    playBtn.classList.add('bg-white', 'text-[#0F172A]');
  }
}

// =========================================================================
// 5. KHỞI TẠO BẢNG CANVAS 2D ĐỒ HỌA CAO CẤP
// =========================================================================
const CANONICAL_CANVAS_SIZE = 360;

function setupCanvas() {
  canvas = document.getElementById('practiceBoard');
  if (!canvas) return;

  ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 2;

  // Giữ kích thước pixel nội tại cố định 360 * DPR, bảo toàn tỷ lệ và không bị co giãn CSS làm méo nét
  canvas.width = Math.round(CANONICAL_CANVAS_SIZE * dpr);
  canvas.height = Math.round(CANONICAL_CANVAS_SIZE * dpr);

  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.scale(dpr, dpr);

  ctx.lineWidth = 5;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.strokeStyle = '#0F2545';

  // Lắng nghe sự kiện vẽ (Chuột & Cảm ứng chạm)
  canvas.addEventListener('mousedown', startDrawing);
  canvas.addEventListener('mousemove', draw);
  canvas.addEventListener('mouseup', stopDrawing);
  canvas.addEventListener('mouseleave', stopDrawing);

  canvas.addEventListener('touchstart', (e) => {
    e.preventDefault();
    const touch = e.touches[0];
    const mouseEvent = new MouseEvent('mousedown', {
      clientX: touch.clientX,
      clientY: touch.clientY
    });
    canvas.dispatchEvent(mouseEvent);
  }, { passive: false });

  canvas.addEventListener('touchmove', (e) => {
    e.preventDefault();
    const touch = e.touches[0];
    const mouseEvent = new MouseEvent('mousemove', {
      clientX: touch.clientX,
      clientY: touch.clientY
    });
    canvas.dispatchEvent(mouseEvent);
  }, { passive: false });

  canvas.addEventListener('touchend', (e) => {
    e.preventDefault();
    const mouseEvent = new MouseEvent('mouseup', {});
    canvas.dispatchEvent(mouseEvent);
  }, { passive: false });
}

function getCanvasCoordinates(e) {
  const rect = canvas.getBoundingClientRect();
  const scale = CANONICAL_CANVAS_SIZE / (rect.width || CANONICAL_CANVAS_SIZE);
  return {
    x: (e.clientX - rect.left) * scale,
    y: (e.clientY - rect.top) * scale
  };
}

function startDrawing(e) {
  isDrawing = true;
  const pos = getCanvasCoordinates(e);
  currentStroke = [pos];

  ctx.beginPath();
  ctx.moveTo(pos.x, pos.y);
}

function draw(e) {
  if (!isDrawing) return;
  const pos = getCanvasCoordinates(e);
  currentStroke.push(pos);

  ctx.lineTo(pos.x, pos.y);
  ctx.stroke();
}

function stopDrawing() {
  if (!isDrawing) return;
  isDrawing = false;
  if (currentStroke.length > 1) {
    drawnStrokes.push([...currentStroke]);
  }
  currentStroke = [];
}

function redrawCanvas() {
  if (!canvas || !ctx) return;
  ctx.clearRect(0, 0, CANONICAL_CANVAS_SIZE, CANONICAL_CANVAS_SIZE);

  drawnStrokes.forEach(stroke => {
    if (stroke.length < 2) return;
    ctx.beginPath();
    ctx.moveTo(stroke[0].x, stroke[0].y);
    for (let i = 1; i < stroke.length; i++) {
      ctx.lineTo(stroke[i].x, stroke[i].y);
    }
    ctx.stroke();
  });
}

function undoLastStroke() {
  if (drawnStrokes.length > 0) {
    drawnStrokes.pop();
    redrawCanvas();
    showToast('Đã hoàn tác một nét vừa vẽ.', 'info');
  } else {
    showToast('Bảng vẽ hiện chưa có nét nào để hoàn tác.', 'info');
  }
}

function clearCanvas(showToastNotification = false) {
  drawnStrokes = [];
  redrawCanvas();
  if (showToastNotification === true) {
    showToast('Đã xóa trắng bảng vẽ.', 'info');
  }
}

function setElementText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text !== null && text !== undefined ? String(text) : '';
}

// =========================================================================
// 6. CẬP NHẬT THÔNG TIN HÁN TỰ & KHAY XEM TỪNG NÉT
// =========================================================================
async function loadKanjiDetailedInfo(character) {
  try {
    let data = null;

    // 1. Thử gọi API chi tiết /api/learner/kanji/detail?char=...
    let res = await fetch(`/api/learner/kanji/detail?char=${encodeURIComponent(character)}`).catch(() => null);
    if (!res || !res.ok) {
      res = await fetch(`/api/kanji/detail?char=${encodeURIComponent(character)}`).catch(() => null);
    }
    if (!res || !res.ok) {
      res = await fetch(`/api/kanji/${encodeURIComponent(character)}`).catch(() => null);
    }

    if (res && res.ok) {
      const json = await res.json();
      data = json.data || json;
    }

    // 2. Dự phòng: Tìm qua search nếu detail chưa trả về
    if (!data || (!data.hanViet && !data.meaning)) {
      const sRes = await fetch(`/api/learner/kanji/search?keyword=${encodeURIComponent(character)}&size=1`).catch(() => null);
      if (sRes && sRes.ok) {
        const sJson = await sRes.json();
        if (sJson.items && sJson.items.length > 0) {
          const found = sJson.items.find(x => (x.kanjiChar === character || x.character === character)) || sJson.items[0];
          data = found;
        }
      }
    }

    // 3. Dự phòng: Kiểm tra localStorage 'selected_kanji_detail' nếu cùng chữ
    if (!data || !data.hanViet) {
      try {
        const cachedRaw = localStorage.getItem('selected_kanji_detail');
        if (cachedRaw) {
          const cached = JSON.parse(cachedRaw);
          if (cached && (cached.character === character || cached.kanjiChar === character)) {
            data = { ...cached, ...data };
          }
        }
      } catch (e) {}
    }

    if (data) {
      const hanViet = data.hanViet || data.han_viet || data.sinoVietnamese || '—';
      const meaning = data.meaning || data.meaningVn || data.definition || 'Chưa cập nhật nghĩa tiếng Việt.';
      const onyomi = data.onyomi || data.on_yomi || data.onReading || '-';
      const kunyomi = data.kunyomi || data.kun_yomi || data.kunReading || '-';
      const strokeCount = data.strokeCount || data.totalStrokes || data.strokes || strokePaths.length || 5;
      const radical = data.radical || data.bushu || data.mainRadical || '-';
      const jlpt = data.jlptLevel || data.jlpt || data.level || 'N3';

      // Binding lên tất cả các DOM IDs chuẩn
      setElementText('canvasActiveHanViet', `(Âm Hán: ${hanViet})`);
      setElementText('detailHanViet', hanViet);
      setElementText('kanjiHanViet', hanViet);
      setElementText('panelHanViet', hanViet);

      setElementText('detailMeaning', meaning);
      setElementText('kanjiMeaning', meaning);
      setElementText('panelMeaning', meaning);

      setElementText('detailOnyomi', onyomi);
      setElementText('kanjiOnyomi', onyomi);
      setElementText('panelOnyomi', onyomi);

      setElementText('detailKunyomi', kunyomi);
      setElementText('kanjiKunyomi', kunyomi);
      setElementText('panelKunyomi', kunyomi);

      setElementText('detailRadical', radical);
      setElementText('kanjiRadical', radical);
      setElementText('panelRadical', radical);

      setElementText('detailJlpt', jlpt);
      setElementText('kanjiJlptLevel', jlpt);
      setElementText('kanjiJlptBadge', jlpt);
      setElementText('panelJlpt', jlpt);

      setElementText('detailStrokeCount', `${strokeCount} nét`);
      setElementText('strokeCountBadge', `${strokeCount} nét`);
      setElementText('kanjiStrokeBadge', `${strokeCount} nét`);
      setElementText('panelStrokeCount', `${strokeCount} nét`);

      // Nếu backend có sẵn svgData và strokePaths đang rỗng
      if (strokePaths.length === 0 && data.svgData) {
        strokePaths = parseKanjiSvgPaths(data.svgData);
        renderGuidelineSvg();
        renderStrokeStepBadges();
      }
    }
  } catch (err) {
    console.debug('Lỗi nạp thông tin chi tiết chữ Hán:', err);
  }
}

async function selectKanji(char) {
  if (!char || typeof char !== 'string') return;
  let targetChar = '';
  try {
    targetChar = decodeURIComponent(char).trim().charAt(0);
  } catch (e) {
    targetChar = char.trim().charAt(0);
  }
  if (!targetChar) return;

  currentChar = targetChar;

  // Cập nhật ngay ký tự lên thanh tiêu đề Canvas
  setElementText('canvasActiveChar', targetChar);
  setElementText('kanjiDisplayChar', targetChar);
  setElementText('canvasActiveHanViet', `(Chữ Hán: ${targetChar})`);

  // Tải đồng thời cả Dữ liệu nét vẽ (KanjiVG) và Thông tin chi tiết từ Database
  const [, detailResult] = await Promise.all([
    (async () => {
      strokePaths = await loadKanjiSvg(targetChar);
      const badgeEl = document.getElementById('strokeCountBadge');

      if (strokePaths.length === 0) {
        if (badgeEl) badgeEl.textContent = '0 nét';
        setElementText('kanjiStrokeBadge', '0 nét');
        setElementText('detailStrokeCount', '0 nét');
        renderGuidelineSvg();
        renderStrokeStepBadges();
        return;
      }

      if (badgeEl) badgeEl.textContent = `${strokePaths.length} nét`;
      setElementText('kanjiStrokeBadge', `${strokePaths.length} nét`);
      setElementText('detailStrokeCount', `${strokePaths.length} nét`);

      activeStrokeFilter = null;
      renderGuidelineSvg();
      renderStrokeStepBadges();
      clearCanvas();
      stopStrokeAnimation();
    })(),
    loadKanjiDetailedInfo(targetChar)
  ]);
}

function renderStrokeStepBadges() {
  const container = document.getElementById('strokeStepsContainer');
  if (!container) return;

  container.innerHTML = '';

  if (strokePaths.length === 0) {
    container.innerHTML = '<span class="text-xs text-[#68737D] italic p-1">Nhập một chữ Hán ở trên để nạp thứ tự từng nét.</span>';
    return;
  }

  // Nút Tất cả nét
  const allBtn = document.createElement('button');
  allBtn.type = 'button';
  allBtn.className = `stroke-step-badge ${activeStrokeFilter === null ? 'active' : ''}`;
  allBtn.textContent = 'Tất cả';
  allBtn.title = 'Hiển thị tất cả các nét';
  allBtn.style.width = 'auto';
  allBtn.style.padding = '0 10px';
  allBtn.addEventListener('click', () => {
    activeStrokeFilter = null;
    const label = document.getElementById('activeStrokeLabel');
    if (label) label.textContent = 'Tất cả nét';
    renderGuidelineSvg();
    renderStrokeStepBadges();
  });
  container.appendChild(allBtn);

  // Các nút số nét 1, 2, 3...
  strokePaths.forEach((_, idx) => {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `stroke-step-badge ${activeStrokeFilter === idx ? 'active' : ''}`;
    btn.textContent = idx + 1;
    btn.title = `Xem riêng nét thứ ${idx + 1}`;
    btn.addEventListener('click', () => {
      activeStrokeFilter = idx;
      const label = document.getElementById('activeStrokeLabel');
      if (label) label.textContent = `Nét số ${idx + 1}`;
      renderGuidelineSvg();
      renderStrokeStepBadges();

      // Cho nét đó hiện rõ lên
      const p = document.getElementById(`strokePath_${idx}`);
      if (p) {
        const len = p.getTotalLength ? p.getTotalLength() : 200;
        p.style.transition = 'none';
        p.style.strokeDashoffset = `${len}`;
        p.setAttribute('stroke', '#831843');
        requestAnimationFrame(() => {
          p.style.transition = 'stroke-dashoffset 0.4s ease-out';
          p.style.strokeDashoffset = '0';
        });
      }
    });
    container.appendChild(btn);
  });
}

/**
 * Cất chữ Hán đang luyện viết trực tiếp vào Bộ thẻ Flashcard cá nhân
 */
function saveShodoCurrentCharToDeck() {
  if (!currentChar) {
    showToast('Vui lòng chọn hoặc nạp một chữ Hán trước khi lưu thẻ.', 'info');
    return;
  }

  const hv = document.getElementById('detailHanViet')?.textContent || '';
  const meaning = document.getElementById('detailMeaning')?.textContent || '';
  const onyomi = document.getElementById('detailOnyomi')?.textContent || '—';
  const kunyomi = document.getElementById('detailKunyomi')?.textContent || '—';
  const jlpt = document.getElementById('kanjiJlptLevel')?.textContent || 'N3';

  try {
    const raw = localStorage.getItem('kizuna_flashcard_decks');
    let decks = raw ? JSON.parse(raw) : [];
    if (!Array.isArray(decks)) decks = [];

    let deck = decks.find(d => d.title === 'Bộ Thẻ Cá Nhân' || d.title === 'Bộ Thẻ Shodo');
    if (!deck && decks.length > 0) {
      deck = decks[0];
    }
    if (!deck) {
      deck = {
        id: 'deck_' + Date.now(),
        title: 'Bộ Thẻ Cá Nhân',
        description: 'Bộ thẻ học viên tạo từ Shodo & Từ điển',
        cards: [],
        color: 'blue'
      };
      decks.push(deck);
    }

    deck.cards = deck.cards || [];
    const existingIdx = deck.cards.findIndex(c => (c.char || c.kanjiChar) === currentChar);
    const newCard = {
      char: currentChar,
      kanjiChar: currentChar,
      hanViet: (hv && hv !== '—') ? hv : 'HÁN TỰ',
      meaning: (meaning && meaning !== '—') ? meaning : 'Chữ Hán luyện viết Shodo',
      onyomi: onyomi,
      kunyomi: kunyomi,
      jlpt: (jlpt && jlpt !== 'Chưa chọn chữ') ? jlpt : 'N3'
    };

    if (existingIdx >= 0) {
      deck.cards[existingIdx] = { ...deck.cards[existingIdx], ...newCard };
    } else {
      deck.cards.push(newCard);
    }

    localStorage.setItem('kizuna_flashcard_decks', JSON.stringify(decks));
    showToast(`Đã cất chữ "${currentChar}" vào bộ thẻ "${deck.title}" thành công! 🗂️`, 'success');
  } catch (err) {
    console.error('Lỗi lưu thẻ từ Shodo:', err);
    showToast('Lỗi khi cất vào bộ thẻ: ' + err.message, 'error');
  }
}

// =========================================================================
// 8. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', async () => {
  setupCanvas();
  renderStrokeStepBadges();

  // Nút Play hoạt ảnh (hỗ trợ cả các ID và data-action)
  const playBtn = document.getElementById('btnPlayStroke') || document.querySelector('[data-action="play-stroke"]');
  playBtn?.addEventListener('click', playStrokeAnimation);

  // Thiết lập bật / tắt nét mờ hướng dẫn (Zero Layout Shift)
  setupGhostStrokeToggle();

  // Nút Undo & Clear
  const undoBtn = document.getElementById('btnUndoStroke') || document.querySelector('[data-action="undo-stroke"]');
  undoBtn?.addEventListener('click', undoLastStroke);

  const clearBtn = document.getElementById('btnClearBoard') || document.getElementById('btnClearCanvas') || document.querySelector('[data-action="clear-canvas"]');
  clearBtn?.addEventListener('click', () => clearCanvas(true));

  // Nút Cất thẻ
  document.getElementById('btnShodoSaveDeck')?.addEventListener('click', saveShodoCurrentCharToDeck);

  // Nạp chữ mới
  document.getElementById('btnLoadChar')?.addEventListener('click', () => {
    const input = document.getElementById('inputQuickChar');
    if (input && input.value.trim()) {
      selectKanji(input.value.trim());
      input.value = '';
    } else {
      showToast('Vui lòng nhập một chữ Hán vào ô tìm kiếm.', 'info');
    }
  });

  document.getElementById('inputQuickChar')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const input = document.getElementById('inputQuickChar');
      if (input && input.value.trim()) {
        selectKanji(input.value.trim());
        input.value = '';
      } else {
        showToast('Vui lòng nhập một chữ Hán vào ô tìm kiếm.', 'info');
      }
    }
  });

  // Bắt tham số ?char=... hoặc ?kanji=... trên URL nếu có
  const params = new URLSearchParams(window.location.search);
  const rawChar = params.get('char') || params.get('kanji');
  let targetChar = '';
  if (rawChar) {
    try {
      targetChar = decodeURIComponent(rawChar).trim();
    } catch (e) {
      targetChar = rawChar.trim();
    }
  }

  if (targetChar) {
    await selectKanji(targetChar);
  } else {
    // Khi chưa tìm kiếm hoặc chưa có tham số trên URL, giữ trạng thái chờ, không nạp trước dữ liệu mẫu
    currentChar = null;
    strokePaths = [];
    renderGuidelineSvg();
    renderStrokeStepBadges();
  }
});

// Gắn toàn cục
window.selectKanji = selectKanji;
window.loadKanjiDetailedInfo = loadKanjiDetailedInfo;
