package com.integration.management.notification.service;

import com.integration.management.notification.entity.NotificationTemplate;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.response.NotificationTemplateResponse;
import com.integration.management.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationTemplateService")
class NotificationTemplateServiceTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationTemplateService notificationTemplateService;

    @Nested
    @DisplayName("getTemplatesForTenant")
    class GetTemplatesForTenant {

        @Test
        @DisplayName("maps templates to response DTOs")
        void maps_templates_to_response_dtos() {
            NotificationTemplate template = NotificationTemplate.builder()
                    .id(UUID.randomUUID())
                    .titleTemplate("Title {{name}}")
                    .messageTemplate("Message {{name}}")
                    .build();
            template.setTenantId(TENANT_ID);
            NotificationTemplateResponse response = NotificationTemplateResponse.builder()
                    .id(template.getId())
                    .titleTemplate("Title {{name}}")
                    .messageTemplate("Message {{name}}")
                    .build();

            when(notificationTemplateRepository.findByTenantId(TENANT_ID))
                    .thenReturn(List.of(template));
            when(notificationMapper.toTemplateResponse(template)).thenReturn(response);

            List<NotificationTemplateResponse> result =
                    notificationTemplateService.getTemplatesForTenant(TENANT_ID);

            assertThat(result).containsExactly(response);
            verify(notificationTemplateRepository).findByTenantId(TENANT_ID);
        }

        @Test
        @DisplayName("returns empty list when no templates exist for tenant")
        void returns_empty_list_when_no_templates() {
            when(notificationTemplateRepository.findByTenantId(TENANT_ID))
                    .thenReturn(List.of());

            List<NotificationTemplateResponse> result =
                    notificationTemplateService.getTemplatesForTenant(TENANT_ID);

            assertThat(result).isEmpty();
        }
    }
}
