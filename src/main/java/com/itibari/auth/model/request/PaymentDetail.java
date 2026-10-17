package com.itibari.auth.model.request;

import java.util.UUID;

public record PaymentDetail(UUID referenceId, UUID subscriptionId, Boolean freeTrial) {
    public PaymentDetail(UUID referenceId, UUID subscriptionId) {
        this(referenceId, subscriptionId, false);
    }
}
