package com.aidevops.dto.response;

import com.aidevops.domain.Role;
import com.aidevops.domain.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    boolean isEnabled,
    boolean isEmailVerified,
    Set<String> roles,
    Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.isEnabled(),
            user.isEmailVerified(),
            roleNames,
            user.getCreatedAt()
        );
    }
}
