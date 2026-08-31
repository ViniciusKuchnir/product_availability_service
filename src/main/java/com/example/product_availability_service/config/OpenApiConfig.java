package com.example.product_availability_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productAvailabilityOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Product Availability API")
                                .description("""
                                        REST API for managing product availability,
                                        stock levels and product popularity.
                                        
                                        PostgreSQL is used as the source of truth,
                                        while Redis provides product caching and
                                        view-based trending rankings.
                                        """)
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("Vinícius Kuchnir")
                                )
                );
    }
}
