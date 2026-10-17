package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record PrivilegeDto(
        @NotEmpty(message = "name is required") String name,
        UUID privilegeGroupPublicId
) {
}
