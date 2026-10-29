/**
 * ============================================================================
 * KIZUNATUTOR - GLOBAL NON-BLOCKING TOAST NOTIFICATIONS MODULE
 * File: kizunaToast.js (shared mirror)
 * ============================================================================
 */

(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        const toast = factory();
        root.KizunaToast = toast;
        root.showToast = toast;
    }
}(typeof self !== 'undefined' ? self : this, function () {
    'use strict';

    let container = null;

    function ensureContainer() {
        if (!container || !document.body.contains(container)) {
            container = document.getElementById('kizunaToastContainer');
            if (!container) {
                container = document.createElement('div');
                container.id = 'kizunaToastContainer';
                container.className = 'fixed bottom-5 right-5 z-[9999] flex flex-col-reverse gap-2.5 pointer-events-none max-w-sm w-full px-3 sm:px-0';
                document.body.appendChild(container);
            }
        }
    }

    const CONFIG = {
        success: { bg: 'bg-[#DCFCE7]', text: 'text-[#14532D]', border: 'border-[#0F172A]', icon: '🌸' },
        danger:  { bg: 'bg-[#FEE2E2]', text: 'text-[#7F1D1D]', border: 'border-[#0F172A]', icon: '🗑️' },
        warning: { bg: 'bg-[#FEF3C7]', text: 'text-[#78350F]', border: 'border-[#0F172A]', icon: '⚠️' },
        info:    { bg: 'bg-[#E0F2FE]', text: 'text-[#0C4A6E]', border: 'border-[#0F172A]', icon: '💡' }
    };

    function escapeHtml(text) {
        if (!text) return '';
        return String(text)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function show(message, type = 'success', duration = 3500) {
        if (typeof document === 'undefined') return;
        ensureContainer();

        const theme = CONFIG[type] || CONFIG.success;
        const safeMsg = escapeHtml(message);

        const toast = document.createElement('div');
        toast.className = `pointer-events-auto flex items-center justify-between gap-3 p-3 ${theme.bg} ${theme.text} border ${theme.border} rounded-[5px] shadow-[3px_3px_0px_0px_#0F172A] transform translate-y-4 opacity-0 transition-all duration-200 ease-out select-none`;

        toast.innerHTML = `
            <div class="flex items-center gap-2.5 min-w-0">
                <span class="text-base shrink-0 leading-none">${theme.icon}</span>
                <span class="font-['Segoe_UI'] text-xs font-semibold leading-snug break-words">${safeMsg}</span>
            </div>
            <button type="button" 
                    class="shrink-0 text-xs font-bold opacity-60 hover:opacity-100 p-1 leading-none transition-opacity cursor-pointer" 
                    aria-label="Đóng thông báo">✕</button>
        `;

        const closeBtn = toast.querySelector('button');
        if (closeBtn) {
            closeBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                removeToast(toast);
            });
        }

        container.appendChild(toast);

        requestAnimationFrame(() => {
            toast.classList.remove('translate-y-4', 'opacity-0');
            toast.classList.add('translate-y-0', 'opacity-100');
        });

        const timer = setTimeout(() => {
            removeToast(toast);
        }, duration);

        function removeToast(el) {
            clearTimeout(timer);
            if (!el || !el.parentNode) return;
            el.classList.remove('translate-y-0', 'opacity-100');
            el.classList.add('translate-y-2', 'opacity-0');
            setTimeout(() => {
                if (el && el.parentNode) {
                    el.remove();
                }
            }, 220);
        }

        return toast;
    }

    const toastFn = function (message, type = 'success', duration = 3500) {
        return show(message, type, duration);
    };

    toastFn.show = show;
    toastFn.success = (msg, dur) => show(msg, 'success', dur);
    toastFn.danger  = (msg, dur) => show(msg, 'danger', dur);
    toastFn.warning = (msg, dur) => show(msg, 'warning', dur);
    toastFn.info    = (msg, dur) => show(msg, 'info', dur);

    return toastFn;
}));
