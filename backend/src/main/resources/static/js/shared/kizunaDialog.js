/**
 * ============================================================================
 * KIZUNATUTOR - GLOBAL INTERACTIVE DIALOG / MODAL PANEL MODULE
 * File: kizunaDialog.js
 * Quản lý tập trung mọi thông báo nổi, nhận xét AI Sensei và phản hồi hệ thống.
 * Thiết kế chuẩn Retro-Brutalist: viền đen 2px, đổ bóng khối cứng 6px 6px.
 * ============================================================================
 */

(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.KizunaDialog = factory();
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    let currentOnCloseCallback = null;
    let allowBackdropClose = true;
    let allowEscapeClose = true;

    /**
     * Tự động khởi tạo cấu trúc DOM của Modal vào body nếu chưa tồn tại
     */
    function ensureDialogDOM() {
        if (document.getElementById('kizunaGlobalDialog')) return;

        const dialogHTML = `
            <div id="kizunaGlobalDialog" 
                 role="dialog" 
                 aria-modal="true" 
                 class="fixed inset-0 z-[9999] hidden items-center justify-center p-3 sm:p-4 bg-black/45 backdrop-blur-[2px] transition-opacity duration-150">
                <div id="kizunaDialogCard" 
                     class="relative w-full max-w-md sm:max-w-lg bg-white border-2 border-[#0F172A] rounded-[6px] shadow-[6px_6px_0px_0px_#0F172A] p-4 sm:p-5 box-border transform transition-all duration-150 scale-95 overflow-hidden">
                    
                    <!-- Header -->
                    <div class="flex items-center justify-between pb-2.5 border-b border-[#0F172A]/15">
                        <div class="flex items-center gap-2 min-w-0">
                            <span id="kizunaDialogIcon" class="text-xl sm:text-2xl shrink-0 select-none">🤖</span>
                            <h3 id="kizunaDialogTitle" class="font-itim text-base sm:text-lg font-bold text-[#0F172A] leading-none truncate">
                                Thông Báo Hệ Thống
                            </h3>
                        </div>
                        <button type="button" 
                                id="kizunaDialogCloseBtn" 
                                aria-label="Đóng bảng thông báo" 
                                class="w-7 h-7 shrink-0 flex items-center justify-center border border-[#0F172A] rounded-[4px] bg-slate-100 hover:bg-rose-100 text-[#0F172A] font-bold text-xs shadow-[2px_2px_0px_0px_#0F172A] active:translate-x-[1px] active:translate-y-[1px] active:shadow-none transition cursor-pointer">
                            ✕
                        </button>
                    </div>

                    <!-- Body -->
                    <div id="kizunaDialogBody" 
                         class="py-3 font-sans text-xs sm:text-sm text-slate-800 leading-relaxed max-h-[62vh] overflow-y-auto pr-1">
                        <!-- Nội dung phản hồi động -->
                    </div>

                    <!-- Footer Actions -->
                    <div id="kizunaDialogActions" 
                         class="pt-2.5 border-t border-[#0F172A]/15 flex items-center justify-end gap-2 flex-wrap">
                        <!-- Nút tương tác động -->
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', dialogHTML);

        // Đăng ký sự kiện nút đóng
        const closeBtn = document.getElementById('kizunaDialogCloseBtn');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => close());
        }

        // Đăng ký sự kiện click ra ngoài màn hình mờ
        const backdrop = document.getElementById('kizunaGlobalDialog');
        if (backdrop) {
            backdrop.addEventListener('click', (e) => {
                if (e.target.id === 'kizunaGlobalDialog' && allowBackdropClose) {
                    close();
                }
            });
        }

        // Bắt phím Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && allowEscapeClose) {
                const modal = document.getElementById('kizunaGlobalDialog');
                if (modal && !modal.classList.contains('hidden')) {
                    close();
                }
            }
        });
    }

    /**
     * Hiển thị Bảng Thông Báo Nổi Tương Tác
     * @param {Object} options
     * @param {string} options.icon - Icon đại diện (🤖, 🎯, 🌸, ⚠️, ❌...)
     * @param {string} options.title - Tiêu đề bảng thông báo
     * @param {string|HTMLElement} options.content - Nội dung hiển thị (HTML string hoặc DOM Node)
     * @param {Array<Object>} options.actions - Danh sách nút tương tác [{ label, variant, onClick, autoClose }]
     * @param {Function} options.onClose - Hàm callback khi bảng đóng
     * @param {boolean} options.preventBackdropClose - Có cấm bấm nền để đóng không
     * @param {boolean} options.preventEscapeClose - Có cấm phím Esc để đóng không
     */
    function show(options = {}) {
        ensureDialogDOM();

        const {
            icon = '🤖',
            title = 'Thông Báo',
            content = '',
            actions = [],
            onClose = null,
            preventBackdropClose = false,
            preventEscapeClose = false
        } = options;

        currentOnCloseCallback = onClose;
        allowBackdropClose = !preventBackdropClose;
        allowEscapeClose = !preventEscapeClose;

        // Gán Icon & Tiêu đề
        const iconEl = document.getElementById('kizunaDialogIcon');
        const titleEl = document.getElementById('kizunaDialogTitle');
        if (iconEl) iconEl.innerHTML = icon;
        if (titleEl) titleEl.textContent = title;

        // Gán Nội dung Body
        const bodyEl = document.getElementById('kizunaDialogBody');
        if (bodyEl) {
            if (typeof content === 'string') {
                bodyEl.innerHTML = content;
            } else if (content instanceof HTMLElement) {
                bodyEl.innerHTML = '';
                bodyEl.appendChild(content);
            }
        }

        // Render các nút hành động tương tác (Footer)
        const actionsEl = document.getElementById('kizunaDialogActions');
        if (actionsEl) {
            actionsEl.innerHTML = '';

            const finalActions = (actions && actions.length > 0)
                ? actions
                : [{ label: 'Đã Hiểu', variant: 'primary', onClick: null }];

            finalActions.forEach(act => {
                const btn = document.createElement('button');
                btn.type = 'button';

                // Tùy biến kiểu nút theo phong cách Retro-Brutalist
                let baseClass = "px-3.5 py-1.5 text-xs font-itim font-bold rounded-[5px] border border-[#0F172A] shadow-[2px_2px_0px_0px_#0F172A] transition-all cursor-pointer active:translate-x-[1px] active:translate-y-[1px] active:shadow-none inline-flex items-center justify-center gap-1.5 ";

                if (act.variant === 'primary') {
                    // Màu xanh Uguisu chuẩn (#15803D)
                    btn.className = baseClass + "bg-[#15803D] hover:bg-[#166534] text-white";
                } else if (act.variant === 'danger') {
                    // Màu đỏ cảnh báo (#DC2626)
                    btn.className = baseClass + "bg-[#DC2626] hover:bg-[#B91C1C] text-white";
                } else if (act.variant === 'warning') {
                    btn.className = baseClass + "bg-amber-100 hover:bg-amber-200 text-amber-900";
                } else {
                    // Mặc định Secondary / Outline
                    btn.className = baseClass + "bg-white hover:bg-slate-100 text-[#0F172A]";
                }

                btn.innerHTML = act.label;

                btn.addEventListener('click', (e) => {
                    e.preventDefault();
                    if (typeof act.onClick === 'function') {
                        act.onClick();
                    }
                    if (act.autoClose !== false) {
                        close();
                    }
                });

                actionsEl.appendChild(btn);
            });
        }

        // Hiệu ứng mở popup
        const modal = document.getElementById('kizunaGlobalDialog');
        const card = document.getElementById('kizunaDialogCard');

        if (modal && card) {
            modal.classList.remove('hidden');
            modal.classList.add('flex');
            
            // Khóa cuộn trang nền để chống giật
            document.body.classList.add('overflow-hidden');

            requestAnimationFrame(() => {
                card.classList.remove('scale-95');
                card.classList.add('scale-100');
            });
        }
    }

    /**
     * Đóng Bảng Thông Báo
     */
    function close() {
        const modal = document.getElementById('kizunaGlobalDialog');
        const card = document.getElementById('kizunaDialogCard');

        if (!modal) return;

        if (card) {
            card.classList.remove('scale-100');
            card.classList.add('scale-95');
        }

        setTimeout(() => {
            modal.classList.add('hidden');
            modal.classList.remove('flex');
            document.body.classList.remove('overflow-hidden');

            if (typeof currentOnCloseCallback === 'function') {
                const cb = currentOnCloseCallback;
                currentOnCloseCallback = null;
                cb();
            }
        }, 120);
    }

    /**
     * Tiện ích thông báo đơn giản (Alert)
     */
    function alert(message, options = {}) {
        return new Promise((resolve) => {
            show({
                icon: options.icon || 'ℹ️',
                title: options.title || 'Thông Báo',
                content: `<div class="p-1">${message}</div>`,
                actions: [
                    {
                        label: options.buttonText || 'Đã Hiểu',
                        variant: 'primary',
                        onClick: () => resolve(true)
                    }
                ],
                onClose: () => resolve(true)
            });
        });
    }

    /**
     * Tiện ích xác nhận tương tác (Confirm)
     */
    function confirm(message, options = {}) {
        return new Promise((resolve) => {
            show({
                icon: options.icon || '❓',
                title: options.title || 'Xác Nhận Thao Tác',
                content: `<div class="p-1">${message}</div>`,
                preventBackdropClose: true,
                actions: [
                    {
                        label: options.cancelText || 'Hủy Bỏ',
                        variant: 'secondary',
                        onClick: () => resolve(false)
                    },
                    {
                        label: options.confirmText || 'Đồng Ý',
                        variant: options.confirmVariant || 'primary',
                        onClick: () => resolve(true)
                    }
                ],
                onClose: () => resolve(false)
            });
        });
    }

    return {
        show,
        close,
        alert,
        confirm
    };
}));
