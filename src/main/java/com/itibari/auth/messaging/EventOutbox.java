package com.itibari.auth.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Transactional
    public UUID enqueue(String exchange, String routingKey, Map<String, Object> payload) {
        UUID id = UUID.randomUUID();
        Map<String, Object> event = new LinkedHashMap<>(payload);
        event.put("eventId", id.toString());
        try {
            jdbc.update("insert into pos_core.rabbit_outbox (event_id, exchange_name, routing_key, payload) values (?, ?, ?, ?)",
                    id, exchange, routingKey, mapper.writeValueAsString(event));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("Cannot serialize event", ex);
        }
        return id;
    }
}
