package io.mersel.dss.signer.api.services.signature.cades;

import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.TimestampBinary;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import io.mersel.dss.signer.api.enums.TimestampType;
import io.mersel.dss.signer.api.exceptions.SignatureException;
import io.mersel.dss.signer.api.models.SignResponse;
import io.mersel.dss.signer.api.models.SigningMaterial;
import io.mersel.dss.signer.api.services.timestamp.TimestampConfigurationService;
import io.mersel.dss.signer.api.util.CryptoUtils;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * CAdES (CMS İleri Seviye Elektronik İmza) imzaları oluşturan servis.
 * BouncyCastle kullanarak farklı CAdES seviyelerini destekler.
 * 
 * <p>Desteklenen seviyeler:
 * <ul>
 *   <li>CAdES-B: Temel imza seviyesi (zaman damgası yok)</li>
 *   <li>CAdES-T: İmza zaman damgalı seviye</li>
 *   <li>CAdES-A: Arşiv zaman damgalı seviye (uzun süreli doğrulama)</li>
 * </ul>
 * 
 * <p>Özellikler:
 * <ul>
 *   <li>Enveloping packaging: İçerik imza içinde gömülü</li>
 *   <li>SigningCertificateV2 özniteliği</li>
 *   <li>SigningTime özniteliği</li>
 *   <li>Parametrik zaman damgası türü seçimi</li>
 * </ul>
 */
@Service
public class CAdESSignatureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CAdESSignatureService.class);
    
    // ETSI OID'leri
    private static final ASN1ObjectIdentifier ID_AA_ETS_ESC_TIMESTAMP = 
        new ASN1ObjectIdentifier("0.4.0.1733.2.4");

    private final TimestampConfigurationService timestampService;
    private final CertificateVerifier certificateVerifier;
    private final Semaphore semaphore;

    public CAdESSignatureService(TimestampConfigurationService timestampService,
                                 CertificateVerifier certificateVerifier,
                                 Semaphore signatureSemaphore) {
        this.timestampService = timestampService;
        this.certificateVerifier = certificateVerifier;
        this.semaphore = signatureSemaphore;
    }

    /**
     * String içeriği CAdES imzası ile imzalar (geriye uyumluluk).
     * 
     * @param content İmzalanacak metin içeriği
     * @param includeTimestamp Zaman damgası eklensin mi (true ise SIGNATURE türü kullanılır)
     * @param signatureId İsteğe bağlı imza tanımlayıcısı
     * @param material İmzalama sertifikası ve private key içeren materyal
     * @return İmzalanmış belge (PKCS#7/CMS formatında) ve imza değeri içeren yanıt
     */
    public SignResponse signContent(String content,
                                    boolean includeTimestamp,
                                    String signatureId,
                                    SigningMaterial material) {
        TimestampType timestampType = includeTimestamp ? TimestampType.SIGNATURE : TimestampType.NONE;
        return signContent(content, timestampType, signatureId, material);
    }

    /**
     * String içeriği belirtilen zaman damgası türü ile CAdES imzası oluşturur.
     * 
     * @param content İmzalanacak metin içeriği
     * @param timestampType Zaman damgası türü (NONE, SIGNATURE, ARCHIVE, ALL)
     * @param signatureId İsteğe bağlı imza tanımlayıcısı
     * @param material İmzalama sertifikası ve private key içeren materyal
     * @return İmzalanmış belge (PKCS#7/CMS formatında) ve imza değeri içeren yanıt
     */
    public SignResponse signContent(String content,
                                    TimestampType timestampType,
                                    String signatureId,
                                    SigningMaterial material) {
        try {
            LOGGER.info("CAdES imzalama başlıyor. Zaman damgası türü: {}", timestampType.getDescription());

            // İçeriği byte dizisine çevir
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            // CMS imzası oluştur
            byte[] signedBytes = createCMSSignature(contentBytes, material);

            // Zaman damgası ekle
            if (timestampType != TimestampType.NONE && timestampService.isAvailable()) {
                signedBytes = addTimestamps(signedBytes, contentBytes, timestampType);
                LOGGER.info("CAdES imzası oluşturuldu ({}). Boyut: {} bytes", 
                    getCAdESLevel(timestampType), signedBytes.length);
            } else {
                if (timestampType != TimestampType.NONE) {
                    LOGGER.warn("Zaman damgası istendi ancak TSP servisi yapılandırılmamış. CAdES-B döndürülüyor.");
                }
                LOGGER.info("CAdES-B imzası oluşturuldu. Boyut: {} bytes", signedBytes.length);
            }

            return new SignResponse(signedBytes, null);

        } catch (SignatureException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("CAdES imzası oluşturulurken hata", e);
            throw new SignatureException("CAdES imzası oluşturulamadı", e);
        }
    }
    
    /**
     * Zaman damgası türüne göre CAdES seviyesini döndürür.
     */
    private String getCAdESLevel(TimestampType type) {
        switch (type) {
            case SIGNATURE:
            case CONTENT:
                return "CAdES-T";
            case ARCHIVE:
            case ESC:
            case ALL:
                return "CAdES-A";
            default:
                return "CAdES-B";
        }
    }

    /**
     * BouncyCastle ile CMS imzası oluşturur.
     * SigningCertificateV2 ve SigningTime öznitelikleri ile.
     */
    private byte[] createCMSSignature(byte[] contentBytes, SigningMaterial material) throws Exception {
        
        // SigningCertificateV2 için sertifika hash'i hesapla
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] certificateHash = messageDigest.digest(
            material.getSigningCertificate().getEncoded());

        // Issuer serial oluştur
        GeneralName generalName = new GeneralName(
            X500Name.getInstance(material.getSigningCertificate()
                .getIssuerX500Principal().getEncoded()));
        GeneralNames generalNames = new GeneralNames(generalName);
        IssuerSerial issuerSerial = new IssuerSerial(
            generalNames, material.getSigningCertificate().getSerialNumber());

        // SigningCertificateV2 attribute oluştur
        ESSCertIDv2 essCert = new ESSCertIDv2(
            new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256),
            certificateHash, issuerSerial);
        SigningCertificateV2 signingCertificateV2 = new SigningCertificateV2(
            new ESSCertIDv2[]{essCert});
        Attribute signingCertAttr = new Attribute(
            PKCSObjectIdentifiers.id_aa_signingCertificateV2,
            new DERSet(signingCertificateV2));

        // SigningTime attribute oluştur
        Attribute signingTimeAttr = new Attribute(
            CMSAttributes.signingTime,
            new DERSet(new ASN1UTCTime(new Date())));

        // Signed attributes oluştur
        ASN1EncodableVector signedAttributes = new ASN1EncodableVector();
        signedAttributes.add(signingCertAttr);
        signedAttributes.add(signingTimeAttr);
        AttributeTable attributeTable = new AttributeTable(signedAttributes);

        // Signer oluştur
        JcaSignerInfoGeneratorBuilder signerInfoGeneratorBuilder = 
            new JcaSignerInfoGeneratorBuilder(
                new JcaDigestCalculatorProviderBuilder().build())
                .setSignedAttributeGenerator(
                    new DefaultSignedAttributeTableGenerator(attributeTable));

        // Dinamik algoritma seçimi (RSA veya EC key'e göre)
        String signatureAlgorithm = CryptoUtils.getSignatureAlgorithm(material.getPrivateKey());
        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm)
            .build(material.getPrivateKey());

        // CMS signed data generator oluştur
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        generator.addSignerInfoGenerator(
            signerInfoGeneratorBuilder.build(contentSigner, 
                material.getSigningCertificate()));
        generator.addCertificates(new JcaCertStore(material.getCertificateChain()));

        semaphore.acquire();
        try {
            // İmzayı oluştur (encapsulated = true, içerik imza içinde gömülü)
            CMSSignedData signedData = generator.generate(
                new CMSProcessableByteArray(contentBytes), true);
            
            byte[] signedBytes = signedData.getEncoded();

            LOGGER.debug("CAdES-B imzası oluşturuldu. Boyut: {} bytes", signedBytes.length);
            return signedBytes;

        } finally {
            semaphore.release();
        }
    }

    /**
     * Belirtilen türe göre zaman damgalarını ekler.
     */
    private byte[] addTimestamps(byte[] signedData, byte[] contentBytes, TimestampType type) {
        try {
            CMSSignedData cms = new CMSSignedData(signedData);
            SignerInformation signerInfo = cms.getSignerInfos().getSigners().iterator().next();
            
            ASN1EncodableVector unsignedAttrs = new ASN1EncodableVector();
            
            // İmza zaman damgası (SIGNATURE veya ALL)
            if (type == TimestampType.SIGNATURE || type == TimestampType.ALL) {
                Attribute sigTs = createSignatureTimestamp(signerInfo);
                unsignedAttrs.add(sigTs);
                LOGGER.debug("İmza zaman damgası eklendi");
            }
            
            // İçerik zaman damgası (CONTENT)
            if (type == TimestampType.CONTENT) {
                Attribute contentTs = createContentTimestamp(contentBytes);
                unsignedAttrs.add(contentTs);
                LOGGER.debug("İçerik zaman damgası eklendi");
            }
            
            // Arşiv zaman damgası için DSS level extension kullan (ATSv3 formatı)
            if (type == TimestampType.ARCHIVE || type == TimestampType.ALL) {
                // Önce mevcut timestamp'ları ekle, sonra DSS ile archive timestamp ekle
                if (unsignedAttrs.size() > 0) {
                    AttributeTable tempTable = new AttributeTable(unsignedAttrs);
                    SignerInformation tempSigner = SignerInformation.replaceUnsignedAttributes(signerInfo, tempTable);
                    Collection<SignerInformation> tempSigners = new ArrayList<>();
                    tempSigners.add(tempSigner);
                    cms = CMSSignedData.replaceSigners(cms, new SignerInformationStore(tempSigners));
                }
                // DSS ile CAdES-A seviyesine yükselt (ATSv3 formatı)
                byte[] upgradedData = upgradeToArchiveLevel(cms.getEncoded());
                LOGGER.debug("Arşiv zaman damgası eklendi (ATSv3 formatı)");
                return upgradedData;
            }
            
            // ESC zaman damgası
            if (type == TimestampType.ESC) {
                Attribute escTs = createESCTimestamp(signerInfo);
                unsignedAttrs.add(escTs);
                LOGGER.debug("ESC zaman damgası eklendi");
            }
            
            // Unsigned attributes tablosunu oluştur
            AttributeTable unsignedTable = new AttributeTable(unsignedAttrs);
            
            // Yeni SignerInfo oluştur
            SignerInformation newSigner = SignerInformation.replaceUnsignedAttributes(
                signerInfo, unsignedTable);
            
            // Yeni CMS oluştur
            Collection<SignerInformation> newSigners = new ArrayList<>();
            newSigners.add(newSigner);
            SignerInformationStore newSignerStore = new SignerInformationStore(newSigners);
            
            CMSSignedData newCms = CMSSignedData.replaceSigners(cms, newSignerStore);
            
            LOGGER.info("Zaman damgaları başarıyla eklendi: {}", type.getDescription());
            return newCms.getEncoded();
            
        } catch (Exception e) {
            LOGGER.error("Zaman damgası eklenirken hata: {}", e.getMessage(), e);
            throw new SignatureException("Zaman damgası eklenemedi", e);
        }
    }

    /**
     * İmza zaman damgası oluşturur (CAdES-T).
     * İmza değerinin SHA-256 hash'i üzerine timestamp alır.
     * OID: 1.2.840.113549.1.9.16.2.14
     */
    private Attribute createSignatureTimestamp(SignerInformation signerInfo) throws Exception {
        byte[] signatureBytes = signerInfo.getSignature();
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] signatureDigest = digest.digest(signatureBytes);
        
        TimestampBinary timestampBinary = timestampService.getTspSource()
            .getTimeStampResponse(DigestAlgorithm.SHA256, signatureDigest);
        byte[] timestampToken = timestampBinary.getBytes();
        
        LOGGER.debug("İmza timestamp token alındı. Boyut: {} bytes", timestampToken.length);
        
        ASN1Primitive tsTokenAsn1 = ASN1Primitive.fromByteArray(timestampToken);
        return new Attribute(
            PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
            new DERSet(tsTokenAsn1));
    }

    /**
     * İçerik zaman damgası oluşturur.
     * İmzalanan içeriğin SHA-256 hash'i üzerine timestamp alır.
     * OID: 1.2.840.113549.1.9.16.2.20
     */
    private Attribute createContentTimestamp(byte[] contentBytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] contentDigest = digest.digest(contentBytes);
        
        TimestampBinary timestampBinary = timestampService.getTspSource()
            .getTimeStampResponse(DigestAlgorithm.SHA256, contentDigest);
        byte[] timestampToken = timestampBinary.getBytes();
        
        LOGGER.debug("İçerik timestamp token alındı. Boyut: {} bytes", timestampToken.length);
        
        ASN1Primitive tsTokenAsn1 = ASN1Primitive.fromByteArray(timestampToken);
        return new Attribute(
            PKCSObjectIdentifiers.id_aa_ets_contentTimestamp,
            new DERSet(tsTokenAsn1));
    }

    /**
     * DSS kullanarak CAdES-A seviyesine yükseltir (ATSv3 formatı).
     * Bu format İmzager ve diğer doğrulama araçları tarafından tanınır.
     */
    private byte[] upgradeToArchiveLevel(byte[] signedData) {
        try {
            LOGGER.debug("CAdES-A seviyesine yükseltiliyor (ATSv3 formatı)...");
            
            // DSS CAdES servisi oluştur
            CAdESService cadesService = new CAdESService(certificateVerifier);
            cadesService.setTspSource(timestampService.getTspSource());
            
            // İmzalı veriyi DSS document'ına çevir
            DSSDocument signedDocument = new InMemoryDocument(signedData);
            
            // CAdES-A parametreleri
            CAdESSignatureParameters parameters = new CAdESSignatureParameters();
            parameters.setSignatureLevel(SignatureLevel.CAdES_BASELINE_LTA);
            
            // Seviye yükseltme
            DSSDocument extendedDocument = cadesService.extendDocument(signedDocument, parameters);
            
            LOGGER.info("CAdES-A seviyesine başarıyla yükseltildi (ATSv3 formatı)");
            
            // Java 8 uyumlu okuma
            java.io.InputStream is = extendedDocument.openStream();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            is.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            LOGGER.error("CAdES-A seviyesine yükseltme başarısız: {}", e.getMessage(), e);
            // Hata durumunda orijinal veriyi döndür
            LOGGER.warn("Archive timestamp eklenemedi, CAdES-T seviyesi korunuyor");
            return signedData;
        }
    }

    /**
     * ESC (Extended Signature and Certificates) zaman damgası oluşturur.
     * OID: 0.4.0.1733.2.4 (ETSI)
     */
    private Attribute createESCTimestamp(SignerInformation signerInfo) throws Exception {
        // ESC timestamp için imza değeri + signed attributes hash'i kullanılır
        byte[] signatureBytes = signerInfo.getSignature();
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] escDigest = digest.digest(signatureBytes);
        
        TimestampBinary timestampBinary = timestampService.getTspSource()
            .getTimeStampResponse(DigestAlgorithm.SHA256, escDigest);
        byte[] timestampToken = timestampBinary.getBytes();
        
        LOGGER.debug("ESC timestamp token alındı. Boyut: {} bytes", timestampToken.length);
        
        ASN1Primitive tsTokenAsn1 = ASN1Primitive.fromByteArray(timestampToken);
        return new Attribute(
            ID_AA_ETS_ESC_TIMESTAMP,
            new DERSet(tsTokenAsn1));
    }
}
