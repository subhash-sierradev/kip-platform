package com.integration.management.notification.service;

import com.integration.execution.contract.model.enums.ServiceType;
import com.integration.management.config.authorization.ServiceTypeAuthorizationHelper;
import com.integration.management.notification.entity.NotificationEventCatalog;
import com.integration.execution.contract.model.enums.NotificationEntityType;
import com.integration.management.notification.mapper.NotificationMapper;
import com.integration.management.notification.model.dto.response.NotificationEventCatalogResponse;
import com.integration.management.notification.repository.NotificationEventCatalogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventCatalogService")
class NotificationEventCatalogServiceTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock
    private NotificationEventCatalogRepository eventCatalogRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private ServiceTypeAuthorizationHelper serviceTypeAuth;

    @InjectMocks
    private NotificationEventCatalogService notificationEventCatalogService;

    @Nested
    @DisplayName("getAllEnabled — entity type filtering")
    class GetAllEnabled {

        @Test
        @DisplayName("includes only SITE_CONFIG when user has no feature roles")
        void includes_only_site_config_when_no_roles() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(false);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(any()))
                    .thenReturn(List.of());

            notificationEventCatalogService.getAllEnabled(TENANT_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(Collection.class);
            org.mockito.Mockito.verify(eventCatalogRepository)
                    .findByIsEnabledTrueAndEntityTypeIn(captor.capture());
            assertThat(captor.getValue())
                    .containsExactlyInAnyOrder(NotificationEntityType.SITE_CONFIG);
        }

        @Test
        @DisplayName("includes JIRA_WEBHOOK and INTEGRATION_CONNECTION when user has Jira role")
        void includes_jira_types_when_has_jira_role() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(true);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(any()))
                    .thenReturn(List.of());

            notificationEventCatalogService.getAllEnabled(TENANT_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(Collection.class);
            org.mockito.Mockito.verify(eventCatalogRepository)
                    .findByIsEnabledTrueAndEntityTypeIn(captor.capture());
            assertThat(captor.getValue()).contains(
                    NotificationEntityType.SITE_CONFIG,
                    NotificationEntityType.JIRA_WEBHOOK,
                    NotificationEntityType.INTEGRATION_CONNECTION);
            assertThat(captor.getValue())
                    .doesNotContain(NotificationEntityType.ARCGIS_INTEGRATION);
        }

        @Test
        @DisplayName("includes ARCGIS_INTEGRATION and INTEGRATION_CONNECTION when user has ArcGIS role")
        void includes_arcgis_types_when_has_arcgis_role() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(false);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(true);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(any()))
                    .thenReturn(List.of());

            notificationEventCatalogService.getAllEnabled(TENANT_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(Collection.class);
            org.mockito.Mockito.verify(eventCatalogRepository)
                    .findByIsEnabledTrueAndEntityTypeIn(captor.capture());
            assertThat(captor.getValue()).contains(
                    NotificationEntityType.ARCGIS_INTEGRATION,
                    NotificationEntityType.INTEGRATION_CONNECTION);
        }

        @Test
        @DisplayName("includes all entity types when user has both Jira and ArcGIS roles")
        void includes_all_types_when_has_both_roles() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(true);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(true);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.CONFLUENCE)).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(any()))
                    .thenReturn(List.of());

            notificationEventCatalogService.getAllEnabled(TENANT_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(Collection.class);
            org.mockito.Mockito.verify(eventCatalogRepository)
                    .findByIsEnabledTrueAndEntityTypeIn(captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrder(
                    NotificationEntityType.SITE_CONFIG,
                    NotificationEntityType.JIRA_WEBHOOK,
                    NotificationEntityType.ARCGIS_INTEGRATION,
                    NotificationEntityType.INTEGRATION_CONNECTION);
            assertThat(captor.getValue())
                    .doesNotContain(NotificationEntityType.CONFLUENCE_INTEGRATION);
        }

        @Test
        @DisplayName("includes CONFLUENCE_INTEGRATION and INTEGRATION_CONNECTION when user has Confluence role")
        void includes_confluence_types_when_has_confluence_role() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(false);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(false);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.CONFLUENCE)).thenReturn(true);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(any()))
                    .thenReturn(List.of());

            notificationEventCatalogService.getAllEnabled(TENANT_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(Collection.class);
            org.mockito.Mockito.verify(eventCatalogRepository)
                    .findByIsEnabledTrueAndEntityTypeIn(captor.capture());
            assertThat(captor.getValue()).contains(
                    NotificationEntityType.CONFLUENCE_INTEGRATION,
                    NotificationEntityType.INTEGRATION_CONNECTION,
                    NotificationEntityType.SITE_CONFIG);
            assertThat(captor.getValue())
                    .doesNotContain(NotificationEntityType.JIRA_WEBHOOK,
                            NotificationEntityType.ARCGIS_INTEGRATION);
        }

        @Test
        @DisplayName("includes all five types when user has all three feature roles")
        void includes_all_five_types_when_has_all_roles() {
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.JIRA)).thenReturn(true);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.ARCGIS)).thenReturn(true);
            when(serviceTypeAuth.hasAccessToServiceType(ServiceType.CONFLUENCE)).thenReturn(true);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(any()))
                    .thenReturn(List.of());

            notificationEventCatalogService.getAllEnabled(TENANT_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<NotificationEntityType>> captor =
                    ArgumentCaptor.forClass(Collection.class);
            org.mockito.Mockito.verify(eventCatalogRepository)
                    .findByIsEnabledTrueAndEntityTypeIn(captor.capture());
            assertThat(captor.getValue()).containsExactlyInAnyOrder(
                    NotificationEntityType.SITE_CONFIG,
                    NotificationEntityType.JIRA_WEBHOOK,
                    NotificationEntityType.ARCGIS_INTEGRATION,
                    NotificationEntityType.CONFLUENCE_INTEGRATION,
                    NotificationEntityType.INTEGRATION_CONNECTION);
        }

        @Test
        @DisplayName("maps catalog entries to responses")
        void maps_catalog_entries_to_responses() {
            NotificationEventCatalog catalog = buildCatalog();
            NotificationEventCatalogResponse response = buildCatalogResponse(catalog.getId());

            when(serviceTypeAuth.hasAccessToServiceType(any())).thenReturn(false);
            when(eventCatalogRepository.findByIsEnabledTrueAndEntityTypeIn(any()))
                    .thenReturn(List.of(catalog));
            when(notificationMapper.toEventCatalogResponse(catalog)).thenReturn(response);

            List<NotificationEventCatalogResponse> result =
                    notificationEventCatalogService.getAllEnabled(TENANT_ID);

            assertThat(result).containsExactly(response);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns response when catalog entry found")
        void returns_response_when_found() {
            UUID id = UUID.randomUUID();
            NotificationEventCatalog catalog = buildCatalog();
            NotificationEventCatalogResponse response = buildCatalogResponse(id);

            when(eventCatalogRepository.findById(id)).thenReturn(Optional.of(catalog));
            when(notificationMapper.toEventCatalogResponse(catalog)).thenReturn(response);

            NotificationEventCatalogResponse result =
                    notificationEventCatalogService.getById(id);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("throws EntityNotFoundException when not found")
        void throws_when_not_found() {
            UUID id = UUID.randomUUID();
            when(eventCatalogRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationEventCatalogService.getById(id))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    private NotificationEventCatalog buildCatalog() {
        return NotificationEventCatalog.builder()
                .id(UUID.randomUUID())
                .eventKey("SITE_CONFIG_UPDATED")
                .entityType(NotificationEntityType.SITE_CONFIG)
                .displayName("Site Config Updated")
                .isEnabled(true)
                .build();
    }

    private NotificationEventCatalogResponse buildCatalogResponse(UUID id) {
        return NotificationEventCatalogResponse.builder()
                .id(id)
                .eventKey("SITE_CONFIG_UPDATED")
                .entityType(String.valueOf(NotificationEntityType.SITE_CONFIG))
                .build();
    }
}
