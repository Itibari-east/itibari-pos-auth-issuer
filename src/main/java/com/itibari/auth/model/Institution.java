package com.itibari.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "institutions", schema = "pos_core")
public class Institution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "identity_number", unique = true)
    private String identityNumber;

    @Column(name = "main_tenant")
    @Builder.Default
    private boolean mainTenant = false;

    @Column(name = "parent_tenant")
    @Builder.Default
    private boolean parentTenant = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Institution parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private List<Institution> children = new ArrayList<>();

    @Column(name = "country")
    private String country;

    @Column(name = "tra_pin")
    private String traPin;

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "base_currency")
    private String baseCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_preference_fk_id")
    private NotificationPreference subscriptionPreference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marketing_preference_fk_id")
    private NotificationPreference marketingPreference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_subscription_detail_fk_id")
    private ShopSubscriptionDetail shopSubscriptionDetail;

    @Column(name = "city")
    private String city;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private AuthUser createdBy;
}
