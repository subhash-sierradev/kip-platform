package com.integration.execution.contract.model.enums;

public enum AuditActivity {

    // ────────────────
    // Special Lifecycle Marker
    // ────────────────
    PENDING,        // action initiated, final outcome not yet resolved

    // ────────────────
    // Lifecycle / State Transitions
    // ────────────────
    ENABLED,
    DISABLED,

    // ────────────────
    // Execution / Operations
    // ────────────────
    EXECUTE,
    RUN_NOW,
    RETRY,

    // ────────────────
    // Data Mutations
    // ────────────────
    CREATE,
    UPDATE,
    UPSERT,
    DELETE,

    // ────────────────
    // Security / User Actions
    // ────────────────
    LOGIN,
    LOGOUT,

    // ────────────────
    // System / Maintenance
    // ────────────────
    CLEAR_CACHE
}