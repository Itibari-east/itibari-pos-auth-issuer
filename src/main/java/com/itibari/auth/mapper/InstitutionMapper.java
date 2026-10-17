package com.itibari.auth.mapper;

import com.itibari.auth.model.Institution;
import com.itibari.auth.model.NotificationPreference;
import com.itibari.auth.model.response.ChildShopResponse;
import com.itibari.auth.model.response.ProfileResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class InstitutionMapper {

    public ProfileResponse toProfile(Institution institution) {
        if (institution == null) {
            return null;
        }
        Institution parent = institution.getParent();
        return ProfileResponse.builder()
                .publicId(institution.getPublicId())
                .name(institution.getName())
                .identityNumber(institution.getIdentityNumber())
                .mainTenant(institution.isMainTenant())
                .parentTenant(institution.isParentTenant())
                .parentProfile(parent == null ? null
                        : new ProfileResponse.ParentProfile(parent.getPublicId(), parent.getName()))
                .childProfileList(mapChildren(institution.getChildren()))
                .parentTenantPublicId(parent == null ? null : parent.getPublicId())
                .country(institution.getCountry())
                .traPin(institution.getTraPin())
                .timeZone(institution.getTimeZone())
                .baseCurrency(institution.getBaseCurrency())
                .notificationPreference(mapNotification(institution.getSubscriptionPreference()))
                .marketingPreference(mapMarketing(institution.getMarketingPreference()))
                .subscriptionName(subscriptionName(institution))
                .activeStatus(institution.isActive() ? "Active" : "In Active")
                .city(institution.getCity())
                .build();
    }

    public ChildShopResponse toChildShop(Institution institution) {
        Institution parent = institution.getParent();
        return new ChildShopResponse(
                institution.getPublicId(),
                institution.getName(),
                institution.getIdentityNumber(),
                parent == null ? null : new ProfileResponse.ParentProfile(parent.getPublicId(), parent.getName()),
                parent == null ? null : parent.getPublicId(),
                institution.getCountry(),
                institution.getTraPin(),
                institution.getTimeZone(),
                institution.getBaseCurrency(),
                mapNotification(institution.getSubscriptionPreference()),
                mapMarketing(institution.getMarketingPreference()),
                subscriptionName(institution),
                institution.isActive() ? "Active" : "In Active",
                institution.getCity()
        );
    }

    private String subscriptionName(Institution institution) {
        if (institution.getShopSubscriptionDetail() != null
                && institution.getShopSubscriptionDetail().getSubscription() != null
                && institution.getShopSubscriptionDetail().getSubscription().getName() != null) {
            return institution.getShopSubscriptionDetail().getSubscription().getName().getLabel();
        }
        if (institution.getParent() != null
                && institution.getParent().getShopSubscriptionDetail() != null
                && institution.getParent().getShopSubscriptionDetail().getSubscription() != null
                && institution.getParent().getShopSubscriptionDetail().getSubscription().getName() != null) {
            return institution.getParent().getShopSubscriptionDetail().getSubscription().getName().getLabel();
        }
        return null;
    }

    private List<ProfileResponse.ChildProfile> mapChildren(List<Institution> children) {
        if (children == null) {
            return List.of();
        }
        return children.stream()
                .filter(Objects::nonNull)
                .filter(child -> !child.isDeleted())
                .map(child -> new ProfileResponse.ChildProfile(child.getPublicId(), child.getName()))
                .toList();
    }

    private ProfileResponse.NotificationPreference mapNotification(NotificationPreference preference) {
        if (preference == null) {
            return null;
        }
        return new ProfileResponse.NotificationPreference(
                preference.isSms(), preference.isEmail(), preference.isPushNotification());
    }

    private ProfileResponse.MarketingPreference mapMarketing(NotificationPreference preference) {
        if (preference == null) {
            return null;
        }
        return new ProfileResponse.MarketingPreference(
                preference.isSms(), preference.isEmail(), preference.isPushNotification());
    }
}
