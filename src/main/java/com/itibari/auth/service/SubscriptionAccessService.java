package com.itibari.auth.service;
import java.util.UUID;
/** Read-only view used when issuing tokens. Licensing owns subscription mutations. */
public interface SubscriptionAccessService {
    boolean hasActiveSubscription(UUID userPublicId);
}
