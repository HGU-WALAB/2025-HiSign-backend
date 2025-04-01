package com.example.backend.auth.filter;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.util.SignerJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
public class SignerTokenFilter extends OncePerRequestFilter {

    private final String secretKey;

    public SignerTokenFilter(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 💡 Authorization 헤더가 없으면 비회원 인증 필터는 스킵
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            Claims claims = SignerJwtUtil.parseToken(jwt, secretKey);

            // 필수 정보 검증
            if (!"ROLE_SIGNER".equals(claims.get("role"))) {
                throw new RuntimeException("잘못된 비회원 토큰입니다.");
            }

            AuthDto signer = AuthDto.builder()
                    .email(claims.getSubject())
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(signer, null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_SIGNER")));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("비회원 토큰 인증 실패: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

}