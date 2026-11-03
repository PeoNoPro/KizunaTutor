package com.kizuna.core.exception;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.OffsetDateTime;
import java.util.Map;
/** Xử lý lỗi dùng chung cho mọi controller: chỉ trả thông điệp tiếng Việt, không lộ tên exception. */
@RestControllerAdvice @Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> uploadLimit(Exception e) { return error(413,"Tệp đính kèm vượt dung lượng cho phép. Mỗi tệp tối đa 10 MB, tối đa 5 tệp mỗi lần gửi."); }
    @ExceptionHandler(BusinessException.class) public ResponseEntity<?> business(BusinessException e) { return error(e.status(),e.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(AccessDeniedException e) { return error(403,"Bạn không có quyền thực hiện thao tác này."); }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> input(Exception e) { return error(400,"Vui lòng kiểm tra thông tin đã nhập."); }
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(org.springframework.web.bind.MethodArgumentNotValidException e) {
        var fields=e.getBindingResult().getFieldErrors().stream().map(f->Map.of("field",f.getField(),"message",f.getDefaultMessage()==null?"Thông tin chưa hợp lệ.":f.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(Map.of("success",false,"message","Vui lòng kiểm tra thông tin đã nhập.","fieldErrors",fields,"timestamp",OffsetDateTime.now()));
    }
    @ExceptionHandler({OptimisticLockingFailureException.class,CannotAcquireLockException.class})
    public ResponseEntity<?> conflict(Exception e) { return error(409,"Dữ liệu đang được thay đổi. Vui lòng tải lại trước khi lưu."); }
    @ExceptionHandler(Exception.class) public ResponseEntity<?> unavailable(Exception e) {
        // Lỗi chuẩn của Spring MVC (sai đường dẫn, sai phương thức…) giữ đúng mã trạng thái.
        if (e instanceof ErrorResponse framework && framework.getStatusCode().is4xxClientError()) {
            int status=framework.getStatusCode().value();
            return error(status,status==404?"Không tìm thấy nội dung bạn yêu cầu.":"Vui lòng kiểm tra thông tin đã nhập.");
        }
        return error(503,"Tạm thời chưa tải được dữ liệu. Vui lòng thử lại sau.");
    }
    private ResponseEntity<?> error(int status,String message) {
        return ResponseEntity.status(status).body(Map.of("success",false,"message",message,"timestamp",OffsetDateTime.now()));
    }
}
