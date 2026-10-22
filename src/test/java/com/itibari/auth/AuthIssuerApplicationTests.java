package com.itibari.auth;

import com.itibari.auth.model.AuthUser;
import com.itibari.auth.model.LoginRequest;
import com.itibari.auth.model.Privilege;
import com.itibari.auth.model.PrivilegeGroup;
import com.itibari.auth.model.Role;
import com.itibari.auth.model.request.CreateProfileDTO;
import com.itibari.auth.repository.AuthUserRepository;
import com.itibari.auth.repository.PrivilegeGroupRepository;
import com.itibari.auth.repository.PrivilegeRepository;
import com.itibari.auth.repository.RoleRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AuthIssuerApplicationTests {

    @LocalServerPort
    protected int port;

    @Autowired
    protected AuthUserRepository authUserRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PrivilegeRepository privilegeRepository;

    @Autowired
    protected PrivilegeGroupRepository privilegeGroupRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        clearAuthData();
    }

    protected void clearAuthData() {
        jdbcTemplate.execute("DELETE FROM pos_core.auth_user_roles");
        jdbcTemplate.execute("DELETE FROM pos_core.users_institutions");
        jdbcTemplate.execute("DELETE FROM pos_core.role_privileges");
        jdbcTemplate.execute("DELETE FROM pos_core.role_privileges_groups");
        jdbcTemplate.execute("DELETE FROM pos_core.privileges");
        jdbcTemplate.execute("DELETE FROM pos_core.privilege_groups");
        jdbcTemplate.execute("DELETE FROM pos_core.roles");
        jdbcTemplate.execute("UPDATE pos_core.auth_users SET institution_fk_id = NULL");
        jdbcTemplate.execute("UPDATE pos_core.institutions SET created_by = NULL");
        jdbcTemplate.execute("UPDATE pos_core.institutions SET parent_id = NULL");
        jdbcTemplate.execute("UPDATE pos_core.institutions SET subscription_preference_fk_id = NULL");
        jdbcTemplate.execute("UPDATE pos_core.institutions SET marketing_preference_fk_id = NULL");
        jdbcTemplate.execute("UPDATE pos_core.institutions SET shop_subscription_detail_fk_id = NULL");
        jdbcTemplate.execute("DELETE FROM pos_core.shop_subscription_details");
        jdbcTemplate.execute("DELETE FROM pos_core.institutions");
        jdbcTemplate.execute("DELETE FROM pos_core.notification_preferences");
        jdbcTemplate.execute("DELETE FROM pos_core.auth_users");
        // keep seeded subscription catalog plans
    }

    protected AuthUser saveUser(String email, String rawPassword) {
        String digits = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return authUserRepository.save(AuthUser.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+2557" + digits)
                .enabled(true)
                .activeStatus(true)
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .accountNonLocked(true)
                .build());
    }

    protected Role saveRole(String name) {
        return saveRole(name, null);
    }

    protected Role saveRole(String name, UUID parentTenantPublicId) {
        return roleRepository.save(Role.builder()
                .name(name)
                .description(name + " description")
                .parentTenantPublicId(parentTenantPublicId)
                .build());
    }

    protected Privilege savePrivilege(String name) {
        return privilegeRepository.save(Privilege.builder()
                .name(name)
                .build());
    }

    protected PrivilegeGroup savePrivilegeGroup(String name, UUID parentTenantPublicId) {
        return privilegeGroupRepository.save(PrivilegeGroup.builder()
                .name(name)
                .parentTenantPublicId(parentTenantPublicId)
                .build());
    }

    protected String createParentShop(String token, String name) {
        return given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(CreateProfileDTO.builder()
                        .name(name)
                        .country("TZ")
                        .city("Dar")
                        .build())
                .post("/institutions")
                .then()
                .statusCode(200)
                .extract()
                .path("publicId");
    }

    protected String login(String identifier, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(identifier, password))
                .post("/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
