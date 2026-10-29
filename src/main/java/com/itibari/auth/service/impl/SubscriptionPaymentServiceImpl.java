package com.itibari.auth.service.impl;
import com.itibari.auth.client.licensing.LicensingFeignClient;
import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import com.itibari.auth.model.response.SubscriptionPaymentResponse;
import com.itibari.auth.service.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;
/** Compatibility adapter; licensing derives the owner from the forwarded JWT. */
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentServiceImpl implements SubscriptionPaymentService {
    private final LicensingFeignClient licensing;
    public SubscriptionPaymentResponse initiate(UUID userId, SubscriptionPaymentRequest request) {
        return licensing.initiate(request);
    }
    public SubscriptionPaymentResponse reconcile(UUID userId, UUID paymentId) {
        return licensing.reconcile(paymentId);
    }
}
