package com.example.backend.document.controller;

import com.example.backend.auth.dto.AuthDto;
import com.example.backend.document.dto.DocumentDTO;
import com.example.backend.document.dto.RejectRequestDTO;
import com.example.backend.document.dto.UploadRequestDTO;
import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.file.service.FileService;
import com.example.backend.member.entity.Member;
import com.example.backend.member.service.MemberService;
import com.example.backend.signatureRequest.service.SignatureRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final FileService fileService;
    private final DocumentService documentService;
    private final MemberService memberService;
    private final SignatureRequestService signatureRequestService;


    @PostMapping(value = "/full-upload", consumes = {"multipart/form-data"})
    @Transactional
    public ResponseEntity<String> fullUpload(
            @RequestParam("file") MultipartFile file,
            @RequestPart("dto") UploadRequestDTO dto
    ) {
        try {
            // 1. 파일 저장
            String storedFileName = fileService.storeFile(file, "DOCUMENT");

            // 2. 업로드한 사용자 조회
            Member owner = memberService.findByUniqueId(dto.getUniqueId());

            // 3. 문서 생성 및 저장
            Document document = new Document();
            document.setRequestName(dto.getRequestName());
            document.setFileName(file.getOriginalFilename());
            document.setSavedFileName(storedFileName);
            document.setStatus(0);
            document.setIsRejectable(dto.getIsRejectable());
            document.setDescription(dto.getDescription());
            document.setType(dto.getType());
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            document.setMember(owner);

            documentService.save(document);

            // 4. 타입에 따라 분기
            if (document.getType() == 1) {
                // 타입 1 → 검토 요청만 (메일 ❌)
                documentService.requestCheckingById(document.getId());
                signatureRequestService.saveSignatureRequestAndFields(document, dto.getSigners(), dto.getPassword());
            } else {
                // 타입 1이 아닐 경우 → 저장 + 메일 발송
                signatureRequestService.saveRequestsAndSendMail(document, dto.getSigners(), dto.getPassword(), dto.getMemberName());
            }

            return ResponseEntity.ok("문서 업로드 및 서명 요청이 성공적으로 처리되었습니다.");

        } catch (Exception e) {
            log.error("❌ fullUpload 실패", e);
            throw new RuntimeException("fullUpload 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
    // 요청한 문서 리스트
    @GetMapping("/requested-documents")
    public List<Map<String, Object>> getRequestedDocuments(@RequestParam(value = "searchQuery", required = false) String searchQuery) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthDto)) {
            throw new IllegalStateException("사용자의 인증 정보가 유효하지 않습니다.");
        }

        AuthDto authDto = (AuthDto) authentication.getPrincipal();
        String uniqueId = authDto.getUniqueId();

        if (uniqueId == null) {
            throw new IllegalStateException("사용자의 고유 ID를 찾을 수 없습니다.");
        }

        log.debug("요청한 문서 리스트 요청 - UniqueId: {}", uniqueId);

        List<Map<String, Object>> documents = documentService.getDocumentsByUniqueId(uniqueId);

        if (searchQuery != null && !searchQuery.isEmpty()) {
            documents = documents.stream()
                    .filter(doc -> doc.get("requestName").toString().toLowerCase().contains(searchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }
        return documents;
    }


    @GetMapping("/received-with-requester")
    public List<Map<String, Object>> getReceivedDocuments(@RequestParam(value = "searchQuery", required = false) String searchQuery) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof AuthDto)) {
            throw new IllegalStateException("사용자의 이메일 정보를 찾을 수 없습니다.");
        }

        String email = ((AuthDto) principal).getEmail();

        System.out.println("[DEBUG] 요청받은 문서 리스트 요청 - 이메일: " + email);

        List<Map<String, Object>> documents = documentService.getDocumentsWithRequesterInfoBySignerEmail(email);

        if (searchQuery != null && !searchQuery.isEmpty()) {
            documents = documents.stream()
                    .filter(doc -> doc.get("requestName").toString().toLowerCase().contains(searchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return documents;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id) {
        boolean deleted = documentService.deleteDocumentById(id);
        if (deleted) {
            return ResponseEntity.ok("문서 및 관련 서명 요청이 성공적으로 삭제되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 문서를 찾을 수 없습니다.");
        }
    }

    //서명용 문서 불러오기
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


    @GetMapping("/info/{id}")
    public ResponseEntity<Map<String, Object>> getDocumentInfo(@PathVariable Long id) {
        Map<String, Object> documentInfo = documentService.getDocumentInfo(id);
        return ResponseEntity.ok(documentInfo);
    }

    @GetMapping("/admin_document")
    public ResponseEntity<List<Map<String, Object>>> getAdminDocuments(@RequestParam(value = "searchQuery", required = false) String searchQuery) {
        List<Map<String, Object>> documents = documentService.getAllAdminDocuments();

        if (searchQuery != null && !searchQuery.isEmpty()) {
            documents = documents.stream()
                    .filter(doc -> doc.get("requestName").toString().toLowerCase().contains(searchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/request-check/{id}")
    public ResponseEntity<String> getDocumentForRequestCheck(@PathVariable Long id) {
        boolean requested = documentService.requestCheckingById(id);
        if (requested) {
            return ResponseEntity.ok("문서에 대한 검토가 성공적으로 요청되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 문서를 찾을 수 없습니다.");
        }
    }

    @GetMapping("/{id}/title")
    public ResponseEntity<String> getDocumentTitle(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        return ResponseEntity.ok(document.getRequestName());
    }

    @PutMapping("/{documentId}/reject")
    public ResponseEntity<?> rejectDocumentReview(
            @PathVariable Long documentId,
            @RequestBody RejectRequestDTO rejectRequestDTO) {

        documentService.rejectDocument(documentId, rejectRequestDTO.getReason());
        return ResponseEntity.ok("문서가 성공적으로 반려 처리되었습니다.");
    }
}


