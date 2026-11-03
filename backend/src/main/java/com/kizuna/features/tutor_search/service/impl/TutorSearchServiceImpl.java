package com.kizuna.features.tutor_search.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO;
import com.kizuna.features.tutor_search.dto.TutorCredentialDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import com.kizuna.features.tutor_search.repository.TutorSearchRepository;
import com.kizuna.features.tutor_search.repository.TutorSearchSpecification;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hiện thực nghiệp vụ tìm kiếm gia sư.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutorSearchServiceImpl implements TutorSearchService {

    private final TutorSearchRepository tutorSearchRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public TutorSearchPageResponse searchTutors(TutorSearchCriteriaDTO criteria) {
        // 1. Xác định thứ tự sắp xếp theo quyết định đã chốt trong spec
        Sort sort;
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "rating";
        switch (sortBy) {
            case "exp":
                sort = Sort.by(Sort.Direction.DESC, "experienceYears");
                break;
            case "price_asc":
                sort = Sort.by(Sort.Direction.ASC, "hourlyRate");
                break;
            case "price_desc":
                sort = Sort.by(Sort.Direction.DESC, "hourlyRate");
                break;
            case "newest":
                sort = Sort.by(Sort.Direction.DESC, "experienceYears");
                break;
            case "rating":
            default:
                sort = Sort.by(Sort.Direction.DESC, "avgRating")
                        .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
                break;
        }

        Pageable pageable = PageRequest.of(Math.max(0, criteria.getPage()), Math.max(1, criteria.getSize()), sort);
        Specification<Tutor> spec = TutorSearchSpecification.build(criteria);
        Page<Tutor> pageResult = tutorSearchRepository.findAll(spec, pageable);

        List<TutorSummaryResponseDTO> dtos = pageResult.getContent().stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        return TutorSearchPageResponse.builder()
                .content(dtos)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<TutorSummaryResponseDTO> getFeaturedTutors() {
        return tutorSearchRepository.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc().stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TutorSummaryResponseDTO getTutorDetail(UUID tutorId) {
        Tutor tutor = tutorSearchRepository.findById(tutorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin gia sư với mã yêu cầu"));
        return mapToSummaryDTO(tutor);
    }

    @Override
    public List<String> getAvailableLocations() {
        Specification<Tutor> spec = TutorSearchSpecification.build(new TutorSearchCriteriaDTO());
        List<Tutor> tutors = tutorSearchRepository.findAll(spec);

        return tutors.stream()
                .map(t -> extractProvince(t.getAddress()))
                .filter(loc -> loc != null && !loc.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private String extractProvince(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "Hà Nội";
        }
        String clean = address.trim();
        String[] parts = clean.split(",");
        if (parts.length >= 2) {
            String lastPart = parts[parts.length - 1].trim();
            if (lastPart.equalsIgnoreCase("Việt Nam") || lastPart.equalsIgnoreCase("Vietnam")) {
                return parts[parts.length - 2].trim();
            }
            return lastPart;
        }
        return clean;
    }

    @Override
    public TutorSummaryResponseDTO getTutorByCode(Long tutorCode) {
        Tutor tutor = tutorSearchRepository.findByTutorCode(tutorCode)
                .orElse(null);
        if (tutor == null) {
            return null;
        }
        return mapToSummaryDTO(tutor);
    }

    private TutorSummaryResponseDTO mapToSummaryDTO(Tutor tutor) {
        List<String> specialties = Collections.emptyList();
        if (tutor.getHeadline() != null && !tutor.getHeadline().trim().isEmpty()) {
            specialties = Arrays.stream(tutor.getHeadline().split("•|,"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<TutorCredentialDTO> credentials = Collections.emptyList();
        try {
            credentials = jdbcTemplate.query(
                "SELECT credential_id, credential_type, title, file_url, verified_status::text " +
                "FROM tutor_credentials WHERE tutor_id = ? AND verified_status = 'VERIFIED' ORDER BY uploaded_at ASC",
                (rs, rowNum) -> TutorCredentialDTO.builder()
                    .credentialId(rs.getObject("credential_id", UUID.class))
                    .credentialType(rs.getString("credential_type"))
                    .title(rs.getString("title"))
                    .fileUrl(rs.getString("file_url"))
                    .verifiedStatus(rs.getString("verified_status"))
                    .build(),
                tutor.getTutorId()
            );
        } catch (Exception e) {
            // ignore if table not present or error
        }

        List<TutorAvailabilityDTO> availabilities = Collections.emptyList();
        try {
            availabilities = jdbcTemplate.query(
                "SELECT availability_id, day_of_week, start_time::text, end_time::text " +
                "FROM tutor_availabilities WHERE tutor_id = ? AND is_active = TRUE ORDER BY day_of_week, start_time",
                (rs, rowNum) -> {
                    int dow = rs.getInt("day_of_week");
                    String dayName = switch (dow) {
                        case 1 -> "Chủ nhật";
                        case 2 -> "Thứ hai";
                        case 3 -> "Thứ ba";
                        case 4 -> "Thứ tư";
                        case 5 -> "Thứ năm";
                        case 6 -> "Thứ sáu";
                        case 7 -> "Thứ bảy";
                        default -> "Thứ " + dow;
                    };
                    String start = rs.getString("start_time");
                    String end = rs.getString("end_time");
                    if (start != null && start.length() >= 5) start = start.substring(0, 5);
                    if (end != null && end.length() >= 5) end = end.substring(0, 5);
                    return TutorAvailabilityDTO.builder()
                        .availabilityId(rs.getObject("availability_id", UUID.class))
                        .dayOfWeek(dow)
                        .dayName(dayName)
                        .startTime(start)
                        .endTime(end)
                        .build();
                },
                tutor.getTutorId()
            );
        } catch (Exception e) {
            // ignore
        }

        return TutorSummaryResponseDTO.builder()
                .tutorId(tutor.getTutorId())
                .tutorCode(tutor.getTutorCode())
                .fullName(tutor.getUser() != null ? tutor.getUser().getFullName() : "Gia sư Kizuna")
                .avatarUrl(tutor.getUser() != null ? tutor.getUser().getAvatarUrl() : null)
                .headline(tutor.getHeadline())
                .bio(tutor.getBio())
                .highestJlptLevel(tutor.getHighestJlptLevel() != null ? tutor.getHighestJlptLevel() : "N1")
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .avgRating(tutor.getAvgRating())
                .totalReviews(tutor.getTotalReviews())
                .location(tutor.getAddress() != null ? tutor.getAddress() : "Hà Nội, Việt Nam")
                .isFeatured(Boolean.TRUE.equals(tutor.getIsFeatured()))
                .isVerified(tutor.getVerificationStatus() == TutorVerificationStatus.APPROVED)
                .specialties(specialties)
                .teachingFormat("Online 1-1")
                .credentials(credentials)
                .availabilities(availabilities)
                .build();
    }
}