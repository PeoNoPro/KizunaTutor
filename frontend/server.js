const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const BACKEND_PORT = process.env.BACKEND_PORT || 8085;
const AGENT_MEMORY_PORT = process.env.AGENT_MEMORY_PORT || 8081;

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
  '.ttf': 'font/ttf',
  '.pdf': 'application/pdf'
};

const URL_ALIASES = {
  '/cs': '/pages/cs-dashboard/index.html',
  '/cs/dashboard': '/pages/cs-dashboard/index.html',
  '/manager/reports': '/pages/manager-reports/index.html',
  '/manager/dashboard': '/pages/manager-dashboard/index.html',
  '/admin/profile': '/pages/admin-profile/index.html',
  '/': '/pages/home/index.html',
  '/admin/operations/logs': '/pages/operations-logs/index.html',
  '/admin/staff': '/pages/admin-staff/index.html',
  '/admin/roles': '/pages/admin-roles/index.html',
  '/admin': '/pages/admin-dashboard/index.html',
  '/admin/dashboard': '/pages/admin-dashboard/index.html',
  '/admin/operations': '/pages/admin-dashboard/index.html',
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
  '/tutor/connections': '/pages/tutor-connections/index.html',
  '/tutor/connections/': '/pages/tutor-connections/index.html',
  '/tutor/schedule': '/pages/tutor-schedule/index.html',
  '/tutor/schedule/': '/pages/tutor-schedule/index.html',
  '/tutor/profile': '/pages/tutor-teaching-profile/index.html',
  '/tutor/profile/': '/pages/tutor-teaching-profile/index.html',
  '/learner/connections': '/pages/learner-connections/index.html',
  '/learner/connections/': '/pages/learner-connections/index.html',
  '/learner/bookings': '/pages/learner-connections/index.html',
  '/learner/requests': '/pages/learner-requests/index.html',
  '/learner/requests/': '/pages/learner-requests/index.html',
  '/admin/operations/booking-policy': '/pages/operations-config/index.html',
  '/admin/operations/incidents': '/pages/operations-incidents/index.html',
  '/admin/operations/reports': '/pages/operations-reports/index.html',
  '/learner/policies': '/pages/policies/index.html',
  '/tutor/policies': '/pages/policies/index.html',
  '/tutor-discovery.js': '/pages/tutor-discovery/tutor-discovery.js',
  '/home.js': '/pages/home/home.js',
  '/tutor-profile.js': '/pages/tutor-profile/tutor-profile.js',
  '/connection-requests.js': '/pages/connection-requests/connection-requests.js',
  '/tutor-connections.js': '/pages/tutor-connections/tutor-connections.js',
  '/tutor-schedule.js': '/pages/tutor-schedule/tutor-schedule.js',
  '/learner-connections.js': '/pages/learner-connections/learner-connections.js',
  '/learner/tutors': '/pages/tutor-discovery/index.html',
  '/kana-quiz': '/pages/kana-quiz/index.html',
  '/kana': '/pages/kana-quiz/index.html',
  '/kana-quiz.html': '/pages/kana-quiz/index.html',
  '/learner/kana': '/pages/kana-quiz/index.html',
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
  '/learner/deck-detail': '/pages/flashcards-detail/index.html',
  '/decks': '/pages/flashcards/index.html',
  '/learner/flashcards': '/pages/flashcards/index.html',
  '/learner/decks': '/pages/flashcards/index.html',
  '/learner/flashcards-match': '/pages/flashcards-match/index.html',
  '/flashcards-match': '/pages/flashcards-match/index.html',
  '/flashcards-match.html': '/pages/flashcards-match/index.html',
  '/match': '/pages/flashcards-match/index.html',
  '/learner/match': '/pages/flashcards-match/index.html',
  '/grammar': '/pages/grammar/index.html',
  '/grammar.html': '/pages/grammar/index.html',
  '/learner/grammar': '/pages/grammar/index.html',
  '/learner/grammar-detail': '/pages/grammar/index.html',
  '/grammar-detail': '/pages/grammar/index.html',
  '/openjlpt': '/pages/grammar/index.html',
  '/ai-writing': '/pages/ai-writing/index.html',
  '/ai-writing.html': '/pages/ai-writing/index.html',
  '/learner/ai-writing': '/pages/ai-writing/index.html',
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
  '/learning-support': '/pages/learning-support/index.html',
  '/vocabulary': '/pages/learning-support/index.html',
  '/vocabulary.html': '/pages/learning-support/index.html',
  '/vocab': '/pages/learning-support/index.html',
  '/learner/vocabulary': '/pages/learning-support/index.html',
  '/learner/learning-support': '/pages/learning-support/index.html',
  '/learner/dashboard': '/pages/learner-dashboard/index.html',
  '/learner/dashboard/': '/pages/learner-dashboard/index.html',
  '/learner-dashboard': '/pages/learner-dashboard/index.html',
  '/learner': '/pages/learner-dashboard/index.html',
  '/learner/': '/pages/learner-dashboard/index.html',
  '/dashboard': '/pages/learner-dashboard/index.html',
  '/dashboard/': '/pages/learner-dashboard/index.html',
  // FE-06 Lớp học & tài liệu học tập
  '/my-courses': '/pages/my-courses/index.html',
  '/learner/my-courses': '/pages/my-courses/index.html',
  '/tutor/classrooms': '/pages/tutor-classrooms/index.html',
  '/tutor/classrooms/': '/pages/tutor-classrooms/index.html',
  '/tutor/verification': '/pages/tutor-verification/index.html',
  '/tutor/verification/': '/pages/tutor-verification/index.html',
  '/tutor/dashboard': '/pages/tutor-dashboard/index.html',
  '/tutor/dashboard/': '/pages/tutor-dashboard/index.html',
  '/tutor': '/pages/tutor-dashboard/index.html',
  '/academic': '/pages/academic-verifications/index.html',
  '/academic/dashboard': '/pages/academic-verifications/index.html',
  '/academic/dashboard/': '/pages/academic-verifications/index.html',
  '/academic/verifications': '/pages/academic-verifications/index.html',
  '/academic/verifications/': '/pages/academic-verifications/index.html',
  '/academic/verifications/detail': '/pages/academic-verification-detail/index.html',
  '/academic/posts': '/pages/academic-posts/index.html',
  '/academic/posts/': '/pages/academic-posts/index.html',
  '/tutor/post': '/pages/tutor-post/index.html',
  '/tutor/post/': '/pages/tutor-post/index.html',
  '/academic/history': '/pages/academic-review-history/index.html',
  '/academic/history/': '/pages/academic-review-history/index.html',
  '/academic/profile': '/pages/academic-profile/index.html',
  '/academic/profile/': '/pages/academic-profile/index.html',
  '/academic/content-import': '/pages/academic-content-import/index.html',
  '/academic/content-import/': '/pages/academic-content-import/index.html',
  // FE-09: gói hội viên và thanh toán
  '/tutor/membership': '/pages/tutor-membership/index.html',
  '/tutor/membership/': '/pages/tutor-membership/index.html',
  '/tutor/membership/result': '/pages/tutor-membership-result/index.html',
  '/manager/membership-plans': '/pages/manager-membership-plans/index.html',
  '/manager/tutor-memberships': '/pages/manager-tutor-memberships/index.html',
  '/manager/users': '/pages/manager-users/index.html',
  '/manager/users/': '/pages/manager-users/index.html',
  '/admin/payments': '/pages/admin-payments/index.html'
};

const server = http.createServer((req, res) => {
  // 1. Proxy TencentDB Agent Memory Microservice (port 8081)
  if (req.url.startsWith('/memory/') || req.url.startsWith('/agent-memory/')) {
    const memoryPath = req.url.startsWith('/agent-memory/')
      ? req.url.replace('/agent-memory', '')
      : req.url;
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => {
      const bodyBuffer = Buffer.concat(chunks);
      const forwardMemory = (targetPort, onErrorFallback) => {
        const opts = {
          hostname: '127.0.0.1',
          port: targetPort,
          path: memoryPath,
          method: req.method,
          headers: { ...req.headers, host: `127.0.0.1:${targetPort}` }
        };
        const pReq = http.request(opts, (pRes) => {
          res.writeHead(pRes.statusCode, pRes.headers);
          pRes.pipe(res, { end: true });
        });
        pReq.on('error', onErrorFallback);
        if (bodyBuffer.length > 0) pReq.write(bodyBuffer);
        pReq.end();
      };

      // Thử microservice 8081 trước; nếu chưa chạy thì fallback về Spring Boot Backend 8085
      forwardMemory(AGENT_MEMORY_PORT, () => {
        forwardMemory(BACKEND_PORT, () => {
          res.writeHead(502, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            status: 502,
            success: false,
            message: `Agent Memory service unreachable on ports ${AGENT_MEMORY_PORT} and ${BACKEND_PORT}`,
            data: null
          }));
        });
      });
    });
    return;
  }

  // 2. Proxy REST API & Google OAuth2 requests tới Spring Boot backend (port 8085 / 8080)
  // 2. Proxy REST API & Google OAuth2 requests tới Spring Boot backend (port 8085 / 8080)
  if (req.url.startsWith('/api/') || req.url.startsWith('/oauth2/') || req.url.startsWith('/login/oauth2/')) {
    const options = {
      hostname: '127.0.0.1',
      port: BACKEND_PORT,
      path: req.url,
      method: req.method,
      headers: {
        ...req.headers,
        host: `127.0.0.1:${BACKEND_PORT}`
      }
    };
    const proxyReq = http.request(options, (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res, { end: true });
    });
    proxyReq.on('error', () => {
      // Thử fallback sang cổng 8080 nếu 8085 chưa phản hồi
      const fallbackOptions = { ...options, port: 8080, headers: { ...req.headers, host: '127.0.0.1:8080' } };
      const fallbackReq = http.request(fallbackOptions, (fallbackRes) => {
        res.writeHead(fallbackRes.statusCode, fallbackRes.headers);
        fallbackRes.pipe(res, { end: true });
      });
      fallbackReq.on('error', () => {
        res.writeHead(502, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          status: 502,
          success: false,
          message: `Backend unreachable on ports ${BACKEND_PORT} and 8080`,
          data: null
        }));
      });
      req.pipe(fallbackReq, { end: true });
    });
    req.pipe(proxyReq, { end: true });
    return;
  }

  let cleanUrl = req.url.split('?')[0];
  const queryString = req.url.slice(cleanUrl.length);

  // Hỗ trợ route hồ sơ gia sư dạng rút gọn /gs/:tutorCode và các file tĩnh gọi từ trang này
  if (cleanUrl.startsWith('/gs/')) {
    if (cleanUrl.endsWith('.js')) {
      const scriptName = path.basename(cleanUrl);
      cleanUrl = `/pages/tutor-profile/${scriptName}`;
    } else if (cleanUrl.endsWith('.css')) {
      const cssName = path.basename(cleanUrl);
      cleanUrl = `/pages/tutor-profile/${cssName}`;
    } else if (/^\/gs\/\d+$/.test(cleanUrl) || cleanUrl === '/gs' || cleanUrl === '/gs/') {
      cleanUrl = '/pages/tutor-profile/index.html';
    }
  }
  const supportRoute = cleanUrl.match(/^\/(learner|tutor|cs|manager)\/support\/tickets(?:\/([0-9a-fA-F-]{36}))?\/?$/);
  if (supportRoute) {
    const params = new URLSearchParams(queryString);
    params.set('role', supportRoute[1]);
    if (supportRoute[2]) params.set('id', supportRoute[2]);
    else params.delete('id');
    res.writeHead(302, {Location: `/pages/${supportRoute[2] ? 'support-ticket' : 'support-tickets'}/?${params}`});
    res.end();
    return;
  }

  // Ánh xạ alias URL thân thiện / tương thích ngược: chuyển hướng về thư mục trang thật
  // để đường dẫn tương đối như ./ten-trang.js trong index.html tìm đúng file.
  if (URL_ALIASES[cleanUrl]) {
    res.writeHead(302, { Location: URL_ALIASES[cleanUrl].replace(/index\.html$/, '') + queryString });
    res.end();
    return;
  }

  let filePath = path.join(__dirname, cleanUrl);

  // Xử lý truy cập thư mục -> nạp index.html (thiếu dấu / ở cuối thì chuyển hướng, cùng lý do trên)
  if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
    if (!cleanUrl.endsWith('/')) {
      res.writeHead(302, { Location: cleanUrl + '/' + queryString });
      res.end();
      return;
    }
    const indexPath = path.join(filePath, 'index.html');
    if (fs.existsSync(indexPath)) {
      filePath = indexPath;
    }
  }

  const ext = path.extname(filePath).toLowerCase();

  // Tránh trả về text/html cho file JS / CSS khi không tìm thấy
  if (!fs.existsSync(filePath)) {
    if (ext && ext !== '.html') {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=UTF-8' });
      res.end(`404 Không tìm thấy tài nguyên: ${cleanUrl}`);
      return;
    }
    filePath = path.join(__dirname, 'index.html');
  }

  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500, { 'Content-Type': 'text/plain; charset=UTF-8' });
      res.end(`Lỗi máy chủ: ${err.code}`);
      return;
    }
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(content);
  });
});

server.listen(PORT, () => {
  console.log(`[Frontend] Máy chủ dev đang chạy tại: http://localhost:${PORT}`);
  console.log(`[Frontend] Trang tìm gia sư: http://localhost:${PORT}/pages/tutor-discovery/`);
  console.log(`[Frontend] Xem hồ sơ ngắn: http://localhost:${PORT}/gs/10001`);
  console.log(`Backend proxy trỏ tới cổng: ${BACKEND_PORT}`);
  console.log(`Agent Memory proxy trỏ tới cổng: ${AGENT_MEMORY_PORT}`);
});
