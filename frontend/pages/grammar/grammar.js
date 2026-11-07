/**
 * KizunaTutor - Cẩm Nang Ngữ Pháp OpenJLPT
 * Feature-Based architecture module, tuân thủ nghiêm ngặt FRONTEND_RULES.md
 * Hỗ trợ Bộ lọc kép: Cấp độ JLPT (N5 - N1) & Khối Ứng dụng Thực tế (Đời thường, Nhà hàng, Du lịch, Công việc, Sức khỏe, Học thuật)
 * TUYỆT ĐỐI KHÔNG DÙNG DỮ LIỆU MẪU (NO MOCK DATA INJECTION)
 */

import { showToast, debounce } from '/js/main.js';
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

// Khởi tạo khung giao diện học viên dùng chung
initLearnerShell({
  activeMenu: 'grammar',
  pageTitle: 'Sổ tay Ngữ pháp JLPT',
  badge: 'Cẩm nang JLPT N5 - N1'
});

// =========================================================================
// 1. CẤU HÌNH KHỐI ỨNG DỤNG THỰC TẾ & CẤP ĐỘ JLPT
// =========================================================================
export const DOMAIN_CONFIG = {
  daily: {
    id: 'daily',
    name: 'Đời thường',
    icon: '☕',
    badgeClass: 'bg-amber-100 text-amber-900 border-amber-200',
    description: 'Giao tiếp sinh hoạt hàng ngày, bạn bè, gia đình, tán gẫu & cảm xúc thân mật',
    tags: ['thân mật / văn nói', 'cảm xúc', 'rủ rê / lời mời', 'hoàn thành', 'hối tiếc', 'hồi tưởng', 'cảm thán', 'trợ từ', 'chủ đề', 'chia thể', 'sự biến đổi', 'trạng thái', 'đồng thời', 'bổ sung', 'nghi vấn', 'chỉ định từ'],
    keywords: ['bạn bè', 'gia đình', 'nói chuyện', 'hàng ngày', 'thường ngày', 'ở nhà', 'cuộc sống', 'thói quen', 'tán gẫu', 'nuối tiếc', 'lỡ', 'rủ', 'hẹn', 'mẹ', 'bố', 'con']
  },
  dining: {
    id: 'dining',
    name: 'Nhà hàng',
    icon: '🍜',
    badgeClass: 'bg-orange-100 text-orange-900 border-orange-200',
    description: 'Gọi món, hỏi giá cả, ăn uống, mua sắm & xin phép nhân viên phục vụ',
    tags: ['thử nghiệm', 'so sánh', 'số lượng', 'cho phép', 'nhờ vả / yêu cầu', 'mức độ', 'cho - nhận ơn huệ', 'ước muốn', 'mong muốn'],
    keywords: ['ăn', 'uống', 'món', 'nhà hàng', 'quán', 'thực đơn', 'gọi món', 'chọn', 'mua', 'giá', 'tiền', 'ngon', 'thử làm', 'cho phép', 'xin phép', 'bữa', 'cơm', 'hoa quả', 'khao', 'bánh']
  },
  travel: {
    id: 'travel',
    name: 'Du lịch',
    icon: '✈️',
    badgeClass: 'bg-sky-100 text-sky-900 border-sky-200',
    description: 'Đi lại, phương tiện tàu xe, hỏi đường, đặt vé & trải nghiệm du lịch',
    tags: ['phương tiện', 'thời gian', 'trình tự thời gian', 'kinh nghiệm', 'ý định', 'mục đích', 'phạm vi', 'giới hạn', 'đích đến'],
    keywords: ['du lịch', 'tàu', 'xe buýt', 'máy bay', 'sân bay', 'ga', 'khách sạn', 'hành lý', 'chuyến đi', 'đến', 'rời', 'xuất phát', 'đường', 'lộ trình', 'vé', 'tham quan', 'ngắm', 'đi lại', 'tokyo', 'kyoto', 'về nước']
  },
  work: {
    id: 'work',
    name: 'Công việc',
    icon: '💼',
    badgeClass: 'bg-blue-100 text-blue-900 border-blue-200',
    description: 'Môi trường công sở, email, báo cáo, trao đổi cấp trên, kính ngữ & đối tác',
    tags: ['trang trọng', 'lịch sự', 'tôn kính ngữ', 'khiêm nhường ngữ', 'lập trường / góc nhìn', 'bổn phận / nghĩa vụ', 'mối quan hệ', 'quyết định', 'cần thiết'],
    keywords: ['công ty', 'khách hàng', 'hội nghị', 'giám đốc', 'báo cáo', 'công việc', 'đồng nghiệp', 'hợp đồng', 'kính ngữ', 'lịch sự', 'văn phòng', 'kinh doanh', 'đối tác', 'trách nhiệm', 'nhiệm vụ', 'bắt buộc', 'thương mại']
  },
  health: {
    id: 'health',
    name: 'Sức khỏe',
    icon: '🏥',
    badgeClass: 'bg-rose-100 text-rose-900 border-rose-200',
    description: 'Khám bệnh, miêu tả triệu chứng cơ thể, chỉ định bác sĩ & an toàn sức khỏe',
    tags: ['lời khuyên', 'cần thiết', 'cấm đoán', 'khuynh hướng', 'khả năng', 'năng lực'],
    keywords: ['bệnh', 'thuốc', 'uống thuốc', 'nghỉ ngơi', 'mệt', 'sốt', 'đau', 'bác sĩ', 'bệnh viện', 'sức khỏe', 'nguy hiểm', 'triệu chứng', 'cơ thể', 'kiêng', 'khuyên', 'không nên', 'nên', 'tai nạn']
  },
  academic: {
    id: 'academic',
    name: 'Học thuật',
    icon: '📚',
    badgeClass: 'bg-purple-100 text-purple-900 border-purple-200',
    description: 'Đọc báo chí, nghiên cứu, viết luận điểm, phân tích logic & luyện thi JLPT N3-N1',
    tags: ['văn viết', 'nhấn mạnh', 'nguyên nhân / lý do', 'điều kiện', 'nhượng bộ', 'tương phản', 'phê phán / chỉ trích', 'suy đoán', 'trích dẫn', 'vẻ bề ngoài'],
    keywords: ['văn viết', 'báo chí', 'luận văn', 'nghiên cứu', 'chính sách', 'xã hội', 'kinh tế', 'thống kê', 'khảo sát', 'nghị luận', 'lập luận', 'phát biểu', 'chính thức', 'phân tích', 'chứng cứ']
  }
};

const JLPT_DESCRIPTIONS = {
  N5: 'Kiến thức sơ cấp N5 căn bản trong giao tiếp tiếng Nhật',
  N4: 'Ngữ pháp sơ trung cấp N4 kết hợp các dạng chia thể và đàm thoại thường ngày',
  N3: 'Ngữ pháp trung cấp N3 bắt đầu ứng dụng trong công việc và đọc hiểu tin tức',
  N2: 'Ngữ pháp trung cao cấp N2 dùng trong văn phòng, công sở và văn bản học thuật',
  N1: 'Ngữ pháp cao cấp N1 trong báo chí chính luận, nghị luận và văn học'
};

// =========================================================================
// 2. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
let currentFilterMode = 'jlpt'; // 'jlpt' hoặc 'domain'
let currentLevel = 'N5';
let currentDomain = 'daily';
let currentDomainLevel = 'ALL'; // 'ALL', 'N5', 'N4', 'N3', 'N2', 'N1'
let currentSearchKeyword = '';
let masterGrammarList = []; // Toàn bộ 526 mẫu ngữ pháp đồng bộ từ Database

/**
 * Phân tích và gán các khối ứng dụng phù hợp cho mẫu ngữ pháp
 */
function detectGrammarDomains(item) {
  const matched = [];
  const tagsStr = (item.tags || '').toLowerCase();
  const searchStr = `${item.pattern || ''} ${item.meaning || ''} ${item.notes || ''}`.toLowerCase();

  // 1. Ưu tiên số 1: So khớp trực tiếp thẻ tags chuẩn hóa từ PostgreSQL Database
  for (const [key, cfg] of Object.entries(DOMAIN_CONFIG)) {
    const tagMatch = cfg.tags.some(t => tagsStr.includes(t));
    if (tagMatch) {
      matched.push(key);
    }
  }

  // 2. Nếu chưa khớp khối nào từ tags, xét tiếp từ khóa ngữ nghĩa & lưu ý đàm thoại
  if (matched.length === 0) {
    for (const [key, cfg] of Object.entries(DOMAIN_CONFIG)) {
      const kwMatch = cfg.keywords.some(k => searchStr.includes(k));
      if (kwMatch) {
        matched.push(key);
      }
    }
  }

  // 3. Fallback mặc định theo cấp độ JLPT nếu không có thông tin tag
  if (matched.length === 0) {
    const lvl = item.jlptLevel || item.jlptlevel || 'N5';
    if (lvl === 'N5' || lvl === 'N4') {
      matched.push('daily');
    } else {
      matched.push('academic');
    }
  }

  return matched;
}

// =========================================================================
// 3. TẢI DỮ LIỆU NGỮ PHÁP TỪ REST API BACKEND
// =========================================================================
async function loadGrammarData() {
  try {
    let res = await fetch('/api/learner/grammar?level=ALL&size=1000');
    if (!res.ok) {
      // Fallback nạp trực tiếp từ file dữ liệu đồng bộ database
      res = await fetch('/data/grammars.json');
    }

    if (res.ok) {
      const data = await res.json();
      let content = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);

      if (content.length > 0) {
        masterGrammarList = content.map(item => {
          const exSentence = item.exampleSentence || item.examplesentence || '';
          const exFurigana = item.exampleFurigana || item.examplefurigana || '';
          const exMeaning = item.exampleMeaning || item.examplemeaning || '';
          const extExamples = item.extendedExamples || item.extendedexamples || '';

          const examples = [];
          if (exSentence) {
            examples.push({
              ja: exSentence,
              reading: exFurigana || exSentence,
              vi: exMeaning || 'Ví dụ minh họa'
            });
          }

          if (extExamples) {
            const lines = extExamples.split('\n').map(l => l.trim()).filter(Boolean);
            for (let i = 0; i < lines.length; i += 2) {
              const jaLine = lines[i]?.replace(/^\d+\.\s*/, '') || '';
              const viLine = lines[i + 1]?.replace(/^\(|\)$/g, '') || '';
              if (jaLine) {
                examples.push({
                  ja: jaLine,
                  reading: jaLine,
                  vi: viLine || 'Ví dụ mở rộng'
                });
              }
            }
          }

          const domains = detectGrammarDomains(item);

          return {
            id: String(item.grammarCode || item.grammarcode || item.id || `gm-${item.pattern}`),
            level: item.jlptLevel || item.jlptlevel || item.level || 'N5',
            pattern: item.pattern || '',
            romaji: item.romaji || '',
            meaning: item.meaning || item.meaningVn || '',
            formation: item.formation || 'Đang cập nhật công thức',
            notes: item.notes || 'Chưa có ghi chú đặc biệt',
            tags: item.tags || '',
            domains: domains,
            examples: examples.length > 0 ? examples : (Array.isArray(item.examples) ? item.examples : [])
          };
        });

        renderGrammarCards();

        // Tự động mở chi tiết nếu URL có param detail hoặc id
        const urlParams = new URLSearchParams(window.location.search);
        const detailId = urlParams.get('detail') || urlParams.get('id');
        if (detailId) {
          openGrammarDetail(detailId, false);
        }
        return;
      }
    }
  } catch (e) {
    console.debug('Chưa kết nối được API Backend hoặc chưa có dữ liệu:', e);
  }

  // Khi chưa có dữ liệu từ backend, trả về danh sách rỗng để hiển thị Empty State chuẩn
  masterGrammarList = [];
  renderGrammarCards();
}

/**
 * Cập nhật số lượng đếm mẫu câu tương ứng trên các nút lọc cấp độ của Khối Ứng dụng
 */
function updateDomainSubLevelCounts() {
  const domainList = masterGrammarList.filter(r => r.domains.includes(currentDomain));
  document.querySelectorAll('#domainJlptTabs button').forEach(btn => {
    const lvl = btn.dataset.domainLevel;
    let count = 0;
    if (lvl === 'ALL') {
      count = domainList.length;
      btn.innerHTML = `<span>Tất cả</span> <span class="opacity-80 text-[10px]">(${count})</span>`;
    } else {
      count = domainList.filter(r => r.level === lvl).length;
      btn.innerHTML = `<span>${lvl}</span> <span class="opacity-80 text-[10px]">(${count})</span>`;
    }

    if (lvl === currentDomainLevel) {
      btn.className = 'px-2.5 py-1 rounded-lg text-xs font-bold bg-[#006194] text-white shadow-xs transition cursor-pointer flex items-center gap-1';
    } else {
      btn.className = 'px-2.5 py-1 rounded-lg text-xs font-semibold bg-[#FAF8F2] text-[#3F4850] hover:bg-[#F2EFE6] border border-[#ECE8DE] transition cursor-pointer flex items-center gap-1';
    }
  });
}

// =========================================================================
// 4. RENDER DANH SÁCH THẺ NGỮ PHÁP (GRID VIEW)
// =========================================================================
function renderGrammarCards() {
  const grid = document.getElementById('grammarGrid');
  const emptyState = document.getElementById('grammarEmptyState');
  const activeLabelEl = document.getElementById('filterActiveLabel');
  const activeCountEl = document.getElementById('filterActiveCount');
  const descEl = document.getElementById('filterDescription');

  if (!grid || !emptyState) return;

  // 1. Lọc theo Chế độ (Cấp độ JLPT hoặc Khối Ứng dụng kết hợp Cấp độ)
  let activeList = [];
  if (currentFilterMode === 'jlpt') {
    activeList = masterGrammarList.filter(r => r.level === currentLevel);
    if (activeLabelEl) activeLabelEl.textContent = `JLPT ${currentLevel}`;
    if (descEl) descEl.textContent = JLPT_DESCRIPTIONS[currentLevel] || '';
  } else {
    // 1a. Lọc theo Khối Ứng dụng
    const domainList = masterGrammarList.filter(r => r.domains.includes(currentDomain));
    
    // 1b. Lọc tiếp theo Cấp độ JLPT của khối (nếu không phải 'ALL')
    if (currentDomainLevel !== 'ALL') {
      activeList = domainList.filter(r => r.level === currentDomainLevel);
    } else {
      activeList = domainList;
    }

    const domCfg = DOMAIN_CONFIG[currentDomain] || DOMAIN_CONFIG.daily;
    const lvlText = currentDomainLevel === 'ALL' ? 'Tất cả cấp độ' : `JLPT ${currentDomainLevel}`;
    if (activeLabelEl) activeLabelEl.textContent = `${domCfg.icon} Khối: ${domCfg.name} • ${lvlText}`;
    if (descEl) descEl.textContent = domCfg.description || '';

    updateDomainSubLevelCounts();
  }

  // 2. Lọc tiếp theo Từ khóa tìm kiếm (nếu có)
  const q = currentSearchKeyword.trim().toLowerCase();
  const filtered = activeList.filter(g => 
    g.pattern.toLowerCase().includes(q) ||
    g.romaji.toLowerCase().includes(q) ||
    g.meaning.toLowerCase().includes(q) ||
    (g.formation && g.formation.toLowerCase().includes(q))
  );

  // 3. Cập nhật số lượng mẫu câu hiển thị
  if (activeCountEl) activeCountEl.textContent = String(filtered.length);

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
    // Badges cho khối ứng dụng
    const domainBadgesHtml = rule.domains.slice(0, 2).map(dId => {
      const d = DOMAIN_CONFIG[dId];
      if (!d) return '';
      return `<span class="px-2 py-0.5 rounded-md text-[11px] font-semibold border ${d.badgeClass}">${d.icon} ${d.name}</span>`;
    }).join('');

    html += `
      <div class="bg-white border border-[#ECE8DE] rounded-xl p-3.5 hover:border-[#CBD5E1] transition flex flex-col justify-between group cursor-pointer"
           data-action="view-detail" data-id="${rule.id}">
        
        <div class="space-y-3">
          <!-- Header Thẻ -->
          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-1.5 flex-wrap">
              <span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8]">
                ${rule.level}
              </span>
              ${domainBadgesHtml}
            </div>
          </div>

          <!-- Mẫu câu & Romaji -->
          <div>
            <h3 class="font-japanese text-xl font-bold text-[#0F2545] group-hover:text-[#006194] transition">
              ${rule.pattern}
            </h3>
            <p class="text-xs text-[#68737D] font-mono mt-0.5">${rule.romaji}</p>
          </div>

          <!-- Ý nghĩa ngắn -->
          <p class="text-xs font-semibold text-[#3F4850] line-clamp-2 leading-relaxed bg-[#F8FAFC] p-2 rounded-lg border border-[#E2E8F0]">
            ${rule.meaning}
          </p>
        </div>

        <!-- Footer Thẻ -->
        <div class="pt-4 border-t border-[#E2E8F0] flex items-center justify-between text-xs text-[#831843] font-semibold mt-4">
          <span>Xem chi tiết công thức &amp; ví dụ</span>
          <span class="material-symbols-outlined text-[16px] group-hover:translate-x-1 transition-transform">arrow_forward</span>
        </div>
      </div>
    `;
  });

  grid.innerHTML = html;

  // Gắn sự kiện click
  grid.querySelectorAll('[data-action="view-detail"]').forEach(card => {
    card.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      openGrammarDetail(card.dataset.id);
    });
  });
}

function speakJapanese(text) {
  if (!text || typeof window === 'undefined') return;
  try {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const cleanText = text.replace(/[（）()]/g, '').trim();
      const utterance = new SpeechSynthesisUtterance(cleanText);
      utterance.lang = 'ja-JP';
      utterance.rate = 0.9;
      window.speechSynthesis.speak(utterance);
    } else {
      showToast('Trình duyệt không hỗ trợ tổng hợp giọng nói Web Speech.', 'info');
    }
  } catch (e) {
    console.debug('Lỗi phát âm tiếng Nhật:', e);
  }
}

// =========================================================================
// 5. GIAO DIỆN CHI TIẾT MẪU NGỮ PHÁP (DETAIL VIEW)
// =========================================================================
let lastScrollTop = 0;

function openGrammarDetail(ruleId, pushHistory = true) {
  const rule = masterGrammarList.find(r => String(r.id) === String(ruleId));
  if (!rule) return;

  const mainEl = document.querySelector('main');
  if (mainEl) {
    lastScrollTop = mainEl.scrollTop;
  } else {
    lastScrollTop = window.scrollY || 0;
  }

  const listView = document.getElementById('grammarListView');
  const detailView = document.getElementById('grammarDetailView');
  if (listView) listView.classList.add('hidden');
  if (detailView) detailView.classList.remove('hidden');

  if (pushHistory) {
    const u = new URL(window.location);
    u.searchParams.set('detail', rule.id);
    window.history.pushState({ grammarDetailId: rule.id }, '', u);
  }

  // Breadcrumb
  const bcLevel = document.getElementById('detailBreadcrumbLevel');
  const bcPattern = document.getElementById('detailBreadcrumbPattern');
  if (bcLevel) bcLevel.textContent = `JLPT ${rule.level}`;
  if (bcPattern) bcPattern.textContent = rule.pattern;

  const container = document.getElementById('grammarDetailContainer');
  let examplesHtml = '';

  if (rule.examples && rule.examples.length > 0) {
    examplesHtml = rule.examples.map(ex => `
      <div class="p-4 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl space-y-1.5 hover:bg-white transition break-words overflow-hidden">
        <div class="flex items-center justify-between gap-2">
          <div class="font-japanese font-bold text-base md:text-lg text-[#0F2545] break-words whitespace-normal min-w-0 flex-1">
            ${ex.ja}
          </div>
          <button type="button" 
                  data-action="speak-sentence" 
                  data-text="${escapeHtml(ex.ja)}"
                  class="p-1.5 rounded-lg border border-[#ECE8DE] bg-white hover:bg-slate-100 text-[#006194] text-xs font-semibold flex items-center gap-1 transition cursor-pointer shrink-0 shadow-2xs active:scale-95"
                  title="Nghe phát âm chuẩn">
            <span>🔊</span>
          </button>
        </div>
        <div class="text-xs text-[#68737D] font-mono break-words">
          Furigana: ${ex.reading}
        </div>
        <div class="text-xs md:text-sm font-semibold text-[#059669] pt-1 break-words">
          Dịch nghĩa: ${ex.vi}
        </div>
      </div>
    `).join('');
  } else {
    examplesHtml = `
      <div class="p-4 bg-[#FAF8F2] border border-dashed border-[#ECE8DE] rounded-xl text-center text-xs text-[#68737D] break-words overflow-hidden">
        Đang cập nhật thêm các câu ví dụ đàm thoại thực tế cho mẫu câu này.
      </div>
    `;
  }

  // Badges khối ứng dụng trong modal chi tiết
  const domainBadgesHtml = rule.domains.map(dId => {
    const d = DOMAIN_CONFIG[dId];
    if (!d) return '';
    return `<span class="px-2.5 py-1 rounded-lg text-xs font-semibold border ${d.badgeClass} shrink-0">${d.icon} Khối: ${d.name}</span>`;
  }).join('');

  container.innerHTML = `
    <!-- Header Thẻ Mẫu Câu -->
    <div class="bg-white border border-[#ECE8DE] rounded-2xl p-6 shadow-sm space-y-3 break-words overflow-hidden">
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div class="flex items-center gap-2.5 min-w-0 flex-wrap">
          <span class="font-japanese text-3xl md:text-4xl font-bold text-[#0F2545] break-words">
            ${rule.pattern}
          </span>
          <button type="button" 
                  data-action="speak-sentence" 
                  data-text="${escapeHtml(rule.pattern)}"
                  class="p-1.5 px-2 rounded-lg border border-[#ECE8DE] bg-[#FAF8F2] hover:bg-white text-[#006194] text-xs font-semibold flex items-center gap-1 transition cursor-pointer shrink-0 shadow-2xs active:scale-95"
                  title="Nghe phát âm mẫu câu">
            <span>🔊</span>
          </button>
          <span class="text-sm text-[#68737D] font-mono italic break-words">
            (${rule.romaji})
          </span>
        </div>
        <div class="flex items-center gap-2 flex-wrap">
          <span class="px-3 py-1 rounded-full text-xs font-bold bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8] shrink-0">
            Trình độ ${rule.level}
          </span>
          ${domainBadgesHtml}
        </div>
      </div>

      <div class="p-3.5 bg-emerald-50/60 border border-emerald-100 rounded-xl break-words overflow-hidden">
        <span class="text-xs font-bold text-emerald-800 uppercase tracking-wider block mb-0.5">Ý nghĩa ngữ pháp:</span>
        <p class="text-sm md:text-base font-bold text-[#0F2545] leading-relaxed break-words">
          ${rule.meaning}
        </p>
      </div>
    </div>

    <!-- Công Thức Kết Hợp (Formation) -->
    <div class="bg-white border border-[#ECE8DE] rounded-2xl p-6 shadow-sm space-y-2 break-words overflow-hidden">
      <h4 class="font-bold text-sm md:text-base text-[#0F2545] flex items-center gap-2">
        <span class="material-symbols-outlined text-[18px] text-[#006194]">account_tree</span>
        <span>Công thức kết hợp (Formation)</span>
      </h4>
      <div class="p-3.5 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl font-mono text-xs md:text-sm font-bold text-[#006194] break-words overflow-hidden whitespace-normal">
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

  // Khối Đặt Câu Hỏi Kiểm Tra Với Sensei AI
  const practiceBox = document.createElement('div');
  practiceBox.className = 'bg-gradient-to-r from-[#EFF6FF] to-[#FAF8F2] border-2 border-[#006194] rounded-2xl p-5 shadow-sm space-y-3 mt-4';
  practiceBox.innerHTML = `
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
      <div class="space-y-1">
        <div class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md bg-[#006194] text-white text-[11px] font-bold">
          <span class="material-symbols-outlined text-[14px]">psychology</span>
          <span>Luyện tập &amp; Kiểm tra ngữ pháp với AI</span>
        </div>
        <h4 class="text-base font-bold text-[#0F2545]">
          Tập đặt câu &amp; Kiểm tra với mẫu "${rule.pattern}"
        </h4>
        <p class="text-xs text-[#475569] leading-relaxed max-w-xl">
          Tự viết câu tiếng Nhật của bạn với mẫu ngữ pháp này. AI Sensei sẽ chấm điểm, bắt mạch cấu trúc câu và phân tích chi tiết bằng tiếng Việt.
        </p>
      </div>

      <button type="button" id="btnGoToAiWritingPractice"
              class="px-4 py-2.5 bg-[#006194] hover:bg-[#004e77] text-white text-xs font-bold rounded-xl transition flex items-center justify-center gap-2 cursor-pointer shrink-0 shadow-sm">
        <span class="material-symbols-outlined text-[18px]">edit_note</span>
        <span>Đặt câu hỏi kiểm tra</span>
      </button>
    </div>
  `;
  container.appendChild(practiceBox);

  document.getElementById('btnGoToAiWritingPractice')?.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      sessionStorage.setItem('kizuna_selected_grammar_for_writing', JSON.stringify({
        id: rule.id,
        pattern: rule.pattern,
        level: rule.level,
        romaji: rule.romaji,
        meaning: rule.meaning,
        formation: rule.formation,
        notes: rule.notes,
        examples: rule.examples
      }));
    } catch (e) {
      console.debug('SessionStorage save error:', e);
    }
    window.location.href = `/ai-writing?pattern=${encodeURIComponent(rule.pattern)}&level=${encodeURIComponent(rule.level)}`;
  });

  if (mainEl) {
    mainEl.scrollTop = 0;
  }
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function closeGrammarDetail(fromPopState = false) {
  const detailView = document.getElementById('grammarDetailView');
  const listView = document.getElementById('grammarListView');
  if (!detailView || !listView) return;

  detailView.classList.add('hidden');
  listView.classList.remove('hidden');

  if (!fromPopState) {
    const u = new URL(window.location);
    const hadDetailInUrl = u.searchParams.has('detail') || u.searchParams.has('id');
    u.searchParams.delete('detail');
    u.searchParams.delete('id');

    if (hadDetailInUrl && window.history.state?.grammarDetailId) {
      window.history.back();
      return;
    } else {
      window.history.replaceState({}, '', u);
    }
  }

  const mainEl = document.querySelector('main');
  if (mainEl && lastScrollTop > 0) {
    mainEl.scrollTop = lastScrollTop;
  } else if (lastScrollTop > 0) {
    window.scrollTo({ top: lastScrollTop, behavior: 'smooth' });
  }
}

function handleGrammarPopState(e) {
  const detailView = document.getElementById('grammarDetailView');
  if (!detailView) return; // Không còn ở trang grammar

  const urlParams = new URLSearchParams(window.location.search);
  const detailId = urlParams.get('detail') || urlParams.get('id') || e.state?.grammarDetailId;

  if (detailId) {
    if (masterGrammarList.length > 0) {
      openGrammarDetail(detailId, false);
    }
  } else {
    closeGrammarDetail(true);
  }
}

if (!window.__grammarPopstateBound) {
  window.__grammarPopstateBound = true;
  window.addEventListener('popstate', handleGrammarPopState);
}
// =========================================================================
// 6. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
// =========================================================================
document.addEventListener('DOMContentLoaded', () => {
  const modeSwitcherBtns = document.querySelectorAll('#filterModeSwitcher button');
  const jlptTabsContainer = document.getElementById('jlptLevelTabs');
  const domainTabsContainer = document.getElementById('domainTabs');
  const domainJlptSubFilter = document.getElementById('domainJlptSubFilter');

  // Đọc Query Parameters từ URL (nếu có)
  const urlParams = new URLSearchParams(window.location.search);
  const qMode = urlParams.get('mode');
  const qLevel = urlParams.get('level');
  const qDomain = urlParams.get('domain');

  if (qMode === 'domain') {
    currentFilterMode = 'domain';
    if (qDomain && DOMAIN_CONFIG[qDomain]) {
      currentDomain = qDomain;
    }
    if (qLevel && ['N5', 'N4', 'N3', 'N2', 'N1'].includes(qLevel.toUpperCase())) {
      currentDomainLevel = qLevel.toUpperCase();
    }
  } else if (qLevel && ['N5', 'N4', 'N3', 'N2', 'N1'].includes(qLevel.toUpperCase())) {
    currentLevel = qLevel.toUpperCase();
  }

  // Cập nhật giao diện Segmented Control ban đầu
  modeSwitcherBtns.forEach(b => {
    if (b.dataset.mode === currentFilterMode) {
      b.className = 'px-4 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 cursor-pointer bg-[#006194] text-white shadow-xs';
    } else {
      b.className = 'px-4 py-1.5 rounded-lg text-xs font-semibold text-[#68737D] hover:text-[#0F2545] transition flex items-center gap-1.5 cursor-pointer';
    }
  });

  if (currentFilterMode === 'jlpt') {
    jlptTabsContainer?.classList.remove('hidden');
    domainTabsContainer?.classList.add('hidden');
    domainJlptSubFilter?.classList.add('hidden');
  } else {
    jlptTabsContainer?.classList.add('hidden');
    domainTabsContainer?.classList.remove('hidden');
    domainJlptSubFilter?.classList.remove('hidden');
  }

  // Cập nhật trạng thái active cho tab JLPT
  document.querySelectorAll('#jlptLevelTabs button').forEach(t => {
    if (t.dataset.level === currentLevel) {
      t.className = 'px-4 py-2 rounded-xl text-xs font-bold bg-[#006194] text-white shadow-xs transition cursor-pointer';
    } else {
      t.className = 'px-4 py-2 rounded-xl text-xs font-semibold bg-[#FAF8F2] text-[#3F4850] hover:bg-[#F2EFE6] border border-[#ECE8DE] transition cursor-pointer';
    }
  });

  // Cập nhật trạng thái active cho tab Domain
  document.querySelectorAll('#domainTabs button').forEach(t => {
    if (t.dataset.domain === currentDomain) {
      t.className = 'px-3.5 py-2 rounded-xl text-xs font-bold bg-[#006194] text-white shadow-xs transition cursor-pointer flex items-center gap-1.5 shrink-0';
    } else {
      t.className = 'px-3.5 py-2 rounded-xl text-xs font-semibold bg-[#FAF8F2] text-[#3F4850] hover:bg-[#F2EFE6] border border-[#ECE8DE] transition cursor-pointer flex items-center gap-1.5 shrink-0';
    }
  });

  loadGrammarData();

  function updateUrlParams() {
    const u = new URL(window.location);
    if (currentFilterMode === 'domain') {
      u.searchParams.set('mode', 'domain');
      u.searchParams.set('domain', currentDomain);
      if (currentDomainLevel !== 'ALL') {
        u.searchParams.set('level', currentDomainLevel);
      } else {
        u.searchParams.delete('level');
      }
    } else {
      u.searchParams.delete('mode');
      u.searchParams.delete('domain');
      u.searchParams.set('level', currentLevel);
    }
    window.history.replaceState({}, '', u);
  }

  // 1. Chuyển đổi giữa 2 Chế độ Lọc: JLPT Level <-> Khối Ứng dụng
  modeSwitcherBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      const selectedMode = btn.dataset.mode;
      if (selectedMode === currentFilterMode) return;

      currentFilterMode = selectedMode;

      // Cập nhật giao diện Segmented Control
      modeSwitcherBtns.forEach(b => {
        if (b.dataset.mode === currentFilterMode) {
          b.className = 'px-4 py-1.5 rounded-lg text-xs font-bold transition flex items-center gap-1.5 cursor-pointer bg-[#006194] text-white shadow-xs';
        } else {
          b.className = 'px-4 py-1.5 rounded-lg text-xs font-semibold text-[#68737D] hover:text-[#0F2545] transition flex items-center gap-1.5 cursor-pointer';
        }
      });

      // Hiển thị dải tab tương ứng
      if (currentFilterMode === 'jlpt') {
        jlptTabsContainer?.classList.remove('hidden');
        domainTabsContainer?.classList.add('hidden');
        domainJlptSubFilter?.classList.add('hidden');
      } else {
        jlptTabsContainer?.classList.add('hidden');
        domainTabsContainer?.classList.remove('hidden');
        domainJlptSubFilter?.classList.remove('hidden');
        updateDomainSubLevelCounts();
      }

      updateUrlParams();
      renderGrammarCards();
    });
  });

  // 2. Chuyển tab cấp độ N5 -> N1
  document.querySelectorAll('#jlptLevelTabs button').forEach(tab => {
    tab.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      currentLevel = tab.dataset.level;

      document.querySelectorAll('#jlptLevelTabs button').forEach(t => {
        if (t.dataset.level === currentLevel) {
          t.className = 'px-4 py-2 rounded-xl text-xs font-bold bg-[#006194] text-white shadow-xs transition cursor-pointer';
        } else {
          t.className = 'px-4 py-2 rounded-xl text-xs font-semibold bg-[#FAF8F2] text-[#3F4850] hover:bg-[#F2EFE6] border border-[#ECE8DE] transition cursor-pointer';
        }
      });

      updateUrlParams();
      renderGrammarCards();
    });
  });

  // 3. Chuyển tab Khối Ứng dụng (Đời thường, Nhà hàng, Du lịch, Công việc, Sức khỏe, Học thuật)
  document.querySelectorAll('#domainTabs button').forEach(tab => {
    tab.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      currentDomain = tab.dataset.domain;

      document.querySelectorAll('#domainTabs button').forEach(t => {
        if (t.dataset.domain === currentDomain) {
          t.className = 'px-3.5 py-2 rounded-xl text-xs font-bold bg-[#006194] text-white shadow-xs transition cursor-pointer flex items-center gap-1.5 shrink-0';
        } else {
          t.className = 'px-3.5 py-2 rounded-xl text-xs font-semibold bg-[#FAF8F2] text-[#3F4850] hover:bg-[#F2EFE6] border border-[#ECE8DE] transition cursor-pointer flex items-center gap-1.5 shrink-0';
        }
      });

      updateUrlParams();
      renderGrammarCards();
    });
  });

  // 3b. Lọc cấp độ JLPT cho Khối Ứng Dụng đã chọn (Tất cả, N5, N4, N3, N2, N1)
  document.querySelectorAll('#domainJlptTabs button').forEach(tab => {
    tab.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      currentDomainLevel = tab.dataset.domainLevel;

      document.querySelectorAll('#domainJlptTabs button').forEach(t => {
        if (t.dataset.domainLevel === currentDomainLevel) {
          t.className = 'px-2.5 py-1 rounded-lg text-xs font-bold bg-[#006194] text-white shadow-xs transition cursor-pointer flex items-center gap-1';
        } else {
          t.className = 'px-2.5 py-1 rounded-lg text-xs font-semibold bg-[#FAF8F2] text-[#3F4850] hover:bg-[#F2EFE6] border border-[#ECE8DE] transition cursor-pointer flex items-center gap-1';
        }
      });

      updateUrlParams();
      renderGrammarCards();
    });
  });

  // 4. Tìm kiếm mẫu câu (Debounce)
  const searchInput = document.getElementById('searchGrammarInput');
  if (searchInput) {
    searchInput.addEventListener('input', debounce((e) => {
      currentSearchKeyword = e.target.value;
      renderGrammarCards();
    }, 200));
  }

  // 5. Nút quay lại danh sách
  document.getElementById('btnBackToList')?.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    closeGrammarDetail(false);
  });
});
