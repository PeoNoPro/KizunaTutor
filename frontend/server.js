const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = Number(process.env.FRONTEND_PORT || 3000);
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
  '/cs': '/pages/cs-dashboard/index.html',
  '/cs/dashboard': '/pages/cs-dashboard/index.html',
  '/manager/reports': '/pages/manager-reports/index.html',
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
  '/tutors.html': '/pages/tutor-discovery/index.html',
  '/tutors': '/pages/tutor-discovery/index.html',
  '/tutor-discovery': '/pages/tutor-discovery/index.html',
  '/tutor-profile': '/pages/tutor-profile/index.html',
  '/connection-requests': '/pages/connection-requests/index.html',
  '/admin/operations/booking-policy': '/pages/operations-config/index.html',
  '/admin/operations/health': '/pages/operations-health/index.html',
  '/admin/operations/incidents': '/pages/operations-incidents/index.html',
  '/admin/operations/reports': '/pages/operations-reports/index.html',
  '/learner/policies': '/pages/policies/index.html',
  '/tutor/policies': '/pages/policies/index.html'
};

const server = http.createServer((req, res) => {
  let cleanUrl = req.url.split('?')[0];
  const queryString = req.url.slice(cleanUrl.length);

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
