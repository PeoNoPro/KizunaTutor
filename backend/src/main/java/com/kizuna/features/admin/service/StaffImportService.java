package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.StaffImportRequests.*;
import com.kizuna.features.admin.repository.StaffAccountRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.ZipInputStream;

/** Nhập tài khoản nhân viên từ Excel: xem trước không ghi DB, xác nhận tạo tất cả hoặc không tạo gì. */
@Service @RequiredArgsConstructor
public class StaffImportService {
    public static final int MAX_ROWS=100;
    private static final long MAX_FILE_BYTES=2_000_000,MAX_UNPACKED_BYTES=20_000_000;
    private static final String[] HEADERS={"Họ và tên","Email","Vai trò","Số điện thoại"};
    private static final int[] WIDTHS={32,36,26,18};
    private static final Map<String,String> ROLES=Map.of("Cán bộ học vụ","ROLE_ACADEMIC_OFFICER","Chăm sóc khách hàng","ROLE_CS_STAFF");
    private static final Validator VALIDATOR=Validation.buildDefaultValidatorFactory().getValidator();
    private final StaffAccountRepository staff;
    private final StaffAccountService accounts;
    private final OperationsActorResolver actors;
    private final StaffCredentialService credentials;
    private final PlatformTransactionManager transactions;

    public byte[] template() {
        actors.requireAdmin();
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("Nhân viên");
            var font=book.createFont();font.setBold(true);
            var bold=book.createCellStyle();bold.setFont(font);
            var header=sheet.createRow(0);
            for(int c=0;c<HEADERS.length;c++){var cell=header.createCell(c);cell.setCellValue(HEADERS[c]);cell.setCellStyle(bold);sheet.setColumnWidth(c,WIDTHS[c]*256);}
            sheet.createFreezePane(0,1);
            // Số điện thoại để dạng văn bản, nếu không Excel bỏ số 0 ở đầu.
            var textStyle=book.createCellStyle();textStyle.setDataFormat(book.createDataFormat().getFormat("@"));
            sheet.setDefaultColumnStyle(3,textStyle);
            var helper=sheet.getDataValidationHelper();
            var validation=helper.createValidation(helper.createExplicitListConstraint(ROLES.keySet().stream().sorted().toArray(String[]::new)),new CellRangeAddressList(1,MAX_ROWS,2,2));
            validation.setShowErrorBox(true);sheet.addValidationData(validation);
            var guide=book.createSheet("Hướng dẫn");guide.setColumnWidth(0,90*256);
            int n=0;
            for(String line:List.of("Nhập từ dòng 2 của trang Nhân viên, tối đa "+MAX_ROWS+" dòng, không đổi tên cột.","Vai trò chỉ gồm Cán bộ học vụ hoặc Chăm sóc khách hàng.","Không dùng công thức. Số điện thoại có thể để trống.","Hệ thống tự tạo mật khẩu tạm và gửi vào email của từng người, không ghi mật khẩu vào tệp này."))guide.createRow(n++).createCell(0).setCellValue(line);
            book.write(out);return out.toByteArray();
        } catch(IOException exception) {
            throw new BusinessException(503,"Tạm thời chưa tạo được tệp mẫu. Vui lòng thử lại sau.");
        }
    }

    public Map<String,Object> preview(MultipartFile file) {
        actors.requireAdmin();
        var rows=parse(file);var checked=check(rows);
        var items=new ArrayList<Map<String,Object>>();int invalid=0;
        for(var row:rows){
            var errors=checked.getOrDefault(row.rowNumber(),List.of());if(!errors.isEmpty())invalid++;
            var item=new LinkedHashMap<String,Object>();
            item.put("rowNumber",row.rowNumber());item.put("fullName",row.fullName());item.put("email",row.email());item.put("phone",row.phone());item.put("role",row.role());item.put("errors",errors);
            items.add(item);
        }
        return Map.of("rows",items,"total",rows.size(),"errorCount",invalid);
    }

    public Map<String,Object> confirm(Confirm r) {
        actors.requireAdmin();
        var rows=r.rows().stream().map(StaffImportService::normalized).toList();
        var transaction=new TransactionTemplate(transactions);transaction.setTimeout(15);
        return transaction.execute(status->{
            UUID actor=accounts.beginWrite();
            var checked=check(rows);
            if(!checked.isEmpty())throw new BusinessException(409,"Dữ liệu đã thay đổi sau khi xem trước nên chưa tạo tài khoản nào. "+summary(checked)+" Vui lòng kiểm tra lại tệp.");
            var time=accounts.now();var emails=new ArrayList<String>();
            for(var row:rows){
                UUID id=UUID.randomUUID();
                staff.create(id,row.fullName(),row.email(),row.phone(),staff.activeRoleId(ROLES.get(row.role())),StaffCredentialService.UNUSABLE_HASH,time);
                staff.audit(actor,id,"CREATE","Nhập tài khoản từ tệp Excel",time);
                credentials.enqueue(id,actor,time);emails.add(row.email());
            }
            return Map.of("created",emails.size(),"pendingEmails",emails.size(),"emails",emails);
        });
    }

    private static String summary(Map<Integer,List<Map<String,String>>> errors) {
        return errors.entrySet().stream().limit(5).map(entry->"Dòng "+entry.getKey()+": "+entry.getValue().get(0).get("message")).reduce((a,b)->a+" "+b).orElse("");
    }

    /** Trả lỗi theo số dòng; không có khóa nghĩa là dòng hợp lệ. Dùng chung cho xem trước và xác nhận. */
    private Map<Integer,List<Map<String,String>>> check(List<Row> rows) {
        var existing=staff.existingEmails(rows.stream().map(Row::email).filter(Objects::nonNull).filter(email->!email.isEmpty()).distinct().toList());
        var locked=staff.lockedStaffEmails(existing);
        var seen=new HashMap<String,Integer>();var result=new TreeMap<Integer,List<Map<String,String>>>();
        for(var row:rows){
            var errors=new ArrayList<Map<String,String>>();
            VALIDATOR.validate(row).forEach(violation->errors.add(Map.of("field",violation.getPropertyPath().toString(),"message",violation.getMessage())));
            if(row.role()!=null&&!row.role().isBlank()&&!ROLES.containsKey(row.role()))errors.add(Map.of("field","role","message","Vai trò chỉ gồm Cán bộ học vụ hoặc Chăm sóc khách hàng."));
            if(row.email()!=null&&!row.email().isEmpty()){
                var first=seen.putIfAbsent(row.email(),row.rowNumber());
                if(first!=null)errors.add(Map.of("field","email","message","Email trùng với dòng "+first+" trong tệp."));
                else if(existing.contains(row.email()))errors.add(Map.of("field","email","message",locked.contains(row.email())?StaffAccountService.LOCKED_EMAIL_MESSAGE:"Email này đã được sử dụng."));
            }
            if(!errors.isEmpty())result.put(row.rowNumber(),errors);
        }
        return result;
    }

    private static String clean(String value) {return value==null?"":Normalizer.normalize(value,Normalizer.Form.NFC).trim();}
    private static Row normalized(Row row) {
        String phone=clean(row.phone());
        return new Row(row.rowNumber(),clean(row.fullName()),clean(row.email()).toLowerCase(Locale.ROOT),phone.isEmpty()?null:phone,clean(row.role()));
    }

    private List<Row> parse(MultipartFile file) {
        if(file==null||file.isEmpty())throw new BusinessException(400,"Vui lòng chọn tệp Excel.");
        String name=file.getOriginalFilename();
        if(name==null||!name.toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw new BusinessException(400,"Chỉ nhận tệp .xlsx.");
        if(file.getSize()>MAX_FILE_BYTES)throw new BusinessException(400,"Tệp vượt quá giới hạn 2 MB.");
        try {
            byte[] bytes=file.getBytes();unpackedSizeWithinLimit(bytes);
            try(var book=new XSSFWorkbook(new ByteArrayInputStream(bytes))) {return rows(book.getSheetAt(0));}
        } catch(BusinessException exception) {
            throw exception;
        } catch(IOException|RuntimeException exception) {
            throw new BusinessException(400,"Chưa đọc được tệp. Vui lòng dùng tệp .xlsx tải từ mẫu.");
        }
    }

    /** Tệp nén có thể bung rất lớn dù chỉ vài MB, nên đo thật trước khi nạp vào bộ nhớ. */
    private void unpackedSizeWithinLimit(byte[] bytes) throws IOException {
        long total=0;var buffer=new byte[8192];
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            while(zip.getNextEntry()!=null){
                int read;while((read=zip.read(buffer))>0){total+=read;if(total>MAX_UNPACKED_BYTES)throw new BusinessException(400,"Nội dung tệp quá lớn. Vui lòng dùng tệp từ mẫu.");}
            }
        }
    }

    private List<Row> rows(Sheet sheet) {
        var formatter=new DataFormatter();var header=sheet.getRow(0);
        for(int c=0;c<HEADERS.length;c++){
            if(header==null||!HEADERS[c].equalsIgnoreCase(clean(formatter.formatCellValue(header.getCell(c)))))throw new BusinessException(400,"Tệp không đúng mẫu. Vui lòng tải tệp mẫu và giữ nguyên tên cột.");
        }
        var rows=new ArrayList<Row>();
        for(int n=1;n<=sheet.getLastRowNum();n++){
            var row=sheet.getRow(n);if(row==null)continue;
            var values=new String[HEADERS.length];boolean blank=true;
            for(int c=0;c<HEADERS.length;c++){
                var cell=row.getCell(c);
                if(cell!=null&&cell.getCellType()==CellType.FORMULA)throw new BusinessException(400,"Dòng "+(n+1)+" có công thức. Vui lòng nhập giá trị trực tiếp.");
                values[c]=clean(formatter.formatCellValue(cell));if(!values[c].isEmpty())blank=false;
            }
            if(blank)continue;
            if(rows.size()==MAX_ROWS)throw new BusinessException(400,"Tệp có quá "+MAX_ROWS+" dòng. Vui lòng chia thành nhiều tệp.");
            rows.add(normalized(new Row(n+1,values[0],values[1],values[3],values[2])));
        }
        if(rows.isEmpty())throw new BusinessException(400,"Tệp chưa có dòng dữ liệu nào.");
        return rows;
    }
}
