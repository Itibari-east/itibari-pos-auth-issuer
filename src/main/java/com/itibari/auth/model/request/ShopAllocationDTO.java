package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record ShopAllocationDTO(
        @NotEmpty(message = "shop public ids are required")
        Set<UUID> shopPublicIds
) {
}
