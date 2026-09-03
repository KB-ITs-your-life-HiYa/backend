# 스키마 설계 노트

`table-spec` 문서를 SQL 로 옮기면서 판단한 것들을 남긴다.
**"왜 이렇게 만들었나" 를 여기서 찾을 수 있게** 하는 것이 목적이다.

테이블 상세(어떤 컬럼이 무엇을 담는지)는 각 마이그레이션 파일의 주석에 있다.

---

## 1. 상태 값은 enum 타입이 아니라 `VARCHAR + CHECK`

설계 문서에 타입이 `enum` 으로 적힌 것들(`account_type`, `status`, `direction` 등)은
전부 `VARCHAR(n)` 에 `CHECK` 제약을 건 형태로 만들었다.

Postgres 의 `CREATE TYPE ... AS ENUM` 은 **값 하나를 추가하는 데도 타입 자체를 바꿔야 하고,
값 삭제와 순서 변경이 사실상 불가능하다.** 스펙이 계속 바뀌는 동안에는 부담이 크다.

`CHECK` 는 제약만 갈아끼우면 된다.

```sql
ALTER TABLE account DROP CONSTRAINT ck_account_type;
ALTER TABLE account ADD  CONSTRAINT ck_account_type
    CHECK (account_type IN ('DEPOSIT', 'SAVINGS', 'CHECKING'));
```

Java 쪽은 어느 쪽이든 똑같다. `@Enumerated(EnumType.STRING)` 으로 매핑하면
컴파일 타임 타입 안전성은 그대로 얻는다. `member` 테이블이 이미 이 방식이다.

---

## 2. 회원이 어긋나는 것을 DB 가 막는다

여러 테이블이 `member_id` 를 중복해서 갖고 있다. 조회 편의 때문인데,
**각 FK 를 따로 걸면 조합이 엉터리여도 통과한다.**

```sql
-- account 10 은 회원 1 의 것인데 거래는 회원 2 로 넣어도 들어간다
INSERT INTO transaction (member_id, account_id, ...) VALUES (2, 10, ...);
```

결과가 심각하다.

| 어긋나면 | 무슨 일이 | 심각도 |
| --- | --- | --- |
| `transaction` ↔ `account` | 남의 거래가 내 지출 합계에 섞인다 | 예산 숫자가 틀림 |
| `money_cycle` ↔ `money_schedule` | 남의 납부 상태가 내 것으로 집계된다 | 미납 판정이 틀림 |
| `care_signal` ↔ `money_cycle` | **남의 이상징후가 내 홈 화면에 뜬다** | 개인정보 노출 |
| `referral_request` ↔ `care_signal` | 담당자에게 엉뚱한 회원 사연이 간다 | 개인정보 노출 |

그래서 **두 컬럼을 묶어서 참조**한다. 참조당하는 쪽에 `UNIQUE (id, member_id)` 를 두고,
참조하는 쪽이 그 조합을 가리키게 한다.

```sql
-- 참조당하는 쪽
CONSTRAINT uq_account_id_member UNIQUE (id, member_id)

-- 참조하는 쪽
CONSTRAINT fk_transaction_account_member
    FOREIGN KEY (account_id, member_id) REFERENCES account (id, member_id)
```

`id` 가 이미 PK 라 이 `UNIQUE` 는 중복 방지가 목적이 아니다.
**합성 FK 가 가리킬 대상을 만드는 것**이 목적이다.

연결은 이렇게 이어진다.

```
member
  └ account ──────────── transaction        (account_id, member_id)
  └ money_schedule ───── money_cycle        (schedule_id, member_id)
                            └ care_signal   (money_cycle_id, member_id)
                                 └ referral_request  (care_signal_id, member_id)
```

`money_cycle.member_id` 는 이 검증을 위해 추가한 컬럼이다. 설계 문서에는 없었다.

---

## 3. 부분 유니크 인덱스로 중복 신호를 막는다

같은 사이클로 **열려 있는** 신호는 하나뿐이어야 한다.

```sql
CREATE UNIQUE INDEX uq_care_signal_open
    ON care_signal (money_cycle_id) WHERE status = 'OPEN';
```

없으면 탐지 배치가 돌 때마다 같은 신호가 쌓이고 **사용자가 매일 같은 말을 듣는다.**

`WHERE` 절이 핵심이다. 그냥 `UNIQUE (money_cycle_id)` 로 걸면
**해결된 뒤에 같은 문제가 다시 생겨도 신호를 못 켠다.**

`referral_request` 도 같은 패턴이다 — 진행 중(`REQUESTED`/`CONTACTED`)인 연계만 하나로 막는다.

> 이 기능은 MySQL 에 없다. 애플리케이션 코드로 막으면 배치와 사용자 요청이
> 동시에 들어올 때 확인과 삽입 사이에 끼어들어 중복이 생긴다.

---

## 4. 앞뒤가 안 맞는 데이터를 막는 CHECK

단순히 값 목록만 검사하지 않고, **컬럼 사이의 관계**도 건다.

| 테이블 | 제약 | 없으면 |
| --- | --- | --- |
| `money_schedule` | `direction`+`type` 조합 검사 | `IN` 인데 `RENT` 같은 조합이 생김 |
| `money_schedule` | `OUT` 이면 금액 필수 | 미납 판정을 할 수 없음 |
| `money_cycle` | `DONE` 이면 근거 거래 필수 | 냈다고 표시됐는데 지출 합계에 안 잡힘 |
| `care_response` | `BUTTON`↔`selected_value`, `FREE_TEXT`↔`input_text` | 빈 대화 줄이 쌓임 |
| `care_signal` | `RESOLVED` 면 해결 시각 필수 | 언제 끝났는지 알 수 없음 |
| `member_survey` | 고용형태는 재직중일 때만 | 학생인데 정규직 |
| `monthly_budget`·`money_cycle` | 월 컬럼은 반드시 1일 | 월별 조회가 어긋남 |
| `housing_notice` | 접수 시작 ≤ 마감 | 캘린더가 깨짐 |
| `transaction` | 금액 > 0 | 방향이 두 곳에서 정해져 합계가 어긋남 |

---

## 5. 삭제 규칙

대부분 `ON DELETE CASCADE` 다. 부모가 사라지면 자식도 의미가 없기 때문이다.

**예외 두 개**

```sql
money_cycle.matched_transaction_id  → ON DELETE SET NULL
referral_request.counselor_id       → ON DELETE SET NULL
```

거래가 지워져도 "9월에 냈었다" 는 기록은 남겨야 한다. 같이 지우면
**"올해 몇 번 놓쳤나" 를 셀 때 숫자가 틀린다.** 그게 위험도 판단의 근거다.
담당자 계정이 지워져도 연계 요청 이력은 남긴다.

---

## 6. 인덱스는 실제 쿼리를 보고 넣었다

| 인덱스 | 받쳐주는 쿼리 |
| --- | --- |
| `transaction (member_id, txn_date)` | 예산 화면의 월 지출 합계 |
| `money_cycle (status, expected_date)` | 매일 도는 배치의 "예정일 지난 PENDING" |
| `care_signal (member_id, status)` | 홈 화면의 "내 열린 신호" |
| `care_signal (recheck_at) WHERE OPEN` | 7일 뒤 재확인 배치 |
| `housing_notice (end_de, begin_de) WHERE NOT superseded` | 캘린더의 이번 달 공고 |
| `housing_notice_unit (brtc_nm)` | 지역 필터 |

FK 컬럼 중 상당수는 `UNIQUE` 제약이 앞자리를 덮고 있어 별도 인덱스를 두지 않았다.
(`UNIQUE (member_id, budget_month)` 가 `member_id` 단독 조회도 받아준다)

---

## 7. 이 PR 에서 만들지 않은 것

| 테이블 | 이유                                        |
| --- |-------------------------------------------|
| `member_subsidy` | `subsidy` 구조가 설계 문서와 달라 정리 후에 만든다         |
| `subsidy_target_tag` | 실제 `subsidy` 는 `target_household TEXT[]` 로 구현되어 있다 |
| `habit` 도메인 | 내가 못찾음..                                  |

---

## 8. 팀 확인이 필요한 값 목록

설계 문서에 한글로만 있던 것을 영문 코드로 옮겼다.
**화면과 API 에 그대로 나가는 값**이라 바꾸려면 지금이 바꿔야한다. 

```
member_survey.employment_status
  EMPLOYED 재직중 · SEEKING 구직중 · STUDENT 학생 · UNEMPLOYED 무직 · SELF_EMPLOYED 자영업

member_survey.employment_type      ← 문서에 값 목록이 없어 새로 정함
  FULL_TIME 정규직 · CONTRACT 계약직 · PART_TIME 아르바이트 · DAILY 일용직

member_survey.housing_type
  OWNED 자가 · JEONSE 전세 · MONTHLY_RENT 월세 · FREE 무상거주
  SELF_RELIANCE_HOUSE 자립생활관 · PUBLIC_RENTAL 공공임대

transaction.category
  HOUSING_UTILITY 주거공과금 · FOOD 식비 · TRANSPORT 교통
  LIVING_MEDICAL 생활의료 · LEISURE_SHOPPING 여가쇼핑 · SAVINGS 저축 · ETC 기타

  ※ money_schedule.type 의 저축은 'SAVING'(단수), 여기와 account_type 은 'SAVINGS'(복수)다.
    표기가 갈리므로 값을 쓸 때 어느 테이블인지 확인할 것.
```
