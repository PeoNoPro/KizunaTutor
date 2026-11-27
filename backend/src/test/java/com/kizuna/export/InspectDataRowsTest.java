package com.kizuna.export;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.File;

public class InspectDataRowsTest {
    @Test
    public void test() throws Exception {
        File file = new File("C:/Users/Vuh26/.gemini/antigravity/brain/9c32454a-b322-4db4-b73f-ca7159e96a64/.user_uploaded/media_1795435645205_a61de299.xlsx");
        try (Workbook wb = WorkbookFactory.create(file)) {
            Sheet s = wb.getSheetAt(0);
            Row r = s.getRow(1);
            if (r != null) {
                System.out.println("Row 1 height: " + r.getHeightInPoints());
                for (int c = 0; c < r.getLastCellNum(); c++) {
                    Cell cell = r.getCell(c);
                    if (cell != null) {
                        CellStyle cs = cell.getCellStyle();
                        Font f = wb.getFontAt(cs.getFontIndex());
                        System.out.println("Data cell " + c + ": font=" + f.getFontName() + " " + f.getFontHeightInPoints() + 
                                ", wrap=" + cs.getWrapText() + ", align=" + cs.getAlignment() + ", borderBottom=" + cs.getBorderBottom());
                    }
                }
            }
        }
    }
}
