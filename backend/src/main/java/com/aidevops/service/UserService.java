package com.aidevops.service;

import com.aidevops.dto.request.UpdateRolesRequest;
import com.aidevops.dto.response.ApiResponse;
import com.aidevops.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse updateUserRoles(Long userId, UpdateRolesRequest request, String actorEmail, String ipAddress, String userAgent);
    ApiResponse<Void> deleteUser(Long userId, String actorEmail, String ipAddress, String userAgent);
}
