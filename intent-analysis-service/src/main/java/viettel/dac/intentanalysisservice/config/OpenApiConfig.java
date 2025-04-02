package viettel.dac.intentanalysisservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration for OpenAPI documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI intentAnalysisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Intent Analysis Service API")
                        .description("API for analyzing user intents and extracting parameters using LLM")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Intent Analysis Team")
                                .email("intent.analysis@example.com")
                                .url("https://wiki.example.com/intent-analysis"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://example.com/licenses/internal")))
                .externalDocs(new ExternalDocumentation()
                        .description("Intent Analysis System Documentation")
                        .url("https://wiki.example.com/intent-analysis"))
                .servers(Arrays.asList(
                        new Server().url("/").description("Current environment"),
                        new Server().url("https://dev.example.com/intent-analysis").description("Development"),
                        new Server().url("https://staging.example.com/intent-analysis").description("Staging"),
                        new Server().url("https://prod.example.com/intent-analysis").description("Production")))
                .addTagsItem(new Tag().name("Intent Analysis").description("APIs for analyzing user intents"))
                .addTagsItem(new Tag().name("Analysis History").description("APIs for retrieving analysis history"))
                .addTagsItem(new Tag().name("Batch Processing").description("APIs for batch processing of intents"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Use a JWT token for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}