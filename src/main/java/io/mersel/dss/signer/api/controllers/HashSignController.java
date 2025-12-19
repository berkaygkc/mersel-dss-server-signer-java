package io.mersel.dss.signer.api.controllers;

import io.mersel.dss.signer.api.dtos.SignHashRequestDto;
import io.mersel.dss.signer.api.dtos.SignHashResponseDto;
import io.mersel.dss.signer.api.models.ErrorModel;
import io.mersel.dss.signer.api.models.SigningMaterial;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Hash imzalama için REST controller.
 * Client tarafında hazırlanan hash'i imzalar ve imza değeri ile sertifika döner.
 * XAdES remote signing senaryoları için idealdir.
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "Hash Sign", description = "Doğrudan hash imzalama işlemleri")
public class HashSignController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashSignController.class);

    private final SigningMaterial signingMaterial;

    public HashSignController(SigningMaterial signingMaterial) {
        this.signingMaterial = signingMaterial;
    }

    @Operation(
        summary = "Hash değerini imzalar",
        description = "Client tarafında hazırlanan hash değerini imzalar ve imza değeri ile sertifika bilgilerini döner.\n\n" +
                      "**Kullanım Senaryosu:**\n" +
                      "1. Client, SignedInfo XML'ini oluşturur\n" +
                      "2. Client, SignedInfo'nun hash'ini hesaplar (SHA-256)\n" +
                      "3. Client, hash'i Base64 encode ederek bu endpoint'e gönderir\n" +
                      "4. Server, hash'i imzalar ve signature value + certificate döner\n" +
                      "5. Client, dönen değerleri XAdES imzasına yerleştirir\n\n" +
                      "**Desteklenen Hash Algoritmaları:**\n" +
                      "- SHA-256 (varsayılan)\n" +
                      "- SHA-384\n" +
                      "- SHA-512\n" +
                      "- SHA-1 (önerilmez)"
    )
    @PostMapping(value = "/v1/signhash",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "Hash başarıyla imzalandı",
            content = @Content(schema = @Schema(implementation = SignHashResponseDto.class))),
        @ApiResponse(responseCode = "400",
            description = "Geçersiz istek",
            content = @Content(schema = @Schema(implementation = ErrorModel.class))),
        @ApiResponse(responseCode = "500",
            description = "Sunucu hatası",
            content = @Content(schema = @Schema(implementation = ErrorModel.class)))
    })
    public ResponseEntity<?> signHash(@RequestBody SignHashRequestDto request) {
        try {
            // Validasyon
            if (request.getHash() == null || request.getHash().trim().isEmpty()) {
                LOGGER.warn("Hash imzalama isteği reddedildi: hash boş");
                return ResponseEntity.badRequest()
                    .body(new ErrorModel("INVALID_INPUT", "Hash değeri zorunludur"));
            }

            // Hash'i decode et
            byte[] hashBytes;
            try {
                hashBytes = Base64.getDecoder().decode(request.getHash());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Hash imzalama isteği reddedildi: geçersiz Base64");
                return ResponseEntity.badRequest()
                    .body(new ErrorModel("INVALID_BASE64", "Hash değeri geçerli Base64 formatında olmalıdır"));
            }

            // İmza algoritmasını belirle
            String signatureAlgorithm = getSignatureAlgorithm(request.getHashAlgorithm());
            
            LOGGER.info("Hash imzalama başlıyor. Hash algoritması: {}, İmza algoritması: {}", 
                request.getHashAlgorithm(), signatureAlgorithm);

            // Hash'i imzala
            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initSign(signingMaterial.getPrivateKey());
            signature.update(hashBytes);
            byte[] signatureBytes = signature.sign();

            // Response oluştur
            SignHashResponseDto response = new SignHashResponseDto();
            response.setSignatureValue(Base64.getEncoder().encodeToString(signatureBytes));
            response.setSignatureAlgorithm(signatureAlgorithm);
            
            // Sertifika bilgilerini ekle
            X509Certificate cert = signingMaterial.getSigningCertificate();
            response.setCertificate(Base64.getEncoder().encodeToString(cert.getEncoded()));
            
            // Sertifika zincirini PKCS7 formatında ekle
            response.setCertificateChain(buildCertificateChainPEM());

            LOGGER.info("Hash başarıyla imzalandı. İmza boyutu: {} bytes", signatureBytes.length);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOGGER.error("Hash imzalama hatası", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorModel("SIGN_ERROR", "Hash imzalama başarısız: " + e.getMessage()));
        }
    }

    /**
     * Hash algoritmasına göre imza algoritmasını belirler.
     * RSA key için SHA*withRSA, EC key için SHA*withECDSA kullanılır.
     */
    private String getSignatureAlgorithm(String hashAlgorithm) {
        String keyAlgorithm = signingMaterial.getPrivateKey().getAlgorithm();
        String normalizedHash = hashAlgorithm.toUpperCase().replace("-", "");
        
        if ("RSA".equals(keyAlgorithm)) {
            switch (normalizedHash) {
                case "SHA1":
                    return "SHA1withRSA";
                case "SHA384":
                    return "SHA384withRSA";
                case "SHA512":
                    return "SHA512withRSA";
                case "SHA256":
                default:
                    return "SHA256withRSA";
            }
        } else if ("EC".equals(keyAlgorithm) || "ECDSA".equals(keyAlgorithm)) {
            switch (normalizedHash) {
                case "SHA1":
                    return "SHA1withECDSA";
                case "SHA384":
                    return "SHA384withECDSA";
                case "SHA512":
                    return "SHA512withECDSA";
                case "SHA256":
                default:
                    return "SHA256withECDSA";
            }
        }
        
        // Fallback
        return "SHA256withRSA";
    }

    /**
     * Sertifika zincirini PEM formatında birleştirir.
     */
    private String buildCertificateChainPEM() throws CertificateEncodingException {
        StringBuilder pemChain = new StringBuilder();
        
        for (X509Certificate cert : signingMaterial.getCertificateChain()) {
            pemChain.append("-----BEGIN CERTIFICATE-----\n");
            pemChain.append(Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(cert.getEncoded()));
            pemChain.append("\n-----END CERTIFICATE-----\n");
        }
        
        return pemChain.toString();
    }
}
