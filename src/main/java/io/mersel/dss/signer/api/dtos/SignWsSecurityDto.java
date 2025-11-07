package io.mersel.dss.signer.api.dtos;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

public class SignWsSecurityDto {
    private MultipartFile Document;
    private Boolean Soap1Dot2;

    public MultipartFile getDocument() {
        return Document;
    }

    @NotBlank
    public void setDocument(MultipartFile document) {
        Document = document;
    }


    public Boolean getSoap1Dot2() {
        return Soap1Dot2;
    }

    public void setSoap1Dot2(Boolean soap1Dot2) {
        Soap1Dot2 = soap1Dot2;
    }
}
