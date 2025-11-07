package io.mersel.dss.signer.api.services.keystore;

import io.mersel.dss.signer.api.exceptions.KeyStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.pkcs11.SunPKCS11;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;

/**
 * PKCS#11 donanım güvenlik modülleri (HSM) için KeyStore sağlayıcısı.
 * Hem slot tabanlı hem de kütüphane tabanlı yapılandırmayı destekler.
 */
public class PKCS11KeyStoreProvider implements KeyStoreProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PKCS11KeyStoreProvider.class);

    private final String libraryPath;
    private final Long slotIndex;
    private final String providerName;

    public PKCS11KeyStoreProvider(String libraryPath, Long slotIndex) {
        this.libraryPath = libraryPath;
        this.slotIndex = slotIndex;
        this.providerName = "PKCS11Provider_" + System.identityHashCode(this);
    }

    @Override
    public KeyStore loadKeyStore(char[] pin) {
        try {
            Provider provider = buildPKCS11Provider();
            KeyStore keyStore = KeyStore.getInstance("PKCS11", provider);
            keyStore.load(null, pin);
            
            LOGGER.info("PKCS11 KeyStore başarıyla yüklendi. Kütüphane: {}", libraryPath);
            return keyStore;
            
        } catch (Exception e) {
            throw new KeyStoreException("PKCS11 keystore yüklenemedi: " + libraryPath, e);
        }
    }

    @Override
    public String getType() {
        return "PKCS11";
    }

    private Provider buildPKCS11Provider() {
        StringBuilder config = new StringBuilder();
        config.append("name = ").append(providerName).append('\n');
        config.append("library = \"").append(libraryPath).append("\"\n");
        
        if (slotIndex != null && slotIndex >= 0) {
            config.append("slotListIndex = ").append(slotIndex).append('\n');
        }
        
        byte[] configBytes = config.toString().getBytes(StandardCharsets.UTF_8);
        SunPKCS11 provider = new SunPKCS11(new ByteArrayInputStream(configBytes));
        Security.addProvider(provider);
        
        LOGGER.debug("PKCS11 provider yapılandırıldı: {}", providerName);
        return provider;
    }
}

