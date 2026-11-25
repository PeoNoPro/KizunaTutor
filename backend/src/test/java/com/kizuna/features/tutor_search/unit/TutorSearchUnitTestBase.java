package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.tutor_post.service.TutorPostService;
import com.kizuna.features.tutor_search.repository.TutorSearchRepository;
import com.kizuna.features.tutor_search.service.impl.TutorSearchServiceImpl;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Dữ liệu và mock dùng chung cho unit test FE-05 (tìm gia sư) - TutorSearchServiceImpl.
 * Repository, JdbcTemplate (nạp lịch / thống kê theo lô), bài đăng và lịch bận là mock: JdbcTemplate mock không trả dòng nào,
 * nên lịch và thống kê của gia sư là rỗng.
 */
abstract class TutorSearchUnitTestBase {
    static final UUID TUTOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    final TutorSearchRepository repo = mock(TutorSearchRepository.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final TutorPostService posts = mock(TutorPostService.class);
    final ConnectionBusyLoader busyLoader = mock(ConnectionBusyLoader.class);
    final TutorSearchServiceImpl service = new TutorSearchServiceImpl(repo, jdbc, posts, busyLoader);

    {
        when(busyLoader.load(any(), any(), any(), any(), any())).thenReturn(BusyIndex.EMPTY);
    }

    static Tutor tutor(UUID id, String name, String address) {
        User u = User.builder().userId(UUID.randomUUID()).fullName(name).email(name + "@test.com").build();
        return Tutor.builder().tutorId(id).tutorCode(10001L).user(u).headline("JLPT teacher").highestJlptLevel("N1")
                .experienceYears((short) 3).hourlyRate(new BigDecimal("200000")).avgRating(new BigDecimal("4.5"))
                .totalReviews(10).address(address).build();
    }
}
