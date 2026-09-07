-- habit_quiz_answer / habit_puzzle_progress 는 member_id FK를 만들 때 ON DELETE CASCADE를
-- 빠뜨렸다(V20260902_1331__create_habit.sql). member를 참조하는 다른 모든 테이블
-- (account, budget, care, housing, member_survey 등)은 전부 CASCADE가 걸려 있어서
-- R__seed_01_member.sql의 "DELETE FROM member;"가 늘 문제없이 돌았는데, 이 둘만 예외라
-- 실제로 퀴즈를 풀거나(오늘의 퀴즈) 퍼즐을 진행한 회원이 생기면 시드를 다시 돌릴 때
-- "update or delete on table member violates foreign key constraint
-- habit_puzzle_progress_member_id_fkey" 로 막혔다. 다른 테이블과 같은 방식으로 맞춘다.
ALTER TABLE habit_quiz_answer DROP CONSTRAINT habit_quiz_answer_member_id_fkey;
ALTER TABLE habit_quiz_answer
    ADD CONSTRAINT habit_quiz_answer_member_id_fkey
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE;

ALTER TABLE habit_puzzle_progress DROP CONSTRAINT habit_puzzle_progress_member_id_fkey;
ALTER TABLE habit_puzzle_progress
    ADD CONSTRAINT habit_puzzle_progress_member_id_fkey
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE;
