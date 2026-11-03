package com.kizuna.core.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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
@Slf4j
@RestControllerAdvice @Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> uploadLimit(Exception e) { return error(413,"Tệp đính kèm vượt dung lượng cho phép. Mỗi tệp tối đa 10 MB, tối đa 5 tệp mỗi lần gửi."); }
    @ExceptionHandler(BusinessException.class) public ResponseEntity<?> business(BusinessException e) { return error(e.status(),e.getMessage()); }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> badRequest(BadRequestException e) { return error(400,e.getMessage()!=null?e.getMessage():"Yêu cầu không hợp lệ"); }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> notFound(ResourceNotFoundException e) { return error(404,e.getMessage()!=null?e.getMessage():"Không tìm thấy dữ liệu yêu cầu"); }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badCredentials(BadCredentialsException e) { return error(401,"Email hoặc mật khẩu không chính xác"); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(AccessDeniedException e) { return error(403,"Bạn không có quyền thực hiện thao tác này."); }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> input(Exception e) { return error(400,"Vui lòng kiểm tra thông tin đã nhập."); }
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(org.springframework.web.bind.MethodArgumentNotValidException e) {
        var fields=e.getBindingResult().getFieldErrors().stream().map(f->Map.of("field",f.getField(),"message",f.getDefaultMessage()==null?"Thông tin chưa hợp lệ.":f.getDefaultMessage())).toList();
        // Trang đăng nhập/đăng ký hiển thị trực tiếp "message" nên trả lỗi của trường đầu tiên.
        String message=fields.isEmpty()?"Vui lòng kiểm tra thông tin đã nhập.":fields.get(0).get("message");
        return ResponseEntity.badRequest().body(Map.of("success",false,"message",message,"fieldErrors",fields,"timestamp",OffsetDateTime.now()));
    }
    @ExceptionHandler({OptimisticLockingFailureException.class,CannotAcquireLockException.class})
    public ResponseEntity<?> conflict(Exception e) { return error(409,"Dữ liệu đang được thay đổi. Vui lòng tải lại trước khi lưu."); }
    @ExceptionHandler(Exception.class) public ResponseEntity<?> unavailable(Exception e) {
        // Lỗi chuẩn của Spring MVC (sai đường dẫn, sai phương thức…) giữ đúng mã trạng thái.
        if (e instanceof ErrorResponse framework && framework.getStatusCode().is4xxClientError()) {
            int status=framework.getStatusCode().value();
            return error(status,status==404?"Không tìm thấy nội dung bạn yêu cầu.":"Vui lòng kiểm tra thông tin đã nhập.");
        }
        log.error("Lỗi không xử lý được", e);
        return error(503,"Tạm thời chưa tải được dữ liệu. Vui lòng thử lại sau.");
    }
    private ResponseEntity<?> error(int status,String message) {
        return ResponseEntity.status(status).body(Map.of("success",false,"message",message,"timestamp",OffsetDateTime.now()));
    }
}
