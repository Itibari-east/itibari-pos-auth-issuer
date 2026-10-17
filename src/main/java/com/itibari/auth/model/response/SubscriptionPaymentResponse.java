package com.itibari.auth.model.response;

import com.itibari.auth.model.SubscriptionPayment;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionPaymentResponse(UUID publicId, String status, long amount, String currency,
                                          int days, String responseCode, LocalDateTime endDate) {

    public static SubscriptionPaymentResponse from(SubscriptionPayment payment) {
        return new SubscriptionPaymentResponse(
                payment.getPublicId(),
                payment.getStatus(),
                payment.getAmount(),
                "TZS",
                payment.getDays(),
                payment.getResponseCode(),
                payment.getDetail() == null ? null : payment.getDetail().getEndDate());
    }
}
