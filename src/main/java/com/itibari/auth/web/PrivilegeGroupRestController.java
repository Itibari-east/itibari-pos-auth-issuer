package com.itibari.auth.web;

import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.request.PrivilegeGroupDto;
import com.itibari.auth.model.request.PrivilegeGroupPrivilegeDto;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.PrivilegeGroupResponse;
import com.itibari.auth.service.PrivilegeGroupService;
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

import java.util.UUID;

@Tag(name = "privilege-groups")
@RestController
@RequestMapping("/privilege-groups")
@RequiredArgsConstructor
public class PrivilegeGroupRestController {

    private final PrivilegeGroupService privilegeGroupService;

    @PostMapping
    @Operation(summary = "Create privilege group")
    public ResponseEntity<CustomResponse> createPrivilegeGroup(
            @Valid @RequestBody PrivilegeGroupDto privilegeGroupDto,
            @AuthenticationPrincipal Jwt jwt) {
        PrivilegeGroup group = privilegeGroupService.createPrivilegeGroup(privilegeGroupDto, userPublicId(jwt));
        return ResponseEntity.ok(new CustomResponse("successfully created privilege group", group.getName()));
    }

    @GetMapping
    @Operation(summary = "List privilege groups")
    public PageResponse<PrivilegeGroupResponse> getAllPrivilegeGroups(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "parentTenantPublicId", required = false) UUID parentTenantPublicId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(
                privilegeGroupService.getPrivilegeGroups(searchParam, parentTenantPublicId, pageable));
    }

    @GetMapping("/tenant")
    @Operation(summary = "List privilege groups for the authenticated user's parent tenant")
    public PageResponse<PrivilegeGroupResponse> getTenantPrivilegeGroups(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return PageResponse.from(
                privilegeGroupService.getTenantPrivilegeGroups(userPublicId(jwt), searchParam, pageable));
    }

    @PatchMapping("/{groupPublicId}/privileges")
    @Operation(summary = "Assign privileges to privilege group")
    public ResponseEntity<CustomResponse> assignPrivileges(
            @PathVariable UUID groupPublicId,
            @Valid @RequestBody PrivilegeGroupPrivilegeDto request) {
        String privileges = privilegeGroupService.assignPrivileges(groupPublicId, request);
        return ResponseEntity.ok(new CustomResponse("successfully added privileges", privileges));
    }

    private UUID userPublicId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("public_id"));
    }
}
