package com.itibari.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification_preferences", schema = "pos_core")
public class NotificationPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sms")
    private boolean sms;

    @Column(name = "email")
    private boolean email;

    @Column(name = "push_notification")
    private boolean pushNotification;
}
