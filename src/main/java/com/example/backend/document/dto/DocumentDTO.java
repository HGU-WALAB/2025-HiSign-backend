package com.example.backend.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class DocumentDTO {

    private Long id;
    private Long memberId; // 업로드한 회원 ID
    private String fileName;
    private String savedFileName;
    private String requestName;

    // 날짜 포맷 설정
    @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
    private LocalDateTime updatedAt;

    private Integer status;
}
