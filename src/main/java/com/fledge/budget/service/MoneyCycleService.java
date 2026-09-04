package com.fledge.budget.service;

import com.fledge.budget.domain.*;
import com.fledge.budget.repository.*;
import com.fledge.care.domain.CareSignal;
import com.fledge.care.repository.CareSignalRepository;
import com.fledge.care.service.CareRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** 호출자는 회원 잠금과 트랜잭션을 먼저 확보한다. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MoneyCycleService {
    private final MoneyScheduleRepository schedules;
    private final MoneyCycleRepository cycles;
    private final FinancialTransactionRepository transactions;
    private final CareSignalRepository signals;

    public void evaluate(Long memberId, OffsetDateTime now) {
        // 정상 재매칭이 신호를 해결하기 전에, 이번 실행에서 기한이 된 재확인 기록을 남긴다.
        for (CareSignal signal : signals.findByMemberIdOrderByDetectedAtAscIdAsc(memberId)) {
            if ("OPEN".equals(signal.getStatus()) && signal.getRecheckedAt() == null
                    && signal.getRecheckAt() != null && !now.isBefore(signal.getRecheckAt())) {
                MoneyCycle cycle = cycles.findByIdAndMemberId(signal.getMoneyCycleId(), memberId).orElseThrow();
                MoneySchedule schedule = schedules.findByIdAndMemberId(cycle.getScheduleId(), memberId).orElseThrow();
                match(cycle, schedule, now);
                signal.setRecheckedAt(now);
                signal.setUpdatedAt(now);
            }
        }
        LocalDate today = now.toLocalDate();
        LocalDate month = today.withDayOfMonth(1);
        for (MoneySchedule s : schedules.findByMemberIdAndIsActiveTrueOrderById(memberId)) {
            if (cycles.findByScheduleIdAndCycleMonth(s.getId(), month).isEmpty()) {
                MoneyCycle c = new MoneyCycle();
                c.setMemberId(memberId);
                c.setScheduleId(s.getId());
                c.setCycleMonth(month);
                c.setExpectedDate(CareRules.dueDate(YearMonth.from(month), s.getExpectedDay()));
                c.setExpectedAmount(s.getExpectedAmount());
                c.setStatus("PENDING");
                c.setUpdatedAt(now);
                cycles.save(c);
            }
        }
        // 이전 월 MISSED도 거래가 뒤늦게 수집되면 해결할 수 있다.
        for (MoneyCycle c : cycles.findByMemberIdOrderByExpectedDateAscIdAsc(memberId)) {
            if ("DONE".equals(c.getStatus()) || c.getCycleMonth().isAfter(month)) continue;
            MoneySchedule s = schedules.findByIdAndMemberId(c.getScheduleId(), memberId).orElseThrow();
            if (!s.isActive()) continue;
            if (match(c, s, now)) continue;
            if (today.isAfter(c.getExpectedDate())) {
                boolean newlyMissed = !"MISSED".equals(c.getStatus());
                c.setStatus("MISSED");
                c.setUpdatedAt(now);
                if (newlyMissed && signals.findByMoneyCycleIdAndStatus(c.getId(), "OPEN").isEmpty()) {
                    CareSignal signal = new CareSignal();
                    signal.setMemberId(memberId);
                    signal.setMoneyCycleId(c.getId());
                    signal.setSignalType(CareRules.signalType(s.getType()));
                    signal.setStatus("OPEN");
                    signal.setDetectedAt(now);
                    signal.setRecheckAt(now.plusDays(7));
                    signal.setUpdatedAt(now);
                    signals.save(signal);
                }
            } else if (today.equals(c.getExpectedDate()) && "OUT".equals(s.getDirection())
                    && c.getReminderSentAt() == null) {
                // 이번 단계에서는 실제 Push가 아닌 앱 내부 알림의 생성 시각.
                c.setReminderSentAt(now);
                c.setUpdatedAt(now);
            }
        }
    }

    public boolean match(MoneyCycle cycle, MoneySchedule schedule, OffsetDateTime now) {
        Set<Long> used = new HashSet<>();
        cycles.findByMemberIdOrderByExpectedDateAscIdAsc(cycle.getMemberId()).stream()
                .filter(c -> !c.getId().equals(cycle.getId()) && c.getMatchedTransactionId() != null)
                .forEach(c -> used.add(c.getMatchedTransactionId()));
        Optional<FinancialTransaction> found = transactions.findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(
                        cycle.getMemberId(), cycle.getCycleMonth(), now.toLocalDate()).stream()
                .filter(t -> !used.contains(t.getId()) && CareRules.matches(schedule, cycle, t, now.toLocalDate()))
                .findFirst();
        if (found.isEmpty()) return false;
        FinancialTransaction tx = found.get();
        cycle.setStatus("DONE");
        cycle.setMatchedTransactionId(tx.getId());
        cycle.setActualDate(tx.getTxnDate());
        cycle.setActualAmount(tx.getAmount());
        cycle.setUpdatedAt(now);
        signals.findByMoneyCycleIdAndStatus(cycle.getId(), "OPEN").ifPresent(s -> resolve(s, now));
        return true;
    }

    public static void resolve(CareSignal signal, OffsetDateTime now) {
        signal.setStatus("RESOLVED");
        signal.setResponseResult("NORMAL_REASON");
        signal.setClassificationSource("RULE");
        signal.setResolvedAt(now);
        signal.setUpdatedAt(now);
    }
}
