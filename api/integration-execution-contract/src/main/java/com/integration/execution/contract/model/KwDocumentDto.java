package com.integration.execution.contract.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KwDocumentDto {
    private String id;
    private String title;
    private String documentType;
    private long createdTimestamp;
    private long updatedTimestamp;
    private List<KwLocationDto> locations;


    public KwDocumentDto(String id, String title, String documentType,
                         long createdTimestamp, long updatedTimestamp) {
        this.id = id;
        this.title = title;
        this.documentType = documentType;
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = updatedTimestamp;
    }
}