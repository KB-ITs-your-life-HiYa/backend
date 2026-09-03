package com.fledge.housing.collect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 마이홈포털 API 응답 한 행. 공고 정보와 단지 정보가 한 행에 섞여 있다.
 *
 * 【전부 String 으로 받는 이유】
 * 같은 필드에 숫자와 빈 문자열이 섞여 온다. 실측에서 totHshldCo 가 319행 중
 * 254행은 숫자, 65행은 "" 였다. Integer 로 받으면 역직렬화에서 터지고
 * 그 페이지 수집이 통째로 실패한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MyHomeItem(
        // 공고
        String pblancId,
        String pblancNm,
        String suplyInsttNm,
        String houseTyNm,
        String suplyTyNm,
        String sttusNm,
        String beforePblancId,
        String rcritPblancDe,
        String beginDe,
        String endDe,
        String przwnerPresnatnDe,
        String refrnc,
        String url,
        String pcUrl,

        // 단지
        String houseSn,
        String hsmpNm,
        String brtcNm,
        String signguNm,
        String fullAdres,
        String heatMthdNm,
        String totHshldCo,
        String sumSuplyCo,
        String rentGtn,
        String mtRntchrg,
        String enty,
        String surlus
) {
}
