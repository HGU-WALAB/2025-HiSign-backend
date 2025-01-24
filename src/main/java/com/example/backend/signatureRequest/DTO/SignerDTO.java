package com.example.backend.signatureRequest.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignerDTO {
    private String email;  // 서명자의 이메일
    private String name;   // 서명자의 이름
}

