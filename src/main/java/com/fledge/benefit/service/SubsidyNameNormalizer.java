package com.fledge.benefit.service;

import java.util.regex.Pattern;

// 검색·매칭에서 "같은 지원금인데 표기만 다른" 경우를 하나로 묶기 위한 이름 정규화.
//
// 여러 소스(정부24·복지로·온통청년)에서 같은 정책을 각자 다르게 적어서 생기는 표기 차이
// (예: "청년월세 지원사업" / "청년월세한시특별지원" / "청년월세지원")를 없앤다.
// 연도·차수·괄호 안 부가설명·"지원사업" 류 관용어를 지우고 남는 핵심 단어만 비교한다.
//
// 일부러 보수적으로 짠다 — 너무 많이 지우면 실제로 다른 정책을 같은 것으로 묶어버릴 수 있다.
// 지역명은 지우지 않는다: 시/군/구별 사업명 차이는 실제로 다른 지역을 가리키는 경우가 많고,
// 어차피 지역 조건이 다르면 dedup key가 달라져서 따로 남는다.
final class SubsidyNameNormalizer {

    private static final Pattern NOISE = Pattern.compile(
            "\\([^)]*\\)"                          // 괄호 안 부가설명: "(2차)", "(국토부)" 등
                    + "|\\d+(?:년도|년|차)?"        // 연도·차수: "2026년", "26년", "2차"
                    + "|한시특별지원사업|한시특별지원|특별지원사업|한시지원사업"
                    + "|특별지원|한시지원|지원사업|변경신청"
                    + "|지원|사업|제도|운영|지급|거주"
    );

    private SubsidyNameNormalizer() {}

    static String normalize(String name) {
        // 공백을 먼저 다 지운 뒤에 잡음 단어를 지운다 — "한시 특별지원"처럼 원본마다
        // 띄어쓰기가 들쭉날쭉해서, 공백이 남은 채로 문구를 매칭하면 못 잡는 경우가 많다
        String noSpace = name.replaceAll("\\s+", "");
        return NOISE.matcher(noSpace).replaceAll("");
    }
}
