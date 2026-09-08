package com.fledge.care.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * docs/rag 아래 .md / .json 파일을 전부 읽어 청크로 쪼개 메모리에 들고 있는다.
 *
 * 【왜 클래스패스가 아니라 파일시스템인가】
 * docs/rag 는 소스코드가 아니라 계속 채워지는 자료다. RagDataExportRunner(rag-export 프로필)가
 * 공공 API에서 받아온 데이터를 여기 JSON으로 새로 써 넣으므로, 빌드에 구워 넣는 클래스패스
 * 리소스보다 평범한 폴더 쪽이 더 잘 맞는다. 앱을 다시 빌드하지 않고 파일만 바꿔도 된다
 * (지금은 기동 시 1회만 읽는다 — 자주 바뀐다면 나중에 주기적 재로딩을 추가하면 된다).
 *
 * 【문서 두 종류】
 *  - .md   : "## 소제목" 마다 하나의 청크. 사람이 직접 쓰는 서비스 안내/설명용 (docs/rag/service 등).
 *  - .json : [{"title":..., "content":..., "sourceUrl":...}, ...] 배열. 공공 API에서 받아온
 *            지원금·주거 공고처럼 건수가 많고 구조가 있는 데이터용 (RagDataExportRunner 가 만든다).
 *  - 하위 폴더(benefits/housing/service)는 자유롭게 나눠도 된다 — docId 에 상대경로가 그대로 들어간다.
 */
@Component
@Slf4j
public class RagKnowledgeBase {

    private static final Pattern HEADING = Pattern.compile("(?m)^##\\s+(.+?)\\s*$");

    @Value("${rag.docs-path:docs/rag}")
    private String docsPath;

    private final ObjectMapper mapper;
    private List<RagChunk> chunks = List.of();

    public RagKnowledgeBase(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    void load() {
        Path root = Path.of(docsPath);
        if (!Files.isDirectory(root)) {
            // 아직 docs/rag 를 안 만든 환경(예: 팀원 새 클론)에서도 앱은 정상 기동돼야 한다.
            log.warn("RAG 문서 폴더가 없음: {} (작업 디렉터리 기준 상대경로)", root.toAbsolutePath());
            this.chunks = List.of();
            return;
        }

        List<RagChunk> loaded = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = file.getFileName().toString();
                try {
                    if (name.endsWith(".md")) {
                        loaded.addAll(parseMarkdown(docId(root, file), Files.readString(file, StandardCharsets.UTF_8)));
                    } else if (name.endsWith(".json")) {
                        loaded.addAll(parseJson(docId(root, file), Files.readString(file, StandardCharsets.UTF_8)));
                    }
                } catch (IOException e) {
                    // 문서 하나가 깨져도 나머지는 계속 서비스한다
                    log.warn("RAG 문서를 읽지 못함: file={}, cause={}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("RAG 문서 폴더를 순회하지 못함: {}", e.getMessage());
        }

        this.chunks = Collections.unmodifiableList(loaded);
        log.info("RAG 지식베이스 로드 완료: 청크 {}개 (폴더: {})", chunks.size(), root.toAbsolutePath());
    }

    public List<RagChunk> chunks() {
        return chunks;
    }

    private static String docId(Path root, Path file) {
        String rel = root.relativize(file).toString().replace('\\', '/');
        return rel.replaceFirst("\\.(md|json)$", "");
    }

    /** "## 소제목" 마다 하나의 청크로 쪼갠다. "# 문서 제목" 같은 최상단 줄은 청크가 아니라 건너뛴다. */
    static List<RagChunk> parseMarkdown(String docId, String markdown) {
        List<RagChunk> result = new ArrayList<>();
        Matcher matcher = HEADING.matcher(markdown);
        List<Integer> contentStarts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            titles.add(matcher.group(1).trim());
            contentStarts.add(matcher.end());
        }
        for (int i = 0; i < contentStarts.size(); i++) {
            int start = contentStarts.get(i);
            int end = (i + 1 < contentStarts.size()) ? nextHeadingStart(markdown, start) : markdown.length();
            String content = markdown.substring(start, end).strip();
            if (content.isBlank()) continue;
            result.add(new RagChunk(docId + "#" + (i + 1), titles.get(i), content, null));
        }
        return result;
    }

    private static int nextHeadingStart(String markdown, int from) {
        Matcher m = HEADING.matcher(markdown);
        return m.find(from) ? m.start() : markdown.length();
    }

    /** [{"title":..., "content":..., "sourceUrl":...}, ...] 형태만 지원한다. 필드가 비면 그 원소는 건너뛴다. */
    List<RagChunk> parseJson(String docId, String json) throws IOException {
        JsonNode array = mapper.readTree(json);
        List<RagChunk> result = new ArrayList<>();
        if (!array.isArray()) return result;
        int i = 0;
        for (JsonNode item : array) {
            i++;
            String title = item.path("title").asText("").strip();
            String content = item.path("content").asText("").strip();
            if (title.isBlank() || content.isBlank()) continue;
            String url = item.path("sourceUrl").asText("");
            result.add(new RagChunk(docId + "#" + i, title, content, url.isBlank() ? null : url));
        }
        return result;
    }
}
