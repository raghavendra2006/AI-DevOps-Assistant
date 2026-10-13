package com.aidevops.service;

import com.aidevops.audit.SecurityAuditEvent;
import com.aidevops.domain.Role;
import com.aidevops.domain.User;
import com.aidevops.dto.request.UpdateRolesRequest;
import com.aidevops.dto.response.UserResponse;
import com.aidevops.exception.ResourceNotFoundException;
import com.aidevops.repository.RoleRepository;
import com.aidevops.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Should retrieve paginated list of users")
    void testGetAllUsers() {
        User user = new User("dev@aidevops.io", "hash", "Jane", "Doe");
        user.setId(1L);
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(PageRequest.class))).thenReturn(userPage);

        Page<UserResponse> result = userService.getAllUsers(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("dev@aidevops.io", result.getContent().get(0).email());
    }

    @Test
    @DisplayName("Should successfully update user roles and trigger audit event")
    void testUpdateUserRoles_Success() {
        User user = new User("dev@aidevops.io", "hash", "Jane", "Doe");
        user.setId(1L);

        Role engineerRole = new Role("ROLE_ENGINEER", "Engineer");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_ENGINEER")).thenReturn(Optional.of(engineerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateRolesRequest request = new UpdateRolesRequest(Set.of("ROLE_ENGINEER"));
        UserResponse response = userService.updateUserRoles(1L, request, "admin@aidevops.io", "127.0.0.1", "Agent");

        assertNotNull(response);
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(SecurityAuditEvent.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user does not exist")
    void testUpdateUserRoles_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateRolesRequest request = new UpdateRolesRequest(Set.of("ROLE_ENGINEER"));

        assertThrows(ResourceNotFoundException.class, () ->
            userService.updateUserRoles(99L, request, "admin@aidevops.io", "127.0.0.1", "Agent")
        );
    }
}
