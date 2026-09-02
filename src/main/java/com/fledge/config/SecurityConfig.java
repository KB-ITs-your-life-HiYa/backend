package com.fledge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.common.ApiResponse;
import com.fledge.common.ErrorCode;
import com.fledge.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // 토큰 없이 접근할 수 있는 경로
    private static final String[] PUBLIC = {
            "/api/v1/auth/**",
            "/api/v1/ping",
            "/actuator/health",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 브라우저 폼이 아니라 앱에서 토큰으로 호출하므로 CSRF 보호가 필요없다
                .csrf(csrf -> csrf.disable())
                // 로그인 폼·브라우저 기본 인증창 사용X
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 세션을 만들지 않는다. 인증 상태는 매 요청이 토큰으로만 판단
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // WebConfig의 CORS 설정을 시큐리티 필터에서도 적용
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(this::unauthorized));
        return http.build();
    }

    // 인증이 필요한데 없을 때. 기본 HTML 대신 우리 공통 응답 형식으로 답한다
    private void unauthorized(jakarta.servlet.http.HttpServletRequest request,
                              jakarta.servlet.http.HttpServletResponse response,
                              org.springframework.security.core.AuthenticationException e)
            throws java.io.IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage()));
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
