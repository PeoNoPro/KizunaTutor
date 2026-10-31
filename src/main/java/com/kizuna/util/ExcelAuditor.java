package com.kizuna.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.regex.Pattern;

public class ExcelAuditor {

    // Regex phát hiện ký tự tiếng Việt có dấu
    private static final Pattern VIETNAMESE_PATTERN = Pattern.compile(
            "[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]"
    );

    // Regex phát hiện chữ cái tiếng Anh / Latin
    private static final Pattern ENGLISH_WORD_PATTERN = Pattern.compile("[a-zA-Z]{3,}");

    public static void main(String[] args) {
        String[] files = {
                "C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\kanji_vietnamese_full.xlsx",
                "C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\Kho_Tu_Vung_OpenJLPT_Tieng_Viet.xlsx",
                "C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx"
        };

        for (String filePath : files) {
            auditFile(new File(filePath));
        }
    }

    public static void auditFile(File file) {
        System.out.println("======================================================================");
        System.out.println("AUDITING FILE: " + file.getName());
        System.out.println("======================================================================");

        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            int totalSheets = workbook.getNumberOfSheets();
            int totalFileRows = 0;
            int totalFileViolations = 0;

            for (int s = 0; s < totalSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                int rowCount = sheet.getPhysicalNumberOfRows();
                if (rowCount <= 1) continue;

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                // Xác định các cột tiếng Việt
                Map<Integer, String> vnColumns = new LinkedHashMap<>();
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    Cell cell = headerRow.getCell(c);
                    if (cell != null) {
                        String header = cell.getStringCellValue();
                        if (header.contains("tiếng Việt") || header.contains("Tiếng Việt") || header.contains("Ý nghĩa") || header.contains("Nghĩa") || header.contains("Ghi chú")) {
                            vnColumns.put(c, header);
                        }
                    }
                }

                int sheetViolations = 0;
                List<String> sampleErrors = new ArrayList<>();

                for (int r = 1; r < rowCount; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    totalFileRows++;

                    boolean rowHasViolation = false;
                    for (Map.Entry<Integer, String> entry : vnColumns.entrySet()) {
                        int colIdx = entry.getKey();
                        String colName = entry.getValue();
                        Cell cell = row.getCell(colIdx);
                        if (cell == null) continue;

                        String text = getCellValue(cell).trim();
                        if (text.isEmpty()) continue;

                        // Kiểm tra nếu text là tiếng Anh thuần hoặc chứa cụm tiếng Anh chưa dịch
                        if (isViolatingVietnamese(text)) {
                            rowHasViolation = true;
                            if (sampleErrors.size() < 5) {
                                sampleErrors.add(String.format("Row %d [%s]: %s", r + 1, colName, text));
                            }
                        }
                    }

                    if (rowHasViolation) {
                        sheetViolations++;
                        totalFileViolations++;
                    }
                }

                System.out.println(String.format("Sheet: '%s' | Rows: %d | Violations: %d (%.1f%%)",
                        sheetName, rowCount - 1, sheetViolations, (sheetViolations * 100.0 / Math.max(1, rowCount - 1))));

                if (!sampleErrors.isEmpty() && s == 0) {
                    System.out.println("  Sample violations in primary sheet:");
                    for (String err : sampleErrors) {
                        System.out.println("   * " + err);
                    }
                }
            }

            System.out.println(String.format("TOTAL: File '%s' has %d violating rows.\n", file.getName(), totalFileViolations));

        } catch (Exception e) {
            System.err.println("Error reading " + file.getName() + ": " + e.getMessage());
        }
    }

    private static final Set<String> VALID_VIETNAMESE_WORDS = Set.of(
            "tay", "chim", "dao", "hai", "hoa", "tai", "tro", "bia", "len", "treo", "may", "bom", "tham",
            "cay", "cho vay", "con voi", "sinh", "nha khoa", "nhi khoa", "doanh thu", "khoan dung", "hoa sen",
            "in sai", "tia x", "logic", "cacbon", "axit", "nylon", "vitamin", "calo", "sandwich", "judo",
            "album", "whisky", "kem", "radio", "tivi", "taxi", "gram", "kimono", "fax", "violon", "centimet",
            "mili", "dung nham", "cocktail", "hy sinh", "vua", "mua", "thanh", "xung", "xem", "ghim", "nhanh",
            "trang", "ghi", "tham quan", "vai", "theo", "katakana", "loa", "chia tay", "chua", "con trai", "roentgen",
            "thua", "mike", "encore", "phong ca?nh", "shogi"
    );

    private static boolean isViolatingVietnamese(String text) {
        // Một số từ viết tắt hợp lệ
        String lower = text.trim().toLowerCase();
        if (text.equalsIgnoreCase("N5") || text.equalsIgnoreCase("N4") || text.equalsIgnoreCase("N3")
                || text.equalsIgnoreCase("N2") || text.equalsIgnoreCase("N1") || text.equalsIgnoreCase("ID")) {
            return false;
        }

        // Bỏ qua các từ tiếng Việt mượn hoặc không dấu hợp lệ
        for (String valid : VALID_VIETNAMESE_WORDS) {
            if (lower.equals(valid) || lower.startsWith(valid + ";") || lower.startsWith(valid + " ") || lower.endsWith(" " + valid)) {
                return false;
            }
        }

        // Nếu không có bất kỳ dấu tiếng Việt nào và có chứa từ tiếng Anh
        boolean hasVietnameseDiacritic = VIETNAMESE_PATTERN.matcher(text).find();
        boolean hasEnglishWord = ENGLISH_WORD_PATTERN.matcher(text).find();

        // Nếu chuỗi hoàn toàn bằng tiếng Anh (hoặc không có dấu tiếng Việt nhưng chứa từ tiếng Anh như "to eat", "bookstore", "while out")
        if (!hasVietnameseDiacritic && hasEnglishWord) {
            return true;
        }

        // Nếu chuỗi chứa cụm từ tiếng Anh phổ biến chưa dịch
        if (lower.contains("while out") || lower.contains("to eat") || lower.contains("student")
                || lower.contains("the weather") || lower.matches(".*\\bi\\s+am\\b.*") || lower.contains("because")
                || lower.contains("although") || lower.contains("used with") || lower.contains("written")
                || lower.contains("compare with") || lower.contains("meaning:") || lower.contains("so there's no way")) {
            return true;
        }

        return false;
    }

    private static String getCellValue(Cell cell) {
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.BOOLEAN) return String.valueOf(cell.getBooleanCellValue());
        return "";
    }
}
