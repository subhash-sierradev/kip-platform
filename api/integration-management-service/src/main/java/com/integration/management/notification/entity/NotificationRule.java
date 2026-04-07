package com.integration.management.notification.entity;

import com.integration.management.entity.base.UuidBaseEntity;
import com.integration.execution.contract.model.enums.NotificationSeverity;
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
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notification_rule", schema = "notifications")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRule extends UuidBaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "event_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_notification_rule_event")
    )
    @ToString.Exclude
    private NotificationEventCatalog event;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private NotificationSeverity severity;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;
}
