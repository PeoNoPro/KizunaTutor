# Feature Specification: FE-12 — Notification Management

**Feature Branch**: `huyendtt` tại thời điểm tạo; không tạo hoặc chuyển nhánh.

**Created**: 2026-10-31

**Status**: Draft — đã rà soát trong phạm vi các giả định được ghi rõ.

**Input**: Đọc FE-12 Notification Management trong Report-3_Software Requirement Specification.docx và tạo đặc tả quản lý thông báo cho KizunaTutor.

## Clarifications

### Session 2026-10-31

- Q: Thông báo khẩn toàn hệ thống có được gửi email dù người nhận đã tắt kênh email không? → A: Không; luôn nhận trong ứng dụng, email chỉ gửi nếu người dùng đã bật. Chỉ kênh trong ứng dụng của thông báo khẩn được bỏ qua tùy chọn tắt.
- Q: Khi người dùng chưa thiết lập tùy chọn, thông báo thường được bật sẵn ở kênh nào? → A: Bật trong ứng dụng, tắt email cho mọi nhóm thông báo.
- Q: Khi quản trị viên hẹn giờ gửi thông báo cho một nhóm, danh sách người nhận được xác định vào lúc nào? → A: Đến hạn gửi mới chốt danh sách theo vai trò và trạng thái tài khoản hiện tại; không cố định danh sách người nhận từ lúc hẹn giờ.
- Q: Khi mở chi tiết một thông báo chưa đọc, hệ thống có tự đánh dấu đã đọc không? → A: Có, sau khi tải chi tiết thành công; vẫn hỗ trợ nút đánh dấu đã đọc từ danh sách. Tải lỗi không đổi trạng thái.
- Q: Với bản đầu FE-12, cần tích hợp thông báo tự động từ những nghiệp vụ nào? → A: Học thử, bài tập, thanh toán hội viên và hỗ trợ, cùng thông báo do quản trị viên gửi. Nguồn nghiệp vụ chưa hoàn thiện phải ghi phụ thuộc, không tạo thông báo giả.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem, đánh dấu đã đọc và xóa thông báo cá nhân (Priority: P1)

Người dùng đã đăng nhập thuộc sáu vai trò xem thông báo dành cho mình, nhận biết chưa đọc, mở nội dung và xóa khỏi danh sách riêng (12.2–12.4, UC-71–UC-73).

**Why this priority**: Đây là đầu ra trực tiếp giúp người dùng nhận biết cập nhật và tác vụ cần làm.

**Independent Test**: Với thông báo thực được tạo cho hai tài khoản trong môi trường kiểm thử, một người xem/đánh dấu/xóa rồi tải lại; trạng thái của người kia không đổi.

**Acceptance Scenarios**:

1. **Given** có thông báo dành cho mình, **When** mở trung tâm thông báo, **Then** thấy tiêu đề, nội dung, loại, thời điểm và trạng thái đọc; danh sách mới nhất trước, số chưa đọc đúng.
2. **Given** một thông báo chưa đọc, **When** chọn đánh dấu đã đọc, **Then** trạng thái và số chưa đọc cập nhật đúng một lần và giữ sau tải lại.
3. **Given** thông báo còn trong danh sách cá nhân, **When** xác nhận xóa, **Then** không còn thấy nó và nếu chưa đọc thì số chưa đọc giảm; nội dung của người nhận khác/lịch sử gửi của quản trị viên không bị xóa.
4. **Given** biết đường truy cập thông báo người khác, **When** cố xem/đánh dấu/xóa, **Then** bị từ chối, không nhận được nội dung hoặc làm thay đổi dữ liệu người đó.
5. **Given** không có thông báo hoặc không tải được dữ liệu, **When** mở danh sách, **Then** hiển thị trạng thái rỗng hoặc lỗi tương ứng, không có dữ liệu giả.
6. **Given** thông báo chưa đọc của mình, **When** mở và tải chi tiết thành công, **Then** tự đánh dấu đã đọc và cập nhật số chưa đọc đúng một lần; nếu tải chi tiết thất bại thì không đổi trạng thái. Nếu lưu trạng thái đọc thất bại sau khi nội dung đã tải, phải báo chưa lưu được trạng thái, không hiển thị số đếm như đã lưu thành công.

### User Story 2 - Cấu hình tùy chọn nhận thông báo (Priority: P1)

Người dùng chọn nhóm thông báo và kênh trong ứng dụng/email muốn nhận (12.1, UC-70).

**Why this priority**: Tôn trọng lựa chọn người nhận và tránh gửi qua kênh đã tắt.

**Independent Test**: Tắt email của một nhóm, giữ trong ứng dụng; gửi một thông báo thường cùng nhóm và kiểm chỉ kênh được bật có lượt nhận, tải lại tùy chọn không mất.

**Acceptance Scenarios**:

1. **Given** đã đăng nhập, **When** mở tùy chọn, **Then** thấy giá trị hiện tại cho từng nhóm và từng kênh hỗ trợ.
2. **Given** thay đổi tùy chọn, **When** bấm lưu, **Then** lưu toàn bộ lần thay đổi hoặc không lưu gì nếu lỗi; chỉ sau lưu thành công mới hiển thị xác nhận.
3. **Given** tùy chọn đã lưu, **When** hệ thống chuẩn bị gửi thông báo mới, **Then** chỉ gửi kênh được bật, đánh dấu kênh bị bỏ qua vì tùy chọn, không coi đó là lỗi gửi.
4. **Given** hai cửa sổ sửa cùng phiên bản tùy chọn, **When** cửa sổ sau lưu trên dữ liệu cũ, **Then** yêu cầu tải lại thay vì ghi đè âm thầm.

### User Story 3 - Gửi thông báo đến nhóm và xem lịch sử (Priority: P2)

Quản trị viên soạn, chọn nhóm người nhận/kênh, xác nhận gửi ngay hoặc hẹn giờ, rồi xem kết quả thực tế (12.5–12.6, UC-74–UC-75).

**Why this priority**: Giúp thông báo đúng nhóm và truy được các lần gửi thất bại hoặc đang chờ.

**Independent Test**: Chọn nhóm gia sư, gửi ngay rồi hẹn giờ một lần khác; kiểm học viên không nhận, thời điểm đúng và lịch sử ghi được trạng thái từng kênh.

**Acceptance Scenarios**:

1. **Given** quản trị viên còn hoạt động, **When** nhập tiêu đề/nội dung, chọn một hoặc nhiều vai trò và kênh, **Then** thấy bản xem trước cùng nhóm nhận trước khi xác nhận.
2. **Given** xác nhận gửi ngay, **When** lưu yêu cầu thành công, **Then** báo đã xếp hàng; không báo email đã gửi khi chưa có kết quả từ kênh gửi.
3. **Given** thời điểm tương lai hợp lệ, **When** xác nhận hẹn giờ, **Then** trạng thái là “Đã lên lịch”, không phát trước giờ; đến hạn chỉ chọn tài khoản còn hoạt động thuộc nhóm đã chọn.
4. **Given** có lịch sử gửi, **When** quản trị viên lọc theo ngày, nhóm, kênh, trạng thái hoặc tìm tiêu đề và mở chi tiết, **Then** thấy nội dung, người tạo, thời điểm, tổng người nhận và số thành công/thất bại/bỏ qua/đang chờ theo kênh.
5. **Given** email thất bại nhưng trong ứng dụng thành công, **When** xem kết quả, **Then** hiển thị hoàn tất một phần và không gửi lại bản trong ứng dụng đã thành công.

### User Story 4 - Gửi thông báo khẩn toàn hệ thống (Priority: P2)

Quản trị viên gửi thông báo quan trọng đến mọi tài khoản còn hoạt động, có bước xác nhận rõ phạm vi để tránh nhầm (12.7, UC-76).

**Why this priority**: Thông báo sự cố/chính sách quan trọng ảnh hưởng toàn hệ thống cần thao tác riêng và lịch sử truy trách nhiệm.

**Independent Test**: Tạo thông báo khẩn trong môi trường thử, chọn tất cả người dùng, kiểm tài khoản của sáu vai trò còn hoạt động đều thuộc tập nhận, tài khoản bị khóa/xóa không nhận; hủy xác nhận không tạo yêu cầu gửi.

**Acceptance Scenarios**:

1. **Given** chọn gửi tất cả, **When** mở xác nhận, **Then** thấy cảnh báo tiếng Việt về toàn bộ tài khoản hoạt động và số người nhận dự kiến trong hộp xác nhận, phải bấm nút gửi trong hộp đó mới được gửi (bổ sung 2026-11-09: bỏ bước gõ “Xác nhận”).
2. **Given** quản trị viên hủy hoặc nhập sai cụm xác nhận, **When** kết thúc hộp thoại, **Then** không tạo lần gửi hoặc thông báo nào.
3. **Given** xác nhận hợp lệ, **When** yêu cầu được ghi nhận, **Then** được đánh dấu khẩn và xếp hàng cho tập người nhận thực tế, có lịch sử; trạng thái nhận trong ứng dụng/email được theo dõi riêng.

### Edge Cases

- Gửi lặp cùng thao tác vì nhấn hai lần/mạng retry: chỉ có một đợt gửi và một bản trong ứng dụng cho mỗi người nhận hợp lệ.
- Nhóm không có người nhận: thông báo rõ số 0, ghi kết quả không có người nhận; không bịa dữ liệu hoặc báo số gửi thành công dương.
- Người nhận bị khóa/xóa/đổi vai trò sau hẹn giờ: đối chiếu trạng thái và vai trò tại lúc chốt tập gửi, kiểm hoạt động lại trước phát; bỏ qua với lý do nếu không còn hợp lệ.
- Người dùng tắt kênh sau khi thông báo vào hàng đợi: kiểm tùy chọn lúc chuẩn bị gửi qua kênh; thông báo đã phát trước đó không bị thu hồi.
- Email timeout có thể chưa biết nhà cung cấp đã nhận hay chưa: ghi trạng thái chưa xác định cần kiểm tra, không báo chắc chắn gửi thất bại hoặc tự gửi lại vô hạn.
- Đánh dấu đọc/xóa lặp: không giảm số chưa đọc nhiều lần; xóa ở một thiết bị có hiệu lực khi thiết bị khác tải lại.
- Thông báo chứa liên kết tới dữ liệu người dùng không còn quyền: kiểm quyền tại nơi đến, không coi thông báo là giấy cấp quyền; nội dung thư không chứa ghi chú nội bộ của FE-10 hoặc bí mật vận hành FE-11.
- Nguồn dữ liệu/lịch gửi bị gián đoạn: không báo gửi thành công; phục hồi không phát lại bản đã thành công hoặc làm mất lịch gửi đã lưu.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: PHẢI kiểm tài khoản và quyền hiện tại mọi thao tác: sáu vai trò dùng thông báo/tùy chọn cá nhân; chỉ quản trị viên và quản lý vận hành (bổ sung 2026-11-09 theo yêu cầu nhóm) được soạn/gửi nhóm, toàn hệ thống và xem lịch sử gửi. Người dùng không đọc/sửa thông báo hoặc tùy chọn của người khác. Kiểm chứng: Stories 1.4, 2 và ma trận vai trò.
- **FR-002**: PHẢI có danh sách thông báo cá nhân, lọc tất cả/chưa đọc và loại, chi tiết có tiêu đề/nội dung/thời điểm/trạng thái; mới nhất trước với thứ tự ổn định, phân trang mặc định 20, tối đa 50. Số chưa đọc chỉ đếm bản chưa đọc, chưa xóa của đúng người. Kiểm chứng: Story 1.1, phân trang và bộ lọc.
- **FR-003**: PHẢI tự đánh dấu đã đọc sau khi người nhận mở và tải chi tiết thành công, đồng thời cho đánh dấu từng thông báo từ danh sách. Lưu thời điểm lần đọc đầu tiên; thao tác lặp hoặc mở lại không đổi thời điểm hay giảm số đếm lần nữa. Tải chi tiết thất bại không đổi trạng thái. Nếu lưu trạng thái đọc thất bại, phải báo chưa lưu được và không cập nhật số đếm như thể đã lưu; nội dung đã tải vẫn có thể được xem. Kiểm chứng: Story 1.2, 1.6, lỗi tải/lưu và thao tác lặp/đa thiết bị.
- **FR-004**: PHẢI cho người nhận xác nhận xóa thông báo khỏi danh sách cá nhân; xóa không ảnh hưởng người khác hoặc lịch sử gửi quản trị. Sau xóa không mở lại được chi tiết qua danh sách/đường trực tiếp của người nhận; không có khôi phục trong bản này. Kiểm chứng: Story 1.3–1.4 và xóa lặp.
- **FR-005**: PHẢI cho chọn kênh trong ứng dụng/email theo nhóm: hệ thống, học thử, bài tập, thanh toán, hỗ trợ; lưu rõ cả hai giá trị bật/tắt và chống cập nhật trên phiên bản cũ. Chọn lưu tường minh, không tự lưu khi chỉ bật/tắt trên form. Kiểm chứng: Story 2.1–2.4.
- **FR-006**: Khi chưa có tùy chọn cá nhân, PHẢI mặc định bật trong ứng dụng và tắt email cho từng nhóm, hiển thị rõ để người dùng sửa. Thông báo thường chỉ phát kênh được bật tại lúc gửi; bị tắt ghi “Bỏ qua theo tùy chọn”, không là lỗi. Thông báo khẩn toàn hệ thống PHẢI có kênh trong ứng dụng và phát qua kênh này dù người nhận đã tắt; email chỉ phát nếu quản trị viên chọn thêm email và người nhận bật email cho nhóm tương ứng. Không có quyền bỏ qua tùy chọn email kể cả thông báo khẩn. Kiểm chứng: Story 2.3 và Story 4 với người nhận bật/tắt từng kênh.
- **FR-007**: Quản trị viên PHẢI nhập tiêu đề 1–150 ký tự, nội dung 1–10.000 ký tự không chỉ khoảng trắng, nhóm thông báo, kênh trong ứng dụng/email/cả hai và tập vai trò nhận không rỗng khi gửi nhóm. Chỉ nhận danh mục vai trò/nhóm hợp lệ; không nhập địa chỉ email hoặc người nhận tùy ý ngoài tài khoản hệ thống. Kiểm chứng: Story 3.1 và trường thiếu/giới hạn.
- **FR-008**: PHẢI cho xem trước và xác nhận trước khi gửi; hủy không tạo lần gửi. Với toàn hệ thống, PHẢI hiển thị cảnh báo/số người dự kiến trong hộp xác nhận và chỉ gửi khi bấm nút gửi trong hộp đó (bổ sung 2026-11-09: bỏ bước gõ “Xác nhận” theo yêu cầu nhóm); chỉ chọn người còn hoạt động thuộc cả sáu vai trò, không guest. Kiểm chứng: Story 3.1–3.2 và Story 4.1–4.3.
- **FR-009**: PHẢI cho gửi ngay hoặc hẹn thời điểm tương lai theo Asia/Ho_Chi_Minh cho đợt gửi nhóm; lưu thời điểm và danh mục nhóm đã chọn, không phát trước hạn. Đợt khẩn toàn hệ thống gửi ngay, không hẹn giờ trong bản này. Khi đến hạn, lúc bắt đầu xử lý đợt PHẢI chốt một lần tập người nhận theo vai trò và trạng thái hoạt động hiện tại, không dùng danh sách cố định từ lúc hẹn giờ. Người mới thuộc nhóm trước lúc chốt được nhận; người đã rời nhóm trước lúc chốt không thuộc tập nhận. Giữ tập đã chốt khi xử lý lại để không mở rộng đợt ngoài ý muốn; kiểm lại hoạt động/tùy chọn trước phát theo FR-014. Có số thực tế riêng với số dự kiến lúc xác nhận. Kiểm chứng: Story 3.3, người tham gia/rời nhóm trước lúc chốt và phục hồi xử lý sau khi đã chốt.
- **FR-010**: PHẢI lưu bền vững yêu cầu gửi và lịch sử trước khi xác nhận đã xếp hàng; nhận cùng mã thao tác gửi lặp không tạo thêm đợt. Lỗi lưu không để lại một phần đợt gửi/lịch sử. Trong ứng dụng tối đa một bản mỗi đợt/người; không phát lại bản đã thành công khi phục hồi. Kiểm chứng: submit trùng, lỗi lưu và phục hồi sau gián đoạn.
- **FR-011**: PHẢI theo dõi kết quả từng người/kênh riêng: chờ, trong ứng dụng đã có/email được dịch vụ gửi chấp nhận, thất bại, bỏ qua hoặc chưa xác định. Email được chấp nhận không có nghĩa đã vào hộp thư/đã đọc. Bản trong ứng dụng đã đọc không làm email thành đã đọc. Kiểm chứng: Story 3.4–3.5, email timeout và kết quả từng kênh.
- **FR-012**: PHẢI tổng hợp trạng thái đợt: đã lên lịch, đang chờ, đang gửi, hoàn tất, hoàn tất một phần, thất bại, không có người nhận hoặc cần kiểm tra khi có kết quả chưa xác định. Hoàn tất khi mọi lượt mục tiêu thành công hoặc bỏ qua có lý do; hoàn tất một phần khi có cả thành công/bỏ qua và thất bại, thất bại khi tất cả lượt cần gửi đã thất bại. Không coi trạng thái chờ/chưa xác định là hoàn tất. Kiểm chứng: Story 3.4–3.5 với các tổ hợp trạng thái.
- **FR-013**: PHẢI cho quản trị viên xem danh sách/chi tiết lịch sử với người tạo, nội dung, nhóm, kênh, thời điểm tạo/hẹn/chốt, tổng người nhận thực tế và số kết quả theo kênh; lọc ngày/nhóm/kênh/trạng thái, tìm tiêu đề và phân trang 20/max50. Số kết quả phải đối chiếu được với lượt nhận thực tế. Kiểm chứng: Story 3.4, tổ hợp kết quả và nhóm rỗng.
- **FR-014**: PHẢI kiểm trạng thái người nhận, vai trò và tùy chọn khi chốt/phát như Edge Cases; tài khoản không còn hoạt động không nhận lượt mới. Thông báo liên quan dữ liệu nghiệp vụ chỉ gửi người có quyền liên quan, không lộ ghi chú nội bộ hoặc liên kết bỏ qua phân quyền. Kiểm chứng: người bị khóa/đổi vai trò/tắt kênh và truy cập đối tượng không còn quyền.
- **FR-015**: Bản đầu PHẢI tích hợp thông báo tự động từ bốn nhóm nghiệp vụ: học thử, bài tập, thanh toán hội viên và hỗ trợ, ngoài thông báo do quản trị viên gửi. Điểm tiếp nhận PHẢI có nhóm, nội dung đã chuyển ngữ, người nhận được phép và định danh sự kiện để chống tạo trùng. Chỉ tạo thông báo từ sự kiện nghiệp vụ thực đã được ghi nhận thành công; một sự kiện xử lý lại không tạo thêm bản cho cùng người/kênh. Nguồn chưa hoàn thiện phải ghi rõ phụ thuộc và phần nghiệm thu chưa đạt, không giả lập thành công hoặc tạo thông báo mẫu. Danh sách sự kiện cụ thể và người nhận từng sự kiện phải được đối chiếu với luồng thực tế của bốn module ở bước plan. Kiểm chứng: mỗi nhóm có luồng nghiệp vụ thực sinh thông báo đúng người theo tùy chọn, cùng sự kiện gửi hai lần không trùng và nguồn chưa tích hợp không được đánh dấu đã đạt.
- **FR-016**: PHẢI hiển thị labels/thông báo hệ thống bằng tiếng Việt, không all-caps ngoài từ viết tắt được duyệt, không raw enum/vai trò/exception/địa chỉ kết nối/token. Nội dung nhập được hiển thị an toàn; có loading/empty/error riêng, dữ liệu thật và không thay lỗi bằng số0/thông báo giả. Kiểm chứng: mọi màn hình, nguồn rỗng/lỗi và chuỗi nhập nguy hiểm.

### Key Entities *(include if feature involves data)*

- **Thông báo**: Tiêu đề, nội dung, nhóm, thời điểm, nguồn sự kiện hoặc đợt gửi; không có trạng thái đọc dùng chung cho mọi người.
- **Thông báo cá nhân**: Người nhận, thông báo nguồn, thời điểm nhận/đọc/xóa và trạng thái chưa đọc; tách biệt giữa các người nhận.
- **Tùy chọn cá nhân**: Người dùng, nhóm, kênh, bật/tắt, phiên bản và thời điểm cập nhật.
- **Đợt gửi quản trị**: Người tạo, mã thao tác chống lặp, nhóm nhận hoặc toàn hệ thống, mức thường/khẩn, nhóm thông báo, kênh, thời điểm hẹn/chốt và trạng thái tổng hợp.
- **Lượt gửi theo người/kênh**: Đợt/sự kiện, người nhận, kênh, trạng thái, lý do bỏ qua/thất bại an toàn, thời điểm và kết quả chấp nhận; bảo đảm không trùng bản trong ứng dụng.
- **Lịch sử gửi**: Nội dung đã xác nhận, người thực hiện, thời gian, tập người nhận thực tế, số kết quả; không bị thay đổi bởi người nhận xóa bản riêng.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% ca đọc/đánh dấu/xóa/tùy chọn sai chủ bị từ chối; thao tác hợp lệ và lặp cập nhật đúng số chưa đọc, không tác động người khác.
- **SC-002**: 100% ca gửi thường theo từng nhóm/kênh tôn trọng tùy chọn lúc phát; ca khẩn có bản trong ứng dụng cho người nhận hợp lệ, email theo quy tắc FR-006, kết quả bỏ qua có lý do.
- **SC-003**: 100% ca hẹn giờ không phát sớm; trong môi trường nguồn gửi khả dụng, bắt đầu xử lý trong60giây sau hạn. Tài khoản không hợp lệ không nhận, tập thực tế và dự kiến được phân biệt.
- **SC-004**: 100% lần submit lặp và sự kiện xử lý lại trong nghiệm thu không tạo thêm đợt/bản trong ứng dụng; khởi động lại không mất yêu cầu đã xác nhận xếp hàng hoặc phát lại bản trong ứng dụng thành công.
- **SC-005**: 100% số kết quả từng kênh trong lịch sử khớp lượt nhận, kể cả email lỗi/timeout, nhóm rỗng và lỗi một phần; không tuyên bố email đã vào hộp thư chỉ từ chấp nhận gửi.
- **SC-006**: Với100người dùng đồng thời/10phút và10.000thông báo cá nhân trong môi trường kiểm thử, ít nhất95% lần mở danh sách/chi tiết hiển thị trong2giây. Xác nhận xếp hàng trong2giây ở ít nhất95% ca, không đợi gửi hết email; lỗi nguồn được báo trong5giây tính từ thao tác yêu cầu.
- **SC-007**: Ít nhất9/10người thử tìm thông báo chưa đọc và đánh dấu/xóa đúng trong2phút không cần trợ giúp; toàn bộ màn hình nghiệm thu có tiếng Việt đúng dấu và không lộ dữ liệu nội bộ bị cấm.

## Assumptions

- Nguồn: SRS mục12.1–12.7 và bảng UC-70–UC-76. Truy vết: 12.1/UC-70→Story2;12.2–12.4/UC-71–73→Story1;12.5–12.6/UC-74–75→Story3;12.7/UC-76→Story4.
- **Sai lệch nguồn**: 12.2–12.4 lặp mô tả dashboard tài chính và actor Admin. Dùng tên mục/bảng UC để đặc tả thông báo cá nhân cho người dùng, không đưa báo cáo tài chính vào FE-12. Mục12.6 có alternative/postcondition lẫn thao tác gửi toàn hệ thống; lấy normal flow lịch sử làm căn cứ, không biến xem lịch sử thành hành động gửi. Không sửa SRS gốc.
- Bản nháp dùng sáu vai trò hiện có, admin-only broadcast/history. Danh mục nhóm theo notifications hiện có trong dữ liệu dự án: hệ thống/học thử/bài tập/thanh toán/hỗ trợ. Không tự thêm loại tin nhắn chat khi chưa có danh mục/nguồn được chốt.
- **Quy tắc đã xác nhận**: Trong ứng dụng bắt buộc với thông báo khẩn dù người nhận tắt; email chỉ gửi khi được chọn và người nhận bật nhóm tương ứng, không có quyền bỏ qua tùy chọn email. Người chưa thiết lập mặc định bật trong ứng dụng và tắt email cho mọi nhóm. Các lựa chọn này đã được người dùng xác nhận trong Clarifications; không khẳng định có nguyên văn trong SRS.
- Người dùng đã xác nhận tự đánh dấu đọc sau khi mở chi tiết thành công, vẫn có nút đánh dấu từ danh sách; tải lỗi không đổi trạng thái. Chọn lưu tùy chọn tường minh thay auto-save vì SRS cho cả hai. Xóa chỉ bản cá nhân; không thêm mark-all-read, restore hoặc sửa/xóa nội dung đã phát.
- Người dùng đã xác nhận chốt danh sách người nhận khi đến hạn gửi theo vai trò và trạng thái hiện tại, không cố định từ lúc hẹn giờ. Nhóm nhận chỉ theo vai trò hoạt động, không bộ lọc marketing tùy ý; hẹn giờ theo UTC+7, xác nhận toàn hệ thống bằng hộp xác nhận (ban đầu gõ “Xác nhận”, bỏ từ 2026-11-09 theo yêu cầu nhóm).
- Email dùng khả năng gửi thư dự án hiện có; chưa xem việc provider nhận thư là deliver/read receipt. Không cam kết email exactly-once khi provider timeout; lưu chưa xác định để kiểm tra thay retry vô hạn. Không thêm nhà cung cấp SMS, push trình duyệt, tệp đính kèm, analytics mở thư, workflow hủy/sửa lịch hoặc nút gửi lại thủ công trong bản này.
- Người dùng đã xác nhận tích hợp cả bốn module học thử/bài tập/thanh toán hội viên/hỗ trợ trong bản đầu, cùng broadcast quản trị. Các module chưa có nguồn thật phải ghi dependency và phần nghiệm thu chưa đạt khi plan, không tự triển khai toàn bộ nghiệp vụ bên kia hoặc dùng fixture thay bằng chứng tích hợp thật. Danh sách sự kiện chi tiết cần đối chiếu luồng hiện có ở plan. OTP/email xác thực là luồng bảo mật riêng, không bị tùy chọn FE-12 chặn.
- Khối lượng/thời gian nghiệm thu là mục tiêu đề xuất, chưa là số đo hiện tại. Fixture/mail giả lập chỉ kiểm thử cách ly, không chèn thông báo mẫu vào vận hành hoặc gửi thư thật trong bước tạo spec.
- Tuân thủ constitution, AGENTS.md, CONVENTIONS.md, DESIGN.md và UI_LOCALIZATION_RULES.md. Bước này chỉ tạo đặc tả/checklist và chọn feature context; không code, migrate, commit/push hoặc gửi thông báo/email.
