package com.example.backend.document.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RequestedDocumentWithEncryptedTokenDTO {
    private Long documentId;
    private String fileName;
    private String createdAt;
    private Integer status;
    private String requestName;
    private String expiredAt;
    private String encryptedToken;
}
