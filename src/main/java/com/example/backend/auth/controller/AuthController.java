package com.example.backend.auth.controller;

import com.example.backend.auth.controller.request.LoginRequest;
import com.example.backend.auth.controller.response.LoginResponse;
import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.service.HisnetLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final HisnetLoginService hisnetLoginService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> Login(@RequestBody LoginRequest request) {
    LoginResponse ls = LoginResponse.from(authService.login(hisnetLoginService.callHisnetLoginApi(AuthDto.from(request))));
    return ResponseEntity.ok(ls);
  }
}
