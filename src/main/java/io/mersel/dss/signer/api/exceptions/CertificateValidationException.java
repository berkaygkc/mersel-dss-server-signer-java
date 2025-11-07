package io.mersel.dss.signer.api.exceptions;

/**
 * Sertifika doğrulaması başarısız olduğunda fırlatılan exception.
 * Zincir doğrulama, güven doğrulama ve iptal kontrollerini içerir.
 */
public class CertificateValidationException extends SignatureException {

    public CertificateValidationException(String message) {
        super("CERTIFICATE_VALIDATION_ERROR", message);
    }

    public CertificateValidationException(String message, Throwable cause) {
        super("CERTIFICATE_VALIDATION_ERROR", message, cause);
    }
}

