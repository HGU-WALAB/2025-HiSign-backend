package com.example.backend.signatureRequest.DTO;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SignatureRequestDTO {
    private Long documentId;  // 문서 ID
    private String memberName; //요청자 이름
    private List<SignerDTO> signers;  // 서명자 리스트
    private String password;
}

