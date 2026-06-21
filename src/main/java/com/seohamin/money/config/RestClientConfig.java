package com.seohamin.money.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 오픈뱅킹 API 호출용 {@link RestClient} 빈 구성. base-url을 고정하고 연결/응답 타임아웃을 둔다.
 */
@Configuration
@EnableConfigurationProperties(OpenBankingProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient openBankingRestClient(RestClient.Builder builder, OpenBankingProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
