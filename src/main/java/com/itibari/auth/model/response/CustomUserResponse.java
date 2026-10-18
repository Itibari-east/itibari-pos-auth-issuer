package com.itibari.auth.model.response;

import java.util.UUID;

public record CustomUserResponse(String message, String identity, UUID publicId) {
}
