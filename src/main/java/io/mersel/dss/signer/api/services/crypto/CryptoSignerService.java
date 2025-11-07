package io.mersel.dss.signer.api.services.crypto;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import io.mersel.dss.signer.api.exceptions.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.Signature;

/**
 * Düşük seviye kriptografik imzalama servisi.
 * Private key kullanarak gerçek imza hesaplamasını gerçekleştirir.
 */
@Service
public class CryptoSignerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoSignerService.class);

    private final SignatureAlgorithmResolverService algorithmResolver;

    public CryptoSignerService(SignatureAlgorithmResolverService algorithmResolver) {
        this.algorithmResolver = algorithmResolver;
    }

    /**
     * Sağlanan private key kullanarak veriyi imzalar.
     * 
     * @param dataToSign İmzalanacak veri
     * @param privateKey İmzalama için private key
     * @param digestAlgorithm Kullanılacak digest algoritması
     * @return Algoritma ve imza byte'larını içeren imza değeri
     * @throws SignatureException İmzalama başarısız olursa
     */
    public SignatureValue sign(ToBeSigned dataToSign, 
                               PrivateKey privateKey,
                               DigestAlgorithm digestAlgorithm) {
        try {
            SignatureAlgorithm signatureAlgorithm = 
                algorithmResolver.determineSignatureAlgorithm(privateKey, digestAlgorithm);

            Signature signature = Signature.getInstance(signatureAlgorithm.getJCEId());
            signature.initSign(privateKey);
            signature.update(dataToSign.getBytes());
            byte[] signatureBytes = signature.sign();

            LOGGER.debug("İmza başarıyla oluşturuldu. Algoritma: {}", 
                signatureAlgorithm);

            return new SignatureValue(signatureAlgorithm, signatureBytes);

        } catch (SignatureException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureException("İmza oluşturulamadı", e);
        }
    }
}

