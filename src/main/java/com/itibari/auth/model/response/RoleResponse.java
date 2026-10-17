package com.itibari.auth.model.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RoleResponse(UUID publicId, String name) {
}
