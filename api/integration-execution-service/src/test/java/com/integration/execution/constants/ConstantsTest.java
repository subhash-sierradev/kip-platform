package com.integration.execution.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstantsTest {

    @Test
    void constants_haveExpectedValues() {
        assertThat(ArcGisConstants.ARCGIS_FIELD_OBJECTID).isEqualTo("OBJECTID");
        assertThat(ArcGisConstants.ARCGIS_FIELD_EXTERNAL_LOCATION_ID).isEqualTo("external_location_id");

        assertThat(ExecutionSecurityConstants.X_TENANT_ID).isEqualTo("tenant_id");
        assertThat(ExecutionSecurityConstants.X_USER_ID).isEqualTo("preferred_username");
        assertThat(ExecutionSecurityConstants.GLOBAL).isEqualTo("GLOBAL");
        assertThat(ExecutionSecurityConstants.SYSTEM_USER).isEqualTo("system");

        assertThat(HttpConstants.AUTHORIZATION).isEqualTo("Authorization");
        assertThat(HttpConstants.BEARER).isEqualTo("Bearer ");
        assertThat(HttpConstants.BASIC).isEqualTo("Basic ");
        assertThat(HttpConstants.JSON_CONTENT_TYPE).isEqualTo("application/json");

        assertThat(KasewareConstants.KW_GRAPHQL_TOKEN_CACHE_KEY).isEqualTo("kw_graphql_token");
        assertThat(KasewareConstants.DOCUMENT_DRAFT_PREFIX).isEqualTo("DOCUMENT_DRAFT_");
    }

    @Test
    void utilityClassConstructors_throwIllegalStateException() throws Exception {
        assertUtilityConstructorThrows(ArcGisConstants.class, "ArcGisConstants is a utility class and cannot be instantiated");
        assertUtilityConstructorThrows(
                ExecutionSecurityConstants.class,
                "ExecutionSecurityConstants is a utility class and cannot be instantiated"
        );
        assertUtilityConstructorThrows(HttpConstants.class, "HttpConstants is a utility class and cannot be instantiated");
        assertUtilityConstructorThrows(KasewareConstants.class, "KasewareConstants is a utility class and cannot be instantiated");
    }

    private void assertUtilityConstructorThrows(Class<?> type, String expectedMessage) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(IllegalStateException.class)
                .extracting(Throwable::getMessage)
                .isEqualTo(expectedMessage);
    }
}
