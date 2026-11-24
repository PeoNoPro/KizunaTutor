/*
 * Cấu hình Tailwind (CDN) dùng chung cho các trang công khai (docs/guidelines/DESIGN.md mục 8).
 * Nạp ngay sau script CDN Tailwind. Màu là hex nên dùng được hậu tố độ mờ (bg-navy/80).
 */
tailwind.config = {
  theme: {
    extend: {
      colors: {
        primary: '#006194',
        'primary-hover': '#004b73',
        'primary-light': '#e6f1f8',
        'on-primary': '#ffffff',
        navy: '#0b2545',
        accent: '#4dd0e1',
        'accent-hover': '#26c6da',
        'accent-light': '#e0f7fa',
        'on-accent': '#202a37',
        fg: '#1f2937',
        'fg-muted': '#5b6475',
        'fg-disabled': '#94a3b8',
        surface: '#ffffff',
        'surface-muted': '#f3f4f6',
        card: '#ffffff',
        line: '#e5e7eb',
        'line-strong': '#cbd5e1',
        'on-dark': '#ffffff',
        success: '#16a34a',
        'success-muted': '#dcfce7',
        warning: '#b45309',
        'warning-muted': '#fef3c7',
        error: '#dc2626',
        'error-muted': '#fee2e2'
      },
      fontFamily: {
        sans: ['"Be Vietnam Pro"', '-apple-system', '"Segoe UI"', 'Roboto', 'sans-serif'],
        kanji: ['"Noto Serif JP"', 'serif']
      },
      fontSize: {
        'display-1': ['3.25rem', { lineHeight: '1.15', fontWeight: '700' }],
        'display-2': ['2.5rem', { lineHeight: '1.2', fontWeight: '700' }],
        'display-3': ['2rem', { lineHeight: '1.25', fontWeight: '700' }],
        'heading-1': ['1.625rem', { lineHeight: '1.3', fontWeight: '700' }],
        'heading-2': ['1.25rem', { lineHeight: '1.4', fontWeight: '600' }],
        'heading-3': ['1.125rem', { lineHeight: '1.4', fontWeight: '600' }],
        'heading-4': ['1rem', { lineHeight: '1.5', fontWeight: '600' }],
        body: ['0.9375rem', { lineHeight: '1.6' }],
        lead: ['1.0625rem', { lineHeight: '1.65' }],
        caption: ['0.8125rem', { lineHeight: '1.5' }]
      },
      borderRadius: {
        inner: '6px',
        element: '10px',
        container: '16px',
        page: '24px'
      },
      boxShadow: {
        low: '0 1px 2px rgba(15, 23, 42, 0.06), 0 2px 8px rgba(15, 23, 42, 0.06)',
        med: '0 4px 12px rgba(15, 23, 42, 0.08), 0 12px 32px rgba(15, 23, 42, 0.08)',
        high: '0 20px 48px rgba(11, 37, 69, 0.18)'
      },
      transitionDuration: {
        fast: '150ms',
        medium: '300ms'
      }
    }
  }
};
