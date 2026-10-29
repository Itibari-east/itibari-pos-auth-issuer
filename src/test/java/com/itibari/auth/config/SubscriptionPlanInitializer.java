package com.itibari.auth.config;

import com.itibari.auth.dataTypes.SubscriptionType;
import com.itibari.auth.model.Subscription;
import com.itibari.auth.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class SubscriptionPlanInitializer implements ApplicationRunner {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(SubscriptionType.Free_Trial, "Default free trial plan", BigDecimal.ZERO, 7, 7);
        seed(SubscriptionType.Single_Shop, "Single shop plan", new BigDecimal("50000.00"), 7, 30);
        seed(SubscriptionType.Enterprise, "Enterprise multi-shop plan", new BigDecimal("150000.00"), 7, 30);
    }

    private void seed(SubscriptionType type, String description, BigDecimal price, int trialDays, int days) {
        if (subscriptionRepository.findByNameAndDeletedIsFalse(type).isPresent()) {
            return;
        }
        subscriptionRepository.save(Subscription.builder()
                .name(type)
                .description(description)
                .price(price)
                .trialDays(trialDays)
                .days(days)
                .active(true)
                .build());
        log.info("Seeded subscription plan {}", type);
    }
}
