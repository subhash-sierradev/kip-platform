package com.integration.execution.contract.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KwLocationDto {
    private String id;
    private long createdTimestamp;
    private long updatedTimestamp;
    private String locationName;
    private String locationType;
    private String street1;
    private String street2;
    private String district;
    private String city;
    private String county;
    private String state;
    private String zipCode;
    private String country;
    private Double latitude;
    private Double longitude;
}
