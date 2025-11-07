package io.mersel.dss.signer.api.controllers;

import io.mersel.dss.signer.api.dtos.TubitakCreditResponseDto;
import io.mersel.dss.signer.api.exceptions.TimestampException;
import io.mersel.dss.signer.api.models.ErrorModel;
import io.mersel.dss.signer.api.services.timestamp.tubitak.TubitakCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TÜBİTAK E-SYA zaman damgası servisi için özel işlemler.
 */
@RestController
@RequestMapping("/api/tubitak")
@Tag(name = "TÜBİTAK", description = "TÜBİTAK E-SYA zaman damgası servisi işlemleri")
public class TubitakController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TubitakController.class);

    private final TubitakCreditService tubitakCreditService;

    public TubitakController(TubitakCreditService tubitakCreditService) {
        this.tubitakCreditService = tubitakCreditService;
    }

    @Operation(
        summary = "TÜBİTAK zaman damgası kontör bilgisini sorgular",
        description = "TÜBİTAK E-SYA zaman damgası servisi için kalan kontör miktarını döndürür. " +
                     "Bu endpoint sadece IS_TUBITAK_TSP=true olarak yapılandırılmışsa kullanılabilir."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Kontör bilgisi başarıyla sorgulandı",
            content = @Content(schema = @Schema(implementation = TubitakCreditResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "TÜBİTAK modu aktif değil veya yapılandırma eksik",
            content = @Content(schema = @Schema(implementation = ErrorModel.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Kontör sorgulaması başarısız",
            content = @Content(schema = @Schema(implementation = ErrorModel.class))
        )
    })
    @GetMapping("/credit")
    public ResponseEntity<?> getCreditInfo() {
        try {
            LOGGER.info("TÜBİTAK kontör sorgulama isteği alındı");

            if (!tubitakCreditService.isAvailable()) {
                LOGGER.warn("TÜBİTAK servisi kullanılamıyor - yapılandırma kontrol edin");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorModel(
                        "TUBITAK_NOT_CONFIGURED",
                        "TÜBİTAK zaman damgası modu aktif değil veya yapılandırma eksik. " +
                        "IS_TUBITAK_TSP=true ve kullanıcı kimlik bilgilerini kontrol edin."
                    ));
            }

            TubitakCreditResponseDto creditInfo = tubitakCreditService.checkCredit();

            LOGGER.info("TÜBİTAK kontör sorgulaması başarılı. Kalan kontör: {}",
                    creditInfo.getRemainingCredit());

            return ResponseEntity.ok(creditInfo);

        } catch (TimestampException e) {
            LOGGER.error("TÜBİTAK kontör sorgulaması başarısız: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorModel("CREDIT_CHECK_FAILED", e.getMessage()));

        } catch (Exception e) {
            LOGGER.error("TÜBİTAK kontör sorgulaması sırasında beklenmeyen hata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorModel("INTERNAL_ERROR", "Kontör sorgulaması başarısız: " + e.getMessage()));
        }
    }
}

