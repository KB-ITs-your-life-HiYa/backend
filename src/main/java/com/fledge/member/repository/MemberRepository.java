package com.fledge.member.repository;

import com.fledge.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    // 회원별 케어 상태 변경을 직렬화하여 날짜 전환/버튼/탐지가 서로 덮어쓰지 않게 한다.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from Member m where m.id = :id")
    Optional<Member> lockForCare(@org.springframework.data.repository.query.Param("id") Long id);
}
