package sn.immosenegal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SCHEMA_BEARER = "bearerAuth";

    @Bean
    OpenAPI immoSenegalOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("ImmoSénégal API")
                .version("0.1.0")
                .description("API REST de la plateforme immobilière ImmoSénégal (projet de mémoire L3)."))
            .components(new Components().addSecuritySchemes(SCHEMA_BEARER,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
