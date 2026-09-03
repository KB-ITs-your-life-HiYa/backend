package com.fledge.housing.collect;

import com.fledge.housing.domain.TargetType;

/**
 * 공고명에서 대상 분류를 판별한다.
 *
 * 【공고명으로 판별하는 이유】
 * API 의 suplyTyNm 은 공급 형태(행복주택·매입임대…)일 뿐 대상을 말하지 않는다.
 * 자립준비청년 전용 공고에도 별도 유형 값이 없고 공고명에만 드러난다.
 *
 *   suplyTyNm: "전세임대"
 *   pblancNm:  "2026년 청년 전세임대 1순위 입주자 수시모집"
 *                     ↑ 여기로 판별
 *
 * 실측(공고 100건)에서 '청년' 이 11건이었고 '자립준비'·'보호종료' 는 0건이었다.
 * 자립준비청년 전용 공고는 상시로 나오지 않으므로, 없다고 규칙을 빼지는 않는다.
 */
final class TargetTypeResolver {

    private static final String[] SELF_RELIANCE_KEYWORDS = {"자립준비", "보호종료", "보호대상아동"};
    private static final String[] YOUTH_KEYWORDS = {"청년", "대학생"};

    private TargetTypeResolver() {
    }

    static TargetType resolve(String pblancNm) {
        if (pblancNm == null || pblancNm.isBlank()) {
            return TargetType.GENERAL;
        }
        if (containsAny(pblancNm, SELF_RELIANCE_KEYWORDS)) {
            return TargetType.SELF_RELIANCE;
        }
        if (containsAny(pblancNm, YOUTH_KEYWORDS)) {
            return TargetType.YOUTH;
        }
        return TargetType.GENERAL;
    }

    private static boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
