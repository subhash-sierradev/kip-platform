package com.integration.management.entity.base;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@MappedSuperclass
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class UuidBaseEntity extends BaseEntity<UUID> {

    @PrePersist
    public void generateId() {
        if (getId() == null) {
            setId(UUID.randomUUID());
        }
        // Ensure audit fields have defaults if not set
        if (getIsDeleted() == null) {
            setIsDeleted(false);
        }
    }
}