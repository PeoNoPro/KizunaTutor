# Quickstart Validation: FE-11

Hướng dẫn dùng sau khi code được triển khai; không chứng minh các endpoint/test đã tồn tại hoặc đã chạy ở bước plan.

## Prerequisites

- Java17/Maven Wrapper, PostgreSQL16 và DB kiểm thử riêng; cấu hình auth hiện có.
- Hai admin active, learner/tutor, manager và tài khoản bị khóa/xóa trong DB test; không dùng user/credential vận hành.
- Profile `operations-test` dự kiến lấy OPERATIONS_TEST_DB_URL/USERNAME/PASSWORD từ môi trường, fail nếu thiếu hoặc DB không được đánh dấu kiểm thử; không đưa password vào lệnh chia sẻ.
- Migration tăng dần FE-11, source policy được duyệt hoặc fixture tài liệu cách ly; không seed nội dung giả vào vận hành.
- Để hoàn tất US1 cần điểm tạo booking thật gọi evaluator và lưu snapshot. Stub controller/service hiện tại chưa đủ; nếu chưa có workflow, ghi blocker và chỉ báo unit/integration subset đã chạy.

## Commands after implementation

Từ repo trong PowerShell:

```powershell
java -version
.\mvnw.cmd -version
docker compose up -d kizuna-postgres
```

Compose mặc định chỉ dựng DB phát triển, không tự tạo DB test cách ly. Chuẩn bị DB test theo hướng dẫn profile được triển khai; chạy migration theo README thực tế `src/main/resources/db/changes/README-fe11.md` với psql ON_ERROR_STOP. Không chạy `DatabasePgAdmin4.txt` chứa DROP CASCADE trên dữ liệu hiện hữu.

Sau khi các test được tạo và biến môi trường được thiết lập:

```powershell
.\mvnw.cmd "-Dspring.profiles.active=operations-test" "-Dtest=Operations*Test" test
.\mvnw.cmd "-Dtest=Operations*Test" test
```

Ứng dụng phát triển dùng cấu hình hiện hữu, không khởi động trỏ DB vận hành để nghiệm thu:

```powershell
.\mvnw.cmd spring-boot:run
```

Không dùng gửi email hoặc đọc/ghi learner memory làm probe. Optional agent-memory health chỉ bật khi kiểm chứng được hợp đồng read-only thật; thiếu hợp đồng phải UNKNOWN.

## Scenario validation

1. **Cấu hình**: Xem giá trị live/units/history; duration15 và120, advance1 và168 hợp lệ; 14/121,0/169,decimal/blank sai. Hold15 chỉ đọc và request giả không sửa được. Thiếu reason hoặc stale version bị từ chối. Đổi cả hai và mô phỏng audit write fail → cả revision/config rollback.
2. **Booking consumer**: Tạo booking trước thay config; đổi config, tạo booking sau và đối chiếu duration/endAt/advance/revision. Equality tại advance boundary hợp lệ, trước1giây không hợp lệ. Booking cũ không bị sửa/tính lại. Race config save/booking create có snapshot nhất quán theo commit, không lấy hai tham số từ hai revision. Nếu chưa có actual booking flow, không ghi SC-001 đạt toàn bộ.
3. **Quan sát**: App process heartbeat ghi đúng scope, DB probe thực. Ngắt DB test: collector vẫn giữ snapshot lỗi DB trong budget; API phải xác minh quyền hiện tại, nên identity DB mất kết nối thì trả lỗi hữu hạn, không bỏ kiểm quyền; nguồn chưa có → UNKNOWN. Collector không chồng lượt; timeouts2giây mỗi probe/tổng4giây. Trang poll30giây, observedAt không đổi khi đọc cache; >60giây stale. Không dùng `/api/test/health` hardcodeUP làm chứng cứ.
4. **Không auto incident**: Trước/sau ngắt DB hoặc dependency, số incident không tăng. Admin chủ động tạo một incident khi DB phục hồi hoặc sẵn để lưu; chỉ báo thành công khi lưu thật.
5. **Sự cố**: Tạo → claim → progress → resolve có result/verification → resume có reason → resolve → close. Kiểm log seq/actor/time, chỉ assignee ghi; admin2 được reassign có reason, admin1 mất quyền ghi sau chuyển. CLOSED cấm ghi, tái diễn tạo incident mới link cũ. Hai claim/update đồng thời chỉ một thắng; mutation/audit failure rollback.
6. **Báo cáo**: Fixture incident tạo trước kỳ, trong kỳ, nhiều RESOLVE, RESUME, CLOSED trước/sau cutoff. Tính độc lập created distinct,resolved distinct,open at cutoff. Kiểm RESOLVED chưa CLOSED vẫn open, phân nhóm từng chỉ số, không cộng ba tập làm total. Kỳ quá khứ dùng event as-of, kiểm 7 ngày default,366 inclusive, future/inverted reject và timezone đầu ngày/cuối ngày. DB unavailable không trả0giả.
7. **Chính sách**: Approved corpus hoặc fixture published effective, draft/future/withdrawn và overlap. Learner/tutor thấy đúng title/version/effective/content. Direct access không mở bản chưa hiệu lực; no corpus → empty, overlap → conflict. Không có editor/publish API. Nghiệm thu nội dung vận hành chờ chủ dự án cung cấp tài liệu duyệt; fixture không thay thế điều kiện này.
8. **Quyền/UI**: Admin-only operations; manager/CS/academic/learner/tutor/guest không đọc vận hành. Revoke role/lock user dù token cũ còn hạn vẫn chặn. Kiểm CSRF missing/wrong/correct. Không raw config key, URL, secret/log/exception; script text escaped, empty/error/loading phân biệt; keyboard/focus đúng, menu tới trang, learner/tutor theo DESIGN mục7.

## Load, acceptance and evidence

- Tạo script `src/test/load/operations.js` khi implement: 100 phiên/10phút,1.000 incident có events, credentials qua env; ghi error và p95 HTTP. Thêm lượt browser instrumentation từ click/navigation đến nội dung hiển thị để chứng minh SC-006≤2giây p95; không thay bằng HTTP latency.
- Lỗi nguồn có trạng thái lỗi/UNKNOWN trong≤5giây; thử khi DB pool nghiệp vụ cạn để chứng minh probe/health snapshot không treo theo pool đó. Khi process web chết, đánh dấu trang không phục vụ; không hứa external monitoring có sẵn.
- SC-007: 10 người thử learner/tutor tìm bản policy đúng≤2phút,≥9 người không cần trợ giúp; thiếu người thì ghi chưa chạy.
- Ghi FR-001–018/SC-001–007 vào validation.md sau triển khai: môi trường, schema version, lệnh, số ca đạt/lỗi/chưa chạy, browser timing, báo cáo arithmetic và blockers booking/content. Không claim chạy tests/migration trong bước plan.
- Kiểm migration giữ giá trị config và booking cũ; baseline không bịa provenance; restore thử backup DB test theo README, không down-migrate xóa lịch sử để giải quyết lỗi.

Tham chiếu: [data-model.md](data-model.md), [contracts/operations.md](contracts/operations.md), [research.md](research.md).

## Lệnh triển khai hiện tại

- Mã FE-11 đã được thêm vào monolith. Chạy migration sau preflight/backup theo `src/main/resources/db/changes/README-fe11.md`; không có auto-migration khi khởi động.
- Dùng cấu hình kết nối dự án được nhóm cung cấp. Nếu chưa cấu hình, đặt `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` trong terminal tại máy; không dùng DB kiểm thử cho vận hành, không gửi mật khẩu vào chat.
- Sau migration, chạy `.\mvnw.cmd spring-boot:run`, đăng nhập admin rồi mở `/admin/operations/booking-policy`, `/admin/operations/health`, `/admin/operations/incidents`, `/admin/operations/reports`. Learner/tutor mở `/learner/policies` hoặc `/tutor/policies`.
- Kiểm thử FE-11: `.\mvnw.cmd '-Dtest=Operations*Test' test`, sau khi đã đặt ba biến OPERATIONS_TEST_DB_* với DB riêng. Không chạy suite cũ mặc định trên DB chung: một số test cũ có thao tác xóa user.
- Hướng dẫn harness/trình duyệt/tải: `src/test/load/README-operations.md`. Trên máy hiện tại cache Maven được đặt riêng ở D bằng `-s '..\.operations-maven-settings.xml'`; đây là thiết lập của lượt kiểm thử, không phải yêu cầu phải đổi cấu hình Maven hệ thống.
- Booking evaluator yêu cầu caller đang ở transaction và lưu đủ snapshot cùng booking. Luồng tạo booking hiện vẫn stub; không có endpoint booking giả để thử thay thế. Trang chính sách hiện có empty state đến khi được cung cấp và nhập bản đã duyệt theo README-fe11-policy-import.md.
