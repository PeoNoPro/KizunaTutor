const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.mjs': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf'
};

const URL_ALIASES = {
  '/home.html': '/pages/home/index.html',
  '/home': '/pages/home/index.html',
  '/login.html': '/pages/login/index.html',
  '/login': '/pages/login/index.html',
  '/register.html': '/pages/register/index.html',
  '/register': '/pages/register/index.html',
  '/oauth2/redirect': '/pages/login/index.html',
  '/tutors.html': '/pages/tutor-discovery/index.html',
  '/tutors': '/pages/tutor-discovery/index.html',
  '/tutor-discovery': '/pages/tutor-discovery/index.html',
  '/tutor-profile': '/pages/tutor-profile/index.html',
  '/connection-requests': '/pages/connection-requests/index.html',
  '/kana-quiz': '/pages/kana-quiz/index.html',
  '/kana': '/pages/kana-quiz/index.html',
  '/kana-quiz.html': '/pages/kana-quiz/index.html',
  '/shodo': '/pages/shodo-practice/index.html',
  '/shodo-practice': '/pages/shodo-practice/index.html',
  '/learner/practice-canvas': '/pages/shodo-practice/index.html',
  '/practice': '/pages/shodo-practice/index.html',
  '/kanji-practice': '/pages/shodo-practice/index.html',
  '/flashcards': '/pages/flashcards/index.html',
  '/flashcards.html': '/pages/flashcards/index.html',
  '/flashcards-study': '/pages/flashcards-study/index.html',
  '/flashcards-study.html': '/pages/flashcards-study/index.html',
  '/flashcards-practice': '/pages/flashcards-study/index.html',
  '/learner/flashcards-study': '/pages/flashcards-study/index.html',
  '/flashcards-detail': '/pages/flashcards-detail/index.html',
  '/learner/flashcards-detail': '/pages/flashcards-detail/index.html',
  '/decks': '/pages/flashcards/index.html',
  '/grammar': '/pages/grammar/index.html',
  '/grammar.html': '/pages/grammar/index.html',
  '/openjlpt': '/pages/grammar/index.html',
  '/ai-writing': '/pages/ai-writing/index.html',
  '/ai-writing.html': '/pages/ai-writing/index.html',
  '/sakubun': '/pages/ai-writing/index.html',
  '/handwriting': '/pages/handwriting-search/index.html',
  '/handwriting-search': '/pages/handwriting-search/index.html',
  '/learner/handwriting-search': '/pages/handwriting-search/index.html',
  '/ohr': '/pages/handwriting-search/index.html',
  '/kanji-dictionary': '/pages/kanji-dictionary/index.html',
  '/kanji-dictionary.html': '/pages/kanji-dictionary/index.html',
  '/learner/dictionary': '/pages/kanji-dictionary/index.html',
  '/dictionary': '/pages/kanji-dictionary/index.html',
  '/kanji': '/pages/kanji-dictionary/index.html',
  '/kanji-game': '/pages/kanji-game/index.html',
  '/kanji-game.html': '/pages/kanji-game/index.html',
  '/game': '/pages/kanji-game/index.html',
  '/rain': '/pages/kanji-game/index.html',
  '/learner/dashboard': '/pages/learner-dashboard/index.html',
  '/learner/dashboard/': '/pages/learner-dashboard/index.html',
  '/learner-dashboard': '/pages/learner-dashboard/index.html',
  '/learner': '/pages/learner-dashboard/index.html',
  '/learner/': '/pages/learner-dashboard/index.html',
  '/dashboard': '/pages/learner-dashboard/index.html',
  '/dashboard/': '/pages/learner-dashboard/index.html'
};

const server = http.createServer((req, res) => {
  // Proxy API requests tới Spring Boot backend (port 8080)
  if (req.url.startsWith('/api/')) {
    const backendPort = 8080;
    const options = {
      hostname: '127.0.0.1',
      port: backendPort,
      path: req.url,
      method: req.method,
      headers: {
        ...req.headers,
        host: `127.0.0.1:${backendPort}`
      }
    };
    const proxyReq = http.request(options, (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res, { end: true });
    });
    proxyReq.on('error', () => {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Backend unreachable on port ' + backendPort }));
    });
    req.pipe(proxyReq, { end: true });
    return;
  }

  let cleanUrl = req.url.split('?')[0];

  // Ánh xạ alias URL thân thiện / tương thích ngược
  if (URL_ALIASES[cleanUrl]) {
    cleanUrl = URL_ALIASES[cleanUrl];
  }

  let filePath = path.join(__dirname, cleanUrl);

  // Xử lý truy cập thư mục -> nạp index.html
  if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
    const indexPath = path.join(filePath, 'index.html');
    if (fs.existsSync(indexPath)) {
      filePath = indexPath;
    }
  }

  // Fallback nếu không tìm thấy file
  if (!fs.existsSync(filePath)) {
    filePath = path.join(__dirname, 'index.html');
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500, { 'Content-Type': 'text/plain; charset=UTF-8' });
      res.end('Lỗi máy chủ nội bộ');
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    }
  });
});

server.listen(PORT, () => {
  console.log(`Frontend server đang chạy tại: http://localhost:${PORT}/`);
});
