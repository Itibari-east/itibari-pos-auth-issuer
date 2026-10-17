package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UserRoleDto(
        @NotEmpty(message = "role public ids are required")
        List<@NotNull(message = "role public id must not be null") UUID> rolePublicIds
) {
}
