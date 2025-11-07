package io.mersel.dss.signer.api.services.timestamp.tubitak;

import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import org.bouncycastle.asn1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TÜBİTAK E-SYA Zaman Damgası servisi için özelleştirilmiş DataLoader.
 * <p>
 * Timestamp request'lerine TÜBİTAK'ın gerektirdiği kimlik doğrulama
 * bilgilerini otomatik olarak ekler.
 */
public class TubitakTimestampDataLoader extends TimestampDataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TubitakTimestampDataLoader.class);

    private static final String IDENTITY_HEADER = "identity";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String TUBITAK_USER_AGENT = "UEKAE TSS Client";

    private final int customerId;
    private final String customerPassword;

    /**
     * TÜBİTAK timestamp data loader oluşturur.
     *
     * @param customerId       Müşteri numarası
     * @param customerPassword Müşteri parolası
     */
    public TubitakTimestampDataLoader(int customerId, String customerPassword) {
        super();
        this.customerId = customerId;
        this.customerPassword = customerPassword;
        
        LOGGER.info("TÜBİTAK Timestamp DataLoader oluşturuldu. Müşteri ID: {}", customerId);
    }

    @Override
    public byte[] post(String url, byte[] content) {
        try {
            byte[] dataHash = extractHashFromTimeStampRequest(content);

            if (dataHash != null) {
                String authToken = TubitakAuthenticationHelper.encryptIdentity(
                        customerId,
                        customerPassword,
                        dataHash
                );

                Map<String, String> headers = new HashMap<>();
                headers.put(IDENTITY_HEADER, authToken);
                headers.put(USER_AGENT_HEADER, TUBITAK_USER_AGENT);

                LOGGER.debug("TÜBİTAK kimlik doğrulama eklendi. URL: {}", url);
                
                return postWithHeaders(url, content, headers);
            } else {
                LOGGER.warn("TimeStamp request parse edilemedi, standart POST yapılıyor");
                return super.post(url, content);
            }

        } catch (Exception e) {
            LOGGER.error("TÜBİTAK kimlik doğrulama hatası: {}", e.getMessage());
            LOGGER.debug("Hata detayı", e);
            throw new RuntimeException("TÜBİTAK timestamp request başarısız", e);
        }
    }

    /**
     * Özel header'lar ile HTTP POST isteği gönderir.
     */
    private byte[] postWithHeaders(String url, byte[] content, Map<String, String> customHeaders) {
        try {
            org.apache.http.client.methods.HttpPost httpPost = 
                    new org.apache.http.client.methods.HttpPost(url);
            
            httpPost.setHeader("Content-Type", "application/timestamp-query");
            httpPost.setHeader("Accept", "application/timestamp-reply");
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }
            
            httpPost.setEntity(new org.apache.http.entity.ByteArrayEntity(content));
            
            org.apache.http.impl.client.CloseableHttpClient httpClient = 
                    org.apache.http.impl.client.HttpClients.createDefault();
            
            try (org.apache.http.client.methods.CloseableHttpResponse response = 
                    httpClient.execute(httpPost)) {
                
                int statusCode = response.getStatusLine().getStatusCode();
                LOGGER.debug("HTTP response status: {}", statusCode);
                
                if (statusCode != 200) {
                    throw new RuntimeException("HTTP error: " + statusCode);
                }
                
                return org.apache.http.util.EntityUtils.toByteArray(
                        response.getEntity());
            } finally {
                httpClient.close();
            }
            
        } catch (Exception e) {
            LOGGER.error("HTTP request hatası: {}", e.getMessage());
            throw new RuntimeException("Timestamp HTTP request başarısız", e);
        }
    }

    /**
     * Timestamp request'inden hash değerini çıkarır.
     */
    private byte[] extractHashFromTimeStampRequest(byte[] tsReq) {
        try {
            ASN1InputStream asn1Stream = new ASN1InputStream(new ByteArrayInputStream(tsReq));
            ASN1Primitive obj = asn1Stream.readObject();
            asn1Stream.close();

            if (!(obj instanceof ASN1Sequence)) {
                return null;
            }

            ASN1Sequence tsReqSeq = (ASN1Sequence) obj;
            if (tsReqSeq.size() < 2) {
                return null;
            }

            ASN1Encodable messageImprintObj = tsReqSeq.getObjectAt(1);
            if (!(messageImprintObj instanceof ASN1Sequence)) {
                return null;
            }

            ASN1Sequence messageImprint = (ASN1Sequence) messageImprintObj;
            if (messageImprint.size() < 2) {
                return null;
            }

            ASN1Encodable hashedMessageObj = messageImprint.getObjectAt(1);
            if (!(hashedMessageObj instanceof ASN1OctetString)) {
                return null;
            }

            ASN1OctetString hashedMessage = (ASN1OctetString) hashedMessageObj;
            byte[] hash = hashedMessage.getOctets();

            LOGGER.debug("Timestamp request hash çıkarıldı. Uzunluk: {} bytes", hash.length);
            return hash;

        } catch (IOException e) {
            LOGGER.warn("Timestamp request parse hatası: {}", e.getMessage());
            return null;
        }
    }
}

