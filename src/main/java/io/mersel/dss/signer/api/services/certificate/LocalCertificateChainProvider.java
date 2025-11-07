package io.mersel.dss.signer.api.services.certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Yerel dosyalardan sertifika zinciri oluşturur.
 * Çevrimiçi zincir oluşturma başarısız olduğunda kullanılan yedek yöntemdir.
 */
public class LocalCertificateChainProvider implements CertificateChainProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalCertificateChainProvider.class);

    private final String issuerCertificatePath;
    private final String caCertificatePath;

    public LocalCertificateChainProvider(String issuerCertificatePath, String caCertificatePath) {
        this.issuerCertificatePath = issuerCertificatePath;
        this.caCertificatePath = caCertificatePath;
    }

    @Override
    public List<X509Certificate> buildChain(X509Certificate cert) throws Exception {
        List<X509Certificate> chain = new ArrayList<>();
        // Not: Yaprak sertifika burada eklenmez - orkestratör tarafından eklenir

        if (StringUtils.hasText(issuerCertificatePath)) {
            try {
                X509Certificate issuer = loadCertificate(issuerCertificatePath);
                chain.add(issuer);
                LOGGER.debug("Issuer sertifikası yüklendi: {}", issuerCertificatePath);
            } catch (Exception e) {
                LOGGER.warn("Issuer sertifikası yüklenemedi: {}", issuerCertificatePath);
            }
        }

        if (StringUtils.hasText(caCertificatePath)) {
            try {
                X509Certificate ca = loadCertificate(caCertificatePath);
                chain.add(ca);
                LOGGER.debug("CA sertifikası yüklendi: {}", caCertificatePath);
            } catch (Exception e) {
                LOGGER.warn("CA sertifikası yüklenemedi: {}", caCertificatePath);
            }
        }

        LOGGER.info("Yerel dosyalardan {} sertifika ile zincir oluşturuldu", chain.size());
        return chain;
    }

    @Override
    public int getPriority() {
        return 50; // Çevrimiçiden daha düşük öncelik
    }

    private X509Certificate loadCertificate(String filePath) throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(in);
        }
    }
}

