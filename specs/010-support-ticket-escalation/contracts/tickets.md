# Interface Contracts: FE-10

API base `/api/v1/tickets`. UUID và enum chỉ là dữ liệu giao tiếp nội bộ; UI dùng nhãn tiếng Việt, không render mã thô. Mọi request xác thực bằng cơ chế hiện có và kiểm lại user ACTIVE, chưa deleted, role DB hiện tại. Role admin/academic không tự được quyền ticket.

Implementation alignment: JSON responses use the project's shared `{success,message,data,timestamp}` envelope; response shapes below describe `data`. The split static frontend replaces SSR token injection with authenticated GET `/csrf` returning `{headerName,token}` and GET `/me` returning `{displayName,viewRole,canCreate,canHandle,canDecide}`. Both endpoints use the same four-role/current-account policy. Existing ticket permissions and role routes are unchanged.

## Common rules

- Đọc không có quyền/không tồn tại trả cùng 404 với thông điệp “Không tìm thấy yêu cầu hỗ trợ hoặc bạn không có quyền truy cập”. Chưa đăng nhập 401; tài khoản không hợp lệ hoặc lệnh sai vai trò 403; dữ liệu sai 400; stale/state conflict 409; quá request limit 413. Không trả exception/stack trace.
- Response lỗi: `{message, fieldErrors?}`; fieldErrors là danh sách `{field, message}` dành cho form mapping, không đưa field identifier ra UI. HTTP status do client ánh xạ, không hiển thị raw.
- Tất cả POST dùng CSRF token `X-CSRF-TOKEN` theo cơ chế Spring Security lấy từ GET `/csrf` đã xác thực. CSRF bảo vệ cả multipart; không miễn trừ chỉ vì có JWT cookie.
- JSON mutation có expectedVersion số nguyên không âm bắt buộc; multipart cập nhật có expectedVersion trong phần payload. Version lấy từ chi tiết mới nhất. Stale trả 409; client giữ nội dung và yêu cầu tải lại, không tự gửi lệnh khác.
- Message/subject/notes hiển thị escaped. Danh sách mặc định page=0,size=20; size 1..50, page không âm; query sai trả 400. Sort updatedAt desc, ticketId desc để phân trang ổn định.

## Reads

CS dashboard extension requested from Figma 1113:39: GET `/dashboard?category=...` is CS-only (other roles 403, guests 401). Returns `{totalActive,countsByCategory,selectedCategory,queue,managerFeedback}` inside the standard envelope. Active queue means OPEN/IN_PROGRESS/ESCALATED; `queue` contains at most four most recently updated items, including requesterDisplayName. Counts are over the whole active CS queue. Optional category uses the existing category enum. `managerFeedback` is null or `{ticketId,subject,returnedAt}` for the latest completed manager return on a currently IN_PROGRESS ticket assigned to this CS; a newer pending escalation supersedes old returns. No new ticket states or business mutations.

| Method/path | Quyền và dữ liệu |
| --- | --- |
| GET `/` | owner: chỉ phiếu của mình; CS: toàn bộ hàng đợi; manager: escalated_to hiện tại là mình. Query status (một hoặc nhiều giá trị ngăn cách bằng dấu phẩy)/category/priority theo enum; assignedTo chỉ CS. Response `{items,page,size,totalElements,totalPages}` |
| GET `/{ticketId}` | Policy theo role. Trả projection tương ứng bên dưới |
| GET `/{ticketId}/messages?page&size` | Cùng quyền đọc phiếu; trang trao đổi công khai, sequenceNo tăng dần; sentAt chỉ hiển thị. Mỗi mục có senderRoleLabel (vai trò hiện tại của người gửi) và fromRequester; không trả sender ID |
| GET `/{ticketId}/history?page&size` | Chỉ CS và manager nhận hiện tại; audit nội bộ, không owner |
| GET `/assignees` | Chỉ CS; danh sách ID/tên CS hoạt động để nhận chuyển phân công |
| GET `/managers` | Chỉ CS; danh sách ID/tên manager hoạt động để chuyển cấp |
| GET `/related-contexts?type=booking\|membership\|payment&page&size` | Chỉ learner/tutor; trả context thuộc quyền người gửi; payment chỉ thanh toán hội viên |
| GET `/{ticketId}/related-context` | Người có quyền trên phiếu; projection tối thiểu, không password/bank/raw gateway response |
| GET `/{ticketId}/attachments/{attachmentId}` | Kiểm ticket/message/tệp và quyền hiện tại; Content-Disposition attachment, Content-Type đã xác minh, X-Content-Type-Options nosniff, Cache-Control no-store; không URL public |

Danh sách trả summary: ticketId, subject, category/categoryLabel, priority/priorityLabel, status/statusLabel, createdAt, updatedAt, version. Owner không nhận escalationNotes, audit hoặc decision. Danh sách của owner có thêm descriptionPreview (tối đa 160 ký tự đầu của message đầu). CS/manager có thêm assignee và recipient tối thiểu phục vụ công việc.

Chi tiết requester: summary, description lấy message đầu, public messages có phân trang riêng, attachment metadata, relatedContext hoặc unavailable, allowedActions. relatedContext dùng label tiếng Việt gồm loại, thông tin nhận diện (lịch: thời gian; hội viên: tên gói; thanh toán: tên gói và thời gian) và trạng thái; không có số tiền, mã giao dịch hay gateway. Chi tiết CS/manager có thêm requesterDisplayName, requesterRoleLabel và mở rộng internal history/escalation/decision theo quyền. Không có trường internal rỗng/ẩn bằng CSS trong payload owner; chúng phải vắng mặt hoàn toàn.

allowedActions chỉ phục vụ UI, server vẫn kiểm quyền từng lệnh. Attachment metadata: attachmentId, originalName, contentType, sizeBytes, uploadedByDisplayName, createdAt, downloadPath; không content bytes/storage URL. Client không gửi uploader/requester/role hay quyền.

## Writes

| POST path | Request | Điều kiện / hiệu ứng |
| --- | --- | --- |
| `/` | multipart payload `{subject,category,message,relatedBookingId?,relatedMembershipId?,relatedTransactionId?}`, files lặp 0..5 | Learner/tutor; tạo ticket OPEN, MEDIUM, first message và attachments; 201 + detail/version, Location |
| `/{id}/claim` | `{expectedVersion}` | CS, chưa assignee, OPEN → IN_PROGRESS |
| `/{id}/assignment` | `{expectedVersion,assigneeId}` | Mọi CS, có assignee, chưa CLOSED; người nhận CS active khác người phụ trách cũ |
| `/{id}/priority` | `{expectedVersion,priority}` | Assignee, chưa CLOSED |
| `/{id}/messages` | multipart payload `{expectedVersion,message}`, files 0..5 | Owner/assignee, chưa CLOSED; owner bổ sung RESOLVED → IN_PROGRESS |
| `/{id}/escalations` | `{expectedVersion,managerId,reason,handlingSummary}` | Assignee, IN_PROGRESS → ESCALATED; không pending khác |
| `/{id}/escalations/current/recipient` | `{expectedVersion,managerId,reason}` | Assignee, ESCALATED; manager mới active, lý do bắt buộc; revision tăng, bỏ quyết định hiện hành vào lịch sử |
| `/{id}/escalations/current/decision` | `{expectedVersion,decisionText}` | Current manager, ESCALATED; lưu decision riêng theo recipient revision |
| `/{id}/escalations/current/return` | `{expectedVersion}` | Current manager, có decision đúng revision; ESCALATED → IN_PROGRESS |
| `/{id}/resolve` | `{expectedVersion,resolutionText}` | Assignee, IN_PROGRESS; ghi public message kết luận + RESOLVED nguyên tử |
| `/{id}/close` | `{expectedVersion}` | Chỉ owner, RESOLVED → CLOSED |

Mọi mutation trừ create trả 200 với projection chi tiết và version mới; messages có attachment metadata. Không có PATCH/DELETE message, file, audit hoặc ticket; không generic endpoint thay status. Resolve chỉ text; nếu cần tệp kết luận, gửi public message kèm tệp trước, rồi resolve bằng văn bản.

## Validation and binary contract

subject 1..200, message/decision/reason/handlingSummary/resolutionText 1..10.000 ký tự không chỉ trắng. Độ dài tính Unicode code points, kiểm thống nhất ở server và giao diện. Tên gốc tệp là metadata, loại bỏ đường dẫn và ký tự điều khiển, tối đa 255 ký tự.

Tối đa 5 files; mỗi file 1..10.000.000 byte; JPG/JPEG/PNG/PDF với extension, MIME và nội dung đọc được tương ứng. Không hỗ trợ encrypted PDF không thể xác minh; thông báo yêu cầu bản PDF mở được. Giới hạn request 52.000.000 byte, form text theo các giới hạn trên; file count/bytes kiểm phía server, không chỉ thuộc tính HTML. Một tệp sai hoặc lỗi lưu làm rollback toàn bộ lần gửi. Bytes không bao giờ vào list/detail JSON.

## UI contract

- Giao diện theo Figma `KizunaTutor · Dashboard` (bộ MỚI) và FRONTEND_RULES. Tab danh sách chỉ gom trạng thái có sẵn: người gửi “Đang theo dõi” (OPEN, IN_PROGRESS, ESCALATED), “Đã giải quyết” (RESOLVED, CLOSED); CS “Cần xử lý” (OPEN, IN_PROGRESS), “Chờ phản hồi” (RESOLVED), “Đã chuyển Quản lý” (ESCALATED), “Đã đóng” (CLOSED); quản lý “Chờ xem xét” (ESCALATED), “Đã phản hồi” (IN_PROGRESS, RESOLVED, CLOSED). Chi tiết vẫn là trang có URL riêng, trình bày theo khung hộp thoại của Figma; chuyển quản lý và ghi nhận kết quả của CS là hộp thoại thật.

- CS/manager detail has two tabs: “Trao đổi với người gửi” (default) and “Xử lý nội bộ”. Ticket acceptance and public replies/resolution stay in the first tab; reassignment, priority, escalation and private decisions stay in the second. History is collapsed initially, loads only on expansion and remains paginated. Requesters have no internal tab or internal history request. Tab changes preserve draft text and support arrow/Home/End keyboard navigation.

- GET `/learner/support/tickets`, `/tutor/support/tickets`: danh sách của mình, tạo phiếu, mở chi tiết `/{id}`; xác nhận trước khi đóng, không có nút đóng cho staff.
- GET `/cs/support/tickets` và `/{id}`: filter, nhận phiếu, phân công lại, trao đổi, ưu tiên, chuyển cấp, giải quyết. Mọi CS có thể phân công lại nhưng các nút ghi khác chỉ assignee.
- GET `/manager/support/tickets` và `/{id}`: danh sách riêng, chi tiết nội bộ, lưu quyết định, trả kết quả; không phản hồi công khai/đóng thay người gửi.
- Tái sử dụng fragments theo phân hệ, learner/tutor theo DESIGN mục 7; labels tiếng Việt, không all-caps. Trạng thái loading/empty/error khác nhau; lỗi tệp giữ text nhưng hướng dẫn chọn lại file nếu browser không giữ được.
- Không tự tạo phiếu mẫu hay dữ liệu dự phòng khi API thất bại. Form được gắn nhãn và lỗi liên kết trường nhập; hỗ trợ bàn phím, focus về thông báo lỗi sau thất bại.

## Cập nhật thiết kế và nghiệm thu FE-10

US1 chỉ nghiệm thu tạo phiếu kèm tệp, list/detail, GET hội thoại và tải tệp có phân quyền. POST bổ sung của owner, phản hồi CS và tệp trong hội thoại thuộc US2; chỉ kết luận toàn bộ FR-020 hoàn tất khi checkpoint US1 và US2 đều đạt.

Hội thoại trả sequenceNo và sắp xếp tăng dần theo trường này; sentAt chỉ hiển thị. sequence_no được cấp dưới khóa ticket, UNIQUE(ticket_id, sequence_no). Hai lệnh cùng expectedVersion chỉ một thành công; lệnh stale tải version mới trước khi gửi lại. Kiểm hai message cạnh tranh không trùng số và hiển thị đúng thứ tự ngay khi timestamp trùng.

Ảnh tối đa 20.000.000 pixel; PDF tối đa 100 trang, từ chối bản mã hóa không đọc được. Kiểm tra cấu trúc bằng ImageIO/PDFBox trong tiến trình con riêng: giới hạn cứng 256 MB RAM (256.000.000 byte) bằng cơ chế hệ điều hành, 10 giây/tệp và tổng 30 giây/lần gửi. -Xmx không thay thế giới hạn RAM toàn tiến trình. Hết hạn/vượt bộ nhớ phải dừng tiến trình, dọn tệp tạm và từ chối toàn bộ lần gửi, không lưu phiếu/message/tệp/audit một phần. Không kiểm tra trong JVM ứng dụng; nếu môi trường chưa áp được cách ly và giới hạn thì từ chối tiếp nhận tệp. Thực hiện kiểm tra tệp trước transaction ghi ngắn, rồi kiểm lại quyền/version/trạng thái dưới khóa trước commit.

SC-005: 100 người dùng đồng thời trong 10 phút, 1.000 phiếu có trao đổi; đo riêng p95 danh sách và chi tiết từ thao tác mở trang trên trình duyệt thật đến khi render hoàn tất, mỗi loại ≤2 giây. Báo cáo HTTP timing riêng để chẩn đoán; không dùng HTTP thay phép đo render.

Menu learner và tutor có mục “Yêu cầu hỗ trợ”, dẫn lần lượt tới /learner/support/tickets và /tutor/support/tickets; danh sách cá nhân có nút tạo phiếu. Kiểm thử điều hướng và quyền cả hai vai trò, kể cả truy cập trực tiếp/đổi route.

## Lịch ca nhân viên hỗ trợ và tự giao yêu cầu — bổ sung 2026-11-07

Nhóm đã thống nhất bổ sung sau cuộc họp. Bảng `support_shifts` khai báo trong `database/seeds/fe10_support_ticket.sql` (`CREATE TABLE IF NOT EXISTS`). Ca cố định theo giờ Asia/Ho_Chi_Minh: `MORNING` 08:00–12:00, `AFTERNOON` 13:00–17:00, `EVENING` 18:00–22:00. Mã ca chỉ dùng nội bộ; giao diện hiện "Ca sáng/chiều/tối".

| Method/path | Quyền | Request | Response `data` / lỗi |
| --- | --- | --- | --- |
| GET `/shifts?weekStart=YYYY-MM-DD` | Manager hoặc CS đang hoạt động; vai trò khác 403 | `weekStart` tùy chọn, mặc định tuần hiện tại, luôn quy về thứ Hai | `{weekStart,today,shiftTypes:[{code,label,start,end}],days:[{date,shifts:{MORNING:[{shiftId,staffId,displayName}],AFTERNOON:[...],EVENING:[...]}}],staff:[{id,name}],canEdit}`; `canEdit=true` chỉ với Manager |
| POST `/shifts` | Chỉ Manager (403 với vai trò khác, kiểm tra trước khi khóa phân việc) | `{staffId,date,shiftCode}` | Trả lại lịch tuần chứa `date`. 400 ngày đã qua, ca sai hoặc người được xếp không phải CS đang hoạt động; 409 nhân viên đã có trong ca đó |
| DELETE `/shifts/{shiftId}?weekStart=` | Chỉ Manager | — | Trả lại lịch tuần `weekStart`. 404 ca không còn; 400 ca của ngày đã qua |

Lệnh ghi dùng CSRF từ GET `/csrf` như các lệnh khác. Không có PUT sửa ca; đổi ca bằng gỡ rồi xếp lại.

Mỗi lần xếp hoặc gỡ ca ghi một dòng vào `support_shift_events` (shiftId, nhân viên, ngày, ca, `ASSIGN`/`REMOVE`, người thực hiện, thời điểm) trong cùng giao dịch; bảng chỉ thêm, trigger chặn sửa/xóa. Gỡ ca vẫn xóa dòng trong `support_shifts` nhưng lịch sử được giữ. Lịch sử hiện ở Nhật ký thao tác của Admin (nhóm "Lịch ca CS").

Quy tắc tự giao (không có endpoint riêng; cập nhật 2026-11-09 theo yêu cầu nhóm):

- Mỗi CS giữ tối đa **3** yêu cầu đang xử lý (`SupportShiftService.MAX_ACTIVE`; IN_PROGRESS do mình phụ trách, chưa tính phiếu Quản lý trả về cho người phụ trách cũ). Lý do: phần lớn thời gian một yêu cầu là chờ người dùng trả lời.
- Yêu cầu OPEN chưa có người phụ trách được tự giao cho CS **đang trong ca** và **còn chỗ**, theo **mức ưu tiên** (Khẩn cấp → Cao → Bình thường → Thấp) rồi tới thời gian gửi; chia vòng, người đang giữ ít việc nhận trước. Ghi lịch sử `AUTO_ASSIGN` và gửi thông báo FE-12 cho CS được giao. Chạy sau commit khi có phiếu mới, khi CS xong việc, khi Quản lý xếp ca, và định kỳ 30 giây.
- **Mức ưu tiên ban đầu** do hệ thống đặt lúc tạo (người gửi không chọn, `SupportTicketPriorityRule`): gắn buổi học thử bắt đầu trong 24 giờ tới → Khẩn cấp; Thanh toán, Sự cố kỹ thuật → Cao; Nội dung khác → Thấp; còn lại → Bình thường. CS phụ trách vẫn đổi được sau khi nhận.
- **Không có nhận thủ công**: CS không bấm "Tiếp nhận"; `POST /{id}/claim` trả 400. Ngoài ca hoặc ngày chưa xếp ca thì CS chỉ xem, không được giao việc mới (bỏ ngoại lệ "ngày chưa xếp ca thì nhận tự do").
- Ca đang diễn ra không có CS nào mà còn yêu cầu chờ: gửi thông báo FE-12 cho mọi Quản lý "Chưa có nhân viên hỗ trợ trong ca" (mỗi ca một lần), link tới trang xếp ca.
- ~~CS bị khóa hoặc đổi vai trò: phiếu đang giữ được giao cho CS khác đang trong ca và đang rảnh~~ — **thay bằng bàn giao tay bởi Quản lý (2026-11-10)**, xem mục "Bàn giao khi CS nghỉ" bên dưới. Bộ tự giao chỉ còn giao phiếu OPEN chưa có người phụ trách.
- Hết ca vẫn giữ việc đang làm; kết quả Quản lý trả về luôn về người phụ trách cũ, có thể vượt mức 3 yêu cầu. CS bàn giao cho CS khác chỉ được khi người nhận đang trong ca và còn chỗ (409 nếu không); Quản lý bàn giao (FR-022) không bị giới hạn này.
- POST `/shifts` (2026-11-10): lượt tự giao sau khi xếp ca chạy nền sau commit, không giữ request của Quản lý (trước đây request vượt thời gian chờ của trình duyệt dù ca đã lưu). Trang xếp ca không còn hiện đoạn chữ mô tả quy tắc tự giao.

## Bàn giao khi CS nghỉ — bổ sung 2026-11-10 (FR-022)

Phiếu "cần bàn giao": `assigned_to` khác rỗng, trạng thái `IN_PROGRESS`/`ESCALATED`/`RESOLVED`, và người phụ trách không còn là CS hoạt động (khóa, xóa hoặc đổi vai trò). Hệ thống không tự chuyển.

| Method/path | Quyền | Thay đổi |
| --- | --- | --- |
| GET `?handover=true` và GET `/counts?handover=true` | Chỉ Manager (vai trò khác 403) | Chỉ trả phiếu cần bàn giao, kể cả phiếu không chuyển cho Manager này. Không có `handover` thì Manager vẫn chỉ thấy phiếu `escalated_to` là mình |
| GET `/{id}`, `/messages`, `/history` | Manager đọc được phiếu cần bàn giao | Summary/detail thêm `assigneeActive` (boolean) và `needsHandover` (boolean) cho nhân viên/Quản lý |
| GET `/handover-candidates` | Chỉ Manager | `[{userId,displayName,activeTickets,onShift,maxActive}]` CS đang hoạt động; người đang trong ca rồi ít việc xếp trước |
| GET `/assignees` | CS hoặc Manager | Manager được lấy danh sách CS (không lấy danh sách Manager) |
| POST `/{id}/assignment` | CS như cũ; Manager chỉ với phiếu cần bàn giao | `{expectedVersion,assigneeId,reason?}`; Manager không bị kiểm ca/giới hạn 3 phiếu. Ghi `REASSIGN_CS`, ghi chú mặc định "Quản lý bàn giao vì nhân viên cũ không còn hoạt động" nếu bỏ trống; báo FE-12 cho người nhận |

`allowedActions` của Manager có `assignment` khi phiếu cần bàn giao; `decision`/`return` chỉ khi `escalated_to` là chính Manager đó.

Thông báo FE-12: tác vụ định kỳ 60 giây tìm phiếu cần bàn giao và gửi mọi Manager "Yêu cầu hỗ trợ cần bàn giao" (`eventKey = ticket:{id}:handover-needed:{assigneeId}`, mỗi cặp phiếu/người cũ một lần), `actionPath=/manager/support/tickets/{id}`.

Giao diện Manager: sidebar "Yêu cầu chuyển lên" có thêm mục con "Cần bàn giao" (`?group=2`) kèm số đếm; danh sách hiện nhãn đỏ "Cần bàn giao". Mở phiếu cần bàn giao tự chuyển sang tab "Xử lý nội bộ" với khung "Bàn giao yêu cầu": người phụ trách cũ (nhãn "Không còn hoạt động"), chọn người nhận dạng thẻ (đang trong ca/ngoài ca, số phiếu đang xử lý trên 3), ghi chú bàn giao tùy chọn, nút "Bàn giao cho …".

GET `/dashboard` (CS) bổ sung `shift: {current,next,activeTickets,maxActive}`; `current`/`next` là `{code,label,start,end,date}` hoặc `null`.
