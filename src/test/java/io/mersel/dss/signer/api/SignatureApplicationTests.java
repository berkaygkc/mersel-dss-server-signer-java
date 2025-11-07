package io.mersel.dss.signer.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SignatureApplication ana sınıfının temel test'leri.
 * 
 * Not: Tam Spring context integration testleri için gerçek keystore gereklidir.
 * Bu testler sadece temel işlevselliği doğrular.
 */
class SignatureApplicationTests {

    @Test
    void testApplicationConstants() {
        // Given/When/Then
        assertNotNull(SignatureApplication.FileSeparator);
        assertEquals(System.getProperty("file.separator"), SignatureApplication.FileSeparator);
        
        assertNotNull(SignatureApplication.ROOT_FILE_FOLDER);
        assertEquals(".mersel-signature-service", SignatureApplication.ROOT_FILE_FOLDER);
        
        assertNotNull(SignatureApplication.ROOT_DIR);
        assertTrue(SignatureApplication.ROOT_DIR.contains(SignatureApplication.ROOT_FILE_FOLDER));
    }

    @Test
    void testMainMethodExists() throws NoSuchMethodException {
        // When - Main metodunun varlığını kontrol et
        java.lang.reflect.Method mainMethod = SignatureApplication.class.getMethod("main", String[].class);
        
        // Then
        assertNotNull(mainMethod);
        assertEquals(void.class, mainMethod.getReturnType());
    }
}

