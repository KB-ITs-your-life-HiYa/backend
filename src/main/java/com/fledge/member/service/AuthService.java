package com.fledge.member.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.member.domain.Member;
import com.fledge.member.dto.LoginRequest;
import com.fledge.member.dto.LoginResponse;
import com.fledge.member.repository.MemberRepository;
import com.fledge.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.LOGIN_FAILED));
        if(!passwordEncoder.matches(request.password(),
                member.getPasswordHash())) {
            throw new ApiException(ErrorCode.LOGIN_FAILED);
        }

        return LoginResponse.of(member, jwtTokenProvider.createToken(member.getId()));
    }
}
