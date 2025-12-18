package io.mersel.dss.signer.api.controllers;

import java.util.UUID;

import io.mersel.dss.signer.api.models.SigningMaterial;
import io.mersel.dss.signer.api.services.signature.cades.CAdESSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.mersel.dss.signer.api.dtos.SignCadesDto;
import io.mersel.dss.signer.api.models.ErrorModel;
import io.mersel.dss.signer.api.models.SignResponse;

/**
 * CAdES (CMS İleri Seviye Elektronik İmza) işlemleri için REST controller.
 * Text/plain formatında string veri alır ve zaman damgalı CAdES imzası oluşturur.
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CadesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CadesController.class);

    private final CAdESSignatureService cadesSignatureService;
    private final SigningMaterial signingMaterial;

    public CadesController(CAdESSignatureService cadesSignatureService,
                          SigningMaterial signingMaterial) {
        this.cadesSignatureService = cadesSignatureService;
        this.signingMaterial = signingMaterial;
    }

    @Operation(
        summary = "Metin içeriğini CAdES imzası ile imzalar",
        description = "Text/plain formatında gelen string veriyi zaman damgalı CAdES-BASELINE-T " +
                      "veya CAdES-BASELINE-B imzası ile imzalar. Çıktı PKCS#7/CMS formatındadır (.p7s)"
    )
    @PostMapping(value = "/v1/cadessign", 
        consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE},
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiResponses({
        @ApiResponse(responseCode = "200", 
            description = "İmzalama başarılı",
            content = @Content(schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "400", 
            description = "Geçersiz istek",
            content = @Content(schema = @Schema(implementation = ErrorModel.class))),
        @ApiResponse(responseCode = "500",
            description = "Sunucu hatası")
    })
    public ResponseEntity<?> signCadesFromPlainText(
            @Parameter(description = "İmzalanacak metin içeriği")
            @RequestBody String content,
            @Parameter(description = "Zaman damgası eklensin mi (varsayılan: true)")
            @RequestParam(value = "includeTimestamp", defaultValue = "true") Boolean includeTimestamp,
            @Parameter(description = "İmza ID'si (opsiyonel)")
            @RequestParam(value = "signatureId", required = false) String signatureId) {
        
        try {
            if (content == null || content.trim().isEmpty()) {
                LOGGER.warn("Geçersiz istek: içerik boş");
                return ResponseEntity.badRequest()
                    .body(new ErrorModel("INVALID_INPUT", "İçerik zorunludur"));
            }

            boolean useTimestamp = includeTimestamp == null || includeTimestamp;

            SignResponse result = cadesSignatureService.signContent(
                content,
                useTimestamp,
                signatureId,
                signingMaterial
            );

            LOGGER.info("CAdES imzası başarıyla oluşturuldu. Zaman damgalı: {}", useTimestamp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "signed-" + UUID.randomUUID() + ".p7s");
            
            if (result.getSignatureValue() != null) {
                headers.set("x-signature-value", result.getSignatureValue());
            }

            return new ResponseEntity<>(result.getSignedDocument(), headers, HttpStatus.OK);

        } catch (Exception e) {
            LOGGER.error("CAdES imzası oluşturulurken hata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorModel("SIGNATURE_FAILED", e.getMessage()));
        }
    }

    @Operation(
        summary = "JSON DTO ile metin içeriğini CAdES imzası ile imzalar",
        description = "JSON formatında gelen DTO'yu kullanarak zaman damgalı CAdES imzası oluşturur"
    )
    @PostMapping(value = "/v1/cadessign/json",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ApiResponses({
        @ApiResponse(responseCode = "200", 
            description = "İmzalama başarılı",
            content = @Content(schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "400", 
            description = "Geçersiz istek",
            content = @Content(schema = @Schema(implementation = ErrorModel.class))),
        @ApiResponse(responseCode = "500",
            description = "Sunucu hatası")
    })
    public ResponseEntity<?> signCadesFromJson(@RequestBody SignCadesDto dto) {
        try {
            if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
                LOGGER.warn("Geçersiz istek: içerik boş");
                return ResponseEntity.badRequest()
                    .body(new ErrorModel("INVALID_INPUT", "İçerik zorunludur"));
            }

            boolean useTimestamp = dto.getIncludeTimestamp() == null || dto.getIncludeTimestamp();

            SignResponse result = cadesSignatureService.signContent(
                dto.getContent(),
                useTimestamp,
                dto.getSignatureId(),
                signingMaterial
            );

            LOGGER.info("CAdES imzası (JSON endpoint) başarıyla oluşturuldu. Zaman damgalı: {}", 
                useTimestamp);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "signed-" + UUID.randomUUID() + ".p7s");
            
            if (result.getSignatureValue() != null) {
                headers.set("x-signature-value", result.getSignatureValue());
            }

            return new ResponseEntity<>(result.getSignedDocument(), headers, HttpStatus.OK);

        } catch (Exception e) {
            LOGGER.error("CAdES imzası oluşturulurken hata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorModel("SIGNATURE_FAILED", e.getMessage()));
        }
    }
}
