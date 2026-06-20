package com.ventureverse.ventureverse_api.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        // ==================== RUNTIME EXCEPTIONS ====================

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
                log.error("Runtime exception: {}", ex.getMessage(), ex);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("error", "Bad Request");
                response.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // ==================== ILLEGAL ARGUMENT ====================

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
                log.warn("Illegal argument: {}", ex.getMessage());

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("error", "Bad Request");
                response.put("message", ex.getMessage());

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // ==================== VALIDATION EXCEPTIONS ====================

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
                log.warn("Validation failed: {}", ex.getMessage());

                Map<String, Object> response = new LinkedHashMap<>();
                Map<String, String> errors = new HashMap<>();

                ex.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                response.put("timestamp", LocalDateTime.now().toString());
                response.put("status", HttpStatus.BAD_REQUEST.value());
                response.put("error", "Validation Failed");
                response.put("message", "One or more fields are invalid");
                response.put("errors", errors);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // ==================== NULL POINTER ====================

        @ExceptionHandler(NullPointerException.class)
        public ResponseEntity<Map<String, Object>> handleNullPointer(NullPointerException ex) {
                log.error("Null pointer exception: {}", ex.getMessage(), ex);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.put("error", "Internal Server Error");
                response.put("message", "A required value was missing. Please check your request.");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // ==================== GENERIC EXCEPTION (CATCH-ALL) ====================

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
                log.error("Unhandled exception: {}", ex.getMessage(), ex);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("timestamp", LocalDateTime.now().toString());
                response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.put("error", "Internal Server Error");
                response.put("message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
}