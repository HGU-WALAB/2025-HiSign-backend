package com.example.backend.signatureRequest.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignatureRequestMailDTO {
    private Long documentId;
    private String memberName;
    private String password;
}
