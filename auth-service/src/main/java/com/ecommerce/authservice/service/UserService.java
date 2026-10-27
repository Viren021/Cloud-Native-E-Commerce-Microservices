package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.request.ChangePasswordRequest;
import com.ecommerce.authservice.dto.request.RegisterRequest;
import com.ecommerce.authservice.dto.request.UpdateUserRequest;
import com.ecommerce.authservice.dto.response.UserResponse;
import com.ecommerce.authservice.entity.User;
import org.springframework.data.domain.Page;

public interface UserService {
    User register(RegisterRequest request);

    UserResponse update(Long userId, UpdateUserRequest request);

    UserResponse updateForPrincipal(Long userId, UpdateUserRequest request, String keycloakUserId, boolean administrator);

    String changePassword(ChangePasswordRequest request);

    String delete(Long id);

    String deleteForPrincipal(Long id, String keycloakUserId, boolean administrator);

    User findById(Long userId);

    User findByUsername(String userName);

    User findByIdForPrincipal(Long userId, String keycloakUserId, boolean administrator);

    User findByUsernameForPrincipal(String username, String keycloakUserId, boolean administrator);

    User findByKeycloakUserId(String keycloakUserId);

    Page<UserResponse> findAllUsers(int page, int size, String sortBy, String sortOrder);
}
