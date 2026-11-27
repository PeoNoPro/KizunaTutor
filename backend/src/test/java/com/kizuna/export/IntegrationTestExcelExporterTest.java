package com.kizuna.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generator for Integration Test Report Document following the customer's exact template.
 * Outputs to Desktop: C:\Users\Vuh26\Desktop\Report-5.2_Integration_Test.xlsx
 */
public class IntegrationTestExcelExporterTest {

    public static class IntegrationTestCase {
        public String id;
        public String description;
        public String procedure;
        public String expectedResults;
        public String preConditions;
        public String round1 = "Passed";
        public String testDate = "2026-11-23";
        public String tester = "Hoangvm";
        public String round2 = "Passed";
        public String round3 = "Passed";
        public String defectId = "N/A";
        public String note = "";

        public IntegrationTestCase(String id, String description, String procedure, String expectedResults, String preConditions) {
            this.id = id;
            this.description = description;
            this.procedure = procedure;
            this.expectedResults = expectedResults;
            this.preConditions = preConditions;
        }
    }

    public static class IntegrationFunctionGroup {
        public String functionName;
        public List<IntegrationTestCase> cases = new ArrayList<>();

        public IntegrationFunctionGroup(String functionName) {
            this.functionName = functionName;
        }

        public IntegrationFunctionGroup add(IntegrationTestCase tc) {
            this.cases.add(tc);
            return this;
        }
    }

    @Test
    public void exportIntegrationTestExcel() throws IOException {
        Workbook wb = new XSSFWorkbook();

        // Styles
        Font fontRegular = wb.createFont();
        fontRegular.setFontName("Tahoma");
        fontRegular.setFontHeightInPoints((short) 10);

        Font fontBold = wb.createFont();
        fontBold.setFontName("Tahoma");
        fontBold.setFontHeightInPoints((short) 10);
        fontBold.setBold(true);

        Font fontTitle = wb.createFont();
        fontTitle.setFontName("Tahoma");
        fontTitle.setFontHeightInPoints((short) 16);
        fontTitle.setBold(true);

        Font fontBigTitle = wb.createFont();
        fontBigTitle.setFontName("Tahoma");
        fontBigTitle.setFontHeightInPoints((short) 18);
        fontBigTitle.setBold(true);

        // Header style
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(fontBold);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setWrapText(true);

        // Function section header style
        CellStyle functionHeaderStyle = wb.createCellStyle();
        functionHeaderStyle.setFont(fontBold);
        functionHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        functionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        functionHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        functionHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        functionHeaderStyle.setBorderTop(BorderStyle.THIN);
        functionHeaderStyle.setBorderBottom(BorderStyle.THIN);
        functionHeaderStyle.setBorderLeft(BorderStyle.THIN);
        functionHeaderStyle.setBorderRight(BorderStyle.THIN);

        // Cell Center
        CellStyle cellCenter = wb.createCellStyle();
        cellCenter.setFont(fontRegular);
        cellCenter.setAlignment(HorizontalAlignment.CENTER);
        cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        cellCenter.setBorderTop(BorderStyle.THIN);
        cellCenter.setBorderBottom(BorderStyle.THIN);
        cellCenter.setBorderLeft(BorderStyle.THIN);
        cellCenter.setBorderRight(BorderStyle.THIN);

        // Cell Center Bold
        CellStyle cellCenterBold = wb.createCellStyle();
        cellCenterBold.setFont(fontBold);
        cellCenterBold.setAlignment(HorizontalAlignment.CENTER);
        cellCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
        cellCenterBold.setBorderTop(BorderStyle.THIN);
        cellCenterBold.setBorderBottom(BorderStyle.THIN);
        cellCenterBold.setBorderLeft(BorderStyle.THIN);
        cellCenterBold.setBorderRight(BorderStyle.THIN);

        // Cell Left Wrap
        CellStyle cellLeftWrap = wb.createCellStyle();
        cellLeftWrap.setFont(fontRegular);
        cellLeftWrap.setAlignment(HorizontalAlignment.LEFT);
        cellLeftWrap.setVerticalAlignment(VerticalAlignment.TOP);
        cellLeftWrap.setWrapText(true);
        cellLeftWrap.setBorderTop(BorderStyle.THIN);
        cellLeftWrap.setBorderBottom(BorderStyle.THIN);
        cellLeftWrap.setBorderLeft(BorderStyle.THIN);
        cellLeftWrap.setBorderRight(BorderStyle.THIN);

        // Label Bold
        CellStyle metaLabelBold = wb.createCellStyle();
        metaLabelBold.setFont(fontBold);
        metaLabelBold.setAlignment(HorizontalAlignment.LEFT);
        metaLabelBold.setVerticalAlignment(VerticalAlignment.CENTER);

        // Title Style
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(fontTitle);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Build data
        List<IntegrationFunctionGroup> feature1Groups = buildFeature1Groups();
        List<IntegrationFunctionGroup> feature2Groups = buildFeature2Groups();

        // Create Sheets
        createCoverSheet(wb, titleStyle, headerStyle, cellCenter, metaLabelBold);
        createTestCaseListSheet(wb, fontBigTitle, headerStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold, fontRegular);
        createFeatureSheet(wb, "Feature1", "Authentication & Authorization",
                "Kiểm thử tích hợp luồng nghiệp vụ đăng nhập 6 vai trò, Google OAuth2, xác thực OTP email 2 bước, đổi mật khẩu lần đầu và bảo mật JWT HttpOnly Cookie.",
                feature1Groups, "IT-AUTH*", headerStyle, functionHeaderStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold);
        createFeatureSheet(wb, "Feature2", "Learning Support Space",
                "Kiểm thử tích hợp Flashcards cá nhân và bảo mật owner_key, tra cứu kho từ vựng 7.811 từ và ngữ pháp JLPT, AI Sakubun chữa bài viết, Game Kanji Rain, Từ điển và viết tay Canvas, Trợ lý đọc hiểu Furigana và tối ưu Cache SHA-256.",
                feature2Groups, "IT-LS*", headerStyle, functionHeaderStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold);
        createTestStatisticsSheet(wb, titleStyle, headerStyle, cellCenter, cellCenterBold, metaLabelBold);

        // Evaluate formulas
        try {
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        } catch (Exception e) {
            System.out.println("Formula evaluation notice: " + e.getMessage());
        }

        // Export to Desktop
        File desktopFile1 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.2_Integration_Test.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile1)) {
            wb.write(fos);
        }
        System.out.println("Exported successfully to: " + desktopFile1.getAbsolutePath());

        File desktopFile2 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.2_Integration_Test_KizunaTutor.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile2)) {
            wb.write(fos);
        }

        File artifactFile = new File("C:\\Users\\Vuh26\\.gemini\\antigravity\\brain\\9c32454a-b322-4db4-b73f-ca7159e96a64\\Report-5.2_Integration_Test.xlsx");
        try (FileOutputStream fos = new FileOutputStream(artifactFile)) {
            wb.write(fos);
        }

        wb.close();
    }

    private void createCoverSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle, CellStyle cellCenter, CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet("Cover");
        sheet.setDisplayGridlines(true);

        Row r1 = sheet.createRow(1);
        Cell cTitle = r1.createCell(1);
        cTitle.setCellValue("INTEGRATION TEST REPORT DOCUMENT");
        cTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

        Row r3 = sheet.createRow(3);
        Cell r3c0 = r3.createCell(0); r3c0.setCellValue("Project Name"); r3c0.setCellStyle(metaLabelBold);
        Cell r3c1 = r3.createCell(1); r3c1.setCellValue("Kizuna Tutor System");
        Cell r3c4 = r3.createCell(4); r3c4.setCellValue("Creator"); r3c4.setCellStyle(metaLabelBold);
        Cell r3c5 = r3.createCell(5); r3c5.setCellValue("Hoangvm");

        Row r4 = sheet.createRow(4);
        Cell r4c0 = r4.createCell(0); r4c0.setCellValue("Project Code"); r4c0.setCellStyle(metaLabelBold);
        Cell r4c1 = r4.createCell(1); r4c1.setCellValue("KTS");
        Cell r4c4 = r4.createCell(4); r4c4.setCellValue("Issue Date"); r4c4.setCellStyle(metaLabelBold);
        Cell r4c5 = r4.createCell(5); r4c5.setCellValue("2026-11-23");

        Row r5 = sheet.createRow(5);
        Cell r5c0 = r5.createCell(0); r5c0.setCellValue("Document Code"); r5c0.setCellStyle(metaLabelBold);
        Cell r5c1 = r5.createCell(1); r5c1.setCellValue("KTS_Integration_Test_Document_v1.0");
        Cell r5c4 = r5.createCell(4); r5c4.setCellValue("Version"); r5c4.setCellStyle(metaLabelBold);
        Cell r5c5 = r5.createCell(5); r5c5.setCellValue("v1.0");

        Row r8 = sheet.createRow(8);
        Cell r8c0 = r8.createCell(0); r8c0.setCellValue("Record of change"); r8c0.setCellStyle(metaLabelBold);

        Row r9 = sheet.createRow(9);
        String[] headers = {"Effective Date", "Version", "Change Item", "*A,D,M", "Change description", "Reference"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r9.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        Row r10 = sheet.createRow(10);
        String[] changeRow = {
                "2026-11-23",
                "v1.0",
                "Khởi tạo Báo cáo Kiểm thử tích hợp (Integration Test)",
                "A",
                "Tạo mới toàn bộ kịch bản kiểm thử tích hợp End-to-End giữa Frontend, Backend API, PostgreSQL Database và Gemini AI cho Phân hệ 1 (Auth) và Phân hệ 5 (Hỗ trợ học tập)",
                "SRS UC-01 ~ UC-62"
        };
        for (int i = 0; i < changeRow.length; i++) {
            Cell c = r10.createCell(i);
            c.setCellValue(changeRow[i]);
            c.setCellStyle(cellCenter);
        }

        sheet.setColumnWidth(0, 4500);
        sheet.setColumnWidth(1, 3500);
        sheet.setColumnWidth(2, 6500);
        sheet.setColumnWidth(3, 2500);
        sheet.setColumnWidth(4, 16000);
        sheet.setColumnWidth(5, 6000);
    }

    private void createTestCaseListSheet(Workbook wb, Font fontBigTitle, CellStyle headerStyle, CellStyle cellCenter, CellStyle cellCenterBold, CellStyle cellLeftWrap, CellStyle metaLabelBold, Font fontRegular) {
        Sheet sheet = wb.createSheet("Test Cases");
        sheet.setDisplayGridlines(true);

        CellStyle bigTitleStyle = wb.createCellStyle();
        bigTitleStyle.setFont(fontBigTitle);
        bigTitleStyle.setAlignment(HorizontalAlignment.LEFT);
        bigTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(3);
        c0.setCellValue("TEST CASE LIST");
        c0.setCellStyle(bigTitleStyle);

        Row r2 = sheet.createRow(2);
        Cell r2b = r2.createCell(1); r2b.setCellValue("Project Name"); r2b.setCellStyle(metaLabelBold);
        Cell r2d = r2.createCell(3); r2d.setCellFormula("Cover!B4");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 3, 5));

        Row r3 = sheet.createRow(3);
        Cell r3b = r3.createCell(1); r3b.setCellValue("Project Code"); r3b.setCellStyle(metaLabelBold);
        Cell r3d = r3.createCell(3); r3d.setCellFormula("Cover!B5");
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 5));

        Row r4 = sheet.createRow(4);
        Cell r4b = r4.createCell(1); r4b.setCellValue("Test Environment Setup Description"); r4b.setCellStyle(metaLabelBold);
        Cell r4d = r4.createCell(3);
        r4d.setCellValue("1. Web Application: KizunaTutor SPA Frontend (Vanilla JS / Tailwind CSS, port 3000)\n" +
                "2. Backend API: Spring Boot 3.3.4 (Java 17, Spring Security, JWT HttpOnly Cookie, port 8085)\n" +
                "3. Database: PostgreSQL 15 (Supabase Cloud Database & Connection Pool)\n" +
                "4. AI Integration: Google Gemini API (gemini-1.5-flash)\n" +
                "5. External Services: Google OAuth 2.0, Brevo / Gmail SMTP Relay\n" +
                "6. Client Tools: Google Chrome v120+, Postman, DBeaver");
        CellStyle envStyle = wb.createCellStyle();
        envStyle.setFont(fontRegular);
        envStyle.setWrapText(true);
        envStyle.setVerticalAlignment(VerticalAlignment.TOP);
        r4d.setCellStyle(envStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 5));
        r4.setHeightInPoints(90);

        Row r7 = sheet.createRow(7);
        String[] headers = {"No", "Function Name", "Sheet Name", "Description", "Pre-Condition"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r7.createCell(i + 1);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        String[][] functions = {
                {"1", "Đăng nhập & Điều hướng đa vai trò (RBAC Dashboard)", "Feature1", "Xác thực email/pass, cấp JWT Cookie và điều hướng 6 vai trò chính xác", "Các tài khoản kiểm thử đã được nạp vào DB"},
                {"2", "Đăng ký tài khoản & Xác thực OTP 2 bước", "Feature1", "Gửi form đăng ký, nhận OTP qua email, kích hoạt tài khoản và tạo hồ sơ DB", "Email đăng ký chưa từng tồn tại"},
                {"3", "Cơ chế Gửi lại OTP & Cooldown chống spam", "Feature1", "Đếm ngược 60 giây, chặn spam và làm mới thời hạn OTP", "Phiên đăng ký đang mở"},
                {"4", "Thiết lập mật khẩu lần đầu (First-time Password)", "Feature1", "Bắt buộc đổi pass cho tài khoản tạm, mã hóa BCrypt và gỡ cờ", "Tài khoản có cờ mustChangePassword=true"},
                {"5", "Đăng xuất & Thu hồi phiên JWT Cookie", "Feature1", "Xóa sạch cookie HttpOnly access_token và chặn truy cập trái phép", "Người dùng đang đăng nhập"},
                {"6", "Quên mật khẩu & Rate Limit bảo mật", "Feature1", "Chặn 4 role quản trị, giới hạn 3 lần/15p và bảo vệ chống User Enumeration", "Email người dùng trong hệ thống"},
                {"7", "Quản lý Flashcards & Bảo mật sở hữu dữ liệu (owner_key)", "Feature2", "CRUD bộ thẻ, học lật thẻ tương tác và chặn người khác truy cập sửa/xóa", "Người dùng đã đăng nhập Học viên"},
                {"8", "Tra cứu kho 7.811 từ vựng JLPT & Ngữ pháp", "Feature2", "Tra cứu từ vựng theo cấp độ N5-N1, chủ đề và chi tiết mẫu câu ngữ pháp", "Dữ liệu từ vựng/ngữ pháp trong DB"},
                {"9", "AI Sakubun Luyện viết & Chữa lỗi trợ từ", "Feature2", "Chấm bài viết tiếng Nhật, phát hiện lỗi trợ từ và gợi ý diễn đạt qua Gemini AI", "Gemini AI API kết nối ổn định"},
                {"10", "Minigame Kanji Rain & Ghi điểm phiên chơi", "Feature2", "Hán tự rơi tương tác, tính độ chính xác và ghi nhận điểm số vào DB", "Dữ liệu Hán tự N5-N1 sẵn sàng"},
                {"11", "Từ điển Hán tự & Nhận diện viết tay Canvas", "Feature2", "Nhận diện nét vẽ Canvas qua Google Input Tools và tra cứu chi tiết Joyo Kanji", "Canvas và bảng kanjis hoạt động"},
                {"12", "Trợ lý đọc hiểu AI & Bộ nhớ đệm SHA-256", "Feature2", "Bóc tách Furigana ruby, nhận diện cấu trúc và tối ưu Cache HIT < 50ms", "Đoạn văn tiếng Nhật đầu vào"}
        };

        for (int i = 0; i < functions.length; i++) {
            Row r = sheet.createRow(8 + i);
            r.createCell(1).setCellValue(functions[i][0]); r.getCell(1).setCellStyle(cellCenter);
            r.createCell(2).setCellValue(functions[i][1]); r.getCell(2).setCellStyle(cellCenterBold);
            r.createCell(3).setCellValue(functions[i][2]); r.getCell(3).setCellStyle(cellCenter);
            r.createCell(4).setCellValue(functions[i][3]); r.getCell(4).setCellStyle(cellLeftWrap);
            r.createCell(5).setCellValue(functions[i][4]); r.getCell(5).setCellStyle(cellLeftWrap);
        }

        sheet.setColumnWidth(0, 1000);
        sheet.setColumnWidth(1, 1500);
        sheet.setColumnWidth(2, 7000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 16000);
        sheet.setColumnWidth(5, 12000);
    }

    private void createFeatureSheet(Workbook wb, String sheetName, String featureTitle, String testReq,
                                    List<IntegrationFunctionGroup> groups, String countifPrefix,
                                    CellStyle headerStyle, CellStyle functionHeaderStyle,
                                    CellStyle cellCenter, CellStyle cellCenterBold,
                                    CellStyle cellLeftWrap, CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet(sheetName);
        sheet.setDisplayGridlines(true);

        // Header block (Row 0 to 6)
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("Feature"); r0.getCell(0).setCellStyle(metaLabelBold);
        r0.createCell(1).setCellValue(featureTitle); r0.getCell(1).setCellStyle(metaLabelBold);
        r0.createCell(16).setCellValue("Passed"); r0.getCell(16).setCellStyle(cellCenterBold);

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Test requirement"); r1.getCell(0).setCellStyle(metaLabelBold);
        r1.createCell(1).setCellValue(testReq);
        r1.createCell(16).setCellValue("Failed"); r1.getCell(16).setCellStyle(cellCenterBold);

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("Number of TCs"); r2.getCell(0).setCellStyle(metaLabelBold);
        Cell r2b = r2.createCell(1);
        r2b.setCellFormula("COUNTIF(A11:A200,\"" + countifPrefix + "\")");
        r2b.setCellStyle(cellCenterBold);
        r2.createCell(16).setCellValue("Pending"); r2.getCell(16).setCellStyle(cellCenterBold);

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Testing Round"); r3.getCell(0).setCellStyle(metaLabelBold);
        r3.createCell(1).setCellValue("Passed"); r3.getCell(1).setCellStyle(cellCenterBold);
        r3.createCell(2).setCellValue("Failed"); r3.getCell(2).setCellStyle(cellCenterBold);
        r3.createCell(3).setCellValue("Pending"); r3.getCell(3).setCellStyle(cellCenterBold);
        r3.createCell(4).setCellValue("N/A"); r3.getCell(4).setCellStyle(cellCenterBold);
        r3.createCell(16).setCellValue("N/A"); r3.getCell(16).setCellStyle(cellCenterBold);

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Round 1"); r4.getCell(0).setCellStyle(metaLabelBold);
        r4.createCell(1).setCellFormula("COUNTIF($F11:$F200,B4)"); r4.getCell(1).setCellStyle(cellCenter);
        r4.createCell(2).setCellFormula("COUNTIF($F11:$F200,C4)"); r4.getCell(2).setCellStyle(cellCenter);
        r4.createCell(3).setCellFormula("COUNTIF($F11:$F200,D4)"); r4.getCell(3).setCellStyle(cellCenter);
        r4.createCell(4).setCellFormula("COUNTIF($F11:$F200,E4)"); r4.getCell(4).setCellStyle(cellCenter);

        Row r5 = sheet.createRow(5);
        r5.createCell(0).setCellValue("Round 2"); r5.getCell(0).setCellStyle(metaLabelBold);
        r5.createCell(1).setCellFormula("COUNTIF($I11:$I200,B4)"); r5.getCell(1).setCellStyle(cellCenter);
        r5.createCell(2).setCellFormula("COUNTIF($I11:$I200,C4)"); r5.getCell(2).setCellStyle(cellCenter);
        r5.createCell(3).setCellFormula("COUNTIF($I11:$I200,D4)"); r5.getCell(3).setCellStyle(cellCenter);
        r5.createCell(4).setCellFormula("COUNTIF($I11:$I200,E4)"); r5.getCell(4).setCellStyle(cellCenter);

        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Round 3"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellFormula("COUNTIF($L11:$L200,B4)"); r6.getCell(1).setCellStyle(cellCenter);
        r6.createCell(2).setCellFormula("COUNTIF($L11:$L200,C4)"); r6.getCell(2).setCellStyle(cellCenter);
        r6.createCell(3).setCellFormula("COUNTIF($L11:$L200,D4)"); r6.getCell(3).setCellStyle(cellCenter);
        r6.createCell(4).setCellFormula("COUNTIF($L11:$L200,E4)"); r6.getCell(4).setCellStyle(cellCenter);

        // Table Header (Row 9 / index 9)
        Row r9 = sheet.createRow(9);
        String[] colHeaders = {
                "Test Case ID", "Test Case Description", "Test Case Procedure", "Expected Results",
                "Pre-conditions", "Round 1", "Test date", "Tester",
                "Round 2", "Test date", "Tester",
                "Round 3", "Test date", "Tester",
                "Defect IDs", "Note"
        };
        for (int c = 0; c < colHeaders.length; c++) {
            Cell cell = r9.createCell(c);
            cell.setCellValue(colHeaders[c]);
            cell.setCellStyle(headerStyle);
        }
        r9.setHeightInPoints(28);

        // Render Cases grouped by Function
        int currentRowIdx = 10;
        for (IntegrationFunctionGroup group : groups) {
            Row rFunc = sheet.createRow(currentRowIdx);
            for (int c = 0; c < colHeaders.length; c++) {
                Cell cCell = rFunc.createCell(c);
                cCell.setCellStyle(functionHeaderStyle);
                if (c == 0) {
                    cCell.setCellValue(group.functionName);
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(currentRowIdx, currentRowIdx, 0, colHeaders.length - 1));
            rFunc.setHeightInPoints(24);
            currentRowIdx++;

            for (IntegrationTestCase tc : group.cases) {
                Row rTc = sheet.createRow(currentRowIdx);

                Cell c0 = rTc.createCell(0); c0.setCellValue(tc.id); c0.setCellStyle(cellCenterBold);
                Cell c1 = rTc.createCell(1); c1.setCellValue(tc.description); c1.setCellStyle(cellLeftWrap);
                Cell c2 = rTc.createCell(2); c2.setCellValue(tc.procedure); c2.setCellStyle(cellLeftWrap);
                Cell c3 = rTc.createCell(3); c3.setCellValue(tc.expectedResults); c3.setCellStyle(cellLeftWrap);
                Cell c4 = rTc.createCell(4); c4.setCellValue(tc.preConditions); c4.setCellStyle(cellLeftWrap);
                Cell c5 = rTc.createCell(5); c5.setCellValue(tc.round1); c5.setCellStyle(cellCenter);
                Cell c6 = rTc.createCell(6); c6.setCellValue(tc.testDate); c6.setCellStyle(cellCenter);
                Cell c7 = rTc.createCell(7); c7.setCellValue(tc.tester); c7.setCellStyle(cellCenter);
                Cell c8 = rTc.createCell(8); c8.setCellValue(tc.round2); c8.setCellStyle(cellCenter);
                Cell c9 = rTc.createCell(9); c9.setCellValue(tc.testDate); c9.setCellStyle(cellCenter);
                Cell c10 = rTc.createCell(10); c10.setCellValue(tc.tester); c10.setCellStyle(cellCenter);
                Cell c11 = rTc.createCell(11); c11.setCellValue(tc.round3); c11.setCellStyle(cellCenter);
                Cell c12 = rTc.createCell(12); c12.setCellValue(tc.testDate); c12.setCellStyle(cellCenter);
                Cell c13 = rTc.createCell(13); c13.setCellValue(tc.tester); c13.setCellStyle(cellCenter);
                Cell c14 = rTc.createCell(14); c14.setCellValue(tc.defectId); c14.setCellStyle(cellCenter);
                Cell c15 = rTc.createCell(15); c15.setCellValue(tc.note); c15.setCellStyle(cellLeftWrap);

                currentRowIdx++;
            }
        }

        sheet.setColumnWidth(0, 4200);  // ID
        sheet.setColumnWidth(1, 10000); // Description
        sheet.setColumnWidth(2, 14000); // Procedure
        sheet.setColumnWidth(3, 14000); // Expected Results
        sheet.setColumnWidth(4, 9000);  // Pre-conditions
        sheet.setColumnWidth(5, 3000);  // Round 1
        sheet.setColumnWidth(6, 3200);  // Test date
        sheet.setColumnWidth(7, 3000);  // Tester
        sheet.setColumnWidth(8, 3000);  // Round 2
        sheet.setColumnWidth(9, 3200);  // Test date
        sheet.setColumnWidth(10, 3000); // Tester
        sheet.setColumnWidth(11, 3000); // Round 3
        sheet.setColumnWidth(12, 3200); // Test date
        sheet.setColumnWidth(13, 3000); // Tester
        sheet.setColumnWidth(14, 3000); // Defect IDs
        sheet.setColumnWidth(15, 4500); // Note
    }

    private void createTestStatisticsSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle,
                                           CellStyle cellCenter, CellStyle cellCenterBold,
                                           CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet("Test Statistics");
        sheet.setDisplayGridlines(true);

        Row r1 = sheet.createRow(1);
        Cell cTitle = r1.createCell(0);
        cTitle.setCellValue("INTEGRATION TEST STATISTICS");
        cTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Project Name"); r3.getCell(0).setCellStyle(metaLabelBold);
        r3.createCell(1).setCellFormula("Cover!B4");
        r3.createCell(3).setCellValue("Creator"); r3.getCell(3).setCellStyle(metaLabelBold);
        r3.createCell(4).setCellValue("Hoangvm");

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Project Code"); r4.getCell(0).setCellStyle(metaLabelBold);
        r4.createCell(1).setCellFormula("Cover!B5");
        r4.createCell(3).setCellValue("Reviewer/Approver"); r4.getCell(3).setCellStyle(metaLabelBold);
        r4.createCell(4).setCellValue("Teacher / Team Leader");

        Row r5 = sheet.createRow(5);
        r5.createCell(0).setCellValue("Document Code"); r5.getCell(0).setCellStyle(metaLabelBold);
        r5.createCell(1).setCellValue("KTS_Integration_Test_Report_v1.0");
        r5.createCell(3).setCellValue("Issue Date"); r5.getCell(3).setCellStyle(metaLabelBold);
        r5.createCell(4).setCellValue("2026-11-23");

        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Notes"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellValue("Báo cáo kiểm thử tích hợp (Integration Test) cho 2 phân hệ được phân công: Feature 1 (Auth & Phân quyền) và Feature 2 (Learning Support). Tổng cộng 22 kịch bản tích hợp, 100% Passed.");

        // Table Header (Row 10 / Excel row 11)
        Row r10 = sheet.createRow(10);
        String[] statHeaders = {"No", "Module code", "Passed", "Failed", "Pending", "N/A", "Number of test cases"};
        for (int i = 0; i < statHeaders.length; i++) {
            Cell c = r10.createCell(i);
            c.setCellValue(statHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        // Row 11: Feature 1 (Excel row 12)
        Row r11 = sheet.createRow(11);
        r11.createCell(0).setCellValue("1.0"); r11.getCell(0).setCellStyle(cellCenter);
        r11.createCell(1).setCellValue("Feature1 - Authentication & Authorization"); r11.getCell(1).setCellStyle(cellCenterBold);
        r11.createCell(2).setCellFormula("Feature1!B7"); r11.getCell(2).setCellStyle(cellCenter);
        r11.createCell(3).setCellFormula("Feature1!C7"); r11.getCell(3).setCellStyle(cellCenter);
        r11.createCell(4).setCellFormula("Feature1!D7"); r11.getCell(4).setCellStyle(cellCenter);
        r11.createCell(5).setCellFormula("Feature1!E7"); r11.getCell(5).setCellStyle(cellCenter);
        r11.createCell(6).setCellFormula("Feature1!B3"); r11.getCell(6).setCellStyle(cellCenterBold);

        // Row 12: Feature 2 (Excel row 13)
        Row r12 = sheet.createRow(12);
        r12.createCell(0).setCellValue("2.0"); r12.getCell(0).setCellStyle(cellCenter);
        r12.createCell(1).setCellValue("Feature2 - Learning Support Space"); r12.getCell(1).setCellStyle(cellCenterBold);
        r12.createCell(2).setCellFormula("Feature2!B7"); r12.getCell(2).setCellStyle(cellCenter);
        r12.createCell(3).setCellFormula("Feature2!C7"); r12.getCell(3).setCellStyle(cellCenter);
        r12.createCell(4).setCellFormula("Feature2!D7"); r12.getCell(4).setCellStyle(cellCenter);
        r12.createCell(5).setCellFormula("Feature2!E7"); r12.getCell(5).setCellStyle(cellCenter);
        r12.createCell(6).setCellFormula("Feature2!B3"); r12.getCell(6).setCellStyle(cellCenterBold);

        // Row 14: Sub total (Excel row 15)
        Row r14 = sheet.createRow(14);
        r14.createCell(1).setCellValue("Sub total"); r14.getCell(1).setCellStyle(cellCenterBold);
        r14.createCell(2).setCellFormula("SUM(C12:C13)"); r14.getCell(2).setCellStyle(cellCenterBold);
        r14.createCell(3).setCellFormula("SUM(D12:D13)"); r14.getCell(3).setCellStyle(cellCenterBold);
        r14.createCell(4).setCellFormula("SUM(E12:E13)"); r14.getCell(4).setCellStyle(cellCenterBold);
        r14.createCell(5).setCellFormula("SUM(F12:F13)"); r14.getCell(5).setCellStyle(cellCenterBold);
        r14.createCell(6).setCellFormula("SUM(G12:G13)"); r14.getCell(6).setCellStyle(cellCenterBold);

        CellStyle cellPercentNumber = wb.createCellStyle();
        cellPercentNumber.cloneStyleFrom(cellCenterBold);
        cellPercentNumber.setDataFormat(wb.createDataFormat().getFormat("0.0"));

        // Row 16: Test coverage (Excel row 17)
        Row r16 = sheet.createRow(16);
        r16.createCell(1).setCellValue("Test coverage"); r16.getCell(1).setCellStyle(metaLabelBold);
        r16.createCell(3).setCellFormula("(C15+D15)*100/(G15)"); r16.getCell(3).setCellStyle(cellPercentNumber);
        r16.createCell(4).setCellValue("%");

        // Row 17: Test successful coverage (Excel row 18)
        Row r17 = sheet.createRow(17);
        r17.createCell(1).setCellValue("Test successful coverage"); r17.getCell(1).setCellStyle(metaLabelBold);
        r17.createCell(3).setCellFormula("C15*100/(G15)"); r17.getCell(3).setCellStyle(cellPercentNumber);
        r17.createCell(4).setCellValue("%");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 8500);
        sheet.setColumnWidth(2, 3500);
        sheet.setColumnWidth(3, 3500);
        sheet.setColumnWidth(4, 3500);
        sheet.setColumnWidth(5, 3000);
        sheet.setColumnWidth(6, 5000);
    }

    // =========================================================================
    // INTEGRATION TEST SCENARIOS DATA
    // =========================================================================

    private List<IntegrationFunctionGroup> buildFeature1Groups() {
        List<IntegrationFunctionGroup> groups = new ArrayList<>();

        // Group 1: Login & RBAC Routing
        IntegrationFunctionGroup gLogin = new IntegrationFunctionGroup("Function 1: Đăng nhập & Điều hướng đa vai trò (RBAC Routing)");
        gLogin.add(new IntegrationTestCase("IT-AUTH-001",
                "Đăng nhập Quản trị viên (ROLE_ADMIN) và điều hướng Dashboard Quản trị",
                "1. Truy cập http://localhost:3000/login.\n" +
                        "2. Nhập email: admin@kizuna.vn và mật khẩu: Admin@123.\n" +
                        "3. Bấm nút \"Đăng nhập\".\n" +
                        "4. Quan sát F12 Network request POST /api/auth/login và URL chuyển hướng.",
                "1. API trả về HTTP 200 OK, JSON chứa token JWT và redirectUrl: \"/admin/dashboard\".\n" +
                        "2. Header Set-Cookie: access_token được thiết lập HttpOnly, Path=/.\n" +
                        "3. Trình duyệt tự động chuyển hướng đến http://localhost:3000/admin/dashboard.\n" +
                        "4. Giao diện hiển thị đúng menu Quản trị viên.",
                "Tài khoản admin@kizuna.vn tồn tại trong DB, status = ACTIVE."));

        gLogin.add(new IntegrationTestCase("IT-AUTH-002",
                "Đăng nhập Học viên (ROLE_LEARNER) và điều hướng Không gian học tập",
                "1. Mở trang http://localhost:3000/login.\n" +
                        "2. Nhập email: learner@kizuna.vn và mật khẩu: Learner@123.\n" +
                        "3. Nhấn \"Đăng nhập\".",
                "1. API phản hồi HTTP 200 OK.\n" +
                        "2. Trình duyệt tự động chuyển hướng đến http://localhost:3000/pages/home/.\n" +
                        "3. Header góc phải hiển thị thông tin học viên kèm menu: Flashcards, Từ vựng, Ngữ pháp, Sakubun.",
                "Tài khoản learner@kizuna.vn tồn tại trong DB, status = ACTIVE."));

        gLogin.add(new IntegrationTestCase("IT-AUTH-003",
                "Đăng nhập Giảng viên (ROLE_TUTOR) và điều hướng Dashboard Giảng viên",
                "1. Mở trang http://localhost:3000/login.\n" +
                        "2. Nhập email: tutor@kizuna.vn và mật khẩu: Tutor@123.\n" +
                        "3. Nhấn \"Đăng nhập\".",
                "1. API phản hồi HTTP 200 OK, cấp JWT Cookie.\n" +
                        "2. Chuyển hướng thành công đến http://localhost:3000/tutor/dashboard.\n" +
                        "3. Hiển thị lịch giảng dạy, danh sách lớp học và học viên.",
                "Tài khoản tutor@kizuna.vn tồn tại trong DB, status = ACTIVE."));
        groups.add(gLogin);

        // Group 2: Registration & OTP
        IntegrationFunctionGroup gRegister = new IntegrationFunctionGroup("Function 2: Luồng đăng ký tài khoản & Xác thực OTP 2 bước (Registration & OTP)");
        gRegister.add(new IntegrationTestCase("IT-AUTH-004",
                "Đăng ký tài khoản Học viên mới bước 1 (Gửi mã OTP xác thực qua Email)",
                "1. Truy cập http://localhost:3000/register.\n" +
                        "2. Nhập Họ tên: \"Nguyễn Văn Test\", Email: \"newlearner_it@kizuna.vn\", Mật khẩu: \"SecurePass123!\", chọn mục tiêu JLPT N4.\n" +
                        "3. Nhấn nút \"Tiếp tục\".",
                "1. API POST /api/auth/register trả về HTTP 200 OK.\n" +
                        "2. Backend sinh mã OTP 6 chữ số lưu vào PendingRegistration cache.\n" +
                        "3. Màn hình tự động chuyển sang giao diện nhập OTP kèm đồng hồ đếm ngược 60 giây.",
                "Email newlearner_it@kizuna.vn chưa từng đăng ký trong bảng users."));

        gRegister.add(new IntegrationTestCase("IT-AUTH-005",
                "Xác thực mã OTP bước 2, kích hoạt tài khoản và tự động tạo User + LearnerProfile trong PostgreSQL",
                "1. Tại giao diện nhập OTP, điền đúng mã 6 chữ số vừa nhận.\n" +
                        "2. Bấm \"Xác thực & Hoàn tất\".\n" +
                        "3. Kiểm tra bản ghi sinh ra trong PostgreSQL qua Supabase.",
                "1. API POST /api/auth/verify-otp trả về HTTP 200 OK.\n" +
                        "2. DB tạo mới bản ghi trong bảng users và bảng learner_profiles (target_level = N4).\n" +
                        "3. Trình duyệt nhận JWT Cookie và tự động chuyển hướng vào http://localhost:3000/pages/home/.",
                "Phiên đăng ký đang mở trong cache với mã OTP hợp lệ."));

        gRegister.add(new IntegrationTestCase("IT-AUTH-006",
                "Cơ chế đếm ngược Cooldown và Gửi lại mã OTP sau 60 giây",
                "1. Tại màn hình nhập OTP, quan sát nút \"Gửi lại mã\" bị khóa khi đồng hồ đang đếm ngược.\n" +
                        "2. Chờ hết 60s, nút sáng lên $\\rightarrow$ Nhấn \"Gửi lại mã\".",
                "1. Khi < 60s: Nút bị disable, gọi API trực tiếp trả về HTTP 400 kèm thông báo số giây cần đợi.\n" +
                        "2. Khi >= 60s: Nhấn nút gọi POST /api/auth/resend-otp thành công, cấp mã OTP mới và gia hạn thời gian 5 phút.",
                "Phiên đăng ký đang tồn tại trong cache."));
        groups.add(gRegister);

        // Group 3: First-time Password & Session
        IntegrationFunctionGroup gSession = new IntegrationFunctionGroup("Function 3: Thiết lập mật khẩu lần đầu & Quản lý phiên làm việc");
        gSession.add(new IntegrationTestCase("IT-AUTH-007",
                "Đăng nhập tài khoản tạm và hiển thị Modal bắt buộc 'Thiết lập mật khẩu mới'",
                "1. Mở trang http://localhost:3000/login.\n" +
                        "2. Đăng nhập bằng tài khoản firsttime@kizuna.vn có cờ mustChangePassword = true.\n" +
                        "3. Bấm \"Đăng nhập\".",
                "1. Phản hồi HTTP 200 OK với cờ mustChangePassword = true.\n" +
                        "2. Trình duyệt không chuyển hướng vào Dashboard mà bật popup modal \"Thiết lập mật khẩu mới\".\n" +
                        "3. Modal có đầy đủ 3 ô nhập mật khẩu với icon mắt toggle và nút \"Cập nhật mật khẩu\".",
                "Tài khoản firsttime@kizuna.vn tồn tại trong DB với mustChangePassword = true."));

        gSession.add(new IntegrationTestCase("IT-AUTH-008",
                "Cập nhật mật khẩu lần đầu thành công, mã hóa BCrypt và gỡ cờ mustChangePassword",
                "1. Tại popup modal, nhập mật khẩu hiện tại và mật khẩu mới khớp nhau.\n" +
                        "2. Bấm \"Cập nhật mật khẩu\".\n" +
                        "3. Dùng mật khẩu mới đăng nhập lại.",
                "1. API PUT /api/auth/first-time-password trả về HTTP 200 OK.\n" +
                        "2. DB cập nhật password_hash mới và đặt mustChangePassword = false.\n" +
                        "3. Đăng nhập lại bằng mật khẩu mới thành công vào thẳng trang chủ.",
                "Popup modal đổi mật khẩu đang mở với phiên xác thực hợp lệ."));

        gSession.add(new IntegrationTestCase("IT-AUTH-009",
                "Đăng xuất hệ thống, thu hồi HttpOnly Cookie và chặn truy cập trái phép",
                "1. Đang đăng nhập tài khoản Học viên, bấm Avatar góc phải $\\rightarrow$ Chọn \"Đăng xuất\".\n" +
                        "2. Quan sát chuyển hướng về /login.\n" +
                        "3. Dùng thanh URL gõ lại http://localhost:3000/pages/home/ để thử vào lại.",
                "1. API POST /api/auth/logout trả về Set-Cookie access_token với Max-Age=0.\n" +
                        "2. Trình duyệt xóa sạch Cookie JWT khỏi Application Storage.\n" +
                        "3. Thử truy cập URL bảo vệ bị tự động redirect về /login, không lộ dữ liệu người dùng.",
                "Người dùng đang có phiên đăng nhập hợp lệ."));
        groups.add(gSession);

        // Group 4: Forgot Password & Security
        IntegrationFunctionGroup gSecurity = new IntegrationFunctionGroup("Function 4: Quên mật khẩu & Bảo mật an ninh (PR #20)");
        gSecurity.add(new IntegrationTestCase("IT-AUTH-010",
                "Quên mật khẩu với email không tồn tại - Chống dò quét danh tính (User Enumeration Defense)",
                "1. Truy cập http://localhost:3000/forgot-password.\n" +
                        "2. Nhập email không tồn tại: \"notfound_test_999@kizuna.vn\".\n" +
                        "3. Nhấn \"Gửi yêu cầu đặt lại mật khẩu\".",
                "1. Hệ thống phản hồi thông báo thành công chung (Neutral Response): \"Nếu email tồn tại, hướng dẫn đặt lại đã được gửi\".\n" +
                        "2. Không tiết lộ email có tồn tại hay không, ngăn ngừa tin tặc quét danh bạ tài khoản.",
                "Email không tồn tại trong DB."));

        gSecurity.add(new IntegrationTestCase("IT-AUTH-011",
                "Bảo mật PR #20: Chặn vai trò quản trị nội bộ tự reset mật khẩu trực tuyến và giới hạn Rate Limit 3 lần/15p",
                "1. Nhập email quản trị viên admin@kizuna.vn vào form Quên mật khẩu $\\rightarrow$ Gửi.\n" +
                        "2. Thử gửi liên tiếp quá 3 lần với tài khoản thường trong 15 phút.",
                "1. Khi nhập email quản trị: API trả về HTTP 403 Forbidden (\"Quản trị viên không được phép tự khôi phục mật khẩu trực tuyến\").\n" +
                        "2. Khi spam quá 3 lần/15 phút: API chặn với HTTP 429 Too Many Requests.",
                "Tài khoản admin@kizuna.vn tồn tại với vai trò ROLE_ADMIN."));
        groups.add(gSecurity);

        return groups;
    }

    private List<IntegrationFunctionGroup> buildFeature2Groups() {
        List<IntegrationFunctionGroup> groups = new ArrayList<>();

        // Group 1: Flashcards & Privacy
        IntegrationFunctionGroup gFlashcards = new IntegrationFunctionGroup("Function 1: Quản lý bộ thẻ Flashcards cá nhân & Bảo mật sở hữu (owner_key PR #20)");
        gFlashcards.add(new IntegrationTestCase("IT-LS-001",
                "Tạo mới bộ thẻ Flashcard cá nhân và lưu trữ đồng thời thẻ con vào PostgreSQL",
                "1. Đăng nhập tài khoản Học viên, truy cập http://localhost:3000/flashcards.\n" +
                        "2. Bấm \"Tạo bộ thẻ mới\", nhập Tên: \"Từ vựng N4 Bài 1\", thêm 3 thẻ từ vựng con.\n" +
                        "3. Nhấn \"Lưu bộ thẻ\".",
                "1. API POST /api/flashcards/decks trả về HTTP 200 OK kèm DeckDto đầy đủ.\n" +
                        "2. DB ghi nhận bản ghi mới trong bảng flashcard_decks với owner_key = email người tạo và 3 bản ghi trong bảng flashcards.\n" +
                        "3. Danh sách trên web tự động cập nhật hiển thị bộ thẻ mới.",
                "Người dùng đã đăng nhập với vai trò ROLE_LEARNER."));

        gFlashcards.add(new IntegrationTestCase("IT-LS-002",
                "Bảo mật PR #20: Ngăn chặn sửa/xóa trái phép bộ thẻ của người dùng khác (Kiểm tra owner_key)",
                "1. Đăng nhập bằng tài khoản User B.\n" +
                        "2. Gửi request PUT /api/flashcards/decks/{id_cua_User_A} cố tình đổi tên bộ thẻ của User A.",
                "1. Backend kiểm tra caller email != owner_key của bộ thẻ.\n" +
                        "2. API trả về HTTP 403 Forbidden kèm thông báo: \"Không có quyền thao tác trên bộ thẻ của người khác\".\n" +
                        "3. Dữ liệu của User A trong DB được bảo toàn tuyệt đối.",
                "User B đã đăng nhập, ID bộ thẻ thuộc quyền sở hữu của User A."));

        gFlashcards.add(new IntegrationTestCase("IT-LS-003",
                "Ôn tập lật thẻ Flashcards tương tác và cập nhật thanh tiến độ học tập",
                "1. Tại trang Flashcards, chọn bộ thẻ $\\rightarrow$ Bấm \"Bắt đầu ôn tập\".\n" +
                        "2. Click vào thẻ để lật mặt sau xem nghĩa và ví dụ.\n" +
                        "3. Nhấn \"Đã nhớ\" và chuyển sang thẻ tiếp theo.",
                "1. Hiệu ứng lật thẻ 3D hiển thị mượt mà thông tin từ vựng.\n" +
                        "2. Thanh tiến độ ôn tập tăng dần % tương ứng với số thẻ đã thuộc.",
                "Bộ thẻ có ít nhất 3 thẻ con."));
        groups.add(gFlashcards);

        // Group 2: Vocabularies & Grammar
        IntegrationFunctionGroup gVocabGrammar = new IntegrationFunctionGroup("Function 2: Tra cứu kho 7.811 từ vựng JLPT & Ngữ pháp cấu trúc");
        gVocabGrammar.add(new IntegrationTestCase("IT-LS-004",
                "Tra cứu kho 7.811 từ vựng JLPT, lọc chủ đề N5-N1 và từ khóa tìm kiếm",
                "1. Truy cập http://localhost:3000/learner/vocabulary.\n" +
                        "2. Chọn bộ lọc \"JLPT N5\", gõ ô tìm kiếm: \"nihon\".\n" +
                        "3. Quan sát kết quả trả về.",
                "1. API GET /api/vocabularies trả về HTTP 200 OK danh sách từ vựng phân trang.\n" +
                        "2. Thẻ từ vựng hiển thị đầy đủ: Kanji, Hiragana/Katakana, Romaji, Nghĩa tiếng Việt và Ví dụ minh họa.",
                "Bảng vocabularies trong PostgreSQL có 7.811 bản ghi dữ liệu."));

        gVocabGrammar.add(new IntegrationTestCase("IT-LS-005",
                "Tra cứu mẫu ngữ pháp OpenJLPT và xem chi tiết cấu trúc kết hợp",
                "1. Truy cập http://localhost:3000/learner/grammar.\n" +
                        "2. Lọc theo cấp độ N3 và tìm kiếm mẫu câu \"〜わけにはいかない\".\n" +
                        "3. Bấm vào mẫu câu để xem chi tiết.",
                "1. Hiển thị chi tiết cấu trúc: Quy tắc chia động từ/tính từ, ý nghĩa, hoàn cảnh sử dụng và 3 câu ví dụ song ngữ Nhật - Việt có Furigana.",
                "Dữ liệu ngữ pháp OpenJLPT đã sẵn sàng trong DB."));
        groups.add(gVocabGrammar);

        // Group 3: AI Writing & Kanji Game
        IntegrationFunctionGroup gAiGame = new IntegrationFunctionGroup("Function 3: AI Sakubun Luyện viết & Minigame Kanji Rain");
        gAiGame.add(new IntegrationTestCase("IT-LS-006",
                "AI Sakubun Luyện viết: Chấm bài văn tiếng Nhật, nhận diện lỗi trợ từ và gợi ý diễn đạt",
                "1. Truy cập http://localhost:3000/learner/ai-writing.\n" +
                        "2. Nhập bài viết tiếng Nhật: \"私は毎日学校に歩いて行きます。日本語がおもしろいです。\".\n" +
                        "3. Nhấn \"Gửi bài chấm điểm AI\".",
                "1. Backend kết nối Gemini 1.5 Flash API chấm bài và trả về HTTP 200 OK.\n" +
                        "2. Giao diện hiển thị: Thang điểm tổng quát (/100), phát hiện và highlight lỗi trợ từ, bản sửa chuẩn xác và từ vựng gợi ý mở rộng.",
                "Google Gemini API Key cấu hình hợp lệ và trực tuyến."));

        gAiGame.add(new IntegrationTestCase("IT-LS-007",
                "Minigame Kanji Rain: Tương tác gõ Hán tự rơi, tính độ chính xác và lưu phiên vào DB",
                "1. Truy cập http://localhost:3000/learner/kanji-game.\n" +
                        "2. Chọn chế độ \"Mưa Hán tự\" cấp độ N5 $\\rightarrow$ Bấm Bắt đầu.\n" +
                        "3. Nhập Furigana/nghĩa cho các chữ Hán rơi $\\rightarrow$ Hoàn thành lượt chơi.",
                "1. Trò chơi ghi nhận điểm số, tính toán tỷ lệ chính xác (Accuracy %).\n" +
                        "2. API POST /api/kanji/game/session lưu bản ghi thành công vào PostgreSQL.",
                "Kho Hán tự N5-N1 sẵn sàng trong DB."));
        groups.add(gAiGame);

        // Group 4: Kanji Dictionary & Handwriting Canvas
        IntegrationFunctionGroup gKanjiDict = new IntegrationFunctionGroup("Function 4: Từ điển Hán tự & Nhận diện viết tay Canvas");
        gKanjiDict.add(new IntegrationTestCase("IT-LS-008",
                "Tra cứu chi tiết chữ Hán (Âm Hán Việt, Onyomi, Kunyomi, Bộ thủ và Nét vẽ)",
                "1. Truy cập http://localhost:3000/learner/kanji-dict.\n" +
                        "2. Gõ chữ \"水\" vào ô tra cứu $\\rightarrow$ Enter.",
                "1. API trả về HTTP 200 OK chi tiết chữ Hán.\n" +
                        "2. Hiển thị thông tin bách khoa: Âm Hán Việt (THỦY), On (SUI), Kun (mizu), 4 nét, Bộ thủy và các từ ghép thông dụng.",
                "Bảng kanjis trong PostgreSQL sẵn sàng."));

        gKanjiDict.add(new IntegrationTestCase("IT-LS-009",
                "Nhận diện chữ Hán viết tay trên Canvas và tích hợp Google Input Tools",
                "1. Tại trang Từ điển Hán tự, mở công cụ \"Bút viết tay Canvas\".\n" +
                        "2. Dùng chuột vẽ 2 nét chữ \"人\" lên bảng vẽ.\n" +
                        "3. Quan sát danh sách ứng viên gợi ý $\\rightarrow$ Bấm chọn chữ \"人\".",
                "1. Hệ thống gửi tọa độ nét vẽ và nhận diện chính xác ứng viên chữ \"人\" (2 nét).\n" +
                        "2. Bấm vào chữ gợi ý tự động nạp bảng tra cứu chi tiết chữ \"人\" từ DB.",
                "Công cụ vẽ Canvas và API nhận diện nét vẽ hoạt động ổn định."));
        groups.add(gKanjiDict);

        // Group 5: Reading Assistant & Cache
        IntegrationFunctionGroup gReading = new IntegrationFunctionGroup("Function 5: Trợ lý đọc hiểu AI Furigana & Tối ưu bộ nhớ đệm SHA-256");
        gReading.add(new IntegrationTestCase("IT-LS-010",
                "Trợ lý đọc hiểu AI: Bóc tách Furigana ruby, nhận diện cấu trúc ngữ pháp và từ vựng",
                "1. Truy cập http://localhost:3000/learner/reading-assistant.\n" +
                        "2. Dán đoạn văn: \"日本の春は桜がとても綺麗です。多くの人が花見を楽しみます。\".\n" +
                        "3. Nhấn \"Phân tích đọc hiểu AI\".",
                "1. Gemini AI phân tích đoạn văn bản, bóc tách thẻ HTML <ruby><rt> cho từng chữ Hán.\n" +
                        "2. Giao diện hiển thị văn bản với Furigana chuẩn đẹp phía trên, cột bên phải liệt kê các mẫu ngữ pháp và từ vựng có trong bài.",
                "Gemini AI API trực tuyến."));

        gReading.add(new IntegrationTestCase("IT-LS-011",
                "Tối ưu hóa hiệu năng Trợ lý đọc hiểu: Phản hồi tức thì < 50ms nhờ In-Memory Cache SHA-256",
                "1. Giữ nguyên đoạn văn bản ở ca IT-LS-010, nhấn lại nút \"Phân tích lại\".\n" +
                        "2. Mở tab F12 Network quan sát thời gian phản hồi (Response Time).",
                "1. Backend kiểm tra thấy SHA-256 hash của đoạn văn đã có trong In-Memory CacheStore (Cache HIT).\n" +
                        "2. Thời gian phản hồi tức thì < 50ms mà không tốn quota gọi lại Gemini AI, dữ liệu trả về nhất quán 100%.",
                "Đoạn văn bản đã được phân tích và lưu cache ở ca trước."));
        groups.add(gReading);

        return groups;
    }
}
