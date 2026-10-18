package com.itibari.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Entity
@Table(name = "auth_users", schema = "pos_core")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    @Column(name = "enabled")
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "active_status", nullable = false)
    @Builder.Default
    private boolean activeStatus = true;

    @Column(name = "account_non_expired")
    @Builder.Default
    private boolean accountNonExpired = true;

    @Column(name = "credentials_non_expired")
    @Builder.Default
    private boolean credentialsNonExpired = true;

    @Column(name = "account_non_locked")
    @Builder.Default
    private boolean accountNonLocked = true;

    @Column(name = "admin")
    @Builder.Default
    private boolean admin = false;

    @Column(name = "force_password_reset")
    @Builder.Default
    private boolean forcePasswordReset = false;

    @Column(name = "accepted_terms")
    @Builder.Default
    private boolean acceptedTerms = false;

    @JsonIgnore
    @Column(name = "reset_password_token_hash")
    private String resetPasswordTokenHash;

    @JsonIgnore
    @Column(name = "reset_password_token_lookup_hash", length = 64, unique = true)
    private String resetPasswordTokenLookupHash;

    @Column(name = "reset_password_token_expires_at")
    private LocalDateTime resetPasswordTokenExpiresAt;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "auth_user_roles", schema = "pos_core",
            joinColumns = @JoinColumn(name = "user_id_fk", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id_fk", referencedColumnName = "id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_institutions", schema = "pos_core",
            joinColumns = @JoinColumn(name = "user_id_fk", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "institution_id_fk", referencedColumnName = "id"))
    @Builder.Default
    private Set<Institution> institutions = new HashSet<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_fk_id")
    private Institution institution;

    public String fullName() {
        return String.join(" ", Stream.of(firstName, lastName)
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    public List<String> roleNames() {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public List<String> authorityNames() {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .flatMap(role -> Stream.concat(
                        role.getPrivilegeList() == null ? Stream.empty() : role.getPrivilegeList().stream(),
                        role.getPrivilegeGroupList() == null ? Stream.empty()
                                : role.getPrivilegeGroupList().stream()
                                .flatMap(group -> group.getPrivileges() == null
                                        ? Stream.empty()
                                        : group.getPrivileges().stream())
                ))
                .map(Privilege::getName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}

