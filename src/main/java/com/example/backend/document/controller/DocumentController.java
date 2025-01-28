package com.example.backend.document.controller;

import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/list")
    public List<DocumentDTO> getAllDocuments() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String uniqueId;
        if (principal instanceof String) {  // uniqueId가 String으로 저장됨
            uniqueId = (String) principal;
        } else {
            throw new IllegalStateException("Unexpected principal type");
        }
        return documentService.getDocumentsByUniqueId(uniqueId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getDocument(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id);

        if (document != null) {
            String savedFileName = document.getSavedFileName();
            Path path = Paths.get(documentService.getStorageLocation().toString(), savedFileName);
            Resource file = new FileSystemResource(path.toString());

            if (file.exists()) {
                try {
                    // 파일명 URL 인코딩
                    String encodedFileName = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8.toString())
                            .replace("+", "%20"); // 공백을 `%20`으로 치환

                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName);

                    return ResponseEntity.ok()
                            .headers(headers)
                            .header("Content-Type", "application/pdf")
                            .body(file);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long documentId) {
        boolean deleted = documentService.deleteDocumentById(documentId);
        if (deleted) {
            return ResponseEntity.ok("문서 및 관련 서명 요청이 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 문서를 찾을 수 없습니다.");
        }
    }
}

