package com.example.backend.signatureRequest.controller;

import com.example.backend.auth.util.EncryptionUtil;
import com.example.backend.document.entity.Document;
import com.example.backend.document.service.DocumentService;
import com.example.backend.mail.service.MailService;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.service.SignatureService;
import com.example.backend.signatureRequest.DTO.SignatureRequestDTO;
import com.example.backend.signatureRequest.DTO.SignatureRequestMailDTO;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.auth.controller.request.SignatureValidationRequest;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.signatureRequest.service.SignatureRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/signature-requests")
@RequiredArgsConstructor
public class SignatureRequestController {

    private final DocumentService documentService;
    private final SignatureService signatureService;
    private final SignatureRequestService signatureRequestService;
    private final MailService mailService;
    private final SignatureRequestRepository signatureRequestRepository;
    private final EncryptionUtil encryptionUtil;

    @PostMapping("/request")
    public ResponseEntity<String> sendSignatureRequest(@RequestBody SignatureRequestDTO requestDto) {
        // 1. 문서 조회
        Document document = documentService.getDocumentById(requestDto.getDocumentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        try {
        // 2. 서명 요청 생성 및 저장
        List<SignatureRequest> requests = signatureRequestService.createSignatureRequests(document, requestDto.getSigners(), requestDto.getPassword());

        // 4. 서명 필드 저장
        for (SignerDTO signer : requestDto.getSigners()) {
            for (SignatureDTO signatureField : signer.getSignatureFields()) {
                signatureService.createSignatureRegion(
                        document,
                        signer.getEmail(),
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
        mailService.sendSignatureRequestEmails(requestDto.getMemberName(), document.getRequestName(),requests, requestDto.getPassword());

        return ResponseEntity.ok("서명 요청이 성공적으로 생성되었습니다.");
        } catch (MailSendException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "이메일 전송에 실패했습니다. 이메일 주소를 확인하세요.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.toString());
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "서명 요청 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.toString());
        }
    }

    @PostMapping("/store")
    public ResponseEntity<String> storeSignatureRequest(@RequestBody SignatureRequestDTO requestDto) {
        // 1. 문서 조회
        Document document = documentService.getDocumentById(requestDto.getDocumentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        try {
            // 2. 서명 요청 생성 및 저장
            signatureRequestService.createSignatureRequests(document, requestDto.getSigners(), requestDto.getPassword());

            // 4. 서명 필드 저장
            for (SignerDTO signer : requestDto.getSigners()) {
                for (SignatureDTO signatureField : signer.getSignatureFields()) {
                    signatureService.createSignatureRegion(
                            document,
                            signer.getEmail(),
                            signatureField.getType(),
                            signatureField.getPosition().getPageNumber(),
                            signatureField.getPosition().getX(),
                            signatureField.getPosition().getY(),
                            signatureField.getWidth(),
                            signatureField.getHeight()
                    );
                }
            }

            return ResponseEntity.ok("서명 요청이 성공적으로 생성되었습니다.");
        }  catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "서명 요청 정보 저장 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.toString());
        }
    }

    @PostMapping("/send-mail")
    public ResponseEntity<String> sendSignatureRequestMail(@RequestBody SignatureRequestMailDTO requestDto) {
        Document document = documentService.getDocumentById(requestDto.getDocumentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        try {
            List<SignatureRequest> requests = signatureRequestService.getSignatureRequestsByDocument(document);

            if (requests.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("서명 요청이 존재하지 않습니다.");
            }

            mailService.sendSignatureRequestEmails(requestDto.getMemberName(), document.getRequestName(), requests, requestDto.getPassword());

            return ResponseEntity.ok("서명 요청 이메일이 성공적으로 발송되었습니다.");
        } catch (MailSendException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "이메일 전송에 실패했습니다. 이메일 주소를 확인하세요.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.toString());
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "서명 요청 이메일 발송 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse.toString());
        }
    }

    @PutMapping("/cancel/{documentId}")
    public ResponseEntity<String> cancelSignatureRequests(@PathVariable Long documentId,
                                                          @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.get("reason");

        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("취소 사유가 필요합니다.");
        }

        boolean isCancelled = signatureRequestService.cancelSignatureRequest(documentId, reason);

        if (isCancelled) {
            return ResponseEntity.ok("서명 요청이 취소되었습니다.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 문서를 찾을 수 없습니다.");
        }
    }

    @PutMapping("/reject/{documentId}")
    public ResponseEntity<String> rejectSignatureRequest(@PathVariable Long documentId,
                                                         @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.get("reason");
        String encryptedToken = requestBody.get("token"); // 🔹 클라이언트에서 전달된 암호화된 토큰
        String email = requestBody.get("email"); // 🔹 클라이언트에서 입력한 이메일

        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("거절 사유가 필요합니다.");
        }
        if (encryptedToken == null || email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효한 인증 정보가 필요합니다.");
        }

        try {
            // 🔹 암호화된 토큰 복호화
            String decryptedToken = encryptionUtil.decryptUUID(encryptedToken);

            // 🔹 토큰과 이메일 검증 (해당 문서의 서명 요청인지 확인)
            Optional<SignatureRequest> signatureRequestOpt = signatureRequestRepository.findByToken(decryptedToken);

            if (!signatureRequestOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("잘못된 서명 요청입니다.");
            }

            SignatureRequest signatureRequest = signatureRequestOpt.get();

            // 🔹 서명 요청을 받은 사용자만 거절 가능하도록 제한
            if (!signatureRequest.getSignerEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일이 일치하지 않습니다.");
            }

            // 🔹 요청 거절 처리
            boolean isRejected = signatureRequestService.rejectSignatureRequest(documentId, reason);

            if (isRejected) {
                return ResponseEntity.ok("요청이 거절되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("해당 문서를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("요청 처리 중 오류가 발생했습니다.");
        }
    }


    @GetMapping("/check")
    public ResponseEntity<?> checkSignatureRequestToken(@RequestParam String token) {
        try {
            System.out.println("token: " + token);
            String decryptedToken = encryptionUtil.decryptUUID(token);
            System.out.println("Decoded token: " + decryptedToken);
            SignatureRequest signatureRequest = signatureRequestRepository.findByToken(decryptedToken)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "잘못된 서명 요청입니다."));

            // 요청 만료 시간 확인 (410)
            if (signatureRequest.getExpiredAt().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.GONE).body("서명 요청이 만료되었습니다.");
            }

            // 서명 요청 상태 확인 (403)
            if (signatureRequest.getStatus() != 0) { // 0 = 대기 중
                Map<String, Object> response = new HashMap<>();
                response.put("message", "서명 요청을 진행할 수 없는 상태입니다.");
                response.put("status", signatureRequest.getStatus()); // 상태 값 추가

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 토큰이 유효하고 서명 요청이 대기 중이며 만료되지 않았다면 200 OK 반환
            Map<String, Object> response = new HashMap<>();
            response.put("message", "유효한 서명 요청입니다.");
            response.put("signerEmail", signatureRequest.getSignerEmail()); // 서명자 이메일 추가

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // 🔹 잘못된 토큰 (복호화 실패 또는 변조됨) → 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청 형식입니다.");

        } catch (ResponseStatusException e) {
            // 🔹 DB에서 토큰을 찾지 못했을 때 → 404 Not Found
            return ResponseEntity.status(e.getStatus()).body(e.getReason());

        } catch (Exception e) {
            // 🔹 예상하지 못한 서버 오류 → 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서명 요청 검증 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/document/{documentId}/signers")
    public ResponseEntity<List<Map<String, Object>>> getSignersByDocument(@PathVariable Long documentId) {
        List<Object[]> results = signatureRequestRepository.findSignerInfoWithSignedAt(documentId);

        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }

        List<Map<String, Object>> signers = results.stream().map(row -> {
            Map<String, Object> signerData = new HashMap<>();
            signerData.put("name", row[0]);
            signerData.put("email", row[1]);
            signerData.put("status", row[2]);
            signerData.put("signedAt", row[3]);
            return signerData;
        }).collect(Collectors.toList());

        log.debug("signers: {}", signers);
        return ResponseEntity.ok(signers);
    }


}
