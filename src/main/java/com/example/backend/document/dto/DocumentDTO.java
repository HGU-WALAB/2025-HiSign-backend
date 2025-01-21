package com.example.backend.document.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
public class DocumentDTO {

    private Long id;
    private Long memberId; // 업로드한 회원 ID
    private String fileName;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer status;
}
