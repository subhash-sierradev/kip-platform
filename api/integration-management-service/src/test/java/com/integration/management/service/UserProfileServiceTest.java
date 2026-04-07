package com.integration.management.service;

import com.integration.management.entity.TenantProfile;
import com.integration.management.entity.UserProfile;
import com.integration.management.exception.IntegrationNotFoundException;
import com.integration.management.model.dto.response.UserProfileResponse;
import com.integration.management.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    private static final String KEYCLOAK_USER_ID = "kc-1";
    private static final String TENANT_ID = "tenant-1";
    private static final String TENANT_NAME = "Tenant One";
    private static final String EMAIL = "user@example.com";
    private static final String DISPLAY_NAME = "Test User";

    @Mock
    private TenantProfileService tenantProfileService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Nested
    @DisplayName("getAllUsersByTenant")
    class GetAllUsersByTenant {

        @Test
        void returns_mapped_dtos_for_all_tenant_users() {
            UserProfile user = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .email(EMAIL)
                    .displayName(DISPLAY_NAME)
                    .isTenantAdmin(true)
                    .build();

            when(userProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(user));

            List<UserProfileResponse> result = userProfileService.getAllUsersByTenant(TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getKeycloakUserId()).isEqualTo(KEYCLOAK_USER_ID);
            assertThat(result.get(0).getEmail()).isEqualTo(EMAIL);
            assertThat(result.get(0).getDisplayName()).isEqualTo(DISPLAY_NAME);
            assertThat(result.get(0).getIsTenantAdmin()).isTrue();
        }

        @Test
        void returns_empty_list_for_tenant_with_no_users() {
            when(userProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            List<UserProfileResponse> result = userProfileService.getAllUsersByTenant(TENANT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserProfileMapByTenant")
    class GetUserProfileMapByTenant {

        @Test
        void returns_map_keyed_by_keycloak_user_id() {
            UserProfile user = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .email(EMAIL)
                    .displayName(DISPLAY_NAME)
                    .build();

            when(userProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(user));

            Map<String, UserProfileResponse> result =
                    userProfileService.getUserProfileMapByTenant(TENANT_ID);

            assertThat(result).containsKey(KEYCLOAK_USER_ID);
            assertThat(result.get(KEYCLOAK_USER_ID).getEmail()).isEqualTo(EMAIL);
        }

        @Test
        void returns_empty_map_for_tenant_with_no_users() {
            when(userProfileRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            Map<String, UserProfileResponse> result =
                    userProfileService.getUserProfileMapByTenant(TENANT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserByKeycloakId")
    class GetUserByKeycloakId {

        @Test
        void returns_user_when_found() {
            UserProfile expected = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(expected));

            UserProfile result = userProfileService.getUserByKeycloakId(KEYCLOAK_USER_ID, TENANT_ID);

            assertThat(result).isSameAs(expected);
        }

        @Test
        void throws_not_found_exception_when_user_missing() {
            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> userProfileService.getUserByKeycloakId(KEYCLOAK_USER_ID, TENANT_ID))
                    .isInstanceOf(IntegrationNotFoundException.class)
                    .hasMessageContaining(KEYCLOAK_USER_ID)
                    .hasMessageContaining(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("syncUser — existing user")
    class SyncUserExisting {

        @Test
        void updates_all_fields_and_saves() {
            UserProfile existing = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .email("old@example.com")
                    .displayName("Old Name")
                    .isTenantAdmin(false)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(userProfileRepository.save(existing)).thenReturn(existing);

            UserProfile result = userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, DISPLAY_NAME, TENANT_ID, TENANT_NAME, true);

            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getDisplayName()).isEqualTo(DISPLAY_NAME);
            assertThat(result.isTenantAdmin()).isTrue();
            verify(userProfileRepository).save(existing);
            verify(tenantProfileService, never()).getOrCreateTenantProfile(any(), any());
        }

        @Test
        void still_saves_when_no_field_changes() {
            UserProfile existing = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .email(EMAIL)
                    .displayName(DISPLAY_NAME)
                    .isTenantAdmin(false)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(userProfileRepository.save(existing)).thenReturn(existing);

            UserProfile result = userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, DISPLAY_NAME, TENANT_ID, TENANT_NAME, false);

            assertThat(result).isSameAs(existing);
            // syncExistingUser has no dirty-check — save is always called
            verify(userProfileRepository).save(existing);
        }

        @Test
        void promotes_user_to_tenant_admin() {
            UserProfile existing = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .email(EMAIL)
                    .isTenantAdmin(false)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(userProfileRepository.save(existing)).thenReturn(existing);

            UserProfile result = userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, DISPLAY_NAME, TENANT_ID, TENANT_NAME, true);

            assertThat(result.isTenantAdmin()).isTrue();
            verify(userProfileRepository).save(existing);
        }

        @Test
        void demotes_user_from_tenant_admin() {
            UserProfile existing = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .email(EMAIL)
                    .isTenantAdmin(true)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(userProfileRepository.save(existing)).thenReturn(existing);

            UserProfile result = userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, DISPLAY_NAME, TENANT_ID, TENANT_NAME, false);

            assertThat(result.isTenantAdmin()).isFalse();
            verify(userProfileRepository).save(existing);
        }
    }

    @Nested
    @DisplayName("syncUser — new user")
    class SyncUserNew {

        @Test
        void creates_user_with_tenant_profile() {
            TenantProfile tenantProfile = TenantProfile.builder()
                    .tenantId(TENANT_ID)
                    .tenantName(TENANT_NAME)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(tenantProfileService.getOrCreateTenantProfile(TENANT_ID, TENANT_NAME))
                    .thenReturn(tenantProfile);
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0, UserProfile.class));

            UserProfile result = userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, DISPLAY_NAME, TENANT_ID, TENANT_NAME, true);

            assertThat(result.getKeycloakUserId()).isEqualTo(KEYCLOAK_USER_ID);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getDisplayName()).isEqualTo(DISPLAY_NAME);
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.isTenantAdmin()).isTrue();
            verify(tenantProfileService).getOrCreateTenantProfile(TENANT_ID, TENANT_NAME);
            verify(userProfileRepository).save(any(UserProfile.class));
        }

        @Test
        void creates_user_with_null_display_name() {
            TenantProfile tenantProfile = TenantProfile.builder()
                    .tenantId(TENANT_ID)
                    .tenantName(TENANT_NAME)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(tenantProfileService.getOrCreateTenantProfile(TENANT_ID, TENANT_NAME))
                    .thenReturn(tenantProfile);
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenAnswer(inv -> inv.getArgument(0, UserProfile.class));

            UserProfile result = userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, null, TENANT_ID, TENANT_NAME, false);

            assertThat(result.getDisplayName()).isNull();
            verify(userProfileRepository).save(any(UserProfile.class));
        }

        @Test
        void recovers_from_race_condition_on_duplicate_save() {
            TenantProfile tenantProfile = TenantProfile.builder()
                    .tenantId(TENANT_ID)
                    .tenantName(TENANT_NAME)
                    .build();
            UserProfile existing = UserProfile.builder()
                    .keycloakUserId(KEYCLOAK_USER_ID)
                    .tenantId(TENANT_ID)
                    .email(EMAIL)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existing));
            when(tenantProfileService.getOrCreateTenantProfile(TENANT_ID, TENANT_NAME))
                    .thenReturn(tenantProfile);
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            UserProfile result = userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, DISPLAY_NAME, TENANT_ID, TENANT_NAME, false);

            assertThat(result).isSameAs(existing);
        }

        @Test
        void throws_runtime_exception_when_race_condition_fallback_also_fails() {
            TenantProfile tenantProfile = TenantProfile.builder()
                    .tenantId(TENANT_ID)
                    .tenantName(TENANT_NAME)
                    .build();

            when(userProfileRepository.findByKeycloakUserIdAndTenantId(KEYCLOAK_USER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(tenantProfileService.getOrCreateTenantProfile(TENANT_ID, TENANT_NAME))
                    .thenReturn(tenantProfile);
            when(userProfileRepository.save(any(UserProfile.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            assertThatThrownBy(() -> userProfileService.syncUser(
                    KEYCLOAK_USER_ID, EMAIL, DISPLAY_NAME, TENANT_ID, TENANT_NAME, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(KEYCLOAK_USER_ID);
        }
    }
}
