package com.kizuna.features.profile.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Trang dữ liệu theo hợp đồng FE-02 (contracts/profile-api.md mục 4.1):
 * { content, page, size, totalElements, totalPages, last }.
 * Khác core.dto.PageResponse (trường items, không có last) nên tách riêng trong feature profile.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
