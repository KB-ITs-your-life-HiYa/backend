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
├── security/       JWT 발급·검증, 인증 필터
└── <도메인>/
    ├── controller/
    ├── service/
    ├── repository/   Spring Data JPA
    ├── dto/          요청·응답 객체
    └── domain/       JPA 엔티티

src/main/resources/
├── db/migration/   Flyway 마이그레이션
└── db/seed/        개발용 시드 (R__)
```

`security/` 가 도메인 밖에 있는 이유는, 특정 도메인의 기능이 아니라
**모든 요청이 거쳐 가는 공통 장치**이기 때문이다. 회원을 다루지만 `member/` 아래가 아니다.

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

## DB 운영 방침

**개발 중에는 로컬 도커를 쓴다.** Supabase 프로필은 통합이 필요해질 때 켠다.

```bash
./gradlew bootRun                                    # 기본
SPRING_PROFILES_ACTIVE=supabase ./gradlew bootRun    # 통합할 때만
```

혼자 쓰는 DB 여야 하는 이유는 `flyway_schema_history` 가 DB 하나에 하나뿐이기 때문이다.
여러 명이 같은 DB 에 붙으면, 남이 올린 마이그레이션이 이미 적용된 상태에서
그 파일을 아직 pull 하지 않은 사람의 앱이 기동에 실패한다.
**자기 도메인만 만지고 있어도 발생한다.**

로컬은 각자 히스토리가 따로라 이 문제가 없고, `docker compose down -v` 로
언제든 백지에서 다시 시작할 수 있다.

**Supabase 로 옮기는 시점은 팀이 함께 정한다.** 각자 임의로 붙지 않는다.

### 테스트 데이터

데이터는 각자 로컬에 따로 쌓인다. 팀원끼리 같은 계정으로 개발하려면
`db/seed/R__dev_seed.sql` 에 넣고 커밋한다. 로컬 프로필에만 적용된다.

`R__` 파일은 내용이 바뀌면 자동으로 다시 적용되므로,
**여러 번 실행돼도 결과가 같게** 쓴다 (`DELETE` 후 `INSERT` 등).

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

**백엔드가 직접 JWT를 발급한다.** Spring Security + JJWT.

```
로그인          POST /api/v1/auth/login   → { token, member }
이후 모든 요청   Authorization: Bearer <token>
```

- 토큰에는 **회원 id와 시각만** 담는다. 이메일·등급은 담지 않는다 —
  값이 바뀌어도 토큰은 그대로라 낡은 정보를 들고 다니게 된다. id로 DB에서 읽는다
- 유효기간 **7일**, **리프레시 토큰 없음.** 만료되면 다시 로그인한다
- 비밀번호는 BCrypt 해시로만 저장한다. 평문을 저장하거나 로그에 남기지 않는다

### 컨트롤러에서 로그인한 회원 꺼내기

**경로에 회원 id를 넣지 않는다.** 토큰에서 꺼낸다.

```java
@GetMapping("/me")
public ApiResponse<MemberResponse> me(@AuthenticationPrincipal AuthenticatedMember me) {
    return ApiResponse.ok(memberService.findMe(me.id()));
}
```

`/members/{id}` 로 만들면 남의 id를 넣어 조회할 수 있다(IDOR).
불가피하게 경로에 `{id}`가 있어야 하면, 조회 쿼리에 소유 조건을 넣어 "내 것 중에서" 찾는다.

### 공개 경로 추가

로그인 없이 접근해야 하는 경로는 `SecurityConfig`의 `PUBLIC` 배열에 추가한다.
**여기에는 `/api/v1`을 직접 쓴다.** `WebConfig`의 prefix는 컨트롤러 매핑에만 적용되고,
시큐리티 필터는 그보다 앞에서 실제 URL로 판단하기 때문이다.

기본은 **인증 필요**다(`anyRequest().authenticated()`). 공개가 예외다.

### Swagger에서 테스트하기

`/swagger-ui.html` → **Authorize** → 로그인으로 받은 `token` 값만 붙여넣기.
`Bearer`는 자동으로 붙으므로 직접 쓰지 않는다.

개발용 계정 (비밀번호 모두 `demo1234`)

| 계정 | 상태 |
| --- | --- |
| `demo1@fledge.dev` | 보호종료 550일차 · D-1275 |
| `demo2@fledge.dev` | 보호종료 1645일차 · **D-180** (데모용) |

### 설정

`jwt.secret`은 환경변수 `JWT_SECRET`으로 덮어쓸 수 있고, 없으면 개발용 기본값을 쓴다.
**최소 32바이트**여야 한다. 짧으면 기동 시 예외가 난다.

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

_최종 갱신: 2026-09-02_
