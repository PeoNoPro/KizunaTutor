/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./pages/**/*.{html,js,ts,jsx,tsx}",
    "./components/**/*.{html,js,ts,jsx,tsx}",
    "./templates/**/*.html"
  ],
  theme: {
    extend: {
      colors: {
        // Nền (Backgrounds)
        'app-bg': '#F8FAFC',
        'app-white': '#FFFFFF',
        'app-pink-tint': '#FFE5EC',
        'app-amber-tint': '#FEF3C7',
        'app-border-light': '#F1F5F9',
        
        // Văn bản (Typography Colors)
        'app-ink': '#0F172A',
        'app-main': '#334155',
        'app-muted': '#64748B',
        'app-secondary-text': '#68737D',
        'app-berry': '#831843',

        // Điểm nhấn & Thương hiệu (Brand & Action)
        'app-primary': '#006194',
        'app-primary-container': '#007BB9',
        'app-secondary': '#4059AA',
        'app-accent-green': '#2A5A1A',
        'app-accent-pink': '#E0399A',

        // Trạng thái hệ thống (Feedback Colors)
        'app-success': '#059669',
        'app-warning': '#D97706',
        'app-error': '#BA1A1A',
        'app-error-alt': '#E11D48',
        'app-red': '#B91C1C',
        'app-info': '#0284C7',

        // Đường viền (Borders)
        'app-border-ink': '#0F172A',
        'app-border-input': '#CBD5E1',
        'app-border-subtle': '#E5E7EB',
        'app-border-card': '#E2E8F0',
        'app-border-pink': '#FBCFE8'
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'Segoe UI', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        body: ['Inter', '-apple-system', 'Segoe UI', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        japanese: ['"Noto Serif JP"', 'serif'],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace']
      },
      fontSize: {
        'xs': ['10px', { lineHeight: '14px' }],
        'sm': ['12px', { lineHeight: '16px' }],
        'base': ['14px', { lineHeight: '20px' }],
        'lg': ['16px', { lineHeight: '24px' }],
        'xl': ['18px', { lineHeight: '26px' }],
        '2xl': ['24px', { lineHeight: '32px' }]
      },
      spacing: {
        '1': '2px',
        '2': '4px',
        '3': '6px',
        '4': '8px',
        '5': '10px',
        '6': '12px',
        '7': '14px',
        '8': '16px'
      },
      borderRadius: {
        'sm': '4px',
        'md': '6px',
        'lg': '8px',
        'xl': '12px',
        'full': '9999px'
      },
      boxShadow: {
        'sm': '1px 1px 0px 0px #0F172A',
        'md': '2px 2px 0px 0px #0F172A',
        'xl': '2px 2px 0px 0px #0F172A',
        'lg': '3px 3px 0px 0px #0F172A'
      },
      transitionTimingFunction: {
        'shodo': 'cubic-bezier(0.4, 0, 0.2, 1)'
      },
      transitionDuration: {
        'shodo': '150ms'
      }
    }
  },
  plugins: []
};