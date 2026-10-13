package com.aidevops.controller;

import com.aidevops.dto.request.*;
import com.aidevops.dto.response.ApiResponse;
import com.aidevops.dto.response.AuthResponse;
import com.aidevops.dto.response.UserResponse;
import com.aidevops.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication & Authorization", description = "Endpoints for user registration, login, JWT rotation, and password management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user with default ROLE_VIEWER role and generates an email verification token")
    public ResponseEntity<ApiResponse<String>> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        ApiResponse<String> response = authService.register(request, ipAddress, userAgent);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify user email", description = "Activates the user account by validating the one-time verification token")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
        @RequestParam("token") String token,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        ApiResponse<String> response = authService.verifyEmail(token, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns JWT Access & Refresh token pair")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate & refresh access token", description = "Validates the refresh token, revokes it, and issues a new token pair")
    public ResponseEntity<AuthResponse> refreshToken(
        @Valid @RequestBody RefreshTokenRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.refreshToken(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes user refresh tokens and records an audit trail", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(
        @AuthenticationPrincipal UserDetails userDetails,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String email = userDetails != null ? userDetails.getUsername() : "anonymous";
        ApiResponse<Void> response = authService.logout(email, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Sends password reset instructions and generates a secure one-time token")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        ApiResponse<String> response = authService.forgotPassword(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token", description = "Validates reset token and sets new password, invalidating old sessions")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        ApiResponse<Void> response = authService.resetPassword(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<UserResponse> getCurrentUser(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserResponse response = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf == null || xf.isEmpty() || "unknown".equalsIgnoreCase(xf)) {
            return request.getRemoteAddr();
        }
        return xf.split(",")[0].trim();
    }
}
