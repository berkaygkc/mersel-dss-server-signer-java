package io.mersel.dss.signer.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CAdES imzalama isteği için DTO.
 * Farklı zaman damgası türlerini destekler: none, signature, content, archive, esc, all.
 */
public class SignCadesDto {

    @Schema(description = "İmzalanacak metin içeriği (text/plain)")
    private String content;

    @Schema(description = "Zaman damgası türü: none, signature, content, archive, esc, all", 
            defaultValue = "signature",
            allowableValues = {"none", "signature", "content", "archive", "esc", "all"})
    private String timestampType;

    @Schema(description = "İmza ID'si (opsiyonel)")
    private String signatureId;
    
    @Schema(description = "Geriye uyumluluk için - timestampType kullanın", deprecated = true)
    private Boolean includeTimestamp;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestampType() {
        return timestampType;
    }

    public void setTimestampType(String timestampType) {
        this.timestampType = timestampType;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(String signatureId) {
        this.signatureId = signatureId;
    }

    public Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(Boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }
    
    /**
     * Geriye uyumluluk için - timestampType veya includeTimestamp'tan türü belirler.
     */
    public String getEffectiveTimestampType() {
        if (timestampType != null && !timestampType.trim().isEmpty()) {
            return timestampType;
        }
        // Geriye uyumluluk
        if (includeTimestamp != null) {
            return includeTimestamp ? "signature" : "none";
        }
        return "signature"; // varsayılan
    }
}
