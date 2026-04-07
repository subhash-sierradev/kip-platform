package com.integration.management.mapper;

import org.mapstruct.Named;

public interface NormalizationMapper {

    @Named("normalize")
    default String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}