package com.example.backend.signatureRequest.DTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenRequestDTO {
    private Long documentId;
    private String email;

}

