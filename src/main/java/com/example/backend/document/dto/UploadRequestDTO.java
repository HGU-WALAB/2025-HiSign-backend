package com.example.backend.document.dto;

import com.example.backend.signatureRequest.DTO.SignerDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UploadRequestDTO {
    private String uniqueId;         // 업로더(작성자) UniqueId
    private String requestName;      // 작업명
    private String description;      // 작업 설명
    private Integer isRejectable;    // 반려 가능 여부
    private Integer type;            // 문서 타입 (1=검토, 2=일반)
    private String password;         // 비회원 접근용 비밀번호 (또는 NONE)
    private String memberName;       // 업로더 이름 (메일 발송 시 필요)
    private List<SignerDTO> signers; // 서명자 리스트
}