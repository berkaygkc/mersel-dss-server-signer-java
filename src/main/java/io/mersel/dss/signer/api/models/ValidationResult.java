package io.mersel.dss.signer.api.models;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.jaxb.object.Message;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.validation.reports.Reports;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private boolean IsValid;
    private String Message;
    private String SerialNumber;
    private String CommonName;
    private List<ValidationResult> Details;

    public ValidationResult() {
        setDetails(new ArrayList<>());
    }

    public ValidationResult(Reports reports) {
        this(reports, null);
    }

    public ValidationResult(Reports reports, String signatureId) {
        this();
        SimpleReport simpleReport = reports != null ? reports.getSimpleReport() : null;
        DiagnosticData diagnosticData = reports != null ? reports.getDiagnosticData() : null;

        if (signatureId == null) {
            signatureId = resolvePrimarySignatureId(simpleReport, diagnosticData);
        }
        if (signatureId == null) {
            setValid(false);
            setMessage("Validation data is missing");
            return;
        }

        Indication indication = simpleReport != null ? simpleReport.getIndication(signatureId) : null;
        boolean passed = simpleReport != null && simpleReport.isValid(signatureId);
        setValid(passed);

        List<String> errors = collectErrors(simpleReport, signatureId);
        List<String> warnings = collectWarnings(simpleReport, signatureId);
        List<String> infos = collectInfos(simpleReport, signatureId);

        setMessage(resolveMessage(indication, errors, warnings, infos, passed));
        populateCertificateMetadata(diagnosticData, signatureId);
        setDetails(buildDetails(errors, warnings, infos));
    }

    @NotBlank
    public boolean isValid() {
        return IsValid;
    }

    public void setValid(boolean valid) {
        IsValid = valid;
    }

    public String getMessage() {
        return Message;
    }

    @NotBlank
    public void setMessage(String message) {
        Message = message;
    }

    public List<ValidationResult> getDetails() {
        return Details;
    }

    @NotBlank
    public void setDetails(List<ValidationResult> details) {
        Details = details;
    }

    public String getSerialNumber() {
        return SerialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        SerialNumber = serialNumber;
    }

    public String getCommonName() {
        return CommonName;
    }

    public void setCommonName(String commonName) {
        CommonName = commonName;
    }

    private static String resolvePrimarySignatureId(SimpleReport simpleReport,
                                                    DiagnosticData diagnosticData) {
        if (simpleReport != null) {
            String first = simpleReport.getFirstSignatureId();
            if (first != null) {
                return first;
            }
            List<String> ids = simpleReport.getSignatureIdList();
            if (ids != null && !ids.isEmpty()) {
                return ids.get(0);
            }
        }
        if (diagnosticData != null) {
            List<String> ids = diagnosticData.getSignatureIdList();
            if (ids != null && !ids.isEmpty()) {
                return ids.get(0);
            }
        }
        return null;
    }

    private List<String> collectErrors(SimpleReport simpleReport, String signatureId) {
        List<String> result = new ArrayList<>();
        if (simpleReport == null || signatureId == null) {
            return result;
        }
        addMessages(result, simpleReport.getAdESValidationErrors(signatureId));
        addMessages(result, simpleReport.getQualificationErrors(signatureId));
        return result;
    }

    private List<String> collectWarnings(SimpleReport simpleReport, String signatureId) {
        List<String> result = new ArrayList<>();
        if (simpleReport == null || signatureId == null) {
            return result;
        }
        addMessages(result, simpleReport.getAdESValidationWarnings(signatureId));
        addMessages(result, simpleReport.getQualificationWarnings(signatureId));
        return result;
    }

    private List<String> collectInfos(SimpleReport simpleReport, String signatureId) {
        List<String> result = new ArrayList<>();
        if (simpleReport == null || signatureId == null) {
            return result;
        }
        addMessages(result, simpleReport.getAdESValidationInfo(signatureId));
        addMessages(result, simpleReport.getQualificationInfo(signatureId));
        return result;
    }

    private void addMessages(List<String> target, List<Message> messages) {
        if (messages == null) {
            return;
        }
        for (Message message : messages) {
            if (message == null) {
                continue;
            }
            String key = message.getKey();
            String value = message.getValue();
            String resolved = (value != null && !value.trim().isEmpty()) ? value.trim() : key;
            if (resolved == null || resolved.isEmpty()) {
                continue;
            }
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty() && !value.contains(key)) {
                resolved = key + ": " + value;
            }
            target.add(resolved);
        }
    }

    private static String resolveMessage(Indication indication,
                                         List<String> errors,
                                         List<String> warnings,
                                         List<String> infos,
                                         boolean passed) {
        if (!errors.isEmpty()) {
            return errors.get(0);
        }
        if (!warnings.isEmpty()) {
            return warnings.get(0);
        }
        if (!infos.isEmpty()) {
            return infos.get(0);
        }
        if (passed) {
            return "Signature validation succeeded";
        }
        return indication != null ? indication.name() : "Signature validation result unavailable";
    }

    private void populateCertificateMetadata(DiagnosticData diagnosticData, String signatureId) {
        if (diagnosticData == null || signatureId == null) {
            return;
        }

        CertificateWrapper certificate = null;
        SignatureWrapper signatureWrapper = diagnosticData.getSignatureById(signatureId);
        if (signatureWrapper != null) {
            certificate = signatureWrapper.getSigningCertificate();
        }

        if ((certificate == null || certificate.getSerialNumber() == null)
                && diagnosticData.getSigningCertificateId(signatureId) != null) {
            String certificateId = diagnosticData.getSigningCertificateId(signatureId);
            certificate = diagnosticData.getUsedCertificateById(certificateId);
        }

        if (certificate == null) {
            return;
        }

        setSerialNumber(certificate.getSerialNumber());
        setCommonName(extractCommonName(certificate.getCertificateDN()));
    }

    private List<ValidationResult> buildDetails(List<String> errors,
                                                List<String> warnings,
                                                List<String> infos) {
        List<ValidationResult> detailList = new ArrayList<>();
        for (String error : errors) {
            detailList.add(createDetail(false, error));
        }
        for (String warning : warnings) {
            detailList.add(createDetail(true, warning));
        }
        for (String info : infos) {
            detailList.add(createDetail(true, info));
        }
        return detailList;
    }

    private ValidationResult createDetail(boolean valid, String message) {
        ValidationResult detail = new ValidationResult();
        detail.setValid(valid);
        detail.setMessage(message);
        detail.setSerialNumber(null);
        detail.setCommonName(null);
        detail.setDetails(new ArrayList<>());
        return detail;
    }

    private String extractCommonName(String distinguishedName) {
        if (distinguishedName == null) {
            return null;
        }
        String[] parts = distinguishedName.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return distinguishedName;
    }
}
