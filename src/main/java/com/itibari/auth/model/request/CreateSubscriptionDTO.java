package com.itibari.auth.model.request;

import com.itibari.auth.dataTypes.SubscriptionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateSubscriptionDTO(
        @NotNull SubscriptionType name,
        String description,
        @NotNull @DecimalMin("0") BigDecimal price,
        @Min(0) Integer trialDays,
        @Min(1) Integer days
) {
}
