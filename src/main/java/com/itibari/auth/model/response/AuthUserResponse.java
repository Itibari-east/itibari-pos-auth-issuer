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
        boolean activeStatus,
        String passportUrl
) {
    public AuthUserResponse(UUID publicId, String firstName, String lastName, String phoneNumber, String email,
                            boolean activeStatus) {
        this(publicId, firstName, lastName, phoneNumber, email, activeStatus, null);
    }

    public AuthUserResponse withPassportUrl(String passportUrl) {
        return new AuthUserResponse(publicId, firstName, lastName, phoneNumber, email, activeStatus, passportUrl);
    }
}
