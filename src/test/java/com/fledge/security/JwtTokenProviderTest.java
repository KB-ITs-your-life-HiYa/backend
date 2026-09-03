package com.fledge.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtTokenProviderTest {

    @Autowired
    JwtTokenProvider provider;

    @Value("${jwt.secret}")
    String secret;

    @Test
    void 토큰을_만들고_다시_읽는다() {
        String token = provider.createToken(2L);
        assertThat(provider.parseMemberId(token)).isEqualTo(2L);
    }

    @Test
    void 서명이_위조되면_null_이다() {
        String[] parts = provider.createToken(2L).split("\\.");
        String signature = parts[2];
        String tampered = signature.substring(0, signature.length() - 1)
                + (signature.endsWith("A") ? "B" : "A");

        assertThat(provider.parseMemberId(parts[0] + "." + parts[1] + "." + tampered)).isNull();
    }

    @Test
    void 내용을_바꾸면_null_이다() {
        String[] parts = provider.createToken(2L).split("\\.");
        // 회원 id 를 999 로 바꾼 페이로드로 갈아끼운다. 서명은 그대로 두었으므로 맞지 않는다
        String forged = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"999\"}".getBytes());

        assertThat(provider.parseMemberId(parts[0] + "." + forged + "." + parts[2])).isNull();
    }

    @Test
    void 만료된_토큰은_null_이다() {
        // 유효기간을 음수로 준 provider 로 이미 만료된 토큰을 만든다
        JwtTokenProvider expired = new JwtTokenProvider(secret, -1);

        assertThat(provider.parseMemberId(expired.createToken(2L))).isNull();
    }

    @Test
    void 토큰이_아니면_null_이다() {
        assertThat(provider.parseMemberId("아무말")).isNull();
        assertThat(provider.parseMemberId("")).isNull();
        assertThat(provider.parseMemberId("a.b.c")).isNull();
    }
}
