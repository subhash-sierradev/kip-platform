package com.integration.execution.controller.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    private GenericExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GenericExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/runtime-test");
    }

    @Test
    void handleRuntimeException_returnsInternalServerErrorResponse() {
        RuntimeException ex = new RuntimeException("runtime failure");

        ResponseEntity<GenericExceptionHandler.ErrorResponse> response =
                handler.handleRuntimeException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("runtime failure");
        assertThat(response.getBody().path()).isEqualTo("/api/runtime-test");
    }

    @Test
    void handleGenericException_returnsInternalServerErrorResponse() {
        Exception ex = new Exception("unexpected failure");

        ResponseEntity<GenericExceptionHandler.ErrorResponse> response =
                handler.handleGenericException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("unexpected failure");
    }
}
