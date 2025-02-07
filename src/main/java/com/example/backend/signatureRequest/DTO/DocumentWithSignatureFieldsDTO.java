package com.example.backend.signatureRequest.DTO;

import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.signature.DTO.SignatureDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentWithSignatureFieldsDTO {
    private Long documentId;  // 문서 정보
    private List<SignatureDTO> signatureFields;  // 서명 필드 리스트
}

