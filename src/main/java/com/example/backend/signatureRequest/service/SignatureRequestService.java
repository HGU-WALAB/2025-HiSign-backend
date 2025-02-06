package com.example.backend.signatureRequest.service;

import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.document.repository.DocumentRepository;
import com.example.backend.document.entity.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SignatureRequestService {

    private final SignatureRequestRepository signatureRequestRepository;
    private final DocumentRepository documentRepository;

    public SignatureRequestService(SignatureRequestRepository signatureRequestRepository,
                                   DocumentRepository documentRepository) {
        this.signatureRequestRepository = signatureRequestRepository;
        this.documentRepository = documentRepository;
    }

    public List<SignatureRequest> createSignatureRequests(Document document, List<SignerDTO> signers) {
        List<SignatureRequest> requests = signers.stream().map(signer -> {
            String token = UUID.randomUUID().toString();
            return SignatureRequest.builder()
                    .document(document)
                    .signerEmail(signer.getEmail())
                    .signerName(signer.getName())
                    .token(token)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusDays(7))
                    .status(0)  // 대기 상태
                    .build();
        }).collect(Collectors.toList());

        return signatureRequestRepository.saveAll(requests);
    }

    @Transactional
    public int cancelSignatureRequestsByDocumentId(Long documentId) {
        List<SignatureRequest> requests = signatureRequestRepository.findByDocumentId(documentId);

        if (requests.isEmpty()) {
            return 0;
        }

        requests.forEach(request -> request.setStatus(3)); // 3: 요청자 취소
        signatureRequestRepository.saveAll(requests);

        return requests.size();
    }

    @Transactional
    public int rejectSignatureRequestsByDocumentId(Long documentId) {
        int updatedRequestRows = signatureRequestRepository.updateRequestStatusToRejected(documentId);
        int updatedDocumentRows = documentRepository.updateDocumentStatusToRejected(documentId);

        System.out.println("[업데이트 완료] 서명 요청: " + updatedRequestRows + "개, 문서: " + updatedDocumentRows + "개 변경됨 (document_id=" + documentId + ")");

        if (updatedRequestRows == 0 || updatedDocumentRows == 0) {
            System.out.println("[실패] 거절할 서명 요청 또는 문서를 찾을 수 없음 - document_id=" + documentId);
            return 0;
        }

        return updatedRequestRows;
    }
}
