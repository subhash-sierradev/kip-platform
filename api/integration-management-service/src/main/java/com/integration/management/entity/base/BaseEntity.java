package com.integration.management.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

/**
 * TIMESTAMP PATTERN: All timestamp fields use Instant (UTC storage).
 * Backend stores only UTC timestamps - frontend handles timezone conversion for display.
 */
@MappedSuperclass
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity<T extends Serializable> {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private T id;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date", nullable = false)
    private Instant lastModifiedDate; // Always UTC - UI handles timezone conversion

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
}
