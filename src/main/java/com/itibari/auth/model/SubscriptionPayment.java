package com.itibari.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "subscription_payments", schema = "pos_core",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "idempotency_key"}))
public class SubscriptionPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AuthUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @ManyToOne
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "conversation_id", nullable = false, unique = true)
    private String conversationId;

    @Column(name = "reference", nullable = false, unique = true, length = 20)
    private String reference;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "days", nullable = false)
    private int days;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "provider_conversation_id")
    private String providerConversationId;

    @Column(name = "response_code")
    private String responseCode;

    @OneToOne
    @JoinColumn(name = "detail_id", unique = true)
    private ShopSubscriptionDetail detail;
}
