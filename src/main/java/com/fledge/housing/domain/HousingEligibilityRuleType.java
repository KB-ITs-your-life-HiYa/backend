package com.fledge.housing.domain;

/** 자격 판정에 사용한 규칙. TargetType은 화면 분류이므로 별도로 둔다. */
public enum HousingEligibilityRuleType {
    SELF_RELIANCE_DEMO,
    LH_YOUTH_PURCHASE,
    UNSUPPORTED
}
