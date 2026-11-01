/**
 * KizunaTutor - Cẩm Nang Ngữ Pháp OpenJLPT
 * Feature-Based architecture module, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast, debounce } from '/js/main.js';

// =========================================================================
// 1. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
let currentLevel = 'N5';
let grammarList = [];
let currentSearchKeyword = '';

// =========================================================================
// 2. TẢI DỮ LIỆU NGỮ PHÁP TỪ REST API BACKEND
// =========================================================================
async function loadGrammarData() {
  const grid = document.getElementById('grammarGrid');
  const emptyState = document.getElementById('grammarEmptyState');

  try {
    const res = await fetch(`/api/learner/grammar?level=${encodeURIComponent(currentLevel)}&size=50`);
    if (res.ok) {
      const data = await res.json();
      const content = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);
      if (content.length > 0) {
        grammarList = content.map(item => ({
          id: String(item.grammarCode || item.id || `gm-${item.pattern}`),
          level: item.jlptLevel || item.level || currentLevel,
          pattern: item.pattern || '',
          romaji: item.romaji || '',
          meaning: item.meaning || item.meaningVn || '',
          formation: item.formation || 'Đang cập nhật công thức',
          notes: item.notes || 'Chưa có ghi chú đặc biệt',
          examples: item.exampleSentence ? [{
            ja: item.exampleSentence,
            reading: item.exampleFurigana || item.exampleSentence,
            vi: item.exampleMeaning || 'Ví dụ minh họa'
          }] : (Array.isArray(item.examples) ? item.examples : [])
        }));
        renderGrammarCards();
        return;
      }
    }
  } catch (e) {
    console.debug('Chưa kết nối được API Backend hoặc chưa có dữ liệu:', e);
  }

  // Khi chưa có dữ liệu từ backend, trả về danh sách rỗng để hiển thị Empty State chuẩn
  grammarList = [];
  renderGrammarCards();
}

// =========================================================================
// 3. RENDER DANH SÁCH THẺ NGỮ PHÁP (GRID VIEW)
// =========================================================================
function renderGrammarCards() {
  const grid = document.getElementById('grammarGrid');
  const emptyState = document.getElementById('grammarEmptyState');
  if (!grid || !emptyState) return;

  const q = currentSearchKeyword.trim().toLowerCase();
  const filtered = grammarList.filter(g => 
    g.pattern.toLowerCase().includes(q) ||
    g.romaji.toLowerCase().includes(q) ||
    g.meaning.toLowerCase().includes(q)
  );

  if (filtered.length === 0) {
    grid.innerHTML = '';
    grid.classList.add('hidden');
    emptyState.classList.remove('hidden');
    return;
  }

  emptyState.classList.add('hidden');
  grid.classList.remove('hidden');

  let html = '';

  filtered.forEach(rule => {
    html += `
      <div class="bg-white border border-[#ECE8DE] rounded-2xl p-5 shadow-sm hover:border-[#006194] hover:shadow-md transition flex flex-col justify-between group cursor-pointer"
           data-action="view-detail" data-id="${rule.id}">
        
        <div class="space-y-3">
          <!-- Header Thẻ -->
          <div class="flex items-center justify-between">
            <span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">
              ${rule.level}
            </span>
            
            <button type="button" data-action="pronounce" data-text="${rule.pattern}" 
                    class="w-8 h-8 rounded-lg border border-[#ECE8DE] bg-[#FAF8F2] hover:bg-white text-[#68737D] hover:text-[#006194] flex items-center justify-center transition cursor-pointer"
                    title="Nghe phát âm">
              <span class="material-symbols-outlined text-[18px]">volume_up</span>
            </button>
          </div>

          <!-- Mẫu câu & Romaji -->
          <div>
            <h3 class="font-japanese text-xl font-bold text-[#0F2545] group-hover:text-[#006194] transition">
              ${rule.pattern}
            </h3>
            <p class="text-xs text-[#68737D] font-mono mt-0.5">${rule.romaji}</p>
          </div>

          <!-- Ý nghĩa ngắn -->
          <p class="text-xs font-semibold text-[#3F4850] line-clamp-2 leading-relaxed bg-[#FAF8F2] p-2.5 rounded-xl border border-[#ECE8DE]">
            ${rule.meaning}
          </p>
        </div>

        <!-- Footer Thẻ -->
        <div class="pt-4 border-t border-[#ECE8DE] flex items-center justify-between text-xs text-[#006194] font-semibold mt-4">
          <span>Xem chi tiết công thức &amp; ví dụ</span>
          <span class="material-symbols-outlined text-[16px] group-hover:translate-x-1 transition-transform">arrow_forward</span>
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;

  // Gắn sự kiện click
  grid.querySelectorAll('[data-action="view-detail"]').forEach(card => {
    card.addEventListener('click', () => {
      openGrammarDetail(card.dataset.id);
    });
  });

  grid.querySelectorAll('[data-action="pronounce"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      pronounceJapanese(btn.dataset.text);
    });
  });
}

// =========================================================================
// 4. GIAO DIỆN CHI TIẾT MẪU NGỮ PHÁP (DETAIL VIEW)
// =========================================================================
function openGrammarDetail(ruleId) {
  const rule = grammarList.find(r => r.id === ruleId);
  if (!rule) return;

  document.getElementById('grammarListView').classList.add('hidden');
  const detailView = document.getElementById('grammarDetailView');
  detailView.classList.remove('hidden');

  // Breadcrumb
  document.getElementById('detailBreadcrumbLevel').textContent = `JLPT ${rule.level}`;
  document.getElementById('detailBreadcrumbPattern').textContent = rule.pattern;

  const container = document.getElementById('grammarDetailContainer');
  let examplesHtml = '';

  if (rule.examples && rule.examples.length > 0) {
    examplesHtml = rule.examples.map(ex => `
      <div class="p-4 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl space-y-1.5 hover:bg-white transition">
        <div class="flex items-center justify-between gap-2">
          <div class="font-japanese font-bold text-base md:text-lg text-[#0F2545]">
            ${ex.ja}
          </div>
          <button type="button" data-action="pronounce-ex" data-text="${ex.ja}"
                  class="w-7 h-7 rounded-lg border border-[#ECE8DE] bg-white hover:bg-[#FFE5EC] hover:text-[#831843] flex items-center justify-center text-xs transition cursor-pointer"
                  title="Nghe câu ví dụ">
            <span class="material-symbols-outlined text-[15px]">volume_up</span>
          </button>
        </div>
        <div class="text-xs text-[#68737D] font-mono">
          Furigana: ${ex.reading}
        </div>
        <div class="text-xs md:text-sm font-semibold text-[#059669] pt-1">
          Dịch nghĩa: ${ex.vi}
        </div>
      </div>
    `).join('');
  } else {
    examplesHtml = `
      <div class="p-4 bg-[#FAF8F2] border border-dashed border-[#ECE8DE] rounded-xl text-center text-xs text-[#68737D]">
        Đang cập nhật thêm các câu ví dụ đàm thoại thực tế cho mẫu câu này.
      </div>
    `;
  }

  container.innerHTML = `
    <!-- Header Thẻ Mẫu Câu -->
    <div class="bg-white border border-[#ECE8DE] rounded-2xl p-6 shadow-sm space-y-3">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div class="flex items-center gap-3">
          <span class="font-japanese text-3xl md:text-4xl font-bold text-[#0F2545]">
            ${rule.pattern}
          </span>
          <span class="text-sm text-[#68737D] font-mono italic">
            (${rule.romaji})
          </span>
        </div>
        <span class="px-3 py-1 rounded-full text-xs font-bold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">
          Trình độ ${rule.level}
        </span>
      </div>

      <div class="p-3.5 bg-emerald-50/60 border border-emerald-100 rounded-xl">
        <span class="text-xs font-bold text-emerald-800 uppercase tracking-wider block mb-0.5">Ý nghĩa ngữ pháp:</span>
        <p class="text-sm md:text-base font-bold text-[#0F2545] leading-relaxed">
          ${rule.meaning}
        </p>
      </div>
    </div>

    <!-- Công Thức Kết Hợp (Formation) -->
    <div class="bg-white border border-[#ECE8DE] rounded-2xl p-6 shadow-sm space-y-2">
      <h4 class="font-bold text-sm md:text-base text-[#0F2545] flex items-center gap-2">
        <span class="material-symbols-outlined text-[18px] text-[#006194]">account_tree</span>
        <span>Công thức kết hợp (Formation)</span>
      </h4>
      <div class="p-3.5 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl font-mono text-xs md:text-sm font-bold text-[#006194]">
        ${rule.formation}
      </div>
    </div>

    <!-- Giải Thích & Lưu Ý (Notes) -->
    <div class="bg-white border border-[#ECE8DE] rounded-2xl p-6 shadow-sm space-y-2">
      <h4 class="font-bold text-sm md:text-base text-[#0F2545] flex items-center gap-2">
        <span class="material-symbols-outlined text-[18px] text-amber-600">info</span>
        <span>Giải thích chi tiết &amp; Lưu ý khi sử dụng</span>
      </h4>
      <p class="text-xs md:text-sm text-[#3F4850] leading-relaxed p-3.5 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl">
        ${rule.notes}
      </p>
    </div>

    <!-- Các Câu Ví Dụ Đàm Thoại Thực Tế -->
    <div class="bg-white border border-[#ECE8DE] rounded-2xl p-6 shadow-sm space-y-3">
      <h4 class="font-bold text-sm md:text-base text-[#0F2545] flex items-center gap-2">
        <span class="material-symbols-outlined text-[18px] text-[#059669]">forum</span>
        <span>Câu ví dụ đàm thoại thực tế</span>
      </h4>
      <div class="space-y-2.5">
        ${examplesHtml}
      </div>
    </div>
  `;

  container.querySelectorAll('[data-action="pronounce-ex"]').forEach(btn => {
    btn.addEventListener('click', () => {
      pronounceJapanese(btn.dataset.text);
    });
  });

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function closeGrammarDetail() {
  document.getElementById('grammarDetailView').classList.add('hidden');
  document.getElementById('grammarListView').classList.remove('hidden');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// =========================================================================
// 5. PHÁT ÂM TIẾNG NHẬT (WEB SPEECH API SYNTHESIS)
// =========================================================================
function pronounceJapanese(text) {
  if (!text || typeof window === 'undefined' || !window.speechSynthesis) {
    showToast('Trình duyệt hiện chưa hỗ trợ đọc âm thanh tự động.', 'info');
    return;
  }

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'ja-JP';
  utterance.rate = 0.9;
  window.speechSynthesis.speak(utterance);
}

// =========================================================================
// 6. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadGrammarData();

  // Chuyển tab cấp độ N5 -> N1
  document.querySelectorAll('#jlptLevelTabs button').forEach(tab => {
    tab.addEventListener('click', () => {
      currentLevel = tab.dataset.level;

      document.querySelectorAll('#jlptLevelTabs button').forEach(t => {
        if (t.dataset.level === currentLevel) {
          t.className = 'px-4 py-2 rounded-xl text-xs font-bold bg-[#006194] text-white shadow-xs transition cursor-pointer';
        } else {
          t.className = 'px-4 py-2 rounded-xl text-xs font-semibold bg-[#FAF8F2] text-[#3F4850] hover:bg-[#F2EFE6] border border-[#ECE8DE] transition cursor-pointer';
        }
      });

      loadGrammarData();
    });
  });

  // Tìm kiếm mẫu câu (Debounce)
  const searchInput = document.getElementById('searchGrammarInput');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      currentSearchKeyword = e.target.value;
      renderGrammarCards();
    }, 200));
  }

  // Nút quay lại danh sách
  document.getElementById('btnBackToList')?.addEventListener('click', closeGrammarDetail);
});
