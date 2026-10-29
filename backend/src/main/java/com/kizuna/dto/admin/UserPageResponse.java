package com.kizuna.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Phản hồi phân trang danh sách người dùng kèm bộ chỉ số KPI tổng hợp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPageResponse {

    private List<UserDTO> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private Map<String, Long> kpis;
}
