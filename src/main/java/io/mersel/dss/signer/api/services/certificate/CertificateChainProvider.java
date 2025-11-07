package io.mersel.dss.signer.api.services.certificate;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Sertifika zincirleri oluşturmak için interface.
 */
public interface CertificateChainProvider {
    
    /**
     * Verilen sertifika için bir sertifika zinciri oluşturur.
     * 
     * @param certificate Yaprak (leaf) sertifika
     * @return Yaprak sertifikayı içeren eksiksiz sertifika zinciri
     * @throws Exception Zincir oluşturma başarısız olursa
     */
    List<X509Certificate> buildChain(X509Certificate certificate) throws Exception;
    
    /**
     * Bu sağlayıcının önceliğini döndürür (düşük = yüksek öncelik).
     * 
     * @return Öncelik değeri
     */
    default int getPriority() {
        return 100;
    }
}

