package com.integration.management.notification.entity;

import com.integration.management.entity.base.UuidBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "notification_template", schema = "notifications")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationTemplate extends UuidBaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "event_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_notification_template_event")
    )
    @ToString.Exclude
    private NotificationEventCatalog event;

    @Column(name = "title_template", nullable = false, length = 255)
    private String titleTemplate;

    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate;

    @Column(name = "allowed_placeholders", columnDefinition = "TEXT", length = 500)
    private String allowedPlaceholders;

}
