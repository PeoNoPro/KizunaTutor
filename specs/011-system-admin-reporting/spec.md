# Feature Specification: FE-11 — System Administration & Operational Reporting

**Feature Branch**: `huyendtt` tại thời điểm tạo; không tạo hoặc chuyển nhánh trong lượt này.

**Created**: 2026-10-29

**Status**: Đã triển khai một phần; báo cáo Manager và xem hồ sơ cá nhân đã bổ sung theo phạm vi mới ngày 2026-11-01. Người dùng yêu cầu để phần AI sau; nghiệm thu toàn tính năng vẫn chưa hoàn tất.

**Input**: Đọc FE-11 (System Administration & Operational Reporting) trong Report-3_Software Requirement Specification.docx và tạo đặc tả dựa trên phạm vi, use case và quy tắc hiện có.

## Clarifications

### Mật khẩu tạm qua email, người dùng duyệt ngày 2026-11-05

Theo SRS mục 1.6: Admin không nhập mật khẩu. Khi tạo từng tài khoản hoặc nhập Excel, hệ thống sinh mật khẩu ngẫu nhiên riêng cho từng người và gửi vào email sau khi lưu thành công; tài khoản phải đổi mật khẩu lần đầu. Mật khẩu không xuất hiện trong API, tệp, nhật ký hay log. Admin được cấp lại mật khẩu tạm khi nhân viên chưa đổi mật khẩu lần đầu; mật khẩu cũ mất hiệu lực.

**Sai khác đã biết với SRS:** mục 1.7 ghi Admin đổi vai trò, trong khi phạm vi đã chốt là Manager đề xuất, Admin duyệt.

### Phạm vi hiện hành, người dùng chốt ngày 2026-11-05 — ưu tiên hơn mọi mục bên dưới

Nguồn: người dùng duyệt trong hội thoại; SRS sẽ được cập nhật sau theo mục này. Học thử diễn ra offline. Không có nghiệp vụ hoàn tiền trong FE-11. Quản lý và chỉnh sửa gói hội viên thuộc feature khác. AI tiếp tục để sau.

- **Cấu hình hệ thống:** phạm vi cần triển khai gồm thông tin nền tảng (tên hiển thị, email hỗ trợ, số điện thoại hỗ trợ) và giới hạn dung lượng tệp đính kèm yêu cầu hỗ trợ trong mức trần server 10 MB. Các vị trí thông tin chung/footer được tích hợp phải dùng cùng nguồn. Có lý do, kiểm tra phiên bản và lịch sử trước/sau. **Banner thông báo bảo trì tạm hoãn, chờ người dùng tải code mới về rồi rà lại; không triển khai hoặc nghiệm thu trong đợt này.** Ảnh đại diện, tài liệu hồ sơ chưa có API tải lên nên chưa có ô cấu hình. Không coi việc cập nhật đặc tả là đã triển khai thông tin nền tảng. Màn cấu hình học thử gỡ khỏi giao diện; API và dữ liệu cũ chưa xóa, phải kiểm tra phụ thuộc trước khi thay đổi.
- **Sự cố:** bỏ phân công và phân công lại. Admin tự nhận; hai người cùng nhận thì chỉ một người thành công. Người phụ trách được trả lại danh sách chờ với lý do; Admin khác chỉ trả lại được khi người phụ trách không còn hoạt động. Lịch sử phân công cũ giữ để tra cứu.
- **Tài khoản nhân viên:** mật khẩu tạm sinh ngay lúc gửi thư và không lưu trong hàng đợi. Trạng thái email gồm chờ gửi, đã gửi, gửi thất bại; "đã gửi" không khẳng định nhân viên đã đọc. Hàng đợi lưu trong DB để tiếp tục sau khi khởi động lại. Tạo tài khoản Quản lý phải có căn cứ bổ nhiệm.
- **Bắt đổi mật khẩu:** API FE-11 từ chối tài khoản chưa đổi mật khẩu lần đầu. API của feature khác là phụ thuộc FE-01.
- **Doanh thu:** chỉ tổng giao dịch mua gói thành công, theo gói và theo ngày; không có chỉ số, bộ lọc hay biểu đồ hoàn tiền; phân biệt không có dữ liệu, chưa tích hợp nguồn và lỗi tải. Cách nhận diện giao dịch mua gói cần thống nhất với feature thanh toán.
- **Nhật ký:** có thêm đề xuất, từ chối, hủy thăng chức và cấu hình mới.
- **Sức khỏe:** ngừng cung cấp API và trang. Bộ đo chưa xóa vì đang giữ `@EnableScheduling` cho cả ứng dụng.
- **Chưa làm, chờ nhóm:** kiểm tra bàn giao công việc khi điều chuyển. Hồ sơ của Cán bộ học vụ chưa có dữ liệu người đang giữ.

### Doanh thu và xuất báo cáo, người dùng yêu cầu ngày 2026-11-05

Nguồn: người dùng yêu cầu trong hội thoại. Bổ sung khối thống kê doanh thu trên tổng quan, trang Doanh thu, xuất Excel/PDF cho báo cáo doanh thu và báo cáo đăng ký gia sư/JLPT. Doanh thu là tổng giao dịch `SUCCESS` không gắn lịch học thử (`booking_id` rỗng), thời gian theo Asia/Ho_Chi_Minh. Admin và Manager được xem theo mô tả vai trò trong `RoleEnum`; vai trò khác bị từ chối. Chỉ đọc số liệu thật, chưa có giao dịch thì báo trống; không lộ mã hoặc payload cổng thanh toán. Luồng thanh toán chưa được xây dựng nên hiện chưa có nguồn ghi giao dịch. PDF dùng hộp thoại in của trình duyệt.

Cập nhật 2026-11-10: trang Doanh thu (`/pages/operations-revenue/`: tổng quan, theo gói, bảng "Giao dịch trong kỳ" lọc trạng thái, xuất Excel/PDF) có mục riêng **"Doanh thu & giao dịch"** trên sidebar Quản lý, ngay dưới "Báo cáo tổng hợp" (trước đây trang có nhưng thiếu link). "Báo cáo tổng hợp" vẫn chỉ là người dùng/hoạt động.

Cập nhật 2026-11-10 — thời gian chờ chung của `fetchAPI` (CSRF + lệnh) tăng từ 4,5 giây lên 12 giây vì DB Supabase ở xa có lúc trả lời chậm hơn 4,5 giây khiến trang báo "Chưa kết nối được hệ thống" dù thao tác đã lưu. Đây là mức tối đa trước khi báo lỗi, không làm chậm phản hồi nhanh.

Cập nhật 2026-11-10 — **Xóa tài khoản nhân viên** (người dùng chọn xóa mềm, không đổi DB): Admin có nút "Xóa" trên từng dòng của Danh sách nhân viên (không có với chính mình và tài khoản Quản trị viên). POST `/api/admin/operations/staff/{id}/delete` `{expectedUpdatedAt, reason}` (lý do bắt buộc, 409 nếu dữ liệu đã đổi): đặt `deleted_at` + `SUSPENDED` (ẩn khỏi danh sách, chặn đăng nhập và phiên đang mở), giữ nguyên lịch sử/yêu cầu hỗ trợ/nhật ký/giao dịch; gỡ ca làm CS từ hôm nay trở đi (ghi `REMOVE` vào `support_shift_events`). Nhật ký ghi hành động `STATUS` với lý do "Xóa tài khoản: …" vì ràng buộc `staff_account_audit_events.action` chỉ cho CREATE/UPDATE/ROLE/STATUS. Không khôi phục trên giao diện; email đã xóa không dùng lại để tạo tài khoản mới. Yêu cầu hỗ trợ đang dở của CS bị xóa vào nhóm "Cần bàn giao" (FE-10 FR-022).

### Chốt quyền đổi vai trò ngày 2026-11-04 — ưu tiên hơn mục 2026-11-03 nếu xung đột

Nguồn: người dùng chọn phương án "quản lý đề xuất, Admin duyệt" trong hội thoại.

| Thao tác | Người thực hiện | Quy tắc |
| --- | --- | --- |
| Đổi qua lại Cán bộ học vụ ↔ Chăm sóc khách hàng | Manager, không cần duyệt | Tài khoản đang hoạt động, không có đề xuất đang chờ; bắt buộc lý do |
| Thăng Cán bộ học vụ/Chăm sóc khách hàng lên Manager | Manager đề xuất, Admin duyệt/từ chối | Tài khoản đang hoạt động; mỗi nhân viên một đề xuất đang chờ; người đề xuất được hủy khi còn chờ; từ chối bắt buộc lý do; lúc duyệt kiểm tra lại trạng thái và vai trò |
| Gán vai trò khi tạo tài khoản | Admin | Cán bộ học vụ, Chăm sóc khách hàng hoặc Manager; không gán Admin |
| Đổi vai trò trực tiếp sau tạo | Admin không được | Thay đổi vai trò khi duyệt đề xuất là ngoại lệ duy nhất |
| Cấp quyền Admin | Không có trong các luồng này | |
| Hạ chức Manager | Chưa làm | Để sau; có thể dùng lại luồng đề xuất |

Đề xuất đã duyệt/từ chối/hủy không sửa được. Duyệt là một transaction gồm đổi vai trò, chốt đề xuất và ghi nhật ký với vai trò trước/sau, người đề xuất, người duyệt. Khi xung đột, bảng này thay thế dòng "Thăng chức/đổi vai trò", FR-019 và SC-008 ở mục 2026-11-03.

**Nhập tài khoản Excel (FR-020), người dùng duyệt ngày 2026-11-04:** tệp `.xlsx` tối đa 2 MB và 100 dòng; cột Họ và tên, Email, Vai trò, Số điện thoại (không bắt buộc). Chỉ tạo Cán bộ học vụ và Chăm sóc khách hàng. Còn dòng lỗi thì không tạo tài khoản nào. Email trùng trong tệp hoặc với tài khoản đã có (kể cả đã xóa) là lỗi, không ghi đè. (Mật khẩu: xem mục 2026-11-05.) Xác nhận kiểm tra lại quyền và toàn bộ dữ liệu; dữ liệu đổi sau xem trước thì không tạo gì.

### Phạm vi Admin cập nhật ngày 2026-11-03 — ưu tiên cho công việc tiếp theo

Nguồn: yêu cầu trực tiếp của người dùng trong hội thoại. Phần này thay thế mô tả cũ bên dưới nếu xung đột; không khẳng định chức năng mới đã được triển khai. Giữ yêu cầu và bằng chứng cũ để truy vết, không dùng quyền Admin đổi vai trò hoặc yêu cầu sức khỏe cũ để triển khai tiếp.

| Mục | Phạm vi hiện hành |
| --- | --- |
| Tổng quan hệ thống | Tóm tắt tài khoản người dùng, nhân viên, sự cố cần xử lý và hoạt động quản trị gần đây; liên kết đến màn chi tiết. Không còn khối giám sát sức khỏe. |
| Quản lý tài khoản nhân viên | Admin xem, tìm kiếm, tạo từng tài khoản hoặc nhập Excel, cập nhật thông tin và khóa/mở khóa. Gán vai trò ban đầu theo phân công được duyệt; không dùng sửa thông tin để đổi vai trò sau tạo. |
| Thăng chức/đổi vai trò | Manager thực hiện trong phạm vi được phép. Không tự nâng quyền hoặc cấp quyền Admin. Người bổ nhiệm Manager khác chưa chốt nên chưa mở luồng này. Admin không còn thao tác đổi vai trò sau tạo trong phạm vi đang chốt. |
| Cấu hình hệ thống | Giữ danh mục, giới hạn, lý do và lịch sử đã duyệt; không tự thêm tham số. |
| Quản lý sự cố kỹ thuật | Ghi nhận thủ công, nhận/phân công, diễn biến, kết quả, xác minh và đóng theo vòng đời hiện có; bổ sung xuất danh sách sự cố Excel để lưu/báo cáo. |
| Nhật ký hệ thống | Chỉ đọc, lọc người thực hiện/thời gian/nhóm thao tác; bao gồm tài khoản, đổi vai trò, cấu hình và sự cố; không lộ bí mật/log thô. |
| Thống kê hệ thống | Biểu đồ tài khoản theo vai trò, tài khoản mới, trạng thái tài khoản và số liệu sự cố; có kỳ, định nghĩa và nguồn thật. Không diễn giải tài khoản hoạt động thành đang online. |
| Thông báo toàn hệ thống | Admin soạn/gửi qua FE-12, không xây cơ chế gửi song song. |
| Hồ sơ cá nhân | Xem chính mình; cập nhật thông tin/đổi mật khẩu dùng chức năng chung của feature sở hữu khi được tích hợp. Trang chỉ đọc hiện tại chưa có chức năng sửa. |

**Loại khỏi phạm vi:** Giám sát tình trạng hệ thống, kiểm tra backend/cơ sở dữ liệu/dịch vụ, thời gian phản hồi, lần đo, tự làm mới/cảnh báo sức khỏe. User Story 2.1–2.2, FR-007–008 và SC-002 cũ không còn là tiêu chí hiện hành. Phần tổng hợp sự cố của Story 2/FR-009–010 vẫn giữ nhưng không kèm sức khỏe. Lượt chỉnh tài liệu chưa xóa code/dữ liệu.

**Tiếp tục để sau:** AI/tích hợp, điều khoản/chính sách (Story 4/FR-015/SC-007 phần tìm đọc chính sách), lượt truy cập/người online/thiết bị, quản lý tài khoản học viên/gia sư. Báo cáo hiệu quả kết nối/học thử và hoạt động kinh doanh thuộc Manager. Quy định tiếng Việt/an toàn thông tin của SC-007 vẫn áp dụng cho màn đang làm.

#### Yêu cầu bổ sung

- **FR-019 — Quyền nhân viên:** Admin còn hoạt động được tạo/nhập Excel/sửa thông tin/khóa/mở; Manager đổi vai trò sau tạo theo ma trận được chốt. Kiểm quyền hiện tại trên máy chủ từng thao tác, kể cả sửa thông tin và nhập tệp; không chỉ ẩn nút. Ghi trước/sau, tác giả, thời điểm và lý do theo cơ chế lịch sử hiện có. Không mở mọi quyền quản trị cho Manager. Chặn tự nâng quyền/cấp Admin và bổ nhiệm Manager khi chưa chốt thẩm quyền.
- **FR-020 — Nhập tài khoản Excel:** tải mẫu → chọn tệp → kiểm tra → xem trước → xác nhận tạo. Báo lỗi theo dòng/trường, email trùng trong tệp/đã tồn tại; không ghi đè tài khoản hoặc đổi vai trò người đã có. Xác nhận phải kiểm lại quyền/dữ liệu; kết quả phản ánh số tạo thực tế và dòng không tạo. Không ghi mật khẩu/bí mật vào nhật ký/báo cáo. Cột, cách cấp mật khẩu, giới hạn tệp và chính sách xử lý lô cần chốt trong hợp đồng trước code.
- **FR-021 — Xuất sự cố Excel:** Admin xuất toàn bộ kết quả phù hợp bộ lọc danh sách, không chỉ trang đang xem. Cột: mã sự cố, tiêu đề, mức độ, trạng thái, người phụ trách, ngày ghi nhận, kết quả xử lý. Trường chưa có để trống có ý nghĩa; thời gian Asia/Ho_Chi_Minh, nhãn tiếng Việt. File mở được bằng Excel và khớp nguồn tại thời điểm xuất, không sửa hồ sơ/lịch sử sự cố. Rỗng/lỗi phải thông báo đúng, không giả báo tải thành công. Nội dung người dùng nhập được xử lý như văn bản, không thành công thức. Đây là **xuất** hồ sơ đã ghi nhận trên hệ thống, không nhập sự cố từ Excel hoặc thay luồng ghi nhận bằng tệp.
- **FR-022 — Tổng quan/thống kê:** dashboard tóm tắt, trang thống kê có biểu đồ; tài khoản mới theo ngày tạo, cơ cấu vai trò/trạng thái theo thời điểm truy vấn và ghi rõ. Sự cố dùng FR-009–010 còn hiệu lực. Thiếu/lỗi không thay bằng 0; không tạo số liệu truy cập giả.

#### Nghiệm thu bổ sung

- **SC-008:** Admin tạo/nhập/khóa/mở nhưng không đổi vai trò sau tạo; Manager chỉ đổi vai trò được duyệt, không tạo/khóa/mở tài khoản, tự nâng quyền/cấp Admin. Luồng bổ nhiệm Manager chưa chốt bị chặn.
- **SC-009:** tệp nhân viên hợp lệ/sai trường/trùng email và dữ liệu đổi sau xem trước đều xử lý theo chính sách lô đã chốt; kết quả khớp số tạo thực, không lộ mật khẩu.
- **SC-010:** Excel sự cố khớp cột/bộ lọc qua nhiều trang; không sửa sự cố hoặc thực thi công thức; sai quyền/rỗng/lỗi xử lý đúng.
- **SC-011:** tổng quan/biểu đồ khớp số tính độc lập từ dữ liệu có mốc ngày/vai trò/trạng thái đã biết; không còn mục sức khỏe trong UI Admin sau triển khai.

**Trạng thái:** cập nhật đặc tả, chưa triển khai. Hợp đồng/task hoàn tất cũ mô tả bản cũ; phải thực hiện task chuyển đổi mới, cập nhật hợp đồng và kiểm chứng. Ma trận Manager và chi tiết lô import còn cần chốt trước phần phụ thuộc.

### Phạm vi được duyệt ngày 2026-11-01 — áp dụng cho phần việc tiếp theo

**Bổ sung trong lượt triển khai tiếp theo:** Người dùng yêu cầu làm báo cáo Manager và mở Hồ sơ cá nhân, sau đó nói “cái AI hãng từ từ đi”. Vì vậy phần tích hợp/giới hạn AI được để sau, không thêm cấu hình/quota hoặc schema AI. Báo cáo hiện tại lọc kết nối/học thử theo ngày tạo trong kỳ và nhóm theo trạng thái tại thời điểm truy vấn; không tái dựng trạng thái ở cuối kỳ lịch sử. Trang hồ sơ chỉ đọc thông tin tài khoản nội bộ đang sử dụng, không mở quyền xem hồ sơ người khác. Admin cũng được xem báo cáo tổng hợp; Manager không có quyền vận hành/quản lý nhân sự.

Nguồn cập nhật là bảng phân công FE-01..FE-12 do người dùng cung cấp và đề xuất đã được đồng ý trong hội thoại. Đây là yêu cầu mới, không khẳng định trích nguyên văn từ SRS cũ. Phần này thay thế giới hạn cũ “báo cáo chỉ về sức khỏe/sự cố” và bổ sung Manager là người xem báo cáo tổng hợp; các ghi chú cũ bên dưới được giữ để truy vết, không dùng để phủ định phạm vi mới. Các API quản trị đang có vẫn admin-only cho đến khi có endpoint báo cáo Manager được triển khai/kiểm chứng riêng.

**Mô tả thống nhất:** Admin quản lý tài khoản nhân sự nội bộ, gán vai trò nội bộ định sẵn, cấu hình hệ thống/tích hợp/giới hạn kỹ thuật AI, xem nhật ký quản trị và xử lý sự cố. Manager xem tổng hợp người dùng, báo cáo hoạt động và hiệu quả kết nối. FE-01 cung cấp xác thực và cơ chế kiểm quyền; FE-12 cung cấp việc gửi thông báo toàn hệ thống.

| Nhóm | Người dùng | Yêu cầu |
| --- | --- | --- |
| Tài khoản nhân viên | Admin | Danh sách, tìm kiếm, tạo/sửa, khóa/mở tài khoản nội bộ; kiểm phiên dữ liệu, ghi lịch sử cùng giao dịch. |
| Vai trò và quyền hạn | Admin | Hiển thị đủ 6 vai trò hiện có; biểu mẫu nhân viên chỉ gán 4 vai trò nội bộ. Không chuyển nhân viên sang learner/tutor hoặc tạo quyền tùy chỉnh từng nút khi chưa có cơ chế trong FE-01. |
| Cài đặt | Admin | Chỉ sửa danh mục tham số được duyệt, có đơn vị/giới hạn/lý do/lịch sử. Giữ các quy tắc booking đã xác nhận cho đến khi có thay đổi được duyệt. |
| Tích hợp/giới hạn AI | Admin | Theo dõi tích hợp; chỉnh giới hạn kỹ thuật đã thống nhất với FE-08. Danh mục cụ thể, đơn vị, phạm vi hợp lệ và thời điểm hiệu lực cần chốt trước khi triển khai. Không trả khóa bí mật ra UI. |
| Nhật ký | Admin | Xem tác giả, hành động, đối tượng, thời gian/kết quả; lọc/phân trang. Không sửa/xóa lịch sử và không hiển thị log thô/bí mật. |
| Sự cố | Admin | Ghi nhận, phân công, diễn biến, kết quả, xác minh và đóng; giữ quy tắc chuyển trạng thái đã có. |
| Báo sự cố | Manager | Bổ sung 2026-11-08: báo sự cố kỹ thuật cho Admin từ yêu cầu hỗ trợ được chuyển lên (vào trạng thái Chưa xử lý, người ghi nhận là Quản lý) và xem trạng thái các sự cố mình đã báo. Không nhận, xử lý hay đóng sự cố. |
| Báo cáo tổng hợp | Manager | Xem tổng hợp người dùng, hoạt động nền tảng và hiệu quả kết nối theo kỳ, có định nghĩa chỉ số và thời điểm chốt. Không đọc bí mật tích hợp hoặc ghi chú điều tra nội bộ. |

**Trách nhiệm phối hợp:** FE-11 cung cấp thao tác quản trị, FE-01 thực thi danh tính/quyền dùng chung; FE-02 quản lý hồ sơ learner/tutor; FE-08 thực hiện AI theo giới hạn; FE-12 nhận và gửi thông báo. Báo cáo lấy dữ liệu thông qua giao diện công khai của feature sở hữu, không làm lại nghiệp vụ hoặc truy cập repository chéo feature.

**Bộ chỉ số đưa vào thiết kế báo cáo:** tổng tài khoản chưa xóa, tài khoản mới trong kỳ, trạng thái hoạt động/khóa và nhóm vai trò; số yêu cầu kết nối và buổi học thử theo trạng thái trong kỳ; tổng yêu cầu kết nối tạo trong kỳ và số được chấp nhận/từ chối/hủy, tỷ lệ chấp nhận trên tổng yêu cầu tạo trong kỳ. Trạng thái tài khoản ACTIVE không được diễn giải thành số người truy cập gần đây. Cần xác nhận nguồn và quy tắc tính ở thời điểm chốt (đặc biệt yêu cầu đổi trạng thái nhiều lần), timezone, hợp đồng liên feature và xử lý mẫu số 0 trước khi lập trình báo cáo.

**Nghiệm thu bổ sung:** Admin thấy 6 vai trò, dropdown nhân viên chỉ có 4; API từ chối chuyển sang learner/tutor. Manager chỉ đọc báo cáo được cấp, không có quyền sửa cấu hình/nhân viên/sự cố; ngoại lệ duy nhất là báo sự cố mới cho Admin và xem sự cố mình đã báo (bổ sung 2026-11-08). Dữ liệu báo cáo khớp bộ kiểm thử có lịch sử đã biết; kỳ rỗng hiển thị số 0 có căn cứ, lỗi nguồn hiển thị lỗi. Thay đổi cấu hình ngoài danh mục bị từ chối. Không đánh dấu tính năng hoàn tất chỉ từ mock hoặc chế độ admin-dev.

**Ngoài phạm vi:** thu học phí/trả lương gia sư; làm lại đăng nhập/hồ sơ; quyền tùy chỉnh từng hành động; trình biên tập chính sách; xuất tệp báo cáo khi chưa được xác nhận; tự tạo nghiệp vụ hợp đồng. Chế độ mở thẳng dashboard chỉ dùng để kiểm thử cục bộ, không thay thế đăng nhập FE-01 khi tích hợp.

### Session 2026-10-29

- Q: Phần báo cáo vận hành của FE-11 cần bao gồm những nội dung nào? → A: Chỉ sức khỏe hệ thống và thống kê sự cố mới ghi nhận, đã giải quyết, còn mở, phân nhóm mức độ và trạng thái; không gồm thống kê sử dụng, doanh thu hoặc thanh toán.
- Q: Trong FE-11, quản trị viên được chỉnh những tham số đặt học thử nào? → A: Thời lượng học thử mặc định và thời gian tối thiểu đặt trước; dữ liệu nguồn hiện có lần lượt 45 phút và 12 giờ. Thời hạn giữ chỗ chưa thanh toán cố định 15 phút theo GB-14.
- Q: Hai tham số đặt học thử được phép thay đổi trong khoảng nào? → A: Thời lượng học thử mặc định là số nguyên từ 15 đến 120 phút; thời gian tối thiểu đặt trước là số nguyên từ 1 đến 168 giờ, bao gồm hai đầu khoảng.

### Session 2026-10-30

- Q: FE-11 có cần màn hình để quản trị viên soạn và công bố điều khoản/chính sách không? → A: Không; chỉ xây màn hình đọc, nội dung được cung cấp từ tài liệu đã duyệt qua quy trình quản lý tài liệu hiện có, không có trình biên tập hoặc công bố trong FE-11.
- Q: Khi hệ thống phát hiện một thành phần bị gián đoạn, có tự tạo hồ sơ sự cố không? → A: Không; hệ thống chỉ hiển thị trạng thái bất thường để quản trị viên kiểm tra và chủ động tạo hồ sơ sự cố.

## Yêu cầu và nghiệm thu hiện hành — đồng bộ 2026-11-05

Mục này và phạm vi hiện hành ở trên là nguồn triển khai. Các story, FR/SC cũ bên dưới được giữ để truy vết; yêu cầu cấu hình học thử, sức khỏe và phân công sự cố trong phần lịch sử không còn là tiêu chí triển khai mới. Kết quả nghiệm thu cũ không chứng nhận phạm vi mới.

### Cấu hình hệ thống

- **FR-023:** Admin còn hoạt động được đọc/sửa tên hiển thị (1–100 ký tự), email hỗ trợ (đúng định dạng, tối đa 254 ký tự) và số điện thoại hỗ trợ (tối đa 20 ký tự, cho phép dấu + đầu chuỗi, chữ số, khoảng trắng, dấu ngoặc và dấu gạch). Các trường bắt buộc, không chỉ khoảng trắng; chuẩn hóa khoảng trắng đầu/cuối. Tên hiển thị không thay tên miền, địa chỉ API hoặc nội dung pháp lý đã duyệt. Không tự tạo thông tin liên hệ minh họa khi chưa có dữ liệu được nhóm cung cấp.
- **FR-024:** Các vị trí hiển thị thông tin chung được tích hợp, gồm footer/trang liên hệ nếu có trong code hiện tại, lấy từ cùng nguồn cấu hình. Thay đổi thành công có hiệu lực ở lần tải dữ liệu tiếp theo, không cần khởi động lại ứng dụng. Thiếu cấu hình và lỗi tải phải phân biệt; không báo thành công nếu nơi sử dụng chưa tích hợp. Không yêu cầu tạo trang liên hệ mới chỉ để dùng cấu hình.
- **FR-025:** Admin chỉnh giới hạn tệp đính kèm hỗ trợ bằng số nguyên 1–10 MB; MB ở đây là 1.048.576 byte. Backend kiểm tra khi tải lên, giao diện đọc cùng giới hạn. Thay đổi chỉ tác động lần tải mới, không xóa/vô hiệu tệp đã lưu; không thay định dạng tệp được phép hoặc trần hạ tầng. Loại tệp chưa tích hợp không có ô chỉnh.
- **FR-026:** Mọi cập nhật cấu hình kiểm quyền hiện tại ở server, yêu cầu phiên bản và lý do 1–2.000 ký tự không trắng; giao diện xác nhận giá trị trước/sau. Lưu dữ liệu và lịch sử nguyên tử. Dữ liệu lỗi bị từ chối; phiên bản cũ không ghi đè; lịch sử chỉ đọc gồm tác giả, thời điểm, lý do, trước/sau. Không có mật khẩu/token/khóa API trong cấu hình công khai.
- **SC-012:** Đổi thông tin nền tảng bằng tài khoản thật rồi đối chiếu các vị trí đã tích hợp; kiểm thiếu/lỗi nguồn, sai định dạng, sai quyền, dữ liệu cũ và rollback khi ghi lịch sử thất bại.
- **SC-013:** Đổi giới hạn tệp rồi tải dưới/đúng/vượt giới hạn; gọi trực tiếp API vẫn bị kiểm tra; frontend/backend cùng giá trị; tệp cũ còn truy cập theo quyền hiện có.

### Sự cố, tài khoản và báo cáo

- **FR-027:** Bỏ phân công/phân công lại; chỉ tự nhận sự cố chưa có người phụ trách. Hai Admin nhận cùng lúc chỉ một người thành công. Người phụ trách trả lại danh sách chờ với lý do; Admin khác chỉ làm được khi người phụ trách không còn hoạt động. Trả lại bỏ người phụ trách, đưa về Mới ghi nhận và ghi lịch sử, giữ nguyên diễn biến/kết quả cũ. Phiếu đã đóng không được trả lại. Lịch sử phân công cũ chỉ dùng tra cứu.
- **FR-028:** Tạo tài khoản không nhận mật khẩu từ Admin. Có trạng thái gửi email chờ/đã gửi/thất bại và hàng đợi bền vững; không lưu mật khẩu rõ trong hàng đợi. API FE-11 chặn người chưa đổi mật khẩu lần đầu; việc chặn API feature khác thuộc FE-01. Tạo Manager cần căn cứ bổ nhiệm. Ghi rõ tạo tài khoản và gửi thư là hai kết quả riêng.
- **FR-029:** Báo cáo học thử offline chỉ dùng trạng thái có nguồn ghi nhận; không tự suy ra hoàn thành từ giờ hẹn đã qua. Báo cáo tiền mua gói chỉ cộng giao dịch mua gói thành công; không có chỉ số/bộ lọc/biểu đồ hoàn tiền. FE-11 không chỉnh gói. Phân biệt kỳ rỗng, chưa tích hợp và lỗi nguồn.
- **SC-014:** Kiểm API/UI không còn phân công, nhận đồng thời, trả lại đúng quyền, phiếu đóng và lịch sử cũ. Kiểm đăng nhập thật bằng mật khẩu tạm, retry email và thay đổi quyền trong phiên đang dùng.

**Hoãn/phụ thuộc:** bảo trì chờ code mới; AI/chính sách tiếp tục hoãn; kiểm tra bàn giao công việc chờ hợp đồng feature sở hữu. Không đánh dấu các phần này hoàn thành. Giới hạn 100/254/20 ký tự là ràng buộc triển khai của bản đặc tả này, không khẳng định trích nguyên văn SRS.

## User Scenarios & Testing *(mandatory)* — lịch sử trước lần đồng bộ 2026-11-05

### User Story 1 - Quản lý cấu hình chính sách đặt học thử (Priority: P1)

Quản trị viên xem và cập nhật các tham số thực thi chính sách đặt học thử đã được phê duyệt, biết thay đổi có hiệu lực thế nào và truy được lịch sử (11.1, UC-101).

**Why this priority**: Cấu hình sai ảnh hưởng trực tiếp đến đặt lịch và người dùng; không cho thay quy tắc đã phê duyệt chỉ bằng một thao tác nhập giá trị.

**Independent Test**: Với một tham số được phép điều chỉnh trong danh mục chính sách đã duyệt, quản trị viên sửa giá trị hợp lệ, xác nhận lưu và thực hiện một yêu cầu đặt học thử mới để kiểm chứng giá trị mới; lịch đã tạo trước đó không bị tính lại.

**Acceptance Scenarios**:

1. **Given** quản trị viên còn hoạt động, **When** mở cấu hình, **Then** thấy danh mục tham số với nhãn, ý nghĩa, đơn vị, giá trị hiện hành, giới hạn hợp lệ và thời điểm hiệu lực; tham số cố định hiển thị chỉ đọc.
2. **Given** tham số được phép điều chỉnh có giới hạn đã xác định, **When** nhập giá trị hợp lệ, lý do và xác nhận lưu, **Then** toàn bộ lần thay đổi được lưu, có lịch sử trước/sau và chỉ áp dụng cho yêu cầu đặt học thử mới sau thời điểm hiệu lực.
3. **Given** thiếu lý do, giá trị ngoài giới hạn hoặc tham số cố định, **When** cố lưu, **Then** bị từ chối, chỉ rõ lỗi bằng tiếng Việt và giá trị có hiệu lực không đổi.
4. **Given** hai quản trị viên cùng mở một phiên bản, **When** người thứ hai lưu sau khi người thứ nhất đã lưu, **Then** báo cấu hình đã thay đổi, yêu cầu tải lại và không ghi đè âm thầm.
5. **Given** thời hạn giữ chỗ chưa thanh toán được GB-14 quy định là 15 phút, **When** xem hoặc cố sửa giá trị này, **Then** thấy giá trị 15 phút chỉ đọc; muốn đổi phải cập nhật chính sách được phê duyệt ngoài tác vụ cấu hình này.

### User Story 2 - Theo dõi sức khỏe và tổng quan vận hành (Priority: P1)

Quản trị viên theo dõi khả dụng, hiệu năng và tình trạng các thành phần phục vụ hệ thống, đồng thời xem tổng hợp sự cố để biết cần can thiệp ở đâu (11.2, UC-102).

**Why this priority**: Cần phát hiện suy giảm và phân biệt dữ liệu thiếu với hệ thống hoạt động bình thường.

**Independent Test**: Quan sát một thành phần hoạt động, mô phỏng thành phần đó mất kết nối trong môi trường kiểm thử, làm mới màn hình và kiểm tra trạng thái/thời điểm; không cần thay cấu hình hoặc tạo sự cố để xem sức khỏe.

**Acceptance Scenarios**:

1. **Given** có kết quả quan sát mới, **When** quản trị viên mở tổng quan, **Then** thấy từng thành phần, trạng thái hoạt động/suy giảm/gián đoạn/chưa xác định, lần kiểm tra gần nhất và thời gian phản hồi quan sát được.
2. **Given** một thành phần không trả lời hoặc dữ liệu quá hạn, **When** làm mới, **Then** thành phần đó không hiển thị như đang hoạt động bình thường; phân biệt kiểm tra thất bại với chưa có dữ liệu, các phần còn lại vẫn xem được khi khả dụng.
3. **Given** có sự cố trong khoảng ngày được chọn, **When** xem tổng hợp vận hành, **Then** thấy số sự cố ghi nhận, số đã giải quyết trong kỳ và số còn mở tại cuối kỳ, phân nhóm mức độ và trạng thái, có định nghĩa cách tính và thời điểm chốt số liệu.
4. **Given** kỳ không có sự cố hoặc không thể lấy số liệu, **When** xem tổng hợp, **Then** lần lượt hiển thị số 0 có căn cứ hoặc thông báo không lấy được dữ liệu, không thay lỗi bằng số 0.

### User Story 3 - Điều tra và ghi nhận xử lý sự cố kỹ thuật (Priority: P2)

Quản trị viên ghi nhận sự cố, nhận trách nhiệm điều tra, lưu diễn biến và kết quả khắc phục, đóng khi đã xác minh phục hồi (11.3, UC-103).

**Why this priority**: Giúp xử lý có người phụ trách, giữ lịch sử và chứng cứ phục hồi thay vì chỉ đổi trạng thái trên bảng điều khiển.

**Independent Test**: Tạo sự cố thủ công từ vấn đề kiểm thử, nhận xử lý, ghi diễn biến, kết quả và xác minh, đóng rồi mở lại chi tiết để đối chiếu lịch sử. Không phụ thuộc việc tự phát hiện sự cố.

**Acceptance Scenarios**:

1. **Given** quản trị viên phát hiện lỗi, **When** nhập tiêu đề, mô tả, mức độ và thành phần bị ảnh hưởng, **Then** tạo một sự cố mới với người ghi nhận và thời điểm thực tế.
2. **Given** sự cố chưa có người xử lý, **When** quản trị viên nhận, **Then** người đó thành người phụ trách và trạng thái chuyển sang “Đang xử lý”.
3. **Given** đang phụ trách sự cố, **When** ghi hành động điều tra/khắc phục, **Then** diễn biến được bổ sung theo thứ tự, lưu tác giả và thời điểm, không sửa mất lịch sử cũ.
4. **Given** đã khắc phục, **When** người phụ trách nhập kết quả và bằng chứng xác minh, **Then** sự cố thành “Đã giải quyết”; chỉ sau bước xác nhận đóng mới thành “Đã đóng”.
5. **Given** đánh dấu đã giải quyết nhưng kiểm tra phát hiện chưa phục hồi trước khi đóng, **When** người phụ trách ghi lý do xử lý tiếp, **Then** trở về “Đang xử lý” và giữ toàn bộ lịch sử.
6. **Given** người dùng thông thường hoặc nhân viên không phải quản trị viên, **When** cố xem hoặc cập nhật hồ sơ sự cố nội bộ, **Then** bị từ chối và không nhận được thông tin điều tra.

### User Story 4 - Xem điều khoản và chính sách hệ thống (Priority: P2)

Học viên và gia sư xem điều khoản sử dụng, chính sách sử dụng và quy định hiện hành ở một nơi dễ tìm, biết phiên bản và ngày hiệu lực (11.4, UC-43).

**Why this priority**: Người dùng cần hiểu quyền, nghĩa vụ và quy định được áp dụng; nội dung không do ứng dụng tự bịa ra.

**Independent Test**: Đăng nhập bằng learner và tutor, mở danh sách chính sách và đọc bản hiện hành đã được phê duyệt; thử tài liệu chưa công bố để xác minh không bị lộ.

**Acceptance Scenarios**:

1. **Given** có bản đã công bố và có hiệu lực, **When** học viên/gia sư mở danh mục và chọn tài liệu, **Then** đọc được tiêu đề, nội dung tiếng Việt, phiên bản và ngày hiệu lực đúng bản hiện hành.
2. **Given** có bản nháp hoặc bản có hiệu lực trong tương lai chưa được phép hiển thị, **When** người dùng tìm hoặc truy cập trực tiếp, **Then** không nhận được nội dung đó.
3. **Given** chưa có tài liệu hợp lệ hoặc tải lỗi, **When** mở trang, **Then** hiển thị trạng thái rỗng hoặc lỗi tương ứng, không có điều khoản mẫu giả làm nội dung được duyệt.

### Edge Cases

- Tài khoản bị khóa, xóa hoặc đổi vai trò sau đăng nhập: kiểm quyền hiện tại, không dựa vào nút đã hiển thị trước đó.
- Lưu cấu hình hoặc sự cố thất bại: không báo thành công và không để trạng thái/lịch sử chỉ cập nhật một phần.
- Dữ liệu quan sát quá hạn, thành phần không có nguồn đo hoặc chỉ một phần nguồn bị lỗi: đánh dấu rõ, giữ thời điểm lần đo thực tế; không dùng thời điểm mở trang giả làm thời điểm đo.
- Kỳ báo cáo đảo ngày, chưa kết thúc hoặc vượt giới hạn cho phép: từ chối hoặc chốt đến thời điểm hiện tại theo quy định trong FR-009; không ngầm đổi ngày người dùng chọn.
- Hai người cùng nhận sự cố hoặc cập nhật cấu hình/sự cố: chỉ chấp nhận thao tác dựa trên phiên bản hiện tại, người còn lại tải lại.
- Sự cố đã đóng tái diễn: tạo hồ sơ mới có liên kết hồ sơ trước; không sửa lịch sử hoặc mở lại hồ sơ đã đóng trong phạm vi này.
- Mất dữ liệu sức khỏe không tự đóng, giải quyết hoặc tạo sự cố; quản trị viên xác định và ghi nhận thực tế.
- Không có danh mục tham số được duyệt: màn hình chỉ đọc/hiển thị thiếu cấu hình, không cung cấp ô nhập tùy ý cho khóa cấu hình nội bộ.
- Hai bản chính sách cùng loại cùng được đánh dấu hiện hành và chồng lấn hiệu lực: báo không xác định được bản hiện hành, không chọn bản ngẫu nhiên.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: PHẢI giới hạn cấu hình, sức khỏe, tổng hợp vận hành và sự cố cho quản trị viên còn hoạt động; kiểm quyền mỗi lần đọc/ghi và từ chối mọi vai trò khác, kể cả quản lý vận hành (ngoại lệ bổ sung 2026-11-08: Quản lý được báo sự cố mới và xem sự cố mình đã báo, xem bảng phạm vi hiện hành). Chức năng đọc chính sách dành cho học viên/gia sư đã đăng nhập. Kiểm chứng: ma trận vai trò trên bốn hành trình và tài khoản bị thu hồi quyền.
- **FR-002**: Danh mục cấu hình đặt học thử PHẢI chỉ cho chỉnh hai tham số: thời lượng học thử mặc định (đơn vị phút) và thời gian tối thiểu đặt trước giờ bắt đầu (đơn vị giờ). Dữ liệu nguồn dự án hiện có lần lượt 45 phút và 12 giờ; khi mở màn hình phải đọc giá trị đang có hiệu lực, không tự ghi đè về giá trị nguồn. PHẢI hiển thị nhãn, ý nghĩa, kiểu giá trị, đơn vị, giá trị hiện tại, giới hạn, khả năng chỉnh sửa và phiên bản/thời điểm hiệu lực. Không cho nhập khóa tùy ý hoặc sửa tham số khác qua chức năng này. Kiểm chứng: Story 1.1, chỉnh từng tham số và thử gửi thay đổi tham số ngoài danh mục.
- **FR-003**: PHẢI chỉ nhận thời lượng học thử mặc định là số nguyên từ 15 đến 120 phút và thời gian tối thiểu đặt trước là số nguyên từ 1 đến 168 giờ, bao gồm hai đầu khoảng. Từ chối số thập phân, giá trị không phải số, giá trị thiếu hoặc ngoài khoảng; không tự làm tròn hay ép về giới hạn. Yêu cầu lý do thay đổi 1–2.000 ký tự không chỉ khoảng trắng và xác nhận trước lưu; thay đổi một lần phải thành công hoặc thất bại toàn bộ. Không thay chính sách được phê duyệt bằng thay tham số. Kiểm chứng: Story 1.2–1.3, giá trị tại biên/ngoài biên, số thập phân, lỗi lưu và biên độ dài lý do.
- **FR-004**: Cấu hình mới PHẢI có hiệu lực ngay sau lưu thành công và chỉ áp dụng cho yêu cầu đặt học thử mới; hồ sơ đã tạo giữ quy tắc có hiệu lực lúc tạo. PHẢI hiển thị thời điểm hiệu lực và lịch sử người sửa, trước/sau, thời điểm, lý do. Kiểm chứng: tạo yêu cầu trước/sau thay đổi và Story 1.2.
- **FR-005**: Thời hạn giữ chỗ chưa thanh toán PHẢI hiển thị 15 phút chỉ đọc theo GB-14. Không có quyền ghi đè quy tắc này trong FE-11 khi chưa có chính sách thay thế được duyệt. Kiểm chứng: Story 1.5 và thử gửi thay đổi trực tiếp.
- **FR-006**: PHẢI phát hiện lưu cấu hình/sự cố trên phiên bản lỗi thời và từ chối ghi đè; lịch sử thay đổi không có thao tác sửa/xóa trong FE-11. Kiểm chứng: hai quản trị viên cùng thao tác và đối chiếu lịch sử trước/sau.
- **FR-007**: PHẢI cho quản trị viên xem trạng thái từng thành phần được quản lý, lần quan sát gần nhất và thời gian phản hồi của lần đo. Tên thành phần dùng nhãn tiếng Việt; nguồn không đo được hiển thị chưa xác định. Kiểm chứng: Story 2.1–2.2, mô phỏng hoạt động/mất kết nối/không có nguồn.
- **FR-008**: Trang sức khỏe PHẢI cho làm mới thủ công, cập nhật tự động mỗi 30 giây khi đang mở; quan sát hơn 60 giây đánh dấu quá hạn. Kết quả kiểm tra thành công dùng trạng thái hoạt động, không khả dụng dùng gián đoạn; suy giảm chỉ khi nguồn quan sát có kết luận suy giảm rõ ràng. Phát hiện bất thường chỉ cập nhật thông tin hiển thị, không tự tạo hồ sơ sự cố; quản trị viên chủ động kiểm tra và tạo hồ sơ theo FR-011. Kiểm chứng: giữ trang mở, ngắt nguồn đo, đối chiếu thời điểm và trạng thái qua mốc 60 giây; xác nhận số hồ sơ không tăng cho tới khi quản trị viên tạo.
- **FR-009**: PHẢI cho chọn kỳ tổng hợp từ ngày–đến ngày theo Asia/Ho_Chi_Minh, tối đa 366 ngày, không nhận ngày tương lai; ngày bắt đầu không sau ngày kết thúc. Với ngày hiện tại, chốt đến thời điểm lập báo cáo. Mặc định 7 ngày gồm hôm nay. PHẢI hiển thị kỳ và thời điểm chốt. Kiểm chứng: Story 2.3, biên ngày/tháng/năm và kỳ sai.
- **FR-010**: Phạm vi báo cáo vận hành PHẢI chỉ gồm sức khỏe hệ thống và thống kê sự cố, không gồm thống kê sử dụng, doanh thu hoặc thanh toán. Tổng hợp sự cố PHẢI gồm số sự cố tạo trong kỳ, số chuyển sang đã giải quyết trong kỳ và số chưa đóng tại cuối kỳ; có phân nhóm mức độ và trạng thái tại thời điểm chốt. Một sự cố giải quyết nhiều lần trong kỳ chỉ tính một lần trong chỉ số đã giải quyết. Số còn mở tính mọi sự cố được tạo trước hoặc tại cuối kỳ chưa đóng lúc đó, kể cả tạo trước ngày đầu kỳ; trạng thái lịch sử phải được dùng để tái lập kỳ quá khứ. Kiểm chứng: Story 2.3–2.4 với bộ sự cố có mốc tạo/giải quyết/mở lại/đóng đã biết và đối chiếu phạm vi chỉ số được hiển thị.
- **FR-011**: PHẢI tạo sự cố với tiêu đề 1–200 ký tự, mô tả 1–10.000 ký tự không chỉ khoảng trắng, mức độ thấp/bình thường/cao/nghiêm trọng và một hoặc nhiều thành phần bị ảnh hưởng trong danh mục; lưu người ghi nhận và thời điểm, mặc định “Mới ghi nhận”. Kiểm chứng: Story 3.1 và từng trường thiếu/biên giới hạn.
- **FR-012**: PHẢI cho quản trị viên nhận sự cố chưa có người phụ trách, ghi diễn biến và kết quả khi là người phụ trách; mọi quản trị viên được xem và phân công lại sự cố chưa đóng cho quản trị viên khác còn hoạt động với lý do 1–2.000 ký tự không trắng. Lưu người thực hiện, người phụ trách cũ/mới, thời điểm và lý do. Kiểm chứng: Story 3.2–3.3 và phân công lại/kiểm quyền người cũ.
- **FR-013**: Vòng đời PHẢI chỉ cho mới ghi nhận → đang xử lý khi nhận; đang xử lý → đã giải quyết khi có kết quả khắc phục và mô tả xác minh; đã giải quyết → đang xử lý khi người phụ trách ghi lý do chưa phục hồi; đã giải quyết → đã đóng khi người phụ trách xác nhận đóng. Kết quả/xác minh/diễn biến 1–10.000 ký tự không trắng; lý do xử lý tiếp 1–2.000 ký tự không trắng. Phiếu đóng không nhận ghi mới; tái diễn tạo hồ sơ liên kết. Kiểm chứng: Story 3.2–3.5 và tất cả chuyển đổi bị cấm.
- **FR-014**: Sự cố PHẢI có danh sách lọc theo trạng thái, mức độ, thành phần, người phụ trách và thời điểm ghi nhận; danh sách/lịch sử chia trang 20 mục mặc định, tối đa 50, thứ tự rõ ràng và ổn định. Chi tiết PHẢI giữ trình tự diễn biến, tác giả, thời điểm và thay đổi trạng thái/phân công; không có chức năng thực thi lệnh máy chủ hoặc sửa dữ liệu nghiệp vụ từ hồ sơ sự cố. Kiểm chứng: danh sách theo từng bộ lọc, phân trang và Story 3.3–3.5.
- **FR-015**: PHẢI cho học viên/gia sư xem danh mục và nội dung điều khoản/chính sách đã công bố, có hiệu lực; mỗi loại chỉ có một bản hiện hành. Hiển thị tên, phiên bản, ngày hiệu lực và nội dung được duyệt, không có bản nháp/bản chưa hiệu lực qua truy cập trực tiếp. Nội dung do quy trình quản lý tài liệu hiện có cung cấp từ tài liệu đã duyệt; FE-11 chỉ đọc, không có màn hình hoặc thao tác soạn, chỉnh sửa, phê duyệt hay công bố chính sách. Kiểm chứng: Story 4.1–4.3, trường hợp chồng lấn và đối chiếu phạm vi chức năng chỉ đọc.
- **FR-016**: PHẢI dùng dữ liệu cấu hình, quan sát, sự cố và chính sách thực tế; phân biệt số 0 hợp lệ, thiếu dữ liệu và lỗi tải. Không dùng dữ liệu giả hoặc tự soạn điều khoản để lấp chỗ trống. Kiểm chứng: các nguồn rỗng/lỗi của Stories 1–4.
- **FR-017**: Mọi nhãn, thông báo và trạng thái hệ thống PHẢI bằng tiếng Việt, sentence case hoặc title case phù hợp; không lộ khóa cấu hình thô, mã lỗi, exception, địa chỉ kết nối nội bộ, mật khẩu, token hoặc log thô lên UI. Chỉ trình bày thông tin vận hành đã chuyển thành nhãn và mô tả người dùng hiểu được. Nội dung nhập tự do hiển thị an toàn. Kiểm chứng: rà màn hình cả bốn vai trò, thông báo lỗi và chuỗi nhập nguy hiểm.
- **FR-018**: Mọi thay đổi cấu hình, nhận/phân công sự cố, diễn biến, kết quả và chuyển trạng thái PHẢI lưu cùng lịch sử hoặc không lưu gì nếu thất bại; hành động theo dõi không tự khởi động lại, triển khai phần mềm, hoàn tiền, đổi lịch hay cập nhật hồ sơ người dùng. Kiểm chứng: mô phỏng lỗi lưu/lịch sử và đối chiếu dữ liệu nghiệp vụ không liên quan.

### Key Entities *(include if feature involves data)*

- **Định nghĩa tham số chính sách**: Tên hiển thị, ý nghĩa, đơn vị, loại giá trị, giới hạn được duyệt, có được chỉnh hay không và căn cứ chính sách.
- **Phiên bản cấu hình có hiệu lực**: Tập giá trị, thời điểm hiệu lực, người cập nhật, lý do và lịch sử trước/sau; gắn quy tắc với yêu cầu đặt học thử tạo mới.
- **Thành phần vận hành**: Tên dễ hiểu, vai trò trong hệ thống, có nguồn quan sát hay không; không phải danh sách địa chỉ kết nối công khai.
- **Kết quả quan sát**: Thành phần, trạng thái, thời gian quan sát, thời gian phản hồi, kết luận suy giảm nếu có và trạng thái dữ liệu quá hạn.
- **Sự cố kỹ thuật**: Tiêu đề, mô tả, mức độ, thành phần ảnh hưởng, người ghi nhận/phụ trách, trạng thái và thời điểm, liên kết sự cố tái diễn nếu có.
- **Diễn biến và lịch sử sự cố**: Người thực hiện, thứ tự, thời điểm, hành động, trạng thái/phân công trước-sau, kết quả và mô tả xác minh.
- **Tổng hợp vận hành**: Kỳ, thời điểm chốt, các chỉ số định nghĩa tại FR-010; là tổng hợp từ lịch sử thực tế, không số liệu nhập tùy ý.
- **Bản điều khoản/chính sách**: Loại, tiêu đề, phiên bản, nội dung được duyệt, trạng thái công bố và ngày hiệu lực.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% ca thay đổi cấu hình hợp lệ/không hợp lệ, cố sửa tham số cố định và lưu đồng thời cho kết quả đúng FR-002–006; yêu cầu đã có không bị áp dụng lại quy tắc mới.
- **SC-002**: Trong nghiệm thu nguồn quan sát kiểm soát được, 100% ca thành phần hoạt động/mất kết nối/nguồn thiếu hiển thị đúng sau lần làm mới kế tiếp; mọi kết quả hơn 60 giây đều được đánh dấu quá hạn.
- **SC-003**: 100% chỉ số tổng hợp khớp số tính độc lập từ bộ sự cố có lịch sử đã biết, bao gồm kỳ quá khứ, giải quyết nhiều lần và sự cố tồn từ trước kỳ; không lấy lỗi dữ liệu làm số 0.
- **SC-004**: 100% kịch bản sự cố hoàn thành ghi nhận → nhận → xử lý → giải quyết → đóng, lưu đủ tác giả/thời điểm; chuyển đổi cấm và thao tác ghi sai quyền đều bị từ chối.
- **SC-005**: 100% ca truy cập quản trị bằng vai trò khác/tài khoản không hoạt động bị từ chối; học viên/gia sư không thấy hồ sơ sự cố nội bộ hoặc chính sách chưa công bố/chưa hiệu lực.
- **SC-006**: Với 100 người dùng đồng thời trong 10 phút và 1.000 sự cố có lịch sử, ít nhất 95% lần mở danh sách, chi tiết và tổng hợp hiển thị trong 2 giây; đo đến lúc nội dung hiển thị, không chỉ thời gian nguồn trả lời. Nguồn gián đoạn hiển thị trạng thái chưa xác định/lỗi trong tối đa 5 giây thay vì chờ vô hạn.
- **SC-007**: Ít nhất 9/10 học viên/gia sư tham gia nghiệm thu tìm và đọc đúng chính sách hiện hành trong 2 phút không cần trợ giúp; mọi màn hình kiểm tra có tiếng Việt đúng dấu và không lộ nội dung kỹ thuật bị cấm.

## Assumptions

- Nguồn chính: `Report-3_Software Requirement Specification.docx`, mục 11, bảng UC-101/102/103 và UC-43, cùng GB-14. Không tìm thấy mô tả FE-11 riêng theo mã trong phần văn bản đọc được; không khẳng định các mặc định bên dưới có nguyên văn trong SRS.
- **Sai lệch nguồn**: Nội dung chi tiết 11.1–11.4 lặp mô tả cài đặt thông báo và tác nhân “All User”, trái tên use case/bảng tác nhân. Đặc tả dùng bảng use case để xác định admin và learner/tutor; không đưa cài đặt thông báo vào FE-11, không sửa tài liệu nguồn.
- Truy vết: 11.1/UC-101 → Story 1; 11.2/UC-102 → Story 2; 11.3/UC-103 → Story 3; 11.4/UC-43 → Story 4.
- **Phạm vi báo cáo đã xác nhận**: Chỉ sức khỏe hệ thống và thống kê sự cố theo Clarifications; không gồm thống kê sử dụng, doanh thu hoặc thanh toán. SRS chưa liệt kê use case báo cáo riêng; không tự tạo mã use case mới. Xuất tệp và báo cáo cho quản lý vẫn ngoài phạm vi bản này.
- **Danh mục cấu hình đã xác nhận**: Chỉ chỉnh thời lượng học thử mặc định (số nguyên 15–120 phút) và thời gian tối thiểu đặt trước (số nguyên 1–168 giờ), bao gồm các giá trị biên. Các giới hạn này đã được người dùng đồng ý trong Clarifications, không khẳng định có nguyên văn trong SRS. Giá trị 45 phút/12 giờ lấy từ dữ liệu nguồn trong DatabasePgAdmin4.txt, không phải bằng chứng giá trị đang chạy. Không tự thêm tham số tài chính hoặc thời hạn hủy/hoàn tiền. GB-14 cố định 15 phút, không được sửa. Nếu thiếu định nghĩa/giá trị hợp lệ, giữ chế độ chỉ đọc thay vì tự tạo cấu hình.
- Mặc định đề xuất để nghiệm thu: cấu hình áp dụng ngay cho yêu cầu mới, sức khỏe làm mới 30 giây/quá hạn 60 giây, kỳ báo cáo 7 ngày mặc định/tối đa 366 ngày, bốn mức độ và vòng đời sự cố theo FR-013. Đây chưa phải quyết định người dùng đã xác nhận qua clarify.
- Phụ thuộc nguồn dữ liệu đặt học thử, cơ chế xác thực/vai trò, nguồn quan sát vận hành và nội dung chính sách đã được người có trách nhiệm duyệt. Không thể tự coi chưa có dữ liệu là hệ thống khỏe.
- Danh mục thành phần lấy từ các dịch vụ thực tế đang dùng; có thể gồm ứng dụng chính, nơi lưu dữ liệu và dịch vụ hỗ trợ nếu có nguồn quan sát. Không thêm dịch vụ bên ngoài chỉ để có số liệu; phạm vi thành phần chốt tại plan theo cấu hình triển khai.
- Người dùng đã xác nhận không tự tạo sự cố từ kết quả giám sát: quản trị viên chủ động tạo hồ sơ sau khi kiểm tra trạng thái bất thường. Việc điều tra/khắc phục trong hồ sơ là ghi nhận hành động và xác minh kết quả. Không bao gồm terminal điều khiển từ xa, tự restart, deploy, backup/restore, thay schema, sửa tài khoản, FE-10 ticket hoặc xử lý thanh toán.
- Phạm vi chỉ đọc chính sách đã được người dùng xác nhận trong Clarifications: nội dung do quy trình quản lý tài liệu hiện có cung cấp từ tài liệu đã duyệt, không xây trình biên tập/phê duyệt/xuất bản trong FE-11. Không bao gồm bắt buộc chấp thuận lại hoặc lưu chữ ký pháp lý.
- Không tự thêm quyền guest/manager xem trang quản trị. Mức kiểm tra và mục tiêu tải ở SC-006 là đề xuất nghiệm thu, không phải kết quả đã đo; fixture chỉ trong môi trường kiểm thử cách ly.
- Tuân thủ constitution, CONVENTIONS.md, DESIGN.md và UI_LOCALIZATION_RULES.md; không sửa khối bất biến hoặc chọn công nghệ triển khai trong đặc tả.
