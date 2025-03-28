package com.example.backend.auth.service;

import com.example.backend.auth.config.CookieProperties;
import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.exception.DoNotExistException;
import com.example.backend.auth.util.JwtUtil;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

  @Value("${custom.jwt.secret}")
  private String SECRET_KEY;
  private final CookieProperties cookieProperties;
  private final MemberRepository memberRepository;

  public Member getLoginMember(String uniqueId) {
    return (Member) memberRepository
        .findByUniqueId(uniqueId)
        .orElseThrow(() -> new DoNotExistException("해당 유저가 없습니다."));
  }

  public AuthDto login(AuthDto dto) {
    Optional<Member> member = memberRepository.findByUniqueId(dto.getUniqueId());
    if (!member.isPresent()) {
      Member newMember=Member.from(dto);
      memberRepository.save(newMember);
        return AuthDto.builder()
                .token(
                        JwtUtil.createToken(newMember,SECRET_KEY,cookieProperties.getAccessTokenMaxAge()))
                .build();
    }else {
      member.get().update(dto);
      return AuthDto.builder()
              .token(
                      JwtUtil.createToken(member.get(),SECRET_KEY,cookieProperties.getAccessTokenMaxAge()))
              .build();
    }
  }
}
