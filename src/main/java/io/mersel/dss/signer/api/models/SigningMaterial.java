package io.mersel.dss.signer.api.models;

import eu.europa.esig.dss.model.x509.CertificateToken;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Private key ve sertifika zinciri dahil imzalama materyalini kapsüller.
 * Thread-safety sağlamak için immutable'dır.
 */
public final class SigningMaterial {
    
    private final PrivateKey privateKey;
    private final X509Certificate signingCertificate;
    private final List<X509Certificate> certificateChain;
    private final List<CertificateToken> certificateTokens;

    public SigningMaterial(PrivateKey privateKey, 
                          X509Certificate signingCertificate,
                          List<X509Certificate> certificateChain) {
        this.privateKey = privateKey;
        this.signingCertificate = signingCertificate;
        this.certificateChain = Collections.unmodifiableList(new ArrayList<>(certificateChain));
        this.certificateTokens = this.certificateChain.stream()
                .map(CertificateToken::new)
                .collect(Collectors.toList());
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }

    public List<X509Certificate> getCertificateChain() {
        return certificateChain;
    }

    public List<CertificateToken> getCertificateTokens() {
        return certificateTokens;
    }

    public CertificateToken getPrimaryCertificateToken() {
        return certificateTokens.get(0);
    }
}

