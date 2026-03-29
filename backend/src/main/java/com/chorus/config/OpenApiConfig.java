package com.chorus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chorusOpenApi(
            @Value("${server.port:8080}") int serverPort,
            @Value("${chorus.open-api.server-url:}") String configuredServerUrl) {
        String defaultUrl = "http://localhost:" + serverPort;
        String url = configuredServerUrl == null || configuredServerUrl.isBlank() ? defaultUrl : configuredServerUrl;
        return new OpenAPI()
                .info(new Info()
                        .title("Chorus API")
                        .description(
                                "REST endpoints for collaborative rooms. Real-time chat uses WebSocket/STOMP at `/ws` — see repository `docs/contract.md`.")
                        .version("0.0.1")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(new Server().url(url).description("API base")));
    }
}
