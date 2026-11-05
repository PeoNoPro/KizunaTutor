# Feature Specification: FE-09 — Membership & Payment Management (Quản lý gói hội viên và thanh toán)

**Feature Branch**: `han` (làm trên nhánh thành viên, không tạo nhánh mới)

**Created**: 2026-11-04

**Status**: Draft

**Input**: SRS `Report-3_Software Requirement Specification.docx` — Business Flow I.2.2 (Membership Purchase & Posting), I.2.4 (Class Management, điều kiện "membership still active"), UC-44, UC-45, UC-97, UC-98, UC-99, UC-100, UC-104 (II.9.1–9.7), API-EXT-01, API-EXT-02, Data Business Rules, GB-01, GB-02, GB-05, GB-06, NFR 2.2–2.4.

## Phạm vi

**Trong phạm vi**

| UC | Tên | Actor | Mục SRS |
| --- | --- | --- | --- |
| UC-104 | Payment Membership — chọn gói, thanh toán, kích hoạt | Gia sư, VNPay | II.9.7 |
| UC-97 | View Membership Expiry Date | Gia sư | II.9.1 |
| UC-99 | Configure Membership Pricing | Quản lý vận hành | II.9.3 |
| UC-100 | Manage Membership Benefits | Quản lý vận hành | II.9.4 |
| UC-44 | View Tutor Membership List | Quản lý vận hành | II.9.5 |
| UC-45 | View Tutor Membership Details | Quản lý vận hành | II.9.6 |
| UC-98 | Handle Payment Issues | Quản trị viên | II.9.2 |
| (chưa có UC) | Đăng bài và duyệt bài sau khi kích hoạt gói (BF I.2.2) | Gia sư, Cán bộ học vụ | Xem [srs-gaps.md](srs-gaps.md) G-01 |

**Ngoài phạm vi**

- II.9.8–II.9.16 (bài tập, tài liệu, báo cáo tiến độ): xếp nhầm vào chương 9, thuộc FE-06/FE-07.
- Ví điện tử, số dư, ký quỹ (escrow), hoàn tiền, trả lương gia sư (AGENTS luật 8; không có UC hoàn tiền).
- SePay/VietQR (NFR, ASM-05, OOS-09): nhóm chốt dùng VNPay theo UC-104, actor VNPAY và AGENTS luật 8.
- Khóa lớp học chỉ đọc khi hết hạn (BF I.2.4): FE-06 tự kiểm tra, FE-09 chỉ cung cấp trạng thái gói.
- Thông báo trong ứng dụng (FE-12); FE-09 chỉ gửi email.

## Clarifications

### Session 2026-11-04

- Q: Nhận kết quả thanh toán bằng cách nào? → A: Phương án C — IPN của VNPay là nguồn chính; khi gia sư quay về trang kết quả mà giao dịch còn chờ, hệ thống chủ động tra cứu cổng (querydr). Cả hai đường dùng chung một bước xử lý chỉ chạy một lần.
- Q: Phần đăng bài/duyệt bài của BF I.2.2 có thuộc FE-09? → A: Có. SRS chưa có UC và DB chưa có bảng — ghi vào srs-gaps.md G-01 để bổ sung; phần này chờ UC và phê duyệt schema.
- Q: Mua gói khi gói hiện tại còn hiệu lực? → A: Cộng nối: gói mới bắt đầu ngay ngày sau ngày hết hạn của gói đang có (srs-gaps G-03).
- Q: Gói hết hạn được cập nhật thế nào? → A: Không có background job; hệ thống cập nhật trạng thái ngay khi đọc dữ liệu (srs-gaps G-04).
- Q: Phân loại gói? → A: Gói chỉ khác nhau theo số tháng và giá; không có cấp bậc. Gán sẵn PREMIUM khi đăng ký thuộc FE-01, người phụ trách sẽ báo lại.
- Q: Quản lý vận hành được tạo/ngừng bán gói? → A: Có.
- Q: UC-98 gồm những thao tác nào? → A: Xem giao dịch và nhật ký, kiểm tra trạng thái với cổng và đồng bộ, ghi kết quả điều tra. Không hoàn tiền.
- Q: Định dạng phản hồi API? → A: Theo BACKEND_RULES mục 6 (`success`, `message`, `data`, `timestamp`; lỗi qua `GlobalExceptionHandler`). Được thêm vào file dùng chung nhưng tuyệt đối không sửa code của người khác.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gia sư mua gói hội viên qua VNPay (Priority: P1)

Gia sư đã được phê duyệt hồ sơ mở trang "Gói hội viên", xem các gói đang mở bán (tên, số tháng, giá, quyền lợi), chọn một gói, xem lại số tiền và xác nhận. Hệ thống tạo giao dịch chờ thanh toán và chuyển gia sư sang cổng VNPay. Sau khi thanh toán, gia sư được đưa về trang kết quả: thành công thì thấy gói và ngày hết hạn; thất bại thì được thanh toán lại.

**Why this priority**: Đây là nguồn doanh thu duy nhất của nền tảng và là điều kiện để gia sư được mở khóa giảng dạy (BF I.2.2).

**Independent Test**: Dùng tài khoản gia sư đã phê duyệt và thẻ thử nghiệm VNPay sandbox; kiểm tra giao dịch, bản ghi hội viên và trạng thái gia sư sau khi thanh toán.

**Acceptance Scenarios**:

1. **Given** gia sư đã phê duyệt, **When** mở trang gói hội viên, **Then** thấy các gói đang mở bán với số tháng, giá (định dạng `199.000 đ`) và danh sách quyền lợi.
2. **Given** gia sư chọn một gói, **When** xác nhận, **Then** hệ thống tạo giao dịch ở trạng thái chờ với số tiền bằng giá gói tại thời điểm đó và chuyển sang trang thanh toán của cổng.
3. **Given** gia sư thanh toán thành công, **When** cổng báo kết quả, **Then** giao dịch thành công, gói được kích hoạt với ngày bắt đầu, ngày hết hạn, gia sư chuyển sang "Đang hoạt động" và nhận email xác nhận.
4. **Given** gia sư hủy hoặc thanh toán thất bại, **When** quay về trang kết quả, **Then** thấy "Thanh toán chưa thành công" và nút thanh toán lại; không có gói nào được kích hoạt.
5. **Given** cổng gửi kết quả nhiều lần cho cùng một giao dịch, hoặc vừa có IPN vừa có lượt tra cứu khi quay về, **When** hệ thống xử lý, **Then** chỉ đúng một bản ghi hội viên được tạo và gia sư không bị thu thêm.
6. **Given** thanh toán thành công nhưng bước kích hoạt gặp lỗi, **When** cổng gửi lại kết quả hoặc quản trị viên đồng bộ, **Then** gói được kích hoạt mà không tạo giao dịch thu tiền mới (UC-104 AF-1).
7. **Given** gia sư chưa được phê duyệt hoặc tài khoản bị khóa, **When** cố mua gói, **Then** bị chặn với thông điệp phù hợp.
8. **Given** gia sư đang có gói còn hiệu lực đến ngày D, **When** mua thêm gói N tháng, **Then** gói mới bắt đầu ngày D+1 và ngày hết hạn chung được kéo dài.

---

### User Story 2 - Gia sư xem gói và ngày hết hạn (Priority: P1)

Gia sư xem gói đang dùng, trạng thái, ngày bắt đầu, ngày hết hạn, các gói đã gia hạn nối tiếp và lịch sử giao dịch của mình.

**Why this priority**: UC-97 là điều kiện để gia sư gia hạn đúng lúc; đi kèm US1 thành MVP.

**Independent Test**: Kiểm tra với ba tài khoản: chưa từng mua, đang có gói, gói đã hết hạn.

**Acceptance Scenarios**:

1. **Given** gia sư có gói hiệu lực, **When** mở trang, **Then** thấy tên gói, trạng thái "Đang hiệu lực", ngày bắt đầu, ngày hết hạn (`dd/MM/yyyy`) và số ngày còn lại.
2. **Given** gia sư chưa từng mua, **When** mở trang, **Then** thấy "Bạn chưa có gói hội viên" và các gói để đăng ký (UC-97 AF-1).
3. **Given** ngày hết hạn đã qua, **When** mở trang, **Then** gói hiển thị "Đã hết hạn" và trạng thái được cập nhật ngay lúc đó (UC-97 AF-2).
4. **Given** không tải được dữ liệu, **When** mở trang, **Then** thấy thông báo lỗi thân thiện và nút "Thử lại" (UC-97 AF-3).

---

### User Story 3 - Quản lý vận hành cấu hình gói, giá và quyền lợi (Priority: P2)

Quản lý vận hành xem danh sách gói, tạo gói mới, đổi giá, sửa quyền lợi, ngừng hoặc mở lại việc bán gói.

**Why this priority**: Gói phải có giá đúng trước khi bán; dữ liệu gói đã có sẵn trong DB nên US1 chạy được trước US3.

**Independent Test**: Đổi giá một gói rồi mở trang mua gói của gia sư để thấy giá mới; giao dịch cũ giữ giá cũ.

**Acceptance Scenarios**:

1. **Given** quản lý mở "Quản lý gói hội viên", **When** trang tải xong, **Then** thấy mọi gói với tên, số tháng, giá, số quyền lợi, trạng thái mở bán.
2. **Given** quản lý nhập giá mới hợp lệ, **When** lưu, **Then** giá mới áp dụng cho lần mua sau; giao dịch và gói đã mua giữ nguyên số tiền (UC-99 POST-2).
3. **Given** giá không hợp lệ (trống, ≤ 0, vượt giới hạn), **When** lưu, **Then** bị chặn, trường lỗi được đánh dấu, giá cũ giữ nguyên (UC-99 AF-1).
4. **Given** quản lý thêm/sửa/xóa quyền lợi, **When** lưu, **Then** danh sách quyền lợi mới hiển thị cho gia sư (UC-100).
5. **Given** quản lý ngừng bán một gói, **When** gia sư mở trang mua, **Then** không thấy gói đó; gói đã mua vẫn hiệu lực đến hết hạn.
6. **Given** quản lý tạo gói mới đủ thông tin, **When** lưu, **Then** gói xuất hiện trong danh sách.

---

### User Story 4 - Quản lý vận hành xem danh sách và chi tiết hội viên (Priority: P2)

Quản lý vận hành xem danh sách gia sư đã đăng ký gói trả phí, tìm theo tên/email, lọc theo gói và trạng thái, mở chi tiết để xem các lần đăng ký và lịch sử thanh toán.

**Why this priority**: UC-44, UC-45 phục vụ giám sát; chỉ đọc.

**Independent Test**: Sau US1, gia sư vừa mua xuất hiện trong danh sách với gói và ngày hết hạn đúng.

**Acceptance Scenarios**:

1. **Given** có gia sư đã mua gói, **When** quản lý mở danh sách, **Then** mỗi dòng có tên gia sư, gói, ngày đăng ký, trạng thái, ngày hết hạn; phân trang mặc định 20, tối đa 50.
2. **Given** bộ lọc không khớp, **When** tìm, **Then** thấy MSG01 và có thể đổi tiêu chí (UC-44 AF-2).
3. **Given** quản lý mở chi tiết, **When** trang tải xong, **Then** thấy thông tin gia sư, các lần đăng ký (ngày đăng ký, bắt đầu, hết hạn, trạng thái, số tiền) và lịch sử thanh toán.
4. **Given** lịch sử thanh toán không tải được, **When** mở chi tiết, **Then** vẫn thấy thông tin gói và thông báo phần lịch sử không khả dụng (UC-45 AF-1).

---

### User Story 5 - Quản trị viên xử lý sự cố thanh toán (Priority: P3)

Quản trị viên xem danh sách giao dịch, lọc theo trạng thái/ngày/từ khóa, mở chi tiết để xem nhật ký trao đổi với cổng, kiểm tra trạng thái với cổng để đồng bộ, và ghi lại kết quả điều tra.

**Why this priority**: Cần cho vận hành nhưng không chặn luồng mua gói.

**Independent Test**: Tạo một giao dịch rồi bỏ dở thanh toán; quản trị viên kiểm tra với cổng và thấy giao dịch được đồng bộ.

**Acceptance Scenarios**:

1. **Given** quản trị viên mở "Quản lý thanh toán", **When** trang tải xong, **Then** thấy giao dịch với mã giao dịch, gia sư, gói, số tiền, trạng thái, thời gian.
2. **Given** giao dịch đang chờ, **When** bấm "Kiểm tra với cổng thanh toán", **Then** hệ thống tra cứu cổng; nếu cổng xác nhận thành công thì giao dịch thành công và gói được kích hoạt; nếu thất bại thì giao dịch thất bại; kết quả và lần tra cứu được ghi nhật ký.
3. **Given** giao dịch đã thành công hoặc thất bại, **When** tra cứu, **Then** trạng thái không bị đổi ngược (GB-01); chỉ ghi nhật ký.
4. **Given** quản trị viên nhập ghi chú điều tra, **When** lưu, **Then** ghi chú được lưu kèm tên người ghi và thời gian; không hiển thị cho gia sư (GB-02).
5. **Given** không tìm thấy giao dịch theo từ khóa, **When** tìm, **Then** thấy MSG01 và có thể tìm theo tiêu chí khác (UC-98 AF-1).

---

### User Story 6 - Gia sư đăng bài sau khi kích hoạt gói; cán bộ học vụ duyệt bài (Priority: P3, chờ UC và schema)

Theo BF I.2.2: sau khi gói được kích hoạt, gia sư tạo bài đăng; bài gửi cán bộ học vụ duyệt; được duyệt thì bài được công bố; bị từ chối thì gia sư nhận thông báo kèm lý do và sửa, gửi lại.

**Why this priority**: Có trong luồng nghiệp vụ nhưng SRS chưa có UC, màn hình, trường dữ liệu; DB chưa có bảng. Chỉ triển khai sau khi bổ sung UC (srs-gaps G-01) và duyệt schema (data-model mục 6).

**Independent Test**: Gia sư có gói hiệu lực tạo bài → cán bộ duyệt → bài hiển thị công khai; gói hết hạn → bài bị ẩn.

**Acceptance Scenarios** (đề xuất, chờ xác nhận trong UC mới):

1. **Given** gia sư có gói hiệu lực, **When** tạo bài đủ tiêu đề và nội dung, **Then** bài ở trạng thái "Chờ duyệt".
2. **Given** gia sư chưa có gói hiệu lực, **When** tạo bài, **Then** bị chặn và được dẫn tới trang mua gói.
3. **Given** cán bộ duyệt, **When** phê duyệt, **Then** bài "Đã công bố"; **When** từ chối kèm lý do, **Then** bài "Bị từ chối", gia sư thấy lý do và sửa, gửi lại.
4. **Given** gói hết hạn, **When** người dùng tìm bài, **Then** bài của gia sư đó không còn hiển thị công khai.

---

### Edge Cases

- Chữ ký HMAC-SHA512 sai, thiếu tham số, mã đơn không tồn tại, số tiền không khớp: không đổi dữ liệu, ghi nhật ký, trả mã phản hồi chuẩn của VNPay (API-EXT-02).
- IPN đến sau khi lượt tra cứu ở trang kết quả đã xử lý xong (hoặc ngược lại): bước thứ hai không làm gì thêm.
- Gia sư mở hai tab và tạo hai giao dịch cho hai gói; cả hai thành công: hai gói được cộng nối theo thứ tự thời điểm xử lý.
- Gia sư không quay về trang kết quả (đóng trình duyệt): IPN vẫn kích hoạt gói; nếu chạy cục bộ không nhận được IPN, giao dịch chờ được đồng bộ khi gia sư mở lại trang hoặc quản trị viên kiểm tra với cổng.
- Giao dịch chờ quá thời hạn thanh toán (15 phút) mà cổng chưa báo: hiển thị "Chưa hoàn tất thanh toán"; trạng thái chỉ đổi khi có kết quả từ cổng.
- Quản lý đổi giá trong lúc gia sư đang thanh toán: giao dịch giữ giá tại lúc tạo.
- Quản lý ngừng bán gói trong lúc có giao dịch chờ: giao dịch đó vẫn được kích hoạt nếu thanh toán thành công.
- Cấu hình VNPay chưa có (thiếu mã website hoặc khóa bí mật): không tạo giao dịch, báo "Cổng thanh toán đang tạm thời không khả dụng".
- Cổng tra cứu không phản hồi hoặc lỗi mạng: giữ nguyên trạng thái chờ, ghi nhật ký, báo người dùng thử lại sau.
- Gia sư bị khóa tài khoản sau khi đã thanh toán: IPN vẫn ghi nhận và kích hoạt gói (tiền đã thu), quyền truy cập do GB-05 quyết định.

## Requirements *(mandatory)*

### Thông điệp hệ thống dùng trong FE-09

SRS chưa có thông điệp riêng cho thanh toán; các mã MSG31–MSG37 là đề xuất, ghi trong srs-gaps G-05 để bổ sung vào SRS V.2.

| Mã | Loại | Ngữ cảnh | Nội dung hiển thị |
| --- | --- | --- | --- |
| MSG01 | Inline | Không có kết quả tìm kiếm | Không tìm thấy kết quả phù hợp. Vui lòng điều chỉnh bộ lọc. |
| MSG02 | Dưới trường | Trường bắt buộc trống | Vui lòng nhập {tên trường}. |
| MSG04 | Toast | Cập nhật thành công | Cập nhật thành công. |
| MSG31 | Toast/Trang kết quả | Thanh toán và kích hoạt thành công | Thanh toán thành công. Gói {tên gói} có hiệu lực đến {dd/MM/yyyy}. |
| MSG32 | Trang kết quả | Thanh toán thất bại hoặc bị hủy | Thanh toán chưa thành công. Bạn có thể thử thanh toán lại. |
| MSG33 | Trang kết quả | Cổng chưa xác nhận kết quả | Giao dịch đang được xác nhận với cổng thanh toán. Vui lòng kiểm tra lại sau ít phút. |
| MSG34 | Inline | Gia sư chưa được phê duyệt hồ sơ | Bạn cần được phê duyệt hồ sơ gia sư trước khi đăng ký gói hội viên. |
| MSG35 | Inline | Gói đã ngừng bán | Gói hội viên này hiện không còn mở bán. |
| MSG36 | Inline | Giá/quyền lợi không hợp lệ | Giá gói phải từ 10.000 đ đến 100.000.000 đ. |
| MSG37 | Inline | Cổng thanh toán không khả dụng | Cổng thanh toán đang tạm thời không khả dụng. Vui lòng thử lại sau. |

### Bảng ánh xạ trạng thái

| Mã | Hiển thị | Dùng cho |
| --- | --- | --- |
| `PENDING` (giao dịch) | Chờ thanh toán / Chưa hoàn tất thanh toán (quá 15 phút) | Giao dịch |
| `SUCCESS` | Thành công | Giao dịch |
| `FAILED` | Thất bại | Giao dịch |
| `ACTIVE` (hội viên, đã bắt đầu) | Đang hiệu lực | Hội viên |
| `ACTIVE` (hội viên, ngày bắt đầu ở tương lai) | Đã gia hạn, bắt đầu từ {dd/MM/yyyy} | Hội viên |
| `EXPIRED` | Đã hết hạn | Hội viên |
| `CANCELLED` | Đã hủy (không phát sinh trong FE-09) | Hội viên |
| Gói `is_active = true/false` | Đang mở bán / Ngừng bán | Gói |

### Functional Requirements

**Mua gói (UC-104)**

- **FR-001**: Hệ thống MUST cho gia sư xem các gói đang mở bán với tên, số tháng, giá VND, quyền lợi.
- **FR-002**: Chỉ gia sư có hồ sơ xác minh "Đã phê duyệt" và tài khoản đang hoạt động MUST được tạo giao dịch mua gói (BF I.2.2, GB-05, GB-06); trường hợp khác báo MSG34 hoặc chặn quyền.
- **FR-003**: Khi gia sư xác nhận, hệ thống MUST tạo giao dịch trạng thái chờ, lưu số tiền bằng giá gói tại thời điểm tạo, rồi trả đường dẫn thanh toán có chữ ký HMAC-SHA512 và hạn thanh toán 15 phút (API-EXT-01).
- **FR-004**: Hệ thống MUST nhận kết quả từ cổng qua IPN (API-EXT-02) và kiểm tra lần lượt: chữ ký, mã đơn tồn tại, số tiền khớp, giao dịch còn chờ; mỗi trường hợp trả đúng mã phản hồi chuẩn của cổng.
- **FR-005**: Khi gia sư quay về trang kết quả, hệ thống MUST kiểm tra chữ ký tham số trả về; nếu giao dịch còn chờ, MUST tra cứu cổng (querydr) và xử lý kết quả như IPN.
- **FR-006**: Bước xử lý kết quả MUST idempotent: giao dịch đã thành công hoặc thất bại không bị xử lý lại; mọi lần gọi lặp chỉ được ghi nhật ký (UC-104 AF-1, GB-01).
- **FR-007**: Kết quả thành công MUST, trong cùng một giao dịch dữ liệu: đặt giao dịch thành công, lưu mã giao dịch phía cổng, tạo bản ghi hội viên `ACTIVE` với số tiền và ngày bắt đầu/hết hạn, cập nhật trạng thái gia sư sang "Đang hoạt động". Lỗi ở bất kỳ bước nào làm hủy cả khối và giao dịch vẫn ở trạng thái chờ để xử lý lại.
- **FR-008**: Ngày bắt đầu = hôm nay (múi giờ Asia/Ho_Chi_Minh) nếu không có gói hiệu lực; ngược lại = ngày sau ngày hết hạn xa nhất của các gói `ACTIVE`. Ngày hết hạn = ngày bắt đầu + số tháng của gói − 1 ngày.
- **FR-009**: Kết quả thất bại hoặc bị hủy MUST đặt giao dịch thất bại; gia sư có thể tạo giao dịch mới để thanh toán lại.
- **FR-010**: Sau khi kích hoạt thành công, hệ thống MUST gửi email xác nhận (actor Google Mail Service); lỗi gửi email không được làm hỏng việc kích hoạt.
- **FR-011**: Mọi lần trao đổi với cổng (tạo đường dẫn, IPN, kết quả trả về, tra cứu) MUST được ghi vào nhật ký xử lý thanh toán, không ghi khóa bí mật.

**Xem gói (UC-97)**

- **FR-012**: Gia sư MUST xem được gói hiện tại, trạng thái, ngày bắt đầu, ngày hết hạn, số ngày còn lại, các gói đã gia hạn nối tiếp và lịch sử giao dịch của chính mình.
- **FR-013**: Khi gia sư mở thông tin gói của mình hoặc quản lý vận hành mở chi tiết hội viên của một gia sư, gói `ACTIVE` có ngày hết hạn trước hôm nay MUST được chuyển sang `EXPIRED`; khi không còn gói hiệu lực, trạng thái gia sư trở về "Đã duyệt, chưa kích hoạt gói" (srs-gaps G-04).

**Cấu hình gói (UC-99, UC-100)**

- **FR-014**: Quản lý vận hành MUST xem, tạo gói (tên 1–100 ký tự, số tháng 1–36, giá 10.000–100.000.000 VND, 0–20 quyền lợi mỗi mục 1–200 ký tự), sửa tên, giá, quyền lợi và bật/tắt mở bán. Số tháng không đổi sau khi tạo. Tên gói không trùng (không phân biệt hoa thường). Không xóa gói (DB ràng buộc RESTRICT). Danh sách hội viên (FR-017) hiển thị trạng thái tính theo ngày hiện tại mà không ghi hàng loạt.
- **FR-015**: Giá mới MUST chỉ áp dụng cho giao dịch tạo sau thời điểm lưu.
- **FR-016**: Lưu thất bại MUST giữ nguyên giá trị cũ và hiện thông điệp lỗi (UC-99/100 AF-1).

**Giám sát hội viên (UC-44, UC-45)**

- **FR-017**: Quản lý vận hành MUST xem danh sách gia sư có ít nhất một bản ghi hội viên, tìm theo tên/email (≤ 100 ký tự), lọc theo gói và trạng thái, phân trang mặc định 20, tối đa 50.
- **FR-018**: Quản lý vận hành MUST xem chi tiết hội viên của một gia sư gồm các lần đăng ký và lịch sử thanh toán; phần lịch sử lỗi không làm hỏng trang.

**Sự cố thanh toán (UC-98)**

- **FR-019**: Quản trị viên MUST xem danh sách giao dịch, lọc theo trạng thái, khoảng ngày, từ khóa (mã giao dịch, email gia sư), xem chi tiết kèm nhật ký xử lý.
- **FR-020**: Quản trị viên MUST tra cứu trạng thái với cổng cho một giao dịch; chỉ giao dịch đang chờ mới đổi trạng thái theo kết quả cổng (dùng lại bước xử lý ở FR-006).
- **FR-021**: Quản trị viên MUST ghi được kết quả điều tra (1–2.000 ký tự) kèm người ghi, thời gian; nội dung không hiển thị cho gia sư (GB-02).

**Đăng bài (BF I.2.2, chờ UC)**

- **FR-022**: Gia sư có gói hiệu lực MUST tạo được bài đăng gửi duyệt; cán bộ học vụ MUST duyệt hoặc từ chối kèm lý do; bài chỉ hiển thị công khai khi đã duyệt và gia sư còn gói hiệu lực. *(Chờ UC mới và schema — srs-gaps G-01, data-model mục 6.)*

**Chung**

- **FR-023**: Mọi API FE-09 MUST yêu cầu đăng nhập và đúng vai trò (gia sư/quản lý vận hành/quản trị viên/cán bộ học vụ), trừ endpoint IPN do cổng gọi (xác thực bằng chữ ký). Gia sư chỉ thấy dữ liệu của chính mình.
- **FR-024**: Giao diện MUST tiếng Việt, không in hoa toàn bộ, không hiển thị mã nội bộ; có trạng thái đang tải, rỗng, lỗi kèm "Thử lại"; số tiền định dạng `199.000 đ`, ngày `dd/MM/yyyy`.
- **FR-025**: Mã website và khóa bí mật của cổng MUST đọc từ biến môi trường; thiếu cấu hình thì báo MSG37, không làm ứng dụng ngừng khởi động.

### Key Entities *(include if feature involves data)*

- **Gói hội viên** (`membership_plans`): tên, số tháng, giá, danh sách quyền lợi, trạng thái mở bán.
- **Giao dịch thanh toán** (`payment_transactions`): gia sư (người dùng), gói, số tiền, cổng, mã giao dịch phía cổng, trạng thái, thời điểm tạo.
- **Hội viên gia sư** (`tutor_memberships`): gia sư, gói, ngày bắt đầu, ngày hết hạn, trạng thái, số tiền, mã giao dịch liên kết.
- **Nhật ký xử lý thanh toán** (`payment_processing_logs`): giao dịch, nội dung yêu cầu, nội dung phản hồi, mã HTTP, thời điểm; cũng chứa ghi chú điều tra của quản trị viên.
- **Bài đăng gia sư** (đề xuất, chưa có bảng): xem data-model mục 6.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Gia sư hoàn tất từ lúc mở trang gói đến lúc thấy gói được kích hoạt trong dưới 3 phút (không tính thời gian thao tác trên cổng).
- **SC-002**: 100% kết quả thanh toán thành công được kích hoạt đúng một lần, kể cả khi cổng gửi kết quả lặp 5 lần hoặc IPN và tra cứu chạy đồng thời.
- **SC-003**: 0 trường hợp kích hoạt gói khi chữ ký sai hoặc số tiền không khớp.
- **SC-004**: Ngày hết hạn hiển thị cho gia sư và quản lý vận hành khớp 100% với quy tắc FR-008.
- **SC-005**: Thao tác xem/mua gói phản hồi trong 2 giây cho 95% yêu cầu, không tính thời gian của cổng (NFR 2.2).
- **SC-006**: 0 lần lộ khóa bí mật, chữ ký hoặc mã nội bộ trên giao diện, log hay kho mã.
- **SC-007**: Người dùng không đúng vai trò nhận 401/403 trên 100% endpoint FE-09 được thử.

## Assumptions

- A-1: Schema hiện tại trên Supabase (đã đối chiếu bằng `information_schema` ngày 2026-11-04) đủ cho US1–US5; không đổi schema. US6 cần bảng mới, chờ duyệt.
- A-2: Mã đơn gửi cổng là `transaction_id` bỏ dấu gạch nối (32 ký tự); `tutor_memberships.transaction_ref` lưu `transaction_id` để liên kết.
- A-3: Trạng thái giao dịch theo enum DB (`PENDING`, `SUCCESS`, `FAILED`); `REFUNDED` không dùng vì không có UC hoàn tiền.
- A-4: Khi kích hoạt, `tutors.membership_status` = `PREMIUM`; khi hết hạn và không còn gói = `EXPIRED`. `STANDARD` không dùng vì gói chỉ khác nhau theo số tháng. Người dùng xác nhận 2026-11-04 (vẫn nên ghi vào SRS, srs-gaps G-06).
- A-5: Gia sư mới đăng ký bị FE-01 gán sẵn `PREMIUM`; người phụ trách FE-01 sẽ được báo, FE-09 không sửa code FE-01.
- A-6: IPN cần URL công khai; khi chạy cục bộ, kết quả được xác nhận qua tra cứu cổng (phương án C).
- A-7: Ghi chú điều tra của quản trị viên lưu trong nhật ký xử lý thanh toán (cột JSONB), không đổi schema.
- A-8: Chỉ gửi email; thông báo trong ứng dụng thuộc FE-12.
- A-9: Ma trận phân quyền SRS 4.4 không có dòng cho Membership; phân quyền theo actor của từng UC.
