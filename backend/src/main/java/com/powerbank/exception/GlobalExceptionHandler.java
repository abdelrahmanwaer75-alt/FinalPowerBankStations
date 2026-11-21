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
        // The exact database message goes to the server console so the real cause is visible.
        String cause = ex.getMostSpecificCause().getMessage();
        System.err.println("[DATA INTEGRITY] " + cause);
        String detail = cause == null ? "" : cause.toLowerCase(java.util.Locale.ROOT);
        if (detail.contains("uq_rentals_active_user")) {
            return body(HttpStatus.CONFLICT, "OLD_ACTIVE_RENTAL_LIMIT",
                    "The database still has the old one-active-rental limit. Stop the backend and apply the latest database migration, then restart it.", null);
        }
        if (detail.contains("uq_rentals_active_powerbank") || detail.contains("uq_reservations_open_powerbank")) {
            return body(HttpStatus.CONFLICT, "POWERBANK_JUST_TAKEN",
                    "This PowerBank was just reserved by someone else. Refresh the station and choose another available unit.", null);
        }
        if (detail.contains("uq_rentals_reservation")) {
            return body(HttpStatus.CONFLICT, "RESERVATION_ALREADY_USED",
                    "This reservation has already been used or is no longer active. Refresh My PowerBanks and try again.", null);
        }
        return body(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "This change conflicts with the current reservation or PowerBank status. Refresh the page and try again.", null);
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
