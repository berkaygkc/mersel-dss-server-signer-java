package io.mersel.dss.signer.api.services.certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Birden fazla sağlayıcı kullanarak sertifika zinciri oluşturmayı orkestre eder.
 * Başarılı olana kadar sağlayıcıları öncelik sırasına göre dener.
 */
@Service
public class CertificateChainBuilderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateChainBuilderService.class);

    private final List<CertificateChainProvider> providers;

    public CertificateChainBuilderService(List<CertificateChainProvider> providers) {
        // Önceliğe göre sırala (düşük = yüksek öncelik)
        this.providers = new ArrayList<>(providers);
        this.providers.sort(Comparator.comparingInt(CertificateChainProvider::getPriority));
    }

    /**
     * Mevcut sağlayıcıları kullanarak sertifika zinciri oluşturur.
     * Biri başarılı olana kadar her sağlayıcıyı öncelik sırasına göre dener.
     * 
     * @param leafCertificate Zincir oluşturulacak yaprak sertifika
     * @return Eksiksiz sertifika zinciri (yaprak dahil)
     */
    public List<X509Certificate> buildCertificateChain(X509Certificate leafCertificate) {
        LOGGER.debug("Sertifika zinciri oluşturuluyor: {}", 
            leafCertificate.getSubjectX500Principal());

        for (CertificateChainProvider provider : providers) {
            try {
                List<X509Certificate> chain = provider.buildChain(leafCertificate);
                if (chain != null && !chain.isEmpty()) {
                    LOGGER.info("Zincir başarıyla oluşturuldu. Sağlayıcı: {}", 
                        provider.getClass().getSimpleName());
                    return chain;
                }
            } catch (Exception e) {
                LOGGER.debug("Sağlayıcı {} zincir oluşturamadı: {}", 
                    provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        // Yedek: Tek sertifika döndür
        LOGGER.warn("Tüm sertifika zinciri sağlayıcıları başarısız, tek sertifika kullanılıyor");
        List<X509Certificate> fallbackChain = new ArrayList<>();
        fallbackChain.add(leafCertificate);
        return fallbackChain;
    }
}

