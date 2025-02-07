package com.example.backend.document.controller;

import com.example.backend.auth.dto.AuthDto;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // 요청한 문서 리스트
    @GetMapping("/requested-documents")
    public List<DocumentDTO> getRequestedDocuments() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String uniqueId = (principal instanceof AuthDto) ? ((AuthDto)principal).getUniqueId()  : null;
        if (uniqueId == null) {
            throw new IllegalStateException("Invalid user");
        }

        return documentService.getDocumentsByUniqueId(uniqueId);
    }

    @GetMapping("/received-documents")
    public List<DocumentDTO> getReceivedDocuments() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String email = (principal instanceof AuthDto) ? ((AuthDto)principal).getEmail()  : null;
        if (email == null) {
            throw new IllegalStateException("사용자의 이메일을 찾을 수 없습니다.");
        }

        return documentService.getDocumentsBySignerEmail(email);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getDocument(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        Resource resource = documentService.loadFileAsResource(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 읽어올 수 없습니다."));

        try {
            String encodedFileName = URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .header("Content-Type", "application/pdf")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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

    //서명용 문서 불러오기 (필터에서 예외처리 되어있음)
    @GetMapping("/sign/{id}")
    public ResponseEntity<Resource> getDocumentForSigning(@PathVariable Long id) throws UnsupportedEncodingException {
        Resource resource = documentService.loadFileAsResource(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 읽어올 수 없습니다."));

        String encodedFileName = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), String.valueOf(StandardCharsets.UTF_8))
                .replace("+", "%20"); // 공백 변환

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

}

