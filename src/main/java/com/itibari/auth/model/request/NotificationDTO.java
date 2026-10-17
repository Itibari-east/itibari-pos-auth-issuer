package com.itibari.auth.model.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NotificationDTO(
        List<Definition> subscriptionPaymentList,
        List<Definition> marketingList
) {
    public record Definition(
            @NotNull(message = "email preference is required") Boolean email,
            @NotNull(message = "sms preference is required") Boolean sms,
            @NotNull(message = "push notification preference is required") Boolean pushNotification
    ) {
    }
}
