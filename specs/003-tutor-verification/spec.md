# Feature Specification: FE-03 — Tutor Verification (Xác minh thông tin gia sư)

**Feature Branch**: `han` (thư mục đặc tả `specs/003-tutor-verification`)

**Created**: 2026-11-03

**Status**: Draft — đã làm rõ (phiên 2026-11-03)

**Input**: Yêu cầu người dùng: "Triển khai FE-03 – Tutor Verification theo SRS Report 3". Nguồn nghiệp vụ: SRS `Report-3_Software Requirement Specification` — mục I.2.1 (Business Flow Tutor Onboarding & Approval), I.3.2–3.3 (Entity, Data Business Rules), I.4.2 (UC-22…UC-28, UC-82…UC-84), II.3.1–3.10 (Use Case Specifications), III.3.1.1 (Application Review Dashboard), IV.2.3 (Security), V.1 (GB-01, GB-02, GB-03, GB-06), V.2 (MSG01, MSG02, MSG12, MSG13).

## Phạm vi

**Trong phạm vi FE-03** (10 use case thuộc nhóm "Tutor Verification"):

| UC | Tên | Tác nhân | Mục SRS |
| --- | --- | --- | --- |
| UC-22 | Xem biểu mẫu xác minh gia sư | Gia sư | II.3.1 |
| UC-23 | Cập nhật thông tin xác minh | Gia sư | II.3.2 |
| UC-24 | Nộp hồ sơ xác minh | Gia sư | II.3.3 |
| UC-25 | Xem danh sách hồ sơ xác minh | Cán bộ học vụ | II.3.4 |
| UC-26 | Yêu cầu bổ sung thông tin xác minh | Cán bộ học vụ | II.3.5 |
| UC-27 | Thẩm định hồ sơ xác minh | Cán bộ học vụ | II.3.6 |
| UC-28 | Phê duyệt hoặc từ chối hồ sơ | Cán bộ học vụ | II.3.7 |
| UC-82 | Xem trạng thái xác minh | Gia sư | II.3.8 |
| UC-83 | Xem phản hồi xác minh | Gia sư | II.3.9 |
| UC-84 | Xem lịch sử thẩm định | Cán bộ học vụ | II.3.10 |

**Ngoài phạm vi FE-03**: tạo/cập nhật hồ sơ giảng dạy công khai (UC-18, UC-19, UC-81 — thuộc FE-02), mua gói hội viên và duyệt bài đăng (Business Flow I.2.2 — thuộc FE-09), chặn đặt lịch với gia sư chưa xác minh (GB-06 được thực thi tại FE-04/FE-05, FE-03 chỉ cung cấp trạng thái), lên lịch phỏng vấn, video giới thiệu, duyệt theo từng môn học, kiểm tra lý lịch trực tiếp (OOS-03).

## Clarifications

### Session 2026-11-03

- Q: Gia sư bị từ chối có được sửa và nộp lại hồ sơ không? → A: ~~Có~~ **Không (sửa đổi trong cùng ngày, xem bên dưới).**
- Q (sửa đổi, 2026-11-03, người phụ trách quyết định khi nghiệm thu): Nộp lại sau khi bị từ chối? → A: **Không.** "Bị từ chối" là quyết định cuối cùng (đúng UC-28 POST-2 "permanently denied"): hồ sơ khóa vĩnh viễn, gia sư không thể chỉnh sửa hay nộp lại. Chỉ trạng thái "Cần bổ sung thông tin" mới mở khóa để sửa và nộp lại. Cán bộ dùng "Yêu cầu bổ sung" khi hồ sơ chỉ thiếu/sai thông tin.
- Q: Tệp giấy tờ xác minh được lưu ở đâu? → A: Theo đề xuất của nhóm phát triển: lưu trên máy chủ ứng dụng, ngoài thư mục được phục vụ công khai, tên tệp ngẫu nhiên; cơ sở dữ liệu chỉ lưu khóa tham chiếu; chỉ xem/tải qua chức năng có kiểm tra quyền. Việc lưu tệp không đổi cấu trúc dữ liệu (thay đổi schema duy nhất của FE-03 nằm ở bảng lịch sử, xem A-1).
- Q: Gia sư mới đăng ký có được coi là đã xác minh không? → A: Không. Đăng ký chỉ tạo tài khoản gia sư; muốn được xác minh, gia sư phải tự lập và nộp hồ sơ xác minh khi muốn ứng tuyển dạy.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gia sư chuẩn bị và nộp hồ sơ xác minh (Priority: P1)

Gia sư vừa đăng ký mở trang "Xác minh hồ sơ", nhập thông tin xác minh (thông tin cá nhân, kinh nghiệm, trình độ JLPT cao nhất), tải lên các giấy tờ chứng minh (giấy tờ tùy thân, chứng chỉ JLPT, bằng cấp…), lưu nháp nhiều lần rồi xác nhận cam kết và nộp hồ sơ để cán bộ học vụ thẩm định.

**Why this priority**: Là điểm vào của toàn bộ quy trình onboarding (BF I.2.1). Không có hồ sơ được nộp thì không có gì để thẩm định.

**Independent Test**: Đăng nhập bằng một tài khoản gia sư ở trạng thái chưa nộp, nhập đủ thông tin, tải giấy tờ, nộp; kiểm tra trạng thái chuyển sang "Đang chờ thẩm định", biểu mẫu bị khóa và thông báo MSG12 hiển thị.

**Acceptance Scenarios**:

1. **Given** gia sư chưa từng nộp hồ sơ, **When** mở trang xác minh, **Then** hệ thống hiển thị trạng thái "Chưa nộp hồ sơ" và nút "Bắt đầu xác minh" thay cho các ô giấy tờ rỗng (UC-22 AF-1).
2. **Given** hồ sơ ở trạng thái có thể chỉnh sửa, **When** gia sư sửa thông tin hoặc thay tệp và bấm "Lưu thay đổi", **Then** dữ liệu được lưu, hồ sơ vẫn có thể chỉnh sửa và hiển thị thông báo "Đã cập nhật thông tin xác minh. Vui lòng nộp hồ sơ khi đã sẵn sàng." (UC-23 bước 7, POST-2).
3. **Given** gia sư tải lên tệp vượt dung lượng hoặc sai định dạng, **When** lưu, **Then** hệ thống không lưu, đánh dấu trường lỗi và hiển thị MSG13 (UC-23 AF-2).
4. **Given** hồ sơ còn thiếu trường bắt buộc hoặc giấy tờ bắt buộc, **When** bấm "Nộp hồ sơ", **Then** hệ thống hủy thao tác, đánh dấu phần thiếu và thông báo "Không thể nộp hồ sơ. Vui lòng hoàn thiện các trường bắt buộc và tải lên đủ giấy tờ trước khi nộp." (UC-24 AF-1).
5. **Given** hồ sơ đầy đủ, **When** gia sư tích vào ô cam kết và xác nhận nộp, **Then** trạng thái chuyển sang "Đang chờ thẩm định", thời điểm nộp được ghi nhận, biểu mẫu chuyển sang chỉ đọc, một mục lịch sử "Nộp hồ sơ" được ghi và hiển thị MSG12 (UC-24 bước 6–9).
6. **Given** hồ sơ đang chờ thẩm định hoặc đã được phê duyệt, **When** gia sư cố chỉnh sửa hoặc nộp lại, **Then** hệ thống từ chối và hiển thị "Hồ sơ của bạn đang được thẩm định nên chưa thể chỉnh sửa. Vui lòng liên hệ bộ phận hỗ trợ nếu cần thay đổi gấp." (UC-23 AF-1, DBR "One active application at a time").

---

### User Story 2 - Cán bộ học vụ thẩm định và ra quyết định (Priority: P1)

Cán bộ học vụ mở bảng điều khiển thẩm định, xem số liệu tóm tắt và hàng đợi hồ sơ (cũ nhất trước), tìm kiếm/lọc, mở chi tiết một hồ sơ để xem thông tin và giấy tờ, rồi phê duyệt hoặc từ chối kèm lý do.

**Why this priority**: Đây là bước tạo ra giá trị cốt lõi của FE-03: chỉ gia sư đã được phê duyệt mới được nhận học viên (GB-06).

**Independent Test**: Với một hồ sơ "Đang chờ thẩm định" có sẵn, đăng nhập bằng cán bộ học vụ, mở hàng đợi, mở chi tiết, xem giấy tờ, phê duyệt; kiểm tra gia sư chuyển sang "Đã phê duyệt", có mục lịch sử, gia sư nhận email. Lặp lại với từ chối.

**Acceptance Scenarios**:

1. **Given** có hồ sơ đang chờ, **When** cán bộ mở bảng điều khiển, **Then** thấy các thẻ tóm tắt (chờ thẩm định, đã duyệt trong tháng, đã từ chối trong tháng) và bảng hàng đợi gồm tên gia sư, trình độ JLPT, kinh nghiệm, ngày nộp (định dạng dd/MM/yyyy, cũ nhất trước), trạng thái (III.3.1.1).
2. **Given** hàng đợi, **When** cán bộ tìm theo tên hoặc email và/hoặc lọc theo ngày nộp, **Then** bảng chỉ hiện hồ sơ khớp; nếu không có kết quả hiển thị MSG01; nếu hàng đợi rỗng hiển thị "Hiện không còn hồ sơ nào chờ thẩm định." (UC-25 AF-1).
3. **Given** một hồ sơ, **When** cán bộ mở chi tiết, **Then** thấy thông tin cá nhân, giới thiệu, kinh nghiệm, trình độ JLPT và danh sách giấy tờ xem được trực tiếp; giấy tờ không tải được hiển thị "Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ." mà không làm hỏng trang (UC-27 AF-1).
4. **Given** hồ sơ đang chờ, **When** cán bộ chọn "Phê duyệt" và xác nhận, **Then** trạng thái chuyển "Đã phê duyệt", thời điểm duyệt được ghi, gia sư được mở quyền giảng dạy, mục lịch sử "Phê duyệt" được ghi kèm người duyệt, email thông báo gửi đến gia sư (UC-28 bước 6–9, BF I.2.1).
5. **Given** hồ sơ đang chờ, **When** cán bộ chọn "Từ chối" mà chưa chọn lý do hoặc chưa nhập ghi chú nội bộ, **Then** hệ thống không lưu và báo "Vui lòng chọn lý do từ chối và nhập ghi chú trước khi xác nhận." (UC-28 AF-1).
6. **Given** hồ sơ đang chờ, **When** cán bộ từ chối với đủ lý do, **Then** trạng thái chuyển "Bị từ chối", lý do hiển thị cho gia sư được lưu, ghi chú nội bộ chỉ lưu trong lịch sử thẩm định, email từ chối kèm lý do gửi đến gia sư.
7. **Given** hai cán bộ cùng mở một hồ sơ, **When** người thứ hai ra quyết định sau khi người thứ nhất đã xử lý, **Then** hệ thống từ chối thao tác thứ hai và báo "Hồ sơ này vừa được xử lý bởi cán bộ khác. Vui lòng tải lại trang." (GB-01).

---

### User Story 3 - Yêu cầu bổ sung và gia sư nộp lại (Priority: P2)

Khi giấy tờ mờ hoặc thiếu, cán bộ học vụ trả hồ sơ về với hướng dẫn cụ thể. Gia sư xem phản hồi, sửa/bổ sung rồi nộp lại.

**Why this priority**: Giảm từ chối không cần thiết; phụ thuộc US1 và US2.

**Independent Test**: Từ một hồ sơ đang chờ, cán bộ gửi yêu cầu bổ sung; gia sư thấy trạng thái "Cần bổ sung thông tin" với nội dung yêu cầu, biểu mẫu mở khóa; gia sư sửa và nộp lại, hồ sơ quay lại hàng đợi.

**Acceptance Scenarios**:

1. **Given** hồ sơ đang chờ, **When** cán bộ gửi yêu cầu bổ sung với nội dung trống, **Then** hệ thống chặn và báo "Vui lòng nêu rõ thông tin cần bổ sung trước khi gửi yêu cầu." (UC-26 AF-1).
2. **Given** hồ sơ đang chờ, **When** cán bộ gửi yêu cầu bổ sung hợp lệ, **Then** trạng thái chuyển "Cần bổ sung thông tin", biểu mẫu của gia sư được mở khóa, mục lịch sử "Yêu cầu bổ sung" được ghi, gia sư nhận email kèm nội dung yêu cầu và cán bộ thấy "Đã gửi yêu cầu bổ sung thông tin." (UC-26 bước 6–9).
3. **Given** hồ sơ "Cần bổ sung thông tin", **When** gia sư mở phần phản hồi, **Then** thấy nội dung yêu cầu của cán bộ và lối tắt đến biểu mẫu cập nhật (UC-83 AF-3); không thấy ghi chú nội bộ (GB-02).
4. **Given** gia sư đã cập nhật, **When** nộp lại, **Then** hồ sơ trở về "Đang chờ thẩm định" như US1 kịch bản 5.
5. **Given** hồ sơ "Bị từ chối", **When** gia sư mở trang xác minh, **Then** thấy lý do từ chối, biểu mẫu ở chế độ chỉ đọc; mọi thao tác sửa, tải giấy tờ hoặc nộp lại bị từ chối với thông điệp "Hồ sơ của bạn đã bị từ chối nên không thể chỉnh sửa hoặc nộp lại. Vui lòng liên hệ bộ phận hỗ trợ nếu bạn cần trao đổi thêm." và không ghi lịch sử (UC-28 POST-2, quyết định sửa đổi 2026-11-03).

---

### User Story 4 - Gia sư theo dõi trạng thái và phản hồi (Priority: P2)

Gia sư xem trạng thái hiện tại, ngày nộp, bản tóm tắt chỉ đọc của những gì đã nộp và phản hồi (nếu có) để biết bước tiếp theo.

**Why this priority**: Minh bạch tiến độ; phần lớn dữ liệu đã có từ US1–US3.

**Independent Test**: Với từng trạng thái (chưa nộp, chờ thẩm định, cần bổ sung, đã duyệt, bị từ chối) mở trang trạng thái và kiểm tra nhãn tiếng Việt, ngày nộp, phản hồi.

**Acceptance Scenarios**:

1. **Given** bất kỳ trạng thái nào, **When** gia sư mở trang, **Then** nhãn trạng thái hiển thị bằng tiếng Việt theo bảng ánh xạ trạng thái, không hiển thị mã nội bộ (UC-82 bước 3).
2. **Given** chưa có phản hồi, **When** mở phần phản hồi, **Then** hiển thị "Hiện chưa có phản hồi nào cho hồ sơ của bạn." (UC-83 AF-1).
3. **Given** lỗi tải dữ liệu, **When** mở trang, **Then** hiển thị thông báo lỗi thân thiện và nút "Thử lại" (UC-82 AF-1, UC-83 AF-2).

---

### User Story 5 - Cán bộ học vụ xem lịch sử thẩm định (Priority: P3)

Cán bộ xem các hồ sơ đã xử lý gần đây trên bảng điều khiển và toàn bộ lịch sử thẩm định của một gia sư theo thứ tự thời gian (ngày, hành động, phản hồi, ghi chú nội bộ, cán bộ thực hiện).

**Why this priority**: Hỗ trợ tính nhất quán và kiểm toán; không chặn luồng chính.

**Independent Test**: Sau khi một hồ sơ đi qua nộp → yêu cầu bổ sung → nộp lại → phê duyệt, mở lịch sử và kiểm tra 4 mục theo thứ tự thời gian kèm tên cán bộ.

**Acceptance Scenarios**:

1. **Given** có hồ sơ đã xử lý, **When** cán bộ mở bảng điều khiển, **Then** khối "Lịch sử thẩm định gần đây" liệt kê tên gia sư, quyết định (kèm lý do nếu từ chối), ngày, người thẩm định và liên kết "Xem toàn bộ lịch sử" (III.3.1.1).
2. **Given** một hồ sơ, **When** cán bộ mở lịch sử, **Then** các mục sắp xếp theo thời gian, mỗi mục có ngày, hành động, nội dung phản hồi, ghi chú nội bộ và cán bộ thực hiện (UC-84 bước 6).
3. **Given** chưa có lịch sử, **When** mở, **Then** hiển thị "Chưa có lịch sử thẩm định." (UC-84 AF-1).

### Edge Cases

- Gia sư nộp hồ sơ khi đã có hồ sơ đang chờ (gửi trùng/bấm hai lần): hệ thống chỉ ghi nhận một lần nộp (DBR "One active application at a time").
- Cán bộ thao tác với hồ sơ không còn ở trạng thái chờ thẩm định (đã được người khác xử lý): từ chối thao tác, không ghi lịch sử trùng.
- Người dùng không phải gia sư gọi chức năng của gia sư, hoặc không phải cán bộ học vụ gọi chức năng thẩm định: bị từ chối truy cập.
- Gia sư cố mở tệp giấy tờ của gia sư khác bằng cách đoán đường dẫn/mã: bị từ chối (DBR "Verified document access control", GB-03).
- Tài khoản bị khóa hoặc tạm ngưng: không thể nộp hay thẩm định (GB-05).
- Gửi email thất bại: quyết định vẫn được lưu; lỗi gửi email được ghi nhật ký nội bộ, không hoàn tác quyết định.
- Gia sư xóa tệp bắt buộc cuối cùng trong khi đang ở trạng thái có thể chỉnh sửa: được phép lưu nháp nhưng không thể nộp.
- Gia sư được yêu cầu bổ sung nhiều lần: mỗi lần nộp lại là một lượt thẩm định mới, lịch sử giữ đầy đủ các lần trước.
- Gia sư bị từ chối cố gửi request sửa/nộp trực tiếp tới API: máy chủ từ chối (409), không thay đổi dữ liệu.
- Tệp trên máy chủ bị mất trong khi dữ liệu giấy tờ còn: màn hình hiển thị "Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ." (UC-27 AF-1) và gia sư (nếu đang chỉnh sửa) có thể tải lại tệp.

## Requirements *(mandatory)*

### Thông điệp hệ thống dùng trong FE-03

| Mã | Nội dung hiển thị |
| --- | --- |
| MSG01 | Không tìm thấy kết quả phù hợp. Vui lòng thử lại với từ khóa hoặc bộ lọc khác. |
| MSG12 | Đã nộp hồ sơ xác minh thành công. Cán bộ học vụ sẽ thẩm định và phản hồi cho bạn qua email. |
| MSG13 | Tệp không hợp lệ. Vui lòng chọn tệp PDF, PNG hoặc JPG có dung lượng tối đa 10MB. |

Ghi chú: nhóm phát triển tự soạn nội dung ba thông điệp này theo ý nghĩa trong SRS V.2, **chưa đối chiếu nguyên văn SRS**. Khi có văn bản SRS, thay bằng nguyên văn nếu khác.

### Bảng ánh xạ trạng thái xác minh

| Trạng thái trong SRS | Nhãn hiển thị cho người dùng | Có thể chỉnh sửa? | Nằm trong hàng đợi thẩm định? |
| --- | --- | --- | --- |
| Not Started / Draft | Chưa nộp hồ sơ | Có | Không |
| Pending Review | Đang chờ thẩm định | Không | Có |
| Needs More Information | Cần bổ sung thông tin | Có | Không |
| Approved | Đã phê duyệt | Không | Không (thuộc nhóm đã hoàn tất) |
| Rejected | Bị từ chối | Không (quyết định cuối cùng, không nộp lại) | Không (thuộc nhóm đã hoàn tất) |

Chuyển trạng thái hợp lệ (một chiều, GB-01):

- Chưa nộp hồ sơ → Đang chờ thẩm định (gia sư nộp)
- Cần bổ sung thông tin → Đang chờ thẩm định (gia sư nộp lại)
- Đang chờ thẩm định → Cần bổ sung thông tin (cán bộ yêu cầu bổ sung)
- Đang chờ thẩm định → Đã phê duyệt (cán bộ phê duyệt)
- Đang chờ thẩm định → Bị từ chối (cán bộ từ chối; trạng thái cuối, không có chuyển tiếp nào ra khỏi "Bị từ chối")

### Functional Requirements

**Gia sư**

- **FR-001**: Hệ thống PHẢI cho gia sư xem trạng thái xác minh hiện tại, thời điểm nộp gần nhất và bản tóm tắt chỉ đọc của thông tin và giấy tờ đã nộp (UC-22, UC-82).
- **FR-002**: Hệ thống PHẢI cho gia sư cập nhật thông tin xác minh (họ tên hiển thị, số điện thoại, ngày sinh, địa chỉ, giới thiệu ngắn, số năm kinh nghiệm, trình độ JLPT cao nhất) chỉ khi hồ sơ ở trạng thái có thể chỉnh sửa (UC-23).
- **FR-003**: Hệ thống PHẢI cho gia sư thêm, thay thế và gỡ giấy tờ (loại giấy tờ, tiêu đề, tệp) khi hồ sơ ở trạng thái có thể chỉnh sửa (UC-23 bước 3).
- **FR-004**: Hệ thống PHẢI chỉ chấp nhận tệp PDF, PNG, JPG/JPEG, tối đa 10MB mỗi tệp; tệp sai định dạng hoặc quá dung lượng bị từ chối với MSG13 (IV.2.3 Upload validation, MSG13).
- **FR-005**: Hệ thống PHẢI kiểm tra đủ trường bắt buộc và đủ giấy tờ bắt buộc trước khi cho nộp; bộ giấy tờ bắt buộc gồm ít nhất một giấy tờ tùy thân và ít nhất một chứng chỉ năng lực tiếng Nhật (UC-24 bước 3, xem Assumptions A-4).
- **FR-006**: Hệ thống PHẢI yêu cầu gia sư tích xác nhận cam kết "Tôi cam kết mọi thông tin và giấy tờ cung cấp là chính xác và trung thực." trước khi nộp (UC-24 bước 4–5).
- **FR-007**: Khi nộp thành công, hệ thống PHẢI chuyển trạng thái sang "Đang chờ thẩm định", ghi thời điểm nộp, khóa biểu mẫu, ghi một mục lịch sử "Nộp hồ sơ" và hiển thị MSG12 (UC-24 bước 6–9).
- **FR-008**: Hệ thống PHẢI không cho tồn tại quá một hồ sơ đang chờ thẩm định cho mỗi gia sư (DBR "One active application at a time").
- **FR-009**: Hệ thống PHẢI cho gia sư xem phản hồi của cán bộ (lý do từ chối hoặc nội dung yêu cầu bổ sung) và PHẢI không bao giờ hiển thị ghi chú nội bộ của cán bộ cho gia sư (UC-83, GB-02).

**Cán bộ học vụ**

- **FR-010**: Hệ thống PHẢI hiển thị bảng điều khiển thẩm định với số hồ sơ đang chờ (kèm số hồ sơ mới trong ngày), số hồ sơ được phê duyệt trong tháng hiện tại và số hồ sơ bị từ chối trong tháng hiện tại (III.3.1.1 Summary cards, xem Assumptions A-2).
- **FR-011**: Hệ thống PHẢI hiển thị hàng đợi hồ sơ theo nhóm "Đang chờ thẩm định", "Cần bổ sung thông tin" và "Đã hoàn tất" (đã duyệt/bị từ chối), có phân trang, mặc định sắp xếp ngày nộp cũ nhất trước (UC-25, III.3.1.1).
- **FR-012**: Hệ thống PHẢI cho tìm kiếm theo tên hoặc email gia sư (không phân biệt hoa thường, khớp một phần, tối đa 100 ký tự) và lọc theo khoảng ngày nộp; không có kết quả thì hiển thị MSG01 (UC-25 bước 4–5).
- **FR-013**: Hệ thống PHẢI cho cán bộ xem chi tiết hồ sơ gồm thông tin cá nhân, giới thiệu, kinh nghiệm, trình độ JLPT và từng giấy tờ, xem trực tiếp được PDF/ảnh (UC-27).
- **FR-014**: Hệ thống PHẢI cho cán bộ gửi yêu cầu bổ sung với nội dung bắt buộc (1–2.000 ký tự) cho hồ sơ đang chờ; chuyển trạng thái "Cần bổ sung thông tin", ghi lịch sử, gửi email cho gia sư (UC-26).
- **FR-015**: Hệ thống PHẢI cho cán bộ phê duyệt hồ sơ đang chờ; chuyển "Đã phê duyệt", ghi thời điểm duyệt, ghi lịch sử kèm cán bộ, gửi email chào mừng (UC-28, BF I.2.1).
- **FR-016**: Hệ thống PHẢI cho cán bộ từ chối hồ sơ đang chờ với lý do bắt buộc chọn từ danh sách ("Chưa đủ trình độ chuyên môn", "Giấy tờ không hợp lệ hoặc có dấu hiệu giả mạo", "Thông tin không trùng khớp với giấy tờ", "Lý do khác"), ghi chú nội bộ bắt buộc (1–2.000 ký tự) và nội dung phản hồi gửi gia sư không bắt buộc (tối đa 2.000 ký tự; để trống thì gia sư nhận nhãn lý do); chuyển "Bị từ chối", ghi lịch sử, gửi email kèm lý do (UC-28 bước 4, AF-1).
- **FR-017**: Hệ thống PHẢI từ chối mọi quyết định trên hồ sơ không còn ở trạng thái "Đang chờ thẩm định" và PHẢI đảm bảo hai quyết định đồng thời không cùng thành công (GB-01).
- **FR-018**: Hệ thống PHẢI cho cán bộ xem các quyết định gần đây và toàn bộ lịch sử thẩm định của một hồ sơ theo thứ tự thời gian, gồm ngày, hành động, phản hồi, ghi chú nội bộ và cán bộ thực hiện (UC-84, III.3.1.1).

**Bảo mật và chung**

- **FR-019**: Chỉ gia sư sở hữu hồ sơ và cán bộ học vụ mới được xem tệp giấy tờ; tệp không được truy cập qua đường dẫn công khai hay đoán được; mọi lần tải tệp đều kiểm tra quyền (DBR "Verified document access control", GB-03, IV.2.3). Ghi chú: SRS GB-03 có nhắc "Administrators", xem Assumptions A-7.
- **FR-020**: Mọi chức năng của FE-03 PHẢI yêu cầu đăng nhập, PHẢI kiểm tra vai trò và trạng thái tài khoản hiện tại ở phía máy chủ; tài khoản không hoạt động bị từ chối (IV.2.3 Authentication/RBAC, GB-05).
- **FR-021**: Mọi nhãn, thông báo, trạng thái trên giao diện PHẢI bằng tiếng Việt, không in hoa toàn bộ, không lộ mã nội bộ; màn hình PHẢI có trạng thái đang tải, rỗng và lỗi (Constitution I, II).
- **FR-022**: Gửi email thất bại PHẢI không hoàn tác quyết định đã lưu và PHẢI được ghi nhật ký nội bộ.
- **FR-023**: Đăng ký chỉ tạo tài khoản gia sư. Gia sư mới PHẢI ở trạng thái "Chưa nộp hồ sơ" cho đến khi tự lập và nộp hồ sơ xác minh, và chỉ trở thành "Đã phê duyệt" thông qua quyết định của cán bộ học vụ. Việc đăng ký gán đúng trạng thái ban đầu thuộc FE-01 (xem A-11).
- **FR-024**: Tệp giấy tờ PHẢI được lưu trên máy chủ ứng dụng, ngoài mọi thư mục được phục vụ công khai, với tên tệp ngẫu nhiên không đoán được; dữ liệu giấy tờ chỉ lưu khóa tham chiếu đến tệp; tệp chỉ được xem/tải qua chức năng có kiểm tra quyền theo FR-019. Gỡ hoặc thay giấy tờ khi hồ sơ đang chỉnh sửa PHẢI xóa tệp cũ khỏi nơi lưu.

### Key Entities *(include if feature involves data)*

- **Hồ sơ xác minh gia sư**: gắn 1-1 với gia sư; gồm trạng thái xác minh, thông tin xác minh (số điện thoại, ngày sinh, địa chỉ, giới thiệu, kinh nghiệm, trình độ JLPT cao nhất), phản hồi mới nhất dành cho gia sư, thời điểm nộp, thời điểm duyệt. Theo SRS là "TutorApplication"; trong dữ liệu hiện có được biểu diễn trên hồ sơ gia sư.
- **Giấy tờ xác minh**: thuộc một gia sư; gồm loại giấy tờ, tiêu đề, tệp, trạng thái giấy tờ (chờ/đã xác minh/từ chối), lý do từ chối, thời điểm tải lên. Theo SRS là "TutorApplicationDocument".
- **Mục lịch sử thẩm định**: thuộc một gia sư; gồm hành động (nộp, phê duyệt, từ chối, yêu cầu bổ sung), nội dung phản hồi gửi gia sư tại thời điểm đó, lý do từ chối (nếu có), ghi chú nội bộ, cán bộ thực hiện (nếu có), thời điểm. Chỉ ghi thêm, không sửa/xóa.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Gia sư hoàn tất nhập thông tin, tải giấy tờ và nộp hồ sơ trong dưới 10 phút ở lần đầu, không cần hướng dẫn.
- **SC-002**: 100% hồ sơ được nộp xuất hiện trong hàng đợi của cán bộ học vụ ngay ở lần tải trang kế tiếp.
- **SC-003**: Cán bộ học vụ mở bảng điều khiển và chi tiết hồ sơ trong vòng 2 giây cho 95% lượt tải, với tối đa 1.000 gia sư và 100 người dùng đồng thời (IV.2.2, IV.2.4).
- **SC-004**: 0 trường hợp gia sư hoặc người dùng khác xem được giấy tờ hay ghi chú nội bộ không thuộc quyền của mình trong bộ kiểm thử phân quyền.
- **SC-005**: 0 trường hợp một hồ sơ có hai quyết định thẩm định cho cùng một lần nộp khi hai cán bộ thao tác đồng thời.
- **SC-006**: 100% thay đổi trạng thái có đúng một mục lịch sử tương ứng.
- **SC-007**: 0 chuỗi giao diện in hoa toàn bộ hoặc lộ mã nội bộ trên các màn hình FE-03.

## Assumptions

- **A-1 (Dữ liệu)**: Dùng cấu trúc dữ liệu hiện có (hồ sơ gia sư mang trạng thái xác minh; bảng giấy tờ gia sư; bảng lịch sử thẩm định). Mỗi gia sư có đúng một hồ sơ xác minh nên "một hồ sơ đang hoạt động" được đảm bảo tự nhiên; không tạo bảng "TutorApplication" riêng như mô hình khái niệm SRS. Ngoại lệ duy nhất, đã được nhóm duyệt ngày 2026-11-03: thêm hai cột cho phép rỗng (phản hồi, mã lý do từ chối) vào bảng lịch sử thẩm định để tách phản hồi gửi gia sư khỏi ghi chú nội bộ (FR-018, GB-02); không xóa hay đổi kiểu cột nào.
- **A-2 (Trạng thái "Đang thẩm định")**: SRS III.3.1.1 có thẻ/tab "In Review" kèm cán bộ phụ trách, nhưng dữ liệu hiện có không có trạng thái này hay trường cán bộ được giao; FE-03 không triển khai bước nhận việc/giao việc, thẻ "In Review" được thay bằng nhóm "Cần bổ sung thông tin".
- **A-3 (Mã hồ sơ)**: Không có mã dạng TA-xxx trong dữ liệu; tìm kiếm theo tên/email gia sư thay cho mã hồ sơ.
- **A-4 (Giấy tờ bắt buộc)**: SRS không liệt kê bộ giấy tờ bắt buộc; giả định tối thiểu một giấy tờ tùy thân và một chứng chỉ năng lực tiếng Nhật (JLPT/BJT…). Loại giấy tờ: Giấy tờ tùy thân, Chứng chỉ JLPT, Chứng chỉ tiếng Nhật khác, Bằng cấp, Chứng chỉ sư phạm, Khác.
- **A-5 (Duyệt theo môn)**: Ví dụ "duyệt Toán, không duyệt Vật lý" của UC-28 không áp dụng cho nền tảng chỉ dạy tiếng Nhật và không có dữ liệu môn học; FE-03 duyệt toàn bộ hồ sơ.
- **A-6 (Giới hạn tệp)**: Áp dụng 10MB theo IV.2.3 thay cho ví dụ 5MB trong UC-23 AF-2; định dạng PDF/PNG/JPG.
- **A-7 (Quản trị viên)**: GB-03 cho phép "Administrators" xem giấy tờ nhưng ma trận phân quyền chỉ trao quyền thẩm định cho cán bộ học vụ; FE-03 chỉ cấp quyền xem tệp cho gia sư chủ hồ sơ và cán bộ học vụ.
- **A-8 (Thông báo)**: Thông báo kết quả gửi qua email (Google Mail Service theo use case diagram). Thông báo trong ứng dụng cho cán bộ khi có hồ sơ mới được thay bằng hàng đợi trên bảng điều khiển vì phân hệ thông báo (FE-12) chưa có dịch vụ.
- **A-9 (Quyền giảng dạy)**: "Mở quyền giảng dạy" sau phê duyệt được hiểu là trạng thái xác minh "Đã phê duyệt" mà các feature tìm kiếm/đặt lịch đang đọc; việc kích hoạt gói hội viên thuộc FE-09.
- **A-10 (Phản hồi gần nhất)**: Gia sư chỉ thấy phản hồi gần nhất (lý do từ chối hoặc nội dung yêu cầu bổ sung); toàn bộ lịch sử chỉ dành cho cán bộ học vụ.
- **A-11 (Phụ thuộc FE-01)**: Chức năng đăng ký hiện gán sẵn "Đã phê duyệt" cho gia sư mới, trái với FR-023. FE-03 không sửa mã đăng ký; người phụ trách FE-01 cần đổi trạng thái ban đầu thành "Chưa nộp hồ sơ". Trước khi sửa, các tài khoản gia sư được tạo theo cách cũ vẫn hiện là "Đã phê duyệt" trên FE-03.
