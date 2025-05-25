package com.example.backend.signature.controller;

import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.controller.request.SignatureFieldRequest;
import com.example.backend.signature.service.SignatureService;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    // 🔹 서명 요청에 연결된 서명 필드 조회
    // 🔹 특정 문서에서 특정 서명자의 서명 필드 조회
    @PostMapping("/fields")
    public ResponseEntity<List<SignatureDTO>> getSignatureFields(@RequestBody SignatureFieldRequest request) {
        List<SignatureDTO> signatureFields = signatureService.getSignatureFields(request.getDocumentId(), request.getSignerEmail());

        if (signatureFields.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(signatureFields);
    }

    @PostMapping("/sign")
    public ResponseEntity<String> saveSignatures(
            @RequestParam Long documentId,
            @RequestBody SignerDTO signerDTO) throws IOException {

        signatureService.saveSignatures(signerDTO, documentId);

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

}
