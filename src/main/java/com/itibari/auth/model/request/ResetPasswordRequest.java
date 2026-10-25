package com.itibari.auth.model.request;

import com.itibari.auth.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "resetToken is required")
        String resetToken,
        @NotBlank(message = "newPassword is required")
        @StrongPassword
        String newPassword,
        @NotBlank(message = "confirmPassword is required")
        String confirmPassword
) {
}
