package io.mersel.dss.signer.api.models;

import java.security.KeyStore;

/**
 * KeyStore private key girdisi ve alias'ı için wrapper.
 */
public final class SigningKeyEntry {
    
    private final String alias;
    private final KeyStore.PrivateKeyEntry entry;

    public SigningKeyEntry(String alias, KeyStore.PrivateKeyEntry entry) {
        this.alias = alias;
        this.entry = entry;
    }

    public String getAlias() {
        return alias;
    }

    public KeyStore.PrivateKeyEntry getEntry() {
        return entry;
    }
}

