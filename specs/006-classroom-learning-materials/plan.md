# Implementation Plan: FE-06 — Classroom & Learning Materials Management

**Branch**: `linhtk` | **Date**: 2026-11-03 | **Spec**: [spec.md](spec.md)

## Summary

Bổ sung API lớp học và tài liệu học tập trong `com.kizuna.features.classroom`, lưu tệp trên Supabase Storage (bucket riêng tư, signed URL), thêm một cột `description` cho `learning_materials`, và hai trang frontend: `/tutor/classrooms` (gia sư) và `/pages/my-courses/` (học viên, trong learner shell).

## Technical Context

- **Language/Version**: Java 17, Spring Boot 3.3.4; JavaScript ES modules (frontend tĩnh, Tailwind CDN).
- **Primary Dependencies**: Spring Web (RestClient), Spring Data JPA, Lombok, `JwtTokenProvider` sẵn có. **Không thêm dependency.**
- **Storage**: PostgreSQL (Supabase) + Supabase Storage.
- **Testing**: JUnit 5 + Mockito (`spring-boot-starter-test` sẵn có) — `.\mvnw.cmd test -Dtest=Classroom*`; `node --test "frontend/tests/*.test.js"`.
- **Constraints**: `ddl-auto=none`; tệp ≤ 10 MB; không sửa `core/`, `pom.xml`, `SecurityConfig`, code feature khác.

## Constitution Check

| Nguyên tắc | Đánh giá |
|---|---|
| I. Tiếng Việt, không lộ kỹ thuật | Mọi message backend/UI tiếng Việt; UI ánh xạ status → nhãn; không hiển thị mã HTTP. ✅ |
| II. Dữ liệu thật | Chỉ đọc từ API; trạng thái rỗng/lỗi; không seed. ✅ |
| III. Kiến trúc hiện có | Package-by-feature, controller/service/repository/dto; UUID; schema thay đổi tối thiểu có migration + đồng bộ `DatabasePgAdmin4.txt`. ✅ (thay đổi schema đã được duyệt) |
| IV. Thiết kế phân hệ | DESIGN.md §7: bo góc 5px, viền đen 1px, bóng khối cứng; tím trầm cho gia sư; một hành động chính mỗi khối. ✅ |
| V. Phạm vi & kiểm chứng | Tasks truy về FR; unit test service/validator/resolver; test node cho helper UI. ✅ |

Ngoại lệ cần ghi: thêm cấu hình `application.properties` (được duyệt Q2); bổ sung (append) 3 file frontend dùng chung (được duyệt Q5).

## Project Structure

### Documentation

```
specs/006-classroom-learning-materials/
├── spec.md  plan.md  research.md  data-model.md  quickstart.md  tasks.md
├── contracts/classrooms.md
└── checklists/requirements.md
```

### Source — tạo mới

```
backend/src/main/java/com/kizuna/features/classroom/
├── controller/ClassroomController.java
├── dto/ClassroomSummaryResponse.java
├── dto/ClassroomCreateRequest.java
├── dto/EligibleLearnerResponse.java
├── dto/LearningMaterialResponse.java
├── dto/MaterialFileLinkResponse.java
├── exception/ClassroomException.java
├── exception/ClassroomExceptionHandler.java      (@RestControllerAdvice(assignableTypes = ClassroomController.class))
├── repository/LearningSpaceRepository.java       (+ native query chỉ đọc connection_requests)
├── repository/LearningMaterialRepository.java
├── service/ClassroomService.java
├── service/LearningMaterialService.java
├── service/MaterialStorageService.java           (interface)
├── service/impl/ClassroomServiceImpl.java
├── service/impl/LearningMaterialServiceImpl.java
├── service/impl/SupabaseMaterialStorageService.java
├── util/CurrentUserResolver.java  (+ CurrentActor record)
└── util/MaterialFileValidator.java

backend/src/test/java/com/kizuna/features/classroom/
├── ClassroomServiceImplTest.java
├── LearningMaterialServiceImplTest.java
├── MaterialFileValidatorTest.java
└── CurrentUserResolverTest.java

database/migrations/006_fe06_learning_materials_description.sql

frontend/pages/tutor-classrooms/{index.html, tutor-classrooms.js}
frontend/pages/my-courses/{index.html, my-courses.js}
frontend/js/classroom-helpers.js      (hàm thuần: nhãn trạng thái, định dạng ngày, kiểm tệp phía client)
frontend/tests/classroom-helpers.test.js
```

### Source — sửa

| File | Thay đổi | Lý do |
|---|---|---|
| `features/classroom/entity/LearningMaterial.java` | Viết lại mapping đúng bảng + `description` | Stub hiện ánh xạ sai, không dùng được |
| `backend/src/main/resources/application.properties` | Append mục 12 Supabase Storage + multipart 12MB | Q2 |
| `database/DatabasePgAdmin4.txt` | Thêm dòng `description TEXT,` vào `CREATE TABLE learning_materials` (không chạy file) | Đồng bộ nguồn schema |
| `frontend/js/api.js` | Append nhóm `API.classrooms` | Q5 |
| `frontend/components/learner-layout/learner-layout.js` | Append 1 mục menu "Lớp học của tôi" + `PAGE_META['my-courses']` | Q5 |
| `frontend/server.js` | Append route `/my-courses`, `/tutor/classrooms` | Q5 |
| `.gitkeep` trong `controller/`, `dto/`, `repository/`, `service/`, `service/impl/` của classroom | Xóa khi đã có file | Dọn dẹp |

Không sửa: `LearningSpace.java`, `KanjiFolder*.java` (cùng package nhưng thuộc người khác), `core/`, `pom.xml`, `SecurityConfig`, `tutor_search`, `progress`.

## Design Notes

1. **Phân quyền**: mọi method service nhận `CurrentActor(userId, role, tutorId?, learnerId?)`. `loadAccessibleSpace(spaceId, actor)`: không tồn tại → 404; không phải tutor/learner của space → 403.
2. **Giao dịch & tệp mồ côi**: upload tệp lên Storage trước, rồi `save` trong `@Transactional`; nếu save lỗi → `storage.delete(path)` trong catch rồi ném lại. Update thay tệp: upload mới → save → sau commit xóa tệp cũ (lỗi xóa tệp cũ chỉ log cảnh báo, không fail request).
3. **Multipart**: `max-file-size=12MB` (lớn hơn 10MB) để yêu cầu 10–12MB tới được service và nhận thông báo 413 tiếng Việt; > 12MB rơi vào handler chung (thông báo chung) — frontend chặn trước > 10MB nên người dùng bình thường không gặp.
4. **Exception**: `ClassroomException(HttpStatus, message)` xử lý bởi advice giới hạn `ClassroomController` → không ảnh hưởng feature khác. Body `{success:false, message, data, timestamp}`.
5. **Frontend auth**: gửi `Authorization: Bearer ${localStorage.kizuna_token}`; 401 → chuyển `/login`.
6. **Trang gia sư**: trang độc lập (chưa có tutor shell, không tạo shell vì ngoài FE-06), header đơn giản có logo + tên trang + nút đăng xuất về `/login`.

## Complexity Tracking

| Bổ sung | Vì sao cần | Phương án đơn giản hơn bị loại vì |
|---|---|---|
| `SupabaseMaterialStorageService` + interface | Q2 chọn Supabase Storage; interface để mock trong test | Gọi thẳng trong service → không test được |
| Advice riêng classroom | Cần mã 403/409/413/503 tiếng Việt | `GlobalExceptionHandler` thuộc `core/` — không sửa |
