package com.kizuna.features.tutor_search.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TutorSearchSpecificationLevelTest {

    @Test
    @DisplayName("Lọc trình độ là 'từ mức này trở lên': N3 → gia sư N1, N2, N3")
    void levelsAtLeast_includesHigherLevels() {
        assertEquals(List.of("N1", "N2", "N3"), TutorSearchSpecification.levelsAtLeast("N3"));
        assertEquals(List.of("N1"), TutorSearchSpecification.levelsAtLeast("n1"));
        assertEquals(List.of("N1", "N2", "N3", "N4", "N5"), TutorSearchSpecification.levelsAtLeast("N5"));
    }

    @Test
    @DisplayName("Không lọc khi bỏ trống, 'all' hoặc giá trị không hợp lệ")
    void levelsAtLeast_noFilter() {
        assertNull(TutorSearchSpecification.levelsAtLeast(null));
        assertNull(TutorSearchSpecification.levelsAtLeast(""));
        assertNull(TutorSearchSpecification.levelsAtLeast("all"));
        assertNull(TutorSearchSpecification.levelsAtLeast("N9"));
    }
}
