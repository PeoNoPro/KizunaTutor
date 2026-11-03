/**
 * KizunaTutor - Trang Học Từ Vựng Theo Chủ Đề (Learning Support)
 * Chuẩn hóa layout: Topic Pills cuộn ngang, Grid 3 cột auto-fill, không giật màn hình
 */
import { initLearnerShell } from '/components/learner-layout/learner-layout.js';

let currentTopics = [
  { id: 'JLPT_N5', code: 'JLPT_N5', titleVn: 'JLPT N5 Căn bản', icon: '🌱', count: '650' },
  { id: 'JLPT_N4', code: 'JLPT_N4', titleVn: 'JLPT N4 Sơ cấp', icon: '🌿', count: '750' },
  { id: 'JLPT_N3', code: 'JLPT_N3', titleVn: 'JLPT N3 Trung cấp', icon: '🎋', count: '1.200' },
  { id: 'JLPT_N2', code: 'JLPT_N2', titleVn: 'JLPT N2 Cao cấp', icon: '🌸', count: '1.800' },
  { id: 'JLPT_N1', code: 'JLPT_N1', titleVn: 'JLPT N1 Thượng cấp', icon: '⛩️', count: '2.400' },
  { id: 'LIFE_DAILY', code: 'LIFE_DAILY', titleVn: 'Giao tiếp hàng ngày', icon: '💬', count: '450' },
  { id: 'WORKPLACE', code: 'WORKPLACE', titleVn: 'Tiếng Nhật công sở', icon: '💼', count: '380' }
];

let activeTopic = 'JLPT_N5';
let vocabList = [];

document.addEventListener('DOMContentLoaded', () => {
  initLearnerShell({
    activeMenu: 'dashboard',
    pageTitle: 'Kho từ vựng & Hỗ trợ học tập',
    badge: 'Học tập & rèn luyện'
  });

  initPage();
});

async function initPage() {
  await loadTopics();
  renderTopicPills();
  await loadVocabularies();

  document.getElementById('searchVocabInput')?.addEventListener('input', (e) => {
    filterVocab(e.target.value);
  });
}

async function loadTopics() {
  try {
    const res = await fetch('/api/learner/topics');
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data) && data.length > 0) {
        currentTopics = data;
      }
    }
  } catch (e) {
    // Sử dụng danh mục mặc định
  }
}

function renderTopicPills() {
  const container = document.getElementById('topicPillsContainer');
  if (!container) return;

  container.innerHTML = currentTopics.map(t => {
    const topicKey = String(t.code || t.id || '');
    const isActive = topicKey === activeTopic;
    return `
      <button type="button"
              data-topic="${topicKey}"
              class="px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap shrink-0 transition flex items-center gap-1.5 cursor-pointer ${
                isActive
                  ? 'bg-[#1E293B] text-white shadow-xs border border-[#1E293B]'
                  : 'bg-[#FAF8F2] border border-[#ECE8DE] text-[#0F2545] hover:bg-white hover:border-[#CBD5E1]'
              }">
        <span>${t.icon || '📚'}</span>
        <span>${t.titleVn}</span>
      </button>
    `;
  }).join('');

  container.querySelectorAll('button[data-topic]').forEach(btn => {
    btn.addEventListener('click', () => {
      activeTopic = btn.dataset.topic;
      const matched = currentTopics.find(t => String(t.code || t.id) === activeTopic);
      const badge = document.getElementById('activeTopicBadge');
      if (badge && matched) {
        badge.textContent = matched.titleVn;
      }
      renderTopicPills();
      loadVocabularies();
    });
  });

  const countSummary = document.getElementById('topicCountSummary');
  if (countSummary) {
    countSummary.textContent = `${currentTopics.length} chủ đề tuyển chọn`;
  }
}

async function loadVocabularies() {
  const grid = document.getElementById('vocabGrid');
  const emptyState = document.getElementById('vocabEmptyState');
  if (!grid) return;

  grid.innerHTML = '<div class="col-span-full py-10 text-center text-xs text-slate-400">Đang tải danh sách từ vựng...</div>';

  try {
    const res = await fetch(`/api/learner/vocabularies?topic=${encodeURIComponent(activeTopic)}`);
    if (res.ok) {
      const data = await res.json();
      vocabList = Array.isArray(data) ? data : (data.content || data.items || []);
    } else {
      vocabList = [];
    }
  } catch (e) {
    vocabList = [];
  }

  if (!vocabList || vocabList.length === 0) {
    // Dữ liệu mẫu nếu API backend chưa có dữ liệu cho topic này
    vocabList = getSampleVocab(activeTopic);
  }

  renderVocabGrid(vocabList);
}

function renderVocabGrid(items) {
  const grid = document.getElementById('vocabGrid');
  const emptyState = document.getElementById('vocabEmptyState');
  if (!grid) return;

  if (!items || items.length === 0) {
    grid.innerHTML = '';
    emptyState?.classList.remove('hidden');
    return;
  }

  emptyState?.classList.add('hidden');

  grid.innerHTML = items.map(v => {
    return `
      <div class="bg-white border border-[#ECE8DE] rounded-2xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between h-full min-w-0 group">
        <div class="space-y-2.5 flex-1 min-w-0">
          <div class="flex items-start justify-between gap-2 pb-2.5 border-b border-[#ECE8DE]">
            <div class="min-w-0">
              <div class="text-xl md:text-2xl font-bold font-japanese text-[#0F2545] leading-snug truncate">
                ${v.word || v.kanji}
              </div>
              <div class="flex items-center gap-1.5 mt-0.5 text-xs text-[#68737D] font-mono truncate">
                <span>${v.furigana || v.reading || ''}</span>
                ${v.hanViet ? `<span class="text-slate-300">•</span><span class="text-amber-800 font-bold uppercase">${v.hanViet}</span>` : ''}
              </div>
            </div>
            <span class="px-2.5 py-0.5 text-[11px] font-bold rounded-full bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8] shrink-0">
              ${v.level || 'JLPT'}
            </span>
          </div>

          <div class="text-xs md:text-sm font-semibold text-[#0F2545] leading-relaxed break-words">
            ${v.meaning || v.meaningVn || 'Đang cập nhật'}
          </div>

          ${v.example ? `
            <div class="p-2.5 bg-[#FAF8F2] border border-[#ECE8DE] rounded-xl text-xs space-y-1 break-words">
              <div class="font-japanese font-medium text-[#0F2545]">${v.example}</div>
              <div class="text-[#68737D] italic">${v.exampleMeaning || ''}</div>
            </div>
          ` : ''}
        </div>

        <!-- Cụm nút hành động bám sát đáy (mt-auto) -->
        <div class="pt-3 mt-auto border-t border-[#ECE8DE] flex items-center justify-between gap-2">
          <a href="/shodo?char=${encodeURIComponent(v.kanji || v.word || '')}"
             class="flex-1 py-1.5 px-2 rounded-xl bg-[#FAF8F2] hover:bg-[#FFE5EC] text-[#0F2545] hover:text-[#831843] border border-[#ECE8DE] hover:border-[#FBCFE8] text-xs font-bold flex items-center justify-center gap-1 transition no-underline text-center truncate">
            <span>🖌️</span>
            <span>Luyện viết</span>
          </a>
          <button type="button"
                  onclick="navigator.clipboard?.writeText('${v.word || v.kanji}');"
                  class="p-1.5 rounded-xl border border-[#ECE8DE] bg-white hover:bg-slate-50 text-[#68737D] hover:text-[#0F2545] transition cursor-pointer"
                  title="Sao chép từ vựng">
            <span class="material-symbols-outlined text-[16px]">content_copy</span>
          </button>
        </div>
      </div>
    `;
  }).join('');
}

function filterVocab(kw) {
  const query = (kw || '').trim().toLowerCase();
  if (!query) {
    renderVocabGrid(vocabList);
    return;
  }
  const filtered = vocabList.filter(v => {
    return (v.word && v.word.toLowerCase().includes(query)) ||
           (v.kanji && v.kanji.toLowerCase().includes(query)) ||
           (v.furigana && v.furigana.toLowerCase().includes(query)) ||
           (v.meaning && v.meaning.toLowerCase().includes(query)) ||
           (v.hanViet && v.hanViet.toLowerCase().includes(query));
  });
  renderVocabGrid(filtered);
}

function getSampleVocab(topic) {
  return [
    { word: '勉強', kanji: '勉強', furigana: 'べんきょう', hanViet: 'MIỄN CƯỠNG', level: 'N5', meaning: 'Học tập, học hành', example: '毎日日本語を勉強します。', exampleMeaning: 'Mỗi ngày tôi đều học tiếng Nhật.' },
    { word: '友達', kanji: '友達', furigana: 'ともだち', hanViet: 'HỮU ĐẠT', level: 'N5', meaning: 'Bạn bè, người bạn', example: '友達と遊びに行きます。', exampleMeaning: 'Tôi đi chơi cùng với bạn bè.' },
    { word: '時間', kanji: '時間', furigana: 'じかん', hanViet: 'THỜI GIAN', level: 'N5', meaning: 'Thời gian, giờ giấc', example: '時間がありません。', exampleMeaning: 'Tôi không có thời gian.' },
    { word: '仕事', kanji: '仕事', furigana: 'しごと', hanViet: 'SĨ SỰ', level: 'N5', meaning: 'Công việc, nghề nghiệp', example: '明日は仕事があります。', exampleMeaning: 'Ngày mai tôi có công việc.' },
    { word: '家族', kanji: '家族', furigana: 'かぞく', hanViet: 'GIA TỘC', level: 'N5', meaning: 'Gia đình', example: '私の家族は4人です。', exampleMeaning: 'Gia đình tôi gồm có 4 người.' },
    { word: '約束', kanji: '約束', furigana: 'やくそく', hanViet: 'ƯỚC THÚC', level: 'N4', meaning: 'Lời hứa, cuộc hẹn', example: '友達と約束があります。', exampleMeaning: 'Tôi có cuộc hẹn với bạn bè.' }
  ];
}
