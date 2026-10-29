# Data Model: FE-10

UUID cho mọi khóa; TIMESTAMPTZ/OffsetDateTime cho thời gian; enum native PostgreSQL hiện có ánh xạ rõ bằng PostgreSQLEnumJdbcType. Dùng snake_case tường minh, không tin mapping stub hiện tại.

## SupportTicket — support_tickets

| Field | Kiểu và ràng buộc |
| --- | --- |
| ticket_id | UUID PK |
| user_id | FK users, bắt buộc; requester bất biến |
| assigned_to | FK users, nullable trước khi nhận; người phụ trách CS hiện tại |
| escalated_to | FK users, nullable; quản lý có quyền hiện tại, giữ sau return, thay khi chuyển tiếp |
| is_escalated | Boolean legacy; true chỉ khi đang chờ quản lý |
| escalation_notes | Cột legacy nội bộ; không dùng làm nguồn lịch sử mới |
| category | ACCOUNT, BILLING, TUTOR, BOOKING, MEMBERSHIP, TECHNICAL, OTHER |
| subject | varchar(200), không trắng |
| priority | LOW, MEDIUM mặc định, HIGH, URGENT |
| status | OPEN mặc định, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED |
| related_booking_id | FK trial_bookings nullable, kiểm quyền participant lúc liên kết |
| related_membership_id | FK tutor_memberships nullable, kiểm tutor sở hữu |
| related_transaction_id | FK payment_transactions nullable, user sở hữu và giao dịch hội viên |
| version | bigint not null default 0; @Version; mỗi mutation tăng version |
| created_at, updated_at | Timestamp có timezone, theo BaseEntity và trigger hiện có |

Không thêm description trùng: message đầu tiên là mô tả, không sửa/xóa. Không yêu cầu liên kết để gửi. Ba liên kết nếu cùng có mặt phải cùng thuộc người gửi và không mâu thuẫn (giao dịch/plan hội viên phải khớp nếu kiểm chứng được). Context mất thì trả unavailable, không làm mất nội dung phiếu.

## TicketMessage — ticket_messages

message_id UUID PK; ticket_id FK bắt buộc; sender_id FK bắt buộc; message_text text (1–10.000 ký tự, không trắng); sent_at TIMESTAMPTZ bất biến. Chỉ chứa trao đổi công khai. Không kế thừa BaseEntity nếu schema không có created_at/updated_at; map sent_at riêng. `attachment_url` legacy không trả nguyên trạng ra client; kiểm kê/migrate thủ công vào kho riêng nếu có tệp thật, không mở tải URL tùy ý và không tự fetch URL legacy.

Thứ tự ổn định: sent_at rồi message_id. Message create + attachment + transition + audit cùng transaction. Với RESOLVED, message của owner mở lại IN_PROGRESS; message của CS không tự mở lại. CLOSED cấm tất cả trao đổi.

## TicketEscalation — ticket_escalations (mới)

- escalation_id UUID PK; ticket_id FK; referred_by FK users; created_at; returned_at nullable.
- reason và handling_summary: text 1–10.000 ký tự không trắng.
- current_manager_id FK users; pending boolean; recipient_revision bigint khởi đầu 0.
- decision_text nullable (khi có: 1–10.000 ký tự không trắng), decision_by nullable, decision_revision nullable, decided_at nullable.
- Đổi quản lý tăng recipient_revision, đưa quyết định hiện tại về chưa có sau khi ghi snapshot vào audit; quyết định trước không xóa khỏi lịch sử. Return yêu cầu decision_by == current_manager_id và decision_revision == recipient_revision.
- Unique partial index trên ticket_id WHERE pending = true. Một phiếu có nhiều lần chuyển; ticket.escalated_to phản ánh quản lý hiện tại của lần gần nhất.

## TicketAuditEvent — ticket_audit_events (mới)

event_id UUID PK; ticket_id FK; actor_user_id FK; event_type; occurred_at; ticket_version; before_values/after_values JSONB; note text nullable. Ghi nhận CREATE, CLAIM, REASSIGN_CS, PRIORITY_CHANGE, MESSAGE_SENT, STATUS_CHANGE, ESCALATE, REASSIGN_MANAGER, SAVE_DECISION, RETURN_RESOLUTION, CLOSE. Snapshot chỉ gồm trường nghiệp vụ cần audit, không token, password hay binary. Audit không có endpoint sửa/xóa và không trả nguyên bản cho requester. Event và mutation nguyên tử. User deletion ngoài FE-10 không được dùng để tuyên bố lịch sử vẫn bất biến vĩnh viễn; preflight rà chính sách FK/cascade hiện tại.

## TicketAttachment — ticket_attachments (mới)

attachment_id UUID PK; ticket_id FK; message_id FK bắt buộc; uploaded_by FK; original_name varchar(255) đã làm sạch để hiển thị; content_type trong image/jpeg, image/png, application/pdf; size_bytes 1..10.000.000; created_at; content bytea. Nội dung ban đầu cũng là message nên không cần hai kiểu owner của tệp. Kiểm tra cùng ticket giữa message và attachment bằng composite FK hoặc ràng buộc tương đương, không chỉ tin client.

Metadata projection không select content. Tải tệp chọn content bằng query riêng sau policy và đọc nhất quán manager recipient hiện tại trong cùng transaction. Stream response không cache công khai; request sau đổi manager phải bị từ chối. Không cam kết thu hồi bytes đã tải về trước đó. Không hỗ trợ tệp nội bộ ở FE-10.

## State machine

| From | Lệnh / người thực hiện | To |
| --- | --- | --- |
| OPEN | Claim / CS hoạt động, chưa assignee | IN_PROGRESS |
| IN_PROGRESS | Escalate / assignee | ESCALATED |
| ESCALATED | Save decision / current manager | ESCALATED |
| ESCALATED | Return / current manager đã có quyết định đúng revision | IN_PROGRESS |
| IN_PROGRESS | Resolve kèm kết quả công khai / assignee | RESOLVED |
| RESOLVED | Message bổ sung / owner | IN_PROGRESS |
| RESOLVED | Close / owner | CLOSED |

Reassign CS: mọi CS, có assignee, chưa CLOSED; giữ trạng thái. Reassign manager: chỉ assignee khi ESCALATED, có lý do, người nhận khác còn hoạt động; giữ trạng thái, thu hồi quyền manager cũ. Priority change: assignee, chưa CLOSED; không đổi trạng thái. Message: owner/assignee khi chưa CLOSED. Các chuyển đổi khác bị từ chối; không auto-close.

## Transaction, indexes and migration

- Mọi mutation nhận expectedVersion; khóa ticket, kiểm version/quyền/trạng thái, ghi dữ liệu và audit rồi commit. Stale → 409, không retry âm thầm lệnh nghiệp vụ.
- Khóa theo cùng thứ tự: ticket → escalation → message/attachment. Kiểm trạng thái người nhận trong transaction; đổi role/account ngoài FE-10 phải dùng chiến lược khóa tương thích hoặc kiểm lại trước commit.
- Index ticket trên (user_id, updated_at, ticket_id), (assigned_to, status, updated_at), (escalated_to, updated_at), (status, category, priority, updated_at); kiểm EXPLAIN trên tập nghiệm thu trước thêm index dư thừa.
- Index message (ticket_id, sent_at, message_id), audit (ticket_id, occurred_at, event_id), attachment (message_id), escalation (ticket_id, created_at).
- SQL migration thêm version/tables/index, kiểm số dòng trước/sau và FK; không sửa/xóa ticket cũ để hợp thức hóa schema. Preflight kiểm orphan/legacy escalation không đủ thông tin, dừng kèm báo cáo nếu không thể chuyển đổi trung thực. Tạo bản ghi legacy với dữ liệu thật hiện hữu, không bịa tác giả/quyết định thiếu.
- Không dùng script dựng lại DatabasePgAdmin4.txt để migrate. Đối chiếu live schema vì entity membership hiện có đã lệch một số chi tiết của bản schema nguồn.
