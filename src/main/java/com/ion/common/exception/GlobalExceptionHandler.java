package com.ion.common.exception;

import com.ion.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IonException.class)
    public ResponseEntity<ApiResponse<Void>> handleIonException(IonException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("IonException [{}]: {}", code.name(), e.getMessage());
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code.name(), code.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.COMMON_002.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.COMMON_002.name(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode code = ErrorCode.COMMON_001;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.fail(code.name(), code.getMessage()));
    }
}
