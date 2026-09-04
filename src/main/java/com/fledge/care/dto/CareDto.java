package com.fledge.care.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class CareDto {
    private CareDto() {}
    public enum Choice { ALREADY_DONE, DIFFICULT, CHANGED, LATER }
    public record ButtonRequest(@NotNull Choice choice, @NotBlank @Size(max = 80) String requestId,
                                @Min(1) @Max(31) Integer expectedDay, @Positive Long expectedAmount) {}
    public record FreeTextRequest(@NotBlank @Size(max = 1000) String input,
                                  @NotBlank @Size(max = 80) String requestId) {}
    public record DemoDateRequest(@NotNull LocalDate date) {}
    public record Option(Choice value, String label) {}
    public record PolicyCard(String id, String category, String name, String support,
                             String applicationPeriod, String organization, String detailUrl) {}
    public record Policies(String status, List<PolicyCard> cards) {}
    public record PolicyContext(Long responseId, String signalType, LocalDate asOf,
                                String regionCode, boolean ready) {}
    public record ReferralConsent(@NotNull @AssertTrue Boolean consent) {}
    public record Referral(Long id, String status, String reason, OffsetDateTime requestedAt) {}
    public record Reply(Long id, String inputType, String choice, String userText, String reply,
                        String requestId, String aiStatus, OffsetDateTime createdAt, Policies policies) {}
    public record Cycle(Long id, Long scheduleId, String name, String type, LocalDate expectedDate,
                        Long expectedAmount, String status, LocalDate actualDate, Long actualAmount) {}
    public record Reminder(Long cycleId, String message) {}
    public record Signal(Long id, Long cycleId, String name, String type, String status,
                         String responseResult, String prompt, List<Option> options,
                         LocalDate expectedDate, Long expectedAmount, OffsetDateTime detectedAt,
                         OffsetDateTime recheckAt, OffsetDateTime recheckedAt, List<Reply> replies,
                         boolean referralEligible, Referral referral) {}
    public record Summary(OffsetDateTime asOf, boolean demoEnabled, boolean hasSchedules,
                          int riskScore, String riskLevel, List<Cycle> cycles,
                          List<Reminder> reminders, List<Signal> signals) {}
}
