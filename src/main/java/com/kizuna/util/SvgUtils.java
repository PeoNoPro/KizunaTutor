package com.kizuna.util;

/**
 * Tiện ích xử lý và làm sạch chuỗi SVG KanjiVG chuẩn.
 */
public final class SvgUtils {

    private SvgUtils() {}

    /**
     * Chuẩn hóa và làm sạch dữ liệu SVG KanjiVG:
     * Cắt bỏ toàn bộ khai báo XML <?xml ... ?>, comment <!-- ... -->,
     * và triệt tiêu hoàn toàn DTD Header <!DOCTYPE svg ... [ ... ]>
     * Loại bỏ tận gốc chuỗi rác `]>` hiển thị ra giao diện HTML.
     */
    public static String cleanKanjiVgSvg(String rawSvg) {
        if (rawSvg == null || rawSvg.isBlank()) {
            return rawSvg;
        }
        int svgStart = rawSvg.indexOf("<svg");
        int svgEnd = rawSvg.lastIndexOf("</svg>");
        if (svgStart != -1 && svgEnd != -1) {
            return rawSvg.substring(svgStart, svgEnd + 6);
        }
        return rawSvg
                .replaceAll("(?is)<\\?xml.*?\\?>", "")
                .replaceAll("(?is)<!--.*?-->", "")
                .replaceAll("(?is)<!DOCTYPE.*?\\]>", "")
                .replaceAll("(?is)<!DOCTYPE[^>]*>", "")
                .trim();
    }
}
