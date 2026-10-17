package com.itibari.auth.service.impl;

import com.itibari.auth.model.Subscription;
import com.itibari.auth.model.request.CreateSubscriptionDTO;
import com.itibari.auth.model.response.SubscriptionResponse;
import com.itibari.auth.repository.SubscriptionRepository;
import com.itibari.auth.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public Subscription createSubscription(CreateSubscriptionDTO dto) {
        Subscription subscription = subscriptionRepository.findByNameAndDeletedIsFalse(dto.name())
                .orElseGet(Subscription::new);
        subscription.setName(dto.name());
        subscription.setActive(true);
        subscription.setDescription(dto.description());
        subscription.setPrice(dto.price() != null ? dto.price() : BigDecimal.ZERO);
        if (dto.trialDays() != null) {
            subscription.setTrialDays(dto.trialDays());
        }
        if (dto.days() != null) {
            subscription.setDays(dto.days());
        }
        return subscriptionRepository.save(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptions(String searchParam, Pageable pageable) {
        return subscriptionRepository.search(searchParam, pageable).map(this::toResponse);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .publicId(subscription.getPublicId())
                .name(subscription.getName().getLabel())
                .description(subscription.getDescription())
                .price(subscription.getPrice())
                .trialDays(subscription.getTrialDays())
                .days(subscription.getDays())
                .build();
    }
}
