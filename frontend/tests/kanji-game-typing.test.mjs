import { test } from 'node:test';
import assert from 'node:assert/strict';

// Mô phỏng logic chuẩn hóa và chuyển đổi input trong kanji-game.js
function removeAccents(str) {
  if (!str) return '';
  return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D');
}

function katakanaToHiragana(str) {
  if (!str) return '';
  let res = '';
  for (let i = 0; i < str.length; i++) {
    const code = str.charCodeAt(i);
    if (code >= 0x30A1 && code <= 0x30F6) {
      res += String.fromCharCode(code - 0x60);
    } else {
      res += str[i];
    }
  }
  return res;
}

function normalizeInput(str) {
  if (!str) return '';
  const trimmed = str.trim().toLowerCase();
  return katakanaToHiragana(trimmed);
}

test('normalizeInput và removeAccents xử lý sạch gõ tiếng Việt lẫn Romaji', () => {
  assert.equal(normalizeInput('  NICHI  '), 'nichi');
  assert.equal(normalizeInput('ニホン'), 'にほん');
  assert.equal(removeAccents('Nhật Bản'), 'Nhat Ban');
});

test('Cơ chế Commit: Chỉ khi bấm Space/Enter mới kích hoạt lệnh bắn', () => {
  let isShotFired = false;
  let clearedInput = false;

  function handleKeydown(e, isComposing, inputValue) {
    if ((e.key === 'Enter' || e.key === ' ') && !isComposing) {
      e.preventDefault();
      const val = (inputValue || '').trim();
      if (val.length > 0) {
        isShotFired = true;
        clearedInput = true;
      }
    }
  }

  let eventPrevented = false;
  const mockEvent = {
    key: ' ',
    preventDefault: () => { eventPrevented = true; }
  };

  // 1. Khi đang trong phiên gõ IME (isComposing = true) -> KHÔNG bắn
  handleKeydown(mockEvent, true, 'nichi');
  assert.equal(isShotFired, false, 'Khi đang composition IME thì không được bắn');

  // 2. Khi không trong IME và bấm Space -> Kích hoạt bắn và xóa ô
  handleKeydown(mockEvent, false, 'nichi');
  assert.equal(isShotFired, true, 'Khi bấm Space phải kích hoạt bắn');
  assert.equal(eventPrevented, true, 'Phải preventDefault để tránh lọt ký tự space thừa');
  assert.equal(clearedInput, true, 'Ô gõ phải được tự động làm sạch');
});

test('Auto-Clear on Miss: Khi bắn hụt cũng phải tự động làm sạch ô gõ', () => {
  let inputVal = 'sai_roi';
  let combo = 5;
  let shakeError = false;

  function simulateShoot(typed, hasMatch) {
    if (hasMatch) {
      combo++;
      inputVal = '';
    } else {
      combo = 0;
      shakeError = true;
      inputVal = ''; // Tự động làm sạch ô gõ khi bắn trượt
    }
  }

  simulateShoot(inputVal, false);

  assert.equal(combo, 0, 'Reset combo khi bắn hụt');
  assert.equal(shakeError, true, 'Hiển thị hiệu ứng lỗi');
  assert.equal(inputVal, '', 'Ô gõ phải tự động làm sạch về rỗng để người chơi không phải ấn Backspace');
});
