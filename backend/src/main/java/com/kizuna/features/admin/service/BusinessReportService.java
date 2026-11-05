package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.repository.BusinessReportRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Doanh thu gói hội viên gia sư và hồ sơ gia sư/JLPT: chỉ đọc số liệu thật, Admin và Manager xem được. */
@Service @RequiredArgsConstructor
public class BusinessReportService {
    public static final int MAX_ROWS=5000;
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Map<String,String> PAYMENT_STATES=Map.of("SUCCESS","Thành công","PENDING","Đang chờ","FAILED","Thất bại");
    private static final Map<String,String> VERIFICATION_STATES=Map.of("UNVERIFIED","Chưa gửi hồ sơ","PENDING","Chờ thẩm định","APPROVED","Đã duyệt","REJECTED","Từ chối","ADDITIONAL_INFO_REQUIRED","Cần bổ sung");
    private static final List<String> REVENUE_HEADERS=List.of("Mã giao dịch","Người thanh toán","Email","Gói hội viên","Số tiền (VND)","Cổng thanh toán","Trạng thái","Thời gian");
    private static final List<String> TUTOR_HEADERS=List.of("Họ tên","Email","JLPT cao nhất","Trạng thái thẩm định","Chứng chỉ đã duyệt","Chứng chỉ chờ duyệt","Ngày gửi hồ sơ","Ngày duyệt");
    private final BusinessReportRepository reports;
    private final OperationsActorResolver actors;
    private final Clock operationsClock;
    /** Luồng thanh toán chưa xây xong; bật khi đã nối để giao diện phân biệt "chưa tích hợp" với "kỳ này không có giao dịch". */
    @org.springframework.beans.factory.annotation.Value("${app.payment.integrated:false}") private boolean sourceIntegrated;

    private record Range(LocalDate startDate,LocalDate endDate,OffsetDateTime start,OffsetDateTime end) { }
    private Range range(LocalDate start,LocalDate end) {
        var period=OperationsIncidentReportService.period(start,end,OffsetDateTime.now(operationsClock));
        return new Range(period.start().toLocalDate(),period.endDate(),period.start(),period.endDate().plusDays(1).atStartOfDay(ZONE).toOffsetDateTime());
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=4)
    public Map<String,Object> revenue(LocalDate start,LocalDate end) {
        actors.requireReporter();var range=range(start,end);
        var byStatus=new HashMap<String,Object[]>();reports.revenueByStatus(range.start(),range.end()).forEach(row->byStatus.put((String)row[0],row));
        var result=new LinkedHashMap<String,Object>();
        result.put("startDate",range.startDate());result.put("endDate",range.endDate());result.put("generatedAt",OffsetDateTime.now(operationsClock));
        result.put("revenue",amount(byStatus.get("SUCCESS")));result.put("successCount",count(byStatus.get("SUCCESS")));
        result.put("sourceIntegrated",sourceIntegrated);
        result.put("pendingCount",count(byStatus.get("PENDING")));result.put("failedCount",count(byStatus.get("FAILED")));
        var byDay=new HashMap<String,Object>();reports.revenueByDay(range.start(),range.end()).forEach(row->byDay.put((String)row[0],row[1]));
        var daily=new ArrayList<Map<String,Object>>();
        for(var day=range.startDate();!day.isAfter(range.endDate());day=day.plusDays(1))daily.add(Map.of("date",day.toString(),"count",byDay.getOrDefault(day.toString(),BigDecimal.ZERO)));
        result.put("daily",daily);
        result.put("byPlan",reports.revenueByPlan(range.start(),range.end()).stream().map(row->{
            var plan=new LinkedHashMap<String,Object>();plan.put("label",row[0]==null?"Gói đã xóa":row[0]);plan.put("count",((Number)row[1]).longValue());plan.put("amount",row[2]);return plan;
        }).toList());
        return result;
    }
    private static long count(Object[] row) {return row==null?0:((Number)row[1]).longValue();}
    private static BigDecimal amount(Object[] row) {return row==null?BigDecimal.ZERO:(BigDecimal)row[2];}

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public Map<String,Object> revenueRows(LocalDate start,LocalDate end) {
        actors.requireReporter();var range=range(start,end);
        return table(REVENUE_HEADERS,revenueCells(range));
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public byte[] revenueExport(LocalDate start,LocalDate end) {
        actors.requireReporter();var range=range(start,end);
        var rows=revenueCells(range);
        return rows.isEmpty()?null:workbook("Doanh thu",REVENUE_HEADERS,rows);
    }
    private List<List<Object>> revenueCells(Range range) {
        limit(reports.transactionCount(range.start(),range.end()),"giao dịch");
        return reports.transactions(range.start(),range.end()).stream().map(row->{
            var cells=new ArrayList<Object>();
            cells.add(row[0].toString());cells.add(row[1]);cells.add(row[2]);cells.add(row[3]==null?"Gói đã xóa":row[3]);cells.add(row[4]);cells.add(row[5]);cells.add(PAYMENT_STATES.get((String)row[6]));cells.add(time(row[7]));
            return (List<Object>)cells;
        }).toList();
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=10)
    public Map<String,Object> tutorRows() {
        actors.requireReporter();
        return table(TUTOR_HEADERS,tutorCells());
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public byte[] tutorExport() {
        actors.requireReporter();
        var rows=tutorCells();
        return rows.isEmpty()?null:workbook("Gia sư và JLPT",TUTOR_HEADERS,rows);
    }
    private List<List<Object>> tutorCells() {
        limit(reports.tutorCount(),"hồ sơ gia sư");
        return reports.tutors().stream().map(row->{
            var cells=new ArrayList<Object>();
            cells.add(row[0]);cells.add(row[1]);cells.add(row[2]);cells.add(VERIFICATION_STATES.getOrDefault((String)row[3],"Cần kiểm tra trạng thái"));
            cells.add(((Number)row[4]).longValue());cells.add(((Number)row[5]).longValue());cells.add(time(row[6]));cells.add(time(row[7]));
            return (List<Object>)cells;
        }).toList();
    }

    private static void limit(long total,String noun) {
        if(total>MAX_ROWS)throw new BusinessException(422,"Có "+total+" "+noun+", vượt giới hạn "+MAX_ROWS+" dòng. Vui lòng thu hẹp khoảng thời gian.");
    }
    private static LocalDateTime time(Object value) {
        if(value==null)return null;
        var instant=value instanceof Instant i?i:value instanceof OffsetDateTime o?o.toInstant():((java.sql.Timestamp)value).toInstant();
        return LocalDateTime.ofInstant(instant,ZONE);
    }
    private static Map<String,Object> table(List<String> headers,List<List<Object>> rows) {
        return Map.of("headers",headers,"rows",rows,"total",rows.size());
    }
    private byte[] workbook(String name,List<String> headers,List<List<Object>> rows) {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet(name);var font=book.createFont();font.setBold(true);
            var bold=book.createCellStyle();bold.setFont(font);
            var dateStyle=book.createCellStyle();dateStyle.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            var moneyStyle=book.createCellStyle();moneyStyle.setDataFormat(book.createDataFormat().getFormat("#,##0"));
            var head=sheet.createRow(0);
            for(int c=0;c<headers.size();c++){var cell=head.createCell(c);cell.setCellValue(headers.get(c));cell.setCellStyle(bold);sheet.setColumnWidth(c,24*256);}
            sheet.createFreezePane(0,1);
            int n=1;
            for(var values:rows){
                var row=sheet.createRow(n++);
                for(int c=0;c<values.size();c++){
                    var value=values.get(c);if(value==null)continue;
                    var cell=row.createCell(c);
                    // Chuỗi luôn là ô văn bản nên tên người dùng bắt đầu bằng "=" không thành công thức.
                    if(value instanceof BigDecimal money){cell.setCellValue(money.doubleValue());cell.setCellStyle(moneyStyle);}
                    else if(value instanceof Long number)cell.setCellValue(number);
                    else if(value instanceof LocalDateTime time){cell.setCellValue(time);cell.setCellStyle(dateStyle);}
                    else cell.setCellValue(value.toString());
                }
            }
            book.write(out);return out.toByteArray();
        } catch(IOException exception) {
            throw new BusinessException(503,"Tạm thời chưa tạo được tệp Excel. Vui lòng thử lại sau.");
        }
    }
}
