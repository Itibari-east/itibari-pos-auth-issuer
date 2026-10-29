package com.itibari.auth.messaging;

import java.util.List;
import java.util.UUID;

/** JSON contract consumed by notification-service; no Java class headers are required. */
public record NotificationCommand(
        Channel channel, String recipient, String recipientName, String subject,
        String body, Boolean html, List<String> cc, String templateName,
        List<String> templateParams, String sourceService, String correlationId,
        UUID parentTenantPublicId) {
    public enum Channel { EMAIL, SMS, WHATSAPP }
}
