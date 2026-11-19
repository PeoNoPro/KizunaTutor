package com.kizuna.features.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Các lỗi nhóm chỉ ra khi review PR #27 (FE-13). */
class PracticeReviewFixesTest {
    private static final UUID LEARNER = UUID.randomUUID(), ATTEMPT = UUID.randomUUID(), QUESTION = UUID.randomUUID();

    @Test
    void expiredAnswerGradesAndReturnsInsteadOfThrowingSoTheGradeIsKept() {
        var jdbc = mock(JdbcTemplate.class);
        var attempt = new HashMap<String, Object>(Map.of("attempt_id", ATTEMPT, "learner_id", LEARNER,
                "deadline_at", OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10)));
        when(jdbc.queryForList(startsWith("SELECT * FROM practice_attempts WHERE attempt_id=?"), eq(ATTEMPT))).thenReturn(List.of(attempt));
        var service = new PracticeLearnerService(jdbc, new ObjectMapper(), new PracticeMediaTokens("test-secret"));

        // Trước đây ném 409 ngay sau khi chấm: transaction rollback nên mất kết quả chấm.
        var result = assertDoesNotThrow(() -> service.answer(LEARNER, ATTEMPT, QUESTION, Map.of("chosen", 2)));

        assertEquals(true, result.get("expired"));
        verify(jdbc).update(contains("SET submitted_at=now()"), eq(ATTEMPT), eq(ATTEMPT));
        verify(jdbc, never()).update(contains("SET chosen=?"), any(), any(), any(), any(), any());
    }

    @Test
    void answerWithinTimeIsSavedNormally() {
        var jdbc = mock(JdbcTemplate.class);
        var attempt = new HashMap<String, Object>(Map.of("attempt_id", ATTEMPT, "learner_id", LEARNER,
                "deadline_at", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30)));
        when(jdbc.queryForList(startsWith("SELECT * FROM practice_attempts WHERE attempt_id=?"), eq(ATTEMPT))).thenReturn(List.of(attempt));
        when(jdbc.update(contains("SET chosen=?"), any(), any(), any(), any(), any())).thenReturn(1);
        var service = new PracticeLearnerService(jdbc, new ObjectMapper(), new PracticeMediaTokens("test-secret"));

        assertEquals(Map.of("saved", true), service.answer(LEARNER, ATTEMPT, QUESTION, Map.of("chosen", 2)));
    }

    @Test
    void mediaLinksStayValidAfterRestartWhenASecretIsConfigured() {
        UUID file = UUID.randomUUID();
        String path = new PracticeMediaTokens("same-server-secret").path(file);
        var query = path.substring(path.indexOf('?') + 1).split("&");
        long exp = Long.parseLong(query[0].substring(4));
        String sig = query[1].substring(4);
        // "Khởi động lại": tạo bộ ký mới với cùng khóa cấu hình.
        assertTrue(new PracticeMediaTokens("same-server-secret").valid(file, exp, sig));
        assertFalse(new PracticeMediaTokens("another-secret").valid(file, exp, sig));
    }

    @Test
    void fileSignaturesAreCheckedNotOnlyTheDeclaredType() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] mp3 = {'I', 'D', '3', 4, 0};
        byte[] text = "<html>not an image</html>".getBytes();
        assertTrue(PracticeFileTypes.image("png", png));
        assertTrue(PracticeFileTypes.image("jpg", jpg));
        assertFalse(PracticeFileTypes.image("png", text));
        assertFalse(PracticeFileTypes.image("png", jpg), "đuôi PNG nhưng nội dung là JPG");
        assertTrue(PracticeFileTypes.audio("mp3", mp3));
        assertFalse(PracticeFileTypes.audio("mp3", text));

        // Đọc trước 16 byte nhưng không làm mất dữ liệu khi lưu.
        var in = new BufferedInputStream(new ByteArrayInputStream(png));
        PracticeFileTypes.head(in);
        assertArrayEquals(png, in.readAllBytes());
    }

    @Test
    void editingCorrectAnswerOfAnExamWithAttemptsIsRejected() {
        var jdbc = mock(JdbcTemplate.class);
        UUID exam = UUID.randomUUID();
        when(jdbc.queryForList(startsWith("SELECT * FROM practice_exams WHERE exam_id=?"), eq(exam))).thenReturn(List.of(Map.of("exam_id", exam, "version", 4L)));
        when(jdbc.queryForList(startsWith("SELECT question_id,correct"), eq(QUESTION), eq(exam)))
                .thenReturn(List.of(Map.of("question_id", QUESTION, "correct", 1, "option_count", 4)));
        when(jdbc.queryForObject(contains("FROM practice_attempt_answers"), eq(Long.class), eq(exam))).thenReturn(3L);   // đã có người làm
        var service = new PracticeAdminService(jdbc, new ObjectMapper(), null, new PracticeMediaTokens("x"), null);

        var body = new HashMap<String, Object>(Map.of("version", 4, "stem", "Câu hỏi", "options", List.of("a", "b", "c", "d"), "correct", 2));
        assertEquals(409, assertThrows(BusinessException.class, () -> service.updateQuestion(exam, QUESTION, body)).status());

        body.put("version", 3);   // người khác vừa sửa
        assertEquals(409, assertThrows(BusinessException.class, () -> service.updateQuestion(exam, QUESTION, body)).status());
        verify(jdbc, never()).update(startsWith("UPDATE practice_questions"), any(Object[].class));
    }
}
