package com.integration.management.controller.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GenericExceptionHandler")
class GenericExceptionHandlerTest {

    private final GenericExceptionHandler handler = new GenericExceptionHandler();

    @Test
    @DisplayName("handleRuntimeException returns 500 generic payload")
    void handleRuntimeException_returns500() {
        ResponseEntity<GenericExceptionHandler.ErrorResponse> response = handler
                .handleRuntimeException(new RuntimeException("boom"), request("/api/runtime"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().path()).isEqualTo("/api/runtime");
    }

    @Test
    @DisplayName("handleResponseStatusException uses explicit reason")
    void handleResponseStatusException_withReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad input");

        ResponseEntity<GenericExceptionHandler.ErrorResponse> response = handler
                .handleResponseStatusException(ex, request("/api/status"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("RESPONSE_STATUS_EXCEPTION");
        assertThat(response.getBody().message()).isEqualTo("bad input");
    }

    @Test
    @DisplayName("handleResponseStatusException falls back to exception message when reason is null")
    void handleResponseStatusException_withoutReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND);

        ResponseEntity<GenericExceptionHandler.ErrorResponse> response = handler
                .handleResponseStatusException(ex, request("/api/status"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("RESPONSE_STATUS_EXCEPTION");
        assertThat(response.getBody().message()).contains("404");
    }

    @Test
    @DisplayName("handleGenericException returns generic 500 payload")
    void handleGenericException_returns500() {
        ResponseEntity<GenericExceptionHandler.ErrorResponse> response = handler
                .handleGenericException(new Exception("checked"), request("/api/generic"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).contains("unexpected error occurred");
    }

    @Test
    @DisplayName("handleAsyncRequestNotUsable executes without throwing")
    void handleAsyncRequestNotUsable_noThrow() {
        handler.handleAsyncRequestNotUsable(new AsyncRequestNotUsableException("client disconnected"));
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("handleResponseStatusException falls back to 500 when HttpStatus.resolve returns null")
    void handleResponseStatusException_unknownStatusCode_fallsBackTo500() {
        // Use a custom status code that HttpStatus.resolve cannot map (no real standard value at 999)
        org.springframework.http.HttpStatusCode unknownCode = org.springframework.http.HttpStatusCode.valueOf(999);
        ResponseStatusException ex = new ResponseStatusException(unknownCode, "unknown error");

        ResponseEntity<GenericExceptionHandler.ErrorResponse> response = handler
                .handleResponseStatusException(ex, request("/api/status"));

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("RESPONSE_STATUS_EXCEPTION");
    }

    private static WebRequest request(String path) {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=" + path);
        return request;
    }
}

