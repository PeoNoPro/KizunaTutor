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
  '/tutors.html': '/pages/tutor-discovery/index.html',
  '/tutors': '/pages/tutor-discovery/index.html',
  '/tutor-discovery': '/pages/tutor-discovery/index.html',
  '/tutor-profile': '/pages/tutor-profile/index.html',
  '/connection-requests': '/pages/connection-requests/index.html',
  // Ánh xạ script khi trình duyệt gọi đường dẫn tương đối từ URL không có dấu gạch chéo
  '/tutor-discovery.js': '/pages/tutor-discovery/tutor-discovery.js',
  '/home.js': '/pages/home/home.js',
  '/tutor-profile.js': '/pages/tutor-profile/tutor-profile.js',
  '/connection-requests.js': '/pages/connection-requests/connection-requests.js'
};

const server = http.createServer((req, res) => {
  let cleanUrl = req.url.split('?')[0];

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
  } else if (URL_ALIASES[cleanUrl]) {
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
});
