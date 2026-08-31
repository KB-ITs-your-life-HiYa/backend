# 자립동행 D-1825 — 백엔드

자립준비청년 금융 자립 플랫폼 **HiYa** 의 백엔드.
KB IT's Your Life 해커톤 · 본선 2026.09.10~12

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 · 런타임 | Java 21 (LTS) |
| 프레임워크 | Spring Boot 3.5.16 (Web MVC, Data JPA, Validation, Actuator) |
| 빌드 | Gradle 9.7.1 (wrapper) |
| DB | PostgreSQL 17 — 로컬은 Docker, 운영은 Supabase |
| 스키마 관리 | Flyway (`src/main/resources/db/migration`) |
| 외부 API | Gemini, 국토교통부 마이홈포털 |

## 준비물

- **JDK 21** — `brew install openjdk@21` (macOS) / Temurin 21 (Windows)
- **Docker** — Docker Desktop 또는 colima

JDK 를 여러 개 깔아둬도 된다. Gradle toolchain 이 `build.gradle` 에 박힌 21 을 알아서 골라 쓴다.

## 로컬 실행

```bash
# 1. 로컬 DB 띄우기 (Postgres 17)
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

## 설정 파일 · 프로필

DB 를 **로컬 도커** 로 쓸지 **Supabase** 로 쓸지는 프로필로 고른다.

```bash
./gradlew bootRun                                    # 로컬 도커 (기본)
SPRING_PROFILES_ACTIVE=supabase ./gradlew bootRun    # Supabase
```

| 파일 | 용도 | 커밋 |
| --- | --- | --- |
| `application.properties` | 공통 설정 (JPA · Flyway · Swagger) | ✅ |
| `application-local.properties` | 로컬 도커 접속 (전부 더미값) | ✅ |
| `application-supabase.properties` | Supabase 접속 | ❌ **금지** |
| `application-supabase.properties.example` | 위 파일의 템플릿 | ✅ |
| `application-secret.properties` | Gemini · 마이홈포털 등 API 키 | ❌ **금지** |
| `application-secret.properties.example` | 위 파일의 템플릿 | ✅ |

### Supabase 로 붙기

```bash
cp src/main/resources/application-supabase.properties.example \
   src/main/resources/application-supabase.properties
```

값을 채운 뒤 `SPRING_PROFILES_ACTIVE=supabase` 로 실행한다.

**연결 문자열은 Session pooler 를 쓴다.** Dashboard > Connect > Direct 탭에 세 종류가 있다.

| 종류 | 쓸 수 있나 |
| --- | --- |
| Direct connection (`db.<ref>.supabase.co`) | ❌ IPv6 전용이라 IPv4 망에서는 연결되지 않는다 |
| Transaction pooler (포트 6543) | ❌ JPA 의 prepared statement 를 지원하지 않는다 |
| **Session pooler (포트 5432, 호스트에 `pooler`)** | ✅ 이걸 쓴다 |

사용자명이 `postgres.<프로젝트ref>` 형태여야 풀러다. 그냥 `postgres` 면 Direct 다.
비밀번호는 percent-encoding 하지 않고 원본 그대로 넣는다.

### API 키

```bash
cp src/main/resources/application-secret.properties.example \
   src/main/resources/application-secret.properties
```

`application.properties` 가 이 파일을 **optional** 로 읽으므로, 파일이 없어도 앱은 정상 기동한다.
(그 키를 쓰는 기능만 동작하지 않는다)

> ⚠️ **DB 접속 정보를 `application-secret.properties` 에 적으면 조용히 무시된다.**
> 프로필 파일(`application-local.properties`) 이 import 된 파일보다 우선하기 때문이다.
> DB 는 반드시 프로필 파일에 적을 것.

## 컨벤션

패키지 구조 · API 규칙 · 커밋과 브랜치 규칙은 [`AGENTS.md`](./AGENTS.md) 에 있다.
작업 전에 한 번 읽을 것.

**아직 정하지 않은 것** — 인증 방식 (Supabase Auth vs 자체 JWT). 팀 논의 후 확정한다.
