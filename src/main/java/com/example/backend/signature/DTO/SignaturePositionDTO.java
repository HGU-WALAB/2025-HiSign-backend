package com.example.backend.signature.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignaturePositionDTO {
    private int pageNumber;  // 페이지 번호
    private float x;  // X 좌표
    private float y;  // Y 좌표
}
