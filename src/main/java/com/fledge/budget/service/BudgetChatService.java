package com.fledge.budget.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fledge.benefit.ingest.GeminiClient;
import com.fledge.budget.domain.ExpenseCategory;
import com.fledge.budget.domain.MoneySchedule;
import com.fledge.budget.dto.AccountSummaryResponse;
import com.fledge.budget.dto.BudgetChatAskResponse;
import com.fledge.budget.dto.BudgetChatSummaryResponse;
import com.fledge.budget.dto.ExpenseReportResponse;
import com.fledge.budget.repository.MoneyScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * 생활비 관리 챗봇. Care RAG(CareFaqService)가 docs/rag 문서를 근거로 삼는 것과 달리,
 * 이 서비스는 이 사용자의 실제 이번 달 지출·예산·고정비·저축·계좌 데이터(ExpenseReportService,
 * AccountService, MoneyScheduleRepository)를 그때그때 모아 근거로 삼는다 — 별도 문서나
 * 벡터 검색이 필요 없는 개인화 데이터라서다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetChatService {

    private static final String OUT_OF_SCOPE_MESSAGE =
            "죄송해요, 저는 생활비·지출·예산·저축 관련 질문에만 답변드릴 수 있어요. 그 외 질문은 답변이 어려워요.";

    private static final String GENERIC_ERROR_MESSAGE =
            "지금 답변을 생성하는 데 문제가 생겼어요. 잠시 후 다시 시도해 주세요.";

    private static final String GREETING =
            "이번 달 생활비 흐름을 같이 살펴볼게요. 궁금한 항목을 물어보시면, 아래 요약과 연결해서 설명해 드릴게요.";

    // 목업 화면의 추천 질문. 사용자 상태와 무관하게 항상 같은 세 개를 보여준다 — 데모 규모에서는
    // 매번 다르게 골라줄 만큼 정교하지 않아도, 이 셋이 가장 많이 묻는 질문을 대표한다.
    private static final List<String> QUICK_QUESTIONS = List.of(
            "식비가 너무 많은가요?", "고정비부터 줄이려면?", "이번 달 적금 여유 있나요?"
    );

    // SupportEndForecastService.REDUCIBLE_CATEGORIES 와 같은 기준: 주거·공과금(고정비)과 저축은
    // 줄이자고 권할 대상이 아니라서 프롬프트 안내에서도 그대로 뺀다.
    private static final Map<ExpenseCategory, String> CATEGORY_LABELS = Map.of(
            ExpenseCategory.HOUSING_UTILITY, "주거·공과금",
            ExpenseCategory.FOOD, "식비",
            ExpenseCategory.TRANSPORT, "교통",
            ExpenseCategory.LIVING_MEDICAL, "생활·의료",
            ExpenseCategory.LEISURE_SHOPPING, "여가·쇼핑",
            ExpenseCategory.SAVINGS, "저축"
    );

    private static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "inScope": {"type": "boolean"},
                "answer": {"type": "string"}
              },
              "required": ["inScope", "answer"]
            }
            """;

    private final ExpenseReportService expenseReportService;
    private final AccountService accountService;
    private final MoneyScheduleRepository moneySchedules;
    private final GeminiClient gemini;
    private final ObjectMapper mapper;

    public BudgetChatSummaryResponse summary(Long memberId) {
        YearMonth month = YearMonth.now();
        ExpenseReportResponse report = expenseReportService.getReport(memberId, month);
        Long totalBudget = report.monthlyBudget();
        long totalExpense = report.summary().totalExpense();
        Long remaining = totalBudget == null ? null : totalBudget - totalExpense;
        Integer progressRatio = (totalBudget == null || totalBudget == 0) ? null
                : (int) Math.round(Math.min(100.0, totalExpense * 100.0 / totalBudget));
        return new BudgetChatSummaryResponse(
                month.toString(), totalExpense, totalBudget, remaining, progressRatio, GREETING, QUICK_QUESTIONS);
    }

    public BudgetChatAskResponse ask(Long memberId, String question) {
        try {
            String prompt = prompt(question, snapshot(memberId));
            GeminiAnswer result = mapper.readValue(
                    gemini.generateJson(prompt, RESPONSE_SCHEMA, 1024), GeminiAnswer.class);
            if (!result.inScope() || result.answer() == null || result.answer().isBlank()) {
                return new BudgetChatAskResponse(OUT_OF_SCOPE_MESSAGE, false);
            }
            return new BudgetChatAskResponse(result.answer().trim(), true);
        } catch (Exception e) {
            log.warn("생활비 챗봇 답변 생성 실패: memberId={}, cause={}", memberId, e.getClass().getSimpleName());
            return new BudgetChatAskResponse(GENERIC_ERROR_MESSAGE, false);
        }
    }

    // Gemini 프롬프트에 그대로 넣을 "이 사용자의 이번 달 생활비 현황" 텍스트 블록.
    private String snapshot(Long memberId) {
        YearMonth month = YearMonth.now();
        ExpenseReportResponse report = expenseReportService.getReport(memberId, month);
        AccountSummaryResponse accounts = accountService.getSummary(memberId);
        List<MoneySchedule> schedules = moneySchedules.findByMemberIdAndIsActiveTrueOrderById(memberId);

        StringBuilder sb = new StringBuilder();
        sb.append("월: ").append(month).append("\n");
        sb.append("이번 달 총 지출: ").append(report.summary().totalExpense()).append("원\n");
        sb.append("이번 달 총 수입: ").append(report.summary().totalIncome()).append("원\n");
        if (report.monthlyBudget() != null) {
            sb.append("이번 달 총 예산: ").append(report.monthlyBudget()).append("원\n");
            sb.append("남은 예산: ").append(report.monthlyBudget() - report.summary().totalExpense()).append("원\n");
        } else {
            sb.append("이번 달 총 예산: 설정 안 함\n");
        }
        sb.append("최근 3개월 평균 지출: ").append(report.monthlyTrend().averageExpense()).append("원\n");

        sb.append("카테고리별 이번 달 지출 (지난달 대비, 예산):\n");
        for (ExpenseReportResponse.CategoryBreakdown c : report.categories()) {
            String label = CATEGORY_LABELS.getOrDefault(c.category(), c.category().name());
            sb.append("- ").append(label).append(": ").append(c.currentAmount()).append("원")
                    .append(" (지난달 ").append(c.previousAmount()).append("원, 차이 ").append(c.difference()).append("원")
                    .append(c.budget() != null ? ", 예산 " + c.budget() + "원" : "")
                    .append(")\n");
        }

        List<MoneySchedule> fixed = schedules.stream()
                .filter(s -> "OUT".equals(s.getDirection()) && !"SAVINGS".equals(s.getType()))
                .toList();
        if (!fixed.isEmpty()) {
            sb.append("등록된 고정 지출(매달 반복):\n");
            for (MoneySchedule s : fixed) {
                sb.append("- ").append(s.getName()).append(" (").append(scheduleTypeLabel(s.getType())).append("): 매달 ")
                        .append(s.getExpectedDay()).append("일, ").append(s.getExpectedAmount()).append("원\n");
            }
        }

        List<MoneySchedule> savings = schedules.stream()
                .filter(s -> "OUT".equals(s.getDirection()) && "SAVINGS".equals(s.getType()))
                .toList();
        if (!savings.isEmpty()) {
            sb.append("등록된 저축(적금) 약속:\n");
            for (MoneySchedule s : savings) {
                sb.append("- ").append(s.getName()).append(": 매달 ").append(s.getExpectedDay()).append("일, ")
                        .append(s.getExpectedAmount()).append("원\n");
            }
        }

        sb.append("예금 계좌 합계: ").append(accounts.depositTotal()).append("원\n");
        sb.append("적금 계좌 합계: ").append(accounts.savingsTotal()).append("원\n");
        sb.append("순자산(예금+적금): ").append(accounts.netAsset()).append("원\n");
        return sb.toString();
    }

    private static String scheduleTypeLabel(String type) {
        return switch (type) {
            case "RENT" -> "월세/주거";
            case "TELECOM" -> "통신비";
            case "UTILITY" -> "공과금";
            default -> type;
        };
    }

    private String prompt(String question, String snapshot) {
        return """
                당신은 자립준비청년을 위한 생활비·예산 관리 챗봇입니다. 아래 "생활비 데이터"는 이 사용자의
                실제 이번 달 지출·예산·고정비·저축·계좌 정보이며, 답변의 유일한 근거입니다.

                이 챗봇은 생활비·지출·예산·저축·계좌 잔액과 관련된 질문만 다룹니다. 질문이 이 범위와
                관련 없으면(예: 날씨, 잡담, 다른 주제) inScope=false 로 답하고 answer="" 로 두세요.

                범위 안이면 inScope=true 로 하고 아래 규칙을 따르세요:
                - 아래 데이터에 실제로 있는 숫자만 근거로 답하세요. 데이터에 없는 금액·날짜를 추측하거나 지어내지 마세요.
                - 필요하면 데이터 안의 숫자로 간단한 산술(차이, 비율, 합계)을 직접 계산해서 답해도 됩니다.
                - 한국어 2~4문장으로 친절하고 구체적으로 답하세요. 사용자를 탓하지 않습니다.
                - 절약을 제안할 때는 "등록된 고정 지출"이 아니라, 식비·여가/쇼핑·생활/의료·교통처럼 줄이기 비교적
                  쉬운 변동 지출 카테고리 위주로 제안하세요. 주거·공과금 같은 고정비는 줄이기 어려운 항목이라고 안내하세요.
                - 확정적인 재무 조언(적금 해지 권유 등)은 하지 말고, 참고할 만한 방향만 제시하세요.

                생활비 데이터:
                """ + snapshot + """

                사용자 질문: """ + question;
    }

    private record GeminiAnswer(boolean inScope, String answer) {
    }
}
