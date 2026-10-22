package com.itibari.auth.model.response;

public record ResetPasswordTokenResponse(String resetToken, String expiresAt) {
}
