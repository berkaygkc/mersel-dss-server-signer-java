package io.mersel.dss.signer.api.dtos;

import javax.validation.constraints.NotBlank;

import org.springframework.web.multipart.MultipartFile;

public class SignPadesDto {
    private MultipartFile Document;
    private MultipartFile Attachment;
    private String AttachmentFileName;
    private Boolean AppendMode;

    public MultipartFile getDocument() {
        return Document;
    }

    @NotBlank
    public void setDocument(MultipartFile document) {
        Document = document;
    }

    public MultipartFile getAttachment() {
        return Attachment;
    }

    public void setAttachment(MultipartFile attachment) {
        Attachment = attachment;
    }

    public String getAttachmentFileName() {
        return AttachmentFileName;
    }

    public void setAttachmentFileName(String attachmentFileName) {
        AttachmentFileName = attachmentFileName;
    }

    public Boolean getAppendMode() {
        return AppendMode;
    }

    public void setAppendMode(Boolean appendMode) {
        AppendMode = appendMode;
    }
}
