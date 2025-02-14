package com.example.backend.signature.service;

import com.example.backend.document.entity.Document;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.service.DocumentService;
import com.example.backend.member.entity.Member;
import com.example.backend.signature.DTO.SignatureDTO;
import com.example.backend.signature.entity.Signature;
import com.example.backend.signature.repository.SignatureRepository;
import com.example.backend.signatureRequest.DTO.SignerDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SignatureService {

    private final DocumentService documentService;
    private final SignatureRepository signatureRepository;

    public SignatureService(SignatureRepository signatureRepository, DocumentService documentService) {
        this.signatureRepository = signatureRepository;
        this.documentService = documentService;
    }

    public void createSignatureRegion(Document document, String signerEmail, int type, int pageNumber, float x, float y, float width, float height) {
        Signature signature = Signature.builder()
                .document(document)
                .signerEmail(signerEmail)
                .signedAt(null)
                .type(type)
                .imageName(null)
                .textData(null)
                .status(0)  // 대기 상태 설정
                .pageNumber(pageNumber)
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .build();

        signatureRepository.save(signature);
    }

    public List<SignatureDTO> getSignatureFields(Long documentId, String signerEmail) {
        return signatureRepository.findByDocumentIdAndSignerEmail(documentId, signerEmail)
                .stream()
                .map(SignatureDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveSignatures(SignerDTO signerDTO, Long documentId) {
        // 📌 문서 정보 조회 (없는 경우 예외 처리)
        Document document = documentService.getDocumentById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서를 찾을 수 없습니다. ID: " + documentId));

        List<Signature> signatures = signerDTO.getSignatureFields().stream()
                .map(dto -> Signature.builder()
                        .document(document)
                        .signerEmail(dto.getSignerEmail())
                        .type(dto.getType())
                        .imageName(dto.getImageName())
                        .textData(dto.getTextData())
                        .status(1)
                        .pageNumber(dto.getPosition().getPageNumber())
                        .x(dto.getPosition().getX())
                        .y(dto.getPosition().getY())
                        .width(dto.getWidth())
                        .height(dto.getHeight())
                        .description(null)
                        .build()
                ).collect(Collectors.toList());

        signatureRepository.saveAll(signatures);
    }
}
