package com.example.backend.auth.util;

import com.example.backend.auth.exception.WrongTokenException;
import com.example.backend.member.entity.Member;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
public class JwtUtil {
  // JWT Token 발급
  public static String createToken(Member member,String secretKey,Long accessExpireTime) {
    Key key = getSigningKey(secretKey);

    return Jwts.builder()
            .setSubject(member.getUniqueId())
            .claim("email", member.getEmail())
            .claim("name", member.getName())
            .claim("role", member.getRole())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessExpireTime*1000))
            .signWith(key)
            .compact();
  }

  public static String createRefreshToken(String uniqueId,String secretKey,Long accessExpireTime) {
    Key key = getSigningKey(secretKey);

    return Jwts.builder()
            .setSubject(uniqueId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessExpireTime*1000))
            .signWith(key)
            .compact();
  }

  public static String createSignerToken(String email, Long documentId, String secretKey, long expirationSeconds) {
    Key key = JwtUtil.getSigningKey(secretKey);

    return Jwts.builder()
            .setSubject(email)
            .claim("documentId", documentId)
            .claim("role", "ROLE_SIGNER")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
            .signWith(key)
            .compact();
  }

  public static Key getSigningKey(String secretKey) {
    byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
    return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
  }

  public static String getUserId(String token, String secretKey) {
    if (token == null || token.isEmpty()) {
      log.error("토큰이 null이거나 비어있습니다");
      throw new WrongTokenException("토큰이 없습니다.");
    }
    return extractClaims(token, secretKey).getSubject();
  }

  public static Claims extractClaims(String token, String secretKey) {
    log.debug("토큰 검증 시도: {}", token.substring(0, Math.min(10, token.length())) + "...");
    try {
      Claims claims = Jwts.parserBuilder()
              .setSigningKey(getSigningKey(secretKey))
              .build()
              .parseClaimsJws(token)
              .getBody();
      log.debug("토큰 검증 성공 - subject: {}, issuedAt: {}", claims.getSubject(), claims.getIssuedAt());
      return claims;
    } catch (ExpiredJwtException e) {
      log.warn("토큰 만료됨: {}", e.getMessage());
      throw new WrongTokenException("만료된 토큰입니다.");
    } catch (JwtException e) {
      log.warn("❌ 유효하지 않은 토큰: {}", e.getMessage());
      throw new WrongTokenException("유효하지 않은 토큰입니다.");
    }
  }

}
