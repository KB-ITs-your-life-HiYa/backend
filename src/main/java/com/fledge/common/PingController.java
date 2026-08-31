package com.fledge.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버가 살아 있는지 확인하는 엔드포인트이자, 컨트롤러 작성 예시.
 *
 * 경로에 /api/v1 을 직접 쓰지 않는다. WebConfig 가 붙여서 실제 경로는 /api/v1/ping 이다.
 * 성공 응답은 ApiResponse.ok(...) 로 감싼다.
 *
 * DB 연결까지 포함한 상태 확인은 actuator 가 한다. → /actuator/health
 */
@Tag(name = "공통")
@RestController
@RequestMapping("/ping")
public class PingController {

    @Operation(summary = "서버 연결 확인")
    @GetMapping
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }
}
