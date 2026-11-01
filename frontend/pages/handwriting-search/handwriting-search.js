/**
 * KizunaTutor - Tra cứu chữ Hán bằng nét vẽ tay AI (OHR)
 * Feature-Based architecture module, tuân thủ FRONTEND_RULES.md
 */

import { showToast } from '/js/main.js';

// =========================================================================
// 1. TỪ ĐIỂN TRA CỨU HÁN TỰ TIÊU BIỂU (OFFLINE ENRICHMENT DICTIONARY)
// =========================================================================
const KANJI_METADATA_MAP = {
  '日': { hanViet: 'NHẬT', meaning: 'Mặt trời, ban ngày, nước Nhật', jlpt: 'N5', onyomi: 'ニチ, ジツ', kunyomi: 'ひ' },
  '月': { hanViet: 'NGUYỆT', meaning: 'Mặt trăng, tháng', jlpt: 'N5', onyomi: 'ゲツ, ガツ', kunyomi: 'つき' },
  '火': { hanViet: 'HỎA', meaning: 'Ngọn lửa, ngày thứ ba', jlpt: 'N5', onyomi: 'カ', kunyomi: 'ひ' },
  '水': { hanViet: 'THỦY', meaning: 'Nước, ngày thứ tư', jlpt: 'N5', onyomi: 'スイ', kunyomi: 'みず' },
  '木': { hanViet: 'MỘC', meaning: 'Cây cối, gỗ, ngày thứ năm', jlpt: 'N5', onyomi: 'モク, ボク', kunyomi: 'き' },
  '金': { hanViet: 'KIM', meaning: 'Vàng, tiền bạc, ngày thứ sáu', jlpt: 'N5', onyomi: 'キン', kunyomi: 'かね' },
  '土': { hanViet: 'THỔ', meaning: 'Đất đai, ngày thứ bảy', jlpt: 'N5', onyomi: 'ド, ト', kunyomi: 'つち' },
  '本': { hanViet: 'BỔN / BẢN', meaning: 'Sách, gốc rễ, nguồn gốc', jlpt: 'N5', onyomi: 'ホン', kunyomi: 'もと' },
  '人': { hanViet: 'NHÂN', meaning: 'Con người, nhân loại', jlpt: 'N5', onyomi: 'ジン, ニン', kunyomi: 'ひと' },
  '学': { hanViet: 'HỌC', meaning: 'Học tập, kiến thức', jlpt: 'N5', onyomi: 'ガク', kunyomi: 'まな.ぶ' },
  '生': { hanViet: 'SINH', meaning: 'Sinh sống, ra đời, học sinh', jlpt: 'N5', onyomi: 'セイ, ショウ', kunyomi: 'い.きる' },
  '先': { hanViet: 'TIÊN', meaning: 'Trước, đi trước, thầy giáo', jlpt: 'N5', onyomi: 'セン', kunyomi: 'さき' },
  '愛': { hanViet: 'ÁI', meaning: 'Tình yêu thương, quý trọng', jlpt: 'N3', onyomi: 'アイ', kunyomi: 'いと.しい' },
  '心': { hanViet: 'TÂM', meaning: 'Trái tim, tấm lòng, tâm trí', jlpt: 'N4', onyomi: 'シン', kunyomi: 'こころ' },
  '道': { hanViet: 'ĐẠO', meaning: 'Con đường, đạo lý', jlpt: 'N4', onyomi: 'ドウ', kunyomi: 'みち' },
  '和': { hanViet: 'HÒA', meaning: 'Hòa bình, hòa thuận, phong cách Nhật', jlpt: 'N3', onyomi: 'ワ', kunyomi: 'やわ.らぐ' },
  '絆': { hanViet: 'BÁN', meaning: 'Sợi dây gắn kết bền vững Kizuna', jlpt: 'N1', onyomi: 'ハン', kunyomi: 'きずな' },
  '山': { hanViet: 'SƠN', meaning: 'Núi non', jlpt: 'N5', onyomi: 'サン', kunyomi: 'やま' },
  '川': { hanViet: 'XUYÊN', meaning: 'Dòng sông', jlpt: 'N5', onyomi: 'セン', kunyomi: 'かわ' },
  '田': { hanViet: 'ĐIỀN', meaning: 'Ruộng đồng', jlpt: 'N5', onyomi: 'デン', kunyomi: 'た' },
  '目': { hanViet: 'MỤC', meaning: 'Con mắt, cái nhìn', jlpt: 'N5', onyomi: 'モク', kunyomi: 'め' },
  '口': { hanViet: 'KHẨU', meaning: 'Miệng, lối vào', jlpt: 'N5', onyomi: 'コウ, ク', kunyomi: 'くち' },
  '白': { hanViet: 'BẠCH', meaning: 'Màu trắng, trong sáng', jlpt: 'N5', onyomi: 'ハク', kunyomi: 'しろ' }
};

// =========================================================================
// 2. TRẠNG THÁI CANVAS & QUẢN LÝ NÉT VẼ
// =========================================================================
let canvas = null;
let ctx = null;
let strokes = []; // Mảng chứa các nét [{x, y, t}]
let currentStroke = [];
let isDrawing = false;
let recognizeDebounceTimer = null;

// =========================================================================
// 3. KHỞI TẠO BẢNG CANVAS Ô MỄ (300x300 HIGH-DPI)
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
  ctx.lineWidth = 5.5;
  ctx.strokeStyle = '#0F2545';
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
  if (currentStroke.length > 0) {
    strokes.push([...currentStroke]);
    currentStroke = [];
    updateStrokeBadge();
    triggerAutoRecognition();
  }
}

function onTouchStart(e) {
  e.preventDefault();
  isDrawing = true;
  const pt = getCoords(e);
  currentStroke = [pt];

  ctx.beginPath();
  ctx.moveTo(pt.x, pt.y);
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.lineWidth = 5.5;
  ctx.strokeStyle = '#0F2545';
}

function onTouchMove(e) {
  e.preventDefault();
  if (!isDrawing) return;
  const pt = getCoords(e);
  currentStroke.push(pt);

  ctx.lineTo(pt.x, pt.y);
  ctx.stroke();
}

function onTouchEnd(e) {
  if (!isDrawing) return;
  isDrawing = false;
  if (currentStroke.length > 0) {
    strokes.push([...currentStroke]);
    currentStroke = [];
    updateStrokeBadge();
    triggerAutoRecognition();
  }
}

function redrawBoard() {
  if (!canvas || !ctx) return;
  const w = 300;
  const h = 300;

  ctx.clearRect(0, 0, w, h);

  // 1. Vẽ nền ô Mễ và khung viền
  ctx.save();
  ctx.fillStyle = '#FFFFFF';
  ctx.fillRect(0, 0, w, h);

  ctx.strokeStyle = '#ECE8DE';
  ctx.lineWidth = 1;
  ctx.strokeRect(0, 0, w, h);

  // Đường kẻ đứt nét chữ thập & đường chéo ô Mễ
  ctx.strokeStyle = '#CBD5E1';
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
// 4. KÍCH HOẠT NHẬN DIỆN HYBRID (GOOGLE OHR + FALLBACK)
// =========================================================================
function triggerAutoRecognition() {
  if (recognizeDebounceTimer) clearTimeout(recognizeDebounceTimer);
  if (strokes.length === 0) return;

  showLoadingState();
  recognizeDebounceTimer = setTimeout(() => {
    executeRecognition();
  }, 220);
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
        // Lọc các ký tự tiếng Nhật / Hán tự
        const candidates = rawCandidates.filter(c => typeof c === 'string' && c.trim().length > 0).slice(0, 8);
        renderCandidates(candidates);
        return;
      }
    }
  } catch (err) {
    console.debug('Không kết nối được dịch vụ nhận diện trực tuyến, kích hoạt fallback:', err);
  }

  // Fallback ngoại tuyến dựa trên số nét vẽ
  fallbackOfflineRecognition();
}

function fallbackOfflineRecognition() {
  const strokeCount = strokes.length;
  // Gợi ý danh sách chữ dựa trên số lượng nét
  const suggestionsByCount = {
    1: ['一', '乙'],
    2: ['二', '十', '人', '入', '八', '力', '刀', '又'],
    3: ['三', '山', '川', '大', '小', '口', '子', '女', '土'],
    4: ['日', '月', '木', '水', '火', '心', '文', '方', '円'],
    5: ['本', '目', '田', '白', '生', '正', '立', '石', '市'],
    6: ['休', '先', '年', '名', '同', '多', '光', '西', '早'],
    8: ['学', '和', '金', '東', '雨', '国', '青', '空', '京'],
    11: ['絆', '道', '教', '理', '動', '野', '悪', '強', '問'],
    13: ['愛', '話', '新', '感', '電', '暗', '数', '業', '想']
  };

  const matched = suggestionsByCount[strokeCount] || ['日', '本', '心', '愛', '道', '学', '和', '絆'];
  renderCandidates(matched);
}

// =========================================================================
// 5. RENDER KHAY KẾT QUẢ CHỮ HÁN GỢI Ý
// =========================================================================
function showStandbyState() {
  document.getElementById('hwStandbyState')?.classList.remove('hidden');
  document.getElementById('hwLoadingState')?.classList.add('hidden');
  document.getElementById('hwCandidateGrid')?.classList.add('hidden');
}

function showLoadingState() {
  document.getElementById('hwStandbyState')?.classList.add('hidden');
  document.getElementById('hwLoadingState')?.classList.remove('hidden');
  document.getElementById('hwCandidateGrid')?.classList.add('hidden');
}

function renderCandidates(candidateList) {
  const grid = document.getElementById('hwCandidateGrid');
  const loading = document.getElementById('hwLoadingState');
  const standby = document.getElementById('hwStandbyState');

  if (!grid) return;

  if (candidateList.length === 0) {
    showStandbyState();
    return;
  }

  if (loading) loading.classList.add('hidden');
  if (standby) standby.classList.add('hidden');
  grid.classList.remove('hidden');

  let html = '';

  candidateList.forEach((char, idx) => {
    const meta = KANJI_METADATA_MAP[char] || {
      hanViet: 'HÁN TỰ',
      meaning: 'Tra cứu nét chữ tương ứng',
      jlpt: 'Tự do',
      onyomi: '—',
      kunyomi: '—'
    };

    html += `
      <div class="bg-white border border-[#ECE8DE] rounded-2xl p-4 flex items-center justify-between gap-3 hover:border-[#006194] hover:shadow-sm transition">
        <div class="flex items-center gap-3 min-w-0">
          <!-- Chữ Hán to -->
          <div class="w-14 h-14 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] flex items-center justify-center font-japanese font-bold text-3xl text-[#0F2545] shrink-0">
            ${char}
          </div>

          <div class="min-w-0">
            <div class="flex items-center gap-1.5 flex-wrap">
              <span class="font-bold text-sm text-[#006194]">${meta.hanViet}</span>
              <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">
                ${meta.jlpt}
              </span>
            </div>
            <p class="text-xs text-[#68737D] mt-0.5 truncate max-w-[180px]">
              ${meta.meaning}
            </p>
          </div>
        </div>

        <div class="flex items-center gap-1.5 shrink-0">
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
