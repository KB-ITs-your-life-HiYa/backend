package com.fledge.housing.domain;

/**
 * 공고의 대상 분류. 캘린더 정렬 기준이다.
 *
 * API 에는 이 값이 없다. 공고명에서 판별해 저장한다.
 * 조회할 때마다 공고명을 검색하면 인덱스를 못 타기 때문이다.
 */
public enum TargetType {
    /** 자립준비청년 전용 공고 */
    SELF_RELIANCE,
    /** 청년 대상 공고 */
    YOUTH,
    /** 그 외 */
    GENERAL
}
