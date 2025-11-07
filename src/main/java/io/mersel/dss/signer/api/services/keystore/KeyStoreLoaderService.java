package io.mersel.dss.signer.api.services.keystore;

import io.mersel.dss.signer.api.exceptions.KeyStoreException;
import io.mersel.dss.signer.api.models.SigningKeyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * KeyStore yükleme ve imzalama anahtarlarını çözümleme servisi.
 * Yapılandırmaya göre uygun KeyStoreProvider'a delege eder.
 */
@Service
public class KeyStoreLoaderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreLoaderService.class);

    /**
     * Uygun sağlayıcı kullanarak KeyStore'u yükler.
     */
    public KeyStore loadKeyStore(KeyStoreProvider provider, char[] pin) {
        return provider.loadKeyStore(pin);
    }

    /**
     * Alias veya seri numarasına göre keystore'dan imzalama anahtar girdisini çözümler.
     * 
     * @param keyStore Yüklenmiş keystore
     * @param pin Private key'lere erişim için PIN
     * @param certificateAlias İsteğe bağlı sertifika alias'ı
     * @param certificateSerialNumber İsteğe bağlı sertifika seri numarası (hex formatında)
     * @return Alias ve private key girdisi içeren SigningKeyEntry
     */
    public SigningKeyEntry resolveKeyEntry(KeyStore keyStore, 
                                          char[] pin,
                                          String certificateAlias,
                                          String certificateSerialNumber) {
        try {
            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(pin);

            // Önce alias ile dene
            if (StringUtils.hasText(certificateAlias) && keyStore.isKeyEntry(certificateAlias)) {
                try {
                    KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) 
                        keyStore.getEntry(certificateAlias, protection);
                    LOGGER.info("İmzalama anahtarı alias ile bulundu: {}", certificateAlias);
                    return new SigningKeyEntry(certificateAlias, entry);
                } catch (Exception e) {
                    throw new KeyStoreException(
                        "Alias için imzalama anahtarı yüklenemedi: " + certificateAlias, e);
                }
            }

            // Seri numarası ile ara veya ilk uygun anahtarı döndür
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }

                try {
                    KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) 
                        keyStore.getEntry(alias, protection);
                    
                    if (entry != null && matchesSerial(entry.getCertificate(), certificateSerialNumber)) {
                        LOGGER.info("İmzalama anahtarı seri numarası ile bulundu: {} (alias: {})", 
                            certificateSerialNumber, alias);
                        return new SigningKeyEntry(alias, entry);
                    }
                } catch (Exception ignored) {
                    LOGGER.debug("Hata nedeniyle alias atlandı: {}", alias);
                }
            }

            throw new KeyStoreException("Keystore'da uygun imzalama anahtarı bulunamadı");
            
        } catch (KeyStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("Keystore'dan imzalama anahtarı çözümlenemedi", e);
        }
    }

    /**
     * Sertifikanın yapılandırılmış seri numarası ile eşleşip eşleşmediğini kontrol eder.
     */
    private boolean matchesSerial(Certificate certificate, String configuredSerial) {
        if (!(certificate instanceof X509Certificate)) {
            return false;
        }

        // Seri numarası yapılandırılmamışsa tüm sertifikaları kabul et
        if (!StringUtils.hasText(configuredSerial)) {
            return true;
        }

        try {
            String certSerial = ((X509Certificate) certificate).getSerialNumber().toString();
            String configuredNormalized = new BigInteger(configuredSerial, 16).toString();
            return configuredNormalized.equals(certSerial);
        } catch (NumberFormatException e) {
            LOGGER.warn("Geçersiz seri numarası formatı: {}", configuredSerial);
            return false;
        }
    }
}

