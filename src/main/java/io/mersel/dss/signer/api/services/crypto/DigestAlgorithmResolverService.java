package io.mersel.dss.signer.api.services.crypto;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.util.Locale;

/**
 * Sertifika özelliklerine göre uygun digest algoritmaları çöz ümleme servisi.
 * Algoritma seçimi için güvenlik en iyi uygulamalarını takip eder.
 */
@Service
public class DigestAlgorithmResolverService {

    private static final DigestAlgorithm DEFAULT_ALGORITHM = DigestAlgorithm.SHA256;

    /**
     * Bir sertifika için uygun digest algoritmasını çözümler.
     * Seçim şunlara dayanır:
     * 1. Sertifikanın imza algoritması
     * 2. Public key boyutu (EC anahtarlar için)
     * 3. Varsayılan yedek (SHA256)
     * 
     * @param certificate Analiz edilecek sertifika
     * @return Önerilen digest algoritması
     */
    public DigestAlgorithm resolveDigestAlgorithm(X509Certificate certificate) {
        if (certificate == null) {
            return DEFAULT_ALGORITHM;
        }

        // Sertifikanın imza algoritmasından belirlemeyi dene
        String sigAlgName = certificate.getSigAlgName();
        if (StringUtils.hasText(sigAlgName)) {
            DigestAlgorithm fromSigAlg = parseFromSignatureAlgorithm(sigAlgName);
            if (fromSigAlg != null) {
                return fromSigAlg;
            }
        }

        // EC anahtarlar için anahtar boyutuna göre belirle
        PublicKey publicKey = certificate.getPublicKey();
        if (publicKey instanceof ECKey) {
            return resolveForECKey((ECKey) publicKey);
        }

        return DEFAULT_ALGORITHM;
    }

    /**
     * İmza algoritması adından digest algoritmasını ayrıştırır.
     */
    private DigestAlgorithm parseFromSignatureAlgorithm(String sigAlgName) {
        String upper = sigAlgName.toUpperCase(Locale.ROOT);
        
        if (upper.contains("SHA512")) {
            return DigestAlgorithm.SHA512;
        }
        if (upper.contains("SHA384")) {
            return DigestAlgorithm.SHA384;
        }
        if (upper.contains("SHA256")) {
            return DigestAlgorithm.SHA256;
        }
        if (upper.contains("SHA224")) {
            return DigestAlgorithm.SHA224;
        }
        if (upper.contains("SHA1")) {
            // SHA1 kullanımdan kaldırıldı ancak hala tanınıyor
            return DigestAlgorithm.SHA1;
        }
        
        return null;
    }

    /**
     * EC anahtarlar için anahtar boyutuna göre digest algoritmasını çözümler.
     * Digest gücünü anahtar gücüne eşlemek için NIST önerilerini takip eder.
     */
    private DigestAlgorithm resolveForECKey(ECKey ecKey) {
        int keySize = ecKey.getParams().getOrder().bitLength();
        
        // NIST SP 800-57 önerileri
        if (keySize > 384) {
            return DigestAlgorithm.SHA512; // 521-bit eğriler
        }
        if (keySize > 256) {
            return DigestAlgorithm.SHA384; // 384-bit eğriler
        }
        if (keySize > 224) {
            return DigestAlgorithm.SHA256; // 256-bit eğriler
        }
        
        return DigestAlgorithm.SHA224; // 224-bit eğriler
    }
}

