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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

  private final AuthService authService;

  private final String SECRET_KEY;

  // 허용할 경로 목록 (CORS 예외 처리)
  private static final List<String> EXCLUDED_PATHS = Arrays.asList(
          "/api/auth/.*"
  );

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String requestURI = request.getRequestURI();

    // 특정 경로는 필터링 제외
    if (isExcludedPath(requestURI)) {
      filterChain.doFilter(request, response);
      return;
    }
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    // Header의 Authorization 값이 비어있으면 => Jwt Token을 전송하지 않음 => 로그인 하지 않음
    if (authorizationHeader == null) throw new DoNotLoginException();

    // Header의 Authorization 값이 'Bearer '로 시작하지 않으면 => 잘못된 토큰
    if (!authorizationHeader.startsWith("Bearer "))
      throw new WrongTokenException("Bearer 로 시작하지 않는 토큰입니다.");

    // 전송받은 값에서 'Bearer ' 뒷부분(Jwt Token) 추출
    String token = authorizationHeader.split(" ")[1];

    Member loginMember = authService.getLoginMember(JwtUtil.getUserId(token, SECRET_KEY));

    // loginUser 정보로 UsernamePasswordAuthenticationToken 발급
    UsernamePasswordAuthenticationToken authenticationToken =
        new UsernamePasswordAuthenticationToken(
                loginMember.getUniqueId(),
            null,
            null);
            //현재는 관리자와 사용자를 구분 하지 않고 있음.
            //이건 자바 11버전
            //List.of(new SimpleGrantedAuthority(loginMember.getName())));
            //이건 자바 8버전
            //Collections.singletonList(new SimpleGrantedAuthority(loginMember.getName())));

    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    // 권한 부여
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    filterChain.doFilter(request, response);
  }
  // 특정 URL이 필터링 예외 대상인지 확인하는 메서드
  private boolean isExcludedPath(String requestURI) {
    return EXCLUDED_PATHS.stream().anyMatch(requestURI::matches);
  }
}
