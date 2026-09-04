-- member.region_sigungu(텍스트) 를 sigungu 코드로 바꾼다.
--
-- 왜: subsidy_region.sigungu_code 는 5자리 코드를 쓰는데 member 쪽은 이름 텍스트라
-- 지역 기반 조인이 안 됐다(#19 리뷰에서 논의). region_code(시/도) 는 이미 코드였으니
-- 시/군/구도 같은 방식으로 맞춘다.
--
-- 이름 표기가 PR #19 v2 에서 실제 데이터(마이홈포털·회원 시드) 기준으로 정리됐으므로
-- 이제 이름으로 코드를 안전하게 찾을 수 있다.

ALTER TABLE member ADD COLUMN region_sigungu_code VARCHAR(5) REFERENCES sigungu (code);

UPDATE member m
   SET region_sigungu_code = g.code
  FROM sigungu g
 WHERE g.name = m.region_sigungu
   AND g.sido_code = m.region_code;

-- 이름만으로 찾으면 '중구'처럼 여러 시/도에 중복되는 이름 때문에 잘못 매칭될 수 있어
-- sido_code 를 반드시 같이 건다(SCHEMA.md 3번 항목과 같은 이유).
--
-- 백필이 하나라도 안 됐으면 여기서 멈춘다. 조용히 NULL 로 넘어가면
-- "왜 이 회원만 지역 필터에 안 걸리지" 를 나중에 원인 모르고 겪는다.
DO $$
DECLARE
    unmatched INT;
BEGIN
    SELECT count(*) INTO unmatched
      FROM member
     WHERE region_sigungu IS NOT NULL
       AND region_sigungu_code IS NULL;

    IF unmatched > 0 THEN
        RAISE EXCEPTION
            '% 명의 회원이 region_sigungu 코드로 변환되지 않았습니다. sigungu 테이블의 이름 표기를 확인하세요.',
            unmatched;
    END IF;
END $$;

ALTER TABLE member DROP COLUMN region_sigungu;

COMMENT ON COLUMN member.region_sigungu_code IS '거주 시/군/구 코드. sigungu 테이블 참조';
