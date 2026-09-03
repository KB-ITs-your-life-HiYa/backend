package com.fledge.habit.controller;

import com.fledge.common.ApiResponse;
import com.fledge.habit.dto.HabitPuzzleProgressResponse;
import com.fledge.habit.dto.HabitPuzzleSetSummaryResponse;
import com.fledge.habit.dto.HabitQuizAnswerRequest;
import com.fledge.habit.dto.HabitQuizAnswerResponse;
import com.fledge.habit.dto.HabitTodayQuizResponse;
import com.fledge.habit.dto.HabitTopicDetailResponse;
import com.fledge.habit.dto.HabitTopicSummaryResponse;
import com.fledge.habit.service.HabitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 놀이 탭(금융습관 트레이닝) — 오늘의 퀴즈, 퍼즐 진행률·갤러리, 금융 상식 토픽. */
@Tag(name = "놀이")
@RestController
@RequestMapping("/habit")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @Operation(summary = "오늘의 퀴즈 조회")
    @GetMapping("/quiz/today")
    public ApiResponse<HabitTodayQuizResponse> getTodayQuiz() {
        return ApiResponse.ok(habitService.getTodayQuiz());
    }

    @Operation(summary = "오늘의 퀴즈 답변 제출")
    @PostMapping("/quiz/today/answer")
    public ApiResponse<HabitQuizAnswerResponse> answerTodayQuiz(@Valid @RequestBody HabitQuizAnswerRequest request) {
        return ApiResponse.ok(habitService.submitTodayAnswer(request.optionId()));
    }

    @Operation(summary = "현재 진행 중인 퍼즐 세트의 수집 진행률 조회")
    @GetMapping("/puzzle/progress")
    public ApiResponse<HabitPuzzleProgressResponse> getPuzzleProgress() {
        return ApiResponse.ok(habitService.getProgress());
    }

    @Operation(summary = "퍼즐 세트 갤러리 조회 (완성/진행중/잠김 전체 목록)")
    @GetMapping("/puzzle/sets")
    public ApiResponse<List<HabitPuzzleSetSummaryResponse>> listPuzzleSets() {
        return ApiResponse.ok(habitService.listPuzzleSets());
    }

    @Operation(summary = "금융 상식 토픽 목록 조회")
    @GetMapping("/topics")
    public ApiResponse<List<HabitTopicSummaryResponse>> listTopics() {
        return ApiResponse.ok(habitService.listTopics());
    }

    @Operation(summary = "금융 상식 토픽 상세 조회")
    @GetMapping("/topics/{topicId}")
    public ApiResponse<HabitTopicDetailResponse> getTopicDetail(@PathVariable Long topicId) {
        return ApiResponse.ok(habitService.getTopicDetail(topicId));
    }
}
