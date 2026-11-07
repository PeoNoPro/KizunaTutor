# Interface Contracts: FE-11

## Hợp đồng hiện hành — đồng bộ 2026-11-05

Áp dụng FR-023–029/SC-012–014 trong spec.md trước các hợp đồng lịch sử bên dưới. Bổ sung thông tin nền tảng dưới đây là **thiết kế cần triển khai**, không khẳng định endpoint đã tồn tại. Banner bảo trì hoãn đến khi người dùng tải code mới; không thêm API bảo trì trong đợt này.

Tiền tố quản trị `/api/admin/operations`, Admin ACTIVE, đã đổi mật khẩu lần đầu; lệnh ghi giữ CSRF và kiểm quyền DB hiện tại.

| Endpoint | Hợp đồng |
| --- | --- |
| `GET /settings/platform-info` | Trả `displayName,supportEmail,supportPhone,version,updatedAt`; thiếu dữ liệu trả trạng thái chưa cấu hình, không tự tạo liên hệ giả |
| `PUT /settings/platform-info` | Body `displayName,supportEmail,supportPhone,expectedVersion,reason`; ràng buộc FR-023/026; lưu cả nhóm và audit trong cùng transaction |
| `GET /api/platform-info` | Công khai, chỉ trả tên hiển thị/email/số hỗ trợ; không có audit, tác giả, khóa cấu hình hoặc bí mật. Thiếu cấu hình và lỗi nguồn có kết quả phân biệt |
| `GET /settings/attachment-limit` | Trả giới hạn hiện hành cùng phiên bản |
| `PUT /settings/attachment-limit` | `maxMegabytes` số nguyên 1–10, `expectedVersion,reason`; 1 MB = 1.048.576 byte |
| `GET /settings/history?page&size` | Lịch sử hai nhóm cấu hình; mặc định 20, tối đa 50; tác giả, thời điểm, lý do, trước/sau; chỉ đọc |

### Email gửi thư và AI — bổ sung 2026-11-07

Nhóm đã thống nhất bổ sung sau cuộc họp. Chỉ Admin còn hoạt động; lệnh ghi cần CSRF, `expectedVersion` và `reason` (1–2.000 ký tự), ghi audit cùng transaction như các cấu hình khác. Mật khẩu ứng dụng và API key lưu mã hóa AES-GCM trong `system_configurations`, **không bao giờ** trả lại giao diện (chỉ 4 ký tự cuối dạng `maskedPassword`/`maskedKey`), không ghi vào log hay lịch sử.

| Endpoint | Hợp đồng |
| --- | --- |
| `GET /settings/mail-sender` | Trả `senderEmail,defaultEmail,configured,ownAccount,maskedPassword,smtpHost,smtpPort,version`. `ownAccount=false` nghĩa là đang gửi bằng tài khoản SMTP của server (`spring.mail.*`) |
| `PUT /settings/mail-sender` | Body `senderEmail,appPassword?,smtpHost?,smtpPort?(1–65535, mặc định 587),expectedVersion,reason`. `appPassword` trống thì giữ mật khẩu cũ khi cùng email; đổi email mà đã có mật khẩu cũ thì bắt buộc nhập mật khẩu mới (400). Trước khi lưu, server **đăng nhập thử** máy chủ SMTP (không gửi thư, tối đa khoảng 10 giây): sai mật khẩu hoặc không kết nối được trả 400 và không lưu gì. Trùng cấu hình hiện hành 400, phiên bản cũ 409. Không có API gửi thư thử riêng: bước đăng nhập thử khi lưu thay cho việc này |
| `GET /settings/ai` | Trả `provider,providerLabel,model,keyConfigured,version,providers:[{code,label,defaultModel,keyConfigured,keySource,maskedKey,baseUrl}]`. `provider` thuộc `GEMINI,OPENAI,CUSTOM` (CUSTOM = máy chủ tương thích OpenAI); `keySource` là `admin`, `server` hoặc rỗng |
| `PUT /settings/ai` | Body `provider,model([A-Za-z0-9._:/-]{1,80}),apiKey?,baseUrl?(chỉ dùng khi CUSTOM, tối đa 200; trống thì giữ địa chỉ đã lưu),expectedVersion,reason`. `apiKey` trống thì giữ key đang có của nhà cung cấp đó. Áp dụng ngay cho lần gọi AI tiếp theo của FE-08 qua `SystemSettingService.aiChoice()` |
| `POST /settings/ai/test` | Body `provider,model,apiKey?,baseUrl?`, chưa lưu. Gửi một câu hỏi ngắn tới AI; trả `{ok,message,latencyMs,reply?}` |
| `GET /settings/history` | Bổ sung các khóa email gửi thư và AI; giá trị bí mật chỉ ghi "đổi mật khẩu ứng dụng"/"đổi API key" |
| `GET /audit-log?source=SHIFT` | Nhật ký thao tác thêm nhóm `SHIFT` ("Lịch ca CS") đọc từ `support_shift_events` của FE-10: hành động "Xếp ca"/"Gỡ ca", đối tượng "Ca sáng 07/11/2026 · tên nhân viên", kết quả "Đã lưu". Chỉ đọc |

Bộ gửi thư dùng chung (`AdminMailConfiguration`, bean `@Primary JavaMailSender`): mọi thư của hệ thống (mật khẩu nhân viên, OTP, xác minh gia sư, thanh toán) gửi bằng tài khoản Admin đã cấu hình; chưa cấu hình thì dùng `spring.mail.*`. Nếu tài khoản Admin không đăng nhập hoặc không kết nối được lúc gửi, thư được gửi lại bằng `spring.mail.*` và ghi cảnh báo vào log máy chủ, để thư của các phân hệ khác không bị chặn.

### Quản lý báo sự cố cho Admin — bổ sung 2026-11-08

Lý do: lỗi do người dùng báo đi theo yêu cầu hỗ trợ (FE-10) tới CS rồi Quản lý, nhưng không có đường nào tới Admin, trong khi hệ thống không tự tạo sự cố. Quản lý được **báo** sự cố; nhận và xử lý vẫn chỉ Admin. Không thêm bảng/cột: dùng `technical_incidents.reporter_id` và event `CREATE` hiện có.

| Endpoint | Hợp đồng |
| --- | --- |
| `GET /reported-incidents/components` | Chỉ Manager. Danh mục phần bị ảnh hưởng, giống `GET /components` của Admin |
| `GET /reported-incidents/csrf` | Chỉ Manager. Mã CSRF cho lệnh báo sự cố (`/csrf` chung chỉ dành cho Admin) |
| `POST /reported-incidents` | Chỉ Manager, cần CSRF. Body như `POST /incidents` nhưng không nhận `recurrenceOf` (gửi kèm → 400). Tạo sự cố NEW, chưa người phụ trách, `reporter_id` = Quản lý, ghi event `CREATE`; trả 201 với `allowedActions` rỗng. Mô tả do giao diện điền sẵn từ yêu cầu hỗ trợ (tiêu đề, mô tả, lý do chuyển lên, mã yêu cầu) và Quản lý sửa được |
| `GET /reported-incidents?page,size` | Chỉ Manager. Sự cố có `reporter_id` là Quản lý đang đăng nhập, mới nhất trước; trả trạng thái, mức độ, người phụ trách, mô tả, phần bị ảnh hưởng; `allowedActions` luôn rỗng |

Admin thấy sự cố này trong `GET /incidents` như mọi sự cố khác (người ghi nhận là tên Quản lý) và tự nhận theo FR-027. Quản lý vẫn bị 403 ở mọi endpoint `/incidents/**`. Giao diện: nút "Báo sự cố kỹ thuật cho Admin" ở tab "Xử lý nội bộ" của yêu cầu được chuyển lên; trang `/pages/manager-incidents/` ("Sự cố đã báo") chỉ xem.

Quy ước: ghi thành công trả dữ liệu và phiên bản mới trong envelope hiện có; sai trường 400, chưa đăng nhập 401, sai quyền/chưa đổi mật khẩu 403, phiên bản cũ 409, lỗi nguồn 503. UI ánh xạ thông báo tiếng Việt. Không bật ô chỉnh loại tệp chưa có consumer. API tải tệp phải kiểm giới hạn thực tế phía server.

Sự cố chỉ có tự nhận và trả lại: `POST /incidents/{id}/release` nhận `expectedVersion,reason` (1–2.000 ký tự không trắng), phiếu chưa đóng và đã có người phụ trách. Chỉ người phụ trách hoặc Admin khác khi người phụ trách không còn hoạt động; sai quyền 403, dữ liệu cũ/không còn phù hợp 409. Xóa người phụ trách, chuyển NEW, ghi event trước/sau nguyên tử; giữ diễn biến cũ. API assignment bị bỏ; giữ event assignment cũ chỉ để đọc.

Tạo/nhập tài khoản không nhận `initialPassword`; email được gửi sau lưu qua hàng đợi DB, không lưu mật khẩu rõ. Kết quả gửi là chờ/đã gửi/thất bại, không khẳng định thư đã được đọc. FE-11 chỉ báo cáo mua gói, không sửa gói; không xuất chỉ số hoàn tiền. Các mô tả sức khỏe/cấu hình học thử cũ dưới đây là lịch sử, không yêu cầu triển khai tiếp.

## Cảnh báo chuyển đổi phạm vi — 2026-11-03

Clarifications 2026-11-03 trong [spec.md](../spec.md) là yêu cầu mới. Các endpoint/quyền bên dưới mô tả hợp đồng bản đã triển khai, **chưa được chuyển đổi**: không được dùng quyền Admin đổi vai trò cũ làm tiêu chí nghiệm thu mới, cũng không tự mở toàn bộ operations cho Manager.

- T067–T069 đã chốt theo Clarifications 2026-11-04, xem "Đổi vai trò và thăng chức" bên dưới. `PUT /staff/{id}/role` đã bỏ; mọi mô tả Admin đổi vai trò sau tạo trong phần lịch sử không còn hiệu lực.
- T070–T071 đã định nghĩa và triển khai nhập tài khoản Excel, xem "Nhập tài khoản Excel" bên dưới.
- T072–T073 đã định nghĩa và triển khai hợp đồng xuất Excel sự cố theo FR-021, xem mục bên dưới.
- Health không còn nằm trong phạm vi Admin sẽ nghiệm thu; không xóa endpoint/code chỉ bằng chỉnh tài liệu này. Chính sách và AI tiếp tục hoãn. Các hợp đồng lịch sử được giữ để đối chiếu tác động khi triển khai.

### Đã triển khai theo phạm vi mới — 2026-11-04

Chỉ Admin còn hoạt động; kiểm quyền DB trên mọi request; không đổi schema.

- `GET /api/admin/operations/incidents/export`: cùng bộ lọc `status, severity, component, assignee, fromDate, toDate` với danh sách, không phân trang. 200 trả `.xlsx` tối đa 5.000 dòng theo thứ tự danh sách, đọc trong transaction repeatable-read; 204 khi không có kết quả; 400 bộ lọc sai; 422 vượt giới hạn. Cột: mã sự cố, tiêu đề, mức độ, trạng thái, người phụ trách, ngày ghi nhận (Asia/Ho_Chi_Minh), kết quả xử lý. Kết quả là lần giải quyết cuối, để trống nếu sự cố chưa giải quyết hoặc đã mở lại. Mọi nội dung người dùng nhập là ô văn bản, không thành công thức.
- `GET /api/admin/operations/dashboard/account-statistics?startDate&endDate`: cùng quy tắc kỳ với báo cáo sự cố (mặc định 7 ngày, tối đa 366 ngày). Trả `roles`, `statuses` tại thời điểm truy vấn và `newAccounts` theo từng ngày tạo; không gồm tài khoản đã xóa. 503 khi lỗi nguồn, giao diện không thay bằng 0.
- Menu/dashboard Admin không còn sức khỏe và không gọi `/health`; endpoint, probe và trang `/pages/operations-health/` được giữ nguyên, chưa xóa.

### Thay đổi phạm vi — 2026-11-05

Cần chạy `database/seeds/fe11_operations_schema.sql` trước khi dùng bản này, nếu không danh sách nhân viên và nhật ký sẽ lỗi.

- Bỏ `POST /incidents/{id}/assignment`, `GET /health`. Thêm `POST /incidents/{id}/release` với `expectedVersion, reason`; nhận khi đã có người phụ trách trả 409 "Sự cố đã có người nhận xử lý."
- Mọi API `/api/admin/operations/**` trả 403 nếu tài khoản chưa đổi mật khẩu lần đầu.
- `POST /staff` nhận thêm `appointmentReason`, bắt buộc khi vai trò là Manager. `GET /staff` trả thêm `emailStatus, emailStatusLabel, emailSentAt` từng dòng và `emailCounts`. `POST /staff/import/confirm` trả thêm `pendingEmails`.
- `GET|PUT /settings/attachment-limit` (`maxMegabytes` 1–10, `expectedVersion`, `reason`), `GET /settings/history`: Admin. `GET /api/v1/tickets/attachment-limit` trả `maxBytes` cho người đã đăng nhập.
- Doanh thu bỏ `refunded, refundedCount`, thêm `sourceIntegrated`; giao dịch đã hoàn không có trong `rows` và tệp xuất.

### Doanh thu và báo cáo gia sư — 2026-11-05

Tiền tố `/api/admin/operations/reports/business`, Admin và Manager, chỉ đọc. Không thêm schema; dùng `payment_transactions`, `membership_plans`, `tutors`, `tutor_credentials`.

| Endpoint | Ghi chú |
| --- | --- |
| `GET /revenue?startDate&endDate` | `revenue`, `successCount`, `pendingCount`, `failedCount`, `byPlan`, `daily`, `sourceIntegrated`. Quy tắc kỳ như báo cáo sự cố |
| `GET /revenue/rows`, `GET /tutors/rows` | `headers`, `rows`, `total` cho bảng và PDF; 422 nếu quá 5.000 dòng |
| `GET /revenue/export`, `GET /tutors/export` | `.xlsx`; 204 nếu không có dữ liệu; 422 nếu quá 5.000 dòng |

### Nhập tài khoản Excel — 2026-11-04

Tiền tố `/api/admin/operations/staff/import`, chỉ Admin. Không thêm schema; lệnh ghi cần CSRF.

| Endpoint | Ghi chú |
| --- | --- |
| `GET /template` | Tải tệp mẫu `.xlsx` |
| `POST /preview` | Multipart `file`. Không ghi DB. 200 trả `rows[{rowNumber, fullName, email, phone, role, errors[{field, message}]}]`, `total`, `errorCount`. 400 nếu tệp sai định dạng/sai mẫu, quá 2 MB, quá 100 dòng, rỗng hoặc có công thức |
| `POST /confirm` | Body `rows`. Sinh mật khẩu riêng cho từng người và gửi email sau khi lưu. Kiểm tra lại toàn bộ; còn lỗi thì 409 và không tạo tài khoản nào. 201 trả `created`, `emails`; không trả mật khẩu |

Mọi dòng được tạo trong một transaction, mỗi tài khoản một dòng nhật ký "Tạo nhân viên", `must_change_password=true`.

### Đổi vai trò và thăng chức — 2026-11-04

Tiền tố `/api/admin/operations/role-changes`. Cần bảng `staff_role_change_requests` (`database/seeds/fe11_operations_schema.sql`). Mọi lệnh ghi cần CSRF và kiểm vai trò hiện tại trong DB.

| Endpoint | Quyền | Ghi chú |
| --- | --- | --- |
| `GET /csrf` | Manager | Token cho màn Manager; `/csrf` chung vẫn chỉ dành cho Admin |
| `GET /staff?q&page&size` | Manager | Chỉ Cán bộ học vụ/Chăm sóc khách hàng đang hoạt động; không trả số điện thoại |
| `PUT /staff/{id}/role` | Manager | Body `expectedUpdatedAt, reason`; đổi sang vai trò còn lại trong cặp. 409 nếu dữ liệu cũ hoặc có đề xuất đang chờ |
| `POST /promotions` | Manager | Body `targetUserId, expectedUpdatedAt, reason`. 201; 409 nếu đã có đề xuất chờ |
| `GET /promotions?status&page&size` | Admin: tất cả; Manager: của mình | |
| `POST /promotions/{id}/cancel` | Manager tạo đề xuất | 403 nếu người khác; 409 nếu đã chốt |
| `POST /promotions/{id}/approve` | Admin | Body `note` tùy chọn; 409 nếu nhân viên đã khóa/đổi vai trò |
| `POST /promotions/{id}/reject` | Admin | Body `note` bắt buộc |

Lý do tối đa 1.500 ký tự, ghi chú duyệt tối đa 300 ký tự để nhật ký không vượt cột 2.000 ký tự. `POST /staff` không nhận mật khẩu, gửi mật khẩu tạm qua email và từ chối vai trò Admin; `POST /staff/{id}/resend-credentials` (Admin, body `expectedUpdatedAt`) cấp lại khi nhân viên chưa đổi mật khẩu lần đầu; `staffAssignable` trong `/roles` nghĩa là được gán khi tạo.

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
- `POST /staff`: `{fullName,email,phone?,roleId,appointmentReason?}`. Tên1..100, email hợp lệ tối đa254, phone<=20; Manager bắt buộc căn cứ bổ nhiệm. Không nhận mật khẩu. Tài khoản mới ACTIVE, `must_change_password=true`; gửi mật khẩu tạm riêng qua hàng đợi email sau khi lưu. Không trả mật khẩu; chỉ gán Manager/Cán bộ học vụ/CS còn hoạt động, không cấp Admin.
- `PUT /staff/{id}`: `{fullName,email,phone?,expectedUpdatedAt,reason}`. Lý do1..2000, timestamp phải khớp; email người đang thao tác không đổi để giữ phiên hiện tại. Không sửa trường quyền qua endpoint này.
- `PUT /staff/{id}/role` cũ của Admin đã bỏ. Đổi vai trò chỉ qua hợp đồng `/role-changes` hiện hành; không nhầm với endpoint Manager dưới tiền tố đó.
- `PUT /staff/{id}/status`: `{status:ACTIVE|SUSPENDED,expectedUpdatedAt,reason}`. Khóa/mở tài khoản, không khóa chính mình hoặc quản trị viên cuối; trạng thái khác bị từ chối.
- `GET /staff/history?page&size`: lịch sử thao tác với nhãn Việt, tên người thực hiện, tên đối tượng, lý do, thời gian. Không trả JSON audit thô, mật khẩu hoặc hash.

Ghi account và audit cùng transaction; khóa tuần tự thao tác nhân sự và kiểm quyền hiện tại trong transaction. Email trùng400; phiên dữ liệu cũ409; sai quyền403; ngoài nhân sự/không tồn tại404. Không xóa tài khoản hay lịch sử. Migration riêng `database/seeds/fe11_operations_schema.sql`, chỉ áp dụng DB phát triển hiện tại và DB test sau kiểm tra; production theo quy trình migration của nhóm.

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
