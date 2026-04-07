package com.integration.management.notification.entity;

import com.integration.management.entity.base.UuidBaseEntity;
import com.integration.execution.contract.model.enums.RecipientType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_recipient_policy", schema = "notifications")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRecipientPolicy extends UuidBaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "rule_id",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(name = "fk_notification_recipient_policy_rule")
    )
    @ToString.Exclude
    private NotificationRule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 50)
    private RecipientType recipientType;

}
