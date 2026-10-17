package com.itibari.auth.repository;

import com.itibari.auth.model.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment> findByUser_PublicIdAndIdempotencyKey(UUID userId, UUID key);

    Optional<SubscriptionPayment> findByPublicIdAndUser_PublicId(UUID publicId, UUID userId);

    boolean existsByUser_PublicIdAndStatus(UUID userId, String status);
}
