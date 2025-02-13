package com.example.backend.file.controller;

import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
import com.example.backend.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final DocumentService documentService;
    private final MemberService memberService;

    public FileController(
            FileService fileService,
            DocumentService documentService,
            MemberService memberService) {
        this.fileService = fileService;
        this.documentService = documentService;
        this.memberService = memberService;
    }

    @PostMapping("/document/upload")
    public ResponseEntity<?> uploadDocumentFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("unique_id") String uniqueId,
            @RequestParam("request_name") String requestName
    ) {
        try {
            // 🔹 1. 유저 조회 (유효성 검사)
            Member member = memberService.findByUniqueId(uniqueId);
            if (member == null) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "해당 unique_id를 가진 사용자가 없습니다."));
            }

            // 🔹 2. 파일 저장 (파일 저장이 성공해야만 DB 저장)
            String storedFileName = fileService.storeFile(file, "DOCUMENT");

            // 🔹 3. 문서 정보 저장 (파일 저장이 성공한 경우만)
            Document document = documentService.saveDocument(requestName, file, storedFileName, member);

            return ResponseEntity.ok(Collections.singletonMap("documentId", document.getId()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", "알 수 없는 오류가 발생했습니다."));
        }
    }

    @PostMapping("/signature/upload")
    public ResponseEntity<Map<String, String>> uploadSignatureFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "파일이 없습니다."));
        }

        // 🔹 서명 이미지 저장
        String fileName = fileService.storeFile(file, "SIGNATURE");

        return ResponseEntity.ok(Collections.singletonMap("filePath", fileName));
    }


}
