package com.kizuna.features.learning_content.service;

import com.kizuna.features.learning_content.service.ContentType.Field;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tệp Excel mẫu cho từng loại học liệu (FE-13), tải ở tab "Nhập từ Excel".
 * - Sheet dữ liệu: chỉ có dòng tiêu đề đúng tên cột của {@link ContentType} (cột bắt buộc tô màu), không có dữ liệu.
 * - Sheet "HuongDan": cách điền; không có dòng tiêu đề hợp lệ nên {@link ContentWorkbookReader} bỏ qua.
 * Không chạm cơ sở dữ liệu.
 */
public final class ContentTemplate {

    private static final Map<ContentType, String> SHEET_NAMES = Map.of(
            ContentType.KANJI, "Kanji", ContentType.VOCABULARY, "TuVung", ContentType.GRAMMAR, "NguPhap");

    private static final Map<ContentType, String> FILE_NAMES = Map.of(
            ContentType.KANJI, "mau-kanji.xlsx", ContentType.VOCABULARY, "mau-tu-vung.xlsx", ContentType.GRAMMAR, "mau-ngu-phap.xlsx");

    private ContentTemplate() {
    }

    public static String fileName(ContentType type) {
        return FILE_NAMES.get(type);
    }

    public static byte[] build(ContentType type) {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font bold = book.createFont();
            bold.setBold(true);
            CellStyle required = headerStyle(book, bold, IndexedColors.LIGHT_CORNFLOWER_BLUE);
            CellStyle optional = headerStyle(book, bold, IndexedColors.GREY_25_PERCENT);

            List<Field> fields = type.fields();
            Sheet data = book.createSheet(SHEET_NAMES.get(type));
            Row header = data.createRow(0);
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                Cell cell = header.createCell(i);
                cell.setCellValue(field.header());
                cell.setCellStyle(field.required() ? required : optional);
                data.setColumnWidth(i, columnWidth(field) * 256);
            }
            data.createFreezePane(0, 1);

            Sheet guide = book.createSheet("HuongDan");
            List<String> lines = guideLines(type);
            for (int i = 0; i < lines.size(); i++) {
                Cell cell = guide.createRow(i).createCell(0);
                cell.setCellValue(lines.get(i));
                if (i == 0) {
                    CellStyle title = book.createCellStyle();
                    title.setFont(bold);
                    cell.setCellStyle(title);
                }
            }
            guide.setColumnWidth(0, 150 * 256);

            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được tệp mẫu.", e);
        }
    }

    private static CellStyle headerStyle(Workbook book, Font font, IndexedColors color) {
        CellStyle style = book.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private static int columnWidth(Field field) {
        if (field.maxLength() == 0) return 36;
        if (field.maxLength() <= 3) return 10;
        return 20;
    }

    static List<String> guideLines(ContentType type) {
        List<String> lines = new ArrayList<>();
        lines.add("Cách điền tệp " + type.label() + " cho kho học liệu KizunaTutor");
        lines.add("• Điền dữ liệu ở sheet " + SHEET_NAMES.get(type) + " từ dòng 2, mỗi dòng một mục. Giữ nguyên dòng tiêu đề (dòng 1).");
        lines.add("• Cột tô xanh là cột bắt buộc: " + String.join(", ", type.requiredFields().stream().map(Field::header).toList())
                + ". Các cột tô xám có thể để trống.");
        lines.add("• Mục đã có trong kho luôn được giữ nguyên, không bị ghi đè.");
        lines.add("• Cấp độ JLPT ghi N5, N4, N3, N2 hoặc N1.");
        switch (type) {
            case KANJI -> {
                lines.add("• Cột Kanji: đúng một chữ Hán. Hai mục trùng nhau khi cùng chữ Kanji.");
                lines.add("• Số nét (1 – 84) bắt buộc với chữ Kanji chưa có trong kho.");
                lines.add("• Nhiều âm đọc ngăn cách bằng dấu 、 hoặc dấu phẩy.");
            }
            case VOCABULARY -> {
                lines.add("• Hai mục trùng nhau khi cùng Từ vựng và Cách đọc. Từ viết bằng kana có thể để trống Cách đọc.");
                lines.add("• Câu ví dụ Furigana: ghi {chữ Hán|cách đọc}, ví dụ {学校|がっこう}へ{行|い}きます。");
                lines.add("• Từ mới được tự liên kết với các chữ Kanji đã có trong kho.");
            }
            case GRAMMAR -> {
                lines.add("• Mã định danh (ID) là mã ngắn không trùng, ví dụ tai, wa, te-iru. Hai mục trùng nhau khi cùng mã (không phân biệt hoa thường).");
                lines.add("• Một mẫu cấu trúc ngữ pháp chỉ thuộc một mã; mẫu đã thuộc mã khác sẽ bị báo lỗi.");
                lines.add("• Câu ví dụ Furigana: ghi {chữ Hán|cách đọc}, ví dụ {日本|にほん}へ{行|い}きたいです。");
                lines.add("• Các câu ví dụ mở rộng: mỗi câu hai dòng (xuống dòng trong ô bằng Alt + Enter): dòng câu tiếng Nhật có số thứ tự, dòng dịch nghĩa trong ngoặc.");
                lines.add("• Thẻ phân loại (Tags): ngăn cách bằng dấu phẩy, ví dụ Mong muốn, Động từ.");
            }
        }
        lines.add("• Lưu tệp dạng .xlsx (tối đa 15 MB, tối đa 50.000 dòng). Bấm \"Kiểm tra tệp\" để xem trước rồi mới \"Nhập vào hệ thống\".");
        return lines;
    }
}
