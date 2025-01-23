package com.example.backend.file.controller;

import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
import com.example.backend.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final DocumentService documentService;
    private final Path documentStorageLocation;
    private final MemberRepository memberRepository;

    public FileController(
            FileService fileService,
            DocumentService documentService,
            @Value("${file.document-dir}") String documentDir,
            MemberRepository memberRepository) {
        this.fileService = fileService;
        this.documentService = documentService;
        this.documentStorageLocation = fileService.createDirectory(documentDir);
        this.memberRepository = memberRepository;
    }

    @PostMapping("/document/upload")
    public ResponseEntity<Document> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("unique_id") String uniqueId
    ) {
        try {
            Member member = (Member) memberRepository.findByUniqueId(uniqueId)
                    .orElseThrow(() -> new IllegalArgumentException("Member not found"));

            // 1. 파일 저장
            String StoredFileName = fileService.storeFile(file.getBytes(), file.getOriginalFilename(), documentStorageLocation);
            // 2. 문서 정보 저장
            Document document = documentService.saveDocument(file, StoredFileName, member);

            return ResponseEntity.ok(document);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
