package com.example.backend.auth.controller.response;

import com.example.backend.auth.dto.AuthDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
  private String token;

  public static LoginResponse from(AuthDto authDto) {
    return LoginResponse.builder().token(authDto.getToken()).build();
  }
}
