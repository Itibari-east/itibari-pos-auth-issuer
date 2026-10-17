package com.itibari.auth.web;

import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.request.PrivilegeDto;
import com.itibari.auth.model.response.CustomResponse;
import com.itibari.auth.model.response.PageResponse;
import com.itibari.auth.model.response.PrivilegeResponse;
import com.itibari.auth.service.PrivilegeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "privileges")
@RestController
@RequestMapping("/privileges")
@RequiredArgsConstructor
public class PrivilegeRestController {

    private final PrivilegeService privilegeService;

    @PostMapping
    @Operation(summary = "Create privilege")
    public ResponseEntity<CustomResponse> createPrivilege(@Valid @RequestBody PrivilegeDto privilegeDto) {
        Privilege privilege = privilegeService.createPrivilege(privilegeDto);
        return ResponseEntity.ok(new CustomResponse("success", privilege.getName()));
    }

    @GetMapping
    @Operation(summary = "List privileges")
    public PageResponse<PrivilegeResponse> getAllPrivileges(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(privilegeService.fetchAllPrivileges(searchParam, pageable));
    }
}
