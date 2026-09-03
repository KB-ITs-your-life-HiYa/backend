-- subsidy.target_household 를 member_survey_tag 와 같은 영문 코드 어휘로 통일한다.
-- Gemini 파싱 초기에는 한글 텍스트로 뽑았는데, member_survey_tag 쪽 CHECK 제약이
-- 영문 코드(SINGLE_PARENT 등)라 매칭 조인이 안 됐다. 이후 파싱부터는 enum 제약으로
-- 영문 코드만 나오게 고쳤고(SubsidyParsingRunner), 이미 저장된 기존 데이터도
-- 같은 어휘로 맞춘다.
--
-- 소득(저소득)과 자립준비청년은 태그가 아니라 income_pct_max, member 쪽 protection_status
-- 로 이미 다뤄지므로 태그 목록에서 제거한다.

UPDATE subsidy SET target_household = array_replace(target_household, '다문화', 'MULTICULTURAL');
UPDATE subsidy SET target_household = array_replace(target_household, '장애인', 'DISABILITY');
UPDATE subsidy SET target_household = array_replace(target_household, '한부모', 'SINGLE_PARENT');
UPDATE subsidy SET target_household = array_replace(target_household, '다자녀', 'MULTI_CHILD');
UPDATE subsidy SET target_household = array_remove(target_household, '저소득');
UPDATE subsidy SET target_household = array_remove(target_household, '자립준비청년');