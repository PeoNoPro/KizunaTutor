package com.kizuna.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;

public class InspectUatTemplateTest {

    @Test
    public void inspect() throws Exception {
        File file = new File("C:/Users/Vuh26/.gemini/antigravity/brain/9c32454a-b322-4db4-b73f-ca7159e96a64/.user_uploaded/media_1795435645205_a61de299.xlsx");
        System.out.println("Exists: " + file.exists() + " len: " + file.length());

        try (Workbook wb = WorkbookFactory.create(file)) {
            System.out.println("Sheet count: " + wb.getNumberOfSheets());
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                System.out.println("\n==================================");
                System.out.println("SHEET " + i + ": " + sheet.getSheetName());
                System.out.println("==================================");
                for (int r = 0; r <= Math.min(sheet.getLastRowNum(), 30); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    StringBuilder sb = new StringBuilder();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null) {
                            String val = cell.toString().replace("\n", "\\n");
                            if (!val.trim().isEmpty()) {
                                sb.append("[").append(c).append("]: ").append(val).append(" | ");
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        System.out.println("Row " + (r + 1) + ": " + sb.toString());
                    }
                }
            }
        }
    }
}
