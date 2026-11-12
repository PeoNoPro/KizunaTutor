# Implementation Plan: FE-13 — Kho học liệu cho cán bộ học vụ

**Branch**: `han` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/013-academic-content-import/spec.md`

**Ghi chú quy trình**: Mã nguồn của feature được viết **trước** khi có spec/plan (sai thứ tự Spec Kit). Plan này mô tả thiết kế mục tiêu, đối chiếu mã hiện có và liệt kê phần phải sửa cho khớp luật (Phase R trong [tasks.md](tasks.md)) mà **không đổi chức năng** người phụ trách đã nghiệm thu và **không sửa mã của thành viên khác**.

## Summary

Cán bộ học vụ xem kho Kanji / từ vựng / ngữ pháp, nhập mục mới từ tệp Excel giống ba tệp mẫu trong `/database` (kiểm tra trước, chỉ thêm mục mới, mục đã có giữ nguyên, báo dòng trùng và dòng lỗi), và xem lịch sử các lần nhập. Backend: phân hệ riêng `com.kizuna.features.learning_content`, đọc Excel bằng Apache POI (đã có trong `pom.xml`), ghi vào các bảng học liệu đã có bằng `JdbcTemplate` trong một giao dịch có khóa theo loại; lịch sử lưu ở bảng có sẵn `system_audit_logs`. Frontend: trang `/academic/content-import` (3 tab) trong khung cán bộ học vụ. Không tạo bảng, không migration.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.4); JavaScript ES6+ module.

**Primary Dependencies**: Spring Web, Spring Security (JWT có sẵn), Spring JDBC (`JdbcTemplate`, `TransactionTemplate`), Jackson, Apache POI 5.2.5 (`poi-ooxml`, có sẵn); Tailwind CDN, `components/academic-layout`, `components/pagination`. Không thêm thư viện.

**Storage**: PostgreSQL Supabase dùng chung. Bảng đã có: `kanji_dictionary`, `kanjis`, `vocabularies`, `vocabulary_kanjis`, `grammars`, `system_audit_logs`, `users`. Không tạo bảng, không đổi schema (xem [data-model.md](data-model.md)).

**Testing**: JUnit 5 + AssertJ (không chạm DB) cho đọc tệp và lập kế hoạch nhập (tệp Excel tạo trong bộ nhớ ngay trong test); `node --test` cho hàm thuần frontend. Kiểm chứng SQL trên DB thật chỉ bằng giao dịch rollback (A-5 của spec).

**Target Platform**: Backend cổng 8085, frontend Node cổng 3000; trình duyệt desktop.

**Project Type**: Web application (backend + frontend tách riêng).

**Performance Goals**: Kiểm tra tệp từ vựng đầy đủ (~15.600 dòng, 6 sheet) dưới 30 giây (SC-003; đo thực tế ~3 giây đọc + so khớp).

**Constraints**: Tệp ≤ 15 MB, ≤ 50.000 dòng; ghi theo lô 500 dòng; lịch sử lưu tối đa 2.000 mục đã thêm mỗi lần; không giữ kết nối DB trong lúc đọc tệp.

**Scale/Scope**: 3 loại học liệu, ~15.000 mục hiện có; 5 endpoint; 1 trang.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Ghi chú |
| --- | --- | --- |
| I. Tiếng Việt, không lộ mã kỹ thuật, không ALL-CAPS | Đạt | Thông báo lỗi tiếng Việt từ `BusinessException`; nhãn JLPT là ngoại lệ được phép. |
| II.1–3 Dữ liệu thật, trạng thái rỗng | Đạt | Danh sách, lịch sử lấy từ API; có trạng thái rỗng và lỗi + thử lại. |
| II.4 Skeleton khi tải | **Chưa đạt → sửa (T030)** | Hiện chỉ hiện chữ "Đang tải...". |
| II.5 Kanji chỉ từ KanjiVG | **Lệch có lý do** | Người phụ trách quyết định giữ nhập Kanji mới (spec, Clarifications). Xem Complexity Tracking. |
| II.5 Fixture test chỉ trong `src/test/`, `frontend/tests/` | **Chưa đạt → sửa (T031)** | Tệp thử đang ở `database/test-import/`. |
| III.2 Không gọi Repository của feature khác | Đạt về mã; **lệch về dữ liệu** | Không import lớp nào của `learning_support` / `admin`; nhưng đọc/thêm trực tiếp vào bảng của họ. Xem Complexity Tracking. |
| III.3 `core/` không chứa nghiệp vụ | Đạt; **sửa `core/` chưa báo trước** | Thêm 1 matcher trong `SecurityConfig`, 1 route trong `WebMvcConfig` (không nghiệp vụ). Ghi lại để báo nhóm. |
| III.4 / BACKEND_RULES 2–4 cấu trúc, đặt tên | **Chưa đạt → sửa (T020–T024)** | Thiếu interface `*Service` + `*ServiceImpl`; DTO chưa theo `*Response`; phản hồi chưa bọc `ApiResponse`. |
| IV. Giao diện | Đạt | Dùng `academic-layout`, `pagination`, token màu khung nhân viên; không inline style/handler. |
| V. Ranh giới, test, Git | Đạt | Không thanh toán / AI; có test; không commit / push. |

**Re-check sau Phase 1**: các mục "Chưa đạt" có task sửa trong Phase R; các mục "Lệch có lý do" được ghi trong Complexity Tracking và cần nhóm trưởng xác nhận. Không còn lệch không có lý do → được qua cổng.

## Project Structure

### Documentation (this feature)

```text
specs/013-academic-content-import/
├── spec.md
├── plan.md              # File này
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── content-import-api.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/kizuna/features/learning_content/
├── controller/ContentImportController.java          # 5 endpoint, bọc ApiResponse
├── dto/                                               # *Response (T021)
├── repository/
│   ├── LearningContentStore.java                     # đọc khóa + thêm mục (JdbcTemplate)
│   └── ContentBrowseStore.java                       # danh sách + lịch sử (JdbcTemplate)
└── service/
    ├── LearningContentService.java                   # interface (T020)
    ├── impl/LearningContentServiceImpl.java          # cài đặt (T020)
    ├── ContentType.java                              # 3 loại, cột, khóa trùng
    ├── ContentWorkbookReader.java                    # đọc + kiểm tra dòng
    └── ContentImportPlanner.java                     # quyết định thêm / đã có / trùng / lỗi
backend/src/test/java/com/kizuna/features/learning_content/
├── ContentImportTest.java                            # reader + planner

frontend/pages/academic-content-import/{index.html, academic-content-import.js}
frontend/js/content-import-helpers.js
frontend/tests/content-import-helpers.test.mjs
```

Sửa thêm (chỉ thêm dòng, không đổi dòng cũ): `core/security/SecurityConfig.java` (matcher `/api/academic/content-imports/**`), `core/config/WebMvcConfig.java` (route trang), `frontend/server.js` (route trang), `frontend/js/api.js` (nhóm `academicContent`), `frontend/components/academic-layout/academic-layout.js` (mục sidebar "Kho học liệu").

**Structure Decision**: Phân hệ riêng `learning_content` của cán bộ học vụ (người phụ trách chọn, spec Clarifications) để không chạm mã `learning_support` (FE-08) của thành viên khác. Tầng repository dùng `JdbcTemplate` (lý do ở research.md §2).

## Complexity Tracking

> Các điểm lệch constitution / luật dự án được giữ có chủ đích — **cần nhóm trưởng xác nhận**.

| Violation | Why Needed | Simpler Alternative Rejected Because |
| --- | --- | --- |
| Thêm Kanji mới từ Excel (constitution II.5, AGENTS luật 7) | Người phụ trách yêu cầu giữ chức năng nhập Kanji như đã nghiệm thu | Chặn Kanji mới làm mất chức năng đã nghiệm thu. Giảm rủi ro: Kanji mới bắt buộc "Số nét"; không ghi `svg_data`; chữ mới chưa luyện viết được (ghi trong quickstart). |
| Feature mới `learning_content` ngoài danh sách 12 feature (BACKEND_RULES §2) | Không được sửa mã `learning_support` của thành viên khác | Đặt trong `learning_support` = sửa phần của người khác; đặt trong `verification` = trộn nghiệp vụ không liên quan. |
| Đọc / thêm trực tiếp vào bảng học liệu và `system_audit_logs` (Nguyên tắc III — tinh thần cô lập) | `learning_support` chưa có service công khai cho từ vựng, ngữ pháp, thêm Kanji; `admin` chỉ có entity rỗng `SystemAuditLog`, không có service | Thêm service vào feature của người khác = sửa mã của họ. Mã FE-13 không import lớp nào của hai feature đó; chỉ INSERT (không UPDATE/DELETE) nên không làm đổi dữ liệu họ đang dùng. |
| Repository dùng `JdbcTemplate` thay vì Spring Data JPA (BACKEND_RULES §3.3) | Không có entity cho `vocabularies`, `grammars`, `kanjis`; entity `KanjiDictionary` thuộc `learning_support` | Tạo entity trùng bảng của feature khác dễ lệch mapping; nhập lô 8.000 dòng qua JPA chậm hơn `batchUpdate`. |
| Sửa `core/` (`SecurityConfig`, `WebMvcConfig`) khi chưa hỏi nhóm (AGENTS §7) | Phân quyền endpoint và route trang phải khai báo ở đây (chỉ một `SecurityConfig`) | Không có chỗ khác hợp lệ. Chỉ thêm dòng; cần báo nhóm. |

## Ghi chú triển khai

- 2026-11-10: Mã chạy được và đã kiểm chứng trên DB thật bằng giao dịch rollback (3 loại: thêm mới, giữ nguyên mục đã có, liên kết Kanji cho từ mới, ghi / đọc lịch sử). Lần đầu thử trên giao diện bị lỗi do Spring DevTools nạp lớp biên dịch dở dang khi IDE tự build — không phải lỗi mã; cách tránh: dừng backend, `mvnw compile` sạch rồi chạy lại.
- Thay đổi cấu trúc phản hồi (bọc `ApiResponse`, T022) phải sửa đồng thời `frontend/js/api.js` (T023).
- 2026-11-10 (Phase R): tách `LearningContentService` / `LearningContentServiceImpl`; DTO `*Response` mỗi lớp một file; repository đổi tên `LearningContentRepository`, `ContentImportHistoryRepository`; phản hồi bọc `ApiResponse`, trang dùng `core/dto/PageResponse`; skeleton khi tải; bộ tệp thử chuyển sang `backend/src/test/resources/content-import/` + `ContentImportFixturesTest`. Chức năng giữ nguyên. Chưa chạy lại kiểm chứng DB do mất mạng (tasks T033).
- 2026-11-12: theo yêu cầu người phụ trách, xóa bộ tệp thử `backend/src/test/resources/content-import/` và `ContentImportFixturesTest` (T031, T032). Test tự động còn `ContentImportTest` (tự tạo tệp Excel trong bộ nhớ). Câu lệnh dọn dữ liệu thử chép vào quickstart.md.
- 2026-11-10: script sinh tệp thử (ngoài repo) từng mở 3 tệp gốc `database/*.xlsx` ở chế độ đọc-ghi của POI làm Git báo đổi (nội dung không đổi); đã khôi phục bằng `git checkout`. Mọi chỗ đọc Excel dùng `InputStream` (chỉ đọc).
- 2026-11-12 (review PR): sửa liên kết từ vựng – Kanji ghép sai tổ hợp (từ, cách đọc) — T034.
