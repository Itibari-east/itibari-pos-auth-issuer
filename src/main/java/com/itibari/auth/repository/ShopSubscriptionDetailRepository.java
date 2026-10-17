package com.itibari.auth.repository;

import com.itibari.auth.dataTypes.SubscriptionType;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.ShopSubscriptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ShopSubscriptionDetailRepository extends JpaRepository<ShopSubscriptionDetail, Long> {

    boolean existsBySubscribedByAndFreeTrialTrue(AuthUser user);

    Optional<ShopSubscriptionDetail> findTopBySubscribedByOrderByCreationTimeDesc(AuthUser user);

    @Query("""
            select count(institution)
            from AuthUser user
            join user.institutions institution
            left join institution.shopSubscriptionDetail detail
            left join detail.subscription subscription
            left join institution.parent parentInstitution
            left join parentInstitution.shopSubscriptionDetail parentDetail
            left join parentDetail.subscription parentSubscription
            where user.publicId = :userPublicId
              and user.deleted = false
              and institution.deleted = false
              and (
                  (institution.parentTenant = true
                      and detail.deleted = false
                      and subscription.deleted = false
                      and detail.dueDate >= :now)
                  or
                  (parentInstitution.parentTenant = true
                      and parentInstitution.deleted = false
                      and parentDetail.deleted = false
                      and parentSubscription.deleted = false
                      and parentDetail.dueDate >= :now)
              )
            """)
    long countActiveSubscriptionsForUser(@Param("userPublicId") UUID userPublicId,
                                         @Param("now") LocalDateTime now);

    @Query("""
            select count(detail)
            from ShopSubscriptionDetail detail
            join detail.subscribedBy user
            join detail.subscription subscription
            where user.publicId = :userPublicId
              and user.deleted = false
              and detail.deleted = false
              and subscription.deleted = false
              and detail.dueDate >= :now
            """)
    long countActiveDirectSubscriptionsForUser(@Param("userPublicId") UUID userPublicId,
                                               @Param("now") LocalDateTime now);

    default boolean hasActiveSubscriptionForUser(UUID userPublicId, LocalDateTime now) {
        return countActiveSubscriptionsForUser(userPublicId, now) > 0
                || countActiveDirectSubscriptionsForUser(userPublicId, now) > 0;
    }

    @Query("""
            select count(detail)
            from Institution institution
            join institution.shopSubscriptionDetail detail
            join detail.subscription subscription
            where institution.publicId = :institutionPublicId
              and institution.deleted = false
              and detail.deleted = false
              and subscription.deleted = false
              and subscription.name = :subscriptionType
              and detail.dueDate >= :now
            """)
    long countActiveSubscriptionsForInstitution(@Param("institutionPublicId") UUID institutionPublicId,
                                                @Param("subscriptionType") SubscriptionType subscriptionType,
                                                @Param("now") LocalDateTime now);

    default boolean hasActiveEnterpriseSubscription(UUID institutionPublicId, LocalDateTime now) {
        return countActiveSubscriptionsForInstitution(institutionPublicId, SubscriptionType.Enterprise, now) > 0;
    }
}
