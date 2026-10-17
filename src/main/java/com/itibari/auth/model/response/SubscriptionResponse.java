package com.itibari.auth.model.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record SubscriptionResponse(
        UUID publicId,
        String name,
        String description,
        BigDecimal price,
        int trialDays,
        int days
) {
}
