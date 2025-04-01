package com.example.backend.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.Key;
import java.util.Date;

public class SignerJwtUtil {
    public static String createToken(String email, Long documentId, String secretKey, long expirationSeconds) {
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

    public static Claims parseToken(String token, String secretKey) {
        return Jwts.parserBuilder()
                .setSigningKey(JwtUtil.getSigningKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}