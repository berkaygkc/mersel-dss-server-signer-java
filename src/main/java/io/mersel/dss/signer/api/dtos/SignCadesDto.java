package io.mersel.dss.signer.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CAdES imzalama isteği için DTO.
 * Text/plain formatında string veri alır ve zaman damgalı CAdES imzası oluşturur.
 */
public class SignCadesDto {

    @Schema(description = "İmzalanacak metin içeriği (text/plain)")
    private String content;

    @Schema(description = "Zaman damgası eklensin mi (varsayılan: true)", defaultValue = "true")
    private Boolean includeTimestamp;

    @Schema(description = "İmza ID'si (opsiyonel)")
    private String signatureId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    public void setIncludeTimestamp(Boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
    }

    public String getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(String signatureId) {
        this.signatureId = signatureId;
    }
}
