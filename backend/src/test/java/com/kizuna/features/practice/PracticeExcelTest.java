package com.kizuna.features.practice;

import com.kizuna.features.practice.service.PracticeExcel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PracticeExcelTest {

    @Test
    void templateReadsBackWithSectionsGroupsAndImages() {
        var exam = PracticeExcel.read(new ByteArrayInputStream(PracticeExcel.template()));
        assertEquals("N5", exam.level());
        assertEquals("2024-07-01", exam.examMonth().toString());
        assertEquals(3, exam.questionCount());
        assertEquals(java.util.List.of("VOCAB", "GRAMMAR_READING", "LISTENING"), exam.mondai().stream().map(PracticeExcel.Mondai::section).toList());
        assertEquals("D1", exam.mondai().get(1).groups().get(0).code());
        assertTrue(exam.imageNames().contains("nghe-1-1.png"));
        assertEquals(20, exam.minutesVocab());
    }

    @Test
    void reportsEveryBadRowWithoutSaving() throws Exception {
        byte[] bytes;
        try (var book = new XSSFWorkbook(new ByteArrayInputStream(PracticeExcel.template())); var out = new ByteArrayOutputStream()) {
            Row row = book.getSheet("CauHoi").getRow(1);
            row.getCell(12).setCellValue("5");                 // đáp án ngoài số lựa chọn
            row.getCell(7).setCellValue("{来月|らいげつ を");     // furigana thiếu }
            book.getSheet("CauHoi").getRow(2).getCell(0).setCellValue("Kanji"); // phần thi sai
            book.write(out); bytes = out.toByteArray();
        }
        var error = assertThrows(PracticeExcel.InvalidFile.class, () -> PracticeExcel.read(new ByteArrayInputStream(bytes)));
        assertTrue(error.problems.stream().anyMatch(p -> p.row() == 2 && p.message().contains("Đáp án")));
        assertTrue(error.problems.stream().anyMatch(p -> p.row() == 2 && p.message().contains("furigana")));
        assertTrue(error.problems.stream().anyMatch(p -> p.row() == 3 && p.message().contains("Phần thi")));
    }

    @Test
    void markupRules() {
        assertNull(PracticeExcel.markup("いえの {前|まえ}で __工事__を"));
        assertNotNull(PracticeExcel.markup("{前まえ}"));
        assertNotNull(PracticeExcel.markup("__工事"));
        assertNotNull(PracticeExcel.markup("前|まえ}"));
    }
}
