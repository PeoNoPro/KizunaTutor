package com.kizuna.features.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.entity.ExerciseQuestionType;
import com.kizuna.features.progress.service.impl.ExerciseImportServiceImpl;
import com.kizuna.features.progress.service.ExerciseImportService.ImportPreview;
import com.kizuna.features.progress.service.ExerciseImportService.ImportRow;
import com.kizuna.features.progress.util.ExerciseQuestionEngine;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Nhập câu hỏi từ Excel: đọc đủ 8 dạng câu, báo lỗi đúng dòng, file mẫu đọc lại được. */
class ExerciseImportServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private ExerciseImportServiceImpl service;
    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;

    @BeforeEach
    void setUp() {
        ClassroomService classrooms = mock(ClassroomService.class);
        service = new ExerciseImportServiceImpl(classrooms, new ExerciseQuestionEngine(mapper), mapper);
        Tutor tutor = tutor();
        space = space(tutor, learner());
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(space.getLearner());
        when(classrooms.loadManageableSpace(eq(tutorActor), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
    }

    private MockMultipartFile xlsx(String[][] rows) throws Exception {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("CauHoi");
            sheet.createRow(0).createCell(0).setCellValue("Loại câu");
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < rows[i].length; j++) row.createCell(j).setCellValue(rows[i][j]);
            }
            book.write(out);
            return new MockMultipartFile("file", "cau-hoi.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void readsAllEightQuestionTypes() throws Exception {
        ImportPreview p = service.preview(tutorActor, space.getSpaceId(), xlsx(new String[][]{
                {"Trắc nghiệm 1 đáp án", "「新聞」の読み方は？", "しんぶん | しんもん | にゅうぶん", "しんぶん", "2"},
                {"trac nghiem nhieu dap an", "Chọn động từ", "食べる | 赤い | 行く", "1 | 3", "2"},
                {"Đúng / Sai", "「右」 nghĩa là bên phải.", "", "Đúng", ""},
                {"Điền khuyết", "毎朝 ___ を飲みます。", "", "牛乳;ぎゅうにゅう", "2"},
                {"WORD_BANK", "私は ___ で ___ を買いました。", "駅/切符 | 学校/本", "駅 / 切符", "2"},
                {"Nối cột", "Nối", "犬 = con chó | 猫 = con mèo", "", "3"},
                {"Sắp xếp thứ tự", "Sắp xếp", "私は | 毎日 | 勉強します", "", "2"},
                {"Tự luận", "Viết 3 câu.", "", "", "5"}
        }));

        assertThat(p.total()).isEqualTo(8);
        assertThat(p.valid()).isEqualTo(8);
        assertThat(p.validPoints()).isEqualByComparingTo("19");
        List<ImportRow> r = p.rows();
        assertThat(r.get(0).type()).isEqualTo(ExerciseQuestionType.SINGLE_CHOICE);
        assertThat(r.get(0).data().get("correct").asInt()).isEqualTo(0);
        assertThat(r.get(1).data().get("correct").toString()).isEqualTo("[0,2]");
        assertThat(r.get(2).data().get("correct").asBoolean()).isTrue();
        assertThat(r.get(2).points()).isEqualByComparingTo("1");
        assertThat(r.get(3).prompt()).isNull();
        assertThat(r.get(3).data().get("answers").toString()).isEqualTo("[[\"牛乳\",\"ぎゅうにゅう\"]]");
        assertThat(r.get(4).data().get("options").toString()).isEqualTo("[\"駅/切符\",\"学校/本\"]");
        assertThat(r.get(4).data().get("correct").asInt()).isZero();
        assertThat(r.get(5).data().get("pairs").get(1).get("right").asText()).isEqualTo("con mèo");
        assertThat(r.get(6).data().get("items").size()).isEqualTo(3);
        // Tự luận: không có đáp án, chờ chấm (AI gợi ý, gia sư duyệt)
        assertThat(r.get(7).data().has("accepted")).isFalse();
        assertThat(r.get(7).data().path("review").asBoolean()).isTrue();
    }

    @Test
    void fillBlankAnswersAreOptionalButMustMatchTheBlanksWhenGiven() throws Exception {
        ImportPreview p = service.preview(tutorActor, space.getSpaceId(), xlsx(new String[][]{
                {"Điền khuyết", "昨日、友達 ___ 映画を見ました。", "", "", "1"},
                {"Điền khuyết", "A ___ B ___", "", "x", "1"},
                {"Điền từ có sẵn", "___ は ___ です", "これ/ペン | あれ/本", "2", "1"},
                {"Điền từ có sẵn", "___ は ___ です", "これ/ペン | あれ", "1", "1"}
        }));
        assertThat(p.rows().get(0).ok()).isTrue();
        assertThat(p.rows().get(0).data().get("answers").toString()).isEqualTo("[[]]");
        assertThat(p.rows().get(1).error()).contains("2 chỗ trống").contains("1 đáp án");
        // Điền từ có sẵn: mỗi phương án đủ từ cho các chỗ trống, đáp án theo số thứ tự
        assertThat(p.rows().get(2).ok()).isTrue();
        assertThat(p.rows().get(2).data().get("correct").asInt()).isEqualTo(1);
        assertThat(p.rows().get(3).error()).contains("phương án B phải có đủ 2 từ");
    }

    @Test
    void badRowsAreReportedWithRowNumberAndValidRowsStillCount() throws Exception {
        ImportPreview p = service.preview(tutorActor, space.getSpaceId(), xlsx(new String[][]{
                {"Trắc nghiệm 1 đáp án", "Câu 1", "a | b", "c", "2"},
                {"Trac nghiem", "Câu 2", "a | b", "a", "2"},
                {"Điền khuyết", "Không có chỗ trống", "", "x", "1"},
                {"Đúng / Sai", "Câu 4", "", "Có lẽ", "1"},
                {"Nối cột", "Câu 5", "chó - dog | mèo - cat", "", "1"},
                {"Tự luận", "Câu 6", "", "", "abc"},
                {"Đúng / Sai", "Câu 7", "", "Sai", "1"}
        }));

        assertThat(p.total()).isEqualTo(7);
        assertThat(p.valid()).isEqualTo(1);
        assertThat(p.invalid()).isEqualTo(6);
        assertThat(p.rows().get(0).error()).startsWith("Dòng 2:").contains("không có trong lựa chọn");
        assertThat(p.rows().get(1).error()).startsWith("Dòng 3:").contains("loại câu");
        assertThat(p.rows().get(2).error()).startsWith("Dòng 4:").contains("chỗ trống");
        assertThat(p.rows().get(3).error()).contains("Đúng\" hoặc \"Sai");
        assertThat(p.rows().get(4).error()).contains("vế trái = vế phải");
        assertThat(p.rows().get(5).error()).contains("không phải là số");
        assertThat(p.rows().get(6).ok()).isTrue();
    }

    @Test
    void fileRulesAndPermissions() throws Exception {
        assertThatThrownBy(() -> service.preview(tutorActor, space.getSpaceId(),
                new MockMultipartFile("file", "a.csv", "text/csv", "x".getBytes())))
                .isInstanceOf(ClassroomException.class).hasMessageContaining(".xlsx");
        assertThatThrownBy(() -> service.preview(tutorActor, space.getSpaceId(), xlsx(new String[0][])))
                .isInstanceOf(ClassroomException.class).hasMessageContaining("chưa có câu hỏi");
        assertThatThrownBy(() -> service.preview(learnerActor, space.getSpaceId(), xlsx(new String[0][])))
                .isInstanceOfSatisfying(ClassroomException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void templateHasThreeSheetsAndItsExamplesAreValid() throws Exception {
        byte[] bytes = service.template(tutorActor, space.getSpaceId());
        try (Workbook book = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(book.getSheet("CauHoi")).isNotNull();
            assertThat(book.getSheet("HuongDan")).isNotNull();
            Sheet examples = book.getSheet("ViDu");
            // Đổi tên sheet ví dụ thành CauHoi để đọc như file gia sư điền
            book.removeSheetAt(book.getSheetIndex("CauHoi"));
            book.setSheetName(book.getSheetIndex(examples), "CauHoi");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            book.write(out);
            ImportPreview p = service.preview(tutorActor, space.getSpaceId(), new MockMultipartFile("file", "vi-du.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray()));
            assertThat(p.total()).isEqualTo(11);
            assertThat(p.invalid()).as(p.rows().toString()).isZero();
            assertThat(p.rows().stream().filter(ImportRow::listening).count()).isEqualTo(2);
        }
    }

    @Test
    void listeningColumnsMarkQuestionsThatNeedAudio() throws Exception {
        ImportPreview p = service.preview(tutorActor, space.getSpaceId(), xlsx(new String[][]{
                {"Trắc nghiệm 1 đáp án", "Nghe rồi chọn", "駅 | 学校", "駅", "2", "Có", "2"},
                {"Đúng / Sai", "Nghe không giới hạn", "", "Đúng", "1", "có", ""},
                {"Đúng / Sai", "Câu thường", "", "Sai", "1", "", ""},
                {"Đúng / Sai", "Sai cột F", "", "Sai", "1", "Hình như", ""},
                {"Đúng / Sai", "Số lần nghe quá lớn", "", "Sai", "1", "Có", "9"},
                {"Đúng / Sai", "Có số lần mà không đánh dấu câu nghe", "", "Sai", "1", "", "2"}
        }));
        List<ImportRow> r = p.rows();
        assertThat(r.get(0).listening()).isTrue();
        assertThat(r.get(0).maxPlays()).isEqualTo(2);
        assertThat(r.get(1).listening()).isTrue();
        assertThat(r.get(1).maxPlays()).isNull();
        assertThat(r.get(2).listening()).isFalse();
        assertThat(r.get(3).error()).contains("ghi \"Có\" hoặc để trống");
        assertThat(r.get(4).error()).contains("số lần nghe phải từ 1 đến 5");
        assertThat(r.get(5).error()).contains("chưa ghi \"Có\"");
        assertThat(p.valid()).isEqualTo(3);
    }
}
