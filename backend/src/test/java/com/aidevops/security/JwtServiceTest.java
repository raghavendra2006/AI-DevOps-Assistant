package com.aidevops.security;

import com.aidevops.domain.Role;
import com.aidevops.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 900000L);
    }

    @Test
    @DisplayName("Should generate token, extract subject, and validate signature")
    void testGenerateAndValidateToken() {
        User user = new User("engineer@aidevops.io", "hash", "Alice", "Smith");
        user.setId(42L);
        user.setRoles(Set.of(new Role("ROLE_ENGINEER", "Engineer")));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String token = jwtService.generateAccessToken(userDetails, 42L);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("engineer@aidevops.io", username);

        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertFalse(jwtService.isTokenExpired(token));
    }
}
