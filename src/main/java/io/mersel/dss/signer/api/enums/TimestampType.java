package io.mersel.dss.signer.api.enums;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * CAdES imzalarında kullanılabilecek zaman damgası türleri.
 * 
 * <p>Her tür farklı bir CAdES seviyesine karşılık gelir:
 * <ul>
 *   <li>NONE: Zaman damgası yok (CAdES-B)</li>
 *   <li>SIGNATURE: İmza zaman damgası (CAdES-T)</li>
 *   <li>CONTENT: İçerik zaman damgası (CAdES-T)</li>
 *   <li>ARCHIVE: Arşiv zaman damgası (CAdES-A)</li>
 *   <li>ALL: Hem imza hem arşiv zaman damgası</li>
 * </ul>
 */
public enum TimestampType {
    
    /**
     * Zaman damgası yok - CAdES-B seviyesi
     */
    NONE("none", null, "Zaman damgası yok"),
    
    /**
     * İmza zaman damgası - CAdES-T seviyesi
     * İmza değerinin hash'i üzerine atılır.
     * OID: 1.2.840.113549.1.9.16.2.14
     */
    SIGNATURE("signature", PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, "İmza zaman damgası"),
    
    /**
     * İçerik zaman damgası - CAdES-T seviyesi
     * İmzalanan içeriğin hash'i üzerine atılır.
     * OID: 1.2.840.113549.1.9.16.2.20
     */
    CONTENT("content", PKCSObjectIdentifiers.id_aa_ets_contentTimestamp, "İçerik zaman damgası"),
    
    /**
     * Arşiv zaman damgası - CAdES-A seviyesi
     * Tüm imza yapısının hash'i üzerine atılır. Uzun süreli arşivleme için kullanılır.
     * OID: 0.4.0.1733.2.5 (ETSI)
     */
    ARCHIVE("archive", new ASN1ObjectIdentifier("0.4.0.1733.2.5"), "Arşiv zaman damgası"),
    
    /**
     * ESC zaman damgası - CAdES-XL seviyesi
     * Extended Signature and Certificate üzerine atılır.
     * OID: 0.4.0.1733.2.4 (ETSI)
     */
    ESC("esc", new ASN1ObjectIdentifier("0.4.0.1733.2.4"), "ESC zaman damgası"),
    
    /**
     * Hem imza hem arşiv zaman damgası
     * CAdES-A seviyesi için tam destek.
     */
    ALL("all", null, "İmza ve arşiv zaman damgası");

    private final String value;
    private final ASN1ObjectIdentifier oid;
    private final String description;

    TimestampType(String value, ASN1ObjectIdentifier oid, String description) {
        this.value = value;
        this.oid = oid;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public ASN1ObjectIdentifier getOid() {
        return oid;
    }

    public String getDescription() {
        return description;
    }

    /**
     * String değerden TimestampType döndürür.
     * Büyük/küçük harf duyarsız.
     */
    public static TimestampType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }
        for (TimestampType type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        // Geriye uyumluluk: "true" -> SIGNATURE, "false" -> NONE
        if ("true".equalsIgnoreCase(value.trim())) {
            return SIGNATURE;
        }
        return NONE;
    }
}
