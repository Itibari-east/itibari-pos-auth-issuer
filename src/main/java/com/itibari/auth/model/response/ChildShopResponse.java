package com.itibari.auth.model.response;

import java.util.UUID;

public record ChildShopResponse(
        UUID publicId,
        String name,
        String identityNumber,
        ProfileResponse.ParentProfile parentProfile,
        UUID parentTenantPublicId,
        String country,
        String traPin,
        String timeZone,
        String baseCurrency,
        ProfileResponse.NotificationPreference notificationPreference,
        ProfileResponse.MarketingPreference marketingPreference,
        String subscriptionName,
        String activeStatus,
        String city
) {
}
