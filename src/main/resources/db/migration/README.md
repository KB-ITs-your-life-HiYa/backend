# 마이그레이션 작성 규칙

DB 스키마는 **여기 있는 SQL 파일로만** 바꾼다. 대시보드나 콘솔에서 직접
`CREATE TABLE` 하지 않는다. 그렇게 하면 내 DB 와 남의 DB 가 달라진다.

앱이 기동될 때 Flyway 가 이 폴더의 파일을 **파일명 순서대로** 적용하고,
어디까지 적용했는지를 `flyway_schema_history` 테이블에 기록한다.
로컬 도커든 Supabase 든 같은 파일을 적용하므로 스키마가 항상 같아진다.

## 파일명

```
V<YYYYMMDD>_<HHmm>__<영문설명>.sql
```

밑줄 **두 개**(`__`)가 버전과 설명을 나눈다. 하나면 인식되지 않는다.

```
V20260901_1430__create_care_signal.sql
V20260902_0910__add_index_to_housing_notice.sql
```

날짜·시각을 버전으로 쓰는 이유는 **팀원끼리 번호가 겹치지 않게** 하기 위함이다.
`V1`, `V2` 로 매기면 두 사람이 동시에 `V3` 을 만들어 충돌한다.

## 절대 하지 말 것

**이미 올라간 `V` 파일은 수정하지 않는다.** 남이 만든 것도 마찬가지다.
Flyway 는 적용할 때 파일 내용의 체크섬을 저장해두기 때문에, 내용이 바뀌면
이미 적용한 팀원의 DB 에서 기동이 실패한다.

잘못 만들었으면 **되돌리는 새 파일을 추가**한다.

## 명령어

```bash
./gradlew bootRun    # 기동하면서 자동 적용
```

적용 상태는 DB 의 `flyway_schema_history` 테이블에서 확인한다.

```sql
SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;
```

## MySQL 과 다른 점

MySQL 만 써보셨다면 이 정도만 알면 된다.
JPA 를 쓰므로 엔티티·리포지토리·서비스 코드는 DB 와 무관하게 동일하다.
차이를 느끼는 건 **여기 SQL 파일을 쓸 때뿐**이다.

| | MySQL | PostgreSQL |
| --- | --- | --- |
| 자동증가 PK | `BIGINT AUTO_INCREMENT` | `BIGSERIAL` |
| 참/거짓 | `TINYINT(1)` | `BOOLEAN` (진짜 불리언) |
| 시각 | `DATETIME` | `TIMESTAMPTZ` (시간대 포함) |
| 현재시각 기본값 | `DEFAULT CURRENT_TIMESTAMP` | `DEFAULT now()` |
| 이름 감싸기 | `` `backtick` `` | `"큰따옴표"` |
| 문자열 잇기 | `CONCAT(a, b)` | `a \|\| b` |
| 상위 N건 | `LIMIT 10` | 같음 |

`VARCHAR`, `INT`, `DATE`, `ALTER TABLE ... ADD COLUMN` 등은 양쪽이 같다.
실제로 앞으로 쓸 일이 많은 것은 대부분 같은 문법이다.

DB 내용을 볼 때는 IntelliJ 우측 **Database** 탭을 쓰면 MySQL 과 화면이 같다.

## 설계 근거

왜 그렇게 만들었는지는 [SCHEMA.md](./SCHEMA.md) 에 있다.
"이 제약은 왜 있지" 싶으면 거기를 먼저 본다.
