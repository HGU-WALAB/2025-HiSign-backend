package com.example.backend.auth.filter;

import com.example.backend.auth.exception.DoNotLoginException;
import com.example.backend.auth.exception.WrongTokenException;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.util.JwtUtil;
import com.example.backend.member.entity.Member;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

  private final AuthService authService;

  private final String SECRET_KEY;

  // 허용할 경로 목록 (CORS 예외 처리)
  private static final Pattern EXCLUDED_PATH_PATTERN = Pattern.compile("^(/hisign_1/api/auth/.*|/hisign_1/swagger-ui/.*|/hisign_1/v3/api-docs/.*|/hisign_1/v3/api-docs|/hisign_1/swagger-resources/.*|/hisign_1/webjars/.*|/hisign_1/swagger-ui.html)$");

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String requestURI = request.getRequestURI();
    System.out.println("Request URI: " + requestURI);

    // 특정 경로는 필터링 제외
    if (isExcludedPath(requestURI)) {
      System.out.println("Excluding from JWT filter: " + requestURI);
      filterChain.doFilter(request, response);
      return;
    }

    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null) {
      System.out.println("Authorization header missing. Blocking request.");
      throw new DoNotLoginException();
    }

    if (!authorizationHeader.startsWith("Bearer ")) {
      System.out.println("Invalid token format.");
      throw new WrongTokenException("Bearer 로 시작하지 않는 토큰입니다.");
    }

    String token = authorizationHeader.split(" ")[1];
    Member loginMember = authService.getLoginMember(JwtUtil.getUserId(token, SECRET_KEY));

    UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(
                    loginMember.getUniqueId(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(loginMember.getRole())));

    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

    System.out.println("User authenticated successfully.");
    filterChain.doFilter(request, response);
  }

  // 특정 URL이 필터링 예외 대상인지 확인하는 메서드
  private boolean isExcludedPath(String requestURI) {
    return EXCLUDED_PATH_PATTERN.matcher(requestURI).matches();
  }

}
