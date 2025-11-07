package io.mersel.dss.signer.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TÜBİTAK zaman damgası kontör sorgulama response DTO.
 */
public class TubitakCreditResponseDto {

    @JsonProperty("remainingCredit")
    private Long remainingCredit;

    @JsonProperty("customerId")
    private Integer customerId;

    @JsonProperty("message")
    private String message;

    public TubitakCreditResponseDto() {
    }

    public TubitakCreditResponseDto(Long remainingCredit, Integer customerId) {
        this.remainingCredit = remainingCredit;
        this.customerId = customerId;
    }

    public TubitakCreditResponseDto(Long remainingCredit, Integer customerId, String message) {
        this.remainingCredit = remainingCredit;
        this.customerId = customerId;
        this.message = message;
    }

    public Long getRemainingCredit() {
        return remainingCredit;
    }

    public void setRemainingCredit(Long remainingCredit) {
        this.remainingCredit = remainingCredit;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

