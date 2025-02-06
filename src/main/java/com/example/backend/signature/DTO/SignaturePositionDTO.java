package com.example.backend.signature.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignaturePositionDTO {
    private int pageNumber;  // 페이지 번호
    private float x;  // X 좌표
    private float y;  // Y 좌표
}
