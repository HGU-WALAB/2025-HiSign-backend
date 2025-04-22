package com.example.backend.auth.controller;

import com.example.backend.auth.controller.request.SignatureValidationRequest;
import com.example.backend.auth.util.EncryptionUtil;
import com.example.backend.auth.util.SignerJwtUtil;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.signatureRequest.service.SignatureRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/signer")
@RequiredArgsConstructor
public class SignerAuthController {

    private final SignatureRequestService signatureRequestService;
    private final SignatureRequestRepository signatureRequestRepository;
    private final EncryptionUtil encryptionUtil;

    @Value("${custom.jwt.secret}")
    private String jwtSecret;

    @PostMapping("/validate")
    public ResponseEntity<?> validateAndIssueToken(@RequestBody SignatureValidationRequest request) {
        try {
            String decryptedToken = encryptionUtil.decryptUUID(request.getToken());

            SignatureRequest signatureRequest = signatureRequestRepository.findByToken(decryptedToken)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "잘못된 서명 요청입니다."));

            if (!signatureRequest.getSignerEmail().equals(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일이 일치하지 않습니다.");
            }

            String jwt = SignerJwtUtil.createToken(
                    signatureRequest.getSignerEmail(),
                    signatureRequest.getDocument().getId(),
                    jwtSecret,
                    60 * 30 // 30분
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("documentId", signatureRequest.getDocument().getId());
            response.put("documentName", signatureRequest.getDocument().getFileName());
            response.put("signerName", signatureRequest.getSignerName());

            return ResponseEntity.ok(response); 
        } catch (IllegalArgumentException e) {
            // 🔹 복호화 실패 → 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청 형식입니다.");

        } catch (ResponseStatusException e) {
            // 🔹 토큰이 DB에 없음 → 404 Not Found
            return ResponseEntity.status(e.getStatus()).body(e.getReason());

        } catch (Exception e) {
            // 🔹 예상치 못한 서버 오류 → 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서명 요청 검증 중 오류가 발생했습니다.");
        }
    }

//    @PostMapping("/validate")
//    public ResponseEntity<?> validateSignatureRequest(@RequestBody SignatureValidationRequest request) {
//        try {
//            // 🔹 암호화된 토큰 복호화
//            String decryptedToken = encryptionUtil.decryptUUID(request.getToken());
//
//            // 🔹 복호화된 토큰을 사용하여 서명 요청 조회
//            SignatureRequest signatureRequest = signatureRequestRepository.findByToken(decryptedToken)
//                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "잘못된 서명 요청입니다."));
//
//            // 🔹 이메일 검증 (해당 서명 요청을 받은 사용자인지 확인)
//            if (!signatureRequest.getSignerEmail().equals(request.getEmail())) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("이메일이 일치하지 않습니다.");
//            }
//
//            // ✅ 유효한 서명 요청이면 문서 정보 반환
//            Map<String, Object> response = new HashMap<>();
//            response.put("documentId", signatureRequest.getDocument().getId());
//            response.put("documentName", signatureRequest.getDocument().getFileName());
//            response.put("signerName", signatureRequest.getSignerName());
//            response.put("requesterName", signatureRequest.getDocument().getMember().getName());
//            response.put("requestName", signatureRequest.getDocument().getRequestName());
//            response.put("description", signatureRequest.getDocument().getDescription());
//            response.put("isRejectable", signatureRequest.getDocument().getIsRejectable());
//
//            return ResponseEntity.ok(response);
//
//        } catch (IllegalArgumentException e) {
//            // 🔹 복호화 실패 → 400 Bad Request
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 요청 형식입니다.");
//
//        } catch (ResponseStatusException e) {
//            // 🔹 토큰이 DB에 없음 → 404 Not Found
//            return ResponseEntity.status(e.getStatus()).body(e.getReason());
//
//        } catch (Exception e) {
//            // 🔹 예상치 못한 서버 오류 → 500 Internal Server Error
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서명 요청 검증 중 오류가 발생했습니다.");
//        }
//    }
}

