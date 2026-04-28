package com.integration.management.ies.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

// TODO KIP-547 REMOVE — Temporary Feign client annotation test
@DisplayName("IesArcGISVerificationClient")
class IesArcGISVerificationClientTest {

    @Test
    @DisplayName("FeignClient name is correct")
    void feignClient_nameIsCorrect() {
        FeignClient annotation = IesArcGISVerificationClient.class.getAnnotation(FeignClient.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("execution-arcgis-verification");
    }

    @Test
    @DisplayName("getFeatures has correct GET mapping")
    void getFeatures_hasCorrectGetMapping() {
        Method method = getMethod("getFeatures");
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(singlePath(mapping.value(), mapping.path()))
                .isEqualTo("/api/execution/arcgis/verification/features");
    }

    @Test
    @DisplayName("getFeatures offset param has default value 0")
    void getFeatures_offsetParam_hasDefaultZero() {
        Method method = getMethod("getFeatures");
        Parameter offsetParam = Arrays.stream(method.getParameters())
                .filter(p -> p.isAnnotationPresent(RequestParam.class))
                .filter(p -> {
                    RequestParam rp = p.getAnnotation(RequestParam.class);
                    return "offset".equals(rp.name()) || "offset".equals(rp.value());
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("offset param not found"));

        RequestParam rp = offsetParam.getAnnotation(RequestParam.class);
        assertThat(rp.defaultValue()).isEqualTo("0");
    }

    @Test
    @DisplayName("getFeatures objectId param is not required")
    void getFeatures_objectIdParam_isNotRequired() {
        assertQueryParamNotRequired("objectId");
    }

    @Test
    @DisplayName("getFeatures locationId param is not required")
    void getFeatures_locationIdParam_isNotRequired() {
        assertQueryParamNotRequired("locationId");
    }

    private static void assertQueryParamNotRequired(String paramName) {
        Method method = getMethod("getFeatures");
        Parameter param = Arrays.stream(method.getParameters())
                .filter(p -> p.isAnnotationPresent(RequestParam.class))
                .filter(p -> {
                    RequestParam rp = p.getAnnotation(RequestParam.class);
                    return paramName.equals(rp.name()) || paramName.equals(rp.value());
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(paramName + " param not found"));

        assertThat(param.getAnnotation(RequestParam.class).required()).isFalse();
    }

    private static Method getMethod(String methodName) {
        return Arrays.stream(IesArcGISVerificationClient.class.getMethods())
                .filter(m -> m.getName().equals(methodName))
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
