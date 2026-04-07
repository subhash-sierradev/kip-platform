package com.integration.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Supported language for multi-language Confluence reports.
 * Future languages are added via SQL INSERT only — no code change required.
 */
@Entity
@Table(name = "languages", schema = "integration_platform")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Language {

    /** BCP-47 language tag (e.g. "en", "ja"). */
    @Id
    @Column(name = "code", length = 10)
    private String code;

    /** English display name (e.g. "English", "Japanese"). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Native script name (e.g. "English", "日本語"). */
    @Column(name = "native_name", nullable = false, length = 100)
    private String nativeName;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
