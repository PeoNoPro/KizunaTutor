/**
 * KizunaTutor - FE-06 Khối nhập địa chỉ học trực tiếp: Tỉnh/Thành phố → Phường/Xã (gõ để tìm) → địa chỉ cụ thể.
 * Dùng chung cho form "Tạo lớp học" và hộp "Sửa hình thức học". Danh mục 34 tỉnh / 3.321 phường xã (sau 01/07/2025)
 * được tải động lần đầu dùng.
 */

import { composeAddress, parseAddress, plainText, validateAddressParts } from './address-helpers.js';

let dataPromise = null;
function loadProvinces() {
  if (!dataPromise) {
    dataPromise = import('./vn-address-data.js').then((m) => m.VN_PROVINCES);
    dataPromise.catch(() => { dataPromise = null; });
  }
  return dataPromise;
}

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/**
 * @param {HTMLElement} container
 * @param {{idPrefix: string}} opts tiền tố id để form có nhiều khối không trùng id
 * @returns {{ready: Promise<void>, setValue(text: string): Promise<{recognized: boolean}>, read(): {value: string, errors: object}, showErrors(errors: object): void}}
 */
export function mountAddressPicker(container, { idPrefix }) {
  const id = (s) => `${idPrefix}-${s}`;
  container.innerHTML = `
    <div class="grid gap-4 sm:grid-cols-2">
      <div>
        <label for="${id('province')}" class="kz-label">Tỉnh / Thành phố <span class="text-error">*</span></label>
        <select id="${id('province')}" class="kz-input" disabled><option value="">Đang tải danh sách…</option></select>
        <p data-err="province" class="field-error hidden"></p>
      </div>
      <div>
        <label for="${id('ward')}" class="kz-label">Phường / Xã <span class="text-error">*</span></label>
        <input id="${id('ward')}" type="text" class="kz-input" list="${id('wards')}" autocomplete="off" disabled
          placeholder="Chọn tỉnh trước" aria-describedby="${id('wardHint')}">
        <datalist id="${id('wards')}"></datalist>
        <p id="${id('wardHint')}" class="font-caption text-caption text-on-surface-variant mt-1"></p>
        <p data-err="ward" class="field-error hidden"></p>
      </div>
    </div>
    <div class="mt-4">
      <label for="${id('detail')}" class="kz-label">Địa chỉ cụ thể <span class="text-error">*</span></label>
      <input id="${id('detail')}" type="text" maxlength="300" class="kz-input" placeholder="Số nhà, ngõ/ngách, tên đường, tòa nhà…">
      <p data-err="detail" class="field-error hidden"></p>
    </div>
    <p data-note class="hidden mt-3 flex items-start gap-2 rounded-xl bg-tertiary-fixed text-on-tertiary-fixed-variant px-4 py-3 font-body-sm text-body-sm"></p>
    <div data-preview class="hidden mt-3 flex items-start gap-2.5 rounded-xl bg-surface-container-low px-4 py-3">
      <span class="material-symbols-outlined text-[20px] text-primary" aria-hidden="true">location_on</span>
      <div class="min-w-0"><p class="font-caption text-caption text-on-surface-variant">Học viên sẽ thấy địa chỉ:</p>
        <p data-preview-text class="font-semibold text-on-surface [overflow-wrap:anywhere]"></p></div>
    </div>`;

  const provinceEl = container.querySelector(`#${id('province')}`);
  const wardEl = container.querySelector(`#${id('ward')}`);
  const wardList = container.querySelector(`#${id('wards')}`);
  const wardHint = container.querySelector(`#${id('wardHint')}`);
  const detailEl = container.querySelector(`#${id('detail')}`);
  const note = container.querySelector('[data-note]');
  const preview = container.querySelector('[data-preview]');
  let provinces = [];

  const currentProvince = () => provinces.find((p) => String(p[0]) === provinceEl.value) || null;
  const currentWard = () => {
    const p = currentProvince();
    const v = plainText(wardEl.value);
    return p && v ? p[2].find((w) => plainText(w[1]) === v) || null : null;
  };

  function fillWards() {
    const p = currentProvince();
    wardList.innerHTML = p ? p[2].map((w) => `<option value="${esc(w[1])}"></option>`).join('') : '';
    wardEl.disabled = !p;
    wardEl.placeholder = p ? 'Gõ để tìm, ví dụ: Hai Bà Trưng' : 'Chọn tỉnh trước';
    wardHint.textContent = p ? `${p[2].length} phường / xã của ${p[1]}` : '';
  }

  function updatePreview() {
    const p = currentProvince();
    const w = currentWard();
    const text = composeAddress({ detail: detailEl.value, ward: w?.[1], province: p?.[1] });
    preview.classList.toggle('hidden', !(p && w && detailEl.value.trim()));
    preview.querySelector('[data-preview-text]').textContent = text;
  }

  const ready = loadProvinces().then((list) => {
    provinces = list;
    provinceEl.innerHTML = '<option value="">Chọn tỉnh / thành phố</option>'
      + list.map((p) => `<option value="${p[0]}">${esc(p[1])}</option>`).join('');
    provinceEl.disabled = false;
  }).catch(() => {
    provinceEl.innerHTML = '<option value="">Không tải được danh sách, vui lòng tải lại trang</option>';
  });

  provinceEl.addEventListener('change', () => {
    wardEl.value = '';
    fillWards();
    updatePreview();
  });
  wardEl.addEventListener('input', updatePreview);
  wardEl.addEventListener('change', updatePreview);
  detailEl.addEventListener('input', updatePreview);

  function showErrors(errors = {}) {
    container.querySelectorAll('[data-err]').forEach((el) => {
      const msg = errors[el.dataset.err] || '';
      el.textContent = msg;
      el.classList.toggle('hidden', !msg);
    });
  }

  return {
    ready,
    /** Điền lại từ chuỗi đã lưu / địa chỉ học viên gõ; không nhận ra tỉnh + phường thì đưa cả chuỗi vào ô địa chỉ cụ thể. */
    async setValue(text, { sourceNote = '' } = {}) {
      await ready;
      const parsed = parseAddress(text, provinces);
      provinceEl.value = parsed.provinceCode ? String(parsed.provinceCode) : '';
      fillWards();
      const p = currentProvince();
      const w = p && parsed.wardCode ? p[2].find((x) => x[0] === parsed.wardCode) : null;
      wardEl.value = w ? w[1] : '';
      detailEl.value = parsed.detail || '';
      const unclear = String(text || '').trim() && !parsed.recognized;
      note.classList.toggle('hidden', !unclear);
      if (unclear) {
        note.innerHTML = `<span class="material-symbols-outlined text-[18px]" aria-hidden="true">edit_location_alt</span>
          <span>${esc(sourceNote || 'Địa chỉ cũ')}: “${esc(text)}”. Hãy chọn lại tỉnh và phường cho đúng.</span>`;
      }
      showErrors({});
      updatePreview();
      return { recognized: parsed.recognized };
    },
    read() {
      const p = currentProvince();
      const w = currentWard();
      const errors = validateAddressParts({ provinceCode: p?.[0], wardCode: w?.[0], detail: detailEl.value });
      return { value: composeAddress({ detail: detailEl.value, ward: w?.[1], province: p?.[1] }), errors };
    },
    showErrors
  };
}
