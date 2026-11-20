package com.kizuna.features.learning_support;

import com.kizuna.features.learning_support.controller.KanjiDictionaryController;
import com.kizuna.features.learning_support.dto.KanjiCardDto;
import com.kizuna.features.learning_support.entity.JlptLevel;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.learning_support.service.KanjiDictionaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Kanji Dictionary & Handwriting Search Controller Unit Tests")
class KanjiDictionaryControllerTest {

    private KanjiDictionaryService kanjiService;
    private KanjiDictionaryController controller;

    @BeforeEach
    void setUp() {
        kanjiService = Mockito.mock(KanjiDictionaryService.class);
        controller = new KanjiDictionaryController(kanjiService);
    }

    @Test
    @DisplayName("UTCID01 (Normal): Tìm kiếm chữ Hán theo từ khóa và JLPT N5 -> Trả về HTTP 200 và cấu trúc phân trang")
    void searchDictionary_validParams_shouldReturn200AndPage() {
        KanjiCardDto dto = KanjiCardDto.builder()
                .kanjiChar("日")
                .hanViet("NHẬT")
                .meaning("mặt trời")
                .strokeCount((short) 4)
                .jlpt("N5")
                .build();
        Page<KanjiCardDto> mockPage = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(kanjiService.searchKanji(eq("日"), eq("N5"), isNull(), isNull(), isNull(), any()))
                .thenReturn(mockPage);

        ResponseEntity<Map<String, Object>> resp = controller.searchDictionary("日", "N5", null, null, null, 0, 20);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1L, resp.getBody().get("totalElements"));
    }

    @Test
    @DisplayName("UTCID02 (Normal): Lấy chi tiết chữ Hán theo ký tự 'char' -> Trả về HTTP 200")
    void getKanjiDetail_byChar_shouldReturnOk() {
        KanjiDictionary kd = KanjiDictionary.builder()
                .kanjiId(UUID.randomUUID())
                .kanjiCharacter("水")
                .hanViet("THỦY")
                .meaning("nước")
                .strokeCount((short) 4)
                .jlptLevel(JlptLevel.N5)
                .build();

        when(kanjiService.findByCharacter("水")).thenReturn(Optional.of(kd));

        ResponseEntity<?> resp = controller.getKanjiDetail("水", null);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("UTCID03 (Normal): Lấy chi tiết chữ Hán theo id -> Trả về HTTP 200")
    void getKanjiDetail_byId_shouldReturnOk() {
        UUID id = UUID.randomUUID();
        KanjiDictionary kd = KanjiDictionary.builder()
                .kanjiId(id)
                .kanjiCharacter("木")
                .hanViet("MỘC")
                .build();

        when(kanjiService.findById(id)).thenReturn(Optional.of(kd));

        ResponseEntity<?> resp = controller.getKanjiDetail(null, id);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    @DisplayName("UTCID04 (Abnormal): Lấy chi tiết chữ Hán không truyền cả char lẫn id -> Trả về HTTP 400 Bad Request")
    void getKanjiDetail_noParams_shouldReturn400() {
        ResponseEntity<?> resp = controller.getKanjiDetail(null, null);

        assertNotNull(resp);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    @DisplayName("UTCID05 (Normal): Tra cứu chữ Hán theo path variable /{character} -> Trả về HTTP 200")
    void getKanjiByChar_existingChar_shouldReturnOk() {
        KanjiDictionary kd = KanjiDictionary.builder()
                .kanjiCharacter("火")
                .hanViet("HỎA")
                .build();

        when(kanjiService.findByCharacter("火")).thenReturn(Optional.of(kd));

        ResponseEntity<KanjiDictionary> resp = controller.getKanjiByChar("火");

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("火", resp.getBody().getKanjiCharacter());
    }

    @Test
    @DisplayName("UTCID06 (Normal): Nhận diện chữ viết tay qua nét vẽ candidates -> Trả về HTTP 200 và danh sách khớp")
    void searchByStrokes_withCandidates_shouldReturnMatches() {
        KanjiDictionary kd = KanjiDictionary.builder()
                .kanjiCharacter("人")
                .hanViet("NHÂN")
                .meaning("người")
                .strokeCount((short) 2)
                .jlptLevel(JlptLevel.N5)
                .build();

        when(kanjiService.findByCharacter("人")).thenReturn(Optional.of(kd));

        Map<String, Object> body = Map.of(
                "candidates", List.of("人", "入"),
                "strokeCount", 2,
                "limit", 5
        );

        ResponseEntity<Map<String, Object>> resp = controller.searchByStrokes(body);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("matches"));
    }

    @Test
    @DisplayName("UTCID07 (Boundary): Tra cứu nét vẽ chỉ có strokeCount không có candidates -> Trả về HTTP 200")
    void searchByStrokes_withOnlyStrokeCount_shouldReturnMatches() {
        KanjiCardDto dto = KanjiCardDto.builder()
                .kanjiChar("一")
                .hanViet("NHẤT")
                .strokeCount((short) 1)
                .build();
        Page<KanjiCardDto> mockPage = new PageImpl<>(List.of(dto));

        when(kanjiService.searchKanji(isNull(), isNull(), eq((short) 1), isNull(), isNull(), any()))
                .thenReturn(mockPage);

        Map<String, Object> body = Map.of(
                "strokeCount", 1,
                "limit", 10
        );

        ResponseEntity<Map<String, Object>> resp = controller.searchByStrokes(body);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("matches"));
    }
}
