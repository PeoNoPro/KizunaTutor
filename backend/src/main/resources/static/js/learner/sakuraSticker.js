/**
 * ==========================================================================
 * KIZUNATUTOR - SAKURA PETAL CLUSTER DECORATION (DECORATIVE SAKURA STICKERS)
 * Tự động rải các cụm cánh hoa ngẫu nhiên vào các góc/mép của Card
 * - Kích cỡ ngẫu nhiên: 14px đến 28px (tuyệt đối không to hơn logo 32px của hệ thống)
 * - Đè lên nền card, viền đen retro, bóng đổ (z-index: 5)
 * - TUYỆT ĐỐI KHÔNG ĐÈ LÊN NỘI DUNG CHỮ, NÚT, CANVAS (z-index: 10 & 11)
 * ==========================================================================
 */
const SakuraSticker = (() => {
  const PETAL_IMG_SRC = '/images/stickers/sakura-petal.png';

  // Logo hệ thống là w-8 h-8 = 32px. Cánh hoa có kích thước ngẫu nhiên từ 14px đến 28px (< 32px)
  const MIN_PETAL_SIZE = 14;
  const MAX_PETAL_SIZE = 28;

  // Các vị trí mép an toàn trên Card (vắt qua viền, mép hoặc góc hộp)
  const SAFE_ANCHORS = [
    { top: '-12px', left: '-10px' },   // Góc trên-trái (vắt qua góc viền)
    { top: '-14px', right: '16px' },   // Góc trên-phải
    { bottom: '-10px', left: '20px' },  // Góc dưới-trái
    { bottom: '-12px', right: '-8px' }, // Góc dưới-phải
    { top: '25%', right: '-10px' },    // Mép sườn phải (vắt ngang viền dọc)
    { top: '40%', left: '-10px' }      // Mép sườn trái (vắt ngang viền dọc)
  ];

  function spawnCluster(parentEl) {
    if (!parentEl) return;
    if (parentEl.querySelector(':scope > .sakura-petal-cluster')) return;

    // Đảm bảo card có relative stacking
    const pos = window.getComputedStyle(parentEl).position;
    if (pos === 'static') {
      parentEl.style.position = 'relative';
    }

    const cluster = document.createElement('div');
    cluster.className = 'sakura-petal-cluster';

    // Chọn ngẫu nhiên 1 góc mép an toàn
    const anchor = SAFE_ANCHORS[Math.floor(Math.random() * SAFE_ANCHORS.length)];
    Object.assign(cluster.style, anchor);

    // Sinh số lượng cánh hoa ngẫu nhiên từ 1 đến 5 cánh
    const petalCount = Math.floor(Math.random() * 5) + 1;

    for (let i = 0; i < petalCount; i++) {
      const img = document.createElement('img');
      img.src = PETAL_IMG_SRC;
      img.alt = '';
      img.className = 'sakura-petal-item';

      // Kích thước ngẫu nhiên: 14px đến 28px (luôn nhỏ hơn logo hệ thống 32px)
      const randomWidth = Math.floor(Math.random() * (MAX_PETAL_SIZE - MIN_PETAL_SIZE + 1)) + MIN_PETAL_SIZE;
      img.style.width = randomWidth + 'px';
      img.style.height = 'auto';

      // Tạo độ lệch ngẫu nhiên để các cánh đè lên nhau tự nhiên
      const offsetX = (Math.random() * 16 - 8).toFixed(1);
      const offsetY = (Math.random() * 14 - 7).toFixed(1);
      const rotate = Math.floor(Math.random() * 360);

      img.style.transform = `translate(${offsetX}px, ${offsetY}px) rotate(${rotate}deg)`;
      img.style.zIndex = i + 1;

      cluster.appendChild(img);
    }

    // Chèn cụm cánh hoa lên đầu DOM của thẻ Card để toàn bộ khối nội dung luôn xếp trên
    parentEl.prepend(cluster);
  }

  function init() {
    // 1. Luôn rải hoa trên TẤT CẢ Hero Banner đầu trang (.card-retro)
    const retroCards = document.querySelectorAll('.card-retro:not(.no-sakura)');
    retroCards.forEach(card => {
      spawnCluster(card);
    });

    // 2. Rải ngẫu nhiên trên các Card nội dung chính (.card-nhai)
    const nhaiCards = document.querySelectorAll('.card-nhai:not(.no-sakura)');
    nhaiCards.forEach(card => {
      // Bỏ qua nếu là dropdown, alert box ẩn, hoặc card con quá hẹp
      if (card.offsetWidth > 0 && card.offsetWidth < 80) return;
      if (Math.random() > 0.45) {
        spawnCluster(card);
      }
    });
  }

  return { init, spawnCluster };
})();

// Tự động khởi chạy an toàn
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    SakuraSticker.init();
    setTimeout(() => SakuraSticker.init(), 200);
    setTimeout(() => SakuraSticker.init(), 600);
  });
} else {
  SakuraSticker.init();
  setTimeout(() => SakuraSticker.init(), 200);
  setTimeout(() => SakuraSticker.init(), 600);
}
window.addEventListener('load', () => {
  SakuraSticker.init();
});
