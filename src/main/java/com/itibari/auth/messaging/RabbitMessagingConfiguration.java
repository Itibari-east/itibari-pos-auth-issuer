package com.itibari.auth.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@org.springframework.scheduling.annotation.EnableScheduling
@Configuration
@ConditionalOnProperty(name = "messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMessagingConfiguration {

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter("com.itibari.auth.messaging");
    }

    @Bean
    public Declarables rabbitTopology() {
        List<Declarable> declarations = new ArrayList<>();
        Set<String> exchanges = new HashSet<>();
        for (RabbitMessaging.Subscription subscription : RabbitMessaging.Subscription.values()) {
            DirectExchange exchange = new DirectExchange(subscription.exchange(), true, false);
            DirectExchange deadExchange = new DirectExchange(subscription.deadExchange(), true, false);
            if (exchanges.add(exchange.getName())) {
                declarations.add(exchange);
            }
            if (exchanges.add(deadExchange.getName())) {
                declarations.add(deadExchange);
            }
            Queue queue = QueueBuilder.durable(subscription.queue())
                    .deadLetterExchange(subscription.deadExchange())
                    .deadLetterRoutingKey(subscription.routingKey()).build();
            Queue deadQueue = QueueBuilder.durable(subscription.deadQueue()).build();
            declarations.add(queue);
            declarations.add(deadQueue);
            declarations.add(BindingBuilder.bind(queue).to(exchange).with(subscription.routingKey()));
            declarations.add(BindingBuilder.bind(deadQueue).to(deadExchange).with(subscription.routingKey()));
        }
        return new Declarables(declarations);
    }
}
