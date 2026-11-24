package com.kizuna.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.junit.jupiter.api.Test;

import java.io.File;

public class InspectUatStylesTest {

    @Test
    public void inspectStyles() throws Exception {
        File file = new File("C:/Users/Vuh26/.gemini/antigravity/brain/9c32454a-b322-4db4-b73f-ca7159e96a64/.user_uploaded/media_1795435645205_a61de299.xlsx");
        try (Workbook wb = WorkbookFactory.create(file)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet s = wb.getSheetAt(i);
                System.out.println("Sheet " + i + ": " + s.getSheetName());
                Row header = s.getRow(0);
                if (header != null) {
                    System.out.println("Header row height: " + header.getHeightInPoints());
                    for (int c = 0; c < header.getLastCellNum(); c++) {
                        Cell cell = header.getCell(c);
                        if (cell != null) {
                            CellStyle cs = cell.getCellStyle();
                            Font font = wb.getFontAt(cs.getFontIndex());
                            System.out.println("Col " + c + " ('" + cell.getStringCellValue() + "'): width=" + s.getColumnWidth(c) + 
                                    ", font=" + font.getFontName() + " " + font.getFontHeightInPoints() + " bold=" + font.getBold() +
                                    ", fillPattern=" + cs.getFillPattern() +
                                    ", align=" + cs.getAlignment());
                        }
                    }
                }
            }
        }
    }
}
