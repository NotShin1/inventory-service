package com.devops.inventory_service.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shin Inventory API Service")
                        .version("V3.0")
                        .description("API document cho hệ thống quản lý kho Inventory - Made by Shin DevSecOps")
                        .contact(new Contact()
                                .name("Shin DevSecOps")
                                .url("https://shin-devops.me")
                                .email("contact@shin-devops.me"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .servers(List.of(

                        new Server().url("https://api.shin-devops.me").description("Production Server"),
                        new Server().url("http://localhost:8080").description("Local Development")
                ))

                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}