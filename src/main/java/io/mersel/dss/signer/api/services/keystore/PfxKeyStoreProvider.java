package io.mersel.dss.signer.api.services.keystore;

import io.mersel.dss.signer.api.exceptions.KeyStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

/**
 * PKCS#12 (PFX) dosyaları için KeyStore sağlayıcısı.
 * Dosya sistemi yolundan keystore yükler.
 */
public class PfxKeyStoreProvider implements KeyStoreProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PfxKeyStoreProvider.class);

    private final String pfxPath;

    public PfxKeyStoreProvider(String pfxPath) {
        this.pfxPath = pfxPath;
    }

    @Override
    public KeyStore loadKeyStore(char[] pin) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            Path path = Paths.get(pfxPath);
            
            if (!Files.exists(path)) {
                throw new KeyStoreException("PFX dosyası bulunamadı: " + pfxPath);
            }
            
            try (InputStream inputStream = Files.newInputStream(path)) {
                keyStore.load(inputStream, pin);
            }
            
            LOGGER.info("PFX KeyStore başarıyla yüklendi: {}", pfxPath);
            return keyStore;
            
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("PFX keystore yüklenemedi: " + pfxPath, e);
        }
    }

    @Override
    public String getType() {
        return "PKCS12";
    }
}

