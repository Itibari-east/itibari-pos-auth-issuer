package com.itibari.auth.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Validator;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Authenticated, explicitly allowlisted operations; no arbitrary method dispatch. */
@Component
@ConditionalOnProperty(name = "messaging.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitRpcHandler {
    private final ObjectMapper mapper;
    private final JwtDecoder decoder;
    private final Validator validator;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final Map<String, Endpoint> endpoints = new HashMap<>();
    private record Endpoint(Object bean, Method method, boolean mutation) {}

    public RabbitRpcHandler(ObjectMapper mapper,
            @org.springframework.beans.factory.annotation.Qualifier("jwtDecoder") JwtDecoder decoder,
            Validator validator, JdbcTemplate jdbc, PlatformTransactionManager transactionManager,
            ApplicationContext context) {
        this.mapper = mapper;
        this.decoder = decoder;
        this.validator = validator;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
        register("createChildUser", context.getBean(com.itibari.auth.web.UserRestController.class), "createTenantUser", true);
        register("addUserRoles", context.getBean(com.itibari.auth.web.UserRestController.class), "assignRoles", true);
    }

    private void register(String operation, Object bean, String method, boolean mutation) {
        Method selected = Arrays.stream(bean.getClass().getMethods()).filter(m -> m.getName().equals(method))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing RPC handler: " + method));
        endpoints.put(operation, new Endpoint(bean, selected, mutation));
    }

    @RabbitListener(queues = RabbitMessaging.Rpc.AUTHORIZATION)
    public Message handle(Message message) throws Exception {
        var previous = SecurityContextHolder.getContext();
        Map<String, Object> response;
        try {
            String bearer = message.getMessageProperties().getHeader("authorization");
            if (bearer == null || bearer.isBlank()) throw new org.springframework.security.authentication.BadCredentialsException("Missing bearer token");
            Jwt jwt = decoder.decode(bearer);
            AbstractAuthenticationToken authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter().convert(jwt);
            var security = SecurityContextHolder.createEmptyContext();
            security.setAuthentication(authentication);
            SecurityContextHolder.setContext(security);
            RpcRequest request = mapper.readValue(message.getBody(), RpcRequest.class);
            if (request.requestId() == null || request.arguments() == null) throw new IllegalArgumentException("Invalid request");
            Endpoint endpoint = endpoints.get(request.operation());
            if (endpoint == null) throw new IllegalArgumentException("Unsupported operation");
            if (request.expiresAt() <= System.currentTimeMillis()) {
                response = error(504, "Request expired before execution");
            } else if (!endpoint.mutation()) {
                response = success(invoke(endpoint, request.arguments(), jwt, authentication));
            } else {
                String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                        (jwt.getSubject() + ":" + jwt.getClaimAsString("public_id") + ":" + request.operation() + ":" + request.arguments())
                                .getBytes(StandardCharsets.UTF_8)));
                // Serialize mutations across replicas. The result and domain changes commit together.
                String result = transaction.execute(status -> {
                    jdbc.queryForObject("select id from pos_core.rabbit_rpc_lock where id = 1 for update", Integer.class);
                    var cached = jdbc.queryForList("select fingerprint, response from pos_core.rabbit_rpc_results where request_id = ?", request.requestId());
                    if (!cached.isEmpty()) {
                        if (!fingerprint.equals(cached.getFirst().get("fingerprint"))) throw new IllegalArgumentException("Request ID reused with different arguments or caller");
                        return cached.getFirst().get("response").toString();
                    }
                    if (request.expiresAt() <= System.currentTimeMillis()) throw new IllegalArgumentException("Request expired waiting for execution");
                    String json = json(success(invoke(endpoint, request.arguments(), jwt, authentication)));
                    jdbc.update("insert into pos_core.rabbit_rpc_results (request_id, fingerprint, response) values (?, ?, ?)",
                            request.requestId(), fingerprint, json);
                    return json;
                });
                response = mapper.readValue(result, new com.fasterxml.jackson.core.type.TypeReference<>() {});
            }
        } catch (org.springframework.security.core.AuthenticationException | org.springframework.security.oauth2.jwt.JwtException ex) {
            response = error(401, "Invalid or expired authentication");
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            response = error(403, "Access denied");
        } catch (Exception ex) {
            String type = ex.getClass().getSimpleName();
            int code = type.contains("NotFound") ? 404 : type.contains("BadRequest") || ex instanceof IllegalArgumentException ? 400 : 500;
            response = error(code, code == 500 ? "Service operation failed" : ex.getMessage());
        } finally {
            SecurityContextHolder.setContext(previous);
        }
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return new Message(mapper.writeValueAsBytes(response), properties);
    }

    private Object invoke(Endpoint endpoint, JsonNode values, Jwt jwt, AbstractAuthenticationToken authentication) {
        Parameter[] parameters = endpoint.method().getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (p.getType() == Jwt.class) { args[i] = jwt; continue; }
            if (p.getType().getSimpleName().equals("LoggedInUserDetails")) { args[i] = authentication.getDetails(); continue; }
            if (Pageable.class.isAssignableFrom(p.getType())) {
                List<Sort.Order> orders = new ArrayList<>();
                JsonNode sort = values.get("sort");
                if (sort != null && sort.isArray()) for (JsonNode entry : sort) {
                    String[] parts = entry.asText().split(",");
                    orders.add(new Sort.Order(parts.length > 1 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC, parts[0]));
                }
                int size = values.path("size").asInt(20);
                args[i] = PageRequest.of(values.path("page").asInt(0), Math.min(size, 2000), Sort.by(orders));
                continue;
            }
            String key = p.getName();
            RequestParam query = p.getAnnotation(RequestParam.class);
            if (query != null) key = name(query.value(), query.name(), key);
            PathVariable path = p.getAnnotation(PathVariable.class);
            if (path != null) key = name(path.value(), path.name(), key);
            if (p.isAnnotationPresent(RequestBody.class)) key = "body";
            args[i] = mapper.convertValue(values.get(key), mapper.constructType(p.getParameterizedType()));
            if (p.isAnnotationPresent(RequestBody.class)) {
                if (args[i] == null || !validator.validate(args[i]).isEmpty()) throw new IllegalArgumentException("Invalid request body");
            }
        }
        try {
            Object result = endpoint.method().invoke(endpoint.bean(), args);
            if (result instanceof ResponseEntity<?> entity) return entity.getBody();
            return result;
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Operation failed", ex.getCause());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Operation dispatch failed", ex);
        }
    }
    private static String name(String value, String name, String fallback) {
        return !value.isBlank() ? value : !name.isBlank() ? name : fallback;
    }
    private static Map<String, Object> success(Object body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", 200); result.put("body", body); return result;
    }
    private static Map<String, Object> error(int code, String detail) {
        return Map.of("status", code, "error", detail == null ? "Request failed" : detail);
    }
    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalArgumentException("Invalid response", ex); }
    }
}
