package com.itibari.auth.service.impl;

import com.itibari.auth.exception.BadRequestException;
import com.itibari.auth.exception.ResourceNotFoundException;
import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.Institution;
import com.itibari.auth.model.ShopSubscriptionDetail;
import com.itibari.auth.model.Subscription;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.repository.InstitutionRepository;
import com.itibari.auth.repository.ShopSubscriptionDetailRepository;
import com.itibari.auth.repository.SubscriptionRepository;
import com.itibari.auth.service.SubscriptionAccessService;
import com.itibari.auth.util.date.FormatDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionAccessServiceImpl implements SubscriptionAccessService {

    private final AuthUserRepository authUserRepository;
    private final InstitutionRepository institutionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ShopSubscriptionDetailRepository detailRepository;

    @Override
    @Transactional
    public ShopSubscriptionDetail startTrial(UUID userId, UUID subscriptionId) {
        AuthUser user = authUserRepository.findByPublicIdAndDeletedIsFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        Subscription plan = subscriptionRepository.findByPublicIdAndDeletedIsFalse(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription"));

        int trialDays = plan.getTrialDays() == 0 ? plan.getDays() : plan.getTrialDays();
        if (!plan.isActive() || trialDays <= 0) {
            throw new BadRequestException("This plan has no available trial");
        }
        if (detailRepository.existsBySubscribedByAndFreeTrialTrue(user)) {
            throw new BadRequestException("Free trial already used");
        }
        if (hasActiveSubscription(userId)) {
            throw new BadRequestException("An active subscription already exists");
        }
        return grant(user, plan, user.getInstitution(), trialDays, true);
    }

    @Override
    @Transactional
    public ShopSubscriptionDetail grant(AuthUser user, Subscription plan, Institution institution,
                                        int days, boolean trial) {
        if (institution != null) {
            institution = institutionRepository.findByPublicIdAndDeletedIsFalse(institution.getPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shop"));
            if (!institution.isParentTenant() || institution.isDeleted()) {
                throw new BadRequestException("Select a parent shop before subscribing");
            }
        }

        LocalDateTime start = FormatDate.currentNairobiDateTime();
        ShopSubscriptionDetail previous = institution == null
                ? detailRepository.findTopBySubscribedByOrderByCreationTimeDesc(user).orElse(null)
                : institution.getShopSubscriptionDetail();

        if (!trial && previous != null && !previous.isDeleted() && !previous.isFreeTrial()
                && previous.getSubscription() != null
                && previous.getSubscription().getId().equals(plan.getId())
                && previous.getDueDate() != null
                && previous.getDueDate().isAfter(start)) {
            start = previous.getDueDate();
        }

        ShopSubscriptionDetail detail = ShopSubscriptionDetail.builder()
                .subscribedBy(user)
                .subscription(plan)
                .institution(institution)
                .subscribedDate(start)
                .days(days)
                .freeTrial(trial)
                .endDate(start.plusDays(days))
                .dueDate(start.plusDays(days))
                .build();
        detailRepository.save(detail);

        if (institution != null) {
            institution.setShopSubscriptionDetail(detail);
            institutionRepository.save(institution);
        }
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(UUID userPublicId) {
        return detailRepository.hasActiveSubscriptionForUser(
                userPublicId, FormatDate.currentNairobiDateTime());
    }
}
