package com.example.backend.member.repository;

import com.example.backend.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    <T> Optional<T> findByUniqueId(String uniqueId);
}
