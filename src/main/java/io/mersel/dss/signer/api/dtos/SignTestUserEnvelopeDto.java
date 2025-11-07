package io.mersel.dss.signer.api.dtos;

import javax.validation.constraints.NotBlank;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;

public class SignTestUserEnvelopeDto {
    private MultipartFile Document;
    private io.mersel.dss.signer.api.models.enums.TestCompany TestCompany;

    public MultipartFile getDocument() {
        return Document;
    }

    @NotBlank
    public void setDocument(MultipartFile document) {
        Document = document;
    }

    public io.mersel.dss.signer.api.models.enums.TestCompany getTestCompany() {
        return TestCompany;
    }

    @NotBlank
    @Schema(enumAsRef = true)
    public void setTestCompany(io.mersel.dss.signer.api.models.enums.TestCompany testCompany) {
        TestCompany = testCompany;
    }
}
