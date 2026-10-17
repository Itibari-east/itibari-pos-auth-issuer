package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record SubscriptionPaymentRequest(
        @NotNull UUID subscriptionId,
        @NotNull UUID idempotencyKey,
        @NotBlank @Pattern(regexp = "255[0-9]{9}|000000000001") String phone) {
}
