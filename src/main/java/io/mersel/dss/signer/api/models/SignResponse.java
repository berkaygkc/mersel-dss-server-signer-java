package io.mersel.dss.signer.api.models;

import javax.validation.constraints.NotBlank;

public class SignResponse {
    private byte[] SignedDocument;
    private String SignatureValue;

    public SignResponse(byte[] signedDocument, String signatureValue) {
        SignedDocument = signedDocument;
        SignatureValue = signatureValue;
    }

    public byte[] getSignedDocument() {
        return SignedDocument;
    }

    @NotBlank
    public void setSignedDocument(byte[] SignedDocument) {
        this.SignedDocument = SignedDocument;
    }

    public String getSignatureValue() {
        return SignatureValue;
    }

    public void setSignatureValue(String SignatureValue) {
        this.SignatureValue = SignatureValue;
    }
}
