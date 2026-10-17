package com.itibari.auth.service.impl;

import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.request.PrivilegeGroupDto;
import com.itibari.auth.model.request.PrivilegeGroupPrivilegeDto;
import com.itibari.auth.model.response.PrivilegeGroupResponse;
import com.itibari.auth.repository.PrivilegeGroupRepository;
import com.itibari.auth.repository.PrivilegeRepository;
import com.itibari.auth.service.PrivilegeGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrivilegeGroupServiceImpl implements PrivilegeGroupService {

    private final PrivilegeGroupRepository privilegeGroupRepository;
    private final PrivilegeRepository privilegeRepository;

    @Override
    @Transactional
    public PrivilegeGroup createPrivilegeGroup(PrivilegeGroupDto privilegeGroupDto) {
        PrivilegeGroup group = PrivilegeGroup.builder()
                .name(privilegeGroupDto.name().trim())
                .parentTenantPublicId(privilegeGroupDto.parentTenantPublicId())
                .build();
        return privilegeGroupRepository.save(group);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrivilegeGroupResponse> getPrivilegeGroups(String searchParam, UUID parentTenantPublicId,
                                                           Pageable pageable) {
        return privilegeGroupRepository.listGroups(searchParam, parentTenantPublicId, pageable);
    }

    @Override
    @Transactional
    public String assignPrivileges(UUID groupPublicId, PrivilegeGroupPrivilegeDto request) {
        PrivilegeGroup group = privilegeGroupRepository.findByPublicIdAndDeletedIsFalse(groupPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Privilege group"));

        List<Privilege> privileges = request.privilegePublicIds().stream()
                .map(id -> privilegeRepository.findByPublicIdAndDeletedIsFalse(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Privilege")))
                .peek(privilege -> privilege.setGroup(group))
                .toList();

        privilegeRepository.saveAll(privileges);
        return privileges.stream().map(Privilege::getName).collect(Collectors.joining(", "));
    }
}
