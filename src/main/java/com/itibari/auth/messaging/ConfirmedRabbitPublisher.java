package com.itibari.auth.messaging;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class ConfirmedRabbitPublisher {
    private final RabbitTemplate rabbitTemplate;

    /** Returns only after the broker confirms a persistent, routable message. */
    public void publish(String exchange, String routingKey, Object payload, UUID eventId) {
        CorrelationData correlation = new CorrelationData(eventId.toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(eventId.toString());
            // Notification-service only trusts its own messaging package. Override the
            // producer's Map/Java type id so the listener can deserialize the JSON.
            if (RabbitMessaging.RoutingKeys.SEND_NOTIFICATION.equals(routingKey)) {
                message.getMessageProperties().setHeader("__TypeId__",
                        "com.itibari.notification_service.messaging.NotificationSendMessage");
            }
            return message;
        }, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new AmqpException("Broker rejected or returned event " + eventId);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted waiting for broker confirmation", ex);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
            throw new AmqpException("Broker confirmation failed for event " + eventId, ex);
        }
    }
}
