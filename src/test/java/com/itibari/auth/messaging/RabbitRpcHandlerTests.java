package com.itibari.auth.messaging;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import jakarta.validation.Validation;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class RabbitRpcHandlerTests {
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    final Map<Class<?>, Object> controllers = new HashMap<>();
    final JwtDecoder decoder = mock(JwtDecoder.class);
    RabbitRpcHandler handler;
    JdbcTemplate jdbc;
    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("create schema pos_core");
        jdbc.execute("create table pos_core.rabbit_rpc_lock (id int primary key)");
        jdbc.update("insert into pos_core.rabbit_rpc_lock values (1)");
        jdbc.execute("create table pos_core.rabbit_rpc_results (request_id uuid primary key, fingerprint varchar(64), response varchar)");
        var context = mock(ApplicationContext.class);
        when(context.getBean(any(Class.class))).thenAnswer(call -> controllers.computeIfAbsent(call.getArgument(0), type -> mock(type)));
        when(decoder.decode("valid")).thenReturn(Jwt.withTokenValue("valid").header("alg", "RS256")
                .subject("caller").claim("public_id", UUID.randomUUID().toString()).build());
        handler = new RabbitRpcHandler(mapper, decoder, Validation.buildDefaultValidatorFactory().getValidator(),
                jdbc, new DataSourceTransactionManager(ds), context);
    }
    Map<String, Object> arguments() { return Map.of("body", Map.of("rolePublicIds", List.of(UUID.randomUUID())) , "userId", UUID.randomUUID()); }
    Message message(UUID id, String operation, Map<String, Object> arguments, String token, long expires) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("authorization", token);
        return new Message(mapper.writeValueAsBytes(new RpcRequest(id, operation, mapper.valueToTree(arguments), expires)), properties);
    }
    @Test void redeliveryReturnsCachedResultWithoutRepeatingMutation() throws Exception {
        var previous = SecurityContextHolder.getContext();
        Message command = message(UUID.randomUUID(), "addUserRoles", arguments(), "valid", System.currentTimeMillis() + 30000);
        assertThat(mapper.readTree(handler.handle(command).getBody()).path("status").asInt()).isEqualTo(200);
        assertThat(mapper.readTree(handler.handle(command).getBody()).path("status").asInt()).isEqualTo(200);
        long calls = mockingDetails(controllers.get(com.itibari.auth.web.UserRestController.class)).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("assignRoles")).count();
        assertThat(calls).isEqualTo(1);
        assertThat(SecurityContextHolder.getContext()).isSameAs(previous);
        assertThat(jdbc.queryForObject("select count(*) from pos_core.rabbit_rpc_results", Integer.class)).isEqualTo(1);
    }
    @Test void rejectsInvalidTokenBeforeDispatch() throws Exception {
        when(decoder.decode("invalid")).thenThrow(new BadJwtException("invalid"));
        Message command = message(UUID.randomUUID(), "addUserRoles", arguments(), "invalid", System.currentTimeMillis() + 30000);
        assertThat(mapper.readTree(handler.handle(command).getBody()).path("status").asInt()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select count(*) from pos_core.rabbit_rpc_results", Integer.class)).isZero();
    }
    @Test void rejectsExpiredAndUnknownOperations() throws Exception {
        assertThat(mapper.readTree(handler.handle(message(UUID.randomUUID(), "addUserRoles", arguments(), "valid", 1)).getBody()).path("status").asInt()).isEqualTo(504);
        assertThat(mapper.readTree(handler.handle(message(UUID.randomUUID(), "deleteEverything", arguments(), "valid", System.currentTimeMillis() + 30000)).getBody()).path("status").asInt()).isEqualTo(400);
    }
    @Test void rejectsInvalidBodyBeforeSideEffects() throws Exception {
        assertThat(mapper.readTree(handler.handle(message(UUID.randomUUID(), "addUserRoles", Map.of("body", Map.of()), "valid", System.currentTimeMillis() + 30000)).getBody()).path("status").asInt()).isEqualTo(400);
        assertThat(jdbc.queryForObject("select count(*) from pos_core.rabbit_rpc_results", Integer.class)).isZero();
    }
}
