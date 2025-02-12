package com.example.backend.member.service;

import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member findByUniqueId(String uniqueId) {
        return (Member) memberRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with uniqueId: " + uniqueId));
    }

    public Member save(Member member) {
        return memberRepository.save(member);
    }

    public List<Member> findOrCreateMembers(List<SignerDTO> signers) {
        List<Member> members = new ArrayList<>();
        for (SignerDTO signerDto : signers) {
            Optional<Member> memberOptional = memberRepository.findByEmail(signerDto.getEmail()).map(Member.class::cast);
            Member member = memberOptional.orElseGet(() -> {
                Member newMember = Member.builder()
                        .name(signerDto.getName())
                        .email(signerDto.getEmail())
                        .build();
                return memberRepository.save(newMember);
            });
            members.add(member);
        }
        return members;
    }
}
