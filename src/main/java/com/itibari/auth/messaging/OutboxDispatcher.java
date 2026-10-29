package com.itibari.auth.messaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ConfirmedRabbitPublisher publisher;

    @Scheduled(initialDelayString = "${messaging.rabbitmq.outbox-initial-delay-ms:1000}",
            fixedDelayString = "${messaging.rabbitmq.outbox-delay-ms:1000}")
    @Transactional
    public void dispatch() throws Exception {
        // One dispatcher across replicas; retain send order and never delete unconfirmed events.
        jdbc.queryForObject("select id from pos_core.rabbit_dispatch_lock where id = 1 for update", Integer.class);
        var pending = jdbc.queryForList("select event_id, exchange_name, routing_key, payload from pos_core.rabbit_outbox where published_at is null order by id limit 50");
        for (var row : pending) {
            UUID id = UUID.fromString(row.get("event_id").toString());
            Map<String, Object> payload = mapper.readValue(row.get("payload").toString(), new TypeReference<>() {});
            publisher.publish(row.get("exchange_name").toString(), row.get("routing_key").toString(), payload, id);
            jdbc.update("update pos_core.rabbit_outbox set published_at = CURRENT_TIMESTAMP where event_id = ?", id);
        }
    }
}
