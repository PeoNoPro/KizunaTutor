package com.kizuna.features.progress.exception;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.progress.controller.AssignmentController;
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

/** Xử lý lỗi riêng cho API bài tập FE-07 (chỉ áp dụng cho AssignmentController). */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AssignmentController.class)
public class AssignmentExceptionHandler {

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
        log.error("Lỗi không mong muốn ở API bài tập", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ClassroomApiResponse.error("Hệ thống đang bận, vui lòng thử lại sau ít phút.", null));
    }
}
