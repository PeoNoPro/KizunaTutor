/**
 * ====================================================================================================
 * KIZUNA TUTOR - TAILWIND CSS RUNTIME CONFIG (NHAITOPIK.COM DESIGN SYSTEM)
 * ====================================================================================================
 * Cấu hình mở rộng Tailwind CDN runtime:
 * - Color tokens: color-1 đến color-11
 * - Font: font-itim (Itim), font-body (Segoe UI), font-meta (ui-sans-serif)
 * - Shadows: shadow-retro (4px 4px 0px 0px #000000), shadow-retro-sm (2px 2px 0px 0px #000000)
 * - Border Radius: Cố định 5px cho toàn bộ hệ thống
 * ====================================================================================================
 */
if (typeof tailwind !== 'undefined') {
    tailwind.config = {
        theme: {
            extend: {
                colors: {
                    'color-1': '#000000',
                    'color-2': '#2A5A1A',
                    'color-3': '#7A2CA8',
                    'color-4': '#BF360C',
                    'color-5': '#1F6FB4',
                    'color-6': '#1E8B3F',
                    'color-10': '#E76F51',
                    'color-11': '#FFFFFF',
                },
                fontFamily: {
                    itim: ['Itim', 'cursive'],
                    body: ['Segoe UI', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
                    meta: ['ui-sans-serif', 'system-ui', 'sans-serif'],
                },
                boxShadow: {
                    'retro': '4px 4px 0px 0px #000000',
                    'retro-sm': '2px 2px 0px 0px #000000',
                    'retro-hover': '5px 5px 0px 0px #000000',
                    'retro-active': '1px 1px 0px 0px #000000',
                },
                borderRadius: {
                    DEFAULT: '5px',
                    'sm': '5px',
                    'md': '5px',
                    'lg': '5px',
                    'xl': '5px',
                    '2xl': '5px',
                    '3xl': '5px',
                    'nhai': '5px',
                }
            }
        }
    };
}
