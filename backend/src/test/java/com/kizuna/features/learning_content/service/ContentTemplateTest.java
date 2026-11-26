package com.kizuna.features.learning_content.service;

import com.kizuna.features.learning_content.service.ContentWorkbookReader.ParsedWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** Tệp mẫu học liệu: tải về rồi nhập lại ngay phải hợp lệ (đúng tiêu đề, không có dòng dữ liệu, sheet hướng dẫn bị bỏ qua). */
class ContentTemplateTest {

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void templateIsReadableByImporter(ContentType type) {
        ParsedWorkbook parsed = ContentWorkbookReader.read(type, new ByteArrayInputStream(ContentTemplate.build(type)));

        assertThat(parsed.issues()).isEmpty();
        assertThat(parsed.sheets()).hasSize(1).doesNotContain("HuongDan");
        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.totalRows()).isZero();
        assertThat(parsed.invalidRows()).isZero();
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void headerListsEveryColumnInOrder(ContentType type) throws IOException {
        try (Workbook book = new XSSFWorkbook(new ByteArrayInputStream(ContentTemplate.build(type)))) {
            Row header = book.getSheetAt(0).getRow(0);
            assertThat(header.getLastCellNum()).isEqualTo((short) type.fields().size());
            for (int i = 0; i < type.fields().size(); i++) {
                assertThat(header.getCell(i).getStringCellValue()).isEqualTo(type.fields().get(i).header());
            }
            assertThat(book.getSheetAt(0).getLastRowNum()).isZero();
            assertThat(book.getSheet("HuongDan")).isNotNull();
        }
    }

    @ParameterizedTest
    @EnumSource(ContentType.class)
    void guideNamesRequiredColumnsAndFileNameIsXlsx(ContentType type) {
        String guide = String.join("\n", ContentTemplate.guideLines(type));
        type.requiredFields().forEach(f -> assertThat(guide).contains(f.header()));
        assertThat(ContentTemplate.fileName(type)).startsWith("mau-").endsWith(".xlsx");
    }
}
