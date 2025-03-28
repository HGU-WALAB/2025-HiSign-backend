package com.example.backend.auth.util;

import com.example.backend.auth.config.CookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * accessToken 및 refreshToken 쿠키를 생성하는 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final CookieProperties properties;

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path("/")
                .maxAge(properties.getAccessTokenMaxAge())
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path("/")
                .maxAge(properties.getRefreshTokenMaxAge())
                .build();
    }

    public ResponseCookie expireAccessTokenCookie() {
        return ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }
}

