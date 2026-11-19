package com.kizuna.features.practice;

import com.kizuna.features.practice.service.PracticeExcel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra các tệp đề đã chép sẵn (docs/de mau/&lt;đề&gt;/nhap-ngan-hang-de/*.xlsx) đọc được bằng đúng bộ đọc của hệ thống
 * và mọi ảnh được nhắc tới đều có trong thư mục ảnh kèm theo. Không có tệp nào thì bỏ qua.
 */
class PracticeSampleFilesTest {

    @Test
    void sampleExamFilesAreValid() throws IOException {
        Path root = Path.of("..", "docs", "de mau");
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".xlsx") && !p.getFileName().toString().startsWith("~$") && p.getParent().getFileName().toString().equals("nhap-ngan-hang-de")).toList()) {
                PracticeExcel.Exam exam;
                try (InputStream in = Files.newInputStream(file)) {
                    exam = PracticeExcel.read(in);
                } catch (PracticeExcel.InvalidFile invalid) {
                    fail(file + ": " + invalid.problems.stream().map(p -> "dòng " + p.row() + " " + p.message()).toList());
                    return;
                }
                assertTrue(exam.questionCount() > 0, file.toString());
                Path images = file.getParent();
                for (String name : exam.imageNames()) {
                    try (Stream<Path> found = Files.walk(images)) {
                        assertTrue(found.anyMatch(p -> p.getFileName().toString().equalsIgnoreCase(name)), file + " thiếu ảnh " + name);
                    }
                }
                System.out.println(file.getFileName() + ": " + exam.questionCount() + " câu, " + exam.mondai().size() + " mondai, " + exam.imageNames().size() + " ảnh");
            }
        }
    }
}
