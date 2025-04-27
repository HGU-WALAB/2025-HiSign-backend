package com.example.backend.auth.filter;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
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

        Cookie[] cookies = request.getCookies();
        String signerToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("signerToken".equals(cookie.getName())) {
                    signerToken = cookie.getValue();
                    break;
                }
            }
        }

        // ✅ signerToken이 없으면 이 필터는 아무것도 하지 않고 다음 필터로 넘긴다
        if (signerToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = JwtUtil.extractClaims(signerToken, secretKey);

            if (!"ROLE_SIGNER".equals(claims.get("role"))) {
                throw new RuntimeException("잘못된 서명자 토큰입니다.");
            }

            AuthDto signer = AuthDto.builder()
                    .email(claims.getSubject())
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            signer,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_SIGNER"))
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.warn("❌ SignerToken 인증 실패: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
