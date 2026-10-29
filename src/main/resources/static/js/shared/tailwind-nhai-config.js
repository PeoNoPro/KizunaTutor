/**
 * ====================================================================================================
 * KIZUNA TUTOR - STANDARDIZED CORE DESIGN SYSTEM (TAILWIND RUNTIME CONFIG)
 * ====================================================================================================
 * Standardized Design Tokens:
 * 1. Base UI (60%): bg-app (#FAF9F6 Washi), bg-canvas (#FFFFFF), bg-surface (#FFFFFF),
 *                   border-subtle (#E2E8F0), border-strong (#CBD5E1)
 * 2. Typography (30%): text-primary (#0F172A), text-secondary (#475569), text-muted (#94A3B8)
 * 3. Script Categorization Colors:
 *    - Hiragana: bg (#FDF2F8), text (#BE185D Sakura), border (#FBCFE8)
 *    - Katakana: bg (#EFF6FF), text (#1D4ED8), border (#BFDBFE)
 *    - Kanji:    bg (#FEFCE8), text (#854D0E Sumi), border (#FEF08A)
 * 4. Actions & AI Feedback (10%):
 *    - color-primary (#15803D Uguisu-iro), color-primary-hover (#166534), color-primary-light (#F0FDF4)
 *    - color-success (#16A34A), color-error (#DC2626)
 * 5. Elevation (Soft Shadows): shadow-sm, shadow-canvas, shadow-md, shadow-lg
 * 6. Font stack: "Noto Sans JP", "Inter", "Segoe UI", sans-serif (Itim reserved for brand only)
 * ====================================================================================================
 */
if (typeof tailwind !== 'undefined') {
    tailwind.config = {
        theme: {
            extend: {
                colors: {
                    // Surfaces & Borders (60% Base UI)
                    'bg-app': '#FAF9F6',
                    'bg-canvas': '#FFFFFF',
                    'bg-surface': '#FFFFFF',
                    'border-subtle': '#E2E8F0',
                    'border-strong': '#CBD5E1',

                    // Text & Calligraphy (30%)
                    'text-primary': '#0F172A',
                    'text-secondary': '#475569',
                    'text-muted': '#94A3B8',

                    // Script Badges & Categorization Colors (Sakura / Modern / Sumi)
                    'script-hiragana-bg': '#FDF2F8',
                    'script-hiragana-text': '#BE185D',
                    'script-hiragana-border': '#FBCFE8',
                    'script-katakana-bg': '#EFF6FF',
                    'script-katakana-text': '#1D4ED8',
                    'script-katakana-border': '#BFDBFE',
                    'script-kanji-bg': '#FEFCE8',
                    'script-kanji-text': '#854D0E',
                    'script-kanji-border': '#FEF08A',

                    // Actions & AI Feedback (10% Uguisu-iro)
                    'primary': {
                        DEFAULT: '#15803D',
                        hover: '#166534',
                        light: '#F0FDF4',
                    },
                    'success': {
                        DEFAULT: '#16A34A',
                        light: '#DCFCE7',
                    },
                    'error': {
                        DEFAULT: '#DC2626',
                        light: '#FEE2E2',
                    },

                    // Clean Token Aliases (mapping away legacy colors)
                    'color-bg-app': '#FAF9F6',
                    'color-1': '#0F172A',
                    'color-2': '#15803D',
                    'color-3': '#15803D',
                    'color-4': '#DC2626',
                    'color-5': '#1D4ED8',
                    'color-6': '#16A34A',
                    'color-10': '#854D0E',
                    'color-11': '#FFFFFF',
                },
                fontFamily: {
                    sans: ['"Noto Sans JP"', 'Inter', '"Segoe UI"', 'sans-serif'],
                    body: ['"Noto Sans JP"', 'Inter', '"Segoe UI"', 'sans-serif'],
                    kanji: ['"Noto Serif JP"', '"Noto Sans JP"', 'serif'],
                    serif: ['"Noto Serif JP"', 'serif'],
                    meta: ['Inter', '"Segoe UI"', 'sans-serif'],
                    brand: ['Comfortaa', 'Itim', 'cursive', 'sans-serif'],
                    itim: ['Comfortaa', 'Itim', 'cursive', 'sans-serif'],
                },
                boxShadow: {
                    'sm': '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
                    'canvas': '0 4px 6px -1px rgba(15, 23, 42, 0.08)',
                    'md': '0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.04)',
                    'lg': '0 10px 15px -3px rgba(15, 23, 42, 0.08)',
                    // Soft shadows replacing legacy harsh shadows
                    'retro': '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
                    'retro-sm': '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
                    'retro-hover': '0 4px 6px -1px rgba(15, 23, 42, 0.08)',
                    'retro-active': '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
                },
                borderRadius: {
                    DEFAULT: '8px',
                    'sm': '6px',
                    'md': '8px',
                    'lg': '12px',
                    'xl': '16px',
                    '2xl': '20px',
                    'nhai': '8px',
                }
            }
        }
    };
}
