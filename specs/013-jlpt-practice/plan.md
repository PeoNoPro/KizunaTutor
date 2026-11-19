# FE-13 — Luyện đề JLPT (Ngân hàng đề + Luyện đề)

**Created**: 2026-11-17 · **Status**: Đã duyệt trong hội thoại, đang triển khai

## Mục tiêu
- Cán bộ học vụ đưa đề JLPT các năm trước (N5–N1) vào **Ngân hàng đề**; học viên **luyện đề** theo một luồng duy nhất.
- Đề gốc là PDF dạng ảnh. Đề mẫu được **chép sang Excel theo mẫu** (dạng chữ, giữ furigana); học vụ tải Excel lên, rà lại, rồi đăng.

## Quyết định đã chốt
1. **Câu hỏi dạng chữ** như đề thật: mondai + câu hướng dẫn, câu hỏi có số, 2–4 lựa chọn bấm được. Furigana ghi `{漢字|かんじ}`, gạch chân ghi `__từ__`. Học viên bật/tắt furigana. Hình minh họa (bảng, tranh phần nghe) giữ dạng ảnh.
2. **Một chức năng "Luyện đề"** cho học viên với một hộp chọn: cấp độ · đề · hình thức (Làm bài mới / Làm lại câu sai) · phần thi (Tất cả / Từ vựng / Ngữ pháp – Đọc hiểu / Nghe).
   - **Bỏ chế độ trộn ngẫu nhiên** (chốt 2026-11-17): học viên luôn làm theo một đề; bài trộn cũ trong lịch sử vẫn xem lại được.
   - Theo đề + tất cả phần thi = thi thử đầy đủ: một đồng hồ tổng = tổng số phút các phần (mặc định theo JLPT, học vụ sửa được); chọn từ 2 phần trở lên thì **như thi thật**: mỗi phần một đồng hồ riêng, nộp phần hoặc hết giờ thì khóa phần đó (N1/N2 gộp hai phần đầu). Một phần, hoặc làm lại câu sai: chuyển phần tự do, một đồng hồ tổng (chốt 2026-11-17).
   - Màn làm bài cuộn liên tục cả phần (mondai → câu), cột phải có đồng hồ, Nộp bài, tiến độ, danh sách phần (Đang làm/Đã làm/Chưa làm), bảng số câu; phần Nghe có thanh âm thanh cố định ở đáy.
   - Kết quả là **số câu đúng từng phần**, không phải điểm quy đổi JLPT chính thức.
3. **File nghe** lưu trong thư mục trên máy chủ (`app.practice.storage-dir`), DB chỉ lưu khóa file. Phần lưu viết qua giao diện chung để sau này đổi sang Cloudflare R2 / dịch vụ chuẩn S3 bằng cấu hình (làm khi nhóm có Cloudflare).
   - **Phần Nghe chỉ làm theo đề** (đổi 2026-11-17): phát cả file liền mạch (~30 phút với N5) như thi thật, được tua; lời thoại chỉ hiện sau khi nộp. bỏ công cụ gắn mốc thời gian (cột audio_start_ms/audio_end_ms giữ trong DB, không dùng).
   - File nghe tải lên theo luồng (không qua multipart) nên không đổi giới hạn tải tệp chung của dự án.
4. **Nguồn đề**: ô không bắt buộc, chỉ học vụ thấy. Nhóm ghi nguồn dữ liệu trong báo cáo đồ án.
5. Đề đã có học viên làm thì không xóa, chỉ ẩn. Chỉ đề Nháp chưa từng đăng mới xóa được.

## Thời gian mặc định (phút)
| Cấp | Từ vựng | Ngữ pháp – Đọc hiểu | Nghe |
|---|---|---|---|
| N5 | 20 | 40 | 30 |
| N4 | 25 | 55 | 35 |
| N3 | 30 | 70 | 40 |
| N2 | 105 (gộp, ghi vào Từ vựng; Ngữ pháp – Đọc hiểu = 0) | | 50 |
| N1 | 110 (gộp) | | 55 |

## Dữ liệu
`database/seeds/fe13_jlpt_practice.sql`: `practice_exams`, `practice_files`, `practice_mondai`, `practice_groups`, `practice_questions`, `practice_attempts`, `practice_attempt_answers`.

## Mẫu Excel
- Sheet `De`: Tên đề, Cấp độ, Tháng thi (YYYY-MM), Nguồn, Phút Từ vựng, Phút Ngữ pháp – Đọc hiểu, Phút Nghe.
- Sheet `CauHoi` (mỗi dòng một câu): Phần thi · Mondai · Hướng dẫn mondai · Mã nhóm · Đoạn văn nhóm · Ảnh nhóm · Câu số · Câu hỏi · Lựa chọn 1–4 · Đáp án · Giải thích · Lời thoại · Ảnh câu.

## Thứ tự
1. SQL + backend (ngân hàng đề, nhập Excel, lưu file, luyện đề, chấm điểm, lịch sử).
2. Trang học vụ "Ngân hàng đề" (danh sách, nhập, xem trước, sửa câu, tải file nghe, đăng/ẩn).
3. Trang học viên "Luyện đề" (hộp chọn, làm bài, kết quả, lịch sử).
4. Chép đề N5 T7-2024, N4 T7-2024 sang Excel; sau đó N3, N2, N1 khi có file.
