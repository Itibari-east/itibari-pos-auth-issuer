package com.itibari.auth.web;

import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import com.itibari.auth.model.response.SubscriptionPaymentResponse;
import com.itibari.auth.service.SubscriptionPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/subscription/payments")
@Tag(name = "subscription-payments")
@RequiredArgsConstructor
public class SubscriptionPaymentController {

    private final SubscriptionPaymentService payments;

    @PostMapping("/mpesa")
    @Operation(summary = "Initiate an M-Pesa C2B payment for a subscription plan")
    public SubscriptionPaymentResponse initiate(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody SubscriptionPaymentRequest request) {
        return payments.initiate(UUID.fromString(jwt.getClaimAsString("public_id")), request);
    }

    @PostMapping("/{paymentId}/reconcile")
    @Operation(summary = "Reconcile a pending subscription payment and grant access on success")
    public SubscriptionPaymentResponse reconcile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return payments.reconcile(UUID.fromString(jwt.getClaimAsString("public_id")), paymentId);
    }
}
