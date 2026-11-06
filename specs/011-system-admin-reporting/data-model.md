# Data Model: FE-11

## Mô hình hiện hành — đồng bộ 2026-11-05

Thiết kế cần đối chiếu schema/code trước migration; không khẳng định các bảng sau đã tồn tại. FR-023–029 trong spec.md ưu tiên hơn mô hình cũ bên dưới. Cấu hình học thử/snapshot và health giữ để truy vết/phụ thuộc, không mở lại trên giao diện.

- **Thông tin nền tảng:** một nhóm cấu hình định danh UUID, tên hiển thị tối đa100, email hỗ trợ tối đa254, số hỗ trợ tối đa20; version tăng khi sửa, updated_at và changed_by. Không seed liên hệ giả; thiếu dữ liệu biểu diễn chưa cấu hình. Nếu schema hiện hữu hỗ trợ nhóm/version thì tái sử dụng, không tạo nguồn thứ hai. Định dạng theo FR-023.
- **Giới hạn đính kèm hỗ trợ:** số nguyên 1–10 MB, quy đổi maxBytes bằng 1.048.576 byte/MB; version và thời điểm/tác giả. Cùng nguồn cho upload validator và UI. Không tác động tệp cũ. Chưa thêm cấu hình ảnh đại diện/tài liệu hồ sơ.
- **Lịch sử cấu hình:** UUID, nhóm, trước/sau chỉ gồm trường được phép, actor FK users, occurred_at, reason 1–2.000 ký tự; append-only, ghi cùng transaction với cấu hình và version; không chứa bí mật.
- **Sự cố:** bỏ write assignment, giữ event cũ. CLAIM kiểm chưa có assignee; RELEASE xóa assignee, đưa NEW, lưu trước/sau/lý do/tác giả. Chỉ người phụ trách hoặc Admin khi người phụ trách không còn hoạt động; CLOSED không nhận RELEASE. Khóa/version bảo đảm chỉ một người nhận thành công.
- **Email cấp tài khoản:** hàng đợi DB có ID, user ID, trạng thái chờ/đã gửi/thất bại, số lần thử, thời điểm thử/gửi và lỗi đã lọc; không lưu mật khẩu rõ hoặc payload thư chứa mật khẩu. Sinh mật khẩu lúc gửi theo thiết kế hiện có; phải kiểm retry/crash và đồng bộ hash với thư, không coi SMTP là transaction DB. Tạo Manager lưu căn cứ bổ nhiệm; must_change_password dùng chung với xác thực.
- **Hoãn:** không thêm mô hình banner bảo trì đến khi có code mới; không thêm bảng quản lý gói hoặc hoàn tiền trong FE-11.

## Mô hình lịch sử trước đồng bộ

Ngày 2026-10-30. UUID cho bảng mới, TIMESTAMPTZ/OffsetDateTime cho timestamp; thời gian tính kỳ theo Asia/Ho_Chi_Minh. Mapping explicit snake_case. Dữ liệu cấu hình nguồn không phải bằng chứng DB live.

## SystemConfiguration (hiện hữu)

config_key varchar(100) PK String; config_value text not null; data_type varchar(20) not null; description varchar(255) nullable; updated_at TIMESTAMPTZ. Không kế thừa BaseEntity yêu cầu created_at khi schema không có cột đó.

FE-11 chỉ ghi TRIAL_DEFAULT_DURATION_MINUTES và BOOKING_MIN_HOURS_IN_ADVANCE. Giá trị lần lượt số nguyên 15..120 và 1..168; không nhận decimal/blank/non-number, không làm tròn. Nhãn tiếng Việt ánh xạ trong DTO, không đưa khóa thô ra UI. Các khóa AI/email khác không được FE-11 chỉnh. Thời hạn giữ chỗ 15 phút là quy tắc cố định, không thêm khóa editable.

## BookingPolicyRevision (mới)

- revision_id UUID PK; revision_number bigint unique tăng đơn điệu; duration_minutes int CHECK 15..120; min_advance_hours int CHECK 1..168; effective_at TIMESTAMPTZ; changed_by UUID users nullable chỉ baseline; change_reason 1..2.000 ký tự không trắng cho mutation.
- Baseline lấy đúng giá trị hiện hữu nếu hợp lệ; provenance BASELINE, không bịa tác giả hoặc thời điểm thay đổi quá khứ. effective_at là thời điểm nhập baseline, không giả sử là ngày áp dụng đầu tiên của dữ liệu cũ.
- BookingPolicyState có singleton UUID cố định cho bản triển khai, current_revision_id FK, version bigint @Version. UUID cố định là định danh kỹ thuật, không mock nghiệp vụ. Khóa hàng này khi đọc cấu hình để tạo booking và khi sửa config để tuyến tính hóa thứ tự commit.
- Revision immutable; cập nhật hai giá trị legacy + current revision + audit cùng transaction. Không snapshot mỗi trường ở thời điểm khác nhau.

## Booking snapshot (bổ sung trial_bookings)

policy_revision_id FK nullable cho booking legacy; policy_duration_minutes int nullable legacy; policy_min_advance_hours int nullable legacy. Booking mới qua evaluator bắt buộc có đủ ba trường và time bounds hợp lệ. scheduled_end_time lấy start + snapshot duration. Validation start >= serverNow + snapshot advanceHours; equality tại biên được nhận.

Booking legacy không tự nhận config hiện tại hoặc bị tính lại. Không dựng lịch sử snapshot giả. Consumer thật tạo booking và snapshot trong cùng transaction; nếu workflow chưa có thì chỉ thiết kế/model không đủ đạt FR-004.

## OperationsComponent và HealthObservation

Component code allowlist internal, nhãn Việt, observation kind, enabled, verified probe available. Danh mục từ deployment config được quản lý, không cho client nhập URL (tránh probe thành công cụ truy cập tùy ý).

Observation immutable snapshot: componentCode, status HEALTHY/DEGRADED/UNAVAILABLE/UNKNOWN, observedAt nullable khi chưa quan sát, responseTimeMs nullable nếu không có phép đo hợp lệ, reasonLabel tiếng Việt, observationScope. DEGRADED chỉ từ nguồn có kết luận rõ, không tự sáng tác ngưỡng. staleness tính riêng observedAt age >60 giây, không đổi timestamp khi đọc cache. Heartbeat ứng dụng không đo uptime Internet; không gọi nó là toàn hệ thống luôn hoạt động.

Collector mỗi 30 giây, timeout probe 2 giây/budget 4 giây; không chạy chồng; snapshot thread-safe in-memory, không bắt buộc lưu telemetry DB. Collector vẫn giữ snapshot khi DB lỗi; API chỉ trả dữ liệu sau khi xác minh quyền hiện tại. Nếu identity DB không khả dụng, API trả lỗi hữu hạn và giữ fail-closed. History báo cáo là lịch sử incident, không giả định có chuỗi telemetry dài hạn.

## TechnicalIncident và TechnicalIncidentEvent

Incident: incident_id UUID PK; subject 1..200 ký tự không trắng; description 1..10.000 không trắng; severity LOW/NORMAL/HIGH/CRITICAL; status NEW/IN_PROGRESS/RESOLVED/CLOSED; reporter_id users bắt buộc; assignee_id nullable trước claim; created_at/updated_at; version bigint @Version; recurrence_of UUID self-FK nullable. Thành phần ảnh hưởng một hoặc nhiều từ allowlist qua incident_components; mức độ/thành phần bất biến sau tạo trong scope bản này.

Event: event_id UUID PK; incident_id FK; sequence_no bigint tăng dưới khóa incident, UNIQUE(incident_id,sequence_no); actor_id FK; occurred_at; action CREATE/CLAIM/REASSIGN/PROGRESS/RESOLVE/RESUME/CLOSE; previous_state/new_state snapshot trạng thái, assignee, severity và affected components; detail 1..10.000 ký tự nếu diễn biến/kết quả, reason 1..2.000 nếu reassign/resume; verification_text 1..10.000 cho resolve. Mọi event chỉ append, không sửa/xóa endpoint; không secret/log thô.

State machine:

| From | Action và quyền | To |
| --- | --- | --- |
| NEW | admin claim khi chưa assignee | IN_PROGRESS |
| IN_PROGRESS | assignee progress | IN_PROGRESS |
| IN_PROGRESS | assignee resolve, có result + verification | RESOLVED |
| RESOLVED | assignee resume có lý do | IN_PROGRESS |
| RESOLVED | assignee xác nhận close | CLOSED |

Reassign: mọi admin active được chuyển incident chưa CLOSED có người phụ trách sang admin khác active, lý do bắt buộc, không đổi trạng thái. CLOSED cấm event mới. Recurrence tạo incident mới liên kết hồ sơ cũ đã đóng; không reopen hồ sơ đóng. Claim cạnh tranh/version stale → conflict, không ghi đè. Chi tiết/lịch sử chỉ admin active.

## ConfigurationAuditEvent

event_id UUID PK; revision_id FK; actor_id FK; occurred_at; before_revision_id FK; after_revision_id FK; reason; sequence hoặc revision_number để thứ tự chắc chắn. Baseline import có provenance riêng, không giả mạo hành động admin. Revision/audit/state/config update nguyên tử.

## PolicyDocumentVersion

document_version_id UUID PK; document_type (terms, usage policy, other regulation từ danh mục được duyệt); title 1..200 không trắng; version_label 1..100 không trắng; approved_content text không trắng; publication_state DRAFT/PUBLISHED/WITHDRAWN; effective_from TIMESTAMPTZ; effective_to nullable exclusive >effective_from; source_reference và approved_at để truy nguồn. UNIQUE(document_type,version_label).

Đọc khi PUBLISHED và effective_from <= now và (effective_to null hoặc now < effective_to). Mỗi loại tối đa một bản hiệu lực: import kiểm overlap trong transaction, query fail closed nếu dữ liệu legacy vi phạm. Không editor/publish API. Nếu không có approved corpus: empty, không seed placeholder. Ngày hiển thị múi giờ dự án; text escaped, không HTML thực thi.

## Reporting projection

- startDate/endDate gồm cả hai ngày, tối đa 366 ngày tính inclusive; endDate <= hôm nay. cutoff = min(đầu ngày kế tiếp endDate, reportNow), đầu kỳ = đầu ngày startDate.
- Created count: created_at thuộc [start,cutoff). Resolved count: COUNT DISTINCT incident có RESOLVE event trong [start,cutoff).
- Open at cutoff: tạo trước cutoff, event cuối trước cutoff có state != CLOSED; gồm tồn trước start. RESOLVED chưa CLOSED vẫn tính còn mở theo spec.
- Severity/status breakdown cho từng tập chỉ số, lấy trạng thái tại cutoff từ event cuối và severity snapshot; không gộp ba tập thành cùng mẫu số. Một incident có thể thuộc cả created/resolved/open nên không cộng ba chỉ số để suy ra tổng.
- Khi query thất bại: response lỗi/không có dữ liệu, không coerced zero. Một lần báo cáo đọc nhất quán với cutoff và snapshot transaction; history đã đóng vẫn được giữ.

## Migration, indexes and concurrency

SQL tăng dần có version table/lock; preflight schema và giá trị config, dừng khi invalid, không ghi 45/12 để che lỗi. Không chạy script nguồn có DROP CASCADE. Không sửa FE-10 migrations đã dùng.

Index incident(status,severity,created_at,id), assignee/status, event(incident_id,sequence_no), event(action,occurred_at,incident_id), policy(type,state,effective_from), unique revision_number/current singleton. Kiểm EXPLAIN để thêm/bớt theo đo tải, không index binary (không có attachments trong FE-11).

Mọi write nhận expectedVersion; khóa incident/state, reload người thao tác/người nhận và kiểm role ACTIVE/deletedAt; commit mutation + event nguyên tử. Account changes cạnh tranh phải kiểm lại người nhận trước commit và dùng lock nhất quán khi cần. Lỗi audit rollback toàn bộ, event sequence bảo đảm thứ tự ngay cả timestamp bằng nhau.
