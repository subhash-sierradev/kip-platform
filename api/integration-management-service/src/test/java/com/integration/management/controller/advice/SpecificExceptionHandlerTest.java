package com.integration.management.controller.advice;

import com.integration.management.exception.EndpointValidationException;
import com.integration.management.exception.IntegrationApiException;
import com.integration.management.exception.IntegrationConnectionDeleteConflictException;
import com.integration.management.exception.IntegrationNameAlreadyExistsException;
import com.integration.management.exception.IntegrationNotFoundException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SpecificExceptionHandler")
class SpecificExceptionHandlerTest {

    private final SpecificExceptionHandler handler = new SpecificExceptionHandler();

    @Test
    @DisplayName("handleValidationExceptions supports MethodArgumentNotValidException branch")
    void handleValidationExceptions_methodArgumentNotValid_branch() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "req");
        bindingResult.addError(new FieldError("req", "name", "must not be blank"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler.handleValidationExceptions(ex,
                request("/api/validate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().path()).isEqualTo("/api/validate");
        assertThat(response.getBody().details().toString()).contains("name");
    }

    @Test
    @DisplayName("handleValidationExceptions supports BindException branch and field error fallbacks")
    void handleValidationExceptions_bindException_branchWithFallbacks() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "req");
        bindingResult.addError(new FieldError("req", "name", "first"));
        bindingResult.addError(new FieldError("req", "name", "second"));
        bindingResult.addError(new FieldError("req", "description", null));
        BindException ex = new BindException(bindingResult);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler.handleValidationExceptions(ex,
                request("/api/bind"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().details().toString()).contains("name=first");
        assertThat(response.getBody().details().toString()).contains("description=Invalid value");
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatchException uses required type simple name when present")
    void handleMethodArgumentTypeMismatchException_requiredTypePresent() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", Integer.class, "limit",
                null, null);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleMethodArgumentTypeMismatchException(ex, request("/api/type"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Expected type: Integer");
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatchException uses Unknown when required type is null")
    void handleMethodArgumentTypeMismatchException_requiredTypeNull() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", null, "limit", null,
                null);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleMethodArgumentTypeMismatchException(ex, request("/api/type"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Expected type: Unknown");
    }

    @Test
    @DisplayName("handleArcGisApiException returns mapped status when valid status code is provided")
    void handleArcGisApiException_validStatus() {
        IntegrationApiException ex = new IntegrationApiException("downstream bad request", 400);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler.handleArcGisApiException(ex,
                request("/api/arcgis"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTEGRATION_API_EXCEPTION");
    }

    @Test
    @DisplayName("handleArcGisApiException falls back to 500 when status code is unknown")
    void handleArcGisApiException_unknownStatusFallsBackTo500() {
        IntegrationApiException ex = new IntegrationApiException("downstream unknown", 799);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler.handleArcGisApiException(ex,
                request("/api/arcgis"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTEGRATION_API_EXCEPTION");
    }

    @Test
    @DisplayName("handleIntegrationNotFoundException returns 404 with integration code")
    void handleIntegrationNotFoundException_returns404() {
        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleIntegrationNotFoundException(new IntegrationNotFoundException("not found"),
                        request("/api/integrations/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTEGRATION_NOT_FOUND");
    }

    @Test
    @DisplayName("handleSourceEndpointValidationException returns 400")
    void handleSourceEndpointValidationException_returns400() {
        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleSourceEndpointValidationException(new EndpointValidationException("bad endpoint"),
                        request("/api/endpoint"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("handleIntegrationNameAlreadyExistsException returns 409")
    void handleIntegrationNameAlreadyExistsException_returns409() {
        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleIntegrationNameAlreadyExistsException(
                        new IntegrationNameAlreadyExistsException("already exists"),
                        request("/api/integrations"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTEGRATION_NAME_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("handleIntegrationConnectionDeleteConflictException returns 409 with details")
    void handleIntegrationConnectionDeleteConflictException_returns409() {
        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleIntegrationConnectionDeleteConflictException(
                        new IntegrationConnectionDeleteConflictException("conflict", "details"),
                        request("/api/connections/1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("INTEGRATION_CONNECTION_DELETE_CONFLICT");
        assertThat(response.getBody().details()).isEqualTo("details");
    }

    @Test
    @DisplayName("handleMissingRequestHeaderException returns 400")
    void handleMissingRequestHeaderException_returns400() throws NoSuchMethodException {
        MissingRequestHeaderException ex = new MissingRequestHeaderException("x-tenant-id", methodParameter());

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleMissingRequestHeaderException(ex, request("/api/header"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_REQUEST_HEADER");
    }

    @Test
    @DisplayName("handleMissingServletRequestParameterException returns 400")
    void handleMissingServletRequestParameterException_returns400() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("page", "int");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleMissingServletRequestParameterException(ex, request("/api/query"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_REQUEST_PARAMETER");
    }

    @Test
    @DisplayName("handleMissingPathVariableException returns 400")
    void handleMissingPathVariableException_returns400() throws NoSuchMethodException {
        MissingPathVariableException ex = new MissingPathVariableException("id", methodParameter());

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleMissingPathVariableException(ex, request("/api/path"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("MISSING_PATH_VARIABLE");
    }

    @Test
    @DisplayName("handleHttpRequestMethodNotSupportedException returns 405")
    void handleHttpRequestMethodNotSupportedException_returns405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleHttpRequestMethodNotSupportedException(ex, request("/api/method"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    @DisplayName("handleHttpMediaTypeNotSupportedException returns 415")
    void handleHttpMediaTypeNotSupportedException_returns415() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleHttpMediaTypeNotSupportedException(ex, request("/api/media"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    @DisplayName("handleNoHandlerFoundException returns 404")
    void handleNoHandlerFoundException_returns404() {
        NoHandlerFoundException ex = new NoHandlerFoundException(
                "GET",
                "/missing",
                org.springframework.http.HttpHeaders.EMPTY);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleNoHandlerFoundException(ex, request("/missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("handleAccessDenied returns 403")
    void handleAccessDenied_returns403() {
        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleAccessDenied(new AccessDeniedException("denied"), request("/api/secure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("handleInvalidEnum returns 400 with error details")
    void handleInvalidEnum_returns400() {
        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response = handler
                .handleInvalidEnum(new IllegalArgumentException("bad enum"), request("/api/enum"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().details().toString()).contains("BAD_REQUEST");
    }

    private static WebRequest request(String path) {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=" + path);
        return request;
    }

    // keeps reflective method metadata available for
    // MethodArgumentNotValidException if needed later
    @SuppressWarnings("unused")
    private void sampleMethod(@NotBlank String value) {
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("sampleMethod", String.class);
        return new MethodParameter(method, 0);
    }
}
