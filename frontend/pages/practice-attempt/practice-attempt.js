/**
 * FE-13: làm bài luyện đề và xem kết quả (giao diện cuộn liên tục theo từng phần thi, đổi 2026-11-17).
 * - Cột phải: tên bài, đồng hồ, Nộp bài, tiến độ, danh sách phần thi (Đang làm / Đã làm / Chưa làm), bảng số câu của phần.
 * - Cột trái: cả phần thi đang chọn — mondai, hướng dẫn, đoạn văn, các câu xếp dọc; chọn đáp án là lưu ngay.
 * - Thi thử đầy đủ (Theo đề, từ 2 phần trở lên, không phải "làm lại câu sai"): như thi thật — mỗi phần một đồng hồ riêng,
 *   "Nộp phần này" hoặc hết giờ thì khóa phần đó, không quay lại (N1/N2 gộp Từ vựng + Ngữ pháp – Đọc hiểu làm một phần).
 *   Các chế độ còn lại chuyển phần tự do, một đồng hồ tổng. Máy chủ giữ hạn chót tổng, quá hạn tự nộp.
 * - Phần Nghe (chỉ có khi làm theo đề): thanh âm thanh cố định ở đáy, phát cả file liền mạch, được tua.
 *   Lời thoại chỉ hiện sau khi nộp.
 */
import { api, el, icon, richText, mediaUrl, clock, furiganaToggle, askConfirm, SECTION_LABELS, SECTION_ORDER } from '/js/pages/practice-common.js';
import { renderQuestion, renderGroup } from '/js/pages/practice-render.js';
import { showToast } from '/js/main.js';

// Khung học viên chỉ chép nội dung <main> khi chuyển trang, nên dấu nhận diện nằm ở thẻ bọc bên trong.
const root = document.querySelector('[data-practice-page="attempt"]');
if (root) init(root);

function init(root) {
  const $ = id => root.querySelector('#' + id);
  const attemptId = new URLSearchParams(location.search).get('id');
  const main = () => root.closest('main');
  let data = null, sections = [], current = null, timer = null, offset = 0, wrongOnly = false;
  // Thi thử đầy đủ: danh sách phần (mỗi phần gồm 1–2 phần thi, số phút) và phần đang làm; lưu trên máy để tải lại không mất.
  let phases = [], phase = 0;
  const storeKey = 'kizuna-practice-' + attemptId;
  const read = () => { try { return JSON.parse(localStorage.getItem(storeKey)) || {}; } catch { return {}; } };
  const write = value => { try { localStorage.setItem(storeKey, JSON.stringify(value)); } catch { /* bỏ qua */ } };
  const locked = () => phases.length > 0 && !data.submitted;
  const phaseOf = code => phases.findIndex(p => p.sections.includes(code));

  const notice = message => { $('paNotice').textContent = message || ''; $('paNotice').hidden = !message; };
  const now = () => Date.now() + offset;
  const inSection = code => data.questions.filter(q => q.section === code);

  /** Số hiển thị: thi theo đề giữ đúng số câu trong đề; trộn ngẫu nhiên đánh 1, 2, 3… theo từng phần. */
  function numberQuestions() {
    for (const code of SECTION_ORDER) inSection(code).forEach((q, i) => { q.displayNo = data.mode === 'EXAM' ? q.number : i + 1; });
  }

  async function load() {
    if (!attemptId) { notice('Thiếu mã bài làm.'); return; }
    try { data = await api('/learner/practice/attempts/' + encodeURIComponent(attemptId)); }
    catch (error) { notice(error.message); return; }
    offset = new Date(data.serverTime).getTime() - Date.now();
    sections = SECTION_ORDER.filter(code => data.questions.some(q => q.section === code));
    buildPhases();
    if (!sections.includes(current) || (locked() && phaseOf(current) !== phase)) current = locked() ? phases[phase].sections[0] : sections[0];
    numberQuestions(); renderAll(); startTimer();
  }

  function buildPhases() {
    phases = [];
    if (data.submitted || data.mode !== 'EXAM' || data.wrongOnly || !data.minutes || sections.length < 2) return;
    for (const code of sections) {
      const minutes = Number(data.minutes[code] || 0);
      if (!minutes && phases.length) phases[phases.length - 1].sections.push(code);
      else phases.push({ sections: [code], ms: minutes * 60000 });
    }
    const saved = read();
    const starts = saved.starts || [];
    phases.forEach((p, i) => { p.start = starts[i] ?? null; });
    if (!phases[0].start) phases[0].start = new Date(data.startedAt).getTime();
    phase = Math.min(saved.phase || 0, phases.length - 1);
    // Quay lại sau khi tắt máy: phần nào đã hết giờ thì coi như đã nộp.
    while (phase < phases.length - 1 && now() >= phases[phase].start + phases[phase].ms) {
      phases[phase + 1].start = phases[phase].start + phases[phase].ms; phase++;
    }
    savePhases();
  }
  function savePhases() { write({ phase, starts: phases.map(p => p.start) }); }

  /** Nộp phần đang làm (bấm nút hoặc hết giờ): khóa phần đó, sang phần kế; phần cuối thì nộp cả bài. */
  function finishPhase(auto) {
    if (phase >= phases.length - 1) { submit(true); return; }
    phase++; phases[phase].start = now(); savePhases();
    current = phases[phase].sections[0]; renderAll(); main()?.scrollTo({ top: 0 });
    showToast(auto ? 'Hết giờ phần trước, đã chuyển sang phần tiếp theo.' : 'Đã nộp phần trước. Bắt đầu phần tiếp theo.', 'info');
  }

  // ---- Đồng hồ: đếm ngược khi có hạn chót (thi theo đề), còn lại đếm thời gian đã làm.
  function startTimer() {
    clearInterval(timer);
    if (data.submitted) { $('paTimerText').textContent = 'Đã nộp'; $('paTimer').dataset.state = ''; return; }
    const tick = () => {
      if (!root.isConnected) { clearInterval(timer); return; }
      if (locked()) {
        const left = phases[phase].start + phases[phase].ms - now();
        $('paTimerText').textContent = clock(left); $('paTimer').dataset.state = left < 5 * 60000 ? 'low' : '';
        if (left <= 0) finishPhase(true);
      } else if (data.deadlineAt) {
        const left = new Date(data.deadlineAt).getTime() - now();
        $('paTimerText').textContent = clock(left); $('paTimer').dataset.state = left < 5 * 60000 ? 'low' : '';
        if (left <= 0) { clearInterval(timer); submit(true); }
      } else $('paTimerText').textContent = clock(now() - new Date(data.startedAt).getTime());
    };
    tick(); timer = setInterval(tick, 1000);
  }

  // ---- Vẽ
  function renderAll() { renderSide(); renderSection(); renderResult(); }

  function renderSide() {
    $('paTitle').textContent = `${data.level} · ${data.examTitle || data.modeLabel}${data.wrongOnly ? ' · Làm lại câu sai' : ''}`;
    $('paSubmit').hidden = data.submitted;
    const shown = data.submitted ? data.correctCount : data.questions.filter(q => q.chosen).length;
    $('paProgressLabel').textContent = data.submitted ? 'Số câu đúng' : 'Hoàn thành';
    $('paProgress').textContent = `${shown}/${data.questionCount}`;
    $('paProgressBar').style.width = Math.round(shown / Math.max(1, data.questionCount) * 100) + '%';

    $('paSections').replaceChildren(...sections.map(code => {
      const list = inSection(code), done = list.filter(q => q.chosen).length;
      const button = el('button', undefined, 'pa-section'); button.type = 'button';
      const own = phaseOf(code), closed = locked() && own < phase, waiting = locked() && own > phase;
      const state = code === current ? 'doing' : closed || done ? 'done' : 'todo';
      button.dataset.state = state; button.setAttribute('aria-current', String(code === current));
      button.disabled = closed || waiting;
      const label = data.submitted ? `${list.filter(q => q.isCorrect).length}/${list.length} đúng`
        : closed ? 'Đã nộp' : state === 'doing' ? 'Đang làm' : waiting ? 'Chưa đến' : done ? 'Đã làm' : 'Chưa làm';
      button.append(el('span', SECTION_LABELS[code]), el('small', label));
      button.onclick = () => go(code);
      return button;
    }));

    const list = inSection(current).filter(q => !wrongOnly || !q.isCorrect);
    $('paCount').textContent = list.length + ' câu';
    $('paGrid').replaceChildren(...list.map(q => {
      const cell = el('button', String(q.displayNo), 'pa-cell'); cell.type = 'button';
      cell.dataset.answered = String(!!q.chosen); cell.dataset.flagged = String(!!q.flagged);
      if (data.submitted) cell.dataset.result = q.isCorrect ? 'correct' : 'wrong';
      cell.setAttribute('aria-label', `Câu ${q.displayNo}${q.chosen ? ', đã trả lời' : ''}${q.flagged ? ', đã đánh dấu' : ''}`);
      cell.onclick = () => root.querySelector(`[data-card="${q.questionId}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return cell;
    }));
    const index = sections.indexOf(current);
    if (locked()) {
      // Chỉ chuyển trong phần đang làm (N1/N2 gộp 2 phần thi); sang phần sau phải "Nộp phần này".
      const inPhase = phases[phase].sections, at = inPhase.indexOf(current);
      $('paPrevSection').hidden = at <= 0;
      $('paNextSection').hidden = phase >= phases.length - 1 && at >= inPhase.length - 1;
      $('paNextSection').firstChild.textContent = at < inPhase.length - 1 ? 'Phần tiếp theo' : 'Nộp phần này, sang phần tiếp';
      $('paTitle').textContent += ` · Phần ${phase + 1}/${phases.length} (${phases[phase].ms / 60000} phút)`;
    } else {
      $('paPrevSection').hidden = index <= 0; $('paNextSection').hidden = index >= sections.length - 1;
      $('paNextSection').firstChild.textContent = 'Phần tiếp theo';
    }
  }

  /** Cả phần thi: các mondai xếp dọc; mỗi mondai có tiêu đề, hướng dẫn, đoạn văn nhóm và các câu. */
  function renderSection() {
    const box = $('paSection'); box.replaceChildren();
    const list = inSection(current).filter(q => !wrongOnly || !q.isCorrect);
    if (!list.length) box.append(el('p', wrongOnly ? 'Phần này bạn làm đúng hết.' : 'Phần này không có câu hỏi.', 'pr-empty pr-card'));
    let mondaiKey = null, block = null, group = null;
    for (const q of list) {
      const key = q.examId + ':' + q.mondaiNumber;
      if (key !== mondaiKey) {
        mondaiKey = key; group = null;
        box.append(el('div', `もんだい${q.mondaiNumber}` + (data.mode === 'RANDOM' ? `　·　${q.examTitle}` : ''), 'pa-mondai-title'));
        block = el('div', undefined, 'pa-block');
        if (q.instruction) block.append(richText('div', q.instruction, 'pr-instruction pr-jp'));
        box.append(block);
      }
      if (q.groupId && q.groupId !== group) {
        group = q.groupId;
        if (q.passage || q.groupImageUrl) { const passage = el('div', undefined, 'pa-qcard'); passage.append(renderGroup(q)); block.append(passage); }
      }
      block.append(questionCard(q));
    }
    // Thanh âm thanh: chỉ hiện ở phần Nghe; tạo một lần nên chuyển phần không làm mất vị trí đang nghe.
    const audio = $('paAudio'), first = inSection('LISTENING').find(q => q.audio);
    if (first && !audio.querySelector('audio')) {
      const player = el('audio'); player.controls = true; player.preload = 'metadata'; player.src = mediaUrl(first.audio.url); audio.append(player);
    }
    audio.hidden = current !== 'LISTENING' || !first;
    if (current !== 'LISTENING') audio.querySelector('audio')?.pause();
  }

  function questionCard(q) {
    const card = el('div', undefined, 'pa-qcard'); card.dataset.card = q.questionId;
    card.append(renderQuestion({ ...q, number: q.displayNo }, { showAnswers: data.submitted, chosen: q.chosen, onChoose: data.submitted ? null : value => choose(q, value, card) }));
    if (!data.submitted) {
      const flag = el('button', undefined, 'pr-btn pa-flag'); flag.type = 'button'; flag.setAttribute('aria-pressed', String(!!q.flagged));
      flag.append(icon('flag'), document.createTextNode(q.flagged ? 'Đã đánh dấu' : 'Đánh dấu'));
      flag.onclick = () => { q.flagged = !q.flagged; card.replaceWith(questionCard(q)); renderSide(); save(q); };
      card.append(flag);
    }
    return card;
  }

  function renderResult() {
    const box = $('paResult');
    if (!data.submitted) { box.hidden = true; return; }
    const scores = el('div', undefined, 'pa-scores');
    const total = el('div', undefined, 'pa-score'); total.append(el('span', 'Tổng'), el('strong', `${data.correctCount}/${data.questionCount}`)); scores.append(total);
    (data.sectionScores || []).forEach(s => { const card = el('div', undefined, 'pa-score'); card.append(el('span', s.label), el('strong', `${s.correct}/${s.total}`)); scores.append(card); });
    const actions = el('div', undefined, 'flex flex-wrap gap-2');
    const filter = el('button', wrongOnly ? 'Xem tất cả câu' : 'Chỉ xem câu sai', 'pr-btn'); filter.type = 'button';
    filter.onclick = () => { wrongOnly = !wrongOnly; renderAll(); };
    const again = el('a', 'Luyện tiếp', 'pr-btn pr-btn-primary'); again.href = '/learner/practice';
    actions.append(filter, again);
    box.replaceChildren(el('strong', 'Kết quả bài làm', 'text-[16px] text-[#0F172A]'), scores,
      el('p', 'Đây là số câu đúng từng phần, không phải điểm quy đổi JLPT chính thức. Bên dưới có đáp án đúng, giải thích và lời thoại phần Nghe của từng câu.', 'pa-sub'), actions);
    box.hidden = false;
  }

  // ---- Thao tác
  async function save(q) {
    try { await api(`/learner/practice/attempts/${attemptId}/answers/${q.questionId}`, { method: 'PUT', body: { chosen: q.chosen ?? null, flagged: !!q.flagged } }); }
    catch (error) { showToast(error.message, 'error'); if (error.status === 409) load(); }
  }
  // Chỉ vẽ lại đúng câu vừa chọn để trang không nhảy vị trí cuộn.
  function choose(q, value, card) { q.chosen = q.chosen === value ? null : value; card.replaceWith(questionCard(q)); renderSide(); save(q); }
  function go(code) { if (code === current) return; current = code; renderAll(); main()?.scrollTo({ top: 0 }); }
  $('paPrevSection').addEventListener('click', () => go(sections[sections.indexOf(current) - 1]));
  $('paNextSection').addEventListener('click', () => {
    if (locked()) {
      const inPhase = phases[phase].sections, at = inPhase.indexOf(current);
      if (at < inPhase.length - 1) { go(inPhase[at + 1]); return; }
      const empty = data.questions.filter(q => inPhase.includes(q.section) && !q.chosen).length;
      askConfirm({ host: root, iconName: 'arrow_forward', title: 'Nộp phần này?', ok: 'Nộp phần này', cancel: 'Làm tiếp',
        message: (empty ? `Phần này còn ${empty} câu chưa trả lời. ` : '') + 'Sau khi nộp bạn sẽ sang phần tiếp theo và không quay lại sửa phần này được nữa (giống thi thật).' })
        .then(yes => { if (yes && locked()) finishPhase(false); });
      return;
    }
    go(sections[sections.indexOf(current) + 1]);
  });

  async function submit(auto) {
    if (data.submitted) return;
    if (!auto) {
      const empty = data.questions.filter(q => !q.chosen).length;
      const yes = await askConfirm({ host: root, iconName: 'send', title: 'Nộp bài?', ok: 'Nộp bài', cancel: 'Làm tiếp',
        message: empty ? `Bạn còn ${empty}/${data.questions.length} câu chưa trả lời, câu bỏ trống tính là sai. Sau khi nộp sẽ xem được kết quả, đáp án và giải thích.`
          : 'Bạn đã trả lời hết các câu. Sau khi nộp sẽ xem được kết quả, đáp án và giải thích.' });
      if (!yes || data.submitted) return;
    }
    clearInterval(timer); $('paSubmit').disabled = true;
    try {
      data = await api(`/learner/practice/attempts/${attemptId}/submit`, { method: 'POST' });
      try { localStorage.removeItem(storeKey); } catch { /* bỏ qua */ }
      phases = []; numberQuestions(); root.querySelector('#paAudio audio')?.pause();
      renderAll(); startTimer(); main()?.scrollTo({ top: 0 });
      showToast(auto ? 'Hết giờ, hệ thống đã nộp bài.' : 'Đã nộp bài.', 'success');
    } catch (error) { showToast(error.message, 'error'); startTimer(); }
    finally { $('paSubmit').disabled = false; }
  }
  $('paSubmit').addEventListener('click', () => submit(false));

  furiganaToggle(root, $('paFurigana'));
  load();
}
