package com.itibari.auth.service;

import com.itibari.auth.client.licensing.LicensingFeignClient;
import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import com.itibari.auth.service.impl.SubscriptionPaymentServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SubscriptionPaymentServiceTest {

    @Test
    void paymentPhoneMustStayTanzaniaMobilePattern() {
        UUID planId = UUID.randomUUID();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(new SubscriptionPaymentRequest(planId, UUID.randomUUID(), "+254712345678")))
                .isNotEmpty();
        assertThat(validator.validate(new SubscriptionPaymentRequest(planId, UUID.randomUUID(), "255712345678")))
                .isEmpty();
    }

    @Test
    void delegatesPaymentAndVerificationToLicensing() {
        var client = mock(LicensingFeignClient.class);
        var service = new SubscriptionPaymentServiceImpl(client);
        var user = UUID.randomUUID();
        var payment = UUID.randomUUID();
        var request = new SubscriptionPaymentRequest(UUID.randomUUID(), UUID.randomUUID(), "255712345678");
        service.initiate(user, request);
        service.reconcile(user, payment);
        verify(client).initiate(request);
        verify(client).reconcile(payment);
        verifyNoMoreInteractions(client);
    }
}
