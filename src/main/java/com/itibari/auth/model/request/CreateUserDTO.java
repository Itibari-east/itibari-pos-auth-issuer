package com.itibari.auth.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record CreateUserDTO(
        @NotEmpty(message = "first name is required") String firstName,
        @NotEmpty(message = "last name is required") String lastName,
        @NotEmpty(message = "contact email is required")
        @Email(message = "contact email must be a valid email address") String contactEmail,
        @NotEmpty(message = "contact phone number is required") String contactPhoneNumber,
        @NotEmpty(message = "password is required") String password,
        @NotEmpty(message = "confirm password is required") String confirmPassword
) {
}
