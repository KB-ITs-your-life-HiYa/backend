package com.fledge.member.controller;

import com.fledge.common.ApiResponse;
import com.fledge.member.dto.LoginRequest;
import com.fledge.member.dto.LoginResponse;
import com.fledge.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name="인증")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "로그인",
            description = """
                    이메일·비밀번호로 로그인하고 JWT 를 발급받는다.
                    발급된 토큰은 이후 요청의 Authorization 헤더에 `Bearer <token>` 으로 담는다.

                    개발용 계정 (비밀번호 모두 `demo1234`)
                    - `demo1@fledge.dev` — 보호종료 550일차, D-1275
                    - `demo2@fledge.dev` — 보호종료 1645일차, D-180
                    """)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.ok(authService.login(loginRequest));
    }
}
