# Feature Specification: FE-10 — Support Ticket & Escalation

**Feature Branch**: Không tạo nhánh mới; nhánh hiện tại không quyết định thư mục đặc tả.

**Created**: 2026-10-29

**Status**: Draft — đã rà soát chất lượng đặc tả

**Input**: Đọc FE-10 trong Report-3_Software Requirement Specification.docx và đặc tả việc tiếp nhận, xử lý, theo dõi, chuyển cấp yêu cầu về sử dụng nền tảng, kết nối gia sư, đặt học thử, đăng ký hội viên và thanh toán hội viên.

## Clarifications

### Session 2026-10-29

- Q: Nếu nhân viên phụ trách nghỉ hoặc không thể tiếp tục, ai được chuyển phiếu hỗ trợ sang nhân viên khác? → A: Mọi nhân viên chăm sóc khách hàng đều được chuyển phiếu cho nhau.
- Q: Sau khi nhân viên thông báo đã giải quyết, ai được đóng phiếu hỗ trợ? → A: Chỉ người gửi xác nhận đóng sau khi nhân viên đánh dấu đã giải quyết; nếu bổ sung vấn đề trước khi đóng, phiếu trở về đang xử lý.
- Q: Khi nhân viên chuyển cấp một phiếu, quản lý nào được xem và xử lý phiếu đó? → A: Nhân viên chọn một quản lý cụ thể; trong nhóm quản lý, chỉ người được chọn được xem và xử lý phiếu chuyển cấp.
- Q: Nếu quản lý được chọn nghỉ hoặc ngừng hoạt động khi phiếu đang chờ, nhân viên có được chuyển phiếu sang quản lý khác không? → A: Chỉ nhân viên đang phụ trách được đổi sang quản lý còn hoạt động khác; bắt buộc ghi lý do và giữ lịch sử.
- Q: FE-10 có cần cho người gửi và nhân viên đính kèm ảnh hoặc tài liệu vào phiếu hỗ trợ không? → A: Cho đính kèm JPG, PNG hoặc PDF, tối đa 10 MB/tệp và 5 tệp mỗi lần gửi; chỉ người có quyền xem phiếu được mở tệp.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gửi và theo dõi yêu cầu hỗ trợ (Priority: P1)

Học viên hoặc gia sư gửi yêu cầu và xem tiến độ, nội dung đã gửi, phản hồi dành cho mình (UC-46, UC-47).

**Why this priority**: Là đầu vào của quy trình hỗ trợ và giúp người gửi theo dõi vấn đề.

**Independent Test**: Gửi một phiếu hợp lệ, mở lại danh sách và chi tiết bằng tài khoản học viên hoặc gia sư; kiểm tra thông tin còn nguyên và tài khoản khác không xem được.

**Acceptance Scenarios**:

1. **Given** đã đăng nhập, **When** gửi tiêu đề, nhóm vấn đề và mô tả hợp lệ, **Then** một phiếu xuất hiện trong danh sách cá nhân với trạng thái “Mới tiếp nhận”, thời điểm gửi và nội dung ban đầu.
2. **Given** thiếu tiêu đề, nhóm hoặc mô tả, **When** gửi phiếu, **Then** hệ thống chỉ rõ trường cần bổ sung, giữ nội dung đã nhập và không tạo phiếu.
3. **Given** phiếu có phản hồi công khai và ghi chú nội bộ, **When** chủ phiếu mở chi tiết, **Then** chỉ thấy nội dung của mình, phản hồi công khai và trạng thái.
4. **Given** không sở hữu phiếu, **When** cố mở hoặc bổ sung nội dung, **Then** bị từ chối và không nhận được dữ liệu của phiếu.
5. **Given** phiếu mới hoặc trao đổi công khai có nội dung văn bản hợp lệ, **When** chủ phiếu gửi kèm tối đa 5 tệp JPG, PNG hoặc PDF không quá 10 MB mỗi tệp, **Then** nội dung và tệp được lưu cùng lần gửi, người có quyền được mở tệp sau khi tải lại.
6. **Given** có tệp sai loại, giả mạo định dạng, vượt dung lượng hoặc số lượng, **When** gửi phiếu/trao đổi, **Then** từ chối toàn bộ lần gửi, chỉ rõ lỗi tiếng Việt, giữ văn bản để sửa và không tạo phiếu, trao đổi hay tệp đính kèm một phần.
7. **Given** không có quyền xem phiếu, **When** truy cập trực tiếp tệp đính kèm của phiếu, **Then** bị từ chối và không nhận nội dung tệp.

### User Story 2 - Tiếp nhận, phản hồi và hoàn tất xử lý (Priority: P1)

Nhân viên chăm sóc khách hàng xem hàng đợi, nhận xử lý, hỏi thêm thông tin, hướng dẫn và ghi kết quả (UC-52, UC-53, UC-55).

**Why this priority**: Biến yêu cầu thành hỗ trợ thực tế với trách nhiệm xử lý rõ ràng.

**Independent Test**: Với một phiếu có sẵn, nhân viên nhận xử lý, hỏi thêm thông tin, nhận bổ sung và gửi kết quả; chủ phiếu xác nhận đóng mà không cần chuyển quản lý.

**Acceptance Scenarios**:

1. **Given** có nhiều phiếu, **When** lọc theo trạng thái, nhóm, ưu tiên hoặc người xử lý, **Then** danh sách chỉ chứa phiếu phù hợp và mở được chi tiết.
2. **Given** phiếu chưa có người xử lý, **When** nhân viên nhận phiếu, **Then** lưu người phụ trách và chuyển thành “Đang xử lý”.
3. **Given** phiếu đang xử lý, **When** nhân viên gửi câu hỏi hoặc hướng dẫn, **Then** chủ phiếu thấy phản hồi theo thứ tự thời gian và có thể bổ sung khi chưa đóng.
4. **Given** vấn đề đã được xử lý, **When** người phụ trách nhập kết quả và đánh dấu giải quyết, **Then** lưu kết quả công khai và trạng thái “Đã giải quyết”; chỉ khi chủ phiếu xác nhận đóng mới chuyển thành “Đã đóng”.
5. **Given** danh sách rỗng hoặc tải thất bại, **When** mở danh sách, **Then** hiển thị trạng thái rỗng hoặc lỗi tiếng Việt tương ứng, không có phiếu giả.
6. **Given** phiếu chưa đóng đã có người phụ trách, **When** bất kỳ nhân viên chăm sóc khách hàng còn hoạt động nào chuyển phiếu cho một nhân viên chăm sóc khách hàng khác còn hoạt động, **Then** người nhận trở thành người phụ trách, quyền xử lý chuyển sang người nhận và lịch sử lưu người thực hiện, người phụ trách cũ/mới cùng thời điểm. Nếu đang chờ quản lý, giữ nguyên trạng thái và quản lý nhận; kết quả được trả về người phụ trách mới.

7. **Given** phiếu đã giải quyết, **When** chủ phiếu bổ sung vấn đề thay vì xác nhận đóng, **Then** phiếu trở về “Đang xử lý” và giữ người phụ trách cùng toàn bộ lịch sử.
8. **Given** phiếu đã giải quyết, **When** nhân viên hoặc quản lý cố đóng, **Then** thao tác bị từ chối; nếu chủ phiếu chưa xác nhận, phiếu giữ trạng thái hiện tại và không tự đóng theo thời gian.
9. **Given** nhân viên đang phụ trách phiếu chưa đóng, **When** gửi phản hồi văn bản kèm tệp hợp lệ theo cùng giới hạn của người gửi, **Then** chủ phiếu xem được phản hồi và tệp; nhân viên không phụ trách không được gửi thay.

### User Story 3 - Chuyển quản lý và nhận hướng giải quyết (Priority: P2)

Nhân viên chuyển trường hợp vượt thẩm quyền đến quản lý. Quản lý xem lịch sử, quyết định phương án và trả kết quả cho nhân viên phản hồi người gửi (UC-48–UC-51, UC-54).

**Why this priority**: Giải quyết trường hợp vượt thẩm quyền mà không mất lịch sử và người phụ trách.

**Independent Test**: Chuyển một phiếu đang xử lý kèm lý do, lưu quyết định, trả nhân viên; kiểm tra chủ phiếu chỉ nhận kết luận sau khi nhân viên gửi phản hồi công khai.

**Acceptance Scenarios**:

1. **Given** phiếu đang xử lý, **When** người phụ trách chọn quản lý còn hoạt động và nhập lý do cùng tóm tắt xử lý, **Then** phiếu thành “Đã chuyển quản lý”, giữ người phụ trách và xuất hiện trong danh sách quản lý nhận.
2. **Given** thiếu lý do hoặc người nhận không hợp lệ, **When** chuyển cấp, **Then** bị từ chối, trạng thái không đổi.
3. **Given** phiếu được chuyển đến mình, **When** quản lý xem danh sách và chi tiết, **Then** thấy vấn đề, ngữ cảnh được phép, trao đổi, lịch sử và lý do chuyển.
4. **Given** đã đánh giá, **When** quản lý lưu hướng giải quyết hoặc phê duyệt phương án, **Then** quyết định được lưu nội bộ, chưa tự gửi người dùng hoặc đóng phiếu.
5. **Given** quyết định đã có, **When** quản lý trả kết quả, **Then** phiếu trở về “Đang xử lý”, giữ lịch sử và cho phép nhân viên gửi kết luận công khai.
6. **Given** đang chờ quản lý, **When** nhân viên đánh dấu giải quyết trước khi nhận kết quả, **Then** bị từ chối; vẫn được hỏi thêm hoặc thông báo tiến độ công khai.
7. **Given** phiếu đang chờ quản lý nhưng người nhận vắng mặt hoặc ngừng hoạt động, **When** nhân viên đang phụ trách chọn quản lý còn hoạt động khác và nhập lý do, **Then** phiếu tiếp tục ở trạng thái “Đã chuyển quản lý”, chỉ quản lý mới được xem/xử lý trong nhóm quản lý và lịch sử giữ người nhận cũ/mới, người đổi, thời điểm, lý do cùng nội dung xử lý trước đó. Quyết định cũ không thay thế quyết định của quản lý mới.
8. **Given** phiếu đang chờ quản lý, **When** nhân viên không phụ trách đổi người nhận, hoặc người phụ trách bỏ trống lý do/chọn người nhận không hợp lệ, **Then** bị từ chối và phân công cũ không đổi.

### User Story 4 - Hỗ trợ đặt học thử và kết nối gia sư (Priority: P2)

Nhân viên tìm yêu cầu đặt học thử/kết nối gia sư, xem ngữ cảnh và ghi hướng dẫn hoặc kết quả can thiệp (10.11, 10.12).

**Why this priority**: Đây là hai tình huống được nêu riêng trong FE-10.

**Independent Test**: Lọc phiếu liên quan, xem ngữ cảnh được phép và gửi hướng dẫn mà không thay đổi nghiệp vụ đặt lịch hoặc thanh toán.

**Acceptance Scenarios**:

1. **Given** phiếu liên quan lịch học thử của người gửi, **When** nhân viên mở chi tiết, **Then** thấy thông tin lịch đủ để tư vấn, không lộ dữ liệu giao dịch khác.
2. **Given** người dùng gặp khó khăn tìm hoặc liên hệ gia sư, **When** nhân viên hướng dẫn, **Then** lưu trao đổi và kết quả cùng phiếu, không tự tạo kết nối hoặc đặt lịch thay.
3. **Given** cần quyết định vượt thẩm quyền, **When** chuyển quản lý, **Then** dùng quy trình Story 3, không tạo phiếu thứ hai cho thao tác chuyển.

### Edge Cases

- Hai nhân viên cùng nhận/cập nhật: không ghi đè âm thầm; người thao tác sau phải tải lại khi thông tin đã đổi.
- Phiếu không tồn tại hoặc không được phép xem: thông báo chung, không tiết lộ người gửi.
- Liên kết lịch, hội viên hoặc giao dịch ngoài quyền: từ chối liên kết.
- Quản lý ngừng hoạt động trước khi lưu: từ chối chuyển, giữ nguyên phiếu.
- Quản lý vắng mặt/ngừng hoạt động sau khi đã nhận chuyển cấp: chỉ nhân viên phụ trách được đổi quản lý nhận kèm lý do; quản lý cũ mất quyền đọc/xử lý phiếu sau khi đổi. Nội dung và quyết định trước đó chỉ còn là lịch sử, quản lý mới phải ghi quyết định của mình trước khi trả kết quả.
- Lưu thất bại: không báo thành công và không để trạng thái, nội dung, lịch sử cập nhật một phần.
- Nội dung chỉ có khoảng trắng hoặc quá giới hạn: từ chối; văn bản chứa mã đánh dấu không được thực thi.
- Tệp sai loại, giả mạo định dạng, lớn hơn 10 MB hoặc nhiều hơn 5 tệp: từ chối toàn bộ lần gửi; không để phiếu, trao đổi hoặc tệp gắn kèm một phần. Tệp hỏng không đọc được theo định dạng khai báo cũng bị từ chối.
- Ranh giới tệp: chấp nhận đúng 5 tệp và tệp đúng 10 MB; 10 MB được tính là 10.000.000 byte. Tệp không bắt buộc; có tệp vẫn phải có nội dung văn bản hợp lệ.
- Khi quyền trên phiếu thay đổi, quyền mở tệp phải thay đổi tương ứng, kể cả khi người dùng đã biết đường truy cập tệp trước đó. Tệp không được dùng làm đường vòng để đọc nội dung nội bộ.
- Phiếu đã đóng: không nhận trao đổi/chuyển trạng thái; hướng dẫn tạo phiếu mới nếu còn vấn đề.
- Đối tượng liên quan không còn khả dụng: giữ nội dung hỗ trợ đã lưu, thông báo thiếu ngữ cảnh, không dựng dữ liệu thay thế.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: PHẢI cho học viên/gia sư đã đăng nhập tạo phiếu với tiêu đề 1–200 ký tự, mô tả 1–10.000 ký tự không chỉ chứa khoảng trắng và một nhóm vấn đề; người gửi lấy từ tài khoản đang dùng, không được mạo danh. Kiểm chứng: Story 1.1–1.2 và biên độ dài.
- **FR-002**: Nhóm vấn đề PHẢI gồm tài khoản/sử dụng nền tảng, thanh toán hội viên, gia sư/kết nối, đặt học thử, đăng ký hội viên, kỹ thuật, khác; mỗi nhóm gửi và lọc được. Kiểm chứng: Story 1.1 và 2.1 với từng nhóm.
- **FR-003**: Chủ phiếu PHẢI có danh sách cá nhân và chi tiết gồm nội dung, nhóm, trạng thái, thời điểm, trao đổi công khai; không xem phiếu của người khác. Kiểm chứng: Story 1.1, 1.3–1.4.
- **FR-004**: Nhân viên PHẢI xem hàng đợi và lọc theo trạng thái, nhóm, ưu tiên, người phụ trách. Danh sách cá nhân/nhân viên/quản lý PHẢI chia trang 20 mục mặc định, tối đa 50 mục, mới cập nhật trước. Kiểm chứng: Story 2.1, 3.3 và biên phân trang.
- **FR-005**: Nhân viên PHẢI nhận được phiếu chưa có người phụ trách; chỉ người phụ trách được phản hồi, đổi ưu tiên, chuyển cấp hoặc đánh dấu đã giải quyết; nhân viên và quản lý không được đóng phiếu thay người gửi. Mọi nhân viên chăm sóc khách hàng còn hoạt động được chuyển phiếu chưa đóng đã có người phụ trách cho nhân viên chăm sóc khách hàng khác còn hoạt động, kể cả phiếu đang do đồng nghiệp phụ trách. Ngoài quyền phân công lại này, nhân viên không phụ trách chỉ được xem để phục vụ hỗ trợ. Khi chuyển, quyền xử lý chuyển sang người nhận; nếu đang chờ quản lý, giữ nguyên trạng thái và quản lý nhận, trả kết quả về người phụ trách mới. Không cho chuyển đến chính người đang phụ trách hoặc tài khoản không hợp lệ. Kiểm chứng: Story 2.2, 2.6, 2.8 và thử thao tác bằng nhân viên khác.
- **FR-006**: PHẢI có ưu tiên thấp, bình thường, cao, khẩn cấp, mặc định bình thường; lưu người và thời điểm đổi. Không tự đặt cam kết thời gian xử lý. Kiểm chứng: Story 2 với từng mức và lịch sử.
- **FR-007**: Chủ phiếu/người phụ trách PHẢI trao đổi công khai được khi phiếu chưa đóng, nội dung 1–10.000 ký tự không chỉ chứa khoảng trắng; lưu người gửi, thời điểm và thứ tự. Không cho sửa/xóa trao đổi đã gửi trong phạm vi này. Kiểm chứng: Story 2.3 và biên nội dung/phiếu đóng.
- **FR-008**: PHẢI có các trạng thái mới tiếp nhận, đang xử lý, đã chuyển quản lý, đã giải quyết, đã đóng. Chỉ cho: mới tiếp nhận → đang xử lý khi nhận; đang xử lý → đã chuyển quản lý khi chuyển; đã chuyển quản lý → đang xử lý khi trả kết quả; đang xử lý → đã giải quyết khi người phụ trách gửi kết quả công khai; đã giải quyết → đã đóng chỉ khi chủ phiếu xác nhận. Chủ phiếu bổ sung khi đã giải quyết nhưng chưa đóng làm phiếu trở về đang xử lý, giữ người phụ trách và lịch sử. Không tự đóng theo thời gian khi chủ phiếu chưa xác nhận. Mọi chuyển đổi khác bị từ chối. Kiểm chứng: Stories 2–3, đặc biệt 2.4, 2.7–2.8 và từng chuyển đổi bị cấm.
- **FR-009**: Chuyển cấp PHẢI có quản lý nhận còn hoạt động, lý do 1–10.000 ký tự và tóm tắt xử lý trước đó 1–10.000 ký tự, đều không chỉ chứa khoảng trắng; tối đa một lần chuyển đang chờ trên mỗi phiếu, giữ người phụ trách. Kiểm chứng: Story 3.1–3.2 và chuyển lặp khi đang chờ.
- **FR-010**: Nhân viên PHẢI chọn một quản lý cụ thể khi chuyển cấp. Trong nhóm quản lý, chỉ người được chọn được xem/xử lý phiếu đó, gồm chi tiết, lịch sử, lý do; lọc được theo trạng thái, nhóm, ưu tiên. Không có hàng đợi chuyển cấp chung cho mọi quản lý. Quy định này không thay đổi quyền của chủ phiếu và nhân viên chăm sóc khách hàng đã nêu. Kiểm chứng: Story 3.3 với đúng quản lý và quản lý khác, bao gồm thử mở trực tiếp chi tiết bằng quản lý không được chọn.
- **FR-011**: Quản lý PHẢI lưu hướng giải quyết hoặc phê duyệt phương án có nội dung 1–10.000 ký tự không chỉ chứa khoảng trắng trước khi trả kết quả; lưu quyết định và trả kết quả là hai hành động phân biệt. Kết quả không tự công khai. Kiểm chứng: Story 3.4–3.5 và trả khi chưa có quyết định.
- **FR-012**: PHẢI tách ghi chú, lý do chuyển, đánh giá và quyết định nội bộ khỏi trao đổi công khai. Chỉ nhân viên hỗ trợ và quản lý được phép trên phiếu xem; học viên/gia sư không nhận nội dung này qua bất kỳ đường truy cập nào, theo GB-02. Kiểm chứng: Story 1.3, 3.4–3.5 với từng vai trò.
- **FR-013**: PHẢI lưu lịch sử nhận xử lý, phân công lại, đổi ưu tiên/trạng thái, chuyển cấp, quyết định và trả kết quả với tác giả, thời điểm, nội dung/giá trị trước-sau; phân công lại phải ghi người phụ trách cũ/mới. Không sửa/xóa lịch sử trong FE-10. Kiểm chứng: sau Stories 2–3, phân công lại và lần chuyển cấp tiếp theo.
- **FR-014**: Chủ phiếu được liên kết lịch học thử, hội viên hoặc thanh toán hội viên thuộc quyền xem, không bắt buộc có liên kết để gửi. Bộ phận hỗ trợ được phép trên phiếu chỉ xem ngữ cảnh cần cho xử lý. Kiểm chứng: Story 4.1 và liên kết đối tượng người khác.
- **FR-015**: PHẢI lọc/xem yêu cầu đặt học thử và ghi hướng dẫn kết nối gia sư trong cùng phiếu. Xử lý phiếu không tự đổi lịch, tạo kết nối, cấp hội viên hoặc hoàn tiền. Kiểm chứng: Story 4.1–4.3 và so sánh đối tượng trước/sau.
- **FR-016**: Nội dung hệ thống PHẢI bằng tiếng Việt, không lộ mã kỹ thuật hoặc viết hoa toàn bộ; dữ liệu thật, phân biệt rỗng và lỗi. Nội dung tự do người dùng nhập giữ nguyên và hiển thị an toàn. Kiểm chứng: toàn bộ màn hình Stories 1–4, kể cả lỗi/rỗng.
- **FR-017**: PHẢI kiểm tra quyền ở mọi lần đọc/thao tác, từ chối tài khoản chưa đăng nhập/không hoạt động; không cấp thêm quyền cho vai trò ngoài học viên/gia sư, chăm sóc khách hàng, quản lý đã mô tả. Kiểm chứng: ma trận vai trò × đọc/tạo/phản hồi/chuyển/quyết định/đóng.
- **FR-018**: PHẢI từ chối cập nhật trên trạng thái lỗi thời, tránh nhận phiếu hai lần hoặc ghi chuyển cấp trùng cho cùng thao tác; lưu lỗi không để dữ liệu cập nhật một phần. Kiểm chứng: ca cạnh tranh và lỗi lưu trong Edge Cases.
- **FR-019**: Khi phiếu đang “Đã chuyển quản lý” và người nhận vắng mặt hoặc ngừng hoạt động, chỉ nhân viên đang phụ trách được đổi sang một quản lý khác còn hoạt động, kèm lý do 1–10.000 ký tự không chỉ chứa khoảng trắng. PHẢI giữ trạng thái đang chuyển cấp, tối đa một người nhận hiện tại và toàn bộ lịch sử; ghi người thực hiện, người nhận cũ/mới, thời điểm và lý do. Quản lý cũ mất quyền đọc/xử lý, quản lý mới phải ghi quyết định riêng trước khi trả kết quả; quyết định cũ chỉ được xem như lịch sử. Không cho chọn lại cùng người nhận. Kiểm chứng: Story 3.7–3.8 và thử đọc/trả kết quả bằng quản lý cũ.
- **FR-020**: Chủ phiếu được đính kèm tệp khi tạo phiếu hoặc gửi trao đổi công khai; nhân viên đang phụ trách được đính kèm khi gửi phản hồi công khai. Tệp là tùy chọn, không thay thế văn bản bắt buộc. Mỗi lần gửi PHẢI giới hạn tối đa 5 tệp, mỗi tệp tối đa 10.000.000 byte, chỉ JPG (đuôi .jpg hoặc .jpeg), PNG và PDF. PHẢI kiểm tra định dạng thực tế, từ chối tệp giả mạo, hỏng hoặc không hợp lệ. Khi bất kỳ tệp nào không hợp lệ hoặc lưu thất bại, không lưu một phần phiếu/trao đổi và các tệp của lần gửi. Kiểm chứng: Story 1.5–1.6, 2.9 và ca biên số lượng/dung lượng/định dạng.
- **FR-021**: Tệp PHẢI gắn với phiếu hoặc trao đổi nguồn, hiển thị tên, loại, dung lượng và người gửi; chỉ người hiện có quyền xem nội dung nguồn mới được mở/tải. PHẢI kiểm tra quyền mỗi lần truy cập, kể cả qua đường truy cập trực tiếp, không công khai tệp cho người ngoài và thu hồi quyền theo thay đổi quyền trên phiếu. Không được sửa/xóa tệp đã gửi qua chức năng FE-10. Không hỗ trợ đính kèm ghi chú/quyết định nội bộ trong phạm vi này. Kiểm chứng: Story 1.7, 2.9 và kiểm tra quản lý cũ mất quyền sau đổi người nhận.

### Key Entities *(include if feature involves data)*

- **Phiếu hỗ trợ**: Người gửi, tiêu đề, nhóm, ưu tiên, trạng thái, người phụ trách, thời điểm và ngữ cảnh được phép.
- **Trao đổi công khai**: Phiếu, người gửi, nội dung, thời điểm; chủ phiếu và bộ phận hỗ trợ được phép xem.
- **Lần chuyển cấp**: Phiếu, nhân viên chuyển, quản lý nhận, lý do, tóm tắt, quyết định, thời điểm chuyển/trả, trạng thái chờ/đã trả; nhiều lần lịch sử nhưng tối đa một lần đang chờ.
- **Ghi chú/lịch sử nội bộ**: Tác giả, thời điểm, hành động, giá trị trước/sau và kết quả; không thuộc trao đổi người dùng.
- **Đối tượng liên quan**: Lịch học thử, đăng ký hoặc thanh toán hội viên thực tế, chỉ cung cấp ngữ cảnh theo quyền.
- **Tệp đính kèm**: Thuộc phiếu ban đầu hoặc một trao đổi công khai, có tên, loại, dung lượng, người gửi và thời điểm; kế thừa quyền đọc nội dung nguồn, không phải tài nguyên công khai.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Ít nhất 9/10 người tham gia nghiệm thu ở vai trò học viên/gia sư tạo và tìm lại phiếu hợp lệ trong 3 phút không cần trợ giúp.
- **SC-002**: 100% kịch bản nghiệm thu phủ năm phạm vi FE-10 hoàn thành gửi → nhận → phản hồi → giải quyết → đóng, giữ nội dung và lịch sử khi mở lại.
- **SC-003**: 100% kịch bản chuyển cấp hoàn thành chuyển → quyết định → trả nhân viên → phản hồi người gửi; không mất người phụ trách/lịch sử hoặc tạo phiếu mới.
- **SC-004**: 100% ca truy cập sai quyền và đọc nội bộ bằng tài khoản học viên/gia sư bị từ chối hoặc không trả nội dung bảo mật.
- **SC-005**: Với 100 người dùng đồng thời trong 10 phút và 1.000 phiếu có trao đổi, ít nhất 95% lần mở danh sách/chi tiết hiển thị trong 2 giây; không mất cập nhật đã thành công hoặc chuyển trạng thái sai.
- **SC-006**: 100% màn hình/trạng thái nghiệm thu dùng thông báo hệ thống tiếng Việt, không lộ mã kỹ thuật, không dữ liệu bịa và không lộ đánh giá nội bộ.
- **SC-007**: 100% ca nghiệm thu tệp đúng/sai loại, giả mạo, hỏng, ranh giới 5 tệp và 10 MB, lỗi lưu, truy cập trực tiếp sai quyền và thu hồi quyền đạt kết quả tại FR-020–FR-021; không có lần gửi lỗi để lại nội dung hoặc tệp gắn kèm một phần.

## Assumptions

- Nguồn: FE-10 người dùng cung cấp, UC-46–UC-55, tiêu đề 10.1–10.12 và GB-02 trong `Report-3_Software Requirement Specification.docx`.
- **Sai lệch nguồn**: Chi tiết 10.1–10.12 lặp mô tả cài đặt thông báo, gán “All User”, mâu thuẫn bảng use case. Dùng FE-10 và bảng use case làm căn cứ, không coi đoạn sao chép là yêu cầu cài đặt thông báo. Không chỉnh tài liệu nguồn.
- Truy vết: 10.1/UC-46 và 10.2/UC-47 → Story 1; 10.3–10.6/UC-48–UC-51 → Story 3; 10.7–10.8/UC-52–UC-53 → Story 2; 10.9/UC-54 → Story 3; 10.10/UC-55 → Story 2; 10.11–10.12 → Story 4, không tự gán số UC mới.
- Quyền mọi nhân viên chăm sóc khách hàng được chuyển phiếu cho nhau, quyền chỉ chủ phiếu xác nhận đóng sau khi đã giải quyết, việc chọn một quản lý cụ thể và quyền chỉ nhân viên phụ trách đổi quản lý nhận khi người cũ vắng mặt/ngừng hoạt động (có lý do, giữ lịch sử) đã được người dùng xác nhận trong Clarifications. Quy tắc nhận phiếu, các quyền xử lý còn lại, độ dài nội dung và các chuyển đổi vòng đời khác là mặc định đề xuất để kiểm chứng được, không khẳng định SRS đã định nghĩa chi tiết; có thể rà soát ở bước clarify.
- Phụ thuộc tài khoản, vai trò, trạng thái hoạt động và dữ liệu lịch/hội viên/thanh toán hiện có. Người dùng vẫn được gửi hỗ trợ khi chưa có lịch hoặc hội viên.
- Không bao gồm thực thi hoàn tiền, thu tiền, sửa chính sách, phê duyệt gia sư hoặc xử lý hạ tầng; hành động nghiệp vụ khác theo chức năng/thẩm quyền tương ứng.
- Tệp JPG/PNG/PDF tối đa 10 MB/tệp, 5 tệp/lần gửi đã được người dùng xác nhận trong Clarifications; quyền mở tệp phụ thuộc quyền xem nội dung nguồn. Quy ước 10 MB = 10.000.000 byte và việc đính kèm chỉ áp dụng nội dung công khai là chi tiết làm rõ để nghiệm thu.
- Không bao gồm chatbot, tự chuyển cấp theo hạn, cam kết thời gian xử lý, mở lại phiếu đã đóng, đánh giá hỗ trợ, email hoặc cấu hình thông báo. Theo dõi qua danh sách/chi tiết.
- Không cung cấp xóa phiếu; lưu giữ/xóa dữ liệu theo chính sách chung, không đặt thời hạn pháp lý mới.
- SC-005 là mục tiêu nghiệm thu đề xuất, không phải số đo hiện tại; dữ liệu kiểm tra cách ly khỏi dữ liệu vận hành.
- Tuân thủ constitution 1.0.0, bản địa hóa và thiết kế phân hệ hiện có; lựa chọn triển khai thuộc bước lập kế hoạch.
