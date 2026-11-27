import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveApiBaseUrl, resolveMemoryUrl } from '../js/config/urlResolver.js';

test('resolves proxied relative URLs on HTTPS default port', () => {
  const loc = { protocol: 'https:', hostname: 'kizuna.example.com', port: '' };
  assert.equal(resolveApiBaseUrl(loc), '/api');
  assert.equal(resolveMemoryUrl(loc), '/memory');
});

test('resolves proxied relative URLs on localhost:3000', () => {
  const loc = { protocol: 'http:', hostname: 'localhost', port: '3000' };
  assert.equal(resolveApiBaseUrl(loc), '/api');
  assert.equal(resolveMemoryUrl(loc), '/memory');
});

test('resolves direct localhost backend URLs on non-3000 local dev server', () => {
  const loc = { protocol: 'http:', hostname: 'localhost', port: '5500' };
  assert.equal(resolveApiBaseUrl(loc), 'http://localhost:8085/api');
  assert.equal(resolveMemoryUrl(loc), 'http://localhost:8081/memory');
});

test('resolves direct localhost backend URLs on file:// protocol', () => {
  const loc = { protocol: 'file:', hostname: '', port: '' };
  assert.equal(resolveApiBaseUrl(loc), 'http://localhost:8085/api');
  assert.equal(resolveMemoryUrl(loc), 'http://localhost:8081/memory');
});
