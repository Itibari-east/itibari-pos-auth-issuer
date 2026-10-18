package com.itibari.auth.client.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID publicId,
        NotificationChannel channel,
        String status,
        String recipient,
        String recipientName,
        String subject,
        String body,
        String templateName,
        String provider,
        String providerMessageId,
        String errorMessage,
        String sourceService,
        String correlationId,
        UUID parentTenantPublicId,
        LocalDateTime creationTime,
        LocalDateTime sentAt
) {
}
