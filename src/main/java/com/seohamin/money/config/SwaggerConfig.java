package com.seohamin.money.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                .info(new Info()
                        .title("money-check API")
                        .description("KFTC 오픈뱅킹 잔액조회 서비스 API (사용자인증 → 토큰 → 계좌 → 잔액)")
                        .version("v0.0.1"));
    }
}
