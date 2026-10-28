package com.itibari.auth.service;
import com.itibari.auth.client.licensing.LicensingFeignClient;
import com.itibari.auth.service.impl.SubscriptionPaymentServiceImpl;
import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.mockito.Mockito.*;
class SubscriptionPaymentServiceTest {
    @Test void delegatesPaymentAndVerificationToLicensing() {
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
