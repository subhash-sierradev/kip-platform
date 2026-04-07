package com.integration.management.notification.aop;

import com.integration.management.entity.ArcGISIntegration;
import com.integration.management.repository.ArcGISIntegrationRepository;
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
@DisplayName("ArcGISNotificationMetadataProvider")
class ArcGISNotificationMetadataProviderTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock private ArcGISIntegrationRepository arcGISIntegrationRepository;

    @InjectMocks
    private ArcGISNotificationMetadataProvider provider;

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("returns metadata map with name and id when integration found")
        void returns_metadata_when_found() {
            UUID integrationId = UUID.randomUUID();
            ArcGISIntegration integration = ArcGISIntegration.builder()
                    .name("My Map").build();
            integration.setId(integrationId);

            when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(
                    integrationId, TENANT_ID))
                    .thenReturn(Optional.of(integration));

            Map<String, Object> result = provider.resolve(integrationId, TENANT_ID);

            assertThat(result).containsEntry("integrationName", "My Map")
                    .containsEntry("integrationId", integrationId.toString());
        }

        @Test
        @DisplayName("returns empty map when integration not found")
        void returns_empty_map_when_not_found() {
            UUID integrationId = UUID.randomUUID();
            when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(
                    integrationId, TENANT_ID))
                    .thenReturn(Optional.empty());

            Map<String, Object> result = provider.resolve(integrationId, TENANT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("substitutes empty string when integration name is null")
        void substitutes_empty_string_when_name_null() {
            UUID integrationId = UUID.randomUUID();
            ArcGISIntegration integration = ArcGISIntegration.builder()
                    .name(null).build();
            integration.setId(integrationId);

            when(arcGISIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(
                    integrationId, TENANT_ID))
                    .thenReturn(Optional.of(integration));

            Map<String, Object> result = provider.resolve(integrationId, TENANT_ID);

            assertThat(result).containsEntry("integrationName", "");
        }
    }
}
