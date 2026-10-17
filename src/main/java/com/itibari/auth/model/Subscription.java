package com.itibari.auth.model;

import com.itibari.auth.dataTypes.SubscriptionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subscriptions", schema = "pos_core")
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name")
    private SubscriptionType name;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private int trialDays = 7;

    @Column(name = "days", nullable = false)
    @Builder.Default
    private int days = 30;
}
