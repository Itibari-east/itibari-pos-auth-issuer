package com.itibari.auth.web;

import com.itibari.auth.doc.Examples;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.request.AuthUserRequest;
import com.itibari.auth.model.request.CreateUserDTO;
import com.itibari.auth.model.request.ShopAllocationDTO;
import com.itibari.auth.model.request.TenantUserRequest;
import com.itibari.auth.model.request.UserRoleDto;
import com.itibari.auth.model.response.AuthUserResponse;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.CustomUserResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.RoleResponse;
import com.itibari.auth.service.AuthUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
    @Operation(summary = "Create user", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.CREATE_USER_OK_RESPONSE)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "Create User Request", value = Examples.CREATE_USER_REQUEST))))
    public ResponseEntity<CustomResponse> createUser(@Valid @RequestBody CreateUserDTO createUserDTO) {
        AuthUser user = authUserService.createUser(createUserDTO);
        return ResponseEntity.ok(new CustomResponse("User created successfully", user.fullName()));
    }

    @PostMapping("/{parentTenantId}")
    @Operation(summary = "Create user under parent tenant", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.CREATE_USER_OK_RESPONSE)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "Create Parent Tenant User Request", value = Examples.CREATE_USER_REQUEST))))
    public ResponseEntity<CustomResponse> createParentTenantUser(
            @PathVariable UUID parentTenantId,
            @Valid @RequestBody CreateUserDTO createUserDTO,
            @AuthenticationPrincipal Jwt jwt) {
        AuthUser user = authUserService.createParentTenantUser(userPublicId(jwt), parentTenantId, createUserDTO);
        return ResponseEntity.ok(new CustomResponse("User created successfully", user.fullName()));
    }

    @PostMapping("/childUser")
    @Operation(summary = "Create a user for the authenticated user's parent tenant",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    examples = {
                            @ExampleObject(name = "Create without initial assignments",
                                    value = Examples.CREATE_TENANT_USER_WITHOUT_ASSIGNMENTS_REQUEST),
                            @ExampleObject(name = "Create with roles and branches",
                                    value = Examples.CREATE_TENANT_USER_WITH_ASSIGNMENTS_REQUEST)
                    })),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created", content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = Examples.CREATE_TENANT_USER_OK_RESPONSE)))
            })
    public ResponseEntity<CustomUserResponse> createTenantUser(
            @Valid @RequestBody TenantUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        AuthUser user = authUserService.createTenantUser(userPublicId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CustomUserResponse("User created successfully", user.fullName(), user.getPublicId()));
    }

    @GetMapping("/childUser")
    @Operation(summary = "List users for the authenticated user's parent tenant", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.USERS_OK_RESPONSE)))
    })
    public PageResponse<AuthUserResponse> getTenantUsers(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return PageResponse.from(authUserService.getTenantUsers(userPublicId(jwt), searchParam, pageable));
    }

    @PatchMapping("/childUser/{userId}/active-status")
    @Operation(summary = "Toggle a tenant user's active status", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.GET_USER_OK_RESPONSE)))
    })
    public AuthUserResponse toggleTenantUserStatus(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        return authUserService.toggleTenantUserStatus(userPublicId(jwt), userId);
    }

    @DeleteMapping("/childUser/{userId}")
    @Operation(summary = "Delete a user from the authenticated user's parent tenant")
    public ResponseEntity<Void> deleteTenantUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        authUserService.deleteTenantUser(userPublicId(jwt), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List users", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.USERS_OK_RESPONSE)))
    })
    public PageResponse<AuthUserResponse> getUsers(
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(authUserService.getUsers(searchParam, pageable));
    }

    @GetMapping({"/me", "/currentUser"})
    @Operation(summary = "Current authenticated user")
    public AuthUserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authUserService.getSessionUser(userPublicId(jwt));
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get user by public id", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.GET_USER_OK_RESPONSE)))
    })
    public AuthUserResponse getUser(@PathVariable UUID publicId) {
        return authUserService.getUserByPublicId(publicId);
    }

    @PatchMapping("/{publicId}")
    @Operation(summary = "Update user", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.GET_USER_OK_RESPONSE)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "Update user request", value = Examples.UPDATE_USER_REQUEST))))
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
    @Operation(summary = "List roles assigned to user", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.USER_ROLES_OK_RESPONSE)))
    })
    public PageResponse<RoleResponse> getUserRoles(
            @PathVariable UUID userId,
            @RequestParam(value = "searchParam", required = false) String searchParam,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(authUserService.getUserRoles(userId, searchParam, pageable));
    }

    @PatchMapping("/{userId}/roles")
    @Operation(summary = "Assign roles to user", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.ASSIGN_ROLE_TO_USER_RESPONSE)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "Assign Roles to User Request",
                    value = Examples.ASSIGN_ROLE_TO_USER_REQUEST))))
    public ResponseEntity<CustomResponse> assignRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UserRoleDto request) {
        String roles = authUserService.assignRoles(userId, request);
        return ResponseEntity.ok(new CustomResponse("successfully assigned roles", roles));
    }

    @GetMapping("/{userId}/privileges")
    @Operation(summary = "List privileges for user roles", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.USER_PRIVILEGES_OK_RESPONSE)))
    })
    public PageResponse<Map<String, Map<String, Object>>> getUserPrivileges(
            @PathVariable UUID userId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(authUserService.getUserPrivileges(userId, pageable));
    }

    @PatchMapping("/{userId}/shops")
    @Operation(summary = "Allocate shops to a user", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.ALLOCATE_SHOPS_TO_USER_RESPONSE)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            examples = @ExampleObject(name = "Allocate Shops to User Request",
                    value = Examples.ALLOCATE_SHOPS_TO_USER_REQUEST))))
    public ResponseEntity<CustomResponse> allocateShops(
            @PathVariable UUID userId,
            @Valid @RequestBody ShopAllocationDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        AuthUser user = authUserService.allocateShops(userPublicId(jwt), userId, request);
        return ResponseEntity.ok(new CustomResponse("successfully allocated shops", user.fullName()));
    }

    @DeleteMapping("/{userId}/shops/{shopId}")
    @Operation(summary = "Remove a shop allocation from a user", responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = Examples.REMOVE_SHOP_ALLOCATION_RESPONSE)))
    })
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
