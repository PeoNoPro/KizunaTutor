import pkg from 'file:///C:/Users/Vuh26/AppData/Local/ms-playwright-go/1.57.0/package/index.js';
const { chromium } = pkg;

(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const page = await browser.newPage();

  const report = {
    aiWriting: { tests: [], issues: [] },
    aiReading: { tests: [], issues: [] },
    backendContracts: { tests: [], issues: [] }
  };

  await page.addInitScript(() => {
    localStorage.setItem('kizuna_token', 'mock-token');
    localStorage.setItem('kizuna_user', JSON.stringify({ fullName: 'Nguyễn Văn A', role: 'LEARNER' }));
  });

  await page.route('**/*learning-tools/access*', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: { allowed: true, openClassCount: 1 } })
    });
  });

  // =========================================================================
  // PART 1: TEST AI WRITING (Chữa câu AI)
  // =========================================================================
  console.log('\n================== TESTING AI WRITING ==================');
  await page.goto('http://localhost:3000/ai-writing', { waitUntil: 'load' });
  await page.waitForTimeout(500);

  // Test 1.1: Mode buttons
  const btnFree = page.locator('#btnModeFree');
  const btnGrammar = page.locator('#btnModeGrammar');
  const hasModes = (await btnFree.isVisible()) && (await btnGrammar.isVisible());
  report.aiWriting.tests.push({ name: 'Hien thi 2 che do viet (Tu do & Theo ngu phap)', pass: hasModes });

  // Test 1.2: Check clear button
  const inputWriting = page.locator('#writingInput');
  await inputWriting.fill('テスト');
  await page.locator('#btnClearWriting').click();
  const cleared = (await inputWriting.inputValue()) === '';
  report.aiWriting.tests.push({ name: 'Nut xoa trang noi dung', pass: cleared });

  // Test 1.3: Chữa câu với chuỗi ngẫu nhiên (Invalid Language)
  await inputWriting.fill('asdasdasd random words');
  let evaluateCall = null;
  await page.route('**/api/learner/ai-writing/check', route => {
    evaluateCall = JSON.parse(route.request().postData());
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        overallScore: 0,
        isInvalid: true,
        senseiComment: 'Văn bản bạn nhập không phải tiếng Nhật. Vui lòng luyện viết bằng Hiragana, Katakana hoặc Kanji.',
        suggestedCorrection: '申し訳ありませんが、意味が分かりません。',
        analysisScoreText: 'Chưa đúng ngôn ngữ / Cần viết bằng tiếng Nhật',
        errors: [
          {
            type: 'warning',
            rule: 'Ngôn ngữ chưa phù hợp',
            original: 'asdasdasd random words',
            suggestion: '申し訳ありませんが、意味が分かりません。',
            explanation: 'Chuỗi ký tự ngẫu nhiên.'
          }
        ],
        keyVocabularies: []
      })
    });
  });

  await page.locator('#btnSubmitCheck').click();
  await page.waitForTimeout(1000);

  // Check if cardNativeSuggestion is hidden when isInvalid
  const isSuggestionHidden = await page.evaluate(() => {
    const card = document.getElementById('cardNativeSuggestion');
    return !card || card.classList.contains('hidden');
  });
  report.aiWriting.tests.push({
    name: 'An Cau Van De Xuat Chuan Ban Xu khi nhap van ban khong hop le (isInvalid=true)',
    pass: isSuggestionHidden
  });
  if (!isSuggestionHidden) {
    report.aiWriting.issues.push('cardNativeSuggestion van hien thi khi chuoi nhap la van ban khong hop le!');
  }

  // Check sensei comments count
  const senseiCommentsCount = await page.evaluate(() => {
    const mainComment = document.getElementById('senseiGeneralComment')?.innerText?.trim();
    const subComment = document.getElementById('senseiPromptComment')?.innerText?.trim();
    let count = 0;
    if (mainComment) count++;
    if (subComment && subComment !== mainComment) count++;
    return count;
  });
  report.aiWriting.tests.push({
    name: 'Chi hien thi 1 nhan xet Sensei (khong trung lap)',
    pass: senseiCommentsCount <= 1
  });

  // Test 1.4: Chữa câu tiếng Nhật hợp lệ
  await inputWriting.fill('私は毎朝7時に起きます。');
  await page.route('**/api/learner/ai-writing/evaluate', route => {
    evaluateCall = JSON.parse(route.request().postData());
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          overallScore: 95,
          isInvalid: false,
          senseiComment: 'Câu văn rất tốt và tự nhiên!',
          suggestedCorrection: '私は毎朝7時に起きます。',
          analysisScoreText: 'Xuất sắc / Chuẩn bản xứ',
          errors: [],
          keyVocabularies: [
            { word: '毎朝', reading: 'まいあさ', meaning: 'Mỗi sáng', jlpt: 'N5' },
            { word: '起きる', reading: 'おきる', meaning: 'Thức dậy', jlpt: 'N5' }
          ]
        }
      })
    });
  });

  await page.locator('#btnSubmitCheck').click();
  await page.waitForTimeout(1000);

  const isCard2VisibleValid = await page.evaluate(() => {
    const card = document.getElementById('cardNativeSuggestion');
    return card && !card.classList.contains('hidden');
  });
  report.aiWriting.tests.push({
    name: 'Hien Cau Van De Xuat Chuan Ban Xu khi cau tieng Nhat hop le',
    pass: isCard2VisibleValid
  });

  const vocabBadges = await page.locator('#writingVocabBadges > div').count();
  report.aiWriting.tests.push({
    name: 'Boc tach tu vung trong tam (Key Vocabularies)',
    pass: vocabBadges >= 2
  });

  // Test 1.5: Switch to Grammar Mode
  await btnGrammar.click();
  await page.waitForTimeout(300);
  const isGrammarPromptVisible = await page.locator('#grammarPromptSection').isVisible();
  report.aiWriting.tests.push({
    name: 'Chuyen sang che do Luyen viet theo ngu phap mo khung chon ngu phap',
    pass: isGrammarPromptVisible
  });

  // =========================================================================
  // PART 2: TEST AI READING (Đọc hiểu Dokkai)
  // =========================================================================
  console.log('\n================== TESTING AI READING ==================');
  await page.goto('http://localhost:3000/reading', { waitUntil: 'load' });
  await page.waitForTimeout(500);

  // Test 2.1: Check preset chips removed
  const presetChipsCount = await page.locator('.btn-sample-preset').count();
  report.aiReading.tests.push({
    name: 'Cac nut goi y mau nhanh (.btn-sample-preset) da duoc go bo hoan toan',
    pass: presetChipsCount === 0
  });

  // Test 2.2: Mock Reading Analysis
  const readingInput = page.locator('#readingInputText');
  await readingInput.fill('先週の日曜日、私は友達と東京へ行きました。とても楽しかったです。');

  await page.route('**/api/learner/ai-reading/analyze', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          overallSummary: 'Bài đọc kể về chuyến đi chơi Tokyo cùng bạn bè vào Chủ nhật tuần trước.',
          estimatedLevel: 'N5',
          annotatedHtml: '<ruby>先週<rt>せんしゅう</rt></ruby>の<ruby>日曜日<rt>にちようび</rt></ruby>、<ruby>私<rt>わたし</rt></ruby>は<ruby>友達<rt>ともだち</rt></ruby>と<ruby>東京<rt>とうきょう</rt></ruby>へ<ruby>行<rt>い</rt></ruby>きました。',
          cleanPassageText: '先週の日曜日、私は友達と東京へ行きました。とても楽しかったです。',
          cleanPassageRubyHtml: '<ruby>先週<rt>せんしゅう</rt></ruby>の<ruby>日曜日<rt>にちようび</rt></ruby>、<ruby>私<rt>わたし</rt></ruby>は<ruby>友達<rt>ともだち</rt></ruby>と<ruby>東京<rt>とうきょう</rt></ruby>へ<ruby>行<rt>い</rt></ruby>きました。',
          interactiveTokens: [
            {
              id: 'tok-0',
              token: '先週',
              reading: 'せんしゅう',
              hanViet: 'TIỀN CHU',
              meaning: 'Tuần trước',
              type: 'VOCAB',
              jlpt: 'N5'
            },
            {
              id: 'tok-1',
              token: '友達',
              reading: 'ともだち',
              hanViet: 'HỮU ĐẠT',
              meaning: 'Bạn bè',
              type: 'VOCAB',
              jlpt: 'N5'
            }
          ],
          dokkaiQuestions: [
            {
              id: 1,
              questionJa: '筆者はいつ東京へ行きましたか。',
              questionVi: 'Tác giả đã đi Tokyo khi nào?',
              optionsJa: ['先週の日曜日', '今週の日曜日', '先月の土曜日', '昨日の夜'],
              optionsVi: ['Chủ nhật tuần trước', 'Chủ nhật tuần này', 'Thứ 7 tháng trước', 'Tối hôm qua'],
              correctIndex: 0,
              explanationVi: 'Trong bài có câu: 先週の日曜日、私は友達と東京へ行きました。',
              evidenceQuote: '先週の日曜日、私は友達と東京へ行きました。'
            }
          ],
          grammars: [
            {
              pattern: '〜へ行く',
              meaning: 'Đi tới đâu',
              level: 'N5',
              example: '東京へ行きました。'
            }
          ],
          vocabularies: [
            {
              word: '先週',
              reading: 'せんしゅう',
              hanViet: 'TIỀN CHU',
              meaning: 'Tuần trước',
              level: 'N5'
            }
          ],
          sentenceTranslations: [
            {
              ja: '先週の日曜日、私は友達と東京へ行きました。',
              vi: 'Chủ nhật tuần trước, tôi đã cùng bạn bè đi Tokyo.'
            }
          ]
        }
      })
    });
  });

  // Click Analyze
  await page.locator('#btnAnalyzeReading').click();
  await page.waitForTimeout(1000);

  // Test 2.3: Workspace Screen shown
  const isWorkspaceVisible = await page.locator('#readingWorkspaceScreen').isVisible();
  report.aiReading.tests.push({
    name: 'Chuyen man hinh tu Input sang Workspace phan tich doc hieu',
    pass: isWorkspaceVisible
  });

  // Test 2.4: Dokkai Questions rendered (Tab 1)
  const questionCount = await page.locator('.dokkai-question-card').count();
  report.aiReading.tests.push({
    name: 'Hien thi cau hoi trac nghiem doc hieu Dokkai (Tab 1)',
    pass: questionCount >= 1
  });

  // Test 2.5: Interactive Furigana & Tooltip
  // Switch to Tab 2: Reader
  await page.locator('.reading-tab-btn[data-tab="reader"]').click();
  await page.waitForTimeout(300);
  const readerTabVisible = await page.locator('#tabPanelReader').isVisible();
  report.aiReading.tests.push({
    name: 'Chuyen muot sang Tab 2 (Khong gian doc tuong tac)',
    pass: readerTabVisible
  });

  // Test 2.6: Furigana toggle button
  const btnToggleFuri = page.locator('#btnToggleFurigana');
  await btnToggleFuri.click();
  const hasHiddenRt = await page.evaluate(() => {
    return document.body.classList.contains('hide-furigana') || !!document.querySelector('.hide-furigana');
  });
  report.aiReading.tests.push({
    name: 'Nut bat/tat Furigana hoat dong chuan xac',
    pass: hasHiddenRt
  });

  // Test 2.7: Vocab Tab & Add Flashcard Modal
  await page.locator('.reading-tab-btn[data-tab="vocab"]').click();
  await page.waitForTimeout(300);
  const vocabTabVisible = await page.locator('#tabPanelVocab').isVisible();
  report.aiReading.tests.push({
    name: 'Chuyen sang Tab 3 (Tu vung boc tach)',
    pass: vocabTabVisible
  });

  const btnSaveCard = page.locator('.btn-save-vocab-flashcard').first();
  const canOpenSaveModal = await btnSaveCard.isVisible();
  if (canOpenSaveModal) {
    await btnSaveCard.click();
    await page.waitForTimeout(300);
    const modalVisible = await page.locator('#modalSaveToDeck').isVisible();
    report.aiReading.tests.push({
      name: 'Mo Modal luu the tu vung vao bo the Flashcards 1-cham',
      pass: modalVisible
    });
    // Close modal
    await page.locator('#btnCancelModal').click();
  } else {
    report.aiReading.tests.push({
      name: 'Mo Modal luu the tu vung vao bo the Flashcards 1-cham',
      pass: false
    });
    report.aiReading.issues.push('Khong tim thay nut luu flashcard tren the tu vung boc tach');
  }

  // =========================================================================
  // PRINT SUMMARY REPORT
  // =========================================================================
  console.log('\n================== FINAL DIAGNOSTIC RESULTS ==================');
  console.log(JSON.stringify(report, null, 2));

  await browser.close();
})();
