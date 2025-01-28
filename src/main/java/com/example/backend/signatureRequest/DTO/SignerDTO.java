package com.example.backend.signatureRequest.DTO;

import com.example.backend.signature.DTO.SignatureDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SignerDTO {
    private String email;  // 서명자의 이메일
    private String name;   // 서명자의 이름
    private List<SignatureDTO> signatureFields;  // 서명 필드 리스트
}

