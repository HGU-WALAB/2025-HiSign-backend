package com.example.backend.member.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkInsertResultDTO {
    private int totalCount;
    private int successCount;
    private int duplicateCount;
    private List<MemberResult> results;

    @Data
    @AllArgsConstructor
    public static class MemberResult {
        private String name;
        private String uniqueId;
        private String email;
        private boolean success;
        private String message; // 예: "추가됨" / "중복된 uniqueId" / "형식 오류"
    }
}

