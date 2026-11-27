package com.mealbooking.backend.exception;

import com.mealbooking.backend.domain.error.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> handleBusiness(BusinessException exception) {
        HttpStatus status = switch (exception.rejection()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status).body(body(status, exception.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<?> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body(HttpStatus.UNAUTHORIZED, "Thông tin đăng nhập không chính xác hoặc tài khoản đã ngừng hoạt động."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream().findFirst()
            .map(error -> error instanceof FieldError field ? field.getField() + ": " + field.getDefaultMessage() : error.getDefaultMessage())
            .orElse("Dữ liệu gửi lên không hợp lệ.");
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<?> handleConstraint(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<?> handleDenied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body(HttpStatus.FORBIDDEN,"Không có quyền thực hiện."));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<?> handleIntegrity() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT,"Dữ liệu trùng hoặc vi phạm ràng buộc."));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.multipart.MaxUploadSizeExceededException.class})
    ResponseEntity<?> handleBadRequest() {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST,"Dữ liệu yêu cầu không hợp lệ hoặc ảnh quá lớn."));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> handleOther(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi máy chủ. Vui lòng thử lại."));
    }

    private Map<String, Object> body(HttpStatus status, String message) {
        return Map.of("timestamp", LocalDateTime.now(), "status", status.value(), "error", status.getReasonPhrase(), "message", message);
    }
}
