package com.fledge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 컨트롤러 경로 앞에 /api/v1 을 일괄로 붙인다.
 * 컨트롤러는 @RequestMapping("/housing") 만 쓰면 실제 경로가 /api/v1/housing 이 된다.
 *
 * 매번 직접 쓰면 누군가 빠뜨리므로 한 곳에서 강제한다.
 * 대상을 com.fledge 패키지로 한정하는 이유는, springdoc 의 문서 엔드포인트
 * (/v3/api-docs) 까지 prefix 가 붙어 Swagger 가 깨지는 것을 막기 위해서다.
 * actuator(/actuator/health) 도 영향받지 않는다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1", HandlerTypePredicate.forBasePackage("com.fledge"));
    }
}
