package com.example.backend.auth.util;

import com.example.backend.auth.exception.WrongTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

public class JwtUtil {
  private static final long EXPIRE_TIME_MS = 1000 * 60 * 60 * 2;

  // JWT Token 발급
  public static String createToken(String uniqueId, String name, String email , String role ,String secretKey) {
    Claims claims = Jwts.claims();
    claims.put("uniqueId", uniqueId);
    claims.put("name", name);
    claims.put("email", email);
    claims.put("role", role);

    return Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME_MS))
        .signWith(SignatureAlgorithm.HS256, secretKey)
        .compact();
  }

  public static String getUserId(String token, String secretKey) {
    return extractClaims(token, secretKey).get("uniqueId", String.class);
  }

  private static Claims extractClaims(String token, String secretKey) {
    try {
      return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    } catch (ExpiredJwtException e) {
      throw new WrongTokenException("만료된 토큰입니다.");
    } catch (Exception e) {
      throw new WrongTokenException("유효하지 않은 토큰입니다.");
    }
  }
}
