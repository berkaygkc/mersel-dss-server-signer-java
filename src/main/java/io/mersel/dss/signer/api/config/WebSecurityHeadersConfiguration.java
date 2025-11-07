package io.mersel.dss.signer.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Güvenlik header'ları yapılandırması.
 * 
 * Security best practices için HTTP response header'larını ayarlar.
 */
@Configuration
public class WebSecurityHeadersConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityHeadersInterceptor());
    }

    /**
     * Güvenlik header'larını ekleyen interceptor.
     */
    private static class SecurityHeadersInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, 
                                HttpServletResponse response, 
                                Object handler) throws Exception {
            
            // X-Content-Type-Options - MIME type sniffing'i engelle
            response.setHeader("X-Content-Type-Options", "nosniff");
            
            // X-Frame-Options - Clickjacking koruması
            response.setHeader("X-Frame-Options", "DENY");
            
            // X-XSS-Protection - XSS koruması
            response.setHeader("X-XSS-Protection", "1; mode=block");
            
            // Referrer-Policy - Referrer bilgisi politikası
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            
            // Cache-Control - Hassas veriler için
            if (request.getRequestURI().contains("/v1/")) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
            
            return true;
        }
    }
}

