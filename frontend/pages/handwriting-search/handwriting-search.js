/**
 * KizunaTutor - Tra cứu chữ Hán bằng nét vẽ tay AI (OHR)
 * Feature-Based architecture module, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'handwriting',
  pageTitle: 'Tra cứu nét vẽ OHR',
  badge: 'Nhận diện nét vẽ AI'
});

// =========================================================================
// 1. TRẠNG THÁI CANVAS & QUẢN LÝ NÉT VẼ
// =========================================================================
let canvas = null;
let ctx = null;
let strokes = []; // Mảng chứa các nét [{x, y, t}]
let currentStroke = [];
let isDrawing = false;
let recognizeDebounceTimer = null;

// =========================================================================
// 2. KHỞI TẠO BẢNG CANVAS Ô MỄ (300x300 HIGH-DPI)
// =========================================================================
function setupCanvas() {
  canvas = document.getElementById('hwCanvas');
  if (!canvas) return;

  ctx = canvas.getContext('2d');
  const dpr = window.devicePixelRatio || 2;
  const w = 300;
  const h = 300;

  canvas.width = Math.round(w * dpr);
  canvas.height = Math.round(h * dpr);
  canvas.style.width = `${w}px`;
  canvas.style.height = `${h}px`;

  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.scale(dpr, dpr);

  // Sự kiện chuột
  canvas.addEventListener('mousedown', onMouseDown);
  canvas.addEventListener('mousemove', onMouseMove);
  window.addEventListener('mouseup', onMouseUp);

  // Sự kiện cảm ứng chạm
  canvas.addEventListener('touchstart', onTouchStart, { passive: false });
  canvas.addEventListener('touchmove', onTouchMove, { passive: false });
  window.addEventListener('touchend', onTouchEnd);

  redrawBoard();
}

function getCoords(e) {
  const rect = canvas.getBoundingClientRect();
  const clientX = (e.touches && e.touches.length > 0) ? e.touches[0].clientX : e.clientX;
  const clientY = (e.touches && e.touches.length > 0) ? e.touches[0].clientY : e.clientY;
  return {
    x: clientX - rect.left,
    y: clientY - rect.top,
    t: Date.now()
  };
}

function onMouseDown(e) {
  if (e.button !== undefined && e.button !== 0) return;
  isDrawing = true;
  const pt = getCoords(e);
  currentStroke = [pt];

  ctx.beginPath();
  ctx.moveTo(pt.x, pt.y);
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.strokeStyle = '#0F2545';
  ctx.lineWidth = 5.5;
}

function onMouseMove(e) {
  if (!isDrawing) return;
  const pt = getCoords(e);
  currentStroke.push(pt);

  ctx.lineTo(pt.x, pt.y);
  ctx.stroke();
}

function onMouseUp() {
  if (!isDrawing) return;
  isDrawing = false;
  if (currentStroke.length > 1) {
    strokes.push([...currentStroke]);
    updateStrokeBadge();
    triggerAutoRecognition();
  }
  currentStroke = [];
}

function onTouchStart(e) {
  e.preventDefault();
  const touch = e.touches[0];
  const me = new MouseEvent('mousedown', {
    clientX: touch.clientX,
    clientY: touch.clientY
  });
  canvas.dispatchEvent(me);
}

function onTouchMove(e) {
  e.preventDefault();
  const touch = e.touches[0];
  const me = new MouseEvent('mousemove', {
    clientX: touch.clientX,
    clientY: touch.clientY
  });
  canvas.dispatchEvent(me);
}

function onTouchEnd(e) {
  e.preventDefault();
  const me = new MouseEvent('mouseup', {});
  window.dispatchEvent(me);
}

// =========================================================================
// 3. VẼ LẠI NỀN Ô MỄ VÀ CÁC NÉT ĐÃ LƯU
// =========================================================================
function redrawBoard() {
  if (!canvas || !ctx) return;
  const w = 300;
  const h = 300;

  ctx.clearRect(0, 0, w, h);

  // 1. Vẽ nền ô mễ (Grid lines)
  ctx.save();
  ctx.strokeStyle = '#ECE8DE';
  ctx.lineWidth = 1;
  ctx.setLineDash([4, 4]);

  ctx.beginPath();
  // 2 đường chéo
  ctx.moveTo(0, 0); ctx.lineTo(w, h);
  ctx.moveTo(w, 0); ctx.lineTo(0, h);
  // Trục chữ thập
  ctx.moveTo(w / 2, 0); ctx.lineTo(w / 2, h);
  ctx.moveTo(0, h / 2); ctx.lineTo(w, h / 2);
  ctx.stroke();
  ctx.restore();

  // 2. Vẽ lại toàn bộ các nét người dùng đã vẽ
  if (strokes.length > 0) {
    ctx.save();
    ctx.strokeStyle = '#0F2545';
    ctx.lineWidth = 5.5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    for (const s of strokes) {
      if (!s || s.length === 0) continue;
      ctx.beginPath();
      ctx.moveTo(s[0].x, s[0].y);
      for (let i = 1; i < s.length; i++) {
        ctx.lineTo(s[i].x, s[i].y);
      }
      ctx.stroke();
    }
    ctx.restore();
  }
}

function undoLastStroke() {
  if (strokes.length > 0) {
    strokes.pop();
    redrawBoard();
    updateStrokeBadge();
    if (strokes.length > 0) {
      triggerAutoRecognition();
    } else {
      showStandbyState();
    }
    showToast('Đã hoàn tác một nét vừa vẽ.', 'info');
  } else {
    showToast('Bảng vẽ hiện chưa có nét nào để hoàn tác.', 'info');
  }
}

function clearBoard() {
  strokes = [];
  currentStroke = [];
  if (recognizeDebounceTimer) clearTimeout(recognizeDebounceTimer);
  redrawBoard();
  updateStrokeBadge();
  showStandbyState();
  showToast('Đã xóa sạch bảng vẽ.', 'info');
}

function updateStrokeBadge() {
  const badge = document.getElementById('hwStrokeCountBadge');
  if (badge) {
    badge.textContent = `${strokes.length} nét`;
  }
}

// =========================================================================
// 4. KÍCH HOẠT NHẬN DIỆN NÉT VẼ AI (GOOGLE OHR SERVICE)
// =========================================================================
function triggerAutoRecognition() {
  if (recognizeDebounceTimer) clearTimeout(recognizeDebounceTimer);
  if (strokes.length === 0) return;

  showLoadingState();
  recognizeDebounceTimer = setTimeout(() => {
    executeRecognition();
  }, 250);
}

async function executeRecognition() {
  if (strokes.length === 0) {
    showStandbyState();
    return;
  }

  showLoadingState();

  // Chuẩn bị Ink format cho Google Handwriting Input Tools
  const ink = strokes.map(stroke => {
    const xs = stroke.map(p => Math.round(p.x));
    const ys = stroke.map(p => Math.round(p.y));
    const ts = stroke.map((p, idx) => idx * 10);
    return [xs, ys, ts];
  });

  const payload = {
    app_version: 0.4,
    api_level: '537.36',
    device: '5',
    input_type: '0',
    options: 'enable_pre_space',
    requests: [{
      writing_guide: { writing_area_width: 300, writing_area_height: 300 },
      pre_context: '',
      max_num_results: 10,
      max_completions: 0,
      language: 'ja',
      ink: ink
    }]
  };

  try {
    const res = await fetch('https://www.google.com.tw/inputtools/request?ime=handwriting&app=mobilesearch&cs=1&oe=UTF-8', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      const data = await res.json();
      if (data && data[1] && data[1][0] && Array.isArray(data[1][0][1])) {
        const rawCandidates = data[1][0][1];
        // Lọc các ký tự tiếng Nhật / Hán tự hợp lệ
        const candidates = rawCandidates
          .filter(c => typeof c === 'string' && c.trim().length > 0)
          .map(c => c.trim())
          .filter((c, idx, arr) => arr.indexOf(c) === idx)
          .slice(0, 10);

        if (candidates.length > 0) {
          renderCandidates(candidates);
          return;
        }
      }
    }
  } catch (err) {
    console.debug('Không thể kết nối đến dịch vụ nhận diện nét vẽ:', err);
  }

  // Khi không tìm thấy kết quả hoặc dịch vụ không phản hồi: Hiển thị Empty State chuẩn (tuyệt đối không chèn fake data)
  showEmptyResultState();
}

// =========================================================================
// 5. RENDER KHAY KẾT QUẢ CHỮ HÁN GỢI Ý & CÁC TRẠNG THÁI HIỂN THỊ
// =========================================================================
function showStandbyState() {
  document.getElementById('hwStandbyState')?.classList.remove('hidden');
  document.getElementById('hwLoadingState')?.classList.add('hidden');
  document.getElementById('hwEmptyResultState')?.classList.add('hidden');
  document.getElementById('hwCandidateGrid')?.classList.add('hidden');
}

function showLoadingState() {
  document.getElementById('hwStandbyState')?.classList.add('hidden');
  document.getElementById('hwLoadingState')?.classList.remove('hidden');
  document.getElementById('hwEmptyResultState')?.classList.add('hidden');
  document.getElementById('hwCandidateGrid')?.classList.add('hidden');
}

function showEmptyResultState() {
  document.getElementById('hwStandbyState')?.classList.add('hidden');
  document.getElementById('hwLoadingState')?.classList.add('hidden');
  document.getElementById('hwEmptyResultState')?.classList.remove('hidden');
  document.getElementById('hwCandidateGrid')?.classList.add('hidden');
}

function renderCandidates(candidateList) {
  const grid = document.getElementById('hwCandidateGrid');
  const loading = document.getElementById('hwLoadingState');
  const standby = document.getElementById('hwStandbyState');
  const emptyRes = document.getElementById('hwEmptyResultState');

  if (!grid) return;

  if (candidateList.length === 0) {
    showEmptyResultState();
    return;
  }

  if (loading) loading.classList.add('hidden');
  if (standby) standby.classList.add('hidden');
  if (emptyRes) emptyRes.classList.add('hidden');
  grid.classList.remove('hidden');

  let html = '';

  candidateList.forEach((char) => {
    html += `
      <div class="bg-white border border-[#ECE8DE] rounded-2xl p-4 flex items-center justify-between gap-3 hover:border-[#006194] hover:shadow-sm transition">
        <div class="flex items-center gap-3 min-w-0">
          <!-- Chữ Hán nhận diện được -->
          <div class="w-14 h-14 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] flex items-center justify-center font-japanese font-bold text-3xl text-[#0F2545] shrink-0">
            ${char}
          </div>

          <div class="min-w-0">
            <div class="flex items-center gap-1.5 flex-wrap">
              <span class="font-bold text-sm text-[#0F2545]">Chữ Hán đề xuất</span>
            </div>
            <p class="text-xs text-[#68737D] mt-0.5 truncate">
              Khớp với nét vẽ trên ô Mễ
            </p>
          </div>
        </div>

        <div class="flex items-center gap-1.5 shrink-0">
          <a href="/kanji-dictionary?keyword=${encodeURIComponent(char)}" 
             class="px-2.5 py-1.5 rounded-xl bg-[#FAF8F2] hover:bg-[#F2EFE6] text-[#0F2545] border border-[#ECE8DE] text-xs font-semibold transition flex items-center gap-1 no-underline"
             title="Tra cứu nghĩa, âm Hán Việt và lưu Flashcard">
            <span class="material-symbols-outlined text-[15px]">menu_book</span>
            <span>Tra từ</span>
          </a>
          <a href="/shodo?kanji=${encodeURIComponent(char)}" 
             class="px-3 py-1.5 rounded-xl bg-[#006194] hover:bg-[#004B73] text-white text-xs font-bold transition flex items-center gap-1 no-underline shadow-xs"
             title="Chuyển sang phòng luyện viết Shodo Canvas cho chữ này">
            <span class="material-symbols-outlined text-[15px]">draw</span>
            <span>Luyện viết</span>
          </a>
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;
}

// =========================================================================
// 6. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  setupCanvas();

  document.getElementById('btnHwUndo')?.addEventListener('click', undoLastStroke);
  document.getElementById('btnHwClear')?.addEventListener('click', clearBoard);
  document.getElementById('btnHwRecognize')?.addEventListener('click', executeRecognition);
});
