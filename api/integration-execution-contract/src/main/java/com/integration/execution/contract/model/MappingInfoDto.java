package com.integration.execution.contract.model;

import lombok.Data;

@Data
public class MappingInfoDto {
    private String id;
    private String sourceField;
    private String targetField;
    private String targetEntity;
    private String dataType;
    private Boolean isMandatory;
}
