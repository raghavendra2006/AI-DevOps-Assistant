package com.aidevops.service;

import com.aidevops.audit.SecurityAuditEvent;
import com.aidevops.domain.*;
import com.aidevops.dto.request.LoginRequest;
import com.aidevops.dto.request.RefreshTokenRequest;
import com.aidevops.dto.request.RegisterRequest;
import com.aidevops.dto.response.ApiResponse;
import com.aidevops.dto.response.AuthResponse;
import com.aidevops.exception.AccountNotVerifiedException;
import com.aidevops.exception.DuplicateResourceException;
import com.aidevops.exception.InvalidTokenException;
import com.aidevops.repository.*;
import com.aidevops.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(authService, "verificationTokenExpirationMs", 86400000L);
    }

    @Test
    @DisplayName("Should successfully register a new user and generate a verification token")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest("dev@aidevops.io", "Password123!", "Jane", "Doe");
        when(userRepository.existsByEmail("dev@aidevops.io")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword");

        Role viewerRole = new Role("ROLE_VIEWER", "Viewer");
        when(roleRepository.findByName("ROLE_VIEWER")).thenReturn(Optional.of(viewerRole));

        ApiResponse<String> response = authService.register(request, "127.0.0.1", "JUnit-Agent");

        assertTrue(response.success());
        assertNotNull(response.data()); // returns verification token

        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(eventPublisher).publishEvent(any(SecurityAuditEvent.class));
    }

    @Test
    @DisplayName("Should reject registration if email is already registered")
    void testRegister_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest("existing@aidevops.io", "Password123!", "Jane", "Doe");
        when(userRepository.existsByEmail("existing@aidevops.io")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
            authService.register(request, "127.0.0.1", "JUnit-Agent")
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should authenticate and return JWT token pair on valid login")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("dev@aidevops.io", "Password123!");
        User user = new User("dev@aidevops.io", "hashedPassword", "Jane", "Doe");
        user.setId(10L);
        user.setEmailVerified(true);
        user.setEnabled(true);

        when(userRepository.findByEmail("dev@aidevops.io")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), eq(10L))).thenReturn("mock.jwt.token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.login(request, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("dev@aidevops.io", response.user().email());

        verify(refreshTokenRepository).revokeAllUserTokens(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(eventPublisher).publishEvent(any(SecurityAuditEvent.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException on invalid password")
    void testLogin_BadCredentials() {
        LoginRequest request = new LoginRequest("dev@aidevops.io", "WrongPassword!");
        User user = new User("dev@aidevops.io", "hashedPassword", "Jane", "Doe");

        when(userRepository.findByEmail("dev@aidevops.io")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
            authService.login(request, "127.0.0.1", "JUnit-Agent")
        );

        verify(eventPublisher).publishEvent(any(SecurityAuditEvent.class));
    }

    @Test
    @DisplayName("Should throw AccountNotVerifiedException if email is not verified")
    void testLogin_UnverifiedEmail() {
        LoginRequest request = new LoginRequest("dev@aidevops.io", "Password123!");
        User user = new User("dev@aidevops.io", "hashedPassword", "Jane", "Doe");
        user.setEmailVerified(false);

        when(userRepository.findByEmail("dev@aidevops.io")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);

        assertThrows(AccountNotVerifiedException.class, () ->
            authService.login(request, "127.0.0.1", "JUnit-Agent")
        );
    }

    @Test
    @DisplayName("Should successfully refresh tokens with rotation")
    void testRefreshToken_Success() {
        String rawToken = "sample-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);

        User user = new User("dev@aidevops.io", "hashedPassword", "Jane", "Doe");
        user.setId(5L);

        RefreshToken tokenEntity = new RefreshToken(user, "hashedToken", Instant.now().plusSeconds(3600));
        tokenEntity.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));
        when(jwtService.generateAccessToken(any(), eq(5L))).thenReturn("new.access.token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.refreshToken(request, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertEquals("new.access.token", response.accessToken());
        assertTrue(tokenEntity.isRevoked()); // Verified rotation revocation
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should reject revoked refresh token")
    void testRefreshToken_Revoked() {
        RefreshTokenRequest request = new RefreshTokenRequest("revoked-token");
        User user = new User("dev@aidevops.io", "hashedPassword", "Jane", "Doe");
        RefreshToken tokenEntity = new RefreshToken(user, "hash", Instant.now().plusSeconds(3600));
        tokenEntity.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(tokenEntity));

        assertThrows(InvalidTokenException.class, () ->
            authService.refreshToken(request, "127.0.0.1", "JUnit-Agent")
        );
    }
}
