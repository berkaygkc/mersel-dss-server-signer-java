package io.mersel.dss.signer.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security yapılandırması test'leri.
 * Sadece CORS yapılandırmasını test eder.
 */
@SpringBootTest(classes = {
    io.mersel.dss.signer.api.config.SecurityConfiguration.class
})
@TestPropertySource(properties = {
    "cors.allowed-origins=https://example.com,https://test.com",
    "cors.allowed-methods=GET,POST",
    "cors.max-age=7200"
})
class SecurityConfigurationTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void testCorsConfigurationLoaded() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        // When
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

        // Then
        assertNotNull(config);
        assertNotNull(config.getAllowedOrigins());
        assertNotNull(config.getAllowedMethods());
    }

    @Test
    void testCorsAllowedMethods() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        // When
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

        // Then
        assertNotNull(config);
        assertNotNull(config.getAllowedMethods());
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
    }

    @Test
    void testCorsExposedHeaders() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        // When
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

        // Then
        assertNotNull(config);
        assertNotNull(config.getExposedHeaders());
        assertTrue(config.getExposedHeaders().contains("x-signature-value"));
        assertTrue(config.getExposedHeaders().contains("Content-Disposition"));
    }

    @Test
    void testCorsMaxAge() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        // When
        CorsConfiguration config = corsConfigurationSource.getCorsConfiguration(request);

        // Then
        assertNotNull(config);
        assertEquals(7200L, config.getMaxAge());
    }
}

