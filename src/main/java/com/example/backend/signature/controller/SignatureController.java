package com.example.backend.signature.controller;

import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.controller.request.SignatureFieldRequest;
import com.example.backend.signature.service.SignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signature")
public class SignatureController {
    private final SignatureService signatureService;

    @Autowired
    public SignatureController(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

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

}
