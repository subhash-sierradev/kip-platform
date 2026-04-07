package com.integration.management.notification.aop;

import com.integration.execution.contract.model.enums.ConfigValueType;
import com.integration.management.entity.SiteConfig;
import com.integration.management.repository.SiteConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteConfigNotificationMetadataProvider")
class SiteConfigNotificationMetadataProviderTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock private SiteConfigRepository siteConfigRepository;

    @InjectMocks
    private SiteConfigNotificationMetadataProvider provider;

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("returns configKey in metadata when site config found")
        void returns_config_key_when_found() {
            UUID configId = UUID.randomUUID();
            SiteConfig config = SiteConfig.builder()
                    .configKey("brandingPrimary")
                    .configValue("blue")
                    .type(ConfigValueType.STRING)
                    .build();
            config.setId(configId);

            when(siteConfigRepository.findByTenant(configId, TENANT_ID))
                    .thenReturn(Optional.of(config));

            Map<String, Object> result = provider.resolve(configId, TENANT_ID);

            assertThat(result).containsEntry("configKey", "brandingPrimary");
        }

        @Test
        @DisplayName("returns empty map when site config not found")
        void returns_empty_map_when_not_found() {
            UUID configId = UUID.randomUUID();
            when(siteConfigRepository.findByTenant(configId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThat(provider.resolve(configId, TENANT_ID)).isEmpty();
        }
    }
}
