package com.itibari.auth.repository;

import com.itibari.auth.dataTypes.SubscriptionType;
import com.itibari.auth.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<Subscription> findByNameAndDeletedIsFalse(SubscriptionType name);

    @Query("""
            select s from Subscription s
            where s.deleted = false
              and (
                   :searchParam is null
                or :searchParam = ''
                or lower(s.description) like lower(concat('%', :searchParam, '%'))
              )
            order by s.creationTime desc
            """)
    Page<Subscription> search(@Param("searchParam") String searchParam, Pageable pageable);
}
