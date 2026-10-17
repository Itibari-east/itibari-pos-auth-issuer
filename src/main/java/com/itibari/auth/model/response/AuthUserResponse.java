package com.itibari.auth.model.response;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthUserResponse(
        UUID publicId,
        String firstName,
        String lastName,
        String phoneNumber,
        String email,
        boolean activeStatus
) {
}
