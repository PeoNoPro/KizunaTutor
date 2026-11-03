package com.kizuna.features.classroom.exception;

import com.kizuna.features.classroom.controller.ClassroomController;
import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

/**
 * Xử lý lỗi riêng cho API FE-06 (chỉ áp dụng cho ClassroomController, không ảnh hưởng feature khác).
 * Không trả stack trace, tên exception hay enum ra ngoài.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ClassroomController.class)
public class ClassroomExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ClassroomApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Lỗi không mong muốn ở API lớp học", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ClassroomApiResponse.error("Hệ thống đang bận, vui lòng thử lại sau ít phút.", null));
    }
}
