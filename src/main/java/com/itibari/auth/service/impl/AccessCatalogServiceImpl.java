package com.itibari.auth.service.impl;

import com.itibari.auth.access.AccessCatalog;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.Role;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.repository.PrivilegeGroupRepository;
import com.itibari.auth.repository.PrivilegeRepository;
import com.itibari.auth.repository.RoleRepository;
import com.itibari.auth.service.AccessCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessCatalogServiceImpl implements AccessCatalogService {

    private final PrivilegeGroupRepository privilegeGroupRepository;
    private final PrivilegeRepository privilegeRepository;
    private final RoleRepository roleRepository;
    private final AuthUserRepository authUserRepository;

    @Override
    @Transactional
    public void seedGlobalCatalog() {
        for (AccessCatalog.GroupSpec spec : AccessCatalog.GROUPS) {
            PrivilegeGroup group = privilegeGroupRepository
                    .findByNameIgnoreCaseAndParentTenantPublicIdIsNullAndDeletedIsFalse(spec.name())
                    .orElseGet(() -> privilegeGroupRepository.save(PrivilegeGroup.builder()
                            .name(spec.name())
                            .parentTenantPublicId(null)
                            .build()));
            for (String privilegeName : spec.privileges()) {
                Privilege privilege = privilegeRepository.findByNameIgnoreCaseAndDeletedIsFalse(privilegeName)
                        .orElseGet(() -> Privilege.builder().name(privilegeName).build());
                privilege.setGroup(group);
                privilegeRepository.save(privilege);
            }
        }
        log.info("Seeded global privilege catalog");
    }

    @Override
    @Transactional
    public void seedTenantRoles(UUID tenantPublicId, AuthUser owner) {
        if (tenantPublicId == null) {
            return;
        }
        seedGlobalCatalog();
        Map<String, PrivilegeGroup> groupsByName = AccessCatalog.groupNames().stream()
                .map(name -> privilegeGroupRepository
                        .findByNameIgnoreCaseAndParentTenantPublicIdIsNullAndDeletedIsFalse(name)
                        .orElseThrow(() -> new IllegalStateException("Missing privilege group " + name)))
                .collect(Collectors.toMap(PrivilegeGroup::getName, Function.identity(), (left, right) -> left));

        Role ownerRole = null;
        for (AccessCatalog.RoleSpec spec : AccessCatalog.ROLES) {
            Role role = roleRepository
                    .findByNameIgnoreCaseAndParentTenantPublicIdAndDeletedIsFalse(spec.name(), tenantPublicId)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name(spec.name())
                            .description(spec.description())
                            .parentTenantPublicId(tenantPublicId)
                            .build()));
            for (String groupName : spec.groupNames()) {
                PrivilegeGroup group = groupsByName.get(groupName);
                if (group == null) {
                    continue;
                }
                role.addPrivilegeGroup(group);
                AccessCatalog.GROUPS.stream()
                        .filter(groupSpec -> groupSpec.name().equals(groupName))
                        .findFirst()
                        .ifPresent(groupSpec -> groupSpec.privileges().forEach(privilegeName ->
                                privilegeRepository.findByNameIgnoreCaseAndDeletedIsFalse(privilegeName)
                                        .ifPresent(role::addPrivilege)));
            }
            roleRepository.save(role);
            if (AccessCatalog.OWNER_ROLE.equalsIgnoreCase(spec.name())) {
                ownerRole = role;
            }
        }

        if (owner != null && ownerRole != null) {
            Role assignedOwnerRole = ownerRole;
            boolean alreadyAssigned = owner.getRoles() != null && owner.getRoles().stream()
                    .anyMatch(role -> assignedOwnerRole.getId().equals(role.getId()));
            if (!alreadyAssigned) {
                owner.getRoles().add(assignedOwnerRole);
                authUserRepository.save(owner);
            }
        }
    }
}
