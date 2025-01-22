package com.example.backend.auth.service;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.exception.DoNotExistException;
import com.example.backend.auth.util.JwtUtil;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

  private final MemberRepository memberRepository;

  @Value("${custom.jwt.secret}")
  private String SECRET_KEY;

  public Member getLoginMember(String uniqueId) {
    return (Member) memberRepository
        .findByUniqueId(uniqueId)
        .orElseThrow(() -> new DoNotExistException("해당 유저가 없습니다."));
  }

  public AuthDto login(AuthDto dto) {
    Optional<Member> member = memberRepository.findByUniqueId(dto.getUniqueId());
    if (!member.isPresent()) {
      Member newMember=Member.from(dto);
      memberRepository.save(Member.from(dto));
        return AuthDto.builder()
                .token(JwtUtil.createToken(newMember.getUniqueId(), newMember.getName(), newMember.getEmail() , SECRET_KEY))
                .build();
    }else {
      member.get().update(dto);
      return AuthDto.builder()
              .token(
                      JwtUtil.createToken(
                              member.get().getUniqueId(), member.get().getName(), member.get().getEmail() , SECRET_KEY))
              .build();
    }
  }
}
