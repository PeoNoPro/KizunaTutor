package com.kizuna.core.dto;
import java.util.List;
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, long totalPages) {
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
        return new PageResponse<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());
    }
}
