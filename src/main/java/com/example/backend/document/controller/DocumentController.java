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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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
    public List<Map<String, Object>> getRequestedDocuments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthDto)) {
            throw new IllegalStateException("사용자의 인증 정보가 유효하지 않습니다.");
        }

        AuthDto authDto = (AuthDto) authentication.getPrincipal();
        String uniqueId = authDto.getUniqueId();

        if (uniqueId == null) {
            throw new IllegalStateException("사용자의 고유 ID를 찾을 수 없습니다.");
        }

        System.out.println("[DEBUG] 요청한 문서 리스트 요청 - UniqueId: " + uniqueId);

        return documentService.getDocumentsByUniqueId(uniqueId);
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

        // 문서의 원본 파일명 조회
        String originalFileName = documentService.getOriginalFileName(id);
        String encodedFileName = URLEncoder.encode(Objects.requireNonNull(originalFileName), String.valueOf(StandardCharsets.UTF_8))
                .replace("+", "%20"); // 공백 변환

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @GetMapping("/received-with-requester")
    public List<Map<String, Object>> getReceivedDocuments() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof AuthDto)) {
            throw new IllegalStateException("사용자의 이메일 정보를 찾을 수 없습니다.");
        }

        String email = ((AuthDto) principal).getEmail();

        System.out.println("[DEBUG] 요청받은 문서 리스트 요청 - 이메일: " + email);

        return documentService.getDocumentsWithRequesterInfoBySignerEmail(email);
    }

}

