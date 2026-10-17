package com.itibari.auth.model.response;

import java.util.List;

public record NotificationPreferenceResponse(
        List<Definition> subscriptionPaymentList,
        List<Definition> marketingList
) {
    public record Definition(boolean email, boolean sms, boolean pushNotification) {
    }
}
