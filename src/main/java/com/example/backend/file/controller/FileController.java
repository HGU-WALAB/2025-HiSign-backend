package com.example.backend.file.controller;

import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
import com.example.backend.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
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

    @PostMapping("/signature/upload")
    public ResponseEntity<Map<String, String>> uploadSignatureFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "파일이 없습니다."));
        }

        // 🔹 서명 이미지 저장
        String fileName = fileService.storeFile(file, "SIGNATURE");

        return ResponseEntity.ok(Collections.singletonMap("fileName", fileName));
    }

    // 과목 목록 조회
    @GetMapping("/subjects")
    public ResponseEntity<List<String>> getSubjectList() throws IOException {
        List<String> subjectList = fileService.readSubjectList();
        return ResponseEntity.ok(subjectList);
    }

    // 과목 목록 저장
    @PostMapping("/subjects")
    public ResponseEntity<String> saveSubjectList(@RequestBody String content) {
        try {
            fileService.saveSubjectList(content);
            return ResponseEntity.ok("과목 목록이 저장되었습니다.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("저장 중 오류 발생");
        }
    }

}
