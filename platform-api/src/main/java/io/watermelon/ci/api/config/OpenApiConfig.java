package io.watermelon.ci.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Watermelon CI API")
                        .version("0.1.0")
                        .description(
                                "Enterprise CI/CD platform API: pipelines, registries, deployments, issues, RBAC"));
    }
}
