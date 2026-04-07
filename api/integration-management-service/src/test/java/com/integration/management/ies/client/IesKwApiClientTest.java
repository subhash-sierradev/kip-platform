package com.integration.management.ies.client;

import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class IesKwApiClientTest {

    @Test
    void feign_client_name_is_correct() {
        FeignClient feignClient = IesKwApiClient.class.getAnnotation(FeignClient.class);
        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("execution-kw-doc");
    }

    @Test
    void get_dynamic_documents_has_mapping_and_cacheable() {
        Method method = getMethodByNameAndParamCount("getDynamicDocuments", 2);

        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(singlePath(mapping.value(), mapping.path())).isEqualTo("/api/dynamic-documents-types");

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable).isNotNull();
        assertThat(singleCacheName(cacheable.value(), cacheable.cacheNames())).isEqualTo("kwDynamicDocTypeCache");
        assertThat(cacheable.key()).isEqualTo("#type + ':' + #subType");
    }

    @Test
    void get_item_subtypes_has_mapping_and_cacheable() {
        Method method = getMethodByNameAndParamCount("getItemSubtypes", 0);

        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(singlePath(mapping.value(), mapping.path())).isEqualTo("/api/item-subtypes");

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable).isNotNull();
        assertThat(singleCacheName(cacheable.value(), cacheable.cacheNames())).isEqualTo("kwItemSubtypesCache");
        assertThat(cacheable.key()).isBlank();
    }

    @Test
    void get_source_field_mappings_has_mapping_and_cacheable() {
        Method method = getMethodByNameAndParamCount("getSourceFieldMappings", 0);

        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(singlePath(mapping.value(), mapping.path())).isEqualTo("/api/source-field-mappings");

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable).isNotNull();
        assertThat(singleCacheName(cacheable.value(), cacheable.cacheNames())).isEqualTo("kwDocFieldsCache");
        assertThat(cacheable.key()).isBlank();
    }

    private static Method getMethodByNameAndParamCount(String methodName, int parameterCount) {
        return Arrays.stream(IesKwApiClient.class.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> method.getParameterCount() == parameterCount)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Method not found: " + methodName));
    }

    private static String singlePath(String[] value, String[] path) {
        if (value != null && value.length > 0) {
            assertThat(value).hasSize(1);
            return value[0];
        }

        assertThat(path).hasSize(1);
        return path[0];
    }

    private static String singleCacheName(String[] value, String[] cacheNames) {
        if (value != null && value.length > 0) {
            assertThat(value).hasSize(1);
            return value[0];
        }

        assertThat(cacheNames).hasSize(1);
        return cacheNames[0];
    }
}
