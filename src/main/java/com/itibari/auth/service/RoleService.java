package com.itibari.auth.service;

import com.itibari.auth.model.request.CreateRoleDTO;
import com.itibari.auth.model.request.RolePrivilegeDto;
import com.itibari.auth.model.request.RolePrivilegeGroupDto;
import com.itibari.auth.model.response.PrivilegeGroupResponse;
import com.itibari.auth.model.response.RoleResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RoleService {

    RoleResponse createRole(@Valid CreateRoleDTO createRoleDTO, UUID loggedInUserPublicId);

    Page<RoleResponse> getAllRoles(String searchParam, UUID parentTenantPublicId, Pageable pageable);

    Page<RoleResponse> getTenantRoles(UUID userPublicId, String searchParam, Pageable pageable);

    String assignPrivilegeToRole(UUID rolePublicId, @Valid RolePrivilegeDto request);

    Page<PrivilegeGroupResponse> getRolePrivilegeGroups(UUID rolePublicId, UUID loggedInUserPublicId,
                                                        Pageable pageable);

    String assignPrivilegeGroupsToRole(UUID rolePublicId, @Valid RolePrivilegeGroupDto request);
}
