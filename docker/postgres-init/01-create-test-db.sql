-- 컨테이너를 처음 만들 때(볼륨이 비어 있을 때)만 실행된다.
-- fledge_test 는 테스트 전용 DB다. 개발용 fledge 와 분리해서,
-- ./gradlew test 를 돌려도 개발 중인 데이터가 지워지지 않게 한다.
--
-- docker compose down -v 로 볼륨을 지우면 이 DB도 같이 사라지는데,
-- 그때는 이 스크립트가 다시 실행돼 자동으로 만들어진다.
CREATE DATABASE fledge_test OWNER fledge;
