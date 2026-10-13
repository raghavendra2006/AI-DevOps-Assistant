package com.aidevops.service;

import com.aidevops.dto.request.*;
import com.aidevops.dto.response.ApiResponse;
import com.aidevops.dto.response.AuthResponse;
import com.aidevops.dto.response.UserResponse;

public interface AuthService {
    ApiResponse<String> register(RegisterRequest request, String ipAddress, String userAgent);
    ApiResponse<String> verifyEmail(String token, String ipAddress, String userAgent);
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);
    ApiResponse<Void> logout(String userEmail, String ipAddress, String userAgent);
    ApiResponse<String> forgotPassword(ForgotPasswordRequest request, String ipAddress, String userAgent);
    ApiResponse<Void> resetPassword(ResetPasswordRequest request, String ipAddress, String userAgent);
    UserResponse getCurrentUser(String userEmail);
}
