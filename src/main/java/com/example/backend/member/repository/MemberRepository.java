package com.example.backend.member.repository;

import com.example.backend.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    <T> Optional<T> findByUniqueId(String uniqueId);

    List<Member> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
    List<Member> findByNameContainingIgnoreCase(String name);
    List<Member> findByEmailContainingIgnoreCase(String email);

    // 📌 특정 문서의 uniqueId를 기반으로 멤버 이름 조회
    @Query("SELECT m.name FROM Member m WHERE m.uniqueId = :uniqueId")
    String findMemberNameByUniqueId(@Param("uniqueId") String uniqueId);

    @Query("SELECT m.uniqueId FROM Member m WHERE m.email = :email")
    String findUniqueIdByEmail(@Param("email") String email);

    Optional<Member> findByUniqueIdOrEmail(String uniqueId, String email);

    @Query("SELECT m.email FROM Member m WHERE m.uniqueId = :uniqueId")
    Optional<String> findEmailByUniqueId(@Param("uniqueId") String uniqueId);

    boolean existsByUniqueId(String uniqueId);

    boolean existsByEmail(String email);
}
