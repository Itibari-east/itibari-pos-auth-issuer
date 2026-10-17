package com.itibari.auth.web;

import com.itibari.auth.model.request.CreateRoleDTO;
import com.itibari.auth.model.request.RolePrivilegeDto;
import com.itibari.auth.model.request.RolePrivilegeGroupDto;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.RoleResponse;
import com.itibari.auth.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "roles")
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleRestController {

    private final RoleService roleService;

    @PostMapping
    @Operation(summary = "Create role")
    public RoleResponse createRole(@Valid @RequestBody CreateRoleDTO createRoleDTO) {
        return roleService.createRole(createRoleDTO);
    }

    @GetMapping
    @Operation(summary = "List roles")
    public PageResponse<RoleResponse> getAllRoles(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "parentTenantPublicId", required = false) UUID parentTenantPublicId,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(roleService.getAllRoles(searchParam, parentTenantPublicId, pageable));
    }

    @PatchMapping("/assignPrivilegeToRole/{rolePublicId}")
    @Operation(summary = "Assign privileges to role")
    public ResponseEntity<CustomResponse> assignPrivilegeToRole(
            @PathVariable UUID rolePublicId,
            @Valid @RequestBody RolePrivilegeDto request) {
        String privileges = roleService.assignPrivilegeToRole(rolePublicId, request);
        return ResponseEntity.ok(new CustomResponse("successfully added privilege", privileges));
    }

    @PatchMapping("/{rolePublicId}/privilege-groups")
    @Operation(summary = "Assign privilege groups to role")
    public ResponseEntity<CustomResponse> assignPrivilegeGroupsToRole(
            @PathVariable UUID rolePublicId,
            @Valid @RequestBody RolePrivilegeGroupDto request) {
        String groups = roleService.assignPrivilegeGroupsToRole(rolePublicId, request);
        return ResponseEntity.ok(new CustomResponse("successfully added privilege groups", groups));
    }
}
