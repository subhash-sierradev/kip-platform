package com.integration.execution.controller.advice;

import com.integration.execution.exception.IntegrationApiException;
import com.integration.execution.exception.IntegrationExecutionException;
import com.integration.execution.exception.IntegrationNameAlreadyExistsException;
import com.integration.execution.exception.IntegrationNotFoundException;
import com.integration.execution.exception.EndpointValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecificExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    private SpecificExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SpecificExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/integrations/test");
    }

    @Test
    void handleIntegrationNotFoundException_returnsNotFoundWithContextMessage() {
        IntegrationNotFoundException ex = new IntegrationNotFoundException("integration missing");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleIntegrationNotFoundException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("integration missing");
        assertThat(response.getBody().path()).isEqualTo("/api/integrations/test");
    }

    @Test
    void handleSourceEndpointValidationException_returnsBadRequest() {
        EndpointValidationException ex = new EndpointValidationException("invalid endpoint");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleSourceEndpointValidationException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("invalid endpoint");
    }

    @Test
    void handleIntegrationNameAlreadyExistsException_returnsConflict() {
        IntegrationNameAlreadyExistsException ex = new IntegrationNameAlreadyExistsException("duplicate name");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleIntegrationNameAlreadyExistsException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("duplicate name");
    }

    @Test
    void handleIntegrationBaseException_returnsInternalServerError() {
        IntegrationExecutionException ex = new IntegrationExecutionException("base failure");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleIntegrationBaseException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("base failure");
    }

    @Test
    void handleValidationExceptions_bindException_returnsBadRequestWithFieldErrors() {
        BindException bindException = new BindException(new Object(), "request");
        bindException.addError(new FieldError("request", "serviceType", "must not be null"));

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleValidationExceptions(bindException, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().details()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().details();
        assertThat(details).containsEntry("serviceType", "must not be null");
    }

    @Test
    void handleValidationExceptions_methodArgumentNotValid_returnsBadRequestWithMergedFieldErrors() {
        BindException bindException = new BindException(new Object(), "request");
        bindException.addError(new FieldError("request", "fieldA", "first error"));
        bindException.addError(new FieldError("request", "fieldA", "second error"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindException.getBindingResult());

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleValidationExceptions(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().details();
        assertThat(details).containsEntry("fieldA", "first error");
    }

    @Test
    void handleConstraintViolationException_returnsBadRequestWithViolationDetails() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("payload.name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);
        ConstraintViolationException ex = new ConstraintViolationException("validation failed", violations);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleConstraintViolationException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details().toString()).contains("payload.name: must not be blank");
    }

    @Test
    void handleMissingRequestHeaderException_returnsBadRequest() {
        MethodParameter methodParameter = createMethodParameter();
        MissingRequestHeaderException ex = new MissingRequestHeaderException("X-Tenant-Id", methodParameter);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleMissingRequestHeaderException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("X-Tenant-Id");
    }

    @Test
    void handleMethodArgumentTypeMismatchException_formatsReadableErrorMessage() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc",
                Integer.class,
                "startAt",
                null,
                null
        );

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleMethodArgumentTypeMismatchException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("Invalid value 'abc' for parameter 'startAt'");
    }

    @Test
    void handleArcGisApiException_invalidStatusCode_fallsBackToInternalServerError() {
        IntegrationApiException ex = new IntegrationApiException("upstream error", 799);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleArcGisApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("upstream error");
    }

    @Test
    void handleInvalidEnum_returnsBadRequestWithErrorCodeDetails() {
        IllegalArgumentException ex = new IllegalArgumentException("No enum constant");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleInvalidEnum(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details()).isEqualTo(Map.of("error", "BAD_REQUEST"));
    }

    @Test
    void handleMissingServletRequestParameterException_returnsBadRequest() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("tenantId", "String");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleMissingServletRequestParameterException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("tenantId");
    }

    @Test
    void handleMissingPathVariableException_returnsBadRequest() {
        MethodParameter methodParameter = createMethodParameter();
        MissingPathVariableException ex = new MissingPathVariableException("id", methodParameter);

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleMissingPathVariableException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("id");
    }

    @Test
    void handleHttpMessageNotReadableException_returnsBadRequest() {
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("Malformed JSON request", mock(HttpInputMessage.class));

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleHttpMessageNotReadableException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Malformed JSON request");
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex =
            new HttpRequestMethodNotSupportedException("TRACE", Collections.singleton("GET"));

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
            handler.handleHttpRequestMethodNotSupportedException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("TRACE");
    }

    @Test
    void handleHttpMediaTypeNotSupportedException_returnsUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex =
            new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_XML, Collections.singletonList(MediaType.APPLICATION_JSON));

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
            handler.handleHttpMediaTypeNotSupportedException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("application/xml");
    }

    @Test
    void handleNoHandlerFoundException_returnsNotFound() throws Exception {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/missing", new HttpHeaders());

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
            handler.handleNoHandlerFoundException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("No handler found for GET /missing");
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleAccessDenied(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    void handleArcGisApiException_validStatusCode_returnsMappedStatus() {
        IntegrationApiException ex = new IntegrationApiException("Unauthorized", HttpStatus.UNAUTHORIZED.value());

        ResponseEntity<SpecificExceptionHandler.ErrorResponse> response =
                handler.handleArcGisApiException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Unauthorized");
    }

    private MethodParameter createMethodParameter() {
        try {
            Method method = SpecificExceptionHandlerTest.class
                    .getDeclaredMethod("sampleMethodParameter", String.class);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private void sampleMethodParameter(String value) {
    }
}
