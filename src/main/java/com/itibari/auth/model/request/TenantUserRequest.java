package com.itibari.auth.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record TenantUserRequest(
        @NotBlank(message = "first name is required")
        @Size(max = 255, message = "first name must be at most 255 characters")
        String firstName,
        @NotBlank(message = "last name is required")
        @Size(max = 255, message = "last name must be at most 255 characters")
        String lastName,
        @NotNull(message = "role assignments are required") List<@NotNull UUID> role,
        @NotNull(message = "branch assignments are required") List<@NotNull UUID> branch,
        @NotBlank(message = "phone is required") String phone,
        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email) {
}
