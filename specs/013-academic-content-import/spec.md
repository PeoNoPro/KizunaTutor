# Feature Specification: FE-13 — Kho học liệu cho cán bộ học vụ (Kanji, từ vựng, ngữ pháp)

**Feature Branch**: `han` (thư mục đặc tả `specs/013-academic-content-import`)

**Created**: 2026-11-10

**Status**: Draft — đã làm rõ (phiên 2026-11-10)

**Input**: Yêu cầu người phụ trách (Hân), 2026-11-10: "Cán bộ học vụ có thể thêm tài liệu học có sẵn trong hệ thống từ các file Excel `kanji_vietnamese_full.xlsx`, `Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx`, `Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx` trong `/database`; import được các file có định dạng tương tự; xử lý trùng lặp." Bổ sung cùng ngày: "phải giữ nguyên các mục đã có, chỉ thêm mục mới"; "xem được danh sách Kanji, từ vựng, ngữ pháp"; "xem được cả lịch sử thêm".

**Nguồn nghiệp vụ**: SRS `Report-3_Software Requirement Specification` **không có use case** cho chức năng này. SRS chỉ mô tả vai trò cán bộ học vụ là "thẩm định bằng cấp gia sư và chuẩn hóa nội dung" (bảng vai trò) và các UC xác minh gia sư (UC-25…UC-28, UC-84). Đây là phần **bổ sung ngoài SRS theo yêu cầu người phụ trách**; nhóm cần thêm 3 UC tương ứng vào SRS (bảng I.4.2, sơ đồ I.4.3.5, đặc tả phần II) — xem mục "Phạm vi".

## Phạm vi

**Trong phạm vi** (đề xuất bổ sung vào SRS, số UC do nhóm cấp khi ghép tài liệu):

| UC đề xuất | Tên | Tác nhân |
| --- | --- | --- |
| UC-A | Xem kho học liệu (View Learning Content) | Cán bộ học vụ |
| UC-B | Nhập học liệu từ Excel (Import Learning Content) | Cán bộ học vụ |
| UC-C | Xem lịch sử nhập học liệu (View Content Import History) | Cán bộ học vụ |

Kho học liệu gồm ba loại đã có sẵn trong hệ thống và đang được học viên dùng ở phần hỗ trợ học tập (tra cứu Kanji, từ vựng, ngữ pháp, minigame): **Kanji**, **Từ vựng**, **Ngữ pháp**.

**Ngoài phạm vi**: sửa hoặc xóa từng mục học liệu; ghi đè mục đã có bằng dữ liệu trong tệp; hoàn tác một lần nhập; nhập từ định dạng khác `.xlsx` (CSV, Google Sheets); nhập dữ liệu nét viết Kanji (chỉ đến từ bộ KanjiVG); sửa mã của phân hệ khác; tài liệu học tập của lớp học (UC "Upload Learning Materials" — thuộc FE-06); thay đổi giao diện học viên.

## Clarifications

### Session 2026-11-10 (đã quyết định trong lúc trao đổi, trước khi lập đặc tả này)

- Q: Mục trong tệp đã có trong kho thì xử lý thế nào? → A: **Luôn giữ nguyên mục đã có, chỉ thêm mục mới.** Không có lựa chọn cập nhật / ghi đè.
- Q: Cán bộ học vụ cần xem gì ngoài việc nhập? → A: Xem danh sách từng loại học liệu và xem lịch sử các lần nhập.

- Q: Có cho phép thêm **Kanji mới** từ Excel không (constitution II.5 quy định dữ liệu Kanji chỉ từ KanjiVG)? → A (người phụ trách, 2026-11-10): **Có — giữ đúng chức năng đang chạy.** Kanji mới bắt buộc có "Số nét"; chữ mới không có dữ liệu nét viết KanjiVG nên chưa dùng được ở phần luyện viết. Đây là điểm lệch constitution, ghi tại `plan.md` (Complexity Tracking) để nhóm trưởng xác nhận.
- Q: Chức năng thuộc phân hệ nào? → A (người phụ trách, 2026-11-10): **Phân hệ riêng của cán bộ học vụ, do Hân phụ trách**, không sửa mã của phân hệ "Hỗ trợ học tập" (FE-08) hay của thành viên khác và không làm mất chức năng của họ. Chỉ đọc / thêm bản ghi vào các bảng học liệu đã có.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cán bộ học vụ xem kho học liệu (Priority: P1)

Cán bộ học vụ mở trang "Kho học liệu", chọn một loại (Kanji, Từ vựng, Ngữ pháp), xem danh sách có phân trang, tìm theo từ khóa, lọc theo cấp độ JLPT, sắp xếp theo cấp độ hoặc theo ngày thêm, và mở một mục để xem đầy đủ thông tin.

**Why this priority**: Là nền để cán bộ học vụ biết kho đang có gì trước khi nhập thêm, và để kiểm tra lại kết quả sau khi nhập. Dùng được độc lập kể cả khi chưa có chức năng nhập.

**Independent Test**: Đăng nhập bằng tài khoản cán bộ học vụ, mở trang, đổi qua lại ba loại, tìm "gặp" ở Từ vựng, lọc N3, mở chi tiết một mẫu ngữ pháp; số mục hiển thị khớp số mục đang có trong hệ thống.

**Acceptance Scenarios**:

1. **Given** kho có dữ liệu, **When** cán bộ học vụ chọn loại "Từ vựng", **Then** hệ thống hiển thị trang đầu danh sách từ vựng (từ, cách đọc, Hán Việt, từ loại, nghĩa, cấp độ, ngày thêm) và tổng số mục.
2. **Given** đang xem danh sách, **When** nhập từ khóa hoặc chọn cấp độ JLPT, **Then** danh sách chỉ còn các mục phù hợp và quay về trang đầu.
3. **Given** không có mục nào phù hợp, **When** tìm kiếm, **Then** hệ thống hiển thị trạng thái rỗng "Không có mục nào phù hợp", không hiển thị dữ liệu giả.
4. **Given** đang xem danh sách, **When** chọn một mục, **Then** hệ thống hiển thị đầy đủ thông tin của mục đó (ví dụ câu ví dụ, furigana, ghi chú với ngữ pháp).
5. **Given** người dùng không phải cán bộ học vụ, **When** truy cập chức năng, **Then** hệ thống từ chối truy cập.

---

### User Story 2 - Cán bộ học vụ nhập học liệu mới từ tệp Excel (Priority: P1)

Cán bộ học vụ chọn loại học liệu, chọn tệp Excel có định dạng giống tệp mẫu, bấm "Kiểm tra tệp" để xem trước kết quả (bao nhiêu mục sẽ được thêm, bao nhiêu mục đã có, bao nhiêu dòng trùng trong tệp, dòng nào lỗi và vì sao), rồi bấm "Nhập vào hệ thống" để thêm các mục mới. Mục đã có luôn được giữ nguyên.

**Why this priority**: Là yêu cầu gốc của người phụ trách; giúp mở rộng kho học liệu mà không cần chạy câu lệnh trên cơ sở dữ liệu.

**Independent Test**: Dùng bộ tệp thử (trùng hoàn toàn / không trùng / hỗn hợp / sai tiêu đề), so kết quả kiểm tra với kết quả mong đợi; nhập tệp hỗn hợp rồi kiểm tra lại cùng tệp thấy "Thêm mới" về 0.

**Acceptance Scenarios**:

1. **Given** tệp có toàn mục đã có trong kho, **When** kiểm tra tệp, **Then** hệ thống báo 0 mục mới, mọi mục "đã có, giữ nguyên", và không cho bấm "Nhập vào hệ thống".
2. **Given** tệp có mục mới, **When** kiểm tra rồi nhập, **Then** đúng các mục mới được thêm; số mục của loại đó tăng đúng bằng số mục mới; mục đã có không thay đổi nội dung kể cả khi tệp ghi nội dung khác.
3. **Given** một mục xuất hiện nhiều lần trong tệp, **When** kiểm tra, **Then** hệ thống chỉ lấy lần xuất hiện đầu tiên; lần lặp giống hệt chỉ được đếm là trùng; lần lặp khác nội dung được cảnh báo kèm số dòng.
4. **Given** tệp có dòng thiếu thông tin bắt buộc, sai cấp độ JLPT hoặc quá dài, **When** kiểm tra, **Then** hệ thống liệt kê từng dòng lỗi (sheet, số dòng, lý do bằng tiếng Việt); khi nhập, các dòng lỗi bị bỏ qua, các dòng hợp lệ vẫn được thêm.
5. **Given** tệp không có dòng tiêu đề phù hợp với loại đã chọn (ví dụ chọn "Từ vựng" nhưng tải tệp Kanji), **When** kiểm tra, **Then** hệ thống báo không tìm thấy dòng tiêu đề phù hợp, nêu các cột bắt buộc và nhắc kiểm tra loại học liệu.
6. **Given** tệp có nhiều sheet (sheet "Tất cả" và các sheet theo cấp độ lặp lại nhau), **When** kiểm tra, **Then** hệ thống đọc mọi sheet có tiêu đề hợp lệ và gộp trùng lặp như kịch bản 3.
7. **Given** hai cán bộ học vụ cùng nhập một loại cùng lúc, **When** cả hai bấm nhập, **Then** không có mục nào bị thêm hai lần.
8. **Given** lỗi xảy ra giữa chừng khi nhập, **When** hệ thống dừng, **Then** không có mục nào của lần nhập đó được thêm và cán bộ nhận thông báo lỗi thân thiện.

---

### User Story 3 - Cán bộ học vụ xem lịch sử nhập (Priority: P2)

Cán bộ học vụ mở phần "Lịch sử nhập" của một loại học liệu, xem các lần nhập (thời gian, người nhập, tên tệp, số mục thêm / đã có / lỗi), và mở một lần nhập để xem danh sách các mục đã thêm.

**Why this priority**: Giúp truy vết ai đã thêm gì, khi nào; cần có chức năng nhập (US2) mới có dữ liệu.

**Independent Test**: Nhập một tệp hỗn hợp, mở lịch sử thấy đúng một dòng mới với số liệu khớp báo cáo nhập; mở chi tiết thấy đúng các mục đã thêm.

**Acceptance Scenarios**:

1. **Given** đã có lần nhập, **When** mở lịch sử của loại đó, **Then** các lần nhập hiển thị mới nhất trước, có phân trang.
2. **Given** cán bộ chỉ bấm "Kiểm tra tệp" mà không nhập, **When** mở lịch sử, **Then** không có dòng lịch sử nào cho lần kiểm tra đó.
3. **Given** một lần nhập, **When** mở chi tiết, **Then** hệ thống hiển thị số liệu của lần nhập và danh sách mục đã thêm.
4. **Given** chưa có lần nhập nào, **When** mở lịch sử, **Then** hệ thống hiển thị trạng thái rỗng.

---

### Edge Cases

- Tệp không phải `.xlsx`, tệp rỗng, tệp hỏng hoặc vượt giới hạn dung lượng → báo lỗi thân thiện, không ghi gì.
- Tệp có cột theo thứ tự khác, tên cột viết hoa/thường, có/không dấu, có phần chú thích trong ngoặc (ví dụ "Từ vựng (Kanji/Kana)") → vẫn nhận đúng cột.
- Dòng trống giữa dữ liệu → bỏ qua, không tính là lỗi.
- Từ vựng viết bằng kana không có cách đọc riêng → dùng chính từ đó làm cách đọc.
- Cùng một từ nhưng khác cách đọc → là hai mục khác nhau.
- Mã ngữ pháp khác nhau về chữ hoa/thường → là cùng một mục.
- Mẫu ngữ pháp đã thuộc về một mã khác (trong kho hoặc trong cùng tệp) → lỗi, không tạo mẫu trùng.
- Kanji: ô có nhiều hơn một chữ hoặc không phải chữ Hán → lỗi. Kanji mới thiếu "Số nét" → lỗi.
- Từ khóa tìm kiếm chứa ký tự đặc biệt (`%`, `_`) → tìm đúng ký tự đó, không lỗi.
- Tệp có quá nhiều dòng lỗi → báo cáo chỉ liệt kê một số dòng đầu và nói rõ còn nữa.
- Mất kết nối khi đang tải danh sách / lịch sử → báo lỗi và cho thử lại.

## Requirements *(mandatory)*

### Functional Requirements

**Xem kho (US1)**

- **FR-001**: Hệ thống PHẢI cho cán bộ học vụ xem danh sách từng loại học liệu (Kanji, Từ vựng, Ngữ pháp) có phân trang (10 / 20 / 50 mục mỗi trang) và tổng số mục.
- **FR-002**: Hệ thống PHẢI cho tìm kiếm theo từ khóa (tối đa 100 ký tự) trên các thông tin chính của từng loại: Kanji (chữ, Hán Việt, âm đọc, nghĩa); Từ vựng (từ, cách đọc, romaji, Hán Việt, nghĩa); Ngữ pháp (mã, mẫu, romaji, ý nghĩa, thẻ phân loại).
- **FR-003**: Hệ thống PHẢI cho lọc theo cấp độ JLPT N5–N1 và sắp xếp theo cấp độ (N5 → N1) hoặc theo ngày thêm (mới nhất trước).
- **FR-004**: Hệ thống PHẢI cho xem đầy đủ thông tin một mục, gồm ngày thêm.
- **FR-005**: Chỉ cán bộ học vụ được dùng các chức năng của feature này.

**Nhập từ Excel (US2)**

- **FR-006**: Hệ thống PHẢI nhận tệp `.xlsx` tối đa 15 MB và tối đa 50.000 dòng dữ liệu cho một lần nhập.
- **FR-007**: Hệ thống PHẢI nhận cột theo **tên tiêu đề** (không phân biệt dấu, chữ hoa/thường, bỏ qua phần trong ngoặc), tìm dòng tiêu đề trong 5 dòng đầu, đọc mọi sheet có đủ cột bắt buộc. Cột bắt buộc: Kanji — "Kanji", "Nghĩa tiếng Việt"; Từ vựng — "Từ vựng", "Nghĩa tiếng Việt"; Ngữ pháp — "Mã định danh", "Mẫu cấu trúc ngữ pháp", "Ý nghĩa tiếng Việt". Các cột còn lại theo tệp mẫu là không bắt buộc.
- **FR-008**: Hệ thống PHẢI hiển thị trước cho cán bộ học vụ cột bắt buộc và cột không bắt buộc của loại đang chọn.
- **FR-009**: Hệ thống PHẢI có bước "Kiểm tra tệp" không ghi dữ liệu, trả về: số dòng đọc được, số mục sẽ thêm, số mục đã có, số dòng trùng trong tệp, số dòng lỗi và danh sách lỗi / cảnh báo (sheet, số dòng, lý do).
- **FR-010**: Hệ thống PHẢI chỉ cho "Nhập vào hệ thống" sau một lần kiểm tra có ít nhất một mục mới, với đúng tệp và loại đã kiểm tra; đổi tệp hoặc loại thì phải kiểm tra lại.
- **FR-011**: Hệ thống PHẢI nhận diện trùng theo khóa: Kanji — chữ Kanji; Từ vựng — (từ, cách đọc); Ngữ pháp — mã định danh (không phân biệt hoa thường).
- **FR-012**: Mục đã có trong kho PHẢI luôn được giữ nguyên; hệ thống chỉ thêm mục chưa có.
- **FR-013**: Trùng trong tệp: hệ thống PHẢI giữ lần xuất hiện đầu tiên; lặp giống hệt chỉ đếm; lặp khác nội dung phải có cảnh báo nêu dòng được giữ.
- **FR-014**: Hệ thống PHẢI bỏ qua và báo lỗi các dòng: thiếu cột bắt buộc; cấp độ JLPT ngoài N1–N5; vượt độ dài cho phép; Kanji không phải đúng một chữ Hán; mẫu ngữ pháp đã thuộc mã khác. Các dòng hợp lệ vẫn được nhập.
- **FR-015**: Một lần nhập PHẢI là trọn vẹn: lỗi giữa chừng thì không mục nào của lần đó được thêm. Hai lần nhập cùng loại không được chạy chồng làm thêm trùng.
- **FR-016**: Từ vựng mới PHẢI được liên kết với các chữ Kanji có trong kho (như dữ liệu từ vựng hiện có).
- **FR-017**: Kanji chưa có trong kho chỉ được thêm khi dòng có "Số nét" (1–84); thiếu thì là dòng lỗi. Kanji đã có không cần "Số nét".

**Lịch sử nhập (US3)**

- **FR-018**: Mỗi lần nhập thật (không tính "Kiểm tra tệp") PHẢI được ghi lại: thời gian, người nhập, loại, tên tệp, các số liệu của báo cáo và danh sách mục đã thêm (lưu tối đa 2.000 mục mỗi lần).
- **FR-019**: Ghi lịch sử PHẢI nằm trong cùng lần nhập: nhập thất bại thì không có dòng lịch sử.
- **FR-020**: Hệ thống PHẢI cho xem lịch sử theo loại học liệu, mới nhất trước, có phân trang, và xem chi tiết một lần nhập.

**Chung**

- **FR-021**: Mọi nhãn, thông báo, lỗi PHẢI bằng tiếng Việt tự nhiên, không viết hoa toàn bộ, không lộ tên bảng, tên trường, mã lỗi kỹ thuật.
- **FR-022**: Khi đang tải PHẢI có khung chờ tải (skeleton); khi không có dữ liệu PHẢI có trạng thái rỗng; khi lỗi tải PHẢI có thông báo và nút thử lại.

### Key Entities *(include if feature involves data)*

- **Mục Kanji**: chữ Kanji (duy nhất), âm Hán Việt, onyomi, kunyomi, nghĩa tiếng Việt, cấp độ JLPT, số nét, ngày thêm. Đã tồn tại trong hệ thống.
- **Mục từ vựng**: từ, cách đọc (cặp từ + cách đọc là duy nhất), romaji, âm Hán Việt, từ loại, nghĩa, câu ví dụ, furigana, dịch câu ví dụ, mã JMdict, cấp độ JLPT, ngày thêm; liên kết với các chữ Kanji có trong từ. Đã tồn tại.
- **Mẫu ngữ pháp**: mã định danh (duy nhất), mẫu câu (duy nhất), romaji, ý nghĩa, công thức, ghi chú, câu ví dụ, furigana, dịch, ví dụ mở rộng, thẻ phân loại, cấp độ JLPT, ngày thêm. Đã tồn tại.
- **Lần nhập học liệu**: thời điểm, cán bộ học vụ thực hiện, loại học liệu, tên tệp, các sheet đã đọc, số dòng đọc được, số mục thêm mới / đã có / trùng trong tệp / lỗi, danh sách mục đã thêm. Mới (cách lưu quyết định ở bước plan).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Nhập lại nguyên ba tệp mẫu trong `/database` cho kết quả 0 mục mới, 0 dòng lỗi và không làm thay đổi bất kỳ mục nào đang có.
- **SC-002**: Với bộ tệp thử chuẩn (trùng hoàn toàn / không trùng / hỗn hợp / sai tiêu đề), số mục thêm mới, đã có, trùng trong tệp và dòng lỗi khớp 100% kết quả mong đợi.
- **SC-003**: Bước kiểm tra tệp từ vựng đầy đủ (khoảng 15.600 dòng, 6 sheet) hoàn tất trong dưới 30 giây.
- **SC-004**: Mỗi lần nhập thật tạo đúng một dòng lịch sử có số liệu khớp báo cáo nhập; lần chỉ kiểm tra không tạo dòng nào.
- **SC-005**: Cán bộ học vụ tìm được một mục bất kỳ trong kho bằng từ khóa hoặc bộ lọc trong tối đa 3 thao tác.
- **SC-006**: 100% thông báo lỗi và trạng thái hiển thị bằng tiếng Việt, không có chữ in hoa toàn bộ, không lộ tên kỹ thuật.

## Assumptions

- A-1: Kho Kanji, từ vựng, ngữ pháp và dữ liệu hiện có (6.701 Kanji, 7.811 từ, 526 mẫu ngữ pháp tại 2026-11-10) đã có sẵn; feature không tạo loại học liệu mới và không đổi cách học viên dùng kho.
- A-2: "Định dạng tương tự" nghĩa là cùng tên cột như ba tệp mẫu; thứ tự cột và số sheet có thể khác.
- A-3: Mọi cán bộ học vụ đều xem được lịch sử nhập của nhau (kho dùng chung).
- A-4: Không có chức năng sửa, xóa từng mục hay hoàn tác lần nhập trong phạm vi này; muốn đổi nội dung mục đã có cần một yêu cầu riêng.
- A-5: Chỉ có cơ sở dữ liệu Supabase dùng chung, không có DB kiểm thử; nghiệm thu nhập thật trên DB chung phải dùng dữ liệu thử có nhãn nhận biết và có câu lệnh dọn dẹp.
- A-6: SRS cần được nhóm bổ sung 3 UC (bảng I.4.2, sơ đồ I.4.3.5, đặc tả phần II) để khớp hệ thống.
