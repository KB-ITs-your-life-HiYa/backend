package com.fledge.care.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 벡터 검색 없이 쓰는 "키워드/간단 유사도" 매칭.
 *
 * 한국어는 조사가 붙어서("자립수당이", "자립수당은") 단어 단위로 쪼개면 질문과 문서의 표현이
 * 정확히 일치하기 어렵다. 형태소 분석기를 새로 붙이는 대신, 데모 규모(문서 수십~수백 개)에서
 * 충분히 잘 작동하는 문자 2-gram(바이그램) 자카드 유사도로 근사한다.
 * 나중에 문서·질문이 많아지면 이 클래스만 임베딩 기반 벡터 검색으로 바꾸면 된다
 * (RagKnowledgeBase 가 만든 청크 구조와 호출부는 그대로 쓸 수 있다).
 */
@Component
public class RagRetriever {

    /** 이 값보다 유사도가 낮으면 "근거 없음" 으로 보고 아예 Gemini를 호출하지 않는다 */
    private static final double MIN_SCORE = 0.02;

    public record Match(RagChunk chunk, double score) {}

    public List<Match> topMatches(String question, List<RagChunk> chunks, int topK) {
        Set<String> questionGrams = bigrams(question);
        if (questionGrams.isEmpty()) return List.of();

        List<Match> scored = new ArrayList<>();
        for (RagChunk chunk : chunks) {
            // 소제목이 맞으면 본문만 맞을 때보다 훨씬 더 관련 있는 청크일 확률이 높다
            double titleScore = jaccard(questionGrams, bigrams(chunk.title()));
            double contentScore = jaccard(questionGrams, bigrams(chunk.content()));
            double score = titleScore * 2 + contentScore;
            if (score >= MIN_SCORE) scored.add(new Match(chunk, score));
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(Match::score).reversed())
                .limit(Math.max(topK, 0))
                .toList();
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        if (intersection.isEmpty()) return 0;
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> bigrams(String text) {
        if (text == null) return Set.of();
        // 공백/문장부호는 매칭에 도움이 안 되니 지우고 순수 글자만 이어붙인 뒤 2글자씩 자른다
        String cleaned = text.toLowerCase(Locale.KOREAN).replaceAll("[\\s\\p{Punct}]+", "");
        if (cleaned.length() < 2) return cleaned.isEmpty() ? Set.of() : Set.of(cleaned);
        Set<String> grams = new HashSet<>();
        for (int i = 0; i < cleaned.length() - 1; i++) {
            grams.add(cleaned.substring(i, i + 2));
        }
        return grams;
    }
}
