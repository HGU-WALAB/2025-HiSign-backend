package com.example.backend.signature.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignatureDTO {
    private String signerEmail;  // 서명자의 이메일
    private int type;  // 서명 타입 (예: 0=서명, 1=텍스트)
    private int width;  // 서명 박스 너비
    private int height; // 서명 박스 높이
    private SignaturePositionDTO position;  // 서명 위치
}

