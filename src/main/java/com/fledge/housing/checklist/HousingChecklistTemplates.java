package com.fledge.housing.checklist;

import com.fledge.housing.domain.ChecklistTemplateType;

import java.util.List;

/**
 * 템플릿별 기본 항목. POST 생성 시 복사되며, 이후 사용자가 수정·삭제할 수 있다.
 * content = 체크 제목, memo = 짧은 안내.
 */
public final class HousingChecklistTemplates {

    private HousingChecklistTemplates() {}

    public record SeedItem(String content, String memo) {}

    public static List<SeedItem> itemsOf(ChecklistTemplateType type) {
        return switch (type) {
            case HOUSE_HUNTING -> HOUSE_HUNTING;
            case MOVE_IN -> MOVE_IN;
            case MOVING -> MOVING;
        };
    }

    private static final List<SeedItem> HOUSE_HUNTING = List.of(
            new SeedItem(
                    "내가 낼 수 있는 돈부터 계산하기",
                    "월세 + 관리비 + 전기·가스·인터넷을 합쳐 세후 월급의 30%를 넘기면 빠듯합니다. 관리비에 뭐가 포함되는지 부동산에 꼭 물어보세요."),
            new SeedItem(
                    "낮에 한 번, 저녁에 한 번 가보기",
                    "낮에는 햇빛이 얼마나 드는지, 저녁에는 옆집 소리·골목 분위기를 봅니다. 한 번만 보고 계약하면 사는 내내 후회하기 쉽습니다."),
            new SeedItem(
                    "수압·온수·곰팡이 확인하기",
                    "싱크대와 화장실 물을 동시에 틀어 수압을 보고, 온수가 빨리 나오는지 확인하세요. 창틀·붙박이장 안쪽 곰팡이·냄새도 봅니다."),
            new SeedItem(
                    "등기부등본으로 담보대출 확인하기",
                    "인터넷등기소에서 등기부등본을 직접 뽑아보세요. 대출액 + 내 보증금이 시세의 70%를 넘으면 위험 신호입니다."),
            new SeedItem(
                    "전세보증보험 가입 가능 여부 확인하기",
                    "계약금 넣기 전에 부동산에 \"이 집 보증보험 가입되나요?\"라고 물어보세요. 불법 증축·대출이 많은 집은 가입이 거절될 수 있습니다.")
    );

    private static final List<SeedItem> MOVE_IN = List.of(
            new SeedItem(
                    "돈은 집주인 본인 계좌로만 보내기",
                    "계약서·등기부등본·입금 계좌 명의가 셋 다 같아야 합니다. 하나라도 다르면 그 자리에서 이유를 확인하고, 애매하면 입금하지 마세요."),
            new SeedItem(
                    "계약서 특약에 보호 조항 넣기",
                    "\"대출·보증보험 심사 거절 시 계약 무효·계약금 전액 반환\", \"잔금 다음 날까지 집주인의 추가 담보대출 금지\"를 특약에 넣어달라고 하세요."),
            new SeedItem(
                    "잔금 당일 전입신고하기",
                    "정부24에서 신청합니다. 효력은 신고 다음 날 0시부터라, 하루만 미뤄도 그사이 집주인 대출에 순위가 밀릴 수 있습니다. 이사 후 14일 초과 시 과태료."),
            new SeedItem(
                    "같은 날 확정일자 받기",
                    "전입신고와 세트로 보증금 보호가 완성됩니다. 정부24에서 계약서 사진을 올리면 확정일자가 함께 처리됩니다(약 500~600원)."),
            new SeedItem(
                    "해당되면 주택임대차 신고하기",
                    "보증금 6천만 원 초과 또는 월세 30만 원 초과면 계약 후 30일 안에 신고해야 합니다. 미신고 시 최대 100만 원 과태료.")
    );

    private static final List<SeedItem> MOVING = List.of(
            new SeedItem(
                    "2주 전 — 이사업체·인터넷 신청하기",
                    "이사업체는 견적 3곳 비교하세요. 인터넷은 기사 일정이 밀리니 최소 2주 전에 신청해야 이사 당일부터 씁니다."),
            new SeedItem(
                    "1주 전 — 대형폐기물·주소 변경하기",
                    "큰 가구·매트리스는 스티커를 사서 버려야 합니다. 은행·카드·통신사 주소도 이때 바꿔 두세요."),
            new SeedItem(
                    "하루 전 — 가스 철거·연결 예약하기",
                    "전기·수도와 달리 가스는 예약 없이 못 뗍니다. 지역 가스회사에 미리 전화하거나 홈페이지·카톡으로 예약하세요."),
            new SeedItem(
                    "나가기 직전 — 계량기 사진 찍기",
                    "전기·가스·수도 계량기 숫자를 사진으로 남기세요. 나중에 \"안 낸 요금 있다\"는 말에 증거가 됩니다. 전기 문의는 한전 123."),
            new SeedItem(
                    "입주 직후 — 사진·비밀번호·자동이체",
                    "짐 넣기 전 빈 집 상태를 사진으로 남기고, 도어락 비밀번호를 바꾸세요. 관리비·공과금 자동이체와 정산도 잊지 마세요.")
    );
}
