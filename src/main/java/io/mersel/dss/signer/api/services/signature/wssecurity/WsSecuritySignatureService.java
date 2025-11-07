package io.mersel.dss.signer.api.services.signature.wssecurity;

import io.mersel.dss.signer.api.constants.XmlConstants;
import io.mersel.dss.signer.api.exceptions.SignatureException;
import io.mersel.dss.signer.api.models.SignResponse;
import io.mersel.dss.signer.api.models.SigningMaterial;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.Merlin;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * SOAP mesajları için WS-Security imzaları oluşturan servis.
 * Hem SOAP 1.1 hem de SOAP 1.2'yi destekler.
 */
@Service
public class WsSecuritySignatureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WsSecuritySignatureService.class);

    private final Semaphore semaphore;

    public WsSecuritySignatureService(Semaphore signatureSemaphore) {
        this.semaphore = signatureSemaphore;
    }

    /**
     * SOAP zarfını WS-Security imzası ile imzalar.
     * 
     * @param soapDocument SOAP zarf belgesi
     * @param useSoap12 SOAP 1.2 (true) veya SOAP 1.1 (false) kullanılıp kullanılmayacağı
     * @param material Sertifika ve private key içeren imzalama materyali
     * @param alias İmzalama için anahtar alias'ı
     * @param pin Private key erişimi için PIN/şifre
     * @return İmzalanmış SOAP belgesi içeren yanıt
     */
    public SignResponse signSoapEnvelope(Document soapDocument,
                                        boolean useSoap12,
                                        SigningMaterial material,
                                        String alias,
                                        char[] pin) {
        try {
            String soapNamespace = useSoap12 
                ? XmlConstants.NS_SOAP_1_DOT_2_ENVELOPE 
                : XmlConstants.NS_SOAP_ENVELOPE;

            // Security header ekle
            WSSecHeader header = new WSSecHeader();
            header.insertSecurityHeader(soapDocument);

            // Zaman damgası ekle
            addTimestamp(soapDocument, header);

            // Body elemanını hazırla
            Element bodyElement = (Element) soapDocument
                .getElementsByTagNameNS(soapNamespace, "Body").item(0);
            
            if (bodyElement != null) {
                bodyElement.setAttribute("Id", "SignedSoapBodyContent");
                bodyElement.removeAttribute("wsu:Id");
                bodyElement.removeAttribute("xmlns:xsi");
                bodyElement.removeAttribute("xmlns:xsd");
            }

            // BinarySecurityToken ekle
            String bstReference = addBinarySecurityToken(
                soapDocument, soapNamespace, material);

            // İmzayı oluştur
            signDocument(soapDocument, header, material, alias, pin, bstReference);

            // Byte'lara dönüştür
            byte[] signedBytes = documentToBytes(soapDocument);

            LOGGER.info("WS-Security imzası başarıyla oluşturuldu");
            return new SignResponse(signedBytes, null);

        } catch (Exception e) {
            LOGGER.error("WS-Security imzası oluşturulurken hata", e);
            throw new SignatureException("WS-Security imzası oluşturulamadı", e);
        }
    }

    /**
     * SOAP security header'ına zaman damgası ekler.
     */
    private void addTimestamp(Document document, WSSecHeader header) throws Exception {
        WSSecTimestamp timestamp = new WSSecTimestamp();
        timestamp.setTimeToLive(3000);
        timestamp.prepare(document);
        timestamp.build(document, header);
        timestamp.getElement().removeAttributeNS(XmlConstants.NS_WSU, "Id");
        timestamp.getElement().setAttribute("Id", "SignedSoapTimestampContent");
    }

    /**
     * Security header'a BinarySecurityToken ekler.
     */
    private String addBinarySecurityToken(Document document,
                                         String soapNamespace,
                                         SigningMaterial material) throws Exception {
        String bstReference = "X509-" + material.getSigningCertificate().getSerialNumber();

        Element headerElement = (Element) document
            .getElementsByTagNameNS(soapNamespace, "Header").item(0);
        
        if (headerElement == null) {
            throw new SignatureException("SOAP Header bulunamadı");
        }

        Element securityElement = (Element) headerElement
            .getElementsByTagNameNS(XmlConstants.NS_WSSE, "Security").item(0);
        
        if (securityElement == null) {
            securityElement = document.createElementNS(XmlConstants.NS_WSSE, "wsse:Security");
            headerElement.appendChild(securityElement);
        }

        Element binarySecurityToken = document.createElementNS(
            XmlConstants.NS_WSSE, "wsse:BinarySecurityToken");
        binarySecurityToken.setAttribute("EncodingType", XmlConstants.ATTR_EncodingType);
        binarySecurityToken.setAttribute("ValueType", XmlConstants.ATTR_ValueType);
        binarySecurityToken.setAttributeNS(XmlConstants.NS_WSU, "wsu:Id", bstReference);
        binarySecurityToken.setTextContent(
            Base64.getEncoder().encodeToString(
                material.getSigningCertificate().getEncoded()));

        securityElement.insertBefore(binarySecurityToken, securityElement.getFirstChild());

        return bstReference;
    }

    /**
     * WS-Security imzasını oluşturur ve uygular.
     */
    private void signDocument(Document document,
                             WSSecHeader header,
                             SigningMaterial material,
                             String alias,
                             char[] pin,
                             String bstReference) throws Exception {
        
        WSSecSignature signatureBuilder = new WSSecSignature();
        signatureBuilder.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
        signatureBuilder.setSignatureAlgorithm(XmlConstants.SignatureAlgorithm);
        signatureBuilder.setUserInfo(alias, new String(pin));

        // Security token referansını ayarla
        SecurityTokenReference securityTokenReference = new SecurityTokenReference(document);
        Reference reference = new Reference(document);
        reference.setURI(bstReference);
        reference.setValueType(XmlConstants.ATTR_ValueType);
        securityTokenReference.setReference(reference);
        signatureBuilder.setSecurityTokenReference(securityTokenReference);

        // İmzalanacak kısımları yapılandır
        List<org.apache.ws.security.WSEncryptionPart> parts = new ArrayList<>();
        parts.add(new org.apache.ws.security.WSEncryptionPart("SignedSoapTimestampContent"));
        parts.add(new org.apache.ws.security.WSEncryptionPart("SignedSoapBodyContent"));
        signatureBuilder.setParts(parts);

        // Crypto oluştur ve imzala
        Crypto crypto = createCrypto(material, alias, pin);

        semaphore.acquire();
        try {
            signatureBuilder.prepare(document, crypto, header);
            signatureBuilder.build(document, crypto, header);
        } finally {
            semaphore.release();
        }
    }

    /**
     * WS-Security için Crypto örneği oluşturur.
     */
    private Crypto createCrypto(SigningMaterial material, String alias, char[] pin) 
            throws Exception {
        Merlin crypto = new Merlin();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry(alias, material.getPrivateKey(), pin,
            material.getCertificateChain().toArray(new Certificate[0]));
        crypto.setKeyStore(keyStore);
        return crypto;
    }

    /**
     * Document'i byte dizisine dönüştürür.
     */
    private byte[] documentToBytes(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        return outputStream.toByteArray();
    }
}

