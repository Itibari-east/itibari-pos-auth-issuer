package com.itibari.auth.messaging;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
public record RpcRequest(UUID requestId, String operation, JsonNode arguments, long expiresAt) {}
