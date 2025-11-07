package io.mersel.dss.signer.api.configurations;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI CustomOpenApiBean() {
        return new OpenAPI().components(new Components())
                .info(new Info()
                        .title("MERSEL YAZILIM A.S. - Signature API")
                        .version("v1.0.0")
                        .description("İmza işlemleri için kullanılabilir. [https://mersel.io](https://mersel.io)"));
    }
}