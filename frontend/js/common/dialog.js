/**
 * KizunaTutor - KizunaDialog Component
 * Impeccable Retro-Brutalist Dialog for AI Sensei Feedback & Interactive Guidance
 */

export class KizunaDialog {
  static dialogElement = null;

  static init() {
    if (this.dialogElement || typeof document === 'undefined') return;

    const el = document.createElement('div');
    el.id = 'kizunaGlobalDialogOverlay';
    el.className = 'fixed inset-0 z-50 bg-[#0F172A]/60 backdrop-blur-xs flex items-center justify-center p-4 hidden';
    el.innerHTML = `
      <div id="kizunaGlobalDialogContainer" 
           class="w-full max-w-lg bg-white border-2 border-[#0F172A] rounded-[8px] shadow-[4px_4px_0px_0px_#0F172A] p-5 sm:p-6 space-y-4 font-sans select-none relative animate-in fade-in zoom-in duration-150">
        
        <!-- Header với Sensei Avatar & Badge -->
        <div class="flex items-start justify-between gap-3 border-b border-[#0F172A]/20 pb-3">
          <div class="flex items-center gap-3">
            <div id="dialogAvatarBox" class="w-12 h-12 rounded-[6px] border border-[#0F172A] bg-[#FFE5EC] shadow-[2px_2px_0px_0px_#0F172A] flex items-center justify-center text-2xl shrink-0 overflow-hidden">
              <img id="dialogStickerImg" src="/assets/images/stickers/happy-capi.png" alt="Sensei Capi" class="w-10 h-10 object-contain" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <span id="dialogBadge" class="text-[11px] font-bold px-2 py-0.5 rounded-[4px] bg-[#831843] text-white border border-[#0F172A]">Sensei AI</span>
                <span id="dialogSubtitle" class="text-xs text-[#64748B] font-semibold">Cố vấn học tập Kizuna</span>
              </div>
              <h3 id="dialogTitle" class="text-base sm:text-lg font-bold text-[#0F172A] mt-0.5">Lời khuyên từ Sensei</h3>
            </div>
          </div>
          <button type="button" id="dialogBtnCloseX" 
                  class="w-7 h-7 flex items-center justify-center rounded-[4px] border border-[#0F172A] bg-white hover:bg-slate-100 text-[#0F172A] font-bold shadow-[1px_1px_0px_0px_#0F172A] cursor-pointer transition">
            ✕
          </button>
        </div>

        <!-- Body Message & Lịch sử Agent Memory -->
        <div id="dialogBodyContent" class="space-y-3 text-xs sm:text-sm text-[#334155] leading-relaxed">
          <div id="dialogMainMessage" class="p-3 bg-white border border-[#0F172A] rounded-[5px] shadow-[2px_2px_0px_0px_#0F172A] font-medium"></div>
          
          <!-- Box Ngữ Cảnh Trí Nhớ Dài Hạn (Agent Memory Context) -->
          <div id="dialogMemoryContext" class="hidden p-3 bg-[#FFE5EC]/70 border border-[#831843] rounded-[5px] space-y-1.5">
            <div class="flex items-center gap-1.5 text-[11px] font-bold text-[#831843] uppercase tracking-wide">
              <span>🧠</span>
              <span>Trí nhớ học tập Sensei (Agent Memory):</span>
            </div>
            <p id="dialogMemoryText" class="text-xs text-[#0F172A] font-medium leading-normal"></p>
          </div>
        </div>

        <!-- Cụm Nút Hành Động Phản Hồi (Safe Triggers) -->
        <div id="dialogActions" class="flex items-center justify-end gap-2.5 pt-2 border-t border-[#0F172A]/15">
          <button type="button" id="dialogBtnCancel" 
                  class="py-1.5 px-4 bg-white hover:bg-slate-100 text-[#0F172A] text-xs sm:text-sm font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] cursor-pointer transition">
            Đóng
          </button>
          <button type="button" id="dialogBtnConfirm" 
                  class="py-1.5 px-4 bg-[#006194] hover:bg-[#004B73] text-white text-xs sm:text-sm font-bold border border-[#0F172A] rounded-[4px] shadow-[2px_2px_0px_0px_#0F172A] cursor-pointer transition">
            Đồng ý
          </button>
        </div>

      </div>
    `;

    document.body.appendChild(el);
    this.dialogElement = el;

    // Lắng nghe đóng dialog
    el.querySelector('#dialogBtnCloseX').addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      this.close();
    });
    el.querySelector('#dialogBtnCancel').addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      this.close();
    });

    // Nhấn ESC đóng dialog
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && !el.classList.contains('hidden')) {
        this.close();
      }
    });
  }

  static show(options = {}) {
    this.init();
    if (!this.dialogElement) return;

    const {
      title = 'Thông báo từ Sensei AI',
      subtitle = 'Cố vấn học tập Kizuna',
      badge = 'Sensei AI',
      message = '',
      memoryAdvice = '',
      confirmText = 'Đồng ý',
      cancelText = 'Đóng',
      showCancel = true,
      sticker = '/assets/images/stickers/happy-capi.png',
      onConfirm = null,
      onCancel = null
    } = options;

    document.getElementById('dialogTitle').textContent = title;
    document.getElementById('dialogSubtitle').textContent = subtitle;
    document.getElementById('dialogBadge').textContent = badge;
    document.getElementById('dialogMainMessage').innerHTML = message;
    
    const stickerImg = document.getElementById('dialogStickerImg');
    if (stickerImg && sticker) {
      stickerImg.src = sticker;
    }

    const memoryBox = document.getElementById('dialogMemoryContext');
    const memoryText = document.getElementById('dialogMemoryText');
    if (memoryAdvice && memoryAdvice.trim()) {
      memoryText.textContent = memoryAdvice;
      memoryBox.classList.remove('hidden');
    } else {
      memoryBox.classList.add('hidden');
    }

    const btnConfirm = document.getElementById('dialogBtnConfirm');
    const btnCancel = document.getElementById('dialogBtnCancel');

    btnConfirm.textContent = confirmText;
    btnCancel.textContent = cancelText;
    btnCancel.style.display = showCancel ? 'inline-flex' : 'none';

    // Thay thế event listener
    const newConfirm = btnConfirm.cloneNode(true);
    btnConfirm.parentNode.replaceChild(newConfirm, btnConfirm);
    newConfirm.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      this.close();
      if (typeof onConfirm === 'function') onConfirm();
    });

    const newCancel = document.getElementById('dialogBtnCancel');
    newCancel.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      this.close();
      if (typeof onCancel === 'function') onCancel();
    });

    this.dialogElement.classList.remove('hidden');
    document.body.classList.add('overflow-hidden');
  }

  /**
   * Hiển thị nhận xét gợi mở từ Sensei AI kèm phân tích lịch sử rèn luyện
   */
  static showSenseiAdvice(options = {}) {
    const {
      title = 'Nhận Xét & Định Hướng Nét Viết Từ Sensei',
      score = 80,
      comment = '',
      historyAdvice = '',
      char = '',
      onAction = null,
      actionText = 'Tiếp tục luyện tập'
    } = options;

    const sticker = score >= 90 
      ? '/assets/images/stickers/sparkle-capi.png'
      : (score >= 70 ? '/assets/images/stickers/happy-capi.png' : '/assets/images/stickers/thinking-capi.png');

    const scoreColor = score >= 90 ? '#15803D' : (score >= 70 ? '#006194' : '#B91C1C');

    const messageHtml = `
      <div class="space-y-2">
        <div class="flex items-center justify-between">
          <span class="font-bold text-sm text-[#0F172A]">Chữ Hán: <span class="text-lg font-serif text-[#831843]">${char || 'Kanji'}</span></span>
          <span class="px-2.5 py-0.5 rounded-[4px] border border-[#0F172A] font-bold text-xs" style="color: ${scoreColor}; background: #FFFFFF;">
            Điểm: ${score} / 100
          </span>
        </div>
        <p class="text-xs sm:text-sm text-[#0F172A] font-medium leading-relaxed">${comment}</p>
      </div>
    `;

    this.show({
      title,
      badge: 'Chấm Điểm AI',
      message: messageHtml,
      memoryAdvice: historyAdvice,
      confirmText: actionText,
      showCancel: true,
      cancelText: 'Đóng',
      sticker,
      onConfirm: onAction
    });
  }

  static close() {
    if (this.dialogElement) {
      this.dialogElement.classList.add('hidden');
      document.body.classList.remove('overflow-hidden');
    }
  }
}

export default KizunaDialog;

if (typeof window !== 'undefined') {
  window.KizunaDialog = KizunaDialog;
}
