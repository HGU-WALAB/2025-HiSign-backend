package com.example.backend.auth.filter;

import com.example.backend.auth.config.CookieProperties;
import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.exception.DoNotLoginException;
import com.example.backend.auth.exception.WrongTokenException;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.util.CookieUtil;
import com.example.backend.auth.util.JwtUtil;
import com.example.backend.member.entity.Member;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

  private final AuthService authService;
  private final CookieProperties cookieProperties;
  private final CookieUtil cookieUtil;
  private final String SECRET_KEY;

  // ✅ 가독성을 위한 정적 메서드로 예외 엔드포인트를 패턴화
  private static Pattern buildExcludedPathPattern() {
    String[] excludedPaths = {
            "/api/auth/.*",
            "/api/signature-requests/check",
            "/api/signature-requests/validate",
            "/api/signature-requests/complete",
            "/api/signature-requests/reject/.*",
            "/api/signature/.*",
            "/api/documents/sign/.*",
            "/api/files/signature/upload",
            "/swagger-ui/.*",
            "/v3/api-docs/.*",
            "/v3/api-docs",
            "/swagger-resources/.*",
            "/webjars/.*",
            "/swagger-ui.html",

            //와랩 배포용
            "/hisign_1/api/auth/.*",
            "/hisign_1/api/signature-requests/check",
            "/hisign_1/api/signature-requests/validate",
            "/hisign_1/api/signature-requests/complete",
            "/hisign_1/api/signature-requests/reject/.*",
            "/hisign_1/api/signature/.*",
            "/hisign_1/api/documents/sign/.*",
            "/hisign_1/api/files/signature/upload",
            "/hisign_1/swagger-ui/.*",
            "/hisign_1/v3/api-docs/.*",
            "/hisign_1/v3/api-docs",
            "/hisign_1/swagger-resources/.*",
            "/hisign_1/webjars/.*",
            "/hisign_1/swagger-ui.html"
    };
    return Pattern.compile("^(" + String.join("|", excludedPaths) + ")$");
  }

  // ✅ 정규식 패턴 적용
  private static final Pattern EXCLUDED_PATH_PATTERN = buildExcludedPathPattern();

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    log.debug("🚀 JwtTokenFilter: 요청 URI: {}", requestURI);

    // ✅ 패턴화된 예외 엔드포인트 검사
    if (EXCLUDED_PATH_PATTERN.matcher(requestURI).matches()) {
      log.debug("🔸 JwtTokenFilter: 제외된 경로입니다. 필터 체인 계속 진행.");
      filterChain.doFilter(request, response);
      return;
    }

    Cookie[] cookies = request.getCookies();
    String accessToken = null;
    String refreshToken = null;

    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("accessToken".equals(cookie.getName())) {
          accessToken = cookie.getValue();
        }
        if ("refreshToken".equals(cookie.getName())) {
          refreshToken = cookie.getValue();
        }
      }
    }

    try {
      log.debug("🛡️ 액세스 토큰 검증 중...");
      Member loginMember = authService.getLoginMember(JwtUtil.getUserId(accessToken, SECRET_KEY));
      setUserPasswordAuthenticationToken(request, loginMember);
    } catch (WrongTokenException e) {
      if(refreshToken != null) {
        try {
          log.debug("🛡️ 리프레시 토큰 검증 중...");
          Member loginMember = authService.getLoginMember(JwtUtil.getUserId(refreshToken, SECRET_KEY));
          String newAccessToken = JwtUtil.createToken(loginMember,SECRET_KEY,cookieProperties.getAccessTokenMaxAge());
          ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(newAccessToken);
          response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

          log.info("🔄 사용자 {} 액세스 토큰 리프레시 성공", loginMember.getName());

          setUserPasswordAuthenticationToken(request, loginMember);
        } catch (Exception refreshEx) {
        // 더 상세한 로깅을 포함한 개선된 예외 처리
        log.error("❌ 토큰 리프레시 실패: {}", refreshEx.getMessage());
        throw new DoNotLoginException();
      }
      } else {
        log.error("❌ refreshToken이 존재하지 않습니다. 로그인이 필요합니다.");
        throw new DoNotLoginException();
      }
    }
    System.out.println("User authenticated successfully.");
    filterChain.doFilter(request, response);
  }

  private void setUserPasswordAuthenticationToken(HttpServletRequest request, Member loginMember) {
    UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(
                    AuthDto.builder()
                            .uniqueId(loginMember.getUniqueId())
                            .name(loginMember.getName())
                            .email(loginMember.getEmail())
                            .level(loginMember.getLevel())
                            .build(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(loginMember.getRole())));

    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
  }

}
