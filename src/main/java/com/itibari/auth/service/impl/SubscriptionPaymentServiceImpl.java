package com.itibari.auth.service.impl;

import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Subscription;
import com.itibari.auth.model.SubscriptionPayment;
import com.itibari.auth.model.request.SubscriptionPaymentRequest;
import com.itibari.auth.model.response.SubscriptionPaymentResponse;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.repository.SubscriptionPaymentRepository;
import com.itibari.auth.repository.SubscriptionRepository;
import com.itibari.auth.service.SubscriptionAccessService;
import com.itibari.auth.service.SubscriptionPaymentService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tz.co.vodampesa.VodaMpesaSdk;
import tz.co.vodampesa.exception.VodaMpesaException;
import tz.co.vodampesa.model.C2bPaymentRequest;
import tz.co.vodampesa.model.QueryStatusRequest;
import tz.co.vodampesa.model.VodaMpesaResult;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SubscriptionPaymentServiceImpl implements SubscriptionPaymentService {

    private final AuthUserRepository authUserRepository;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionPaymentRepository payments;
    private final SubscriptionAccessService access;
    private final ObjectProvider<VodaMpesaSdk> sdkProvider;
    private final TransactionTemplate tx;

    public SubscriptionPaymentServiceImpl(AuthUserRepository authUserRepository,
                                          SubscriptionRepository subscriptions,
                                          SubscriptionPaymentRepository payments,
                                          SubscriptionAccessService access,
                                          ObjectProvider<VodaMpesaSdk> sdkProvider,
                                          PlatformTransactionManager manager) {
        this.authUserRepository = authUserRepository;
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.access = access;
        this.sdkProvider = sdkProvider;
        this.tx = new TransactionTemplate(manager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public SubscriptionPaymentResponse initiate(UUID userId, SubscriptionPaymentRequest request) {
        VodaMpesaSdk sdk = sdk();
        boolean[] created = {false};
        SubscriptionPayment payment = tx.execute(status -> {
            AuthUser user = authUserRepository.lockByPublicId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            var existing = payments.findByUser_PublicIdAndIdempotencyKey(userId, request.idempotencyKey());
            if (existing.isPresent()) {
                SubscriptionPayment p = existing.get();
                if (!p.getSubscription().getPublicId().equals(request.subscriptionId())
                        || !p.getPhone().equals(request.phone())) {
                    throw invalid("Idempotency key already used for a different request");
                }
                SubscriptionPaymentResponse.from(p);
                return p;
            }
            if (payments.existsByUser_PublicIdAndStatus(userId, "PENDING")) {
                throw invalid("Reconcile your pending payment before submitting another payment");
            }
            Subscription plan = subscriptions.findByPublicIdAndDeletedIsFalse(request.subscriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription"));
            if (!plan.isActive() || plan.getDays() <= 0) {
                throw invalid("Subscription is unavailable");
            }
            long amount;
            try {
                amount = plan.getPrice().longValueExact();
            } catch (ArithmeticException | NullPointerException e) {
                throw invalid("M-Pesa requires a whole TZS price");
            }
            if (amount <= 0) {
                throw invalid("M-Pesa requires a positive price");
            }
            if (plan.getPrice().compareTo(BigDecimal.valueOf(amount)) != 0) {
                throw invalid("Payment amount must match the subscription price");
            }
            if (user.getInstitution() != null
                    && (!user.getInstitution().isParentTenant() || user.getInstitution().isDeleted())) {
                throw invalid("Select a parent shop before subscribing");
            }
            SubscriptionPayment p = new SubscriptionPayment();
            p.setUser(user);
            p.setSubscription(plan);
            p.setInstitution(user.getInstitution());
            p.setIdempotencyKey(request.idempotencyKey());
            p.setConversationId(VodaMpesaSdk.newConversationId());
            p.setReference(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            p.setPhone(request.phone());
            p.setAmount(amount);
            p.setDays(plan.getDays());
            created[0] = true;
            return payments.saveAndFlush(p);
        });
        if (!created[0]) {
            return SubscriptionPaymentResponse.from(payment);
        }
        try {
            VodaMpesaResult result = sdk.c2bPayment(C2bPaymentRequest.builder()
                    .amount(Long.toString(payment.getAmount()))
                    .customerMsisdn(payment.getPhone())
                    .transactionReference(payment.getReference())
                    .thirdPartyConversationId(payment.getConversationId())
                    .purchasedItemsDesc("Subscription payment")
                    .build());
            return tx.execute(status -> {
                authUserRepository.lockByPublicId(userId).orElseThrow();
                SubscriptionPayment p = owned(userId, payment.getPublicId());
                if (!"COMPLETED".equals(p.getStatus())) {
                    p.setResponseCode(result.getResponseCode());
                    p.setTransactionId(result.getTransactionId().orElse(null));
                    p.setProviderConversationId(result.getConversationId().orElse(null));
                }
                return SubscriptionPaymentResponse.from(p);
            });
        } catch (VodaMpesaException e) {
            return SubscriptionPaymentResponse.from(payment);
        }
    }

    @Override
    public SubscriptionPaymentResponse reconcile(UUID userId, UUID paymentId) {
        SubscriptionPayment saved = tx.execute(status -> {
            SubscriptionPayment p = owned(userId, paymentId);
            SubscriptionPaymentResponse.from(p);
            return p;
        });
        if (!"PENDING".equals(saved.getStatus())) {
            return SubscriptionPaymentResponse.from(saved);
        }
        VodaMpesaResult result;
        try {
            result = saved.getTransactionId() == null
                    ? sdk().queryTransactionStatus(new QueryStatusRequest(
                    saved.getConversationId(), saved.getConversationId()))
                    : sdk().queryTransactionStatus(saved.getTransactionId());
        } catch (VodaMpesaException e) {
            return SubscriptionPaymentResponse.from(saved);
        }
        return tx.execute(status -> {
            AuthUser user = authUserRepository.lockByPublicId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User"));
            SubscriptionPayment p = owned(userId, paymentId);
            if ("COMPLETED".equals(p.getStatus())) {
                return SubscriptionPaymentResponse.from(p);
            }
            p.setResponseCode(result.getResponseCode());
            String transactionId = result.getTransactionId().orElse(null);
            boolean matches = p.getTransactionId() != null
                    ? p.getTransactionId().equals(transactionId)
                    : result.getThirdPartyConversationId().filter(p.getConversationId()::equals).isPresent();
            if (result.isCompleted() && transactionId != null && matches) {
                p.setTransactionId(transactionId);
                p.setDetail(access.grant(user, p.getSubscription(), p.getInstitution(), p.getDays(), false));
                p.setStatus("COMPLETED");
            } else if (result.isAccepted() && matches
                    && result.getTransactionStatus().filter("Failed"::equalsIgnoreCase).isPresent()) {
                p.setStatus("FAILED");
            }
            return SubscriptionPaymentResponse.from(p);
        });
    }

    private SubscriptionPayment owned(UUID userId, UUID paymentId) {
        return payments.findByPublicIdAndUser_PublicId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment"));
    }

    private VodaMpesaSdk sdk() {
        VodaMpesaSdk sdk = sdkProvider.getIfAvailable();
        if (sdk == null) {
            throw invalid("M-Pesa payments are not enabled");
        }
        return sdk;
    }

    private static BadRequestException invalid(String message) {
        return new BadRequestException(message);
    }
}
