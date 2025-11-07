package io.mersel.dss.signer.api.services;

import io.mersel.dss.signer.api.dtos.CertificateInfoDto;
import io.mersel.dss.signer.api.exceptions.KeyStoreException;
import io.mersel.dss.signer.api.services.keystore.KeyStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.PolicyQualifierInfo;
import org.bouncycastle.asn1.x509.Extension;

/**
 * Keystore içerisindeki sertifika bilgilerini listeleme servisi.
 */
@Service
public class CertificateInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateInfoService.class);
    
    // Policy Qualifier OID'leri (RFC 3280)
    private static final String ID_QT_CPS = "1.3.6.1.5.5.7.2.1";
    private static final String ID_QT_UNOTICE = "1.3.6.1.5.5.7.2.2";

    /**
     * Verilen keystore provider'dan tüm sertifikaları listeler.
     * 
     * @param provider KeyStore sağlayıcısı (PKCS11 veya PFX)
     * @param pin KeyStore için PIN/şifre
     * @return Sertifika bilgileri listesi
     */
    public List<CertificateInfoDto> listCertificates(KeyStoreProvider provider, char[] pin) {
        List<CertificateInfoDto> certificates = new ArrayList<>();
        
        try {
            LOGGER.info("Keystore'dan sertifikalar listeleniyor: {}", provider.getType());
            
            KeyStore keyStore = provider.loadKeyStore(pin);
            Enumeration<String> aliases = keyStore.aliases();
            
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                
                try {
                    // Sertifika var mı kontrol et
                    if (keyStore.isCertificateEntry(alias) || keyStore.isKeyEntry(alias)) {
                        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                        
                        if (cert != null) {
                            CertificateInfoDto dto = new CertificateInfoDto();
                            dto.setAlias(alias);
                            dto.setSerialNumberHex(cert.getSerialNumber().toString(16).toUpperCase());
                            dto.setSerialNumberDec(cert.getSerialNumber().toString());
                            dto.setSubject(cert.getSubjectX500Principal().toString());
                            dto.setIssuer(cert.getIssuerX500Principal().toString());
                            dto.setValidFrom(cert.getNotBefore());
                            dto.setValidTo(cert.getNotAfter());
                            dto.setHasPrivateKey(keyStore.isKeyEntry(alias));
                            dto.setType(cert.getType());
                            dto.setSignatureAlgorithm(cert.getSigAlgName());
                            
                            // Sertifika kullanım alanlarını çıkar
                            dto.setKeyUsage(extractKeyUsage(cert));
                            dto.setExtendedKeyUsage(extractExtendedKeyUsage(cert));
                            dto.setCertificatePolicies(extractCertificatePolicies(cert));
                            
                            certificates.add(dto);
                            
                            LOGGER.debug("Sertifika bulundu - Alias: {}, Serial: {}, Subject: {}", 
                                alias, dto.getSerialNumberHex(), dto.getSubject());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Alias için sertifika bilgisi alınamadı: {} - {}", alias, e.getMessage());
                }
            }
            
            LOGGER.info("Toplam {} sertifika bulundu", certificates.size());
            
        } catch (Exception e) {
            throw new KeyStoreException("Sertifika listesi alınamadı: " + e.getMessage(), e);
        }
        
        return certificates;
    }

    /**
     * Sertifika bilgilerini konsol formatında yazdırır.
     * Command-line kullanımı için.
     */
    public void printCertificates(List<CertificateInfoDto> certificates) {
        if (certificates.isEmpty()) {
            System.out.println("\n⚠️  Keystore'da sertifika bulunamadı\n");
            return;
        }
        
        String separator = createSeparator(80, '=');
        String lineSeparator = createSeparator(80, '-');
        
        System.out.println("\n" + separator);
        System.out.println("🔐 KEYSTORE SERTİFİKALARI");
        System.out.println(separator);
        System.out.println();
        
        for (int i = 0; i < certificates.size(); i++) {
            CertificateInfoDto cert = certificates.get(i);
            
            System.out.println(String.format("📜 Sertifika #%d", i + 1));
            System.out.println(lineSeparator);
            System.out.println(String.format("  Alias:             %s", cert.getAlias()));
            System.out.println(String.format("  Serial (hex):      %s", cert.getSerialNumberHex()));
            System.out.println(String.format("  Serial (dec):      %s", cert.getSerialNumberDec()));
            System.out.println(String.format("  Subject:           %s", cert.getSubject()));
            System.out.println(String.format("  Issuer:            %s", cert.getIssuer()));
            System.out.println(String.format("  Valid From:        %s", cert.getValidFrom()));
            System.out.println(String.format("  Valid To:          %s", cert.getValidTo()));
            System.out.println(String.format("  Has Private Key:   %s", cert.isHasPrivateKey() ? "✅ Yes" : "❌ No"));
            System.out.println(String.format("  Type:              %s", cert.getType()));
            System.out.println(String.format("  Signature Algo:    %s", cert.getSignatureAlgorithm()));
            
            // OID bilgileri
            if (cert.getKeyUsage() != null && !cert.getKeyUsage().isEmpty()) {
                System.out.println(String.format("  Key Usage:         %s", cert.getKeyUsage()));
            }
            if (cert.getExtendedKeyUsage() != null && !cert.getExtendedKeyUsage().isEmpty()) {
                System.out.println(String.format("  Ext. Key Usage:    %s", cert.getExtendedKeyUsage()));
            }
            if (cert.getCertificatePolicies() != null && !cert.getCertificatePolicies().isEmpty()) {
                System.out.println(String.format("  Cert. Policies:    %s", cert.getCertificatePolicies()));
            }
            System.out.println();
        }
        
        System.out.println(separator);
        System.out.println(String.format("✅ Toplam %d sertifika bulundu\n", certificates.size()));
        
        // Environment variable örnekleri
        if (!certificates.isEmpty()) {
            CertificateInfoDto first = certificates.get(0);
            System.out.println("💡 Environment Variable Örnekleri:");
            System.out.println(lineSeparator);
            System.out.println(String.format("export CERTIFICATE_ALIAS=%s", first.getAlias()));
            System.out.println(String.format("export CERTIFICATE_SERIAL_NUMBER=%s", first.getSerialNumberHex()));
            System.out.println();
        }
    }

    /**
     * Java 8 uyumlu separator oluşturur (String.repeat() Java 11'de geldi).
     */
    private String createSeparator(int length, char character) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(character);
        }
        return sb.toString();
    }

    /**
     * X509 sertifikasından Key Usage bilgilerini çıkarır.
     */
    private String extractKeyUsage(X509Certificate cert) {
        try {
            boolean[] keyUsage = cert.getKeyUsage();
            if (keyUsage == null) {
                return null;
            }

            List<String> usages = new ArrayList<String>();
            String[] keyUsageNames = {
                "Digital Signature",      // 0
                "Non Repudiation",        // 1
                "Key Encipherment",       // 2
                "Data Encipherment",      // 3
                "Key Agreement",          // 4
                "Key Cert Sign",          // 5
                "CRL Sign",               // 6
                "Encipher Only",          // 7
                "Decipher Only"           // 8
            };

            for (int i = 0; i < keyUsage.length && i < keyUsageNames.length; i++) {
                if (keyUsage[i]) {
                    usages.add(keyUsageNames[i]);
                }
            }

            return usages.isEmpty() ? null : String.join(", ", usages);
        } catch (Exception e) {
            LOGGER.debug("Key Usage bilgisi alınamadı: {}", e.getMessage());
            return null;
        }
    }

    /**
     * X509 sertifikasından Extended Key Usage bilgilerini çıkarır.
     * OID'leri olduğu gibi gösterir.
     */
    private String extractExtendedKeyUsage(X509Certificate cert) {
        try {
            List<String> extKeyUsage = cert.getExtendedKeyUsage();
            if (extKeyUsage == null || extKeyUsage.isEmpty()) {
                return null;
            }

            // OID'leri olduğu gibi göster
            return String.join(", ", extKeyUsage);
        } catch (Exception e) {
            LOGGER.debug("Extended Key Usage bilgisi alınamadı: {}", e.getMessage());
            return null;
        }
    }

    /**
     * X509 sertifikasından Certificate Policies bilgilerini çıkarır.
     */
    private String extractCertificatePolicies(X509Certificate cert) {
        try {
            byte[] extValue = cert.getExtensionValue(Extension.certificatePolicies.getId());
            if (extValue == null) {
                return null;
            }

            org.bouncycastle.asn1.ASN1InputStream asn1InputStream = 
                new org.bouncycastle.asn1.ASN1InputStream(extValue);
            org.bouncycastle.asn1.ASN1OctetString octets = 
                (org.bouncycastle.asn1.ASN1OctetString) asn1InputStream.readObject();
            asn1InputStream.close();

            org.bouncycastle.asn1.ASN1InputStream asn1InputStream2 = 
                new org.bouncycastle.asn1.ASN1InputStream(octets.getOctets());
            ASN1Sequence sequence = (ASN1Sequence) asn1InputStream2.readObject();
            asn1InputStream2.close();

            CertificatePolicies policies = CertificatePolicies.getInstance(sequence);
            PolicyInformation[] policyInfos = policies.getPolicyInformation();

            if (policyInfos == null || policyInfos.length == 0) {
                return null;
            }

            List<String> policyDescriptions = new ArrayList<String>();
            for (PolicyInformation policyInfo : policyInfos) {
                ASN1ObjectIdentifier oid = policyInfo.getPolicyIdentifier();
                String oidStr = oid.getId();
                
                // Sadece OID'yi göster
                StringBuilder policyDesc = new StringBuilder(oidStr);
                
                // Policy qualifiers varsa ekle (CPS URL vs.)
                ASN1Sequence qualifiers = policyInfo.getPolicyQualifiers();
                if (qualifiers != null && qualifiers.size() > 0) {
                    List<String> qualifierTexts = new ArrayList<String>();
                    
                    for (int i = 0; i < qualifiers.size(); i++) {
                        try {
                            PolicyQualifierInfo qualifierInfo = 
                                PolicyQualifierInfo.getInstance(qualifiers.getObjectAt(i));
                            
                            ASN1ObjectIdentifier qualifierId = qualifierInfo.getPolicyQualifierId();
                            String qualifierIdStr = qualifierId.getId();
                            
                            // CPS URI (1.3.6.1.5.5.7.2.1)
                            if (ID_QT_CPS.equals(qualifierIdStr)) {
                                ASN1String cpsUri = (ASN1String) qualifierInfo.getQualifier();
                                if (cpsUri != null) {
                                    qualifierTexts.add(cpsUri.getString());
                                }
                            }
                            // User Notice (1.3.6.1.5.5.7.2.2)
                            else if (ID_QT_UNOTICE.equals(qualifierIdStr)) {
                                ASN1Sequence userNoticeSeq = ASN1Sequence.getInstance(qualifierInfo.getQualifier());
                                if (userNoticeSeq != null && userNoticeSeq.size() > 0) {
                                    for (int j = 0; j < userNoticeSeq.size(); j++) {
                                        try {
                                            ASN1String noticeText = (ASN1String) userNoticeSeq.getObjectAt(j);
                                            if (noticeText != null) {
                                                qualifierTexts.add(noticeText.getString());
                                                break;
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.debug("Policy qualifier parse edilemedi: {}", e.getMessage());
                        }
                    }
                    
                    if (!qualifierTexts.isEmpty()) {
                        policyDesc.append(" (").append(String.join(", ", qualifierTexts)).append(")");
                    }
                }
                
                policyDescriptions.add(policyDesc.toString());
            }

            return policyDescriptions.isEmpty() ? null : String.join(", ", policyDescriptions);
        } catch (Exception e) {
            LOGGER.debug("Certificate Policies bilgisi alınamadı: {}", e.getMessage());
            return null;
        }
    }
}

