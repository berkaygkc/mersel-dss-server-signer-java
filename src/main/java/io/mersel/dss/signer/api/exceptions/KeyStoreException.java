package io.mersel.dss.signer.api.exceptions;

/**
 * Keystore işlemleri başarısız olduğunda fırlatılan exception.
 * Yükleme, anahtarlara erişim veya keystore yapılandırma hatalarını içerir.
 */
public class KeyStoreException extends SignatureException {

    public KeyStoreException(String message) {
        super("KEYSTORE_ERROR", message);
    }

    public KeyStoreException(String message, Throwable cause) {
        super("KEYSTORE_ERROR", message, cause);
    }
}

