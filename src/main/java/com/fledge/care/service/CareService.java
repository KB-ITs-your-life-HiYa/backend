package com.fledge.care.service;

import com.fledge.budget.domain.*;
import com.fledge.budget.repository.*;
import com.fledge.budget.service.MoneyCycleService;
import com.fledge.care.domain.*;
import com.fledge.care.dto.CareDto.*;
import com.fledge.care.repository.*;
import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.member.repository.MemberRepository;
import com.fledge.member.domain.Member;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CareService {
    private final ReferralRequestRepository referrals;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;
    private final MemberRepository members;
    private final MoneyScheduleRepository schedules;
    private final MoneyCycleRepository cycles;
    private final CareSignalRepository signals;
    private final CareResponseRepository responses;
    private final CareDemoStateRepository demoStates;
    private final MoneyCycleService moneyCycles;
    private final CareTime time;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    private Member lock(Long memberId) {
        return members.lockForCare(memberId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }

    public Summary summary(Long memberId) {
        lock(memberId);
        return snapshot(memberId);
    }

    public Summary evaluate(Long memberId) {
        lock(memberId);
        moneyCycles.evaluate(memberId, time.now(memberId));
        return snapshot(memberId);
    }

    public Summary respond(Long memberId, Long signalId, ButtonRequest request) {
        lock(memberId);
        CareSignal signal = signals.findByIdAndMemberId(signalId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CARE_SIGNAL_NOT_FOUND));
        Optional<CareResponse> previous = responses.findByCareSignalIdAndRequestId(signalId, request.requestId());
        if (previous.isPresent()) {
            if (!Objects.equals(previous.get().getRequestPayload(), request.toString()))
                throw new ApiException(ErrorCode.CARE_REQUEST_CONFLICT);
            return snapshot(memberId);
        }
        if (!"OPEN".equals(signal.getStatus())) throw new ApiException(ErrorCode.CARE_SIGNAL_RESOLVED);
        MoneyCycle cycle = cycles.findByIdAndMemberId(signal.getMoneyCycleId(), memberId).orElseThrow();
        MoneySchedule schedule = schedules.findByIdAndMemberId(cycle.getScheduleId(), memberId).orElseThrow();
        OffsetDateTime now = time.now(memberId);
        String reply;
        switch (request.choice()) {
            case ALREADY_DONE -> {
                boolean matched = moneyCycles.match(cycle, schedule, now);
                reply = matched ? "실제 거래가 확인되어 이번 상담을 해결했어요."
                        : "아직 거래 내역이 확인되지 않았어요. 확인될 때까지 이 상담을 유지할게요.";
            }
            case DIFFICULT -> {
                signal.setResponseResult("NEEDS_CARE");
                reply = "현재 어려운 상황을 기록했어요. 무리하지 마시고, 상황이 바뀌면 다시 알려주세요.";
            }
            case LATER -> {
                signal.setResponseResult(null);
                reply = "알겠어요. 지금 상태는 유지하고, 필요할 때 다시 이야기할 수 있어요.";
            }
            case CHANGED -> {
                String changed = changeSchedule(schedule, request, now);
                MoneyCycleService.resolve(signal, now);
                reply = changed + " 다음 달 일정부터 반영할게요.";
            }
            default -> throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        signal.setClassificationSource("RULE");
        signal.setUpdatedAt(now);
        CareResponse response = new CareResponse();
        response.setCareSignalId(signalId);
        response.setInputType("BUTTON");
        response.setSelectedValue(request.choice().name());
        response.setRequestId(request.requestId());
        response.setRequestPayload(request.toString());
        response.setRuleReply(reply);
        if (request.choice() == Choice.DIFFICULT) response.setPolicyStatus("PENDING");
        response.setCreatedAt(now);
        responses.save(response);
        return snapshot(memberId);
    }

    private String changeSchedule(MoneySchedule schedule, ButtonRequest request, OffsetDateTime now) {
        boolean changesDay = request.expectedDay() != null;
        boolean changesAmount = request.expectedAmount() != null;
        if ((!changesDay && !changesAmount)
                || (changesDay && (request.expectedDay() < 1 || request.expectedDay() > 31))
                || (changesAmount && request.expectedAmount() <= 0))
            throw new ApiException(ErrorCode.CARE_INVALID_CHANGE);
        List<String> changes = new ArrayList<>();
        if (changesDay) {
            schedule.setExpectedDay(request.expectedDay());
            changes.add("날짜를 매월 " + request.expectedDay() + "일로");
        }
        if (changesAmount) {
            schedule.setExpectedAmount(request.expectedAmount());
            changes.add("금액을 " + String.format(Locale.KOREA, "%,d원", request.expectedAmount()) + "으로");
        }
        String result = String.join(", ", changes) + " 변경했어요.";
        schedule.setUpdatedAt(now);
        return result;
    }

    private void requireDemo(Long memberId) {
        lock(memberId);
        if (!time.isDemo(memberId) || members.findById(memberId)
                .filter(m -> "demo2@fledge.dev".equals(m.getEmail())).isEmpty())
            throw new ApiException(ErrorCode.CARE_DEMO_FORBIDDEN);
    }

    public Summary setDemoDate(Long memberId, LocalDate date) {
        requireDemo(memberId);
        List<LocalDate> allowed = List.of(CareTime.START, CareTime.START.plusDays(1), CareTime.START.plusDays(3), LocalDate.of(2026, 10, 1));
        if (!allowed.contains(date) || date.isBefore(time.now(memberId).toLocalDate()))
            throw new ApiException(ErrorCode.CARE_DEMO_DATE);
        saveDemoDate(memberId, date);
        moneyCycles.evaluate(memberId, time.now(memberId));
        return snapshot(memberId);
    }

    public Summary resetDemo(Long memberId) {
        requireDemo(memberId);
        try {
            // SQL 파일의 명시 트랜잭션은 제거: 서비스 트랜잭션 안에서 시드와 시각을 함께 롤백한다.
            String sql = new ClassPathResource("db/seed/R__seed_demo2_finance_scenario.sql")
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replaceAll("(?m)^BEGIN;\\s*$", "").replaceAll("(?m)^COMMIT;\\s*$", "");
            entityManager.flush();
            jdbc.execute((ConnectionCallback<Void>) connection -> {
                try (var statement = connection.createStatement()) { statement.execute(sql); }
                return null;
            });
            entityManager.clear();
        } catch (Exception e) {
            log.warn("demo2 시연 초기화 실패", e);
            throw new ApiException(ErrorCode.CARE_DEMO_RESET_FAILED);
        }
        saveDemoDate(memberId, CareTime.START);
        moneyCycles.evaluate(memberId, time.now(memberId));
        return snapshot(memberId);
    }

    private void saveDemoDate(Long memberId, LocalDate date) {
        CareDemoState state = demoStates.findById(memberId).orElseGet(CareDemoState::new);
        state.setMemberId(memberId);
        state.setAsOf(date.atTime(10, 0).atZone(CareTime.ZONE).toOffsetDateTime());
        demoStates.saveAndFlush(state);
    }

    public PolicyContext policyContext(Long memberId, Long signalId, Long responseId) {
        Member member = lock(memberId);
        CareSignal signal = signals.findByIdAndMemberId(signalId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CARE_SIGNAL_NOT_FOUND));
        CareResponse response = policyResponse(signalId, responseId);
        return new PolicyContext(responseId, signal.getSignalType(), time.now(memberId).toLocalDate(),
                member.getRegionCode(),
                "READY".equals(response.getPolicyStatus()));
    }

    private CareResponse policyResponse(Long signalId, Long responseId) {
        return responses.findByCareSignalIdOrderByCreatedAtAscIdAsc(signalId).stream()
                .filter(r -> r.getId().equals(responseId) && "DIFFICULT".equals(r.getSelectedValue()))
                .findFirst().orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    public Summary savePolicies(Long memberId, Long signalId, Long responseId, Policies policies) {
        lock(memberId);
        signals.findByIdAndMemberId(signalId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CARE_SIGNAL_NOT_FOUND));
        CareResponse response = policyResponse(signalId, responseId);
        // 동시에 조회한 요청 중 먼저 성공한 카드 묶음을 보존한다.
        if (!"READY".equals(response.getPolicyStatus())) {
            try {
                response.setPolicyCards(mapper.writeValueAsString(policies.cards()));
                response.setPolicyStatus(policies.status());
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new ApiException(ErrorCode.INTERNAL_ERROR);
            }
        }
        return snapshot(memberId);
    }

    private Policies policyView(CareResponse response) {
        if (response.getPolicyStatus() == null) return null;
        try {
            List<PolicyCard> cards = response.getPolicyCards() == null ? List.of()
                    : mapper.readValue(response.getPolicyCards(), new com.fasterxml.jackson.core.type.TypeReference<List<PolicyCard>>() {});
            return new Policies(response.getPolicyStatus(), cards);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public Summary requestReferral(Long memberId, Long signalId) {
        lock(memberId);
        CareSignal signal = signals.findByIdAndMemberId(signalId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.CARE_SIGNAL_NOT_FOUND));
        moneyCycles.evaluate(memberId, time.now(memberId));
        if (referralView(signal) != null) return snapshot(memberId);
        int risk = snapshot(memberId).riskScore();
        String reason = referralReason(signal, risk);
        if (reason == null) throw new ApiException(ErrorCode.CARE_REFERRAL_NOT_ELIGIBLE);
        ReferralRequest request = new ReferralRequest();
        request.setMemberId(memberId);
        request.setCareSignalId(signalId);
        request.setStatus("REQUESTED");
        request.setReason(reason);
        request.setRiskScoreAtRequest(risk);
        request.setRequestedAt(time.now(memberId));
        referrals.saveAndFlush(request);
        return snapshot(memberId);
    }

    private String referralReason(CareSignal signal, int risk) {
        if (!"OPEN".equals(signal.getStatus())) return null;
        if (risk >= 60) return "HIGH_RISK";
        return signal.getRecheckedAt() != null ? "UNRESOLVED_AFTER_7_DAYS" : null;
    }

    private Referral referralView(CareSignal signal) {
        return referrals.findFirstByCareSignalIdAndMemberIdAndStatusInOrderByIdDesc(
                        signal.getId(), signal.getMemberId(), List.of("REQUESTED", "CONTACTED"))
                .map(r -> new Referral(r.getId(), r.getStatus(), r.getReason(), r.getRequestedAt())).orElse(null);
    }

    private Summary snapshot(Long memberId) {
        OffsetDateTime now = time.now(memberId);
        List<MoneyCycle> allCycles = cycles.findByMemberIdOrderByExpectedDateAscIdAsc(memberId);
        Map<Long, MoneyCycle> cycleById = allCycles.stream().collect(Collectors.toMap(MoneyCycle::getId, c -> c));
        Map<Long, MoneySchedule> scheduleById = new HashMap<>();
        allCycles.forEach(c -> scheduleById.computeIfAbsent(c.getScheduleId(),
                id -> schedules.findByIdAndMemberId(id, memberId).orElseThrow()));
        List<CareSignal> allSignals = signals.findByMemberIdOrderByDetectedAtAscIdAsc(memberId);
        List<String> openTypes = allSignals.stream().filter(s -> "OPEN".equals(s.getStatus()))
                .map(s -> scheduleById.get(cycleById.get(s.getMoneyCycleId()).getScheduleId()).getType()).toList();
        int risk = CareRules.risk(openTypes);
        List<Cycle> cycleViews = allCycles.stream().map(c -> {
            MoneySchedule s = scheduleById.get(c.getScheduleId());
            return new Cycle(c.getId(), s.getId(), s.getName(), s.getType(), c.getExpectedDate(),
                    c.getExpectedAmount(), c.getStatus(), c.getActualDate(), c.getActualAmount());
        }).toList();
        List<Reminder> reminders = allCycles.stream()
                .filter(c -> "PENDING".equals(c.getStatus()) && c.getExpectedDate().equals(now.toLocalDate())
                        && c.getReminderSentAt() != null)
                .map(c -> new Reminder(c.getId(), "오늘은 " + scheduleById.get(c.getScheduleId()).getName()
                        + " 납입·납부 예정일이에요. 아직 거래 내역이 확인되지 않았어요.")).toList();
        List<Signal> signalViews = allSignals.stream().map(s -> {
            MoneyCycle c = cycleById.get(s.getMoneyCycleId());
            List<Option> options = CareRules.options(s.getSignalType());
            List<Reply> replies = responses.findByCareSignalIdOrderByCreatedAtAscIdAsc(s.getId()).stream()
                    .map(r -> new Reply(r.getId(), r.getSelectedValue(),
                            "BUTTON".equals(r.getInputType()) ? options.stream()
                                    .filter(o -> o.value().name().equals(r.getSelectedValue()))
                                    .map(Option::label).findFirst().orElse(r.getSelectedValue()) : r.getInputText(),
                            r.getRuleReply() != null ? r.getRuleReply() : r.getAiReply(), r.getCreatedAt(), policyView(r))).toList();
            return new Signal(s.getId(), c.getId(), scheduleById.get(c.getScheduleId()).getName(), s.getSignalType(),
                    s.getStatus(), s.getResponseResult(), CareRules.prompt(s.getSignalType()),
                    "OPEN".equals(s.getStatus()) && replies.isEmpty() ? options : List.of(),
                    c.getExpectedDate(), c.getExpectedAmount(),
                    s.getDetectedAt(), s.getRecheckAt(), s.getRecheckedAt(), replies,
                    referralReason(s, risk) != null, referralView(s));
        }).toList();
        return new Summary(now, time.isDemo(memberId), !schedules.findByMemberIdAndIsActiveTrueOrderById(memberId).isEmpty(),
                risk, risk == 0 ? "NORMAL" : risk < 60 ? "CARE" : "HUMAN_CARE", cycleViews, reminders, signalViews);
    }
}
