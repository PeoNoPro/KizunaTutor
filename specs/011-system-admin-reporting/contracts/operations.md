# Interface Contracts: FE-11

Hợp đồng FE-11 và các bổ sung đã được duyệt. JSON chỉ là dữ liệu giao tiếp nội bộ; UI không render ID, enum, khóa config hoặc exception thô. Kiểm current User ACTIVE, deletedAt null, role DB trên mọi request. Admin quản trị vận hành; Admin/Manager đọc báo cáo tổng hợp; nhân sự nội bộ đọc danh tính/hồ sơ bản thân; learner/tutor-only policy read. Quyền không được suy ra từ nút UI hoặc role token cũ.

## Common behavior

## Báo cáo quản lý và hồ sơ — bổ sung 2026-11-01

- `GET /management-report?startDate&endDate`: Admin hoặc Manager ACTIVE, mặc định7ngày, tối đa366ngày, không ngày tương lai. Trả kỳ, thời điểm cập nhật, tổng tài khoản chưa xóa và nhóm vai trò/trạng thái hiện tại, số tài khoản mới tạo trong kỳ; các yêu cầu kết nối và booking tạo trong kỳ theo **trạng thái hiện tại lúc truy vấn**. Không diễn giải thành snapshot trạng thái ở cuối kỳ quá khứ. Tỷ lệ chấp nhận = số yêu cầu thuộc cohort tạo trong kỳ hiện có trạng thái ACCEPTED / tổng cohort; không có yêu cầu thì tỷ lệ null. Nguồn lấy qua service công khai FE-04/05; không có thông tin cá nhân hoặc ghi chú sự cố trong báo cáo.
- `GET /profile`: hồ sơ **chính tài khoản nội bộ đang sử dụng**: fullName,email,phone,avatarUrl,roleLabel,statusLabel,createdAt. Không có tham số chọn người khác, không có hash/mật khẩu. Lượt này là màn hình xem, không làm lại phần sửa hồ sơ FE-02.
- `GET /me`: bổ sung canAdmin/canReport theo role DB hiện tại; cho tài khoản nội bộ đọc danh tính bản thân. Learner/tutor không truy cập endpoint quản trị này. Manager chỉ được đọc profile/me/management-report, không được sửa cấu hình, nhân viên hay sự cố.
- Phần cấu hình tích hợp/giới hạn AI được người dùng yêu cầu để sau trong cùng lượt này; không thêm endpoint, migration hoặc giá trị giới hạn chưa chốt.

## Bổ sung quản lý nhân viên theo yêu cầu ngày 2026-11-01

Mọi endpoint quản lý nhân sự dưới `/api/admin/operations` yêu cầu quản trị viên ACTIVE hiện tại; mọi POST/PUT giữ CSRF. Chỉ quản lý tài khoản bốn vai trò nội bộ hiện có, không chuyển học viên/gia sư thành nhân sự và không tạo vai trò tùy ý. Các quyền vận hành theo kiểm tra role của backend hiện tại; chưa có hệ thống quyền tùy chỉnh từng nút.

- `GET /staff?q&page&size`: tìm tên/email, page>=0, size1..50 mặc định20. Trả `{items,page,size,totalElements}`; item `{userId,fullName,email,phone,roleId,roleLabel,status,statusLabel,updatedAt,self}`. Không trả hash/mật khẩu.
- `GET /roles`: danh mục đủ 6 vai trò từ DB `{roleId,label,active,staffAssignable,accountCount,capabilities}`. Chỉ 4 vai trò nội bộ có staffAssignable=true; learner/tutor hiển thị để tra cứu, không gán trong biểu mẫu nhân viên. Quyền mô tả quyền quản trị thực được backend FE-11 kiểm tra, không hứa quyền của module chưa triển khai.
- `POST /staff`: `{fullName,email,phone?,roleId,initialPassword}`. Tên1..100, email hợp lệ tối đa254, phone<=20, mật khẩu12..72 ký tự và <=72 byte UTF-8, lưu BCrypt, yêu cầu đổi mật khẩu ở lần đăng nhập thật. Tài khoản mới ACTIVE. Chỉ gán vai trò còn hoạt động. Không gửi email tự động hoặc trả lại mật khẩu.
- `PUT /staff/{id}`: `{fullName,email,phone?,expectedUpdatedAt,reason}`. Lý do1..2000, timestamp phải khớp; email người đang thao tác không đổi để giữ phiên hiện tại. Không sửa trường quyền qua endpoint này.
- `PUT /staff/{id}/role`: `{roleId,expectedUpdatedAt,reason}`. Không đổi vai trò của chính mình; không làm mất quản trị viên cuối đang hoạt động.
- `PUT /staff/{id}/status`: `{status:ACTIVE|SUSPENDED,expectedUpdatedAt,reason}`. Khóa/mở tài khoản, không khóa chính mình hoặc quản trị viên cuối; trạng thái khác bị từ chối.
- `GET /staff/history?page&size`: lịch sử thao tác với nhãn Việt, tên người thực hiện, tên đối tượng, lý do, thời gian. Không trả JSON audit thô, mật khẩu hoặc hash.

Ghi account và audit cùng transaction; khóa tuần tự thao tác nhân sự và kiểm quyền hiện tại trong transaction. Email trùng400; phiên dữ liệu cũ409; sai quyền403; ngoài nhân sự/không tồn tại404. Không xóa tài khoản hay lịch sử. Migration riêng `database/seeds/admin_staff_schema.sql`, chỉ áp dụng DB phát triển hiện tại và DB test sau kiểm tra; production theo quy trình migration của nhóm.

- `GET /api/admin/operations/me`: tài khoản nội bộ đang hoạt động, trả `{displayName,roleLabel,avatarUrl,canAdmin,canReport}` trong `ApiResponse.data`; tên/ảnh/vai trò lấy từ DB hiện tại. Phiên `admin-dev` dùng chính tài khoản DB đã chọn. Ảnh trống dùng chữ viết tắt tên. Không trả mã vai trò, email hoặc token; email bản thân nằm trong `/profile`.

- FE-11 POST/PUT có CSRF token header từ SSR, kể cả JWT cookie; scoped security không đổi endpoint ngoài tính năng.
- Mutation trừ create có expectedVersion số nguyên không âm bắt buộc. Stale state → 409, giữ text và yêu cầu reload, không tự retry lệnh.
- 400 dữ liệu sai; 401 chưa đăng nhập; 403 sai role/account; 404 không tìm thấy hoặc không được đọc object; 409 version/state conflict; 503 nguồn nghiệp vụ không truy vấn được. Response lỗi `{message,fieldErrors?}`, tiếng Việt, không stack/log/config raw.
- List page>=0,size 1..50, default 20; invalid query →400. Incident list createdAt desc/id desc; event sequence asc. Độ dài văn bản tính Unicode code points, không chỉ khoảng trắng. Ngày JSON ISO, UI theo Asia/Ho_Chi_Minh.
- DTO allowlist, không serialize entity. Không có thao tác execute shell/restart/deploy/financial mutation.

## Configuration — `/api/admin/operations/booking-policy`

| Method/path | Input | Output/behavior |
| --- | --- | --- |
| GET base | Không | `{version,revision,effectiveAt,durationMinutes,minAdvanceHours,holdMinutes:15,definitions,editable}`; definitions dùng nhãn Việt, giới hạn 15..120/1..168, hold chỉ đọc |
| PUT base | `{expectedVersion,durationMinutes,minAdvanceHours,reason}` | Admin, số nguyên, lý do 1..2.000 không trắng; confirmation ở UI; cập nhật cả hai+revision+audit atomically, 200 config mới |
| GET `/history?page&size` | Pagination | Actor displayName, before/after approved values, reason, timestamp/revision; không key/value khác ngoài allowlist |

Không nhận configKey tùy ý hoặc holdMinutes từ client. Giá trị hiện tại thiếu/invalid: editable=false cùng thông báo cần kiểm tra cấu hình, không tự fallback 45/12 và không cho tạo booking dùng policy không hợp lệ. Không có cấu hình lùi thời gian/hẹn hiệu lực trong bản này.

## Booking integration boundary (internal service)

`BookingPolicyEvaluator.evaluate(startAt,serverNow)` trả revisionId, durationMinutes, minAdvanceHours, computedEndAt; reject startAt < now+advance. Khi tạo booking, caller khóa/read current revision và lưu snapshot+booking chung transaction. Không client-supplied revision override; old bookings không bị tính lại.

Không tạo một API đặt booking giả ở FE-11. Điểm tích hợp là TrialBookingServiceImpl khi luồng đặt học thử thực tế được triển khai; nếu vẫn là stub, đánh dấu acceptance FR-004 chưa hoàn tất, không coi unit test evaluator là E2E.

## Audit log — `/api/admin/operations/audit-log`

`GET ?source=CONFIG|STAFF|INCIDENT&actor&fromDate&toDate&page&size`: quản trị viên đang hoạt động; hợp nhất lịch sử cấu hình, nhân sự và sự cố, chỉ đọc. Trả `{items,page,size,totalElements}`; item `{eventId,occurredAt,actorDisplayName,sourceLabel,actionLabel,targetLabel,resultLabel,reason?,incidentId?}`, mới nhất trước. Ngày theo Asia/Ho_Chi_Minh. Không trả mô tả, diễn biến hoặc xác minh sự cố; nội dung đó xem ở chi tiết sự cố. `incidentId` chỉ dùng để mở liên kết, UI không hiển thị. Tham số sai 400; vai trò khác 403.

## Health and reports — `/api/admin/operations`

### Bổ sung tổng quan tài khoản theo yêu cầu ngày 2026-11-01

`GET /dashboard/accounts` yêu cầu quản trị viên đang hoạt động; trả `{totalAccounts,activeAccounts,tutorAccounts,activeTutorAccounts,learnerAccounts,activeLearnerAccounts,staffAccounts,activeStaffAccounts,otherAccounts,generatedAt}` trong `ApiResponse.data`. Đếm tài khoản chưa xóa theo vai trò, không đếm hồ sơ, lớp học hoặc gói hội viên thay thế. Nhân sự gồm quản trị viên, quản lý, học vụ, chăm sóc khách hàng. Hoạt động yêu cầu tài khoản ACTIVE và vai trò còn hoạt động. Số đếm là số nguyên không âm; nguồn lỗi trả lỗi hữu hạn, không thay bằng 0. Không trả email, danh sách người dùng hoặc mã vai trò. Tổng quan là số liệu hiện tại, không bị lọc theo khoảng ngày báo cáo sự cố.

| Method/path | Input | Behavior |
| --- | --- | --- |
| GET `/health` | Không | Snapshot collector: `{generatedAt,components:[{label,statusLabel,observedAt,responseTimeMs?,stale,reasonLabel,scopeLabel}]}`; không internal URL/probe raw. Thành phần không nguồn UNKNOWN, chỉ trả snapshot sau khi xác minh quyền hiện tại; identity DB outage trả 503 hữu hạn |
| GET `/reports/incidents` | `startDate,endDate` optional cùng nhau; `metric=created|resolved|open`, page/size và cutoff cho đối chiếu cùng thời điểm chốt | Default 7 ngày gồm hôm nay, max366 inclusive, không future; trả kỳ, cutoff, createdCount,resolvedCount,openAtCutoff và từng breakdown; lỗi nguồn 503, không zero fallback |

GET health không tự tạo incident hoặc kích hoạt probe từ URL người dùng. Poll 30 giây, data >60 giây stale. Nhấn refresh đọc snapshot, hiển thị lần đo thực tế. Không khẳng định heartbeat ứng dụng đo khả dụng từ Internet. Không thêm báo cáo usage/finance/export hoặc guest/manager access.

## Incidents — `/api/admin/operations/incidents`

| Method/path | Request | Điều kiện và kết quả |
| --- | --- | --- |
| GET base | status,severity,component,assignee,fromDate,toDate,page,size | Danh sách/filter theo nguồn thật, pagination ổn định |
| POST base | `{subject,description,severity,components,recurrenceOf?}` | Subject1..200, description1..10.000, components không rỗng trong allowlist; severity LOW/NORMAL/HIGH/CRITICAL; NEW chưa assignee, 201+Location/version; recurrence chỉ hồ sơ cũ CLOSED |
| GET `/{id}` | Không | Summary/detail, assignee/reporter nhãn, version, allowedActions; log thô không trả |
| GET `/{id}/events` | page,size | Lịch sử thứ tự seq; actor/time/action/before-after/detail/verification đã làm sạch |
| POST `/{id}/claim` | `{expectedVersion}` | Admin active, NEW và chưa assignee → IN_PROGRESS |
| POST `/{id}/assignment` | `{expectedVersion,assigneeId,reason}` | Mọi admin, chưa CLOSED, đã assignee, nhận admin khác active; reason1..2.000 không trắng, giữ state |
| POST `/{id}/progress` | `{expectedVersion,detail}` | Chỉ assignee, IN_PROGRESS, detail1..10.000 không trắng |
| POST `/{id}/resolve` | `{expectedVersion,result,verification}` | Chỉ assignee, IN_PROGRESS → RESOLVED, result/verification mỗi trường1..10.000 không trắng |
| POST `/{id}/resume` | `{expectedVersion,reason}` | Chỉ assignee, RESOLVED → IN_PROGRESS, reason1..2.000 không trắng |
| POST `/{id}/close` | `{expectedVersion}` | Chỉ assignee, RESOLVED → CLOSED |

Mỗi lệnh chỉ nhận đúng các trường trong bảng; gửi kèm trường của lệnh khác bị 400 và không ghi lịch sử. Tất cả update trả 200 detail/version mới, command+event atomic. Không generic status PATCH, sửa/xóa event/incident, attachment hoặc auto-create. State CLOSED từ chối ghi; không tự đóng vì nguồn probe đã khỏe. Claim/reassign/state race →409 hoặc403 theo quyền thực tế.

## Approved policy read — `/api/policies`

GET base: danh mục các bản PUBLISHED đang có hiệu lực, chỉ learner/tutor; GET `/{type}`: một bản hiện hành với title/versionLabel/effectiveAt/content. Không trả draft/future/withdrawn qua ID trực tiếp, không CRUD endpoint.

Không có tài liệu: danh sách rỗng, chi tiết404; chồng lấn:409 thông báo không xác định được bản hiện hành, không chọn ngẫu nhiên. Import nguồn được duyệt nằm ngoài web UI và phải kiểm overlap/provenance theo data-model. Không soạn nội dung thay người phê duyệt, không tự thêm bắt buộc ký/chấp thuận.

## UI routes and behavior

- `/admin/operations/booking-policy`: hai tham số có units/range, hold15 read-only, lý do/xác nhận, history. Khi conflict giữ text và tải giá trị mới để admin quyết định.
- `/admin/operations/health`: heartbeat/DB/verified optional dependency, age và source scope, refresh; bỏ serverStatus hardcode trong admin dashboard phần tích hợp.
- `/admin/operations/incidents` và `/{id}`: list/filter/create/claim/progress/assignment/result/verification/close, chỉ actions được phép. Tạo thủ công, không auto từ probe.
- `/admin/operations/logs`: nhật ký chung, lọc theo nhóm, người thực hiện và ngày; chỉ đọc.
- `/admin/operations/reports`: chọn kỳ, từng chỉ số có định nghĩa/mẫu số; số0 khác missing/error.
- `/learner/policies`, `/tutor/policies`, cùng `/{type}`: menu dẫn tới danh mục/chính sách, version/effective date, text escaped. Không editor.
- Admin theo DESIGN chung/shell hiện có; learner/tutor mục7 DESIGN. Mọi trạng thái/nhãn Việt đúng dấu, không all-caps ngoài từ viết tắt được duyệt; focus/error/keyboard và loading/empty/error riêng; không mock khi nguồn thiếu.
