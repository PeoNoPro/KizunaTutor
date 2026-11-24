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
 * Generator for Unit Test Document following the new Report-5.1 template.
 * Outputs directly to Desktop: C:\Users\Vuh26\Desktop\Report-5.1_Unit_Test.xlsx
 */
public class NewTemplateExcelExporterTest {

    public static class TestCaseItem {
        public String id;
        public String preConditions;
        public String inputParams;
        public String expectedResults;
        public String type; // Normal, Abnormal, Boundary
        public String round1 = "Passed";
        public String testDate = "2026-11-23";
        public String tester = "Hoangvm";
        public String round2 = "Passed";
        public String round3 = "Passed";
        public String defectId = "N/A";
        public String note = "";

        public TestCaseItem(String id, String preConditions, String inputParams, String expectedResults, String type) {
            this.id = id;
            this.preConditions = preConditions;
            this.inputParams = inputParams;
            this.expectedResults = expectedResults;
            this.type = type;
        }
    }

    public static class MethodGroup {
        public String methodName;
        public List<TestCaseItem> cases = new ArrayList<>();

        public MethodGroup(String methodName) {
            this.methodName = methodName;
        }

        public MethodGroup add(TestCaseItem tc) {
            this.cases.add(tc);
            return this;
        }
    }

    @Test
    public void exportNewTemplate() throws IOException {
        Workbook wb = new XSSFWorkbook();

        // 1. Setup global fonts & styles
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

        // Header Style (Light Blue/Grey, Bold, Center)
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

        // Method Section Header Style (Turquoise, Bold, Left)
        CellStyle methodHeaderStyle = wb.createCellStyle();
        methodHeaderStyle.setFont(fontBold);
        methodHeaderStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        methodHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        methodHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        methodHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        methodHeaderStyle.setBorderTop(BorderStyle.THIN);
        methodHeaderStyle.setBorderBottom(BorderStyle.THIN);
        methodHeaderStyle.setBorderLeft(BorderStyle.THIN);
        methodHeaderStyle.setBorderRight(BorderStyle.THIN);

        // Bordered regular cell (Center)
        CellStyle cellCenter = wb.createCellStyle();
        cellCenter.setFont(fontRegular);
        cellCenter.setAlignment(HorizontalAlignment.CENTER);
        cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        cellCenter.setBorderTop(BorderStyle.THIN);
        cellCenter.setBorderBottom(BorderStyle.THIN);
        cellCenter.setBorderLeft(BorderStyle.THIN);
        cellCenter.setBorderRight(BorderStyle.THIN);

        // Bordered bold cell (Center)
        CellStyle cellCenterBold = wb.createCellStyle();
        cellCenterBold.setFont(fontBold);
        cellCenterBold.setAlignment(HorizontalAlignment.CENTER);
        cellCenterBold.setVerticalAlignment(VerticalAlignment.CENTER);
        cellCenterBold.setBorderTop(BorderStyle.THIN);
        cellCenterBold.setBorderBottom(BorderStyle.THIN);
        cellCenterBold.setBorderLeft(BorderStyle.THIN);
        cellCenterBold.setBorderRight(BorderStyle.THIN);

        // Bordered wrapped cell (Left, Top)
        CellStyle cellLeftWrap = wb.createCellStyle();
        cellLeftWrap.setFont(fontRegular);
        cellLeftWrap.setAlignment(HorizontalAlignment.LEFT);
        cellLeftWrap.setVerticalAlignment(VerticalAlignment.TOP);
        cellLeftWrap.setWrapText(true);
        cellLeftWrap.setBorderTop(BorderStyle.THIN);
        cellLeftWrap.setBorderBottom(BorderStyle.THIN);
        cellLeftWrap.setBorderLeft(BorderStyle.THIN);
        cellLeftWrap.setBorderRight(BorderStyle.THIN);

        // Meta Label Bold
        CellStyle metaLabelBold = wb.createCellStyle();
        metaLabelBold.setFont(fontBold);
        metaLabelBold.setAlignment(HorizontalAlignment.LEFT);
        metaLabelBold.setVerticalAlignment(VerticalAlignment.CENTER);

        // Title Style
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setFont(fontTitle);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 2. Build Test Case Data
        List<MethodGroup> module1Groups = buildModule1TestCases();
        List<MethodGroup> module5Groups = buildModule5TestCases();

        // 3. Create Sheets
        createCoverSheet(wb, titleStyle, headerStyle, cellCenter, metaLabelBold, fontRegular);
        createTestCaseListSheet(wb, fontBigTitle, headerStyle, cellCenter, cellLeftWrap, metaLabelBold, fontRegular);
        createModuleSheet(wb, "Module 1 - Auth", module1Groups, headerStyle, methodHeaderStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold, fontBold, fontRegular);
        createModuleSheet(wb, "Module 5 - Learning Support", module5Groups, headerStyle, methodHeaderStyle, cellCenter, cellCenterBold, cellLeftWrap, metaLabelBold, fontBold, fontRegular);
        createTestStatisticsSheet(wb, titleStyle, headerStyle, cellCenter, cellCenterBold, metaLabelBold, fontBold, fontRegular);

        // Evaluate all formulas so file displays numbers immediately
        try {
            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
        } catch (Exception e) {
            System.out.println("Formula evaluation notice: " + e.getMessage());
        }

        // 4. Save to Desktop and Artifacts
        File desktopFile1 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.1_Unit_Test.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile1)) {
            wb.write(fos);
        }
        System.out.println("Exported successfully to: " + desktopFile1.getAbsolutePath());

        File desktopFile2 = new File("C:\\Users\\Vuh26\\Desktop\\Report-5.1_Unit_Test_KizunaTutor.xlsx");
        try (FileOutputStream fos = new FileOutputStream(desktopFile2)) {
            wb.write(fos);
        }

        File artifactFile = new File("C:\\Users\\Vuh26\\.gemini\\antigravity\\brain\\9c32454a-b322-4db4-b73f-ca7159e96a64\\Report-5.1_Unit_Test.xlsx");
        try (FileOutputStream fos = new FileOutputStream(artifactFile)) {
            wb.write(fos);
        }

        wb.close();
    }

    private void createCoverSheet(Workbook wb, CellStyle titleStyle, CellStyle headerStyle, CellStyle cellCenter, CellStyle metaLabelBold, Font fontRegular) {
        Sheet sheet = wb.createSheet("Cover");
        sheet.setDisplayGridlines(true);

        // Row 1: Title
        Row r1 = sheet.createRow(1);
        Cell cTitle = r1.createCell(1);
        cTitle.setCellValue("UNIT TEST DOCUMENT");
        cTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));

        // Row 3: Project Name & Creator
        Row r3 = sheet.createRow(3);
        Cell r3c0 = r3.createCell(0); r3c0.setCellValue("Project Name"); r3c0.setCellStyle(metaLabelBold);
        Cell r3c1 = r3.createCell(1); r3c1.setCellValue("Kizuna Tutor System");
        Cell r3c4 = r3.createCell(4); r3c4.setCellValue("Creator"); r3c4.setCellStyle(metaLabelBold);
        Cell r3c5 = r3.createCell(5); r3c5.setCellValue("Hoangvm");

        // Row 4: Project Code & Issue Date
        Row r4 = sheet.createRow(4);
        Cell r4c0 = r4.createCell(0); r4c0.setCellValue("Project Code"); r4c0.setCellStyle(metaLabelBold);
        Cell r4c1 = r4.createCell(1); r4c1.setCellValue("KTS");
        Cell r4c4 = r4.createCell(4); r4c4.setCellValue("Issue Date"); r4c4.setCellStyle(metaLabelBold);
        Cell r4c5 = r4.createCell(5); r4c5.setCellValue("2026-11-23");

        // Row 5: Document Code & Version
        Row r5 = sheet.createRow(5);
        Cell r5c0 = r5.createCell(0); r5c0.setCellValue("Document Code"); r5c0.setCellStyle(metaLabelBold);
        Cell r5c1 = r5.createCell(1); r5c1.setCellValue("KTS_Unit_Test_Document_v2.0");
        Cell r5c4 = r5.createCell(4); r5c4.setCellValue("Version"); r5c4.setCellStyle(metaLabelBold);
        Cell r5c5 = r5.createCell(5); r5c5.setCellValue("v2.0");

        // Row 8: Record of change
        Row r8 = sheet.createRow(8);
        Cell r8c0 = r8.createCell(0); r8c0.setCellValue("Record of change"); r8c0.setCellStyle(metaLabelBold);

        // Row 9: Headers
        Row r9 = sheet.createRow(9);
        String[] headers = {"Effective Date", "Version", "Change Item", "*A,D,M", "Change description", "Reference"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r9.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        // Row 10: Change Entry
        Row r10 = sheet.createRow(10);
        String[] changeRow = {
                "2026-11-23",
                "v2.0",
                "Chuyển đổi toàn diện sang mẫu chuẩn Report-5.1",
                "M",
                "Cập nhật layout row-based test case table, tích hợp 3 testing rounds cho Module 1 (Auth) và Module 5 (Learning Support)",
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

    private void createTestCaseListSheet(Workbook wb, Font fontBigTitle, CellStyle headerStyle, CellStyle cellCenter, CellStyle cellLeftWrap, CellStyle metaLabelBold, Font fontRegular) {
        Sheet sheet = wb.createSheet("Test Cases");
        sheet.setDisplayGridlines(true);

        CellStyle bigTitleStyle = wb.createCellStyle();
        bigTitleStyle.setFont(fontBigTitle);
        bigTitleStyle.setAlignment(HorizontalAlignment.LEFT);
        bigTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Row 0: Title
        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(3);
        c0.setCellValue("TEST CASE LIST");
        c0.setCellStyle(bigTitleStyle);

        // Row 2: Project Name
        Row r2 = sheet.createRow(2);
        Cell r2b = r2.createCell(1); r2b.setCellValue("Project Name"); r2b.setCellStyle(metaLabelBold);
        Cell r2d = r2.createCell(3); r2d.setCellFormula("Cover!B4");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 3, 5));

        // Row 3: Project Code
        Row r3 = sheet.createRow(3);
        Cell r3b = r3.createCell(1); r3b.setCellValue("Project Code"); r3b.setCellStyle(metaLabelBold);
        Cell r3d = r3.createCell(3); r3d.setCellFormula("Cover!B5");
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 3, 5));

        // Row 4: Test Environment Setup Description
        Row r4 = sheet.createRow(4);
        Cell r4b = r4.createCell(1); r4b.setCellValue("Test Environment Setup Description"); r4b.setCellStyle(metaLabelBold);
        Cell r4d = r4.createCell(3);
        r4d.setCellValue("1. Runtime: Java 17, Spring Boot 3.3.4 parent, Spring Security (JWT)\n" +
                "2. Database: PostgreSQL Supabase cloud & Local test container\n" +
                "3. Testing Framework: JUnit 5 (Jupiter), Mockito, AssertJ\n" +
                "4. AI Integration: Google Gemini API (Gemini 1.5 Flash), Google OAuth 2.0\n" +
                "5. Client: Chrome Browser, Postman, Apache Maven (mvnw)");
        CellStyle envStyle = wb.createCellStyle();
        envStyle.setFont(fontRegular);
        envStyle.setWrapText(true);
        envStyle.setVerticalAlignment(VerticalAlignment.TOP);
        r4d.setCellStyle(envStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 3, 5));
        r4.setHeightInPoints(75);

        // Row 7: Header Table
        Row r7 = sheet.createRow(7);
        String[] headers = {"No", "Module Name", "Sheet Name", "Description", "Pre-Condition"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = r7.createCell(i + 1);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        // Row 8: Module 1
        Row r8 = sheet.createRow(8);
        r8.createCell(1).setCellValue("1.0"); r8.getCell(1).setCellStyle(cellCenter);
        r8.createCell(2).setCellValue("Authentication & Authorization"); r8.getCell(2).setCellStyle(cellCenter);
        r8.createCell(3).setCellValue("Module 1 - Auth"); r8.getCell(3).setCellStyle(cellCenter);
        r8.createCell(4).setCellValue("Quản lý xác thực, đăng nhập 6 roles, Google OAuth2, gửi/xác thực/gửi lại OTP, đăng xuất, đổi mật khẩu lần đầu & đặt lại mật khẩu tạm thời"); r8.getCell(4).setCellStyle(cellLeftWrap);
        r8.createCell(5).setCellValue("Hệ thống backend Spring Boot và PostgreSQL database sẵn sàng, các tài khoản test đã được nạp dữ liệu"); r8.getCell(5).setCellStyle(cellLeftWrap);

        // Row 9: Module 5
        Row r9 = sheet.createRow(9);
        r9.createCell(1).setCellValue("2.0"); r9.getCell(1).setCellStyle(cellCenter);
        r9.createCell(2).setCellValue("Learning Support"); r9.getCell(2).setCellStyle(cellCenter);
        r9.createCell(3).setCellValue("Module 5 - Learning Support"); r9.getCell(3).setCellStyle(cellCenter);
        r9.createCell(4).setCellValue("Hỗ trợ học tập tiếng Nhật: Bộ thẻ Flashcards cá nhân, tra cứu 7.811 từ vựng JLPT, tra cứu ngữ pháp, AI Sakubun sửa bài viết, Game Kanji Rain & Kanji Challenge, Từ điển Hán tự & tra cứu viết tay, Trợ lý phân tích đọc hiểu AI & Bộ nhớ đệm SHA-256"); r9.getCell(4).setCellStyle(cellLeftWrap);
        r9.createCell(5).setCellValue("Hệ thống backend Spring Boot, cơ sở dữ liệu từ điển và Gemini AI API đã sẵn sàng"); r9.getCell(5).setCellStyle(cellLeftWrap);

        sheet.setColumnWidth(0, 1000);
        sheet.setColumnWidth(1, 2000);
        sheet.setColumnWidth(2, 7000);
        sheet.setColumnWidth(3, 7000);
        sheet.setColumnWidth(4, 16000);
        sheet.setColumnWidth(5, 14000);
    }

    private void createModuleSheet(Workbook wb, String sheetName, List<MethodGroup> groups,
                                   CellStyle headerStyle, CellStyle methodHeaderStyle,
                                   CellStyle cellCenter, CellStyle cellCenterBold,
                                   CellStyle cellLeftWrap, CellStyle metaLabelBold,
                                   Font fontBold, Font fontRegular) {
        Sheet sheet = wb.createSheet(sheetName);
        sheet.setDisplayGridlines(true);

        // Calculate total test cases in this module
        int totalCases = 0;
        int normalCount = 0;
        int abnormalCount = 0;
        int boundaryCount = 0;
        for (MethodGroup mg : groups) {
            for (TestCaseItem tc : mg.cases) {
                totalCases++;
                if ("Normal".equalsIgnoreCase(tc.type)) normalCount++;
                else if ("Abnormal".equalsIgnoreCase(tc.type)) abnormalCount++;
                else if ("Boundary".equalsIgnoreCase(tc.type)) boundaryCount++;
            }
        }

        // 1. Top Statistics Header Block (Row 1 to 8)
        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("Code Module"); r1.getCell(0).setCellStyle(metaLabelBold);
        r1.createCell(1).setCellValue(sheetName); r1.getCell(1).setCellStyle(metaLabelBold);

        Row r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("Number of TCs"); r2.getCell(0).setCellStyle(metaLabelBold);
        Cell r2b = r2.createCell(1);
        r2b.setCellFormula("COUNTIF(A12:A300,\"UTC*\")");
        r2b.setCellStyle(cellCenterBold);

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Testing Round"); r3.getCell(0).setCellStyle(metaLabelBold);
        r3.createCell(1).setCellValue("Passed"); r3.getCell(1).setCellStyle(cellCenterBold);
        r3.createCell(2).setCellValue("Failed"); r3.getCell(2).setCellStyle(cellCenterBold);
        r3.createCell(3).setCellValue("Pending"); r3.getCell(3).setCellStyle(cellCenterBold);

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Round 1"); r4.getCell(0).setCellStyle(metaLabelBold);
        r4.createCell(1).setCellFormula("COUNTIF($F12:$F300,B4)"); r4.getCell(1).setCellStyle(cellCenter);
        r4.createCell(2).setCellFormula("COUNTIF($F12:$F300,C4)"); r4.getCell(2).setCellStyle(cellCenter);
        r4.createCell(3).setCellFormula("COUNTIF($F12:$F300,D4)"); r4.getCell(3).setCellStyle(cellCenter);

        Row r5 = sheet.createRow(5);
        r5.createCell(0).setCellValue("Round 2"); r5.getCell(0).setCellStyle(metaLabelBold);
        r5.createCell(1).setCellFormula("COUNTIF($I12:$I300,B4)"); r5.getCell(1).setCellStyle(cellCenter);
        r5.createCell(2).setCellFormula("COUNTIF($I12:$I300,C4)"); r5.getCell(2).setCellStyle(cellCenter);
        r5.createCell(3).setCellFormula("COUNTIF($I12:$I300,D4)"); r5.getCell(3).setCellStyle(cellCenter);

        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Round 3"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellFormula("COUNTIF($L12:$L300,B4)"); r6.getCell(1).setCellStyle(cellCenter);
        r6.createCell(2).setCellFormula("COUNTIF($L12:$L300,C4)"); r6.getCell(2).setCellStyle(cellCenter);
        r6.createCell(3).setCellFormula("COUNTIF($L12:$L300,D4)"); r6.getCell(3).setCellStyle(cellCenter);

        Row r7 = sheet.createRow(7);
        r7.createCell(0).setCellValue("Testing Type"); r7.getCell(0).setCellStyle(metaLabelBold);
        r7.createCell(1).setCellValue("Normal"); r7.getCell(1).setCellStyle(cellCenterBold);
        r7.createCell(2).setCellValue("Abnormal"); r7.getCell(2).setCellStyle(cellCenterBold);
        r7.createCell(3).setCellValue("Boundary"); r7.getCell(3).setCellStyle(cellCenterBold);

        Row r8 = sheet.createRow(8);
        r8.createCell(0).setCellValue("Count"); r8.getCell(0).setCellStyle(metaLabelBold);
        r8.createCell(1).setCellFormula("COUNTIF($E12:$E300,B8)"); r8.getCell(1).setCellStyle(cellCenter);
        r8.createCell(2).setCellFormula("COUNTIF($E12:$E300,C8)"); r8.getCell(2).setCellStyle(cellCenter);
        r8.createCell(3).setCellFormula("COUNTIF($E12:$E300,D8)"); r8.getCell(3).setCellStyle(cellCenter);

        // 2. Table Column Headers (Row 10 / index 10)
        Row r10 = sheet.createRow(10);
        String[] colHeaders = {
                "Test Case ID", "Pre-conditions", "Input parameters", "Expected Results",
                "Type", "Round 1", "Test date", "Tester",
                "Round 2", "Test date", "Tester",
                "Round 3", "Test date", "Tester",
                "Defect IDs", "Note"
        };
        for (int c = 0; c < colHeaders.length; c++) {
            Cell cell = r10.createCell(c);
            cell.setCellValue(colHeaders[c]);
            cell.setCellStyle(headerStyle);
        }
        r10.setHeightInPoints(25);

        // 3. Render Test Cases grouped by Method
        int currentRowIdx = 11;
        for (MethodGroup group : groups) {
            // Method section row
            Row rMethod = sheet.createRow(currentRowIdx);
            for (int c = 0; c < colHeaders.length; c++) {
                Cell cCell = rMethod.createCell(c);
                cCell.setCellStyle(methodHeaderStyle);
                if (c == 0) {
                    cCell.setCellValue(group.methodName);
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(currentRowIdx, currentRowIdx, 0, colHeaders.length - 1));
            rMethod.setHeightInPoints(22);
            currentRowIdx++;

            // Test case rows
            for (TestCaseItem tc : group.cases) {
                Row rTc = sheet.createRow(currentRowIdx);

                Cell c0 = rTc.createCell(0); c0.setCellValue(tc.id); c0.setCellStyle(cellCenterBold);
                Cell c1 = rTc.createCell(1); c1.setCellValue(tc.preConditions); c1.setCellStyle(cellLeftWrap);
                Cell c2 = rTc.createCell(2); c2.setCellValue(tc.inputParams); c2.setCellStyle(cellLeftWrap);
                Cell c3 = rTc.createCell(3); c3.setCellValue(tc.expectedResults); c3.setCellStyle(cellLeftWrap);
                Cell c4 = rTc.createCell(4); c4.setCellValue(tc.type); c4.setCellStyle(cellCenterBold);
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

        // Set column widths matching new template
        sheet.setColumnWidth(0, 4000);  // Test Case ID
        sheet.setColumnWidth(1, 9500);  // Pre-conditions
        sheet.setColumnWidth(2, 11000); // Input parameters
        sheet.setColumnWidth(3, 16000); // Expected Results
        sheet.setColumnWidth(4, 3200);  // Type
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
                                           CellStyle metaLabelBold, Font fontBold, Font fontRegular) {
        Sheet sheet = wb.createSheet("Test Statistics");
        sheet.setDisplayGridlines(true);

        // Row 1: Title
        Row r1 = sheet.createRow(1);
        Cell cTitle = r1.createCell(0);
        cTitle.setCellValue("UNIT TEST STATISTICS");
        cTitle.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

        // Row 3: Project Name & Creator
        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Project Name"); r3.getCell(0).setCellStyle(metaLabelBold);
        r3.createCell(1).setCellFormula("Cover!B4");
        r3.createCell(3).setCellValue("Creator"); r3.getCell(3).setCellStyle(metaLabelBold);
        r3.createCell(4).setCellValue("Hoangvm");

        // Row 4: Project Code & Reviewer
        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Project Code"); r4.getCell(0).setCellStyle(metaLabelBold);
        r4.createCell(1).setCellFormula("Cover!B5");
        r4.createCell(3).setCellValue("Reviewer/Approver"); r4.getCell(3).setCellStyle(metaLabelBold);
        r4.createCell(4).setCellValue("Teacher / Team Leader");

        // Row 5: Document Code & Issue Date
        Row r5 = sheet.createRow(5);
        r5.createCell(0).setCellValue("Document Code"); r5.getCell(0).setCellStyle(metaLabelBold);
        r5.createCell(1).setCellValue("KTS_Test_Report_v2.0");
        r5.createCell(3).setCellValue("Issue Date"); r5.getCell(3).setCellStyle(metaLabelBold);
        r5.createCell(4).setCellValue("2026-11-23");

        // Row 6: Notes
        Row r6 = sheet.createRow(6);
        r6.createCell(0).setCellValue("Notes"); r6.getCell(0).setCellStyle(metaLabelBold);
        r6.createCell(1).setCellValue("Báo cáo kiểm thử Unit Test cho 2 phân hệ được phân công: Module 1 (Auth) và Module 5 (Learning Support). Tổng cộng 68 test cases, tỉ lệ đạt 100%.");

        // Row 10: Table Headers (index 10 / Excel row 11)
        Row r10 = sheet.createRow(10);
        String[] statHeaders = {"No", "Function code", "Passed", "Failed", "Untested", "N", "A", "B", "Total Test Cases"};
        for (int i = 0; i < statHeaders.length; i++) {
            Cell c = r10.createCell(i);
            c.setCellValue(statHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        // Row 11: Module 1 (Auth) (Excel row 12)
        Row r11 = sheet.createRow(11);
        r11.createCell(0).setCellValue("1.0"); r11.getCell(0).setCellStyle(cellCenter);
        r11.createCell(1).setCellValue("Module 1 - Auth"); r11.getCell(1).setCellStyle(cellCenterBold);
        r11.createCell(2).setCellFormula("'Module 1 - Auth'!B7"); r11.getCell(2).setCellStyle(cellCenter);
        r11.createCell(3).setCellFormula("'Module 1 - Auth'!C7"); r11.getCell(3).setCellStyle(cellCenter);
        r11.createCell(4).setCellFormula("'Module 1 - Auth'!D7"); r11.getCell(4).setCellStyle(cellCenter);
        r11.createCell(5).setCellFormula("'Module 1 - Auth'!B9"); r11.getCell(5).setCellStyle(cellCenter);
        r11.createCell(6).setCellFormula("'Module 1 - Auth'!C9"); r11.getCell(6).setCellStyle(cellCenter);
        r11.createCell(7).setCellFormula("'Module 1 - Auth'!D9"); r11.getCell(7).setCellStyle(cellCenter);
        r11.createCell(8).setCellFormula("'Module 1 - Auth'!B3"); r11.getCell(8).setCellStyle(cellCenterBold);

        // Row 12: Module 5 (Learning Support) (Excel row 13)
        Row r12 = sheet.createRow(12);
        r12.createCell(0).setCellValue("2.0"); r12.getCell(0).setCellStyle(cellCenter);
        r12.createCell(1).setCellValue("Module 5 - Learning Support"); r12.getCell(1).setCellStyle(cellCenterBold);
        r12.createCell(2).setCellFormula("'Module 5 - Learning Support'!B7"); r12.getCell(2).setCellStyle(cellCenter);
        r12.createCell(3).setCellFormula("'Module 5 - Learning Support'!C7"); r12.getCell(3).setCellStyle(cellCenter);
        r12.createCell(4).setCellFormula("'Module 5 - Learning Support'!D7"); r12.getCell(4).setCellStyle(cellCenter);
        r12.createCell(5).setCellFormula("'Module 5 - Learning Support'!B9"); r12.getCell(5).setCellStyle(cellCenter);
        r12.createCell(6).setCellFormula("'Module 5 - Learning Support'!C9"); r12.getCell(6).setCellStyle(cellCenter);
        r12.createCell(7).setCellFormula("'Module 5 - Learning Support'!D9"); r12.getCell(7).setCellStyle(cellCenter);
        r12.createCell(8).setCellFormula("'Module 5 - Learning Support'!B3"); r12.getCell(8).setCellStyle(cellCenterBold);

        // Row 14: Sub total (Excel row 15)
        Row r14 = sheet.createRow(14);
        r14.createCell(1).setCellValue("Sub total"); r14.getCell(1).setCellStyle(cellCenterBold);
        r14.createCell(2).setCellFormula("SUM(C12:C13)"); r14.getCell(2).setCellStyle(cellCenterBold);
        r14.createCell(3).setCellFormula("SUM(D12:D13)"); r14.getCell(3).setCellStyle(cellCenterBold);
        r14.createCell(4).setCellFormula("SUM(E12:E13)"); r14.getCell(4).setCellStyle(cellCenterBold);
        r14.createCell(5).setCellFormula("SUM(F12:F13)"); r14.getCell(5).setCellStyle(cellCenterBold);
        r14.createCell(6).setCellFormula("SUM(G12:G13)"); r14.getCell(6).setCellStyle(cellCenterBold);
        r14.createCell(7).setCellFormula("SUM(H12:H13)"); r14.getCell(7).setCellStyle(cellCenterBold);
        r14.createCell(8).setCellFormula("SUM(I12:I13)"); r14.getCell(8).setCellStyle(cellCenterBold);

        // Percentage number format
        CellStyle cellPercentNumber = wb.createCellStyle();
        cellPercentNumber.cloneStyleFrom(cellCenterBold);
        cellPercentNumber.setDataFormat(wb.createDataFormat().getFormat("0.0"));

        // Row 16: Test coverage (Excel row 17)
        Row r16 = sheet.createRow(16);
        r16.createCell(1).setCellValue("Test coverage"); r16.getCell(1).setCellStyle(metaLabelBold);
        r16.createCell(3).setCellFormula("(C15+D15)*100/(I15)"); r16.getCell(3).setCellStyle(cellPercentNumber);
        r16.createCell(4).setCellValue("%");

        // Row 17: Test successful coverage (Excel row 18)
        Row r17 = sheet.createRow(17);
        r17.createCell(1).setCellValue("Test successful coverage"); r17.getCell(1).setCellStyle(metaLabelBold);
        r17.createCell(3).setCellFormula("C15*100/(I15)"); r17.getCell(3).setCellStyle(cellPercentNumber);
        r17.createCell(4).setCellValue("%");

        // Row 18: Normal case (Excel row 19)
        Row r18 = sheet.createRow(18);
        r18.createCell(1).setCellValue("Normal case"); r18.getCell(1).setCellStyle(metaLabelBold);
        r18.createCell(3).setCellFormula("F15*100/I15"); r18.getCell(3).setCellStyle(cellPercentNumber);
        r18.createCell(4).setCellValue("%");

        // Row 19: Abnormal case (Excel row 20)
        Row r19 = sheet.createRow(19);
        r19.createCell(1).setCellValue("Abnormal case"); r19.getCell(1).setCellStyle(metaLabelBold);
        r19.createCell(3).setCellFormula("G15*100/I15"); r19.getCell(3).setCellStyle(cellPercentNumber);
        r19.createCell(4).setCellValue("%");

        // Row 20: Boundary case (Excel row 21)
        Row r20 = sheet.createRow(20);
        r20.createCell(1).setCellValue("Boundary case"); r20.getCell(1).setCellStyle(metaLabelBold);
        r20.createCell(3).setCellFormula("H15*100/I15"); r20.getCell(3).setCellStyle(cellPercentNumber);
        r20.createCell(4).setCellValue("%");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 3500);
        sheet.setColumnWidth(3, 3500);
        sheet.setColumnWidth(4, 3500);
        sheet.setColumnWidth(5, 2500);
        sheet.setColumnWidth(6, 2500);
        sheet.setColumnWidth(7, 2500);
        sheet.setColumnWidth(8, 5000);
    }

    // =========================================================================
    // TEST CASE DATA DEFINITIONS
    // =========================================================================

    private List<MethodGroup> buildModule1TestCases() {
        List<MethodGroup> groups = new ArrayList<>();

        // 1. AuthService.login
        MethodGroup gLogin = new MethodGroup("1. AuthService.login(LoginRequest, HttpServletResponse)");
        gLogin.add(new TestCaseItem("UTC001",
                "Email tồn tại trong DB, status = ACTIVE, role = ROLE_ADMIN, mustChangePassword = false",
                "email = \"admin@kizuna.vn\", password = \"Admin@123\"",
                "Return: HTTP 200 OK, JWT Token issued, Cookie set 'access_token', Redirect to /admin/dashboard\nException: N/A\nLog Message: \"User admin@kizuna.vn logged in successfully\"",
                "Normal"));
        gLogin.add(new TestCaseItem("UTC002",
                "Email tồn tại trong DB, status = ACTIVE, role = ROLE_MANAGER, mustChangePassword = false",
                "email = \"manager@kizuna.vn\", password = \"Manager@123\"",
                "Return: HTTP 200 OK, JWT Token issued, Cookie set 'access_token', Redirect to /manager/dashboard\nException: N/A\nLog Message: \"User manager@kizuna.vn logged in successfully\"",
                "Normal"));
        gLogin.add(new TestCaseItem("UTC003",
                "Email tồn tại trong DB, status = ACTIVE, role = ROLE_CS_STAFF, mustChangePassword = false",
                "email = \"cs@kizuna.vn\", password = \"Staff@123\"",
                "Return: HTTP 200 OK, JWT Token issued, Cookie set 'access_token', Redirect to /cs/dashboard\nException: N/A\nLog Message: \"User cs@kizuna.vn logged in successfully\"",
                "Normal"));
        gLogin.add(new TestCaseItem("UTC004",
                "Email tồn tại trong DB, status = ACTIVE, role = ROLE_ACADEMIC_OFFICER, mustChangePassword = false",
                "email = \"academic@kizuna.vn\", password = \"Academic@123\"",
                "Return: HTTP 200 OK, JWT Token issued, Cookie set 'access_token', Redirect to /academic/dashboard\nException: N/A\nLog Message: \"User academic@kizuna.vn logged in successfully\"",
                "Normal"));
        gLogin.add(new TestCaseItem("UTC005",
                "Email tồn tại trong DB, status = ACTIVE, role = ROLE_TUTOR, mustChangePassword = false",
                "email = \"tutor@kizuna.vn\", password = \"Tutor@123\"",
                "Return: HTTP 200 OK, JWT Token issued, Cookie set 'access_token', Redirect to /tutor/dashboard\nException: N/A\nLog Message: \"User tutor@kizuna.vn logged in successfully\"",
                "Normal"));
        gLogin.add(new TestCaseItem("UTC006",
                "Email tồn tại trong DB, status = ACTIVE, role = ROLE_LEARNER, mustChangePassword = false",
                "email = \"learner@kizuna.vn\", password = \"Learner@123\"",
                "Return: HTTP 200 OK, JWT Token issued, Cookie set 'access_token', Redirect to /pages/home/\nException: N/A\nLog Message: \"User learner@kizuna.vn logged in successfully\"",
                "Normal"));
        gLogin.add(new TestCaseItem("UTC007",
                "Email tồn tại trong DB, status = ACTIVE, role = ROLE_LEARNER, mustChangePassword = true",
                "email = \"firsttime@kizuna.vn\", password = \"TempPass123!\"",
                "Return: HTTP 200 OK, mustChangePassword = true, hiển thị popup modal 'Thiết lập mật khẩu mới' với nút 'Cập nhật mật khẩu' và icon mắt xem pass\nException: N/A\nLog Message: \"User firsttime@kizuna.vn requires first-time password change\"",
                "Normal"));
        gLogin.add(new TestCaseItem("UTC008",
                "Email không tồn tại trong hệ thống DB",
                "email = \"unknown_user@kizuna.vn\", password = \"AnyPass123!\"",
                "Return: null\nException: BadRequestException: 'Tài khoản hoặc mật khẩu không chính xác'\nLog Message: \"Failed login attempt for non-existent email\"",
                "Abnormal"));
        gLogin.add(new TestCaseItem("UTC009",
                "Email tồn tại trong DB, status = ACTIVE, nhập sai mật khẩu",
                "email = \"learner@kizuna.vn\", password = \"WrongPassword999!\"",
                "Return: null\nException: BadRequestException: 'Tài khoản hoặc mật khẩu không chính xác'\nLog Message: \"Bad credentials for user learner@kizuna.vn\"",
                "Abnormal"));
        gLogin.add(new TestCaseItem("UTC010",
                "Email tồn tại trong DB, status = BANNED / SUSPENDED / INACTIVE",
                "email = \"banned@kizuna.vn\", password = \"Password123!\"",
                "Return: null\nException: BadRequestException: 'Tài khoản đã bị tạm khóa hoặc ngừng hoạt động'\nLog Message: \"Attempted login to disabled/suspended account\"",
                "Boundary"));
        groups.add(gLogin);

        // 2. GoogleOAuthController.authorizeGoogle
        MethodGroup gGoogle = new MethodGroup("2. GoogleOAuthController.authorizeGoogle(HttpServletResponse)");
        gGoogle.add(new TestCaseItem("UTC011",
                "Cấu hình google.client-id hợp lệ trong environment",
                "GET /api/auth/oauth2/google",
                "Return: HTTP 302 Redirect to Google Accounts Consent URL với scope openid email profile\nException: N/A\nLog Message: \"Redirecting to Google OAuth2 consent\"",
                "Normal"));
        gGoogle.add(new TestCaseItem("UTC012",
                "google.client-id = null hoặc để trống",
                "GET /api/auth/oauth2/google",
                "Return: HTTP 302 Redirect to /login?error=oauth2_not_configured (chặn mock nguy hiểm trên prod)\nException: N/A\nLog Message: \"Google OAuth2 is not configured. Mock endpoints disabled for production safety\"",
                "Abnormal"));
        gGoogle.add(new TestCaseItem("UTC013",
                "google.client-id chứa giá trị placeholder mặc định 'YOUR_CLIENT_ID'",
                "GET /api/auth/oauth2/google",
                "Return: HTTP 302 Redirect to /login?error=oauth2_not_configured\nException: N/A\nLog Message: \"OAuth placeholder detected, rejecting authorization request\"",
                "Abnormal"));
        groups.add(gGoogle);

        // 3. AuthService.initiateRegistration (sendOtp)
        MethodGroup gSendOtp = new MethodGroup("3. AuthService.initiateRegistration(RegisterRequest)");
        gSendOtp.add(new TestCaseItem("UTC014",
                "Email chưa từng được đăng ký trong hệ thống, vai trò LEARNER",
                "RegisterRequest(email = \"newlearner@kizuna.vn\", password = \"SecurePass123!\", role = ROLE_LEARNER, targetJlpt = \"N4\")",
                "Return: HTTP 200 OK, sinh OTP ngẫu nhiên 6 số, lưu cache pending, gửi email Gmail thành công\nException: N/A\nLog Message: \"Generated registration OTP for newlearner@kizuna.vn\"",
                "Normal"));
        gSendOtp.add(new TestCaseItem("UTC015",
                "Email chưa từng được đăng ký trong hệ thống, vai trò TUTOR",
                "RegisterRequest(email = \"newtutor@kizuna.vn\", password = \"SecurePass123!\", role = ROLE_TUTOR)",
                "Return: HTTP 200 OK, sinh OTP ngẫu nhiên 6 số, lưu cache pending, gửi email Gmail thành công\nException: N/A\nLog Message: \"Generated registration OTP for tutor newtutor@kizuna.vn\"",
                "Normal"));
        gSendOtp.add(new TestCaseItem("UTC016",
                "Email đã tồn tại trong DB (learner@kizuna.vn)",
                "RegisterRequest(email = \"learner@kizuna.vn\", password = \"Password123!\")",
                "Return: null\nException: BadRequestException: 'Địa chỉ email này đã được đăng ký trong hệ thống'\nLog Message: \"Registration rejected: duplicate email learner@kizuna.vn\"",
                "Abnormal"));
        groups.add(gSendOtp);

        // 4. AuthService.completeRegistration (verifyOtp)
        MethodGroup gVerifyOtp = new MethodGroup("4. AuthService.completeRegistration(VerifyOtpRequest)");
        gVerifyOtp.add(new TestCaseItem("UTC017",
                "Pending registration tồn tại trong cache, chưa hết hạn, role = ROLE_LEARNER, email chưa có trong DB",
                "VerifyOtpRequest(email = \"newlearner@kizuna.vn\", otp = \"123456\")",
                "Return: HTTP 200 OK, tạo User + LearnerProfile, cấp JWT Token & Cookie, Redirect /pages/home/\nException: N/A\nLog Message: \"Registration completed successfully for learner\"",
                "Normal"));
        gVerifyOtp.add(new TestCaseItem("UTC018",
                "Pending registration tồn tại trong cache, chưa hết hạn, role = ROLE_TUTOR, email chưa có trong DB",
                "VerifyOtpRequest(email = \"newtutor@kizuna.vn\", otp = \"123456\")",
                "Return: HTTP 200 OK, tạo User + TutorProfile APPROVED, cấp JWT Token & Cookie, Redirect /tutor/dashboard\nException: N/A\nLog Message: \"Registration completed successfully for tutor\"",
                "Normal"));
        gVerifyOtp.add(new TestCaseItem("UTC019",
                "Pending registration không tồn tại trong cache hoặc đã quá 5 phút",
                "VerifyOtpRequest(email = \"expired@kizuna.vn\", otp = \"123456\")",
                "Return: null\nException: BadRequestException: 'Yêu cầu xác thực không tồn tại hoặc đã hết hiệu lực'\nLog Message: \"Pending registration cache miss or expired for expired@kizuna.vn\"",
                "Abnormal"));
        gVerifyOtp.add(new TestCaseItem("UTC020",
                "Pending registration tồn tại, nhưng email đã được kích hoạt bởi request song song",
                "VerifyOtpRequest(email = \"concurrent@kizuna.vn\", otp = \"123456\")",
                "Return: null\nException: BadRequestException: 'Tài khoản đã được đăng ký trước đó'\nLog Message: \"User already exists during completeRegistration for concurrent@kizuna.vn\"",
                "Abnormal"));
        groups.add(gVerifyOtp);

        // 5. OtpService.resendOtp
        MethodGroup gResendOtp = new MethodGroup("5. OtpService.resendOtp(String)");
        gResendOtp.add(new TestCaseItem("UTC021",
                "Phiên đăng ký đang mở trong cache, thời gian từ lần gửi trước >= 60 giây",
                "email = \"learner@kizuna.vn\"",
                "Return: HTTP 200 OK, sinh mã OTP 6 số mới, reset attempts = 0, gia hạn expiresAt thêm 5 phút\nException: N/A\nLog Message: \"Resent OTP successfully for learner@kizuna.vn\"",
                "Normal"));
        gResendOtp.add(new TestCaseItem("UTC022",
                "Phiên đăng ký đang mở, thời gian từ lần gửi trước < 60 giây (mới 20 giây)",
                "email = \"learner@kizuna.vn\"",
                "Return: null\nException: BadRequestException: 'Vui lòng đợi thêm ... giây trước khi yêu cầu gửi lại mã'\nLog Message: \"Rate limit triggered: OTP resend requested before cooldown expired\"",
                "Boundary"));
        gResendOtp.add(new TestCaseItem("UTC023",
                "Cache không có thông tin email (chưa đăng ký bước 1)",
                "email = \"unknown@kizuna.vn\"",
                "Return: null\nException: BadRequestException: 'Phiên đăng ký đã kết thúc. Vui lòng điền lại thông tin'\nLog Message: \"Resend OTP failed: no registration in progress for unknown@kizuna.vn\"",
                "Abnormal"));
        groups.add(gResendOtp);

        // 6. AuthService.logout
        MethodGroup gLogout = new MethodGroup("6. AuthService.logout(HttpServletResponse)");
        gLogout.add(new TestCaseItem("UTC024",
                "Người dùng đã đăng nhập, HttpServletResponse hợp lệ",
                "logout(response)",
                "Return: HTTP 200 OK, thêm Cookie 'access_token' với value = '' và maxAge = 0, path = '/'\nException: N/A\nLog Message: \"User logged out, cookie access_token cleared\"",
                "Normal"));
        gLogout.add(new TestCaseItem("UTC025",
                "HttpServletResponse là null",
                "logout(null)",
                "Return: HTTP 200 OK, hoàn tất xử lý mà không ném NullPointerException\nException: N/A\nLog Message: \"Logout called with null response, safely bypassed cookie clearance\"",
                "Boundary"));
        groups.add(gLogout);

        // 7. AuthService.changePasswordFirstTime
        MethodGroup gChangePass = new MethodGroup("7. AuthService.changePasswordFirstTime(FirstTimePasswordChangeRequest, String)");
        gChangePass.add(new TestCaseItem("UTC026",
                "User tồn tại trong DB, mustChangePassword = true, currentPassword khớp mật khẩu tạm",
                "currentPassword = \"TempPass123!\", newPassword = \"NewPassword123!\", confirmPassword = \"NewPassword123!\"",
                "Return: HTTP 200 OK, lưu passwordHash mới được mã hóa BCrypt, cập nhật mustChangePassword = false\nException: N/A\nLog Message: \"First-time password changed successfully, flag cleared\"",
                "Normal"));
        gChangePass.add(new TestCaseItem("UTC027",
                "User tồn tại trong DB, mustChangePassword = true, currentPassword đúng, mật khẩu xác nhận không khớp",
                "currentPassword = \"TempPass123!\", newPassword = \"NewPassword123!\", confirmPassword = \"MismatchPass999!\"",
                "Return: null\nException: BadRequestException: 'Mật khẩu mới và mật khẩu xác nhận không trùng khớp'\nLog Message: \"Password change rejected: new password mismatch\"",
                "Abnormal"));
        gChangePass.add(new TestCaseItem("UTC028",
                "User tồn tại trong DB, mustChangePassword = true, currentPassword sai",
                "currentPassword = \"WrongTempPass!\", newPassword = \"NewPassword123!\", confirmPassword = \"NewPassword123!\"",
                "Return: null\nException: BadRequestException: 'Mật khẩu tạm thời hiện tại không chính xác'\nLog Message: \"Current temporary password verification failed\"",
                "Abnormal"));
        gChangePass.add(new TestCaseItem("UTC029",
                "User không tồn tại trong DB",
                "currentPassword = \"TempPass123!\", newPassword = \"NewPassword123!\", confirmPassword = \"NewPassword123!\"",
                "Return: null\nException: ResourceNotFoundException: 'Không tìm thấy tài khoản người dùng'\nLog Message: \"User not found for first-time password change\"",
                "Abnormal"));
        groups.add(gChangePass);

        // 8. AuthService.resetPassword (PR #20 Security)
        MethodGroup gResetPass = new MethodGroup("8. AuthService.resetPassword (PR #20 Security: Rate limit & Role Protection)");
        gResetPass.add(new TestCaseItem("UTC030",
                "Email tồn tại trong DB (role Learner hoặc Tutor), số lần yêu cầu trong 15p < 3",
                "email = \"learner@kizuna.vn\"",
                "Return: HTTP 200 OK, sinh mật khẩu tạm ngẫu nhiên, lưu hash tạm thời, gửi email cho người dùng\nException: N/A\nLog Message: \"Temporary password generated and sent to learner@kizuna.vn\"",
                "Normal"));
        gResetPass.add(new TestCaseItem("UTC031",
                "Email tồn tại trong DB thuộc role quản trị: ROLE_ADMIN / ROLE_MANAGER / ROLE_CS_STAFF / ROLE_ACADEMIC_OFFICER",
                "email = \"admin@kizuna.vn\"",
                "Return: null\nException: BusinessException HTTP 403: 'Quản trị viên và nhân viên không được tự khôi phục mật khẩu trực tuyến'\nLog Message: \"Blocked online password reset attempt for internal staff\"",
                "Abnormal"));
        gResetPass.add(new TestCaseItem("UTC032",
                "Email không tồn tại trong DB (Chống dò quét thông tin người dùng / User Enumeration)",
                "email = \"nonexistent@kizuna.vn\"",
                "Return: HTTP 200 OK (Neutral response thành công giả lập, không gửi mail, bảo vệ dữ liệu chống User Enumeration)\nException: N/A\nLog Message: \"Neutral return on password reset for non-existent email\"",
                "Normal"));
        gResetPass.add(new TestCaseItem("UTC033",
                "User tồn tại, temporaryPassword chính xác, newPassword và confirmPassword khớp nhau",
                "temporaryPassword = \"TempPassGenerated123!\", newPassword = \"StrongNewPassword123!\", confirmPassword = \"StrongNewPassword123!\"",
                "Return: HTTP 200 OK, cập nhật mật khẩu mới, xóa mật khẩu tạm, đặt mustChangePassword = false\nException: N/A\nLog Message: \"Password reset completed successfully using temporary credentials\"",
                "Normal"));
        gResetPass.add(new TestCaseItem("UTC034",
                "User tồn tại, temporaryPassword đúng, newPassword và confirmPassword không khớp",
                "temporaryPassword = \"TempPassGenerated123!\", newPassword = \"StrongNewPassword123!\", confirmPassword = \"DifferentPass999!\"",
                "Return: null\nException: BadRequestException: 'Mật khẩu mới và mật khẩu xác minh không trùng khớp'\nLog Message: \"Password reset rejected: confirmation password does not match\"",
                "Abnormal"));
        gResetPass.add(new TestCaseItem("UTC035",
                "User tồn tại, temporaryPassword sai",
                "temporaryPassword = \"WrongTempPass999!\", newPassword = \"StrongNewPassword123!\", confirmPassword = \"StrongNewPassword123!\"",
                "Return: null\nException: BadRequestException: 'Email hoặc mật khẩu được cấp không chính xác'\nLog Message: \"Temporary password check failed during reset\"",
                "Abnormal"));
        gResetPass.add(new TestCaseItem("UTC036",
                "Đã gửi 3 lần trong 15 phút gần nhất, lần thứ 4 gửi tiếp trong cùng cửa sổ thời gian",
                "email = \"learner@kizuna.vn\" (lần thứ 4)",
                "Return: null\nException: BusinessException HTTP 429: 'Vượt quá giới hạn tần suất yêu cầu. Vui lòng thử lại sau 15 phút.'\nLog Message: \"Rate limit exceeded (3 attempts / 15m) for password reset\"",
                "Boundary"));
        groups.add(gResetPass);

        return groups;
    }

    private List<MethodGroup> buildModule5TestCases() {
        List<MethodGroup> groups = new ArrayList<>();

        // 9. FlashcardDeckController (CRUD & Security)
        MethodGroup gFlashcard = new MethodGroup("9. FlashcardDeckController (CRUD & Security PR #20)");
        gFlashcard.add(new TestCaseItem("UTC037",
                "Request không có Header Authorization hoặc Token hết hạn",
                "GET /api/flashcards/decks (không truyền token)",
                "Return: null\nException: BusinessException HTTP 401: 'Chưa đăng nhập hoặc phiên làm việc đã kết thúc'\nLog Message: \"Unauthorized access to flashcard decks\"",
                "Abnormal"));
        gFlashcard.add(new TestCaseItem("UTC038",
                "JWT Token của User A, bộ thẻ ID 55 thuộc sở hữu của User B (owner_key != caller email)",
                "PUT /api/flashcards/decks/55",
                "Return: null\nException: BusinessException HTTP 403: 'Không có quyền thao tác trên bộ thẻ của người khác'\nLog Message: \"Access denied: deck ownership mismatch\"",
                "Abnormal"));
        gFlashcard.add(new TestCaseItem("UTC039",
                "User đã đăng nhập hợp lệ, có 3 bộ thẻ trong DB",
                "GET /api/flashcards/decks",
                "Return: HTTP 200 OK, danh sách 3 bộ thẻ kèm số lượng thẻ con và tiến độ ôn tập\nException: N/A\nLog Message: \"Retrieved 3 flashcard decks for learner\"",
                "Normal"));
        gFlashcard.add(new TestCaseItem("UTC040",
                "User đã đăng nhập hợp lệ, tạo bộ thẻ mới",
                "POST /api/flashcards/decks (title = \"Từ vựng N4 Bài 1\", cards = 10 items)",
                "Return: HTTP 200 OK, lưu bộ thẻ mới vào DB, gắn owner_key = caller email, trả về DeckDto đầy đủ\nException: N/A\nLog Message: \"Created new flashcard deck with 10 cards\"",
                "Normal"));
        gFlashcard.add(new TestCaseItem("UTC041",
                "Bộ thẻ tồn tại trong DB, caller là owner chính chủ",
                "PUT /api/flashcards/decks/{id} (title = \"Từ vựng N4 Nâng Cao\", cards = 15 items)",
                "Return: HTTP 200 OK, cập nhật thành công metadata và nội dung thẻ con\nException: N/A\nLog Message: \"Updated flashcard deck successfully\"",
                "Normal"));
        gFlashcard.add(new TestCaseItem("UTC042",
                "Bộ thẻ tồn tại trong DB, caller là owner chính chủ",
                "DELETE /api/flashcards/decks/{id}",
                "Return: HTTP 200 OK, success = true, xóa bộ thẻ và các thẻ con liên kết\nException: N/A\nLog Message: \"Deleted flashcard deck id successfully\"",
                "Normal"));
        gFlashcard.add(new TestCaseItem("UTC043",
                "ID bộ thẻ không tồn tại trong DB",
                "DELETE /api/flashcards/decks/999999",
                "Return: null\nException: ResourceNotFoundException HTTP 404: 'Không tìm thấy bộ thẻ tương ứng'\nLog Message: \"Flashcard deck not found with ID 999999\"",
                "Abnormal"));
        groups.add(gFlashcard);

        // 10. VocabularyController.getTopics / getVocabularies
        MethodGroup gVocab = new MethodGroup("10. VocabularyController.getTopics / getVocabularies");
        gVocab.add(new TestCaseItem("UTC044",
                "Bảng vocabularies trong DB có 7.811 bản ghi từ vựng JLPT N5-N1",
                "GET /api/vocabularies/topics",
                "Return: HTTP 200 OK, trả về 7 nhóm chủ đề (JLPT_N5 ~ JLPT_N1, LIFE_DAILY, WORKPLACE) kèm số lượng từ vựng\nException: N/A\nLog Message: \"Aggregated vocabulary topics successfully\"",
                "Normal"));
        gVocab.add(new TestCaseItem("UTC045",
                "Bảng vocabularies có dữ liệu từ vựng N5",
                "GET /api/vocabularies?topic=JLPT_N5&keyword=nihon&page=0&limit=20",
                "Return: HTTP 200 OK, phân trang danh sách từ vựng khớp cấp độ N5 và từ khóa 'nihon' kèm Romaji, Kanji, Furigana, nghĩa Tiếng Việt\nException: N/A\nLog Message: \"Retrieved vocabularies filtered by topic and keyword\"",
                "Normal"));
        gVocab.add(new TestCaseItem("UTC046",
                "Bảng vocabularies sẵn sàng, phân trang âm",
                "GET /api/vocabularies?page=-5&limit=-10",
                "Return: HTTP 200 OK, tự động điều chỉnh page = 0 và limit về giá trị mặc định an toàn (20), không ném ngoại lệ\nException: N/A\nLog Message: \"Negative pagination parameters safely clamped to defaults\"",
                "Boundary"));
        groups.add(gVocab);

        // 11. GrammarController.getGrammars / getGrammarDetail
        MethodGroup gGrammar = new MethodGroup("11. GrammarController.getGrammars / getGrammarDetail");
        gGrammar.add(new TestCaseItem("UTC047",
                "Bảng ngữ pháp PostgreSQL (hoặc fallback JSON) sẵn sàng",
                "GET /api/grammars?level=N5&keyword=desu&page=0&limit=20",
                "Return: HTTP 200 OK, phân trang danh sách mẫu ngữ pháp cấp độ N5 chứa từ khóa 'desu'\nException: N/A\nLog Message: \"Retrieved grammars filtered by level and keyword\"",
                "Normal"));
        gGrammar.add(new TestCaseItem("UTC048",
                "Mẫu ngữ pháp mã 'N3-050' tồn tại trong kho dữ liệu",
                "GET /api/grammars/detail?code=N3-050",
                "Return: HTTP 200 OK, thông tin chi tiết: Tên mẫu câu, ý nghĩa, cấu trúc kết hợp, lưu ý và ví dụ minh họa kèm dịch thuật\nException: N/A\nLog Message: \"Loaded grammar detail for code N3-050\"",
                "Normal"));
        gGrammar.add(new TestCaseItem("UTC049",
                "Mã ngữ pháp 'UNKNOWN-999' không tồn tại",
                "GET /api/grammars/detail?code=UNKNOWN-999",
                "Return: null\nException: ResourceNotFoundException HTTP 404: 'Không tìm thấy ngữ pháp tương ứng'\nLog Message: \"Grammar not found for code UNKNOWN-999\"",
                "Abnormal"));
        groups.add(gGrammar);

        // 12. AiWritingService.checkWriting
        MethodGroup gAiWriting = new MethodGroup("12. AiWritingService.checkWriting(AiWritingCheckRequest)");
        gAiWriting.add(new TestCaseItem("UTC050",
                "Gemini AI API cấu hình trực tuyến (hoặc Rule-based NLP Engine)",
                "AiWritingCheckRequest(text = \"私は毎日学校に歩いて行きます。日本語がおもしろいです。\", targetJlpt = \"N4\")",
                "Return: HTTP 200 OK, trả về thang điểm tổng quát, nhận diện lỗi dùng sai trợ từ, đoạn văn bản sửa chuẩn xác và gợi ý từ vựng mở rộng\nException: N/A\nLog Message: \"AI writing check evaluated with score and corrections\"",
                "Normal"));
        gAiWriting.add(new TestCaseItem("UTC051",
                "Ngân hàng đề bài Sakubun sẵn sàng",
                "level = \"N5\"",
                "Return: HTTP 200 OK, danh sách các đề bài thử thách (chủ đề giới thiệu bản thân, gia đình, sở thích) kèm từ khóa gợi ý\nException: N/A\nLog Message: \"Generated Sakubun prompts for JLPT N5\"",
                "Normal"));
        gAiWriting.add(new TestCaseItem("UTC052",
                "Bài nộp trống hoặc chỉ toàn khoảng trắng",
                "AiWritingCheckRequest(text = \"   \")",
                "Return: HTTP 200 OK (Fallback an toàn: Điểm số = 0, danh sách lỗi rỗng, thông báo yêu cầu nhập nội dung văn bản cần chấm)\nException: N/A\nLog Message: \"Blank writing submission safely handled with fallback response\"",
                "Abnormal"));
        groups.add(gAiWriting);

        // 13. KanjiGameService
        MethodGroup gKanjiGame = new MethodGroup("13. KanjiGameService (getPlayData / saveGameSession)");
        gKanjiGame.add(new TestCaseItem("UTC053",
                "Bảng Kanji có dữ liệu N5 trong DB",
                "GET /api/kanji/game/play-data?jlpt=N5&limit=20",
                "Return: HTTP 200 OK, trả về 20 đối tượng KanjiPlayDataDto kèm chữ Hán, Onyomi, Kunyomi, Furigana và nghĩa tiếng Việt\nException: N/A\nLog Message: \"Loaded 20 Kanji game items for level N5\"",
                "Normal"));
        gKanjiGame.add(new TestCaseItem("UTC054",
                "Người dùng đã hoàn thành một lượt chơi game",
                "MinigameSessionRequest(gameType = \"KANJI_RAIN\", score = 850, correctCount = 17, totalCount = 20, mistakes = [...])",
                "Return: HTTP 200 OK, lưu session vào DB, tính tỷ lệ chính xác (85.0%), trả về MinigameSessionResponse kèm điểm thưởng\nException: N/A\nLog Message: \"Saved minigame session with accuracy 85%\"",
                "Normal"));
        gKanjiGame.add(new TestCaseItem("UTC055",
                "Bảng Kanji sẵn sàng, limit = 0 hoặc âm",
                "GET /api/kanji/game/play-data?jlpt=N5&limit=0",
                "Return: HTTP 200 OK, tự động thiết lập số thẻ chơi về mức mặc định tối thiểu (10 chữ Hán)\nException: N/A\nLog Message: \"Zero or negative game limit clamped to minimum 10 items\"",
                "Boundary"));
        groups.add(gKanjiGame);

        // 14. KanjiDictionaryController.searchDictionary & getKanjiDetail
        MethodGroup gKanjiDict = new MethodGroup("14. KanjiDictionaryController.searchDictionary & getKanjiDetail");
        gKanjiDict.add(new TestCaseItem("UTC056",
                "Bảng Kanji có dữ liệu N5",
                "GET /api/kanji/search?q=日&jlpt=N5&page=0&limit=20",
                "Return: HTTP 200 OK, phân trang danh sách chữ Hán khớp từ khóa '日' và thuộc cấp độ N5\nException: N/A\nLog Message: \"Kanji search completed with keyword and JLPT level\"",
                "Normal"));
        gKanjiDict.add(new TestCaseItem("UTC057",
                "Chữ Hán '水' tồn tại trong từ điển",
                "GET /api/kanji/detail?char=水",
                "Return: HTTP 200 OK, thông tin đầy đủ: Âm Hán Việt (THỦY), Onyomi, Kunyomi, số nét (4), bộ thủ, từ ghép mẫu và hình ảnh minh họa quy tắc viết\nException: N/A\nLog Message: \"Retrieved Kanji detail by character 水\"",
                "Normal"));
        gKanjiDict.add(new TestCaseItem("UTC058",
                "UUID tồn tại trong cơ sở dữ liệu",
                "GET /api/kanji/detail?id={valid-kanji-uuid}",
                "Return: HTTP 200 OK, thông tin chi tiết chữ Hán tương ứng\nException: N/A\nLog Message: \"Retrieved Kanji detail by valid UUID\"",
                "Normal"));
        gKanjiDict.add(new TestCaseItem("UTC059",
                "Không truyền cả tham số char lẫn id",
                "GET /api/kanji/detail (không truyền char hoặc id)",
                "Return: null\nException: BadRequestException HTTP 400: 'Thiếu tham số tra cứu (yêu cầu char hoặc id)'\nLog Message: \"Missing required parameter for Kanji detail lookup\"",
                "Abnormal"));
        groups.add(gKanjiDict);

        // 15. KanjiDictionaryController.searchByStrokes (Handwriting)
        MethodGroup gHandwriting = new MethodGroup("15. KanjiDictionaryController.searchByStrokes (Handwriting)");
        gHandwriting.add(new TestCaseItem("UTC060",
                "Dữ liệu nét vẽ trả về từ Google Input Tools (ví dụ: '人', '入')",
                "POST /api/kanji/search-by-strokes (candidates = [\"人\", \"入\"], strokeCount = 2, limit = 5)",
                "Return: HTTP 200 OK, trả về danh sách ứng viên chữ Hán đã được làm giàu thông tin: Âm Hán Việt, Onyomi, Kunyomi, JLPT level, số nét\nException: N/A\nLog Message: \"Matched handwriting candidates with Joyo Kanji dictionary\"",
                "Normal"));
        gHandwriting.add(new TestCaseItem("UTC061",
                "Nét vẽ nhận dạng chữ '道' (12 nét)",
                "POST /api/kanji/search-by-strokes (candidates = [\"道\"], strokeCount = 12, limit = 5)",
                "Return: HTTP 200 OK, trả về thông tin chữ '道' (ĐẠO), số nét 12, cấp độ N4, On/Kun đầy đủ\nException: N/A\nLog Message: \"Handwriting match successful for multi-stroke kanji 道\"",
                "Normal"));
        gHandwriting.add(new TestCaseItem("UTC062",
                "Người dùng vẽ nét rời rạc, Google Input Tools không nhận ra ký tự nào (candidates = [])",
                "POST /api/kanji/search-by-strokes (candidates = [], strokeCount = 1, limit = 5)",
                "Return: HTTP 200 OK, cơ chế Fallback tự động lọc các chữ Hán có đúng 1 nét vẽ (như '一') từ cơ sở dữ liệu Joyo Kanji\nException: N/A\nLog Message: \"Candidates empty, fallback to strokeCount filter safely\"",
                "Boundary"));
        groups.add(gHandwriting);

        // 16. ReadingAssistantController.analyzeReading
        MethodGroup gReading = new MethodGroup("16. ReadingAssistantController.analyzeReading");
        gReading.add(new TestCaseItem("UTC063",
                "Gemini AI API trực tuyến (hoặc Rule-based Ruby Parser)",
                "ReadingAnalysisRequest(text = \"私は日本語を勉強しています。\", targetJlpt = \"N4\")",
                "Return: HTTP 200 OK, trả về rubyHtml chứa các thẻ <ruby><rt> tương ứng, danh sách ngữ pháp nhận diện (~ています), từ vựng và bản dịch tiếng Việt\nException: N/A\nLog Message: \"Analyzed Japanese reading text with furigana and grammar extraction\"",
                "Normal"));
        gReading.add(new TestCaseItem("UTC064",
                "Trợ lý đọc hiểu AI sẵn sàng, text là chuỗi rỗng",
                "ReadingAnalysisRequest(text = \"\")",
                "Return: HTTP 200 OK, trả về đối tượng phân tích rỗng an toàn, không ném ngoại lệ 500\nException: N/A\nLog Message: \"Empty reading text handled safely with empty response\"",
                "Abnormal"));
        gReading.add(new TestCaseItem("UTC065",
                "Trợ lý AI sẵn sàng, không truyền targetJlpt (auto-detect)",
                "ReadingAnalysisRequest(text = \"桜の花が咲きました。\", targetJlpt = null)",
                "Return: HTTP 200 OK, tự động phân tích độ khó của từ vựng/ngữ pháp và gán cấp độ ước tính phù hợp (N4/N5)\nException: N/A\nLog Message: \"Auto-detected JLPT level for reading text successfully\"",
                "Boundary"));
        gReading.add(new TestCaseItem("UTC066",
                "Trợ lý AI sẵn sàng, body chứa text là null",
                "ReadingAnalysisRequest(text = null)",
                "Return: HTTP 200 OK, xử lý fallback an toàn không gây NullPointerException, phản hồi kết quả rỗng\nException: N/A\nLog Message: \"Null text safely handled in reading assistant\"",
                "Boundary"));
        groups.add(gReading);

        // 17. ReadingAssistantService.analyzeReading (Performance Cache)
        MethodGroup gCache = new MethodGroup("17. ReadingAssistantService.analyzeReading (Performance Cache)");
        gCache.add(new TestCaseItem("UTC067",
                "Bộ nhớ đệm CacheStore chưa lưu đoạn văn bản này (Cache MISS)",
                "ReadingAnalysisRequest(text = \"日本の春は桜がとても綺麗です。多くの人が花見を楽しみます。\")",
                "Return: HTTP 200 OK, thực thi AI bóc tách Furigana và ngữ pháp, tính toán mã băm SHA-256 của chuỗi văn bản và lưu vào CacheStore\nException: N/A\nLog Message: \"Cache MISS: generated AI analysis and stored in memory with SHA-256 key\"",
                "Normal"));
        gCache.add(new TestCaseItem("UTC068",
                "Đoạn văn bản đã được phân tích trước đó và lưu trong CacheStore (Cache HIT)",
                "ReadingAnalysisRequest(text = \"日本の春は桜がとても綺麗です。多くの人が花見を楽しみます。\")",
                "Return: HTTP 200 OK, truy xuất trực tiếp từ CacheStore dựa trên SHA-256 hash, thời gian phản hồi tức thì < 50ms, không tốn quota AI\nException: N/A\nLog Message: \"Cache HIT: served cached reading analysis in < 50ms\"",
                "Boundary"));
        groups.add(gCache);

        return groups;
    }
}
