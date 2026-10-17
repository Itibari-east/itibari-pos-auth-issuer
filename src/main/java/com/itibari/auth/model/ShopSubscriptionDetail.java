package com.itibari.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shop_subscription_details", schema = "pos_core")
public class ShopSubscriptionDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_fk_id")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_fk_id")
    @JsonIgnore
    private Institution institution;

    @Column(name = "subscribed_date")
    private LocalDateTime subscribedDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "days", nullable = false)
    private int days;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "free_trial", nullable = false)
    @Builder.Default
    private boolean freeTrial = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscribed_by_fk_id")
    @JsonIgnore
    private AuthUser subscribedBy;
}
