package io.mersel.dss.signer.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Hash imzalama isteği için DTO.
 * Client tarafında hazırlanan hash'i imzalamak için kullanılır.
 */
@Schema(description = "Hash imzalama isteği")
public class SignHashRequestDto {

    @Schema(description = "Base64 encoded hash değeri", required = true, example = "SGVsbG8gV29ybGQ=")
    private String hash;

    @Schema(description = "Hash algoritması", example = "SHA-256", defaultValue = "SHA-256")
    private String hashAlgorithm = "SHA-256";

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }
}
