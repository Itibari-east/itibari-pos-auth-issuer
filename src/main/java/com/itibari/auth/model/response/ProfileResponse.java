package com.itibari.auth.model.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ProfileResponse(
        UUID publicId,
        String name,
        String identityNumber,
        boolean mainTenant,
        boolean parentTenant,
        ParentProfile parentProfile,
        List<ChildProfile> childProfileList,
        UUID parentTenantPublicId,
        String country,
        String traPin,
        String timeZone,
        String baseCurrency,
        NotificationPreference notificationPreference,
        MarketingPreference marketingPreference,
        String subscriptionName,
        String activeStatus,
        String city
) {
    @Builder
    public record ParentProfile(UUID publicId, String name) {
    }

    @Builder
    public record ChildProfile(UUID publicId, String name) {
    }

    public record NotificationPreference(boolean sms, boolean email, boolean pushNotification) {
    }

    public record MarketingPreference(boolean sms, boolean email, boolean pushNotification) {
    }
}
