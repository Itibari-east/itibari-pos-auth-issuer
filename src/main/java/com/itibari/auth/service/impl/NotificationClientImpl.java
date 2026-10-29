package com.itibari.auth.service.impl;

import com.itibari.auth.client.notification.NotificationChannel;
import com.itibari.auth.messaging.EventOutbox;
import com.itibari.auth.messaging.RabbitMessaging;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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

    private final EventOutbox outbox;
    private final ObjectMapper mapper;
    @org.springframework.beans.factory.annotation.Value("${messaging.rabbitmq.enabled:true}")
    private boolean enabled;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void sendEmail(String recipient,
                          String recipientName,
                          String subject,
                          String htmlBody,
                          UUID parentTenantPublicId,
                          String correlationId) {
        if (!enabled) return;
        SendNotificationRequest command = new SendNotificationRequest(
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
            );
        outbox.enqueue(RabbitMessaging.Exchanges.NOTIFICATIONS, RabbitMessaging.RoutingKeys.SEND_NOTIFICATION,
                mapper.convertValue(command, new TypeReference<>() {}));
    }
}
