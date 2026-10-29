package com.itibari.auth.messaging;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.amqp.core.Queue;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(properties = {
    "messaging.rabbitmq.enabled=true", "notification.rabbitmq.enabled=true",
    "spring.rabbitmq.listener.simple.auto-startup=false", "spring.rabbitmq.dynamic=false",
    "messaging.rabbitmq.outbox-initial-delay-ms=600000",
    "spring.autoconfigure.exclude=org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration"
})
@ActiveProfiles("test")
class RabbitWiringTests {
    @Autowired RabbitMessagingConfiguration config;
    @Test void enabledMessagingWiresAndDeclaresNotificationContract() {
        assertThat(config.rabbitTopology().getDeclarablesByType(Queue.class)).anySatisfy(queue -> {
            assertThat(queue.getName()).isEqualTo("pos.notifications.send");
            assertThat(queue.isDurable()).isTrue();
            assertThat(queue.getArguments()).containsEntry("x-dead-letter-exchange", "pos.notifications.dead")
                    .containsEntry("x-dead-letter-routing-key", "pos.notifications.send");
        });
    }
}
