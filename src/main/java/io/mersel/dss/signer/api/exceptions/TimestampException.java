package io.mersel.dss.signer.api.exceptions;

/**
 * Zaman damgası işlemleri başarısız olduğunda fırlatılan exception.
 * TSA bağlantı sorunları ve zaman damgası doğrulama hatalarını içerir.
 */
public class TimestampException extends SignatureException {

    public TimestampException(String message) {
        super("TIMESTAMP_ERROR", message);
    }

    public TimestampException(String message, Throwable cause) {
        super("TIMESTAMP_ERROR", message, cause);
    }
}

