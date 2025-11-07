package io.mersel.dss.signer.api;

import io.mersel.dss.signer.api.exceptions.CertificateValidationException;
import io.mersel.dss.signer.api.exceptions.SignatureException;
import io.mersel.dss.signer.api.exceptions.TimestampException;
import io.mersel.dss.signer.api.models.ErrorModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Global exception handler test'leri.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleSignatureException() {
        // Given
        SignatureException exception = new SignatureException(
            "SIGNATURE_FAILED", 
            "İmza oluşturulamadı"
        );

        // When
        ResponseEntity<ErrorModel> response = exceptionHandler.handleSignatureException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SIGNATURE_FAILED", response.getBody().getCode());
        assertEquals("İmza oluşturulamadı", response.getBody().getMessage());
    }

    @Test
    void testHandleCertificateValidationException() {
        // Given
        CertificateValidationException exception = new CertificateValidationException(
            "Sertifika geçersiz"
        );

        // When
        ResponseEntity<ErrorModel> response = 
            exceptionHandler.handleCertificateValidationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CERTIFICATE_VALIDATION_ERROR", response.getBody().getCode());
    }

    @Test
    void testHandleTimestampException() {
        // Given
        TimestampException exception = new TimestampException(
            "Zaman damgası sunucusuna erişilemiyor"
        );

        // When
        ResponseEntity<ErrorModel> response = exceptionHandler.handleTimestampException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TIMESTAMP_ERROR", response.getBody().getCode());
    }

    @Test
    void testHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Beklenmeyen hata");

        // When
        ResponseEntity<ErrorModel> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getCode());
        assertTrue(response.getBody().getMessage().contains("Beklenmeyen bir hata"));
    }
}

