# AGENTS.md — 자립동행 D-1825 백엔드

AI 코딩 도구(Claude Code·Cursor·Copilot 등)와 팀원이 함께 지키는 규칙.
**프로젝트 규칙은 이 문서를 기준으로 합니다.**

## 스택

Java 21 · Spring Boot 3.5.16 · Gradle 9.7.1(wrapper) · PostgreSQL 17 ·
Flyway 11.7.2 · springdoc-openapi 2.9.0

**버전 주의.** springdoc은 Boot 버전에 맞춰야 한다. Boot 3.5 → **springdoc 2.x**.
검색해서 나오는 Boot 4 자료(springdoc 3.x)를 그대로 따라 하면 동작하지 않는다.

프론트는 별도 레포다. → `KB-ITs-your-life-HiYa/frontend`

---

## 폴더 구조

```
src/main/java/com/fledge/
├── common/         ApiResponse, ErrorCode
├── config/         WebConfig, OpenApiConfig, SecurityConfig
├── exception/      ApiException, GlobalExceptionHandler
└── <도메인>/
    ├── controller/
    ├── service/
    ├── repository/   Spring Data JPA
    ├── dto/          요청·응답 객체
    └── domain/       JPA 엔티티

src/main/resources/
└── db/migration/   Flyway 마이그레이션
```

**도메인 우선 구조다.** 계층(`controller/`)이 아니라 도메인(`housing/`)이 먼저 온다.
기능 하나를 고칠 때 폴더 하나만 보면 되고, 담당 경계가 폴더로 드러나 머지 충돌이 줄어든다.

**빈 폴더는 만들지 않는다.** 엔티티가 없으면 `domain/`도 없다. 필요할 때 만든다.

### 도메인

| 도메인 | 화면 |
| --- | --- |
| `housing` | 독립지원 — 임대주택 캘린더·체크리스트 |
| `care` | 온라인 케어 — 대화·개입 |
| `benefit` | 정부지원금 매칭 |
| `budget` | D-day 생활비 관리 |
| `habit` | 금융습관 트레이닝 |
| `member` | 회원·프로필 |

### 계층별 역할

| 폴더 | 역할 | 금지 |
| --- | --- | --- |
| `controller/` | 요청을 서비스에 넘기고 응답을 감싼다 | 비즈니스 로직, DB 접근 |
| `service/` | 비즈니스 로직. 트랜잭션 경계 | HTTP 관련 코드(`HttpServletRequest` 등) |
| `repository/` | Spring Data JPA 인터페이스 | — |
| `dto/` | 요청·응답 전용 객체 | 비즈니스 로직 |
| `domain/` | JPA 엔티티 | — |

**DTO와 엔티티를 구분한다.** DTO는 API 계약, 엔티티는 DB 구조다.
하나로 쓰면 컬럼을 바꿀 때 API 응답이 같이 바뀐다. **엔티티를 그대로 반환하지 않는다.**

명명은 **단수**를 쓴다 (`HousingNotice`, 테이블 `housing_notice`).
URL만 컬렉션일 때 복수형.

---

## API 규칙

### 경로

**컨트롤러에 `/api/v1`을 쓰지 않는다.** `WebConfig`가 일괄로 붙인다.

```java
@Override
public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix("/api/v1", HandlerTypePredicate.forBasePackage("com.fledge"));
}
```

```java
@RestController
@RequestMapping("/housing")     // 실제 경로 = /api/v1/housing
```

매번 직접 쓰면 누군가 빠뜨린다.

대상을 `com.fledge` 패키지로 한정하는 이유는, `@RestController` 로 잡으면
springdoc 의 문서 엔드포인트(`/v3/api-docs`)까지 prefix 가 붙어 Swagger 가 깨지기 때문이다.
actuator(`/actuator/health`)는 어느 쪽이든 영향받지 않는다.

### CORS

브라우저에서 API 를 호출할 수 있는 출처는 `cors.allowed-origin-patterns` 로 관리한다.
프론트를 웹(`expo start` 후 `w`)으로 띄울 때 필요하고, 실기기(Expo Go)는
네이티브 fetch 라 CORS 와 무관하다.

`allowCredentials` 는 켜지 않는다. 인증은 쿠키가 아니라 `Authorization` 헤더로 하므로
필요 없고, 켜면 출처를 와일드카드로 둘 수 없게 된다.

운영 배포 시에는 prod 프로필에서 실제 도메인으로 덮어쓴다.

**개인 리소스는 `/members/me/` 아래에 둔다.** 회원 식별자를 경로에 노출하지 않아
남의 리소스를 지목할 수 없게 한다(IDOR 차단).
경로에 `{id}`가 있으면 조회 쿼리에 소유 조건을 넣어 "내 것 중에서" 찾는다.

### 응답

성공은 `ApiResponse.ok(data)`로 감싼다.
실패는 `ApiException`을 던진다. `GlobalExceptionHandler`가 변환한다.
**컨트롤러에서 직접 실패 응답을 만들지 않는다.**

```java
return ApiResponse.ok(housingService.findNotice(id));

throw new ApiException(ErrorCode.HOUSING_NOTICE_NOT_FOUND);
```

```json
{ "success": true,  "data": { ... }, "error": null }
{ "success": false, "data": null, "error": { "code": "HOUSING_NOTICE_NOT_FOUND", "message": "공고를 찾을 수 없습니다" } }
```

HTTP 상태 코드도 의미대로 쓴다(404, 400, 401 …).
새 에러는 `ErrorCode`에 추가하고 쓴다. 코드명은 `<도메인>_<사유>`.

### 문서

**API 명세서를 따로 쓰지 않는다.** springdoc이 컨트롤러를 스캔해 자동 생성한다.

- Swagger UI `localhost:8080/swagger-ui.html`
- OpenAPI JSON `localhost:8080/v3/api-docs`

여유가 있으면 `@Operation(summary = "...")`을 붙인다.
안 붙여도 경로·파라미터·응답 타입은 들어간다.

---

## DB 규칙

**스키마는 Flyway 마이그레이션 파일로만 바꾼다.**
대시보드·콘솔에서 직접 `CREATE TABLE` 하지 않는다.

```
V<YYYYMMDD>_<HHmm>__<영문설명>.sql        ← 밑줄 2개
V20260901_1430__create_care_signal.sql
```

- 날짜·시각을 버전으로 쓰는 이유는 **두 사람이 동시에 `V3`을 만들어 충돌하는 것**을 막기 위함
- **이미 푸시된 `V` 파일은 수정 금지.** 남이 만든 것도 마찬가지다.
  Flyway가 체크섬을 저장하므로 내용이 바뀌면 이미 적용한 팀원의 앱이 기동에 실패한다.
  잘못됐으면 **되돌리는 새 파일**을 추가한다
- `ddl-auto=none` 이므로 **엔티티와 마이그레이션 SQL을 같이 작성**해야 한다

---

## 설정 파일

DB를 로컬 도커로 쓸지 Supabase로 쓸지는 **프로필**로 고른다.

| 파일 | 용도 | 커밋 |
| --- | --- | --- |
| `application.properties` | 공통 (JPA·Flyway·Swagger) | ✅ |
| `application-local.properties` | 로컬 도커 접속 (더미값) | ✅ |
| `application-supabase.properties` | Supabase 접속 | ❌ |
| `application-secret.properties` | Gemini·마이홈포털 API 키 | ❌ |

**DB 접속 정보를 `application-secret.properties`에 적지 않는다.**
프로필 파일이 import된 파일보다 우선해서, 적어도 조용히 무시되고 로컬로 붙는다.

Supabase 연결 문자열은 **Session pooler**만 쓴다.
Direct는 IPv6 전용, Transaction pooler(6543)는 JPA의 prepared statement를 지원하지 않는다.

---

## 인증

> **미정.** 팀 논의 후 확정한다. 확정 전까지 인증이 필요한 엔드포인트는 만들지 않는다.
>
> - **A안** Supabase Auth 발급 + Spring Security Resource Server 검증 (JWKS · ES256)
> - **B안** 백엔드 자체 JWT 발급 + Spring Security 전체 구성

---

## 명령어

```bash
docker compose up -d                                 # 로컬 DB
./gradlew bootRun                                    # 로컬 도커로 실행
SPRING_PROFILES_ACTIVE=supabase ./gradlew bootRun    # Supabase 로 실행
./gradlew build                                      # 빌드 + 테스트
docker compose down -v && docker compose up -d       # 로컬 DB 완전 초기화
```

---

## Git

### 커밋

**제목 한 줄만 쓴다.** 상세 설명은 전부 PR 본문에 적는다.

```
<type>: <한글 제목>

feat: 임대주택 캘린더 조회 API 추가
```

- type: `feat` · `fix` · `docs` · `style` · `refactor` · `perf` · `test` · `chore`
- 명령조·현재시제, **끝에 마침표 없음**, 한글 30자 내외

### 브랜치

**모든 작업은 새 브랜치에서 한다.** `develop`·`main`에 직접 커밋·push 금지.

```bash
git switch develop && git pull
git switch -c feature/housing-calendar
```

- 기본 브랜치는 **`develop`**. `main`은 안정 완성본만
- 이름은 `<type>/<영문-설명>` — 소문자와 하이픈만 (공백·`_`·대문자·한글 금지)
- type: `feature` · `fix` · `refactor` · `docs` · `chore`

### PR

- 타겟은 `develop`, 머지 후 브랜치 삭제
- 본문에 **무엇을 / 왜 / 변경 사항 / 확인 방법**을 적는다
- 커밋에 본문을 쓰지 않는 대신 여기를 충실히 쓴다

---

_최종 갱신: 2026-08-31_
