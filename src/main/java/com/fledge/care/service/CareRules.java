package com.fledge.care.service;

import com.fledge.budget.domain.FinancialTransaction;
import com.fledge.budget.domain.MoneyCycle;
import com.fledge.budget.domain.MoneySchedule;
import com.fledge.care.dto.CareDto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public final class CareRules {
    private CareRules() {}

    public static LocalDate dueDate(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    public static boolean matches(MoneySchedule schedule, MoneyCycle cycle, FinancialTransaction tx, LocalDate today) {
        if (schedule.getMatchKeyword() == null || schedule.getMatchKeyword().isBlank()) return false;
        return schedule.getMemberId().equals(tx.getMemberId())
                && !tx.getTxnDate().isBefore(cycle.getCycleMonth())
                && !tx.getTxnDate().isAfter(today)
                && tx.getTxnDate().isBefore(cycle.getCycleMonth().plusMonths(1))
                && (schedule.getDirection().equals("OUT") ? "EXPENSE" : "INCOME").equals(tx.getTxnType())
                && tx.getMerchantName() != null && tx.getMerchantName().contains(schedule.getMatchKeyword())
                && (!schedule.getDirection().equals("OUT") || tx.getAmount().equals(cycle.getExpectedAmount()));
    }

    public static String signalType(String scheduleType) {
        return switch (scheduleType) {
            case "SAVINGS" -> "MISSED_SAVING";
            case "RENT", "TELECOM", "UTILITY" -> "MISSED_PAYMENT";
            default -> "INCOME_MISSING";
        };
    }

    public static int weight(String type) {
        return switch (type) {
            case "SAVINGS" -> 25;
            case "TELECOM" -> 30;
            case "UTILITY" -> 35;
            default -> 40;
        };
    }

    public static int risk(List<String> openScheduleTypes) {
        Map<String, Integer> groups = new java.util.HashMap<>();
        openScheduleTypes.forEach(t -> groups.merge(signalType(t), weight(t), Math::max));
        return Math.min(100, groups.values().stream().mapToInt(Integer::intValue).sum());
    }

    public static String prompt(String type) {
        return switch (type) {
            case "MISSED_SAVING" -> "이번 달 적금 납입이 확인되지 않았어요. 현재 상황을 알려주세요.";
            case "MISSED_PAYMENT" -> "이번 달 고정비 납부가 확인되지 않았어요. 현재 납부 상황을 알려주세요.";
            default -> "이번 달 예정된 소득 입금이 확인되지 않았어요. 혹시 소득 상황에 변화가 있으신가요?";
        };
    }

    public static List<CareDto.Option> options(String type) {
        String[] labels = switch (type) {
            case "MISSED_SAVING" -> new String[]{"이미 납입했어요", "납입이 어려워요", "납입 계획이 바뀌었어요", "나중에 볼게요"};
            case "MISSED_PAYMENT" -> new String[]{"이미 납부했어요", "납부가 어려워요", "납부 계획이 바뀌었어요", "나중에 볼게요"};
            default -> new String[]{"이미 받았어요", "현재 소득이 어려워요", "소득 계획이 바뀌었어요", "나중에 볼게요"};
        };
        return java.util.stream.IntStream.range(0, 4)
                .mapToObj(i -> new CareDto.Option(CareDto.Choice.values()[i], labels[i])).toList();
    }
}
