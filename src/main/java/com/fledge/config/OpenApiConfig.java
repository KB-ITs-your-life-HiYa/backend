package com.fledge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 에 표시될 문서 제목·설명을 지정한다.
 * 이 설정이 없으면 "OpenAPI definition v0" 으로 뜬다.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("자립동행 D-1825 API")
                        .description("자립준비청년 자립 지원 플랫폼")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        /auth/login 으로 받은 token 값을 붙여넣는다.
                                        `Bearer ` 는 자동으로 붙으므로 토큰만 넣는다.
                                        """)))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}
