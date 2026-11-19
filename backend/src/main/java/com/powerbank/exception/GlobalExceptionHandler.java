package com.powerbank.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return body(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Some fields are invalid", fields);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException ex) {
        return body(HttpStatus.CONFLICT, "CONFLICT", "This record already exists or breaks a data rule", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        // Details go to the server log only, never to the client.
        ex.printStackTrace();
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong on the server", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message, Object fields) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("message", message);
        if (fields != null) map.put("fields", fields);
        return ResponseEntity.status(status).body(map);
    }
}
