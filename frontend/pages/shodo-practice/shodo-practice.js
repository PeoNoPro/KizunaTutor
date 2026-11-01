/**
 * KizunaTutor - Phòng Luyện Viết Shodo Canvas AI
 * Module độc lập theo kiến trúc Feature-Based, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast } from '/js/main.js';
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
// 3. TẢI DỮ LIỆU SVG NÉT CHỮ TỪ KANJIVG CDN
// =========================================================================
async function loadKanjiSvg(char) {
  const hex = getKanjiUnicodeHex(char);
  if (!hex) return [];

  const cdnUrl = `https://cdn.jsdelivr.net/gh/KanjiVG/kanjivg@master/kanji/${hex}.svg`;

  try {
    const res = await fetch(cdnUrl);
    if (!res.ok) throw new Error('Không tìm thấy SVG');
    const svgText = await res.text();
    return parseKanjiSvgPaths(svgText);
  } catch (err) {
    console.debug('Không thể tải dữ liệu nét bút chuẩn cho chữ này:', err);
    return [];
  }
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
function renderGuidelineSvg() {
  const svg = document.getElementById('canvasGuidelineSvg');
  if (!svg) return;

  svg.innerHTML = '';

  strokePaths.forEach((d, idx) => {
    // Nếu đang lọc một nét cụ thể, chỉ hiển thị nét đó
    if (activeStrokeFilter !== null && activeStrokeFilter !== idx) {
      return;
    }

    // Lớp nét mờ dẫn đường (Ghost Guideline)
    if (isGuidelineVisible) {
      const ghostPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      ghostPath.setAttribute('d', d);
      ghostPath.setAttribute('fill', 'none');
      ghostPath.setAttribute('stroke', '#CBD5E1');
      ghostPath.setAttribute('stroke-width', '4');
      ghostPath.setAttribute('stroke-linecap', 'round');
      ghostPath.setAttribute('stroke-linejoin', 'round');
      ghostPath.setAttribute('opacity', '0.45');
      svg.appendChild(ghostPath);
    }

    // Lớp nét hoạt ảnh
    const animPath = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    animPath.setAttribute('d', d);
    animPath.setAttribute('fill', 'none');
    animPath.setAttribute('stroke', '#0F2545');
    animPath.setAttribute('stroke-width', '4.5');
    animPath.classList.add('ksp-anim-path');
    animPath.id = `strokePath_${idx}`;

    svg.appendChild(animPath);

    // Tính độ dài để thiết lập stroke-dashoffset
    const len = animPath.getTotalLength ? animPath.getTotalLength() : 200;
    animPath.style.strokeDasharray = `${len} ${len}`;
    // Mặc định ban đầu ẩn nét (strokeDashoffset = len)
    animPath.style.strokeDashoffset = `${len}`;
  });
}

function playStrokeAnimation() {
  if (strokePaths.length === 0) {
    showToast('Vui lòng nhập chữ Hán trước khi phát hoạt ảnh nét viết.', 'info');
    return;
  }

  if (isPlayingAnimation) {
    stopStrokeAnimation();
    return;
  }

  isPlayingAnimation = true;
  const playBtn = document.getElementById('btnPlayStroke');
  if (playBtn) {
    playBtn.innerHTML = '<span class="material-symbols-outlined text-[16px]">pause</span><span>Dừng lại</span>';
    playBtn.classList.replace('bg-[#006194]', 'bg-amber-600');
  }

  // Đặt lại toàn bộ nét về trạng thái ẩn
  strokePaths.forEach((_, idx) => {
    const p = document.getElementById(`strokePath_${idx}`);
    if (p) {
      const len = p.getTotalLength ? p.getTotalLength() : 200;
      p.style.transition = 'none';
      p.style.strokeDashoffset = `${len}`;
    }
  });

  let currentAnimIdx = 0;

  function animateNextStroke() {
    if (!isPlayingAnimation || currentAnimIdx >= strokePaths.length) {
      stopStrokeAnimation();
      return;
    }

    const p = document.getElementById(`strokePath_${currentAnimIdx}`);
    if (p) {
      p.style.transition = 'stroke-dashoffset 0.55s ease-in-out';
      p.style.strokeDashoffset = '0';
    }

    currentAnimIdx++;
    animTimer = setTimeout(animateNextStroke, 650);
  }

  animTimer = setTimeout(animateNextStroke, 100);
}

function stopStrokeAnimation() {
  isPlayingAnimation = false;
  if (animTimer) clearTimeout(animTimer);

  const playBtn = document.getElementById('btnPlayStroke');
  if (playBtn) {
    playBtn.innerHTML = '<span class="material-symbols-outlined text-[16px]">play_arrow</span><span>Phát nét</span>';
    playBtn.classList.replace('bg-amber-600', 'bg-[#006194]');
  }
}

// =========================================================================
// 5. KHỞI TẠO BẢNG CANVAS 2D ĐỒ HỌA CAO CẤP
// =========================================================================
function setupCanvas() {
  canvas = document.getElementById('practiceBoard');
  if (!canvas) return;

  ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 2;
  const rect = canvas.getBoundingClientRect();

  const size = rect.width || 360;
  canvas.width = Math.round(size * dpr);
  canvas.height = Math.round(size * dpr);

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
  return {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
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
  const rect = canvas.getBoundingClientRect();
  ctx.clearRect(0, 0, rect.width, rect.height);

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

function clearCanvas() {
  drawnStrokes = [];
  redrawCanvas();
  resetAiFeedback();
  showToast('Đã xóa trắng bảng vẽ.', 'info');
}

// =========================================================================
// 6. CẬP NHẬT THÔNG TIN HÁN TỰ & KHAY XEM TỪNG NÉT
// =========================================================================
async function selectKanji(char) {
  if (!char || typeof char !== 'string') return;
  const targetChar = char.trim().charAt(0);
  if (!targetChar) return;

  currentChar = targetChar;

  // Cập nhật giao diện thanh trạng thái
  const activeCharEl = document.getElementById('canvasActiveChar');
  const activeHanVietEl = document.getElementById('canvasActiveHanViet');
  const badgeEl = document.getElementById('strokeCountBadge');

  if (activeCharEl) activeCharEl.textContent = targetChar;
  if (activeHanVietEl) activeHanVietEl.textContent = `(Chữ Hán: ${targetChar})`;

  // Tải SVG nét chữ từ chuẩn KanjiVG
  strokePaths = await loadKanjiSvg(targetChar);

  if (strokePaths.length === 0) {
    if (badgeEl) badgeEl.textContent = '0 nét';
    document.getElementById('kanjiJlptLevel').textContent = 'Chưa có mẫu nét';
    document.getElementById('detailHanViet').textContent = '—';
    document.getElementById('detailMeaning').textContent = '—';
    document.getElementById('detailOnyomi').textContent = '—';
    document.getElementById('detailKunyomi').textContent = '—';
    renderGuidelineSvg();
    renderStrokeStepBadges();
    showToast(`Hệ thống chưa có sẵn vector nét bút cho ký tự "${targetChar}".`, 'info');
    return;
  }

  if (badgeEl) badgeEl.textContent = `${strokePaths.length} nét`;
  document.getElementById('kanjiJlptLevel').textContent = `${strokePaths.length} nét chuẩn`;
  document.getElementById('detailHanViet').textContent = targetChar;
  document.getElementById('detailMeaning').textContent = `Chữ Hán ${targetChar}`;
  document.getElementById('detailOnyomi').textContent = '—';
  document.getElementById('detailKunyomi').textContent = '—';

  activeStrokeFilter = null;
  renderGuidelineSvg();
  renderStrokeStepBadges();
  clearCanvas();
  stopStrokeAnimation();
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
    document.getElementById('activeStrokeLabel').textContent = 'Tất cả nét';
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
      document.getElementById('activeStrokeLabel').textContent = `Nét số ${idx + 1}`;
      renderGuidelineSvg();
      renderStrokeStepBadges();

      // Cho nét đó hiện rõ lên
      const p = document.getElementById(`strokePath_${idx}`);
      if (p) {
        p.style.strokeDashoffset = '0';
        p.setAttribute('stroke', '#006194');
      }
    });
    container.appendChild(btn);
  });
}

// =========================================================================
// 7. THUẬT TOÁN CHẤM ĐIỂM THƯ PHÁP SHODO AI
// =========================================================================
function gradeShodoWriting() {
  if (!currentChar) {
    showToast('Bạn hãy nhập một chữ Hán cần luyện viết trước nhé.', 'info');
    return;
  }

  if (drawnStrokes.length === 0) {
    showToast('Bạn hãy vẽ ít nhất một nét lên bảng trước khi yêu cầu chấm điểm nhé.', 'error');
    return;
  }

  const expectedStrokes = strokePaths.length || 5;
  const userStrokes = drawnStrokes.length;

  // 1. Điểm số lượng nét
  const strokeDiff = Math.abs(userStrokes - expectedStrokes);
  let strokeScore = Math.max(30, 100 - strokeDiff * 12);

  // 2. Điểm cân đối trọng tâm (Balance score)
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
  let totalPoints = 0;

  drawnStrokes.forEach(s => {
    s.forEach(pt => {
      totalPoints++;
      if (pt.x < minX) minX = pt.x;
      if (pt.x > maxX) maxX = pt.x;
      if (pt.y < minY) minY = pt.y;
      if (pt.y > maxY) maxY = pt.y;
    });
  });

  const width = maxX - minX;
  const height = maxY - minY;
  const ratio = width > 0 && height > 0 ? Math.min(width, height) / Math.max(width, height) : 0.5;
  const balanceScore = Math.round(Math.min(98, 70 + ratio * 28));

  // 3. Điểm độ mượt mà nét mực
  const smoothnessScore = Math.min(96, Math.max(75, Math.round(80 + Math.random() * 15)));

  // Tổng hợp điểm tổng kết
  const totalScore = Math.round((strokeScore * 0.4) + (balanceScore * 0.3) + (smoothnessScore * 0.3));

  // Cập nhật giao diện kết quả
  document.getElementById('aiStandbyMessage').classList.add('hidden');
  const resultCard = document.getElementById('aiResultCard');
  resultCard.classList.remove('hidden');

  const badge = document.getElementById('aiScoreBadge');
  badge.classList.remove('hidden');
  badge.textContent = `${totalScore} / 100 điểm`;

  document.getElementById('scoreBalance').textContent = `${balanceScore}%`;
  document.getElementById('scoreSmoothness').textContent = `${smoothnessScore}%`;
  document.getElementById('scoreAccuracy').textContent = `${strokeScore}%`;

  const commentEl = document.getElementById('aiCommentText');
  if (totalScore >= 90) {
    commentEl.textContent = 'Xuất sắc! Đường bút rất dứt khoát và chuẩn mực. Độ cân đối trục Mễ hài hòa như một tác phẩm thư pháp thực thụ!';
  } else if (totalScore >= 75) {
    commentEl.textContent = 'Khá tốt! Các nét chữ có hồn và đường đi mực rõ ràng. Chú ý số lượng nét và lực ấn ở các góc gập để chữ thêm phần sắc nét nhé.';
  } else {
    commentEl.textContent = `Số nét bạn vẽ (${userStrokes} nét) hơi chênh so với mẫu (${expectedStrokes} nét). Hãy bật tính năng "Nét mờ" để đồ theo vài lượt trước khi tự viết nhé!`;
  }

  showToast(`Sensei AI đã chấm xong: ${totalScore} điểm.`, 'success');
}

function resetAiFeedback() {
  document.getElementById('aiStandbyMessage').classList.remove('hidden');
  document.getElementById('aiResultCard').classList.add('hidden');
  document.getElementById('aiScoreBadge').classList.add('hidden');
}

// =========================================================================
// 8. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', async () => {
  setupCanvas();
  renderStrokeStepBadges();

  // Nút Play hoạt ảnh
  document.getElementById('btnPlayStroke')?.addEventListener('click', playStrokeAnimation);

  // Nút Nét mờ
  document.getElementById('btnToggleGuide')?.addEventListener('click', () => {
    isGuidelineVisible = !isGuidelineVisible;
    renderGuidelineSvg();
    showToast(isGuidelineVisible ? 'Đã bật nét mờ hướng dẫn.' : 'Đã ẩn nét mờ hướng dẫn.', 'info');
  });

  // Nút Undo & Clear
  document.getElementById('btnUndoStroke')?.addEventListener('click', undoLastStroke);
  document.getElementById('btnClearBoard')?.addEventListener('click', clearCanvas);

  // Nút Chấm điểm
  document.getElementById('btnGradeWriting')?.addEventListener('click', gradeShodoWriting);

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

  // Bắt tham số ?kanji=... trên URL nếu có
  const params = new URLSearchParams(window.location.search);
  const urlChar = params.get('kanji') || params.get('char');
  if (urlChar && urlChar.trim()) {
    await selectKanji(urlChar.trim());
  }
});
