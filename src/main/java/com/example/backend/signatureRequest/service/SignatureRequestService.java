package com.example.backend.signatureRequest.service;

import com.example.backend.signatureRequest.DTO.SignerDTO;
import com.example.backend.signatureRequest.entity.SignatureRequest;
import com.example.backend.signatureRequest.repository.SignatureRequestRepository;
import com.example.backend.document.entity.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SignatureRequestService {

    private final SignatureRequestRepository signatureRequestRepository;

    public SignatureRequestService(SignatureRequestRepository signatureRequestRepository) {
        this.signatureRequestRepository = signatureRequestRepository;
    }

    public List<SignatureRequest> createSignatureRequests(Document document, List<SignerDTO> signers) {
        List<SignatureRequest> requests = signers.stream().map(signer -> {
            String token = UUID.randomUUID().toString();
            return SignatureRequest.builder()
                    .document(document)
                    .signerEmail(signer.getEmail()) // 서명자 추가
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
}
