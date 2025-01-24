package com.example.backend.signature.service;

import com.example.backend.document.entity.Document;
import com.example.backend.member.entity.Member;
import com.example.backend.signature.entity.Signature;
import com.example.backend.signature.repository.SignatureRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SignatureService {

    private final SignatureRepository signatureRepository;

    public SignatureService(SignatureRepository signatureRepository) {
        this.signatureRepository = signatureRepository;
    }

    public void createSignatureRegion(Document document, Member signer, int type, int pageNumber, float x, float y, float width, float height) {
        Signature signature = Signature.builder()
                .document(document)
                .member(signer)
                .signedAt(null)
                .type(type)
                .image_data(null)
                .text_data(null)
                .status(0)  // 대기 상태 설정
                .pageNumber(pageNumber)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .build();

        signatureRepository.save(signature);
    }

    public Signature saveSignature(Document document, Member signer, int type, String imageData, String textData) {
        Signature signature = Signature.builder()
                .document(document)
                .member(signer)
                .signedAt(LocalDateTime.now())
                .type(type)
                .image_data(imageData)
                .text_data(textData)
                .status(1)  // 완료 상태 설정
                .build();

        return signatureRepository.save(signature);
    }
}
