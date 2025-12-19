package io.mersel.dss.signer.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Hash imzalama yanıtı için DTO.
 * İmza değeri ve sertifika bilgilerini içerir.
 */
@Schema(description = "Hash imzalama yanıtı")
public class SignHashResponseDto {

    @Schema(description = "Base64 encoded imza değeri")
    private String signatureValue;

    @Schema(description = "Base64 encoded imzalama sertifikası (DER format)")
    private String certificate;

    @Schema(description = "Base64 encoded sertifika zinciri (PKCS7 format)")
    private String certificateChain;

    @Schema(description = "Kullanılan imza algoritması")
    private String signatureAlgorithm;

    public String getSignatureValue() {
        return signatureValue;
    }

    public void setSignatureValue(String signatureValue) {
        this.signatureValue = signatureValue;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain(String certificateChain) {
        this.certificateChain = certificateChain;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }
}
