package com.itibari.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "privilege_groups", schema = "pos_core")
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivilegeGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    /** Tenant/shop public id when tenant-scoped; null for global groups. */
    @Column(name = "parent_tenant_public_id")
    private UUID parentTenantPublicId;

    @Builder.Default
    @OneToMany(mappedBy = "group", fetch = FetchType.EAGER)
    private Set<Privilege> privileges = new HashSet<>();
}
