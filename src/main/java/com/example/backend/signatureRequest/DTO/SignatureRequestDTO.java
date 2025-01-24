package com.example.backend.signatureRequest.DTO;

import com.example.backend.signature.DTO.SignatureDTO;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SignatureRequestDTO {
    private Long documentId;  // 문서 ID
    private List<SignerDTO> signers;  // 서명자 리스트
    private List<SignatureDTO> signatureFields;  // 서명 필드 리스트
}

