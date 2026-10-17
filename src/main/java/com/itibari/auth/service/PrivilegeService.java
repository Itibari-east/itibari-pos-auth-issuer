package com.itibari.auth.service;

import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.request.PrivilegeDto;
import com.itibari.auth.model.response.PrivilegeResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PrivilegeService {

    Privilege createPrivilege(@Valid PrivilegeDto privilegeDto);

    Page<PrivilegeResponse> fetchAllPrivileges(String searchParam, Pageable pageable);
}
