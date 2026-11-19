import { test } from 'node:test';
import assert from 'node:assert/strict';
import { brandLogoHtml } from '../components/brand-logo/brand-logo.js';
import { isEligibleForSpaNavigation } from '../js/sidebarRouter.js';

function mockLink(attrs = {}) {
  return {
    getAttribute: (name) => (name in attrs ? attrs[name] : null),
    hasAttribute: (name) => name in attrs,
    closest: (selector) => {
      if (selector === '[data-no-spa]' && attrs['data-no-spa'] !== undefined) {
        return true;
      }
      return null;
    },
    target: attrs.target || '',
    href: attrs.href || ''
  };
}

test('brandLogoHtml chứa thuộc tính data-no-spa và href chuẩn', () => {
  const html = brandLogoHtml({ href: '/home', subtitle: 'Không gian học viên' });
  assert.ok(html.includes('data-no-spa'), 'Logo phải có thuộc tính data-no-spa');
  assert.ok(html.includes('href="/home"'), 'Logo mặc định dẫn về /home');
  assert.ok(html.includes('Không gian học viên'), 'Hiển thị đúng subtitle');
});

test('isEligibleForSpaNavigation loại trừ link về trang chủ và link có data-no-spa', () => {
  const homeLink = mockLink({ href: '/home', 'data-no-spa': '' });
  assert.equal(isEligibleForSpaNavigation(homeLink), false, 'Không được chặn link /home');

  const rootLink = mockLink({ href: '/' });
  assert.equal(isEligibleForSpaNavigation(rootLink), false, 'Không được chặn link /');

  const homePageLink = mockLink({ href: '/pages/home/index.html' });
  assert.equal(isEligibleForSpaNavigation(homePageLink), false, 'Không được chặn link /pages/home/');
});

test('isEligibleForSpaNavigation loại trừ các link không thuộc danh mục menu SPA học viên', () => {
  // Thẻ link không có data-menu-key và không có data-spa-link (như logo thương hiệu hoặc link tự do)
  const logoLinkWithoutNoSpa = mockLink({ href: '/home' });
  assert.equal(isEligibleForSpaNavigation(logoLinkWithoutNoSpa), false);

  // Link hồ sơ trong menu tài khoản không có data-spa-link
  const userMenuHomeLink = mockLink({ href: '/home', 'data-no-spa': '' });
  assert.equal(isEligibleForSpaNavigation(userMenuHomeLink), false);
});

test('isEligibleForSpaNavigation chấp nhận link điều hướng học viên hợp lệ', () => {
  const kanjiLink = mockLink({ href: '/kanji-dictionary', 'data-menu-key': 'kanji-dictionary', 'data-spa-link': '' });
  assert.equal(isEligibleForSpaNavigation(kanjiLink), true, 'Menu tra cứu Hán tự phải được SPA điều hướng');

  const grammarLink = mockLink({ href: '/grammar', 'data-menu-key': 'grammar', 'data-spa-link': '' });
  assert.equal(isEligibleForSpaNavigation(grammarLink), true, 'Menu ngữ pháp phải được SPA điều hướng');
});

test('isEligibleForSpaNavigation loại trừ các link portal độc lập hoặc trang ngoài', () => {
  const tutorDiscovery = mockLink({ href: '/pages/tutor-discovery/', 'data-menu-key': 'tutors', 'data-no-spa': '' });
  assert.equal(isEligibleForSpaNavigation(tutorDiscovery), false);

  const loginLink = mockLink({ href: '/login', 'data-menu-key': 'login' });
  assert.equal(isEligibleForSpaNavigation(loginLink), false);

  const externalLink = mockLink({ href: 'https://google.com', 'data-spa-link': '' });
  assert.equal(isEligibleForSpaNavigation(externalLink), false);
});

