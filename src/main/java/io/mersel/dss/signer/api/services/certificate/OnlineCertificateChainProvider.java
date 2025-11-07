package io.mersel.dss.signer.api.services.certificate;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * AIA (Authority Information Access) üzerinden issuer sertifikalarını indirerek sertifika zinciri oluşturur.
 * Güncel sertifikaları sağladığı için tercih edilen yöntemdir.
 */
public class OnlineCertificateChainProvider implements CertificateChainProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineCertificateChainProvider.class);
    private static final String CA_ISSUER_OID = "1.3.6.1.5.5.7.48.2";

    @Override
    public List<X509Certificate> buildChain(X509Certificate cert) throws Exception {
        List<X509Certificate> chain = new ArrayList<>();
        chain.add(cert);

        X509Certificate currentCert = cert;
        while (!isSelfSigned(currentCert)) {
            X509Certificate issuerCert = fetchIssuerCertificate(currentCert);
            if (issuerCert == null) {
                LOGGER.warn("Could not fetch issuer certificate for: {}", 
                    currentCert.getSubjectX500Principal());
                break;
            }
            
            chain.add(issuerCert);
            currentCert = issuerCert;

            // Güvenlik kontrolü: Sonsuz döngüyü önle
            if (chain.size() > 10) {
                LOGGER.warn("Sertifika zinciri çok uzun (>10), durduruluyor");
                break;
            }
        }

        LOGGER.info("Sertifika zinciri çevrimiçi olarak {} sertifika ile oluşturuldu", chain.size());
        return chain;
    }

    @Override
    public int getPriority() {
        return 10; // Yüksek öncelik
    }

    private X509Certificate fetchIssuerCertificate(X509Certificate cert) {
        try {
            String aiaUrl = getAiaIssuerUrl(cert);
            if (aiaUrl == null) {
                LOGGER.debug("Sertifikada AIA issuer URL bulunamadı");
                return null;
            }

            LOGGER.debug("Issuer sertifikası indiriliyor: {}", aiaUrl);
            return downloadCertificate(aiaUrl);
            
        } catch (Exception e) {
            LOGGER.debug("Issuer sertifikası getirilemedi: {}", e.getMessage());
            return null;
        }
    }

    private String getAiaIssuerUrl(X509Certificate cert) throws Exception {
        byte[] aiaExt = cert.getExtensionValue(Extension.authorityInfoAccess.getId());
        if (aiaExt == null) {
            return null;
        }

        ASN1Sequence seq = ASN1Sequence.getInstance(
            JcaX509ExtensionUtils.parseExtensionValue(aiaExt));

        for (ASN1Encodable encodable : seq.toArray()) {
            ASN1Sequence subSeq = ASN1Sequence.getInstance(encodable);
            if (subSeq.size() < 2) {
                continue;
            }

            ASN1ObjectIdentifier id = (ASN1ObjectIdentifier) subSeq.getObjectAt(0);
            if (CA_ISSUER_OID.equals(id.getId())) {
                GeneralName generalName = GeneralName.getInstance(subSeq.getObjectAt(1));
                return ((DERIA5String) generalName.getName()).getString();
            }
        }
        return null;
    }

    private X509Certificate downloadCertificate(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        try (InputStream in = url.openStream()) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(in);
        }
    }

    private boolean isSelfSigned(X509Certificate cert) {
        return cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
    }
}

