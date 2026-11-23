import test from 'node:test';
import assert from 'node:assert/strict';
import { previousPage, pageLabel } from '../js/back-nav.js';

const here = { origin: 'http://localhost:3000', pathname: '/pages/exercise-editor/index.html', search: '?space=s1&exercise=e1' };

test('quay lại đúng trang vừa đứng trong cùng web', () => {
  assert.equal(previousPage('/fallback', { here, referrer: 'http://localhost:3000/tutor/classrooms?view=assignments' }),
    '/tutor/classrooms?view=assignments');
  assert.equal(previousPage('/fallback', { here, referrer: 'http://localhost:3000/tutor/classrooms?space=s1&tab=bai-tap' }),
    '/tutor/classrooms?space=s1&tab=bai-tap');
});

test('không biết trang trước, trang khác web, trang đăng nhập hoặc chính trang này → về trang cha', () => {
  assert.equal(previousPage('/fallback', { here, referrer: '' }), '/fallback');
  assert.equal(previousPage('/fallback', { here, referrer: 'https://google.com/search?q=x' }), '/fallback');
  assert.equal(previousPage('/fallback', { here, referrer: 'http://localhost:3000/login' }), '/fallback');
  assert.equal(previousPage('/fallback', { here, referrer: 'http://localhost:3000/pages/exercise-editor/index.html?space=s1&exercise=e1' }), '/fallback');
  assert.equal(previousPage('/fallback', { here, referrer: 'not a url' }), '/fallback');
});

test('tên trang trên nút "Quay lại …"', () => {
  assert.equal(pageLabel('/tutor/classrooms?view=assignments'), 'Quản lý bài tập');
  assert.equal(pageLabel('/tutor/classrooms?view=materials'), 'Tài liệu học tập');
  assert.equal(pageLabel('/tutor/classrooms?space=s1&tab=bai-tap'), 'Lớp học');
  assert.equal(pageLabel('/tutor/classrooms'), 'Học viên của tôi');
  assert.equal(pageLabel('/pages/my-courses/?view=assignments'), 'Bài tập của tôi');
  assert.equal(pageLabel('/learner/my-courses?space=s1&tab=bai-tap'), 'Lớp học');
  assert.equal(pageLabel('/learner/my-courses'), 'Lớp học của tôi');
  assert.equal(pageLabel('/learner/dashboard'), 'Tổng quan');
  assert.equal(pageLabel('/vocabulary'), 'Trang trước');
});
