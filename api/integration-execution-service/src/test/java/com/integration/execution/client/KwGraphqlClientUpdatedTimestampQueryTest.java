package com.integration.execution.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: ensure updated timestamp filters stay present in the KW searchWithData queries.
 *
 * Why this matters:
 * - Without startUpdatedTimestamp/endUpdatedTimestamp, the integration may miss documents that were
 *   created earlier but updated inside the execution window.
 */
class KwGraphqlClientUpdatedTimestampQueryTest {

    @Test
    void searchDocumentsQuery_containsUpdatedTimestampFilters() throws Exception {
        String query = (String) getPrivateStaticField(KwGraphqlClient.class, "SEARCH_DOCUMENTS_WITH_LOCATIONS_QUERY");

        assertThat(query)
                .contains("startUpdatedTimestamp: $startTs")
                .contains("endUpdatedTimestamp: $endTs");
    }

    @Test
    void searchDynamicDocumentsQuery_containsUpdatedTimestampFilters() throws Exception {
        String query = (String) getPrivateStaticField(KwGraphqlClient.class, "FETCH_DYNAMIC_DOCUMENTS_WITH_LOCATIONS_QUERY");

        assertThat(query)
                .contains("$startUpdatedTimestamp")
                .contains("$endUpdatedTimestamp")
                .contains("$start: Int")
                .contains("$limit: Int")
                .contains("start: $start")
                .contains("limit: $limit");
    }

    private static Object getPrivateStaticField(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }
}

