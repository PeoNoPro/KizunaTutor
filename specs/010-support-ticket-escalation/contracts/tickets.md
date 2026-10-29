# Interface Contracts: FE-10

Hợp đồng dự kiến, chưa triển khai. API base `/api/v1/tickets`. UUID và enum chỉ là dữ liệu giao tiếp nội bộ; UI dùng nhãn tiếng Việt, không render mã thô. Mọi request xác thực bằng cơ chế hiện có và kiểm lại user ACTIVE, chưa deleted, role DB hiện tại. Role admin/academic không tự được quyền ticket.

## Common rules

- Đọc không có quyền/không tồn tại trả cùng 404 với thông điệp “Không tìm thấy yêu cầu hỗ trợ hoặc bạn không có quyền truy cập”. Chưa đăng nhập 401; tài khoản không hợp lệ hoặc lệnh sai vai trò 403; dữ liệu sai 400; stale/state conflict 409; quá request limit 413. Không trả exception/stack trace.
- Response lỗi: `{message, fieldErrors?}`; fieldErrors là danh sách `{field, message}` dành cho form mapping, không đưa field identifier ra UI. HTTP status do client ánh xạ, không hiển thị raw.
- Tất cả POST dùng CSRF token `X-CSRF-TOKEN` theo cơ chế token của Spring Security được đưa vào trang SSR. CSRF bảo vệ cả multipart; không miễn trừ chỉ vì có JWT cookie.
- JSON mutation có expectedVersion số nguyên không âm bắt buộc; multipart cập nhật có expectedVersion trong phần payload. Version lấy từ chi tiết mới nhất. Stale trả 409; client giữ nội dung và yêu cầu tải lại, không tự gửi lệnh khác.
- Message/subject/notes hiển thị escaped. Danh sách mặc định page=0,size=20; size 1..50, page không âm; query sai trả 400. Sort updatedAt desc, ticketId desc để phân trang ổn định.

## Reads

| Method/path | Quyền và dữ liệu |
| --- | --- |
| GET `/` | owner: chỉ phiếu của mình; CS: toàn bộ hàng đợi; manager: escalated_to hiện tại là mình. Query status/category/priority theo enum; assignedTo chỉ CS. Response `{items,page,size,totalElements,totalPages}` |
| GET `/{ticketId}` | Policy theo role. Trả projection tương ứng bên dưới |
| GET `/{ticketId}/messages?page&size` | Cùng quyền đọc phiếu; trang trao đổi công khai, sentAt asc/messageId asc |
| GET `/{ticketId}/history?page&size` | Chỉ CS và manager nhận hiện tại; audit nội bộ, không owner |
| GET `/assignees` | Chỉ CS; danh sách ID/tên CS hoạt động để nhận chuyển phân công |
| GET `/managers` | Chỉ CS; danh sách ID/tên manager hoạt động để chuyển cấp |
| GET `/related-contexts?type=booking\|membership\|payment&page&size` | Chỉ learner/tutor; trả context thuộc quyền người gửi; payment chỉ thanh toán hội viên |
| GET `/{ticketId}/related-context` | Người có quyền trên phiếu; projection tối thiểu, không password/bank/raw gateway response |
| GET `/{ticketId}/attachments/{attachmentId}` | Kiểm ticket/message/tệp và quyền hiện tại; Content-Disposition attachment, Content-Type đã xác minh, X-Content-Type-Options nosniff, Cache-Control no-store; không URL public |

Danh sách trả summary: ticketId, subject, category/categoryLabel, priority/priorityLabel, status/statusLabel, createdAt, updatedAt, version. Owner không nhận escalationNotes, audit hoặc decision. CS/manager có thêm assignee và recipient tối thiểu phục vụ công việc.

Chi tiết requester: summary, description lấy message đầu, public messages có phân trang riêng, attachment metadata, relatedContext hoặc unavailable, allowedActions. Chi tiết CS/manager mở rộng internal history/escalation/decision theo quyền. Không có trường internal rỗng/ẩn bằng CSS trong payload owner; chúng phải vắng mặt hoàn toàn.

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

- GET `/learner/support/tickets`, `/tutor/support/tickets`: danh sách của mình, tạo phiếu, mở chi tiết `/{id}`; xác nhận trước khi đóng, không có nút đóng cho staff.
- GET `/cs/support/tickets` và `/{id}`: filter, nhận phiếu, phân công lại, trao đổi, ưu tiên, chuyển cấp, giải quyết. Mọi CS có thể phân công lại nhưng các nút ghi khác chỉ assignee.
- GET `/manager/support/tickets` và `/{id}`: danh sách riêng, chi tiết nội bộ, lưu quyết định, trả kết quả; không phản hồi công khai/đóng thay người gửi.
- Tái sử dụng fragments theo phân hệ, learner/tutor theo DESIGN mục 7; labels tiếng Việt, không all-caps. Trạng thái loading/empty/error khác nhau; lỗi tệp giữ text nhưng hướng dẫn chọn lại file nếu browser không giữ được.
- Không tự tạo phiếu mẫu hay dữ liệu dự phòng khi API thất bại. Form được gắn nhãn và lỗi liên kết trường nhập; hỗ trợ bàn phím, focus về thông báo lỗi sau thất bại.
