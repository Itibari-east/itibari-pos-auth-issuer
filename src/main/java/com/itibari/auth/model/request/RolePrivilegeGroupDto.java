package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RolePrivilegeGroupDto(
        @NotEmpty(message = "privilege group public ids are required")
        List<@NotNull(message = "privilege group public id must not be null") UUID> privilegeGroupPublicIds
) {
}
