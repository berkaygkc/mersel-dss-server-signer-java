package io.mersel.dss.signer.api.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.europa.esig.dss.model.x509.CertificateToken;

/**
 * KamuSM güvenilir kök sertifikalarını indirir ve yönetir.
 * Periyodik olarak güncelleme yapar ve cache'de tutar.
 */
@Service
public class KamusmRootCertificateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KamusmRootCertificateService.class);
    private static final String DEFAULT_URL = "http://depo.kamusm.gov.tr/depo/SertifikaDeposu.xml";

    private final RestTemplate restTemplate;
    private final ResourceLoader resourceLoader;
    private final String rootUrl;
    private final AtomicReference<List<X509Certificate>> trustedRoots = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<List<CertificateToken>> trustedRootTokens = new AtomicReference<>(
            Collections.emptyList());

    public KamusmRootCertificateService(RestTemplateBuilder restTemplateBuilder,
                                        ResourceLoader resourceLoader,
                                        @Value("${kamusm.root.url:" + DEFAULT_URL + "}") String rootUrl) {
        this.restTemplate = restTemplateBuilder
                .setReadTimeout(Duration.ofSeconds(10))
                .setConnectTimeout(Duration.ofSeconds(5))
                .build();
        this.resourceLoader = resourceLoader;
        this.rootUrl = rootUrl;
    }

    @PostConstruct
    @Scheduled(cron = "${kamusm.root.refresh-cron:0 15 3 * * *}")
    public void refreshTrustedRoots() {
        try {
            LOGGER.info("KamuSM güvenilir kök sertifikaları yenileniyor: {}", rootUrl);
            String xmlBody = loadRepositoryXml();
            if (xmlBody == null || xmlBody.trim().isEmpty()) {
                LOGGER.warn("KamuSM kök sertifika verisi boş - mevcut liste korunuyor");
                return;
            }
            List<X509Certificate> certificates = parseCertificates(xmlBody);
            if (certificates.isEmpty()) {
                LOGGER.warn("KamuSM kök sertifika listesi boş - mevcut liste korunuyor");
                return;
            }
            List<CertificateToken> tokens = new ArrayList<>(certificates.size());
            for (X509Certificate certificate : certificates) {
                tokens.add(new CertificateToken(certificate));
            }
            trustedRoots.set(Collections.unmodifiableList(certificates));
            trustedRootTokens.set(Collections.unmodifiableList(tokens));
            LOGGER.info("KamuSM kök sertifikaları başarıyla yenilendi ({} adet)", certificates.size());
        } catch (Exception ex) {
            LOGGER.warn("KamuSM kök sertifikalarını yenileme başarısız: {}", ex.getMessage());
            LOGGER.debug("Kök sertifika yenileme hata detayı", ex);
        }
    }

    /**
     * Şu an cache'lenmiş KamuSM güvenilir kök sertifikalarını döndürür.
     */
    public List<X509Certificate> getTrustedRoots() {
        return trustedRoots.get();
    }

    /**
     * Şu an cache'lenmiş KamuSM güvenilir kök sertifikalarını DSS CertificateToken olarak döndürür.
     */
    public List<CertificateToken> getTrustedRootTokens() {
        return trustedRootTokens.get();
    }

    private String loadRepositoryXml() throws Exception {
        if (rootUrl.startsWith("classpath:") || rootUrl.startsWith("file:")) {
            Resource resource = resourceLoader.getResource(rootUrl);
            if (!resource.exists()) {
                throw new IllegalStateException("Resource not found: " + rootUrl);
            }
            try (InputStream inputStream = resource.getInputStream()) {
                byte[] bytes = IOUtils.toByteArray(inputStream);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        ResponseEntity<String> response = restTemplate.getForEntity(rootUrl, String.class);
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            LOGGER.warn("KamuSM kök sertifika indirme başarısız. HTTP durum: {}", response.getStatusCode());
            return null;
        }
        return response.getBody();
    }

    private List<X509Certificate> parseCertificates(String xmlBody) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlBody.getBytes()));

        List<String> values = new ArrayList<>();

        // KamuSM XML formatı: Her <koksertifika> altında <mValue> tag'i var
        // SADECE <mValue> içindeki Base64 certificate'ı almalıyız
        // kok.getTextContent() kullanırsak TÜM child element'ler karışır!
        NodeList kokNodes = document.getElementsByTagName("koksertifika");
        for (int i = 0; i < kokNodes.getLength(); i++) {
            Element kokElement = (Element) kokNodes.item(i);
            
            // Sadece <mValue> tag'ini al
            NodeList mValueNodes = kokElement.getElementsByTagName("mValue");
            if (mValueNodes.getLength() > 0) {
                String certBase64 = mValueNodes.item(0).getTextContent();
                values.add(certBase64);
            } else {
                LOGGER.warn("koksertifika #{} için mValue bulunamadı, atlanıyor", i + 1);
            }
        }


        List<X509Certificate> certificates = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        
        for (int i = 0; i < values.size(); i++) {
            String base64 = values.get(i);
            if (base64 == null || base64.isEmpty()) {
                continue;
            }
            
            // Base64 76 kolon formatında olabilir (satır sonları, boşluklar var)
            // Tüm whitespace karakterlerini temizle
            String normalized = base64.replaceAll("\\s+", "");
            if (normalized.isEmpty()) {
                continue;
            }
            
            try {
                // Base64.getMimeDecoder() 76 kolon formatını otomatik handle eder
                byte[] der = Base64.getMimeDecoder().decode(normalized);
                X509Certificate certificate = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(der));
                certificates.add(certificate);
                LOGGER.debug("Kök sertifika #{} başarıyla parse edildi", i + 1);
            } catch (Exception e) {
                LOGGER.warn("Kök sertifika #{} parse edilemedi, atlanıyor: {}", i + 1, e.getMessage());
            }
        }
        
        LOGGER.info("Toplam {} adet kök sertifika parse edildi", certificates.size());
        return certificates;
    }
}
