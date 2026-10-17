package com.itibari.auth.model.response;

import java.util.UUID;

public record Custom2Response(
        String message,
        String identity,
        UUID publicId,
        String accessToken,
        boolean subscription
) {
}
