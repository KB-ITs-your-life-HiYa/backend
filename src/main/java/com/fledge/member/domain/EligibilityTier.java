package com.fledge.member.domain;

// 사용자의 자격 단계
public enum EligibilityTier {
    // 보호종료 5년 이내, 전용 임대주택 1순위, 자립수당 대상
    SELF_RELIANCE("자립준비청년"),

    // 5년 초과 + 만 39세 이하, 청년 임대주택 2·3순위
    YOUTH("청년"),

    // 그 외: 국민임대·영구임대는 소득 요건만
    GENERAL("일반");

    private final String label;

    EligibilityTier(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
