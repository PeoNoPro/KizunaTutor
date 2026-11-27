import pkg from 'file:///C:/Users/Vuh26/AppData/Local/ms-playwright-go/1.57.0/package/index.js';
const { chromium } = pkg;

(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const page = await browser.newPage();

  const report = {
    module1_Auth: { pages: [], issues: [] },
    module5_LearningSupport: { pages: [], issues: [] }
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
  // KIỂM TRA MODULE 1: TÀI KHOẢN & PHÂN QUYỀN
  // =========================================================================
  const authUrls = [
    { url: 'http://localhost:3000/login', name: 'Đăng nhập & Quên mật khẩu', checkSelector: '#formLogin' },
    { url: 'http://localhost:3000/register', name: 'Đăng ký & Xác thực OTP', checkSelector: '#formRegister' }
  ];

  for (const item of authUrls) {
    const pageErrors = [];
    page.on('pageerror', err => pageErrors.push(err.message));
    try {
      await page.goto(item.url, { waitUntil: 'load', timeout: 8000 });
      await page.waitForTimeout(500);
      const isCheckVisible = item.checkSelector ? await page.locator(item.checkSelector).isVisible() : true;
      report.module1_Auth.pages.push({
        name: item.name,
        url: item.url,
        title: await page.title(),
        pass: pageErrors.length === 0 && isCheckVisible,
        errors: pageErrors
      });
    } catch (e) {
      report.module1_Auth.pages.push({
        name: item.name,
        url: item.url,
        pass: false,
        errors: [e.message]
      });
    }
  }

  // =========================================================================
  // KIỂM TRA MODULE 5: HỖ TRỢ HỌC TẬP (6 TÍNH NĂNG HOÀNG PHỤ TRÁCH)
  // =========================================================================
  const module5Urls = [
    // 25. Tra cứu từ vựng, kanji, ngữ pháp
    { url: 'http://localhost:3000/learning-support', name: '25a. Tra cứu từ vựng theo chủ đề', checkSelector: '#vocabSearchInput' },
    { url: 'http://localhost:3000/kanji-dictionary', name: '25b. Từ điển chữ Hán', checkSelector: '#kanjiSearchInput' },
    { url: 'http://localhost:3000/grammar', name: '25c. Kho ngữ pháp JLPT', checkSelector: '#grammarSearchInput' },

    // 26. Tra kanji bằng nét viết tay
    { url: 'http://localhost:3000/handwriting', name: '26. Tra kanji bằng bảng vẽ viết tay', checkSelector: 'canvas' },

    // 27. Bộ thẻ Flashcard (học thẻ, ghép thẻ)
    { url: 'http://localhost:3000/flashcards', name: '27a. Quản lý bộ thẻ Flashcards', checkSelector: '#deckGrid' },
    { url: 'http://localhost:3000/flashcards-study?id=1', name: '27b. Chế độ học Flashcard 3D & Voicevox', checkSelector: '.study-card-container, #studyContainer' },
    { url: 'http://localhost:3000/flashcards-match?id=1', name: '27c. Minigame ghép thẻ Flashcard Match', checkSelector: '#matchGameArea, .match-board' },

    // 28. Trò chơi kanji (Mưa chữ Hán)
    { url: 'http://localhost:3000/kanji-game', name: '28. Trò chơi phản xạ Kanji Rain', checkSelector: '#kanjiRainCanvas, .game-container' },

    // 29. Ôn Kana, luyện thư pháp
    { url: 'http://localhost:3000/kana', name: '29a. Đấu trường gõ bảng chữ cái Kana', checkSelector: '#kanaInput, .kana-grid' },
    { url: 'http://localhost:3000/shodo', name: '29b. Luyện viết nét chữ Hán Shodo Canvas', checkSelector: 'canvas' },

    // 30. Luyện viết cùng AI & Đọc hiểu AI
    { url: 'http://localhost:3000/ai-writing', name: '30a. Luyện viết & Chữa câu cùng AI Sensei', checkSelector: '#writingInput' },
    { url: 'http://localhost:3000/reading', name: '30b. Luyện đọc hiểu văn bản Dokkai AI', checkSelector: '#readingInputText' }
  ];

  for (const item of module5Urls) {
    const pageErrors = [];
    page.on('pageerror', err => pageErrors.push(err.message));
    try {
      await page.goto(item.url, { waitUntil: 'load', timeout: 8000 });
      await page.waitForTimeout(500);
      const isCheckVisible = item.checkSelector ? await page.locator(item.checkSelector).isVisible() : true;
      report.module5_LearningSupport.pages.push({
        name: item.name,
        url: item.url,
        title: await page.title(),
        pass: pageErrors.length === 0,
        interactiveElementVisible: isCheckVisible,
        errors: pageErrors
      });
    } catch (e) {
      report.module5_LearningSupport.pages.push({
        name: item.name,
        url: item.url,
        pass: false,
        errors: [e.message]
      });
    }
  }

  console.log('\n================== SUMMARY FOR HOANG MODULES ==================');
  console.log(JSON.stringify(report, null, 2));

  await browser.close();
})();
