package com.integration.management.controller.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("DatabaseExceptionHandler")
class DatabaseExceptionHandlerTest {

    private final DatabaseExceptionHandler handler = new DatabaseExceptionHandler();

    @Test
    @DisplayName("should return 409 with foreign key constraint message")
    void handleDataIntegrityViolation_foreignKeyConstraint_returnsConflictWithDetails() {
        WebRequest request = mockRequestPath("/api/test");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violates foreign key constraint fk_123");

        ResponseEntity<DatabaseExceptionHandler.ErrorResponse> response = handler.handleDataIntegrityViolation(ex,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().errorCode()).isEqualTo("DATA_INTEGRITY_VIOLATION");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().message())
                .isEqualTo("Cannot perform operation due to related data constraints");
        assertThat(response.getBody().details())
                .isEqualTo("Referenced data exists that prevents this operation");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("should return 409 with unique constraint message")
    void handleDataIntegrityViolation_uniqueConstraint_returnsConflictWithDetails() {
        WebRequest request = mockRequestPath("/api/test");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violates unique constraint uq_abc");

        ResponseEntity<DatabaseExceptionHandler.ErrorResponse> response = handler.handleDataIntegrityViolation(ex,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Duplicate entry - record with this information already exists");
        assertThat(response.getBody().details())
                .isEqualTo("A record with similar data already exists in the system");
    }

    @Test
    @DisplayName("should return 409 with duplicate key message")
    void handleDataIntegrityViolation_duplicateKey_returnsConflictWithDetails() {
        WebRequest request = mockRequestPath("/api/test");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint");

        ResponseEntity<DatabaseExceptionHandler.ErrorResponse> response = handler.handleDataIntegrityViolation(ex,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Duplicate entry - record with this information already exists");
        assertThat(response.getBody().details())
                .isEqualTo("A record with similar data already exists in the system");
    }

    @Test
    @DisplayName("should return 409 with not-null constraint message")
    void handleDataIntegrityViolation_notNullConstraint_returnsConflictWithDetails() {
        WebRequest request = mockRequestPath("/api/test");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violates not-null constraint");

        ResponseEntity<DatabaseExceptionHandler.ErrorResponse> response = handler.handleDataIntegrityViolation(ex,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Required field is missing");
        assertThat(response.getBody().details()).isEqualTo("All mandatory fields must be provided");
    }

    @Test
    @DisplayName("should return 409 with check constraint message")
    void handleDataIntegrityViolation_checkConstraint_returnsConflictWithDetails() {
        WebRequest request = mockRequestPath("/api/test");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violates check constraint");

        ResponseEntity<DatabaseExceptionHandler.ErrorResponse> response = handler.handleDataIntegrityViolation(ex,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Data validation failed");
        assertThat(response.getBody().details()).isEqualTo("The provided data does not meet system requirements");
    }

    @Test
    @DisplayName("should return generic 409 response when exception message is null")
    void handleDataIntegrityViolation_nullMessage_returnsGenericConflict() {
        WebRequest request = mockRequestPath("/api/test");
        DataIntegrityViolationException ex = Mockito.mock(DataIntegrityViolationException.class);
        when(ex.getMessage()).thenReturn(null);

        ResponseEntity<DatabaseExceptionHandler.ErrorResponse> response = handler.handleDataIntegrityViolation(ex,
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Data integrity constraint violation occurred");
        assertThat(response.getBody().details()).isNull();
    }

    private static WebRequest mockRequestPath(String path) {
        WebRequest request = Mockito.mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=" + path);
        return request;
    }
}
