package com.itibari.auth.service;

import java.util.UUID;

public interface NotificationClient {

    void sendEmail(String recipient,
                   String recipientName,
                   String subject,
                   String htmlBody,
                   UUID parentTenantPublicId,
                   String correlationId);
}
