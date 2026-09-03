package com.fledge.care.controller;

import com.fledge.care.dto.CareDto.*;
import com.fledge.care.service.CareService;
import com.fledge.common.ApiResponse;
import com.fledge.security.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Profile("local & !prod & !supabase")
@ConditionalOnProperty(name = "care.demo.enabled", havingValue = "true")
@RequestMapping("/members/me/care/demo")
public class CareDemoController {
    private final CareService care;

    @PostMapping("/date")
    public ApiResponse<Summary> date(@AuthenticationPrincipal AuthenticatedMember me,
                                    @Valid @RequestBody DemoDateRequest request) {
        return ApiResponse.ok(care.setDemoDate(me.id(), request.date()));
    }

    @PostMapping("/reset")
    public ApiResponse<Summary> reset(@AuthenticationPrincipal AuthenticatedMember me) {
        return ApiResponse.ok(care.resetDemo(me.id()));
    }
}
