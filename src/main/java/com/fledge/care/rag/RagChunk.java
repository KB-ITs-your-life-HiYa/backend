package com.fledge.care.rag;

/**
 * docs/rag 문서 하나를 검색 단위로 쪼갠 조각.
 *
 * docId: 상대경로(확장자 제외) + "#" + 문서 안 순번. 예) benefits/01_self_reliance_allowance#2
 * title: 마크다운의 "## 소제목" 또는 JSON 배열 원소의 "title". 검색 매칭 가중치를 더 준다.
 * content: 그 아래 본문 또는 JSON 원소의 "content".
 * sourceUrl: JSON으로 들어온 데이터(공공 API)만 채워진다. 손으로 쓴 마크다운은 null.
 */
public record RagChunk(String docId, String title, String content, String sourceUrl) {

    /** Gemini 프롬프트에 넣을 때 쓰는 "제목 + 본문" 형태 */
    public String toPromptBlock() {
        return "## " + title + "\n" + content.strip();
    }
}
