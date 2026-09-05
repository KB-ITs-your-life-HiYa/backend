package com.fledge.housing.domain;

/**
 * 체크리스트 종류. DB CHECK·UNIQUE 로 회원당 종류별 1개(최대 3개)를 강제한다.
 */
public enum ChecklistTemplateType {
    /** 좋은 집 찾기 */
    HOUSE_HUNTING("좋은 집 찾기"),
    /** 입주 준비 */
    MOVE_IN("입주 준비"),
    /** 이사 준비 */
    MOVING("이사 준비");

    private final String title;

    ChecklistTemplateType(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
