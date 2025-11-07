package io.mersel.dss.signer.api.services;

import io.mersel.dss.signer.api.models.SigningContext;
import io.mersel.dss.signer.api.models.SigningKeyEntry;
import io.mersel.dss.signer.api.models.SigningMaterial;
import io.mersel.dss.signer.api.services.certificate.CertificateChainBuilderService;
import io.mersel.dss.signer.api.services.certificate.CertificateValidatorService;
import io.mersel.dss.signer.api.services.keystore.KeyStoreLoaderService;
import io.mersel.dss.signer.api.services.keystore.KeyStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * SigningMaterial örnekleri oluşturan fabrika servisi.
 * Keystore yükleme, anahtar çözümleme ve sertifika zinciri oluşturmayı orkestre eder.
 */
@Service
public class SigningMaterialFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SigningMaterialFactory.class);

    private final KeyStoreLoaderService keyStoreLoader;
    private final CertificateChainBuilderService chainBuilder;
    private final CertificateValidatorService certificateValidator;

    public SigningMaterialFactory(KeyStoreLoaderService keyStoreLoader,
                                 CertificateChainBuilderService chainBuilder,
                                 CertificateValidatorService certificateValidator) {
        this.keyStoreLoader = keyStoreLoader;
        this.chainBuilder = chainBuilder;
        this.certificateValidator = certificateValidator;
    }

    /**
     * Gerekli tüm imzalama materyali ile SigningContext oluşturur.
     * 
     * @param provider KeyStore sağlayıcısı (PKCS11 veya PFX)
     * @param pin Keystore için PIN/şifre
     * @param certificateAlias İsteğe bağlı sertifika alias'ı
     * @param certificateSerialNumber İsteğe bağlı sertifika seri numarası
     * @return İmzalama işlemleri için hazır eksiksiz SigningContext
     */
    public SigningContext createSigningContext(KeyStoreProvider provider,
                                              char[] pin,
                                              String certificateAlias,
                                              String certificateSerialNumber) {
        try {
            LOGGER.info("{} keystore kullanılarak signing context oluşturuluyor", provider.getType());

            // 1. KeyStore'u yükle
            KeyStore keyStore = keyStoreLoader.loadKeyStore(provider, pin);

            // 2. İmzalama anahtarını çözümle
            SigningKeyEntry keyEntry = keyStoreLoader.resolveKeyEntry(
                keyStore, pin, certificateAlias, certificateSerialNumber);

            // 3. Private key ve sertifikayı çıkar
            PrivateKey privateKey = keyEntry.getEntry().getPrivateKey();
            X509Certificate certificate = (X509Certificate) keyEntry.getEntry().getCertificate();

            // 4. Sertifika tarihlerini doğrula
            certificateValidator.validateCertificateDates(certificate);

            // 5. Sertifika zincirini oluştur
            List<X509Certificate> chain = chainBuilder.buildCertificateChain(certificate);

            // 6. İmzalama materyalini oluştur
            SigningMaterial material = new SigningMaterial(privateKey, certificate, chain);

            LOGGER.info("Signing context başarıyla oluşturuldu. Sertifika: {}, Zincir uzunluğu: {}",
                certificate.getSubjectX500Principal(), chain.size());

            return new SigningContext(keyEntry.getAlias(), material);

        } catch (Exception e) {
            LOGGER.error("Signing context oluşturulamadı", e);
            throw new io.mersel.dss.signer.api.exceptions.KeyStoreException(
                "Signing context oluşturulamadı: " + e.getMessage(), e);
        }
    }
}

