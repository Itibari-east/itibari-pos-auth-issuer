package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateRoleDTO(
        @NotEmpty(message = "name is required") String name,
        String description,
        UUID parentTenantPublicId
) {
}
