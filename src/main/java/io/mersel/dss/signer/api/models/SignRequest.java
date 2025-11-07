package io.mersel.dss.signer.api.models;

import io.mersel.dss.signer.api.models.enums.DocumentType;

/**
 * @author erdembas
 */
public class SignRequest {
    private byte[] XmlContent;
    private DocumentType DocumentType;
    private String SignatureId;

    /**
     * @return the XmlContent
     */
    public byte[] getXmlContent() {
        return XmlContent;
    }

    /**
     * @param XmlContent the XmlContentBase64 to set
     */
    public void setXmlContent(byte[] XmlContent) {
        this.XmlContent = XmlContent;
    }

    /**
     * @return the DocumentType
     */
    public DocumentType getDocumentType() {
        return DocumentType;
    }

    /**
     * @param DocumentType the DocumentType to set
     */
    public void setDocumentType(DocumentType DocumentType) {
        this.DocumentType = DocumentType;
    }

    /**
     * @return the SignatureId
     */
    public String getSignatureId() {
        return SignatureId;
    }

    /**
     * @param SignatureId the SignatureId to set
     */
    public void setSignatureId(String SignatureId) {
        this.SignatureId = SignatureId;
    }
}
