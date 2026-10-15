package com.platform.creatorstudio.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI configuration for Creator Studio Service.
 * Configures API documentation groups and settings.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi creatorStudioApi() {
        return GroupedOpenApi.builder()
                .group("creator-studio-service")
                .pathsToMatch("/api/v1/**")
                .build();
    }
}