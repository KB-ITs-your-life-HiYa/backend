# 자립동행: D-1825

<div align="center">

  <!-- TODO: 팀/서비스 로고 이미지 -->

  **자립준비청년의 D-1825 자립을 함께 준비하는 서비스**

  생활비·자산 관리, 정부지원금 매칭, 독립지원(주거) 정보, 금융습관 트레이닝,
  그리고 이상징후를 감지해 먼저 말을 거는 온라인 케어까지 한 곳에서 제공하는
  자립준비청년 전용 자립 지원 플랫폼입니다.

</div>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3.5" />
  <img src="https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL 17" />
  <img src="https://img.shields.io/badge/Gradle-9.7.1-02303A?logo=gradle&logoColor=white" alt="Gradle" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white" alt="Docker" />
</p>

---

## 목차

- [서비스 소개](#서비스-소개)
- [핵심 기능](#핵심-기능)
- [팀원 및 담당 기능](#팀원-및-담당-기능)
- [시스템 아키텍처](#시스템-아키텍처)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [로컬 실행](#로컬-실행)
- [주요 API](#주요-api)
- [협업 방식](#협업-방식)

## 서비스 소개

자립동행은 보호종료(시설·위탁 퇴소)를 앞두거나 이미 퇴소한 자립준비청년이 스스로 생활비를 관리하고,
받을 수 있는 정부·지자체 지원금과 임대주택 정보를 놓치지 않도록 돕는 서비스입니다.
소비·저축 데이터를 바탕으로 이상 징후를 먼저 감지해 말을 걸고, 필요하면 담당 상담사에게 연결합니다.

### 해결하고자 한 문제

- 보호 종료 후 지원(수당)이 끊기는 시점을 스스로 준비하지 못해 생활비가 급격히 어려워지는 문제
- 지자체·기관마다 흩어져 있는 지원금·임대주택 정보를 자립준비청년이 놓치는 문제
- 담당 상담사 1인이 많은 청년을 맡아, 위기 징후를 조기에 발견하기 어려운 문제
- 예금·적금 등 기초 금융 지식을 배울 기회가 부족한 문제

### 주요 사용자

- 보호 종료를 앞두고 있거나 이미 자립한 자립준비청년
- 담당 청년의 상태를 관리하고 위기 신호에 대응해야 하는 자립지원 전담 상담사

### 개발 기간

2026년 8월 31일 ~ 2026년 9월 12일 (KB IT's Your Life 해커톤 본선)

## 핵심 기능

### 1. 생활비 관리 · D-day 예측

> 담당: [정유민](https://github.com/yumin0411)

- 연동 계좌 요약 및 잔액 조회
- 월별 예산 등록·조회·삭제
- 월별 소비 요약 및 소비 리포트
- 남은 지원(수당) 종료 시점을 기준으로 한 생활비 소진 예측(D-day)
- 사용자의 실제 이번 달 지출·예산·고정비·저축·계좌 데이터만 근거로 답하는 생활비 챗봇
  (범위 밖 질문이거나 근거가 부족하면 "답변이 어렵다"고 안내)

### 2. 정부지원금 매칭

> 담당: [조하얀](https://github.com/yanh2)

- 복지로·정부24·온통청년 공공 API에서 지원금 데이터를 수집해 정기 적재하는 배치 파이프라인
- Gemini를 이용한 지원금 원문 파싱 및 카테고리·지역 정보 보정
- 회원 설문(가족관계, 소득, 거주지 등) 기반 맞춤 지원금 매칭 조회
- 관심 지원금 저장·조회·삭제(스크랩)

### 3. 독립지원 — 주거

> 담당: [임민지](https://github.com/im-minji)

- 국토교통부 마이홈포털 연동 임대주택 공고 캘린더·상세 조회
- 특정 지원금과 연관된(전세임대 등) 자립준비청년 대상 상시 모집 공고 조회
- 입주를 위한 개인별 체크리스트 등록·항목 추가/완료 체크/삭제
- 개인 주거 자격 프로필(자격 판별용 정보) 조회·수정

### 4. 온라인 케어 — 이상징후 감지 & 대화 상담

> 담당: [이은수](https://github.com/lusnue), [전소현](https://github.com/ssohy)

- 기준일까지의 금융 거래를 확인해 케어 상태를 갱신하고 이상징후를 평가
- 버튼형 상담 응답 및 일정 변경, 자유 메시지 입력
- Gemini 기반 상담 답변 생성 및 실패 시 재시도
- 상담 맥락에 맞는 온통청년 정책 카드 조회
- 사용자 동의 후 담당 상담사에게 연계(referral) 요청
- 지원금·독립지원·서비스 이용 FAQ 챗봇(RAG): 질문을 문자 2-gram 자카드 유사도로 사내 문서(`docs/rag`)와 매칭해 관련 근거만 추려 Gemini에 전달하고, 근거 문서에 없는 내용은 답하지 않도록 제한. 답변에는 참고한 문서 출처를 함께 반환

### 5. 금융습관 트레이닝

> 담당: [전소현](https://github.com/ssohy)

- 오늘의 금융 퀴즈 조회 및 답안 제출
- 퀴즈 정답으로 퍼즐 조각을 모으는 수집형 퍼즐 세트 진행률·갤러리 조회
- 신용/대출, 저축/투자, 소비습관 카테고리별 금융 상식 토픽·상세 콘텐츠 제공

### 6. 상담사 포털

> 담당: [임민지](https://github.com/im-minji)

- 담당 청년 목록 조회
- 청년으로부터 받은 상담 연계 요청 조회

### 7. 회원 · 인증

> 담당: [임민지](https://github.com/im-minji)

- Spring Security + JJWT 기반 자체 JWT 로그인(`/auth/login`), 비밀번호는 BCrypt 저장
- 내 정보 조회
- 온보딩 설문 조회·저장

## 팀원 및 담당 기능

| 사진 | 팀원 | GitHub | 주요 담당 | 구현 기여 |
|:---:|:---:|:---:|---|---|
| <img width="100" height="100" src="https://github.com/im-minji.png?size=200" /> | 임&nbsp;민&nbsp;지(팀장) | [@im-minji](https://github.com/im-minji) | 독립지원(주거), 상담사 포털, 회원·인증 | 마이홈포털 연동 임대주택 캘린더·공고 조회, 입주 체크리스트 CRUD, 주거 자격 프로필 조회·수정, 상담사 포털(담당 청년·연계 요청 조회), JWT 로그인·BCrypt 인증, 온보딩 설문 조회·저장 |
| <img width="100" height="100" src="https://github.com/lusnue.png?size=200" /> | 이&nbsp;은&nbsp;수 | [@lusnue](https://github.com/lusnue) | 온라인 케어(상담·이상징후) | 금융 거래 기반 케어 상태 평가·갱신, 버튼형 상담 응답·일정 변경, Gemini 상담 답변 생성·재시도, 온통청년 정책 카드 조회, 담당 상담사 연계 요청 |
| <img width="100" height="100" src="https://github.com/ssohy.png?size=200" /> | 전&nbsp;소&nbsp;현 | [@ssohy](https://github.com/ssohy) | 온라인 케어 FAQ(RAG), 금융습관 트레이닝 | 문자 2-gram 자카드 유사도 기반 경량 RAG 검색과 Gemini 응답 생성으로 지원금·독립지원·서비스 이용 FAQ 챗봇 구현, 오늘의 퀴즈·퍼즐 세트, 금융 상식 카테고리·토픽 |
| <img width="100" height="100" src="https://github.com/yumin0411.png?size=200" /> | 정&nbsp;유&nbsp;민 | [@yumin0411](https://github.com/yumin0411) | 생활비 관리·D-day 예측 | 계좌 요약 조회, 월별 예산 CRUD, 소비 요약·리포트, 지원 종료 시점 기반 D-day 생활비 소진 예측, 실데이터 기반 생활비 챗봇 |
| <img width="100" height="100" src="https://github.com/yanh2.png?size=200" /> | 조&nbsp;하&nbsp;얀 | [@yanh2](https://github.com/yanh2) | 정부지원금 매칭 | 복지로·정부24·온통청년 공공 API 수집 배치, Gemini 기반 지원금 원문 파싱·카테고리/지역 보정, 맞춤 지원금 매칭 조회, 관심 지원금 스크랩 |

## 시스템 아키텍처

<!-- TODO: 아키텍처 다이어그램 이미지 추가 -->

프론트(React Native/Expo) ↔ 백엔드(Spring Boot, `/api/v1`) ↔ PostgreSQL(로컬 Docker / 운영 Supabase) 구조이며,
백엔드가 Gemini API와 공공 데이터 API(마이홈포털·정부24·복지로·온통청년)를 직접 호출합니다.
별도의 AI 서버 없이 백엔드 안에서 검색(RAG)과 생성 호출을 함께 처리합니다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| Frontend | React Native(Expo SDK 54), TypeScript, React Navigation, Expo Secure Store |
| Backend | Java 21, Spring Boot 3.5.16, Spring MVC, Spring Data JPA, Spring Security, Spring Validation, Spring Actuator, Spring Scheduling |
| 인증 | JJWT(JSON Web Token) 기반 자체 발급·검증, BCrypt |
| AI | Google Gemini API, 자체 구현 경량 RAG(문자 2-gram 자카드 유사도 검색 + 구조화 JSON 응답) |
| 빌드 | Gradle 9.7.1 (wrapper) |
| DB | PostgreSQL 17 — 로컬 Docker / 운영 Supabase |
| 스키마 관리 | Flyway |
| API 문서 | springdoc-openapi (Swagger UI 자동 생성) |
| External API | 국토교통부 마이홈포털, 한국사회보장정보원 복지서비스(복지로), 행정안전부 공공서비스(정부24), 온통청년 청년정책 API |
| Infra | Docker, Docker Compose, GitHub Actions(CI) |
| Collaboration |  |
| Test | JUnit 5, Spring Boot Test(`@SpringBootTest`, 실제 DB 연동), Spring Security Test |

## 프로젝트 구조

```text
backend/
├── src/main/java/com/fledge/
│   ├── common/       ApiResponse, ErrorCode
│   ├── config/        WebConfig, OpenApiConfig, SecurityConfig
│   ├── exception/     ApiException, GlobalExceptionHandler
│   ├── security/      JWT 발급·검증, 인증 필터
│   ├── member/        회원·인증·설문
│   ├── benefit/       정부지원금 매칭 · 공공 API 수집 배치
│   ├── budget/        생활비·계좌·소비 리포트·D-day 예측·생활비 챗봇
│   ├── housing/       독립지원(주거) 공고·체크리스트·자격 프로필
│   ├── care/          온라인 케어(상담·이상징후) · FAQ RAG
│   ├── habit/         금융습관 트레이닝(퀴즈·퍼즐·토픽)
│   ├── counselor/     상담사 포털(청년 관리·연계 요청)
│   └── region/        시도·시군구 참조 데이터
├── src/main/resources/
│   ├── db/migration/  Flyway 마이그레이션
│   ├── db/seed/        개발용 시드 데이터
│   └── application*.properties
├── docs/rag/          FAQ 챗봇이 검색하는 정책·서비스 문서
├── docker/            로컬 Postgres 초기화 스크립트
└── docker-compose.yml
```

### 저장소

| 구성요소 | 저장소 | 설명 |
|---|---|---|
| Backend | [backend](https://github.com/KB-ITs-your-life-HiYa/backend) | Spring Boot API 서버 |
| Frontend | [frontend](https://github.com/KB-ITs-your-life-HiYa/frontend) | React Native(Expo) 모바일 앱 |

## 로컬 실행

### 요구 사항

- JDK 21
- Docker / Docker Desktop (또는 colima)

### 실행

```bash
# 1. 로컬 DB 띄우기 (Postgres 17)
docker compose up -d

# 2. 앱 실행
./gradlew bootRun

# 3. 확인
curl http://localhost:8080/actuator/health   # -> {"status":"UP", ...}
```

정지는 `docker compose down` (데이터는 볼륨에 남습니다).

Gemini·공공 API 키가 필요한 기능(지원금 수집, 케어 상담, FAQ 챗봇 등)을 쓰려면
`src/main/resources/application-secret.properties.example`을 복사해 키를 채워야 합니다.
키가 없어도 앱 자체는 정상 기동하며, 해당 기능만 동작하지 않습니다.

```bash
cp src/main/resources/application-secret.properties.example \
   src/main/resources/application-secret.properties
```

## 주요 API

모든 API는 `/api/v1` 접두사가 붙습니다. 상세 요청/응답 스키마는 로컬 실행 후
`http://localhost:8080/swagger-ui.html`에서 확인할 수 있습니다.

| 영역 | 경로 | 설명 |
|---|---|---|
| 인증 | `/auth/login` | JWT 로그인 |
| 회원 | `/members/me`, `/members/me/survey` | 내 정보·온보딩 설문 조회/저장 |
| 지원금 | `/subsidies`, `/members/me/benefit/matches` | 지원금 목록, 맞춤 매칭 조회 |
| 관심 지원금 | `/members/me/subsidies` | 관심 지원금 저장·조회·삭제 |
| 계좌·예산 | `/members/me/accounts`, `/members/me/budget` | 계좌 요약, 월별 예산 CRUD |
| 소비 리포트 | `/members/me/expense-summary`, `/members/me/expense-report` | 소비 요약·리포트 |
| 지원 종료 예측 | `/members/me/support-end-forecast` | D-day 생활비 소진 예측 |
| 생활비 챗봇 | `/members/me/budget/chat/summary`, `/members/me/budget/chat/ask` | 요약 카드, 자유질문 응답 |
| 독립지원(주거) | `/housing/calendar`, `/housing/notices/{id}` | 공고 캘린더·상세 조회 |
| 주거 체크리스트 | `/members/me/housing/checklists` | 입주 체크리스트 CRUD |
| 주거 자격 프로필 | `/members/me/housing-eligibility` | 자격 판별용 프로필 조회·수정 |
| 온라인 케어 | `/members/me/care`, `/members/me/care/evaluate` | 케어 상태 조회·갱신 |
| 케어 상담 | `/members/me/care/signals/{id}/messages`, `/{id}/responses/{rid}/gemini` | 상담 메시지, Gemini 답변·재시도 |
| 케어 FAQ(RAG) | `/members/me/care/faq` | 지원금·독립지원·서비스 이용 자유질문 |
| 상담사 연계 | `/members/me/care/signals/{id}/referrals` | 담당 상담사 연계 요청 |
| 금융습관 | `/habit/quiz/today`, `/habit/puzzle/*`, `/habit/topics/*` | 퀴즈, 퍼즐, 금융 토픽 |
| 상담사 포털 | `/members/me/counselor/youths`, `/members/me/counselor/referrals` | 담당 청년, 연계 요청 조회 |

## 협업 방식

- 기본 브랜치는 `develop`은 안정 완성본만 유지합니다.
- 브랜치명: `<type>/<영문-설명>` (예: `feature/housing-calendar`), type은 `feature`·`fix`·`refactor`·`docs`·`chore`
- 커밋 메시지: `<type>: <한글 제목>` 형식의 한 줄 제목만 작성하고, 상세 설명은 PR 본문에 작성
- PR은 `develop`을 타겟으로 하며, 본문에 무엇을/왜/변경 사항/확인 방법을 기록
- GitHub Actions로 PR마다 빌드·테스트(CI)를 실행해 `develop` 병합 전 검증
- 그 외 협업 툴(이슈 관리, 커뮤니케이션, 디자인 등): 

> 자세한 코드 컨벤션·패키지 구조 규칙은 [`AGENTS.md`](./AGENTS.md)를 참고하세요.
