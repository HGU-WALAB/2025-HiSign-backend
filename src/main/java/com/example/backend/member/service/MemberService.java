package com.example.backend.member.service;

import com.example.backend.member.DTO.BulkInsertResultDTO;
import com.example.backend.member.DTO.MemberDTO;
import com.example.backend.member.DTO.SearchMemberDTO;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
                        convertGradeToPosition(member.getGrade(),member.getUniqueId())
                ))
                .collect(Collectors.toList());
    }

    public List<SearchMemberDTO> searchSignersByName(String query) {
        List<Member> members = memberRepository.findByNameContainingIgnoreCase(query);
        return members.stream()
                .map(member -> new SearchMemberDTO(
                        member.getName(),
                        member.getEmail(),
                        convertGradeToPosition(member.getGrade(),member.getUniqueId())
                ))
                .collect(Collectors.toList());
    }

    public List<SearchMemberDTO> searchSignersByEmail(String query) {
        List<Member> members = memberRepository.findByEmailContainingIgnoreCase(query);
        return members.stream()
                .map(member -> new SearchMemberDTO(
                        member.getName(),
                        member.getEmail(),
                        convertGradeToPosition(member.getGrade(),member.getUniqueId())
                ))
                .collect(Collectors.toList());
    }

    private String convertGradeToPosition(Integer grade, String uniqueId) {
        if (uniqueId.equals("50666") || uniqueId.equals("50654")) return "선생님";
        else if (grade == -1) return "교수님";
        return "학생";
    }

    public List<MemberDTO> getMembers() {
        List<Member> members = memberRepository.findAll();
        return members.stream()
                .map(member -> new MemberDTO(
                        member.getUniqueId(),
                        member.getName(),
                        member.getEmail(),
                        member.getActive() != null ? member.getActive() : false
                ))
                .collect(Collectors.toList());
    }

    public BulkInsertResultDTO addMembersFromString(String input) {
        String[] lines = input.split("\\r?\\n");

        int successCount = 0;
        int duplicateCount = 0;
        List<BulkInsertResultDTO.MemberResult> results = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length != 3) {
                results.add(new BulkInsertResultDTO.MemberResult("", "", "", false, "형식 오류"));
                continue;
            }

            String name = parts[0].trim();
            String uniqueId = parts[1].trim();
            String email = parts[2].trim();

            if (name.isEmpty() || uniqueId.isEmpty() || email.isEmpty()) {
                results.add(new BulkInsertResultDTO.MemberResult(name, uniqueId, email, false, "빈 필드 존재"));
                continue;
            }

            Optional<Member> existingMemberOpt = memberRepository.findByUniqueIdOrEmail(uniqueId, email);
            if (existingMemberOpt.isPresent()) {
                Member existing = existingMemberOpt.get();
                if (Boolean.FALSE.equals(existing.getActive())) {
                    existing.setActive(true);
                    memberRepository.save(existing);
                    results.add(new BulkInsertResultDTO.MemberResult(name, uniqueId, email, false, "이미 존재하여 활성화 처리됨"));
                } else {
                    results.add(new BulkInsertResultDTO.MemberResult(name, uniqueId, email, false, "이미 활성화된 회원"));
                }
                duplicateCount++;
                continue;
            }
            // 신규 회원 저장
            Member member = Member.builder()
                    .name(name)
                    .uniqueId(uniqueId)
                    .email(email)
                    .active(true)
                    .level(0)
                    .semester(0)
                    .loginTime(null) // 처음 추가 시 로그인 시간 없음
                    .build();

            memberRepository.save(member);
            successCount++;
            results.add(new BulkInsertResultDTO.MemberResult(name, uniqueId, email, true, "추가됨"));
        }

        return new BulkInsertResultDTO(
                lines.length,
                successCount,
                duplicateCount,
                results
        );
    }

    public MemberDTO updateMemberActiveStatus(String uniqueId, Boolean active) {
        Member member = (Member) memberRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다: " + uniqueId));

        member.setActive(active != null ? active : false); // null 방지

        Member saved = memberRepository.save(member);
        return new MemberDTO(saved);
    }

    public MemberDTO addMember(MemberDTO dto) {
        // 중복 검사
        boolean exists = memberRepository.existsByUniqueId(dto.getUniqueId()) ||
                memberRepository.existsByEmail(dto.getEmail());

        if (exists) {
            throw new IllegalArgumentException("이미 존재하는 회원입니다.");
        }

        Member member = Member.builder()
                .name(dto.getName())
                .uniqueId(dto.getUniqueId())
                .email(dto.getEmail())
                .active(true)
                .level(0)
                .semester(0)
                .loginTime(null)
                .build();

        Member saved = memberRepository.save(member);

        return new MemberDTO(saved);
    }
}
