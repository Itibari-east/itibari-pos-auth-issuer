package com.itibari.auth.service.impl;

import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.request.PrivilegeDto;
import com.itibari.auth.model.response.PrivilegeResponse;
import com.itibari.auth.repository.PrivilegeGroupRepository;
import com.itibari.auth.repository.PrivilegeRepository;
import com.itibari.auth.service.PrivilegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivilegeServiceImpl implements PrivilegeService {

    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeGroupRepository privilegeGroupRepository;

    @Override
    @Transactional
    public Privilege createPrivilege(PrivilegeDto privilegeDto) {
        Privilege privilege = privilegeRepository.findByNameIgnoreCaseAndDeletedIsFalse(privilegeDto.name())
                .orElseGet(() -> Privilege.builder().name(privilegeDto.name().trim()).build());
        privilege.setGroup(resolveGroup(privilegeDto.privilegeGroupPublicId()));
        return privilegeRepository.save(privilege);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PrivilegeResponse> fetchAllPrivileges(String searchParam, Pageable pageable) {
        return privilegeRepository.listPrivileges(searchParam, pageable);
    }

    private PrivilegeGroup resolveGroup(java.util.UUID groupPublicId) {
        if (groupPublicId == null) {
            return null;
        }
        return privilegeGroupRepository.findByPublicIdAndDeletedIsFalse(groupPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Privilege group"));
    }
}
