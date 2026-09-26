package com.moviebooking.web.error;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.moviebooking.exception.ConflictException;
import com.moviebooking.exception.ForbiddenException;
import com.moviebooking.exception.PaymentFailedException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Translates exceptions into the shared {@link ApiError} response shape, so clients see
 * one consistent error format and controllers never build error responses themselves.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiError> handlePaymentFailed(PaymentFailedException ex) {
        return build(HttpStatus.PAYMENT_REQUIRED, ex.getMessage(), null);
    }

    /** A unique-index violation surfacing from MongoDB (e.g. duplicate email). */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> handleDuplicateKey(DuplicateKeyException ex) {
        return build(HttpStatus.CONFLICT, "Resource already exists", null);
    }

    /** Request body failed bean validation (@Valid on a @RequestBody DTO). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<ApiError.ValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toValidationError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    /** Validation failure on a path/query parameter (@Validated on the controller). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    /** A malformed request that validation annotations can't express (e.g. duplicate seats). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    /** Anything unmapped is a server-side fault; log it and hide internals from clients. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private static ApiError.ValidationError toValidationError(FieldError fieldError) {
        return new ApiError.ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
            List<ApiError.ValidationError> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                currentPath(),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String currentPath() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRequestURI();
        }
        return null;
    }
}
