package com.itibari.auth.service.impl;

import com.itibari.auth.client.notification.NotificationChannel;
import com.itibari.auth.client.notification.NotificationFeignClient;
import com.itibari.auth.client.notification.SendNotificationRequest;
import com.itibari.auth.service.NotificationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationClientImpl implements NotificationClient {

    private static final String SOURCE_SERVICE = "pos-auth-issuer";

    private final NotificationFeignClient notificationFeignClient;

    @Override
    public void sendEmail(String recipient,
                          String recipientName,
                          String subject,
                          String htmlBody,
                          UUID parentTenantPublicId,
                          String correlationId) {
        try {
            notificationFeignClient.send(new SendNotificationRequest(
                    NotificationChannel.EMAIL,
                    recipient,
                    recipientName,
                    subject,
                    htmlBody,
                    true,
                    List.of(),
                    null,
                    null,
                    SOURCE_SERVICE,
                    correlationId,
                    parentTenantPublicId
            ));
        } catch (Exception ex) {
            log.warn("Failed to send email via notification-service to {}: {}", recipient, ex.getMessage());
        }
    }
}
