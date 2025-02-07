package com.example.backend.auth.filter;

import com.example.backend.auth.dto.AuthDto;
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

  // ✅ 가독성을 위한 정적 메서드로 예외 엔드포인트를 패턴화
  private static Pattern buildExcludedPathPattern() {
    String[] excludedPaths = {
            "/api/auth/.*",
            "/api/signature-requests/check",
            "/api/signature-requests/validate",
            "/api/signature/.*",
            "/api/documents/sign/.*",
            "/swagger-ui/.*",
            "/v3/api-docs/.*",
            "/v3/api-docs",
            "/swagger-resources/.*",
            "/webjars/.*",
            "/swagger-ui.html",

            //와랩 배포용
//            "/hisign_1/api/auth/.*",
//            "/hisign_1/swagger-ui/.*",
//            "/hisign_1/v3/api-docs/.*",
//            "/hisign_1/v3/api-docs",
//            "/hisign_1/swagger-resources/.*",
//            "/hisign_1/webjars/.*",
//            "/hisign_1/swagger-ui.html"
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

    // ✅ 패턴화된 예외 엔드포인트 검사
    if (EXCLUDED_PATH_PATTERN.matcher(requestURI).matches()) {
      System.out.println("Request URI: " + requestURI);
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
                    AuthDto.builder()
                            .uniqueId(loginMember.getUniqueId())
                            .email(loginMember.getEmail())
                            .build(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(loginMember.getRole())));

    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

    System.out.println("User authenticated successfully.");
    filterChain.doFilter(request, response);
  }

}
