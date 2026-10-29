/**
 * KizunaTutor Shared Core Kanji API Client & Utilities
 * Dùng chung logic gọi dữ liệu và xử lý SVG cho cả 6 Actor, tách biệt khỏi tầng UI.
 */
const KizunaKanjiApi = (function() {
    /**
     * Tra cứu Kanji theo phân hệ của từng Actor
     * @param {string} actorRole - 'admin' | 'tutor' | 'learner'
     * @param {object} params - { page, size, keyword, jlpt }
     */
    async function fetchKanjiList(actorRole, params = {}) {
        const query = new URLSearchParams({
            page: params.page || 0,
            size: params.size || 16,
            keyword: params.keyword || '',
            jlpt: params.jlpt || 'ALL'
        });
        const url = `/api/${actorRole}/kanji/${actorRole === 'admin' ? 'list' : (actorRole === 'tutor' ? 'catalog' : 'search')}?${query.toString()}`;
        const res = await fetch(url);
        if (!res.ok) {
            throw new Error(`Lỗi khi tải dữ liệu Kanji cho [${actorRole}]: HTTP ${res.status}`);
        }
        return await res.json();
    }

    /**
     * Làm sạch mã XML/SVG từ KanjiVG để render chuẩn trên mọi trình duyệt
     */
    function sanitizeKanjiSvg(svgContent) {
        if (!svgContent) return '';
        return svgContent
            .replace(/<\?xml.*?\?>/gi, '')
            .replace(/<!DOCTYPE.*?>/gi, '')
            .replace(/<!--.*?-->/gis, '')
            .trim();
    }

    return {
        fetchKanjiList,
        sanitizeKanjiSvg
    };
})();

// Gán toàn cục nếu dùng trong script thuần
if (typeof window !== 'undefined') {
    window.KizunaKanjiApi = KizunaKanjiApi;
}
