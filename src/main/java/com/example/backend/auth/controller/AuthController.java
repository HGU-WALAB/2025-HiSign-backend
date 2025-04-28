package com.example.backend.auth.controller;

import com.example.backend.auth.config.CookieProperties;
import com.example.backend.auth.controller.request.LoginRequest;
import com.example.backend.auth.controller.request.SignatureValidationRequest;
import com.example.backend.auth.controller.response.LoginResponse;
import com.example.backend.auth.dto.AuthDto;
import com.example.backend.auth.service.AuthService;
import com.example.backend.auth.service.HisnetLoginService;
import com.example.backend.auth.util.CookieUtil;
import com.example.backend.auth.util.JwtUtil;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.backend.auth.util.EncryptionUtil;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  @Value("${custom.jwt.secret}")
  private String SECRET_KEY;
  private final CookieProperties cookieProperties;
  private final CookieUtil cookieUtil;
  private final EncryptionUtil encryptionUtil;
  private final AuthService authService;
  private final HisnetLoginService hisnetLoginService;
  private final SignatureRequestRepository signatureRequestRepository;
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

  @PostMapping("/signer/validate")
  public ResponseEntity<?> validateAndIssueToken(@RequestBody SignatureValidationRequest request) {
    try {
      String decryptedToken = encryptionUtil.decryptUUID(request.getToken());

      SignatureRequest signatureRequest = signatureRequestRepository.findByToken(decryptedToken)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "잘못된 서명 요청입니다."));

      if (!signatureRequest.getPassword().equals(request.getPassword())) {
        log.debug("저장된 비밀번호: {}",signatureRequest.getPassword());
        log.debug("전달받은 비밀번호: {}",request.getPassword());
        return ResponseEntity.status(401).body("비밀번호가 일치하지 않습니다.");
      }

      if (signatureRequest.getExpiredAt().isBefore(LocalDateTime.now())) {
        return ResponseEntity.status(410).body("서명 요청이 만료되었습니다.");
      }

      String jwt = JwtUtil.createSignerToken(
              signatureRequest.getSignerEmail(),
              signatureRequest.getDocument().getId(),
              SECRET_KEY,
              cookieProperties.getSignerTokenMaxAge()
      );

      ResponseCookie signerCookie = cookieUtil.createSignerTokenCookie(jwt);

      Map<String, Object> response = new HashMap<>();
      response.put("documentId", signatureRequest.getDocument().getId());
      response.put("documentName", signatureRequest.getDocument().getFileName());
      response.put("signerName", signatureRequest.getSignerName());

      return ResponseEntity.ok()
              .header(HttpHeaders.SET_COOKIE, signerCookie.toString())
              .body(response);

    } catch (Exception e) {
      return ResponseEntity.status(500).body("서명 인증 중 오류가 발생했습니다.");
    }
  }
}
