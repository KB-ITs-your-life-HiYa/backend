package com.fledge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 에 표시될 문서 제목·설명을 지정한다.
 * 이 설정이 없으면 "OpenAPI definition v0" 으로 뜬다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fledgeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("자립동행 D-1825 API")
                .description("자립준비청년 금융 자립 플랫폼 백엔드 API")
                .version("v0.0.1"));
    }
}
