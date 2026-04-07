package com.integration.execution.contract.model.enums;

/**
 * Enumeration of Jira field data types.
 */
public enum JiraDataType {
    STRING,      // text fields
    NUMBER,      // numeric fields
    DATE,        // date/datetime fields
    ARRAY,       // multi-select, labels, etc.
    OBJECT,      // complex objects like user, project
    BOOLEAN,     // boolean/checkbox fields
    URL,         // URL fields
    EMAIL,        // email fields
    USER,         // user picker fields
    MULTIUSER   // multi-user picker fields
}
