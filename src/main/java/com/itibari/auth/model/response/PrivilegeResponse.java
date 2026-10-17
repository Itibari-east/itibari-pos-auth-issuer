package com.itibari.auth.model.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record PrivilegeResponse(UUID publicId, String name) {
}
