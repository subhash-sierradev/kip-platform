package com.integration.management.ies.client;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class IesConnectionClientTest {

    @Test
    void feign_client_name_is_correct() {
        FeignClient feignClient = IesConnectionClient.class.getAnnotation(FeignClient.class);
        assertThat(feignClient).isNotNull();
        assertThat(feignClient.name()).isEqualTo("execution-integration-connection");
    }

    @Test
    void mappings_are_correct() {
        assertPostMapping("testAndCreateConnection", "/api/integrations/connections/test-connection");
        assertPostMapping("testExistingConnection", "/api/integrations/connections/{connectionId}/test");
    }

    private static void assertPostMapping(String methodName, String expectedPath) {
        Method method = getMethodByName(methodName);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(singlePath(mapping.value(), mapping.path())).isEqualTo(expectedPath);
    }

    private static Method getMethodByName(String methodName) {
        return Arrays.stream(IesConnectionClient.class.getMethods())
                .filter(method -> method.getName().equals(methodName))
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
}
