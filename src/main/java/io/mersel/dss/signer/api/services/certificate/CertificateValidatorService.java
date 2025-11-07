package io.mersel.dss.signer.api.services.certificate;

import io.mersel.dss.signer.api.exceptions.CertificateValidationException;
import io.mersel.dss.signer.api.models.SigningMaterial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.cert.*;
import java.util.*;

/**
 * Sertifikaları ve sertifika zincirlerini doğrulayan servis.
 * Yapılandırılmış kök sertifikalara karşı güven doğrulaması yapar.
 */
@Service
public class CertificateValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateValidatorService.class);

    /**
     * İmzalama sertifikasının verilen kök sertifikalar tarafından güvenilir olduğunu doğrular.
     * 
     * @param material Sertifika zinciri içeren imzalama materyali
     * @param trustedRoots Güvenilir kök sertifika listesi
     * @throws CertificateValidationException Doğrulama başarısız olursa
     */
    public void validateSignerTrust(SigningMaterial material, List<X509Certificate> trustedRoots) {
        if (trustedRoots == null || trustedRoots.isEmpty()) {
            LOGGER.warn("Güvenilir kök sertifika yapılandırılmamış; güven doğrulaması atlanıyor");
            return;
        }

        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            List<X509Certificate> chain = new ArrayList<>(material.getCertificateChain());
            
            CertPath certPath = factory.generateCertPath(chain);
            PKIXParameters params = new PKIXParameters(buildTrustAnchors(trustedRoots));
            params.setRevocationEnabled(false); // Revocation is checked separately by DSS
            
            CertPathValidator.getInstance("PKIX").validate(certPath, params);
            
            LOGGER.info("Sertifika güven doğrulaması başarılı: {}", 
                material.getSigningCertificate().getSubjectX500Principal());
                
        } catch (Exception ex) {
            throw new CertificateValidationException(
                "İmzalama sertifikası yapılandırılmış kök sertifikalar tarafından güvenilmiyor", ex);
        }
    }

    /**
     * Sertifika geçerlilik tarihlerini doğrular.
     * 
     * @param certificate Doğrulanacak sertifika
     * @throws CertificateValidationException Sertifika süresi dolmuşsa veya henüz geçerli değilse
     */
    public void validateCertificateDates(X509Certificate certificate) {
        Date now = Calendar.getInstance().getTime();
        Date notBefore = certificate.getNotBefore();
        Date notAfter = certificate.getNotAfter();

        if (now.before(notBefore)) {
            throw new CertificateValidationException(
                String.format("Sertifika henüz geçerli değil. Geçerlilik başlangıcı: %s", notBefore));
        }

        if (now.after(notAfter)) {
            throw new CertificateValidationException(
                String.format("Sertifikanın süresi dolmuş. Geçerlilik sonu: %s", notAfter));
        }

        LOGGER.debug("Sertifika tarihleri geçerli: {} - {}", notBefore, notAfter);
    }

    private Set<TrustAnchor> buildTrustAnchors(List<X509Certificate> roots) {
        Set<TrustAnchor> anchors = new HashSet<>();
        for (X509Certificate certificate : roots) {
            anchors.add(new TrustAnchor(certificate, null));
        }
        return anchors;
    }
}

