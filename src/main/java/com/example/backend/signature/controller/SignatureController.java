package com.example.backend.signature.controller;

import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.service.DocumentService;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.controller.request.SignatureFieldRequest;
import com.example.backend.signature.controller.response.SignatureFieldResponse;
import com.example.backend.signature.entity.Signature;
import com.example.backend.signature.service.SignatureService;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
public class SignatureController {

    @Value("${file.signature-dir}")
    private String signatureImageBasePath;
    private final SignatureService signatureService;
    private final DocumentRepository documentRepository;

    // 🔹 서명 요청에 연결된 서명 필드 조회
    // 🔹 특정 문서에서 특정 서명자의 서명 필드 조회
    @PostMapping("/fields")
    public ResponseEntity<SignatureFieldResponse> getSignatureFields(@RequestBody SignatureFieldRequest request) {
        List<SignatureDTO> signatureFields = signatureService.getSignatureFields(request.getDocumentId(), request.getSignerEmail());

        boolean hasExistingSignature = signatureService.hasExistingSignature(request.getSignerEmail());

        if (signatureFields.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        SignatureFieldResponse response = new SignatureFieldResponse(hasExistingSignature, signatureFields);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign")
    public ResponseEntity<String> saveSignatures(
            @RequestParam Long documentId,
            @RequestBody SignerDTO signerDTO) throws IOException {

        signatureService.saveSignatures(signerDTO, documentId);

        // 2. 문서 수정일 업데이트
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문서입니다."));;
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(document); // 변경 감지를 이용한 경우 생략 가능

        return ResponseEntity.ok("서명 정보가 성공적으로 저장되었습니다.");
    }

    @GetMapping(value = "/latest-image-signature", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getLatestImageSignature(@RequestParam String signerEmail) {
        Optional<Signature> result = signatureService.getLatestImageSignature(signerEmail);
        if (!result.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        String imagePath = signatureImageBasePath + "/" + result.get().getImageName();
        Path path = Paths.get(imagePath);

        if (!Files.exists(path)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        try {
            byte[] imageBytes = Files.readAllBytes(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .cacheControl(CacheControl.noCache().mustRevalidate())
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    //이전 서명 존재 여부 관련 API
    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkExistingSignature(@RequestParam String signerEmail) {
        boolean exists = signatureService.hasExistingSignature(signerEmail);
        return ResponseEntity.ok(exists);
    }
}
