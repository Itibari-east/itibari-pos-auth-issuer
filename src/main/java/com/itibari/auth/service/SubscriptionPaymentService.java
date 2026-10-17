package com.itibari.auth.service;

import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import com.itibari.auth.model.response.SubscriptionPaymentResponse;

import java.util.UUID;

public interface SubscriptionPaymentService {

    SubscriptionPaymentResponse initiate(UUID userId, SubscriptionPaymentRequest request);

    SubscriptionPaymentResponse reconcile(UUID userId, UUID paymentId);
}
