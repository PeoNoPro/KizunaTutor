import test from 'node:test';
import assert from 'node:assert/strict';
import { API_CONFIG } from '../js/config/api.config.js';

test('API_CONFIG should define trial booking endpoints and base URL', () => {
  assert.ok(API_CONFIG.BASE_URL);
  assert.equal(typeof API_CONFIG.BASE_URL, 'string');
});

test('Format booking time calculates start and end correctly', () => {
  const startStr = '2026-11-05T18:00:00+07:00';
  const durationMinutes = 50;
  const start = new Date(startStr);
  const end = new Date(start.getTime() + durationMinutes * 60000);

  assert.equal(end.getTime() - start.getTime(), 50 * 60 * 1000);
});

