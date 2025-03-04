package su.opencode.digsigcik.pvv_rr_m4d_adapter.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Dev-Team",
                        url = "https://www.o-code.ru/",
                        email = "vereschagina@o-code.ru"

                ),
                title = "API Documentation",
                version = "2.8"
        )
)

public class SwaggerConfig {
    @Value("${springdoc.server.url}")
    private String url;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(Collections.singletonList(
                        new Server().url(url)
                                .description("Default Server URL")
                ));
    }
}
