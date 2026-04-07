package com.integration.management.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Constants coverage")
class ConstantsCoverageTest {

    @Test
    @DisplayName("constants should have expected values")
    void constants_haveExpectedValues() {
        assertThat(CacheConstants.ALL_CACHE).isEqualTo("allCaches");
        assertThat(CacheConstants.NOTIFICATION_EVENTS_BY_TENANT_CACHE).isEqualTo("notificationEventsByTenantCache");

        assertThat(ManagementSecurityConstants.GLOBAL).isEqualTo("GLOBAL");
        assertThat(ManagementSecurityConstants.SYSTEM_USER).isEqualTo("system");
        assertThat(ManagementSecurityConstants.ROLE_APP_ADMIN).isEqualTo("app_admin");

        assertThat(ReflectionConstants.METHOD_PREFIX_GET).isEqualTo("getSecret");
        assertThat(ReflectionConstants.METHOD_PREFIX_IS).isEqualTo("is");

        assertThat(IntegrationManagementConstants.STATUS_ENABLED).isEqualTo("ENABLED");
        assertThat(IntegrationManagementConstants.Security.GLOBAL).isEqualTo("GLOBAL");
        assertThat(IntegrationManagementConstants.Headers.TENANT_ID).isEqualTo("tenant_id");
    }

    @Test
    @DisplayName("utility constant classes should have private constructors")
    void constantClasses_havePrivateConstructors() throws Exception {
        assertPrivateCtorThrows(CacheConstants.class, "CacheConstants is a utility class");
        assertPrivateCtorThrows(ManagementSecurityConstants.class, "ManagementSecurityConstants is a utility class");
        assertPrivateCtorThrows(ReflectionConstants.class, "ReflectionConstants is a utility class");

        assertPrivateCtorInstantiable(IntegrationManagementConstants.class);
        assertPrivateCtorInstantiable(IntegrationManagementConstants.Security.class);
        assertPrivateCtorInstantiable(IntegrationManagementConstants.Headers.class);
    }

    private static void assertPrivateCtorThrows(Class<?> clazz, String messageContains) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasRootCauseInstanceOf(IllegalStateException.class)
            .rootCause()
            .hasMessageContaining(messageContains);
    }

    private static void assertPrivateCtorInstantiable(Class<?> clazz) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
