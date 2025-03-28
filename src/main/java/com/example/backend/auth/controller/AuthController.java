package com.example.backend.auth.controller;

import com.example.backend.auth.controller.request.LoginRequest;
import com.example.backend.auth.controller.response.LoginResponse;
import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.service.HisnetLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final HisnetLoginService hisnetLoginService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> Login(@RequestBody LoginRequest request) {

    LoginResponse loginResponse = LoginResponse.from(authService.login(hisnetLoginService.callHisnetLoginApi(AuthDto.from(request))));
    String accessToken = loginResponse.getToken();

    // ✅ 쿠키 설정
    ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
            .path("/")
            .maxAge(7200)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .build();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
    //headers.add(HttpHeaders.SET_COOKIE, "refreshToken=" + refreshToken + "; HttpOnly; Path=/; Max-Age=604800; SameSite=Lax;");

    return ResponseEntity.ok()
            .headers(headers)
            .body(loginResponse);
  }

  @GetMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletResponse response) {
    Cookie accessCookie = new Cookie("accessToken", "");
    accessCookie.setHttpOnly(true);
    accessCookie.setSecure(true);
    accessCookie.setPath("/");
    accessCookie.setMaxAge(0); // 쿠키 삭제

//    Cookie refreshCookie = new Cookie("refreshToken", "");
//    refreshCookie.setHttpOnly(true);
//    refreshCookie.setSecure(false);
//    refreshCookie.setPath("/");
//    refreshCookie.setMaxAge(0); // 쿠키 삭제

    response.addCookie(accessCookie);
//    response.addCookie(refreshCookie);
    return ResponseEntity.ok().build();
  }
}
