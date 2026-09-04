-- subsidy_region 3건이 LLM 파싱 과정에서 지역코드가 아니라 소관기관코드(institution code)를
-- 잘못 넣은 채로 들어가 있었다. 원본 raw_payload의 소관기관명을 직접 확인해서 바로잡는다.
--
--   subsidy_id 18: 원문 "소관기관명": "충청남도 예산군" (시군구 단위) → 44 / 44810
--   subsidy_id 20: 원문 "소관기관명": "인천광역시" (광역시도 단위)   → 28 / NULL(시도 전체)
--   subsidy_id 21: 원문 "소관기관명": "전남광주통합특별시" (광역시도 단위) → 12 / NULL(시도 전체)

UPDATE subsidy_region SET sido_code = '44', sigungu_code = '44810' WHERE subsidy_id = 18;
UPDATE subsidy_region SET sido_code = '28', sigungu_code = NULL WHERE subsidy_id = 20;
UPDATE subsidy_region SET sido_code = '12', sigungu_code = NULL WHERE subsidy_id = 21;