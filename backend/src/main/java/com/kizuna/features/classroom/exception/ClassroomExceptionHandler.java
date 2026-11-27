package com.kizuna.features.classroom.exception;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

/**
 * Xử lý lỗi riêng cho API FE-06/FE-07. ClassroomController và AssignmentController kế thừa lớp này,
 * nên các handler là handler cục bộ của controller — Spring luôn ưu tiên chúng hơn mọi @RestControllerAdvice
 * (kể cả GlobalExceptionHandler của core đang đặt HIGHEST_PRECEDENCE và bắt mọi Exception).
 * Không trả stack trace, tên exception hay enum ra ngoài.
 */
@Slf4j
public abstract class ClassroomExceptionHandler {

    @ExceptionHandler(ClassroomException.class)
    public ResponseEntity<ClassroomApiResponse<Object>> handleClassroom(ClassroomException ex) {
        Object data = ex.getFieldErrors() == null ? null : Map.of("fieldErrors", ex.getFieldErrors());
        return ResponseEntity.status(ex.getStatus()).body(ClassroomApiResponse.error(ex.getMessage(), data));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ClassroomApiResponse<Object>> handleMissingPart(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ClassroomApiResponse.error("Vui lòng điền đầy đủ các thông tin bắt buộc.", null));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ClassroomApiResponse<Object>> handleBadInput(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ClassroomApiResponse.error("Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại.", null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ClassroomApiResponse<Object>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ClassroomApiResponse.error("Tệp vượt quá dung lượng cho phép (mỗi tệp tối đa 25 MB, mỗi lần tải lên tối đa 5 tệp, tổng 100 MB).", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ClassroomApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Lỗi không mong muốn ở API lớp học/bài tập", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ClassroomApiResponse.error("Hệ thống đang bận, vui lòng thử lại sau ít phút.", null));
    }
}
