package io.mersel.dss.signer.api.models;

import javax.validation.constraints.NotBlank;

public class ErrorModel {
    private String Code;
    private String Message;

    public ErrorModel(String code, String message) {
        Code = code;
        Message = message;
    }


    public String getCode() {
        return Code;
    }

    @NotBlank
    public void setCode(String code) {
        Code = code;
    }

    public String getMessage() {
        return Message;
    }

    @NotBlank
    public void setMessage(String message) {
        Message = message;
    }
}
