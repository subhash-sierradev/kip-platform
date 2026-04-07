package com.integration.management.mapper;

import com.integration.execution.contract.model.SiteConfigDto;
import com.integration.execution.contract.model.enums.ConfigValueType;
import com.integration.management.entity.SiteConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SiteConfigMapper")
class SiteConfigMapperTest {

    @Test
    @DisplayName("fromDto should ignore id and map key/value")
    void fromDto_ignoresId() {
        SiteConfigMapper mapper = Mappers.getMapper(SiteConfigMapper.class);

        SiteConfigDto dto = SiteConfigDto.builder()
                .id(UUID.randomUUID())
                .configKey("k")
                .configValue("v")
                .type(ConfigValueType.STRING)
                .build();

        SiteConfig entity = mapper.fromDto(dto);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getConfigKey()).isEqualTo("k");
        assertThat(entity.getConfigValue()).isEqualTo("v");
        assertThat(entity.getType()).isEqualTo(ConfigValueType.STRING);
    }

    @Test
    @DisplayName("toDto should map id/key/value")
    void toDto_mapsFields() {
        SiteConfigMapper mapper = Mappers.getMapper(SiteConfigMapper.class);

        UUID id = UUID.randomUUID();
        SiteConfig entity = SiteConfig.builder()
                .id(id)
                .configKey("k")
                .configValue("v")
                .type(ConfigValueType.NUMBER)
                .build();

        SiteConfigDto dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getConfigKey()).isEqualTo("k");
        assertThat(dto.getConfigValue()).isEqualTo("v");
        assertThat(dto.getType()).isEqualTo(ConfigValueType.NUMBER);
    }
}
