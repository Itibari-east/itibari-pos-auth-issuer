package com.itibari.auth.web;

import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.request.AuthUserRequest;
import com.itibari.auth.model.request.CreateUserDTO;
import com.itibari.auth.model.request.ShopAllocationDTO;
import com.itibari.auth.model.request.UserRoleDto;
import com.itibari.auth.model.response.AuthUserResponse;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.RoleResponse;
import com.itibari.auth.service.AuthUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserRestController {
    private final AuthUserService authUserService;

    @PostMapping
    @Operation(summary = "Create user")
    public ResponseEntity<CustomResponse> createUser(@Valid @RequestBody CreateUserDTO createUserDTO) {
        AuthUser user = authUserService.createUser(createUserDTO);
        return ResponseEntity.ok(new CustomResponse("User created successfully", user.fullName()));
    }

    @GetMapping
    @Operation(summary = "List users")
    public PageResponse<AuthUserResponse> getUsers(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(authUserService.getUsers(searchParam, pageable));
    }

    @GetMapping({"/me", "/currentUser"})
    @Operation(summary = "Current authenticated user")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authUserService.getSessionUser(userPublicId(jwt));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get user by public id")
    public AuthUserResponse getUser(@PathVariable UUID publicId) {
        return authUserService.getUserByPublicId(publicId);
    }

    @PatchMapping("/{publicId}")
    @Operation(summary = "Update user")
    public AuthUserResponse updateUser(@PathVariable UUID publicId,
                                       @Valid @RequestBody AuthUserRequest request) {
        return authUserService.updateUser(publicId, request);
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Soft-delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID publicId) {
        authUserService.deleteUser(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/roles")
    @Operation(summary = "List roles assigned to user")
    public PageResponse<RoleResponse> getUserRoles(
            @PathVariable UUID userId,
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(authUserService.getUserRoles(userId, searchParam, pageable));
    }

    @PatchMapping("/{userId}/roles")
    @Operation(summary = "Assign roles to user")
    public ResponseEntity<CustomResponse> assignRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleDto request) {
        String roles = authUserService.assignRoles(userId, request);
        return ResponseEntity.ok(new CustomResponse("successfully assigned roles", roles));
    }

    @GetMapping("/{userId}/privileges")
    @Operation(summary = "List privileges for user roles")
    public PageResponse<Map<String, Map<String, Object>>> getUserPrivileges(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(authUserService.getUserPrivileges(userId, pageable));
    }

    @PatchMapping("/{userId}/shops")
    @Operation(summary = "Allocate shops to a user")
    public ResponseEntity<CustomResponse> allocateShops(
            @PathVariable UUID userId,
            @Valid @RequestBody ShopAllocationDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        AuthUser user = authUserService.allocateShops(userPublicId(jwt), userId, request);
        return ResponseEntity.ok(new CustomResponse("successfully allocated shops", user.fullName()));
    }

    @DeleteMapping("/{userId}/shops/{shopId}")
    @Operation(summary = "Remove a shop allocation from a user")
    public ResponseEntity<CustomResponse> removeShopAllocation(
            @PathVariable UUID userId,
            @PathVariable UUID shopId,
            @RequestParam UUID parentTenantId,
            @AuthenticationPrincipal Jwt jwt) {
        AuthUser user = authUserService.removeShopAllocation(
                userPublicId(jwt), userId, parentTenantId, shopId);
        return ResponseEntity.ok(new CustomResponse("successfully removed shop allocation", user.fullName()));
    }

    private UUID userPublicId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("public_id"));
    }
}
