package com.itibari.auth.service.impl;

import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.Role;
import com.itibari.auth.model.request.CreateRoleDTO;
import com.itibari.auth.model.request.RolePrivilegeDto;
import com.itibari.auth.model.request.RolePrivilegeGroupDto;
import com.itibari.auth.model.response.RoleResponse;
import com.itibari.auth.repository.PrivilegeGroupRepository;
import com.itibari.auth.repository.PrivilegeRepository;
import com.itibari.auth.repository.RoleRepository;
import com.itibari.auth.service.AuthUserService;
import com.itibari.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeGroupRepository privilegeGroupRepository;
    private final AuthUserService authUserService;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleDTO createRoleDTO) {
        Optional<Role> existing = createRoleDTO.parentTenantPublicId() == null
                ? roleRepository.findByNameIgnoreCaseAndParentTenantPublicIdIsNullAndDeletedIsFalse(createRoleDTO.name())
                : roleRepository.findByNameIgnoreCaseAndParentTenantPublicIdAndDeletedIsFalse(
                createRoleDTO.name(), createRoleDTO.parentTenantPublicId());

        if (existing.isPresent()) {
            return existing.get().toRoleResponse();
        }

        Role role = Role.builder()
                .name(createRoleDTO.name().trim())
                .description(createRoleDTO.description())
                .parentTenantPublicId(createRoleDTO.parentTenantPublicId())
                .build();
        return roleRepository.save(role).toRoleResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> getAllRoles(String searchParam, UUID parentTenantPublicId, Pageable pageable) {
        return roleRepository.listRoles(searchParam, parentTenantPublicId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> getTenantRoles(UUID userPublicId, String searchParam, Pageable pageable) {
        return roleRepository.listRoles(searchParam, authUserService.getParentTenantPublicId(userPublicId), pageable);
    }

    @Override
    @Transactional
    public String assignPrivilegeToRole(UUID rolePublicId, RolePrivilegeDto request) {
        Role role = requireRole(rolePublicId);
        if (request.privilegeUUIDs() == null || request.privilegeUUIDs().isEmpty()) {
            return "";
        }

        List<Privilege> privileges = new ArrayList<>();
        for (UUID privilegeId : request.privilegeUUIDs()) {
            privilegeRepository.findByPublicIdAndDeletedIsFalse(privilegeId)
                    .ifPresent(privileges::add);
        }
        privileges.forEach(role::addPrivilege);
        roleRepository.save(role);
        return privileges.stream().map(Privilege::getName).collect(Collectors.joining(", "));
    }

    @Override
    @Transactional
    public String assignPrivilegeGroupsToRole(UUID rolePublicId, RolePrivilegeGroupDto request) {
        Role role = requireRole(rolePublicId);
        List<PrivilegeGroup> groups = request.privilegeGroupPublicIds().stream()
                .map(id -> privilegeGroupRepository.findByPublicIdAndDeletedIsFalse(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Privilege group")))
                .toList();
        groups.forEach(role::addPrivilegeGroup);
        roleRepository.save(role);
        return groups.stream().map(PrivilegeGroup::getName).collect(Collectors.joining(", "));
    }

    private Role requireRole(UUID rolePublicId) {
        return roleRepository.findByPublicIdAndDeletedIsFalse(rolePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Role"));
    }
}
