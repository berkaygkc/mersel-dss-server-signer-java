package io.mersel.dss.signer.api.models;

/**
 * Anahtar alias'ı ve imzalama materyalini içeren imzalama context'i.
 */
public final class SigningContext {
    
    private final String alias;
    private final SigningMaterial material;

    public SigningContext(String alias, SigningMaterial material) {
        this.alias = alias;
        this.material = material;
    }

    public String getAlias() {
        return alias;
    }

    public SigningMaterial getMaterial() {
        return material;
    }
}

