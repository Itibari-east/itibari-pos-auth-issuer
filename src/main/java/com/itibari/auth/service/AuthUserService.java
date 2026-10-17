package com.itibari.auth.service;

import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.request.AuthUserRequest;
import com.itibari.auth.model.request.CreateUserDTO;
import com.itibari.auth.model.request.ShopAllocationDTO;
import com.itibari.auth.model.request.UserRoleDto;
import com.itibari.auth.model.response.AuthUserResponse;
import com.itibari.auth.model.response.RoleResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface AuthUserService {
    AuthUser createUser(@Valid CreateUserDTO createUserDTO);

    AuthUserResponse getUserByPublicId(UUID publicId);

    Page<AuthUserResponse> getUsers(String searchParam, Pageable pageable);

    AuthUserResponse updateUser(UUID userId, @Valid AuthUserRequest request);

    void deleteUser(UUID userId);

    AuthUserResponse getSessionUser(UUID requestingUserId);

    Page<RoleResponse> getUserRoles(UUID userId, String searchParam, Pageable pageable);

    String assignRoles(UUID userId, @Valid UserRoleDto request);

    Page<Map<String, Map<String, Object>>> getUserPrivileges(UUID userId, Pageable pageable);

    AuthUser allocateShops(UUID requestingUserId, UUID userId, @Valid ShopAllocationDTO shopAllocationDTO);

    AuthUser removeShopAllocation(UUID requestingUserId, UUID userId, UUID parentTenantId, UUID shopId);
}
