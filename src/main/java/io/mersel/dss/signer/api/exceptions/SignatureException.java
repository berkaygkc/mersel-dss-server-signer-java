package io.mersel.dss.signer.api.exceptions;

/**
 * İmza ile ilgili tüm hatalar için temel exception.
 * İmza oluşturma veya manipülasyon başarısız olduğunda fırlatılır.
 */
public class SignatureException extends RuntimeException {

    private final String errorCode;

    public SignatureException(String message) {
        super(message);
        this.errorCode = "SIGNATURE_ERROR";
    }

    public SignatureException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SIGNATURE_ERROR";
    }

    public SignatureException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SignatureException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

