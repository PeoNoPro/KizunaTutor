package com.kizuna.features.tutor_search.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-05 / UC-05.06 Open Tutor Profile by Short Link - TutorSearchServiceImpl.getTutorByCode(tutorCode).
 */
class TutorByCodeTest extends TutorSearchUnitTestBase {

    @Test @DisplayName("UTCID01: Link ngắn với mã gia sư 10001 -> trả hồ sơ")
    void utcid01_byCode() {
        when(repo.findByTutorCode(10001L)).thenReturn(Optional.of(tutor(TUTOR_ID, "Nguyen Van A", "Hà Nội")));
        assertEquals(TUTOR_ID, service.getTutorByCode(10001L).getTutorId());
    }

    @Test @DisplayName("UTCID02: Mã gia sư không tồn tại -> service trả null (controller trả 404)")
    void utcid02_unknownCode() {
        when(repo.findByTutorCode(99999L)).thenReturn(Optional.empty());
        assertNull(service.getTutorByCode(99999L));
    }
}
