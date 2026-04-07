package com.integration.management.mapper;

import com.integration.execution.contract.model.SiteConfigDto;
import com.integration.management.entity.SiteConfig;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SiteConfigMapper {
    @Mapping(target = "id", ignore = true)
    SiteConfig fromDto(@Valid SiteConfigDto request);

    SiteConfigDto toDto(SiteConfig siteConfig);
}
