"""Xuất bảng unit test FE-10 theo mẫu PDF (Workbook1 - Sheet1). Mỗi hàm một sheet + sheet Tổng hợp."""
import sys
from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

OUT = sys.argv[1]
AUTHOR = "HuyenDTT"
DATE = "19/11/2026"

# ---- Câu dùng chung ----
INVALID = "'Vui lòng nhập nội dung hợp lệ và đúng độ dài cho phép.'"
CHOICE = "'Lựa chọn chưa hợp lệ. Vui lòng kiểm tra lại.'"
NOPERM = "'Bạn không có quyền thực hiện thao tác này.'"
STATE = "'Trạng thái yêu cầu đã thay đổi. Vui lòng tải lại trước khi thao tác.'"
STALE = "'Yêu cầu đã thay đổi hoặc chưa đủ điều kiện xử lý. Vui lòng tải lại trước khi gửi.'"
NOTFOUND = "'Không tìm thấy yêu cầu hỗ trợ hoặc bạn không có quyền truy cập.'"
RELOAD = "'Vui lòng tải lại yêu cầu trước khi thực hiện thao tác.'"


def r(*nums):
    return set(nums)


def rng(a, b):
    return set(range(a, b + 1))


ALL = None  # dùng khi mọi cột đều đánh dấu

# Mỗi sheet: name, method, file, requirement, n (số cột), types, pre/input: [(nhóm, [(giá trị, cột)])], ret, exc
SHEETS = [
    dict(name="createTicket", method="create", file="TicketCreateTest.java",
         req="Học viên / gia sư gửi yêu cầu hỗ trợ mới (tiêu đề, nhóm vấn đề, nội dung, thông tin liên quan, tệp đính kèm)",
         n=12, types="NNNBBBBAAAAA",
         pre=[("Người gửi (đã đăng nhập)", [("Học viên", r(1, 3, 4, 5, 6, 7, 8, 9, 11)), ("Gia sư", r(2)),
                                             ("Nhân viên hỗ trợ (CS)", r(10)), ("Chưa đăng nhập", r(12))]),
              ("Buổi học thử liên quan", [("Không gắn", r(1, 2, 4, 5, 6, 7, 8, 9, 10, 12)),
                                          ("Thuộc người gửi, bắt đầu trong 24 giờ tới", r(3)),
                                          ("Không thuộc người gửi", r(11))])],
         inp=[("subject (tiêu đề)", [("Hợp lệ (1–200 ký tự)", r(1, 2, 3, 6, 7, 9, 10, 11, 12)), ("Đúng 200 ký tự (biên)", r(4)),
                                     ("201 ký tự (vượt biên)", r(5)), ("Chỉ có khoảng trắng", r(8))]),
              ("category (nhóm vấn đề)", [("ACCOUNT", r(1, 5, 6, 7, 8, 10, 12)), ("BILLING", r(2)), ("BOOKING", r(3, 11)),
                                          ("OTHER", r(4)), ("Không có trong danh mục", r(9))]),
              ("message (nội dung)", [("Hợp lệ (1–10.000 ký tự)", r(1, 2, 3, 4, 5, 8, 9, 10, 11, 12)), ("Đúng 10.000 ký tự (biên)", r(6)),
                                      ("10.001 ký tự (vượt biên)", r(7))]),
              ("files (tệp đính kèm)", [("Không có", rng(1, 12) - r(3)), ("1 ảnh hợp lệ", r(3))])],
         ret=[("Tạo yêu cầu ưu tiên Bình thường, lưu nội dung, ghi lịch sử CREATE, báo nhân viên hỗ trợ", r(1, 6)),
              ("Tạo yêu cầu ưu tiên Cao (HIGH)", r(2)),
              ("Tạo yêu cầu ưu tiên Khẩn cấp (URGENT), lưu tệp đính kèm", r(3)),
              ("Tạo yêu cầu ưu tiên Thấp (LOW)", r(4))],
         exc=[(INVALID, r(5, 7, 8)), (CHOICE, r(9)), ("'Chỉ học viên và gia sư được gửi yêu cầu hỗ trợ.'", r(10)),
              ("'Thông tin liên quan không tồn tại hoặc không thuộc tài khoản của bạn.'", r(11)),
              ("'Vui lòng đăng nhập để tiếp tục.'", r(12))]),

    dict(name="sendMessage", method="message", file="TicketMessageTest.java",
         req="Chủ yêu cầu hoặc nhân viên hỗ trợ gửi trao đổi công khai (kèm tệp) trên yêu cầu chưa đóng",
         n=13, types="NNNNBBAAAAAAA",
         pre=[("Người thao tác", [("Chủ yêu cầu", r(1, 2, 5, 6, 7, 8, 9, 10, 13)), ("Học viên khác", r(11)),
                                  ("Nhân viên hỗ trợ (CS)", r(3, 4)), ("Quản lý được chuyển cấp", r(12))]),
              ("Trạng thái yêu cầu", [("Đang xử lý", r(1, 4, 5, 6, 7, 8, 9, 11)), ("Đã giải quyết", r(2)), ("Mới tiếp nhận", r(3)),
                                      ("Đã chuyển quản lý", r(12)), ("Đã đóng", r(10)), ("Không tồn tại", r(13))])],
         inp=[("expectedVersion", [("Khớp phiên bản hiện tại", rng(1, 13) - r(8, 9)), ("Cũ hơn (đã bị cập nhật)", r(8)),
                                   ("Không gửi (null)", r(9))]),
              ("message (nội dung)", [("Hợp lệ (1–10.000 ký tự)", r(1, 2, 3, 4, 8, 9, 10, 11, 12, 13)), ("Đúng 10.000 ký tự (biên)", r(5)),
                                      ("10.001 ký tự (vượt biên)", r(6)), ("Chỉ khoảng trắng / xuống dòng", r(7))]),
              ("files (tệp đính kèm)", [("Không có", rng(1, 13) - r(4)), ("2 tệp hợp lệ", r(4))])],
         ret=[("Lưu tin nhắn, giữ trạng thái, ghi lịch sử MESSAGE_SENT, báo người liên quan", r(1, 5)),
              ("Đã giải quyết → Đang xử lý (mở lại yêu cầu)", r(2)),
              ("Tự nhận xử lý: người phụ trách = nhân viên, Đang xử lý", r(3)),
              ("Lưu 2 tệp vào cùng tin nhắn", r(4))],
         exc=[(INVALID, r(6, 7)), (STALE, r(8)), (RELOAD, r(9)), (STATE, r(10)), (NOTFOUND, r(11, 13)), (NOPERM, r(12))]),

    dict(name="claimTicket", method="command(\"claim\")", file="TicketClaimTest.java",
         req="Nhân viên hỗ trợ nhận xử lý yêu cầu còn Mới tiếp nhận",
         n=8, types="NAAAAAAA",
         pre=[("Người thao tác", [("Nhân viên hỗ trợ (CS)", r(1, 2, 3, 6, 7, 8)), ("Chủ yêu cầu", r(4)),
                                  ("Quản lý không được chuyển cấp", r(5))]),
              ("Trạng thái yêu cầu", [("Mới tiếp nhận", r(1, 4, 5, 6, 7)), ("Đang xử lý", r(2)), ("Đã đóng", r(3)),
                                      ("Không tồn tại", r(8))])],
         inp=[("expectedVersion", [("Khớp phiên bản hiện tại", r(1, 2, 3, 4, 5, 8)), ("Cũ hơn (người khác vừa nhận)", r(6)),
                                   ("Số âm", r(7))])],
         ret=[("Người phụ trách = nhân viên, Đang xử lý, ghi lịch sử CLAIM, báo người gửi", r(1))],
         exc=[(STATE, r(2, 3)), (NOPERM, r(4)), (NOTFOUND, r(5, 8)), (STALE, r(6)), (RELOAD, r(7))]),

    dict(name="changePriority", method="command(\"priority\")", file="TicketPriorityTest.java",
         req="Nhân viên hỗ trợ đổi mức ưu tiên (Thấp, Bình thường, Cao, Khẩn cấp) của yêu cầu chưa đóng",
         n=8, types="NNNAAAAA",
         pre=[("Người thao tác", [("Nhân viên hỗ trợ (CS)", rng(1, 8) - r(6)), ("Chủ yêu cầu", r(6))]),
              ("Trạng thái yêu cầu", [("Đang xử lý", r(1, 4, 5, 6, 8)), ("Mới tiếp nhận, chưa có người phụ trách", r(2)),
                                      ("Đã chuyển quản lý", r(3)), ("Đã đóng", r(7))])],
         inp=[("expectedVersion", [("Khớp phiên bản hiện tại", rng(1, 7)), ("Cũ hơn", r(8))]),
              ("priority", [("URGENT", r(1, 6)), ("LOW", r(2)), ("HIGH", r(3, 7, 8)), ("Không có trong danh mục ('CRITICAL')", r(4)),
                            ("Không chọn (null)", r(5))])],
         ret=[("Lưu mức ưu tiên mới, ghi lịch sử PRIORITY_CHANGE, báo người liên quan", r(1, 2, 3)),
              ("Ghi người phụ trách = nhân viên đang thao tác", r(2))],
         exc=[(CHOICE, r(4, 5)), (NOPERM, r(6)), (STATE, r(7)), (STALE, r(8))]),

    dict(name="resolveTicket", method="command(\"resolve\")", file="TicketResolveTest.java",
         req="Nhân viên hỗ trợ giải quyết yêu cầu Đang xử lý kèm hướng giải quyết gửi cho người gửi",
         n=8, types="NBBAAAAA",
         pre=[("Người thao tác", [("Nhân viên hỗ trợ (CS)", rng(1, 8) - r(7)), ("Chủ yêu cầu", r(7))]),
              ("Trạng thái yêu cầu", [("Đang xử lý", r(1, 2, 3, 4, 7, 8)), ("Mới tiếp nhận", r(5)), ("Đã chuyển quản lý", r(6))])],
         inp=[("expectedVersion", [("Khớp phiên bản hiện tại", rng(1, 7)), ("Cũ hơn", r(8))]),
              ("resolutionText", [("Hợp lệ (1–10.000 ký tự)", r(1, 5, 6, 7, 8)), ("Đúng 10.000 ký tự (biên)", r(2)),
                                  ("10.001 ký tự (vượt biên)", r(3)), ("Chỉ có khoảng trắng", r(4))])],
         ret=[("Gửi hướng giải quyết cho người gửi, Đã giải quyết, ghi lịch sử STATUS_CHANGE, báo người gửi", r(1, 2))],
         exc=[(INVALID, r(3, 4)), (STATE, r(5, 6)), (NOPERM, r(7)), (STALE, r(8))]),

    dict(name="closeTicket", method="command(\"close\")", file="TicketCloseTest.java",
         req="Chủ yêu cầu đóng yêu cầu đã được giải quyết",
         n=8, types="NNAAAAAA",
         pre=[("Người thao tác", [("Học viên (chủ yêu cầu)", r(1, 3, 4, 7, 8)), ("Gia sư (chủ yêu cầu)", r(2)),
                                  ("Nhân viên hỗ trợ (CS)", r(5)), ("Học viên khác", r(6))]),
              ("Trạng thái yêu cầu", [("Đã giải quyết", r(1, 2, 5, 6, 7, 8)), ("Đang xử lý", r(3)), ("Đã đóng", r(4))])],
         inp=[("expectedVersion", [("Khớp phiên bản hiện tại", rng(1, 8) - r(7)), ("Cũ hơn", r(7))]),
              ("action", [("close", rng(1, 7)), ("Không tồn tại ('archive')", r(8))])],
         ret=[("Đã đóng, ghi lịch sử CLOSE, báo nhân viên", r(1, 2))],
         exc=[(STATE, r(3, 4)), (NOPERM, r(5, 8)), (NOTFOUND, r(6)), (STALE, r(7))]),

    dict(name="escalateTicket", method="command(\"escalate\")", file="TicketEscalateTest.java",
         req="Nhân viên hỗ trợ chuyển yêu cầu cho một quản lý cụ thể kèm lý do và tóm tắt xử lý",
         n=10, types="NBBAAAAAAA",
         pre=[("Người thao tác", [("Nhân viên hỗ trợ (CS)", rng(1, 10) - r(9)), ("Chủ yêu cầu", r(9))]),
              ("Trạng thái yêu cầu", [("Đang xử lý", r(1, 2, 3, 4, 5, 6, 9, 10)), ("Mới tiếp nhận", r(7)), ("Đã chuyển quản lý", r(8))]),
              ("Quản lý nhận", [("Còn hoạt động, đúng vai trò Quản lý", r(1, 2, 3, 4, 7, 8, 9, 10)), ("Không chọn", r(5)),
                                ("Đã bị khóa / không phải Quản lý", r(6))])],
         inp=[("expectedVersion", [("Khớp phiên bản hiện tại", rng(1, 9)), ("Cũ hơn", r(10))]),
              ("reason (lý do)", [("Hợp lệ (1–10.000 ký tự)", r(1, 4, 5, 6, 7, 8, 9, 10)), ("Đúng 10.000 ký tự (biên)", r(2)),
                                  ("10.001 ký tự (vượt biên)", r(3))]),
              ("handlingSummary (tóm tắt xử lý)", [("Hợp lệ", rng(1, 10) - r(4)), ("Chỉ có khoảng trắng", r(4))])],
         ret=[("Lưu lượt chuyển cấp, Đã chuyển quản lý, ghi lịch sử ESCALATE kèm lý do, báo quản lý", r(1, 2))],
         exc=[(INVALID, r(3, 4)), ("'Người nhận không còn hoạt động hoặc không phù hợp.'", r(5, 6)), (STATE, r(7, 8)),
              (NOPERM, r(9)), (STALE, r(10))]),

    dict(name="saveDecision", method="command(\"decision\")", file="TicketDecisionTest.java",
         req="Quản lý được chuyển cấp lưu hướng giải quyết / phê duyệt phương án cho yêu cầu",
         n=10, types="NBBAAAAAAA",
         pre=[("Người thao tác", [("Quản lý được chuyển cấp", r(1, 2, 3, 4, 6, 7, 9, 10)), ("Quản lý khác", r(5)),
                                  ("Nhân viên hỗ trợ (CS)", r(8))]),
              ("Trạng thái yêu cầu", [("Đã chuyển quản lý", rng(1, 10) - r(9)), ("Đang xử lý (đã trả về)", r(9))]),
              ("Lượt chuyển cấp", [("Đang chờ, do quản lý này phụ trách", r(1, 2, 3, 4, 5, 8, 10)),
                                   ("Đã đổi sang quản lý khác", r(6)), ("Không còn lượt đang chờ", r(7, 9))])],
         inp=[("expectedVersion", [("Khớp phiên bản hiện tại", rng(1, 9)), ("Cũ hơn", r(10))]),
              ("decisionText", [("Hợp lệ (1–10.000 ký tự)", r(1, 5, 6, 7, 8, 9, 10)), ("Đúng 10.000 ký tự (biên)", r(2)),
                                ("10.001 ký tự (vượt biên)", r(3)), ("Trống", r(4))])],
         ret=[("Lưu hướng giải quyết, giữ Đã chuyển quản lý, ghi lịch sử SAVE_DECISION, báo nhân viên", r(1, 2))],
         exc=[(INVALID, r(3, 4)), (NOTFOUND, r(5)), ("'Bạn không còn phụ trách yêu cầu này.'", r(6)), (STALE, r(7, 10)),
              (NOPERM, r(8)), (STATE, r(9))]),

    dict(name="addInternalNote", method="addNote", file="TicketAddNoteTest.java",
         req="Nhân viên hỗ trợ / quản lý thêm ghi chú nội bộ (người gửi không thấy) vào yêu cầu chưa đóng",
         n=9, types="NNBBAAAAA",
         pre=[("Người thao tác", [("Nhân viên hỗ trợ (CS)", r(1, 3, 4, 5, 7, 9)), ("Quản lý được chuyển cấp", r(2)),
                                  ("Chủ yêu cầu", r(6)), ("Quản lý không được chuyển cấp", r(8))]),
              ("Trạng thái yêu cầu", [("Đang xử lý", r(1, 3, 4, 5, 6)), ("Đã chuyển quản lý", r(2, 8)), ("Đã đóng", r(7)),
                                      ("Không tồn tại", r(9))])],
         inp=[("note (ghi chú)", [("Hợp lệ (1–5.000 ký tự)", r(1, 2, 6, 7, 8, 9)), ("Đúng 5.000 ký tự (biên)", r(3)),
                                  ("5.001 ký tự (vượt biên)", r(4)), ("Chỉ có khoảng trắng", r(5))])],
         ret=[("Lưu ghi chú (bỏ khoảng trắng đầu/cuối), trả danh sách ghi chú", r(1, 2, 3))],
         exc=[(INVALID, r(4, 5)), ("'Ghi chú nội bộ chỉ dành cho bộ phận xử lý.'", r(6)),
              ("'Yêu cầu đã đóng, không thêm ghi chú được nữa.'", r(7)), (NOTFOUND, r(8, 9))]),
]

# ---- Kiểu ----
NAVY = PatternFill("solid", fgColor="000080")
F = "Arial"
thin = Side(style="thin", color="000000")
medium = Side(style="medium", color="000000")
BOX = Border(left=thin, right=thin, top=thin, bottom=thin)
font = lambda **k: Font(name=F, size=k.pop("size", 10), **k)


def style_range(ws, ref, **kw):
    for row in ws[ref]:
        for c in row:
            for k, v in kw.items():
                setattr(c, k, v)


def build(ws, s):
    n = s["n"]
    first, last = 3, 2 + n                      # cột C.. cho UTCID
    L = get_column_letter(last)
    ws.column_dimensions["A"].width = 17
    ws.column_dimensions["B"].width = 74
    for c in range(first, max(last, 12) + 1):
        ws.column_dimensions[get_column_letter(c)].width = 6.5
    ws.sheet_view.showGridLines = False

    # ---- Phần đầu ----
    head = [("Code Module", "Support Ticket (FE-10)", "Method", s["method"]),
            ("Created By", AUTHOR, "Executed By", AUTHOR)]
    for i, (a, b, c, d) in enumerate(head, start=1):
        ws.cell(i, 1, a).font = font(bold=True)
        ws.cell(i, 2, b).font = font(italic=True)
        ws.merge_cells(start_row=i, start_column=3, end_row=i, end_column=6)
        ws.cell(i, 3, c).font = font(bold=True)
        ws.merge_cells(start_row=i, start_column=7, end_row=i, end_column=12)
        ws.cell(i, 7, d).font = font(italic=True)
    ws.cell(3, 1, "Test requirement").font = font(bold=True)
    ws.merge_cells("B3:L3")
    ws.cell(3, 2, s["req"]).font = font(italic=True)
    # Thống kê (công thức)
    labels = [(1, 1, "Passed"), (2, 2, "Failed"), (3, 5, "Untested"), (6, 7, "N"), (8, 9, "A"), (10, 10, "B"), (11, 12, "Total Test Cases")]
    ws.merge_cells("F4:J4"); ws.cell(4, 6, "N/A/B")
    for a, b, t in [(1, 1, "Passed"), (2, 2, "Failed"), (3, 5, "Untested"), (11, 12, "Total Test Cases")]:
        if a != b: ws.merge_cells(start_row=4, start_column=a, end_row=4, end_column=b)
        ws.cell(4, a, t)
    return_row = None
    return first, last, L


def matrix(ws, s, first, last, L):
    n = s["n"]
    top = 7
    # Dòng tiêu đề UTCID
    ws.row_dimensions[top].height = 62
    ws.merge_cells(start_row=top, start_column=1, end_row=top, end_column=2)
    for c in range(1, last + 1):
        cell = ws.cell(top, c); cell.fill = NAVY; cell.border = BOX
    for i in range(n):
        cell = ws.cell(top, first + i, f"UTCID{i + 1:02d}")
        cell.font = font(bold=True, color="FFFFFF", size=9)
        cell.alignment = Alignment(text_rotation=90, horizontal="center", vertical="center")

    row = top + 1
    sections = []

    def line(text, cols=None, bold=False, right=False, indent=0):
        nonlocal row
        b = ws.cell(row, 2, text)
        b.font = font(bold=bold)
        b.alignment = Alignment(horizontal="right" if right else "left", indent=indent, vertical="center", wrap_text=True)
        for c in range(2, last + 1):
            ws.cell(row, c).border = BOX
        if cols:
            for k in cols:
                m = ws.cell(row, first + k - 1, "O")
                m.font = font(bold=True); m.alignment = Alignment(horizontal="center", vertical="center")
        if right and len(text) > 70:
            ws.row_dimensions[row].height = 26
        row += 1

    def section(label, body):
        start = row
        body()
        sections.append((label, start, row - 1))

    def cond():
        line("Precondition", bold=True)
        for group, values in s["pre"]:
            line(group, bold=True, indent=1)
            for v, cols in values:
                line(v, cols, right=True)
        line("")

    def inputs():
        line("Input", bold=True)
        for group, values in s["inp"]:
            line(group, bold=True, indent=1)
            for v, cols in values:
                line(v, cols, right=True)
        line("")

    def confirm():
        line("Return", bold=True)
        for v, cols in s["ret"]:
            line(v, cols, right=True)
        line("")
        line("Exception", bold=True)
        for v, cols in s["exc"]:
            line(v, cols, right=True)
        line("")

    section("Condition", cond)
    section("", inputs)
    section("Confirm", confirm)

    # Kết quả
    res_start = row
    names = ["Type(N : Normal, A : Abnormal, B : Boundary)", "Passed/Failed", "Executed Date", "Defect ID"]
    rows = {}
    for nm in names:
        ws.cell(row, 2, nm).font = font()
        for c in range(2, last + 1):
            ws.cell(row, c).border = BOX
            ws.cell(row, c).alignment = Alignment(horizontal="center", vertical="center")
        rows[nm] = row
        row += 1
    for i in range(n):
        col = first + i
        ws.cell(rows[names[0]], col, s["types"][i]).font = font()
        ws.cell(rows[names[1]], col, "P").font = font()
        ws.cell(rows[names[2]], col, DATE).font = font(size=7)
    ws.cell(rows[names[0]], 2).alignment = Alignment(horizontal="left")
    for nm in names[1:]:
        ws.cell(rows[nm], 2).alignment = Alignment(horizontal="left")
    sections.append(("Result", res_start, row - 1))

    # Cột A: nhãn khu vực nền xanh đậm
    for label, a, b in sections:
        ws.merge_cells(start_row=a, start_column=1, end_row=b, end_column=1)
        cell = ws.cell(a, 1, label)
        cell.font = font(bold=True, color="FFFFFF"); cell.alignment = Alignment(vertical="top", horizontal="left")
        for rr in range(a, b + 1):
            ws.cell(rr, 1).fill = NAVY
            ws.cell(rr, 1).border = Border(left=medium, right=medium, top=medium if rr == a else None, bottom=medium if rr == b else None)

    # Thống kê đầu trang bằng công thức
    tr, pr = rows[names[0]], rows[names[1]]
    span_type = f"$C${tr}:${L}${tr}"
    span_pf = f"$C${pr}:${L}${pr}"
    span_id = f"$C${top}:${L}${top}"
    vals = [(1, f'=COUNTIF({span_pf},"P")'), (2, f'=COUNTIF({span_pf},"F")'), (3, f'=L5-A5-B5'),
            (6, f'=COUNTIF({span_type},"N")'), (8, f'=COUNTIF({span_type},"A")'), (10, f'=COUNTIF({span_type},"B")'),
            (11, f'=COUNTA({span_id})')]
    ws.merge_cells("C5:E5"); ws.merge_cells("F5:G5"); ws.merge_cells("H5:I5"); ws.merge_cells("K5:L5")
    for c, f in vals:
        ws.cell(5, c, f)
    # K5 là ô gộp K5:L5 nên Untested phải trỏ K5
    ws.cell(5, 3, "=K5-A5-B5")
    for rr in (4, 5):
        for c in range(1, 13):
            cell = ws.cell(rr, c)
            cell.font = font(bold=(rr == 4))
            cell.alignment = Alignment(horizontal="center", vertical="center")
            cell.border = Border(top=medium if rr == 4 else None, bottom=medium if rr == 5 else thin)
    for rr in (1, 2, 3):
        for c in range(1, 13):
            ws.cell(rr, c).border = Border(bottom=thin, top=medium if rr == 1 else None)
    ws.freeze_panes = ws.cell(top + 1, 3)
    return rows


wb = Workbook()
summary = wb.active
summary.title = "Tong hop"
info = []
for s in SHEETS:
    ws = wb.create_sheet(s["name"])
    first, last, L = build(ws, s)
    matrix(ws, s, first, last, L)
    ws.page_setup.orientation = "landscape"
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    info.append(s)

# ---- Sheet Tổng hợp ----
ws = summary
ws.sheet_view.showGridLines = False
ws["A1"] = "Unit test FE-10 — Hỗ trợ khách hàng (Support Ticket)"
ws["A1"].font = font(bold=True, size=14)
ws["A2"] = f"Người thực hiện: {AUTHOR} · Ngày chạy: {DATE} · Mỗi sheet là một hàm; mỗi cột UTCID là một test trong file Java tương ứng (thư mục backend/src/test/java/com/kizuna/features/ticket/unit/)."
ws["A2"].font = font(italic=True, size=9)
heads = ["STT", "Sheet", "Hàm được test", "File test (Java)", "Passed", "Failed", "Untested", "N", "A", "B", "Tổng"]
widths = [6, 18, 36, 28, 9, 9, 10, 7, 7, 7, 8]
for i, (h, w) in enumerate(zip(heads, widths), start=1):
    c = ws.cell(4, i, h)
    c.font = font(bold=True, color="FFFFFF"); c.fill = NAVY; c.border = BOX
    c.alignment = Alignment(horizontal="center", vertical="center")
    ws.column_dimensions[get_column_letter(i)].width = w
for k, s in enumerate(info, start=1):
    rr = 4 + k
    q = f"'{s['name']}'!"
    values = [k, s["name"], f"SupportTicket · {s['method']}", s["file"],
              f"={q}A5", f"={q}B5", f"={q}C5", f"={q}F5", f"={q}H5", f"={q}J5", f"={q}K5"]
    for i, v in enumerate(values, start=1):
        c = ws.cell(rr, i, v)
        c.font = font(); c.border = BOX
        c.alignment = Alignment(horizontal="left" if i in (2, 3, 4) else "center")
    ws.cell(rr, 2).hyperlink = f"#'{s['name']}'!A1"
    ws.cell(rr, 2).font = font(color="0563C1", underline="single")
tot = 5 + len(info)
ws.cell(tot, 1, "Cộng").font = font(bold=True)
ws.merge_cells(start_row=tot, start_column=1, end_row=tot, end_column=4)
for i in range(5, 12):
    col = get_column_letter(i)
    c = ws.cell(tot, i, f"=SUM({col}5:{col}{tot - 1})")
    c.font = font(bold=True); c.alignment = Alignment(horizontal="center")
for i in range(1, 12):
    ws.cell(tot, i).border = BOX
ws.freeze_panes = "A5"
ws.page_setup.orientation = "landscape"
ws.sheet_properties.pageSetUpPr.fitToPage = True
ws.page_setup.fitToWidth = 1
ws.page_setup.fitToHeight = 0

wb.calculation.fullCalcOnLoad = True
wb.save(OUT)
print("saved", OUT)
