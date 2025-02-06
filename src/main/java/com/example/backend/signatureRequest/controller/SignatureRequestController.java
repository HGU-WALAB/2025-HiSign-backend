package com.example.backend.signatureRequest.controller;

import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.service.DocumentService;
import com.example.backend.mail.service.MailService;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.repository.SignatureRepository;
import com.example.backend.signature.service.SignatureService;
import com.example.backend.signatureRequest.DTO.DocumentWithSignatureFieldsDTO;
import com.example.backend.signatureRequest.DTO.SignatureRequestDTO;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.signatureRequest.controller.request.SignatureValidationRequest;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.signatureRequest.service.SignatureRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Signature;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/signature-requests")
public class SignatureRequestController {

    private final DocumentService documentService;
    private final SignatureService signatureService;
    private final SignatureRequestService signatureRequestService;
    private final MailService mailService;
    private final SignatureRequestRepository signatureRequestRepository;
    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;

    public SignatureRequestController(DocumentService documentService,
                                      SignatureService signatureService,
                                      SignatureRequestService signatureRequestService,
                                      MailService mailService,
                                      SignatureRequestRepository signatureRequestRepository,
                                      DocumentRepository documentRepository,
                                      SignatureRepository signatureRepository) {
        this.documentService = documentService;
        this.signatureService = signatureService;
        this.signatureRequestService = signatureRequestService;
        this.mailService = mailService;
        this.signatureRequestRepository = signatureRequestRepository;
        this.documentRepository = documentRepository;
        this.signatureRepository = signatureRepository;
    }

    @PostMapping("/request")
    public ResponseEntity<String> sendSignatureRequest(@RequestBody SignatureRequestDTO requestDto) {
        // 1. 문서 조회
        Document document = documentService.getDocumentById(requestDto.getDocumentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        // 2. 서명 요청 생성 및 저장
        List<SignatureRequest> requests = signatureRequestService.createSignatureRequests(document, requestDto.getSigners());

        // 4. 서명 필드 저장
        for (SignerDTO singer : requestDto.getSigners()) {
            for (SignatureDTO signatureField : singer.getSignatureFields()) {
                signatureService.createSignatureRegion(
                        document,
                        singer.getEmail(),
                        signatureField.getType(),
                        signatureField.getPosition().getPageNumber(),
                        signatureField.getPosition().getX(),
                        signatureField.getPosition().getY(),
                        signatureField.getWidth(),
                        signatureField.getHeight()
                );
            }
        }

        //5. 메일 전송
        mailService.sendSignatureRequestEmails(requests);

        return ResponseEntity.ok("서명 요청이 성공적으로 생성되었습니다.");
    }

    @PutMapping("/cancel/{documentId}")
    public ResponseEntity<String> cancelSignatureRequests(@PathVariable Long documentId) {
        System.out.println("API 호출됨: /cancel/" + documentId);

        boolean isCancelled = documentService.cancelRequest(documentId);
        int cancelledCount = signatureRequestService.cancelSignatureRequestsByDocumentId(documentId);

        if (cancelledCount > 0 && isCancelled) {
            System.out.println("서명 요청 취소됨: " + cancelledCount + "개, documentId: " + documentId);
            return ResponseEntity.ok("총 " + cancelledCount + "개의 서명 요청이 취소되었습니다.");
        } else {
            System.out.println("취소할 서명 요청을 찾을 수 없음: documentId=" + documentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 문서에 대한 서명 요청을 찾을 수 없습니다.");
        }
    }

    @PutMapping("/reject/{documentId}")
    public ResponseEntity<String> rejectSignatureRequestsByDocument(@PathVariable Long documentId) {
        System.out.println("[API 호출] 요청 거절 - 문서 ID: " + documentId);

        int rejectedCount = signatureRequestService.rejectSignatureRequestsByDocumentId(documentId);

        if (rejectedCount > 0) {
            System.out.println("[성공] 서명 요청 거절됨: " + rejectedCount + "개 (document_id=" + documentId + ")");
            return ResponseEntity.ok("총 " + rejectedCount + "개의 서명 요청이 거절되었습니다.");
        } else {
            System.out.println("[실패] 거절할 서명 요청을 찾을 수 없음 - document_id=" + documentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 문서에 대한 서명 요청을 찾을 수 없습니다.");
        }
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkSignatureRequestToken(@RequestParam String token) {
        System.out.println("checkSignatureRequestToken: " + token);
        Optional<SignatureRequest> signatureRequestOpt = signatureRequestRepository.findByToken(token);

        // 1️⃣ 토큰이 존재하지 않는 경우
        if (!signatureRequestOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("잘못된 서명 요청입니다.");
        }

        // 2️⃣ 토큰이 유효하면 200 OK 반환
        return ResponseEntity.ok("유효한 서명 요청입니다.");
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateSignatureRequest(@RequestBody SignatureValidationRequest request) {
        Optional<SignatureRequest> signatureRequestOpt = signatureRequestRepository.findByToken(request.getToken());

        // 1️⃣ 토큰이 존재하는지 확인
        if (!signatureRequestOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 요청입니다.");
        }

        SignatureRequest signatureRequest = signatureRequestOpt.get();

        // 2️⃣ 이메일 검증 (해당 서명 요청을 받은 사용자인지 확인)
        if (!signatureRequest.getSignerEmail().equals(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일이 일치하지 않습니다.");
        }

        // 3️⃣ 요청 만료 시간 확인
        if (signatureRequest.getExpiredAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("서명 요청이 만료되었습니다.");
        }

        // 4️⃣ 서명 요청 상태 확인 (대기 중(0)이 아닐 경우 차단)
        if (signatureRequest.getStatus() != 0) { // 0 = 대기 중
            Map<String, Object> response = new HashMap<>();
            response.put("message", "서명 요청을 진행할 수 없는 상태입니다.");
            response.put("status", signatureRequest.getStatus()); // 상태 값 추가

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // 5️⃣ 문서 ID 반환
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", signatureRequest.getDocument().getId());

        return ResponseEntity.ok(response);
    }
}
