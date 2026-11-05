package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.entity.TechnicalIncident;
import com.kizuna.features.admin.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.io.*;
import java.time.*;
import java.util.*;

/** Xuất hồ sơ sự cố đã ghi nhận ra Excel; chỉ đọc, không nhập sự cố từ tệp. */
@Service @RequiredArgsConstructor
public class TechnicalIncidentExportService {
    public static final int MAX_ROWS=5000;
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String[] HEADERS={"Mã sự cố","Tiêu đề","Mức độ","Trạng thái","Người phụ trách","Ngày ghi nhận","Kết quả xử lý"};
    private static final int[] WIDTHS={38,48,14,16,26,18,60};
    private final TechnicalIncidentRepository incidents;
    private final TechnicalIncidentEventRepository events;
    private final OperationsActorResolver actors;

    /** Trả null khi không có sự cố phù hợp để giao diện báo đúng, không tải về tệp rỗng. */
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=20)
    public byte[] export(String status,String severity,String component,UUID assignee,LocalDate from,LocalDate to) {
        actors.requireAdmin();
        var spec=TechnicalIncidentService.filter(status,severity,component,assignee,from,to);
        long total=incidents.count(spec);
        if(total==0) return null;
        if(total>MAX_ROWS) throw new BusinessException(422,"Có "+total+" sự cố phù hợp, vượt giới hạn "+MAX_ROWS+" dòng. Vui lòng thu hẹp bộ lọc.");
        var rows=incidents.findAll(spec,TechnicalIncidentService.LIST_ORDER);
        var results=results(rows);
        var names=actors.names(rows.stream().map(TechnicalIncident::getAssigneeId).filter(Objects::nonNull).distinct().toList());
        return workbook(rows,results,names);
    }

    /** Sự cố đã mở lại không còn kết quả hiệu lực nên chỉ lấy lần giải quyết cuối của hồ sơ đang giải quyết/đóng. */
    private Map<UUID,String> results(List<TechnicalIncident> rows) {
        var ids=rows.stream().filter(i->Set.of("RESOLVED","CLOSED").contains(i.getStatus())).map(TechnicalIncident::getIncidentId).toList();
        var results=new HashMap<UUID,String>();
        if(!ids.isEmpty()) events.findResolutions(ids).forEach(e->results.put(e.getIncidentId(),e.getDetail()));
        return results;
    }

    private byte[] workbook(List<TechnicalIncident> rows,Map<UUID,String> results,Map<UUID,String> names) {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Sự cố kỹ thuật");
            var font=book.createFont(); font.setBold(true);
            var headerStyle=book.createCellStyle(); headerStyle.setFont(font);
            var dateStyle=book.createCellStyle(); dateStyle.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            var header=sheet.createRow(0);
            for(int c=0;c<HEADERS.length;c++) { var cell=header.createCell(c); cell.setCellValue(HEADERS[c]); cell.setCellStyle(headerStyle); sheet.setColumnWidth(c,WIDTHS[c]*256); }
            sheet.createFreezePane(0,1);
            int n=1;
            for(var i:rows) {
                var row=sheet.createRow(n++);
                // setCellValue(String) luôn tạo ô văn bản nên nội dung bắt đầu bằng "=" không thành công thức.
                row.createCell(0).setCellValue(i.getIncidentId().toString());
                row.createCell(1).setCellValue(i.getSubject());
                row.createCell(2).setCellValue(TechnicalIncidentService.SEVERITY_LABELS.get(i.getSeverity()));
                row.createCell(3).setCellValue(TechnicalIncidentService.STATUS_LABELS.get(i.getStatus()));
                row.createCell(4).setCellValue(i.getAssigneeId()==null?"Chưa phân công":names.get(i.getAssigneeId()));
                var created=row.createCell(5); created.setCellValue(i.getCreatedAt().atZoneSameInstant(ZONE).toLocalDateTime()); created.setCellStyle(dateStyle);
                var result=results.get(i.getIncidentId());
                if(result!=null) row.createCell(6).setCellValue(result);
            }
            book.write(out);
            return out.toByteArray();
        } catch(IOException exception) {
            throw new BusinessException(503,"Tạm thời chưa tạo được tệp Excel. Vui lòng thử lại sau.");
        }
    }
}
