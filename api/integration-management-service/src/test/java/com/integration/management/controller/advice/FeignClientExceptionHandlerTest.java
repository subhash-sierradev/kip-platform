package com.integration.management.controller.advice;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeignClientExceptionHandler")
class FeignClientExceptionHandlerTest {

    private final FeignClientExceptionHandler handler = new FeignClientExceptionHandler();

    @Test
    @DisplayName("handleFeignException returns 503 for network failures (-1)")
    void handleFeignException_networkFailure_returns503() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(-1);
        when(ex.getMessage()).thenReturn("I/O error http://internal.service.local");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/management/connections");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(out.getBody().errorCode()).isEqualTo("EXTERNAL_SERVICE_UNAVAILABLE");
        assertThat(out.getBody().message()).contains("temporarily unavailable");
        assertThat(out.getBody().path()).isEqualTo("/api/management/connections");
    }

    @Test
    @DisplayName("handleFeignException returns 503 for downstream 5xx")
    void handleFeignException_serverError_returns503() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(500);
        when(ex.getMessage()).thenReturn("500 http://internal.service.local");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/x");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().errorCode()).isEqualTo("EXTERNAL_SERVICE_UNAVAILABLE");
        assertThat(out.getBody().path()).isEqualTo("/api/x");
    }

    @Test
    @DisplayName("handleFeignException extracts JSON message and redacts URLs")
    void handleFeignException_extractsMessage_andRedactsUrl() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(400);
        when(ex.contentUTF8()).thenReturn("{\"message\":\"Downstream failed at https://secret.example/a?b=c\"}");
        when(ex.getMessage()).thenReturn("400 https://secret.example");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/y");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().errorCode()).isEqualTo("DOWNSTREAM_SERVICE_ERROR");
        assertThat(out.getBody().message()).doesNotContain("secret.example");
        assertThat(out.getBody().message()).contains("[redacted]");
        assertThat(out.getBody().path()).isEqualTo("/api/y");
    }

    @Test
    @DisplayName("handleFeignException falls back when response body is empty")
    void handleFeignException_emptyBody_fallsBackToGenericStatusMessage() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(404);
        when(ex.contentUTF8()).thenReturn("");
        when(ex.getMessage()).thenReturn("404");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/not-found");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().message()).contains("status 404");
        assertThat(out.getBody().errorCode()).isEqualTo("DOWNSTREAM_SERVICE_ERROR");
    }

    @Test
    @DisplayName("handleFeignException uses 502 for non-standard HTTP statuses")
    void handleFeignException_unknownStatus_usesBadGateway() {
        FeignException ex = mock(FeignException.class);
        // Use an unrecognized non-5xx code so we don't get classified as 'service
        // unavailable'
        // (5xx is intentionally mapped to 503 by the handler).
        when(ex.status()).thenReturn(499);
        when(ex.contentUTF8()).thenReturn("{\"message\":\"oops\"}");
        when(ex.getMessage()).thenReturn("499");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/z");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().errorCode()).isEqualTo("DOWNSTREAM_SERVICE_ERROR");
    }

    @Test
    @DisplayName("handleFeignException falls back when JSON parsing fails")
    void handleFeignException_invalidJson_fallsBack() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(400);
        when(ex.contentUTF8()).thenReturn("{not-json");
        when(ex.getMessage()).thenReturn("400");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/bad");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().message()).contains("status 400");
    }

    @Test
    @DisplayName("handleFeignException falls back when JSON body has no 'message' key")
    void handleFeignException_jsonBodyMissingMessageKey_fallsBack() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(422);
        when(ex.contentUTF8()).thenReturn("{\"error\":\"validation failed\"}");
        when(ex.getMessage()).thenReturn("422");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/validate");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode().value()).isEqualTo(422);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().message()).contains("422");
    }

    @Test
    @DisplayName("handleFeignException handles null message in sanitize path")
    void handleFeignException_nullMessageInBody_usesDefault() {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(403);
        when(ex.contentUTF8()).thenReturn("{\"message\":null}");
        when(ex.getMessage()).thenReturn("403");

        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/auth");

        ResponseEntity<FeignClientExceptionHandler.ErrorResponse> out = handler.handleFeignException(ex, request);

        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(out.getBody()).isNotNull();
        assertThat(out.getBody().message()).contains("status 403");
    }
}
