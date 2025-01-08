package kr.hhplus.be.server.common.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Concert Reservation API")
                .version("v1.0")
                .description("콘서트 예약 시스템 API 문서");

        return new OpenAPI()
                .info(info);
    }
}
