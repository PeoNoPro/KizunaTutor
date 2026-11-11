import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { LOCKED_PAGE_DIRS, isLockedToolPath, normalizePath } from '../js/learning-tools-paths.js';

const here = dirname(fileURLToPath(import.meta.url));
const serverSource = readFileSync(join(here, '..', 'server.js'), 'utf8');

/** Đọc bảng định tuyến "'/route': '/pages/<dir>/index.html'" trong server.js. */
function routesTo(dir) {
  const pattern = new RegExp(`'([^']+)':\\s*'/pages/${dir}/index\\.html'`, 'g');
  return [...serverSource.matchAll(pattern)].map((m) => m[1]);
}

test('normalizePath bỏ query, index.html và dấu / ở cuối', () => {
  assert.equal(normalizePath('/pages/flashcards/index.html'), '/pages/flashcards');
  assert.equal(normalizePath('/grammar/?id=1#top'), '/grammar');
  assert.equal(normalizePath(''), '/');
});

test('mọi alias trong server.js của trang công cụ bị khóa đều bị chặn', () => {
  for (const dir of LOCKED_PAGE_DIRS) {
    const routes = routesTo(dir);
    assert.ok(routes.length > 0, `server.js không có route nào tới pages/${dir}`);
    for (const route of routes) {
      assert.ok(isLockedToolPath(route), `Thiếu alias "${route}" (pages/${dir}) trong LOCKED_PATHS`);
    }
    assert.ok(isLockedToolPath(`/pages/${dir}/`), `Thiếu đường dẫn thật /pages/${dir}/`);
  }
});

test('Tra cứu chữ Hán, viết tay và Kana luôn mở', () => {
  for (const dir of ['kanji-dictionary', 'handwriting-search', 'kana-quiz']) {
    for (const route of routesTo(dir)) {
      assert.equal(isLockedToolPath(route), false, `"${route}" (pages/${dir}) không được bị khóa`);
    }
  }
  assert.equal(isLockedToolPath('/learner/dashboard'), false);
});
