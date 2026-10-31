package com.kizuna.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class TranslatorBatch {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, String> KANJI_POLISH = new HashMap<>();
    static {
        KANJI_POLISH.put("(kokuji)", "(Quốc tự - chữ Hán người Nhật tự sáng tạo)");
        KANJI_POLISH.put("Decigram, (kokuji)", "Đêxigam (Quốc tự)");
        KANJI_POLISH.put("Miligam, (kokuji)", "Miligam (Quốc tự)");
        KANJI_POLISH.put("Centigram, (kokuji)", "Xentigam (Quốc tự)");
        KANJI_POLISH.put("Surplice, (kokuji)", "Áo cà sa nghi lễ (Quốc tự)");
        KANJI_POLISH.put("Boron, B", "Bo (nguyên tố hóa học Bo, ký hiệu B)");
        KANJI_POLISH.put("(no Known Meaning; S&h Uses Jabberwocky Words)", "Chữ Hán cổ (không rõ nghĩa cụ thể)");
        KANJI_POLISH.put("Good crops, prosperity, 10**28", "Mùa màng bội thu, phồn vinh (số 10^28)");
        KANJI_POLISH.put("Kilolit, (kokuji)", "Kilôlít (Quốc tự)");
        KANJI_POLISH.put("Mililit, (kokuji)", "Mililít (Quốc tự)");
        KANJI_POLISH.put("Deciliter, (kokuji)", "Đêxilít (Quốc tự)");
        KANJI_POLISH.put("Hectolit, (kokuji)", "Héctôlít (Quốc tự)");
        KANJI_POLISH.put("Centilit, (kokuji)", "Xentilít (Quốc tự)");
        KANJI_POLISH.put("Dekamet, (kokuji)", "Đêkamét (Quốc tự)");
        KANJI_POLISH.put("km, (kokuji)", "Kilômét (Quốc tự)");
        KANJI_POLISH.put("Milimet, (kokuji)", "Milimét (Quốc tự)");
        KANJI_POLISH.put("centimet, (kokuji)", "Xentimét (Quốc tự)");
        KANJI_POLISH.put("The Thread/braid (of Armour), (kokuji)", "Dây bện áo giáp (Quốc tự)");
        KANJI_POLISH.put("Reel, Hank, Skein, (kokuji)", "Cuộn chỉ, ống sợi (Quốc tự)");
        KANJI_POLISH.put("Python, Boa Constrictor", "Trăn gấm, trăn khổng lồ");
        KANJI_POLISH.put("Gizzard Shad, (kokuji)", "Cá mòi cờ chấm (Quốc tự)");
        KANJI_POLISH.put("Sillaginoid, (kokuji)", "Cá đục (Quốc tự)");
        KANJI_POLISH.put("Grebe, (kokuji)", "Chim le hôi, chim lặn (Quốc tự)");
        KANJI_POLISH.put("Chim choi choi, (kokuji)", "Chim choi choi (Quốc tự)");
        KANJI_POLISH.put("Holly", "Cây nhựa ruồi (Ilex)");
        KANJI_POLISH.put("Monme, 3,75 gam, (kokuji)", "Đơn vị Monme (3,75 gam, Quốc tự)");
        KANJI_POLISH.put("Anh trai", "Anh trai ruột");
        KANJI_POLISH.put("Cao to", "Cao to, cao lớn");
        KANJI_POLISH.put("Cao, xa", "Cao vút, xa xôi");
        KANJI_POLISH.put("Tin theo", "Tin tưởng noi theo");
        KANJI_POLISH.put("Gai", "Gai nhọn, có gai");
        KANJI_POLISH.put("Lan can", "Tay vịn lan can");
        KANJI_POLISH.put("Xe tang", "Xe tang lễ");
        KANJI_POLISH.put("Ngang", "Nằm ngang, phương ngang");
        KANJI_POLISH.put("Thau", "Chậu thau, đồng thau");
        KANJI_POLISH.put("Linh tinh", "Vụn vặt, linh tinh");
        KANJI_POLISH.put("Con chim non", "Chim non mới nở");
        KANJI_POLISH.put("Run", "Run rẩy, run sợ");
        KANJI_POLISH.put("Con la", "Con la (lai lừa và ngựa)");
        KANJI_POLISH.put("Chim oanh", "Chim vàng anh");
        KANJI_POLISH.put("Con nai", "Con nai vàng");
        KANJI_POLISH.put("Con ba ba", "Con ba ba trơn");
        KANJI_POLISH.put("Quanh co", "Quanh co, uốn khúc");
        KANJI_POLISH.put("Hoa, tinh hoa", "Bông hoa, tinh hoa");
        KANJI_POLISH.put("hoa sen", "Hoa sen thanh khiết");
        KANJI_POLISH.put("Rau vi", "Cây rau vi non");
        KANJI_POLISH.put("Hoa lan", "Hoa phong lan rừng");
        KANJI_POLISH.put("Hoa phong lan", "Hoa phong lan rừng");
        KANJI_POLISH.put("Cái am (nhà tranh nhỏ), nhà nhỏ để thờ Phật", "Am thất (nhà tranh nhỏ), am nhỏ để thờ Phật");
        KANJI_POLISH.put("Con trai", "Đứa con trai");
        KANJI_POLISH.put("Con cua", "Con cua đồng");
        KANJI_POLISH.put("Cua", "Con cua đồng, cua biển");
        KANJI_POLISH.put("cung vua", "Cung điện của vua chúa");
    }

    public static void main(String[] args) throws Exception {
        polishKanjiJsonAndExcel();
    }

    public static void polishKanjiJsonAndExcel() throws Exception {
        System.out.println("=== POLISHING KANJI JSON & EXCEL ===");
        File jsonFile = new File("src/main/resources/data/kanji-dictionary-data.json");
        List<Map<String, Object>> kanjiList = mapper.readValue(new FileInputStream(jsonFile), new TypeReference<>() {});
        int jsonUpdated = 0;

        for (Map<String, Object> map : kanjiList) {
            String m = (String) map.get("meaning");
            if (m == null) continue;
            String clean = m.trim();
            if (KANJI_POLISH.containsKey(clean)) {
                map.put("meaning", KANJI_POLISH.get(clean));
                jsonUpdated++;
            } else if (clean.startsWith("(xem:")) {
                map.put("meaning", clean.replace("(xem:", "(Xem thêm:"));
                jsonUpdated++;
            }
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, kanjiList);
        File targetJson = new File("target/classes/data/kanji-dictionary-data.json");
        if (targetJson.getParentFile().exists()) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(targetJson, kanjiList);
        }
        System.out.println("Updated " + jsonUpdated + " items in kanji-dictionary-data.json");

        // Map characters to meaning
        Map<String, String> charToMeaning = new HashMap<>();
        for (Map<String, Object> map : kanjiList) {
            String ch = (String) map.get("character");
            String m = (String) map.get("meaning");
            if (ch != null && m != null) {
                charToMeaning.put(ch.trim(), m.trim());
            }
        }

        // Now update Excel
        File excelFile = new File("C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\kanji_vietnamese_full.xlsx");
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getPhysicalNumberOfRows();
            int excelUpdated = 0;

            for (int r = 1; r < rowCount; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell charCell = row.getCell(0);
                Cell meaningCell = row.getCell(4);
                if (charCell == null || meaningCell == null) continue;

                String ch = charCell.getStringCellValue().trim();
                if (charToMeaning.containsKey(ch)) {
                    String correct = charToMeaning.get(ch);
                    if (!correct.equals(meaningCell.getStringCellValue())) {
                        meaningCell.setCellValue(correct);
                        excelUpdated++;
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
            System.out.println("Updated " + excelUpdated + " rows in kanji_vietnamese_full.xlsx!");
        }
    }
}
