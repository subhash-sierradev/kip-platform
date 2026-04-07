package com.integration.management.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("AppConfig")
class AppConfigTest {

    private final AppConfig appConfig = new AppConfig();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("taskExecutor")
    class TaskExecutor {

        @Test
        @DisplayName("returns DelegatingSecurityContextAsyncTaskExecutor")
        void returns_delegating_security_context_executor() {
            Executor executor = appConfig.taskExecutor();

            assertThat(executor).isInstanceOf(DelegatingSecurityContextAsyncTaskExecutor.class);
        }
    }

    @Nested
    @DisplayName("objectMapper")
    class ObjectMapperTests {

        private ObjectMapper mapper;

        @BeforeEach
        void setUp() {
            mapper = appConfig.objectMapper();
        }

        @Test
        @DisplayName("serializes Instant as ISO-8601 string not as timestamp")
        void serializes_instant_as_iso_string() throws Exception {
            String json = mapper.writeValueAsString(Instant.parse("2025-01-01T00:00:00Z"));

            assertThat(json).isEqualTo("\"2025-01-01T00:00:00Z\"");
        }

        @Test
        @DisplayName("ignores unknown properties during deserialization")
        void ignores_unknown_properties() {
            assertThat(mapper.getDeserializationConfig()
                    .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
        }

        @Test
        @DisplayName("does not throw when JSON has extra unknown fields")
        void does_not_throw_on_unknown_fields_in_json() {
            String json = "{\"name\":\"Alice\",\"unexpectedField\":\"value\",\"anotherExtra\":42}";

            assertThatNoException().isThrownBy(() -> mapper.readValue(json, Map.class));
        }

        @Test
        @DisplayName("trims leading and trailing whitespace from string values")
        void trims_leading_and_trailing_whitespace() throws Exception {
            String json = "{\"name\":\"  hello world  \"}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertThat(result.get("name")).isEqualTo("hello world");
        }

        @Test
        @DisplayName("trims leading-only whitespace from string values")
        void trims_leading_whitespace() throws Exception {
            String json = "{\"value\":\"   leading\"}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertThat(result.get("value")).isEqualTo("leading");
        }

        @Test
        @DisplayName("trims trailing-only whitespace from string values")
        void trims_trailing_whitespace() throws Exception {
            String json = "{\"value\":\"trailing   \"}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertThat(result.get("value")).isEqualTo("trailing");
        }

        @Test
        @DisplayName("string with only spaces trims to empty string")
        void trims_whitespace_only_string_to_empty() throws Exception {
            String json = "{\"value\":\"     \"}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertThat(result.get("value")).isEqualTo("");
        }

        @Test
        @DisplayName("null JSON string value stays null after deserialization")
        void null_json_string_remains_null() throws Exception {
            String json = "{\"value\":null}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertThat(result.get("value")).isNull();
        }

        @Test
        @DisplayName("string without whitespace is returned unchanged")
        void string_without_whitespace_unchanged() throws Exception {
            String json = "{\"value\":\"noWhitespace\"}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertThat(result.get("value")).isEqualTo("noWhitespace");
        }

        @Test
        @DisplayName("non-string values are not affected by the string trimmer")
        void non_string_values_unaffected() throws Exception {
            String json = "{\"count\":42,\"active\":true,\"items\":[1,2,3]}";

            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);

            assertThat(result.get("count")).isEqualTo(42);
            assertThat(result.get("active")).isEqualTo(true);
            assertThat(result.get("items")).isEqualTo(List.of(1, 2, 3));
        }
    }

    @Nested
    @DisplayName("jwtTokenForwardingInterceptor")
    class JwtTokenForwardingInterceptorTests {

        private RequestInterceptor interceptor;
        private RequestTemplate requestTemplate;

        @BeforeEach
        void setUp() {
            interceptor = appConfig.jwtTokenForwardingInterceptor();
            requestTemplate = new RequestTemplate();
        }

        @Test
        @DisplayName("forwards JWT token as Authorization Bearer header")
        void forwards_jwt_as_bearer_header() {
            String tokenValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMSJ9.sig";
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(buildJwt(tokenValue)));

            interceptor.apply(requestTemplate);

            assertThat(requestTemplate.headers())
                .containsKey("Authorization");
            assertThat(requestTemplate.headers().get("Authorization"))
                .containsExactly("Bearer " + tokenValue);
        }

        @Test
        @DisplayName("does not add any header when security context has no authentication")
        void no_header_when_no_authentication() {
            SecurityContextHolder.clearContext();

            interceptor.apply(requestTemplate);

            assertThat(requestTemplate.headers())
                    .doesNotContainKey("Authorization");
        }

        @Test
        @DisplayName("does not add any header when authentication is not JwtAuthenticationToken")
        void no_header_when_non_jwt_authentication() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("user", "password"));

            interceptor.apply(requestTemplate);

            assertThat(requestTemplate.headers())
                .doesNotContainKey("Authorization");
        }

        @Test
        @DisplayName("forwards exact token value unchanged")
        void forwards_exact_token_value() {
            String tokenValue = "token.for.tenant-abc";
            SecurityContextHolder.getContext()
                    .setAuthentication(new JwtAuthenticationToken(buildJwt(tokenValue)));

            interceptor.apply(requestTemplate);

            assertThat(requestTemplate.headers())
                .containsKey("Authorization");
            assertThat(requestTemplate.headers().get("Authorization"))
                .containsExactly("Bearer token.for.tenant-abc");
        }

        private Jwt buildJwt(String tokenValue) {
            Map<String, Object> headers = new HashMap<>();
            headers.put("alg", "HS256");
            headers.put("typ", "JWT");

            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "testuser");
            claims.put("tenant_id", "tenant123");

            Instant issuedAt = Instant.now();
            return new Jwt(tokenValue, issuedAt, issuedAt.plusSeconds(3600), headers, claims);
        }
    }
}
