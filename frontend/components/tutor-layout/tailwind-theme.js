/**
 * KizunaTutor - Theme Tailwind cho khu vực Gia sư (theo thiết kế Google Stitch "Tutor Dashboard").
 * Nạp NGAY SAU script Tailwind CDN: <script src="/components/tutor-layout/tailwind-theme.js"></script>
 */
tailwind.config = {
  theme: {
    extend: {
      colors: {
        'primary': '#006194',
        'on-primary': '#ffffff',
        'primary-container': '#007bb9',
        'on-primary-container': '#fdfcff',
        'primary-fixed': '#cce5ff',
        'on-primary-fixed-variant': '#004b73',
        'secondary': '#4059aa',
        'on-secondary': '#ffffff',
        'secondary-fixed': '#dce1ff',
        'on-secondary-fixed': '#00164e',
        'tertiary': '#8d4b00',
        'tertiary-fixed': '#ffdcc3',
        'on-tertiary-fixed': '#2f1500',
        'error': '#ba1a1a',
        'on-error': '#ffffff',
        'error-container': '#ffdad6',
        'on-error-container': '#93000a',
        'success': '#1E8B3F',
        'surface': '#faf8ff',
        'surface-container-lowest': '#ffffff',
        'surface-container-low': '#f2f3ff',
        'surface-container': '#eaedff',
        'surface-container-high': '#e2e7ff',
        'surface-container-highest': '#dae2fd',
        'on-surface': '#131b2e',
        'on-surface-variant': '#3f4850',
        'outline': '#707881',
        'outline-variant': '#bfc7d2',
        'brand-navy': '#0F2545'
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif']
      },
      fontSize: {
        'caption': ['11px', { lineHeight: '14px', fontWeight: '500' }],
        'label-sm': ['12px', { lineHeight: '16px', letterSpacing: '0.02em', fontWeight: '600' }],
        'label-md': ['14px', { lineHeight: '20px', fontWeight: '500' }],
        'body-sm': ['13px', { lineHeight: '18px', fontWeight: '400' }],
        'body-md': ['14px', { lineHeight: '22px', fontWeight: '400' }],
        'title-sm': ['16px', { lineHeight: '24px', fontWeight: '600' }],
        'title-md': ['18px', { lineHeight: '26px', fontWeight: '600' }],
        'headline-sm': ['20px', { lineHeight: '28px', fontWeight: '600' }],
        'headline-md': ['24px', { lineHeight: '32px', letterSpacing: '-0.01em', fontWeight: '600' }],
        'headline-lg': ['32px', { lineHeight: '40px', letterSpacing: '-0.015em', fontWeight: '600' }]
      }
    }
  }
};
