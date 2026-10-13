package com.aidevops.exception;

import com.aidevops.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("API Exception [{}]: {}", ex.getStatus(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            "Invalid email or password",
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "Account is disabled. Please contact system administrator.",
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for URI {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "Access denied: You do not have sufficient permissions to perform this operation",
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Request validation failed",
            request.getRequestURI(),
            errors
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error occurred for URI {}", request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
