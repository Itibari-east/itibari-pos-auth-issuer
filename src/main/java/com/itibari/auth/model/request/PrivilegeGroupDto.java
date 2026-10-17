package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.UUID;

public record PrivilegeGroupDto(
        @NotEmpty(message = "name is required") String name,
        UUID parentTenantPublicId
) {
}
