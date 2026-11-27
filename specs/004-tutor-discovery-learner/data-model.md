# Data Model: Filter Tìm kiếm, Đồng bộ URL & Định tuyến Gia sư

**Feature**: FE-04 — Tutor Discovery & Search (Learner)  
**Date**: 2026-11-01  
**Status**: Completed  

---

## 1. Frontend Filter State Model (`state`)

Đối tượng trạng thái điều khiển trung tâm trong `frontend/pages/tutor-discovery/tutor-discovery.js`:

```typescript
interface TutorFilterState {
  keyword: string;       // Từ khóa gõ vào ô tìm kiếm (tên, kỹ năng, chuyên môn)
  jlptLevel: string;     // 'all' | 'N1' | 'N2' | 'N3' | 'N4' | 'N5'
  goal: string;          // '' | 'kaiwa' | 'jlpt' | 'business' | 'it'
  format: string;        // '' | 'online' | 'offline' | 'hybrid'
  priceRange: string;    // '' | '150-250' | '250-400' | '400-600'
  schedule: string;      // '' | 'today' | 'weekend' | 'evening'
  sortBy: string;        // 'rating' | 'price_asc' | 'price_desc' | 'exp'
  currentPage: number;   // Số nguyên >= 1 (Mặc định: 1)
  pageSize: number;      // Số bản ghi / trang (Mặc định: 10)
}
```

---

## 2. Ánh xạ giữa URL Query Parameters và State

Bảng chuyển đổi hai chiều giữa URL và Bộ nhớ trạng thái:

| Khóa trong State | Tham số URL | Kiểu dữ liệu | Giá trị mặc định | Quy tắc làm sạch URL |
| :--- | :--- | :--- | :--- | :--- |
| `keyword` | `q` | string | `""` | Xóa nếu rỗng hoặc chỉ có khoảng trắng |
| `jlptLevel` | `jlpt` | string | `"all"` | Xóa nếu là `"all"` hoặc rỗng |
| `goal` | `goal` | string | `""` | Xóa nếu rỗng |
| `format` | `format` | string | `""` | Xóa nếu rỗng |
| `priceRange` | `price` | string | `""` | Xóa nếu rỗng |
| `schedule` | `schedule` | string | `""` | Xóa nếu rỗng |
| `sortBy` | `sort` | string | `"rating"` | Xóa nếu là `"rating"` |
| `currentPage` | `page` | number | `1` | Xóa nếu là `1` hoặc `<= 0` |

---

## 3. Backend Request & Response DTOs

### 3.1. DTO Tiêu chí tìm kiếm (`TutorSearchCriteriaDTO`)
Đã triển khai trong `com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO`:
```java
public class TutorSearchCriteriaDTO {
    private String keyword;
    private String jlptLevel;
    private String goal;
    private String format;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String schedule;
    private String sortBy; // "rating", "price_asc", "price_desc", "exp"
    private int page;      // 0-indexed (trên UI là 1-indexed)
    private int size;      // 10
}
```

### 3.2. DTO Tóm tắt Gia sư (`TutorSummaryResponseDTO`)
Đã triển khai trong `com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO`:
```java
public class TutorSummaryResponseDTO {
    private UUID tutorId;
    private Long tutorCode; // Mã tự tăng (10001, 10002, ...)
    private String fullName;
    private String avatarUrl;
    private String headline;
    private String highestJlptLevel;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private Double avgRating;
    private Integer totalReviews;
    private String teachingFormat;
    private String location;
    private List<String> specialties;
    private Boolean isVerified;
    private String bio;
}
```

### 3.3. DTO Phân trang kết quả (`TutorSearchPageResponse`)
```java
public class TutorSearchPageResponse {
    private List<TutorSummaryResponseDTO> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
```
