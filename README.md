# 자립동행 D-1825 — 백엔드

자립준비청년 금융 자립 플랫폼 **HiYa** 의 백엔드.
KB IT's Your Life 해커톤 · 본선 2026.09.10~12

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 · 런타임 | Java 21 (LTS) |
| 프레임워크 | Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Actuator) |
| 빌드 | Gradle 9.7.1 (wrapper) |
| DB | PostgreSQL 16 — 로컬은 Docker, 운영은 Supabase |
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
- DB 스키마 마이그레이션 도구 (Flyway 제안)
- 인증 방식 (Supabase Auth vs 자체 JWT)

팀 논의 후 `AGENTS.md` 에 정본으로 정리한다.
