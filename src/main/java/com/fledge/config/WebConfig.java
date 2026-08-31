package com.fledge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 컨트롤러 경로 prefix 와 CORS 설정.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 브라우저가 API 를 호출할 수 있는 출처.
     * 값은 application.properties 에서 프로필별로 바꾼다.
     */
    @Value("${cors.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;

    /**
     * 컨트롤러 경로 앞에 /api/v1 을 일괄로 붙인다.
     * 컨트롤러는 @RequestMapping("/housing") 만 쓰면 실제 경로가 /api/v1/housing 이 된다.
     *
     * 매번 직접 쓰면 누군가 빠뜨리므로 한 곳에서 강제한다.
     * 대상을 com.fledge 패키지로 한정하는 이유는, springdoc 의 문서 엔드포인트
     * (/v3/api-docs) 까지 prefix 가 붙어 Swagger 가 깨지는 것을 막기 위해서다.
     * actuator(/actuator/health) 도 영향받지 않는다.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", HandlerTypePredicate.forBasePackage("com.fledge"));
    }

    /**
     * 프론트를 웹(expo start 후 w)으로 띄우면 출처가 localhost:8081 이라
     * localhost:8080 인 백엔드 호출이 브라우저에서 차단된다. 그것을 열어준다.
     *
     * 실기기(Expo Go)는 브라우저가 아니라 네이티브 fetch 라 CORS 와 무관하다.
     *
     * allowCredentials 는 켜지 않는다. 인증은 쿠키가 아니라 Authorization 헤더로 하므로
     * 필요 없고, 켜면 출처를 와일드카드로 둘 수 없게 된다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
