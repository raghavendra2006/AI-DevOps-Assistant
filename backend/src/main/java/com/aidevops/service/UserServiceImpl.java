package com.aidevops.service;

import com.aidevops.audit.SecurityAuditEvent;
import com.aidevops.domain.Role;
import com.aidevops.domain.User;
import com.aidevops.dto.request.UpdateRolesRequest;
import com.aidevops.dto.response.ApiResponse;
import com.aidevops.dto.response.UserResponse;
import com.aidevops.exception.ResourceNotFoundException;
import com.aidevops.repository.RoleRepository;
import com.aidevops.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserServiceImpl(
        UserRepository userRepository,
        RoleRepository roleRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(
        Long userId,
        UpdateRolesRequest request,
        String actorEmail,
        String ipAddress,
        String userAgent
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Set<Role> updatedRoles = new HashSet<>();
        for (String roleName : request.roles()) {
            Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            updatedRoles.add(role);
        }

        user.setRoles(updatedRoles);
        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new SecurityAuditEvent(
            actorEmail,
            "ROLE_CHANGED",
            "USER_" + userId,
            "SUCCESS",
            ipAddress,
            userAgent,
            "Updated roles for user " + user.getEmail() + " to " + request.roles()
        ));

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteUser(Long userId, String actorEmail, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        userRepository.delete(user);

        eventPublisher.publishEvent(new SecurityAuditEvent(
            actorEmail,
            "USER_DELETED",
            "USER_" + userId,
            "SUCCESS",
            ipAddress,
            userAgent,
            "User deleted: " + user.getEmail()
        ));

        return ApiResponse.ok("User deleted successfully.");
    }
}
