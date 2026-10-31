package com.kizuna.features.tutor_search.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO;
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

    private TutorSummaryResponseDTO mapToSummaryDTO(Tutor tutor) {
        List<String> specialties = Collections.emptyList();
        if (tutor.getHeadline() != null && !tutor.getHeadline().trim().isEmpty()) {
            specialties = Arrays.stream(tutor.getHeadline().split("•|,"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return TutorSummaryResponseDTO.builder()
                .tutorId(tutor.getTutorId())
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
                .build();
    }
}

