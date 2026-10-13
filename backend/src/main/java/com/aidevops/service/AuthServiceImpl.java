package com.aidevops.service;

import com.aidevops.audit.SecurityAuditEvent;
import com.aidevops.domain.*;
import com.aidevops.dto.request.*;
import com.aidevops.dto.response.ApiResponse;
import com.aidevops.dto.response.AuthResponse;
import com.aidevops.dto.response.UserResponse;
import com.aidevops.exception.*;
import com.aidevops.repository.*;
import com.aidevops.security.CustomUserDetails;
import com.aidevops.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.verification-token-expiration-ms:86400000}")
    private long verificationTokenExpirationMs;

    public AuthServiceImpl(
        UserRepository userRepository,
        RoleRepository roleRepository,
        RefreshTokenRepository refreshTokenRepository,
        VerificationTokenRepository verificationTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ApiResponse<String> register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.email().toLowerCase().trim())) {
            throw new DuplicateResourceException("An account with email " + request.email() + " already exists.");
        }

        Role viewerRole = roleRepository.findByName("ROLE_VIEWER")
            .orElseGet(() -> roleRepository.save(new Role("ROLE_VIEWER", "Read-only viewer")));

        User user = new User(
            request.email().toLowerCase().trim(),
            passwordEncoder.encode(request.password()),
            request.firstName().trim(),
            request.lastName().trim()
        );
        user.getRoles().add(viewerRole);
        user.setEmailVerified(false);
        userRepository.save(user);

        // Generate email verification token
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plusMillis(verificationTokenExpirationMs);

        VerificationToken verificationToken = new VerificationToken(
            user,
            tokenHash,
            TokenType.EMAIL_VERIFICATION,
            expiresAt
        );
        verificationTokenRepository.save(verificationToken);

        // Publish audit event
        eventPublisher.publishEvent(new SecurityAuditEvent(
            user.getEmail(),
            "USER_REGISTERED",
            "USER",
            "SUCCESS",
            ipAddress,
            userAgent,
            "User registered successfully. Verification token generated."
        ));

        return ApiResponse.ok("Registration successful. Please verify your email.", rawToken);
    }

    @Override
    @Transactional
    public ApiResponse<String> verifyEmail(String token, String ipAddress, String userAgent) {
        String tokenHash = hashToken(token);
        VerificationToken verificationToken = verificationTokenRepository
            .findByTokenHashAndTokenType(tokenHash, TokenType.EMAIL_VERIFICATION)
            .orElseThrow(() -> new InvalidTokenException("Invalid or non-existent email verification token."));

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("This verification token has already been used.");
        }

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("This verification token has expired.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        eventPublisher.publishEvent(new SecurityAuditEvent(
            user.getEmail(),
            "EMAIL_VERIFIED",
            "USER",
            "SUCCESS",
            ipAddress,
            userAgent,
            "Email verified successfully."
        ));

        return ApiResponse.ok("Email successfully verified. You can now log in.");
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            eventPublisher.publishEvent(new SecurityAuditEvent(
                email,
                "LOGIN_FAILED",
                "AUTH",
                "FAILURE",
                ipAddress,
                userAgent,
                "Invalid credentials provided."
            ));
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (!user.isEmailVerified()) {
            eventPublisher.publishEvent(new SecurityAuditEvent(
                email,
                "LOGIN_FAILED",
                "AUTH",
                "FAILURE",
                ipAddress,
                userAgent,
                "Login attempt failed: Email not verified."
            ));
            throw new AccountNotVerifiedException("Email is not verified. Please check your email to verify your account.");
        }

        if (!user.isEnabled()) {
            throw new ApiException("User account is disabled.", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails, user.getId());

        // Revoke prior tokens and generate new Refresh Token
        refreshTokenRepository.revokeAllUserTokens(user);

        String rawRefreshToken = UUID.randomUUID().toString();
        String hashedRefreshToken = hashToken(rawRefreshToken);
        Instant refreshTokenExpiry = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken refreshToken = new RefreshToken(user, hashedRefreshToken, refreshTokenExpiry);
        refreshTokenRepository.save(refreshToken);

        eventPublisher.publishEvent(new SecurityAuditEvent(
            user.getEmail(),
            "LOGIN_SUCCESS",
            "AUTH",
            "SUCCESS",
            ipAddress,
            userAgent,
            "User logged in successfully."
        ));

        return new AuthResponse(
            accessToken,
            rawRefreshToken,
            jwtService.getAccessTokenExpirationMs() / 1000,
            UserResponse.fromEntity(user)
        );
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token."));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new InvalidTokenException("Refresh token is revoked or expired. Please log in again.");
        }

        User user = refreshToken.getUser();

        // Token Rotation: revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue new refresh token
        String newRawRefreshToken = UUID.randomUUID().toString();
        String newHashedRefreshToken = hashToken(newRawRefreshToken);
        Instant newRefreshTokenExpiry = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken newRefreshToken = new RefreshToken(user, newHashedRefreshToken, newRefreshTokenExpiry);
        refreshTokenRepository.save(newRefreshToken);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getId());

        eventPublisher.publishEvent(new SecurityAuditEvent(
            user.getEmail(),
            "TOKEN_REFRESH",
            "AUTH",
            "SUCCESS",
            ipAddress,
            userAgent,
            "Access token refreshed successfully with rotation."
        ));

        return new AuthResponse(
            newAccessToken,
            newRawRefreshToken,
            jwtService.getAccessTokenExpirationMs() / 1000,
            UserResponse.fromEntity(user)
        );
    }

    @Override
    @Transactional
    public ApiResponse<Void> logout(String userEmail, String ipAddress, String userAgent) {
        userRepository.findByEmail(userEmail).ifPresent(user -> {
            refreshTokenRepository.revokeAllUserTokens(user);
            eventPublisher.publishEvent(new SecurityAuditEvent(
                user.getEmail(),
                "LOGOUT",
                "AUTH",
                "SUCCESS",
                ipAddress,
                userAgent,
                "User logged out successfully. Refresh tokens revoked."
            ));
        });

        return ApiResponse.ok("Logged out successfully.");
    }

    @Override
    @Transactional
    public ApiResponse<String> forgotPassword(ForgotPasswordRequest request, String ipAddress, String userAgent) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmail(email).orElse(null);

        String rawToken = null;
        if (user != null) {
            rawToken = UUID.randomUUID().toString();
            String tokenHash = hashToken(rawToken);
            Instant expiresAt = Instant.now().plusSeconds(3600); // 1 hour

            VerificationToken resetToken = new VerificationToken(
                user,
                tokenHash,
                TokenType.PASSWORD_RESET,
                expiresAt
            );
            verificationTokenRepository.save(resetToken);

            eventPublisher.publishEvent(new SecurityAuditEvent(
                user.getEmail(),
                "PASSWORD_RESET_REQUESTED",
                "AUTH",
                "SUCCESS",
                ipAddress,
                userAgent,
                "Password reset token issued."
            ));
        }

        return ApiResponse.ok("If an account with that email exists, reset instructions have been sent.", rawToken);
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request, String ipAddress, String userAgent) {
        String tokenHash = hashToken(request.token());
        VerificationToken resetToken = verificationTokenRepository
            .findByTokenHashAndTokenType(tokenHash, TokenType.PASSWORD_RESET)
            .orElseThrow(() -> new InvalidTokenException("Invalid or non-existent password reset token."));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new InvalidTokenException("Password reset token is invalid, used, or expired.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        verificationTokenRepository.save(resetToken);

        // Revoke all existing sessions
        refreshTokenRepository.revokeAllUserTokens(user);

        eventPublisher.publishEvent(new SecurityAuditEvent(
            user.getEmail(),
            "PASSWORD_RESET_SUCCESS",
            "AUTH",
            "SUCCESS",
            ipAddress,
            userAgent,
            "Password changed successfully. Existing sessions revoked."
        ));

        return ApiResponse.ok("Password reset successfully. You can now log in with your new password.");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return UserResponse.fromEntity(user);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
