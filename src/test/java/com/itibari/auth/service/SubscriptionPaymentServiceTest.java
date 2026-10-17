package com.itibari.auth.service;

import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.ShopSubscriptionDetail;
import com.itibari.auth.model.Subscription;
import com.itibari.auth.model.SubscriptionPayment;
import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.repository.SubscriptionPaymentRepository;
import com.itibari.auth.repository.SubscriptionRepository;
import com.itibari.auth.service.impl.SubscriptionPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tz.co.vodampesa.VodaMpesaSdk;
import tz.co.vodampesa.exception.VodaMpesaException;
import tz.co.vodampesa.model.QueryStatusRequest;
import tz.co.vodampesa.model.VodaMpesaResponse;
import tz.co.vodampesa.model.VodaMpesaResult;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class SubscriptionPaymentServiceTest {
    private final AuthUserRepository users = mock(AuthUserRepository.class);
    private final SubscriptionRepository plans = mock(SubscriptionRepository.class);
    private final SubscriptionPaymentRepository payments = mock(SubscriptionPaymentRepository.class);
    private final SubscriptionAccessService access = mock(SubscriptionAccessService.class);
    private final VodaMpesaSdk sdk = mock(VodaMpesaSdk.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final UUID userId = UUID.randomUUID(), planId = UUID.randomUUID();
    private final SubscriptionPaymentRequest request = new SubscriptionPaymentRequest(planId, UUID.randomUUID(), "255712345678");
    private SubscriptionPaymentService service;
    private AuthUser user;
    private Subscription plan;
    private SubscriptionPayment saved;

    @BeforeEach
    void setup() {
        ObjectProvider<VodaMpesaSdk> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sdk);
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        service = new SubscriptionPaymentServiceImpl(users, plans, payments, access, provider, manager);
        user = new AuthUser();
        user.setPublicId(userId);
        plan = new Subscription();
        plan.setPublicId(planId);
        plan.setActive(true);
        plan.setPrice(BigDecimal.valueOf(1500));
        plan.setDays(45);
        when(users.lockByPublicId(userId)).thenReturn(Optional.of(user));
        when(plans.findByPublicIdAndDeletedIsFalse(planId)).thenReturn(Optional.of(plan));
        when(payments.saveAndFlush(any())).thenAnswer(i -> {
            saved = i.getArgument(0);
            saved.setPublicId(UUID.randomUUID());
            when(payments.findByPublicIdAndUser_PublicId(saved.getPublicId(), userId)).thenReturn(Optional.of(saved));
            return saved;
        });
        when(sdk.c2bPayment(any())).thenReturn(result("INS-0", "tx-123", null));
    }

    @Test
    void commitsPendingSnapshotBeforeDebitAndAcceptanceDoesNotGrantAccess() {
        var response = service.initiate(userId, request);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(saved.getAmount()).isEqualTo(1500);
        assertThat(saved.getDays()).isEqualTo(45);
        assertThat(saved.getTransactionId()).isEqualTo("tx-123");
        var order = inOrder(payments, manager, sdk);
        order.verify(payments).saveAndFlush(any());
        order.verify(manager).commit(any());
        order.verify(sdk).c2bPayment(argThat(r -> r.thirdPartyConversationId().equals(saved.getConversationId())));
        verifyNoInteractions(access);
    }

    @Test
    void sameKeyNeverSendsAnotherDebit() {
        service.initiate(userId, request);
        when(payments.findByUser_PublicIdAndIdempotencyKey(userId, request.idempotencyKey())).thenReturn(Optional.of(saved));
        service.initiate(userId, request);
        verify(sdk, times(1)).c2bPayment(any());
        assertThatThrownBy(() -> service.initiate(userId, new SubscriptionPaymentRequest(planId, request.idempotencyKey(), "255711111111")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void pendingPaymentBlocksNewKey() {
        when(payments.existsByUser_PublicIdAndStatus(userId, "PENDING")).thenReturn(true);
        assertThatThrownBy(() -> service.initiate(userId, request)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(sdk);
    }

    @Test
    void timeoutKeepsDurableConversationAndDoesNotGrantAccess() {
        when(sdk.c2bPayment(any())).thenThrow(new VodaMpesaException("timeout", null, null, null));
        assertThat(service.initiate(userId, request).status()).isEqualTo("PENDING");
        assertThat(saved.getConversationId()).isNotBlank();
        verifyNoInteractions(access);
    }

    @Test
    void completedOwnedTransactionGrantsOnceUsingPurchasedDays() {
        service.initiate(userId, request);
        plan.setDays(90);
        when(sdk.queryTransactionStatus("tx-123")).thenReturn(result("INS-0", "tx-123", "Completed"));
        when(access.grant(user, plan, null, 45, false)).thenReturn(new ShopSubscriptionDetail());
        assertThat(service.reconcile(userId, saved.getPublicId()).status()).isEqualTo("COMPLETED");
        service.reconcile(userId, saved.getPublicId());
        verify(access, times(1)).grant(user, plan, null, 45, false);
        verify(sdk, times(1)).queryTransactionStatus("tx-123");
    }

    @Test
    void mismatchedCompletedTransactionNeverGrantsAccess() {
        service.initiate(userId, request);
        when(sdk.queryTransactionStatus("tx-123")).thenReturn(result("INS-0", "other-transaction", "Completed"));
        assertThat(service.reconcile(userId, saved.getPublicId()).status()).isEqualTo("PENDING");
        verifyNoInteractions(access);
    }

    @Test
    void pendingAndRejectedQueriesNeverGrantAccess() {
        service.initiate(userId, request);
        when(sdk.queryTransactionStatus("tx-123")).thenReturn(result("INS-0", "tx-123", "Pending"), result("INS-9", "tx-123", "Completed"));
        service.reconcile(userId, saved.getPublicId());
        service.reconcile(userId, saved.getPublicId());
        verifyNoInteractions(access);
    }

    @Test
    void anotherUsersPaymentCannotBeQueried() {
        assertThatThrownBy(() -> service.reconcile(UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(sdk, access);
    }

    @Test
    void fractionalPriceIsRejectedBeforeDebit() {
        plan.setPrice(new BigDecimal("1500.50"));
        assertThatThrownBy(() -> service.initiate(userId, request)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(sdk);
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void timeoutCanBeReconciledOnlyWithMatchingConversation() {
        when(sdk.c2bPayment(any())).thenThrow(new VodaMpesaException("timeout", null, null, null));
        service.initiate(userId, request);
        when(sdk.queryTransactionStatus(any(QueryStatusRequest.class))).thenReturn(
                new VodaMpesaResult(new VodaMpesaResponse("INS-0", "ok", "recovered", null,
                        "unrelated", "Completed"), saved.getConversationId()),
                new VodaMpesaResult(new VodaMpesaResponse("INS-0", "ok", "recovered", null,
                        saved.getConversationId(), "Completed"), saved.getConversationId()));
        assertThat(service.reconcile(userId, saved.getPublicId()).status()).isEqualTo("PENDING");
        verifyNoInteractions(access);
        when(access.grant(user, plan, null, 45, false)).thenReturn(new ShopSubscriptionDetail());
        assertThat(service.reconcile(userId, saved.getPublicId()).status()).isEqualTo("COMPLETED");
        verify(sdk, times(2)).queryTransactionStatus(new QueryStatusRequest(saved.getConversationId(), saved.getConversationId()));
    }

    @Test
    void confirmedFailureAllowsANewAttemptWithoutGrantingAccess() {
        service.initiate(userId, request);
        when(sdk.queryTransactionStatus("tx-123")).thenReturn(result("INS-0", "tx-123", "Failed"));
        assertThat(service.reconcile(userId, saved.getPublicId()).status()).isEqualTo("FAILED");
        verifyNoInteractions(access);
    }

    private VodaMpesaResult result(String code, String transaction, String status) {
        return new VodaMpesaResult(new VodaMpesaResponse(code, "result", transaction, "conversation", null, status), "request");
    }
}
