package com.seohamin.money.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 설정. Swagger UI: /swagger-ui.html, OpenAPI 문서(JSON): /v3/api-docs.
 * 컨트롤러는 springdoc가 자동 스캔하며, 여기서는 문서 메타데이터만 정의한다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI moneyCheckOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("money-check API")
                        .description("JWT 로그인 기반 KFTC 오픈뱅킹 계좌·잔액조회 API")
                        .version("v0.0.1"));
    }
}
