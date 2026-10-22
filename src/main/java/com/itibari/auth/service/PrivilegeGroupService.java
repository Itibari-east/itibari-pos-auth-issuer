package com.itibari.auth.service;

import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.request.PrivilegeGroupDto;
import com.itibari.auth.model.request.PrivilegeGroupPrivilegeDto;
import com.itibari.auth.model.response.PrivilegeGroupResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PrivilegeGroupService {

    PrivilegeGroup createPrivilegeGroup(@Valid PrivilegeGroupDto privilegeGroupDto, UUID loggedInUserPublicId);

    Page<PrivilegeGroupResponse> getPrivilegeGroups(String searchParam, UUID parentTenantPublicId, Pageable pageable);

    Page<PrivilegeGroupResponse> getTenantPrivilegeGroups(UUID userPublicId, String searchParam, Pageable pageable);

    String assignPrivileges(UUID groupPublicId, @Valid PrivilegeGroupPrivilegeDto request);
}
