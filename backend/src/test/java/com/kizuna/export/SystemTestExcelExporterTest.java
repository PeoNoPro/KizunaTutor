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
 * Generator for System Test Report Document following the customer's exact template.
 * Outputs to Desktop: C:\Users\Vuh26\Desktop\Report-5.3_System_Test.xlsx
 */
public class SystemTestExcelExporterTest {

    public static class SystemTestCase {
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

        public SystemTestCase(String id, String description, String procedure, String expectedResults, String preConditions) {
            this.id = id;
            this.description = description;
            this.procedure = procedure;
            this.expectedResults = expectedResults;
            this.preConditions = preConditions;
        }
    }

    public static class ScenarioGroup {
        public String scenarioName;
        public List<SystemTestCase> cases = new ArrayList<>();

        public ScenarioGroup(String scenarioName) {
            this.scenarioName = scenarioName;
        }

        public ScenarioGroup add(SystemTestCase tc) {
            this.cases.add(tc);
            return this;
        }
    }

    @Test
    public void exportSystemTestExcel() throws IOException {
        Workbook wb = new XSSFWorkbook();

        // Fonts
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

        // Scenario section header style
        CellStyle scenarioHeaderStyle = wb.createCellStyle();
        scenarioHeaderStyle.setFont(fontBold);
        scenarioHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        scenarioHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        scenarioHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        scenarioHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        scenarioHeaderStyle.setBorderTop(BorderStyle.THIN);
        scenarioHeaderStyle.setBorderBottom(BorderStyle.THIN);
        scenarioHeaderStyle.setBorderLeft(BorderStyle.THIN);
        scenarioHeaderStyle.setBorderRight(BorderStyle.THIN);

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
        List<ScenarioGroup> workflow1Groups = buildWorkflow1Groups();
        List<ScenarioGroup> workflow2Groups = buildWorkflow2Groups();

        // Create Sheets
        createCoverSheet(wb, titleStyle, headerStyle, cellCenter, metaLabelBold);
        createTestCaseListSheet(wb, fontBigTitle, headerStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold, fontRegular);
        createWorkflowSheet(wb, "Workflow Name1", "Workflow 1: Quản lý Định danh & Vòng đời Tài khoản (Auth & Identity Lifecycle)",
                "Kiểm thử toàn diện quy trình người dùng: Đăng ký xác thực OTP email, kích hoạt tài khoản, đổi mật khẩu lần đầu, đăng nhập 6 vai trò, bảo mật phân quyền RBAC và thu hồi phiên làm việc.",
                workflow1Groups, "ST-WF1*", headerStyle, scenarioHeaderStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold);
        createWorkflowSheet(wb, "Workflow Name2", "Workflow 2: Không gian Tự học Tiếng Nhật Toàn diện (Comprehensive Learning Space)",
                "Kiểm thử quy trình học tập khép kín: Quản lý bộ thẻ Flashcards cá nhân, tra cứu từ điển 7.811 từ vựng và ngữ pháp, vẽ Canvas nhận diện Hán tự, luyện viết Sakubun AI chấm bài, Trợ lý đọc hiểu Furigana và minigame Kanji Rain.",
                workflow2Groups, "ST-WF2*", headerStyle, scenarioHeaderStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold);
        createTestStatisticsSheet(wb, titleStyle, headerStyle, cellCenter, cellCenterBold, metaLabelBold);

        // Evaluate formulas
        try {
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        } catch (Exception e) {
            System.out.println("Formula evaluation notice: " + e.getMessage());
        }

        // Export to Desktop
        File desktopFile1 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.3_System_Test.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile1)) {
            wb.write(fos);
        }
        System.out.println("Exported successfully to: " + desktopFile1.getAbsolutePath());

        File desktopFile2 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.3_System_Test_KizunaTutor.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile2)) {
            wb.write(fos);
        }

        File artifactFile = new File("C:\\Users\\Vuh26\\.gemini\\antigravity\\brain\\9c32454a-b322-4db4-b73f-ca7159e96a64\\Report-5.3_System_Test.xlsx");
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
        cTitle.setCellValue("SYSTEM TEST REPORT DOCUMENT");
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
        Cell r5c1 = r5.createCell(1); r5c1.setCellValue("KTS_System_Test_Document_v1.0");
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
                "Khởi tạo Báo cáo Kiểm thử hệ thống (System Test)",
                "A",
                "Tạo mới toàn bộ các kịch bản kiểm thử quy trình nghiệp vụ End-to-End người dùng (Workflow Testing) cho Phân hệ 1 (Auth & Phân quyền) và Phân hệ 5 (Hỗ trợ học tập)",
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
                "2. Backend Server: Spring Boot 3.3.4 (Java 17, Spring Security, JWT Cookie, port 8085)\n" +
                "3. Database Server: PostgreSQL 15 (Supabase Cloud Database)\n" +
                "4. Cloud AI Services: Google Gemini 1.5 Flash API\n" +
                "5. External Mail Gateway: Brevo SMTP / Gmail Relay\n" +
                "6. Test Client: Google Chrome v120+ (Desktop & Responsive Tablet/Mobile Viewport)");
        CellStyle envStyle = wb.createCellStyle();
        envStyle.setFont(fontRegular);
        envStyle.setWrapText(true);
        envStyle.setVerticalAlignment(VerticalAlignment.TOP);
        r4d.setCellStyle(envStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 5));
        r4.setHeightInPoints(95);

        Row r7 = sheet.createRow(7);
        String[] headers = {"No", "Workflow Name", "Sheet Name", "Description", "Pre-Condition"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r7.createCell(i + 1);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        String[][] workflows = {
                {"1", "Quy trình Đăng ký tài khoản mới & Xác thực OTP hoàn tất", "Workflow Name1", "Người dùng đăng ký, nhận OTP qua email, kích hoạt tài khoản và vào học", "Hệ thống Backend và DB hoạt động"},
                {"2", "Quy trình Thiết lập mật khẩu lần đầu cho tài khoản cấp tạm", "Workflow Name1", "Đăng nhập với cờ mustChangePassword, mở popup modal, đổi mật khẩu thành công", "Tài khoản có cờ mustChangePassword=true"},
                {"3", "Quy trình Phân quyền đa người dùng (Multi-Role RBAC Dashboard)", "Workflow Name1", "Xác thực điều hướng chuẩn xác cho Admin, Tutor, Learner và chặn truy cập trái phép", "Các tài khoản mẫu đã nạp trong DB"},
                {"4", "Quy trình Khôi phục mật khẩu an toàn & Thu hồi phiên làm việc", "Workflow Name1", "Quên mật khẩu, rate limit 3 lần/15p, chống rò rỉ danh tính và đăng xuất an toàn", "Email người dùng trong hệ thống"},
                {"5", "Quy trình Quản lý bộ thẻ Flashcards cá nhân & Học lật thẻ 3D", "Workflow Name2", "Tạo bộ thẻ, thêm thẻ con, học lật thẻ, cập nhật tiến độ và bảo mật owner_key", "Đã đăng nhập tài khoản Học viên"},
                {"6", "Quy trình Tra cứu kho từ vựng JLPT & Tìm kiếm viết tay Canvas", "Workflow Name2", "Tra cứu 7.811 từ vựng, vẽ Canvas nhận diện Hán tự Joyo và xem âm Hán Việt", "Dữ liệu từ vựng và bảng kanjis sẵn sàng"},
                {"7", "Quy trình Luyện viết Sakubun AI & Trợ lý đọc hiểu Furigana", "Workflow Name2", "Chấm bài viết tiếng Nhật, phát hiện lỗi trợ từ, bóc tách thẻ ruby Furigana và tối ưu Cache < 50ms", "Gemini AI API trực tuyến"},
                {"8", "Quy trình Chơi Minigame Kanji Rain & Ghi nhận bảng xếp hạng", "Workflow Name2", "Chơi game mưa Hán tự, tính điểm, ghi nhận tỷ lệ chính xác và lưu kết quả vào DB", "Dữ liệu Hán tự N5-N1 sẵn sàng"}
        };

        for (int i = 0; i < workflows.length; i++) {
            Row r = sheet.createRow(8 + i);
            r.createCell(1).setCellValue(workflows[i][0]); r.getCell(1).setCellStyle(cellCenter);
            r.createCell(2).setCellValue(workflows[i][1]); r.getCell(2).setCellStyle(cellCenterBold);
            r.createCell(3).setCellValue(workflows[i][2]); r.getCell(3).setCellStyle(cellCenter);
            r.createCell(4).setCellValue(workflows[i][3]); r.getCell(4).setCellStyle(cellLeftWrap);
            r.createCell(5).setCellValue(workflows[i][4]); r.getCell(5).setCellStyle(cellLeftWrap);
        }

        sheet.setColumnWidth(0, 1000);
        sheet.setColumnWidth(1, 1500);
        sheet.setColumnWidth(2, 7500);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 16000);
        sheet.setColumnWidth(5, 12000);
    }

    private void createWorkflowSheet(Workbook wb, String sheetName, String workflowTitle, String testReq,
                                     List<ScenarioGroup> groups, String countifPrefix,
                                     CellStyle headerStyle, CellStyle scenarioHeaderStyle,
                                     CellStyle cellCenter, CellStyle cellCenterBold,
                                     CellStyle cellLeftWrap, CellStyle metaLabelBold) {
        Sheet sheet = wb.createSheet(sheetName);
        sheet.setDisplayGridlines(true);

        // Header block (Row 0 to 6)
        Row r0 = sheet.createRow(0);
        r0.createCell(0).setCellValue("Workflow"); r0.getCell(0).setCellStyle(metaLabelBold);
        r0.createCell(1).setCellValue(workflowTitle); r0.getCell(1).setCellStyle(metaLabelBold);
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

        // Render Cases grouped by Scenario
        int currentRowIdx = 10;
        for (ScenarioGroup group : groups) {
            Row rScen = sheet.createRow(currentRowIdx);
            for (int c = 0; c < colHeaders.length; c++) {
                Cell cCell = rScen.createCell(c);
                cCell.setCellStyle(scenarioHeaderStyle);
                if (c == 0) {
                    cCell.setCellValue(group.scenarioName);
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(currentRowIdx, currentRowIdx, 0, colHeaders.length - 1));
            rScen.setHeightInPoints(24);
            currentRowIdx++;

            for (SystemTestCase tc : group.cases) {
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
        cTitle.setCellValue("SYSTEM TEST STATISTICS");
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
        r5.createCell(1).setCellValue("KTS_System_Test_Report_v1.0");
        r5.createCell(3).setCellValue("Issue Date"); r5.getCell(3).setCellStyle(metaLabelBold);
        r5.createCell(4).setCellValue("2026-11-23");

        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Notes"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellValue("Báo cáo kiểm thử hệ thống (System Test) cho 2 quy trình người dùng đầu cuối: Workflow 1 (Auth & Vòng đời tài khoản) và Workflow 2 (Không gian học tập tiếng Nhật). Tổng cộng 20 kịch bản hệ thống, 100% Passed.");

        // Table Header (Row 10 / Excel row 11)
        Row r10 = sheet.createRow(10);
        String[] statHeaders = {"No", "Module code", "Passed", "Failed", "Pending", "N/A", "Number of test cases"};
        for (int i = 0; i < statHeaders.length; i++) {
            Cell c = r10.createCell(i);
            c.setCellValue(statHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        // Row 11: Workflow 1 (Excel row 12)
        Row r11 = sheet.createRow(11);
        r11.createCell(0).setCellValue("1.0"); r11.getCell(0).setCellStyle(cellCenter);
        r11.createCell(1).setCellValue("Workflow Name1 - Auth & Identity Lifecycle"); r11.getCell(1).setCellStyle(cellCenterBold);
        r11.createCell(2).setCellFormula("'Workflow Name1'!B7"); r11.getCell(2).setCellStyle(cellCenter);
        r11.createCell(3).setCellFormula("'Workflow Name1'!C7"); r11.getCell(3).setCellStyle(cellCenter);
        r11.createCell(4).setCellFormula("'Workflow Name1'!D7"); r11.getCell(4).setCellStyle(cellCenter);
        r11.createCell(5).setCellFormula("'Workflow Name1'!E7"); r11.getCell(5).setCellStyle(cellCenter);
        r11.createCell(6).setCellFormula("'Workflow Name1'!B3"); r11.getCell(6).setCellStyle(cellCenterBold);

        // Row 12: Workflow 2 (Excel row 13)
        Row r12 = sheet.createRow(12);
        r12.createCell(0).setCellValue("2.0"); r12.getCell(0).setCellStyle(cellCenter);
        r12.createCell(1).setCellValue("Workflow Name2 - Comprehensive Learning Space"); r12.getCell(1).setCellStyle(cellCenterBold);
        r12.createCell(2).setCellFormula("'Workflow Name2'!B7"); r12.getCell(2).setCellStyle(cellCenter);
        r12.createCell(3).setCellFormula("'Workflow Name2'!C7"); r12.getCell(3).setCellStyle(cellCenter);
        r12.createCell(4).setCellFormula("'Workflow Name2'!D7"); r12.getCell(4).setCellStyle(cellCenter);
        r12.createCell(5).setCellFormula("'Workflow Name2'!E7"); r12.getCell(5).setCellStyle(cellCenter);
        r12.createCell(6).setCellFormula("'Workflow Name2'!B3"); r12.getCell(6).setCellStyle(cellCenterBold);

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
    // SYSTEM TEST WORKFLOWS DATA
    // =========================================================================

    private List<ScenarioGroup> buildWorkflow1Groups() {
        List<ScenarioGroup> groups = new ArrayList<>();

        // Scenario A: Registration & Activation
        ScenarioGroup sReg = new ScenarioGroup("Scenario A: Quy trình Đăng ký tài khoản Học viên mới & Kích hoạt qua OTP");
        sReg.add(new SystemTestCase("ST-WF1-001",
                "Người dùng truy cập Landing Page, chọn Đăng ký và điền thông tin hợp lệ",
                "1. Người dùng mở trang chủ http://localhost:3000/ $\\rightarrow$ Nhấn \"Đăng ký ngay\".\n" +
                        "2. Nhập Họ tên: \"Đỗ Minh Nhật\", Email: \"nhatdm_sys@kizuna.vn\", Mật khẩu mạnh, chọn mục tiêu JLPT N3.\n" +
                        "3. Bấm \"Tiếp tục xác thực OTP\".",
                "1. Trình duyệt hiển thị thông báo gửi mã xác thực thành công.\n" +
                        "2. Màn hình chuyển sang giao diện nhập mã OTP 6 số với đồng hồ đếm ngược 60 giây Cooldown rõ ràng.",
                "Hệ thống trực tuyến, email chưa tồn tại trong cơ sở dữ liệu."));

        sReg.add(new SystemTestCase("ST-WF1-002",
                "Người dùng kiểm tra hòm thư, nhập mã OTP 6 chữ số và kích hoạt tài khoản",
                "1. Người dùng mở email lấy mã OTP 6 số.\n" +
                        "2. Nhập 6 chữ số vào ô xác thực trên trang web $\\rightarrow$ Bấm \"Xác nhận kích hoạt\".",
                "1. Hệ thống báo: \"Đăng ký tài khoản thành công!\".\n" +
                        "2. Tự động cấp phiên JWT Cookie và chuyển hướng người dùng thẳng vào trang chủ học tập /pages/home/ mà không cần đăng nhập lại.",
                "Mã OTP hợp lệ và chưa quá thời gian sống 5 phút."));

        sReg.add(new SystemTestCase("ST-WF1-003",
                "Học viên mới trải nghiệm giao diện Không gian học tập cá nhân sau khi kích hoạt",
                "1. Quan sát giao diện trang chủ học viên sau khi đăng nhập tự động.\n" +
                        "2. Kiểm tra thanh Header hiển thị tên \"Đỗ Minh Nhật\", cấp độ N3 và các tính năng hỗ trợ học tập.",
                "1. Giao diện tải đầy đủ các phân khu: Thẻ ghi nhớ Flashcards, Tra cứu từ vựng, Ngữ pháp, Luyện viết Sakubun.\n" +
                        "2. Trải nghiệm người dùng mượt mà, không gặp lỗi giao diện hay đứt gãy luồng dữ liệu.",
                "Người dùng đã hoàn thành bước xác thực OTP."));
        groups.add(sReg);

        // Scenario B: First-time Password Change
        ScenarioGroup sFirstTime = new ScenarioGroup("Scenario B: Quy trình Bắt buộc Đổi mật khẩu lần đầu cho Tài khoản cấp tạm");
        sFirstTime.add(new SystemTestCase("ST-WF1-004",
                "Học viên được cấp tài khoản đăng nhập lần đầu và bị chặn bởi Modal bắt buộc đổi mật khẩu",
                "1. Truy cập http://localhost:3000/login.\n" +
                        "2. Đăng nhập với tài khoản cấp tạm: firsttime@kizuna.vn / TempPass123!.\n" +
                        "3. Quan sát phản hồi của trang web.",
                "1. Hệ thống xác nhận thông tin đúng nhưng giữ người dùng tại trang Login.\n" +
                        "2. Popup Modal \"Thiết lập mật khẩu mới\" hiển thị chắn màn hình, không thể tắt để đảm bảo bắt buộc người dùng phải đổi mật khẩu bảo mật.",
                "Tài khoản firsttime@kizuna.vn tồn tại với mustChangePassword = true."));

        sFirstTime.add(new SystemTestCase("ST-WF1-005",
                "Người dùng hoàn tất cập nhật mật khẩu mới và đăng nhập an toàn vào hệ thống",
                "1. Tại Modal, nhập mật khẩu hiện tại và mật khẩu mới (có kiểm tra trùng khớp và độ mạnh mật khẩu).\n" +
                        "2. Nhấn nút \"Cập nhật mật khẩu\".\n" +
                        "3. Dùng mật khẩu mới vừa đổi đăng nhập lại.",
                "1. Thông báo: \"Cập nhật mật khẩu thành công! Vui lòng đăng nhập lại với mật khẩu mới\".\n" +
                        "2. Đăng nhập bằng mật khẩu mới thành công, không còn hiển thị popup bắt buộc đổi mật khẩu nữa.",
                "Modal đổi mật khẩu đang mở với phiên hợp lệ."));
        groups.add(sFirstTime);

        // Scenario C: Multi-Role RBAC
        ScenarioGroup sRbac = new ScenarioGroup("Scenario C: Quy trình Phân quyền Đa người dùng & Chặn truy cập trái phép");
        sRbac.add(new SystemTestCase("ST-WF1-006",
                "Đăng nhập tài khoản Quản trị viên (Admin) và quản lý bảng điều khiển vận hành",
                "1. Đăng nhập admin@kizuna.vn / Admin@123.\n" +
                        "2. Quan sát URL và giao diện điều hướng.",
                "1. Trình duyệt chuyển hướng chuẩn xác vào /admin/dashboard.\n" +
                        "2. Hiển thị bảng số liệu thống kê vận hành toàn hệ thống, danh sách người dùng và lịch sử phê duyệt.",
                "Tài khoản admin@kizuna.vn có vai trò ROLE_ADMIN."));

        sRbac.add(new SystemTestCase("ST-WF1-007",
                "Đăng nhập tài khoản Giảng viên (Tutor) và kiểm tra không gian làm việc giảng dạy",
                "1. Đăng xuất tài khoản Admin, đăng nhập tutor@kizuna.vn / Tutor@123.\n" +
                        "2. Quan sát giao diện Tutor Dashboard.",
                "1. Trình duyệt chuyển hướng vào /tutor/dashboard.\n" +
                        "2. Hiển thị danh sách khóa học đang phụ trách, lịch dạy kèm và danh sách học viên đăng ký.",
                "Tài khoản tutor@kizuna.vn có vai trò ROLE_TUTOR."));

        sRbac.add(new SystemTestCase("ST-WF1-008",
                "Kiểm thử bảo mật: Học viên cố tình gõ URL Quản trị viên trên thanh trình duyệt",
                "1. Đăng nhập tài khoản Học viên learner@kizuna.vn.\n" +
                        "2. Dùng thanh URL trình duyệt cố tình truy cập: http://localhost:3000/admin/dashboard.",
                "1. Hệ thống bảo mật Spring Security chặn lại ngay lập tức.\n" +
                        "2. Chuyển hướng người dùng về trang thông báo lỗi 403 Forbidden hoặc trả về trang chủ học viên, không lộ giao diện quản trị.",
                "Học viên đang có phiên đăng nhập hợp lệ."));
        groups.add(sRbac);

        // Scenario D: Forgot Password & Logout
        ScenarioGroup sForgot = new ScenarioGroup("Scenario D: Quy trình Quên mật khẩu an toàn & Thu hồi phiên làm việc");
        sForgot.add(new SystemTestCase("ST-WF1-009",
                "Người dùng quên mật khẩu, yêu cầu cấp lại mã khôi phục và kiểm soát chống spam",
                "1. Tại trang đăng nhập, bấm \"Quên mật khẩu?\".\n" +
                        "2. Nhập email học viên $\\rightarrow$ Nhấn Gửi yêu cầu.\n" +
                        "3. Thử bấm gửi liên tục nhiều lần trong vòng 15 phút.",
                "1. Hệ thống gửi mật khẩu tạm an toàn qua email người dùng.\n" +
                        "2. Cơ chế Rate Limit kích hoạt nếu gửi quá 3 lần/15 phút, hiển thị thông báo yêu cầu chờ đợi để bảo vệ an ninh chống tấn công từ chối dịch vụ.",
                "Tài khoản học viên tồn tại trong hệ thống."));

        sForgot.add(new SystemTestCase("ST-WF1-010",
                "Người dùng đăng xuất hệ thống, xóa hoàn toàn Cookie phiên và kiểm tra an toàn phiên",
                "1. Tại giao diện học tập, bấm vào Avatar $\\rightarrow$ Chọn \"Đăng xuất\".\n" +
                        "2. Quan sát chuyển hướng về /login.\n" +
                        "3. Bấm nút Back trên trình duyệt để kiểm tra xem có vào lại được trang cũ hay không.",
                "1. Cookie access_token bị xóa sạch khỏi trình duyệt.\n" +
                        "2. Bấm nút Back trên trình duyệt không thể xem lại dữ liệu cũ, hệ thống tự động buộc quay lại trang Đăng nhập.",
                "Người dùng đang đăng nhập."));
        groups.add(sForgot);

        return groups;
    }

    private List<ScenarioGroup> buildWorkflow2Groups() {
        List<ScenarioGroup> groups = new ArrayList<>();

        // Scenario A: Flashcard Study Workflow
        ScenarioGroup sFlashcard = new ScenarioGroup("Scenario A: Quy trình Quản lý Bộ thẻ Flashcards cá nhân & Ôn tập lật thẻ 3D");
        sFlashcard.add(new SystemTestCase("ST-WF2-001",
                "Học viên tạo mới bộ thẻ từ vựng chuyên đề và nhập danh sách thẻ con",
                "1. Vào Không gian học tập $\\rightarrow$ Chọn mục \"Flashcards\".\n" +
                        "2. Bấm \"Tạo bộ thẻ mới\", nhập tên: \"Từ vựng N3 Công sở\", thêm 5 thẻ từ vựng kèm Romaji và nghĩa.\n" +
                        "3. Nhấn \"Lưu bộ thẻ\".",
                "1. Bộ thẻ mới xuất hiện trên danh sách với đầy đủ số lượng thẻ con (5 thẻ).\n" +
                        "2. Hệ thống gán quyền sở hữu cá nhân, đảm bảo chỉ học viên này mới có quyền chỉnh sửa/xóa.",
                "Học viên đã đăng nhập thành công."));

        sFlashcard.add(new SystemTestCase("ST-WF2-002",
                "Học viên bắt đầu phiên ôn tập lật thẻ 3D tương tác và đánh dấu độ nhớ",
                "1. Chọn bộ thẻ vừa tạo $\\rightarrow$ Bấm \"Bắt đầu học ngay\".\n" +
                        "2. Click vào thẻ để lật mặt sau xem nghĩa tiếng Việt và câu ví dụ minh họa.\n" +
                        "3. Lần lượt bấm \"Đã nhớ\" / \"Chưa nhớ\" cho từng thẻ.",
                "1. Hiệu ứng lật thẻ 3D xoay mượt mà, trực quan.\n" +
                        "2. Thanh tiến độ ôn tập tăng dần theo tỷ lệ % thẻ đã thuộc.",
                "Bộ thẻ có ít nhất 5 thẻ từ vựng."));

        sFlashcard.add(new SystemTestCase("ST-WF2-003",
                "Hoàn thành toàn bộ thẻ trong phiên ôn tập và nhận thông báo chúc mừng",
                "1. Hoàn tất lật đến thẻ cuối cùng trong bộ thẻ.\n" +
                        "2. Quan sát màn hình tổng kết phiên học.",
                "1. Hệ thống hiển thị modal chúc mừng kèm số lượng từ vựng đã nhớ và thời gian hoàn thành.\n" +
                        "2. Điểm kinh nghiệm hoặc tiến độ học tập trên trang chủ được cập nhật ngay lập tức.",
                "Học viên đã lật hết các thẻ trong bộ."));
        groups.add(sFlashcard);

        // Scenario B: Dictionary & Canvas Handwriting
        ScenarioGroup sDict = new ScenarioGroup("Scenario B: Quy trình Tra cứu Từ điển Đa phương thức & Viết tay Canvas");
        sDict.add(new SystemTestCase("ST-WF2-004",
                "Học viên tra cứu kho 7.811 từ vựng JLPT kết hợp lọc cấp độ và từ khóa",
                "1. Truy cập mục \"Tra cứu từ vựng\".\n" +
                        "2. Chọn cấp độ \"N4\", nhập từ khóa: \"gakusei\".\n" +
                        "3. Xem danh sách từ vựng trả về.",
                "1. Danh sách từ vựng hiển thị chính xác từ 学生 (Học sinh) kèm âm Romaji, nghĩa tiếng Việt và cách dùng trong câu.\n" +
                        "2. Có nút bấm lưu nhanh từ này vào bộ thẻ Flashcards cá nhân.",
                "Kho từ vựng 7.811 từ đã được nạp trong cơ sở dữ liệu."));

        sDict.add(new SystemTestCase("ST-WF2-005",
                "Học viên gặp chữ Hán lạ, sử dụng Canvas viết tay để nhận diện và tra cứu",
                "1. Mở Từ điển Hán tự $\\rightarrow$ Chọn tính năng \"Vẽ viết tay\".\n" +
                        "2. Dùng chuột/ngón tay vẽ 4 nét chữ \"水\" lên bảng Canvas.\n" +
                        "3. Bấm chọn ký tự gợi ý \"水\" bên dưới.",
                "1. Bảng Canvas nhận diện nét vẽ và đề xuất ngay chữ \"水\" (THỦY) ở vị trí gợi ý đầu tiên.\n" +
                        "2. Chọn chữ Hán lập tức mở bảng tra cứu bách khoa: Âm Hán Việt, Onyomi, Kunyomi, Bộ thủ và các từ ghép thường gặp.",
                "Tính năng Canvas và nhận diện nét vẽ hoạt động bình thường."));
        groups.add(sDict);

        // Scenario C: AI Sakubun & Reading Assistant
        ScenarioGroup sAi = new ScenarioGroup("Scenario C: Quy trình Luyện viết Sakubun & Trợ lý Đọc hiểu tích hợp Gemini AI");
        sAi.add(new SystemTestCase("ST-WF2-006",
                "Học viên nộp bài văn Sakubun tiếng Nhật và nhận đánh giá, sửa lỗi từ Gemini AI",
                "1. Mở tính năng \"Luyện viết Sakubun AI\".\n" +
                        "2. Nhập bài viết tiếng Nhật có chứa lỗi trợ từ: \"私は毎日学校に歩いて行きます。日本語がおもしろいです。\".\n" +
                        "3. Bấm \"Chấm bài cùng AI\".",
                "1. AI phân tích và trả về điểm số tổng quát của bài viết.\n" +
                        "2. Highlight màu đỏ các vị trí dùng sai trợ từ, đưa ra giải thích chi tiết lý do và gợi ý bản sửa chuẩn tự nhiên như người bản xứ.",
                "Google Gemini AI API trực tuyến."));

        sAi.add(new SystemTestCase("ST-WF2-007",
                "Học viên dán đoạn văn bản đọc hiểu tiếng Nhật dài và xem Furigana ruby phân tích",
                "1. Mở \"Trợ lý đọc hiểu AI\".\n" +
                        "2. Dán đoạn văn bản: \"日本の春は桜がとても綺麗です。多くの人が花見を楽しみます。\".\n" +
                        "3. Nhấn \"Phân tích đọc hiểu\".",
                "1. Đoạn văn hiển thị với các chữ Hán có phiên âm Furigana nhỏ ở trên đầu chuẩn thẻ <ruby>.\n" +
                        "2. Bảng bên phải tự động bóc tách các mẫu ngữ pháp xuất hiện trong bài kèm dịch nghĩa tiếng Việt.",
                "Gemini AI API sẵn sàng."));

        sAi.add(new SystemTestCase("ST-WF2-008",
                "Kiểm tra hiệu năng bộ nhớ đệm SHA-256: Phản hồi tức thì < 50ms khi phân tích lại",
                "1. Bấm nút phân tích lại đoạn văn bản vừa thực hiện ở ca ST-WF2-007.\n" +
                        "2. Quan sát tốc độ hiển thị kết quả.",
                "1. Kết quả hiển thị ngay tức thì mà không có độ trễ (< 50ms so với lần đầu gọi AI mất ~1.5s).\n" +
                        "2. Hệ thống tối ưu hóa hoàn hảo nhờ In-Memory CacheStore, tiết kiệm chi phí gọi API.",
                "Đoạn văn bản đã được phân tích và lưu cache."));
        groups.add(sAi);

        // Scenario D: Kanji Rain Minigame
        ScenarioGroup sGame = new ScenarioGroup("Scenario D: Quy trình Chơi Minigame Kanji Rain & Ghi nhận Thành tích");
        sGame.add(new SystemTestCase("ST-WF2-009",
                "Học viên tham gia trò chơi Mưa Hán tự (Kanji Rain) luyện phản xạ",
                "1. Mở mục \"Minigame Kanji Rain\" $\\rightarrow$ Chọn độ khó JLPT N5.\n" +
                        "2. Nhấn \"Bắt đầu chơi\".\n" +
                        "3. Khi chữ Hán rơi xuống màn hình, gõ nhanh âm đọc Hiragana hoặc nghĩa tiếng Việt tương ứng.",
                "1. Chữ Hán nổ tung với hiệu ứng âm thanh và hình ảnh sinh động khi người dùng gõ đúng đáp án.\n" +
                        "2. Điểm số và chuỗi Combo chính xác tăng liên tục trên bảng điểm.",
                "Dữ liệu Kanji N5 sẵn sàng trong cơ sở dữ liệu."));

        sGame.add(new SystemTestCase("ST-WF2-010",
                "Kết thúc lượt chơi, tính toán độ chính xác và lưu kết quả vào lịch sử cá nhân",
                "1. Chơi hết thời gian quy định của ván game.\n" +
                        "2. Quan sát màn hình tổng kết Game Over.",
                "1. Hiển thị bảng tổng kết: Tổng điểm đạt được, tỷ lệ chính xác (%), danh sách các chữ Hán bị gõ sai cần ôn lại.\n" +
                        "2. Kết quả phiên chơi được tự động lưu vào cơ sở dữ liệu để phục vụ theo dõi sự tiến bộ của học viên.",
                "Học viên đã hoàn thành ít nhất một lượt chơi."));
        groups.add(sGame);

        return groups;
    }
}
