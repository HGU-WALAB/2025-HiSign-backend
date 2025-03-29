package com.example.backend.auth.controller;

import com.example.backend.auth.config.CookieProperties;
import com.example.backend.auth.controller.request.LoginRequest;
import com.example.backend.auth.controller.response.LoginResponse;
import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.service.HisnetLoginService;
import com.example.backend.auth.util.CookieUtil;
import com.example.backend.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final CookieProperties cookieProperties;
  @Value("${custom.jwt.secret}")
  private String SECRET_KEY;
  private final CookieUtil cookieUtil;
  private final AuthService authService;
  private final HisnetLoginService hisnetLoginService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> Login(@RequestBody LoginRequest request) {

    AuthDto hisnetLoggedinAuthDTO = hisnetLoginService.callHisnetLoginApi(AuthDto.from(request));
    log.debug("hisnetAutMember: {}", hisnetLoggedinAuthDTO.toString());
    AuthDto LoggedinMemberAuthDTO = authService.login(hisnetLoggedinAuthDTO);
    String accessToken = LoggedinMemberAuthDTO.getToken();
    String refreshToken = JwtUtil.createRefreshToken(hisnetLoggedinAuthDTO.getUniqueId(),SECRET_KEY,cookieProperties.getRefreshTokenMaxAge());

    log.debug("✅ Generated AccessToken: {}", accessToken);
    log.debug("✅ Generated RefreshToken: {}", refreshToken);

    ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(accessToken);
    ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(refreshToken);

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
    headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    LoginResponse loginResponse = LoginResponse.from(LoggedinMemberAuthDTO);
    return ResponseEntity.ok()
            .headers(headers)
            .body(loginResponse);
  }

  @GetMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletResponse response) {
    // accessToken 쿠키 제거
    ResponseCookie expiredAccessCookie = cookieUtil.expireAccessTokenCookie();
    response.addHeader(HttpHeaders.SET_COOKIE, expiredAccessCookie.toString());

    // refreshToken 제거도 하고 싶다면 여기에 추가
    ResponseCookie expiredRefreshCookie = cookieUtil.expireRefreshTokenCookie();
    response.addHeader(HttpHeaders.SET_COOKIE, expiredRefreshCookie.toString());

    return ResponseEntity.ok().build();
  }
}
