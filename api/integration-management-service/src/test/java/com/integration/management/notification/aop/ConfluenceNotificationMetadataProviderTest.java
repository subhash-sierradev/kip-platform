package com.integration.management.notification.aop;

import com.integration.management.entity.ConfluenceIntegration;
import com.integration.management.entity.IntegrationSchedule;
import com.integration.management.repository.ConfluenceIntegrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceNotificationMetadataProvider")
class ConfluenceNotificationMetadataProviderTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private ConfluenceIntegrationRepository confluenceIntegrationRepository;

    @InjectMocks
    private ConfluenceNotificationMetadataProvider provider;

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("returns metadata map with name and id when integration found")
        void returns_metadata_when_found() {
            UUID integrationId = UUID.randomUUID();
            IntegrationSchedule schedule = IntegrationSchedule.builder()
                    .id(UUID.randomUUID())
                    .executionTime(LocalTime.of(10, 0))
                    .cronExpression("0 0 10 * * ?")
                    .build();
            ConfluenceIntegration integration = ConfluenceIntegration.builder()
                    .name("My Confluence Report")
                    .schedule(schedule)
                    .build();
            integration.setId(integrationId);

            when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(
                    integrationId, TENANT_ID))
                    .thenReturn(Optional.of(integration));

            Map<String, Object> result = provider.resolve(integrationId, TENANT_ID);

            assertThat(result).containsEntry("integrationName", "My Confluence Report")
                    .containsEntry("integrationId", integrationId.toString());
        }

        @Test
        @DisplayName("substitutes empty string when integration name is null")
        void substitutes_empty_string_when_name_null() {
            UUID integrationId = UUID.randomUUID();
            ConfluenceIntegration integration = ConfluenceIntegration.builder()
                    .name(null)
                    .build();
            integration.setId(integrationId);

            when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(
                    integrationId, TENANT_ID))
                    .thenReturn(Optional.of(integration));

            Map<String, Object> result = provider.resolve(integrationId, TENANT_ID);

            assertThat(result).containsEntry("integrationName", "");
            assertThat(result).containsKey("integrationId");
        }

        @Test
        @DisplayName("returns empty map when integration not found")
        void returns_empty_map_when_not_found() {
            UUID integrationId = UUID.randomUUID();
            when(confluenceIntegrationRepository.findByIdAndTenantIdAndIsDeletedFalse(
                    integrationId, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThat(provider.resolve(integrationId, TENANT_ID)).isEmpty();
        }
    }
}
