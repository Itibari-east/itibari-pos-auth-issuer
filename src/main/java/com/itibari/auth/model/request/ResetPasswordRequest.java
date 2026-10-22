package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "resetToken is required")
        String resetToken,
        @NotBlank(message = "newPassword is required")
        String newPassword,
        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}
