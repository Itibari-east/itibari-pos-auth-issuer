package com.itibari.auth.messaging;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.ArrayList;
import java.util.List;
@Configuration
@ConditionalOnProperty(name = "messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitRpcTopology {
    @Bean
    public Declarables rpcTopology() {
        DirectExchange exchange = new DirectExchange(RabbitMessaging.Rpc.EXCHANGE, true, false);
        List<Declarable> declarations = new ArrayList<>();
        declarations.add(exchange);
        for (String name : List.of(RabbitMessaging.Rpc.AUTHORIZATION)) {
            Queue queue = QueueBuilder.durable(name).ttl(30000).build();
            declarations.add(queue);
            declarations.add(BindingBuilder.bind(queue).to(exchange).with(name));
        }
        return new Declarables(declarations);
    }
}
