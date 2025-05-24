package com.example.backend.signatureRequest.DTO;

import com.example.backend.signature.DTO.SignatureDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignerDTO {
    private String email;  // 서명자의 이메일
    private String name;   // 서명자의 이름
    private List<SignatureDTO> signatureFields;  // 서명 필드 리스트
}

