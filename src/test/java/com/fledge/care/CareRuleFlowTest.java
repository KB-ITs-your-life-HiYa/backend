package com.fledge.care;

import com.fledge.budget.domain.*;
import com.fledge.budget.repository.*;
import com.fledge.budget.service.MoneyCycleService;
import com.fledge.care.domain.*;
import com.fledge.care.dto.CareDto.*;
import com.fledge.care.repository.*;
import com.fledge.care.service.*;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.member.domain.Member;
import com.fledge.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 실제 시드 거래 + 실제 서비스 규칙, 저장소만 메모리로 대체. 실행 중인 DB에는 연결하지 않는다. */
class CareRuleFlowTest {
    final List<MoneySchedule> scheduleRows = new ArrayList<>();
    final List<MoneyCycle> cycleRows = new ArrayList<>();
    final List<FinancialTransaction> txRows = new ArrayList<>();
    final List<CareSignal> signalRows = new ArrayList<>();
    final List<CareResponse> responseRows = new ArrayList<>();
    final Map<Long, CareDemoState> demoRows = new HashMap<>();
    final List<ReferralRequest> referralRows = new ArrayList<>();
    ReferralRequestRepository referrals;
    CareService care;
    CareTime time;
    MemberRepository members;
    JdbcTemplate jdbc;
    CareDemoStateRepository demo;

    @BeforeEach
    void fixture() throws Exception {
        var schedules = mock(MoneyScheduleRepository.class);
        var cycles = mock(MoneyCycleRepository.class);
        var transactions = mock(FinancialTransactionRepository.class);
        var signals = mock(CareSignalRepository.class);
        var responses = mock(CareResponseRepository.class);
        demo = mock(CareDemoStateRepository.class);
        members = mock(MemberRepository.class);
        jdbc = mock(JdbcTemplate.class);
        Member member = mock(Member.class);
        when(member.getEmail()).thenReturn("demo2@fledge.dev");
        when(member.getRegionCode()).thenReturn("28");
        when(members.lockForCare(anyLong())).thenReturn(Optional.of(member));
        when(members.findById(2L)).thenReturn(Optional.of(member));
        when(schedules.findByMemberIdAndIsActiveTrueOrderById(anyLong())).thenAnswer(a -> scheduleRows.stream()
                .filter(s -> s.getMemberId().equals(a.getArgument(0)) && s.isActive()).toList());
        when(schedules.findByIdAndMemberId(anyLong(), anyLong())).thenAnswer(a -> scheduleRows.stream()
                .filter(s -> s.getId().equals(a.getArgument(0)) && s.getMemberId().equals(a.getArgument(1))).findFirst());
        when(cycles.findByMemberIdOrderByExpectedDateAscIdAsc(anyLong())).thenAnswer(a -> cycleRows.stream()
                .filter(c -> c.getMemberId().equals(a.getArgument(0))).sorted(Comparator.comparing(MoneyCycle::getExpectedDate)).toList());
        when(cycles.findByScheduleIdAndCycleMonth(anyLong(), any())).thenAnswer(a -> cycleRows.stream()
                .filter(c -> c.getScheduleId().equals(a.getArgument(0)) && c.getCycleMonth().equals(a.getArgument(1))).findFirst());
        when(cycles.findByIdAndMemberId(anyLong(), anyLong())).thenAnswer(a -> cycleRows.stream()
                .filter(c -> c.getId().equals(a.getArgument(0)) && c.getMemberId().equals(a.getArgument(1))).findFirst());
        when(cycles.save(any())).thenAnswer(a -> {
            MoneyCycle c = a.getArgument(0); c.setId((long) cycleRows.size() + 1); cycleRows.add(c); return c;
        });
        when(transactions.findByMemberIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(anyLong(), any(), any())).thenAnswer(a -> txRows.stream()
                .filter(t -> t.getMemberId().equals(a.getArgument(0)) && !t.getTxnDate().isBefore(a.getArgument(1))
                        && !t.getTxnDate().isAfter(a.getArgument(2))).toList());
        when(signals.findByMemberIdOrderByDetectedAtAscIdAsc(anyLong())).thenAnswer(a -> signalRows.stream()
                .filter(s -> s.getMemberId().equals(a.getArgument(0))).toList());
        when(signals.findByIdAndMemberId(anyLong(), anyLong())).thenAnswer(a -> signalRows.stream()
                .filter(s -> s.getId().equals(a.getArgument(0)) && s.getMemberId().equals(a.getArgument(1))).findFirst());
        when(signals.findByMoneyCycleIdAndStatus(anyLong(), anyString())).thenAnswer(a -> signalRows.stream()
                .filter(s -> s.getMoneyCycleId().equals(a.getArgument(0)) && s.getStatus().equals(a.getArgument(1))).findFirst());
        when(signals.save(any())).thenAnswer(a -> {
            CareSignal s = a.getArgument(0); s.setId((long) signalRows.size() + 1); signalRows.add(s); return s;
        });
        when(responses.findByCareSignalIdOrderByCreatedAtAscIdAsc(anyLong())).thenAnswer(a -> responseRows.stream()
                .filter(r -> r.getCareSignalId().equals(a.getArgument(0))).toList());
        when(responses.findByCareSignalIdAndRequestId(anyLong(), anyString())).thenAnswer(a -> responseRows.stream()
                .filter(r -> r.getCareSignalId().equals(a.getArgument(0)) && r.getRequestId().equals(a.getArgument(1))).findFirst());
        when(responses.save(any())).thenAnswer(a -> {
            CareResponse r = a.getArgument(0); r.setId((long) responseRows.size() + 1); responseRows.add(r); return r;
        });
        when(demo.findById(anyLong())).thenAnswer(a -> Optional.ofNullable(demoRows.get(a.getArgument(0))));
        when(demo.saveAndFlush(any())).thenAnswer(a -> {
            CareDemoState s = a.getArgument(0); demoRows.put(s.getMemberId(), s); return s;
        });
        MockEnvironment env = new MockEnvironment(); env.setActiveProfiles("local");
        time = new CareTime(demo, env, true);
        var money = new MoneyCycleService(schedules, cycles, transactions, signals);
        referrals = mock(ReferralRequestRepository.class);
        when(referrals.findFirstByCareSignalIdAndMemberIdAndStatusInOrderByIdDesc(anyLong(), anyLong(), any()))
                .thenAnswer(a -> referralRows.stream().filter(r -> r.getCareSignalId().equals(a.getArgument(0))
                        && r.getMemberId().equals(a.getArgument(1)) && List.of("REQUESTED", "CONTACTED").contains(r.getStatus())).findFirst());
        when(referrals.saveAndFlush(any())).thenAnswer(a -> {
            ReferralRequest r = a.getArgument(0); r.setId((long) referralRows.size() + 1); referralRows.add(r); return r;
        });
        care = new CareService(referrals, new com.fasterxml.jackson.databind.ObjectMapper(), members, schedules, cycles, signals, responses, demo, money, time, jdbc, mock(EntityManager.class));
        String sql = new ClassPathResource("db/seed/R__seed_demo2_finance_scenario.sql").getContentAsString(StandardCharsets.UTF_8);
        var sm = Pattern.compile("\\((20[1-7]),\\s*'(OUT|IN)',\\s*'([^']+)',\\s*'([^']+)',\\s*(NULL|[0-9]+),\\s*([0-9]+),\\s*'([^']+)'\\)").matcher(sql);
        while (sm.find()) {
            MoneySchedule s = new MoneySchedule(); s.setId(Long.valueOf(sm.group(1))); s.setMemberId(2L);
            s.setDirection(sm.group(2)); s.setType(sm.group(3)); s.setName(sm.group(4));
            s.setExpectedAmount(sm.group(5).equals("NULL") ? null : Long.valueOf(sm.group(5)));
            s.setExpectedDay(Integer.valueOf(sm.group(6))); s.setMatchKeyword(sm.group(7)); s.setActive(true); scheduleRows.add(s);
        }
        var tm = Pattern.compile("\\(2,\\s*([0-9]+),\\s*'([0-9-]+)',\\s*'(EXPENSE|INCOME)',\\s*([0-9]+),\\s*'([^']+)',\\s*(NULL|'[^']+')\\)").matcher(sql);
        while (tm.find()) addTransaction(2L, LocalDate.parse(tm.group(2)), tm.group(3), Long.parseLong(tm.group(4)), tm.group(5));
        assertThat(scheduleRows).hasSize(7);
        assertThat(txRows).hasSize(83);
    }

    void addTransaction(long member, LocalDate date, String direction, long amount, String name) {
        FinancialTransaction t = new FinancialTransaction(); t.setId((long) txRows.size() + 1); t.setMemberId(member);
        t.setAccountId(11L); t.setTxnDate(date); t.setTxnType(direction); t.setAmount(amount); t.setMerchantName(name); txRows.add(t);
    }

    ButtonRequest button(Choice choice, String id) { return new ButtonRequest(choice, id, null, null); }
    Summary day(int day) { return care.setDemoDate(2L, LocalDate.of(2026, 9, day)); }

    @Test void scenarioDatesProduceZero25And65WithoutDuplicates() {
        Summary first = day(23);
        assertThat(first.riskScore()).isZero(); assertThat(first.reminders()).hasSize(1);
        assertThat(first.cycles()).filteredOn(c -> c.status().equals("DONE")).hasSize(5);
        var reminderTime = cycleRows.stream().filter(c -> c.getScheduleId() == 201L).findFirst().orElseThrow().getReminderSentAt();
        day(23); assertThat(cycleRows).hasSize(7);
        assertThat(cycleRows.stream().filter(c -> c.getScheduleId() == 201L).findFirst().orElseThrow().getReminderSentAt()).isEqualTo(reminderTime);
        assertThat(day(24).riskScore()).isEqualTo(25);
        assertThat(day(24).signals()).hasSize(1);
        assertThat(day(26).riskScore()).isEqualTo(65);
        assertThat(day(26).signals()).hasSize(2);
        assertThat(day(26).reminders()).isEmpty();
        assertThat(signalRows.getFirst().getRecheckAt()).isEqualTo(signalRows.getFirst().getDetectedAt().plusDays(7));
    }

    @Test void difficultAndLaterPreserveRiskAndHistory() {
        day(24);
        assertThat(care.respond(2L, 1L, button(Choice.DIFFICULT, "difficult")).riskScore()).isEqualTo(25);
        assertThat(signalRows.getFirst().getResponseResult()).isEqualTo("NEEDS_CARE");
        var summary = care.respond(2L, 1L, button(Choice.LATER, "later"));
        assertThat(summary.riskScore()).isEqualTo(25);
        assertThat(summary.signals().getFirst().replies()).hasSize(2);
        assertThat(signalRows.getFirst().getResponseResult()).isNull();
        assertThat(care.summary(2L).signals().getFirst().replies().getFirst().reply()).contains("기록");
    }

    @Test void alreadyDoneRequiresRealTransactionAndDuplicateRetryIsIdempotent() {
        day(24);
        var request = button(Choice.ALREADY_DONE, "retry-one");
        assertThat(care.respond(2L, 1L, request).riskScore()).isEqualTo(25);
        care.respond(2L, 1L, request); assertThat(responseRows).hasSize(1);
        addTransaction(2L, LocalDate.of(2026, 9, 24), "EXPENSE", 200000, "KB국민 시연 정기적금");
        var done = button(Choice.ALREADY_DONE, "confirmed");
        assertThat(care.respond(2L, 1L, done).riskScore()).isZero();
        care.respond(2L, 1L, done); assertThat(responseRows).hasSize(2);
        assertThat(signalRows.getFirst().getStatus()).isEqualTo("RESOLVED");
        assertThat(cycleRows.stream().filter(c -> c.getScheduleId() == 201L).findFirst().orElseThrow().getMatchedTransactionId()).isNotNull();
    }

    @Test void cannotReuseRequestIdForDifferentPayload() {
        day(24); care.respond(2L, 1L, button(Choice.DIFFICULT, "same"));
        assertThatThrownBy(() -> care.respond(2L, 1L, button(Choice.LATER, "same")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CARE_REQUEST_CONFLICT));
    }

    @Test void changedScheduleAppliesNextMonthWithoutRewritingCurrentCycle() {
        day(24);
        var current = cycleRows.stream().filter(c -> c.getScheduleId() == 201L).findFirst().orElseThrow();
        var changed = new ButtonRequest(Choice.CHANGED, "change", 10, 100000L);
        assertThat(care.respond(2L, 1L, changed).riskScore()).isZero();
        care.respond(2L, 1L, changed);
        assertThat(responseRows).hasSize(1);
        assertThat(current.getExpectedDate()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(current.getExpectedAmount()).isEqualTo(200000L);
        assertThat(current.getStatus()).isEqualTo("MISSED");
        assertThat(day(26).riskScore()).isEqualTo(40);
        assertThat(signalRows).hasSize(2);
        care.setDemoDate(2L, LocalDate.of(2026, 10, 1));
        var next = cycleRows.stream().filter(c -> c.getScheduleId() == 201L && c.getCycleMonth().getMonthValue() == 10).findFirst().orElseThrow();
        assertThat(next.getExpectedDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(next.getExpectedAmount()).isEqualTo(100000L);
    }

    @Test void amountOnlyChangePreservesDayAndCurrentAmount() {
        day(24);
        var summary = care.respond(2L, 1L, new ButtonRequest(Choice.CHANGED, "amount", null, 150000L));
        assertThat(scheduleRows.getFirst().getExpectedDay()).isEqualTo(23);
        assertThat(scheduleRows.getFirst().getExpectedAmount()).isEqualTo(150000L);
        assertThat(summary.signals().getFirst().expectedAmount()).isEqualTo(200000L);
        assertThat(summary.signals().getFirst().replies().getFirst().reply()).contains("150,000원", "다음 달");
        assertThat(summary.signals().getFirst().options()).isEmpty();
    }

    @Test void dayOnlyChangeAllowsEarlierDayAndPreservesAmount() {
        day(24);
        care.respond(2L, 1L, new ButtonRequest(Choice.CHANGED, "day", 5, null));
        assertThat(scheduleRows.getFirst().getExpectedDay()).isEqualTo(5);
        assertThat(scheduleRows.getFirst().getExpectedAmount()).isEqualTo(200000L);
    }

    @Test void invalidChangeDoesNotWriteReplyOrChangeSchedule() {
        day(24);
        for (var invalid : List.of(
                new ButtonRequest(Choice.CHANGED, "empty", null, null),
                new ButtonRequest(Choice.CHANGED, "bad-day", 32, 200000L),
                new ButtonRequest(Choice.CHANGED, "bad-amount", 10, 0L))) {
            assertThatThrownBy(() -> care.respond(2L, 1L, invalid)).isInstanceOf(ApiException.class);
        }
        assertThat(scheduleRows.getFirst().getExpectedDay()).isEqualTo(23);
        assertThat(responseRows).isEmpty(); assertThat(care.summary(2L).riskScore()).isEqualTo(25);
    }

    @Test void answeredSignalNoLongerOffersInitialChoicesEvenAfterReload() {
        day(24);
        assertThat(care.summary(2L).signals().getFirst().options()).extracting(Option::value)
                .containsExactly(Choice.ALREADY_DONE, Choice.DIFFICULT, Choice.CHANGED, Choice.LATER);
        care.respond(2L, 1L, button(Choice.DIFFICULT, "hide-options"));
        assertThat(care.evaluate(2L).signals().getFirst().options()).isEmpty();
        var next = day(26);
        assertThat(next.signals().getFirst().options()).isEmpty();
        assertThat(next.signals().getLast().options()).hasSize(4);
        care.respond(2L, 2L, button(Choice.LATER, "hide-later"));
        assertThat(care.summary(2L).signals().getLast().options()).isEmpty();
    }

    @Test void anotherMembersTransactionCannotResolveSignal() {
        day(24); addTransaction(1L, LocalDate.of(2026, 9, 24), "EXPENSE", 200000, "KB국민 시연 정기적금");
        assertThat(care.respond(2L, 1L, button(Choice.ALREADY_DONE, "wrong-owner")).riskScore()).isEqualTo(25);
        assertThatThrownBy(() -> care.respond(1L, 1L, button(Choice.DIFFICULT, "other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CARE_SIGNAL_NOT_FOUND));
        assertThat(care.summary(1L).signals()).isEmpty();
    }

    @Test void futureOrWrongAmountTransactionCannotResolveToday() {
        day(24); addTransaction(2L, LocalDate.of(2026, 9, 26), "EXPENSE", 200000, "KB국민 시연 정기적금");
        addTransaction(2L, LocalDate.of(2026, 9, 24), "EXPENSE", 100000, "KB국민 시연 정기적금");
        assertThat(care.respond(2L, 1L, button(Choice.ALREADY_DONE, "future")).riskScore()).isEqualTo(25);
        assertThat(day(26).riskScore()).isEqualTo(40);
    }

    @Test void oneTransactionCannotSatisfyTwoSchedules() {
        MoneySchedule duplicate = new MoneySchedule(); duplicate.setId(999L); duplicate.setMemberId(2L);
        duplicate.setActive(true); duplicate.setDirection("OUT"); duplicate.setType("SAVINGS"); duplicate.setName("별도 적금");
        duplicate.setExpectedDay(23); duplicate.setExpectedAmount(200000L); duplicate.setMatchKeyword("KB국민 시연 정기적금");
        scheduleRows.add(duplicate);
        addTransaction(2L, LocalDate.of(2026, 9, 23), "EXPENSE", 200000, "KB국민 시연 정기적금");
        assertThat(day(24).riskScore()).isEqualTo(25);
    }

    @Test void demoDateIsPersistedRejectsRewindAndOtherMember() {
        day(26); assertThat(time.now(2L).toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 26));
        assertThat(demoRows.get(2L).getAsOf().getHour()).isEqualTo(10);
        assertThatThrownBy(() -> day(23)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> care.setDemoDate(1L, CareTime.START)).isInstanceOf(ApiException.class);
    }

    @Test void productionAndDisabledConfigAlwaysUseRealClock() {
        MockEnvironment env = new MockEnvironment(); env.setActiveProfiles("local", "prod");
        assertThat(new CareTime(demo, env, true).isDemo(2L)).isFalse();
        env.setActiveProfiles("local", "supabase"); assertThat(new CareTime(demo, env, true).isDemo(2L)).isFalse();
        env.setActiveProfiles("local"); assertThat(new CareTime(demo, env, false).isDemo(2L)).isFalse();
        assertThat(time.clock(1L).instant()).isCloseTo(Instant.now(), within(2, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test void riskDeduplicatesTypesAndClampsAndMonthEndIsAdjusted() {
        assertThat(CareRules.risk(List.of("SAVINGS", "SAVINGS", "TELECOM", "UTILITY"))).isEqualTo(60);
        assertThat(CareRules.risk(List.of("SAVINGS", "RENT", "PART_TIME"))).isEqualTo(100);
        assertThat(CareRules.dueDate(YearMonth.of(2026, 2), 31)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(CareRules.dueDate(YearMonth.of(2028, 2), 31)).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test void resetUsesServiceTransactionAndRestartsAt23() throws Exception {
        day(26);
        var connection = mock(java.sql.Connection.class);
        var statement = mock(java.sql.Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenAnswer(a -> {
            String sql = a.<String>getArgument(0).replace("\r", "");
            assertThat(sql).doesNotContain("\nBEGIN;", "\nCOMMIT;");
            assertThat(sql).contains("DO $guard$", "END;\n$guard$;", "member_id = 2");
            assertThat(sql.indexOf("DELETE FROM money_cycle")).isLessThan(sql.indexOf("DELETE FROM transaction"));
            cycleRows.clear(); signalRows.clear(); responseRows.clear(); referralRows.clear();
            return false;
        });
        when(jdbc.execute(org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.ConnectionCallback<Void>>any()))
                .thenAnswer(a -> a.<org.springframework.jdbc.core.ConnectionCallback<Void>>getArgument(0).doInConnection(connection));
        var reset = care.resetDemo(2L);
        assertThat(reset.asOf().toLocalDate()).isEqualTo(CareTime.START);
        assertThat(reset.riskScore()).isZero(); assertThat(reset.cycles()).hasSize(7);
        assertThat(reset.reminders()).hasSize(1);
        assertThat(day(24).riskScore()).isEqualTo(25);
        verify(statement).close();
    }

    @Test void resetFailureDoesNotAdvanceDemoClock() {
        day(26);
        when(jdbc.execute(org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.ConnectionCallback<Void>>any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("fixture conflict"));
        assertThatThrownBy(() -> care.resetDemo(2L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CARE_DEMO_RESET_FAILED));
        assertThat(time.now(2L).toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 26));
    }

    @Test void referralRequiresEligibilityAndIsIdempotentAndOwned() {
        day(24);
        assertThat(care.summary(2L).signals().getFirst().referralEligible()).isFalse();
        assertThatThrownBy(() -> care.requestReferral(2L, 1L)).isInstanceOf(ApiException.class);
        day(26);
        assertThat(care.summary(2L).signals().getLast().referralEligible()).isTrue();
        assertThat(referralRows).isEmpty(); // 평가·상담은 접수하지 않는다.
        care.requestReferral(2L, 2L); care.requestReferral(2L, 2L);
        assertThat(referralRows).hasSize(1);
        assertThat(referralRows.getFirst().getReason()).isEqualTo("HIGH_RISK");
        assertThat(referralRows.getFirst().getRiskScoreAtRequest()).isEqualTo(65);
        assertThat(referralRows.getFirst().getCounselorId()).isNull();
        assertThatThrownBy(() -> care.requestReferral(1L, 2L)).isInstanceOf(ApiException.class);
    }

    @Test void resolvedSignalCannotReceiveNewReferral() {
        day(26);
        addTransaction(2L, LocalDate.of(2026, 9, 24), "EXPENSE", 200000, "KB국민 시연 정기적금");
        assertThatThrownBy(() -> care.requestReferral(2L, 1L)).isInstanceOf(ApiException.class);
        assertThat(referralRows).isEmpty();
    }

    @Test void sevenDayRecheckWithoutPaymentOffersReferralEvenBelow60() {
        day(24);
        // 급여는 정상 처리하여 7일 조건만으로 연결되는 경로를 검증한다.
        addTransaction(2L, LocalDate.of(2026, 9, 25), "INCOME", 300000, "카페모디 급여");
        var summary = care.setDemoDate(2L, LocalDate.of(2026, 10, 1));
        assertThat(summary.riskScore()).isEqualTo(25);
        assertThat(summary.signals().getFirst().recheckedAt()).isEqualTo(time.now(2L));
        care.requestReferral(2L, 1L);
        assertThat(referralRows.getFirst().getReason()).isEqualTo("UNRESOLVED_AFTER_7_DAYS");
        var checkedAt = signalRows.getFirst().getRecheckedAt();
        care.evaluate(2L);
        assertThat(signalRows.getFirst().getRecheckedAt()).isEqualTo(checkedAt);
    }

    @Test void sevenDayRecheckResolvesBackfilledPaymentAndRecordsCheck() {
        day(24);
        addTransaction(2L, LocalDate.of(2026, 9, 24), "EXPENSE", 200000, "KB국민 시연 정기적금");
        care.setDemoDate(2L, LocalDate.of(2026, 10, 1));
        var signal = care.summary(2L).signals().getFirst();
        assertThat(signal.status()).isEqualTo("RESOLVED");
        assertThat(signal.recheckedAt()).isNotNull();
        assertThat(signal.referralEligible()).isFalse();
    }

    @Test void recheckDoesNotRunBeforeDueTimeAndHighRiskReasonWins() {
        day(24); day(26);
        assertThat(signalRows.getFirst().getRecheckedAt()).isNull();
        care.setDemoDate(2L, LocalDate.of(2026, 10, 1));
        care.requestReferral(2L, 1L);
        assertThat(referralRows.getFirst().getReason()).isEqualTo("HIGH_RISK");
        assertThat(signalRows.getLast().getRecheckedAt()).isNull();
    }

    @Test void policyFailurePreservesReplyAndSuccessSnapshotCannotBeOverwritten() {
        day(24);
        care.respond(2L, 1L, button(Choice.DIFFICULT, "policies"));
        var error = care.savePolicies(2L, 1L, 1L, new Policies("ERROR", List.of()));
        assertThat(error.riskScore()).isEqualTo(25);
        assertThat(error.signals().getFirst().replies()).hasSize(1);
        var card = new PolicyCard("123", "FINANCE", "정책", "지원 내용", "상시", "기관", "https://www.youthcenter.go.kr/");
        care.savePolicies(2L, 1L, 1L, new Policies("READY", List.of(card)));
        care.savePolicies(2L, 1L, 1L, new Policies("ERROR", List.of()));
        assertThat(care.summary(2L).signals().getFirst().replies().getFirst().policies().cards()).containsExactly(card);
        assertThat(care.policyContext(2L, 1L, 1L).ready()).isTrue();
        assertThat(care.policyContext(2L, 1L, 1L).regionCode()).isEqualTo("28");
        assertThatThrownBy(() -> care.policyContext(1L, 1L, 1L)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> care.policyContext(2L, 1L, 999L)).isInstanceOf(ApiException.class);
    }
}
