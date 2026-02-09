package com.devops.inventory_service.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // Import chuẩn security
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. MONITORING: BẮT LỖI 403 (QUAN TRỌNG NHẤT CHO SECURITY)
    // Giúp Grafana vẽ được biểu đồ tấn công RBAC
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.FORBIDDEN.value()); // Trả về 403
        error.put("error", "Forbidden");
        error.put("message", "Access Denied: Bro không đủ quyền hạn!");

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 2. MONITORING: BẮT LỖI 401 (SAI PASS / HẾT HẠN TOKEN)
    // Giúp phát hiện Brute-force attack
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.UNAUTHORIZED.value()); // Trả về 401
        error.put("error", "Unauthorized");
        error.put("message", "Sai tài khoản hoặc mật khẩu rồi bro!");

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 3. MONITORING: BẮT LỖI VALIDATION (LỖI INPUT - 400)
    // Cái này giữ nguyên như cũ của bro là ngon rồi
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // 4. MONITORING: BẮT CÁC LỖI LOGIC KHÁC (400)
    // Ví dụ: User đã tồn tại, Hết hàng...
    @ExceptionHandler(RuntimeException.class) // Cái này chỉ hứng những lỗi Runtime còn lại thôi
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 5. MONITORING: BẮT LỖI SERVER (500)
    // Những lỗi không ngờ tới (NullPointer, DB chết...)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Internal Server Error");
        error.put("message", "Lỗi Server nghiêm trọng: " + ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}