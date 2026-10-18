package com.itibari.auth.client.notification;

import java.util.List;
import java.util.UUID;

public record SendNotificationRequest(
        NotificationChannel channel,
        String recipient,
        String recipientName,
        String subject,
        String body,
        Boolean html,
        List<String> cc,
        String templateName,
        List<String> templateParams,
        String sourceService,
        String correlationId,
        UUID parentTenantPublicId
) {
}
