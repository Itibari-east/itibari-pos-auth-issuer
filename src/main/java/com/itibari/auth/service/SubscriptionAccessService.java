package com.itibari.auth.service;

import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Institution;
import com.itibari.auth.model.ShopSubscriptionDetail;
import com.itibari.auth.model.Subscription;

import java.util.UUID;

public interface SubscriptionAccessService {

    ShopSubscriptionDetail startTrial(UUID userId, UUID subscriptionId);

    /** Used by free trial today; later also by finance after successful payment. */
    ShopSubscriptionDetail grant(AuthUser user, Subscription plan, Institution institution, int days, boolean trial);

    boolean hasActiveSubscription(UUID userPublicId);
}
