package com.kizuna.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Pattern;

public class Print83Vocab {
    private static final Pattern VIETNAMESE_PATTERN = Pattern.compile(
            "[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]"
    );
    private static final Pattern ENGLISH_WORD_PATTERN = Pattern.compile("[a-zA-Z]{3,}");

    public static void main(String[] args) throws Exception {
        Workbook wb = new XSSFWorkbook(new FileInputStream("C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\Kho_Tu_Vung_OpenJLPT_Tieng_Viet.xlsx"));
        Sheet sheet = wb.getSheetAt(0);
        int count = 0;
        for (int r = 1; r < sheet.getPhysicalNumberOfRows(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String word = row.getCell(2).getStringCellValue();
            String reading = row.getCell(3).getStringCellValue();
            String meaning = row.getCell(7).getStringCellValue();
            String exVn = row.getCell(10).getStringCellValue();
            boolean meaningViolates = !VIETNAMESE_PATTERN.matcher(meaning).find() && ENGLISH_WORD_PATTERN.matcher(meaning).find();
            boolean exViolates = !VIETNAMESE_PATTERN.matcher(exVn).find() && ENGLISH_WORD_PATTERN.matcher(exVn).find();
            if (meaningViolates || exViolates) {
                count++;
                System.out.println(String.format("%d. Row %d [%s / %s]: Meaning=\"%s\" | Ex=\"%s\"",
                        count, r + 1, word, reading, meaning, exVn));
            }
        }
    }
}
