package io.mersel.dss.signer.api.services.keystore;

import java.security.KeyStore;

/**
 * Farklı kaynaklardan (PKCS11, PFX vb.) KeyStore yüklemek için interface.
 */
public interface KeyStoreProvider {
    
    /**
     * Yapılandırılmış kaynaktan bir KeyStore yükler.
     * 
     * @param pin Keystore'u açmak için PIN/şifre
     * @return Yüklenmiş KeyStore örneği
     * @throws io.mersel.dss.signer.api.exceptions.KeyStoreException Yükleme başarısız olursa
     */
    KeyStore loadKeyStore(char[] pin);
    
    /**
     * Bu sağlayıcının yönettiği keystore tipini döndürür.
     * 
     * @return KeyStore tipi (örn. "PKCS11", "PKCS12")
     */
    String getType();
}

