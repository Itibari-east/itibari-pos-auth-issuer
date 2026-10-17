package com.itibari.auth.model;

import com.itibari.auth.model.response.RoleResponse;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roles", schema = "pos_core")
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    /** Tenant/shop public id when tenant-scoped; null for global roles. */
    @Column(name = "parent_tenant_public_id")
    private UUID parentTenantPublicId;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "role_privileges", schema = "pos_core",
            joinColumns = @JoinColumn(name = "role_id_fk"),
            inverseJoinColumns = @JoinColumn(name = "privilege_id_fk"))
    @Builder.Default
    private List<Privilege> privilegeList = new ArrayList<>();

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "role_privileges_groups", schema = "pos_core",
            joinColumns = @JoinColumn(name = "role_id_fk"),
            inverseJoinColumns = @JoinColumn(name = "privilege_group_id_fk"))
    @Builder.Default
    private List<PrivilegeGroup> privilegeGroupList = new ArrayList<>();

    public RoleResponse toRoleResponse() {
        return RoleResponse.builder()
                .publicId(this.getPublicId())
                .name(this.getName())
                .build();
    }

    public void addPrivilege(Privilege privilege) {
        if (privilegeList == null) {
            privilegeList = new ArrayList<>();
        }
        boolean alreadyAssigned = privilegeList.stream()
                .anyMatch(existing -> existing.getId().equals(privilege.getId()));
        if (!alreadyAssigned) {
            privilegeList.add(privilege);
        }
    }

    public void addPrivilegeGroup(PrivilegeGroup privilegeGroup) {
        if (privilegeGroupList == null) {
            privilegeGroupList = new ArrayList<>();
        }
        boolean alreadyAssigned = privilegeGroupList.stream()
                .anyMatch(existing -> existing.getId().equals(privilegeGroup.getId()));
        if (!alreadyAssigned) {
            privilegeGroupList.add(privilegeGroup);
        }
    }
}
