# 자립동행 D-1825 — 백엔드

자립준비청년 금융 자립 플랫폼 **HiYa** 의 백엔드.
KB IT's Your Life 해커톤 · 본선 2026.09.10~12

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 · 런타임 | Java 21 (LTS) |
| 프레임워크 | Spring Boot 3.5.16 (Web MVC, Data JPA, Validation, Actuator) |
| 빌드 | Gradle 9.7.1 (wrapper) |
| DB | PostgreSQL 16 — 로컬은 Docker, 운영은 Supabase |
| 스키마 관리 | Flyway (`src/main/resources/db/migration`) |
| 외부 API | Gemini, 국토교통부 마이홈포털 |

## 준비물

- **JDK 21** — `brew install openjdk@21` (macOS) / Temurin 21 (Windows)
- **Docker** — Docker Desktop 또는 colima

JDK 를 여러 개 깔아둬도 된다. Gradle toolchain 이 `build.gradle` 에 박힌 21 을 알아서 골라 쓴다.

## 로컬 실행

```bash
# 1. 로컬 DB 띄우기 (Postgres 16)
docker compose up -d

# 2. 앱 실행
./gradlew bootRun

# 3. 확인
curl http://localhost:8080/actuator/health   # -> {"status":"UP", ...}
```

정지는 `docker compose down` (데이터는 볼륨에 남는다).

## API 문서

컨트롤러를 만들면 springdoc-openapi 가 스캔해서 문서를 자동으로 만든다. 명세서를 따로 쓰지 않는다.

| 주소 | 내용 |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Swagger UI (브라우저에서 바로 호출 테스트 가능) |
| http://localhost:8080/v3/api-docs | OpenAPI 3 JSON |

설명을 더 붙이고 싶으면 `@Operation(summary = "...")`, `@Schema(description = "...")` 를 쓴다.
안 붙여도 경로 · 파라미터 · 응답 타입은 자동으로 문서에 들어간다.

운영 배포 시에는 prod 프로필에 `springdoc.api-docs.enabled=false` 를 넣어 문서를 막는다.

## DB 스키마

스키마는 **Flyway 마이그레이션 파일로만** 바꾼다. 대시보드나 콘솔에서 직접
`CREATE TABLE` 하지 않는다. 앱이 기동될 때 `src/main/resources/db/migration` 의
SQL 이 순서대로 적용되고, 어디까지 적용했는지가 `flyway_schema_history` 에 기록된다.

로컬 도커든 Supabase 든 같은 파일을 적용하므로 스키마가 항상 같아진다.

작성 규칙은 [`db/migration/README.md`](src/main/resources/db/migration/README.md) 를 볼 것.
요약하면 파일명은 `V<YYYYMMDD>_<HHmm>__<영문설명>.sql` 이고,
**이미 올라간 파일은 절대 수정하지 않는다.**

## 설정 파일

| 파일 | 용도 | 커밋 |
| --- | --- | --- |
| `application.properties` | 공통 설정 | ✅ |
| `application-local.properties` | 로컬 개발용 (전부 더미값) | ✅ |
| `application-secret.properties` | 실제 비밀값 (Supabase·Gemini·마이홈포털 키) | ❌ **금지** |
| `application-secret.properties.example` | 위 파일의 템플릿 | ✅ |

비밀값이 필요해지면:

```bash
cp src/main/resources/application-secret.properties.example \
   src/main/resources/application-secret.properties
```

`application.properties` 가 이 파일을 **optional** 로 읽으므로, 파일이 없어도 앱은 정상 기동한다.
(그 키를 쓰는 기능만 동작하지 않는다)

## 아직 정하지 않은 것

- API 경로 규칙 (`/api/v1/...` prefix 방식)
- 패키지 구조 컨벤션 — 도메인 우선(`com.fledge.<도메인>/{controller,service,repository,dto,domain}`) 제안
- 인증 방식 (Supabase Auth vs 자체 JWT)

팀 논의 후 `AGENTS.md` 에 정본으로 정리한다.
