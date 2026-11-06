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
