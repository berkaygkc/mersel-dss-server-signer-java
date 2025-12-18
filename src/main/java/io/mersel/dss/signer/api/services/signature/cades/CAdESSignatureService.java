package io.mersel.dss.signer.api.services.signature.cades;

import io.mersel.dss.signer.api.exceptions.SignatureException;
import io.mersel.dss.signer.api.models.SignResponse;
import io.mersel.dss.signer.api.models.SigningMaterial;
import io.mersel.dss.signer.api.services.timestamp.TimestampConfigurationService;
import io.mersel.dss.signer.api.services.timestamp.TimestampService;
import io.mersel.dss.signer.api.dtos.TimestampResponseDto;
import io.mersel.dss.signer.api.util.CryptoUtils;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
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
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.Semaphore;

/**
 * CAdES (CMS İleri Seviye Elektronik İmza) imzaları oluşturan servis.
 * Text/plain formatında string veri alır ve zaman damgalı CAdES imzası oluşturur.
 * BouncyCastle kullanarak CMS/PKCS#7 imzası oluşturur.
 * 
 * <p>Özellikler:
 * <ul>
 *   <li>CAdES-BASELINE-B: Temel imza seviyesi</li>
 *   <li>CAdES-BASELINE-T: Zaman damgalı imza seviyesi</li>
 *   <li>Enveloping packaging: İçerik imza içinde gömülü</li>
 *   <li>SigningCertificateV2 özniteliği</li>
 * </ul>
 */
@Service
public class CAdESSignatureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CAdESSignatureService.class);

    private final TimestampConfigurationService timestampConfigService;
    private final TimestampService timestampService;
    private final Semaphore semaphore;

    public CAdESSignatureService(TimestampConfigurationService timestampConfigService,
                                 TimestampService timestampService,
                                 Semaphore signatureSemaphore) {
        this.timestampConfigService = timestampConfigService;
        this.timestampService = timestampService;
        this.semaphore = signatureSemaphore;
    }

    /**
     * String içeriği CAdES imzası ile imzalar.
     * 
     * @param content İmzalanacak metin içeriği
     * @param includeTimestamp Zaman damgası eklensin mi
     * @param signatureId İsteğe bağlı imza tanımlayıcısı (kullanılmıyor)
     * @param material İmzalama sertifikası ve private key içeren materyal
     * @return İmzalanmış belge (PKCS#7/CMS formatında) ve imza değeri içeren yanıt
     */
    public SignResponse signContent(String content,
                                    boolean includeTimestamp,
                                    String signatureId,
                                    SigningMaterial material) {
        try {
            LOGGER.info("CAdES imzalama başlıyor. Zaman damgası: {}", includeTimestamp);

            // 1. İçeriği byte dizisine çevir
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            // 2. CMS imzası oluştur
            byte[] signedBytes = createCMSSignature(contentBytes, material, includeTimestamp);

            LOGGER.info("CAdES imzası başarıyla oluşturuldu. Boyut: {} bytes", signedBytes.length);

            return new SignResponse(signedBytes, null);

        } catch (SignatureException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("CAdES imzası oluşturulurken hata", e);
            throw new SignatureException("CAdES imzası oluşturulamadı", e);
        }
    }

    /**
     * BouncyCastle ile CMS imzası oluşturur.
     * SigningCertificateV2 özniteliği ile SHA-256 hash kullanır.
     */
    private byte[] createCMSSignature(byte[] contentBytes,
                                      SigningMaterial material,
                                      boolean includeTimestamp) throws Exception {
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

        // Signed attributes oluştur
        ASN1EncodableVector signedAttributes = new ASN1EncodableVector();
        signedAttributes.add(signingCertAttr);
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

            // Zaman damgası ekle (CAdES-T)
            if (includeTimestamp && timestampConfigService.isAvailable()) {
                signedBytes = addTimestamp(signedBytes);
            } else if (includeTimestamp) {
                LOGGER.warn("Zaman damgası istendi ancak servis yapılandırılmamış. " +
                        "CAdES-BASELINE-B döndürülüyor.");
            }

            LOGGER.debug("CAdES imzası oluşturuldu. Boyut: {} bytes", signedBytes.length);
            return signedBytes;

        } finally {
            semaphore.release();
        }
    }

    /**
     * CMS imzasına zaman damgası ekler (CAdES-T).
     */
    private byte[] addTimestamp(byte[] signedData) {
        try {
            // İmza değerini al ve zaman damgası iste
            CMSSignedData cms = new CMSSignedData(signedData);
            byte[] signatureBytes = cms.getSignerInfos().getSigners().iterator().next().getSignature();
            
            // Timestamp al
            TimestampResponseDto tsResponse = timestampService.getTimestamp(signatureBytes, "SHA256");
            byte[] timestampToken = Base64.getDecoder().decode(tsResponse.getTimestampToken());

            // Timestamp'ı unsigned attribute olarak ekle
            org.bouncycastle.tsp.TimeStampToken tst = new org.bouncycastle.tsp.TimeStampToken(
                new org.bouncycastle.cms.CMSSignedData(timestampToken));

            ASN1EncodableVector timestampVector = new ASN1EncodableVector();
            timestampVector.add(tst.toCMSSignedData().toASN1Structure().getContentType());
            timestampVector.add(tst.toCMSSignedData().toASN1Structure().getContent());

            Attribute tsAttribute = new Attribute(
                PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
                new DERSet(tst.toCMSSignedData().toASN1Structure()));

            // Unsigned attributes tablosu oluştur
            ASN1EncodableVector unsignedAttrs = new ASN1EncodableVector();
            unsignedAttrs.add(tsAttribute);
            AttributeTable unsignedTable = new AttributeTable(unsignedAttrs);

            // Yeni SignerInfo oluştur (timestamp ile)
            org.bouncycastle.cms.SignerInformation originalSigner = 
                cms.getSignerInfos().getSigners().iterator().next();
            org.bouncycastle.cms.SignerInformation newSigner = 
                org.bouncycastle.cms.SignerInformation.replaceUnsignedAttributes(
                    originalSigner, unsignedTable);

            // Yeni CMS oluştur
            java.util.ArrayList<org.bouncycastle.cms.SignerInformation> newSigners = new java.util.ArrayList<>();
            newSigners.add(newSigner);
            org.bouncycastle.cms.SignerInformationStore newSignerStore = 
                new org.bouncycastle.cms.SignerInformationStore(newSigners);
            
            CMSSignedData newCms = CMSSignedData.replaceSigners(cms, newSignerStore);
            
            LOGGER.info("CAdES-T: Zaman damgası başarıyla eklendi");
            return newCms.getEncoded();

        } catch (Exception e) {
            LOGGER.warn("Zaman damgası eklenemedi, CAdES-B döndürülüyor: {}", e.getMessage());
            return signedData;
        }
    }
}
