package com.example.backend.auth.dto;


import com.example.backend.auth.controller.request.LoginRequest;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AuthDto {
  private String uniqueId;
  private String hisnetToken;
  private String token;
  private String name;
  private String email;

  public static AuthDto from(LoginRequest request) {
    return AuthDto.builder().hisnetToken(request.getHisnetToken()).build();
  }
}
