package com.itibari.auth.messaging;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationCommandPublisher {
    private final ConfirmedRabbitPublisher publisher;

    public void send(NotificationCommand command) {
        if (command == null || command.channel() == null || command.recipient() == null
                || command.recipient().isBlank()) {
            throw new IllegalArgumentException("Notification channel and recipient are required");
        }
        publisher.publish(RabbitMessaging.Exchanges.NOTIFICATIONS,
                RabbitMessaging.RoutingKeys.SEND_NOTIFICATION, command, UUID.randomUUID());
    }
}
