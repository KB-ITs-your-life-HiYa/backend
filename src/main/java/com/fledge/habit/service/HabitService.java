package com.fledge.habit.service;

import com.fledge.common.ErrorCode;
import com.fledge.exception.ApiException;
import com.fledge.habit.domain.HabitPuzzleProgress;
import com.fledge.habit.domain.HabitPuzzleSet;
import com.fledge.habit.domain.HabitQuiz;
import com.fledge.habit.domain.HabitQuizAnswer;
import com.fledge.habit.domain.HabitQuizOption;
import com.fledge.habit.domain.HabitTopic;
import com.fledge.habit.dto.HabitPuzzleProgressResponse;
import com.fledge.habit.dto.HabitPuzzleSetSummaryResponse;
import com.fledge.habit.dto.HabitQuizAnswerResponse;
import com.fledge.habit.dto.HabitQuizOptionResponse;
import com.fledge.habit.dto.HabitQuizResultResponse;
import com.fledge.habit.dto.HabitTodayQuizResponse;
import com.fledge.habit.dto.HabitTopicCategoryResponse;
import com.fledge.habit.dto.HabitTopicDetailResponse;
import com.fledge.habit.dto.HabitTopicSummaryResponse;
import com.fledge.habit.repository.HabitPuzzleProgressRepository;
import com.fledge.habit.repository.HabitPuzzleSetRepository;
import com.fledge.habit.repository.HabitQuizAnswerRepository;
import com.fledge.habit.repository.HabitQuizOptionRepository;
import com.fledge.habit.repository.HabitQuizRepository;
import com.fledge.habit.repository.HabitTopicCategoryRepository;
import com.fledge.habit.repository.HabitTopicRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final HabitQuizRepository quizRepository;
    private final HabitQuizOptionRepository optionRepository;
    private final HabitQuizAnswerRepository answerRepository;
    private final HabitPuzzleSetRepository puzzleSetRepository;
    private final HabitPuzzleProgressRepository progressRepository;
    private final HabitTopicCategoryRepository topicCategoryRepository;
    private final HabitTopicRepository topicRepository;

    public HabitTodayQuizResponse getTodayQuiz(Long memberId) {
        HabitQuiz quiz = resolveTodayQuiz();
        List<HabitQuizOptionResponse> options = optionRepository.findByQuizIdOrderBySortOrderAsc(quiz.getId()).stream()
                .map(o -> new HabitQuizOptionResponse(o.getId(), o.getLabel()))
                .toList();

        Optional<HabitQuizAnswer> answer = answerRepository.findByMemberIdAndAnsweredDate(memberId, LocalDate.now(KST));
        HabitQuizResultResponse result = answer
                .map(a -> new HabitQuizResultResponse(a.getSelectedOptionId(), a.isCorrect(), quiz.getExplanation()))
                .orElse(null);

        return new HabitTodayQuizResponse(quiz.getId(), quiz.getQuestion(), options, answer.isPresent(), result);
    }

    @Transactional
    public HabitQuizAnswerResponse submitTodayAnswer(Long memberId, Long optionId) {
        LocalDate today = LocalDate.now(KST);
        if (answerRepository.findByMemberIdAndAnsweredDate(memberId, today).isPresent()) {
            throw new ApiException(ErrorCode.HABIT_QUIZ_ALREADY_ANSWERED);
        }

        HabitQuiz quiz = resolveTodayQuiz();
        HabitQuizOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ApiException(ErrorCode.HABIT_QUIZ_OPTION_INVALID));
        if (!option.getQuizId().equals(quiz.getId())) {
            throw new ApiException(ErrorCode.HABIT_QUIZ_OPTION_INVALID);
        }

        boolean correct = option.isCorrect();
        answerRepository.save(new HabitQuizAnswer(memberId, quiz.getId(), option.getId(), correct, today));

        boolean justCompleted = false;
        HabitPuzzleProgressResponse progressResponse;
        if (correct) {
            AwardResult award = awardPiece(memberId);
            justCompleted = award.justCompleted();
            progressResponse = award.progress();
        } else {
            progressResponse = getProgress(memberId);
        }

        return new HabitQuizAnswerResponse(correct, quiz.getExplanation(), progressResponse, justCompleted);
    }

    public HabitPuzzleProgressResponse getProgress(Long memberId) {
        List<HabitPuzzleSet> sets = puzzleSetRepository.findAllByOrderBySortOrderAsc();
        if (sets.isEmpty()) {
            throw new ApiException(ErrorCode.HABIT_PUZZLE_SET_NOT_FOUND);
        }
        Map<Long, HabitPuzzleProgress> progressBySetId = loadProgressBySetId(memberId);

        Optional<HabitPuzzleSet> current = findCurrentSet(sets, progressBySetId);
        if (current.isPresent()) {
            HabitPuzzleSet set = current.get();
            HabitPuzzleProgress progress = progressBySetId.get(set.getId());
            int collected = progress != null ? progress.getCollectedPieces() : 0;
            return new HabitPuzzleProgressResponse(set.getId(), set.getTitle(), set.getAssetKey(), collected, set.getTotalPieces(), false, false);
        }

        // 준비된 세트를 전부 완성한 상태 — 마지막 세트 정보를 completed=true 로 보여줌
        HabitPuzzleSet last = sets.get(sets.size() - 1);
        return new HabitPuzzleProgressResponse(last.getId(), last.getTitle(), last.getAssetKey(), last.getTotalPieces(), last.getTotalPieces(), true, true);
    }

    public List<HabitPuzzleSetSummaryResponse> listPuzzleSets(Long memberId) {
        List<HabitPuzzleSet> sets = puzzleSetRepository.findAllByOrderBySortOrderAsc();
        Map<Long, HabitPuzzleProgress> progressBySetId = loadProgressBySetId(memberId);

        List<HabitPuzzleSetSummaryResponse> result = new ArrayList<>();
        boolean reachedCurrent = false;
        for (HabitPuzzleSet set : sets) {
            HabitPuzzleProgress progress = progressBySetId.get(set.getId());
            boolean completed = progress != null && progress.isCompleted();

            String status;
            int collected;
            if (completed) {
                status = "COMPLETED";
                collected = set.getTotalPieces();
            } else if (!reachedCurrent) {
                status = "IN_PROGRESS";
                collected = progress != null ? progress.getCollectedPieces() : 0;
                reachedCurrent = true;
            } else {
                status = "LOCKED";
                collected = 0;
            }
            result.add(new HabitPuzzleSetSummaryResponse(set.getId(), set.getTitle(), set.getAssetKey(), set.getSortOrder(), status, collected, set.getTotalPieces()));
        }
        return result;
    }

    public List<HabitTopicCategoryResponse> listTopicCategories() {
        return topicCategoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(c -> new HabitTopicCategoryResponse(c.getId(), c.getTitle(), c.getSubtitle(), c.getIcon()))
                .toList();
    }

    public List<HabitTopicSummaryResponse> listTopicsByCategory(Long categoryId) {
        return topicRepository.findAllByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(t -> new HabitTopicSummaryResponse(t.getId(), t.getTitle(), t.getSubtitle(), t.getIcon()))
                .toList();
    }

    public HabitTopicDetailResponse getTopicDetail(Long topicId) {
        HabitTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ApiException(ErrorCode.HABIT_TOPIC_NOT_FOUND));
        return new HabitTopicDetailResponse(topic.getId(), topic.getTitle(), topic.getSubtitle(), topic.getIcon(), topic.getBody());
    }

    private HabitQuiz resolveTodayQuiz() {
        List<HabitQuiz> quizzes = quizRepository.findAllByOrderByIdAsc();
        if (quizzes.isEmpty()) {
            throw new ApiException(ErrorCode.HABIT_QUIZ_NOT_FOUND);
        }
        long dayIndex = LocalDate.now(KST).toEpochDay();
        int index = (int) Math.floorMod(dayIndex, quizzes.size());
        return quizzes.get(index);
    }

    private AwardResult awardPiece(Long memberId) {
        List<HabitPuzzleSet> sets = puzzleSetRepository.findAllByOrderBySortOrderAsc();
        if (sets.isEmpty()) {
            throw new ApiException(ErrorCode.HABIT_PUZZLE_SET_NOT_FOUND);
        }
        Map<Long, HabitPuzzleProgress> progressBySetId = loadProgressBySetId(memberId);

        Optional<HabitPuzzleSet> currentOpt = findCurrentSet(sets, progressBySetId);
        if (currentOpt.isEmpty()) {
            // 이미 모든 세트를 완성한 상태
            HabitPuzzleSet last = sets.get(sets.size() - 1);
            HabitPuzzleProgressResponse response =
                    new HabitPuzzleProgressResponse(last.getId(), last.getTitle(), last.getAssetKey(), last.getTotalPieces(), last.getTotalPieces(), true, true);
            return new AwardResult(response, false);
        }

        HabitPuzzleSet set = currentOpt.get();
        HabitPuzzleProgress progress = progressBySetId.getOrDefault(set.getId(), new HabitPuzzleProgress(memberId, set.getId()));
        boolean wasCompleted = progress.isCompleted();
        progress.addPiece(set.getTotalPieces());
        progressRepository.save(progress);
        boolean justCompleted = !wasCompleted && progress.isCompleted();

        HabitPuzzleProgressResponse response =
                new HabitPuzzleProgressResponse(set.getId(), set.getTitle(), set.getAssetKey(), progress.getCollectedPieces(), set.getTotalPieces(), progress.isCompleted(), false);
        return new AwardResult(response, justCompleted);
    }

    private Optional<HabitPuzzleSet> findCurrentSet(List<HabitPuzzleSet> sets, Map<Long, HabitPuzzleProgress> progressBySetId) {
        return sets.stream()
                .filter(set -> {
                    HabitPuzzleProgress progress = progressBySetId.get(set.getId());
                    return progress == null || !progress.isCompleted();
                })
                .findFirst();
    }

    private Map<Long, HabitPuzzleProgress> loadProgressBySetId(Long memberId) {
        return progressRepository.findAllByMemberId(memberId).stream()
                .collect(Collectors.toMap(HabitPuzzleProgress::getPuzzleSetId, Function.identity()));
    }

    private record AwardResult(HabitPuzzleProgressResponse progress, boolean justCompleted) {
    }
}
