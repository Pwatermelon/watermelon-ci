package io.watermelon.ci.api.advice;

import io.watermelon.ci.common.error.ErrorCode;
import io.watermelon.ci.common.error.NotFoundException;
import io.watermelon.ci.common.error.PlatformException;
import io.watermelon.ci.manifest.validate.ManifestValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(ManifestValidationException.class)
    public ResponseEntity<Map<String, Object>> manifest(ManifestValidationException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), ex.getViolations());
    }

    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<Map<String, Object>> platform(PlatformException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case CONFLICT -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED, MANIFEST_INVALID -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return body(status, ex.getCode(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return body(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "validation failed", details);
    }

    private static ResponseEntity<Map<String, Object>> body(
            HttpStatus status, ErrorCode code, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "code", code.name(),
                        "message", message,
                        "details", details));
    }
}
