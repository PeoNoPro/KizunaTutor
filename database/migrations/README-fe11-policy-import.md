# Nạp tài liệu chính sách đã duyệt

FE-11 chỉ đọc, không có màn hình hoặc API biên tập/công bố. Hiện chưa có tài liệu thật được người phụ trách cung cấp; không dùng fixture kiểm thử thay nội dung được duyệt.

Người quản lý tài liệu chuẩn bị: loại `terms`, `usage-policy` hoặc `regulations`; tiêu đề; nhãn phiên bản; nội dung văn bản nguyên bản đã duyệt; ngày phê duyệt; căn cứ nguồn và khoảng hiệu lực có múi giờ. Nội dung chỉ được hiển thị dạng văn bản, không thực thi HTML.

Quy trình triển khai có kiểm soát:

1. Xác minh nguồn phê duyệt, UTF-8 không BOM và thông tin truy nguồn; lưu bản đã duyệt theo quy trình tài liệu của nhóm. Không tự soạn nội dung pháp lý để nhập thử.
2. Backup/restore thử theo README-fe11.md. Dùng tài khoản triển khai riêng, không đưa mật khẩu vào script hoặc Git.
3. Trong transaction, khóa danh mục: `SELECT pg_advisory_xact_lock(11001002);`. Mọi lần import đều dùng khóa này.
4. Dùng câu lệnh tham số hóa của công cụ triển khai để kiểm không chồng lấn: cùng document_type, publication_state='PUBLISHED', và `tstzrange(effective_from,effective_to,'[)') && tstzrange(:from,:to,'[)')`. Nếu có kết quả thì dừng; không tự chọn tài liệu ưu tiên.
5. Nếu thay bản đang hiệu lực, chỉ kết thúc khoảng hiệu lực cũ khi người phụ trách đã duyệt mốc chuyển tiếp; giữ nguyên nội dung/phiên bản cũ. Nạp UUID mới, title/version_label/approved_content/source_reference/approved_at thật. effective_to là cận trên không bao gồm. Kiểm lại overlap trước commit.
6. Kiểm bằng tài khoản learner/tutor: đúng nội dung, phiên bản, ngày hiệu lực; draft/future/withdrawn không truy cập được. Không có nguồn thì giữ trạng thái rỗng. Nếu phát hiện dữ liệu chồng lấn, chức năng đọc trả lỗi thay vì chọn ngẫu nhiên.

Không có sẵn câu INSERT chứa điều khoản mẫu. SQL migration không seed policy. Fixture trong src/test chỉ dùng DB operations_test riêng.
