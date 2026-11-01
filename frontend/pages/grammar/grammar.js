/**
 * KizunaTutor - Cẩm Nang Ngữ Pháp OpenJLPT
 * Feature-Based architecture module, tuân thủ FRONTEND_RULES.md
 */

import { showToast, debounce } from '/js/main.js';

// =========================================================================
// 1. DỮ LIỆU MẪU CÂU NGỮ PHÁP TIÊU BIỂU OPENJLPT (N5 - N1)
// =========================================================================
const OPENJLPT_BUILTIN_GRAMMAR = [
  // --- CẤP ĐỘ N5 ---
  {
    id: 'n5-wa',
    level: 'N5',
    pattern: '〜は',
    romaji: 'wa',
    meaning: 'Thì, là; đóng vai trò làm chủ đề chính của câu',
    formation: 'Danh từ + は',
    notes: 'Viết là chữ は nhưng đọc là "wa". Dùng để đưa ra chủ đề đàm thoại của câu; thông tin quan trọng mới sẽ nằm ở phía sau.',
    examples: [
      { ja: '私は学生です。', reading: 'わたしはがくせいです。', vi: 'Tôi là một sinh viên.' },
      { ja: 'ここは教室です。', reading: 'ここはきょうしつです。', vi: 'Đây là phòng học.' },
      { ja: '田中さんは親切です。', reading: 'たなかさんはしんせつです。', vi: 'Anh Tanaka rất tốt bụng.' }
    ]
  },
  {
    id: 'n5-desu',
    level: 'N5',
    pattern: '〜です',
    romaji: 'desu',
    meaning: 'Là; trợ động từ lịch sự kết thúc câu',
    formation: 'Danh từ / Tính từ đuôi な + です',
    notes: 'Được sử dụng ở cuối câu để biểu thị thái độ lịch sự, trang trọng đối với người nghe.',
    examples: [
      { ja: 'これは日本語の本です。', reading: 'これはにほんごのほんです。', vi: 'Đây là cuốn sách tiếng Nhật.' },
      { ja: '富士山はきれいです。', reading: 'ふじさんはきれいです。', vi: 'Núi Phú Sĩ rất đẹp.' }
    ]
  },
  {
    id: 'n5-ga',
    level: 'N5',
    pattern: '〜が',
    romaji: 'ga',
    meaning: 'Chỉ chủ ngữ thực hiện hành động; nhấn mạnh thông tin mới',
    formation: 'Danh từ + が',
    notes: 'Từ để hỏi như だれ, なに khi làm chủ ngữ bắt buộc phải đi với が, tuyệt đối không dùng は. Cũng đi với すき, きらい, じょうず, わかります.',
    examples: [
      { ja: '雨が降っています。', reading: 'あめがふっています。', vi: 'Trời đang mưa.' },
      { ja: '私は日本語が好きです。', reading: 'わたしはにほんごがすきです。', vi: 'Tôi rất thích tiếng Nhật.' }
    ]
  },
  {
    id: 'n5-o',
    level: 'N5',
    pattern: '〜を',
    romaji: 'o',
    meaning: 'Chỉ tân ngữ trực tiếp chịu tác động của hành động',
    formation: 'Danh từ + を + Động từ tha động từ',
    notes: 'Viết là chữ を nhưng phát âm là "o". Đứng trước các tha động từ chỉ hành động có chủ ý.',
    examples: [
      { ja: 'ご飯を食べます。', reading: 'ごはんをたべます。', vi: 'Tôi ăn cơm.' },
      { ja: 'お茶を飲みます。', reading: 'おちゃをのみます。', vi: 'Tôi uống trà xanh.' }
    ]
  },
  {
    id: 'n5-ni-time',
    level: 'N5',
    pattern: '〜に (thời gian / địa điểm)',
    romaji: 'ni',
    meaning: 'Vào lúc; tại; đến nơi nào đó',
    formation: 'Thời gian cụ thể / Địa điểm đích đến + に',
    notes: 'Chỉ đi với mốc thời gian có con số cụ thể (6 giờ, ngày mai thì không thêm に).',
    examples: [
      { ja: '毎朝七時に起きます。', reading: 'まいあさしちじにおきます。', vi: 'Mỗi sáng tôi thức dậy lúc 7 giờ.' },
      { ja: '日本に行きます。', reading: 'にほんにいきます。', vi: 'Tôi sẽ đi Nhật Bản.' }
    ]
  },
  {
    id: 'n5-de',
    level: 'N5',
    pattern: '〜で (phương tiện / nơi chốn)',
    romaji: 'de',
    meaning: 'Bằng (phương tiện/công cụ); tại (nơi diễn ra hành động)',
    formation: 'Phương tiện / Địa điểm + で',
    notes: 'Phân biệt: で chỉ nơi diễn ra hành động, còn に chỉ nơi tồn tại (có người/vật).',
    examples: [
      { ja: '電車で会社へ行きます。', reading: 'でんしゃでかいしゃへいきます。', vi: 'Tôi đi đến công ty bằng tàu điện.' },
      { ja: '図書館で本を読みます。', reading: 'としょかんでほんをよみます。', vi: 'Tôi đọc sách tại thư viện.' }
    ]
  },
  {
    id: 'n5-te-kudasai',
    level: 'N5',
    pattern: '〜てください',
    romaji: 'te kudasai',
    meaning: 'Xin hãy...; làm ơn hãy làm việc gì đó',
    formation: 'Động từ thể て + ください',
    notes: 'Mẫu câu nhờ vả, yêu cầu lịch sự trong giao tiếp thường nhật.',
    examples: [
      { ja: 'ここに名前を書いてください。', reading: 'ここになまえをかいてください。', vi: 'Làm ơn hãy viết tên của bạn vào đây.' },
      { ja: 'ゆっくり話してください。', reading: 'ゆっくりはなしてください。', vi: 'Xin hãy nói chậm lại một chút.' }
    ]
  },
  {
    id: 'n5-kara-made',
    level: 'N5',
    pattern: '〜から 〜まで',
    romaji: 'kara ... made',
    meaning: 'Từ... đến...',
    formation: 'Thời gian / Địa điểm + から + Thời gian / Địa điểm + まで',
    notes: 'Có thể dùng riêng lẻ 〜から hoặc 〜まで tùy ngữ cảnh.',
    examples: [
      { ja: '九時から五時まで働きます。', reading: 'くじからごじまではたらきます。', vi: 'Tôi làm việc từ 9 giờ đến 5 giờ.' },
      { ja: 'ハノイから東京まで飛行機で行きます。', reading: 'ハノイからとうきょうまでひこうきでいきます。', vi: 'Đi từ Hà Nội đến Tokyo bằng máy bay.' }
    ]
  },

  // --- CẤP ĐỘ N4 ---
  {
    id: 'n4-te-mo-ii',
    level: 'N4',
    pattern: '〜てもいいです',
    romaji: 'te mo ii desu',
    meaning: 'Được phép làm gì...; làm việc gì cũng được',
    formation: 'Động từ thể て + もいいです',
    notes: 'Dùng để cho phép hoặc xin phép ai đó làm một hành động.',
    examples: [
      { ja: 'ここで写真を撮ってもいいですか。', reading: 'ここでしゃしんをとってもいいですか。', vi: 'Tôi có thể chụp ảnh ở đây được không?' },
      { ja: 'どうぞ、座ってもいいですよ。', reading: 'どうぞ、すわってもいいですよ。', vi: 'Xin mời, bạn có thể ngồi xuống.' }
    ]
  },
  {
    id: 'n4-te-wa-ikenai',
    level: 'N4',
    pattern: '〜てはいけません',
    romaji: 'te wa ikemasen',
    meaning: 'Không được phép làm gì; cấm đoán',
    formation: 'Động từ thể て + はいけません',
    notes: 'Dùng để diễn tả sự cấm đoán, quy tắc bắt buộc không được vi phạm.',
    examples: [
      { ja: 'ここでタバコを吸ってはいけません。', reading: 'ここでタバコをすってはいけません。', vi: 'Không được hút thuốc ở đây.' },
      { ja: 'テスト中に話してはいけません。', reading: 'テストちゅうにはなしてはいけません。', vi: 'Không được nói chuyện trong giờ thi.' }
    ]
  },
  {
    id: 'n4-nakereba-naranai',
    level: 'N4',
    pattern: '〜なければなりません',
    romaji: 'nakereba narimasen',
    meaning: 'Phải làm gì...; không thể không làm',
    formation: 'Động từ thể ない (bỏ い) + ければなりません',
    notes: 'Biểu thị nghĩa vụ hoặc sự cần thiết khách quan phải làm việc gì.',
    examples: [
      { ja: '毎日薬を飲まなければなりません。', reading: 'まいにちくすりをのまなければなりません。', vi: 'Hàng ngày tôi phải uống thuốc.' },
      { ja: '明日レポートを出さなければなりません。', reading: 'あしたレポートをださなければなりません。', vi: 'Ngày mai tôi phải nộp báo cáo.' }
    ]
  },
  {
    id: 'n4-to-omou',
    level: 'N4',
    pattern: '〜と思います',
    romaji: 'to omoimasu',
    meaning: 'Tôi nghĩ rằng...; bày tỏ quan điểm cá nhân',
    formation: 'Thể thông thường (Futsuukei) + と思います',
    notes: 'Dùng để nêu ý kiến phán đoán cá nhân hoặc dự định tương lai.',
    examples: [
      { ja: '明日は雨が降ると思います。', reading: 'あしたはあめがふるとおもいます。', vi: 'Tôi nghĩ ngày mai trời sẽ mưa.' },
      { ja: '日本は物価が高いと思います。', reading: 'にほんはぶっかがたかいとおもいます。', vi: 'Tôi nghĩ giá cả ở Nhật Bản khá đắt đỏ.' }
    ]
  },

  // --- CẤP ĐỘ N3 ---
  {
    id: 'n3-koto-ga-dekiru',
    level: 'N3',
    pattern: '〜ことがある',
    romaji: 'koto ga aru',
    meaning: 'Thỉnh thoảng làm gì...; có khi xảy ra việc gì',
    formation: 'Động từ thể từ điển / thể ない + ことがある',
    notes: 'Khác với 〜たことがある (đã từng trong quá khứ), mẫu này chỉ thói quen thỉnh thoảng xảy ra.',
    examples: [
      { ja: '朝ご飯を食べないことがあります。', reading: 'あさごはんをたべないことがあります。', vi: 'Thỉnh thoảng tôi không ăn sáng.' },
      { ja: 'たまに一人で旅行に行くことがあります。', reading: 'たまにひとりでりょこうにいくことがあります。', vi: 'Đôi khi tôi có chuyến đi du lịch một mình.' }
    ]
  },
  {
    id: 'n3-wake-dewa-nai',
    level: 'N3',
    pattern: '〜わけではない',
    romaji: 'wake dewa nai',
    meaning: 'Không hẳn là...; không nhất thiết là...',
    formation: 'Thể thông thường + わけではない',
    notes: 'Dùng để phủ định một phần nhận định hoặc cách hiểu suy đoán của người khác.',
    examples: [
      { ja: '嫌いなわけではないが、あまり食べない。', reading: 'きらいなわけではないが、あまりたべない。', vi: 'Không hẳn là tôi ghét món này, chỉ là tôi không hay ăn lắm.' },
      { ja: '日本語が全く話せないわけではありません。', reading: 'にほんごがまったくはなせないわけではありません。', vi: 'Không phải là tôi hoàn toàn không nói được tiếng Nhật.' }
    ]
  },

  // --- CẤP ĐỘ N2 & N1 ---
  {
    id: 'n2-ni-kakawawarazu',
    level: 'N2',
    pattern: '〜にかかわらず',
    romaji: 'ni kakawarazu',
    meaning: 'Bất kể là...; không phân biệt...',
    formation: 'Danh từ / Động thể từ điển + thể ない + にかかわらず',
    notes: 'Chỉ hành động hay sự việc xảy ra không bị ảnh hưởng bởi điều kiện phía trước.',
    examples: [
      { ja: '天候にかかわらず試合は行われます。', reading: 'てんこうにかかわらずしあいはてんこなわれます。', vi: 'Trận đấu vẫn sẽ diễn ra bất kể thời tiết như thế nào.' }
    ]
  },
  {
    id: 'n1-wo-kagirini',
    level: 'N1',
    pattern: '〜を限りに',
    romaji: 'wo kagiri ni',
    meaning: 'Kể từ lúc này trở đi chấm dứt...; đến hết...',
    formation: 'Danh từ chỉ thời gian + を限りに',
    notes: 'Thường dùng trong các thông báo chính thức tuyên bố kết thúc một giai đoạn.',
    examples: [
      { ja: '今日を限りにタバコをやめることにした。', reading: 'きょうをかぎりにタバコをやめることにした。', vi: 'Tôi đã quyết định từ bỏ thuốc lá kể từ ngày hôm nay.' }
    ]
  }
];

// =========================================================================
// 2. TRẠNG THÁI TOÀN CỤC CỦA TRANG
// =========================================================================
let currentLevel = 'N5';
let grammarList = [];
let currentSearchKeyword = '';

// =========================================================================
// 3. TẢI DỮ LIỆU NGỮ PHÁP (TỰ ĐỘNG THÍCH ỨNG REST API & FALLBACK)
// =========================================================================
async function loadGrammarData() {
  try {
    const res = await fetch(`/api/learner/grammar?level=${encodeURIComponent(currentLevel)}&size=30`);
    if (res.ok) {
      const data = await res.json();
      const content = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);
      if (content.length > 0) {
        grammarList = content.map(item => ({
          id: item.grammarCode || item.id || `gm-${item.pattern}`,
          level: item.jlptLevel || item.level || currentLevel,
          pattern: item.pattern,
          romaji: item.romaji,
          meaning: item.meaning || item.meaningVn,
          formation: item.formation || 'Đang cập nhật công thức',
          notes: item.notes || 'Chưa có ghi chú đặc biệt',
          examples: item.exampleSentence ? [{
            ja: item.exampleSentence,
            reading: item.exampleFurigana || item.exampleSentence,
            vi: item.exampleMeaning || 'Ví dụ minh họa'
          }] : []
        }));
        renderGrammarCards();
        return;
      }
    }
  } catch (e) {
    console.debug('Không kết nối được API Backend, sử dụng kho OpenJLPT tích hợp:', e);
  }

  // Sử dụng kho dữ liệu OpenJLPT tích hợp
  grammarList = OPENJLPT_BUILTIN_GRAMMAR.filter(g => g.level === currentLevel);
  renderGrammarCards();
}

// =========================================================================
// 4. RENDER DANH SÁCH THẺ NGỮ PHÁP (GRID VIEW)
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
      <div class="bg-white rounded-2xl border border-[#ECE8DE] p-5 flex flex-col justify-between hover:border-[#006194] hover:shadow-md transition duration-200 cursor-pointer group"
           data-action="view-detail" data-id="${rule.id}">
        <div class="space-y-3">
          <!-- Hàng tiêu đề & Cấp độ -->
          <div class="flex items-start justify-between gap-2 pb-3 border-b border-[#ECE8DE]">
            <div>
              <h3 class="text-xl md:text-2xl font-bold font-japanese text-[#0F2545] group-hover:text-[#006194] transition leading-snug">
                ${rule.pattern}
              </h3>
              <span class="text-xs text-[#68737D] font-mono italic">
                ${rule.romaji}
              </span>
            </div>

            <div class="flex items-center gap-1.5 shrink-0" onclick="event.stopPropagation()">
              <span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#FAF8F2] text-[#006194] border border-[#ECE8DE]">
                ${rule.level}
              </span>
              <button type="button" data-action="pronounce" data-text="${rule.pattern.replace(/〜|~/g, '')}"
                      class="w-7 h-7 rounded-lg border border-[#ECE8DE] bg-[#FAF8F2] hover:bg-[#FFE5EC] hover:text-[#831843] flex items-center justify-center text-xs transition cursor-pointer"
                      title="Nghe phát âm">
                <span class="material-symbols-outlined text-[15px]">volume_up</span>
              </button>
            </div>
          </div>

          <!-- Nghĩa tiếng Việt -->
          <div class="pt-1">
            <p class="text-xs md:text-sm font-semibold text-[#059669] bg-emerald-50/60 p-3 rounded-xl border border-emerald-100 leading-relaxed">
              👉 ${rule.meaning}
            </p>
          </div>
        </div>

        <!-- Chân thẻ: Nút xem chi tiết -->
        <div class="pt-3 mt-3 border-t border-[#ECE8DE] flex items-center justify-between text-xs shrink-0">
          <span class="text-[11px] font-bold text-[#831843] flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">menu_book</span>
            <span>Chi tiết công thức &amp; ví dụ</span>
          </span>
          <span class="font-bold text-[#006194] group-hover:translate-x-1 transition-transform flex items-center">
            Xem ngay &rarr;
          </span>
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
// 5. GIAO DIỆN CHI TIẾT MẪU NGỮ PHÁP (DETAIL VIEW)
// =========================================================================
function openGrammarDetail(ruleId) {
  const rule = grammarList.find(r => r.id === ruleId) || OPENJLPT_BUILTIN_GRAMMAR.find(r => r.id === ruleId);
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
// 6. PHÁT ÂM TIẾNG NHẬT (WEB SPEECH API SYNTHESIS)
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
// 7. KHỞI TẠO DOM & LẮNG NGHE SỰ KIỆN (DOM READY)
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
