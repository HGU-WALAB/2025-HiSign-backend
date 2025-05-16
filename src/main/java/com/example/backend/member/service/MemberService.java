package com.example.backend.member.service;

import com.example.backend.member.DTO.SearchMemberDTO;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Member findByUniqueId(String uniqueId) {
        return (Member) memberRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with uniqueId: " + uniqueId));
    }

    public List<SearchMemberDTO> searchSignersByNameOrEmail(String query) {
        List<Member> members = memberRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
        return members.stream()
                .map(member -> new SearchMemberDTO(
                        member.getName(),
                        member.getEmail(),
                        convertGradeToPosition(member.getGrade())
                ))
                .collect(Collectors.toList());
    }

    public List<SearchMemberDTO> searchSignersByName(String query) {
        List<Member> members = memberRepository.findByNameContainingIgnoreCase(query);
        return members.stream()
                .map(member -> new SearchMemberDTO(
                        member.getName(),
                        member.getEmail(),
                        convertGradeToPosition(member.getGrade())
                ))
                .collect(Collectors.toList());
    }

    public List<SearchMemberDTO> searchSignersByEmail(String query) {
        List<Member> members = memberRepository.findByEmailContainingIgnoreCase(query);
        return members.stream()
                .map(member -> new SearchMemberDTO(
                        member.getName(),
                        member.getEmail(),
                        convertGradeToPosition(member.getGrade())
                ))
                .collect(Collectors.toList());
    }

    private String convertGradeToPosition(Integer grade) {
        if (grade == -1) return "교수님";
        if (grade == 0) return "선생님";
        return "학생";
    }
}
