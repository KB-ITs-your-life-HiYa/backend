-- 지역코드 컬럼을 CHAR(2) 에서 VARCHAR(2) 로 바꾼다.
--
-- CHAR(n) 은 PostgreSQL 에서 쓸 이유가 없다.
-- 다른 DB 와 달리 성능 이점이 없고, 공백 패딩 때문에 저장 비용만 늘며,
-- 길이보다 짧은 값을 넣으면 뒤에 공백이 붙어 비교가 조용히 어긋난다.
-- 길이 제한은 VARCHAR(2) 로도 똑같이 걸린다.
--
-- Java 쪽에서도 CHAR 은 @JdbcTypeCode(SqlTypes.CHAR) 를 붙여야 매핑이 맞는다.
-- 지역코드는 앞으로 다른 테이블에도 들어가므로, 테이블이 늘기 전에 여기서 정리한다.
-- 앞으로 만드는 지역코드 컬럼도 VARCHAR(2) 로 쓴다.
--
-- bpchar -> varchar 변환 시 뒤쪽 공백은 제거된다. 기존 값은 모두 2자리라 영향 없다.

ALTER TABLE member ALTER COLUMN region_code      TYPE VARCHAR(2);
ALTER TABLE member ALTER COLUMN home_region_code TYPE VARCHAR(2);
